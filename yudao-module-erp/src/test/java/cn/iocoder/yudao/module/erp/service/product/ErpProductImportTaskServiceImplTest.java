package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.product.ErpProductImportTaskStageEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpProductImportTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpProductImportTaskServiceImpl service;

    @Mock
    private ErpProductService productService;
    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;

    @AfterEach
    void tearDownTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void startTask_reusesFreshTaskForSameTenant() throws Exception {
        TenantContextHolder.setTenantId(10L);
        ErpImportExportRecordRespVO active = processingRecord();
        when(redissonClient.getLock("erp:product-import-task:10")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(importExportRecordService.getLatestProcessingRecord("IMPORT", "erp_product"))
                .thenReturn(active);
        MockMultipartFile file = new MockMultipartFile("file", "配件.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});

        ErpProductImportTaskStartRespVO result = service.startTask(file);

        assertEquals(active.getId(), result.getTaskId());
        assertTrue(result.getReused());
        verify(taskExecutor, never()).execute(any(Runnable.class));
        verify(lock).unlock();
    }

    @Test
    void getTask_calculatesImportingProgress() {
        ErpImportExportRecordRespVO record = processingRecord();
        record.setTotalCount(100);
        record.setProcessedCount(50);
        record.setSuccessCount(48);
        record.setFailureCount(2);
        when(importExportRecordService.getRecordForCurrentTenant(record.getId())).thenReturn(record);

        ErpProductImportTaskRespVO result = service.getTask(record.getId());

        assertEquals(50, result.getProgressPercent());
        assertEquals(50, result.getProcessedCount());
        verify(importExportRecordService, never()).getFailureDetailList(record.getId());
    }

    @Test
    void getTask_returnsFailuresAfterTaskFinishes() {
        ErpImportExportRecordRespVO record = processingRecord();
        record.setTotalCount(100);
        record.setProcessedCount(100);
        record.setSuccessCount(98);
        record.setFailureCount(2);
        ErpImportExportRecordFailureDetailExportRespVO failure = new ErpImportExportRecordFailureDetailExportRespVO();
        failure.setRowNo(12);
        failure.setBizKey("P-001");
        failure.setFailureReason("分类不存在");
        record.setStatus(ErpImportExportRecordStatusEnum.PARTIAL_SUCCESS.getStatus());
        when(importExportRecordService.getRecordForCurrentTenant(record.getId())).thenReturn(record);
        when(importExportRecordService.getFailureDetailList(record.getId()))
                .thenReturn(Collections.singletonList(failure));

        ErpProductImportTaskRespVO result = service.getTask(record.getId());

        assertEquals(100, result.getProgressPercent());
        assertEquals(100, result.getProcessedCount());
        assertEquals("P-001", result.getFailureDetails().get(0).getCode());
    }

    @Test
    void getActiveTask_marksTaskFailedAfterHeartbeatTimeout() {
        ErpImportExportRecordRespVO active = processingRecord();
        active.setHeartbeatTime(LocalDateTime.now().minusMinutes(31));
        when(importExportRecordService.getLatestProcessingRecord("IMPORT", "erp_product"))
                .thenReturn(active);

        ErpProductImportTaskRespVO result = service.getActiveTask();

        assertEquals(null, result);
        verify(importExportRecordService).finishRecord(any(), any());
    }

    private ErpImportExportRecordRespVO processingRecord() {
        ErpImportExportRecordRespVO record = new ErpImportExportRecordRespVO();
        record.setId(100L);
        record.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType());
        record.setModuleKey("erp_product");
        record.setStatus(ErpImportExportRecordStatusEnum.PROCESSING.getStatus());
        record.setStage(ErpProductImportTaskStageEnum.IMPORTING.getStage());
        record.setStartTime(LocalDateTime.now().minusSeconds(10));
        record.setHeartbeatTime(LocalDateTime.now());
        record.setTotalCount(0);
        record.setProcessedCount(0);
        record.setSuccessCount(0);
        record.setFailureCount(0);
        record.setCreateCount(0);
        record.setUpdateCount(0);
        return record;
    }

}
