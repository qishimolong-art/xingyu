package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintCallbackLogDO;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.config.SwPrintProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpCloudPrintCallbackServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCloudPrintCallbackServiceImpl service;

    @Mock
    private SwPrintProperties properties;
    @Mock
    private ErpCloudPrintCallbackStorageService storageService;
    @Mock
    private ErpCloudPrintCallbackProcessor callbackProcessor;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getCallbackPathToken()).thenReturn("callback-token");
        lenient().when(storageService.saveIncoming(anyString())).thenReturn(11L);
        lenient().when(storageService.claim(eq(11L), anyString(), any(LocalDateTime.class))).thenReturn(true);
        lenient().when(storageService.get(11L)).thenReturn(new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody("{}").setRetryCount(0));
        lenient().when(storageService.updateParsed(eq(11L), anyString(), any(ErpCloudPrintCallbackRequest.class)))
                .thenReturn(true);
    }

    @Test
    void wrongToken_shouldReturn404AndNotPersist() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.receiveCallback("wrong", "{}"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(storageService, never()).saveIncoming(anyString());
    }

    @Test
    void oversizedBody_shouldReturn413AndNotPersist() {
        String body = repeat('x', ErpCloudPrintCallbackConstants.MAX_BODY_BYTES + 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.receiveCallback("callback-token", body));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getStatus());
        verify(storageService, never()).saveIncoming(anyString());
    }

    @Test
    void stringCode_shouldNormalizeAndFinishSuccess() {
        String body = "{\"method\":\"printRlt\",\"devid\":\"SW1\",\"reqid\":\"REQ1\","
                + "\"code\":\"201\",\"message\":\"失败\"}";
        ErpCloudPrintCallbackLogDO callback = new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody(body).setRetryCount(0);
        callback.setCreateTime(LocalDateTime.now());
        when(storageService.get(11L)).thenReturn(callback);
        when(callbackProcessor.process(any(), any())).thenReturn(
                ErpCloudPrintCallbackProcessResult.success(true, "已处理"));

        service.receiveCallback("callback-token", body);

        ArgumentCaptor<ErpCloudPrintCallbackRequest> captor =
                ArgumentCaptor.forClass(ErpCloudPrintCallbackRequest.class);
        verify(callbackProcessor).process(captor.capture(), any());
        assertEquals(201, captor.getValue().getCode());
        verify(storageService).finish(eq(11L), anyString(),
                eq(ErpCloudPrintCallbackConstants.PROCESS_STATUS_SUCCESS), eq(true),
                eq("已处理"), any(LocalDateTime.class));
    }

    @Test
    void numericCode_shouldNormalizeAndFinishSuccess() {
        String body = "{\"method\":\"devStatus\",\"devid\":\"SW1\",\"code\":0}";
        ErpCloudPrintCallbackLogDO callback = new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody(body).setRetryCount(0);
        callback.setCreateTime(LocalDateTime.now());
        when(storageService.get(11L)).thenReturn(callback);
        when(callbackProcessor.process(any(), any())).thenReturn(
                ErpCloudPrintCallbackProcessResult.success(true, "已处理"));

        service.receiveCallback("callback-token", body);

        ArgumentCaptor<ErpCloudPrintCallbackRequest> captor =
                ArgumentCaptor.forClass(ErpCloudPrintCallbackRequest.class);
        verify(callbackProcessor).process(captor.capture(), any());
        assertEquals(0, captor.getValue().getCode());
    }

    @Test
    void doubleEncodedPrintResult_shouldDecodeExactlyOneLayer() {
        String body = "\"{\\\"method\\\":\\\"printRlt\\\",\\\"devid\\\":\\\"SW1\\\"," 
                + "\\\"reqid\\\":\\\"REQ1\\\",\\\"code\\\":\\\"0\\\"}\"";
        ErpCloudPrintCallbackLogDO callback = new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody(body).setRetryCount(0);
        callback.setCreateTime(LocalDateTime.now());
        when(storageService.get(11L)).thenReturn(callback);
        when(callbackProcessor.process(any(), any())).thenReturn(
                ErpCloudPrintCallbackProcessResult.success(true, "已处理"));

        service.receiveCallback("callback-token", body);

        ArgumentCaptor<ErpCloudPrintCallbackRequest> captor =
                ArgumentCaptor.forClass(ErpCloudPrintCallbackRequest.class);
        verify(callbackProcessor).process(captor.capture(), any());
        assertEquals("printRlt", captor.getValue().getMethod());
        assertEquals("SW1", captor.getValue().getDevid());
        assertEquals("REQ1", captor.getValue().getReqid());
        assertEquals(0, captor.getValue().getCode());
        verify(storageService).saveIncoming(body);
    }

    @Test
    void doubleEncodedDeviceStatus_shouldDecodeExactlyOneLayer() {
        String body = "\"{\\\"method\\\":\\\"devStatus\\\",\\\"devid\\\":\\\"SW1\\\"," 
                + "\\\"code\\\":101,\\\"message\\\":\\\"online\\\"}\"";
        ErpCloudPrintCallbackLogDO callback = new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody(body).setRetryCount(0);
        callback.setCreateTime(LocalDateTime.now());
        when(storageService.get(11L)).thenReturn(callback);
        when(callbackProcessor.process(any(), any())).thenReturn(
                ErpCloudPrintCallbackProcessResult.success(true, "已处理"));

        service.receiveCallback("callback-token", body);

        ArgumentCaptor<ErpCloudPrintCallbackRequest> captor =
                ArgumentCaptor.forClass(ErpCloudPrintCallbackRequest.class);
        verify(callbackProcessor).process(captor.capture(), any());
        assertEquals("devStatus", captor.getValue().getMethod());
        assertEquals(101, captor.getValue().getCode());
        assertEquals("online", captor.getValue().getMessage());
    }

    @Test
    void invalidJson_shouldPersistAndFinishIgnored() {
        when(storageService.get(11L)).thenReturn(new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody("not-json").setRetryCount(0));

        assertDoesNotThrow(() -> service.receiveCallback("callback-token", "not-json"));

        verify(storageService).saveIncoming("not-json");
        verify(storageService).finish(eq(11L), anyString(),
                eq(ErpCloudPrintCallbackConstants.PROCESS_STATUS_IGNORED), eq(false),
                eq("JSON 格式不合法"), any(LocalDateTime.class));
        verify(callbackProcessor, never()).process(any(), any());
    }

    @Test
    void nonObjectJsonAndInvalidNestedJson_shouldFinishIgnored() {
        java.util.List<String> bodies = Arrays.asList(
                "[]",
                "null",
                "123",
                "true",
                "\"plain-text\"",
                "\"[1,2]\"",
                "\"\\\"{\\\\\\\"method\\\\\\\":\\\\\\\"devStatus\\\\\\\"}\\\"\"");

        for (String body : bodies) {
            when(storageService.get(11L)).thenReturn(new ErpCloudPrintCallbackLogDO()
                    .setId(11L).setRawBody(body).setRetryCount(0));
            assertDoesNotThrow(() -> service.receiveCallback("callback-token", body));
        }

        verify(storageService, times(bodies.size())).finish(eq(11L), anyString(),
                eq(ErpCloudPrintCallbackConstants.PROCESS_STATUS_IGNORED), eq(false),
                eq("请求正文必须是 JSON 对象"), any(LocalDateTime.class));
        verify(callbackProcessor, never()).process(any(), any());
    }

    @Test
    void processingFailure_shouldReturnNormallyAndScheduleRetry() {
        String body = "{\"method\":\"devStatus\",\"devid\":\"SW1\",\"code\":104}";
        ErpCloudPrintCallbackLogDO callback = new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody(body).setRetryCount(0);
        callback.setCreateTime(LocalDateTime.now());
        when(storageService.get(11L)).thenReturn(callback);
        when(callbackProcessor.process(any(), any())).thenThrow(new IllegalStateException("database busy"));

        assertDoesNotThrow(() -> service.receiveCallback("callback-token", body));

        verify(storageService).markRetry(eq(11L), anyString(), eq(1),
                any(LocalDateTime.class), anyString());
    }

    @Test
    void persistFailure_shouldPropagateForVendorRetry() {
        when(storageService.saveIncoming(anyString())).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class,
                () -> service.receiveCallback("callback-token", "{}"));
        verify(storageService, never()).claim(anyLong(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void retryPendingCallbacks_shouldRecoverLeaseAndUseBatchLimit() {
        String body = "{\"method\":\"devStatus\",\"devid\":\"SW1\",\"code\":0}";
        ErpCloudPrintCallbackLogDO callback = new ErpCloudPrintCallbackLogDO()
                .setId(11L).setRawBody(body).setRetryCount(0);
        callback.setCreateTime(LocalDateTime.now());
        when(storageService.getDueCallbacks(any(LocalDateTime.class), eq(100)))
                .thenReturn(Collections.singletonList(callback));
        when(storageService.get(11L)).thenReturn(callback);
        when(callbackProcessor.process(any(), any())).thenReturn(
                ErpCloudPrintCallbackProcessResult.success(true, "已处理"));

        assertEquals(1, service.retryPendingCallbacks());

        verify(storageService).recoverStuckProcessing(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(storageService).getDueCallbacks(any(LocalDateTime.class), eq(100));
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

}
