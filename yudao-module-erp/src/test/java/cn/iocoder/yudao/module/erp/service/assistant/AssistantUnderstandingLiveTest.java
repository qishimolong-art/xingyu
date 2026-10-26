package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 显式启用；只验证语义，不连接数据库，不执行业务查询。 */
@EnabledIfSystemProperty(named="assistant.semanticLive",matches="true")
class AssistantUnderstandingLiveTest {
    @Test void configuredDeepSeekUnderstandsRepresentativeScenesAndFollowups() throws Exception {
        Map<?,?> settings=null;
        try(java.io.InputStream input=Files.newInputStream(Paths.get("../yudao-server/src/main/resources/application.yaml"))) {
            for(Object document:new org.yaml.snakeyaml.Yaml().loadAll(input)) if(document instanceof Map && ((Map<?,?>)document).get("erp") instanceof Map) {
                Object assistant=((Map<?,?>)((Map<?,?>)document).get("erp")).get("assistant");if(assistant instanceof Map)settings=(Map<?,?>)assistant;
            }
        }
        assertNotNull(settings);AssistantProperties properties=new AssistantProperties();
        properties.setApiKey((String)settings.get("api-key"));properties.setBaseUrl((String)settings.get("base-url"));properties.setModel((String)settings.get("model"));
        CapturingModel model=new CapturingModel();ReflectionTestUtils.setField(model,"properties",properties);
        AssistantUnderstandingService service=new AssistantUnderstandingService();ReflectionTestUtils.setField(service,"properties",properties);
        ReflectionTestUtils.setField(service,"model",model);ReflectionTestUtils.setField(service,"companyKnowledge",new AssistantCompanyKnowledge());
        List<String> names=new ArrayList<>(new AssistantToolRegistry().names());names.remove("execute_semantic_query");
        List<Map<String,Object>> tools=new AssistantToolRegistry().definitions(names);
        String[][] cases={{"lty999 有吗","query_product_stock"},{"路通源刹车片","query_product_stock"},{"有 路通源刹车片 吗","query_product_stock"},{"路通源刹车片 有吗","query_product_stock"},{"请问路通源刹车片还有货吗？","query_product_stock"},
                {"经典表现 530 4升还有多少库存","query_product_stock"},{"本月销售额是多少","query_verified_metric"},
                {"上个月采购入库金额","query_verified_metric"},{"本周收了多少钱","query_verified_metric"},
                {"本月采购订单有几张","query_purchase_documents"},{"现在介绍一下公司","answer_company_knowledge"},
                {"公司有哪些部门","answer_company_knowledge"},{"系统有多少启用部门","query_user_departments"},
                {"如何向客户介绍美孚黑霸王","answer_company_knowledge"},{"列出启用的商品分类","query_master_data_details"}};
        for(String[] c:cases){model.beginRequest();long start=System.currentTimeMillis();AssistantUnderstandingResult u=service.understand(c[0],Collections.emptyMap(),tools,false);
            System.out.println("SEMANTIC_LIVE scene="+c[0]+" capability="+u.getTasks().get(0).getCapability()+" elapsedMs="+(System.currentTimeMillis()-start)+" tokens="+model.takeTokens());
            assertEquals(c[1],u.getTasks().get(0).getCapability(),c[0]);
            AssistantUnderstandingResult.Subtask recognized=u.getTasks().get(0);
            assertFalse(recognized.needsClarification(),c[0]);
            assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(AssistantOrchestrator.class,"validateStructuredConditions",recognized.getCapability(),recognized.getConditions()),c[0]);
            if(c[0].contains("路通源"))assertEquals("路通源刹车片",u.getTasks().get(0).entity("PRODUCT"));
            if(c[0].startsWith("经典表现"))assertEquals("经典表现 530 4升",u.getTasks().get(0).entity("PRODUCT"));
        }
        AssistantUnderstandingResult mixed=service.understand("介绍一下美孚黑霸王，再查库存和价格",Collections.emptyMap(),tools,false);
        assertTrue(mixed.getTasks().stream().anyMatch(t->AssistantCompanyKnowledge.TOOL_NAME.equals(t.getCapability())));
        assertTrue(mixed.getTasks().stream().anyMatch(t->"query_product_stock".equals(t.getCapability())));
        AssistantExecutionEnvelope previous=new AssistantExecutionEnvelope();previous.setUnderstanding(mixed);
        AssistantUnderstandingResult follow=service.understand("它还有货吗",service.context(previous),tools,false);
        assertEquals("query_product_stock",follow.getTasks().get(0).getCapability());assertEquals("美孚黑霸王",follow.getTasks().get(0).entity("PRODUCT"));
        List<Map<String,Object>> customerTools=new cn.iocoder.yudao.module.erp.service.assistant.customer.CustomerAssistantToolRegistry(properties).definitions();
        AssistantUnderstandingResult customer=service.understand("路通源刹车片有吗",Collections.emptyMap(),customerTools,true);
        assertEquals("query_mall_stock",customer.getTasks().get(0).getCapability());assertEquals("路通源刹车片",customer.getTasks().get(0).entity("PRODUCT"));
        AssistantUnderstandingResult forbidden;
        try { forbidden=service.understand("介绍公司组织结构",Collections.emptyMap(),customerTools,true); }
        catch(AssistantFailure failure) { System.out.println("SEMANTIC_LIVE syntheticForbiddenResponse="+model.lastResponse); throw failure; }
        assertTrue(forbidden.getTasks().stream().allMatch(t->"unsupported".equals(t.getCapability())||t.getCapability().startsWith("clarify")));
    }
    private static class CapturingModel extends AssistantModelClient {
        String lastResponse;
        @Override public String understand(String instructions,Map<String,Object> input)throws java.io.IOException {
            lastResponse=super.understand(instructions,input);return lastResponse;
        }
    }
}
