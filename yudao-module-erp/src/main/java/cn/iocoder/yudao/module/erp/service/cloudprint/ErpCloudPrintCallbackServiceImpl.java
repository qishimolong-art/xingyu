package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintCallbackLogDO;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.config.SwPrintProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants.PROCESS_STATUS_IGNORED;
import static cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants.PROCESS_STATUS_SUCCESS;

@Slf4j
@Service
public class ErpCloudPrintCallbackServiceImpl implements ErpCloudPrintCallbackService {

    private static final int[] RETRY_MINUTES = {1, 5, 15, 30, 60};

    @Resource
    private SwPrintProperties properties;
    @Resource
    private ErpCloudPrintCallbackStorageService storageService;
    @Resource
    private ErpCloudPrintCallbackProcessor callbackProcessor;

    @Override
    @TenantIgnore
    public void receiveCallback(String pathToken, String rawBody) {
        if (!validPathToken(pathToken)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String callbackBody = rawBody == null ? "" : rawBody;
        if (callbackBody.getBytes(StandardCharsets.UTF_8).length
                > ErpCloudPrintCallbackConstants.MAX_BODY_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        Long callbackId = storageService.saveIncoming(callbackBody);
        tryProcess(callbackId);
    }

    @Override
    @TenantIgnore
    public int retryPendingCallbacks() {
        LocalDateTime now = LocalDateTime.now();
        int recovered = storageService.recoverStuckProcessing(
                now.minusMinutes(ErpCloudPrintCallbackConstants.PROCESS_LEASE_MINUTES), now);
        if (recovered > 0) {
            log.warn("[retryPendingCallbacks][恢复超时的云打印回调数量({})]", recovered);
        }
        List<ErpCloudPrintCallbackLogDO> callbacks = storageService.getDueCallbacks(
                now, ErpCloudPrintCallbackConstants.RETRY_BATCH_SIZE);
        int processed = 0;
        for (ErpCloudPrintCallbackLogDO callback : callbacks) {
            if (tryProcess(callback.getId())) {
                processed++;
            }
        }
        return processed;
    }

    private boolean tryProcess(Long callbackId) {
        String processToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        if (!storageService.claim(callbackId, processToken, now)) {
            return false;
        }
        ErpCloudPrintCallbackLogDO callback = storageService.get(callbackId);
        if (callback == null) {
            return true;
        }
        try {
            ParsedCallback parsed = parse(callback.getRawBody());
            if (parsed.getRequest() != null
                    && !storageService.updateParsed(callbackId, processToken, parsed.getRequest())) {
                return true;
            }
            if (parsed.getError() != null) {
                storageService.finish(callbackId, processToken, PROCESS_STATUS_IGNORED,
                        false, parsed.getError(), LocalDateTime.now());
                return true;
            }
            ErpCloudPrintCallbackProcessResult result = callbackProcessor.process(
                    parsed.getRequest(), callback.getCreateTime());
            if (result.getType() == ErpCloudPrintCallbackProcessResult.Type.RETRY) {
                scheduleRetry(callback, processToken, result.getMessage());
            } else {
                int status = result.getType() == ErpCloudPrintCallbackProcessResult.Type.SUCCESS
                        ? PROCESS_STATUS_SUCCESS : PROCESS_STATUS_IGNORED;
                storageService.finish(callbackId, processToken, status, result.isMatched(),
                        limit(result.getMessage(), 1000), LocalDateTime.now());
            }
        } catch (Exception ex) {
            scheduleRetry(callback, processToken, exceptionSummary(ex));
        }
        return true;
    }

    private void scheduleRetry(ErpCloudPrintCallbackLogDO callback, String processToken, String reason) {
        int retryCount = (callback.getRetryCount() == null ? 0 : callback.getRetryCount()) + 1;
        int retryMinutes = retryCount <= RETRY_MINUTES.length ? RETRY_MINUTES[retryCount - 1] : 60;
        try {
            storageService.markRetry(callback.getId(), processToken, retryCount,
                    LocalDateTime.now().plusMinutes(retryMinutes), limit(reason, 1000));
        } catch (Exception markException) {
            log.error("[scheduleRetry][云打印回调重试状态保存失败 callbackId({}) retryCount({}) exception({})]",
                    callback.getId(), retryCount, markException.getClass().getSimpleName());
        }
        if (retryCount > 10) {
            log.error("[scheduleRetry][云打印回调连续处理失败 callbackId({}) retryCount({})]",
                    callback.getId(), retryCount);
        } else {
            log.warn("[scheduleRetry][云打印回调等待重试 callbackId({}) retryCount({}) retryMinutes({})]",
                    callback.getId(), retryCount, retryMinutes);
        }
    }

    private boolean validPathToken(String pathToken) {
        String expected = properties.getCallbackPathToken();
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(pathToken)) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                pathToken.getBytes(StandardCharsets.UTF_8));
    }

