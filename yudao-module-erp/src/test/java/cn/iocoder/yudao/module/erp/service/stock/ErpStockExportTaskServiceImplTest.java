package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockExportTaskStageEnum;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockExportTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockExportTaskServiceImpl service;

    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private ErpImportExportRecordMapper recordMapper;
    @Spy
    private ErpAsyncExportTaskSupport asyncExportTaskSupport = new ErpAsyncExportTaskSupport();
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;

    @BeforeEach
    void setUpAsyncExportTaskSupport() {
        ReflectionTestUtils.setField(asyncExportTaskSupport, "recordMapper", recordMapper);
    }

    @AfterEach
    void tearDownTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void startTask_reusesProcessingTaskForSameTenantAndOperator() {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO active = processingRecord();
        when(redissonClient.getLock("erp:stock-export-task:10:88")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recordMapper.selectLatestProcessingByOperator("EXPORT", "erp_stock", 10L, 88L))
                .thenReturn(active);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockExportTaskStartRespVO result = service.startTask(new ErpStockPageReqVO(), reqVO -> null);

            assertEquals(active.getId(), result.getTaskId());
            assertTrue(result.getReused());
            verify(taskExecutor, never()).execute(any(Runnable.class));
            verify(lock).unlock();
        }
    }

    @Test
    void getTask_calculatesMonotonicExportProgress() {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO record = processingRecord();
        record.setTotalCount(100);
        record.setProcessedCount(50);
        record.setSuccessCount(50);
        when(recordMapper.selectByIdAndTenant(100L, 10L)).thenReturn(record);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockExportTaskRespVO result = service.getTask(100L);

            assertEquals(47, result.getProgressPercent());
            assertEquals(50, result.getProcessedCount());
            assertEquals(50, result.getExportedCount());
        }
    }

    @Test
    void getActiveTask_marksStaleTaskFailed() {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO active = processingRecord();
        active.setHeartbeatTime(LocalDateTime.now().minusMinutes(31));
        when(recordMapper.selectLatestProcessingByOperator("EXPORT", "erp_stock", 10L, 88L))
                .thenReturn(active);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertEquals(null, service.getActiveTask());
            verify(importExportRecordService).finishRecord(any(), any());
        }
    }

    @Test
    void getTask_marksStaleTaskFailedForPollingClient() {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordDO active = processingRecord();
        active.setHeartbeatTime(LocalDateTime.now().minusMinutes(31));
        ErpImportExportRecordDO failed = processingRecord();
        failed.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        failed.setStage(ErpStockExportTaskStageEnum.FINISHED.getStage());
        failed.setErrorMessage("任务超过 30 分钟未更新进度，已自动结束，请重新导出");
        when(recordMapper.selectByIdAndTenant(100L, 10L)).thenReturn(active, failed);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockExportTaskRespVO result = service.getTask(100L);

            assertEquals(ErpImportExportRecordStatusEnum.FAILURE.getStatus(), result.getStatus());
            assertEquals(100, result.getProgressPercent());
            verify(importExportRecordService).finishRecord(any(), any());
        }
    }

    @Test
    void batchSort_usesExternalMergeAndKeepsGlobalOrder() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setShowBatchNo(true);
        reqVO.setOrderField("batchNo");
        reqVO.setOrderDirection("asc");
        ErpStockRespVO later = new ErpStockRespVO().setRowKey("stock:1:batch:B-9").setBatchNo("B-9");
        ErpStockRespVO earlier = new ErpStockRespVO().setRowKey("stock:2:batch:A-1").setBatchNo("A-1");

        assertTrue(service.requiresExternalBatchMerge(reqVO));
        assertTrue(service.compareBatchExportRows(later, earlier, reqVO) > 0);

        reqVO.setOrderDirection("desc");
        assertTrue(service.compareBatchExportRows(later, earlier, reqVO) < 0);
    }

    private ErpImportExportRecordDO processingRecord() {
        return ErpImportExportRecordDO.builder()
                .id(100L)
                .tenantId(10L)
                .operatorId(88L)
                .operatorName("测试用户")
                .operationType(ErpImportExportOperationTypeEnum.EXPORT.getType())
                .moduleKey("erp_stock")
                .status(ErpImportExportRecordStatusEnum.PROCESSING.getStatus())
                .stage(ErpStockExportTaskStageEnum.EXPORTING.getStage())
                .fileName("产品库存.xlsx")
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
