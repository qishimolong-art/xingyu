package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockTransferOutControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockTransferOutController controller;

    @Mock
    private ErpSaleCartService saleCartService;
    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpStockMoveController stockMoveController;

    @Test
    void unlockSaleCart_passesTransferOutIdToDedicatedService() {
        CommonResult<Boolean> result = controller.unlockSaleCart(301L);

        assertTrue(result.getData());
        verify(stockMoveService).validateStockTransferOutVisible(301L);
        verify(saleCartService).unlockSaleCartByTransferOutId(301L);
    }

    @Test
    void unlockSaleCart_usesIndependentPermission() throws Exception {
        Method method = ErpStockTransferOutController.class.getMethod("unlockSaleCart", Long.class);

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertEquals("@ss.hasPermission('erp:stock-transfer-out:unlock-cart')", annotation.value());
    }

    @Test
    void approveStockTransferOut_usesDedicatedPermissionAwareService() {
        CommonResult<Boolean> result = controller.approveStockTransferOut(302L);

        assertTrue(result.getData());
        verify(stockMoveService).updateStockTransferOutStatus(302L, ErpAuditStatus.APPROVE.getStatus());
    }

    @Test
    void getStockTransferOutPage_reusesOnePermissionScopeForQueryAndRowFlags() {
        ErpStockMovePageReqVO request = new ErpStockMovePageReqVO();
        ErpStockTransferOutPermissionScope scope = new ErpStockTransferOutPermissionScope(
                false, Collections.singleton(20L));
        PageResult<ErpStockMoveDO> dataPage = new PageResult<>(Collections.emptyList(), 0L);
        PageResult<ErpStockMoveRespVO> responsePage = new PageResult<>(Collections.emptyList(), 0L);
        when(stockMoveService.getTransferOutPermissionScope()).thenReturn(scope);
        when(stockMoveService.getVisibleStockTransferOutPage(request, scope)).thenReturn(dataPage);
        when(stockMoveController.buildStockMoveVOPageResult(
                dataPage, "erp_stock_transfer_out", scope)).thenReturn(responsePage);

        CommonResult<PageResult<ErpStockMoveRespVO>> result = controller.getStockTransferOutPage(request);

        assertSame(responsePage, result.getData());
        verify(stockMoveService).getTransferOutPermissionScope();
        verify(stockMoveService).getVisibleStockTransferOutPage(request, scope);
        verify(stockMoveController).buildStockMoveVOPageResult(dataPage, "erp_stock_transfer_out", scope);
    }

}
