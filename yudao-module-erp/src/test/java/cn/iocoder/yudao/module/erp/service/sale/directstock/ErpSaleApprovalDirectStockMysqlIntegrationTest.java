package cn.iocoder.yudao.module.erp.service.sale.directstock;
import cn.iocoder.yudao.module.erp.service.sale.returncost.*;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import java.util.concurrent.atomic.AtomicLong;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPickReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
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

/** 00H independent fixture: real Cart, SaleOut, StockLock, StockRecord and StockService; peripheral services are explicit mocks.
 * Peripheral product sync and descriptive snapshot enrichment may be mocked; inventory writes are real. */
@EnabledIfEnvironmentVariable(named="ERP_SALE_DIRECT_STOCK_MYSQL_TEST_URL",matches=".+")
class ErpSaleApprovalDirectStockMysqlIntegrationTest {
    Object previousSpringBeanFactory;
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    TransactionTemplate tx;
    final LocalDateTime cutover=LocalDateTime.of(2026,1,1,0,0);
    final Map<Class<?>,Object> actual=new LinkedHashMap<>();
    ErpSaleCartService carts;
    ErpSaleCartServiceImpl cartTarget;
    ErpSaleOutService sales;
    ErpStockLockService locks;
    ErpStockMoveService moves;
    ErpStockOutBillService oldBills;
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
    boolean failSaleAfterActualStock;
    volatile CyclicBarrier initialTransferInsertBarrier;
    ErpStockRecordCreateReqBO lastRealStockRequest;

