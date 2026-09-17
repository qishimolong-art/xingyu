package cn.iocoder.yudao.module.erp.service.sale.returncost;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.datapermission.core.db.DataPermissionRuleHandler;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRuleFactoryImpl;
import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.*;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.*;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.*;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.framework.datapermission.config.ErpDataPermissionConfiguration;
import cn.iocoder.yudao.module.erp.service.sale.*;
import cn.iocoder.yudao.module.erp.service.stock.*;
import cn.iocoder.yudao.module.erp.service.stock.cost.*;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService;
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
import javax.annotation.Resource;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Independent real parent approvals, MyBatis interceptors, accounting writer and Spring transactions.
 * The legacy stock-record service alone is an explicit SQL adapter; this is not a running ERP server. */
@EnabledIfEnvironmentVariable(named="ERP_SALE_RETURN_COST_MYSQL_TEST_URL",matches=".+")
class ErpSaleReturnCostMysqlIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    TransactionTemplate tx;
    final LocalDateTime cutover=LocalDateTime.of(2026,1,1,0,0);
    final Map<Class<?>,Object> actual=new LinkedHashMap<>();
    ErpSaleOutMapper outHeaders;
    ErpSaleOutItemMapper outItems;
    ErpSaleReturnMapper returnHeaders;
    ErpSaleReturnItemMapper returnItems;
    ErpSaleOutService saleApproval;
    ErpSaleReturnService returnApproval;
    ErpSaleReturnCostService returns,returnTarget;
    ErpTradeSnapshotService trade,tradeTarget;
    ErpDualCostPostingService writer,writerTarget;
    DeptDataPermissionRespDTO dataPermission;
    PermissionApi permissions;
    volatile Long failAfterLegacyItem;

    @BeforeEach void setup() throws Exception {
        String url=System.getenv("ERP_SALE_RETURN_COST_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_sale_return_cost_test(?:\\?.*)?"));
        DriverManagerDataSource ds=new DriverManagerDataSource(url,"root","");jdbc=new JdbcTemplate(ds);
        assertTrue(jdbc.queryForObject("SELECT @@datadir",String.class).replace('\\','/').contains("/Temp/xingyu-report-mysql-"));
        manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);
        for(String script:Arrays.asList("erp_report_dual_cost_foundation_20260909.sql","erp_sale_return_original_cost_v225.sql","erp_trade_snapshot_v224.sql"))executeDdl(script);
        for(Class<?> type:Arrays.asList(ErpSalePriceAdjustDO.class,ErpSaleOutDO.class,ErpSaleOutItemDO.class,ErpSaleReturnDO.class,ErpSaleReturnItemDO.class,ErpStockDO.class,ErpWarehouseDO.class,ErpProductDO.class))createEntityTable(type);
        createEntityTable(cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO.class);
        executeDdl("erp_report_source_item_lock_index_20260909.sql");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_stock_record(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,product_id BIGINT,warehouse_id BIGINT,deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_customer(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_product_unit(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_dept(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,nickname VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for(String table:Arrays.asList("erp_sale_return_cost_allocation","erp_sale_return_cost_progress","erp_business_report_item_snapshot","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance","erp_stock_record","erp_sale_return_items","erp_sale_return","erp_sale_out_items","erp_sale_out","erp_stock","erp_warehouse","erp_product","erp_customer","erp_product_unit"))jdbc.execute("DELETE FROM "+table);
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,status,sale_enabled,creator,deleted) VALUES(201,1,301,'stock warehouse',0,1,'99',0)");
        jdbc.update("INSERT INTO erp_product(id,tenant_id,code,name,brand,category_id,unit_id,status,creator,deleted) VALUES(101,1,'P101','part A','original brand',701,801,0,'99',0)");
        jdbc.update("INSERT INTO erp_customer(id,tenant_id,name) VALUES(501,1,'original customer')");
        jdbc.update("INSERT INTO erp_product_unit(id,tenant_id,name) VALUES(801,1,'piece')");
        jdbc.update("INSERT INTO erp_stock(id,tenant_id,product_id,warehouse_id,dept_id,count,cost_price,cost_amount,creator,deleted) VALUES(11,1,101,201,301,3,216.666667,650,'99',0)");
        jdbc.update("INSERT INTO erp_sale_out(id,tenant_id,no,status,customer_id,dept_id,sale_user_id,out_time,total_count,total_product_price,total_price,discount_price,fee_amount,creator,deleted) VALUES(401,1,'SALE-401',?,501,302,88,?,3,600,600,0,0,'99',0)",ErpAuditStatus.PROCESS.getStatus(),cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_sale_out_items(id,tenant_id,out_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(601,1,401,101,201,301,3,200,600,'99',0)");
        dataPermission=new DeptDataPermissionRespDTO();dataPermission.setAll(true);
        permissions=mock(PermissionApi.class);when(permissions.getDeptDataPermission(eq(99L),anyString())).thenAnswer(c->dataPermission);
        when(permissions.hasAnyRoles(eq(99L),any(String[].class))).thenReturn(true);
        actual.put(PermissionApi.class,permissions);actual.put(JdbcTemplate.class,jdbc);
        MybatisConfiguration config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);config.setCacheEnabled(false);
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(TenantContextHolder.getRequiredTenantId());}}));
        DeptDataPermissionRule rule=new DeptDataPermissionRule(permissions);new ErpDataPermissionConfiguration().erpDeptDataPermissionRuleCustomizer().customize(rule);
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new DataPermissionRuleHandler(new DataPermissionRuleFactoryImpl(Collections.singletonList(rule)))));
        MybatisSqlSessionFactoryBean bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(config);bean.setPlugins(interceptor);
        bean.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setLogicDeleteValue("1").setLogicNotDeleteValue("0")));
        SqlSessionFactory factory=bean.getObject();
        List<Class<?>> mapperTypes=Arrays.asList(ErpSalePriceAdjustMapper.class,ErpSaleOutMapper.class,ErpSaleOutItemMapper.class,ErpSaleReturnMapper.class,ErpSaleReturnItemMapper.class,ErpStockMapper.class,ErpWarehouseMapper.class,ErpProductMapper.class);
        for(Class<?> type:mapperTypes)factory.getConfiguration().addMapper(type);
        SqlSessionTemplate template=new SqlSessionTemplate(factory);for(Class<?> type:mapperTypes)actual.put(type,template.getMapper(type));
        outHeaders=(ErpSaleOutMapper)actual.get(ErpSaleOutMapper.class);outItems=(ErpSaleOutItemMapper)actual.get(ErpSaleOutItemMapper.class);
        returnHeaders=(ErpSaleReturnMapper)actual.get(ErpSaleReturnMapper.class);returnItems=(ErpSaleReturnItemMapper)actual.get(ErpSaleReturnItemMapper.class);
        ErpWarehouseServiceImpl warehouse=new ErpWarehouseServiceImpl();inject(warehouse);actual.put(ErpWarehouseService.class,warehouse);
        ErpSaleReturnCostRepository returnRepo=new ErpSaleReturnCostRepository();inject(returnRepo);actual.put(ErpSaleReturnCostRepository.class,returnRepo);
        tradeTarget=new ErpTradeSnapshotService();inject(tradeTarget);set(tradeTarget,"enabled",true);trade=proxy(tradeTarget);actual.put(ErpTradeSnapshotService.class,trade);
        returnTarget=new ErpSaleReturnCostService();inject(returnTarget);set(returnTarget,"enabled",true);returns=proxy(returnTarget);actual.put(ErpSaleReturnCostService.class,returns);
        ErpDualCostLedgerRepository ledger=new ErpDualCostLedgerRepository();inject(ledger);actual.put(ErpDualCostLedgerRepository.class,ledger);
        writerTarget=new ErpDualCostPostingService();inject(writerTarget);set(writerTarget,"enabled",false);set(writerTarget,"cutover",cutover.toString());writer=proxy(writerTarget);actual.put(ErpDualCostPostingService.class,writer);
        login(1);writer.confirmOpening(101,201,new BigDecimal("3"),new BigDecimal("650"),new BigDecimal("720"),cutover,"independent actual-sale opening",99);set(writerTarget,"enabled",true);
        ErpStockRecordService stockRecords=mock(ErpStockRecordService.class);
        doAnswer(call->{ErpStockRecordCreateReqBO bo=call.getArgument(0);Runnable legacy=()->{
            jdbc.update("UPDATE erp_stock SET count=count+? WHERE tenant_id=? AND product_id=? AND warehouse_id=?",bo.getCount(),TenantContextHolder.getRequiredTenantId(),bo.getProductId(),bo.getWarehouseId());
            jdbc.update("INSERT INTO erp_stock_record(tenant_id,product_id,warehouse_id) VALUES(?,?,?)",TenantContextHolder.getRequiredTenantId(),bo.getProductId(),bo.getWarehouseId());
            if(Objects.equals(failAfterLegacyItem,bo.getBizItemId()))throw new IllegalStateException("independent post-stock failure");
        };if(!writer.post(bo,legacy))legacy.run();return null;}).when(stockRecords).createStockRecord(any());actual.put(ErpStockRecordService.class,stockRecords);
        ErpSaleOutServiceImpl saleTarget=new ErpSaleOutServiceImpl();inject(saleTarget);saleApproval=proxy(saleTarget);actual.put(ErpSaleOutService.class,saleApproval);
        ErpSaleReturnServiceImpl approvalTarget=new ErpSaleReturnServiceImpl();inject(approvalTarget);returnApproval=proxy(approvalTarget);
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
    void sale(){saleApproval.updateSaleOutStatus(401L,ErpAuditStatus.APPROVE.getStatus());}
    void prepareReturn(long id,String...quantities){BigDecimal total=Arrays.stream(quantities).map(BigDecimal::new).reduce(BigDecimal.ZERO,BigDecimal::add);jdbc.update("INSERT INTO erp_sale_return(id,tenant_id,no,status,customer_id,dept_id,sale_user_id,return_mode,source_out_id,source_out_no,return_time,total_count,total_product_price,total_price,discount_price,fee_amount,creator,deleted) VALUES(?,1,?,10,501,302,99,10,401,'SALE-401',?,?,?,?,0,0,'99',0)",id,"RETURN-"+id,cutover.plusDays(2),total,total.multiply(new BigDecimal("200")),total.multiply(new BigDecimal("200")));
        for(int n=0;n<quantities.length;n++){BigDecimal quantity=new BigDecimal(quantities[n]);jdbc.update("INSERT INTO erp_sale_return_items(id,tenant_id,return_id,source_out_item_id,product_id,warehouse_id,dept_id,count,product_price,total_price,creator,deleted) VALUES(?,1,?,601,101,201,301,?,200,?,'99',0)",id*10+n,id,quantity,quantity.multiply(new BigDecimal("200")));}}
    void approve(long id){returnApproval.updateSaleReturnStatus(id,ErpAuditStatus.APPROVE.getStatus());}
    void amount(String expected,String sql){BigDecimal actualValue=jdbc.queryForObject(sql,BigDecimal.class);assertNotNull(actualValue);assertEquals(0,new BigDecimal(expected).compareTo(actualValue),sql);}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}

    @Test void enabledPriceAdjustmentCannotPoisonStockWatermarkAndNextReturnStillPosts(){
        sale();
        jdbc.update("DELETE FROM erp_sale_price_adjust");
        ErpSalePriceAdjustServiceImpl target=new ErpSalePriceAdjustServiceImpl();inject(target);
        set(target,"dualCostEnabled",true);ErpSalePriceAdjustService service=proxy(target);
        List<Map<String,Object>> originalStock=jdbc.queryForList("SELECT * FROM erp_stock");
        List<Map<String,Object>> originalBalance=jdbc.queryForList("SELECT * FROM erp_stock_dual_cost_balance");
        for(int adjustType:new int[]{1,2}){
            long id=901+adjustType;
            jdbc.update("INSERT INTO erp_sale_price_adjust(id,tenant_id,no,status,adjust_type,customer_id,dept_id,original_sale_out_id,creator,deleted) VALUES(?,1,?,10,?,501,302,401,'99',0)",id,"ADJUST-"+id,adjustType);
            cn.iocoder.yudao.framework.common.exception.ServiceException rejected=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.updateSalePriceAdjustStatus(id,20));
            assertTrue(rejected.getMessage().contains("调价"));
            assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_price_adjust WHERE id=?",Integer.class,id));
            amount("200","SELECT product_price FROM erp_sale_out_items WHERE id=601");
            assertEquals(originalStock,jdbc.queryForList("SELECT * FROM erp_stock"));
            assertEquals(originalBalance,jdbc.queryForList("SELECT * FROM erp_stock_dual_cost_balance"));
            assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_stock_dual_cost_posting"));assertEquals(1,count("erp_business_report_item_snapshot"));
        }
        prepareReturn(701,"1");approve(701);
        amount("216.666667","SELECT financial_amount FROM erp_stock_dual_cost_balance");
        assertEquals(2,count("erp_stock_record"));assertEquals(1,count("erp_sale_return_cost_allocation"));
    }
    @Test void realSaleAndThreePartialReturnsRecoverExactOriginalCostsAndMicrosecondSnapshots(){
        sale();amount("-650","SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE biz_type=50");
        for(long id:new long[]{701,702,703}){prepareReturn(id,"1");approve(id);}
        amount("650","SELECT SUM(financial_amount) FROM erp_sale_return_cost_allocation");amount("720","SELECT SUM(settlement_amount) FROM erp_sale_return_cost_allocation");
        amount("650","SELECT financial_amount FROM erp_stock_dual_cost_balance");amount("3","SELECT count FROM erp_stock");assertEquals(4,count("erp_business_report_item_snapshot"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_business_report_item_snapshot s JOIN erp_stock_dual_cost_posting p ON p.tenant_id=s.tenant_id AND p.id=s.posting_id WHERE s.posted_at<>p.posted_at",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_business_report_item_snapshot WHERE net_amount IS NOT NULL",Integer.class));
    }
    @Test void twoRowsSharingSourceAdvanceOneProgressWithoutLosingTheRoundingRemainder(){sale();prepareReturn(701,"1","2");approve(701);assertEquals(2,count("erp_sale_return_cost_allocation"));amount("650","SELECT returned_financial_amount FROM erp_sale_return_cost_progress");amount("3","SELECT returned_quantity FROM erp_sale_return_cost_progress");}
    @Test void secondLineFailureRollsBackRealApprovalStockCostProgressAndTradeSnapshot(){sale();prepareReturn(701,"1","1");failAfterLegacyItem=7011L;assertThrows(RuntimeException.class,()->approve(701));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));amount("0","SELECT count FROM erp_stock");amount("0","SELECT quantity FROM erp_stock_dual_cost_balance");assertEquals(1,count("erp_stock_dual_cost_posting"));assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_business_report_item_snapshot"));assertEquals(0,count("erp_sale_return_cost_allocation"));assertEquals(0,count("erp_sale_return_cost_progress"));}
    @Test void concurrentReturnApprovalsCannotBothConsumeMoreThanOriginalQuantity()throws Exception {sale();prepareReturn(701,"2");prepareReturn(702,"2");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch go=new CountDownLatch(1);try{List<Future<Boolean>> results=new ArrayList<>();for(long id:new long[]{701,702})results.add(pool.submit(()->{login(1);go.await();try{approve(id);return true;}catch(RuntimeException failure){return false;}finally{cleanup();}}));go.countDown();int success=0;for(Future<Boolean> f:results)if(f.get(15,TimeUnit.SECONDS))success++;assertEquals(1,success);amount("2","SELECT returned_quantity FROM erp_sale_return_cost_progress");assertEquals(1,count("erp_sale_return_cost_allocation"));}finally{pool.shutdownNow();}}
    @Test void currentPriceAndBrandChangesNeverRewriteOriginalOrReturnAttributeHistory(){sale();jdbc.update("UPDATE erp_sale_out_items SET product_price=999,total_price=2997 WHERE id=601");jdbc.update("UPDATE erp_product SET brand='current brand',category_id=999 WHERE id=101");prepareReturn(701,"1");approve(701);assertEquals(Arrays.asList("original brand","original brand"),jdbc.queryForList("SELECT brand FROM erp_business_report_item_snapshot ORDER BY posting_id",String.class));amount("600","SELECT gross_amount FROM erp_business_report_item_snapshot WHERE business_type='SALE_OUT'");assertEquals(88L,jdbc.queryForObject("SELECT sale_user_id FROM erp_business_report_item_snapshot WHERE business_type='SALE_RETURN'",Long.class));}
    @Test void otherTenantAndDepartmentCannotApproveReturnFromHiddenSource(){sale();prepareReturn(701,"1");login(2);assertThrows(RuntimeException.class,()->approve(701));login(1);dataPermission.setAll(false);dataPermission.setDeptIds(Collections.singleton(999L));assertThrows(RuntimeException.class,()->approve(701));assertEquals(0,count("erp_sale_return_cost_allocation"));}

    @Test void missingOriginalPostingAndNoOriginalModeNeverUseCurrentOrZeroCost(){
        sale();prepareReturn(701,"1");jdbc.update("UPDATE erp_sale_return SET return_mode=20 WHERE id=701");
        assertThrows(RuntimeException.class,()->approve(701));jdbc.update("UPDATE erp_sale_return SET return_mode=10 WHERE id=701");
        jdbc.update("DELETE FROM erp_stock_dual_cost_posting WHERE biz_type=50");assertThrows(RuntimeException.class,()->approve(701));
        assertEquals(0,count("erp_sale_return_cost_allocation"));amount("0","SELECT count FROM erp_stock");
    }
    @Test void suspendedOuterPreparedContextsCannotAuthorizeAnIndependentTransaction(){
        sale();prepareReturn(701,"1");final ErpStockRecordCreateReqBO[] captured=new ErpStockRecordCreateReqBO[1];
        tx.executeWithoutResult(outer->{
            returnHeaders.selectByIdForUpdate(701L);returnHeaders.updateByIdAndStatus(701L,10,new ErpSaleReturnDO().setStatus(20));
            ErpSaleReturnDO header=returnHeaders.selectByIdForUpdate(701L);List<ErpSaleReturnItemDO> lines=returnItems.selectListByReturnIdForUpdate(701L);
            ErpStockRecordCreateReqBO bo=returns.prepareApproval(header,lines).get(7010L);captured[0]=bo;
            Map<Long,Long> sources=Collections.singletonMap(7010L,bo.getReversalPostingId());
            bo.setTradeContext(trade.prepareSaleReturn(header,lines,sources).get(7010L));
            TransactionTemplate independent=new TransactionTemplate(manager);independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThrows(RuntimeException.class,()->independent.executeWithoutResult(inner->returns.validatePosting(bo)));
            assertThrows(RuntimeException.class,()->independent.executeWithoutResult(inner->trade.validatePreparedForPosting(bo)));
            returns.validatePosting(bo);trade.validatePreparedForPosting(bo);
            ((ErpStockRecordService)actual.get(ErpStockRecordService.class)).createStockRecord(bo);
        });
        assertEquals(1,count("erp_sale_return_cost_allocation"));assertEquals(2,count("erp_business_report_item_snapshot"));
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->returns.validatePosting(captured[0])));
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->trade.validatePreparedForPosting(captured[0])));
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
    }
    @Test void disabledReturnApprovalDoesNotReadAnyNewCostOrSnapshotTable(){
        sale();prepareReturn(701,"1");set(returnTarget,"enabled",false);set(tradeTarget,"enabled",false);set(writerTarget,"enabled",false);
        List<String> names=Arrays.asList("erp_sale_return_cost_allocation","erp_sale_return_cost_progress","erp_business_report_item_snapshot","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance");
        List<String> forward=new ArrayList<>(),backward=new ArrayList<>();for(String name:names){forward.add(name+" TO saved_"+name);backward.add("saved_"+name+" TO "+name);}
        jdbc.execute("RENAME TABLE "+String.join(",",forward));
        try{approve(701);amount("1","SELECT count FROM erp_stock");assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=701",Integer.class));}
        finally{jdbc.execute("RENAME TABLE "+String.join(",",backward));}
    }
    @Test void sourceReturnStatusIncludesCommitThatOccurredAfterAnOlderReadSnapshot()throws Exception {
        sale();prepareReturn(701,"1");prepareReturn(702,"2");
        // Setup executes the actual three-table parent-lock index migration before this concurrency test.
        CountDownLatch sourceHeld=new CountDownLatch(1),oldSnapshot=new CountDownLatch(1),finishFirst=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Future<?> first=pool.submit(()->{login(1);try{tx.executeWithoutResult(s->{returnHeaders.selectByIdForUpdate(701L);returnItems.selectListByReturnIdForUpdate(701L);outHeaders.selectByIdForUpdate(401L);sourceHeld.countDown();try{if(!finishFirst.await(10,TimeUnit.SECONDS))throw new IllegalStateException("first release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}approve(701);});}finally{cleanup();}});
            assertTrue(sourceHeld.await(5,TimeUnit.SECONDS));
            Future<?> second=pool.submit(()->{login(1);try{tx.executeWithoutResult(s->{jdbc.queryForObject("SELECT COUNT(*) FROM erp_sale_return WHERE status=20",Integer.class);oldSnapshot.countDown();approve(702);});}finally{cleanup();}});
            assertTrue(oldSnapshot.await(5,TimeUnit.SECONDS));boolean waiting=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<deadline){int count=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON w.REQUESTING_ENGINE_LOCK_ID=l.ENGINE_LOCK_ID WHERE l.OBJECT_SCHEMA='report_sale_return_cost_test' AND l.OBJECT_NAME='erp_sale_out'",Integer.class);if(count>0){waiting=true;break;}Thread.sleep(20);}
            assertTrue(waiting,"second approval must actually wait on original sale source row");finishFirst.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
            amount("3","SELECT returned_quantity FROM erp_sale_return_cost_progress");assertEquals(2,jdbc.queryForObject("SELECT return_status FROM erp_sale_out WHERE id=401",Integer.class));
        }finally{finishFirst.countDown();pool.shutdownNow();}
    }
    @Test void missingOriginalTradeSnapshotAllowsReliableCostReturnButDoesNotInventHistoricalAttributes(){sale();jdbc.update("DELETE FROM erp_business_report_item_snapshot");prepareReturn(701,"1");approve(701);amount("216.666667","SELECT financial_amount FROM erp_sale_return_cost_allocation");assertNull(jdbc.queryForObject("SELECT brand FROM erp_business_report_item_snapshot",String.class));assertNull(jdbc.queryForObject("SELECT sale_user_id FROM erp_business_report_item_snapshot",Long.class));assertTrue(jdbc.queryForObject("SELECT snapshot_json FROM erp_business_report_item_snapshot",String.class).contains("ORIGINAL_SNAPSHOT_MISSING"));}
    @Test void historicalApprovedReturnWithoutAllocationBlocksFurtherCostConsumption(){sale();prepareReturn(701,"1");jdbc.update("UPDATE erp_sale_return SET status=20 WHERE id=701");prepareReturn(702,"1");assertThrows(RuntimeException.class,()->approve(702));assertEquals(0,count("erp_sale_return_cost_allocation"));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_sale_return WHERE id=702",Integer.class));}
    @Test void ambiguousOriginalSalePostingsNeverSelectAnArbitraryOriginalCost(){sale();Map<String,Object> duplicate=new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM erp_stock_dual_cost_posting WHERE biz_type=50"));duplicate.put("id",99L);duplicate.put("action_key","independent-ambiguous-source");jdbc.update("INSERT INTO erp_stock_dual_cost_posting ("+String.join(",",duplicate.keySet())+") VALUES ("+String.join(",",Collections.nCopies(duplicate.size(),"?"))+")",duplicate.values().toArray());prepareReturn(701,"1");assertThrows(RuntimeException.class,()->approve(701));assertEquals(0,count("erp_sale_return_cost_allocation"));}
}
