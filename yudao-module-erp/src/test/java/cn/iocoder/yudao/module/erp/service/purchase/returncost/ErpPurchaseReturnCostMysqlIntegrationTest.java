package cn.iocoder.yudao.module.erp.service.purchase.returncost;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.datapermission.core.db.DataPermissionRuleHandler;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRuleFactoryImpl;
import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.*;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.*;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.*;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.*;
import cn.iocoder.yudao.module.erp.framework.datapermission.config.ErpDataPermissionConfiguration;
import cn.iocoder.yudao.module.erp.service.purchase.*;
import cn.iocoder.yudao.module.erp.service.stock.*;
import cn.iocoder.yudao.module.erp.service.stock.cost.*;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService;
import cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeReportRepository;
import cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeReportService;
import cn.iocoder.yudao.module.erp.controller.admin.report.trade.ErpTradeReportModels;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.mock.web.MockHttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import javax.annotation.Resource;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual procurement approval, stock-record service, moving-average service and database mappers.
 * Peripheral product sync and descriptive snapshot enrichment may be mocked; inventory writes are real. */
@EnabledIfEnvironmentVariable(named="ERP_PURCHASE_RETURN_COST_MYSQL_TEST_URL",matches=".+")
class ErpPurchaseReturnCostMysqlIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    TransactionTemplate tx;
    final LocalDateTime cutover=LocalDateTime.of(2026,1,1,0,0);
    final Map<Class<?>,Object> actual=new LinkedHashMap<>();
    ErpPurchaseReturnService approval;
    ErpPurchaseReturnServiceImpl approvalTarget;
    ErpPurchaseReturnCostService cost,costTarget;
    ErpTradeSnapshotService trade,tradeTarget;
    ErpDualCostPostingService writer,writerTarget;
    ErpStockRecordService stockRecords;
    ErpStockService stockService;
    ErpTradeReportService report;
    PermissionApi permissions;
    DeptDataPermissionRespDTO documentPermission,stockPermission;
    volatile Long failAfterStockItem;
    boolean checkSuspendedLegacyCallback;
    ErpStockRecordCreateReqBO lastRealStockRequest;

    @BeforeEach void setup() throws Exception {
        String url=System.getenv("ERP_PURCHASE_RETURN_COST_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_purchase_return_cost_test(?:\\?.*)?"));
        DriverManagerDataSource ds=new DriverManagerDataSource(url,"root","");jdbc=new JdbcTemplate(ds);
        assertTrue(jdbc.queryForObject("SELECT @@datadir",String.class).replace('\\','/').contains("/Temp/xingyu-report-mysql-"));
        manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);
        for(Class<?> type:Arrays.asList(ErpPurchaseReturnDO.class,ErpPurchaseReturnItemDO.class,ErpPurchaseInDO.class,ErpPurchaseInItemDO.class,ErpPurchaseOrderDO.class,ErpPurchaseOrderItemDO.class,ErpSaleReturnDO.class,ErpSaleReturnItemDO.class,ErpSaleOutItemDO.class,ErpStockDO.class,ErpStockRecordDO.class,ErpWarehouseDO.class,ErpProductDO.class,ErpSupplierDO.class))createEntityTable(type);
        // Match the existing erp_stock_cost_v1.sql storage precision, not the six-place new ledger.
        jdbc.execute("ALTER TABLE erp_stock MODIFY cost_amount DECIMAL(20,4), MODIFY cost_price DECIMAL(16,6)");
        jdbc.execute("ALTER TABLE erp_stock_record MODIFY cost_amount DECIMAL(20,4), MODIFY total_price DECIMAL(20,4), MODIFY cost_price DECIMAL(16,6), MODIFY unit_price DECIMAL(16,6)");
        for(String script:Arrays.asList("erp_report_dual_cost_foundation_20260909.sql","erp_trade_snapshot_v224.sql","erp_report_source_item_lock_index_20260909.sql","erp_purchase_return_current_cost_20260909.sql","erp_purchase_return_source_lock_index_20260909.sql","erp_stock_dimension_lookup_index_20260909.sql"))executeDdl(script);
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_product_unit(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_dept(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,nickname VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for(String table:Arrays.asList("erp_purchase_return_posting_link","erp_business_report_item_snapshot","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance","erp_stock_record","erp_purchase_return_items","erp_purchase_return","erp_purchase_in_items","erp_purchase_in","erp_purchase_order_items","erp_purchase_order","erp_sale_return_items","erp_sale_return","erp_stock","erp_warehouse","erp_product","erp_supplier","erp_product_unit"))jdbc.execute("DELETE FROM "+table);
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,status,purchase_enabled,creator,deleted) VALUES(201,1,301,'return warehouse',0,1,'99',0)");
        jdbc.update("INSERT INTO erp_product(id,tenant_id,code,name,brand,category_id,unit_id,status,creator,deleted) VALUES(101,1,'P101','part A','brand',701,801,0,'99',0),(102,1,'P102','part B','brand',701,801,0,'99',0)");
        jdbc.update("INSERT INTO erp_supplier(id,tenant_id,name,status,creator,deleted) VALUES(501,1,'source supplier',0,'99',0)");
        jdbc.update("INSERT INTO erp_product_unit(id,tenant_id,name) VALUES(801,1,'piece')");
        jdbc.update("INSERT INTO erp_stock(id,tenant_id,product_id,warehouse_id,dept_id,count,cost_price,cost_amount,creator,deleted) VALUES(11,1,101,201,301,2,80,160,'99',0),(12,1,102,201,301,2,70,140,'99',0)");
        jdbc.update("INSERT INTO erp_purchase_in(id,tenant_id,no,status,supplier_id,dept_id,in_time,total_count,total_product_price,total_price,discount_price,fee_amount,creator,deleted) VALUES(401,1,'IN-401',20,501,302,?,4,400,400,0,0,'99',0)",cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_purchase_in_items(id,tenant_id,in_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(601,1,401,101,201,301,2,100,200,'99',0),(602,1,401,102,201,301,2,100,200,'99',0)");
        documentPermission=new DeptDataPermissionRespDTO();documentPermission.setAll(true);
        stockPermission=new DeptDataPermissionRespDTO();stockPermission.setAll(true);
        permissions=mock(PermissionApi.class);
        when(permissions.getDeptDataPermission(eq(99L),anyString())).thenAnswer(c->"erp_stock".equals(c.getArgument(1))?stockPermission:documentPermission);
        actual.put(PermissionApi.class,permissions);actual.put(JdbcTemplate.class,jdbc);
        MybatisConfiguration config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);config.setCacheEnabled(false);
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(TenantContextHolder.getRequiredTenantId());}}));
        DeptDataPermissionRule rule=new DeptDataPermissionRule(permissions);new ErpDataPermissionConfiguration().erpDeptDataPermissionRuleCustomizer().customize(rule);
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new DataPermissionRuleHandler(new DataPermissionRuleFactoryImpl(Collections.singletonList(rule)))));
        MybatisSqlSessionFactoryBean bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(config);bean.setPlugins(interceptor);
        bean.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setLogicDeleteValue("1").setLogicNotDeleteValue("0")));
        SqlSessionFactory factory=bean.getObject();
        List<Class<?>> mappers=Arrays.asList(ErpPurchaseReturnMapper.class,ErpPurchaseReturnItemMapper.class,ErpPurchaseInMapper.class,ErpPurchaseInItemMapper.class,ErpPurchaseOrderMapper.class,ErpPurchaseOrderItemMapper.class,ErpSaleReturnMapper.class,ErpSaleReturnItemMapper.class,ErpStockMapper.class,ErpStockRecordMapper.class,ErpWarehouseMapper.class,ErpProductMapper.class,ErpSupplierMapper.class);
        for(Class<?> type:mappers)factory.getConfiguration().addMapper(type);
        SqlSessionTemplate template=new SqlSessionTemplate(factory);for(Class<?> type:mappers)actual.put(type,template.getMapper(type));
        ErpWarehouseServiceImpl warehouseTarget=new ErpWarehouseServiceImpl();inject(warehouseTarget);actual.put(ErpWarehouseService.class,proxy(warehouseTarget));
        ErpDualCostLedgerRepository ledger=new ErpDualCostLedgerRepository();inject(ledger);actual.put(ErpDualCostLedgerRepository.class,ledger);
        tradeTarget=new ErpTradeSnapshotService();inject(tradeTarget);set(tradeTarget,"enabled",true);trade=proxy(tradeTarget);actual.put(ErpTradeSnapshotService.class,trade);
        ErpPurchaseReturnCostRepository costRepository=new ErpPurchaseReturnCostRepository();inject(costRepository);actual.put(ErpPurchaseReturnCostRepository.class,costRepository);
        costTarget=new ErpPurchaseReturnCostService();inject(costTarget);set(costTarget,"enabled",true);cost=proxy(costTarget);actual.put(ErpPurchaseReturnCostService.class,cost);
        writerTarget=new ErpDualCostPostingService();inject(writerTarget);set(writerTarget,"enabled",false);set(writerTarget,"cutover",cutover.toString());writer=proxy(writerTarget);actual.put(ErpDualCostPostingService.class,writer);
        ErpStockServiceImpl stockTarget=new ErpStockServiceImpl();inject(stockTarget);stockService=proxy(stockTarget);actual.put(ErpStockService.class,stockService);
        ErpStockItemSnapshotSupport description=mock(ErpStockItemSnapshotSupport.class);
        doAnswer(call->{ErpStockRecordCreateReqBO bo=call.getArgument(0);
            lastRealStockRequest=bo;
            if(checkSuspendedLegacyCallback){checkSuspendedLegacyCallback=false;
                TransactionTemplate inner=new TransactionTemplate(manager);inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);inner.setTimeout(2);
                // A database lock timeout must not be mistaken for successful authorization rejection.
                assertThrows(IllegalStateException.class,()->inner.executeWithoutResult(s->stockService.updateStockCountAndCost(bo.getProductId(),bo.getWarehouseId(),bo.getCount(),bo.getUnitPrice(),bo.getBizType())));
                writer.assertLegacyMutationAllowed(true);
            }
            if(Objects.equals(failAfterStockItem,bo.getBizItemId()))throw new IllegalStateException("independent failure after actual stock cost update");return null;}).when(description).fillStockRecordSnapshot(any(),any());
        actual.put(ErpStockItemSnapshotSupport.class,description);
        ErpStockRecordServiceImpl stockRecordTarget=new ErpStockRecordServiceImpl();inject(stockRecordTarget);stockRecords=proxy(stockRecordTarget);actual.put(ErpStockRecordService.class,stockRecords);
        approvalTarget=new ErpPurchaseReturnServiceImpl();inject(approvalTarget);approval=proxy(approvalTarget);
        ErpTradeReportRepository reports=new ErpTradeReportRepository();inject(reports);actual.put(ErpTradeReportRepository.class,reports);
        ErpTradeReportService reportTarget=new ErpTradeReportService();inject(reportTarget);set(reportTarget,"enabled",true);set(reportTarget,"cutover",cutover.toString());report=proxy(reportTarget);
        login(1);
        writer.confirmOpening(101,201,new BigDecimal("2"),new BigDecimal("160"),new BigDecimal("180"),cutover,"independent current cost opening",99);
        writer.confirmOpening(102,201,new BigDecimal("2"),new BigDecimal("140"),new BigDecimal("170"),cutover,"independent second stock opening",99);
        set(writerTarget,"enabled",true);
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    void executeDdl(String name)throws Exception {String ddl=new String(Files.readAllBytes(Paths.get("../sql/mysql/"+name)),StandardCharsets.UTF_8);tx.executeWithoutResult(s->{for(String sql:ddl.split(";"))if(!sql.trim().isEmpty())jdbc.execute(sql);});}
    void inject(Object target){for(Field field:target.getClass().getDeclaredFields())if(field.isAnnotationPresent(Resource.class)){Object value=actual.get(field.getType());if(value==null)value=mock(field.getType());set(target,field.getName(),value);}}
    static void set(Object object,String name,Object value){ReflectionTestUtils.setField(object,name,value);}
    @SuppressWarnings("unchecked") <T>T proxy(T target){ProxyFactory factory=new ProxyFactory(target);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));return (T)factory.getProxy();}
    void login(long tenant){TenantContextHolder.setTenantId(tenant);LoginUser user=new LoginUser();user.setId(99L);user.setUserType(UserTypeEnum.ADMIN.getValue());user.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID,"301"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));}
    void createEntityTable(Class<?> type){String table=type.getAnnotation(TableName.class).value();Map<String,String> columns=new LinkedHashMap<>();columns.put("tenant_id","BIGINT");for(Class<?> current=type;current!=Object.class;current=current.getSuperclass())for(Field field:current.getDeclaredFields()){
        if(Modifier.isStatic(field.getModifiers()))continue;TableField annotation=field.getAnnotation(TableField.class);if(annotation!=null&&!annotation.exist())continue;
        String name=annotation!=null&&!annotation.value().isEmpty()?annotation.value():field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
        Class<?> value=field.getType();String sql=value==BigDecimal.class?"DECIMAL(24,6)":value==Long.class?"BIGINT":value==Integer.class?"INT":value==Boolean.class?"BIT":value==LocalDateTime.class?"DATETIME(6)":"TEXT";
        if("id".equals(name))sql="BIGINT PRIMARY KEY";if("deleted".equals(name))sql="BIT DEFAULT 0";columns.put(name,sql);
    }List<String> defs=new ArrayList<>();columns.forEach((key,value)->defs.add("`"+key+"` "+value));jdbc.execute("CREATE TABLE IF NOT EXISTS "+table+" ("+String.join(",",defs)+") ENGINE=InnoDB");}
    void prepareReturn(long id,int mode,String...quantities){BigDecimal total=Arrays.stream(quantities).map(BigDecimal::new).reduce(BigDecimal.ZERO,BigDecimal::add);jdbc.update("INSERT INTO erp_purchase_return(id,tenant_id,no,status,supplier_id,dept_id,return_mode,return_time,total_count,total_product_price,total_price,discount_price,refund_price,creator,deleted) VALUES(?,1,?,10,501,302,?,?,?, ?,?,0,0,'99',0)",id,"RETURN-"+id,mode,cutover.plusDays(2),total,total.multiply(new BigDecimal("100")),total.multiply(new BigDecimal("100")));
        for(int n=0;n<quantities.length;n++){BigDecimal quantity=new BigDecimal(quantities[n]);jdbc.update("INSERT INTO erp_purchase_return_items(id,tenant_id,return_id,source_in_item_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(?,1,?,?,?,201,301,?,100,?,'99',0)",id*10+n,id,mode==10?601L+n:null,101L+n,quantity,quantity.multiply(new BigDecimal("100")));}}
    void approve(long id){approval.updatePurchaseReturnStatus(id,20);}
    void amount(String expected,String sql){BigDecimal actualValue=jdbc.queryForObject(sql,BigDecimal.class);assertNotNull(actualValue);assertEquals(0,new BigDecimal(expected).compareTo(actualValue),sql);}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    ErpTradeReportModels.Filter filter(){ErpTradeReportModels.Filter f=new ErpTradeReportModels.Filter();f.setPostedFrom(cutover);f.setPostedTo(LocalDateTime.now().plusDays(1));return f;}

    @Test void refundHundredUsesCurrentEightyCostThroughRealLegacyAndDualWriter(){
        prepareReturn(701,10,"1");approve(701);
        amount("1","SELECT count FROM erp_stock WHERE id=11");amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");amount("80","SELECT cost_price FROM erp_stock WHERE id=11");
        amount("80","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("90","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");
        amount("-80","SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE biz_type=80");amount("-90","SELECT settlement_movement FROM erp_stock_dual_cost_posting WHERE biz_type=80");
        amount("100","SELECT unit_price FROM erp_stock_record WHERE biz_type=80");amount("-100","SELECT total_price FROM erp_stock_record WHERE biz_type=80");
        amount("100","SELECT product_price FROM erp_purchase_return_items WHERE return_id=701");
        assertNull(jdbc.queryForObject("SELECT net_amount FROM erp_business_report_item_snapshot WHERE business_type='PURCHASE_RETURN'",BigDecimal.class));
        ErpTradeReportModels.Bundle bundle=report.page(false,filter(),false);
        assertEquals(1,bundle.getPage().getTotal());
        Map<String,Object> row=bundle.getPage().getList().get(0);
        assertEquals("100.000000",row.get("returnGrossAmount"));
        assertEquals("80.000000",row.get("returnFinancialCost"));
        assertEquals("90.000000",row.get("returnSettlementCost"));
        assertNull(row.get("returnDifferenceProfit"));assertNull(bundle.getSummary().get("returnDifferenceProfit"));
        assertEquals("TAX_BASIS_UNCONFIRMED",((Map<?,?>)row.get("metricStates")).get("returnDifferenceProfit"));
        assertNull(row.get("sourceOrderId"));assertNull(row.get("sourceOrderItemId"));
    }
    @Test void secondLineFailureRollsBackActualLegacyCostAndWholeApproval(){
        prepareReturn(701,10,"1","1");failAfterStockItem=7011L;
        assertThrows(RuntimeException.class,()->approve(701));
        assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=701",Integer.class));
        amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");amount("140","SELECT cost_amount FROM erp_stock WHERE id=12");
        amount("160","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("170","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=12");
        assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));assertEquals(0,count("erp_business_report_item_snapshot"));assertEquals(0,count("erp_purchase_return_posting_link"));
    }
    @Test void byStockHasNoInventedOriginalPurchaseAndUsesCurrentCost(){
        prepareReturn(701,20,"1");approve(701);
        String json=jdbc.queryForObject("SELECT snapshot_json FROM erp_business_report_item_snapshot",String.class);
        assertTrue(json.contains("NO_ORIGINAL_PURCHASE"));assertTrue(json.contains("CURRENT_AVERAGE"));
        assertNull(jdbc.queryForObject("SELECT source_biz_id FROM erp_stock_dual_cost_posting",Long.class));
        amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");
    }
    @Test void concurrentApprovalsCannotReturnMoreThanOriginalInQuantity()throws Exception {
        // Physical stock can satisfy both requests. Only source quantity protection may reject one.
        jdbc.update("UPDATE erp_purchase_in_items SET count=1,total_price=100 WHERE id=601");
        jdbc.update("UPDATE erp_purchase_in SET total_count=3,total_product_price=300,total_price=300 WHERE id=401");
        prepareReturn(701,10,"1");prepareReturn(702,10,"1");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch go=new CountDownLatch(1);
        try{List<Future<Boolean>> results=new ArrayList<>();for(long id:new long[]{701,702})results.add(pool.submit(()->{login(1);go.await();try{approve(id);return true;}catch(RuntimeException failure){return false;}finally{cleanup();}}));go.countDown();int successes=0;for(Future<Boolean> result:results)if(result.get(15,TimeUnit.SECONDS))successes++;assertEquals(1,successes);
            amount("1","SELECT count FROM erp_stock WHERE id=11");amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_business_report_item_snapshot"));
        }finally{pool.shutdownNow();}
    }
    @Test void tenantDepartmentAndStockCreatorProtectActualApproval(){
        prepareReturn(701,10,"1");login(2);assertThrows(RuntimeException.class,()->approve(701));login(1);
        documentPermission.setAll(false);documentPermission.setDeptIds(Collections.singleton(999L));assertThrows(RuntimeException.class,()->approve(701));documentPermission.setAll(true);
        stockPermission.setAll(false);stockPermission.setSelf(true);jdbc.update("UPDATE erp_stock SET creator='88' WHERE id=11");assertThrows(RuntimeException.class,()->approve(701));
        assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=701",Integer.class));
    }
    @Test void sourceSupplierProductAndApprovalAreValidatedBeforeStockChanges(){
        prepareReturn(701,10,"1");jdbc.update("UPDATE erp_purchase_in SET status=10 WHERE id=401");assertThrows(RuntimeException.class,()->approve(701));
        jdbc.update("UPDATE erp_purchase_in SET status=20,supplier_id=999 WHERE id=401");assertThrows(RuntimeException.class,()->approve(701));
        jdbc.update("UPDATE erp_purchase_in SET supplier_id=501 WHERE id=401");jdbc.update("UPDATE erp_purchase_return SET supplier_id=NULL WHERE id=701");assertThrows(RuntimeException.class,()->approve(701));
        jdbc.update("UPDATE erp_purchase_return SET supplier_id=501 WHERE id=701");
        jdbc.update("UPDATE erp_purchase_in SET supplier_id=501 WHERE id=401");jdbc.update("UPDATE erp_purchase_in_items SET product_id=102 WHERE id=601");assertThrows(RuntimeException.class,()->approve(701));
        amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(0,count("erp_stock_record"));
    }
    @Test void threePartialReturnsExhaustRealLegacyAndDualAmountsWithoutRoundingResidue(){
        jdbc.update("INSERT INTO erp_product(id,tenant_id,code,name,unit_id,status,creator,deleted) VALUES(103,1,'P103','precision fixture',801,0,'99',0)");
        jdbc.update("INSERT INTO erp_stock(id,tenant_id,product_id,warehouse_id,dept_id,count,cost_price,cost_amount,creator,deleted) VALUES(13,1,103,201,301,3,216.666667,650,'99',0)");
        jdbc.update("INSERT INTO erp_purchase_in_items(id,tenant_id,in_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(603,1,401,103,201,301,3,100,300,'99',0)");
        jdbc.update("UPDATE erp_purchase_in SET total_count=7,total_product_price=700,total_price=700 WHERE id=401");
        set(writerTarget,"enabled",false);writer.confirmOpening(103,201,new BigDecimal("3"),new BigDecimal("650"),new BigDecimal("720"),cutover,"precision opening verified before activity",99);set(writerTarget,"enabled",true);
        String[] balances={"433.333333","216.666666","0"};
        String[] legacyBalances={"433.3333","216.6667","0"};
        for(int n=0;n<3;n++){
            long id=701+n;prepareReturn(id,10,"1");jdbc.update("UPDATE erp_purchase_return_items SET product_id=103,source_in_item_id=603 WHERE return_id=?",id);approve(id);
            amount(legacyBalances[n],"SELECT cost_amount FROM erp_stock WHERE id=13");amount(balances[n],"SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=13");
        }
        amount("-650","SELECT SUM(financial_movement) FROM erp_stock_dual_cost_posting WHERE product_id=103");amount("-720","SELECT SUM(settlement_movement) FROM erp_stock_dual_cost_posting WHERE product_id=103");
        amount("-300","SELECT SUM(total_price) FROM erp_stock_record WHERE product_id=103");amount("0","SELECT cost_price FROM erp_stock WHERE id=13");
    }
    @Test void authenticatedLegacyCallbackCannotBeBorrowedByRequiresNewAndRestoresOuter(){
        prepareReturn(701,10,"1");checkSuspendedLegacyCallback=true;approve(701);
        assertFalse(checkSuspendedLegacyCallback);amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");
        assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_stock_dual_cost_posting"));
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->stockService.updateStockCountAndCost(101L,201L,new BigDecimal("-1"),new BigDecimal("100"),80)));
    }
    @Test void oldReadSnapshotCannotHideReturnCommittedWhileWaitingForSource()throws Exception {
        jdbc.update("UPDATE erp_purchase_in_items SET count=1,total_price=100 WHERE id=601");
        jdbc.update("UPDATE erp_purchase_in SET total_count=3,total_product_price=300,total_price=300 WHERE id=401");
        prepareReturn(701,10,"1");prepareReturn(702,10,"1");
        CountDownLatch sourceHeld=new CountDownLatch(1),oldSnapshot=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Future<?> first=pool.submit(()->{login(1);try{tx.executeWithoutResult(s->{cost.lockBeforeMutation(701L,null);sourceHeld.countDown();await(release);approve(701);});}finally{cleanup();}});
            assertTrue(sourceHeld.await(5,TimeUnit.SECONDS));
            Future<?> second=pool.submit(()->{login(1);try{assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx.executeWithoutResult(s->{jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_return WHERE status=20",Integer.class);oldSnapshot.countDown();approve(702);}));}finally{cleanup();}});
            assertTrue(oldSnapshot.await(5,TimeUnit.SECONDS));assertSourceWait();release.countDown();first.get(12,TimeUnit.SECONDS);second.get(12,TimeUnit.SECONDS);
            amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(1,count("erp_purchase_return_posting_link"));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=702",Integer.class));
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void sourceReplacementWaitingForApprovalCannotEditTheApprovedReturn()throws Exception {
        prepareReturn(701,10,"1");
        jdbc.update("INSERT INTO erp_purchase_in(id,tenant_id,no,status,supplier_id,dept_id,in_time,total_count,total_product_price,total_price,creator,deleted) VALUES(402,1,'IN-402',20,501,302,?,2,200,200,'99',0)",cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_purchase_in_items(id,tenant_id,in_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(604,1,402,101,201,301,2,100,200,'99',0)");
        ErpPurchaseReturnSaveReqVO edit=new ErpPurchaseReturnSaveReqVO();edit.setId(701L);edit.setReturnMode(10);edit.setSupplierId(501L);edit.setDeptId(302L);edit.setReturnTime(cutover.plusDays(2));
        ErpPurchaseReturnSaveReqVO.Item line=new ErpPurchaseReturnSaveReqVO.Item();line.setId(7010L);line.setSourceInItemId(604L);line.setProductId(101L);line.setWarehouseId(201L);line.setCount(BigDecimal.ONE);line.setProductPrice(new BigDecimal("200"));edit.setItems(Collections.singletonList(line));
        CountDownLatch sourceHeld=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Future<?> first=pool.submit(()->{login(1);try{tx.executeWithoutResult(s->{cost.lockBeforeMutation(701L,null);sourceHeld.countDown();await(release);approve(701);});}finally{cleanup();}});
            assertTrue(sourceHeld.await(5,TimeUnit.SECONDS));
            Future<?> second=pool.submit(()->{login(1);try{assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->approval.updatePurchaseReturn(edit));}finally{cleanup();}});
            assertSourceWait();release.countDown();first.get(12,TimeUnit.SECONDS);second.get(12,TimeUnit.SECONDS);
            assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=701",Integer.class));assertEquals(601L,jdbc.queryForObject("SELECT source_in_item_id FROM erp_purchase_return_items WHERE id=7010",Long.class));
            amount("100","SELECT product_price FROM erp_purchase_return_items WHERE id=7010");assertEquals(1,count("erp_stock_record"));
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void disabledModeKeepsLegacyRefundPriceDeductionWithoutAnyNewTable(){
        prepareReturn(701,10,"1");set(costTarget,"enabled",false);set(tradeTarget,"enabled",false);set(writerTarget,"enabled",false);
        List<String> tables=Arrays.asList("erp_purchase_return_posting_link","erp_business_report_item_snapshot","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance");
        List<String> away=new ArrayList<>(),back=new ArrayList<>();for(String table:tables){away.add(table+" TO saved_"+table);back.add("saved_"+table+" TO "+table);}
        jdbc.execute("RENAME TABLE "+String.join(",",away));
        try{approve(701);amount("60","SELECT cost_amount FROM erp_stock WHERE id=11");amount("100","SELECT unit_price FROM erp_stock_record");assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=701",Integer.class));}
        finally{jdbc.execute("RENAME TABLE "+String.join(",",back));}
    }
    @Test void realReturnExcelKeepsRefundAndCostSeparateAndMasksDerivedDifference()throws Exception {
        prepareReturn(701,10,"1");approve(701);
        MockHttpServletResponse response=new MockHttpServletResponse();report.export(false,filter(),"DETAIL",response);
        try(Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))){
            assertEquals(CellType.STRING,excelCell(workbook,"退货含税交易额").getCellType());
            assertEquals("100.000000",excelCell(workbook,"退货含税交易额").getStringCellValue());
            assertEquals("80.000000",excelCell(workbook,"退货财务成本").getStringCellValue());
            assertTrue(excelCell(workbook,"退货差额损益").getStringCellValue().contains("TAX_BASIS_UNCONFIRMED"));
            assertEquals("IN-401",excelCell(workbook,"来源采购入库单").getStringCellValue());
        }
        when(permissions.getCurrentUserHiddenFields(anyString(),eq(301L))).thenReturn(Arrays.asList("costAmount","costPrice"));
        ErpTradeReportModels.Bundle rows=report.page(false,filter(),false),groups=report.page(false,filter(),true);
        for(Map<String,Object> row:Arrays.asList(rows.getPage().getList().get(0),rows.getSummary(),groups.getPage().getList().get(0))){assertNull(row.get("returnFinancialCost"));assertNull(row.get("returnDifferenceProfit"));assertEquals("MASKED",((Map<?,?>)row.get("metricStates")).get("returnDifferenceProfit"));}
        MockHttpServletResponse masked=new MockHttpServletResponse();report.export(false,filter(),"DETAIL",masked);
        try(Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(masked.getContentAsByteArray()))){assertEquals("****",excelCell(workbook,"退货财务成本").getStringCellValue());assertEquals("****",excelCell(workbook,"退货差额损益").getStringCellValue());}
    }
    @Test void emptyOrPurchaseOnlyRangeDoesNotDeclareReturnDifferenceUnknown(){
        Map<String,Object> empty=report.page(false,filter(),false).getSummary();assertNull(empty.get("returnDifferenceProfit"));assertEquals("NOT_APPLICABLE",((Map<?,?>)empty.get("metricStates")).get("returnDifferenceProfit"));
        prepareReturn(701,10,"1");approve(701);ErpTradeReportModels.Filter onlyIn=filter();onlyIn.setBusinessTypes(Collections.singletonList("PURCHASE_IN"));
        Map<String,Object> none=report.page(false,onlyIn,false).getSummary();assertEquals("NOT_APPLICABLE",((Map<?,?>)none.get("metricStates")).get("returnDifferenceProfit"));
        Map<String,Object> includesReturn=report.page(false,filter(),false).getSummary();assertEquals("TAX_BASIS_UNCONFIRMED",((Map<?,?>)includesReturn.get("metricStates")).get("returnDifferenceProfit"));
    }
    @Test void historicalSaleReturnIsTraceOnlyAndDoesNotInventPurchaseSource(){
        jdbc.update("INSERT INTO erp_sale_return(id,tenant_id,no,status,dept_id,customer_id,return_mode,return_time,total_count,total_price,creator,deleted) VALUES(801,1,'SALE-RETURN-801',20,302,901,20,?,1,300,'99',0)",cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_sale_return_items(id,tenant_id,return_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(8010,1,801,101,201,301,1,300,300,'99',0)");
        prepareReturn(701,20,"1");jdbc.update("UPDATE erp_purchase_return_items SET source_sale_return_item_id=8010 WHERE return_id=701");approve(701);
        amount("-80","SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE biz_type=80");amount("100","SELECT unit_price FROM erp_stock_record WHERE biz_type=80");
        assertNull(jdbc.queryForObject("SELECT source_in_id FROM erp_purchase_return_posting_link",Long.class));assertEquals(801L,jdbc.queryForObject("SELECT trace_sale_return_id FROM erp_purchase_return_posting_link",Long.class));
        Map<String,Object> row=report.page(false,filter(),false).getPage().getList().get(0);assertEquals("NO_ORIGINAL_PURCHASE",row.get("sourceRole"));assertNull(row.get("sourcePurchaseInId"));assertEquals(801L,((Number)row.get("traceSaleReturnId")).longValue());assertEquals("CURRENT_AVERAGE",row.get("costBasis"));
    }
    @Test void historicalApprovedReturnWithoutNewLinkStillConsumesOriginalQuantity(){
        prepareReturn(701,10,"1");jdbc.update("UPDATE erp_purchase_return SET status=20 WHERE id=701");
        assertEquals(0,count("erp_purchase_return_posting_link"));prepareReturn(702,10,"2");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->approve(702));
        amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_purchase_return_posting_link"));
        assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=702",Integer.class));
    }
    @Test void twoOriginalReceiptsKeepTheirOwnUpstreamOrderInsteadOfOneReturnHeader(){
        jdbc.update("INSERT INTO erp_purchase_order(id,tenant_id,no,status,supplier_id,dept_id,creator,deleted) VALUES(1001,1,'ORDER-1001',20,501,302,'99',0),(1002,1,'ORDER-1002',20,501,302,'99',0),(9999,1,'RETURN-DOCUMENT-ORDER',20,501,302,'99',0)");
        jdbc.update("UPDATE erp_purchase_in SET order_id=1001,order_no='ORDER-1001' WHERE id=401");jdbc.update("UPDATE erp_purchase_in_items SET order_item_id=1101 WHERE id=601");
        jdbc.update("INSERT INTO erp_purchase_in(id,tenant_id,no,status,supplier_id,dept_id,in_time,total_count,total_product_price,total_price,order_id,order_no,creator,deleted) VALUES(402,1,'IN-402',20,501,302,?,2,200,200,1002,'ORDER-1002','99',0)",cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_purchase_in_items(id,tenant_id,in_id,product_id,warehouse_id,dept_id,count,product_price,total_price,order_item_id,creator,deleted) VALUES(604,1,402,102,201,301,2,100,200,1102,'99',0)");
        prepareReturn(701,10,"1","1");jdbc.update("UPDATE erp_purchase_return_items SET source_in_item_id=604 WHERE id=7011");
        // The return document's own saved order is a separate relationship and must not overwrite origins.
        jdbc.update("UPDATE erp_purchase_return SET order_id=9999,order_no='RETURN-DOCUMENT-ORDER' WHERE id=701");
        approve(701);List<Map<String,Object>> rows=report.page(false,filter(),false).getPage().getList();assertEquals(2,rows.size());
        for(Map<String,Object> row:rows){boolean first=((Number)row.get("productId")).longValue()==101L;assertEquals(first?1001L:1002L,((Number)row.get("sourceOrderId")).longValue());assertEquals(first?1101L:1102L,((Number)row.get("sourceOrderItemId")).longValue());assertEquals(first?"ORDER-1001":"ORDER-1002",row.get("sourceOrderNo"));assertEquals(9999L,((Number)row.get("returnDocumentOrderId")).longValue());}
        assertEquals(9999L,jdbc.queryForObject("SELECT order_id FROM erp_purchase_return WHERE id=701",Long.class));
    }
    @Test void crossWarehouseReturnUsesActualOutboundDimensionCostWithoutRewritingSource(){
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,status,purchase_enabled,creator,deleted) VALUES(202,1,303,'actual return warehouse',0,1,'99',0)");
        jdbc.update("INSERT INTO erp_stock(id,tenant_id,product_id,warehouse_id,dept_id,count,cost_price,cost_amount,creator,deleted) VALUES(14,1,101,202,303,2,60,120,'99',0)");
        set(writerTarget,"enabled",false);writer.confirmOpening(101,202,new BigDecimal("2"),new BigDecimal("120"),new BigDecimal("140"),cutover,"second warehouse verified opening",99);set(writerTarget,"enabled",true);
        prepareReturn(701,10,"1");jdbc.update("UPDATE erp_purchase_return_items SET warehouse_id=202,dept_id=303 WHERE id=7010");approve(701);
        amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");amount("60","SELECT cost_amount FROM erp_stock WHERE id=14");
        amount("60","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=14");amount("70","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=14");
        assertEquals(202L,jdbc.queryForObject("SELECT warehouse_id FROM erp_stock_dual_cost_posting WHERE biz_type=80",Long.class));assertEquals(601L,jdbc.queryForObject("SELECT source_biz_item_id FROM erp_stock_dual_cost_posting WHERE biz_type=80",Long.class));
    }
    @Test void identicalPreparedActionRetryDoesNotDuplicateActualLegacyOrSourceConsumption(){
        prepareReturn(701,10,"1");tx.executeWithoutResult(s->{approve(701);assertNotNull(lastRealStockRequest);stockRecords.createStockRecord(lastRealStockRequest);});
        amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_stock_dual_cost_posting"));assertEquals(1,count("erp_purchase_return_posting_link"));
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->stockRecords.createStockRecord(lastRealStockRequest)));
    }
    Cell excelCell(Workbook book,String label){Sheet sheet=book.getSheetAt(0);Row head=sheet.getRow(0);for(Cell cell:head)if(label.equals(cell.getStringCellValue()))return sheet.getRow(1).getCell(cell.getColumnIndex());throw new AssertionError("Missing Excel column "+label);}
    @Test void refundBusinessRoundingRemainsTwoPlacesWhileInventoryUsesCurrentCost(){
        prepareReturn(701,10,"1");jdbc.update("UPDATE erp_purchase_return_items SET product_price=100.005,total_price=100.01 WHERE return_id=701");
        jdbc.update("UPDATE erp_purchase_return SET total_product_price=100.01,total_price=100.01 WHERE id=701");approve(701);
        amount("100.005","SELECT unit_price FROM erp_stock_record");amount("-100.01","SELECT total_price FROM erp_stock_record");
        amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");amount("-80","SELECT financial_movement FROM erp_stock_dual_cost_posting");
    }
    @Test void twoOriginalSourcesWithOppositeLineOrderSerializeAndRejectOverReturn()throws Exception {
        jdbc.update("UPDATE erp_purchase_in_items SET count=1,total_price=100 WHERE id=601");
        jdbc.update("INSERT INTO erp_purchase_in(id,tenant_id,no,status,supplier_id,dept_id,in_time,total_count,total_product_price,total_price,creator,deleted) VALUES(402,1,'IN-402',20,501,302,?,1,100,100,'99',0)",cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_purchase_in_items(id,tenant_id,in_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(604,1,402,102,201,301,1,100,100,'99',0)");
        prepareReturn(701,10,"1","1");prepareReturn(702,10,"1","1");
        jdbc.update("UPDATE erp_purchase_return_items SET source_in_item_id=604 WHERE id=7011");
        jdbc.update("UPDATE erp_purchase_return_items SET product_id=102,source_in_item_id=604 WHERE id=7020");
        jdbc.update("UPDATE erp_purchase_return_items SET product_id=101,source_in_item_id=601 WHERE id=7021");
        CountDownLatch go=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try{List<Future<Boolean>> results=new ArrayList<>();for(long id:new long[]{701,702})results.add(pool.submit(()->{login(1);go.await();try{approve(id);return true;}catch(cn.iocoder.yudao.framework.common.exception.ServiceException expected){return false;}finally{cleanup();}}));
            go.countDown();int successes=0;for(Future<Boolean> result:results)if(result.get(15,TimeUnit.SECONDS))successes++;assertEquals(1,successes);
            amount("1","SELECT count FROM erp_stock WHERE id=11");amount("1","SELECT count FROM erp_stock WHERE id=12");assertEquals(2,count("erp_stock_record"));assertEquals(2,count("erp_purchase_return_posting_link"));
        }finally{pool.shutdownNow();}
    }
    @Test void actualSaleReturnTransferCreatesTraceableDraftWithoutPostingStock(){
        jdbc.update("INSERT INTO erp_sale_return(id,tenant_id,no,status,dept_id,customer_id,return_mode,return_time,total_count,total_price,creator,deleted) VALUES(801,1,'SALE-RETURN-801',20,302,901,20,?,1,300,'99',0)",cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_sale_return_items(id,tenant_id,return_id,product_id,product_unit_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(8010,1,801,101,801,201,301,1,300,300,'99',0)");
        cn.iocoder.yudao.module.erp.service.product.ErpProductService products=mock(cn.iocoder.yudao.module.erp.service.product.ErpProductService.class);
        ErpProductDO sourceProduct=((ErpProductMapper)actual.get(ErpProductMapper.class)).selectById(101L);
        when(products.validProductList(any())).thenReturn(Collections.singletonList(sourceProduct));set(approvalTarget,"productService",products);
        cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO numbers=mock(cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO.class);when(numbers.generate(anyString())).thenReturn("RETURN-TRANSFER-801");set(approvalTarget,"noRedisDAO",numbers);
        actual.put(ErpPurchaseReturnService.class,approval);
        cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnServiceImpl sourceTarget=new cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnServiceImpl();inject(sourceTarget);
        cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService source=proxy(sourceTarget);
        cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreatePurchaseReturnReqVO request=new cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreatePurchaseReturnReqVO();
        request.setReturnId(801L);request.setSupplierId(501L);request.setDeptId(302L);
        cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreatePurchaseReturnReqVO.Item item=new cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreatePurchaseReturnReqVO.Item();item.setSourceSaleReturnItemId(8010L);item.setCount(BigDecimal.ONE);item.setProductPrice(new BigDecimal("100"));request.setItems(Collections.singletonList(item));
        Object previousBeanFactory=ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class,"beanFactory");
        org.springframework.beans.factory.support.DefaultListableBeanFactory localBeans=new org.springframework.beans.factory.support.DefaultListableBeanFactory();localBeans.registerSingleton("dataSource",jdbc.getDataSource());
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(localBeans);
        try {
        Long id=source.createPurchaseReturnFromSaleReturn(request).getId();assertNotNull(id);
        assertEquals(0,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=?",Integer.class,id));assertEquals(20,jdbc.queryForObject("SELECT return_mode FROM erp_purchase_return WHERE id=?",Integer.class,id));
        assertEquals(8010L,jdbc.queryForObject("SELECT source_sale_return_item_id FROM erp_purchase_return_items WHERE return_id=?",Long.class,id));assertNull(jdbc.queryForObject("SELECT source_in_item_id FROM erp_purchase_return_items WHERE return_id=?",Long.class,id));
        amount("100","SELECT product_price FROM erp_purchase_return_items WHERE return_id="+id);amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_purchase_return_posting_link"));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->source.createPurchaseReturnFromSaleReturn(request));assertEquals(1,count("erp_purchase_return"));
        } finally {ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class,"beanFactory",previousBeanFactory);}
    }
    void await(CountDownLatch latch){try{if(!latch.await(10,TimeUnit.SECONDS))throw new IllegalStateException("fixture release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
    @Test void batchDepartmentMutationWaitingForApprovalCannotChangePostedRows()throws Exception {
        cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemBatchUpdateReqVO request=new cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemBatchUpdateReqVO();request.setReturnId(701L);request.setItemIds(Collections.singletonList(7010L));request.setDeptId(999L);
        assertApprovalWinsMutation(()->approval.batchUpdatePurchaseReturnItems(request));
    }
    @Test void deletionWaitingForApprovalCannotDeletePostedDocument()throws Exception {
        assertApprovalWinsMutation(()->approval.deletePurchaseReturn(Collections.singletonList(701L)));
    }
    void assertApprovalWinsMutation(Runnable mutation)throws Exception {
        prepareReturn(701,10,"1");CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Future<?> approving=pool.submit(()->{login(1);try{tx.executeWithoutResult(s->{cost.lockBeforeMutation(701L,null);held.countDown();await(release);approve(701);});}finally{cleanup();}});
            assertTrue(held.await(5,TimeUnit.SECONDS));
            Future<?> editing=pool.submit(()->{login(1);try{assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,mutation::run);}finally{cleanup();}});
            assertSourceWait();release.countDown();approving.get(12,TimeUnit.SECONDS);editing.get(12,TimeUnit.SECONDS);
            assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_purchase_return WHERE id=701 AND deleted=0",Integer.class));assertEquals(301L,jdbc.queryForObject("SELECT dept_id FROM erp_purchase_return_items WHERE id=7010 AND deleted=0",Long.class));
            amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_purchase_return_posting_link"));
        }finally{release.countDown();pool.shutdownNow();}
    }
    void assertSourceWait()throws Exception {long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<deadline){int count=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON w.REQUESTING_ENGINE_LOCK_ID=l.ENGINE_LOCK_ID WHERE l.OBJECT_SCHEMA='report_purchase_return_cost_test' AND l.OBJECT_NAME='erp_purchase_in'",Integer.class);if(count>0)return;Thread.sleep(20);}fail("second actual business method must wait for the source parent row");}
}
