package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintPreviewSaleOutRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintTemplateDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintDeviceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintTemplateMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpCloudPrintPreviewServiceTest {

    private final ErpCloudPrintServiceImpl service = new ErpCloudPrintServiceImpl();
    private final ErpCloudPrintDeviceMapper deviceMapper = mock(ErpCloudPrintDeviceMapper.class);
    private final ErpCloudPrintTaskMapper taskMapper = mock(ErpCloudPrintTaskMapper.class);
    private final ErpSaleOutService saleOutService = mock(ErpSaleOutService.class);
    private final ErpSaleOutItemMapper itemMapper = mock(ErpSaleOutItemMapper.class);
    private final ErpWarehouseMapper warehouseMapper = mock(ErpWarehouseMapper.class);
    private final ErpPrintService printService = mock(ErpPrintService.class);
    private final ErpPrintTemplateMapper templateMapper = mock(ErpPrintTemplateMapper.class);
    private final ErpCloudPrintHtmlRenderer renderer = mock(ErpCloudPrintHtmlRenderer.class);
    private final FileApi fileApi = mock(FileApi.class);
    private final ErpCloudPrintQueueService queueService = mock(ErpCloudPrintQueueService.class);
    private final ErpCloudPrintPreviewCacheService cacheService = mock(ErpCloudPrintPreviewCacheService.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    private final byte[] previewPdf = "%PDF-preview-identical".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(service, "deviceMapper", deviceMapper);
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "saleOutService", saleOutService);
        ReflectionTestUtils.setField(service, "saleOutItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "warehouseMapper", warehouseMapper);
        ReflectionTestUtils.setField(service, "printService", printService);
        ReflectionTestUtils.setField(service, "printTemplateMapper", templateMapper);
        ReflectionTestUtils.setField(service, "htmlRenderer", renderer);
        ReflectionTestUtils.setField(service, "fileApi", fileApi);
        ReflectionTestUtils.setField(service, "queueService", queueService);
        ReflectionTestUtils.setField(service, "previewCacheService", cacheService);
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(service, "transactionManager", transactionManager);
        stubSaleOutSource();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void previewSaleOut_shouldCachePdfWithoutCreatingTaskOrUploading() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ErpCloudPrintPreviewSaleOutRespVO response = service.previewSaleOut(1L, 2, 10L);

            assertNotNull(response.getPreviewToken());
            assertEquals(1, response.getItems().size());
            assertEquals("一号仓", response.getItems().get(0).getWarehouseName());
            assertEquals(2, response.getItems().get(0).getCopies());
            ArgumentCaptor<ErpCloudPrintPreviewCacheService.PreviewRecord> captor =
                    ArgumentCaptor.forClass(ErpCloudPrintPreviewCacheService.PreviewRecord.class);
            verify(cacheService).put(captor.capture());
            assertEquals(7L, captor.getValue().getTenantId());
            assertEquals(99L, captor.getValue().getUserId());
            verify(taskMapper, never()).insert(any(ErpCloudPrintTaskDO.class));
            verify(fileApi, never()).createFile(any(), anyString(), anyString(), anyString());
            verify(queueService, never()).dispatchDeviceAsync(anyLong());
        }
    }

    @Test
    void previewSaleOut_shouldKeepDetailSortInPreviewCacheAndDataRequest() {
        ErpSaleOutItemDO item = new ErpSaleOutItemDO()
                .setWarehouseId(20L)
                .setCount(BigDecimal.ONE)
                .setTotalPrice(new BigDecimal("12.34"))
                .setTotalWeight(BigDecimal.ZERO);
        Map<String, Object> printData = new LinkedHashMap<>();
        printData.put("document", new LinkedHashMap<>());
        printData.put("main", new LinkedHashMap<>());
        printData.put("items", Collections.singletonList(
                new LinkedHashMap<>(Map.of("items.productName", "测试配件"))));
        when(itemMapper.selectListByOutId(1L, "productName", "desc")).thenReturn(List.of(item));
        when(printService.getPrintData(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT, 1L,
                "productName", "desc")).thenReturn(printData);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            service.previewSaleOut(1L, 2, 10L, "productName", "desc");
        }

        ArgumentCaptor<ErpCloudPrintPreviewCacheService.PreviewRecord> captor =
                ArgumentCaptor.forClass(ErpCloudPrintPreviewCacheService.PreviewRecord.class);
        verify(cacheService).put(captor.capture());
        assertEquals("productName", captor.getValue().getItemOrderField());
        assertEquals("desc", captor.getValue().getItemOrderDirection());
        verify(itemMapper).selectListByOutId(1L, "productName", "desc");
        verify(printService).getPrintData(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT, 1L,
                "productName", "desc");
    }

    @Test
    void submitSaleOut_shouldUploadTheExactPreviewPdfAndCacheIdempotentResult() throws Exception {
        ErpCloudPrintPreviewCacheService.PreviewRecord record;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            service.previewSaleOut(1L, 2, 10L);
            ArgumentCaptor<ErpCloudPrintPreviewCacheService.PreviewRecord> captor =
                    ArgumentCaptor.forClass(ErpCloudPrintPreviewCacheService.PreviewRecord.class);
            verify(cacheService).put(captor.capture());
            record = captor.getValue();
        }

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(cacheService.getBoundResult(record.getPreviewToken())).thenReturn(null);
        when(cacheService.getBound(record.getPreviewToken())).thenReturn(record);
        TransactionStatus status = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(status);
        when(fileApi.createFile(any(), anyString(), anyString(), anyString())).thenReturn("https://file/preview.pdf");
        org.mockito.Mockito.doAnswer(invocation -> {
            ((ErpCloudPrintTaskDO) invocation.getArgument(0)).setId(501L);
            return 1;
        }).when(taskMapper).insert(any(ErpCloudPrintTaskDO.class));
        when(taskMapper.selectQueuePosition(100L, 501L)).thenReturn(1L);
        when(deviceMapper.selectById(100L)).thenReturn(device());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertEquals(1, service.submitSaleOut(record.getPreviewToken()).size());
        }

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(fileApi).createFile(contentCaptor.capture(), anyString(), anyString(), anyString());
        assertArrayEquals(previewPdf, contentCaptor.getValue());
        verify(cacheService).putResult(record.getPreviewToken(), List.of(501L));
        verify(cacheService).delete(record.getPreviewToken());
        verify(queueService).dispatchDeviceAsync(100L);
    }

    @Test
    void submitSaleOut_shouldRejectWhenPreviewInputHasChanged() throws Exception {
        ErpCloudPrintPreviewCacheService.PreviewRecord record = createPreviewRecord();
        RLock lock = stubConfirmationLock(record.getPreviewToken());
        when(cacheService.getBoundResult(record.getPreviewToken())).thenReturn(null);
        when(cacheService.getBound(record.getPreviewToken())).thenReturn(record);
        when(templateMapper.selectById(10L)).thenReturn(new ErpPrintTemplateDO()
                .setId(10L)
                .setModuleKey(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setTemplateJson("{\"cells\":[{\"changed\":true}]}")
                .setPaperConfig("{}"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(RuntimeException.class, () -> service.submitSaleOut(record.getPreviewToken()));
        }

        verify(lock).unlock();
        verify(fileApi, never()).createFile(any(), anyString(), anyString(), anyString());
        verify(taskMapper, never()).insert(any(ErpCloudPrintTaskDO.class));
        verify(cacheService, never()).delete(record.getPreviewToken());
    }

    @Test
    void submitSaleOut_shouldReturnCachedResultWithoutCreatingDuplicateTask() throws Exception {
        String token = "confirmed-token";
        stubConfirmationLock(token);
        when(cacheService.getBoundResult(token)).thenReturn(new ErpCloudPrintPreviewCacheService.ConfirmResult()
                .setTenantId(7L).setUserId(99L).setTaskIds(List.of(501L)));
        ErpCloudPrintTaskDO task = new ErpCloudPrintTaskDO()
                .setId(501L)
                .setDeviceId(100L)
                .setStatus(ErpCloudPrintConstants.STATUS_SUBMITTED);
        when(taskMapper.selectBatchIds(List.of(501L))).thenReturn(List.of(task));
        when(deviceMapper.selectById(100L)).thenReturn(device());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertEquals(501L, service.submitSaleOut(token).get(0).getId());
        }

        verify(cacheService, never()).getBound(token);
        verify(fileApi, never()).createFile(any(), anyString(), anyString(), anyString());
        verify(taskMapper, never()).insert(any(ErpCloudPrintTaskDO.class));
        verify(queueService, never()).dispatchDeviceAsync(anyLong());
    }

    private ErpCloudPrintPreviewCacheService.PreviewRecord createPreviewRecord() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            service.previewSaleOut(1L, 2, 10L);
            ArgumentCaptor<ErpCloudPrintPreviewCacheService.PreviewRecord> captor =
                    ArgumentCaptor.forClass(ErpCloudPrintPreviewCacheService.PreviewRecord.class);
            verify(cacheService).put(captor.capture());
            return captor.getValue();
        }
    }

    private RLock stubConfirmationLock(String token) throws InterruptedException {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        return lock;
    }

    private void stubSaleOutSource() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(1L).setNo("XSCK20260929000001");
        ErpSaleOutItemDO item = new ErpSaleOutItemDO()
                .setWarehouseId(20L)
                .setCount(BigDecimal.ONE)
                .setTotalPrice(new BigDecimal("12.34"))
                .setTotalWeight(BigDecimal.ZERO);
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder().id(20L).name("一号仓")
                .cloudPrintDeviceId(100L).build();
        ErpPrintTemplateDO template = new ErpPrintTemplateDO()
                .setId(10L)
                .setModuleKey(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setTemplateJson("{\"cells\":[]}")
                .setPaperConfig("{}");
        Map<String, Object> printData = new LinkedHashMap<>();
        printData.put("document", new LinkedHashMap<>());
        printData.put("main", new LinkedHashMap<>());
        printData.put("items", Collections.singletonList(new LinkedHashMap<>(Map.of("items.productName", "测试配件"))));

        when(saleOutService.validateSaleOut(1L)).thenReturn(saleOut);
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(itemMapper.selectListByOutId(1L)).thenReturn(List.of(item));
        when(warehouseMapper.selectByIds(any())).thenReturn(List.of(warehouse));
        when(deviceMapper.selectByIds(any())).thenReturn(List.of(device()));
        when(printService.getPrintData(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT, 1L)).thenReturn(printData);
        when(renderer.renderSaleOutTemplatePdf(anyString(), anyString(), any(), any(), any())).thenReturn(previewPdf);
    }

    private ErpCloudPrintDeviceDO device() {
        return new ErpCloudPrintDeviceDO()
                .setId(100L)
                .setDevid("SW-100")
                .setNickname("一号打印机")
                .setDevType(ErpCloudPrintConstants.DEV_TYPE_DOT_MATRIX)
                .setContentType(ErpCloudPrintConstants.CONTENT_TYPE_PDF)
                .setPrintWidth(241)
                .setPrintHeight(140)
                .setPaperType(4)
                .setRotate(0)
                .setCopies(1)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setQueuePaused(false);
    }
}
