package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockBatchNoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockInTransitDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockOccupiedDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPendingInDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchQuantityDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockBatchQuantityMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOccupiedDetailMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockPendingInDetailMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.product.ErpProductPriceSystemService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.config.ErpStockSelectPriceConfigService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

@MockitoSettings(strictness = Strictness.LENIENT)
public class ErpStockControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockController controller;

    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpStockBatchQuantityMapper stockBatchQuantityMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpStockPendingInDetailMapper stockPendingInDetailMapper;
    @Mock
    private ErpStockOccupiedDetailMapper stockOccupiedDetailMapper;
    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Mock
    private ErpProductPriceSystemService productPriceSystemService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpStockSelectPriceConfigService stockSelectPriceConfigService;

    @BeforeEach
    public void setUpPendingChangeSummary() {
        lenient().when(stockMapper.selectOccupiedCountMap(any(), any())).thenReturn(Collections.emptyMap());
        lenient().when(stockMapper.selectPendingInCountMap(any(), any())).thenReturn(Collections.emptyMap());
        lenient().when(stockBatchQuantityMapper.selectOccupiedList(any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(stockBatchQuantityMapper.selectPendingInList(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(stockBatchQuantityMapper.selectInTransitList(any(), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    public void testGetAvailableBatchNoList_saleUsesSaleWarehousePermission() {
        Long productId = 10L;
        Long warehouseId = 20L;
        List<ErpStockBatchNoRespVO> batchNoList = Collections.singletonList(new ErpStockBatchNoRespVO());
        when(stockService.getAvailableBatchNoList(productId, warehouseId)).thenReturn(batchNoList);

        CommonResult<List<ErpStockBatchNoRespVO>> result =
                controller.getAvailableBatchNoList(productId, warehouseId, "sale");

        assertEquals(batchNoList, result.getData());
        verify(warehouseService).validSaleWarehouseList(Collections.singleton(warehouseId));
    }

    @Test
    public void testGetAvailableBatchNoList_defaultKeepsStockPermission() {
        Long productId = 10L;
        Long warehouseId = 20L;
        ErpStockDO stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId);
        when(stockService.getStock(productId, warehouseId)).thenReturn(stock);
        when(stockService.getAvailableBatchNoList(productId, warehouseId)).thenReturn(Collections.emptyList());

        controller.getAvailableBatchNoList(productId, warehouseId, null);

        verify(warehouseService).validateCurrentUserStockPermission(Collections.singleton(stock));
    }

    @Test
    public void testGetStockPage_saleWithoutDeptShowsExternalPricesAllowedForSaleDept() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("sale");
        reqVO.setPriceSystemId(9L);
        reqVO.setOrderField("salePrice");
        reqVO.setOrderDirection("desc");

        ErpStockDO ownStock = new ErpStockDO().setId(1L).setProductId(10L).setWarehouseId(20L)
                .setCount(BigDecimal.TEN).setCostPrice(new BigDecimal("5.00"))
                .setCostAmount(new BigDecimal("50.00")).setPurchasePrice(new BigDecimal("6.00"));
        ErpStockDO externalStock = new ErpStockDO().setId(2L).setProductId(10L).setWarehouseId(21L)
                .setCount(new BigDecimal("8")).setCostPrice(new BigDecimal("5.00"))
                .setCostAmount(new BigDecimal("40.00")).setPurchasePrice(new BigDecimal("6.00"));
        ErpProductRespVO product = new ErpProductRespVO().setId(10L).setName("P1").setCode("P001")
                .setPurchasePrice(new BigDecimal("6.20"))
                .setLastPurchasePrice(new BigDecimal("6.50")).setSalePrice(new BigDecimal("10.00"))
                .setMinPrice(new BigDecimal("8.00")).setGrossProfitRate(25)
                .setReferencePrice(new BigDecimal("11.00")).setRetailPrice(new BigDecimal("12.00"))
                .setBackupPrice1(new BigDecimal("13.00")).setWholesalePrice(new BigDecimal("9.00"))
                .setSharePrice(new BigDecimal("8.80"))
                .setCustomFields(Collections.singletonMap("vipPrice", new BigDecimal("7.70")));
        ErpWarehouseDO ownWarehouse = new ErpWarehouseDO().setId(20L).setName("Own").setDeptId(30L);
        ErpWarehouseDO externalWarehouse = new ErpWarehouseDO().setId(21L).setName("External").setDeptId(31L);

        when(stockService.getStockPage(any(ErpStockPageReqVO.class)))
                .thenReturn(new PageResult<>(Arrays.asList(ownStock, externalStock), 2L));
        when(productService.getProductVOMap(any(), eq(30L)))
                .thenReturn(Collections.singletonMap(10L, product));
        Map<Long, ErpWarehouseDO> warehouseMap = new HashMap<>();
        warehouseMap.put(20L, ownWarehouse);
        warehouseMap.put(21L, externalWarehouse);
        when(warehouseService.getWarehouseMap(any())).thenReturn(warehouseMap);
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());
        when(stockMapper.selectOccupiedCountMap(any(), any())).thenReturn(
                Collections.singletonMap("10_21", new BigDecimal("3")));
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.emptyMap());
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());
        when(productPriceSystemService.getProductPriceMap(any(), eq(9L)))
                .thenReturn(Collections.singletonMap(10L, new BigDecimal("14.00")));

        PageResult<ErpStockRespVO> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(30L);
            result = controller.getStockPage(reqVO).getData();
        }

        ErpStockRespVO ownRow = result.getList().get(0);
        assertTrue(ownRow.getPriceVisible());
        assertEquals(new BigDecimal("10.00"), ownRow.getSalePrice());
        assertEquals(new BigDecimal("14.00"), ownRow.getCurrentPrice());

        ErpStockRespVO externalRow = result.getList().get(1);
        assertTrue(externalRow.getPriceVisible());
        assertEquals(new BigDecimal("5.00"), externalRow.getCostPrice());
        assertEquals(new BigDecimal("40.00"), externalRow.getCostAmount());
        assertEquals(new BigDecimal("6.00"), externalRow.getPurchasePrice());
        assertEquals(new BigDecimal("6.20"), externalRow.getProductPurchasePrice());
        assertEquals(new BigDecimal("6.50"), externalRow.getLastPurchasePrice());
        assertEquals(new BigDecimal("10.00"), externalRow.getSalePrice());
        assertEquals(new BigDecimal("8.00"), externalRow.getMinPrice());
        assertEquals(new BigDecimal("11.00"), externalRow.getReferencePrice());
        assertEquals(new BigDecimal("12.00"), externalRow.getRetailPrice());
        assertEquals(25, externalRow.getGrossProfitRate());
        assertEquals(new BigDecimal("13.00"), externalRow.getBackupPrice1());
        assertEquals(new BigDecimal("9.00"), externalRow.getWholesalePrice());
        assertEquals(new BigDecimal("8.80"), externalRow.getSharePrice());
        assertEquals(new BigDecimal("7.70"), externalRow.getCustomFields().get("vipPrice"));
        assertEquals(new BigDecimal("14.00"), externalRow.getCurrentPrice());
        assertEquals(new BigDecimal("112.00"), externalRow.getCurrentPriceAmount());
        assertEquals(new BigDecimal("8"), externalRow.getCount());
        assertEquals(new BigDecimal("3"), externalRow.getOccupiedCount());
        assertEquals(new BigDecimal("5"), externalRow.getAvailableCount());
        assertEquals("salePrice", reqVO.getOrderField());
        assertEquals("desc", reqVO.getOrderDirection());
    }

    @Test
    public void testGetStockPage_usesSaleDepartmentForConfiguredPriceMaskingAndSort() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("sale");
        reqVO.setSaleDeptId(40L);
        reqVO.setPriceSystemId(9L);
        reqVO.setOrderField("referencePrice");
        reqVO.setOrderDirection("asc");

        ErpStockDO stock = new ErpStockDO().setId(3L).setProductId(11L).setWarehouseId(22L)
                .setCount(new BigDecimal("2")).setCostPrice(new BigDecimal("5.00"))
                .setCostAmount(new BigDecimal("10.00")).setPurchasePrice(new BigDecimal("6.00"));
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("vipPrice", new BigDecimal("7.70"));
        customFields.put("internalPrice", new BigDecimal("6.60"));
        ErpProductRespVO product = new ErpProductRespVO().setId(11L).setName("P2")
                .setPurchasePrice(new BigDecimal("6.20")).setSalePrice(new BigDecimal("10.00"))
                .setMinPrice(new BigDecimal("8.00")).setReferencePrice(new BigDecimal("11.00"))
                .setGrossProfitRate(25).setBackupPrice1(new BigDecimal("13.00"))
                .setSharePrice(new BigDecimal("8.80"))
                .setCustomFields(customFields);
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(22L).setName("Own").setDeptId(40L);

        when(permissionApi.getCurrentUserHiddenFields("erp_product", 40L))
                .thenReturn(Arrays.asList("referencePrice", "col_referencePrice", "backupPrice1",
                        "purchasePrice", "minPrice", "grossProfitRate", "sharePrice", "internalPrice"));
        when(stockSelectPriceConfigService.getSceneHiddenPriceFields("sale"))
                .thenReturn(new java.util.LinkedHashSet<>(Arrays.asList(
                        "salePrice", "col_salePrice", "vipPrice", "col_vipPrice")));
        when(stockService.getStockPage(any(ErpStockPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any(), eq(40L)))
                .thenReturn(Collections.singletonMap(11L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(22L, warehouse));
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.emptyMap());
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());
        when(productPriceSystemService.getProductPriceMap(any(), eq(9L)))
                .thenReturn(Collections.singletonMap(11L, new BigDecimal("14.00")));

        ErpStockRespVO row = controller.getStockPage(reqVO).getData().getList().get(0);

        assertTrue(row.getPriceVisible());
        assertEquals(new BigDecimal("5.00"), row.getCostPrice());
        assertNull(row.getSalePrice());
        assertNull(row.getProductPurchasePrice());
        assertNull(row.getMinPrice());
        assertNull(row.getReferencePrice());
        assertNull(row.getGrossProfitRate());
        assertNull(row.getBackupPrice1());
        assertNull(row.getSharePrice());
        assertTrue(row.getCustomFields().isEmpty());
        assertNull(row.getCurrentPrice());
        assertNull(row.getCurrentPriceAmount());
        assertNull(reqVO.getOrderField());
        assertNull(reqVO.getOrderDirection());
        verify(productService).getProductVOMap(any(), eq(40L));
        verify(stockSelectPriceConfigService).getSceneHiddenPriceFields("sale");
    }

    @Test
    public void testGetStockPage_purchaseUsesLoginDepartmentForConfiguredPriceMasking() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("purchase");

        ErpStockDO stock = new ErpStockDO().setId(4L).setProductId(12L).setWarehouseId(23L)
                .setCount(new BigDecimal("3")).setPurchasePrice(new BigDecimal("6.00"));
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("vipPrice", new BigDecimal("7.70"));
        customFields.put("internalPrice", new BigDecimal("6.60"));
        ErpProductRespVO product = new ErpProductRespVO().setId(12L).setName("Purchase Product")
                .setPurchasePrice(new BigDecimal("6.20")).setSalePrice(new BigDecimal("10.00"))
                .setMinPrice(new BigDecimal("8.00")).setReferencePrice(new BigDecimal("11.00"))
                .setRetailPrice(new BigDecimal("12.00")).setGrossProfitRate(25)
                .setBackupPrice1(new BigDecimal("13.00")).setWholesalePrice(new BigDecimal("9.00"))
                .setSharePrice(new BigDecimal("8.80")).setCustomFields(customFields);
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(23L).setName("Purchase").setDeptId(41L);

        when(permissionApi.getCurrentUserHiddenFields("erp_product", 41L))
                .thenReturn(Arrays.asList("purchasePrice", "minPrice", "grossProfitRate",
                        "sharePrice", "internalPrice"));
        when(stockService.getStockPage(any(ErpStockPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any(), eq(41L)))
                .thenReturn(Collections.singletonMap(12L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(23L, warehouse));
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.emptyMap());
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());

        ErpStockRespVO row;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(41L);
            row = controller.getStockPage(reqVO).getData().getList().get(0);
        }

        assertNull(row.getProductPurchasePrice());
        assertNull(row.getMinPrice());
        assertNull(row.getGrossProfitRate());
        assertNull(row.getSharePrice());
        assertEquals(new BigDecimal("10.00"), row.getSalePrice());
        assertEquals(new BigDecimal("11.00"), row.getReferencePrice());
        assertEquals(new BigDecimal("12.00"), row.getRetailPrice());
        assertEquals(new BigDecimal("13.00"), row.getBackupPrice1());
        assertEquals(new BigDecimal("9.00"), row.getWholesalePrice());
        assertEquals(Collections.singletonMap("vipPrice", new BigDecimal("7.70")), row.getCustomFields());
        verify(permissionApi).getCurrentUserHiddenFields("erp_product", 41L);
        verify(productService).getProductVOMap(any(), eq(41L));
    }

    @Test
    public void testGetStockPage_deptNameFilledFromWarehouseDeptId() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        ErpStockDO stock = new ErpStockDO();
        stock.setId(1L);
        stock.setProductId(10L);
        stock.setWarehouseId(20L);
        stock.setDeptId(99L);
        stock.setCount(BigDecimal.ONE);

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(10L);
        product.setName("P1");
        product.setCode("P001");
        ErpWarehouseDO warehouse = new ErpWarehouseDO();
        warehouse.setId(20L);
        warehouse.setName("Qionglai Warehouse");
        warehouse.setDeptId(30L);
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(30L);
        dept.setName("Qionglai Dept");

        when(stockService.getStockPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(10L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(20L, warehouse));
        when(warehouseService.getCurrentUserAuthorizedWarehouseList())
                .thenReturn(Collections.singletonList(warehouse));
        when(warehouseService.getCurrentUserStockVisibleWarehouseList())
                .thenReturn(Collections.singletonList(warehouse));
        when(saleCartItemMapper.selectOccupiedCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(purchaseInItemMapper.selectPendingInCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());
        Map<Long, DeptRespDTO> deptMap = new HashMap<>();
        deptMap.put(30L, dept);
        when(deptApi.getDeptMap(any())).thenReturn(deptMap);

        CommonResult<PageResult<ErpStockRespVO>> result = controller.getStockPage(reqVO);

        ErpStockRespVO row = result.getData().getList().get(0);
        assertEquals("Qionglai Warehouse", row.getWarehouseName());
        assertEquals(30L, row.getDeptId());
        assertEquals("Qionglai Dept", row.getDeptName());
        assertFalse(row.getReadonlyBySaleDistribution());
        assertTrue(row.getPriceVisible());
    }

    @Test
    public void testGetStockPage_deptNameFilledFromStockDeptIdWhenWarehouseDeptEmpty() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        ErpStockDO stock = new ErpStockDO();
        stock.setId(2L);
        stock.setProductId(11L);
        stock.setWarehouseId(21L);
        stock.setDeptId(31L);
        stock.setCount(BigDecimal.ONE);

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(11L);
        product.setName("P2");
        product.setCode("P002");
        ErpWarehouseDO warehouse = new ErpWarehouseDO();
        warehouse.setId(21L);
        warehouse.setName("Warehouse Without Dept");
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(31L);
        dept.setName("Stock Dept");

        when(stockService.getStockPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(11L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(21L, warehouse));
        when(saleCartItemMapper.selectOccupiedCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(purchaseInItemMapper.selectPendingInCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());
        Map<Long, DeptRespDTO> deptMap = new HashMap<>();
        deptMap.put(31L, dept);
        when(deptApi.getDeptMap(any())).thenReturn(deptMap);

        CommonResult<PageResult<ErpStockRespVO>> result = controller.getStockPage(reqVO);

        ErpStockRespVO row = result.getData().getList().get(0);
        assertEquals("Warehouse Without Dept", row.getWarehouseName());
        assertEquals(31L, row.getDeptId());
        assertEquals("Stock Dept", row.getDeptName());
        assertFalse(row.getReadonlyBySaleDistribution());
    }

    @Test
    public void testGetStockPage_deptMapLoadedIgnoringDataPermission() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        ErpStockDO stock = new ErpStockDO();
        stock.setId(3L);
        stock.setProductId(12L);
        stock.setWarehouseId(22L);
        stock.setDeptId(32L);
        stock.setCount(BigDecimal.ONE);

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(12L);
        product.setName("P3");
        product.setCode("P003");
        ErpWarehouseDO warehouse = new ErpWarehouseDO();
        warehouse.setId(22L);
        warehouse.setName("Cross Dept Warehouse");
        warehouse.setDeptId(32L);
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(32L);
        dept.setName("Cross Dept");

        when(stockService.getStockPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(12L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(22L, warehouse));
        when(saleCartItemMapper.selectOccupiedCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(purchaseInItemMapper.selectPendingInCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenAnswer(invocation -> {
            assertFalse(DataPermissionContextHolder.get().enable());
            return Collections.singletonMap(32L, dept);
        });

        CommonResult<PageResult<ErpStockRespVO>> result = controller.getStockPage(reqVO);

        ErpStockRespVO row = result.getData().getList().get(0);
        assertEquals(32L, row.getDeptId());
        assertEquals("Cross Dept", row.getDeptName());
    }

    @Test
    public void testGetStockPage_stockSummariesLoadedIgnoringDataPermission() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        ErpStockDO stock = new ErpStockDO();
        stock.setId(31L);
        stock.setProductId(312L);
        stock.setWarehouseId(322L);
        stock.setDeptId(332L);
        stock.setCount(new BigDecimal("100"));

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(312L);
        product.setName("Cross Dept Product");
        ErpWarehouseDO warehouse = new ErpWarehouseDO();
        warehouse.setId(322L);
        warehouse.setName("Authorized Cross Dept Warehouse");
        warehouse.setDeptId(332L);
        String key = "312_322";

        when(stockService.getStockPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(312L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(322L, warehouse));
        when(stockMapper.selectOccupiedCountMap(any(), any())).thenAnswer(invocation -> {
            assertFalse(DataPermissionContextHolder.get().enable());
            return Collections.singletonMap(key, new BigDecimal("6"));
        });
        when(stockMapper.selectPendingInCountMap(any(), any())).thenAnswer(invocation -> {
            assertFalse(DataPermissionContextHolder.get().enable());
            return Collections.singletonMap(key, new BigDecimal("7"));
        });
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any())).thenAnswer(invocation -> {
            assertFalse(DataPermissionContextHolder.get().enable());
            return Collections.singletonMap(key, new BigDecimal("8"));
        });
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpStockRespVO>> result = controller.getStockPage(reqVO);

        ErpStockRespVO row = result.getData().getList().get(0);
        assertEquals(new BigDecimal("6"), row.getOccupiedCount());
        assertEquals(new BigDecimal("94"), row.getAvailableCount());
        assertEquals(new BigDecimal("7"), row.getPendingInCount());
        assertEquals(new BigDecimal("8"), row.getInTransitCount());
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    public void testGetStockPage_inTransitUsesPurchaseOrderSummaryOnly() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        ErpStockDO stock = new ErpStockDO();
        stock.setId(4L);
        stock.setProductId(13L);
        stock.setWarehouseId(23L);
        stock.setCount(BigDecimal.TEN);
        stock.setPendingInCount(new BigDecimal("99"));
        stock.setInTransitCount(new BigDecimal("88"));

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(13L);
        product.setName("P4");
        ErpWarehouseDO warehouse = new ErpWarehouseDO();
        warehouse.setId(23L);
        warehouse.setName("Warehouse");
        String key = "13_23";

        when(stockService.getStockPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(13L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(23L, warehouse));
        when(saleCartItemMapper.selectOccupiedCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(stockMapper.selectOccupiedCountMap(any(), any()))
                .thenReturn(Collections.singletonMap(key, new BigDecimal("3")));
        when(stockMapper.selectPendingInCountMap(any(), any()))
                .thenReturn(Collections.singletonMap(key, new BigDecimal("7")));
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.singletonMap(key, new BigDecimal("5")));
        when(stockService.getAvailableBatchNoListMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpStockRespVO>> result = controller.getStockPage(reqVO);

        verify(purchaseOrderItemMapper).selectInTransitCountMap(any(), any(),
                eq(Collections.singletonList(ErpAuditStatus.APPROVE.getStatus())));
        ErpStockRespVO row = result.getData().getList().get(0);
        assertEquals(new BigDecimal("3"), row.getOccupiedCount());
        assertEquals(new BigDecimal("7"), row.getAvailableCount());
        assertEquals(new BigDecimal("7"), row.getPendingInCount());
        assertEquals(new BigDecimal("5"), row.getInTransitCount());
    }

    @Test
    public void testGetStockSummary_inTransitUsesPurchaseOrderSummaryOnly() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        ErpStockDO stock = new ErpStockDO();
        stock.setId(5L);
        stock.setProductId(14L);
        stock.setWarehouseId(24L);
        stock.setCount(BigDecimal.TEN);
        stock.setPendingInCount(new BigDecimal("99"));
        stock.setInTransitCount(new BigDecimal("88"));

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(14L);
        product.setName("P5");
        ErpWarehouseDO warehouse = new ErpWarehouseDO();
        warehouse.setId(24L);
        warehouse.setName("Warehouse");
        String key = "14_24";

        when(stockService.getStockPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(14L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(24L, warehouse));
        when(saleCartItemMapper.selectOccupiedCountMap(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(stockMapper.selectPendingInCountMap(any(), any()))
                .thenReturn(Collections.singletonMap(key, new BigDecimal("7")));
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.singletonMap(key, new BigDecimal("5")));

        CommonResult<ErpStockSummaryRespVO> result = controller.getStockSummary(reqVO);

        verify(purchaseOrderItemMapper).selectInTransitCountMap(any(), any(),
                eq(Collections.singletonList(ErpAuditStatus.APPROVE.getStatus())));
        assertEquals(new BigDecimal("7"), result.getData().getTotalPendingInCount());
        assertEquals(new BigDecimal("5"), result.getData().getTotalInTransitCount());
    }

    @Test
    public void testGetStockSummary_aggregatesAllMatchingRows() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        ErpStockDO firstStock = new ErpStockDO()
                .setId(51L)
                .setProductId(101L)
                .setWarehouseId(201L)
                .setCount(new BigDecimal("2"))
                .setCostAmount(new BigDecimal("20"))
                .setOccupiedCount(new BigDecimal("99"));
        ErpStockDO secondStock = new ErpStockDO()
                .setId(52L)
                .setProductId(102L)
                .setWarehouseId(202L)
                .setCount(new BigDecimal("3"))
                .setCostAmount(new BigDecimal("45"))
                .setOccupiedCount(new BigDecimal("88"));

        ErpProductRespVO firstProduct = new ErpProductRespVO()
                .setId(101L)
                .setName("P101")
                .setBackupPrice1(new BigDecimal("5"))
                .setWeight(new BigDecimal("3"));
        ErpProductRespVO secondProduct = new ErpProductRespVO()
                .setId(102L)
                .setName("P102")
                .setBackupPrice1(new BigDecimal("7"))
                .setWeight(new BigDecimal("4"));
        ErpWarehouseDO firstWarehouse = new ErpWarehouseDO().setId(201L).setName("W201");
        ErpWarehouseDO secondWarehouse = new ErpWarehouseDO().setId(202L).setName("W202");

        when(stockService.getStockPage(reqVO)).thenReturn(
                new PageResult<>(Arrays.asList(firstStock, secondStock), 2L));
        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        productMap.put(101L, firstProduct);
        productMap.put(102L, secondProduct);
        when(productService.getProductVOMap(any())).thenReturn(productMap);
        Map<Long, ErpWarehouseDO> warehouseMap = new HashMap<>();
        warehouseMap.put(201L, firstWarehouse);
        warehouseMap.put(202L, secondWarehouse);
        when(warehouseService.getWarehouseMap(any())).thenReturn(warehouseMap);
        Map<String, BigDecimal> occupiedMap = new HashMap<>();
        occupiedMap.put("101_201", BigDecimal.ONE);
        occupiedMap.put("102_202", new BigDecimal("2"));
        when(stockMapper.selectOccupiedCountMap(any(), any())).thenReturn(occupiedMap);
        Map<String, BigDecimal> pendingInMap = new HashMap<>();
        pendingInMap.put("101_201", new BigDecimal("4"));
        pendingInMap.put("102_202", new BigDecimal("5"));
        when(stockMapper.selectPendingInCountMap(any(), any())).thenReturn(pendingInMap);
        Map<String, BigDecimal> inTransitMap = new HashMap<>();
        inTransitMap.put("101_201", new BigDecimal("6"));
        inTransitMap.put("102_202", new BigDecimal("7"));
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any())).thenReturn(inTransitMap);

        ErpStockSummaryRespVO summary = controller.getStockSummary(reqVO).getData();

        assertEquals(new BigDecimal("5"), summary.getTotalStockCount());
        assertEquals(new BigDecimal("65"), summary.getTotalCostAmount());
        assertEquals(new BigDecimal("31"), summary.getTotalCurrentPriceAmount());
        assertEquals(new BigDecimal("9"), summary.getTotalPendingInCount());
        assertEquals(new BigDecimal("3"), summary.getTotalOccupiedCount());
        assertEquals(new BigDecimal("13"), summary.getTotalInTransitCount());
        assertEquals(new BigDecimal("18"), summary.getTotalWeight());
        assertEquals(2L, summary.getTotalRows());
    }

    @Test
    public void testGetInTransitDetails_usesApprovedPurchaseOrderDetailsAndFillsCreatorName() {
        ErpStockInTransitDetailRespVO detail = new ErpStockInTransitDetailRespVO();
        detail.setOrderId(100L);
        detail.setItemId(1001L);
        detail.setNo("PO-001");
        detail.setProductId(15L);
        detail.setProductCode("P001");
        detail.setProductName("Filter");
        detail.setWarehouseId(25L);
        detail.setWarehouseName("Main Warehouse");
        detail.setCount(new BigDecimal("3"));
        detail.setVehicleModel("Model-A");
        detail.setOriginPlace("Shanghai");
        detail.setDrawingNo("D001");
        detail.setStandard("STD");
        detail.setCreateTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        detail.setCreator("7");
        AdminUserRespDTO creator = new AdminUserRespDTO();
        creator.setId(7L);
        creator.setNickname("Creator");

        when(purchaseOrderItemMapper.selectInTransitDetails(eq(15L), eq(25L),
                eq(Collections.singletonList(ErpAuditStatus.APPROVE.getStatus())), eq("B-1"), eq(false)))
                .thenReturn(Collections.singletonList(detail));
        when(adminUserApi.getUserMap(eq(Collections.singleton(7L))))
                .thenReturn(Collections.singletonMap(7L, creator));

        CommonResult<List<ErpStockInTransitDetailRespVO>> result =
                controller.getInTransitDetails(15L, 25L, "B-1", false);

        verify(warehouseService).validateCurrentUserStockWarehousePermission(Collections.singleton(25L));
        verify(purchaseOrderItemMapper).selectInTransitDetails(15L, 25L,
                Collections.singletonList(ErpAuditStatus.APPROVE.getStatus()), "B-1", false);
        ErpStockInTransitDetailRespVO row = result.getData().get(0);
        assertEquals("PO-001", row.getNo());
        assertEquals("P001", row.getProductCode());
        assertEquals("Filter", row.getProductName());
        assertEquals("Main Warehouse", row.getWarehouseName());
        assertEquals(new BigDecimal("3"), row.getCount());
        assertEquals("Model-A", row.getVehicleModel());
        assertEquals("Shanghai", row.getOriginPlace());
        assertEquals("D001", row.getDrawingNo());
        assertEquals("STD", row.getStandard());
        assertEquals(LocalDateTime.of(2026, 7, 13, 10, 0), row.getCreateTime());
        assertEquals("Creator", row.getCreatorName());
    }

    @Test
    public void testGetPendingInDetails_usesPurchaseAndTransferInProcessDetailsAndFillsCreatorName() {
        ErpStockPendingInDetailRespVO detail = new ErpStockPendingInDetailRespVO();
        detail.setInId(200L);
        detail.setItemId(2001L);
        detail.setNo("PI-001");
        detail.setProductId(16L);
        detail.setProductCode("P002");
        detail.setProductName("Brake Pad");
        detail.setWarehouseId(26L);
        detail.setWarehouseName("Pending Warehouse");
        detail.setCount(new BigDecimal("4"));
        detail.setVehicleModel("Model-B");
        detail.setOriginPlace("Chengdu");
        detail.setDrawingNo("D002");
        detail.setStandard("STD-B");
        detail.setCreateTime(LocalDateTime.of(2026, 7, 13, 11, 0));
        detail.setCreator("8");
        AdminUserRespDTO creator = new AdminUserRespDTO();
        creator.setId(8L);
        creator.setNickname("Pending Creator");

        when(stockPendingInDetailMapper.selectList(16L, 26L, 10, 1, 20, null, true))
                .thenReturn(Collections.singletonList(detail));
        when(adminUserApi.getUserMap(eq(Collections.singleton(8L))))
                .thenReturn(Collections.singletonMap(8L, creator));

        CommonResult<List<ErpStockPendingInDetailRespVO>> result =
                controller.getPendingInDetails(16L, 26L, null, true);

        verify(warehouseService).validateCurrentUserStockWarehousePermission(Collections.singleton(26L));
        verify(stockPendingInDetailMapper).selectList(16L, 26L, 10, 1, 20, null, true);
        ErpStockPendingInDetailRespVO row = result.getData().get(0);
        assertEquals("PI-001", row.getNo());
        assertEquals("P002", row.getProductCode());
        assertEquals("Brake Pad", row.getProductName());
        assertEquals("Pending Warehouse", row.getWarehouseName());
        assertEquals(new BigDecimal("4"), row.getCount());
        assertEquals("Model-B", row.getVehicleModel());
        assertEquals("Chengdu", row.getOriginPlace());
        assertEquals("D002", row.getDrawingNo());
        assertEquals("STD-B", row.getStandard());
        assertEquals(LocalDateTime.of(2026, 7, 13, 11, 0), row.getCreateTime());
        assertEquals("Pending Creator", row.getCreatorName());
    }

    @Test
    public void testGetOccupiedDetails_usesAllOccupiedSourcesAndFillsCreatorName() {
        ErpStockOccupiedDetailRespVO detail = new ErpStockOccupiedDetailRespVO();
        detail.setCartId(300L);
        detail.setItemId(3001L);
        detail.setNo("SC-001");
        detail.setProductId(17L);
        detail.setProductCode("P003");
        detail.setProductName("Spark Plug");
        detail.setWarehouseId(27L);
        detail.setWarehouseName("Occupied Warehouse");
        detail.setCount(new BigDecimal("5"));
        detail.setVehicleModel("Model-C");
        detail.setOriginPlace("Guangzhou");
        detail.setDrawingNo("D003");
        detail.setStandard("STD-C");
        detail.setCreateTime(LocalDateTime.of(2026, 7, 13, 12, 0));
        detail.setCreator("9");
        AdminUserRespDTO creator = new AdminUserRespDTO();
        creator.setId(9L);
        creator.setNickname("Occupied Creator");

        when(stockOccupiedDetailMapper.selectList(17L, 27L, 10, Arrays.asList(10, 20, 30), 1, 10,
                "B-2", false))
                .thenReturn(Collections.singletonList(detail));
        when(adminUserApi.getUserMap(eq(Collections.singleton(9L))))
                .thenReturn(Collections.singletonMap(9L, creator));

        CommonResult<List<ErpStockOccupiedDetailRespVO>> result =
                controller.getOccupiedDetails(17L, 27L, "B-2", false);

        verify(warehouseService).validateCurrentUserStockWarehousePermission(Collections.singleton(27L));
        verify(stockOccupiedDetailMapper).selectList(17L, 27L, 10, Arrays.asList(10, 20, 30), 1, 10,
                "B-2", false);
        ErpStockOccupiedDetailRespVO row = result.getData().get(0);
        assertEquals("SC-001", row.getNo());
        assertEquals("P003", row.getProductCode());
        assertEquals("Spark Plug", row.getProductName());
        assertEquals("Occupied Warehouse", row.getWarehouseName());
        assertEquals(new BigDecimal("5"), row.getCount());
        assertEquals("Model-C", row.getVehicleModel());
        assertEquals("Guangzhou", row.getOriginPlace());
        assertEquals("D003", row.getDrawingNo());
        assertEquals("STD-C", row.getStandard());
        assertEquals(LocalDateTime.of(2026, 7, 13, 12, 0), row.getCreateTime());
        assertEquals("Occupied Creator", row.getCreatorName());
    }

    @Test
    public void testGetStockPage_showBatchNo_splitsRowsAndKeepsUnassignedResidual() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setShowBatchNo(true);
        reqVO.setKeyword("PC202607");
        reqVO.setPageNo(1);
        reqVO.setPageSize(2);
        ErpStockDO stock = new ErpStockDO()
                .setId(61L)
                .setProductId(161L)
                .setWarehouseId(261L)
                .setCount(new BigDecimal("30"))
                .setCostPrice(new BigDecimal("2"))
                .setCostAmount(new BigDecimal("60"));
        ErpProductRespVO product = new ErpProductRespVO()
                .setId(161L)
                .setName("测试")
                .setBatchNoEnabled(true)
                .setBackupPrice1(new BigDecimal("4"))
                .setWeight(BigDecimal.ONE);
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(261L).setName("蛟龙港仓");
        List<ErpStockBatchNoRespVO> balances = Arrays.asList(
                new ErpStockBatchNoRespVO().setBatchNo("PC20260701")
                        .setAvailableCount(new BigDecimal("10")),
                new ErpStockBatchNoRespVO().setBatchNo("PC20260715")
                        .setAvailableCount(new BigDecimal("15")));

        when(stockService.getStockPage(argThat(
                request -> Boolean.TRUE.equals(request.getShowBatchNo())
                        && "PC202607".equals(request.getKeyword()))))
                .thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(stockService.getStockBatchBalanceListMap(any()))
                .thenReturn(Collections.singletonMap("161_261", balances));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(161L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(261L, warehouse));
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(true);
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.emptyMap());
        when(stockBatchQuantityMapper.selectOccupiedList(any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(new ErpStockBatchQuantityDO()
                        .setProductId(161L).setWarehouseId(261L).setBatchNo("PC20260701")
                        .setCount(new BigDecimal("2"))));
        when(stockBatchQuantityMapper.selectPendingInList(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(new ErpStockBatchQuantityDO()
                        .setProductId(161L).setWarehouseId(261L).setBatchNo("PC20260701")
                        .setCount(new BigDecimal("3"))));
        when(stockBatchQuantityMapper.selectInTransitList(any(), any(), any()))
                .thenReturn(Collections.singletonList(new ErpStockBatchQuantityDO()
                        .setProductId(161L).setWarehouseId(261L).setBatchNo("PC20260701")
                        .setCount(new BigDecimal("4"))));

        PageResult<ErpStockRespVO> firstPage = controller.getStockPage(reqVO).getData();

        assertEquals(3L, firstPage.getTotal());
        assertEquals(2, firstPage.getList().size());
        assertEquals("PC20260701", firstPage.getList().get(0).getBatchNo());
        assertEquals(new BigDecimal("10"), firstPage.getList().get(0).getCount());
        assertEquals(new BigDecimal("20"), firstPage.getList().get(0).getCostAmount());
        assertEquals(new BigDecimal("2"), firstPage.getList().get(0).getOccupiedCount());
        assertEquals(new BigDecimal("8"), firstPage.getList().get(0).getAvailableCount());
        assertEquals(new BigDecimal("3"), firstPage.getList().get(0).getPendingInCount());
        assertEquals(new BigDecimal("4"), firstPage.getList().get(0).getInTransitCount());
        assertTrue(firstPage.getList().get(0).getBatchRow());
        assertEquals("PC20260715", firstPage.getList().get(1).getBatchNo());

        reqVO.setPageNo(2);
        PageResult<ErpStockRespVO> secondPage = controller.getStockPage(reqVO).getData();
        assertEquals(1, secondPage.getList().size());
        assertNull(secondPage.getList().get(0).getBatchNo());
        assertEquals(new BigDecimal("5"), secondPage.getList().get(0).getCount());
        assertTrue(secondPage.getList().get(0).getRowKey().contains("__UNASSIGNED__"));

        reqVO.setPageNo(1);
        reqVO.setCountMin(new BigDecimal("12"));
        PageResult<ErpStockRespVO> filteredPage = controller.getStockPage(reqVO).getData();
        assertEquals(1L, filteredPage.getTotal());
        assertEquals("PC20260715", filteredPage.getList().get(0).getBatchNo());
    }

    @Test
    public void testGetStockPage_showBatchNo_keepsSoldOutNamedBatchVisible() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setShowBatchNo(true);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        ErpStockDO stock = new ErpStockDO()
                .setId(63L)
                .setProductId(163L)
                .setWarehouseId(263L)
                .setCount(BigDecimal.ZERO)
                .setCostPrice(new BigDecimal("2"))
                .setCostAmount(BigDecimal.ZERO);
        ErpProductRespVO product = new ErpProductRespVO()
                .setId(163L)
                .setName("测试")
                .setBatchNoEnabled(true);
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(263L).setName("蛟龙港仓");
        ErpStockBatchNoRespVO soldOutBatch = new ErpStockBatchNoRespVO()
                .setBatchNo("PC20260720")
                .setAvailableCount(BigDecimal.ZERO)
                .setFirstInTime(LocalDateTime.of(2026, 7, 20, 9, 0));

        when(stockService.getStockPage(any(ErpStockPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(stockService.getStockBatchBalanceListMap(any()))
                .thenReturn(Collections.singletonMap("163_263", Collections.singletonList(soldOutBatch)));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(163L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(263L, warehouse));
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.emptyMap());

        PageResult<ErpStockRespVO> result = controller.getStockPage(reqVO).getData();

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        ErpStockRespVO row = result.getList().get(0);
        assertEquals("PC20260720", row.getBatchNo());
        assertEquals(BigDecimal.ZERO, row.getCount());
        assertEquals(LocalDateTime.of(2026, 7, 20, 9, 0), row.getFirstInTime());
        assertTrue(row.getBatchRow());
    }

    @Test
    public void testGetStockSummary_showBatchNo_doesNotDuplicateWarehouseAggregates() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setShowBatchNo(true);
        ErpStockDO stock = new ErpStockDO()
                .setId(62L)
                .setProductId(162L)
                .setWarehouseId(262L)
                .setCount(new BigDecimal("30"))
                .setCostPrice(new BigDecimal("2"))
                .setCostAmount(new BigDecimal("60"));
        ErpProductRespVO product = new ErpProductRespVO()
                .setId(162L)
                .setName("测试")
                .setBatchNoEnabled(true)
                .setBackupPrice1(new BigDecimal("4"))
                .setWeight(BigDecimal.ONE);
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(262L).setName("蛟龙港仓");
        List<ErpStockBatchNoRespVO> balances = Arrays.asList(
                new ErpStockBatchNoRespVO().setBatchNo("B1").setAvailableCount(new BigDecimal("10")),
                new ErpStockBatchNoRespVO().setBatchNo("B2").setAvailableCount(new BigDecimal("20")));
        when(stockService.getStockPage(any(ErpStockPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(stock), 1L));
        when(stockService.getStockBatchBalanceListMap(any()))
                .thenReturn(Collections.singletonMap("162_262", balances));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(162L, product));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(262L, warehouse));
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(true);
        when(stockMapper.selectOccupiedCountMap(any(), any()))
                .thenReturn(Collections.singletonMap("162_262", new BigDecimal("3")));
        when(stockMapper.selectPendingInCountMap(any(), any()))
                .thenReturn(Collections.singletonMap("162_262", new BigDecimal("4")));
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any()))
                .thenReturn(Collections.singletonMap("162_262", new BigDecimal("5")));
        when(stockBatchQuantityMapper.selectOccupiedList(any(), any(), any(), any(), any(), any()))
                .thenReturn(Arrays.asList(
                        new ErpStockBatchQuantityDO().setProductId(162L).setWarehouseId(262L)
                                .setBatchNo("B1").setCount(BigDecimal.ONE),
                        new ErpStockBatchQuantityDO().setProductId(162L).setWarehouseId(262L)
                                .setBatchNo("B2").setCount(new BigDecimal("2"))));
        when(stockBatchQuantityMapper.selectPendingInList(any(), any(), any(), any(), any()))
                .thenReturn(Arrays.asList(
                        new ErpStockBatchQuantityDO().setProductId(162L).setWarehouseId(262L)
                                .setBatchNo("B1").setCount(BigDecimal.ONE),
                        new ErpStockBatchQuantityDO().setProductId(162L).setWarehouseId(262L)
                                .setBatchNo("B2").setCount(new BigDecimal("3"))));
        when(stockBatchQuantityMapper.selectInTransitList(any(), any(), any()))
                .thenReturn(Arrays.asList(
                        new ErpStockBatchQuantityDO().setProductId(162L).setWarehouseId(262L)
                                .setBatchNo("B1").setCount(new BigDecimal("2")),
                        new ErpStockBatchQuantityDO().setProductId(162L).setWarehouseId(262L)
                                .setBatchNo("B2").setCount(new BigDecimal("3"))));

        ErpStockSummaryRespVO summary = controller.getStockSummary(reqVO).getData();

        assertEquals(new BigDecimal("30"), summary.getTotalStockCount());
        assertEquals(new BigDecimal("60"), summary.getTotalCostAmount());
        assertEquals(new BigDecimal("120"), summary.getTotalCurrentPriceAmount());
        assertEquals(new BigDecimal("3"), summary.getTotalOccupiedCount());
        assertEquals(new BigDecimal("4"), summary.getTotalPendingInCount());
        assertEquals(new BigDecimal("5"), summary.getTotalInTransitCount());
        assertEquals(2L, summary.getTotalRows());
    }

}
