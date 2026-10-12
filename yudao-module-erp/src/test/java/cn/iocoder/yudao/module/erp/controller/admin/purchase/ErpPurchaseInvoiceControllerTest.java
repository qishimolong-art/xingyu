package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseInvoiceControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInvoiceController controller;

    @Mock
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    void getPurchaseInvoiceReturnsDistinctSourceRefsWithoutLoadingAllItems() {
        when(purchaseInvoiceService.getPurchaseInvoice(1L))
                .thenReturn(new ErpPurchaseInvoiceDO().setId(1L));
        when(purchaseInvoiceService.getPurchaseInvoiceSourceRefList(1L)).thenReturn(List.of(
                new ErpPurchaseInvoiceItemDO().setSourceType(10).setSourceDocId(101L),
                new ErpPurchaseInvoiceItemDO().setSourceType(20).setSourceDocId(202L),
                new ErpPurchaseInvoiceItemDO().setSourceInId(303L)));
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());

        ErpPurchaseInvoiceRespVO result = controller.getPurchaseInvoice(1L, false, false).getData();

        assertEquals(List.of(10, 20, 10), result.getSourceRefs().stream()
                .map(ErpPurchaseInvoiceRespVO.SourceRef::getSourceType).toList());
        assertEquals(List.of(101L, 202L, 303L), result.getSourceRefs().stream()
                .map(ErpPurchaseInvoiceRespVO.SourceRef::getSourceDocId).toList());
        verify(purchaseInvoiceService, never()).getPurchaseInvoiceItemListByInvoiceId(1L);
    }

    @Test
    void getSupplierAvailableDeptSimpleListDelegatesToPurchaseInvoiceService() {
        List<DeptSimpleRespVO> expected = Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L));
        when(purchaseInvoiceService.getSupplierAvailableDeptSimpleList(100L)).thenReturn(expected);

        assertEquals(expected, controller.getSupplierAvailableDeptSimpleList(100L).getData());
        verify(purchaseInvoiceService).getSupplierAvailableDeptSimpleList(100L);
    }

    @Test
    void getSupplierAvailableDeptSimplePageDelegatesToPurchaseInvoiceService() {
        PageParam pageParam = new PageParam().setPageNo(1).setPageSize(20);
        PageResult<DeptSimpleRespVO> expected = new PageResult<>(Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L)), 1L);
        when(purchaseInvoiceService.getSupplierAvailableDeptSimplePage(100L, pageParam)).thenReturn(expected);

        assertEquals(expected, controller.getSupplierAvailableDeptSimplePage(100L, pageParam).getData());
        verify(purchaseInvoiceService).getSupplierAvailableDeptSimplePage(100L, pageParam);
    }

}
