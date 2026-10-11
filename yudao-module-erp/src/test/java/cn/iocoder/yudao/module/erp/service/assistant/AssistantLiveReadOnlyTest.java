package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Explicit opt-in against the configured local read-only connection; performs SELECT only. */
@EnabledIfSystemProperty(named="assistant.readOnlyLive",matches="true")
class AssistantLiveReadOnlyTest {

    @Test
    @SuppressWarnings("unchecked")
    void departmentLookupUsesTheSameJavaReadOnlyExecutorAsRuntime() throws Exception {
        AssistantSemanticReadOnly reader=reader();
        String sql="SELECT e.id,e.name name,NULL code,"+
                "CASE WHEN parent.name IS NULL THEN e.name ELSE CONCAT(parent.name,' / ',e.name) END description,"+
                "'NAME' matchedBy FROM system_dept e LEFT JOIN system_dept parent "+
                "ON parent.id=e.parent_id AND parent.tenant_id=e.tenant_id AND parent.deleted=0 "+
                "WHERE e.tenant_id=? AND e.deleted=0 AND e.status=0 AND (e.name=?) ORDER BY e.id LIMIT 21";
        try {
            List<Map<String,Object>> rows=reader.query(sql,Arrays.asList(1L,"甘孜分公司"),21);
            assertEquals(1,rows.size());assertEquals("甘孜分公司",rows.get(0).get("name"));
        } finally {
            reader.close();
        }
    }

    @Test
    void customerAndProductKeywordsMatchCurrentBusinessMasterData() throws Exception {
        AssistantSemanticReadOnly reader=reader();
        try {
            Map<String,Object> customer=entityContext("CUSTOMER","EXACT_NAME","理塘众鑫进口汽修");
            List<Map<String,Object>> exact=query(reader,customer);
            assertEquals(1,exact.size());assertEquals("理塘众鑫进口汽修",exact.get(0).get("name"));

            customer.put("entityMatchMode","FUZZY");customer.put("entityKeyword","理塘众鑫");
            List<Map<String,Object>> partial=query(reader,customer);
            assertTrue(partial.size()>=2);assertTrue(partial.stream().allMatch(row->String.valueOf(row.get("name")).contains("理塘众鑫")));

            Map<String,Object> product=entityContext("PRODUCT","TOKEN_FUZZY","美孚 CF 4L");
            for(String flag:Arrays.asList("searchCode","searchFactoryCode","searchBarCode","searchPinyin","searchWubi",
                    "searchVehicleModel","searchStandard","searchRemark","searchBrand","searchOeNumber","searchOriginPlace",
                    "searchFeatureCode","searchDrawingNo","searchShelf")) product.put(flag,true);
            List<Map<String,Object>> products=query(reader,product);
            assertTrue(products.size()>1);
            assertTrue(products.stream().allMatch(row->row.get("id")!=null&&row.get("name")!=null));
            assertTrue(products.stream().anyMatch(row->{
                String name=String.valueOf(row.get("name")).toUpperCase();
                return name.contains("美孚黑霸王CF-4")&&name.contains("4L");
            }));
        } finally {
            reader.close();
        }
    }

