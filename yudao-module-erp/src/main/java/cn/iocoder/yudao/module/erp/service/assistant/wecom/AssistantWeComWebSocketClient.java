package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 企业微信智能机器人 WebSocket 长连接。 */
@Service
@Slf4j
public class AssistantWeComWebSocketClient {

    private static final long LEASE_SECONDS = 90L;
    private static final int MAX_AUTH_FAILURES = 5;
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('expire',KEYS[1],ARGV[2]) else return 0 end", Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end", Long.class);

    @Resource private WeComProperties properties;
    @Resource(name = "stringRedisTemplate") private StringRedisTemplate redisTemplate;
    @Resource private ApplicationEventPublisher eventPublisher;

    private final String instanceId = UUID.randomUUID().toString();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Deque<PendingFrame>> replyQueues = new HashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingTimeouts = new HashMap<>();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private OkHttpClient httpClient;
    private volatile WebSocket webSocket;
    private volatile boolean leader;
    private volatile boolean authenticated;
    private volatile boolean shuttingDown;
    private volatile boolean authStopped;
    private volatile int reconnectAttempts;
    private volatile int authFailures;
    private volatile int missedHeartbeats;
    private volatile String authRequestId;

    @PostConstruct
    public void start() {
        WeComProperties.AiBotProperties bot = properties.getAiBot();
        if (bot == null || !bot.isEnabled()) {
            return;
        }
        if (StrUtil.hasBlank(bot.getBotId(), bot.getSecret())) {
            log.warn("企业微信智能问数机器人未启动：BotID 或 Secret 未配置（敏感配置不会输出）");
            return;
        }
        httpClient = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build();
        scheduler.scheduleWithFixedDelay(this::maintainLeaseSafely, 0, 30, TimeUnit.SECONDS);
        long heartbeatSeconds = Math.max(5L, duration(bot.getHeartbeatInterval(), Duration.ofSeconds(30)).getSeconds());
        scheduler.scheduleWithFixedDelay(this::heartbeatSafely, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        shuttingDown = true;
        closeSocket("service stopping");
        releaseLease();
        clearPending(new IllegalStateException("企业微信连接已关闭"));
        scheduler.shutdownNow();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    public boolean isAvailable() {
        return leader && authenticated && webSocket != null;
    }

    public CompletableFuture<JsonNode> replyStream(String requestId, String streamId, String content, boolean finish) {
        Map<String, Object> stream = new LinkedHashMap<>();
        stream.put("id", streamId);
        stream.put("finish", finish);
        stream.put("content", content);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "stream");
        body.put("stream", stream);
        return enqueue("aibot_respond_msg", requestId, body);
    }

    public CompletableFuture<JsonNode> sendMarkdown(String chatId, String content) {
        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("content", content);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chatid", chatId);
        body.put("msgtype", "markdown");
        body.put("markdown", markdown);
        return enqueue("aibot_send_msg", reqId("aibot_send_msg"), body);
    }

    private void maintainLeaseSafely() {
        try {
            maintainLease();
        } catch (Exception exception) {
            log.warn("企业微信机器人连接租约维护失败：{}", exception.getMessage());
            loseLeadership();
        }
    }

    private void maintainLease() {
        if (shuttingDown || authStopped) return;
        String key = leaseKey();
        if (!leader) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, instanceId, LEASE_SECONDS, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(acquired)) {
                leader = true;
                reconnectAttempts = 0;
                connect();
            }
            return;
        }
        Long renewed = redisTemplate.execute(RENEW_SCRIPT, Collections.singletonList(key), instanceId,
                String.valueOf(LEASE_SECONDS));
        if (!Long.valueOf(1L).equals(renewed)) {
            log.warn("企业微信机器人连接租约已丢失，立即关闭当前连接");
            loseLeadership();
        } else if (webSocket == null && !reconnectScheduled.get()) {
            connect();
        }
    }

    private synchronized void connect() {
        if (!leader || shuttingDown || authStopped || webSocket != null || httpClient == null) return;
        Request request = new Request.Builder().url(properties.getAiBot().getWebsocketUrl()).build();
        webSocket = httpClient.newWebSocket(request, new Listener());
    }

    private void sendAuth(WebSocket socket) {
        String requestId = reqId("aibot_subscribe");
        authRequestId = requestId;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", properties.getAiBot().getBotId());
        body.put("secret", properties.getAiBot().getSecret());
        socket.send(frame("aibot_subscribe", requestId, body));
    }

    private void heartbeatSafely() {
        try {
            if (!isAvailable()) return;
            if (missedHeartbeats >= 2) {
                log.warn("企业微信机器人连续未收到心跳回执，重新建立连接");
                closeSocket("heartbeat timeout");
                scheduleReconnect(false);
                return;
            }
            missedHeartbeats++;
            WebSocket socket = webSocket;
            if (socket != null) socket.send(frame("ping", reqId("ping"), null));
        } catch (Exception exception) {
            log.warn("企业微信机器人心跳发送失败：{}", exception.getMessage());
        }
    }

    private synchronized CompletableFuture<JsonNode> enqueue(String cmd, String requestId, Map<String, Object> body) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        if (!isAvailable()) {
            future.completeExceptionally(new IllegalStateException("企业微信机器人连接不可用"));
            return future;
        }
        Deque<PendingFrame> queue = replyQueues.computeIfAbsent(requestId, ignored -> new ArrayDeque<>());
        if (queue.size() >= 100) {
            future.completeExceptionally(new IllegalStateException("企业微信回复队列已满"));
            return future;
        }
        queue.addLast(new PendingFrame(frame(cmd, requestId, body), future));
        if (queue.size() == 1) sendQueueHead(requestId);
        return future;
    }

    private synchronized void sendQueueHead(String requestId) {
        Deque<PendingFrame> queue = replyQueues.get(requestId);
        if (queue == null || queue.isEmpty()) {
            replyQueues.remove(requestId);
            return;
        }
        WebSocket socket = webSocket;
        if (socket == null || !socket.send(queue.peekFirst().payload)) {
            PendingFrame failed = queue.removeFirst();
            failed.future.completeExceptionally(new IllegalStateException("企业微信回复发送失败"));
            sendQueueHead(requestId);
            return;
        }
        ScheduledFuture<?> timeout = scheduler.schedule(() -> ackTimeout(requestId), 5, TimeUnit.SECONDS);
        ScheduledFuture<?> old = pendingTimeouts.put(requestId, timeout);
        if (old != null) old.cancel(false);
    }

    private synchronized void handleAck(String requestId, JsonNode frame) {
        Deque<PendingFrame> queue = replyQueues.get(requestId);
        if (queue == null || queue.isEmpty()) return;
        ScheduledFuture<?> timeout = pendingTimeouts.remove(requestId);
        if (timeout != null) timeout.cancel(false);
        PendingFrame pending = queue.removeFirst();
        if (frame.path("errcode").asInt(-1) == 0) pending.future.complete(frame);
        else pending.future.completeExceptionally(new IllegalStateException("企业微信回复回执失败：" + frame.path("errmsg").asText("unknown")));
        sendQueueHead(requestId);
    }

    private synchronized void ackTimeout(String requestId) {
        pendingTimeouts.remove(requestId);
        Deque<PendingFrame> queue = replyQueues.get(requestId);
        if (queue == null || queue.isEmpty()) return;
        queue.removeFirst().future.completeExceptionally(new TimeoutException("企业微信回复回执超时"));
        sendQueueHead(requestId);
    }

    private synchronized void clearPending(Exception reason) {
        pendingTimeouts.values().forEach(timeout -> timeout.cancel(false));
        pendingTimeouts.clear();
        replyQueues.values().forEach(queue -> queue.forEach(item -> item.future.completeExceptionally(reason)));
        replyQueues.clear();
    }

    private void handleFrame(String text) {
        JsonNode frame;
        try {
            frame = JsonUtils.parseTree(text);
        } catch (RuntimeException exception) {
            log.warn("忽略无法解析的企业微信 WebSocket 消息");
            return;
        }
        String cmd = frame.path("cmd").asText("");
        String requestId = frame.path("headers").path("req_id").asText("");
        if ("aibot_msg_callback".equals(cmd) || "aibot_event_callback".equals(cmd)) {
            if ("disconnected_event".equals(frame.path("body").path("event").path("eventtype").asText())) {
                log.warn("企业微信服务端通知当前机器人连接已被新连接替换");
                releaseLease();
                loseLeadership();
                return;
            }
            eventPublisher.publishEvent(new AssistantWeComInboundEvent(frame));
            return;
        }
        if (requestId.equals(authRequestId)) {
            if (frame.path("errcode").asInt(-1) == 0) {
                authenticated = true;
                authFailures = 0;
                reconnectAttempts = 0;
                missedHeartbeats = 0;
                log.info("企业微信智能问数机器人长连接认证成功");
            } else {
                authenticated = false;
                authFailures++;
                log.error("企业微信智能问数机器人认证失败（第 {} 次，错误码 {}）", authFailures,
                        frame.path("errcode").asInt(-1));
                if (authFailures >= MAX_AUTH_FAILURES) {
                    authStopped = true;
                    log.error("企业微信智能问数机器人连续认证失败 5 次，已停止重连；请修正配置后重启服务");
                }
                closeSocket("authentication failed");
                if (!authStopped) scheduleReconnect(true);
            }
            return;
        }
        if (requestId.startsWith("ping_")) {
            if (frame.path("errcode").asInt(-1) == 0) missedHeartbeats = 0;
            return;
        }
        handleAck(requestId, frame);
    }

    private void scheduleReconnect(boolean authenticationFailure) {
        if (!leader || shuttingDown || authStopped || !reconnectScheduled.compareAndSet(false, true)) return;
        int attempt = authenticationFailure ? Math.max(1, authFailures) : ++reconnectAttempts;
        long delay = reconnectDelaySeconds(attempt);
        scheduler.schedule(() -> {
            reconnectScheduled.set(false);
            if (leader && !shuttingDown && !authStopped) connect();
        }, delay, TimeUnit.SECONDS);
    }

    private synchronized void closeSocket(String reason) {
        authenticated = false;
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) socket.close(1000, reason);
        clearPending(new IllegalStateException("企业微信连接已断开"));
    }

    private void loseLeadership() {
        leader = false;
        closeSocket("lease lost");
    }

    private void releaseLease() {
        try {
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(leaseKey()), instanceId);
        } catch (Exception ignored) {
            // 服务退出时 Redis 可能已先关闭。
        }
    }

    private String leaseKey() {
        return "erp:assistant:wecom:leader:" + properties.getAiBot().getBotId();
    }

    private static String reqId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String frame(String cmd, String requestId, Map<String, Object> body) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("cmd", cmd);
        frame.put("headers", Collections.singletonMap("req_id", requestId));
        if (body != null) frame.put("body", body);
        return JsonUtils.toJsonString(frame);
    }

    static long reconnectDelaySeconds(int attempt) {
        return Math.min(30L, 1L << Math.min(5, Math.max(0, attempt - 1)));
    }

    private static Duration duration(Duration value, Duration fallback) { return value == null ? fallback : value; }

    private static class PendingFrame {
        final String payload;
        final CompletableFuture<JsonNode> future;
        PendingFrame(String payload, CompletableFuture<JsonNode> future) { this.payload = payload; this.future = future; }
    }

    private class Listener extends WebSocketListener {
        @Override public void onOpen(WebSocket socket, Response response) {
            if (socket != webSocket || !leader) { socket.close(1000, "not leader"); return; }
            sendAuth(socket);
        }
        @Override public void onMessage(WebSocket socket, String text) { if (socket == webSocket) handleFrame(text); }
        @Override public void onMessage(WebSocket socket, ByteString bytes) { if (socket == webSocket) handleFrame(bytes.utf8()); }
        @Override public void onClosed(WebSocket socket, int code, String reason) { disconnected(socket); }
        @Override public void onFailure(WebSocket socket, Throwable throwable, Response response) {
            log.warn("企业微信机器人长连接异常：{}", throwable.getMessage());
            disconnected(socket);
        }
        private void disconnected(WebSocket socket) {
            synchronized (AssistantWeComWebSocketClient.this) {
                if (socket != webSocket) return;
                webSocket = null;
                authenticated = false;
                clearPending(new IllegalStateException("企业微信连接已断开"));
            }
            scheduleReconnect(false);
        }
    }
}
