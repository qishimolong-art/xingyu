package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataCommonApi;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.*;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ErpStockMovementExportTest {
    private final List<ErpStockMoveRespVO> mains = new ArrayList<>();
    private final List<ErpStockMoveItemDO> detail = new ArrayList<>();
    private final AtomicInteger itemQueries = new AtomicInteger();
    @BeforeEach void setup() {
        DictDataCommonApi api = mock(DictDataCommonApi.class);
        when(api.getDictDataList(anyString())).thenReturn(Collections.emptyList());
        DictFrameworkUtils.init(api); DictFrameworkUtils.clearCache();
        mains.add(new ErpStockMoveRespVO().setId(9L).setNo("OUT9").setStatus(20)
                .setTotalCount(new BigDecimal("1.123456")).setTotalPrice(new BigDecimal("3.5")));
        detail.add(new ErpStockMoveItemDO().setId(1L).setMoveId(9L).setCount(new BigDecimal("1.123456"))
                .setProductPrice(new BigDecimal("2")).setTotalPrice(new BigDecimal("3.50")));
    }
    private MockHttpServletResponse export(String fields, Set<String> hidden, IntFunction<PageResult<ErpStockMoveRespVO>> pages) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ErpStockMovementExportSupport.write(response, "movement", fields, ErpStockMoveRespVO.class,
                ErpStockTransferExportRespVO.class, hidden, pages, PageResult::getList,
                (ids, page) -> {
                    itemQueries.incrementAndGet();
                    assertEquals(new ArrayList<>(ids), mains.stream().map(ErpStockMoveRespVO::getId).collect(Collectors.toList()));
                    return detail.subList(Math.min((page - 1) * 500, detail.size()), Math.min(page * 500, detail.size()));
                }, ErpStockMoveItemDO::getMoveId,
                items -> items.stream().map(item -> new ErpStockMoveRespVO.Item().setId(item.getId())
                        .setCount(item.getCount()).setProductPrice(item.getProductPrice()).setTotalPrice(item.getTotalPrice())
                        .setPackageQty(2).setProductName("商品")).collect(Collectors.toList()));
        return response;
    }
    private Workbook workbook(String fields, Set<String> hidden) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(export(fields, hidden,
                page -> new PageResult<>(mains, (long)mains.size())).getContentAsByteArray()));
    }
    private Map<String,Integer> columns(Sheet sheet) {
        Map<String,Integer> columns = new HashMap<>();
        for (Cell cell : sheet.getRow(0)) columns.put(cell.getStringCellValue(), cell.getColumnIndex());
        return columns;
    }
    @Test void selectedMainOnlyDoesNotQueryItemsAndKeepsNaturalNumericPrecision() throws Exception {
        try (Workbook book = workbook("no,totalCount,totalPrice", Collections.emptySet())) {
            Sheet sheet = book.getSheetAt(0); assertEquals(1, sheet.getLastRowNum());
            Map<String,Integer> columns = columns(sheet);
            Cell quantity = sheet.getRow(1).getCell(columns.get("合计数量"));
            assertEquals(CellType.NUMERIC, quantity.getCellType()); assertEquals(1.123456, quantity.getNumericCellValue());
            assertEquals("0.####################", quantity.getCellStyle().getDataFormatString());
            Cell money = sheet.getRow(1).getCell(columns.get("合计金额"));
            assertEquals("0.####################", money.getCellStyle().getDataFormatString());
        }
        assertEquals(0, itemQueries.get());
    }
    @Test void detailUsesSavedAmountAndNumericQuantityAndPieces() throws Exception {
        try (Workbook book = workbook("no,itemQuantity,itemTotalPrice,itemPieceCount", Collections.emptySet())) {
            Sheet sheet = book.getSheetAt(0); Map<String,Integer> columns = columns(sheet);
            assertEquals(1.123456, sheet.getRow(1).getCell(columns.get("明细-数量")).getNumericCellValue());
            assertEquals(3.5, sheet.getRow(1).getCell(columns.get("明细-金额")).getNumericCellValue());
            assertEquals(.561728, sheet.getRow(1).getCell(columns.get("明细-件数")).getNumericCellValue());
        }
    }
    @Test void batchesItemsAndPreservesDocumentOrderIncludingEmptyDrafts() throws Exception {
        mains.add(0, new ErpStockMoveRespVO().setId(11L).setNo("EMPTY"));
        detail.clear();
        for (int id=1; id<=501; id++) detail.add(new ErpStockMoveItemDO().setId((long)id).setMoveId(9L).setCount(BigDecimal.ONE));
        try (Workbook book = workbook("no,itemQuantity", Collections.emptySet())) {
            Sheet sheet=book.getSheetAt(0); assertEquals(502,sheet.getLastRowNum());
            assertEquals("EMPTY",sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("OUT9",sheet.getRow(2).getCell(0).getStringCellValue());
        }
        assertEquals(2,itemQueries.get());
    }
    @Test void hiddenFieldsAndForgedParametersCannotExposeValues() throws Exception {
        Set<String> mainHidden = ErpStockMovementExportSupport.hidden(ErpWarehouseMoveExportRespVO.class,
                new HashSet<>(Arrays.asList("creator", "updater", "approveUserId", "confirmUserId", "status")), Collections.emptyMap());
        assertTrue(mainHidden.containsAll(Arrays.asList("creatorName", "updaterName", "approveUserName", "confirmUserName", "statusName")));
        Set<String> hidden=ErpStockMovementExportSupport.hidden(ErpStockTransferExportRespVO.class,
                new HashSet<>(Arrays.asList("item_count","item_totalPrice","item_fromWarehouseId")),Collections.emptyMap());
        assertTrue(hidden.containsAll(Arrays.asList("itemQuantity","itemPieceCount","itemTotalPrice","totalCount","totalPrice","itemFromWarehouseName")));
        assertFalse(ErpStockMovementExportSupport.fields(ErpStockTransferExportRespVO.class,hidden).stream().anyMatch(field->field.getField().equals("itemQuantity")));
        try (Workbook book=workbook("no,itemQuantity,itemTotalPrice,forged",hidden)) { assertEquals(1,book.getSheetAt(0).getRow(0).getLastCellNum()); }
        assertEquals(0,itemQueries.get());
        assertThrows(RuntimeException.class,()->workbook("itemQuantity",hidden));
    }
    @Test void legacyKeepsOneRowAndOriginalHead() throws Exception {
        try (Workbook book=workbook(null,Collections.emptySet())) {
            assertEquals(1,book.getSheetAt(0).getLastRowNum());
            assertFalse(columns(book.getSheetAt(0)).containsKey("明细-数量"));
        }
        assertEquals(0,itemQueries.get());
    }
    @Test void excessiveOrEmptyFieldRequestsFailBeforeResponse() {
        assertThrows(RuntimeException.class,()->export("no",Collections.emptySet(),page->new PageResult<>(mains,5001L)));
        assertThrows(RuntimeException.class,()->workbook("",Collections.emptySet()));
    }
    @Test void warehouseCostAndLiveStockLabelsAreSeparateAndMasked() {
        ErpWarehouseMoveRespVO main=new ErpWarehouseMoveRespVO().setId(1L).setStatus(15).setStatusName("待拣货");
        ErpWarehouseMoveRespVO.Item item=new ErpWarehouseMoveRespVO.Item().setCount(new BigDecimal("1.123456"))
                .setCostPrice(new BigDecimal("2.50")).setCostAmount(new BigDecimal("2.80864")).setFromStockCount(BigDecimal.TEN);
        ErpWarehouseMoveExportRespVO row=ErpStockMovementExportSupport.row(ErpWarehouseMoveExportRespVO.class,main,item);
        assertEquals("待拣货",row.getStatusName()); assertEquals(new BigDecimal("2.80864"),row.getItemCostAmount());
        assertEquals(BigDecimal.TEN,row.getItemFromStockCount());
        item.setPriceVisible(false);row=ErpStockMovementExportSupport.row(ErpWarehouseMoveExportRespVO.class,main,item);
        assertNull(row.getItemCostPrice()); assertNull(row.getItemCostAmount());
    }
}
