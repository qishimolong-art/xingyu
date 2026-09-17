package cn.iocoder.yudao.module.erp.service.sale.returncost;
import cn.iocoder.yudao.module.erp.service.purchase.returncost.*;
import cn.iocoder.yudao.module.erp.service.sale.*;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.Header;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.ConfirmRequest;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.ConfirmItem;

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

/** 00G independent fixture preparation: actual sale-return approval and both real inventory services.
 * Peripheral product sync and descriptive snapshot enrichment may be mocked; inventory writes are real. */
@EnabledIfEnvironmentVariable(named="ERP_SALE_RETURN_CURRENT_COST_MYSQL_TEST_URL",matches=".+")
class ErpSaleReturnCurrentCostMysqlIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    TransactionTemplate tx;
    final LocalDateTime cutover=LocalDateTime.of(2026,1,1,0,0);
    final Map<Class<?>,Object> actual=new LinkedHashMap<>();
    ErpSaleReturnService approval;
    ErpSaleReturnServiceImpl approvalTarget;
    ErpPurchaseReturnCostService cost,costTarget;
    ErpTradeSnapshotService trade,tradeTarget;
    ErpDualCostPostingService writer,writerTarget;
    ErpStockRecordService stockRecords;
    ErpStockService stockService;
    ErpTradeReportService report;
    ErpSaleReturnCurrentCostService current,currentTarget;
    PermissionApi permissions;
    DeptDataPermissionRespDTO documentPermission,stockPermission;
    volatile Long failAfterStockItem;
    boolean checkSuspendedLegacyCallback;
    ErpStockRecordCreateReqBO lastRealStockRequest;

    @BeforeEach void setup() throws Exception {
        String url=System.getenv("ERP_SALE_RETURN_CURRENT_COST_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_sale_return_current_cost_test(?:\\?.*)?"));
        DriverManagerDataSource ds=new DriverManagerDataSource(url,"root","");jdbc=new JdbcTemplate(ds);
        assertTrue(jdbc.queryForObject("SELECT @@datadir",String.class).replace('\\','/').contains("/Temp/xingyu-report-mysql-"));
        manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);
        // This named private fixture is reset before migrations; obsolete unregistered evidence is tested separately.
        for(String table:Arrays.asList("erp_sale_return_current_cost_posting_link","erp_sale_return_current_cost_line","erp_sale_return_current_cost_confirmation","erp_sale_return_current_cost_state"))if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",Integer.class,table)>0)jdbc.execute("DELETE FROM "+table);
        for(Class<?> type:Arrays.asList(ErpPurchaseReturnDO.class,ErpPurchaseReturnItemDO.class,ErpPurchaseInDO.class,ErpPurchaseInItemDO.class,ErpPurchaseOrderDO.class,ErpPurchaseOrderItemDO.class,ErpCustomerDO.class,ErpCustomerDeptDO.class,ErpSaleOutDO.class,ErpSaleReturnDO.class,ErpSaleReturnItemDO.class,ErpSaleOutItemDO.class,ErpStockDO.class,ErpStockRecordDO.class,ErpWarehouseDO.class,ErpProductDO.class,ErpSupplierDO.class))createEntityTable(type);
        // Match the existing erp_stock_cost_v1.sql storage precision, not the six-place new ledger.
        jdbc.execute("ALTER TABLE erp_stock MODIFY cost_amount DECIMAL(20,4), MODIFY cost_price DECIMAL(16,6)");
        jdbc.execute("ALTER TABLE erp_stock_record MODIFY cost_amount DECIMAL(20,4), MODIFY total_price DECIMAL(20,4), MODIFY cost_price DECIMAL(16,6), MODIFY unit_price DECIMAL(16,6)");
        for(String script:Arrays.asList("erp_report_dual_cost_foundation_20260909.sql","erp_trade_snapshot_v224.sql","erp_report_source_item_lock_index_20260909.sql","erp_purchase_return_current_cost_20260909.sql","erp_purchase_return_source_lock_index_20260909.sql","erp_stock_dimension_lookup_index_20260909.sql","erp_sale_return_original_cost_v225.sql","erp_sale_return_current_cost_20260909.sql"))executeDdl(script);
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_product_unit(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_dept(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,nickname VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for(String table:Arrays.asList("erp_sale_return_current_cost_state","erp_sale_return_current_cost_posting_link","erp_sale_return_current_cost_line","erp_sale_return_current_cost_confirmation","erp_sale_return_cost_allocation","erp_sale_return_cost_progress","erp_purchase_return_posting_link","erp_business_report_item_snapshot","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance","erp_stock_record","erp_purchase_return_items","erp_purchase_return","erp_purchase_in_items","erp_purchase_in","erp_purchase_order_items","erp_purchase_order","erp_sale_return_items","erp_sale_return","erp_sale_out_items","erp_sale_out","erp_customer_dept","erp_customer","erp_stock","erp_warehouse","erp_product","erp_supplier","erp_product_unit"))jdbc.execute("DELETE FROM "+table);
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,status,purchase_enabled,sale_enabled,creator,deleted) VALUES(201,1,301,'return warehouse',0,1,1,'99',0)");
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
        List<Class<?>> mappers=Arrays.asList(ErpPurchaseReturnMapper.class,ErpPurchaseReturnItemMapper.class,ErpPurchaseInMapper.class,ErpPurchaseInItemMapper.class,ErpPurchaseOrderMapper.class,ErpPurchaseOrderItemMapper.class,ErpCustomerMapper.class,ErpSaleOutMapper.class,ErpSaleOutItemMapper.class,ErpSaleReturnMapper.class,ErpSaleReturnItemMapper.class,ErpStockMapper.class,ErpStockRecordMapper.class,ErpWarehouseMapper.class,ErpProductMapper.class,ErpSupplierMapper.class);
        for(Class<?> type:mappers)factory.getConfiguration().addMapper(type);
        SqlSessionTemplate template=new SqlSessionTemplate(factory);for(Class<?> type:mappers)actual.put(type,template.getMapper(type));
        ErpWarehouseServiceImpl warehouseTarget=new ErpWarehouseServiceImpl();inject(warehouseTarget);actual.put(ErpWarehouseService.class,proxy(warehouseTarget));
        ErpDualCostLedgerRepository ledger=new ErpDualCostLedgerRepository();inject(ledger);actual.put(ErpDualCostLedgerRepository.class,ledger);
        ErpCustomerServiceImpl customerTarget=new ErpCustomerServiceImpl();inject(customerTarget);actual.put(ErpCustomerService.class,proxy(customerTarget));
        ErpSaleReturnCurrentCostRepository currentRepository=new ErpSaleReturnCurrentCostRepository();inject(currentRepository);actual.put(ErpSaleReturnCurrentCostRepository.class,currentRepository);
        currentTarget=new ErpSaleReturnCurrentCostService();inject(currentTarget);set(currentTarget,"enabled",true);set(currentTarget,"cutover",cutover.toString());current=proxy(currentTarget);actual.put(ErpSaleReturnCurrentCostService.class,current);
        tradeTarget=new ErpTradeSnapshotService();inject(tradeTarget);set(tradeTarget,"enabled",true);trade=proxy(tradeTarget);actual.put(ErpTradeSnapshotService.class,trade);
        ErpPurchaseReturnCostRepository costRepository=new ErpPurchaseReturnCostRepository();inject(costRepository);actual.put(ErpPurchaseReturnCostRepository.class,costRepository);
        costTarget=new ErpPurchaseReturnCostService();inject(costTarget);set(costTarget,"enabled",true);cost=proxy(costTarget);actual.put(ErpPurchaseReturnCostService.class,cost);
        ErpSaleReturnCostRepository originalRepository=new ErpSaleReturnCostRepository();inject(originalRepository);actual.put(ErpSaleReturnCostRepository.class,originalRepository);
        ErpSaleReturnCostService originalTarget=new ErpSaleReturnCostService();inject(originalTarget);set(originalTarget,"enabled",true);actual.put(ErpSaleReturnCostService.class,proxy(originalTarget));
        // Register the actual no-source preview/confirmation service here after its interface freezes.
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
        approvalTarget=new ErpSaleReturnServiceImpl();inject(approvalTarget);approval=proxy(approvalTarget);
        ErpTradeReportRepository reports=new ErpTradeReportRepository();inject(reports);actual.put(ErpTradeReportRepository.class,reports);
        ErpTradeReportService reportTarget=new ErpTradeReportService();inject(reportTarget);set(reportTarget,"enabled",true);set(reportTarget,"cutover",cutover.toString());report=proxy(reportTarget);
        jdbc.update("INSERT INTO erp_customer(id,tenant_id,name,status,creator,deleted) VALUES(901,1,'return customer',0,'99',0)");
        login(1);
        IsolatedStockCursorMigration.apply(jdbc,"report_sale_return_current_cost_test");
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
    // Prepared against the shared interface; execute only after the authors freeze their implementation.
    void prepareNoSourceReturn(long id,String... quantities){
        BigDecimal total=Arrays.stream(quantities).map(BigDecimal::new).reduce(BigDecimal.ZERO,BigDecimal::add);
        jdbc.update("INSERT INTO erp_sale_return(id,tenant_id,no,status,customer_id,dept_id,return_mode,return_time,total_count,total_product_price,total_price,discount_price,refund_price,creator,deleted) VALUES(?,1,?,10,901,302,20,?,?,?,?,0,0,'99',0)",id,"NO-SOURCE-"+id,cutover.plusDays(2),total,total.multiply(new BigDecimal("100")),total.multiply(new BigDecimal("100")));
        for(int n=0;n<quantities.length;n++){BigDecimal quantity=new BigDecimal(quantities[n]);jdbc.update("INSERT INTO erp_sale_return_items(id,tenant_id,return_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(?,1,?,?,201,301,?,100,?,'99',0)",id*10+n,id,101L+n,quantity,quantity.multiply(new BigDecimal("100")));}
    }
    void amount(String expected,String sql){BigDecimal value=jdbc.queryForObject(sql,BigDecimal.class);assertNotNull(value);assertEquals(0,new BigDecimal(expected).compareTo(value),sql);}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    Header preview(long id){return current.preview(id);}
    void approve(long id,String basis){approval.updateSaleReturnStatusWithCostBasis(id,20,basis);}
    ConfirmRequest confirmation(long id,Header basis,String key,String financial,String settlement){
        ConfirmRequest request=new ConfirmRequest();request.setId(id);request.setExpectedSourceSignature(basis.getSourceSignature());request.setExpectedBasisSignature(basis.getBasisSignature());request.setExpectedRevision(basis.getLatestRevision());request.setRequestKey(key);request.setEvidence("independently checked original cost evidence");
        List<ConfirmItem> items=new ArrayList<>();for(Row row:current.itemPage(id,basis.getSourceSignature(),basis.getBasisSignature(),1,200).getList()){ConfirmItem item=new ConfirmItem();item.setSourceItemId(row.getSourceItemId());item.setFinancialAmount(new BigDecimal(financial));item.setSettlementAmount(new BigDecimal(settlement));item.setEvidence("explicit independent financial and department amounts");items.add(item);}request.setItems(items);return request;
    }
    ErpTradeReportModels.Filter filter(){ErpTradeReportModels.Filter f=new ErpTradeReportModels.Filter();f.setPostedFrom(cutover);f.setPostedTo(LocalDateTime.now().plusDays(1));return f;}
    @Test void currentEightyAndNinetyReturnTwoKeepRefundTwoHundredAndNoOriginalSource(){
        prepareNoSourceReturn(701,"2");Header basis=preview(701);assertEquals("READY",basis.getStatus());assertTrue(basis.isCanApprove());assertEquals(0,new BigDecimal("160").compareTo(basis.getFinancialAmount()));assertEquals(0,new BigDecimal("180").compareTo(basis.getSettlementAmount()));approve(701,basis.getBasisSignature());
        amount("4","SELECT count FROM erp_stock WHERE id=11");amount("320","SELECT cost_amount FROM erp_stock WHERE id=11");amount("320","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("360","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");
        amount("200","SELECT total_price FROM erp_stock_record");amount("160","SELECT financial_movement FROM erp_stock_dual_cost_posting");amount("180","SELECT settlement_movement FROM erp_stock_dual_cost_posting");
        assertNull(jdbc.queryForObject("SELECT reversal_posting_id FROM erp_stock_dual_cost_posting",Long.class));assertNull(jdbc.queryForObject("SELECT source_biz_id FROM erp_stock_dual_cost_posting",Long.class));assertEquals(0,count("erp_sale_return_cost_allocation"));assertEquals(0,count("erp_sale_return_cost_progress"));assertEquals(0,count("erp_sale_return_current_cost_confirmation"));
        Map<String,Object> row=report.page(true,filter(),false).getPage().getList().get(0);assertEquals("NO_ORIGINAL_SALE",row.get("sourceRole"));assertNull(row.get("originalPostingId"));assertNull(row.get("saleUserId"));assertTrue(row.containsKey("financialGrossProfit"));assertNull(row.get("financialGrossProfit"));assertEquals("TAX_BASIS_UNCONFIRMED",((Map<?,?>)row.get("metricStates")).get("financialGrossProfit"));assertEquals("200.000000",row.get("returnGrossAmount"));
    }
    @Test void manualEmptyInventoryAllowsExplicitZeroAndTwoIndependentAmounts(){
        jdbc.update("UPDATE erp_stock SET count=0,cost_price=0,cost_amount=0 WHERE id=11");jdbc.update("UPDATE erp_stock_dual_cost_balance SET quantity=0,financial_amount=0,settlement_amount=0,legacy_cost_price=0,legacy_cost_amount=0 WHERE stock_id=11");
        prepareNoSourceReturn(701,"2");Header before=preview(701);assertEquals("CONFIRMATION_REQUIRED",before.getStatus());assertNull(before.getFinancialAmount());assertFalse(before.isCanApprove());
        Header confirmed=current.confirm(confirmation(701,before,"zero-financial", "0","180"));assertEquals("READY",confirmed.getStatus());approve(701,confirmed.getBasisSignature());
        amount("2","SELECT count FROM erp_stock WHERE id=11");amount("0","SELECT cost_amount FROM erp_stock WHERE id=11");amount("0","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("180","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("200","SELECT total_price FROM erp_stock_record");assertNotNull(jdbc.queryForObject("SELECT consumed_at FROM erp_sale_return_current_cost_confirmation",java.sql.Timestamp.class));
    }
    @Test void missingOrStaleBalanceCannotBeRepairedByEnteringAPrice(){
        prepareNoSourceReturn(701,"1");jdbc.update("DELETE FROM erp_stock_dual_cost_balance WHERE stock_id=11");Header missing=preview(701);assertEquals("STOCK_RECONCILIATION_REQUIRED",missing.getStatus());assertFalse(missing.isCanConfirm());assertFalse(missing.isCanApprove());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->current.confirm(confirmation(701,missing,"cannot-replace-opening","80","90")));
        assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_sale_return_current_cost_confirmation"));
    }
    @Test void signedPreviewBecomesInvalidAfterAnotherApprovedReturn(){
        prepareNoSourceReturn(701,"1");prepareNoSourceReturn(702,"1");Header first=preview(701),second=preview(702);approve(701,first.getBasisSignature());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->approve(702,second.getBasisSignature()));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=702",Integer.class));
        Header refreshed=preview(702);assertNotEquals(second.getBasisSignature(),refreshed.getBasisSignature());approve(702,refreshed.getBasisSignature());amount("320","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(2,count("erp_stock_record"));
    }
    @Test void secondRealLegacyStockUpdateFailureRollsBackApprovalAndManualConsumption(){
        prepareNoSourceReturn(701,"1","1");Header basis=preview(701);Header confirmed=current.confirm(confirmation(701,basis,"whole-order-rollback","80","90"));failAfterStockItem=7011L;
        assertThrows(RuntimeException.class,()->approve(701,confirmed.getBasisSignature()));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));
        amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");amount("140","SELECT cost_amount FROM erp_stock WHERE id=12");assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));assertEquals(0,count("erp_business_report_item_snapshot"));assertEquals(0,count("erp_sale_return_current_cost_posting_link"));assertNull(jdbc.queryForObject("SELECT consumed_at FROM erp_sale_return_current_cost_confirmation",java.sql.Timestamp.class));
    }
    @Test void sourceAndOrderReferencesCannotMasqueradeAsNoOriginalSale(){
        prepareNoSourceReturn(701,"1");jdbc.update("UPDATE erp_sale_return SET source_out_id=401 WHERE id=701");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->preview(701));
        jdbc.update("UPDATE erp_sale_return SET source_out_id=NULL,order_id=402 WHERE id=701");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->preview(701));
        jdbc.update("UPDATE erp_sale_return SET order_id=NULL WHERE id=701");jdbc.update("UPDATE erp_sale_return_items SET source_out_item_id=601 WHERE id=7010");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->preview(701));
        assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_sale_return_cost_allocation"));
    }
    @Test void identicalConfirmationRequestIsIdempotentButDifferentPayloadConflicts(){
        prepareNoSourceReturn(701,"1");Header basis=preview(701);ConfirmRequest request=confirmation(701,basis,"same-key","81","91");Header first=current.confirm(request),repeated=current.confirm(request);assertEquals(first.getConfirmationId(),repeated.getConfirmationId());assertEquals(1,count("erp_sale_return_current_cost_confirmation"));
        request.getItems().get(0).setFinancialAmount(new BigDecimal("82"));assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->current.confirm(request));amount("81","SELECT financial_amount FROM erp_sale_return_current_cost_line");
    }
    @Test void rowChangesAfterPreviewPreventApprovalAndPartialConfirmation(){
        prepareNoSourceReturn(701,"1","1");Header basis=preview(701);ConfirmRequest partial=confirmation(701,basis,"partial","80","90");partial.setItems(Collections.singletonList(partial.getItems().get(0)));assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->current.confirm(partial));
        jdbc.update("UPDATE erp_sale_return_items SET count=2,total_price=200 WHERE id=7010");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->approve(701,basis.getBasisSignature()));assertEquals(0,count("erp_stock_record"));
    }
    @Test void noSourceReturnQuantityCanExceedCurrentStockWithoutOriginalSaleLimit(){
        prepareNoSourceReturn(701,"5");Header basis=preview(701);approve(701,basis.getBasisSignature());amount("7","SELECT count FROM erp_stock WHERE id=11");amount("400","SELECT financial_movement FROM erp_stock_dual_cost_posting");amount("450","SELECT settlement_movement FROM erp_stock_dual_cost_posting");amount("500","SELECT total_price FROM erp_stock_record");
    }
    @Test void legacyAuthenticationIsSuspendedForRequiresNewAndRestoresOuterPosting(){
        prepareNoSourceReturn(701,"1");checkSuspendedLegacyCallback=true;approve(701,preview(701).getBasisSignature());assertFalse(checkSuspendedLegacyCallback);amount("240","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(1,count("erp_stock_record"));assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
    }
    @Test void twoRowsInSameStockUseCumulativeExactAmountsInsteadOfRoundedUnitPrice(){
        jdbc.update("UPDATE erp_stock SET count=3,cost_amount=650,cost_price=216.666667 WHERE id=11");jdbc.update("UPDATE erp_stock_dual_cost_balance SET quantity=3,financial_amount=650,settlement_amount=720,legacy_cost_price=216.666667,legacy_cost_amount=650 WHERE stock_id=11");
        prepareNoSourceReturn(701,"1","2");jdbc.update("UPDATE erp_sale_return_items SET product_id=101 WHERE id=7011");Header basis=preview(701);approve(701,basis.getBasisSignature());
        amount("216.666667","SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE biz_item_id=7010");amount("433.333333","SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE biz_item_id=7011");amount("650","SELECT SUM(financial_movement) FROM erp_stock_dual_cost_posting");
        amount("1300","SELECT cost_amount FROM erp_stock WHERE id=11");amount("1300","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("1440","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("6","SELECT count FROM erp_stock WHERE id=11");
    }
    @Test void twoPlaceRefundDoesNotChangeFourPlaceLegacyAndSixPlaceNewCost(){
        prepareNoSourceReturn(701,"1");jdbc.update("UPDATE erp_sale_return_items SET product_price=100.005,total_price=100.01 WHERE id=7010");jdbc.update("UPDATE erp_sale_return SET total_product_price=100.01,total_price=100.01 WHERE id=701");approve(701,preview(701).getBasisSignature());
        amount("100.005","SELECT unit_price FROM erp_stock_record");amount("100.01","SELECT total_price FROM erp_stock_record");amount("240","SELECT cost_amount FROM erp_stock WHERE id=11");amount("80","SELECT financial_movement FROM erp_stock_dual_cost_posting");
    }
    @Test void actualTenantDocumentAndStockCreatorPermissionsGuardPreview(){
        prepareNoSourceReturn(701,"1");login(2);assertThrows(RuntimeException.class,()->preview(701));login(1);
        documentPermission.setAll(false);documentPermission.setDeptIds(Collections.singleton(999L));assertThrows(RuntimeException.class,()->preview(701));documentPermission.setAll(true);
        stockPermission.setAll(false);stockPermission.setSelf(true);jdbc.update("UPDATE erp_stock SET creator='88' WHERE id=11");assertThrows(RuntimeException.class,()->preview(701));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_sale_return_current_cost_confirmation"));
    }
    @Test void hiddenCostCannotLeakInPreviewRowsOrAuthorizeManualConfirmation(){
        prepareNoSourceReturn(701,"1");Header initial=preview(701);ConfirmRequest request=confirmation(701,initial,"hidden-manual","80","90");
        when(permissions.getCurrentUserHiddenFields(eq("erp_stock"),eq(301L))).thenReturn(Collections.singletonList("costAmount"));Header hidden=preview(701);assertTrue(hidden.isCostMasked());assertNull(hidden.getFinancialAmount());assertNull(hidden.getSettlementAmount());assertFalse(hidden.isCanConfirm());assertFalse(hidden.isCanApprove());
        Row row=current.itemPage(701L,hidden.getSourceSignature(),hidden.getBasisSignature(),1,20).getList().get(0);assertNull(row.getFinancialAmount());assertNull(row.getSettlementAmount());assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->current.confirm(request));assertEquals(0,count("erp_sale_return_current_cost_confirmation"));
    }
    @Test void staleStockTakesPriorityOverInvalidManualConfirmation(){
        prepareNoSourceReturn(701,"1");Header basis=preview(701);current.confirm(confirmation(701,basis,"previous-confirmed","81","91"));jdbc.update("UPDATE erp_stock SET cost_amount=170,cost_price=85 WHERE id=11");
        Header stale=preview(701);assertEquals("STOCK_RECONCILIATION_REQUIRED",stale.getStatus());assertFalse(stale.isCanConfirm());assertNull(stale.getFinancialAmount());assertFalse(stale.isCanApprove());assertEquals(0,count("erp_stock_record"));
    }
    @Test void postedHistoryUsesRecordedCostDespiteLaterStockChanges(){
        prepareNoSourceReturn(701,"1");approve(701,preview(701).getBasisSignature());jdbc.update("UPDATE erp_stock SET cost_price=999,cost_amount=2997 WHERE id=11");
        Header history=preview(701);assertEquals("POSTED",history.getStatus());assertFalse(history.isCanConfirm());assertFalse(history.isCanApprove());assertEquals(0,new BigDecimal("80").compareTo(history.getFinancialAmount()));assertEquals(0,new BigDecimal("90").compareTo(history.getSettlementAmount()));
        Row row=current.itemPage(701L,history.getSourceSignature(),history.getBasisSignature(),1,20).getList().get(0);assertEquals(0,new BigDecimal("80").compareTo(row.getFinancialAmount()));
    }
    @Test void futureCutoverAndMissingSchemaNeverEnableSilentLegacyApproval(){
        prepareNoSourceReturn(701,"1");set(currentTarget,"cutover",LocalDateTime.now().plusYears(1).withNano(0).toString());assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->preview(701));set(currentTarget,"cutover",cutover.toString());
        jdbc.execute("RENAME TABLE erp_sale_return_current_cost_posting_link TO saved_current_cost_link");try{Header unavailable=preview(701);assertEquals("SCHEMA_MISSING",unavailable.getStatus());assertFalse(unavailable.isCanApprove());assertThrows(RuntimeException.class,()->approval.updateSaleReturnStatus(701L,20));assertEquals(0,count("erp_stock_record"));}finally{jdbc.execute("RENAME TABLE saved_current_cost_link TO erp_sale_return_current_cost_posting_link");}
    }
    @Test void disabledModeRequiresNoCurrentCostTablesAndRetainsOldRefundPriceAlgorithm()throws Exception {
        prepareNoSourceReturn(701,"1");set(currentTarget,"enabled",false);set(writerTarget,"enabled",false);set(tradeTarget,"enabled",false);set(actual.get(ErpSaleReturnCostService.class),"enabled",false);
        List<String> tables=Arrays.asList("erp_sale_return_current_cost_state","erp_sale_return_current_cost_confirmation","erp_sale_return_current_cost_line","erp_sale_return_current_cost_posting_link","erp_stock_dual_cost_balance","erp_stock_dual_cost_posting","erp_business_report_item_snapshot");List<String> away=new ArrayList<>(),back=new ArrayList<>();for(String table:tables){away.add(table+" TO saved_"+table);back.add("saved_"+table+" TO "+table);}jdbc.execute("RENAME TABLE "+String.join(",",away));
        jdbc.execute("DROP TRIGGER erp_stock_record_cursor_ai");jdbc.execute("DROP TRIGGER erp_stock_record_cursor_bu");jdbc.execute("ALTER TABLE erp_stock DROP COLUMN legacy_record_cursor_id");
        try{assertEquals("DISABLED",preview(701).getStatus());approval.updateSaleReturnStatus(701L,20);amount("260","SELECT cost_amount FROM erp_stock WHERE id=11");amount("100","SELECT total_price FROM erp_stock_record");}finally{jdbc.execute("RENAME TABLE "+String.join(",",back));IsolatedStockCursorMigration.apply(jdbc,"report_sale_return_current_cost_test");}
    }
    @Test void twoOldSignaturesCompeteThenLoserRefreshesWithoutDuplicatePosting()throws Exception {
        prepareNoSourceReturn(701,"1");prepareNoSourceReturn(702,"1");Map<Long,String> signatures=new HashMap<>();signatures.put(701L,preview(701).getBasisSignature());signatures.put(702L,preview(702).getBasisSignature());
        CountDownLatch go=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);List<Long> failed=new ArrayList<>();
        try{Map<Long,Future<Boolean>> results=new LinkedHashMap<>();for(long id:new long[]{701,702})results.put(id,pool.submit(()->{login(1);go.await();try{approve(id,signatures.get(id));return true;}catch(cn.iocoder.yudao.framework.common.exception.ServiceException expected){return false;}finally{cleanup();}}));go.countDown();for(Map.Entry<Long,Future<Boolean>> result:results.entrySet())if(!result.getValue().get(15,TimeUnit.SECONDS))failed.add(result.getKey());assertEquals(1,failed.size());assertEquals(1,count("erp_stock_record"));
            long loser=failed.get(0);approve(loser,preview(loser).getBasisSignature());assertEquals(2,count("erp_stock_record"));amount("320","SELECT cost_amount FROM erp_stock WHERE id=11");
        }finally{pool.shutdownNow();}
    }
    @Test void oldReadSnapshotAfterParentWaitSeesConfirmationLinesAndIdempotentRequest()throws Exception {
        prepareNoSourceReturn(701,"1");Header basis=preview(701);ConfirmRequest request=confirmation(701,basis,"waiting-same-key","81","91");
        CountDownLatch held=new CountDownLatch(1),oldSnapshot=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Future<Header> first=pool.submit(()->{login(1);try{return tx.execute(s->{((ErpSaleReturnMapper)actual.get(ErpSaleReturnMapper.class)).selectByIdForUpdate(701L);held.countDown();await(oldSnapshot);Header result=current.confirm(request);await(release);return result;});}finally{cleanup();}});assertTrue(held.await(5,TimeUnit.SECONDS));
            Future<Header> second=pool.submit(()->{login(1);try{return tx.execute(s->{jdbc.queryForObject("SELECT COUNT(*) FROM erp_sale_return_current_cost_confirmation",Integer.class);oldSnapshot.countDown();return current.confirm(request);});}finally{cleanup();}});
            assertParentWait();release.countDown();Header one=first.get(12,TimeUnit.SECONDS),two=second.get(12,TimeUnit.SECONDS);assertEquals(one.getConfirmationId(),two.getConfirmationId());assertEquals(0,new BigDecimal("81").compareTo(two.getFinancialAmount()));assertEquals(1,count("erp_sale_return_current_cost_confirmation"));assertEquals(1,count("erp_sale_return_current_cost_line"));
        }finally{release.countDown();oldSnapshot.countDown();pool.shutdownNow();}
    }
    void await(CountDownLatch latch){try{if(!latch.await(10,TimeUnit.SECONDS))throw new IllegalStateException("fixture release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
    @Test void confirmationRequiresAll205RowsAndBothPagesKeepTheSameSignatures(){
        String[] quantities=new String[205];Arrays.fill(quantities,"1");prepareNoSourceReturn(701,quantities);jdbc.update("UPDATE erp_sale_return_items SET product_id=101 WHERE return_id=701");Header basis=preview(701);assertEquals(205,basis.getSourceItemCount());
        cn.iocoder.yudao.framework.common.pojo.PageResult<Row> first=current.itemPage(701L,basis.getSourceSignature(),basis.getBasisSignature(),1,200),second=current.itemPage(701L,basis.getSourceSignature(),basis.getBasisSignature(),2,200);assertEquals(205L,first.getTotal());assertEquals(200,first.getList().size());assertEquals(5,second.getList().size());
        ConfirmRequest partial=confirmation(701,basis,"all-pages","1","2");assertEquals(200,partial.getItems().size());assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->current.confirm(partial));assertEquals(0,count("erp_sale_return_current_cost_confirmation"));
        for(Row row:second.getList()){assertEquals(basis.getSourceSignature(),row.getSourceSignature());assertEquals(basis.getBasisSignature(),row.getBasisSignature());ConfirmItem item=new ConfirmItem();item.setSourceItemId(row.getSourceItemId());item.setFinancialAmount(BigDecimal.ONE);item.setSettlementAmount(new BigDecimal("2"));item.setEvidence("independently checked final page");partial.getItems().add(item);}Header complete=current.confirm(partial);assertEquals(1,complete.getLatestRevision());assertEquals(205,count("erp_sale_return_current_cost_line"));
    }
    @Test void hugeConfirmedAmountsRemainStringsInJsonAndRealExcelAndReasonsAreReadable()throws Exception {
        prepareNoSourceReturn(701,"1000000");Header basis=preview(701);Header confirmed=current.confirm(confirmation(701,basis,"precise-excel","123456789012345.123456","123456789012346.123456"));
        String json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(confirmed);assertTrue(json.contains("\"financialAmount\":\"123456789012345.123456\""));approve(701,confirmed.getBasisSignature());
        amount("123456789012345.123456","SELECT financial_movement FROM erp_stock_dual_cost_posting");amount("123456789012505.1235","SELECT cost_amount FROM erp_stock WHERE id=11");
        MockHttpServletResponse response=new MockHttpServletResponse();report.export(true,filter(),"DETAIL",response);try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))){Cell cost=excelCell(book,"退货财务成本");assertEquals(CellType.STRING,cost.getCellType());assertEquals("123456789012345.123456",cost.getStringCellValue());String reason=excelCell(book,"财务毛利").getStringCellValue();assertTrue(reason.contains("税"),reason);assertFalse(reason.contains("TAX_BASIS_UNCONFIRMED"),reason);}
        when(permissions.getCurrentUserHiddenFields(eq("erp_stock"),eq(301L))).thenReturn(Collections.singletonList("costAmount"));MockHttpServletResponse masked=new MockHttpServletResponse();report.export(true,filter(),"DETAIL",masked);try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(masked.getContentAsByteArray()))){assertEquals("****",excelCell(book,"退货财务成本").getStringCellValue());assertEquals("****",excelCell(book,"财务毛利").getStringCellValue());}
    }
    @Test void reusedOrChangedPreparedBoCannotAddAnotherLegacyInventoryEvent(){
        prepareNoSourceReturn(701,"1");String basis=preview(701).getBasisSignature();tx.executeWithoutResult(s->{approve(701,basis);stockRecords.createStockRecord(lastRealStockRequest);});assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_sale_return_current_cost_posting_link"));amount("240","SELECT cost_amount FROM erp_stock WHERE id=11");
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->stockRecords.createStockRecord(lastRealStockRequest)));approve(701,basis);assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->approve(701,"changed-basis"));assertEquals(1,count("erp_stock_record"));
    }
    Cell excelCell(Workbook book,String label){Sheet sheet=book.getSheetAt(0);org.apache.poi.ss.usermodel.Row head=sheet.getRow(0);for(Cell cell:head)if(label.equals(cell.getStringCellValue()))return sheet.getRow(1).getCell(cell.getColumnIndex());throw new AssertionError("Missing Excel column "+label);}
    @Test void historicalApprovedWithoutLinkCannotInventCurrentCostsOrBeClaimed(){
        prepareNoSourceReturn(701,"1");jdbc.update("UPDATE erp_sale_return SET status=20 WHERE id=701");Header old=preview(701);assertEquals("HISTORICAL_COST_MISSING",old.getStatus());assertNull(old.getFinancialAmount());assertNull(old.getSettlementAmount());assertFalse(old.isCanConfirm());assertFalse(old.isCanApprove());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->approve(701,old.getBasisSignature()));assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->{current.authorizeApproval(701L,old.getBasisSignature());current.prepareApproval(((ErpSaleReturnMapper)actual.get(ErpSaleReturnMapper.class)).selectByIdForUpdate(701L),((ErpSaleReturnItemMapper)actual.get(ErpSaleReturnItemMapper.class)).selectListByReturnIdForUpdate(701L));}));
        amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_sale_return_current_cost_posting_link"));
    }
    @Test void claimWithoutCompletingInventoryRollsBackActualStatusTransition(){
        prepareNoSourceReturn(701,"1");Header basis=preview(701);assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->{current.authorizeApproval(701L,basis.getBasisSignature());assertEquals(1,current.claimApproval(701L));assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));}));
        assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));
    }
    @Test void callerChangingDatabaseStatusCannotForgeThePrivateClaimContext(){
        prepareNoSourceReturn(701,"1");Header basis=preview(701);assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->{jdbc.update("UPDATE erp_sale_return SET status=20 WHERE id=701");current.authorizeApproval(701L,basis.getBasisSignature());current.prepareApproval(((ErpSaleReturnMapper)actual.get(ErpSaleReturnMapper.class)).selectByIdForUpdate(701L),((ErpSaleReturnItemMapper)actual.get(ErpSaleReturnItemMapper.class)).selectListByReturnIdForUpdate(701L));}));
        assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));assertEquals(0,count("erp_stock_record"));
    }
    @Test void subFourPlaceIncomingAmountsKeepActualLegacyWatermarkUsableForNextPosting(){
        jdbc.update("UPDATE erp_stock SET count=0,cost_price=0,cost_amount=0 WHERE id=11");jdbc.update("UPDATE erp_stock_dual_cost_balance SET quantity=0,financial_amount=0,settlement_amount=0,legacy_cost_price=0,legacy_cost_amount=0 WHERE stock_id=11");
        String[] quantities=new String[53];Arrays.fill(quantities,"1");prepareNoSourceReturn(701,quantities);jdbc.update("UPDATE erp_sale_return_items SET product_id=101 WHERE return_id=701");Header basis=preview(701);Header confirmed=current.confirm(confirmation(701,basis,"microscopic-cost-evidence","0.000001","0.000002"));approve(701,confirmed.getBasisSignature());
        // The unchanged legacy four-place column rounds each actual update; it cannot carry six-place pennies.
        amount("0","SELECT cost_amount FROM erp_stock WHERE id=11");amount("0.000053","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("0.000106","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");
        prepareNoSourceReturn(801,"1");Header next=preview(801);assertEquals("READY",next.getStatus());assertEquals(0,new BigDecimal("0.000001").compareTo(next.getFinancialAmount()));approve(801,next.getBasisSignature());
        amount("54","SELECT count FROM erp_stock WHERE id=11");amount("0","SELECT cost_amount FROM erp_stock WHERE id=11");amount("0.000054","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");assertEquals(54,count("erp_stock_record"));
    }
    @Test void historicalPostedCostStillRequiresActualCustomerVisibility(){
        prepareNoSourceReturn(701,"1");approve(701,preview(701).getBasisSignature());
        DeptDataPermissionRespDTO deniedCustomer=new DeptDataPermissionRespDTO();deniedCustomer.setAll(false);deniedCustomer.setSelf(false);deniedCustomer.setDeptIds(Collections.singleton(999L));
        when(permissions.getDeptDataPermission(eq(99L),eq("erp_customer"))).thenReturn(deniedCustomer);
        assertNull(((ErpCustomerService)actual.get(ErpCustomerService.class)).getCustomer(901L),"real customer mapper must reject the now-invisible customer");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->preview(701),"historical posting costs must not bypass customer visibility");
    }
    @Test void visibleDisabledCustomerCanStillReadItsHistoricalPostedCost(){
        prepareNoSourceReturn(701,"1");approve(701,preview(701).getBasisSignature());jdbc.update("UPDATE erp_customer SET status=1 WHERE id=901");
        assertNotNull(((ErpCustomerService)actual.get(ErpCustomerService.class)).getCustomer(901L));Header history=preview(701);assertEquals("POSTED",history.getStatus());assertEquals(0,new BigDecimal("80").compareTo(history.getFinancialAmount()));
    }
    @Test void independentNewDocumentsInDifferentStocksCanBothApproveConcurrently()throws Exception {
        prepareNoSourceReturn(701,"1");prepareNoSourceReturn(702,"1");jdbc.update("UPDATE erp_sale_return_items SET product_id=102 WHERE return_id=702");
        String first=preview(701).getBasisSignature(),second=preview(702).getBasisSignature();CountDownLatch go=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try{Future<?> one=pool.submit(()->{login(1);try{await(go);approve(701,first);}finally{cleanup();}});Future<?> two=pool.submit(()->{login(1);try{await(go);approve(702,second);}finally{cleanup();}});go.countDown();one.get(15,TimeUnit.SECONDS);two.get(15,TimeUnit.SECONDS);
            assertEquals(2,count("erp_stock_record"));assertEquals(2,count("erp_sale_return_current_cost_posting_link"));amount("240","SELECT cost_amount FROM erp_stock WHERE id=11");amount("210","SELECT cost_amount FROM erp_stock WHERE id=12");
        }finally{pool.shutdownNow();}
    }
    @Test void reportCostBasisComesOnlyFromItsActualPostingLinkAndSurvivesOptionalTableAbsence(){
        prepareNoSourceReturn(701,"1");approve(701,preview(701).getBasisSignature());assertEquals("CURRENT_AVERAGE",report.page(true,filter(),false).getPage().getList().get(0).get("costBasis"));
        jdbc.update("UPDATE erp_sale_return_current_cost_posting_link SET return_item_id=999999 WHERE return_id=701");assertNull(report.page(true,filter(),false).getPage().getList().get(0).get("costBasis"));
        jdbc.update("UPDATE erp_sale_return_current_cost_posting_link SET return_item_id=7010,tenant_id=2 WHERE return_id=701");assertNull(report.page(true,filter(),false).getPage().getList().get(0).get("costBasis"));
        jdbc.update("UPDATE erp_sale_return_current_cost_posting_link SET tenant_id=1 WHERE return_id=701");
        jdbc.execute("RENAME TABLE erp_sale_return_current_cost_posting_link TO saved_report_optional_link");try{Map<String,Object> historical=report.page(true,filter(),false).getPage().getList().get(0);assertNull(historical.get("costBasis"));assertEquals("80.000000",historical.get("returnFinancialCost"));}finally{jdbc.execute("RENAME TABLE saved_report_optional_link TO erp_sale_return_current_cost_posting_link");}
    }
    @Test void reportManualCostBasisIsRecordedAndSourceCostPermissionMasksRowAndSummary(){
        prepareNoSourceReturn(701,"1");Header manual=current.confirm(confirmation(701,preview(701),"report-manual-source","81","92"));approve(701,manual.getBasisSignature());assertEquals("MANUAL_CONFIRMED",report.page(true,filter(),false).getPage().getList().get(0).get("costBasis"));
        when(permissions.getCurrentUserHiddenFields(eq("erp_sale_return"),eq(302L))).thenReturn(Collections.singletonList("costAmount"));
        ErpTradeReportModels.Bundle bundle=report.page(true,filter(),false);assertNull(bundle.getPage().getList().get(0).get("returnFinancialCost"));assertNull(bundle.getSummary().get("returnFinancialCost"));assertEquals("MASKED",((Map<?,?>)bundle.getSummary().get("metricStates")).get("returnFinancialCost"));
    }
    @Test void readOnlyPreviewAndItemPagesDoNotCreateEvidenceLocatorRows(){
        prepareNoSourceReturn(701,"1");Header basis=preview(701);current.itemPage(701L,basis.getSourceSignature(),basis.getBasisSignature(),1,20);preview(701);
        assertEquals(0,count("erp_sale_return_current_cost_state"));assertEquals(0,count("erp_sale_return_current_cost_confirmation"));assertEquals(0,count("erp_sale_return_current_cost_line"));assertEquals(0,count("erp_sale_return_current_cost_posting_link"));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));
    }
    @Test void reportGlobalStockFinancialMaskLeavesSettlementRefundAndQuantityVisible(){
        prepareNoSourceReturn(701,"1");approve(701,preview(701).getBasisSignature());when(permissions.getCurrentUserHiddenFields("erp_stock")).thenReturn(Collections.singletonList("financialAmount"));
        ErpTradeReportModels.Bundle bundle=report.page(true,filter(),false);Map<String,Object> row=bundle.getPage().getList().get(0);assertNull(row.get("returnFinancialCost"));assertNull(bundle.getSummary().get("returnFinancialCost"));assertEquals("90.000000",row.get("returnSettlementCost"));assertEquals("100.000000",row.get("returnGrossAmount"));assertEquals("1.000000",row.get("returnQuantity"));
    }
    @Test void reportSourcePrefixedCostPermissionAppliesToDetailAndGroupWithoutHidingRefund(){
        prepareNoSourceReturn(701,"1");approve(701,preview(701).getBasisSignature());when(permissions.getCurrentUserHiddenFields(eq("erp_sale_return"),eq(302L))).thenReturn(Collections.singletonList("item_costAmount"));
        ErpTradeReportModels.Filter filter=filter();filter.setGroupBy("PRODUCT");ErpTradeReportModels.Bundle grouped=report.page(true,filter,true);assertNull(grouped.getPage().getList().get(0).get("returnFinancialCost"));assertNull(grouped.getSummary().get("returnSettlementCost"));assertEquals("100.000000",grouped.getSummary().get("returnGrossAmount"));
    }
    @Test void failedPostingCompletionRollsBackActualApprovalAndManualEvidenceConsumption(){
        prepareNoSourceReturn(701,"1");Header confirmed=current.confirm(confirmation(701,preview(701),"completion-failure","81","92"));
        jdbc.execute("CREATE TRIGGER fixture_reject_actual_posting_completion BEFORE UPDATE ON erp_stock_dual_cost_posting FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='real approval completion failure'");
        try{assertThrows(org.springframework.transaction.UnexpectedRollbackException.class,()->tx.executeWithoutResult(s->{try{approve(701,confirmed.getBasisSignature());}catch(org.springframework.dao.DataAccessException expected){assertTrue(expected.getMessage().contains("real approval completion failure"));}}));
            assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));assertEquals(0,count("erp_business_report_item_snapshot"));assertEquals(0,count("erp_sale_return_current_cost_posting_link"));assertNull(jdbc.queryForObject("SELECT consumed_at FROM erp_sale_return_current_cost_confirmation",Object.class));amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");amount("180","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");
        }finally{jdbc.execute("DROP TRIGGER fixture_reject_actual_posting_completion");}
    }
    @Test void enabledApprovalCannotProceedWithoutActualCursorTrigger()throws Exception {
        prepareNoSourceReturn(701,"1");Header basis=preview(701);jdbc.execute("DROP TRIGGER erp_stock_record_cursor_ai");
        try{assertThrows(RuntimeException.class,()->approve(701,basis.getBasisSignature()));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");}
        finally{IsolatedStockCursorMigration.apply(jdbc,"report_sale_return_current_cost_test");}
    }
    @Test void cursorPointingAtAnotherInventoryCannotAuthorizeCurrentCost(){
        prepareNoSourceReturn(701,"1");prepareNoSourceReturn(702,"1");jdbc.update("UPDATE erp_sale_return_items SET product_id=102 WHERE return_id=702");approve(702,preview(702).getBasisSignature());
        long unrelatedRecord=jdbc.queryForObject("SELECT id FROM erp_stock_record WHERE product_id=102",Long.class);jdbc.update("UPDATE erp_stock SET legacy_record_cursor_id=? WHERE id=11",unrelatedRecord);
        assertThrows(RuntimeException.class,()->preview(701));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));assertEquals(1,count("erp_stock_record"));amount("160","SELECT cost_amount FROM erp_stock WHERE id=11");
    }
    void assertParentWait()throws Exception {long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<deadline){int count=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON w.REQUESTING_ENGINE_LOCK_ID=l.ENGINE_LOCK_ID WHERE l.OBJECT_SCHEMA='report_sale_return_current_cost_test' AND l.OBJECT_NAME='erp_sale_return'",Integer.class);if(count>0)return;Thread.sleep(20);}fail("second real transaction must wait for the source parent");}
}
