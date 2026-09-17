package cn.iocoder.yudao.module.erp.service.purchase.cost;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.datapermission.core.db.DataPermissionRuleHandler;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRuleFactoryImpl;
import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpPurchaseCostConfirmationModels.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.*;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.*;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.*;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.framework.datapermission.config.ErpDataPermissionConfiguration;
import cn.iocoder.yudao.module.erp.service.stock.*;
import cn.iocoder.yudao.module.erp.service.stock.cost.*;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.purchase.*;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
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

/** Real MySQL/MyBatis tenant+department interceptors and real Spring transaction proxies. */
@EnabledIfEnvironmentVariable(named="ERP_PURCHASE_COST_MYSQL_TEST_URL",matches=".+")
class ErpPurchaseCostMysqlIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    TransactionTemplate tx;
    ErpPurchaseCostConfirmationService confirmation;
    ErpPurchaseCostConfirmationService confirmationTarget;
    ErpPurchaseInMapper headers;
    ErpPurchaseInItemMapper items;
    ErpStockMapper stocks;
    ErpWarehouseMapper warehouses;
    ErpProductMapper products;
    ErpWarehouseServiceImpl warehouseService;
    ErpDualCostPostingService writer;
    ErpDualCostPostingService writerTarget;
    ErpTradeSnapshotService trade,tradeTarget;
    ErpStockRecordService stockRecordAdapter;
    PermissionApi permissions;
    ErpPurchaseInService approval;
    ErpStockInBillService oldPickup;
    volatile boolean failSecondLine;
    DeptDataPermissionRespDTO purchasePermission,stockPermission;
    final LocalDateTime cutover=LocalDateTime.of(2026,1,1,0,0);

    @BeforeEach void setup() throws Exception {
        String url=System.getenv("ERP_PURCHASE_COST_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_purchase_cost_test(?:\\?.*)?"));
        DriverManagerDataSource ds=new DriverManagerDataSource(url,"root","");jdbc=new JdbcTemplate(ds);
        assertTrue(jdbc.queryForObject("SELECT @@datadir",String.class).replace('\\','/').contains("/Temp/xingyu-report-mysql-"));
        manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);
        for(String name:Arrays.asList("erp_report_dual_cost_foundation_20260909.sql","erp_report_purchase_cost_confirmation_20260909.sql","erp_trade_snapshot_v224.sql")) {
            String ddl=new String(Files.readAllBytes(Paths.get("../sql/mysql/"+name)),StandardCharsets.UTF_8);
            for(String statement:ddl.split(";")) if(!statement.trim().isEmpty())jdbc.execute(statement);
        }
        for(Class<?> type:Arrays.asList(ErpPurchaseInDO.class,ErpPurchaseInItemDO.class,ErpStockDO.class,ErpWarehouseDO.class,ErpProductDO.class,ErpSupplierDO.class)) createEntityTable(type);
        createEntityTable(cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO.class);
        createEntityTable(cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO.class);
        String lockDdl=new String(Files.readAllBytes(Paths.get("../sql/mysql/erp_report_source_item_lock_index_20260909.sql")),StandardCharsets.UTF_8);
        tx.executeWithoutResult(status->{for(String statement:lockDdl.split(";"))if(!statement.trim().isEmpty())jdbc.execute(statement);});
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_product_unit(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_dept(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,nickname VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_stock_record(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,product_id BIGINT,warehouse_id BIGINT,deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for(String table:Arrays.asList("erp_business_report_item_snapshot","erp_supplier","erp_product_unit","erp_purchase_cost_posting_link","erp_purchase_cost_confirmation_line","erp_purchase_cost_confirmation","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance","erp_stock_record","erp_purchase_in_items","erp_purchase_in","erp_stock","erp_warehouse","erp_product"))jdbc.execute("DELETE FROM "+table);
        jdbc.update("INSERT INTO erp_supplier(id,tenant_id,name,deleted) VALUES(501,1,'supplier snapshot fixture',0)");
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,status,stock_bill_enabled,creator,deleted) VALUES(201,1,301,'legacy enabled warehouse',0,1,'99',0)");
        jdbc.update("INSERT INTO erp_product(id,tenant_id,code,name,creator,deleted) VALUES(101,1,'P101','part A','99',0),(102,1,'P102','part B','99',0)");
        jdbc.update("INSERT INTO erp_stock(id,tenant_id,product_id,warehouse_id,dept_id,count,cost_price,cost_amount,creator,deleted) VALUES(11,1,101,201,301,10,100,1000,'99',0),(12,1,102,201,301,10,100,1000,'99',0)");
        jdbc.update("INSERT INTO erp_purchase_in(id,tenant_id,no,status,supplier_id,dept_id,in_time,total_count,total_product_price,total_price,discount_price,fee_amount,creator,deleted) VALUES(401,1,'PUR-401',?,501,301,?,6,690,690,0,0,'99',0),(402,2,'OTHER-TENANT',?,501,301,?,6,690,690,0,0,'99',0)",ErpAuditStatus.PROCESS.getStatus(),cutover.plusDays(1),ErpAuditStatus.PROCESS.getStatus(),cutover.plusDays(1));
        jdbc.update("INSERT INTO erp_purchase_in_items(id,tenant_id,in_id,product_id,warehouse_id,dept_id,count,product_price,total_price,gift,creator,deleted) VALUES(601,1,401,101,201,301,3,220,660,0,'99',0),(602,1,401,102,201,301,3,10,30,0,'99',0)");
        permissions=mock(PermissionApi.class);purchasePermission=new DeptDataPermissionRespDTO();purchasePermission.setAll(true);
        stockPermission=new DeptDataPermissionRespDTO();stockPermission.setAll(true);
        when(permissions.getDeptDataPermission(eq(99L),anyString())).thenAnswer(call->"erp_stock".equals(call.getArgument(1))?stockPermission:purchasePermission);
        MybatisConfiguration config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);config.setCacheEnabled(false);
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(TenantContextHolder.getRequiredTenantId());}}));
        DeptDataPermissionRule rule=new DeptDataPermissionRule(permissions);new ErpDataPermissionConfiguration().erpDeptDataPermissionRuleCustomizer().customize(rule);
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new DataPermissionRuleHandler(new DataPermissionRuleFactoryImpl(Collections.singletonList(rule)))));
        MybatisSqlSessionFactoryBean bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(config);bean.setPlugins(interceptor);
        bean.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setLogicDeleteValue("1").setLogicNotDeleteValue("0")));
        SqlSessionFactory factory=bean.getObject();
        for(Class<?> type:Arrays.asList(ErpPurchaseInMapper.class,ErpPurchaseInItemMapper.class,ErpStockMapper.class,ErpWarehouseMapper.class,ErpProductMapper.class))factory.getConfiguration().addMapper(type);
        SqlSessionTemplate template=new SqlSessionTemplate(factory);
        headers=template.getMapper(ErpPurchaseInMapper.class);items=template.getMapper(ErpPurchaseInItemMapper.class);stocks=template.getMapper(ErpStockMapper.class);warehouses=template.getMapper(ErpWarehouseMapper.class);products=template.getMapper(ErpProductMapper.class);
        warehouseService=new ErpWarehouseServiceImpl();set(warehouseService,"permissionApi",permissions);set(warehouseService,"warehouseMapper",warehouses);
        ErpStockService stockService=mock(ErpStockService.class);when(stockService.getStock(anyLong(),anyLong())).thenAnswer(call->stocks.selectOne(new LambdaQueryWrapperX<ErpStockDO>().eq(ErpStockDO::getProductId,call.getArgument(0)).eq(ErpStockDO::getWarehouseId,call.getArgument(1))));
        ErpPurchaseCostConfirmationRepository repository=new ErpPurchaseCostConfirmationRepository();set(repository,"jdbcTemplate",jdbc);
        confirmationTarget=new ErpPurchaseCostConfirmationService();set(confirmationTarget,"purchaseInMapper",headers);set(confirmationTarget,"itemMapper",items);set(confirmationTarget,"productMapper",products);set(confirmationTarget,"repository",repository);set(confirmationTarget,"permissionApi",permissions);set(confirmationTarget,"warehouseService",warehouseService);set(confirmationTarget,"enabled",true);confirmation=proxy(confirmationTarget);
        for(Field field:ErpPurchaseCostConfirmationService.class.getDeclaredFields())if(field.getType()==ErpStockMapper.class)set(confirmationTarget,field.getName(),stocks);
        ErpDualCostLedgerRepository ledger=new ErpDualCostLedgerRepository();set(ledger,"jdbcTemplate",jdbc);
        tradeTarget=new ErpTradeSnapshotService();set(tradeTarget,"jdbcTemplate",jdbc);set(tradeTarget,"purchaseInMapper",headers);set(tradeTarget,"purchaseInItemMapper",items);set(tradeTarget,"enabled",true);trade=proxy(tradeTarget);
        writerTarget=new ErpDualCostPostingService();set(writerTarget,"repository",ledger);set(writerTarget,"enabled",false);set(writerTarget,"cutover",cutover.toString());
        // The production writer resolves persisted confirmation references; injected after author hook exists.
        for(Field field:ErpDualCostPostingService.class.getDeclaredFields())if(field.getType()==ErpPurchaseCostConfirmationService.class)set(writerTarget,field.getName(),confirmation);
        for(Field field:ErpDualCostPostingService.class.getDeclaredFields())if(field.getType()==ErpTradeSnapshotService.class)set(writerTarget,field.getName(),trade);
        writer=proxy(writerTarget);login(1);
        writer.confirmOpening(101,201,new BigDecimal("10"),new BigDecimal("1000"),new BigDecimal("1000"),cutover,"opening",99);
        writer.confirmOpening(102,201,new BigDecimal("10"),new BigDecimal("1000"),new BigDecimal("1000"),cutover,"opening",99);
        set(writerTarget,"enabled",true);
        ErpPurchaseInServiceImpl approvalTarget=new ErpPurchaseInServiceImpl();
        for(Field field:ErpPurchaseInServiceImpl.class.getDeclaredFields())if(field.getType()==ErpTradeSnapshotService.class)set(approvalTarget,field.getName(),trade);
        set(approvalTarget,"purchaseInMapper",headers);set(approvalTarget,"purchaseInItemMapper",items);
        set(approvalTarget,"warehouseService",warehouseService);set(approvalTarget,"costConfirmationService",confirmation);
        ErpSupplierService supplier=mock(ErpSupplierService.class);when(supplier.validateSupplier(501L)).thenReturn(new ErpSupplierDO().setId(501L));
        set(approvalTarget,"supplierService",supplier);
        ErpSupplierDeptPermissionService supplierScope=mock(ErpSupplierDeptPermissionService.class);when(supplierScope.hasAvailableDept(any(),any(),any())).thenReturn(true);set(approvalTarget,"supplierDeptPermissionService",supplierScope);
        ErpProductService productService=mock(ErpProductService.class);doAnswer(call->{jdbc.update("UPDATE erp_product SET last_purchase_price=? WHERE id=? AND tenant_id=?",call.getArgument(1),call.getArgument(0),TenantContextHolder.getRequiredTenantId());return null;}).when(productService).updateProductLastPurchasePrice(anyLong(),any(BigDecimal.class));set(approvalTarget,"productService",productService);
        set(approvalTarget,"operateLogService",mock(ErpOperateLogService.class));oldPickup=mock(ErpStockInBillService.class);set(approvalTarget,"stockInBillService",oldPickup);
        ErpStockRecordService stockRecords=mock(ErpStockRecordService.class);stockRecordAdapter=stockRecords;
        doAnswer(call->{ErpStockRecordCreateReqBO bo=call.getArgument(0);Runnable legacy=()->{
            jdbc.update("UPDATE erp_stock SET count=count+?,cost_amount=cost_amount+?*? WHERE tenant_id=? AND product_id=? AND warehouse_id=?",bo.getCount(),bo.getCount(),bo.getUnitPrice(),TenantContextHolder.getRequiredTenantId(),bo.getProductId(),bo.getWarehouseId());
            jdbc.update("INSERT INTO erp_stock_record(tenant_id,product_id,warehouse_id) VALUES(?,?,?)",TenantContextHolder.getRequiredTenantId(),bo.getProductId(),bo.getWarehouseId());
            if(failSecondLine&&bo.getBizItemId()==602)throw new IllegalStateException("independent second-line failure");
        };if(!writer.post(bo,legacy))legacy.run();return null;}).when(stockRecords).createStockRecord(any());set(approvalTarget,"stockRecordService",stockRecords);
        approval=proxy(approvalTarget);
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    void login(long tenant){TenantContextHolder.setTenantId(tenant);LoginUser user=new LoginUser();user.setId(99L);user.setUserType(UserTypeEnum.ADMIN.getValue());user.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID,"301"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));}
    static void set(Object object,String name,Object value){ReflectionTestUtils.setField(object,name,value);}
    @SuppressWarnings("unchecked") <T>T proxy(T target){ProxyFactory factory=new ProxyFactory(target);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));return (T)factory.getProxy();}
    void createEntityTable(Class<?> type){
        String table=type.getAnnotation(TableName.class).value();Map<String,String> columns=new LinkedHashMap<>();columns.put("tenant_id","BIGINT");
        for(Class<?> current=type;current!=Object.class;current=current.getSuperclass())for(Field field:current.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers()))continue;TableField annotation=field.getAnnotation(TableField.class);if(annotation!=null&&!annotation.exist())continue;
            String name=annotation!=null&&!annotation.value().isEmpty()?annotation.value():field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
            Class<?> value=field.getType();String sql=value==BigDecimal.class?"DECIMAL(24,6)":value==Long.class?"BIGINT":value==Integer.class?"INT":value==Boolean.class?"BIT":value==LocalDateTime.class?"DATETIME(6)":"TEXT";
            if("id".equals(name))sql="BIGINT PRIMARY KEY";if("deleted".equals(name))sql="BIT DEFAULT 0";columns.put(name,sql);
        }
        List<String> definitions=new ArrayList<>();columns.forEach((key,value)->definitions.add("`"+key+"` "+value));jdbc.execute("CREATE TABLE IF NOT EXISTS "+table+" ("+String.join(",",definitions)+") ENGINE=InnoDB");
    }
    ConfirmRequest request(String key){Header preview=confirmation.preview(401L);ConfirmRequest r=new ConfirmRequest();r.setPurchaseInId(401L);r.setExpectedSignature(preview.getSourceSignature());r.setExpectedRevision(preview.getLatestRevision());r.setRequestKey(key);r.setEvidence("independent confirmed net amounts");r.setFeeTreatment("explicit fee reconciliation; unknown source tax remains unknown");r.setItems(new ArrayList<>());for(long id:new long[]{601,602}){ConfirmItem line=new ConfirmItem();line.setSourceItemId(id);line.setConfirmedNetTotalAmount(new BigDecimal(id==601?"650":"30"));line.setEvidence("source reconciliation "+id);r.getItems().add(line);}return r;}
    void amount(String expected,BigDecimal actual){assertNotNull(actual);assertEquals(0,new BigDecimal(expected).compareTo(actual));}

    @Test void realMapperTenantAndDepartmentGuardsProtectPreviewAndConfirm(){
        assertEquals("NONE",confirmation.preview(401L).getStatus());
        assertThrows(ServiceException.class,()->confirmation.preview(402L));
        purchasePermission.setAll(false);purchasePermission.setDeptIds(Collections.singleton(999L));login(1);
        assertThrows(ServiceException.class,()->confirmation.preview(401L));
        login(2);assertThrows(ServiceException.class,()->confirmation.preview(401L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_confirmation",Integer.class));
    }
    @Test void wholeDocumentConfirmationRejectsMissingPagesAndPreservesUnknownTax(){
        ConfirmRequest r=request("all-pages");Header preview=confirmation.preview(401L);
        assertEquals(2,confirmation.itemPage(401L,preview.getSourceSignature(),1,1).getTotal());
        assertEquals(Long.valueOf(602),confirmation.itemPage(401L,preview.getSourceSignature(),2,1).getList().get(0).getSourceItemId());
        ConfirmItem second=r.getItems().remove(1);assertThrows(ServiceException.class,()->confirmation.confirm(r));r.getItems().add(second);
        Header confirmed=confirmation.confirm(r);assertEquals("UNKNOWN",confirmed.getTaxStatus());assertEquals("INCLUSIVE_UNCONFIRMED",confirmed.getPriceBasis());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_confirmation_line",Integer.class));
        amount("650",confirmation.itemPage(401L,preview.getSourceSignature(),1,1).getList().get(0).getConfirmedNetTotalAmount());
    }
    @Test void requestRetriesRequireIdenticalPayloadAndCannotReplaceConsumedVersion(){
        ConfirmRequest r=request("retry");Header first=confirmation.confirm(r);Header retry=confirmation.confirm(r);assertEquals(first.getConfirmationId(),retry.getConfirmationId());
        r.getItems().get(0).setConfirmedNetTotalAmount(new BigDecimal("649"));assertThrows(ServiceException.class,()->confirmation.confirm(r));
        approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());
        assertThrows(ServiceException.class,()->confirmation.confirm(request("replace-consumed")));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_confirmation",Integer.class));
    }
    @Test void sourcePriceChangeInvalidatesEarlierConfirmation(){
        confirmation.confirm(request("before-change"));jdbc.update("UPDATE erp_purchase_in_items SET product_price=221 WHERE id=601");
        assertEquals("STALE",confirmation.preview(401L).getStatus());
        assertThrows(ServiceException.class,()->approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus()));
        assertNull(jdbc.queryForObject("SELECT consumed_at FROM erp_purchase_cost_confirmation",LocalDateTime.class));
    }
    @Test void stockCreatorAndCostFieldPermissionsApplyToAllPagesAndConfirmation(){
        stockPermission.setAll(false);stockPermission.setSelf(true);jdbc.update("UPDATE erp_stock SET creator='88' WHERE id=12");login(1);
        assertThrows(RuntimeException.class,()->confirmation.preview(401L));
        jdbc.update("UPDATE erp_stock SET creator='99' WHERE id=12");
        ConfirmRequest r=request("masked");when(permissions.getCurrentUserHiddenFields("erp_product",301L)).thenReturn(Collections.singletonList("costAmount"));
        Header preview=confirmation.preview(401L);assertTrue(preview.isCostMasked());assertNull(preview.getRawProductAmount());
        assertNull(confirmation.itemPage(401L,preview.getSourceSignature(),2,1).getList().get(0).getRawUnitPrice());
        assertThrows(ServiceException.class,()->confirmation.confirm(r));
    }
    @Test void disabledApprovalDoesNotRequireNewConfirmationTables(){
        set(confirmationTarget,"enabled",false);
        set(writerTarget,"enabled",false);
        set(tradeTarget,"enabled",false);
        jdbc.execute("RENAME TABLE erp_purchase_cost_confirmation TO saved_purchase_confirmation,erp_purchase_cost_confirmation_line TO saved_purchase_confirmation_line,erp_purchase_cost_posting_link TO saved_purchase_link,erp_stock_dual_cost_balance TO saved_dual_balance,erp_stock_dual_cost_posting TO saved_dual_posting");
        try {approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());amount("13",jdbc.queryForObject("SELECT count FROM erp_stock WHERE id=11",BigDecimal.class));verifyNoInteractions(oldPickup);}
        finally {jdbc.execute("RENAME TABLE saved_purchase_confirmation TO erp_purchase_cost_confirmation,saved_purchase_confirmation_line TO erp_purchase_cost_confirmation_line,saved_purchase_link TO erp_purchase_cost_posting_link,saved_dual_balance TO erp_stock_dual_cost_balance,saved_dual_posting TO erp_stock_dual_cost_posting");}
    }

    @Test void approvalUsesExactAmountFor650DividedBy3AndIgnoresLegacyPickupFlag(){
        confirmation.confirm(request("amount-priority"));approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());
        amount("650",jdbc.queryForObject("SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE biz_item_id=601",BigDecimal.class));
        amount("1650",jdbc.queryForObject("SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11",BigDecimal.class));
        amount("13",jdbc.queryForObject("SELECT count FROM erp_stock WHERE id=11",BigDecimal.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_posting_link",Integer.class));
        assertEquals("CONSUMED",confirmation.preview(401L).getStatus());verifyNoInteractions(oldPickup);
        assertThrows(ServiceException.class,()->approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus()));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_record",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_business_report_item_snapshot",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_business_report_item_snapshot WHERE net_amount IS NOT NULL",Integer.class));
    }
    @Test void secondLineFailureRollsBackActualPurchaseStatusLegacyStockNewLedgerAndConsumption(){
        confirmation.confirm(request("atomic"));failSecondLine=true;
        assertThrows(IllegalStateException.class,()->approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus()));
        assertEquals(ErpAuditStatus.PROCESS.getStatus(),jdbc.queryForObject("SELECT status FROM erp_purchase_in WHERE id=401",Integer.class));
        amount("10",jdbc.queryForObject("SELECT count FROM erp_stock WHERE id=11",BigDecimal.class));
        amount("10",jdbc.queryForObject("SELECT count FROM erp_stock WHERE id=12",BigDecimal.class));
        amount("1000",jdbc.queryForObject("SELECT financial_amount FROM erp_stock_dual_cost_balance WHERE stock_id=11",BigDecimal.class));
        for(String table:Arrays.asList("erp_stock_record","erp_stock_dual_cost_posting","erp_purchase_cost_posting_link"))assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
        assertNull(jdbc.queryForObject("SELECT consumed_at FROM erp_purchase_cost_confirmation",LocalDateTime.class));
        assertNull(jdbc.queryForObject("SELECT last_purchase_price FROM erp_product WHERE id=101",BigDecimal.class));
        failSecondLine=false;approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());assertEquals("CONSUMED",confirmation.preview(401L).getStatus());
    }
    @Test void missingPostsAbortCommitAndSavedReferenceCannotPostInAnotherTransaction() {
        confirmation.confirm(request("missing-posts"));
        List<ErpStockRecordCreateReqBO> captured=new ArrayList<>();
        assertThrows(ServiceException.class,()->tx.execute(status->{
            headers.selectByIdForUpdate(401L);
            headers.updateById(new ErpPurchaseInDO().setId(401L).setStatus(ErpAuditStatus.APPROVE.getStatus()));
            captured.addAll(confirmation.prepareApproval(headers.selectById(401L),items.selectListByInIdForUpdate(401L)).values());
            return null;
        }));
        assertEquals(2,captured.size());
        assertEquals(ErpAuditStatus.PROCESS.getStatus(),jdbc.queryForObject("SELECT status FROM erp_purchase_in WHERE id=401",Integer.class));
        assertNull(jdbc.queryForObject("SELECT consumed_at FROM erp_purchase_cost_confirmation",LocalDateTime.class));
        assertThrows(ServiceException.class,()->tx.execute(status->writer.post(captured.get(0),()->fail("unauthorized reference reached legacy mutation"))));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting",Integer.class));
        approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());
        assertEquals("CONSUMED",confirmation.preview(401L).getStatus());
    }
    @Test void newUnconsumedRevisionPreservesOldAmountsAndApprovalConsumesLatestOnly() {
        Header old=confirmation.confirm(request("original-version"));
        ConfirmRequest next=request("corrected-version");next.getItems().get(0).setConfirmedNetTotalAmount(new BigDecimal("651"));
        Header revised=confirmation.confirm(next);
        assertEquals(Integer.valueOf(old.getLatestRevision()+1),revised.getLatestRevision());
        amount("650",jdbc.queryForObject("SELECT confirmed_net_total_amount FROM erp_purchase_cost_confirmation_line WHERE confirmation_id=? AND source_item_id=601",BigDecimal.class,old.getConfirmationId()));
        approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());
        amount("651",jdbc.queryForObject("SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE biz_item_id=601",BigDecimal.class));
        assertNull(jdbc.queryForObject("SELECT consumed_at FROM erp_purchase_cost_confirmation WHERE id=?",LocalDateTime.class,old.getConfirmationId()));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_posting_link WHERE confirmation_id=?",Integer.class,revised.getConfirmationId()));
    }
    @Test void unknownNetAmountIsRejectedWhileExplicitZeroIsPreserved() {
        ConfirmRequest missing=request("missing-amount");missing.getItems().get(0).setConfirmedNetTotalAmount(null);
        assertThrows(ServiceException.class,()->confirmation.confirm(missing));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_confirmation",Integer.class));
        missing.getItems().get(0).setConfirmedNetTotalAmount(BigDecimal.ZERO);
        Header explicit=confirmation.confirm(missing);
        assertEquals("UNKNOWN",explicit.getTaxStatus());
        amount("0",jdbc.queryForObject("SELECT confirmed_net_total_amount FROM erp_purchase_cost_confirmation_line WHERE source_item_id=601",BigDecimal.class));
    }
    @Test void newDimensionBirthConnectsActualPurchaseApprovalWriterAndBalanceReport() throws Exception {
        String ddl=new String(Files.readAllBytes(Paths.get("../sql/mysql/erp_stock_new_dimension_origin_20260909.sql")),StandardCharsets.UTF_8);
        for(String statement:ddl.split(";"))if(!statement.trim().isEmpty())jdbc.execute(statement);
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_stock_lock(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,product_id BIGINT,warehouse_id BIGINT,deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("ALTER TABLE erp_stock MODIFY id BIGINT NOT NULL AUTO_INCREMENT");
        jdbc.execute("DELETE FROM erp_stock_dual_cost_origin");jdbc.execute("DELETE FROM erp_stock_dual_cost_dimension_mutex");
        jdbc.update("DELETE FROM erp_stock_dual_cost_balance WHERE stock_id=11");jdbc.update("DELETE FROM erp_stock WHERE id=11");jdbc.update("UPDATE erp_product SET status=0");
        cn.iocoder.yudao.module.erp.service.stock.cost.ErpStockDimensionService target=new cn.iocoder.yudao.module.erp.service.stock.cost.ErpStockDimensionService();
        set(target,"jdbcTemplate",jdbc);set(target,"enabled",true);set(target,"cutover",cutover.toString());
        cn.iocoder.yudao.module.erp.service.stock.cost.ErpStockDimensionService dimension=proxy(target);
        try {
            tx.execute(status->dimension.initializeDimensions(Collections.singletonList(new ErpStockDO().setProductId(101L).setWarehouseId(201L))));
            Map<String,Object> origin=jdbc.queryForMap("SELECT * FROM erp_stock_dual_cost_origin WHERE product_id=101");
            confirmation.confirm(request("new-birth-purchase"));approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());
            assertEquals(origin,jdbc.queryForMap("SELECT * FROM erp_stock_dual_cost_origin WHERE product_id=101"));
            amount("3",jdbc.queryForObject("SELECT count FROM erp_stock WHERE product_id=101",BigDecimal.class));
            amount("650",jdbc.queryForObject("SELECT financial_movement FROM erp_stock_dual_cost_posting WHERE product_id=101",BigDecimal.class));
            cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Repository reports=new cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Repository();set(reports,"jdbcTemplate",jdbc);
            cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Repository.Scope scope=new cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Repository.Scope();scope.setTenantId(1);scope.setUserId(99);scope.setAll(true);scope.setWarehouseIds(Collections.singleton(201L));scope.setWholeWarehouseIds(Collections.singleton(201L));
            cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.Filter filter=new cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.Filter();filter.setProductId(101L);filter.setPostedFrom(cutover);filter.setPostedTo(LocalDateTime.now().plusMinutes(1));
            cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.BalanceRow row=reports.balancePage(filter,scope,cutover,0,10).get(0);
            assertEquals("NEW_DIMENSION",row.getOriginKind());assertEquals("READY",row.getDataStatus());amount("0",row.getOpeningQuantity());amount("3",row.getClosingQuantity());amount("650",row.getClosingFinancialAmount());amount("650",row.getClosingSettlementAmount());assertNotNull(row.getAvailableFrom());
        } finally {jdbc.execute("DELETE FROM erp_stock_dual_cost_origin");jdbc.execute("DELETE FROM erp_stock_dual_cost_dimension_mutex");}
    }
    @Test void concurrentSameRequestCreatesOneVersion() throws Exception {
        ConfirmRequest r=request("parallel-retry");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        Callable<Header> call=()->{login(1);start.await();try{return confirmation.confirm(r);}finally{cleanup();}};
        try{Future<Header> one=pool.submit(call),two=pool.submit(call);start.countDown();assertEquals(one.get(10,TimeUnit.SECONDS).getConfirmationId(),two.get(10,TimeUnit.SECONDS).getConfirmationId());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_confirmation",Integer.class));}
        finally{pool.shutdownNow();}
    }
    @Test void purchaseAndTradeContextsAreIsolatedWhenOuterTransactionIsSuspended(){
        confirmation.confirm(request("context-isolation"));
        tx.executeWithoutResult(outer->{
            headers.selectByIdForUpdate(401L);headers.updateByIdAndStatus(401L,10,new ErpPurchaseInDO().setStatus(20));
            ErpPurchaseInDO head=headers.selectByIdForUpdate(401L);List<ErpPurchaseInItemDO> all=items.selectListByInIdForUpdate(401L);
            Map<Long,ErpStockRecordCreateReqBO> costs=confirmation.prepareApproval(head,all);
            Map<Long,ErpTradeSnapshotService.PreparedTradeContext> contexts=trade.preparePurchaseIn(head,all);
            for(ErpStockRecordCreateReqBO bo:costs.values())bo.setTradeContext(contexts.get(bo.getBizItemId()));
            ErpStockRecordCreateReqBO first=costs.get(601L);
            TransactionTemplate nested=new TransactionTemplate(manager);nested.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThrows(RuntimeException.class,()->nested.executeWithoutResult(inner->confirmation.validatePosting(first)));
            assertThrows(RuntimeException.class,()->nested.executeWithoutResult(inner->trade.validatePreparedForPosting(first)));
            confirmation.validatePosting(first);trade.validatePreparedForPosting(first);
            for(ErpStockRecordCreateReqBO bo:costs.values())stockRecordAdapter.createStockRecord(bo);
        });
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_posting_link",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_business_report_item_snapshot",Integer.class));
        assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getResourceMap().isEmpty());
    }
    @Test void confirmationLocksSerializeWithApprovalAndCannotChangeConsumedVersion() throws Exception {
        ConfirmRequest first=request("before-approval");confirmation.confirm(first);ConfirmRequest revision=request("racing-revision");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            Future<?> approving=pool.submit(()->{login(1);try{tx.execute(status->{headers.selectByIdForUpdate(401L);locked.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){throw new RuntimeException(e);}approval.updatePurchaseInStatus(401L,ErpAuditStatus.APPROVE.getStatus());return null;});}finally{cleanup();}});
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            Future<?> changing=pool.submit(()->{login(1);try{return confirmation.confirm(revision);}finally{cleanup();}});
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean waiting=false;
            while(System.nanoTime()<deadline){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON w.REQUESTING_ENGINE_LOCK_ID=l.ENGINE_LOCK_ID WHERE l.OBJECT_SCHEMA='report_purchase_cost_test' AND l.OBJECT_NAME='erp_purchase_in'",Integer.class);if(n>0){waiting=true;break;}Thread.sleep(20);}
            assertTrue(waiting,"actual database lock wait must be observed");release.countDown();approving.get(10,TimeUnit.SECONDS);
            assertTrue(assertThrows(ExecutionException.class,()->changing.get(10,TimeUnit.SECONDS)).getCause() instanceof ServiceException);
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_confirmation",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_record",Integer.class));
        } finally {release.countDown();pool.shutdownNow();}
    }
}
