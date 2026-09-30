package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintDeviceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpCloudPrintCallbackProcessorTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCloudPrintCallbackProcessor processor;

    @Mock
    private ErpCloudPrintTaskMapper taskMapper;
    @Mock
    private ErpCloudPrintDeviceMapper deviceMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpPrintRecordMapper printRecordMapper;
    @Mock
    private ErpCloudPrintQueueService queueService;

    private ErpCloudPrintTaskDO task;
    private ErpCloudPrintDeviceDO device;

    @BeforeEach
    void setUp() {
        task = new ErpCloudPrintTaskDO().setId(101L).setReqid("REQ-101").setDeviceId(9L)
                .setBizType(ErpCloudPrintConstants.BIZ_TYPE_SALE_OUT).setBizId(88L)
                .setBizNo("XSCK20260929000001").setStatus(ErpCloudPrintConstants.STATUS_SUBMITTED);
        task.setTenantId(1L);
        device = new ErpCloudPrintDeviceDO().setId(9L).setDevid("SW1");
        device.setTenantId(1L);
        lenient().when(taskMapper.selectListByReqid("REQ-101")).thenReturn(Collections.singletonList(task));
        lenient().when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(device));
        lenient().when(taskMapper.selectById(101L)).thenReturn(task);
    }

    @Test
    void duplicateSuccess_shouldRecordPrintOnlyOnceAndKeepFirstTerminalResult() {
        when(taskMapper.updatePrintCallbackIfProcessable(anyLong(), eq(ErpCloudPrintConstants.STATUS_SUCCESS),
                eq(0), any(), any())).thenReturn(1, 0);
        ErpCloudPrintCallbackRequest request = printRequest(0, "成功");

        processor.process(request, LocalDateTime.now());
        processor.process(request, LocalDateTime.now());

        verify(saleOutMapper, times(1)).incrementPrintCount(88L);
        verify(printRecordMapper, times(1)).insert(any(ErpPrintRecordDO.class));
        verify(queueService, times(1)).clearPauseIfTask(9L, 101L);
        verify(queueService, times(1)).dispatchDeviceAsync(9L);
        verify(taskMapper, never()).updateById(any(ErpCloudPrintTaskDO.class));
    }

    @Test
    void lateSuccessForTimeoutTask_shouldRecordAndReleaseOnlyItsPause() {
        task.setStatus(ErpCloudPrintConstants.STATUS_TIMEOUT);
        when(taskMapper.updatePrintCallbackIfProcessable(anyLong(), eq(ErpCloudPrintConstants.STATUS_SUCCESS),
                eq(0), any(), any())).thenReturn(1);

        processor.process(printRequest(0, "成功"), LocalDateTime.now().minusMinutes(11));

        verify(saleOutMapper).incrementPrintCount(88L);
        verify(printRecordMapper).insert(any(ErpPrintRecordDO.class));
        verify(queueService).clearPauseIfTask(9L, 101L);
        verify(queueService).dispatchDeviceAsync(9L);
        verify(queueService, never()).resumeQueue(anyLong(), anyString());
    }

    @Test
    void unknownNonZeroCode_shouldFailAndPauseDevice() {
        when(taskMapper.updatePrintCallbackIfProcessable(anyLong(), eq(ErpCloudPrintConstants.STATUS_FAILED),
                eq(999), anyString(), any())).thenReturn(1);

        ErpCloudPrintCallbackProcessResult result = processor.process(printRequest(999, null), LocalDateTime.now());

        assertEquals(ErpCloudPrintCallbackProcessResult.Type.SUCCESS, result.getType());
        verify(queueService).pauseDevice(9L, 101L, "打印失败，回调码：999");
        verify(saleOutMapper, never()).incrementPrintCount(anyLong());
    }

    @ParameterizedTest
    @CsvSource({
            "201, 无任务 ID",
            "202, 没有内容 type",
            "203, 无效内容类型 type",
            "204, 无任务内容",
            "205, 无效 MSG",
            "206, 打印内容下载失败",
            "207, 打印任务超时",
            "210, 打印内容数据格式错误"
    })
    void knownPrintFailureCode_shouldUseDocumentedMessage(int code, String expectedMessage) {
        when(taskMapper.updatePrintCallbackIfProcessable(anyLong(), eq(ErpCloudPrintConstants.STATUS_FAILED),
                eq(code), eq(expectedMessage), any())).thenReturn(1);

        processor.process(printRequest(code, null), LocalDateTime.now());

        verify(queueService).pauseDevice(9L, 101L, expectedMessage);
    }

    @Test
    void taskNotFound_shouldRetryForTenMinutesThenIgnore() {
        when(taskMapper.selectListByReqid("REQ-101")).thenReturn(Collections.emptyList());

        assertEquals(ErpCloudPrintCallbackProcessResult.Type.RETRY,
                processor.process(printRequest(0, "成功"), LocalDateTime.now()).getType());
        assertEquals(ErpCloudPrintCallbackProcessResult.Type.IGNORED,
                processor.process(printRequest(0, "成功"), LocalDateTime.now().minusMinutes(11)).getType());
    }

    @Test
    void crossTenantAmbiguity_shouldNotUpdateAnyTask() {
        ErpCloudPrintTaskDO task2 = new ErpCloudPrintTaskDO().setId(102L).setReqid("REQ-101").setDeviceId(10L);
        task2.setTenantId(2L);
        ErpCloudPrintDeviceDO device2 = new ErpCloudPrintDeviceDO().setId(10L).setDevid("SW1");
        device2.setTenantId(2L);
        when(taskMapper.selectListByReqid("REQ-101")).thenReturn(Arrays.asList(task, task2));
        when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(device, device2));

        ErpCloudPrintCallbackProcessResult result = processor.process(printRequest(0, "成功"), LocalDateTime.now());

        assertEquals(ErpCloudPrintCallbackProcessResult.Type.IGNORED, result.getType());
        verify(taskMapper, never()).updatePrintCallbackIfProcessable(anyLong(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void devidMismatch_shouldRetryWithoutUpdatingTask() {
        device.setDevid("OTHER-DEVICE");

        ErpCloudPrintCallbackProcessResult result = processor.process(
                printRequest(0, "成功"), LocalDateTime.now());

        assertEquals(ErpCloudPrintCallbackProcessResult.Type.RETRY, result.getType());
        verify(taskMapper, never()).updatePrintCallbackIfProcessable(anyLong(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void deviceNormal_shouldUpdateButNotResumeQueue() {
        when(deviceMapper.selectListByDevid("SW1")).thenReturn(Collections.singletonList(device));

        processor.process(deviceRequest(0, "正常"), LocalDateTime.now());

        verify(deviceMapper).updateById(any(ErpCloudPrintDeviceDO.class));
        verify(queueService, never()).pauseDevice(anyLong(), any(), anyString());
        verify(queueService, never()).resumeQueue(anyLong(), anyString());
    }

    @Test
    void knownPaperLowStatus_shouldPauseQueueWithDocumentedMessage() {
        when(deviceMapper.selectListByDevid("SW1")).thenReturn(Collections.singletonList(device));

        processor.process(deviceRequest(104, null), LocalDateTime.now());

        verify(queueService).pauseDevice(9L, null, "纸将尽");
    }

    @Test
    void knownDeviceFailure_shouldPreferDocumentedMessageOverVendorMessage() {
        when(deviceMapper.selectListByDevid("SW1")).thenReturn(Collections.singletonList(device));

        processor.process(deviceRequest(101, "online"), LocalDateTime.now());

        ArgumentCaptor<ErpCloudPrintDeviceDO> captor = ArgumentCaptor.forClass(ErpCloudPrintDeviceDO.class);
        verify(deviceMapper).updateById(captor.capture());
        assertEquals(101, captor.getValue().getLastStatusCode());
        assertEquals("缺纸", captor.getValue().getLastStatusMessage());
        verify(queueService).pauseDevice(9L, null, "缺纸");
    }

    @Test
    void unknownDeviceFailure_shouldKeepVendorMessage() {
        when(deviceMapper.selectListByDevid("SW1")).thenReturn(Collections.singletonList(device));

        processor.process(deviceRequest(999, "厂商自定义故障"), LocalDateTime.now());

        verify(queueService).pauseDevice(9L, null, "厂商自定义故障");
    }

    @ParameterizedTest
    @CsvSource({
            "100, 其他错误",
            "101, 缺纸",
            "102, 开盖",
            "103, 故障",
            "104, 纸将尽",
            "109, 打印机未接入",
            "300, 固件升级中",
            "301, 固件升级中"
    })
    void knownDeviceFailureCode_shouldUseDocumentedMessage(int code, String expectedMessage) {
        when(deviceMapper.selectListByDevid("SW1")).thenReturn(Collections.singletonList(device));

        processor.process(deviceRequest(code, null), LocalDateTime.now());

        verify(queueService).pauseDevice(9L, null, expectedMessage);
    }

    private static ErpCloudPrintCallbackRequest printRequest(int code, String message) {
        ErpCloudPrintCallbackRequest request = new ErpCloudPrintCallbackRequest();
        request.setMethod("printRlt");
        request.setDevid("SW1");
        request.setReqid("REQ-101");
        request.setCode(code);
        request.setMessage(message);
        return request;
    }

    private static ErpCloudPrintCallbackRequest deviceRequest(int code, String message) {
        ErpCloudPrintCallbackRequest request = new ErpCloudPrintCallbackRequest();
        request.setMethod("devStatus");
        request.setDevid("SW1");
        request.setCode(code);
        request.setMessage(message);
        return request;
    }

}
