package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseInControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInController controller;

    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockInBillService stockInBillService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    private final List<ErpPurchaseInItemDO> items = Arrays.asList(
            new ErpPurchaseInItemDO().setId(101L).setInId(10L).setProductId(1001L)
                    .setWarehouseId(1L).setCount(new BigDecimal("4")),
            new ErpPurchaseInItemDO().setId(102L).setInId(10L).setProductId(1002L)
                    .setWarehouseId(1L).setCount(new BigDecimal("6")));

    @BeforeEach
    void setUp() {
        when(purchaseInService.getPurchaseIn(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK-001").setTotalCount(new BigDecimal("10")));
        when(purchaseInService.getPurchaseInItemListByInId(eq(10L))).thenReturn(items);
        when(purchaseInvoiceService.getPurchaseInvoiceItemListBySourceInIds(any()))
                .thenReturn(Collections.emptyList());
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(purchaseInService.getApprovedReturnCountMapByInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        when(stockInBillService.getStockInBillListByPurchaseInId(eq(10L)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void getPurchaseInKeepsEmptyTransferOutStatusAtZero() {
        when(purchaseInService.getTransferOutCountMapByInItemIds(any()))
                .thenReturn(Collections.emptyMap());

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L).getData();

        assertNotNull(data);
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getTransferOutCount()));
        assertEquals(Integer.valueOf(0), data.getTransferOutStatus());
    }

    @Test
    void getSupplierAvailableDeptSimpleListDelegatesToPurchaseInService() {
        List<DeptSimpleRespVO> depts = Collections.singletonList(
                new DeptSimpleRespVO(20L, "采购二部", 0L));
        when(purchaseInService.getSupplierAvailableDeptSimpleList(eq(100L))).thenReturn(depts);

        controller.getPurchaseIn(10L);
        CommonResult<List<DeptSimpleRespVO>> result = controller.getSupplierAvailableDeptSimpleList(100L);

        assertSame(depts, result.getData());
        verify(purchaseInService).getSupplierAvailableDeptSimpleList(eq(100L));
    }

    @Test
    void getPurchaseInAggregatesPartialTransferOutStatus() {
        Map<Long, BigDecimal> movedCountMap = new HashMap<>();
        movedCountMap.put(101L, new BigDecimal("2"));
        movedCountMap.put(102L, new BigDecimal("3"));
        when(purchaseInService.getTransferOutCountMapByInItemIds(any())).thenReturn(movedCountMap);

        CommonResult<ErpPurchaseInRespVO> result = controller.getPurchaseIn(10L);

        ErpPurchaseInRespVO data = result.getData();
        assertNotNull(data);
        assertEquals(0, new BigDecimal("5").compareTo(data.getTransferOutCount()));
        assertEquals(Integer.valueOf(1), data.getTransferOutStatus());
        verify(purchaseInService).getTransferOutCountMapByInItemIds(any());
        verify(fieldPermissionMasker).mask(eq("erp_purchase_in"), eq(data));
    }

    @Test
    void getPurchaseInAggregatesFullTransferOutStatus() {
        Map<Long, BigDecimal> movedCountMap = new HashMap<>();
        movedCountMap.put(101L, new BigDecimal("4"));
        movedCountMap.put(102L, new BigDecimal("6"));
        when(purchaseInService.getTransferOutCountMapByInItemIds(any())).thenReturn(movedCountMap);

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("10").compareTo(data.getTransferOutCount()));
        assertEquals(Integer.valueOf(2), data.getTransferOutStatus());
    }

}
