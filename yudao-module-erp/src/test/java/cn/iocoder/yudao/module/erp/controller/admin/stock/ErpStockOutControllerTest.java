package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickSourceDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutPickPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockOutControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockOutController controller;

    @Mock
    private ErpStockOutService stockOutService;
    @Mock
    private ErpSalePickDeliveryService pickDeliveryService;

    @Test
    void draftCreationUsesExistingCreatePermissionAndDelegatesWithoutConfirming() throws Exception {
        cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO req =
                new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO();
        when(stockOutService.createStockOutDraft(req)).thenReturn(9L);
        assertEquals(9L, controller.createStockOutDraft(req).getData());
        Method method = ErpStockOutController.class.getMethod("createStockOutDraft", req.getClass());
        assertEquals("@ss.hasPermission('erp:stock-out:create')", method.getAnnotation(PreAuthorize.class).value());
        verify(stockOutService).createStockOutDraft(req);
        verify(stockOutService, never()).createStockOut(req);
    }

    @Test
    void pickEndpoints_useStockOutSourceAndQueryPermission() throws Exception {
        ErpStockOutDO stockOut = new ErpStockOutDO().setId(44L);
        ErpStockOutPickPageReqVO pageReqVO = new ErpStockOutPickPageReqVO();
        pageReqVO.setOutId(44L);
        when(stockOutService.getStockOut(44L)).thenReturn(stockOut);
        when(pickDeliveryService.getSourcePickDetail(100, 44L))
                .thenReturn(new ErpSalePickSourceDetailRespVO());
        when(pickDeliveryService.getSourcePickItemPage(100, 44L, pageReqVO))
                .thenReturn(new PageResult<>(Collections.<ErpSalePickDeliveryItemRespVO>emptyList(), 0L));
        when(pickDeliveryService.getSourcePickOperationItemPage(100, 44L, pageReqVO))
                .thenReturn(new PageResult<>(
                        Collections.<ErpSaleOutPickDeliveryOperationItemRespVO>emptyList(), 0L));

        assertNotNull(controller.getStockOutPickDetail(44L).getData());
        assertEquals(0L, controller.getStockOutPickItemPage(pageReqVO).getData().getTotal());
        assertEquals(0L, controller.getStockOutPickOperationItemPage(pageReqVO).getData().getTotal());

        verify(pickDeliveryService).getSourcePickDetail(100, 44L);
        verify(pickDeliveryService).getSourcePickItemPage(100, 44L, pageReqVO);
        verify(pickDeliveryService).getSourcePickOperationItemPage(100, 44L, pageReqVO);

        for (String methodName : List.of("getStockOutPickDetail", "getStockOutPickItemPage",
                "getStockOutPickOperationItemPage")) {
            Method method = methodName.equals("getStockOutPickDetail")
                    ? ErpStockOutController.class.getMethod(methodName, Long.class)
                    : ErpStockOutController.class.getMethod(methodName, ErpStockOutPickPageReqVO.class);
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertNotNull(annotation);
            assertTrue(annotation.value().contains("erp:stock-out:query"));
        }
    }

    @Test
    void pickDetailEndpoint_rejectsMissingStockOutBeforeReadingFulfillment() {
        when(stockOutService.getStockOut(404L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> controller.getStockOutPickDetail(404L));

        verify(pickDeliveryService, never()).getSourcePickDetail(any(), any());
    }

}
