package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.*;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.apache.poi.ss.usermodel.*;
import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataCommonApi;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpStockCheckExportTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpStockCheckController controller;
    @Mock private ErpStockCheckService stockCheckService;
    @Mock private ErpStockCheckItemMapper stockCheckItemMapper;
    @Mock private ErpProductService productService;
    @Mock private ErpWarehouseService warehouseService;
    @Mock private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock private DeptApi deptApi;
    @Mock private AdminUserApi adminUserApi;
    @Mock private ErpPrintService printService;
    @Mock private DictDataCommonApi dictDataApi;

    @BeforeEach
    void setup() {
        DictFrameworkUtils.init(dictDataApi);
        DictFrameworkUtils.clearCache();
        lenient().when(dictDataApi.getDictDataList(anyString())).thenReturn(Collections.emptyList());
        ErpStockCheckDO main = new ErpStockCheckDO().setId(10L).setNo("PD001").setCheckType(1).setStatus(0);
        lenient().when(stockCheckService.getStockCheckPage(any())).thenReturn(new PageResult<>(Collections.singletonList(main), 1L));
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", new BigDecimal("-1.250"));
        summary.put("totalPrice", new BigDecimal("-12.50"));
        summary.put("itemCount", 2);
        lenient().when(stockCheckItemMapper.selectSummaryMapByCheckIds(anyCollection()))
                .thenReturn(Collections.singletonMap(10L, summary));
        lenient().when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(printService.getPrintCountMap(any(), anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(printService.getLastPrintTimeMap(any(), anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(stockCheckItemMapper.selectExportPageByCheckIds(anyCollection(), anyInt()))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        lenient().when(fieldPermissionMasker.getHiddenFieldSet(anyString())).thenReturn(Collections.emptySet());
    }

    private ErpStockCheckItemDO item(long id) {
        ErpStockCheckItemDO item = new ErpStockCheckItemDO();
        item.setId(id); item.setCheckId(10L); item.setProductId(1L); item.setWarehouseId(2L);
        item.setCount(new BigDecimal("-1.250")); item.setStockCount(new BigDecimal("8"));
        item.setActualCount(new BigDecimal("6.750")); item.setProductPrice(new BigDecimal("10"));
        item.setTotalPrice(new BigDecimal("-12.50")); item.setPackageQty(2);
        return item;
    }

    private Workbook export(String fields) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportStockCheckExcel(new ErpStockCheckPageReqVO(), fields, response);
        assertTrue(response.getContentAsByteArray().length > 0);
        return WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
    }

    @Test void legacyKeepsMainShapeAndNumericFormats() throws Exception {
        try (Workbook workbook = export(null)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum());
            Map<String, Integer> columns = columns(sheet);
            assertFalse(columns.containsKey("明细仓库"));
            Cell amount = sheet.getRow(1).getCell(columns.get("合计金额"));
            assertEquals(CellType.NUMERIC, amount.getCellType());
            assertEquals(-12.5, amount.getNumericCellValue());
            assertEquals("0.####################", amount.getCellStyle().getDataFormatString());
        }
        verify(stockCheckItemMapper, never()).selectExportPageByCheckIds(anyCollection(), anyInt());
    }

    @Test void selectedMainFieldsDoNotReadRawItems() throws Exception {
        try (Workbook workbook = export("no,totalCount,totalPrice")) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(3, sheet.getRow(0).getLastCellNum());
            assertEquals(1, sheet.getLastRowNum());
            assertEquals(-1.25, sheet.getRow(1).getCell(1).getNumericCellValue());
        }
        verify(stockCheckItemMapper, never()).selectExportPageByCheckIds(anyCollection(), anyInt());
    }

    @Test void detailRowsUseSavedSnapshotsAndSignedNumericCells() throws Exception {
        when(stockCheckItemMapper.selectExportPageByCheckIds(anyCollection(), eq(1)))
                .thenReturn(new PageResult<>(Arrays.asList(item(1), item(2)), 2L));
        try (Workbook workbook = export("no,detailStockCount,detailActualCount,detailCount,detailPieceCount,detailProductPrice,detailTotalPrice")) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columns = columns(sheet);
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("PD001", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(8, sheet.getRow(1).getCell(columns.get("明细账面数量")).getNumericCellValue());
            assertEquals(-1.25, sheet.getRow(1).getCell(columns.get("明细调整数量")).getNumericCellValue());
            assertEquals(-0.625, sheet.getRow(1).getCell(columns.get("明细件数")).getNumericCellValue());
            Cell price = sheet.getRow(1).getCell(columns.get("明细业务单价"));
            assertEquals(CellType.NUMERIC, price.getCellType());
            assertEquals("0.####################", price.getCellStyle().getDataFormatString());
        }
    }

    @Test void detailExportPagesAtFiveHundredRows() throws Exception {
        List<ErpStockCheckItemDO> first = IntStream.range(1, 501).mapToObj(this::item).collect(Collectors.toList());
        when(stockCheckItemMapper.selectExportPageByCheckIds(anyCollection(), eq(1))).thenReturn(new PageResult<>(first, 501L));
        when(stockCheckItemMapper.selectExportPageByCheckIds(anyCollection(), eq(2))).thenReturn(new PageResult<>(Collections.singletonList(item(501)), 501L));
        try (Workbook workbook = export("no,detailCount")) {
            assertEquals(501, workbook.getSheetAt(0).getLastRowNum());
        }
        verify(stockCheckItemMapper).selectExportPageByCheckIds(eq(Collections.singleton(10L)), eq(2));
        verify(productService, times(2)).getProductVOMap(anyCollection());
        verify(warehouseService, times(2)).getWarehouseMap(anyCollection());
        verify(stockCheckService, never()).getStockCheckItemListByCheckIds(anyCollection());
    }

    @Test void emptyDraftStillHasMainRow() throws Exception {
        try (Workbook workbook = export("no,detailCount")) {
            assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test void hiddenFieldsAreExcludedFromMetadataAndForgedSelections() throws Exception {
        when(fieldPermissionMasker.getHiddenFieldSet(anyString())).thenReturn(new HashSet<>(Arrays.asList("item_count", "item_totalPrice")));
        Set<String> fields = controller.getStockCheckExportFields().getData().stream().map(field -> field.getField()).collect(Collectors.toSet());
        assertFalse(fields.contains("detailCount"));
        assertFalse(fields.contains("detailPieceCount"));
        assertFalse(fields.contains("totalPrice"));
        assertFalse(fields.contains("balanceType"));
        try (Workbook workbook = export("no,detailCount")) {
            assertEquals(1, workbook.getSheetAt(0).getRow(0).getLastCellNum());
        }
        assertThrows(RuntimeException.class, () -> export("detailCount"));
        assertThrows(RuntimeException.class, () -> export("unknownField"));
    }

    @Test void legacyAlsoHonorsHiddenItemAmount() throws Exception {
        when(fieldPermissionMasker.getHiddenFieldSet(anyString())).thenReturn(Collections.singleton("item_totalPrice"));
        try (Workbook workbook = export(null)) {
            assertFalse(columns(workbook.getSheetAt(0)).containsKey("合计金额"));
        }
    }

    @Test void limitIsCheckedBeforeReturningAFile() {
        when(stockCheckService.getStockCheckPage(any())).thenReturn(new PageResult<>(Collections.emptyList(), 5001L));
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(RuntimeException.class, () -> controller.exportStockCheckExcel(new ErpStockCheckPageReqVO(), "no", response));
        assertEquals(0, response.getContentAsByteArray().length);
    }

    private Map<String, Integer> columns(Sheet sheet) {
        Map<String, Integer> result = new HashMap<>();
        sheet.getRow(0).forEach(cell -> result.put(cell.getStringCellValue(), cell.getColumnIndex()));
        return result;
    }
}
