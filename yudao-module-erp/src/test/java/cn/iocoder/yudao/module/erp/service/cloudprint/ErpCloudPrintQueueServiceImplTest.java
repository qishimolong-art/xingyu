package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintDeviceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.SwPrintClient;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.DeviceInfo;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.PtFileData;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpCloudPrintQueueServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCloudPrintQueueServiceImpl service;

    @Mock
    private ErpCloudPrintDeviceMapper deviceMapper;
    @Mock
    private ErpCloudPrintTaskMapper taskMapper;
    @Mock
    private SwPrintClient swPrintClient;
    @Mock
    private FileApi fileApi;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    @BeforeEach
    void setUpLock() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void dispatchPendingTasks_shouldSubmitOnlyFirstTaskForSameDevice() throws Exception {
        ErpCloudPrintTaskDO first = task(11L, 1L);
        ErpCloudPrintTaskDO second = task(12L, 1L);
        when(taskMapper.selectPendingTasks()).thenReturn(Arrays.asList(first, second));
        prepareDeviceDispatch(1L, first, accepted("0"));

        try (MockedStatic<TenantContextHolder> tenant = mockStatic(TenantContextHolder.class)) {
            tenant.when(TenantContextHolder::getRequiredTenantId).thenReturn(100L);
            assertEquals(1, service.dispatchPendingTasks());
        }

        verify(fileApi).getFileContent("/file/11.pdf");
        verify(fileApi, never()).getFileContent("/file/12.pdf");
        verify(swPrintClient).ptFile(any(), any(), eq("11.pdf"));
    }

    @Test
    void dispatchPendingTasks_shouldPauseQueueWhenVendorReturns012() throws Exception {
        ErpCloudPrintTaskDO task = task(21L, 2L);
        when(taskMapper.selectPendingTasks()).thenReturn(Collections.singletonList(task));
        prepareDeviceDispatch(2L, task, accepted("012"));

        try (MockedStatic<TenantContextHolder> tenant = mockStatic(TenantContextHolder.class)) {
            tenant.when(TenantContextHolder::getRequiredTenantId).thenReturn(100L);
            assertEquals(1, service.dispatchPendingTasks());
        }

        ArgumentCaptor<ErpCloudPrintTaskDO> updates = ArgumentCaptor.forClass(ErpCloudPrintTaskDO.class);
        verify(taskMapper, atLeastOnce()).updateById(updates.capture());
        assertTrue(updates.getAllValues().stream().anyMatch(update ->
                Integer.valueOf(ErpCloudPrintConstants.STATUS_UNKNOWN).equals(update.getStatus())));
        verify(deviceMapper).pauseQueue(eq(2L), anyString(), eq(21L));
    }

    @Test
    void resumeQueue_shouldCreatePriorityRetryAndCancelOriginal() throws Exception {
        ErpCloudPrintDeviceDO device = device(3L).setQueuePaused(true).setQueuePauseTaskId(31L);
        ErpCloudPrintTaskDO failed = task(31L, 3L)
                .setStatus(ErpCloudPrintConstants.STATUS_TIMEOUT)
                .setBizNo("XSCK20260929000001");
        when(deviceMapper.selectById(3L)).thenReturn(device);
        when(taskMapper.selectById(31L)).thenReturn(failed);

        ErpCloudPrintTaskDO retry;
        try (MockedStatic<TenantContextHolder> tenant = mockStatic(TenantContextHolder.class)) {
            tenant.when(TenantContextHolder::getRequiredTenantId).thenReturn(100L);
            retry = service.resumeQueue(3L, "RETRY");
        }

        assertNotNull(retry);
        assertEquals(31L, retry.getRetryOf());
        assertEquals(ErpCloudPrintConstants.STATUS_PENDING, retry.getStatus());
        verify(taskMapper).insert(retry);
        verify(deviceMapper).resumeQueue(3L);
        verify(taskMapper).updateById(any(ErpCloudPrintTaskDO.class));
    }

    private void prepareDeviceDispatch(Long deviceId, ErpCloudPrintTaskDO task, PtFileData result) throws Exception {
        ErpCloudPrintDeviceDO device = device(deviceId);
        when(deviceMapper.selectById(deviceId)).thenReturn(device);
        when(taskMapper.selectInFlightByDevice(deviceId)).thenReturn(null);
        when(taskMapper.selectFirstPendingByDevice(deviceId)).thenReturn(task);
        DeviceInfo info = new DeviceInfo();
        info.setState("online");
        info.setStatus(0);
        info.setCode(0);
        when(swPrintClient.getDevice(device.getDevid())).thenReturn(info);
        when(fileApi.getFileContent(task.getFileUrl())).thenReturn(new byte[]{1, 2, 3});
        when(swPrintClient.ptFile(any(), any(), eq(task.getFileName()))).thenReturn(result);
    }

    private ErpCloudPrintDeviceDO device(Long id) {
        return new ErpCloudPrintDeviceDO()
                .setId(id)
                .setDevid("DEV-" + id)
                .setNickname("设备" + id)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setQueuePaused(false)
                .setPrintWidth(210)
                .setPrintHeight(140)
                .setPaperType(4)
                .setRotate(0);
    }

    private ErpCloudPrintTaskDO task(Long id, Long deviceId) {
        return new ErpCloudPrintTaskDO()
                .setId(id)
                .setDeviceId(deviceId)
                .setDevid("DEV-" + deviceId)
                .setReqid("REQ-" + id)
                .setContentType(ErpCloudPrintConstants.CONTENT_TYPE_PDF)
                .setCopies(1)
                .setFileUrl("/file/" + id + ".pdf")
                .setFileName(id + ".pdf")
                .setStatus(ErpCloudPrintConstants.STATUS_PENDING);
    }

    private PtFileData accepted(String code) {
        PtFileData result = new PtFileData();
        result.setSuccess(true);
        result.setCode(code);
        result.setMessage("result-" + code);
        return result;
    }

}
