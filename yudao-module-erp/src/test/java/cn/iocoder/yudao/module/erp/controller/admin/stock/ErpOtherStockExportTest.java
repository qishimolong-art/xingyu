package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataCommonApi;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.*;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.*;
import cn.iocoder.yudao.module.erp.service.stock.*;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ErpOtherStockExportTest {
    private final List<ErpStockInRespVO> mains = new ArrayList<>();
    private final List<ErpStockInItemDO> details = new ArrayList<>();
    private final AtomicInteger queries = new AtomicInteger();
    @BeforeEach void setup() {
        DictDataCommonApi dict = mock(DictDataCommonApi.class);
        when(dict.getDictDataList(anyString())).thenReturn(Collections.emptyList());
        DictFrameworkUtils.init(dict); DictFrameworkUtils.clearCache();
        mains.add(new ErpStockInRespVO().setId(1L).setNo("IN1").setStatus(15).setStatusName("待拣货/部分拣货")
            .setTotalCount(new BigDecimal("1.123456")).setTotalPrice(new BigDecimal("3.5")));
        details.add(new ErpStockInItemDO().setId(1L).setInId(1L).setCount(new BigDecimal("1.123456"))
            .setProductPrice(new BigDecimal("2.50")).setTotalPrice(new BigDecimal("3.50")).setPackageQty(2));
    }
    private Workbook workbook(String fields, Set<String> hidden) throws Exception {return export(fields, hidden, mains.size());}
    private Workbook export(String fields, Set<String> hidden, int total) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ErpStockMovementExportSupport.write(response,"other",fields,ErpStockInRespVO.class,ErpStockInExportRespVO.class,hidden,
            page -> new PageResult<>(mains.subList(Math.min((page-1)*100,mains.size()),Math.min(page*100,mains.size())),(long)total),PageResult::getList,
            (ids,page) -> {queries.incrementAndGet();List<ErpStockInItemDO> matched=details.stream().filter(item->ids.contains(item.getInId())).collect(Collectors.toList());return matched.subList(Math.min((page-1)*500,matched.size()),Math.min(page*500,matched.size()));},ErpStockInItemDO::getInId,
            batch -> batch.stream().map(item -> new ErpOtherStockExportSupport.Item().setCount(item.getCount()).setProductPrice(item.getProductPrice()).setTotalPrice(item.getTotalPrice()).setPackageQty(item.getPackageQty()).setProductBarCode("001234")).collect(Collectors.toList()));
        return WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
    }
    private Map<String,Integer> columns(Sheet sheet) {Map<String,Integer> result=new HashMap<>();for(Cell cell:sheet.getRow(0))result.put(cell.getStringCellValue(),cell.getColumnIndex());return result;}
    @Test void mainOnlyKeepsNumbersAndDoesNotReadDetails() throws Exception {
        try(Workbook book=workbook("no,totalCount,totalPrice",Collections.emptySet())) {
            Sheet sheet=book.getSheetAt(0);Map<String,Integer> cols=columns(sheet);assertEquals(1,sheet.getLastRowNum());
            Cell count=sheet.getRow(1).getCell(cols.get("合计数量"));assertEquals(CellType.NUMERIC,count.getCellType());assertEquals(1.123456,count.getNumericCellValue());assertEquals("0.####################",count.getCellStyle().getDataFormatString());
            Cell money=sheet.getRow(1).getCell(cols.get("合计金额"));assertEquals(3.5,money.getNumericCellValue());assertEquals("0.####################",money.getCellStyle().getDataFormatString());
        } assertEquals(0,queries.get());
    }
    @Test void detailRepeatsMainAndKeepsSavedAmountAndBarcodeText() throws Exception {
        details.add(new ErpStockInItemDO().setId(2L).setInId(1L).setCount(BigDecimal.ONE));
        try(Workbook book=workbook("no,itemQuantity,itemTotalPrice,itemProductBarCode",Collections.emptySet())) {
            Sheet sheet=book.getSheetAt(0);Map<String,Integer> cols=columns(sheet);assertEquals(2,sheet.getLastRowNum());assertEquals("IN1",sheet.getRow(2).getCell(cols.get("单号")).getStringCellValue());
            assertEquals(3.5,sheet.getRow(1).getCell(cols.get("业务金额")).getNumericCellValue());assertEquals("001234",sheet.getRow(1).getCell(cols.get("条码")).getStringCellValue());
        }
    }
    @Test void legacyIsOneMainRowWithOriginalColumnsAndNoDetailRead() throws Exception {
        details.add(new ErpStockInItemDO().setId(2L).setInId(1L));
        try(Workbook book=workbook(null,Collections.emptySet())) {assertEquals(1,book.getSheetAt(0).getLastRowNum());assertTrue(columns(book.getSheetAt(0)).containsKey("入库单号"));assertFalse(columns(book.getSheetAt(0)).containsKey("数量"));}
        assertEquals(0,queries.get());
    }
    @Test void detailBatchesAndEmptyHeadersRetainPosition() throws Exception {
        mains.add(0,new ErpStockInRespVO().setId(9L).setNo("EMPTY"));details.clear();for(int i=0;i<501;i++)details.add(new ErpStockInItemDO().setId((long)i+1).setInId(1L).setCount(BigDecimal.ONE));
        try(Workbook book=workbook("no,itemQuantity",Collections.emptySet())) {Sheet sheet=book.getSheetAt(0);assertEquals(502,sheet.getLastRowNum());assertEquals("EMPTY",sheet.getRow(1).getCell(0).getStringCellValue());assertEquals("IN1",sheet.getRow(2).getCell(0).getStringCellValue());}assertEquals(2,queries.get());
    }
    @Test void headersUseBatchesOf100() throws Exception {
        mains.clear();for(int i=0;i<101;i++)mains.add(new ErpStockInRespVO().setId((long)i+1).setNo("N"+i));try(Workbook book=workbook("no",Collections.emptySet())) {assertEquals(101,book.getSheetAt(0).getLastRowNum());}assertEquals(0,queries.get());
    }
    @Test void dependenciesAndForgedParametersUseSameWhitelist() throws Exception {
        Set<String> hidden=ErpOtherStockExportSupport.hidden(ErpStockInExportRespVO.class,new HashSet<>(Arrays.asList("item_count","col_creator","item_warehouseId","supplierId")));
        assertTrue(hidden.containsAll(Arrays.asList("itemQuantity","itemPieceCount","itemTotalPrice","itemTotalWeight","totalCount","totalPrice","totalWeight","creatorName","itemWarehouseDeptName","supplierName")));
        assertFalse(ErpStockMovementExportSupport.fields(ErpStockInExportRespVO.class,hidden).stream().anyMatch(f->f.getField().equals("itemQuantity")));
        try(Workbook book=workbook("no,itemQuantity,forged",hidden)){assertEquals(1,book.getSheetAt(0).getRow(0).getLastCellNum());}assertEquals(0,queries.get());
    }
    @Test void excessiveAndEmptyFieldRequestsFail() {assertThrows(RuntimeException.class,()->export("no",Collections.emptySet(),5001));assertThrows(RuntimeException.class,()->workbook("",Collections.emptySet()));}
    @Test void bothDirectionsHaveFieldMetadataAndSavedPriceSemantics() {
        ErpStockOutExportRespVO row=ErpStockMovementExportSupport.row(ErpStockOutExportRespVO.class,mains.get(0),new ErpOtherStockExportSupport.Item().setCount(BigDecimal.TEN).setProductPrice(BigDecimal.ONE).setTotalPrice(new BigDecimal("8.25")));
        assertEquals(new BigDecimal("8.25"),row.getItemTotalPrice());assertEquals("待拣货/部分拣货",row.getStatus());
        ErpStockOutExportRespVO masked=ErpStockMovementExportSupport.row(ErpStockOutExportRespVO.class,mains.get(0),new ErpOtherStockExportSupport.Item().setCount(BigDecimal.TEN).setProductPrice(BigDecimal.ONE).setTotalPrice(BigDecimal.TEN).setPriceVisible(false));assertNull(masked.getItemProductPrice());assertNull(masked.getItemTotalPrice());
    }
    private void relations(Object controller) {
        ReflectionTestUtils.setField(controller,"deptApi",mock(DeptApi.class));ReflectionTestUtils.setField(controller,"adminUserApi",mock(AdminUserApi.class));ReflectionTestUtils.setField(controller,"pickDeliveryService",mock(ErpSalePickDeliveryService.class));if(controller instanceof ErpStockInController) ReflectionTestUtils.setField(controller,"printService",mock(ErpPrintService.class));
        ErpStockFieldPermissionMasker masker=mock(ErpStockFieldPermissionMasker.class);when(masker.getHiddenFieldSet(anyString())).thenReturn(Collections.emptySet());ReflectionTestUtils.setField(controller,"fieldPermissionMasker",masker);
    }
    @Test void incomingSelectedIdsUseScopedPagesWithoutRawDetails() throws Exception {
        ErpStockInController c=new ErpStockInController();relations(c);ErpStockInService service=mock(ErpStockInService.class);ErpStockInItemMapper mapper=mock(ErpStockInItemMapper.class);ReflectionTestUtils.setField(c,"stockInService",service);ReflectionTestUtils.setField(c,"stockInItemMapper",mapper);ReflectionTestUtils.setField(c,"supplierService",mock(ErpSupplierService.class));
        ErpStockInPageReqVO req=new ErpStockInPageReqVO();req.setIds(Arrays.asList(1L,99L));when(service.getStockInPage(any())).thenAnswer(call->{assertEquals(Arrays.asList(1L,99L),req.getIds());assertEquals(100,req.getPageSize());assertFalse(req.getIncludeItems());return new PageResult<>(Collections.singletonList(new ErpStockInDO().setId(1L).setNo("VISIBLE")),1L);});
        MockHttpServletResponse response=new MockHttpServletResponse();c.exportStockInExcel(req,"no",response);verify(mapper,never()).selectExportPageByInIds(any(),anyInt());verify(service,never()).getStockInItemListByInId(anyLong());try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))){assertEquals("VISIBLE",book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());}
    }
    @Test void outgoingSelectedIdsUseScopedPagesAndMetadataWhitelist() throws Exception {
        ErpStockOutController c=new ErpStockOutController();relations(c);ErpStockOutService service=mock(ErpStockOutService.class);ErpStockOutItemMapper mapper=mock(ErpStockOutItemMapper.class);ReflectionTestUtils.setField(c,"stockOutService",service);ReflectionTestUtils.setField(c,"stockOutItemMapper",mapper);ReflectionTestUtils.setField(c,"customerService",mock(ErpCustomerService.class));
        ErpStockOutPageReqVO req=new ErpStockOutPageReqVO();req.setIds(Arrays.asList(2L,99L));when(service.getStockOutPage(any())).thenReturn(new PageResult<>(Collections.singletonList(new ErpStockOutDO().setId(2L).setNo("OUT")),1L));
        c.exportStockOutExcel(req,"no",new MockHttpServletResponse());verify(service).getStockOutPage(req);verify(mapper,never()).selectExportPageByOutIds(any(),anyInt());assertTrue(c.getStockOutExportFields().getData().stream().anyMatch(f->f.getField().equals("itemQuantity") && f.getGroup().equals("detail")));
    }
    @Test void mapperSelectionIsNarrowAndStableWithoutUnboundedQueries() {
        com.baomidou.mybatisplus.core.MybatisConfiguration config = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        config.addMapper(ErpStockInMapper.class); config.addMapper(ErpStockOutMapper.class);
        ErpStockInPageReqVO in=new ErpStockInPageReqVO();in.setIds(Arrays.asList(1L,2L));assertTrue(ErpStockInMapper.buildQuery(in).getSqlSegment().contains("IN"));
        ErpStockOutPageReqVO out=new ErpStockOutPageReqVO();out.setIds(Collections.singletonList(3L));assertTrue(ErpStockOutMapper.buildQuery(out).getSqlSegment().contains("IN"));
    }
    @Test void incomingListMasksDependentTotalsWithoutChangingPermissionDefinitions() {
        ErpStockInController c=new ErpStockInController();relations(c);
        ErpStockInService service=mock(ErpStockInService.class);ErpStockInItemMapper mapper=mock(ErpStockInItemMapper.class);
        ReflectionTestUtils.setField(c,"stockInService",service);ReflectionTestUtils.setField(c,"stockInItemMapper",mapper);ReflectionTestUtils.setField(c,"supplierService",mock(ErpSupplierService.class));
        ErpStockFieldPermissionMasker masker=mock(ErpStockFieldPermissionMasker.class);when(masker.getHiddenFieldSet(anyString())).thenReturn(Collections.singleton("item_count"));ReflectionTestUtils.setField(c,"fieldPermissionMasker",masker);
        when(service.getStockInPage(any())).thenReturn(new PageResult<>(Collections.singletonList(new ErpStockInDO().setId(1L)),1L));
        Map<String,Object> totals=new HashMap<>();totals.put("totalCount",new BigDecimal("7"));totals.put("totalPrice",new BigDecimal("14"));
        when(mapper.selectSummaryMapByInIds(any())).thenReturn(Collections.singletonMap(1L,totals));
        ErpStockInPageReqVO req=new ErpStockInPageReqVO();req.setIncludeItems(false);ErpStockInRespVO row=c.getStockInPage(req).getData().getList().get(0);
        assertNull(row.getTotalCount());assertNull(row.getTotalPrice());
    }

}
