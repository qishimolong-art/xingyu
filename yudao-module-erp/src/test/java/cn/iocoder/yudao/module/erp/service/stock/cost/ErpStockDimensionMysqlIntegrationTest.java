package cn.iocoder.yudao.module.erp.service.stock.cost;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.*;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.*;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.*;
import cn.iocoder.yudao.module.erp.service.product.*;
import cn.iocoder.yudao.module.erp.service.stock.*;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.mall.ErpMallProductSyncPublisher;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import net.sf.jsqlparser.expression.*;
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

/** Opt-in private MySQL; real parent services, mapper writes, dimension service and transaction boundaries. */
@EnabledIfEnvironmentVariable(named="ERP_DIMENSION_MYSQL_TEST_URL",matches=".+")
class ErpStockDimensionMysqlIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    TransactionTemplate tx;
    ErpStockDimensionService dimensions,dimensionTarget;
    ErpStockService stockService;
    ErpProductService products;
    ErpProductServiceImpl productTarget;
    ErpWarehouseService warehouses;
    ErpWarehouseServiceImpl warehouseTarget;
    ErpProductMapper productMapper;
    ErpWarehouseMapper warehouseMapper;
    ErpStockMapper stockMapper;
    final LocalDateTime cutover=LocalDateTime.of(2026,1,1,0,0);

    @BeforeEach void setup() throws Exception {
        String url=System.getenv("ERP_DIMENSION_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_dimension_test(?:\\?.*)?"));
        DriverManagerDataSource ds=new DriverManagerDataSource(url,"root","");jdbc=new JdbcTemplate(ds);
        assertTrue(jdbc.queryForObject("SELECT @@datadir",String.class).replace('\\','/').contains("/Temp/xingyu-report-mysql-"));
        manager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(manager);login();
        for(String file:Arrays.asList("erp_report_dual_cost_foundation_20260909.sql","erp_stock_new_dimension_origin_20260909.sql")) {
            String ddl=new String(Files.readAllBytes(Paths.get("../sql/mysql/"+file)),StandardCharsets.UTF_8);
            for(String statement:ddl.split(";"))if(!statement.trim().isEmpty())jdbc.execute(statement);
        }
        for(Class<?> type:Arrays.asList(ErpProductDO.class,ErpWarehouseDO.class,ErpStockDO.class))createTable(type);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='erp_stock' AND index_name='idx_test_tenant_product_warehouse'",Integer.class)==0)
            jdbc.execute("ALTER TABLE erp_stock ADD INDEX idx_test_tenant_product_warehouse(tenant_id,product_id,warehouse_id)");
        for(String table:Arrays.asList("erp_stock_record","erp_stock_lock"))
            jdbc.execute("CREATE TABLE IF NOT EXISTS "+table+"(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,product_id BIGINT,warehouse_id BIGINT,deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for(String table:Arrays.asList("erp_stock_dual_cost_origin","erp_stock_dual_cost_dimension_mutex","erp_stock_dual_cost_posting","erp_stock_dual_cost_balance","erp_stock_record","erp_stock_lock","erp_stock","erp_product","erp_warehouse"))jdbc.execute("DELETE FROM "+table);
        jdbc.update("INSERT INTO erp_product(id,tenant_id,code,name,status,creator,dept_id,create_dept_id,merged_flag,deleted) VALUES(101,1,'P101','part A',0,'99',301,301,0,0),(102,1,'P102','part B',0,'99',301,301,0,0)");
        jdbc.update("INSERT INTO erp_warehouse(id,tenant_id,warehouse_code,name,dept_id,status,creator,deleted) VALUES(201,1,'W201','主仓',301,0,'99',0),(202,1,'W202','新仓',301,0,'99',0)");
        MybatisConfiguration config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);config.setCacheEnabled(false);
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(TenantContextHolder.getRequiredTenantId());}}));
        MybatisSqlSessionFactoryBean bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(config);bean.setPlugins(interceptor);
        bean.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setLogicDeleteValue("1").setLogicNotDeleteValue("0")));
        SqlSessionFactory factory=bean.getObject();for(Class<?> type:Arrays.asList(ErpProductMapper.class,ErpWarehouseMapper.class,ErpStockMapper.class))factory.getConfiguration().addMapper(type);
        SqlSessionTemplate sessions=new SqlSessionTemplate(factory);productMapper=sessions.getMapper(ErpProductMapper.class);warehouseMapper=sessions.getMapper(ErpWarehouseMapper.class);stockMapper=sessions.getMapper(ErpStockMapper.class);
        dimensionTarget=new ErpStockDimensionService();set(dimensionTarget,"jdbcTemplate",jdbc);set(dimensionTarget,"enabled",true);set(dimensionTarget,"cutover",cutover.toString());dimensions=proxy(dimensionTarget);
        PermissionApi permission=mock(PermissionApi.class);DeptDataPermissionRespDTO all=new DeptDataPermissionRespDTO();all.setAll(true);when(permission.getDeptDataPermission(anyLong(),anyString())).thenReturn(all);
        warehouseTarget=new ErpWarehouseServiceImpl();mockResources(warehouseTarget);set(warehouseTarget,"warehouseMapper",warehouseMapper);set(warehouseTarget,"stockMapper",stockMapper);set(warehouseTarget,"stockDimensionService",dimensions);set(warehouseTarget,"transactionManager",manager);set(warehouseTarget,"permissionApi",permission);warehouses=proxy(warehouseTarget);
        ErpStockServiceImpl stockTarget=new ErpStockServiceImpl();mockResources(stockTarget);set(stockTarget,"stockMapper",stockMapper);set(stockTarget,"warehouseService",warehouses);set(stockTarget,"stockDimensionService",dimensions);stockService=proxy(stockTarget);
        productTarget=new ErpProductServiceImpl();mockResources(productTarget);set(productTarget,"productMapper",productMapper);set(productTarget,"stockMapper",stockMapper);set(productTarget,"warehouseService",warehouses);set(productTarget,"stockDimensionService",dimensions);set(productTarget,"transactionManager",manager);set(productTarget,"permissionApi",permission);
        ErpProductCategoryService category=resource(productTarget,"productCategoryService");ErpProductCategoryDO categoryRow=ErpProductCategoryDO.builder().id(401L).code("CAT").name("配件").build();when(category.getProductCategoryList(any(cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO.class))).thenReturn(Collections.singletonList(categoryRow));when(category.getProductCategory(401L)).thenReturn(categoryRow);when(category.getProductCategoryChildCount(401L)).thenReturn(0L);
        ErpProductUnitService units=resource(productTarget,"productUnitService");when(units.getProductUnitListByStatus(any())).thenReturn(Collections.singletonList(ErpProductUnitDO.builder().id(501L).name("个").build()));
        products=proxy(productTarget);
    }
    void login(){TenantContextHolder.setTenantId(1L);LoginUser user=new LoginUser();user.setId(99L);user.setUserType(UserTypeEnum.ADMIN.getValue());user.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID,"301"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));}
    @AfterEach void cleanup(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    static void set(Object target,String field,Object value){ReflectionTestUtils.setField(target,field,value);}
    @SuppressWarnings("unchecked") static <T>T resource(Object target,String field){return (T)ReflectionTestUtils.getField(target,field);}
    static void mockResources(Object target){for(Field field:target.getClass().getDeclaredFields())if(field.isAnnotationPresent(Resource.class))set(target,field.getName(),mock(field.getType()));}
    @SuppressWarnings("unchecked") <T>T proxy(T target){ProxyFactory factory=new ProxyFactory(target);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));return (T)factory.getProxy();}
    void createTable(Class<?> type){
        Map<String,String> columns=new LinkedHashMap<>();columns.put("tenant_id","BIGINT");
        for(Class<?> c=type;c!=Object.class;c=c.getSuperclass())for(Field field:c.getDeclaredFields()){
            if(Modifier.isStatic(field.getModifiers()))continue;TableField a=field.getAnnotation(TableField.class);if(a!=null&&!a.exist())continue;
            String name=a!=null&&!a.value().isEmpty()?a.value():field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
            Class<?> value=field.getType();String sql=value==BigDecimal.class?"DECIMAL(24,6)":value==Long.class?"BIGINT":value==Integer.class?"INT":value==Boolean.class?"BIT":value==LocalDateTime.class?"DATETIME(6)":"TEXT";
            if("id".equals(name))sql="BIGINT AUTO_INCREMENT PRIMARY KEY";if("deleted".equals(name))sql="BIT DEFAULT 0";columns.put(name,sql);
        }
        List<String> definitions=new ArrayList<>();columns.forEach((key,value)->definitions.add("`"+key+"` "+value));jdbc.execute("CREATE TABLE IF NOT EXISTS "+type.getAnnotation(TableName.class).value()+" ("+String.join(",",definitions)+") ENGINE=InnoDB");
    }
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    ErpStockDO dimension(long product,long warehouse){return new ErpStockDO().setProductId(product).setWarehouseId(warehouse);}
    void zero(BigDecimal value){assertNotNull(value);assertEquals(0,BigDecimal.ZERO.compareTo(value));}

    @Test void actualEnsureCreatesOneZeroBirthAndDoesNotInventHistoricInventory(){
        LocalDateTime before=LocalDateTime.now().minusSeconds(1);stockService.ensureStockExists(101L,201L);stockService.ensureStockExists(101L,201L);
        assertEquals(1,count("erp_stock"));assertEquals(1,count("erp_stock_dual_cost_origin"));assertEquals(1,count("erp_stock_dual_cost_balance"));
        Map<String,Object> origin=jdbc.queryForMap("SELECT * FROM erp_stock_dual_cost_origin");assertEquals("NEW_DIMENSION",origin.get("origin_kind"));
        LocalDateTime born=jdbc.queryForObject("SELECT available_from FROM erp_stock_dual_cost_origin",LocalDateTime.class);assertTrue(born.isAfter(before));assertNotEquals(cutover,born);
        for(String field:Arrays.asList("quantity","financial_amount","settlement_amount"))zero(jdbc.queryForObject("SELECT "+field+" FROM erp_stock_dual_cost_balance",BigDecimal.class));
        assertEquals(0,count("erp_stock_record"));
    }
    @Test void oldZeroAndSoftDeletedStockCannotBeAutomaticallyReborn(){
        jdbc.update("INSERT INTO erp_stock(tenant_id,product_id,warehouse_id,dept_id,count,cost_price,cost_amount,deleted) VALUES(1,101,201,301,0,0,0,0)");
        assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));
        jdbc.update("UPDATE erp_stock SET deleted=1");assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));
        assertEquals(0,count("erp_stock_dual_cost_origin"));assertEquals(0,count("erp_stock_dual_cost_balance"));
    }
    @Test void anyDeletedHistoricalEvidenceRejectsNewBirth(){
        for(String table:Arrays.asList("erp_stock_record","erp_stock_lock")){
            jdbc.update("INSERT INTO "+table+"(tenant_id,product_id,warehouse_id,deleted) VALUES(1,101,201,1)");
            assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));assertEquals(0,count("erp_stock"));jdbc.execute("DELETE FROM "+table);
        }
        jdbc.update("INSERT INTO erp_stock_dual_cost_origin(tenant_id,stock_id,product_id,warehouse_id,origin_kind,available_from,created_by,action_key,history_check_version,created_at) VALUES(1,999,101,201,'NEW_DIMENSION',?,99,'old','TEST',?)",cutover,cutover);
        assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));assertEquals(0,count("erp_stock"));
    }
    @Test void laterFailureRollsBackStockOriginAndBalanceTogether(){
        assertThrows(IllegalStateException.class,()->tx.execute(status->{stockService.ensureStockExists(101L,201L);throw new IllegalStateException("after initialization");}));
        for(String table:Arrays.asList("erp_stock","erp_stock_dual_cost_origin","erp_stock_dual_cost_balance"))assertEquals(0,count(table));
        stockService.ensureStockExists(101L,201L);assertEquals(1,count("erp_stock_dual_cost_origin"));
    }
    @Test void futureCutoverCannotCreatePrematureBirth(){
        set(dimensionTarget,"cutover",LocalDateTime.now().plusDays(1).toString());assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));assertEquals(0,count("erp_stock"));
    }
    @Test void subsecondCutoverCannotPersistRoundedAndUnusableOpening(){
        set(dimensionTarget,"cutover",cutover.plusNanos(123456789).toString());assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));assertEquals(0,count("erp_stock"));assertEquals(0,count("erp_stock_dual_cost_balance"));
    }
    @Test void nonPositiveWarehouseDepartmentCannotCreateUnownedBalance(){
        jdbc.update("UPDATE erp_warehouse SET dept_id=0 WHERE id=201");assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));assertEquals(0,count("erp_stock"));assertEquals(0,count("erp_stock_dual_cost_balance"));
    }
    @Test void orphanOpeningInSameTenantBlocksBirthButOtherTenantEvidenceDoesNot(){
        jdbc.update("INSERT INTO erp_stock_dual_cost_balance(tenant_id,stock_id,quantity,financial_amount,settlement_amount,opening_quantity,opening_financial_amount,opening_settlement_amount,cutover_at,confirmed_by,confirmed_at,evidence,updated_at,legacy_record_id,legacy_cost_price,legacy_cost_amount,stock_dept_id) VALUES(1,999,0,0,0,0,0,0,?,99,?,'orphan',?,0,0,0,301)",cutover,cutover,cutover);
        assertThrows(ServiceException.class,()->stockService.ensureStockExists(101L,201L));assertEquals(0,count("erp_stock"));
        jdbc.update("UPDATE erp_stock_dual_cost_balance SET tenant_id=2 WHERE stock_id=999");
        stockService.ensureStockExists(101L,201L);assertEquals(1,count("erp_stock_dual_cost_origin"));
    }
    @Test void disabledEnsureWorksWithNoNewTables(){
        set(dimensionTarget,"enabled",false);
        jdbc.execute("RENAME TABLE erp_stock_dual_cost_origin TO saved_origin,erp_stock_dual_cost_dimension_mutex TO saved_mutex,erp_stock_dual_cost_balance TO saved_balance,erp_stock_dual_cost_posting TO saved_posting");
        try{stockService.ensureStockExists(101L,201L);assertEquals(1,count("erp_stock"));}
        finally{jdbc.execute("RENAME TABLE saved_origin TO erp_stock_dual_cost_origin,saved_mutex TO erp_stock_dual_cost_dimension_mutex,saved_balance TO erp_stock_dual_cost_balance,saved_posting TO erp_stock_dual_cost_posting");}
    }
    @Test void concurrentActualEnsureCreatesSingleDimension() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        Runnable work=()->{login();try{start.await();stockService.ensureStockExists(101L,201L);}catch(InterruptedException e){throw new RuntimeException(e);}finally{cleanup();}};
        try{Future<?> a=pool.submit(work),b=pool.submit(work);start.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);assertEquals(1,count("erp_stock"));assertEquals(1,count("erp_stock_dual_cost_origin"));}
        finally{pool.shutdownNow();}
    }
    ErpProductImportExcelVO productRow(String code){ErpProductImportExcelVO row=new ErpProductImportExcelVO();row.setCode(code);row.setName(code);row.setCategoryCode("CAT");row.setUnitName("个");row.setDefaultWarehouseName("主仓");return row;}
    @Test void actualProductImportCommitsGoodRowsAndRollsBackFailureAfterBirth(){
        ErpMallProductSyncPublisher publisher=resource(productTarget,"mallProductSyncPublisher");
        doAnswer(call->{String name=jdbc.queryForObject("SELECT name FROM erp_product WHERE id=?",String.class,(Long)call.getArgument(0));if("BAD".equals(name))throw new IllegalStateException("after stock birth");return null;}).when(publisher).publishProductSync(anyLong());
        ErpProductImportRespVO result=products.importProductList(Arrays.asList(productRow("GOOD-A"),productRow("BAD"),productRow("GOOD-B")));
        assertEquals(2,result.getSuccessCount(),result.getFailureDetails().toString());assertEquals(1,result.getFailureCount());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM erp_product WHERE code='BAD'",Integer.class));
        assertEquals(2,count("erp_stock"));assertEquals(2,count("erp_stock_dual_cost_origin"));assertEquals(2,count("erp_stock_dual_cost_balance"));
    }
    ErpWarehouseImportExcelVO warehouseRow(String code,String name){ErpWarehouseImportExcelVO row=new ErpWarehouseImportExcelVO();row.setWarehouseCode(code);row.setName(name);row.setDeptId(301L);return row;}
    @Test void actualWarehouseImportRollsBackFailedUpdateAndContinues(){
        ErpOperateLogService logs=resource(warehouseTarget,"operateLogService");
        doAnswer(call->{if("W201".equals(call.getArgument(4)))throw new IllegalStateException("after warehouse SQL update");return null;}).when(logs).record(anyString(),anyString(),anyLong(),anyString(),anyString());
        ErpWarehouseImportRespVO result=warehouses.importWarehouseList(Arrays.asList(warehouseRow("GOOD-A","成功A"),warehouseRow("W201","失败修改"),warehouseRow("GOOD-B","成功B")));
        assertEquals(2,result.getSuccessCount(),result.getFailureDetails().toString());assertEquals(1,result.getFailureCount());
        assertEquals("主仓",jdbc.queryForObject("SELECT name FROM erp_warehouse WHERE id=201",String.class));assertEquals(4,count("erp_warehouse"));
    }
    @Test void productDeleteMergeAndWarehouseOwnershipChangesCannotRewriteBirth(){
        stockService.ensureStockExists(101L,201L);
        assertThrows(ServiceException.class,()->products.deleteProduct(101L));
        assertThrows(ServiceException.class,()->products.mergeProduct(101L,102L));
        ErpWarehouseSaveReqVO change=new ErpWarehouseSaveReqVO();change.setId(201L);change.setDeptId(999L);change.setName("主仓");
        assertThrows(ServiceException.class,()->warehouses.updateWarehouse(change));
        assertEquals(Long.valueOf(301),jdbc.queryForObject("SELECT dept_id FROM erp_warehouse WHERE id=201",Long.class));assertEquals(1,count("erp_stock_dual_cost_origin"));
    }
    @Test void reverseOrderedActualBatchDistributionDoesNotDeadlockOrDuplicate() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        java.util.function.Consumer<Boolean> work=reverse->{login();try{start.await();ErpProductStockDistributionBatchSaveReqVO request=new ErpProductStockDistributionBatchSaveReqVO();request.setProductIds(reverse?Arrays.asList(102L,101L):Arrays.asList(101L,102L));request.setWarehouseIds(reverse?Arrays.asList(202L,201L):Arrays.asList(201L,202L));products.batchUpdateProductStockDistribution(request);}catch(InterruptedException e){throw new RuntimeException(e);}finally{cleanup();}};
        try{Future<?> a=pool.submit(()->work.accept(false)),b=pool.submit(()->work.accept(true));start.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);assertEquals(4,count("erp_stock"));assertEquals(4,count("erp_stock_dual_cost_origin"));}
        finally{pool.shutdownNow();}
    }
    @Test void orphanScanDoesNotBlockUnrelatedExistingStockWriter() throws Exception {
        stockService.ensureStockExists(101L,201L);
        jdbc.update("UPDATE erp_stock SET count=10,cost_price=100,cost_amount=1000 WHERE product_id=101");
        jdbc.update("UPDATE erp_stock_dual_cost_balance SET quantity=10,financial_amount=1000,settlement_amount=1000,opening_quantity=10,opening_financial_amount=1000,opening_settlement_amount=1000,legacy_cost_price=100,legacy_cost_amount=1000");
        ErpDualCostLedgerRepository ledger=new ErpDualCostLedgerRepository();set(ledger,"jdbcTemplate",jdbc);
        ErpDualCostPostingService writerTarget=new ErpDualCostPostingService();set(writerTarget,"repository",ledger);set(writerTarget,"enabled",true);set(writerTarget,"cutover",cutover.toString());ErpDualCostPostingService writer=proxy(writerTarget);
        CountDownLatch scanned=new CountDownLatch(1),release=new CountDownLatch(1);
        JdbcTemplate paused=new JdbcTemplate(jdbc.getDataSource()) {
            @Override public List<Map<String,Object>> queryForList(String sql,Object... args) {
                List<Map<String,Object>> result=super.queryForList(sql,args);
                if(sql.contains("LEFT JOIN erp_stock s")&&sql.contains("s.id IS NULL")) {
                    scanned.countDown();
                    try {if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("test pause expired");}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
                }
                return result;
            }
        };
        set(dimensionTarget,"jdbcTemplate",paused);
        ExecutorService pool=Executors.newFixedThreadPool(2);Future<?> initializing=null,posting=null;
        try {
            initializing=pool.submit(()->{login();try{stockService.ensureStockExists(102L,202L);}finally{cleanup();}});
            assertTrue(scanned.await(5,TimeUnit.SECONDS),"pause only after real orphan scan SQL completed");
            posting=pool.submit(()->{login();try{tx.execute(status->{
                cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO request=new cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO(101L,201L,null,new BigDecimal("-1"),50,701L,801L,"NORMAL-SALE",new BigDecimal("150"),cutover.plusDays(1));
                request.setAccountingDeptId(301L);
                return writer.post(request,()->{jdbc.update("UPDATE erp_stock SET count=9,cost_amount=900 WHERE product_id=101");jdbc.update("INSERT INTO erp_stock_record(tenant_id,product_id,warehouse_id) VALUES(1,101,201)");});
            });}finally{cleanup();}});
            posting.get(2,TimeUnit.SECONDS);
            assertEquals(1,count("erp_stock_dual_cost_posting"));
        } finally {
            release.countDown();
            if(initializing!=null)initializing.get(10,TimeUnit.SECONDS);
            if(posting!=null)posting.get(10,TimeUnit.SECONDS);
            pool.shutdownNow();set(dimensionTarget,"jdbcTemplate",jdbc);
        }
    }
}
