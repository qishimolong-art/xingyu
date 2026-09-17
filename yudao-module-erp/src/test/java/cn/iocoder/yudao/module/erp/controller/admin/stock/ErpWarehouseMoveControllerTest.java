package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
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

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

}
