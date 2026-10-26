package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpAsyncExportTaskStageEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpAsyncExportTaskSupport;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpWarehouseExportTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpWarehouseExportTaskServiceImpl service;

    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpWarehouseQueryService warehouseQueryService;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private ErpImportExportRecordMapper recordMapper;
    @Spy
    private ErpAsyncExportTaskSupport taskSupport = new ErpAsyncExportTaskSupport();
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;

    @BeforeEach
    void setUpTaskSupport() {
        ReflectionTestUtils.setField(taskSupport, "recordMapper", recordMapper);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void startTask_reusesActiveTaskForSameTenantAndOperator() {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO active = processingRecord();
        when(redissonClient.getLock("erp:warehouse-export-task:10:88")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doReturn(active).when(taskSupport).findActiveRecord("erp_warehouse");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpWarehouseExportTaskStartRespVO result = service.startTask(
                    new ErpWarehousePageReqVO(), new LinkedHashSet<>());

            assertEquals(active.getId(), result.getTaskId());
            assertTrue(result.getReused());
            verify(importExportRecordService, never()).createRecord(any());
            verify(taskExecutor, never()).execute(any(Runnable.class));
            verify(lock).unlock();
        }
    }

    @Test
    void getTask_returnsProgressAndSelectedFieldsForOwnedTask() {
        ErpImportExportRecordDO record = processingRecord();
        record.setTotalCount(100);
        record.setProcessedCount(50);
        record.setSuccessCount(48);
        record.setExportFields("[\"name\",\"warehouseCode\"]");
        doReturn(record).when(taskSupport).findOwnedRecord(100L, "erp_warehouse");

        ErpWarehouseExportTaskRespVO result = service.getTask(100L);

        assertEquals(47, result.getProgressPercent());
        assertEquals(50, result.getProcessedCount());
        assertEquals(48, result.getExportedCount());
        assertEquals(2, result.getSelectedFields().size());
        assertFalse(result.getDownloadAvailable());
    }

    @Test
    void getTask_rejectsTaskOutsideCurrentOwnerOrModule() {
        doReturn(null).when(taskSupport).findOwnedRecord(100L, "erp_warehouse");

        assertThrows(ServiceException.class, () -> service.getTask(100L));
    }

    private ErpImportExportRecordDO processingRecord() {
        return ErpImportExportRecordDO.builder()
                .id(100L)
                .tenantId(10L)
                .operatorId(88L)
                .operatorName("测试用户")
                .operationType(ErpImportExportOperationTypeEnum.EXPORT.getType())
                .moduleKey("erp_warehouse")
                .status(ErpImportExportRecordStatusEnum.PROCESSING.getStatus())
                .stage(ErpAsyncExportTaskStageEnum.EXPORTING.getStage())
                .fileName("仓库信息.xlsx")
                .startTime(LocalDateTime.now().minusSeconds(10))
                .heartbeatTime(LocalDateTime.now())
                .totalCount(0)
                .processedCount(0)
                .successCount(0)
                .failureCount(0)
                .build();
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser(Long userId) {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
        return security;
    }

}
