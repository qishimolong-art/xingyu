package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.assistant.ErpAssistantWeComBindingDO;
import cn.iocoder.yudao.module.erp.service.assistant.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 企业微信消息到智能问数执行服务的适配层。 */
@Service
@Slf4j
public class AssistantWeComMessageService {

    private static final String DEDUP_KEY = "erp:assistant:wecom:msg:%s";
    private static final String CHOICE_KEY = "erp:assistant:wecom:choice:%s:%s";

    @Resource private WeComProperties properties;
    @Resource private AssistantWeComWebSocketClient webSocketClient;
    @Resource private AssistantWeComBindingService bindingService;
    @Resource private AssistantExecutionService executionService;
    @Resource private AssistantStore store;
    @Resource private AssistantWeComResultFormatter formatter;
    @Resource private AdminUserApi adminUserApi;
    @Resource private PermissionApi permissionApi;
    @Resource(name = "stringRedisTemplate") private StringRedisTemplate redisTemplate;

    @EventListener
    public void onInbound(AssistantWeComInboundEvent event) {
        try {
            handleInbound(event.getFrame());
        } catch (Exception exception) {
            log.error("企业微信机器人消息处理失败：{}", exception.getMessage(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBound(AssistantWeComBoundEvent event) {
        if (!webSocketClient.isAvailable()) return;
        try {
            processQuestion(event.getTenantId(), event.getWecomUserId(), event.getQuestion(), null, null, true);
        } catch (Exception exception) {
            log.error("企业微信绑定后自动续问失败，用户需重新提问：{}", exception.getMessage());
        }
    }

    private void handleInbound(JsonNode frame) {
        JsonNode body = frame.path("body");
        String requestId = frame.path("headers").path("req_id").asText("");
        String msgId = body.path("msgid").asText("");
        String botId = body.path("aibotid").asText("");
        String wecomUserId = body.path("from").path("userid").asText("");
        if (StrUtil.hasBlank(requestId, msgId, botId, wecomUserId)
                || !botId.equals(properties.getAiBot().getBotId())) return;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(String.format(DEDUP_KEY, msgId), "1", 24, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(first)) return;
        if ("group".equals(body.path("chattype").asText())) {
            replyOnce(requestId, "智能问数暂仅支持与机器人单聊使用");
            return;
        }
        if (!"text".equals(body.path("msgtype").asText())) {
            replyOnce(requestId, "智能问数首期仅支持单聊文本消息");
            return;
        }
        String question = StrUtil.trim(body.path("text").path("content").asText(""));
        if (StrUtil.isBlank(question)) {
            replyOnce(requestId, "请输入需要查询的问题");
            return;
        }
        processQuestion(properties.getAiBot().getTenantId(), wecomUserId, question, requestId,
                "stream_" + UUID.randomUUID().toString().replace("-", ""), false);
    }

    private void processQuestion(Long tenantId, String wecomUserId, String question,
                                 String requestId, String streamId, boolean proactive) {
        AtomicReference<ErpAssistantWeComBindingDO> bindingRef = new AtomicReference<>();
        TenantUtils.execute(tenantId, () -> bindingRef.set(bindingService.getBinding(wecomUserId)));
        ErpAssistantWeComBindingDO binding = bindingRef.get();
        if (binding == null) {
            if (proactive) return;
            String ticket = bindingService.createTicket(wecomUserId, question);
            String link = bindingService.getBindEntryUrl(ticket, tenantId);
            replyOnce(requestId, "首次使用需要绑定 ERP 身份（5 分钟内有效）：[点击完成免登绑定](" + link + ")");
            return;
        }
        AtomicReference<AdminUserRespDTO> userRef = new AtomicReference<>();
        AtomicBoolean permitted = new AtomicBoolean();
        TenantUtils.execute(tenantId, () -> {
            userRef.set(adminUserApi.getUser(binding.getUserId()));
            permitted.set(permissionApi.hasAnyPermissions(binding.getUserId(), "erp:assistant:query"));
        });
        AdminUserRespDTO user = userRef.get();
        if (user == null || !CommonStatusEnum.isEnable(user.getStatus())) {
            send(proactive, requestId, streamId, wecomUserId, "ERP 用户不存在或已停用", true);
            return;
        }
        if (!permitted.get()) {
            send(proactive, requestId, streamId, wecomUserId, "没有智能问数权限，请联系管理员授权后重试", true);
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        LoginUser loginUser = loginUser(tenantId, user);
        context.setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, "", Collections.emptyList()));
        SecurityContextHolder.setContext(context);
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        try {
            String conversationId = binding.getConversationId();
            if ("新会话".equals(question) || "清空上下文".equals(question)) {
                if (conversationId != null && executionService.isConversationBusy(conversationId)) {
                    send(proactive, requestId, streamId, wecomUserId, "上一条问题正在处理中", true);
                    return;
                }
                conversationId = store.create();
                bindingService.updateConversation(binding.getId(), conversationId);
                clearChoice(wecomUserId);
                send(proactive, requestId, streamId, wecomUserId, "已创建新会话，旧会话仍可在 ERP 问数历史中查看", true);
                return;
            }
            if (StrUtil.isBlank(conversationId)) {
                conversationId = store.create();
            }
            // 每次有效提问都刷新会话与最后活跃时间，便于审计和后续主动续问。
            bindingService.updateConversation(binding.getId(), conversationId);
            if (executionService.isConversationBusy(conversationId)) {
                send(proactive, requestId, streamId, wecomUserId, "上一条问题正在处理中", true);
                return;
            }
            PendingChoice choice = readChoice(wecomUserId, question);
            AssistantExecutionService.Request execute = new AssistantExecutionService.Request();
            execute.setConversationId(conversationId);
            execute.setQuestion(question);
            if (choice != null) {
                execute.setChoiceField(choice.field);
                execute.setChoiceId(choice.id);
                clearChoice(wecomUserId);
            }
            if (proactive) webSocketClient.sendMarkdown(wecomUserId, "身份绑定成功，正在继续查询首次问题……");
            else webSocketClient.replyStream(requestId, streamId, "正在查询……", false);
            executionService.execute(execute, resultSink(proactive, requestId, streamId, wecomUserId));
        } catch (AssistantFailure failure) {
            send(proactive, requestId, streamId, wecomUserId, failure.getMessage(), true);
        } finally {
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private AssistantExecutionService.EventSink resultSink(boolean proactive, String requestId,
                                                           String streamId, String wecomUserId) {
        return new AssistantExecutionService.EventSink() {
            private final AtomicBoolean completed = new AtomicBoolean();
            private Map<String, Object> result;
            private String explanation;
            private String finalText;

            @Override
            @SuppressWarnings("unchecked")
            public void emit(String event, Object data) {
                if ("result".equals(event) && data instanceof Map) result = (Map<String, Object>) data;
                else if ("explanation".equals(event) && data instanceof Map) {
                    Object text = ((Map<?, ?>) data).get("text");
                    explanation = text == null ? null : String.valueOf(text);
                } else if ("clarification".equals(event)) {
                    AssistantWeComResultFormatter.ChoiceResult choice = formatter.clarification(data);
                    finalText = choice.getContent();
                    if (choice.hasChoices()) {
                        redisTemplate.opsForValue().set(choiceKey(wecomUserId), choice.toJson(),
                                choiceTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    }
                } else if ("error".equals(event) && data instanceof Map) {
                    Object text = ((Map<?, ?>) data).get("text");
                    finalText = formatter.error(text == null ? null : String.valueOf(text));
                }
            }

            @Override
            public void complete() {
                if (!completed.compareAndSet(false, true)) return;
                String content = finalText != null ? finalText : formatter.result(result, explanation);
                send(proactive, requestId, streamId, wecomUserId, content, true);
            }
        };
    }

    private PendingChoice readChoice(String wecomUserId, String question) {
        if (!question.matches("^[0-9]{1,2}$")) return null;
        String raw = redisTemplate.opsForValue().get(choiceKey(wecomUserId));
        if (StrUtil.isBlank(raw)) return null;
        JsonNode value = JsonUtils.parseTree(raw);
        int index = Integer.parseInt(question) - 1;
        JsonNode ids = value.path("ids");
        if (index < 0 || !ids.isArray() || index >= ids.size()) return null;
        String field = value.path("field").asText("");
        return StrUtil.isBlank(field) ? null : new PendingChoice(field, ids.get(index).asLong());
    }

    private void clearChoice(String wecomUserId) { redisTemplate.delete(choiceKey(wecomUserId)); }

    private String choiceKey(String wecomUserId) {
        return String.format(CHOICE_KEY, properties.getAiBot().getBotId(), wecomUserId);
    }

    private Duration choiceTimeout() {
        Duration value = properties.getAiBot().getChoiceTimeout();
        return value == null ? Duration.ofMinutes(10) : value;
    }

    private void replyOnce(String requestId, String content) {
        webSocketClient.replyStream(requestId, "stream_" + UUID.randomUUID().toString().replace("-", ""), content, true)
                .exceptionally(error -> { log.warn("企业微信机器人回复失败：{}", error.getMessage()); return null; });
    }

    private void send(boolean proactive, String requestId, String streamId, String wecomUserId,
                      String content, boolean finish) {
        if (proactive) {
            webSocketClient.sendMarkdown(wecomUserId, content)
                    .exceptionally(error -> { log.warn("企业微信机器人主动消息发送失败：{}", error.getMessage()); return null; });
        } else {
            webSocketClient.replyStream(requestId, streamId, content, finish)
                    .exceptionally(error -> { log.warn("企业微信机器人被动回复失败：{}", error.getMessage()); return null; });
        }
    }

    private static LoginUser loginUser(Long tenantId, AdminUserRespDTO user) {
        LoginUser login = new LoginUser();
        login.setId(user.getId());
        login.setUserType(UserTypeEnum.ADMIN.getValue());
        login.setTenantId(tenantId);
        login.setVisitTenantId(tenantId);
        Map<String, String> info = new HashMap<>();
        if (user.getNickname() != null) info.put(LoginUser.INFO_KEY_NICKNAME, user.getNickname());
        if (user.getDeptId() != null) info.put(LoginUser.INFO_KEY_DEPT_ID, String.valueOf(user.getDeptId()));
        login.setInfo(info);
        return login;
    }

    private static class PendingChoice {
        final String field; final Long id;
        PendingChoice(String field, Long id) { this.field = field; this.id = id; }
    }
}
