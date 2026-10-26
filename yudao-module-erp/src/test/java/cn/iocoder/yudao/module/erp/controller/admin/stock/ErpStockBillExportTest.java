package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.*;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.*;
import cn.iocoder.yudao.module.erp.service.stock.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ErpStockBillExportTest {
    private Map<String,String> groups() {
        Map<String,String> result=new LinkedHashMap<>();
        for (String key:Arrays.asList("no","totalCount","pickedCount","remainingCount","warehouseDeptName")) result.put(key,"main");
        for (String key:Arrays.asList("itemCount","productPrice","itemTotalPrice","barCode")) result.put(key,"detail");
        return result;
    }
    private ErpStockInBillDO main(long id,String no) { return new ErpStockInBillDO().setId(id).setNo(no).setTotalCount(new BigDecimal("1.123456")); }
    private ErpStockInBillItemDO item(long id,long owner) { return new ErpStockInBillItemDO().setId(id).setBillId(owner).setCount(new BigDecimal("1.123456")).setProductPrice(new BigDecimal("2.50")); }
    private Workbook export(String fields,List<ErpStockInBillDO> mains,List<ErpStockInBillItemDO> items,AtomicInteger calls) throws Exception {
        MockHttpServletResponse response=new MockHttpServletResponse();
        ErpStockBillExportSupport.write(response,"bills",fields,ErpStockInBillExportRespVO.class,groups(),Collections.emptySet(),
                new LinkedHashSet<>(Arrays.asList("no","totalCount","itemCount","productPrice","itemTotalPrice","barCode")),
                page -> new PageResult<>(mains,(long)mains.size()),ErpStockInBillDO::getId,
                (ids,page) -> { calls.incrementAndGet(); assertEquals(mains.size(),ids.size()); return items.subList(Math.min((page-1)*500,items.size()),Math.min(page*500,items.size())); },
                ErpStockInBillItemDO::getBillId,(headers, batch) -> (main,item) -> {
                    ErpStockInBillExportRespVO row=new ErpStockInBillExportRespVO(); row.setNo(main.getNo()); row.setTotalCount(main.getTotalCount());
                    if(item!=null) { row.setItemCount(item.getCount()); row.setProductPrice(item.getProductPrice()); row.setItemTotalPrice(item.getCount().multiply(item.getProductPrice())); row.setBarCode("000123"); }
                    return row;
                });
        return WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
    }
    @Test void mainOnlyIsOneRowAndDoesNotReadItems() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        try(Workbook book=export("no,totalCount",Arrays.asList(main(1,"A"),main(2,"B")),Collections.emptyList(),calls)) {
            assertEquals(2,book.getSheetAt(0).getLastRowNum()); Cell value=book.getSheetAt(0).getRow(1).getCell(1);
            assertEquals(CellType.NUMERIC,value.getCellType()); assertEquals(1.123456,value.getNumericCellValue()); assertEquals("0.####################",value.getCellStyle().getDataFormatString());
        }
        assertEquals(0,calls.get());
    }
    @Test void detailRepeatsMainAndPreservesNumericAmountsAndBarcode() throws Exception {
        try(Workbook book=export("no,itemCount,productPrice,itemTotalPrice,barCode",Collections.singletonList(main(1,"A")),Arrays.asList(item(1,1),item(2,1)),new AtomicInteger())) {
            Sheet sheet=book.getSheetAt(0); assertEquals("A",sheet.getRow(1).getCell(0).getStringCellValue()); assertEquals("A",sheet.getRow(2).getCell(0).getStringCellValue());
            Map<String,Integer> cols=new HashMap<>(); for(Cell cell:sheet.getRow(0)) cols.put(cell.getStringCellValue(),cell.getColumnIndex());
            Cell price=sheet.getRow(1).getCell(cols.get("单价")); assertEquals(CellType.NUMERIC,price.getCellType()); assertEquals("0.####################",price.getCellStyle().getDataFormatString());
            assertEquals("000123",sheet.getRow(1).getCell(cols.get("条形码")).getStringCellValue());
        }
    }
    @Test void legacyKeepsDetailsAndOnlyFirstMainValues() throws Exception {
        try(Workbook book=export(null,Collections.singletonList(main(1,"A")),Arrays.asList(item(1,1),item(2,1)),new AtomicInteger())) {
            Sheet sheet=book.getSheetAt(0); assertEquals(2,sheet.getLastRowNum()); assertEquals("A",sheet.getRow(1).getCell(0).getStringCellValue());
            Cell no=sheet.getRow(2).getCell(0); assertTrue(no==null || no.getCellType()==CellType.BLANK);
            for(Cell cell:sheet.getRow(0)) assertNotEquals("仓库所属部门",cell.getStringCellValue());
        }
    }
    @Test void moreThan500DetailsAndEmptyHeaderKeepOrderedRows() throws Exception {
        List<ErpStockInBillItemDO> items=new ArrayList<>(); for(int i=0;i<501;i++) items.add(item(i+1,2));
        AtomicInteger calls=new AtomicInteger();
        try(Workbook book=export("no,itemCount",Arrays.asList(main(1,"EMPTY"),main(2,"FULL"),main(3,"LAST")),items,calls)) {
            Sheet sheet=book.getSheetAt(0); assertEquals(503,sheet.getLastRowNum()); assertEquals("EMPTY",sheet.getRow(1).getCell(0).getStringCellValue()); assertEquals("FULL",sheet.getRow(2).getCell(0).getStringCellValue()); assertEquals("LAST",sheet.getRow(503).getCell(0).getStringCellValue());
        }
        assertEquals(2,calls.get());
    }
    @Test void dependencyWhitelistIncludesBothOperandsAndPeople() {
        Map<String,String> aliases=new HashMap<>(); aliases.put("itemCount","item_count"); aliases.put("productPrice","item_productPrice");
        Set<String> hidden=ErpStockBillExportSupport.hidden(new HashSet<>(Arrays.asList("col_item_count","item_pickedCount","creator","updater","auditor")),aliases);
        assertTrue(hidden.containsAll(Arrays.asList("itemCount","itemTotalPrice","itemRemainCount","remainingCount","creatorName","updaterName","auditorName")));
        ErpStockInBillExportRespVO bean=new ErpStockInBillExportRespVO(); bean.setRemainingCount(BigDecimal.TEN); bean.setCreatorName("secret"); ErpStockBillExportSupport.mask(bean,hidden); assertNull(bean.getRemainingCount()); assertNull(bean.getCreatorName());
        ErpStockInBillItemRespVO item=new ErpStockInBillItemRespVO(); item.setWarehouseName("hidden warehouse"); item.setCount(BigDecimal.TEN);
        ErpStockBillExportSupport.maskItem(item,new HashSet<>(Arrays.asList("itemWarehouseName","itemCount"))); assertNull(item.getWarehouseName()); assertNull(item.getCount());
        ErpStockInBillRespVO header=new ErpStockInBillRespVO(); header.setWarehouseId(7L); ErpStockBillExportSupport.mask(header,new HashSet<>(Collections.singletonList("item_warehouseId"))); assertEquals(7L,header.getWarehouseId());
    }
    @Test void emptyFieldsAndOverLimitRejectBeforeWorkbook() {
        assertThrows(RuntimeException.class,()->export("",Collections.singletonList(main(1,"A")),Collections.emptyList(),new AtomicInteger()));
        assertThrows(RuntimeException.class,()->ErpStockBillExportSupport.write(new MockHttpServletResponse(),"test","no",ErpStockInBillExportRespVO.class,groups(),Collections.emptySet(),groups().keySet(),
                page -> new PageResult<>(Collections.singletonList(main(1,"A")),5001L),ErpStockInBillDO::getId,(ids,page)->Collections.<ErpStockInBillItemDO>emptyList(),ErpStockInBillItemDO::getBillId,
                (headers,items)->(header,item)->new ErpStockInBillExportRespVO()));
    }
    @Test void moreThan100HeadersReadInTwoBatches() throws Exception {
        List<ErpStockInBillDO> mains=new ArrayList<>(); for(int i=1;i<=101;i++) mains.add(main(i,"B"+i));
        AtomicInteger calls=new AtomicInteger(); MockHttpServletResponse response=new MockHttpServletResponse();
        ErpStockBillExportSupport.write(response,"bills","no",ErpStockInBillExportRespVO.class,groups(),Collections.emptySet(),groups().keySet(),
                page -> { calls.incrementAndGet(); return new PageResult<>(mains.subList((page-1)*100,Math.min(page*100,mains.size())),101L); },ErpStockInBillDO::getId,
                (ids,page)->{throw new AssertionError("Main-only export must not load details");},ErpStockInBillItemDO::getBillId,
                (headers,items)->(header,item)->{ErpStockInBillExportRespVO row=new ErpStockInBillExportRespVO();row.setNo(header.getNo());return row;});
        assertEquals(2,calls.get());
        try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) { assertEquals(101,book.getSheetAt(0).getLastRowNum()); assertEquals("B101",book.getSheetAt(0).getRow(101).getCell(0).getStringCellValue()); }
    }

    private ErpStockFieldPermissionMasker masker(List<String> hidden) {
        PermissionApi api=mock(PermissionApi.class); when(api.getCurrentUserHiddenFields(anyString())).thenReturn(hidden);
        ErpStockFieldPermissionMasker masker=new ErpStockFieldPermissionMasker(); ReflectionTestUtils.setField(masker,"permissionApi",api); return masker;
    }
    private void relations(Object controller) {
        ReflectionTestUtils.setField(controller,"warehouseService",mock(ErpWarehouseService.class));
        ReflectionTestUtils.setField(controller,"adminUserApi",mock(AdminUserApi.class));
        ReflectionTestUtils.setField(controller,"deptApi",mock(DeptApi.class));
    }
    @Test void inboundControllerSelectedIdsUseScopedPageAndMainOnlyAvoidsItemQuery() throws Exception {
        ErpStockInBillController controller=new ErpStockInBillController(); ErpStockInBillService service=mock(ErpStockInBillService.class); ErpStockInBillItemMapper mapper=mock(ErpStockInBillItemMapper.class);
        ReflectionTestUtils.setField(controller,"stockInBillService",service); ReflectionTestUtils.setField(controller,"billItemMapper",mapper); ReflectionTestUtils.setField(controller,"fieldPermissionMasker",masker(Collections.emptyList())); relations(controller);
        when(service.getStockInBillPage(any())).thenAnswer(inv -> { ErpStockInBillPageReqVO req=inv.getArgument(0); assertEquals(Arrays.asList(1L,999L),req.getIds()); assertEquals(100,req.getPageSize()); return new PageResult<>(Collections.singletonList(main(1,"VISIBLE")),1L); });
        ErpStockInBillPageReqVO req=new ErpStockInBillPageReqVO(); req.setIds(Arrays.asList(1L,999L)); MockHttpServletResponse response=new MockHttpServletResponse(); controller.exportStockInBillExcel(req,"no",response);
        verify(service,never()).getStockInBillList(anyList()); verifyNoInteractions(mapper);
        try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) { assertEquals(1,book.getSheetAt(0).getLastRowNum()); assertEquals("VISIBLE",book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()); }
    }
    @Test void outboundFieldMetadataAndForgedFieldsUseSameWhitelist() throws Exception {
        ErpStockOutBillController controller=new ErpStockOutBillController(); ErpStockOutBillService service=mock(ErpStockOutBillService.class); ErpStockOutBillItemMapper mapper=mock(ErpStockOutBillItemMapper.class);
        ReflectionTestUtils.setField(controller,"stockOutBillService",service); ReflectionTestUtils.setField(controller,"billItemMapper",mapper); ReflectionTestUtils.setField(controller,"fieldPermissionMasker",masker(Arrays.asList("item_count","item_productPrice"))); relations(controller);
        assertFalse(controller.getStockOutBillExportFields().getData().stream().anyMatch(f -> Arrays.asList("itemCount","productPrice","itemTotalPrice","itemRemainCount").contains(f.getField())));
        when(service.getStockOutBillPage(any())).thenReturn(new PageResult<>(Collections.singletonList(new ErpStockOutBillDO().setId(1L).setNo("OUT")),1L));
        MockHttpServletResponse response=new MockHttpServletResponse(); controller.exportStockOutBillExcel(new ErpStockOutBillPageReqVO(),"no,itemCount,itemTotalPrice,forged",response); verifyNoInteractions(mapper);
        try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) { assertEquals(1,book.getSheetAt(0).getRow(0).getLastCellNum()); }
    }
}
