package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutReceiptSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutUpdateExpressFileReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSaleOutController} 的单元测试
 */
public class ErpSaleOutControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleOutController controller;

    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockOutBillService stockOutBillService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private ErpSaleCartMapper saleCartMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpSaleItemPriceReferenceFiller itemPriceReferenceFiller;
    @Mock
    private ErpSalePickDeliveryService salePickDeliveryService;
    @Mock
    private ErpDataPermissionDeptService dataPermissionDeptService;

    // ========== createSaleOut ==========

    @Test
    public void testCreateSaleOut_paramPassThrough() {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        when(saleOutService.createSaleOut(any())).thenReturn(101L);

        CommonResult<Long> result = controller.createSaleOut(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(101L, result.getData());
        verify(saleOutService).createSaleOut(eq(reqVO));
    }

    @Test
    public void testCreateSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("createSaleOut", ErpSaleOutSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:create"));
    }

    // ========== updateSaleOut ==========

    @Test
    public void testUpdateSaleOut_paramPassThrough() {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setId(22L);

        CommonResult<Boolean> result = controller.updateSaleOut(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOut(eq(reqVO));
    }

    @Test
    public void testUpdateSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("updateSaleOut", ErpSaleOutSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update"));
    }

    // ========== updateSaleOutExpressFile ==========

    @Test
    public void testUpdateSaleOutExpressFile_paramPassThrough() {
        ErpSaleOutUpdateExpressFileReqVO reqVO = new ErpSaleOutUpdateExpressFileReqVO();
        reqVO.setId(22L);
        reqVO.setExpressFileUrl("https://example.com/express.jpg");

        CommonResult<Boolean> result = controller.updateSaleOutExpressFile(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOutExpressFile(eq(reqVO));
    }

    @Test
    public void testUpdateSaleOutExpressFile_hasDedicatedPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod(
                "updateSaleOutExpressFile", ErpSaleOutUpdateExpressFileReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:upload-express"));
    }

    // ========== uploadSaleOutExpressFile ==========

    @Test
    public void testUploadSaleOutExpressFile_paramPassThrough() throws Exception {
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        MockMultipartFile file = new MockMultipartFile(
                "file", "express.jpg", "image/jpeg", content);
        when(saleOutService.uploadSaleOutExpressFile(eq(22L), any(byte[].class), eq("express.jpg")))
                .thenReturn("https://example.com/express.jpg");

        CommonResult<String> result = controller.uploadSaleOutExpressFile(22L, file);

        assertEquals(0, result.getCode());
        assertEquals("https://example.com/express.jpg", result.getData());
        verify(saleOutService).uploadSaleOutExpressFile(eq(22L),
                argThat(bytes -> Arrays.equals(content, bytes)), eq("express.jpg"));
    }

    @Test
    public void testUploadSaleOutExpressFile_hasDedicatedMappingAndPermission() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod(
                "uploadSaleOutExpressFile", Long.class, MultipartFile.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/upload-express-file"));
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("erp:sale-out:upload-express"));
    }

    @Test
    public void testUploadSaleOutExpressFile_rejectsOversizedFileBeforeReading() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "express.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.uploadSaleOutExpressFile(22L, file));

        assertEquals(SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED.getCode(), exception.getCode());
        verify(saleOutService, never()).uploadSaleOutExpressFile(any(), any(byte[].class), anyString());
    }

    // ========== updateSaleOutStatus ==========

    @Test
    public void testUpdateSaleOutStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateSaleOutStatus(22L, 20);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOutStatus(eq(22L), eq(20));
    }

    @Test
    public void testUpdateSaleOutStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("updateSaleOutStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update-status"));
    }

    // ========== deleteSaleOut ==========

    @Test
    public void testDeleteSaleOut_paramPassThrough() {
        List<Long> ids = Arrays.asList(10L, 20L);

        CommonResult<Boolean> result = controller.deleteSaleOut(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).deleteSaleOut(eq(ids));
    }

    @Test
    public void testDeleteSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("deleteSaleOut", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:delete"));
    }

    // ========== getSaleOut ==========

    @Test
    public void testGetSaleOut_nullShortCircuit() {
        when(saleOutService.getSaleOut(eq(1024L))).thenReturn(null);

        CommonResult<ErpSaleOutRespVO> result = controller.getSaleOut(1024L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(saleOutService).getSaleOut(eq(1024L));
    }

    @Test
    public void testGetSaleOut_crossDeptItem_enrichesSourceWarehouseAndDept() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(1025L);
        ErpSaleOutItemDO item = new ErpSaleOutItemDO().setId(11L).setOutId(1025L)
                .setProductId(201L).setWarehouseId(888L).setDeptId(100L)
                .setSourceWarehouseId(401L).setSourceDeptId(200L);
        when(saleOutService.getSaleOut(1025L)).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(1025L)).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setName("产品")));
        Map<Long, ErpWarehouseDO> warehouseMap = new HashMap<>();
        warehouseMap.put(888L, new ErpWarehouseDO().setId(888L).setName("销售部门直发仓").setDeptId(100L));
        warehouseMap.put(401L, new ErpWarehouseDO().setId(401L).setName("来源部门仓库").setDeptId(200L));
        when(warehouseService.getWarehouseMap(any())).thenReturn(warehouseMap);
        Map<Long, DeptRespDTO> deptMap = new HashMap<>();
        deptMap.put(100L, new DeptRespDTO().setId(100L).setName("销售部门"));
        deptMap.put(200L, new DeptRespDTO().setId(200L).setName("来源部门"));
        when(deptApi.getDeptMap(any())).thenReturn(deptMap);
        when(saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockOutBillService.getStockOutBillListBySaleOutId(1025L)).thenReturn(Collections.emptyList());

        ErpSaleOutRespVO.Item result = controller.getSaleOut(1025L).getData().getItems().get(0);

        assertEquals("销售部门直发仓", result.getWarehouseName());
        assertEquals("来源部门仓库", result.getSourceWarehouseName());
        assertEquals("来源部门", result.getSourceDeptName());
        assertEquals(Boolean.TRUE, result.getCrossDept());
        verify(warehouseService).getWarehouseMap(argThat(ids ->
                ids.contains(888L) && ids.contains(401L) && ids.size() == 2));
    }

    @Test
    public void testGetSaleOut_fillsFreightTypeFromSourceCart() {
        LocalDateTime sourceCreateTime = LocalDateTime.of(2026, 7, 28, 10, 51, 14);
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(1026L)
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType()).setSourceId(900L);
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(900L).setFreightType("self-pay");
        cart.setCreateTime(sourceCreateTime);
        when(saleOutService.getSaleOut(1026L)).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(1026L)).thenReturn(Collections.emptyList());
        when(saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(saleCartMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(cart));
        when(stockOutBillService.getStockOutBillListBySaleOutId(1026L)).thenReturn(Collections.emptyList());

        ErpSaleOutRespVO result = controller.getSaleOut(1026L).getData();

        assertEquals("self-pay", result.getFreightType());
        assertEquals(sourceCreateTime, result.getSourceCreateTime());
        verify(saleCartMapper).selectBatchIds(argThat(ids -> ids.contains(900L) && ids.size() == 1));
    }

    @Test
    public void testGetSaleOut_costComesFromStockRecordAndIgnoresFreight() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(1027L)
                .setFeeAmount(new BigDecimal("30")).setFreight(new BigDecimal("40"));
        ErpSaleOutItemDO item = new ErpSaleOutItemDO().setId(12L).setOutId(1027L)
                .setProductId(202L).setWarehouseId(889L).setCount(new BigDecimal("2"))
                .setProductPrice(new BigDecimal("100"));
        ErpStockRecordDO stockRecord = new ErpStockRecordDO().setBizItemId(12L)
                .setUnitPrice(new BigDecimal("25")).setTotalPrice(new BigDecimal("-50"));
        when(saleOutService.getSaleOut(1027L)).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(1027L)).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(202L,
                new ErpProductRespVO().setId(202L).setName("产品")));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(889L,
                new ErpWarehouseDO().setId(889L).setName("销售仓").setDeptId(100L)));
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());
        when(saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockRecordMapper.selectList(any())).thenReturn(Collections.singletonList(stockRecord));
        when(stockOutBillService.getStockOutBillListBySaleOutId(1027L)).thenReturn(Collections.emptyList());

        ErpSaleOutRespVO.Item result = controller.getSaleOut(1027L).getData().getItems().get(0);

        assertEquals(0, new BigDecimal("25").compareTo(result.getSaleCostPrice()));
        assertEquals(0, new BigDecimal("50").compareTo(result.getSaleCostAmount()));
    }

    @Test
    public void testGetSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getSaleOut", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    @Test
    public void testGetSaleOutReceiptSummaryReturnsUnreceivedStatusWithOriginalSettlementAmount() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(10L)
                .setTotalPrice(new BigDecimal("120")).setDiscountPercent(new BigDecimal("10"))
                .setFeeAmount(new BigDecimal("5"));
        List<ErpSaleOutItemDO> items = Collections.singletonList(new ErpSaleOutItemDO()
                .setOutId(10L).setOriginalProductPrice(new BigDecimal("100"))
                .setProductPrice(new BigDecimal("120")).setCount(BigDecimal.ONE));
        when(saleOutService.getSaleOut(eq(10L))).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(eq(10L))).thenReturn(items);
        when(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.SALE_OUT.getType()))).thenReturn(BigDecimal.ZERO);

        ErpSaleOutReceiptSummaryRespVO data = controller.getSaleOutReceiptSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("95").compareTo(data.getReceivableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getReceivedAmount()));
        assertEquals(0, new BigDecimal("95").compareTo(data.getUnreceivedAmount()));
        assertEquals(Integer.valueOf(0), data.getReceiptStatus());
    }

    @Test
    public void testGetSaleOutReceiptSummaryReturnsPartialAndReceivedStatus() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(10L).setTotalPrice(new BigDecimal("100"));
        when(saleOutService.getSaleOut(eq(10L))).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(eq(10L))).thenReturn(Collections.emptyList());
        when(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.SALE_OUT.getType()))).thenReturn(new BigDecimal("40"));

        ErpSaleOutReceiptSummaryRespVO partial = controller.getSaleOutReceiptSummary(10L).getData();

        assertNotNull(partial);
        assertEquals(0, new BigDecimal("60").compareTo(partial.getUnreceivedAmount()));
        assertEquals(Integer.valueOf(1), partial.getReceiptStatus());

        when(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.SALE_OUT.getType()))).thenReturn(new BigDecimal("100"));

        ErpSaleOutReceiptSummaryRespVO received = controller.getSaleOutReceiptSummary(10L).getData();

        assertNotNull(received);
        assertEquals(0, BigDecimal.ZERO.compareTo(received.getUnreceivedAmount()));
        assertEquals(Integer.valueOf(2), received.getReceiptStatus());
    }

    // ========== getSaleOutPage ==========

    @Test
    public void testGetSaleOutPage_emptyShortCircuit() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        PageResult<ErpSaleOutDO> emptyPage = new PageResult<>(new ArrayList<>(), 0L);
        when(saleOutService.getSaleOutPage(eq(reqVO))).thenReturn(emptyPage);

        CommonResult<PageResult<ErpSaleOutRespVO>> result = controller.getSaleOutPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(saleOutService).getSaleOutPage(eq(reqVO));
    }

    @Test
    public void testGetSaleOutPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getSaleOutPage", ErpSaleOutPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    @Test
    public void testGetSaleOutPage_fillsAdjustAndReturnStatuses() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        List<ErpSaleOutDO> saleOuts = Arrays.asList(
                new ErpSaleOutDO().setId(1L).setAdjusted(false),
                new ErpSaleOutDO().setId(2L).setAdjusted(false),
                new ErpSaleOutDO().setId(3L).setAdjusted(false),
                new ErpSaleOutDO().setId(4L).setAdjusted(true));
        List<ErpSaleOutItemDO> items = Arrays.asList(
                saleOutItem(11L, 1L, false, "10"),
                saleOutItem(12L, 1L, false, "8"),
                saleOutItem(21L, 2L, true, "10"),
                saleOutItem(22L, 2L, false, "8"),
                saleOutItem(31L, 3L, true, "2"),
                saleOutItem(32L, 3L, true, "3"));
        Map<Long, BigDecimal> returnedCountMap = new HashMap<>();
        returnedCountMap.put(21L, new BigDecimal("5"));
        returnedCountMap.put(31L, new BigDecimal("2"));
        returnedCountMap.put(32L, new BigDecimal("3"));

        when(saleOutService.getSaleOutPage(eq(reqVO))).thenReturn(new PageResult<>(saleOuts, 4L));
        when(saleOutService.getSaleOutItemListByOutIds(any())).thenReturn(items);
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());
        when(stockOutBillService.getStockOutBillListBySaleOutIds(any())).thenReturn(Collections.emptyList());
        when(saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(any())).thenReturn(returnedCountMap);

        PageResult<ErpSaleOutRespVO> result = controller.getSaleOutPage(reqVO).getData();

        assertEquals(4L, result.getTotal());
        assertEquals(0, result.getList().get(0).getAdjustStatus());
        assertEquals(0, result.getList().get(0).getReturnStatus());
        assertEquals(1, result.getList().get(1).getAdjustStatus());
        assertEquals(1, result.getList().get(1).getReturnStatus());
        assertEquals(2, result.getList().get(2).getAdjustStatus());
        assertEquals(2, result.getList().get(2).getReturnStatus());
        assertEquals(2, result.getList().get(3).getAdjustStatus());
        assertEquals(0, result.getList().get(3).getReturnStatus());
        verify(fieldPermissionMasker).maskSaleDetailFormsWithItems(eq("erp_sale_out"), argThat(collection -> {
            List<?> list = new ArrayList<>(collection);
            return list.size() == 4
                    && Integer.valueOf(0).equals(((ErpSaleOutRespVO) list.get(0)).getAdjustStatus())
                    && Integer.valueOf(1).equals(((ErpSaleOutRespVO) list.get(1)).getAdjustStatus())
                    && Integer.valueOf(2).equals(((ErpSaleOutRespVO) list.get(2)).getAdjustStatus())
                    && Integer.valueOf(2).equals(((ErpSaleOutRespVO) list.get(3)).getAdjustStatus());
        }));
    }

    @Test
    public void testGetSaleOutPage_includeItemsFalse_usesLightList() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        reqVO.setIncludeItems(false);
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(5L).setAdjusted(false);
        ErpSaleOutItemDO item = saleOutItem(51L, 5L, true, "2");
        when(saleOutService.getSaleOutPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(saleOut), 1L));
        when(saleOutItemMapper.selectLightListByOutIds(any())).thenReturn(Collections.singletonList(item));
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());
        when(stockOutBillService.getStockOutBillListBySaleOutIds(any())).thenReturn(Collections.emptyList());
        when(salePickDeliveryService.getSummaryMapBySaleOutIds(any())).thenReturn(Collections.emptyMap());
        when(saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(any())).thenReturn(Collections.emptyMap());

        PageResult<ErpSaleOutRespVO> result = controller.getSaleOutPage(reqVO).getData();

        ErpSaleOutRespVO respVO = result.getList().get(0);
        assertEquals(2, respVO.getAdjustStatus());
        assertEquals(0, respVO.getReturnStatus());
        assertNull(respVO.getItems());
        verify(saleOutService, never()).getSaleOutItemListByOutIds(any());
        verify(saleOutItemMapper).selectLightListByOutIds(any());
    }

    private ErpSaleOutItemDO saleOutItem(Long id, Long outId, Boolean adjusted, String count) {
        return new ErpSaleOutItemDO().setId(id).setOutId(outId)
                .setAdjusted(adjusted).setCount(new BigDecimal(count));
    }

    // ========== getReturnableItems ==========

    @Test
    public void testGetReturnableItems_paramPassThrough() {
        List<ErpSaleReturnableItemRespVO> mockItems = Collections.singletonList(new ErpSaleReturnableItemRespVO());
        when(saleOutService.getReturnableItemsByOutId(eq(17386L))).thenReturn(mockItems);

        CommonResult<List<ErpSaleReturnableItemRespVO>> result = controller.getReturnableItems(17386L);

        assertEquals(0, result.getCode());
        assertEquals(mockItems, result.getData());
        verify(saleOutService).getReturnableItemsByOutId(eq(17386L));
    }

    @Test
    public void testGetReturnableItems_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getReturnableItems", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:create"));
    }

    // ========== exportSaleOutExcel ==========

    @Test
    public void testExportSaleOutExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("exportSaleOutExcel",
                ErpSaleOutPageReqVO.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:export"));
    }

}