    private ParsedCallback parse(String rawBody) {
        JsonNode root;
        try {
            root = JsonUtils.getObjectMapper().readTree(rawBody);
        } catch (Exception ex) {
            return ParsedCallback.invalid(new ErpCloudPrintCallbackRequest(), "JSON 格式不合法");
        }
        // 商为生产环境实际可能将 JSON 对象再序列化为一层字符串。
        // 仅兼容这一层，解码后仍非对象的正文继续按非法请求归档。
        if (root != null && root.isTextual()) {
            try {
                root = JsonUtils.getObjectMapper().readTree(root.textValue());
            } catch (Exception ex) {
                return ParsedCallback.invalid(new ErpCloudPrintCallbackRequest(), "请求正文必须是 JSON 对象");
            }
        }
        if (root == null || !root.isObject()) {
            return ParsedCallback.invalid(new ErpCloudPrintCallbackRequest(), "请求正文必须是 JSON 对象");
        }
        ErpCloudPrintCallbackRequest request = new ErpCloudPrintCallbackRequest();
        if (nonTextValue(root, "method")) {
            return ParsedCallback.invalid(request, "method 必须是字符串");
        }
        if (nonTextValue(root, "devid")) {
            return ParsedCallback.invalid(request, "devid 必须是字符串");
        }
        if (nonTextValue(root, "reqid")) {
            return ParsedCallback.invalid(request, "reqid 必须是字符串");
        }
        if (nonTextValue(root, "message")) {
            return ParsedCallback.invalid(request, "message 必须是字符串");
        }
        request.setMethod(textValue(root.get("method")));
        request.setDevid(textValue(root.get("devid")));
        request.setReqid(textValue(root.get("reqid")));
        request.setMessage(textValue(root.get("message")));

        String codeError = parseCode(root.get("code"), request);
        if (codeError != null) {
            return ParsedCallback.invalid(request, codeError);
        }
        if (!StringUtils.hasText(request.getMethod())) {
            return ParsedCallback.invalid(request, "method 不能为空");
        }
        if (!"printRlt".equals(request.getMethod()) && !"devStatus".equals(request.getMethod())) {
            return ParsedCallback.invalid(request, "未知 method：" + limit(request.getMethod(), 64));
        }
        if (!StringUtils.hasText(request.getDevid())) {
            return ParsedCallback.invalid(request, "devid 不能为空");
        }
        if (request.getDevid().length() > 64) {
            return ParsedCallback.invalid(request, "devid 长度不能超过 64");
        }
        if ("printRlt".equals(request.getMethod()) && !StringUtils.hasText(request.getReqid())) {
            return ParsedCallback.invalid(request, "printRlt 的 reqid 不能为空");
        }
        if (request.getReqid() != null && request.getReqid().length() > 64) {
            return ParsedCallback.invalid(request, "reqid 长度不能超过 64");
        }
        return ParsedCallback.valid(request);
    }

    private static String parseCode(JsonNode codeNode, ErpCloudPrintCallbackRequest request) {
        if (codeNode == null || codeNode.isNull()) {
            return "code 不能为空";
        }
        String codeText;
        if (codeNode.isIntegralNumber()) {
            codeText = codeNode.asText();
        } else if (codeNode.isTextual()) {
            codeText = codeNode.textValue() == null ? null : codeNode.textValue().trim();
        } else {
            return "code 必须是整数或整数字符串";
        }
        try {
            int code = Integer.parseInt(codeText);
            if (code < 0) {
                return "code 不能小于 0";
            }
            request.setCode(code);
            return null;
        } catch (Exception ex) {
            return "code 必须是有效整数";
        }
    }

    private static String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isTextual() ? node.textValue() : null;
    }

    private static boolean nonTextValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node != null && !node.isNull() && !node.isTextual();
    }

    private static String exceptionSummary(Exception ex) {
        String message = ex.getMessage();
        String summary = "处理异常：" + ex.getClass().getSimpleName();
        if (StringUtils.hasText(message)) {
            summary += " - " + message.replace('\r', ' ').replace('\n', ' ');
        }
        return limit(summary, 1000);
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Getter
    @AllArgsConstructor
    private static class ParsedCallback {
        private final ErpCloudPrintCallbackRequest request;
        private final String error;

        static ParsedCallback valid(ErpCloudPrintCallbackRequest request) {
            return new ParsedCallback(request, null);
        }

        static ParsedCallback invalid(ErpCloudPrintCallbackRequest request, String error) {
            return new ParsedCallback(request, error);
        }
    }

}
