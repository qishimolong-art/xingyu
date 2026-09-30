package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockLockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErpSaleCartFulfillmentGuardTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleCartServiceImpl service;
    @Mock private ErpSaleCartMapper saleCartMapper;
    @Mock private ErpSalePickDeliveryService salePickDeliveryService;
    @Mock private ErpSaleDirectDeptPermissionService saleDirectDeptPermissionService;
    @Mock private ErpStockMoveService stockMoveService;
    @Mock private ErpStockLockService stockLockService;

    @Test
    void manualFinalApprove_withFulfillmentOrder_isForbidden() {
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(1L).setNo("C1").setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(saleCartMapper.selectByIdForUpdate(1L)).thenReturn(cart);
        when(salePickDeliveryService.getSummaryBySaleCartId(1L)).thenReturn(summary(30));

        assertServiceException(() -> service.finalApproveSaleCart(1L),
                SALE_CART_FULFILLMENT_MANUAL_FINAL_FORBIDDEN);

        verify(saleCartMapper).selectByIdForUpdate(1L);
        verify(saleCartMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void cancelFirstApprove_withFulfillmentOrder_isForbidden() {
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(2L).setNo("C2")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(saleCartMapper.selectByIdForUpdate(2L)).thenReturn(cart);
        when(salePickDeliveryService.getSummaryBySaleCartId(2L)).thenReturn(summary(10));

        assertServiceException(() -> service.cancelFirstApproveSaleCart(2L),
                SALE_CART_FULFILLMENT_ROLLBACK_FORBIDDEN);

        verifyNoInteractions(stockMoveService, stockLockService);
        verify(saleCartMapper, never()).cancelFirstApprove(any());
    }

    @Test
    void autoFinalApproveAfterDelivery_beforeDeliveryDone_isRejected() {
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(3L).setNo("C3").setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(saleCartMapper.selectByIdForUpdate(3L)).thenReturn(cart);
        when(salePickDeliveryService.getSummaryBySaleCartId(3L)).thenReturn(summary(20));

        ServiceException error = org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
                () -> service.autoFinalApproveAfterDelivery(3L, 88L));
        org.junit.jupiter.api.Assertions.assertEquals(SALE_CART_FULFILLMENT_NOT_COMPLETED.getCode(), error.getCode());
        verify(saleCartMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    private ErpSalePickDeliverySummaryRespVO summary(Integer deliveryStatus) {
        ErpSalePickDeliverySummaryRespVO summary = new ErpSalePickDeliverySummaryRespVO();
        summary.setOrderId(100L);
        summary.setDeliveryStatus(deliveryStatus);
        return summary;
    }

}
