package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpAsyncExportTaskSupportTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAsyncExportTaskSupport support;

    @Mock
    private ErpImportExportRecordMapper recordMapper;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void findOwnedRecord_acceptsOnlyCurrentTenantOperatorAndModule() {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO record = exportRecord(88L, "erp_product");
        when(recordMapper.selectByIdAndTenant(100L, 10L)).thenReturn(record);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertSame(record, support.findOwnedRecord(100L, "erp_product"));
            assertNull(support.findOwnedRecord(100L, "erp_stock"));
        }
    }

    @Test
    void findOwnedRecord_rejectsAnotherOperator() {
        TenantContextHolder.setTenantId(10L);
        when(recordMapper.selectByIdAndTenant(100L, 10L))
                .thenReturn(exportRecord(99L, "erp_product"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertNull(support.findOwnedRecord(100L, "erp_product"));
        }
    }

    private ErpImportExportRecordDO exportRecord(Long operatorId, String moduleKey) {
        return ErpImportExportRecordDO.builder()
                .id(100L)
                .tenantId(10L)
                .operatorId(operatorId)
                .operationType(ErpImportExportOperationTypeEnum.EXPORT.getType())
                .moduleKey(moduleKey)
                .build();
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser(Long userId) {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
        return security;
    }

}
