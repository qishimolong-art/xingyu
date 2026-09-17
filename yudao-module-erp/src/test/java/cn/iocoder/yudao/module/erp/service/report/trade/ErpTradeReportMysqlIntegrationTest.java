package cn.iocoder.yudao.module.erp.service.report.trade;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.trade.ErpTradeReportModels.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Query tests deliberately seed immutable posting/snapshot fixtures; approval generation is verified separately. */
@EnabledIfEnvironmentVariable(named="ERP_TRADE_REPORT_MYSQL_TEST_URL",matches=".+")
class ErpTradeReportMysqlIntegrationTest {
    JdbcTemplate jdbc;
    DriverManagerDataSource ds;
    DataSourceTransactionManager manager;
    ErpTradeReportRepository repository;
    ErpTradeReportService service,target;
    PermissionApi permissions;
    DeptDataPermissionRespDTO document,party;
    final LocalDateTime cutover=LocalDateTime.of(2026,1,1,0,0),at=LocalDateTime.of(2026,1,2,12,34,56,123456000);

    @BeforeEach void setup()throws Exception {
        String url=System.getenv("ERP_TRADE_REPORT_MYSQL_TEST_URL");assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_trade_test(?:\\?.*)?"));
        ds=new DriverManagerDataSource(url,"root","");jdbc=new JdbcTemplate(ds);assertTrue(jdbc.queryForObject("SELECT @@datadir",String.class).replace('\\','/').contains("/Temp/xingyu-report-mysql-"));manager=new DataSourceTransactionManager(ds);
        for(String file:Arrays.asList("erp_report_dual_cost_foundation_20260909.sql","erp_trade_snapshot_v224.sql")){String ddl=new String(Files.readAllBytes(Paths.get("../sql/mysql/"+file)),StandardCharsets.UTF_8);for(String sql:ddl.split(";"))if(!sql.trim().isEmpty())jdbc.execute(sql);}
        for(String name:Arrays.asList("erp_sale_out","erp_sale_return","erp_purchase_in","erp_purchase_return"))jdbc.execute("CREATE TABLE IF NOT EXISTS "+name+"(id BIGINT,tenant_id BIGINT,customer_id BIGINT,supplier_id BIGINT,creator VARCHAR(64),deleted BIT DEFAULT 0,PRIMARY KEY(tenant_id,id)) ENGINE=InnoDB");
        for(String name:Arrays.asList("erp_customer","erp_supplier"))jdbc.execute("CREATE TABLE IF NOT EXISTS "+name+"(id BIGINT,tenant_id BIGINT,dept_id BIGINT,creator VARCHAR(64),allow_multi_dept BIT DEFAULT 0,deleted BIT DEFAULT 0,PRIMARY KEY(tenant_id,id)) ENGINE=InnoDB");
        for(String name:Arrays.asList("erp_customer_dept","erp_supplier_dept"))jdbc.execute("CREATE TABLE IF NOT EXISTS "+name+"(id BIGINT,tenant_id BIGINT,customer_id BIGINT,supplier_id BIGINT,dept_id BIGINT,deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for(String name:Arrays.asList("erp_business_report_item_snapshot","erp_stock_dual_cost_posting","erp_sale_out","erp_sale_return","erp_purchase_in","erp_purchase_return","erp_customer","erp_supplier","erp_customer_dept","erp_supplier_dept"))jdbc.execute("DELETE FROM "+name);
        jdbc.update("INSERT INTO erp_customer(id,tenant_id,dept_id,creator,allow_multi_dept) VALUES(501,1,999,'88',1)");jdbc.update("INSERT INTO erp_customer_dept(id,tenant_id,customer_id,dept_id) VALUES(1,1,501,301)");jdbc.update("INSERT INTO erp_supplier(id,tenant_id,dept_id,creator) VALUES(502,1,301,'99')");
        event(1,1,50,"-3","-650","-720","600",301,501,at);event(2,1,60,"1","216.666667","240","200",301,501,at.plusNanos(1000));event(3,1,70,"5","650","750","700",301,502,at.plusNanos(2000));
        document=new DeptDataPermissionRespDTO();document.setAll(true);party=new DeptDataPermissionRespDTO();party.setAll(true);permissions=mock(PermissionApi.class);
        when(permissions.getDeptDataPermission(eq(99L),anyString())).thenAnswer(c->{String module=c.getArgument(1);return module.endsWith("_report")?document:party;});
        repository=new ErpTradeReportRepository();ReflectionTestUtils.setField(repository,"jdbcTemplate",jdbc);target=new ErpTradeReportService();ReflectionTestUtils.setField(target,"repository",repository);ReflectionTestUtils.setField(target,"permissionApi",permissions);ReflectionTestUtils.setField(target,"enabled",true);ReflectionTestUtils.setField(target,"cutover",cutover.toString());
        ProxyFactory factory=new ProxyFactory(target);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));service=(ErpTradeReportService)factory.getProxy();login(1);
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    void login(long tenant){TenantContextHolder.setTenantId(tenant);LoginUser user=new LoginUser();user.setId(99L);user.setUserType(UserTypeEnum.ADMIN.getValue());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));}
    Filter filter(){Filter f=new Filter();f.setPostedFrom(cutover);f.setPostedTo(at.plusDays(1));f.setOrderDirection("asc");return f;}
    void event(long id,long tenant,int type,String quantity,String financial,String settlement,String gross,long dept,long partyId,LocalDateTime posted){
        jdbc.update("INSERT INTO erp_stock_dual_cost_posting(id,tenant_id,action_key,request_hash,stock_id,product_id,warehouse_id,stock_dept_id,accounting_dept_id,biz_type,biz_id,biz_item_id,biz_no,biz_date,posted_at,quantity,financial_movement,settlement_movement,balance_quantity,financial_balance,settlement_balance,rule_version) VALUES(?,?,?, ?,11,101,201,302,?,?,?,? ,?,?,?, ?,?,?,0,0,0,'INDEPENDENT_QUERY_FIXTURE')",id,tenant,"fixture-"+id,"a",dept,type,400+id,600+id,"DOC-"+id,posted,posted,new BigDecimal(quantity),new BigDecimal(financial),new BigDecimal(settlement));
        jdbc.update("INSERT INTO erp_business_report_item_snapshot(tenant_id,posting_id,posted_at,signature,business_type,biz_id,biz_item_id,party_id,party_name,sale_user_id,purchaser,source_creator,product_code,product_name,oe_number,brand,category_id,raw_gross_amount,gross_amount,net_amount,gross_status,snapshot_json) VALUES(?,?,?,'a',?,?,?,?,?,88,'buyer','99','P101','old name','OE1','original brand',701,?,?,NULL,'AVAILABLE',?)",tenant,id,posted,type==50?"SALE_OUT":type==60?"SALE_RETURN":"PURCHASE_IN",400+id,600+id,partyId,"party",new BigDecimal(gross),new BigDecimal(gross),"{\"rawUnitPrice\":\"200\",\"attributeSource\":\"POSTING_SOURCE_SNAPSHOT\"}");
    }
    static void amount(String expected,Object actual){assertTrue(actual instanceof String,"Decimals must be explicit JSON strings, got "+actual);assertEquals(0,new BigDecimal(expected).compareTo(new BigDecimal((String)actual)));}
    @SuppressWarnings("unchecked") static String state(Map<String,Object> row,String metric){return ((Map<String,String>)row.get("metricStates")).get(metric);}

    @Test void salePurchaseRemainSeparateAndReturnDirectionsPreserveExactTotals(){Bundle sale=service.page(true,filter(),false);assertEquals(2,sale.getPage().getTotal());amount("2",sale.getSummary().get("netQuantity"));amount("400",sale.getSummary().get("grossAmount"));amount("433.333333",sale.getSummary().get("financialCost"));amount("-216.666667",sale.getPage().getList().get(1).get("financialCost"));Bundle purchase=service.page(false,filter(),false);assertEquals(1,purchase.getPage().getTotal());amount("650",purchase.getSummary().get("financialCost"));amount("700",purchase.getSummary().get("grossAmount"));}
    @Test void unknownTaxAndSettlementSourcesNeverProduceFakeZeroProfitOrPayments(){Bundle b=service.page(true,filter(),false);for(Map<String,Object> row:Arrays.asList(b.getPage().getList().get(0),b.getSummary())){assertNull(row.get("netAmount"));assertNull(row.get("financialGrossProfit"));assertEquals("TAX_BASIS_UNCONFIRMED",state(row,"financialGrossProfit"));assertNull(row.get("receivedAmount"));assertEquals("SETTLEMENT_ALLOCATION_PENDING",state(row,"receivedAmount"));assertNull(row.get("freightAllocatedAmount"));}}
    @Test void missingSnapshotMakesWholeGroupAmountUnknownWithoutLosingReliableCost(){jdbc.update("DELETE FROM erp_business_report_item_snapshot WHERE posting_id=2");Bundle b=service.page(true,filter(),false);assertEquals(2,b.getPage().getTotal());assertNull(b.getSummary().get("grossAmount"));assertEquals(1,((Number)b.getSummary().get("missingSnapshotCount")).intValue());amount("433.333333",b.getSummary().get("financialCost"));assertEquals("SOURCE_SNAPSHOT_MISSING",b.getPage().getList().get(1).get("dataStatus"));}
    @Test void microsecondLeftClosedRightOpenAndAllTotalsIgnorePageSize(){Filter f=filter();f.setPostedFrom(at);f.setPostedTo(at.plusNanos(1000));assertEquals(1,service.page(true,f,false).getPage().getTotal());f=filter();f.setPageSize(1);Bundle b=service.page(true,f,false);assertEquals(1,b.getPage().getList().size());assertEquals(2,b.getPage().getTotal());amount("400",b.getSummary().get("grossAmount"));f.setPageNo(2);amount("400",service.page(true,f,false).getSummary().get("grossAmount"));}
    @Test void customerAvailableDepartmentDoesNotReplaceEventAccountingDepartment(){document.setAll(false);document.setDeptIds(Collections.singleton(999L));party.setAll(false);party.setDeptIds(Collections.singleton(301L));assertEquals(0,service.page(true,filter(),false).getPage().getTotal());document.setDeptIds(Collections.singleton(301L));assertEquals(2,service.page(true,filter(),false).getPage().getTotal());login(2);assertEquals(0,service.page(true,filter(),false).getPage().getTotal());}
    @Test void fieldMasksApplyToRowsGroupsTotalsAndActualExcel()throws Exception {when(permissions.getCurrentUserHiddenFields("erp_product",301L)).thenReturn(Arrays.asList("financialCost","salePrice"));Bundle b=service.page(true,filter(),false);for(Map<String,Object> row:Arrays.asList(b.getPage().getList().get(0),b.getSummary())){assertNull(row.get("financialCost"));assertEquals("MASKED",state(row,"financialCost"));assertNull(row.get("grossAmount"));assertEquals("MASKED",state(row,"financialGrossProfit"));}assertNull(service.page(true,filter(),true).getPage().getList().get(0).get("financialCost"));MockHttpServletResponse response=new MockHttpServletResponse();service.export(true,filter(),"DETAIL",response);try(Workbook book=new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))){Sheet data=book.getSheetAt(0);int column=column(data,"财务净成本");assertEquals("****",data.getRow(1).getCell(column).getStringCellValue());assertEquals("****",data.getRow(1).getCell(column(data,"含税净交易额")).getStringCellValue());}}
    @Test void hugeDecimalSurvivesJsonAndRealExcelStringCellExactly()throws Exception {String value="123456789012345.123456";jdbc.update("UPDATE erp_business_report_item_snapshot SET gross_amount=?,raw_gross_amount=? WHERE posting_id=1",new BigDecimal(value),new BigDecimal(value));Filter f=filter();f.setBusinessTypes(Collections.singletonList("SALE_OUT"));Bundle b=service.page(true,f,false);assertEquals(value,b.getPage().getList().get(0).get("grossAmount"));assertEquals(value,b.getSummary().get("grossAmount"));MockHttpServletResponse response=new MockHttpServletResponse();service.export(true,f,"DETAIL",response);try(Workbook book=new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))){Sheet data=book.getSheetAt(0);Cell cell=data.getRow(1).getCell(column(data,"含税净交易额"));assertEquals(CellType.STRING,cell.getCellType());assertEquals(value,cell.getStringCellValue());}}
    @Test void onePageAndSummaryUseOneDatabaseSnapshotDespiteConcurrentCommit()throws Exception {
        CountDownLatch paused=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean once=new AtomicBoolean();JdbcTemplate pausing=new JdbcTemplate(ds){@Override public <T>T queryForObject(String sql,Class<T> required,Object...args){T result=super.queryForObject(sql,required,args);if(sql.startsWith("SELECT COUNT(*)")&&sql.contains("erp_stock_dual_cost_posting")&&once.compareAndSet(false,true)){paused.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}return result;}};
        ReflectionTestUtils.setField(repository,"jdbcTemplate",pausing);ExecutorService pool=Executors.newSingleThreadExecutor();try{Future<Bundle> pending=pool.submit(()->{login(1);try{return service.page(true,filter(),false);}finally{cleanup();}});assertTrue(paused.await(10,TimeUnit.SECONDS));event(4,1,50,"-1","-10","-12","20",301,501,at.plusSeconds(1));release.countDown();Bundle old=pending.get(10,TimeUnit.SECONDS);assertEquals(2,old.getPage().getTotal());assertEquals(2,old.getPage().getList().size());assertEquals(2,((Number)old.getSummary().get("rowCount")).intValue());amount("400",old.getSummary().get("grossAmount"));assertEquals(3,service.page(true,filter(),false).getPage().getTotal());}finally{release.countDown();pool.shutdownNow();}}
    @Test void invalidFiltersAreRejectedRatherThanSilentlyIgnored(){Filter f=filter();f.setOrderField("financialCost");assertThrows(RuntimeException.class,()->service.page(true,f,false));f.setOrderField("postedAt");f.setSupplierId(502L);assertThrows(RuntimeException.class,()->service.page(true,f,false));f.setSupplierId(null);f.setGroupBy("PURCHASER");assertThrows(RuntimeException.class,()->service.page(true,f,true));}
    @Test void rawUnitPriceFromSnapshotNeverTraversesDoubleOrLosesSixDecimalPlaces()throws Exception {
        String exact="123456789012345.123456";
        jdbc.update("UPDATE erp_business_report_item_snapshot SET snapshot_json=? WHERE posting_id=1","{\"rawUnitPrice\":"+exact+",\"attributeSource\":\"POSTING_SOURCE_SNAPSHOT\"}");
        Filter f=filter();f.setBusinessTypes(Collections.singletonList("SALE_OUT"));assertEquals(exact,service.page(true,f,false).getPage().getList().get(0).get("rawUnitPrice"));
        MockHttpServletResponse response=new MockHttpServletResponse();service.export(true,f,"DETAIL",response);
        try(Workbook book=new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))){Cell cell=book.getSheetAt(0).getRow(1).getCell(column(book.getSheetAt(0),"原含税单价"));assertEquals(CellType.STRING,cell.getCellType());assertEquals(exact,cell.getStringCellValue());}
    }
    @Test void missingSnapshotStillRequiresBothDocumentSelfAndPartyPermission(){
        jdbc.update("DELETE FROM erp_business_report_item_snapshot WHERE posting_id=1");
        jdbc.update("INSERT INTO erp_sale_out(id,tenant_id,customer_id,creator) VALUES(401,1,501,'99')");
        document.setAll(false);document.setSelf(true);party.setAll(false);party.setDeptIds(Collections.singleton(888L));
        assertEquals(0,service.page(true,filter(),false).getPage().getTotal());
        party.setDeptIds(Collections.singleton(301L));assertEquals(2,service.page(true,filter(),false).getPage().getTotal());
        jdbc.update("UPDATE erp_sale_out SET creator='88' WHERE id=401");assertEquals(1,service.page(true,filter(),false).getPage().getTotal());
    }
    @Test void costHiddenByStockDepartmentAlsoMasksCrossDepartmentAccountingView(){
        when(permissions.getCurrentUserHiddenFields("erp_product",302L)).thenReturn(Collections.singletonList("financialCost"));
        Bundle b=service.page(true,filter(),false);assertNull(b.getPage().getList().get(0).get("financialCost"));assertEquals("MASKED",state(b.getSummary(),"financialCost"));
    }
    @Test void exportUsesItsOwnConsistentDatabaseSnapshotWhileNewBusinessCommits()throws Exception {
        CountDownLatch paused=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean once=new AtomicBoolean();
        JdbcTemplate pausing=new JdbcTemplate(ds){@Override public <T>T queryForObject(String sql,Class<T> required,Object...args){T value=super.queryForObject(sql,required,args);if(sql.startsWith("SELECT COUNT(*)")&&sql.contains("erp_stock_dual_cost_posting")&&once.compareAndSet(false,true)){paused.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}return value;}};
        ReflectionTestUtils.setField(repository,"jdbcTemplate",pausing);ExecutorService pool=Executors.newSingleThreadExecutor();
        try{Future<byte[]> pending=pool.submit(()->{login(1);try{MockHttpServletResponse response=new MockHttpServletResponse();service.export(true,filter(),"DETAIL",response);return response.getContentAsByteArray();}finally{cleanup();}});assertTrue(paused.await(10,TimeUnit.SECONDS));event(4,1,50,"-1","-10","-12","20",301,501,at.plusSeconds(1));release.countDown();try(Workbook book=new XSSFWorkbook(new ByteArrayInputStream(pending.get(10,TimeUnit.SECONDS)))){assertEquals(2,book.getSheetAt(0).getLastRowNum());boolean found=false;for(Row row:book.getSheetAt(1))if("事件行数".equals(row.getCell(0).getStringCellValue())){assertEquals("2",row.getCell(1).getStringCellValue());found=true;}assertTrue(found);}assertEquals(3,service.page(true,filter(),false).getPage().getTotal());}
        finally{release.countDown();pool.shutdownNow();}
    }
    @Test void sameEntityRenamedAcrossSnapshotsRemainsOneGroupWithExportedLabel()throws Exception {
        jdbc.update("UPDATE erp_business_report_item_snapshot SET product_name='renamed snapshot' WHERE posting_id=2");Filter f=filter();f.setGroupBy("PRODUCT");Bundle b=service.page(true,f,true);assertEquals(1,b.getPage().getTotal());Map<String,Object> row=b.getPage().getList().get(0);assertEquals(101L,((Number)row.get("groupKey")).longValue());assertTrue(row.get("groupLabel") instanceof String);amount("400",row.get("grossAmount"));MockHttpServletResponse response=new MockHttpServletResponse();service.export(true,f,"GROUP",response);try(Workbook book=new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))){Sheet data=book.getSheetAt(0);assertEquals(row.get("groupLabel"),data.getRow(1).getCell(column(data,"历史名称标签")).getStringCellValue());}
    }
    static int column(Sheet sheet,String label){for(Cell cell:sheet.getRow(0))if(label.equals(cell.getStringCellValue()))return cell.getColumnIndex();throw new AssertionError("Missing Excel column "+label);}
}
