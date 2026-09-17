package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockTransferInControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockTransferInController controller;

    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpStockMoveController stockMoveController;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Test
    void getStockTransferInPage_usesVisibleTransferInService() {
        ErpStockMovePageReqVO request = new ErpStockMovePageReqVO();
        PageResult<ErpStockMoveDO> dataPage = new PageResult<>(Collections.emptyList(), 0L);
        PageResult<ErpStockMoveRespVO> responsePage = new PageResult<>(Collections.emptyList(), 0L);
        when(stockMoveService.getVisibleStockTransferInPage(request)).thenReturn(dataPage);
        when(stockMoveController.buildStockMoveVOPageResult(dataPage, "erp_stock_transfer_in", null, null))
                .thenReturn(responsePage);

        CommonResult<PageResult<ErpStockMoveRespVO>> result = controller.getStockTransferInPage(request);

        assertSame(responsePage, result.getData());
        assertEquals(Integer.valueOf(20), request.getTransferDirection());
        verify(stockMoveService).getVisibleStockTransferInPage(request);
        verify(stockMoveService, never()).getStockMovePage(request);
    }

    @Test
    void exportStockTransferInExcel_usesVisibleTransferInService() throws Exception {
        ErpStockMovePageReqVO request = new ErpStockMovePageReqVO();
        PageResult<ErpStockMoveDO> dataPage = new PageResult<>(Collections.emptyList(), 0L);
        PageResult<ErpStockMoveRespVO> responsePage = new PageResult<>(Collections.emptyList(), 0L);
        when(stockMoveService.getVisibleStockTransferInPage(request)).thenReturn(dataPage);
        when(stockMoveController.buildStockMoveVOPageResult(dataPage, "erp_stock_transfer_in"))
                .thenReturn(responsePage);
        when(fieldPermissionMasker.getHiddenFieldSet("erp_stock_transfer_in"))
                .thenReturn(Collections.emptySet());
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.exportStockTransferInExcel(request, null, response);

        assertEquals(Integer.valueOf(20), request.getTransferDirection());
        assertEquals(1, request.getPageNo());
        assertEquals(5000, request.getPageSize());
        assertTrue(response.getContentAsByteArray().length > 0);
        verify(stockMoveService).getVisibleStockTransferInPage(request);
        verify(stockMoveService, never()).getStockMovePage(request);
    }

    @Test
    void getStockTransferIn_usesVisibleEntityWithoutGenericSecondQuery() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setId(301L).setTransferDirection(20);
        CommonResult<ErpStockMoveRespVO> expected = CommonResult.success(new ErpStockMoveRespVO().setId(301L));
        when(stockMoveService.getVisibleStockTransferIn(301L)).thenReturn(stockMove);
        when(stockMoveController.buildStockMoveDetail(stockMove, "erp_stock_transfer_in")).thenReturn(expected);

        CommonResult<ErpStockMoveRespVO> result = controller.getStockTransferIn(301L);

        assertSame(expected, result);
        verify(stockMoveService).getVisibleStockTransferIn(301L);
        verify(stockMoveService, never()).getStockMove(301L);
    }

    @Test
    void getStockTransferIn_outOfScope_returnsNotExists() {
        when(stockMoveService.getVisibleStockTransferIn(302L)).thenReturn(null);

        assertServiceException(() -> controller.getStockTransferIn(302L), STOCK_MOVE_NOT_EXISTS);

        verify(stockMoveController, never()).buildStockMoveDetail(null, "erp_stock_transfer_in");
    }

    @Test
    void getStockTransferIn_wrongDirection_returnsNotExists() {
        ErpStockMoveDO transferOut = new ErpStockMoveDO().setId(303L).setTransferDirection(10);
        when(stockMoveService.getVisibleStockTransferIn(303L)).thenReturn(transferOut);

        assertServiceException(() -> controller.getStockTransferIn(303L), STOCK_MOVE_NOT_EXISTS);

        verify(stockMoveController, never()).buildStockMoveDetail(transferOut, "erp_stock_transfer_in");
    }

    @Test
    void getStockTransferInItemPage_usesVisibleCheckBeforeSkippingGenericMoveValidation() {
        ErpStockMoveItemPageReqVO request = new ErpStockMoveItemPageReqVO();
        request.setMoveId(401L);
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setId(401L).setTransferDirection(20);
        PageResult<ErpStockMoveRespVO.Item> responsePage = new PageResult<>(Collections.emptyList(), 0L);
        CommonResult<PageResult<ErpStockMoveRespVO.Item>> expected = CommonResult.success(responsePage);
        when(stockMoveService.getVisibleStockTransferIn(401L)).thenReturn(stockMove);
        when(stockMoveController.getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_in"))
                .thenReturn(expected);

        CommonResult<PageResult<ErpStockMoveRespVO.Item>> result = controller.getStockTransferInItemPage(request);

        assertSame(expected, result);
        verify(stockMoveService).getVisibleStockTransferIn(401L);
        verify(stockMoveController).getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_in");
        verify(stockMoveController, never()).getStockMoveItemPage(request, "erp_stock_transfer_in");
    }

    @Test
    void getStockTransferInItemPage_outOfScope_returnsNotExists() {
        ErpStockMoveItemPageReqVO request = new ErpStockMoveItemPageReqVO();
        request.setMoveId(402L);
        when(stockMoveService.getVisibleStockTransferIn(402L)).thenReturn(null);

        assertServiceException(() -> controller.getStockTransferInItemPage(request), STOCK_MOVE_NOT_EXISTS);

        verify(stockMoveController, never()).getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_in");
    }

    @Test
    void getStockTransferInItemPage_wrongDirection_returnsNotExists() {
        ErpStockMoveItemPageReqVO request = new ErpStockMoveItemPageReqVO();
        request.setMoveId(403L);
        ErpStockMoveDO transferOut = new ErpStockMoveDO().setId(403L).setTransferDirection(10);
        when(stockMoveService.getVisibleStockTransferIn(403L)).thenReturn(transferOut);

        assertServiceException(() -> controller.getStockTransferInItemPage(request), STOCK_MOVE_NOT_EXISTS);

        verify(stockMoveController, never()).getStockMoveItemPageAfterVisibleCheck(request, "erp_stock_transfer_in");
    }

}
