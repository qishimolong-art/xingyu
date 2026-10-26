package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.idev.excel.annotation.format.NumberFormat;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableStockReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableStockRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseOrderControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseOrderController purchaseOrderController;
    @InjectMocks
    private ErpPurchaseInController purchaseInController;
    @InjectMocks
    private ErpPurchaseReturnController purchaseReturnController;
    @InjectMocks
    private ErpPurchaseInvoiceController purchaseInvoiceController;
    @InjectMocks
    private ErpPurchasePriceAdjustController purchasePriceAdjustController;

    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private ErpPurchaseOrderService purchaseOrderService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Test
    void getPurchaseOrderExportFields_includesBusinessDetailFieldsAndKeepsAmountNumericStyle() throws Exception {
        when(fieldPermissionMasker.getHiddenFieldSet("erp_purchase_order"))
                .thenReturn(Collections.emptySet());

        CommonResult<java.util.List<ErpExportFieldRespVO>> result =
                purchaseOrderController.getPurchaseOrderExportFields();

        assertTrue(result.isSuccess());
        Set<String> fields = result.getData().stream()
                .map(ErpExportFieldRespVO::getField)
                .collect(Collectors.toSet());
        assertTrue(fields.containsAll(Set.of(
                "deptName", "purchaserName", "orderDate", "totalProductPrice", "discountPrice", "depositPrice",
                "itemInStatus", "warehouseName", "itemDeptName", "stockCount", "lastPurchasePrice",
                "vehicleModel", "standard", "featureCode", "pieceCount", "arrivalCount", "warehousePosition",
                "drawingNo", "batchNo", "factoryCode", "brand", "originPlace", "itemInCount", "itemReturnCount")));
        Field amountField = ErpPurchaseOrderExportRespVO.class.getDeclaredField("productPrice");
        NumberFormat numberFormat = amountField.getAnnotation(NumberFormat.class);
        assertNotNull(numberFormat);
        assertEquals("0.00####", numberFormat.value());
        assertEquals(BigDecimal.class,
                ErpPurchaseOrderExportRespVO.class.getDeclaredField("stockCount").getType());
    }

    @Test
    void downloadImportFailureDetails_delegatesPurchaseOrderModule() throws Exception {
        Long recordId = 300L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        purchaseOrderController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_purchase_order", response);
    }

    @Test
    void getPurchaseOrderItemPage_buildsPagedItemsWithProductAndStock() {
        ErpPurchaseOrderItemPageReqVO reqVO = new ErpPurchaseOrderItemPageReqVO();
        reqVO.setOrderId(10L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setMask(true);
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L)
                .setOrderId(10L)
                .setProductId(200L)
                .setWarehouseId(30L)
                .setDeptId(40L)
                .setCount(new BigDecimal("3"))
                .setProductPrice(new BigDecimal("4.50"));
        when(purchaseOrderService.getPurchaseOrderItemPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(item), 1L));
        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(200L);
        product.setCode("P001");
        product.setName("滤芯");
        product.setBarCode("BAR001");
        product.setUnitName("个");
        product.setWeight(new BigDecimal("1.25"));
        product.setPackageQty(12);
        product.setBatchNoEnabled(true);
        product.setPurchasePrice(new BigDecimal("4.10"));
        product.setSalePrice(new BigDecimal("6.20"));
        product.setMinPrice(new BigDecimal("5.00"));
        product.setReferencePrice(new BigDecimal("5.80"));
        product.setRetailPrice(new BigDecimal("6.80"));
        product.setLastPurchasePrice(new BigDecimal("4.20"));
        product.setGrossProfitRate(20);
        product.setBackupPrice1(new BigDecimal("5.50"));
        product.setWholesalePrice(new BigDecimal("5.30"));
        product.setSharePrice(new BigDecimal("5.10"));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(200L, product));
        when(stockService.getStockCountMap(any())).thenReturn(Collections.singletonMap(200L, new BigDecimal("8")));
        when(stockService.getStockMap(any(), any())).thenReturn(Collections.singletonMap("200_30",
                new ErpStockDO().setProductId(200L).setWarehouseId(30L).setPurchasePrice(new BigDecimal("4.05"))));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(30L,
                new ErpWarehouseDO().setId(30L).setName("主仓库")));
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(40L);
        dept.setName("采购一部");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(40L, dept));
        when(saleOutItemMapper.selectLatestSalePriceMap(any()))
                .thenReturn(Collections.singletonMap(200L, new BigDecimal("6.00")));

        CommonResult<PageResult<ErpPurchaseOrderRespVO.Item>> result =
                purchaseOrderController.getPurchaseOrderItemPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        ErpPurchaseOrderRespVO.Item respItem = result.getData().getList().get(0);
        assertEquals(Long.valueOf(1L), respItem.getId());
        assertEquals("P001", respItem.getProductCode());
        assertEquals("滤芯", respItem.getProductName());
        assertEquals("个", respItem.getProductUnitName());
        assertEquals("主仓库", respItem.getWarehouseName());
        assertEquals("采购一部", respItem.getDeptName());
        assertEquals(0, new BigDecimal("8").compareTo(respItem.getStockCount()));
        assertEquals(0, new BigDecimal("4.05").compareTo(respItem.getProductPurchasePrice()));
        assertEquals(0, new BigDecimal("6.20").compareTo(respItem.getSalePrice()));
        assertEquals(0, new BigDecimal("6.00").compareTo(respItem.getLastSalePrice()));
        assertEquals(0, new BigDecimal("5.00").compareTo(respItem.getMinPrice()));
        assertEquals(0, new BigDecimal("5.80").compareTo(respItem.getReferencePrice()));
        assertEquals(0, new BigDecimal("6.80").compareTo(respItem.getRetailPrice()));
        assertEquals(0, new BigDecimal("4.20").compareTo(respItem.getLastPurchasePrice()));
        assertEquals(Integer.valueOf(20), respItem.getGrossProfitRate());
        assertEquals(0, new BigDecimal("5.50").compareTo(respItem.getBackupPrice1()));
        assertEquals(0, new BigDecimal("5.30").compareTo(respItem.getWholesalePrice()));
        assertEquals(0, new BigDecimal("5.10").compareTo(respItem.getSharePrice()));
        verify(purchaseOrderService).getPurchaseOrderItemPage(eq(reqVO));
        verify(productService).getProductVOMap(any());
        verify(stockService).getStockCountMap(any());
        verify(stockService).getStockMap(any(), any());
        verify(warehouseService).getWarehouseMap(any());
        verify(deptApi).getDeptMap(any());
        verify(saleOutItemMapper).selectLatestSalePriceMap(any());
        verify(fieldPermissionMasker).clearHiddenItemFields(eq("erp_purchase_order"), eq(result.getData().getList()));
    }

    @Test
    void getInableItemPage_delegatesPagedQuery() {
        ErpPurchaseOrderInableItemPageReqVO reqVO = new ErpPurchaseOrderInableItemPageReqVO();
        reqVO.setOrderId(10L);
        reqVO.setProductKeyword("P001");
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        ErpPurchaseOrderInableItemRespVO item = new ErpPurchaseOrderInableItemRespVO();
        item.setOrderItemId(101L);
        item.setProductCode("P001");
        item.setInableCount(new BigDecimal("3"));
        when(purchaseOrderService.getInableItemPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(item), 1L));

        CommonResult<PageResult<ErpPurchaseOrderInableItemRespVO>> result =
                purchaseOrderController.getInableItemPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals("P001", result.getData().getList().get(0).getProductCode());
        assertEquals(0, new BigDecimal("3").compareTo(result.getData().getList().get(0).getInableCount()));
        verify(purchaseOrderService).getInableItemPage(eq(reqVO));
    }

    @Test
    void getInableItemStockList_delegatesAndKeepsPurchaseInCreatePermission() throws Exception {
        ErpPurchaseOrderInableStockReqVO reqVO = new ErpPurchaseOrderInableStockReqVO();
        reqVO.setOrderId(10L);
        ErpPurchaseOrderInableStockRespVO item = new ErpPurchaseOrderInableStockRespVO();
        item.setOrderItemId(101L);
        item.setWarehouseId(8L);
        item.setStockCount(new BigDecimal("3"));
        item.setAvailableCount(new BigDecimal("-2"));
        when(purchaseOrderService.getInableItemStockList(eq(reqVO)))
                .thenReturn(Collections.singletonList(item));

        CommonResult<java.util.List<ErpPurchaseOrderInableStockRespVO>> result =
                purchaseOrderController.getInableItemStockList(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(0, new BigDecimal("-2").compareTo(result.getData().get(0).getAvailableCount()));
        verify(purchaseOrderService).getInableItemStockList(eq(reqVO));
        Method method = ErpPurchaseOrderController.class.getMethod(
                "getInableItemStockList", ErpPurchaseOrderInableStockReqVO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("erp:purchase-in:create"));
    }

    @Test
    void detailImportTaskEndpoints_allowCreateOrUpdatePermission() throws Exception {
        for (String methodName : Arrays.asList(
                "startDetailImportTask", "getActiveDetailImportTask", "getDetailImportTask",
                "getDetailImportTaskResult", "downloadDetailImportFailureDetails")) {
            Method method = Arrays.stream(ErpPurchaseOrderController.class.getMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            assertNotNull(preAuthorize);
            assertTrue(preAuthorize.value().contains("erp:purchase-order:create"));
            assertTrue(preAuthorize.value().contains("erp:purchase-order:update"));
        }
    }

    @Test
    void downloadImportFailureDetails_delegatesPurchaseInModule() throws Exception {
        Long recordId = 301L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        purchaseInController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_purchase_in", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesPurchaseReturnModule() throws Exception {
        Long recordId = 302L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        purchaseReturnController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_purchase_return", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesPurchaseInvoiceModule() throws Exception {
        Long recordId = 303L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        purchaseInvoiceController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_purchase_invoice", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesPurchasePriceAdjustModule() throws Exception {
        Long recordId = 304L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        purchasePriceAdjustController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId,
                "erp_purchase_price_adjust", response);
    }

}