    @BeforeEach void setup() throws Exception {
        String url=System.getenv("ERP_SALE_DIRECT_STOCK_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_sale_direct_stock_test(?:\\?.*)?"));
        DriverManagerDataSource ds=new DriverManagerDataSource(url,"root","");jdbc=new JdbcTemplate(ds);
        assertTrue(jdbc.queryForObject("SELECT @@datadir",String.class).replace('\\','/').contains("/Temp/xingyu-report-mysql-"));
        manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);
        previousSpringBeanFactory=ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class,"beanFactory");
        org.springframework.beans.factory.support.DefaultListableBeanFactory localBeans=new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        localBeans.registerSingleton("dataSource",ds);localBeans.registerSingleton("transactionManager",manager);
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(localBeans);
        // This named private fixture is reset before migrations; obsolete unregistered evidence is tested separately.
        for(String table:Arrays.asList("erp_sale_return_current_cost_posting_link","erp_sale_return_current_cost_line","erp_sale_return_current_cost_confirmation","erp_sale_return_current_cost_state"))if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",Integer.class,table)>0)jdbc.execute("DELETE FROM "+table);
        for(Class<?> type:Arrays.asList(ErpWarehouseSaleDeptPermissionDO.class,ErpSaleCartDO.class,ErpSaleCartItemDO.class,ErpSaleConfigDO.class,ErpStockInDO.class,ErpStockInItemDO.class,ErpStockInBillDO.class,ErpStockInBillItemDO.class,ErpStockLockDO.class,ErpStockMoveDO.class,ErpStockMoveItemDO.class,ErpStockOutDO.class,ErpStockOutItemDO.class,ErpStockCheckDO.class,ErpStockCheckItemDO.class,ErpWarehouseMoveDO.class,ErpWarehouseMoveItemDO.class,ErpStockOutBillDO.class,ErpStockOutBillItemDO.class,ErpStockOutBillPickRecordDO.class,ErpPurchaseReturnDO.class,ErpPurchaseReturnItemDO.class,ErpPurchaseInDO.class,ErpPurchaseInItemDO.class,ErpPurchaseOrderDO.class,ErpPurchaseOrderItemDO.class,ErpCustomerDO.class,ErpCustomerDeptDO.class,ErpSaleOutDO.class,ErpSaleReturnDO.class,ErpSaleReturnItemDO.class,ErpSaleOutItemDO.class,ErpStockDO.class,ErpStockRecordDO.class,ErpWarehouseDO.class,ErpProductDO.class,ErpSupplierDO.class))createEntityTable(type);
        // Match the existing erp_stock_cost_v1.sql storage precision, not the six-place new ledger.
        jdbc.execute("ALTER TABLE erp_stock MODIFY cost_amount DECIMAL(20,4), MODIFY cost_price DECIMAL(16,6)");
        jdbc.execute("ALTER TABLE erp_stock_record MODIFY cost_amount DECIMAL(20,4), MODIFY total_price DECIMAL(20,4), MODIFY cost_price DECIMAL(16,6), MODIFY unit_price DECIMAL(16,6)");
        for(String script:Arrays.asList("erp_sale_cart_direct_stock_lock_index_20260909.sql","erp_report_dual_cost_foundation_20260909.sql","erp_trade_snapshot_v224.sql","erp_report_source_item_lock_index_20260909.sql","erp_purchase_return_current_cost_20260909.sql","erp_purchase_return_source_lock_index_20260909.sql","erp_stock_dimension_lookup_index_20260909.sql","erp_sale_return_original_cost_v225.sql","erp_sale_return_current_cost_20260909.sql"))executeDdl(script);
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_product_unit(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_dept(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,nickname VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for(String table:Arrays.asList("erp_warehouse_sale_dept_permission","erp_stock_in","erp_stock_in_item","erp_stock_in_bill","erp_stock_in_bill_item","erp_sale_cart_items","erp_sale_cart","erp_sale_config","erp_stock_lock","erp_stock_move_item","erp_stock_move","erp_stock_out_item","erp_stock_out","erp_stock_check_item","erp_stock_check","erp_warehouse_move_item","erp_warehouse_move","erp_stock_out_bill_item","erp_stock_out_bill","erp_stock_out_bill_pick_record","erp_sale_return_current_cost_state","erp_sale_return_current_cost_posting_link","erp_sale_return_current_cost_line","erp_sale_return_current_cost_confirmation","erp_sale_return_cost_allocation","erp_sale_return_cost_progress","erp_purchase_return_posting_link","erp_business_report_item_snapshot","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance","erp_stock_record","erp_purchase_return_items","erp_purchase_return","erp_purchase_in_items","erp_purchase_in","erp_purchase_order_items","erp_purchase_order","erp_sale_return_items","erp_sale_return","erp_sale_out_items","erp_sale_out","erp_customer_dept","erp_customer","erp_stock","erp_warehouse","erp_product","erp_supplier","erp_product_unit"))jdbc.execute("DELETE FROM "+table);
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
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new DataPermissionRuleHandler(new DataPermissionRuleFactoryImpl(Collections.singletonList(rule)))));
        MybatisSqlSessionFactoryBean bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(config);bean.setPlugins(interceptor);
        bean.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setLogicDeleteValue("1").setLogicNotDeleteValue("0")));
        SqlSessionFactory factory=bean.getObject();localBeans.registerSingleton("sqlSessionFactory",factory);
        List<Class<?>> mappers=Arrays.asList(ErpWarehouseSaleDeptPermissionMapper.class,ErpSaleCartMapper.class,ErpSaleCartItemMapper.class,ErpSaleConfigMapper.class,ErpStockLockMapper.class,ErpStockMoveMapper.class,ErpStockMoveItemMapper.class,ErpStockOutBillMapper.class,ErpStockOutBillItemMapper.class,ErpStockOutBillPickRecordMapper.class,ErpPurchaseReturnMapper.class,ErpPurchaseReturnItemMapper.class,ErpPurchaseInMapper.class,ErpPurchaseInItemMapper.class,ErpPurchaseOrderMapper.class,ErpPurchaseOrderItemMapper.class,ErpCustomerMapper.class,ErpSaleOutMapper.class,ErpSaleOutItemMapper.class,ErpSaleReturnMapper.class,ErpSaleReturnItemMapper.class,ErpStockMapper.class,ErpStockRecordMapper.class,ErpWarehouseMapper.class,ErpProductMapper.class,ErpSupplierMapper.class);
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
            if(failSaleAfterActualStock && Integer.valueOf(50).equals(bo.getBizType()))throw new IllegalStateException("independent sale failure after actual stock within transfer listener");
            if(checkSuspendedLegacyCallback){checkSuspendedLegacyCallback=false;
                TransactionTemplate inner=new TransactionTemplate(manager);inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);inner.setTimeout(2);
                // A database lock timeout must not be mistaken for successful authorization rejection.
                assertThrows(IllegalStateException.class,()->inner.executeWithoutResult(s->stockService.updateStockCountAndCost(bo.getProductId(),bo.getWarehouseId(),bo.getCount(),bo.getUnitPrice(),bo.getBizType())));
                writer.assertLegacyMutationAllowed(true);
            }
            if(Objects.equals(failAfterStockItem,bo.getBizItemId()) || (failAfterStockItem!=null && failAfterStockItem.equals(-bo.getProductId())))throw new IllegalStateException("independent failure after actual stock cost update");return null;}).when(description).fillStockRecordSnapshot(any(),any());
        actual.put(ErpStockItemSnapshotSupport.class,description);
        ErpStockRecordServiceImpl stockRecordTarget=new ErpStockRecordServiceImpl();inject(stockRecordTarget);stockRecords=proxy(stockRecordTarget);actual.put(ErpStockRecordService.class,stockRecords);
        approvalTarget=new ErpSaleReturnServiceImpl();inject(approvalTarget);approval=proxy(approvalTarget);
        ErpTradeReportRepository reports=new ErpTradeReportRepository();inject(reports);actual.put(ErpTradeReportRepository.class,reports);
        ErpTradeReportService reportTarget=new ErpTradeReportService();inject(reportTarget);set(reportTarget,"enabled",true);set(reportTarget,"cutover",cutover.toString());report=proxy(reportTarget);
        jdbc.update("INSERT INTO erp_customer(id,tenant_id,name,status,creator,deleted) VALUES(901,1,'return customer',0,'99',0)");
        login(1);
        IsolatedSaleDirectCursorMigration.apply(jdbc,"report_sale_direct_stock_test");
        writer.confirmOpening(101,201,new BigDecimal("2"),new BigDecimal("160"),new BigDecimal("180"),cutover,"independent current cost opening",99);
        writer.confirmOpening(102,201,new BigDecimal("2"),new BigDecimal("140"),new BigDecimal("170"),cutover,"independent second stock opening",99);
        set(writerTarget,"enabled",true);
        jdbc.update("UPDATE erp_stock SET lock_count=0");
        jdbc.update("UPDATE erp_customer SET dept_id=301 WHERE id=901");
        jdbc.update("INSERT INTO erp_customer_dept(id,tenant_id,customer_id,dept_id,deleted) VALUES(9001,1,901,301,0)");
        ErpStockLockServiceImpl lockTarget=new ErpStockLockServiceImpl();inject(lockTarget);locks=proxy(lockTarget);actual.put(ErpStockLockService.class,locks);
        ErpProductService products=mock(ErpProductService.class);
        when(products.validProductList(anyCollection())).thenAnswer(c->((ErpProductMapper)actual.get(ErpProductMapper.class)).selectByIds(c.getArgument(0)));
        actual.put(ErpProductService.class,products);
        ErpNoRedisDAO numbers=mock(ErpNoRedisDAO.class);AtomicLong sequence=new AtomicLong(1);
        when(numbers.generate(anyString())).thenAnswer(c->{if(initialTransferInsertBarrier!=null && ErpNoRedisDAO.STOCK_MOVE_NO_PREFIX.equals(c.getArgument(0)))initialTransferInsertBarrier.await(10,TimeUnit.SECONDS);return "DIRECT-"+sequence.getAndIncrement();});actual.put(ErpNoRedisDAO.class,numbers);
        ErpStockOutBillServiceImpl billTarget=new ErpStockOutBillServiceImpl();inject(billTarget);oldBills=proxy(billTarget);actual.put(ErpStockOutBillService.class,oldBills);
        ErpSaleDocumentDefaultService defaults=new ErpSaleDocumentDefaultService();inject(defaults);actual.put(ErpSaleDocumentDefaultService.class,defaults);
        ErpSaleOutServiceImpl saleTarget=new ErpSaleOutServiceImpl();inject(saleTarget);sales=proxy(saleTarget);actual.put(ErpSaleOutService.class,sales);
        ErpStockMoveServiceImpl moveTarget=new ErpStockMoveServiceImpl();inject(moveTarget);moves=proxy(moveTarget);actual.put(ErpStockMoveService.class,moves);
        cartTarget=new ErpSaleCartServiceImpl();inject(cartTarget);carts=proxy(cartTarget);
        ApplicationEventPublisher publisher=mock(ApplicationEventPublisher.class);
        doAnswer(c->{Object event=c.getArgument(0);if(event instanceof ErpSaleCartTransferOutApprovedEvent)new ErpSaleCartTransferOutApprovedListener(carts).onTransferOutApproved((ErpSaleCartTransferOutApprovedEvent)event);return null;}).when(publisher).publishEvent(any(Object.class));
        set(moveTarget,"eventPublisher",publisher);
    }
    void cleanup(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @AfterEach void restoreGlobalFixture(){cleanup();ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class,"beanFactory",previousSpringBeanFactory);}
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

    void prepareCart(long id, int status, long[] products, long[] warehouses, String... quantities) {
        BigDecimal total=Arrays.stream(quantities).map(BigDecimal::new).reduce(BigDecimal.ZERO,BigDecimal::add);
        jdbc.update("INSERT INTO erp_sale_cart(id,tenant_id,no,status,customer_id,dept_id,cart_time,total_count,total_product_price,total_price,discount_percent,fee_amount,other_price,creator,deleted) VALUES(?,1,?,?,901,301,?,?,?, ?,100,0,0,'99',0)",id,"CART-"+id,status,cutover.plusDays(2),total,total.multiply(new BigDecimal("100")),total.multiply(new BigDecimal("100")));
        for(int n=0;n<quantities.length;n++)jdbc.update("INSERT INTO erp_sale_cart_items(id,tenant_id,cart_id,product_id,warehouse_id,dept_id,product_unit_id,count,product_price,total_price,gift_flag,creator,deleted) VALUES(?,1,?,?,?,301,801,?,100,?,0,'99',0)",id*10+n,id,products[n],warehouses[n],new BigDecimal(quantities[n]),new BigDecimal(quantities[n]).multiply(new BigDecimal("100")));
    }
    void lockCart(long id){for(ErpSaleCartItemDO row:((ErpSaleCartItemMapper)actual.get(ErpSaleCartItemMapper.class)).selectListByCartId(id))locks.lockStock(row.getProductId(),row.getWarehouseId(),row.getCount(),30,id,row.getId(),"CART-"+id);}
    void assertSoldOnce(long cartId){assertEquals(50,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=?",Integer.class,cartId));assertEquals(1,count("erp_sale_out"));assertEquals(0,count("erp_stock_out_bill"));assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_sale_out",Integer.class));amount("0","SELECT lock_count FROM erp_stock WHERE id=11");assertEquals(3,jdbc.queryForObject("SELECT status FROM erp_stock_lock WHERE biz_id=?",Integer.class,cartId));}
    @Test void legacyWarehouseFlagTrueStillApprovesAndDeductsRealStockImmediately(){jdbc.update("UPDATE erp_warehouse SET stock_bill_enabled=1 WHERE id=201");prepareCart(701,30,new long[]{101},new long[]{201},"1");lockCart(701);carts.finalApproveSaleCart(701L);assertSoldOnce(701);amount("1","SELECT count FROM erp_stock WHERE id=11");amount("80","SELECT cost_amount FROM erp_stock WHERE id=11");amount("80","SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("90","SELECT settlement_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11");amount("-100","SELECT total_price FROM erp_stock_record");assertEquals(1,count("erp_business_report_item_snapshot"));}
    @Test void legacyWarehouseFlagFalseHasTheSameDirectFlow(){jdbc.update("UPDATE erp_warehouse SET stock_bill_enabled=0 WHERE id=201");prepareCart(701,30,new long[]{101},new long[]{201},"1");lockCart(701);carts.finalApproveSaleCart(701L);assertSoldOnce(701);assertEquals(1,count("erp_stock_dual_cost_posting"));}
    @Test void repeatedFinalApprovalCannotDuplicateStockOrReleaseAnotherLock(){prepareCart(701,30,new long[]{101},new long[]{201},"1");lockCart(701);carts.finalApproveSaleCart(701L);assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->carts.finalApproveSaleCart(701L));assertSoldOnce(701);assertEquals(1,count("erp_stock_record"));amount("1","SELECT count FROM erp_stock WHERE id=11");}

    @Test void mixedWarehouseFlagsProduceOneSaleAndDeductBothWarehouses(){
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,status,purchase_enabled,sale_enabled,stock_bill_enabled,creator,deleted) VALUES(202,1,301,'second warehouse',0,1,1,1,'99',0)");
        jdbc.update("UPDATE erp_warehouse SET stock_bill_enabled=0 WHERE id=201");
        jdbc.update("UPDATE erp_stock SET warehouse_id=202 WHERE id=12");
        prepareCart(701,30,new long[]{101,102},new long[]{201,202},"1","1");lockCart(701);carts.finalApproveSaleCart(701L);
        assertEquals(1,count("erp_sale_out"));assertEquals(2,count("erp_sale_out_items"));assertEquals(2,count("erp_stock_record"));assertEquals(2,count("erp_stock_dual_cost_posting"));assertEquals(0,count("erp_stock_out_bill"));amount("2","SELECT SUM(count) FROM erp_stock");amount("0","SELECT SUM(lock_count) FROM erp_stock");assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_lock WHERE status=3",Integer.class));
    }
    @Test void secondRealStockUpdateFailureRollsBackCartSaleAndEveryAccountingEffect(){
        prepareCart(701,30,new long[]{101,102},new long[]{201,201},"1","1");lockCart(701);failAfterStockItem=-102L;
        IllegalStateException error=assertThrows(IllegalStateException.class,()->carts.finalApproveSaleCart(701L));assertTrue(error.getMessage().contains("after actual stock"));
        assertEquals(30,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=701",Integer.class));assertEquals(0,count("erp_sale_out"));assertEquals(0,count("erp_sale_out_items"));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));assertEquals(0,count("erp_business_report_item_snapshot"));amount("4","SELECT SUM(count) FROM erp_stock");amount("300","SELECT SUM(cost_amount) FROM erp_stock");amount("350","SELECT SUM(settlement_amount) FROM erp_stock_dual_cost_balance");amount("2","SELECT SUM(lock_count) FROM erp_stock");amount("0","SELECT SUM(legacy_record_cursor_id) FROM erp_stock");assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_lock WHERE status=1",Integer.class));
    }
    @Test void legacyModeStillDirectlyDeductsWithNoNewAccountingTables(){
        set(writerTarget,"enabled",false);set(tradeTarget,"enabled",false);
        jdbc.execute("DROP TABLE erp_business_report_item_snapshot");jdbc.execute("DROP TABLE erp_stock_dual_cost_posting");jdbc.execute("DROP TABLE erp_stock_dual_cost_balance");
        jdbc.update("UPDATE erp_warehouse SET stock_bill_enabled=1 WHERE id=201");prepareCart(701,30,new long[]{101},new long[]{201},"1");lockCart(701);carts.finalApproveSaleCart(701L);assertSoldOnce(701);amount("1","SELECT count FROM erp_stock WHERE id=11");assertEquals(1,count("erp_stock_record"));
    }
    @Test void firstApprovalOldFlagDoesNotSkipShortageOrLocking(){
        jdbc.update("INSERT INTO erp_sale_config(id,tenant_id,config_type,code,config_value,deleted) VALUES(901,1,'SALE_CART_FIRST_APPROVE','GLOBAL','{\"enabled\":true,\"deptAuthEnabled\":false}',0)");
        jdbc.update("UPDATE erp_warehouse SET stock_bill_enabled=1 WHERE id=201");prepareCart(701,20,new long[]{101},new long[]{201},"3");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->carts.firstApproveSaleCart(701L));assertEquals(0,count("erp_stock_lock"));assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=701",Integer.class));
        jdbc.update("UPDATE erp_sale_cart_items SET count=1,total_price=100 WHERE cart_id=701");carts.firstApproveSaleCart(701L);amount("1","SELECT lock_count FROM erp_stock WHERE id=11");assertEquals(1,count("erp_stock_lock"));assertEquals(30,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=701",Integer.class));
    }
    @Test void oldBillWriteEntrypointsRejectEverySourceAndKeepHistoryUntouched(){
        for(Integer source:Arrays.asList(20,30,999,null)){
            jdbc.update("DELETE FROM erp_stock_out_bill");jdbc.update("INSERT INTO erp_stock_out_bill(id,tenant_id,no,source_biz_type,source_id,status,warehouse_id,total_count,picked_count,deleted) VALUES(801,1,'HISTORICAL',?,701,10,201,1,0,0)",source);
            ErpStockOutBillPickReqVO request=new ErpStockOutBillPickReqVO();request.setId(801L);
            cn.iocoder.yudao.framework.common.exception.ServiceException error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->oldBills.pick(request));assertEquals(409,error.getCode());assertNotNull(oldBills.getStockOutBill(801L));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_stock_out_bill WHERE id=801",Integer.class));
        }
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->oldBills.createFromSaleOut(new ErpSaleOutDO().setId(701L),Collections.emptyList()));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));amount("2","SELECT count FROM erp_stock WHERE id=11");
    }
    @Test void explicitDeferArgumentCannotBypassNormalSalePosting(){
        ErpSaleOutSaveReqVO request=new ErpSaleOutSaveReqVO();request.setCustomerId(901L);request.setDeptId(301L);request.setOutTime(cutover.plusDays(3));request.setDiscountPercent(new BigDecimal("100"));
        ErpSaleOutSaveReqVO.Item item=new ErpSaleOutSaveReqVO.Item();item.setProductId(101L);item.setWarehouseId(201L);item.setDeptId(301L);item.setCount(BigDecimal.ONE);item.setProductPrice(new BigDecimal("100"));request.setItems(Collections.singletonList(item));
        sales.createGeneratedSaleOut(request,30,701L,"CART-701",true);assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_stock_dual_cost_posting"));assertEquals(0,count("erp_stock_out_bill"));amount("1","SELECT count FROM erp_stock WHERE id=11");
    }
    @Test void quoteAndCartNumericIdCollisionDoesNotEraseOccupiedStockInBothQueries(){
        prepareCart(701,20,new long[]{101},new long[]{201},"1");jdbc.update("INSERT INTO erp_sale_out(id,tenant_id,no,status,source_type,source_id,dept_id,deleted) VALUES(901,1,'UNRELATED-QUOTE',20,20,701,301,0)");
        ErpStockMapper mapper=(ErpStockMapper)actual.get(ErpStockMapper.class);
        assertEquals(0,BigDecimal.ONE.compareTo(mapper.selectOccupiedCountMap(Arrays.asList(101L,102L),Collections.singletonList(201L)).get("101_201")));
        ErpStockPageReqVO request=new ErpStockPageReqVO();request.setPageNo(1);request.setPageSize(20);request.setOrderDirection("asc");
        assertEquals(2,mapper.selectPage(request).getList().size());
        List<ErpStockDO> sorted=mapper.selectPageOrderByAvailableCount(request,null,null,null,null,null,null,null,null).getList();assertEquals(11L,sorted.get(0).getId());
        jdbc.update("UPDATE erp_sale_out SET source_type=30 WHERE id=901");assertEquals(0,BigDecimal.ZERO.compareTo(mapper.selectOccupiedCountMap(Arrays.asList(101L,102L),Collections.singletonList(201L)).get("101_201")));
        sorted=mapper.selectPageOrderByAvailableCount(request,null,null,null,null,null,null,null,null).getList();assertEquals(12L,sorted.get(0).getId());
    }

    void prepareCrossDepartmentTransfer(){
        jdbc.update("UPDATE erp_warehouse SET dept_id=302 WHERE id=201");jdbc.update("UPDATE erp_stock SET dept_id=302 WHERE warehouse_id=201");jdbc.update("UPDATE erp_stock_dual_cost_balance SET stock_dept_id=302 WHERE stock_id IN (11,12)");
        jdbc.update("INSERT INTO erp_warehouse_sale_dept_permission(id,tenant_id,warehouse_id,dept_id,deleted) VALUES(1,1,201,301,0)");
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,status,purchase_enabled,sale_enabled,stock_bill_enabled,creator,deleted) VALUES(202,1,301,'direct warehouse',0,1,1,1,'99',0)");
        jdbc.update("INSERT INTO erp_stock(id,tenant_id,product_id,warehouse_id,dept_id,count,lock_count,cost_price,cost_amount,creator,deleted) VALUES(13,1,101,202,301,0,0,0,0,'99',0)");
        prepareCart(701,30,new long[]{101},new long[]{201},"1");lockCart(701);
        jdbc.update("INSERT INTO erp_stock_move(id,tenant_id,no,status,transfer_direction,source_type,source_id,source_no,dept_id,from_dept_id,to_dept_id,move_time,total_count,total_price,creator,deleted) VALUES(801,1,'TRANSFER-801',10,10,30,701,'CART-701',301,302,301,?,1,100,'99',0)",cutover.plusDays(2));
        jdbc.update("INSERT INTO erp_stock_move_item(id,tenant_id,move_id,product_id,product_unit_id,from_warehouse_id,to_warehouse_id,from_dept_id,to_dept_id,count,product_price,total_price,creator,deleted) VALUES(8011,1,801,101,801,201,202,302,301,1,100,100,'99',0)");
    }
    @Test void realTransferApprovalListenerGeneratesDirectSaleAndMovesThenDeductsLocks(){
        set(writerTarget,"enabled",false);set(tradeTarget,"enabled",false);prepareCrossDepartmentTransfer();moves.updateStockMoveStatus(801L,20);
        assertEquals(50,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=701",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_move WHERE status=20",Integer.class));assertEquals(1,count("erp_sale_out"));assertEquals(3,count("erp_stock_record"));assertEquals(0,count("erp_stock_out_bill"));
        amount("1","SELECT count FROM erp_stock WHERE id=11");amount("0","SELECT count FROM erp_stock WHERE id=13");amount("0","SELECT SUM(lock_count) FROM erp_stock");assertEquals(3,jdbc.queryForObject("SELECT status FROM erp_stock_lock WHERE biz_id=701",Integer.class));assertEquals(202L,jdbc.queryForObject("SELECT warehouse_id FROM erp_sale_out_items",Long.class));assertEquals(201L,jdbc.queryForObject("SELECT source_warehouse_id FROM erp_sale_out_items",Long.class));assertEquals(301L,jdbc.queryForObject("SELECT dept_id FROM erp_sale_out",Long.class));
    }
    @Test void failureAfterSaleStockWriteRollsBackTransferMirrorInventoryAndCartTogether(){
        set(writerTarget,"enabled",false);set(tradeTarget,"enabled",false);prepareCrossDepartmentTransfer();failSaleAfterActualStock=true;
        IllegalStateException error=assertThrows(IllegalStateException.class,()->moves.updateStockMoveStatus(801L,20));assertTrue(error.getMessage().contains("within transfer listener"));
        assertEquals(1,count("erp_stock_move"));assertEquals(1,count("erp_stock_move_item"));assertEquals(10,jdbc.queryForObject("SELECT status FROM erp_stock_move WHERE id=801",Integer.class));assertNull(jdbc.queryForObject("SELECT related_move_id FROM erp_stock_move WHERE id=801",Long.class));assertEquals(30,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=701",Integer.class));assertEquals(0,count("erp_sale_out"));assertEquals(0,count("erp_stock_record"));amount("2","SELECT count FROM erp_stock WHERE id=11");amount("0","SELECT count FROM erp_stock WHERE id=13");amount("1","SELECT lock_count FROM erp_stock WHERE id=11");amount("0","SELECT lock_count FROM erp_stock WHERE id=13");assertEquals(201L,jdbc.queryForObject("SELECT warehouse_id FROM erp_stock_lock WHERE biz_id=701",Long.class));assertEquals(1,jdbc.queryForObject("SELECT status FROM erp_stock_lock WHERE biz_id=701",Integer.class));
    }
    @Test void newAccountingRejectsUnadaptedTransferWithoutMirrorOrPartialPosting(){
        prepareCrossDepartmentTransfer();RuntimeException error=assertThrows(RuntimeException.class,()->moves.updateStockMoveStatus(801L,20));assertTrue(error.getMessage().contains("尚未")||error.getMessage().contains("未适配")||error.getMessage().contains("不支持"),error.getMessage());assertEquals(1,count("erp_stock_move"));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_stock_dual_cost_posting"));assertEquals(0,count("erp_sale_out"));amount("2","SELECT count FROM erp_stock WHERE id=11");assertEquals(30,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=701",Integer.class));
    }
    ErpSaleCartSaveReqVO cartEdit(long id,String quantity,String price){
        ErpSaleCartSaveReqVO request=new ErpSaleCartSaveReqVO();request.setId(id);request.setCustomerId(901L);request.setDeptId(301L);request.setDiscountPercent(new BigDecimal("100"));
        ErpSaleCartSaveReqVO.Item item=new ErpSaleCartSaveReqVO.Item();item.setId(id*10);item.setProductId(101L);item.setWarehouseId(201L);item.setDeptId(301L);item.setCount(new BigDecimal(quantity));item.setProductPrice(new BigDecimal(price));request.setItems(Collections.singletonList(item));return request;
    }
    static void await(CountDownLatch latch){try{assertTrue(latch.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
    @Test void finalApprovalAfterOldReadViewUsesCommittedEditedHeaderAndEveryItem()throws Exception {
        prepareCart(701,20,new long[]{101},new long[]{201},"1");jdbc.update("INSERT INTO erp_sale_config(id,tenant_id,config_type,code,config_value,deleted) VALUES(901,1,'SALE_CART_FIRST_APPROVE','GLOBAL','{\"enabled\":false}',0)");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch oldRead=new CountDownLatch(1),edited=new CountDownLatch(1),releaseEdit=new CountDownLatch(1);
        try {
            Future<List<Long>> approveFuture=pool.submit(()->{login(1);try{TransactionTemplate rr=new TransactionTemplate(manager);rr.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);return rr.execute(s->{jdbc.queryForObject("SELECT total_price FROM erp_sale_cart WHERE id=701",BigDecimal.class);oldRead.countDown();await(edited);return carts.finalApproveSaleCart(701L);});}finally{cleanup();}});
            Future<?> editFuture=pool.submit(()->{login(1);try{await(oldRead);tx.executeWithoutResult(s->{jdbc.queryForObject("SELECT id FROM erp_sale_cart WHERE id=701 FOR UPDATE",Long.class);carts.updateSaleCart(cartEdit(701,"2","120"));edited.countDown();await(releaseEdit);});}finally{cleanup();}});
            await(edited);boolean waiting=false;for(int n=0;n<150;n++){if(jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits",Integer.class)>0){waiting=true;break;}Thread.sleep(20);}assertTrue(waiting,"approval must actually wait on the editing transaction");releaseEdit.countDown();editFuture.get(15,TimeUnit.SECONDS);assertEquals(1,approveFuture.get(15,TimeUnit.SECONDS).size());
            amount("2","SELECT count FROM erp_sale_out_items");amount("120","SELECT product_price FROM erp_sale_out_items");amount("240","SELECT total_price FROM erp_sale_out");amount("0","SELECT count FROM erp_stock WHERE id=11");amount("-160","SELECT financial_movement FROM erp_stock_dual_cost_posting");
        } finally {releaseEdit.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }

    @Test void finalTransferApprovalWaitingOnCancelCannotInvertCartAndMoveLocks()throws Exception {
        set(writerTarget,"enabled",false);set(tradeTarget,"enabled",false);prepareCrossDepartmentTransfer();
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch cartLocked=new CountDownLatch(1),cancelNow=new CountDownLatch(1);
        try {
            Future<?> cancel=pool.submit(()->{login(1);try{tx.executeWithoutResult(s->{jdbc.queryForObject("SELECT id FROM erp_sale_cart WHERE id=701 FOR UPDATE",Long.class);cartLocked.countDown();await(cancelNow);carts.cancelFirstApproveSaleCart(701L);});}finally{cleanup();}});
            await(cartLocked);Future<Throwable> approve=pool.submit(()->{login(1);try{moves.updateStockMoveStatus(801L,20);return null;}catch(Throwable e){return e;}finally{cleanup();}});
            boolean waiting=false;for(int n=0;n<150;n++){if(jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits",Integer.class)>0){waiting=true;break;}Thread.sleep(20);}assertTrue(waiting,"move approval must actually wait on cart before locking move");cancelNow.countDown();cancel.get(15,TimeUnit.SECONDS);Throwable error=approve.get(15,TimeUnit.SECONDS);assertInstanceOf(cn.iocoder.yudao.framework.common.exception.ServiceException.class,error);
            assertEquals(20,jdbc.queryForObject("SELECT status FROM erp_sale_cart WHERE id=701",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_move WHERE deleted=0",Integer.class));assertEquals(0,count("erp_stock_record"));assertEquals(0,count("erp_sale_out"));amount("0","SELECT SUM(lock_count) FROM erp_stock");assertEquals(2,jdbc.queryForObject("SELECT status FROM erp_stock_lock WHERE biz_id=701",Integer.class));
        } finally {cancelNow.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }
    @Test void twoConcurrentFinalApprovalsCreateExactlyOneRealSale()throws Exception {
        prepareCart(701,30,new long[]{101},new long[]{201},"1");lockCart(701);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        Callable<Boolean> task=()->{login(1);try{await(start);carts.finalApproveSaleCart(701L);return true;}catch(cn.iocoder.yudao.framework.common.exception.ServiceException expected){return false;}finally{cleanup();}};
        try{Future<Boolean> a=pool.submit(task),b=pool.submit(task);start.countDown();assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertSoldOnce(701);assertEquals(1,count("erp_stock_record"));assertEquals(1,count("erp_stock_dual_cost_posting"));}finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }

    @Test void differentNewCartsCanCreateInitialTransferDraftsWithoutEmptyRangeDeadlock()throws Exception {
        set(writerTarget,"enabled",false);set(tradeTarget,"enabled",false);prepareCrossDepartmentTransfer();
        jdbc.update("DELETE FROM erp_stock_move_item");jdbc.update("DELETE FROM erp_stock_move");jdbc.update("DELETE FROM erp_stock_lock");jdbc.update("UPDATE erp_stock SET lock_count=0");jdbc.update("UPDATE erp_sale_cart SET status=20 WHERE id=701");jdbc.update("UPDATE erp_warehouse SET name='直发仓' WHERE id=202");
        jdbc.update("INSERT INTO erp_stock(id,tenant_id,product_id,warehouse_id,dept_id,count,lock_count,cost_price,cost_amount,creator,deleted) VALUES(14,1,102,202,301,0,0,0,0,'99',0)");prepareCart(702,20,new long[]{102},new long[]{201},"1");
        initialTransferInsertBarrier=new CyclicBarrier(2);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        Callable<Void> first=()->{login(1);try{await(start);carts.firstApproveSaleCart(701L);return null;}finally{cleanup();}};
        Callable<Void> second=()->{login(1);try{await(start);carts.firstApproveSaleCart(702L);return null;}finally{cleanup();}};
        try{Future<Void> a=pool.submit(first),b=pool.submit(second);start.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_sale_cart WHERE status=30",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_move WHERE transfer_direction=10 AND deleted=0",Integer.class));assertEquals(0,count("erp_stock_record"));amount("2","SELECT SUM(lock_count) FROM erp_stock");}finally{initialTransferInsertBarrier=null;pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }
}
