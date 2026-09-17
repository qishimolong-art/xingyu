package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockCheckService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockCheckControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockCheckController controller;

    @Mock
    private ErpStockCheckService stockCheckService;
    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private ErpStockImportService stockImportService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
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
    private ErpPrintService printService;
    @Mock
    private ErpStockCheckItemMapper stockCheckItemMapper;

    @Test
    void getStockCheckPage_masksLegacyListAmountWhenItemAmountHidden() {
        ErpStockCheckPageReqVO reqVO = new ErpStockCheckPageReqVO();
        reqVO.setIncludeItems(false);
        ErpStockCheckDO stockCheck = new ErpStockCheckDO()
                .setId(10L)
                .setTotalPrice(new BigDecimal("120.00"));
        when(stockCheckService.getStockCheckPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(stockCheck), 1L));
        when(stockCheckItemMapper.selectSummaryMapByCheckIds(anyCollection())).thenReturn(Collections.emptyMap());
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(printService.getPrintCountMap(any(), anyCollection())).thenReturn(Collections.emptyMap());
        when(printService.getLastPrintTimeMap(any(), anyCollection())).thenReturn(Collections.emptyMap());
        when(fieldPermissionMasker.isFieldHidden(eq("erp_stock_check"), eq("item_totalPrice"))).thenReturn(true);

        CommonResult<PageResult<ErpStockCheckRespVO>> result = controller.getStockCheckPage(reqVO);

        assertNull(result.getData().getList().get(0).getTotalPrice());
        verify(fieldPermissionMasker).maskListColumns(eq("erp_stock_check"), anyCollection());
    }

}
