package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpWarehouseImportTaskStageEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpAsyncImportTaskSupport;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpWarehouseImportTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpWarehouseImportTaskServiceImpl service;

    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private ErpImportExportRecordMapper recordMapper;
    @Spy
    private ErpAsyncImportTaskSupport taskSupport = new ErpAsyncImportTaskSupport();
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
    void startTask_reusesFreshTaskForSameOperator() throws Exception {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO active = processingRecord();
        when(redissonClient.getLock("erp:warehouse-import-task:10")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doReturn(active).when(taskSupport).findTenantActiveRecord("erp_warehouse");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpWarehouseImportTaskStartRespVO result = service.startTask(validFile());

            assertEquals(active.getId(), result.getTaskId());
            assertTrue(result.getReused());
            verify(importExportRecordService, never()).createRecord(any());
            verify(taskExecutor, never()).execute(any(Runnable.class));
            verify(lock).unlock();
        }
    }

    @Test
    void startTask_rejectsOtherOperatorWithoutLeakingTaskId() {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO active = processingRecord();
        active.setOperatorId(99L);
        active.setOperatorName("其他操作人");
        when(redissonClient.getLock("erp:warehouse-import-task:10")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doReturn(active).when(taskSupport).findTenantActiveRecord("erp_warehouse");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> service.startTask(validFile()));

            assertTrue(ex.getMessage().contains("其他操作人"));
            verify(importExportRecordService, never()).createRecord(any());
            verify(taskExecutor, never()).execute(any(Runnable.class));
        }
    }

    @Test
    void getTask_returnsProgressOnlyForOwnedTask() {
        ErpImportExportRecordDO record = processingRecord();
        record.setTotalCount(100);
        record.setProcessedCount(50);
        record.setSuccessCount(45);
        record.setFailureCount(5);
        record.setCreateCount(20);
        record.setUpdateCount(25);
        doReturn(record).when(taskSupport).findOwnedRecord(100L, "erp_warehouse");

        ErpWarehouseImportTaskRespVO result = service.getTask(100L);

        assertEquals(50, result.getProgressPercent());
        assertEquals(45, result.getSuccessCount());
        assertEquals(5, result.getFailureCount());
        assertEquals(20, result.getCreateCount());
        assertEquals(25, result.getUpdateCount());
        verify(importExportRecordService, never()).getFailureDetailList(record.getId());
    }

    @Test
    void getTask_rejectsTaskOutsideCurrentOwnerOrModule() {
        doReturn(null).when(taskSupport).findOwnedRecord(100L, "erp_warehouse");

        assertThrows(ServiceException.class, () -> service.getTask(100L));
    }

    private MockMultipartFile validFile() {
        return new MockMultipartFile("file", "仓库.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
    }

    private ErpImportExportRecordDO processingRecord() {
        return ErpImportExportRecordDO.builder()
                .id(100L)
                .tenantId(10L)
                .operatorId(88L)
                .operatorName("测试用户")
                .operationType(ErpImportExportOperationTypeEnum.IMPORT.getType())
                .moduleKey("erp_warehouse")
                .status(ErpImportExportRecordStatusEnum.PROCESSING.getStatus())
                .stage(ErpWarehouseImportTaskStageEnum.IMPORTING.getStage())
                .fileName("仓库.xlsx")
                .startTime(LocalDateTime.now().minusSeconds(10))
                .heartbeatTime(LocalDateTime.now())
                .totalCount(0)
                .processedCount(0)
                .successCount(0)
                .failureCount(0)
                .createCount(0)
                .updateCount(0)
                .build();
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser(Long userId) {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
        return security;
    }

}
