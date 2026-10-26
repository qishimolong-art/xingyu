package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickSourceDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInPickPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInService;
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

class ErpStockInControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockInController controller;

    @Mock
    private ErpStockInService stockInService;
    @Mock
    private ErpSalePickDeliveryService pickDeliveryService;

    @Test
    void draftCreationUsesExistingCreatePermissionAndDelegatesWithoutConfirming() throws Exception {
        cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO req =
                new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO();
        when(stockInService.createStockInDraft(req)).thenReturn(9L);
        assertEquals(9L, controller.createStockInDraft(req).getData());
        Method method = ErpStockInController.class.getMethod("createStockInDraft", req.getClass());
        assertEquals("@ss.hasPermission('erp:stock-in:create')", method.getAnnotation(PreAuthorize.class).value());
        verify(stockInService).createStockInDraft(req);
        verify(stockInService, never()).createStockIn(req);
    }

    @Test
    void pickEndpoints_useStockInSourceAndQueryPermission() throws Exception {
        ErpStockInDO stockIn = new ErpStockInDO().setId(33L);
        ErpStockInPickPageReqVO pageReqVO = new ErpStockInPickPageReqVO();
        pageReqVO.setInId(33L);
        when(stockInService.getStockIn(33L)).thenReturn(stockIn);
        when(pickDeliveryService.getSourcePickDetail(90, 33L))
                .thenReturn(new ErpSalePickSourceDetailRespVO());
        when(pickDeliveryService.getSourcePickItemPage(90, 33L, pageReqVO))
                .thenReturn(new PageResult<>(Collections.<ErpSalePickDeliveryItemRespVO>emptyList(), 0L));
        when(pickDeliveryService.getSourcePickOperationItemPage(90, 33L, pageReqVO))
                .thenReturn(new PageResult<>(
                        Collections.<ErpSaleOutPickDeliveryOperationItemRespVO>emptyList(), 0L));

        assertNotNull(controller.getStockInPickDetail(33L).getData());
        assertEquals(0L, controller.getStockInPickItemPage(pageReqVO).getData().getTotal());
        assertEquals(0L, controller.getStockInPickOperationItemPage(pageReqVO).getData().getTotal());

        verify(pickDeliveryService).getSourcePickDetail(90, 33L);
        verify(pickDeliveryService).getSourcePickItemPage(90, 33L, pageReqVO);
        verify(pickDeliveryService).getSourcePickOperationItemPage(90, 33L, pageReqVO);

        for (String methodName : List.of("getStockInPickDetail", "getStockInPickItemPage",
                "getStockInPickOperationItemPage")) {
            Method method = methodName.equals("getStockInPickDetail")
                    ? ErpStockInController.class.getMethod(methodName, Long.class)
                    : ErpStockInController.class.getMethod(methodName, ErpStockInPickPageReqVO.class);
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertNotNull(annotation);
            assertTrue(annotation.value().contains("erp:stock-in:query"));
        }
    }

    @Test
    void pickDetailEndpoint_rejectsMissingStockInBeforeReadingFulfillment() {
        when(stockInService.getStockIn(404L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> controller.getStockInPickDetail(404L));

        verify(pickDeliveryService, never()).getSourcePickDetail(any(), any());
    }

}
