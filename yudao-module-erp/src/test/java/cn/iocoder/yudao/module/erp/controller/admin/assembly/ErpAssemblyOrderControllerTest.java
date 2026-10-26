package cn.iocoder.yudao.module.erp.controller.admin.assembly;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.*;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeItemMapper;
import cn.iocoder.yudao.module.erp.service.assembly.ErpAssemblyOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ErpAssemblyOrderControllerTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpAssemblyOrderController controller;
    @Mock private ErpAssemblyOrderService orderService;
    @Mock private ErpAssemblyRecipeController recipeReader;
    @Mock private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Mock private ErpStockService stockService;
    @Mock private ErpWarehouseService warehouseService;
    @Mock private cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper warehouseMapper;

    @Test void warehouseCandidatesPreserveAllWarehousePermissionWithoutExplicitAssignments() {
        cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO req = new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO();
        req.setPageSize(20);
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(true);
        when(warehouseMapper.selectPage(req)).thenReturn(new PageResult<>(Collections.singletonList(new cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO().setId(5L).setName("测试仓")), 1L));
        assertEquals(1L, controller.warehouseOptionPage(req).getData().getTotal());
        verify(warehouseMapper, never()).selectPageByIds(any(), any());
    }
    @Test void warehouseCandidatesRestrictOrdinaryUsersToAssignedWarehouseIds() {
        cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO req = new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO();
        req.setPageSize(20);
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds()).thenReturn(Collections.singleton(5L));
        when(warehouseMapper.selectPageByIds(req, Collections.singleton(5L))).thenReturn(PageResult.empty());
        controller.warehouseOptionPage(req);
        verify(warehouseMapper, never()).selectPage(req);
        verify(warehouseMapper).selectPageByIds(req, Collections.singleton(5L));
    }
    @Test void availabilityUsesSavedSnapshotAndDeduplicatesBatchSplits() {
        when(orderService.getOrder(7L)).thenReturn(new ErpAssemblyOrderDO().setId(7L));
        when(orderService.getOrderItemList(7L)).thenReturn(Arrays.asList(
                new ErpAssemblyOrderItemDO().setRecipeItemId(1L).setProductId(10L).setQuantityPerSet(new BigDecimal("2")),
                new ErpAssemblyOrderItemDO().setRecipeItemId(1L).setProductId(10L).setQuantityPerSet(new BigDecimal("2")),
                new ErpAssemblyOrderItemDO().setRecipeItemId(2L).setProductId(11L).setQuantityPerSet(new BigDecimal("3"))));
        Map<String, ErpStockDO> stocks = new HashMap<>();
        stocks.put("10_5", new ErpStockDO().setCount(new BigDecimal("21")).setLockCount(BigDecimal.ONE));
        stocks.put("11_5", new ErpStockDO().setCount(new BigDecimal("31")).setLockCount(BigDecimal.ONE));
        when(stockService.getStockMap(anySet(), anySet())).thenReturn(stocks);
        ErpAssemblyAvailabilityRespVO result = controller.getAvailability(99L, 7L, false, Collections.singletonList(10L), 5L, null).getData();
        assertEquals(10, result.getMaxAssemblableCount());
        assertEquals(new BigDecimal("5"), result.getQuantityPerSetTotal());
        assertEquals(1, result.getItems().size());
        verifyNoInteractions(recipeItemMapper, recipeReader);
        verify(warehouseService).validateCurrentUserWarehousePermission(Collections.singleton(5L));
    }
    @Test void legacyAvailabilityKeepsAllItemsAndNewSummaryOmitsUnrequestedItems() {
        when(recipeReader.readRecipe(9L, false)).thenReturn(new ErpAssemblyRecipeRespVO().setStatus(1));
        when(recipeItemMapper.selectListByRecipeId(9L)).thenReturn(Collections.singletonList(
                new ErpAssemblyRecipeItemDO().setProductId(10L).setQuantityPerSet(BigDecimal.ONE)));
        when(stockService.getStockMap(anySet(), anySet())).thenReturn(Collections.emptyMap());
        assertEquals(1, controller.getAvailability(9L, null, true, null, 5L, null).getData().getItems().size());
        assertEquals(0, controller.getAvailability(9L, null, false, null, 5L, null).getData().getItems().size());
    }
    @Test void inaccessibleParentStopsSnapshotAndChildQueries() {
        assertThrows(RuntimeException.class, () -> controller.getAvailability(null, 7L, false, null, 5L, null));
        verify(orderService, never()).getOrderItemList(anyLong());
        ErpAssemblyOrderItemPageReqVO req = new ErpAssemblyOrderItemPageReqVO(); req.setOrderId(7L);
        assertThrows(RuntimeException.class, () -> controller.getOrderItemPage(req));
        verify(orderService, never()).getOrderItemPage(any());
    }
    @Test void assemblyCandidatesRequireEnabledRecipesAndBoundedPages() {
        ErpAssemblyRecipePageReqVO req = new ErpAssemblyRecipePageReqVO(); req.setPageSize(20); req.setStatus(0);
        when(recipeReader.readRecipePage(req)).thenReturn(PageResult.empty());
        controller.recipeOptionPage(req); assertEquals(1, req.getStatus());
        req.setPageSize(101); assertThrows(RuntimeException.class, () -> controller.recipeOptionPage(req));
        when(recipeReader.readRecipe(9L, false)).thenReturn(new ErpAssemblyRecipeRespVO().setStatus(0));
        assertThrows(RuntimeException.class, () -> controller.recipeHeader(9L));
    }
    @Test void exportIsXlsxAndPreservesTextCodesAndNumericAmounts() throws Exception {
        ErpAssemblyOrderExportRespVO row = new ErpAssemblyOrderExportRespVO();
        row.setProductCode("0000123"); row.setSetCount(5); row.setTotalCost(new BigDecimal("12.345678"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ErpAssemblyOrderController.writeExport(response, row);
        assertEquals('P', response.getContentAsByteArray()[0]); assertEquals('K', response.getContentAsByteArray()[1]);
        assertTrue(response.getContentType().contains("openxmlformats"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            org.apache.poi.ss.usermodel.Row data = workbook.getSheetAt(0).getRow(1);
            assertEquals(CellType.STRING, data.getCell(5).getCellType()); assertEquals("0000123", data.getCell(5).getStringCellValue());
            assertEquals(CellType.NUMERIC, data.getCell(14).getCellType()); assertEquals(5, data.getCell(14).getNumericCellValue());
            assertEquals(CellType.NUMERIC, data.getCell(15).getCellType());
            assertEquals("0.00", data.getCell(15).getCellStyle().getDataFormatString());
        }
    }
}
