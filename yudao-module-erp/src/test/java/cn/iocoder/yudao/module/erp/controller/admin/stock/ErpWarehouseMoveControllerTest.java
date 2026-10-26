package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickSourceDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePickPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class ErpWarehouseMoveControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpWarehouseMoveController controller;

    @Mock
    private ErpWarehouseMoveService warehouseMoveService;
    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private ErpStockImportService stockImportService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpStockItemPriceReferenceFiller itemPriceReferenceFiller;
    @Mock
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpWarehouseMoveItemMapper warehouseMoveItemMapper;
    @Mock
    private ErpSalePickDeliveryService salePickDeliveryService;

    @Test
    void getWarehouseMovePage_masksLegacyListAmountColumns() {
        ErpWarehouseMovePageReqVO reqVO = new ErpWarehouseMovePageReqVO();
        reqVO.setIncludeItems(false);
        ErpWarehouseMoveDO move = new ErpWarehouseMoveDO()
                .setId(20L)
                .setTotalCostAmount(new BigDecimal("80.00"))
                .setTotalPrice(new BigDecimal("100.00"));
        when(warehouseMoveService.getWarehouseMovePage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(move), 1L));
        when(warehouseMoveItemMapper.selectSummaryMapByMoveIds(anyCollection())).thenReturn(Collections.emptyMap());
        when(salePickDeliveryService.getSummaryMapByWarehouseMoveIds(anyCollection()))
                .thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpWarehouseMoveRespVO>> result = controller.getWarehouseMovePage(reqVO);

        verify(fieldPermissionMasker).maskListColumns(eq("erp_warehouse_move"), eq(result.getData().getList()));
    }

    @Test
    void getWarehouseMoveSummary_masksLegacySummaryAmountColumns() {
        ErpWarehouseMovePageReqVO reqVO = new ErpWarehouseMovePageReqVO();
        ErpWarehouseMoveSummaryRespVO summary = new ErpWarehouseMoveSummaryRespVO();
        summary.setTotalCostAmount(new BigDecimal("80.00"));
        summary.setTotalPrice(new BigDecimal("100.00"));
        when(warehouseMoveService.getWarehouseMoveSummary(reqVO)).thenReturn(summary);

        controller.getWarehouseMoveSummary(reqVO);

        verify(fieldPermissionMasker).maskListColumns("erp_warehouse_move", summary);
    }

    @Test
    void confirmWarehouseMove_delegatesToService() {
        controller.confirmWarehouseMove(10L);

        verify(warehouseMoveService).confirmWarehouseMove(10L);
    }

    @Test
    void cancelConfirmWarehouseMove_delegatesToService() {
        controller.cancelConfirmWarehouseMove(10L);

        verify(warehouseMoveService).cancelConfirmWarehouseMove(10L);
    }

    @Test
    void pickDetailEndpoints_validateSourceAndKeepQueryPermission() throws NoSuchMethodException {
        when(warehouseMoveService.getWarehouseMove(33L)).thenReturn(new ErpWarehouseMoveDO().setId(33L));
        ErpSalePickSourceDetailRespVO detail = new ErpSalePickSourceDetailRespVO();
        when(salePickDeliveryService.getSourcePickDetail(80, 33L)).thenReturn(detail);
        ErpWarehouseMovePickPageReqVO pageReqVO = new ErpWarehouseMovePickPageReqVO();
        pageReqVO.setMoveId(33L);
        when(salePickDeliveryService.getSourcePickItemPage(80, 33L, pageReqVO))
                .thenReturn(PageResult.empty());
        when(salePickDeliveryService.getSourcePickOperationItemPage(80, 33L, pageReqVO))
                .thenReturn(PageResult.empty());

        assertEquals(detail, controller.getWarehouseMovePickDetail(33L).getData());
        assertEquals(0L, controller.getWarehouseMovePickItemPage(pageReqVO).getData().getTotal());
        assertEquals(0L, controller.getWarehouseMovePickOperationItemPage(pageReqVO).getData().getTotal());
        verify(warehouseMoveService, times(3)).getWarehouseMove(33L);
        verify(salePickDeliveryService).getSourcePickDetail(80, 33L);
        verify(salePickDeliveryService).getSourcePickItemPage(80, 33L, pageReqVO);
        verify(salePickDeliveryService).getSourcePickOperationItemPage(80, 33L, pageReqVO);

        for (String methodName : List.of("getWarehouseMovePickDetail", "getWarehouseMovePickItemPage",
                "getWarehouseMovePickOperationItemPage")) {
            Method method = methodName.equals("getWarehouseMovePickDetail")
                    ? ErpWarehouseMoveController.class.getMethod(methodName, Long.class)
                    : ErpWarehouseMoveController.class.getMethod(methodName, ErpWarehouseMovePickPageReqVO.class);
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertNotNull(annotation);
            assertTrue(annotation.value().contains("erp:warehouse-move:query"));
        }
    }

    @Test
    void pickDetailEndpoint_rejectsMissingWarehouseMoveBeforeReadingFulfillment() {
        when(warehouseMoveService.getWarehouseMove(404L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> controller.getWarehouseMovePickDetail(404L));

        verify(salePickDeliveryService, never()).getSourcePickDetail(any(), any());
    }

}
