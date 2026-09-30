package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.mysql.assistant.*;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.expression.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real MySQL, guarded by port/schema/datadir. Every fixture is rolled back. */
@EnabledIfSystemProperty(named="assistant.mysql",matches="true")
class AssistantMysqlTest {
    static com.github.fppt.jedismock.RedisServer testRedis;
    @BeforeAll static void redisStart() throws Exception {
        testRedis=com.github.fppt.jedismock.RedisServer.newRedisServer(13379,java.net.InetAddress.getByName("127.0.0.1")).start();
        try(redis.clients.jedis.Jedis client=new redis.clients.jedis.Jedis("127.0.0.1",13379)) {
            assertEquals("PONG",client.ping());client.setex("assistant:isolated-test",10,"ok");assertEquals("ok",client.get("assistant:isolated-test"));
        }
    }
    @AfterAll static void redisStop() throws Exception { if(testRedis!=null) testRedis.stop(); }
    JdbcTemplate jdbc;
    AssistantQueryMapper mapper;
    DataSourceTransactionManager transactions;
    TransactionStatus transaction;
    org.apache.ibatis.session.Configuration mybatis;
    SqlSessionTemplate sqlSession;
    @BeforeEach void setup() throws Exception {
        Path root=Paths.get("../..").toRealPath();
        Map<?,?> config=new ObjectMapper().readValue(root.resolve(".local/erp-assistant/test-connection.json").toFile(),Map.class);
        assertEquals("127.0.0.1",config.get("host")); assertEquals(13316,config.get("port"));assertEquals("erp_assistant_test",config.get("database"));
        DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:13316/erp_assistant_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",(String)config.get("user"),(String)config.get("password"));
        jdbc=new JdbcTemplate(ds);
        assertEquals(root.resolve(".local/erp-assistant/mysql").toRealPath(),Paths.get(jdbc.queryForObject("SELECT @@datadir",String.class)).toRealPath());
        transactions=new DataSourceTransactionManager(ds); transaction=transactions.getTransaction(new DefaultTransactionDefinition());
        TenantContextHolder.setTenantId(1L);
        MybatisConfiguration configuration=new MybatisConfiguration();configuration.setMapUnderscoreToCamelCase(true);configuration.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(TenantContextHolder.getRequiredTenantId());}}));
        MybatisSqlSessionFactoryBean bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(configuration);bean.setPlugins(interceptor);
        SqlSessionFactory factory=bean.getObject();factory.getConfiguration().addMapper(AssistantQueryMapper.class);
        sqlSession=new SqlSessionTemplate(factory);mapper=sqlSession.getMapper(AssistantQueryMapper.class);mybatis=factory.getConfiguration();
        seed();
    }
    @AfterEach void cleanup() { if(transaction!=null) transactions.rollback(transaction);TenantContextHolder.clear(); }

    @Test void warehousePermissionProjectionKeepsTenantStatusAndRequiredColumns() {
        Class<cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper> type=cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper.class;
        mybatis.addMapper(type);
        List<cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> rows=sqlSession.getMapper(type).selectEnabledPermissionScopeRows();
        assertFalse(rows.isEmpty());assertTrue(rows.stream().allMatch(r->r.getId()!=null && r.getDeptId()!=null));
        List<Long> expected=jdbc.queryForList("SELECT id FROM erp_warehouse WHERE deleted=0 AND status=0 AND tenant_id=1",Long.class);
        assertEquals(new HashSet<>(expected),rows.stream().map(cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO::getId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(rows.stream().allMatch(r->r.getName()==null),"Permission resolution must not hydrate unrelated warehouse data");
        TenantContextHolder.setTenantId(980099L);
        assertTrue(sqlSession.getMapper(type).selectEnabledPermissionScopeRows().isEmpty());
    }

    void insert(String table,Object... pairs) {
        Map<String,Object> values=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2) values.put((String)pairs[i],pairs[i+1]);
        List<Map<String,Object>> columns=jdbc.queryForList("SELECT column_name,data_type,is_nullable,column_default,extra FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=?",table);
        for(Map<String,Object> c:columns) {
            String name=(String)c.get("COLUMN_NAME");
            if(values.containsKey(name)) continue;
            if("tenant_id".equals(name)) {values.put(name,1L);continue;}
            if("creator".equals(name)) {values.put(name,"101");continue;}
            if(!"NO".equals(c.get("IS_NULLABLE")) || c.get("COLUMN_DEFAULT")!=null || String.valueOf(c.get("EXTRA")).contains("auto_increment")) continue;
            String type=(String)c.get("DATA_TYPE");
            values.put(name,Arrays.asList("varchar","char","text","longtext").contains(type)?"test-"+values.get("id"):
                    Arrays.asList("datetime","timestamp","date").contains(type)?"2026-09-22 12:00:00":0);
        }
        jdbc.update("INSERT INTO "+table+" (`"+String.join("`,`",values.keySet())+"`) VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")",values.values().toArray());
    }
    void seed() {
        insert("system_dept","id",910001L,"name","测试部门");
        insert("erp_customer","id",910001L,"name","测试客户","dept_id",910001L,"sale_user_id",101L);
        insert("erp_supplier","id",910001L,"name","测试供应商","dept_id",910001L);
        insert("erp_product_unit","id",910001L,"name","个");insert("erp_product_unit","id",910002L,"name","米");
        insert("erp_product","id",910001L,"name","测试配件A","unit_id",910001L);
        insert("erp_product","id",910002L,"name","测试配件B","unit_id",910002L);
        insert("erp_warehouse","id",910001L,"name","蛟龙港仓","dept_id",910001L);
        insert("erp_stock","id",910001L,"product_id",910001L,"warehouse_id",910001L,"dept_id",910001L,"count",10);
        insert("erp_stock","id",910002L,"product_id",910002L,"warehouse_id",910001L,"dept_id",910001L,"count",5);
        for(String kind:Arrays.asList("sale_out","purchase_in")) {
            boolean sale=kind.startsWith("sale");
            insert("erp_"+kind,"id",910001L,"no","TEST-"+kind,"status",20,"dept_id",910001L,sale?"customer_id":"supplier_id",910001L,sale?"out_time":"in_time","2026-09-22 12:00:00","total_price",100,"total_count",2);
            insert("erp_"+kind,"id",910002L,"no","DRAFT-"+kind,"status",0,"dept_id",910001L,sale?"customer_id":"supplier_id",910001L,sale?"out_time":"in_time","2026-09-22 12:00:00","total_price",900,"total_count",9);
            insert("erp_"+(sale?"sale_return":"purchase_return"),"id",910001L,"no","RETURN-"+kind,"status",20,"dept_id",910001L,sale?"customer_id":"supplier_id",910001L,"return_time","2026-09-23 12:00:00","total_price",20,"total_count",1);
            insert("erp_"+kind+"_items","id",910001L,sale?"out_id":"in_id",910001L,"product_id",910001L,"product_unit_id",910001L,"count",1,"total_price",60);
            insert("erp_"+kind+"_items","id",910002L,sale?"out_id":"in_id",910001L,"product_id",910002L,"product_unit_id",910002L,"count",1,"total_price",40);
        }
        for(String kind:Arrays.asList("receipt","payment")) {
            String party=kind.equals("receipt")?"customer_id":"supplier_id";
            insert("erp_finance_"+kind,"id",910001L,"no","TEST-"+kind,"status",20,"dept_id",910001L,party,910001L,"finance_user_id",101L,kind+"_time","2026-09-22 12:00:00","total_price",100,"discount_price",2,kind+"_price",98);
            insert("erp_finance_"+kind,"id",910002L,"no","PENDING-"+kind,"status",10,"dept_id",910001L,party,910001L,"finance_user_id",101L,kind+"_time","2026-09-22 12:00:00","total_price",50,"discount_price",0,kind+"_price",50);
        }
    }
    Map<String,Object> context(AssistantPlan.Metric metric) {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(metric);plan.setPeriod(plan.current()?"CURRENT":"CUSTOM");plan.setStartDate("2026-09-21");plan.setEndDate("2026-09-24");
        Map<String,Object> p=new HashMap<>();p.put("plan",plan);p.put("mode","summary");p.put("rowLimit",101);p.put("tenantId",1L);p.put("userId","101");
        LocalDateTime[] range={LocalDateTime.of(2026,9,21,0,0),LocalDateTime.of(2026,9,25,0,0)};p.put("start",range[0]);p.put("end",range[1]);
        ErpSaleReportPageReqVO sale=new ErpSaleReportPageReqVO();sale.setBizTime(range);
        ErpPurchaseReportPageReqVO purchase=new ErpPurchaseReportPageReqVO();purchase.setBizTime(range);
        p.put("reqVO",metric==AssistantPlan.Metric.SALE?sale:metric==AssistantPlan.Metric.PURCHASE?purchase:metric==AssistantPlan.Metric.RECEIVABLE?new ErpReceivableAccountPageReqVO():new ErpPayableAccountPageReqVO());
        for(String prefix:Arrays.asList("party","document","doc")) {p.put(prefix+"All",true);p.put(prefix+"DeptIds",Collections.emptyList());p.put(prefix+"SelfUserId",null);}
        p.put("all",true);p.put("deptIds",Collections.emptyList());p.put("selfUserId",null);
        p.put("stockAll",true);p.put("stockDeptIds",Collections.emptyList());p.put("stockSelfIds",Collections.emptyList());return p;
    }
    BigDecimal amount(List<Map<String,Object>> rows) { return rows.stream().map(r->new BigDecimal(r.get("amount").toString())).reduce(BigDecimal.ZERO,BigDecimal::add); }

    @Test void allBusinessDomainsExecuteWithRealSchema() {
        for(AssistantPlan.Metric metric:AssistantPlan.Metric.values()) {
            List<Map<String,Object>> rows=mapper.select(context(metric));assertFalse(rows.isEmpty(),metric.name());
            if(metric==AssistantPlan.Metric.SALE || metric==AssistantPlan.Metric.PURCHASE) assertEquals(0,new BigDecimal("80").compareTo(amount(rows)),metric.name());
            if(metric==AssistantPlan.Metric.RECEIPT || metric==AssistantPlan.Metric.PAYMENT) {assertEquals(0,new BigDecimal("98").compareTo(amount(rows)));assertEquals(0,new BigDecimal("50").compareTo(new BigDecimal(rows.get(0).get("pending").toString())));}
            if(metric==AssistantPlan.Metric.RECEIVABLE || metric==AssistantPlan.Metric.PAYABLE) assertEquals(0,new BigDecimal("-20").compareTo(amount(rows)),metric.name());
        }
    }
    @Test void tenantAndDepartmentCannotEscape() {
        for(AssistantPlan.Metric metric:AssistantPlan.Metric.values()) {
            Map<String,Object> p=context(metric);TenantContextHolder.setTenantId(2L);p.put("tenantId",2L);
            assertTrue(mapper.select(p).isEmpty(),metric.name());TenantContextHolder.setTenantId(1L);
            p=context(metric);p.put("documentAll",false);p.put("docAll",false);p.put("partyAll",false);p.put("all",false);p.put("stockAll",false);
            assertTrue(mapper.select(p).isEmpty(),metric.name());
        }
    }
    @Test void groupingDetailsAndSkuHaveCorrectGrain() {
        for(AssistantPlan.Metric metric:AssistantPlan.Metric.values()) {
            Map<String,Object> p=context(metric);AssistantPlan plan=(AssistantPlan)p.get("plan");
            plan.setGroup(metric==AssistantPlan.Metric.STOCK || metric==AssistantPlan.Metric.STOCK_SKU?AssistantPlan.Group.WAREHOUSE:AssistantPlan.Group.PARTY);
            p.put("mode","groups");assertFalse(mapper.select(p).isEmpty(),metric.name());
            p.put("mode","details");p.put("pageSize",1);p.put("offset",0);assertEquals(1,mapper.select(p).size(),metric.name());
        }
        Map<String,Object> stock=context(AssistantPlan.Metric.STOCK);assertEquals(2,mapper.select(stock).size());
        Map<String,Object> sku=context(AssistantPlan.Metric.STOCK_SKU);((AssistantPlan)sku.get("plan")).setStockMode("POSITIVE");assertEquals(0,new BigDecimal("2").compareTo(amount(mapper.select(sku))));
        jdbc.update("UPDATE erp_stock SET count=-5 WHERE id=910002 AND tenant_id=1");
        assertEquals(0,BigDecimal.ONE.compareTo(amount(mapper.select(sku))));
        sku.put("mode","count");assertEquals(1,((Number)mapper.select(sku).get(0).get("total")).intValue());
        sku.put("mode","details");sku.put("pageSize",20);sku.put("offset",0);
        assertEquals(1,mapper.select(sku).size());assertEquals("测试配件A",mapper.select(sku).get(0).get("name"));
        ((AssistantPlan)stock.get("plan")).setGroup(AssistantPlan.Group.WAREHOUSE);stock.put("mode","groups");stock.put("rowLimit",1);
        List<Map<String,Object>> warehouseRanking=mapper.select(stock);
        assertEquals(1,warehouseRanking.size(),"warehouse inventory is ranked once by its total quantity");
        assertEquals("数量",warehouseRanking.get(0).get("unit"));
        assertEquals(0,new BigDecimal("5").compareTo(new BigDecimal(warehouseRanking.get(0).get("amount").toString())));
    }
    @Test void productRankingAndAvailableStockUseExistingFormulas() {
        for(AssistantPlan.Metric metric:Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.PURCHASE)) {
            Map<String,Object> p=context(metric);((AssistantPlan)p.get("plan")).setGroup(AssistantPlan.Group.PRODUCT);p.put("mode","groups");
            assertEquals(2,mapper.select(p).size()); assertEquals(0,new BigDecimal("100").compareTo(amount(mapper.select(p))));
        }
        Map<String,Object> p=context(AssistantPlan.Metric.STOCK);((AssistantPlan)p.get("plan")).setStockMode("AVAILABLE");assertEquals(0,new BigDecimal("15").compareTo(amount(mapper.select(p))));
    }

    @Test void stockRankingIgnoresUnitsIncludesTiesAndUsesPositiveMinimum() {
        insert("erp_product","id",910003L,"name","测试配件C","unit_id",910001L);
        insert("erp_product","id",910004L,"name","测试配件D","unit_id",910001L);
        jdbc.update("UPDATE erp_stock SET count=20 WHERE id=910002 AND tenant_id=1");
        insert("erp_stock","id",910003L,"product_id",910003L,"warehouse_id",910001L,"dept_id",910001L,"count",20);
        insert("erp_stock","id",910004L,"product_id",910004L,"warehouse_id",910001L,"dept_id",910001L,"count",3);
        insert("erp_warehouse","id",910002L,"name","测试二仓","dept_id",910001L);
        insert("erp_stock","id",910005L,"product_id",910001L,"warehouse_id",910002L,"dept_id",910001L,"count",-15);
        Map<String,Object> p=context(AssistantPlan.Metric.STOCK);AssistantPlan plan=(AssistantPlan)p.get("plan");
        plan.setGroup(AssistantPlan.Group.PRODUCT);plan.setLimit(1);p.put("mode","groups");p.put("rowLimit",1);

        List<Map<String,Object>> descending=mapper.select(p);
        assertEquals(2,descending.size(),"different product units share one numeric ranking and keep ties");
        assertEquals(new HashSet<>(Arrays.asList("个","米")),descending.stream().map(row->String.valueOf(row.get("unit"))).collect(java.util.stream.Collectors.toSet()));
        assertTrue(descending.stream().allMatch(row->new BigDecimal("20").compareTo(new BigDecimal(row.get("amount").toString()))==0));

        plan.setRankOrder(AssistantPlan.RankOrder.ASC);plan.setStockMode("POSITIVE");
        List<Map<String,Object>> ascending=mapper.select(p);
        assertEquals(1,ascending.size());
        assertEquals("测试配件D",ascending.get(0).get("label"));
        assertEquals(0,new BigDecimal("3").compareTo(new BigDecimal(ascending.get(0).get("amount").toString())));

        plan.setRankOrder(AssistantPlan.RankOrder.DESC);plan.setStockMode("ALL");plan.setGroup(AssistantPlan.Group.WAREHOUSE);
        List<Map<String,Object>> warehouses=mapper.select(p);
        assertEquals(1,warehouses.size());
        assertEquals("蛟龙港仓",warehouses.get(0).get("label"));
        assertEquals("数量",warehouses.get(0).get("unit"));
        assertEquals(0,new BigDecimal("53").compareTo(new BigDecimal(warehouses.get(0).get("amount").toString())));
    }
    @Test void skuSumsDuplicateProductRowsBeforeCountingAndSelfScopeIsStrict() {
        insert("erp_stock","id",910003L,"product_id",910001L,"warehouse_id",910001L,"dept_id",910002L,"count",-10,"creator","102");
        Map<String,Object> p=context(AssistantPlan.Metric.STOCK_SKU);((AssistantPlan)p.get("plan")).setStockMode("POSITIVE");
        assertEquals(0,BigDecimal.ONE.compareTo(amount(mapper.select(p))));
        p.put("stockAll",false);p.put("stockSelfIds",Collections.singleton(910001L));
        assertEquals(0,new BigDecimal("2").compareTo(amount(mapper.select(p))));
    }
    @Test void conversationOwnershipAndDeletionAreEnforcedWithoutStoringResults() {
        cn.iocoder.yudao.framework.security.core.LoginUser user=new cn.iocoder.yudao.framework.security.core.LoginUser();user.setId(101L);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,"",Collections.emptyList()));
        try {
            AssistantStore store=new AssistantStore();AssistantProperties properties=new AssistantProperties();properties.setEnabled(true);
            org.springframework.test.util.ReflectionTestUtils.setField(store,"jdbc",jdbc);org.springframework.test.util.ReflectionTestUtils.setField(store,"properties",properties);
            String id=store.create();String message=store.begin(id,"本周收款");
            store.finish(message,"{\"metric\":\"RECEIPT\",\"period\":\"THIS_WEEK\"}","RECEIPT","test","SUCCESS",10,0);
            assertTrue(store.queryPlan(message).contains("RECEIPT"));
            user.setId(102L);assertThrows(AssistantFailure.class,()->store.owner(id));assertThrows(AssistantFailure.class,()->store.queryPlan(message));
            user.setId(101L);TenantContextHolder.setTenantId(2L);assertThrows(AssistantFailure.class,()->store.owner(id));
            TenantContextHolder.setTenantId(1L);store.delete(id);assertThrows(AssistantFailure.class,()->store.queryPlan(message));
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    }

    @Test void cashDateEdgesAndAuditReversalNeverIncludeOutOfRangeAmounts() {
        for(String kind:Arrays.asList("receipt","payment")) {
            Map<String,Object> p=context(kind.equals("receipt")?AssistantPlan.Metric.RECEIPT:AssistantPlan.Metric.PAYMENT);
            String table="erp_finance_"+kind;
            jdbc.update("UPDATE "+table+" SET "+kind+"_time='2026-09-25 00:00:00' WHERE id=910001 AND tenant_id=1");
            assertEquals(0,BigDecimal.ZERO.compareTo(amount(mapper.select(p))),"end is exclusive");
            jdbc.update("UPDATE "+table+" SET "+kind+"_time='2026-09-21 00:00:00' WHERE id=910001 AND tenant_id=1");
            assertEquals(0,new BigDecimal("98").compareTo(amount(mapper.select(p))),"start is inclusive");
            jdbc.update("UPDATE "+table+" SET status=10 WHERE id=910001 AND tenant_id=1");
            assertEquals(0,BigDecimal.ZERO.compareTo(amount(mapper.select(p))),"reversed approval is pending only");
        }
    }

    @Test void eachPartyAndDocumentScopeIndependentlyConstrainsAllNonStockMetrics() {
        for(AssistantPlan.Metric metric:AssistantPlan.Metric.values()) {
            if(metric==AssistantPlan.Metric.STOCK || metric==AssistantPlan.Metric.STOCK_SKU) continue;
            Map<String,Object> p=context(metric);p.put("partyAll",false);p.put("all",false);
            assertTrue(mapper.select(p).isEmpty(),metric+" party");
            p=context(metric);p.put("documentAll",false);p.put("docAll",false);
            assertTrue(mapper.select(p).isEmpty(),metric+" document");
        }
    }

    @Test void tenThousandDocumentSummaryIsCompleteWhileDetailsStayPaged() throws Exception {
        List<String> columns=jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_sale_out' ORDER BY ordinal_position",String.class);
        List<String> selected=new ArrayList<>();
        for(String col:columns) selected.add(col.equals("id")?"920000+n.seq":col.equals("no")?"CONCAT('PERF-',n.seq)":"src.`"+col+"`");
        String digits="(SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9)";
        String numbers="SELECT a.n+10*b.n+100*c.n+1000*d.n seq FROM "+digits+" a CROSS JOIN "+digits+" b CROSS JOIN "+digits+" c CROSS JOIN "+digits+" d";
        assertEquals(10000,jdbc.update("INSERT INTO erp_sale_out (`"+String.join("`,`",columns)+"`) SELECT "+String.join(",",selected)+" FROM erp_sale_out src CROSS JOIN ("+numbers+") n WHERE src.id=910001 AND src.tenant_id=1"));
        Map<String,Object> p=context(AssistantPlan.Metric.SALE);long begin=System.nanoTime();
        assertEquals(0,new BigDecimal("1000080").compareTo(amount(mapper.select(p))));
        long elapsed=(System.nanoTime()-begin)/1_000_000;
        assertTrue(elapsed<10000,"synthetic 10000-row summary must meet query budget");
        org.apache.ibatis.mapping.MappedStatement statement=mybatis.getMappedStatement(AssistantQueryMapper.class.getName()+".select");
        org.apache.ibatis.mapping.BoundSql bound=statement.getBoundSql(p);
        List<Map<String,Object>> explain=new ArrayList<>();
        try(java.sql.PreparedStatement prepared=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(jdbc.getDataSource()).prepareStatement("EXPLAIN "+bound.getSql())) {
            new org.apache.ibatis.scripting.defaults.DefaultParameterHandler(statement,p,bound).setParameters(prepared);
            try(java.sql.ResultSet rows=prepared.executeQuery()) {
                while(rows.next()) {Map<String,Object> row=new LinkedHashMap<>();for(String key:Arrays.asList("table","type","key","rows","Extra")) row.put(key,rows.getObject(key));explain.add(row);}
            }
        }
        p.put("mode","details");p.put("pageSize",20);p.put("offset",0);assertEquals(20,mapper.select(p).size());
        Map<String,Object> evidence=new LinkedHashMap<>();evidence.put("environment","isolated-synthetic");evidence.put("insertedDocuments",10000);evidence.put("summaryElapsedMs",elapsed);evidence.put("detailRows",20);evidence.put("explain",explain);evidence.put("productionBenchmark",false);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Paths.get("../../修改文档/0921-0927/0924/智能问数/构造数据性能证据.json").toFile(),evidence);
    }
    @Test void dedicatedReaderUsesSelectOnlyAccountAndCannotSeeUncommittedMasterRows() throws Exception {
        Path root=Paths.get("../..").toRealPath();
        Map<?,?> config=new ObjectMapper().readValue(root.resolve(".local/erp-assistant/test-readonly.json").toFile(),Map.class);
        assertEquals("127.0.0.1",config.get("host"));assertEquals(13316,config.get("port"));assertEquals("erp_assistant_test",config.get("database"));assertEquals("assistant_ro_local",config.get("user"));
        AssistantProperties properties=new AssistantProperties();properties.getReadOnly().setUrl("jdbc:mysql://127.0.0.1:13316/erp_assistant_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");
        properties.getReadOnly().setUsername((String)config.get("user"));properties.getReadOnly().setPassword((String)config.get("password"));
        SqlSessionFactory source=org.mockito.Mockito.mock(SqlSessionFactory.class);org.mockito.Mockito.when(source.getConfiguration()).thenReturn(mybatis);
        AssistantReadOnly reader=new AssistantReadOnly();org.springframework.test.util.ReflectionTestUtils.setField(reader,"properties",properties);org.springframework.test.util.ReflectionTestUtils.setField(reader,"sqlSessionFactory",source);
        try {
            reader.probe();assertFalse(mapper.select(context(AssistantPlan.Metric.STOCK)).isEmpty());
            assertTrue(reader.snapshot(()->reader.select(context(AssistantPlan.Metric.STOCK))).isEmpty());
            JdbcTemplate readJdbc=new JdbcTemplate(new DriverManagerDataSource(properties.getReadOnly().getUrl(),properties.getReadOnly().getUsername(),properties.getReadOnly().getPassword()));
            List<String> grants=readJdbc.queryForList("SHOW GRANTS",String.class);
            assertTrue(grants.stream().anyMatch(g->g.startsWith("GRANT SELECT ON `erp_assistant_test`.*")));
            assertTrue(grants.stream().allMatch(g->g.startsWith("GRANT SELECT ON `erp_assistant_test`.*") || g.startsWith("GRANT USAGE ON *.*")));
        } finally {reader.close();}
    }
    @Test void allocationDoesNotMultiplyCashAndAccountDetailsAreTransactions() {
        insert("erp_finance_receipt_item","id",910051L,"receipt_id",910001L,"biz_id",910001L,"biz_type",21,"receipt_price",30,"write_off_status",1);
        insert("erp_finance_receipt_item","id",910052L,"receipt_id",910001L,"biz_id",910001L,"biz_type",21,"receipt_price",20,"write_off_status",1);
        insert("erp_finance_receipt_item","id",910053L,"receipt_id",910001L,"biz_id",910001L,"biz_type",21,"receipt_price",40,"write_off_status",2);
        Map<String,Object> row=mapper.select(context(AssistantPlan.Metric.RECEIPT)).get(0);
        assertEquals(0,new BigDecimal("98").compareTo(new BigDecimal(row.get("amount").toString())));
        assertEquals(0,new BigDecimal("50").compareTo(new BigDecimal(row.get("writeOff").toString())));
        for(AssistantPlan.Metric metric:Arrays.asList(AssistantPlan.Metric.RECEIVABLE,AssistantPlan.Metric.PAYABLE)) {
            Map<String,Object> args=context(metric);args.put("mode","details");args.put("offset",0);args.put("pageSize",20);
            List<Map<String,Object>> details=mapper.select(args);assertTrue(details.size()>=3);assertTrue(details.stream().allMatch(r->r.containsKey("docType")));
        }
    }
}
