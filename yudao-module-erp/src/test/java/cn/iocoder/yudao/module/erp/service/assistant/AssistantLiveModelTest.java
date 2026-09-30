package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in. Synthetic questions only; no database or login context. */
@EnabledIfSystemProperty(named="assistant.modelLive",matches="true")
class AssistantLiveModelTest {
    @Test void actualJavaAdapterParsesFourQuestionsAndFollowup() throws Exception {
        Map<?,?> settings=null;
        try(java.io.InputStream input=Files.newInputStream(Paths.get("../yudao-server/src/main/resources/application.yaml"))) {
            for(Object document:new org.yaml.snakeyaml.Yaml().loadAll(input)) {
                if(document instanceof Map && ((Map<?,?>)document).get("erp") instanceof Map) {
                    Object assistant=((Map<?,?>)((Map<?,?>)document).get("erp")).get("assistant");
                    if(assistant instanceof Map) settings=(Map<?,?>)assistant;
                }
            }
        }
        assertNotNull(settings);AssistantProperties properties=new AssistantProperties();properties.setApiKey((String)settings.get("api-key"));
        properties.setModel((String)settings.get("model"));properties.setBaseUrl((String)settings.get("base-url"));
        AssistantModelClient model=new AssistantModelClient();ReflectionTestUtils.setField(model,"properties",properties);
        List<Map<String,Object>> catalog=new ArrayList<>();
        for(String id:Arrays.asList("SALE","STOCK_SKU","STOCK","RECEIPT")) catalog.add(Collections.singletonMap("id",id));
        String[] questions={"本月销售总金额是多少","蛟龙港仓当前有库存的SKU数","蛟龙港仓当前库存","本周的收款情况"};
        String[] metrics={"SALE","STOCK_SKU","STOCK","RECEIPT"};AssistantPlan prior=null;
        for(int i=0;i<questions.length;i++) {
            AssistantPlan plan=model.parse(questions[i],null,catalog);assertEquals(metrics[i],plan.getMetric().name());plan.validate();
            if(i==1) assertEquals("POSITIVE",plan.getStockMode());prior=plan;
        }
        AssistantPlan followup=model.parse("换成上周",prior,catalog);assertEquals(AssistantPlan.Metric.RECEIPT,followup.getMetric());assertEquals("LAST_WEEK",followup.getPeriod());
        AssistantPlan skuPrior=new AssistantPlan();skuPrior.setMetric(AssistantPlan.Metric.STOCK_SKU);
        skuPrior.setPeriod("CURRENT");skuPrior.setStockMode("POSITIVE");
        AssistantPlan productStock=model.parse("卫斯卡空调滤清器WSC56208/WSC20856在各个仓库中的库存情况",skuPrior,catalog);
        assertEquals(AssistantPlan.Metric.STOCK,productStock.getMetric());
        assertEquals(AssistantPlan.Group.WAREHOUSE,productStock.getGroup());
        assertEquals("ALL",productStock.getStockMode());
        AssistantToolCall purchaseRoute=model.chooseTool("今天做了几个采购订单",Collections.emptyMap(),
                new AssistantToolRegistry().definitions(Arrays.asList("query_verified_metric","query_purchase_documents")));
        assertEquals("query_purchase_documents",purchaseRoute.getName());
        Map<String,Object> dataset=new LinkedHashMap<>();dataset.put("name","purchase_orders");dataset.put("title","采购订单");
        Map<String,String> columns=new LinkedHashMap<>();columns.put("id","订单标识");columns.put("order_time","下单时间");dataset.put("columns",columns);
        Map<String,Object> semantic=model.semanticSql("今年有多少个采购订单",Collections.singletonList(dataset),null);
        String sql=String.valueOf(semantic.get("sql"));assertFalse(sql.matches("(?is).*(?:CURDATE|NOW|YEAR)\\s*\\(.*"));
        assertTrue(semantic.get("parameters") instanceof Map && !((Map<?,?>)semantic.get("parameters")).isEmpty());
        properties.getOrchestration().setPublishedDatasets(Collections.singletonList("purchase_orders"));
        AssistantSemanticCatalog semanticCatalog=new AssistantSemanticCatalog();ReflectionTestUtils.setField(semanticCatalog,"properties",properties);
        AssistantSemanticSqlGuard guard=new AssistantSemanticSqlGuard();ReflectionTestUtils.setField(guard,"catalog",semanticCatalog);
        assertDoesNotThrow(()->guard.validate(sql));
    }
}
