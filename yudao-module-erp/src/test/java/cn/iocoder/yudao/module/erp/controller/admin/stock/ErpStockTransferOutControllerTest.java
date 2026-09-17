package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
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
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

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
    void createStockTransferOutDraft_delegatesWithoutFormalValidation() {
        ErpStockTransferOutDraftCreateReqVO request = new ErpStockTransferOutDraftCreateReqVO();
        when(stockMoveService.createStockTransferOutDraft(request)).thenReturn(303L);

        CommonResult<Long> result = controller.createStockTransferOutDraft(request);

        assertEquals(303L, result.getData());
        assertEquals(10, request.getTransferDirection());
        verify(stockMoveService).createStockTransferOutDraft(request);
    }

    @Test
    void updateAndSubmitStockTransferOutDraft_delegatesCurrentEdit() {
        ErpStockMoveSaveReqVO request = new ErpStockMoveSaveReqVO();

        CommonResult<Boolean> result = controller.updateAndSubmitStockTransferOutDraft(request);

        assertTrue(result.getData());
        assertEquals(10, request.getTransferDirection());
        verify(stockMoveService).updateAndSubmitStockTransferOutDraft(request);
    }

    @Test
    void submitStockTransferOutDraft_delegatesAndUsesUpdatePermission() throws Exception {
        CommonResult<Boolean> result = controller.submitStockTransferOutDraft(304L);

        assertTrue(result.getData());
        verify(stockMoveService).submitStockTransferOutDraft(304L);
        Method method = ErpStockTransferOutController.class
                .getMethod("submitStockTransferOutDraft", Long.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('erp:stock-transfer-out:update')", annotation.value());
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
                dataPage, "erp_stock_transfer_out", scope, null)).thenReturn(responsePage);

        CommonResult<PageResult<ErpStockMoveRespVO>> result = controller.getStockTransferOutPage(request);

        assertSame(responsePage, result.getData());
        verify(stockMoveService).getTransferOutPermissionScope();
        verify(stockMoveService).getVisibleStockTransferOutPage(request, scope);
        verify(stockMoveController).buildStockMoveVOPageResult(dataPage, "erp_stock_transfer_out", scope, null);
    }

    @Test
    void getStockTransferOutExportFields_includesDirectCustomerName() {
        when(fieldPermissionMasker.getHiddenFieldSet("erp_stock_transfer_out")).thenReturn(Collections.emptySet());

        CommonResult<List<ErpExportFieldRespVO>> result = controller.getStockTransferOutExportFields();

        assertTrue(result.getData().stream().anyMatch(field ->
                "directCustomerName".equals(field.getField()) && "直发客户".equals(field.getLabel())));
    }

    @Test
    void getStockTransferOutItemPage_usesVisibleCheckBeforeSkippingGenericMoveValidation() {
        ErpStockMoveItemPageReqVO request = new ErpStockMoveItemPageReqVO();
        request.setMoveId(401L);
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setId(401L).setTransferDirection(10);
        PageResult<ErpStockMoveRespVO.Item> responsePage = new PageResult<>(Collections.emptyList(), 0L);
        CommonResult<PageResult<ErpStockMoveRespVO.Item>> expected = CommonResult.success(responsePage);
        when(stockMoveService.getVisibleStockTransferOut(401L)).thenReturn(stockMove);
        when(stockMoveController.getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_out"))
                .thenReturn(expected);

        CommonResult<PageResult<ErpStockMoveRespVO.Item>> result = controller.getStockTransferOutItemPage(request);

        assertSame(expected, result);
        verify(stockMoveService).getVisibleStockTransferOut(401L);
        verify(stockMoveController).getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_out");
        verify(stockMoveController, never()).getStockMoveItemPage(request, "erp_stock_transfer_out");
    }

    @Test
    void getStockTransferOutItemPage_outOfScope_returnsNotExists() {
        ErpStockMoveItemPageReqVO request = new ErpStockMoveItemPageReqVO();
        request.setMoveId(402L);
        when(stockMoveService.getVisibleStockTransferOut(402L)).thenReturn(null);

        assertServiceException(() -> controller.getStockTransferOutItemPage(request), STOCK_MOVE_NOT_EXISTS);

        verify(stockMoveController, never()).getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_out");
    }

    @Test
    void getStockTransferOutItemPage_wrongDirection_returnsNotExists() {
        ErpStockMoveItemPageReqVO request = new ErpStockMoveItemPageReqVO();
        request.setMoveId(403L);
        ErpStockMoveDO transferIn = new ErpStockMoveDO().setId(403L).setTransferDirection(20);
        when(stockMoveService.getVisibleStockTransferOut(403L)).thenReturn(transferIn);

        assertServiceException(() -> controller.getStockTransferOutItemPage(request), STOCK_MOVE_NOT_EXISTS);

        verify(stockMoveController, never()).getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_out");
    }

}
