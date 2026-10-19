package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CLOUD_PRINT_SUBMIT_FAILED;

@Component
public class ErpCloudPrintPreviewCacheService {

    public static final Duration PREVIEW_TTL = Duration.ofMinutes(15);
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    private static final String PREVIEW_KEY_PREFIX = "erp:cloud-print:preview:";
    private static final String RESULT_KEY_PREFIX = "erp:cloud-print:preview-result:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void put(PreviewRecord record) {
        stringRedisTemplate.opsForValue().set(PREVIEW_KEY_PREFIX + record.getPreviewToken(),
                JsonUtils.toJsonString(record), PREVIEW_TTL);
    }

    public PreviewRecord getBound(String previewToken) {
        String json = stringRedisTemplate.opsForValue().get(PREVIEW_KEY_PREFIX + previewToken);
        PreviewRecord record = json == null ? null : JsonUtils.parseObject(json, PreviewRecord.class);
        if (record == null || !Objects.equals(record.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(record.getUserId(), getLoginUserId())) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "云打印预览已过期或无权访问，请重新生成预览");
        }
        return record;
    }

    public void delete(String previewToken) {
        stringRedisTemplate.delete(PREVIEW_KEY_PREFIX + previewToken);
    }

    public ConfirmResult getBoundResult(String previewToken) {
        String json = stringRedisTemplate.opsForValue().get(RESULT_KEY_PREFIX + previewToken);
        ConfirmResult result = json == null ? null : JsonUtils.parseObject(json, ConfirmResult.class);
        if (result == null) {
            return null;
        }
        if (!Objects.equals(result.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(result.getUserId(), getLoginUserId())) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "无权访问该云打印确认结果");
        }
        return result;
    }

    public void putResult(String previewToken, List<Long> taskIds) {
        ConfirmResult result = new ConfirmResult()
                .setTenantId(TenantContextHolder.getRequiredTenantId())
                .setUserId(getLoginUserId())
                .setTaskIds(taskIds);
        stringRedisTemplate.opsForValue().set(RESULT_KEY_PREFIX + previewToken,
                JsonUtils.toJsonString(result), RESULT_TTL);
    }

    @Data
    @Accessors(chain = true)
    public static class PreviewRecord {
        private String previewToken;
        private Long tenantId;
        private Long userId;
        private Long saleOutId;
        private Long templateId;
        private Integer requestedCopies;
        private String itemOrderField;
        private String itemOrderDirection;
        private LocalDateTime expiresAt;
        private String inputSha256;
        private List<PreviewItem> items;
    }

    @Data
    @Accessors(chain = true)
    public static class PreviewItem {
        private String previewItemId;
        private Long warehouseId;
        private String warehouseName;
        private Long deviceId;
        private String printerName;
        private Integer copies;
        private String fileName;
        private String pdfBase64;
        private String pdfSha256;
    }

    @Data
    @Accessors(chain = true)
    public static class ConfirmResult {
        private Long tenantId;
        private Long userId;
        private List<Long> taskIds;
    }
}
