package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpCloudPrintPreviewCacheServiceTest {

    private final ErpCloudPrintPreviewCacheService service = new ErpCloudPrintPreviewCacheService();
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getBound_shouldReturnPreviewForSameTenantAndUser() {
        stubRecord(7L, 99L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertEquals("preview-token", service.getBound("preview-token").getPreviewToken());
        }
    }

    @Test
    void getBound_shouldRejectCrossTenantOrCrossUserAccess() {
        stubRecord(8L, 99L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(RuntimeException.class, () -> service.getBound("preview-token"));
        }

        stubRecord(7L, 100L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(RuntimeException.class, () -> service.getBound("preview-token"));
        }
    }

    @Test
    void getBound_shouldTreatMissingRedisValueAsExpired() {
        when(valueOperations.get("erp:cloud-print:preview:expired-token")).thenReturn(null);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(RuntimeException.class, () -> service.getBound("expired-token"));
        }
    }

    private void stubRecord(Long tenantId, Long userId) {
        ErpCloudPrintPreviewCacheService.PreviewRecord record =
                new ErpCloudPrintPreviewCacheService.PreviewRecord()
                        .setPreviewToken("preview-token")
                        .setTenantId(tenantId)
                        .setUserId(userId);
        when(valueOperations.get("erp:cloud-print:preview:preview-token"))
                .thenReturn(JsonUtils.toJsonString(record));
    }
}