    @Test
    void everyStage5ServerOwnedSqlContractExecutesAgainstConfiguredSchema() throws Exception {
        AssistantSemanticReadOnly reader=reader();
        AssistantToolPermissionService permission=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{
            Map<String,Object> scope=invocation.getArgument(0);String prefix=invocation.getArgument(1);
            scope.put(prefix+"All",true);scope.put(prefix+"DeptIds",Collections.emptySet());scope.put(prefix+"SelfUserId",null);
            return null;
        }).when(permission).documentScope(anyMap(),anyString(),anyString(),anyBoolean());
        doAnswer(invocation->{
            Map<String,Object> scope=invocation.getArgument(0);scope.put("stockAll",true);
            scope.put("stockDeptIds",Collections.emptySet());scope.put("stockSelfIds",Collections.emptySet());return null;
        }).when(permission).stockScope(anyMap());
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"toolPermissions",permission);
        Map<String,List<String>> questions=new LinkedHashMap<>();
        questions.put(AssistantStage5BusinessQuery.MASTER_TOOL,Arrays.asList(
                "商品分类","商品品牌","商品单位","结算方式"));
        questions.put(AssistantStage5BusinessQuery.STOCK_TOOL,Arrays.asList(
                "价格体系","本月配件价格调整历史","仓库详情","今天库存盘点","今天其他入库","今天库存入库单","今天其他出库",
                "今天库存出库单","今天调拨单","今天调拨出库","今天调拨入库","今天移仓"));
        questions.put(AssistantStage5BusinessQuery.SALE_TOOL,Arrays.asList(
                "今天销售报价","今天销售开单","今天销售出库","今天销售退货","今天销售拣货",
                "今天销售配送","今天销售调价"));
        questions.put(AssistantStage5BusinessQuery.PURCHASE_TOOL,Arrays.asList(
                "今天采购入库","今天采购退货","今天采购发票","今天发票识别批次","今天采购调价"));
        questions.put(AssistantStage5BusinessQuery.FINANCE_TOOL,Arrays.asList(
                "资金账户","今天账户转账","今天收款单","今天付款单","今天应收杂项","今天其他收入",
                "今天应收调账","今天应付杂项","今天应付费用","今天应付调账","今天收款核销","今天付款核销"));
        TenantContextHolder.setTenantId(1L);
        try {
            for(Map.Entry<String,List<String>> group:questions.entrySet()) for(String question:group.getValue())
                assertNotNull(service.execute(group.getKey(),question,new AssistantExecutionEnvelope()),question);
        } finally {
            TenantContextHolder.clear();reader.close();
        }
    }

    @Test
    void verifiedSaleAndPurchaseBusinessSourcesExecuteAgainstConfiguredSchema() throws Exception {
        AssistantReadOnly reader=new AssistantReadOnly();
        ReflectionTestUtils.setField(reader,"properties",properties());
        ReflectionTestUtils.setField(reader,"sqlSessionFactory",
                new SqlSessionFactoryBuilder().build(new MybatisConfiguration()));
        LocalDateTime end=LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime start=end.minusDays(7);
        TenantContextHolder.setTenantId(1L);
        try {
            for(AssistantPlan.Metric metric:Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.PURCHASE))
                for(AssistantPlan.Group group:Arrays.asList(AssistantPlan.Group.NONE,AssistantPlan.Group.PRODUCT)) {
                    AssistantPlan plan=new AssistantPlan();plan.setMetric(metric);plan.setGroup(group);plan.setPeriod("CUSTOM");
                    plan.setStartDate(start.toLocalDate().toString());plan.setEndDate(end.minusDays(1).toLocalDate().toString());
                    Map<String,Object> context=new LinkedHashMap<>();context.put("plan",plan);context.put("mode","summary");
                    context.put("tenantId",1L);context.put("userId","1");context.put("start",start);context.put("end",end);
                    context.put("rowLimit",101);context.put("partyAll",true);context.put("partyDeptIds",Collections.emptySet());
                    context.put("partySelfUserId",null);
                    for(String prefix:Arrays.asList("saleOut","saleReturn","purchaseIn","purchaseReturn")) {
                        context.put(prefix+"All",true);context.put(prefix+"DeptIds",Collections.emptySet());context.put(prefix+"SelfUserId",null);
                    }
                    assertNotNull(reader.snapshot(()->reader.select(context)),metric+"/"+group);
                }
        } finally {
            TenantContextHolder.clear();reader.close();
        }
    }

    @Test
    void stage6BoundedSemanticQueryExecutesThroughTheConfiguredReadOnlyAccount() throws Exception {
        AssistantProperties configured=properties();
        configured.getOrchestration().setPublishedDatasets(Collections.singletonList("purchase_orders"));
        AssistantSemanticCatalog catalog=new AssistantSemanticCatalog();ReflectionTestUtils.setField(catalog,"properties",configured);
        AssistantSemanticSqlGuard guard=new AssistantSemanticSqlGuard();ReflectionTestUtils.setField(guard,"catalog",catalog);
        AssistantSemanticReadOnly reader=new AssistantSemanticReadOnly();ReflectionTestUtils.setField(reader,"properties",configured);
        AssistantToolPermissionService permission=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{Map<String,Object> scope=invocation.getArgument(0);scope.put(invocation.getArgument(1)+"All",true);return null;})
                .when(permission).documentScope(anyMap(),anyString(),anyString(),anyBoolean());
        AssistantSemanticQueryService service=new AssistantSemanticQueryService();
        ReflectionTestUtils.setField(service,"catalog",catalog);ReflectionTestUtils.setField(service,"guard",guard);
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"permissions",permission);
        LoginUser user=new LoginUser();user.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        LocalDateTime end=LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();LocalDateTime start=end.minusDays(7);
        Map<String,Object> parameters=new LinkedHashMap<>();parameters.put("p1",start.toString());parameters.put("p2",end.toString());
        try {
            Map<String,Object> result=service.execute("SELECT COUNT(q.id) document_count FROM purchase_orders q "
                    +"WHERE q.order_time>=:p1 AND q.order_time<:p2",parameters);
            assertEquals("SUCCESS",result.get("status"));
            assertEquals("PASSED",((Map<?,?>)result.get("queryTrace")).get("validation"));
        } finally {
            TenantContextHolder.clear();SecurityContextHolder.clearContext();reader.close();
        }
    }

    private static AssistantSemanticReadOnly reader() throws Exception {
        AssistantSemanticReadOnly reader=new AssistantSemanticReadOnly();
        ReflectionTestUtils.setField(reader,"properties",properties());
        return reader;
    }

    @SuppressWarnings("unchecked")
    private static AssistantProperties properties() throws Exception {
        Map<String,Object> assistant=null;
        try(InputStream input=Files.newInputStream(Paths.get("../yudao-server/src/main/resources/application.yaml"))) {
            for(Object document:new Yaml().loadAll(input)) {
                if(!(document instanceof Map)) continue;
                Object erp=((Map<?,?>)document).get("erp");
                if(erp instanceof Map && ((Map<?,?>)erp).get("assistant") instanceof Map)
                    assistant=(Map<String,Object>)((Map<?,?>)erp).get("assistant");
            }
        }
        if(assistant==null) throw new IllegalStateException("assistant settings not found");
        Map<String,Object> readOnly=(Map<String,Object>)assistant.get("read-only");
        AssistantProperties properties=new AssistantProperties();
        properties.getReadOnly().setUrl(String.valueOf(readOnly.get("url")));
        properties.getReadOnly().setUsername(String.valueOf(readOnly.get("username")));
        properties.getReadOnly().setPassword(String.valueOf(readOnly.get("password")));
        return properties;
    }

    private static Map<String,Object> entityContext(String type,String mode,String keyword) {
        Map<String,Object> context=new HashMap<>();context.put("tenantId",1L);context.put("entityType",type);
        context.put("entityMatchMode",mode);context.put("entityKeyword",keyword);context.put("entityAll",true);
        context.put("rowLimit",21);context.put("showCode",false);context.put("searchShortName",true);
        context.put("searchPinyin",true);context.put("searchWubi",true);return context;
    }

    private static List<Map<String,Object>> query(AssistantSemanticReadOnly reader,Map<String,Object> context) {
        cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantEntitySql.Query query=
                cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantEntitySql.select(context);
        return reader.query(query.getSql(),query.getParameters(),query.getMaximumRows());
    }
}
