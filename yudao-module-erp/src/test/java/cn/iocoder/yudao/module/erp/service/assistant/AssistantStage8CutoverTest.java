package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** 阶段 8：默认切换、可回退配置、答案/权限/失败/性能对比与汇总门禁。 */
class AssistantStage8CutoverTest {

    @Test
    @SuppressWarnings("unchecked")
    void publicConfigurationCutsOverToV2AndKeepsEveryRollbackSwitch() throws Exception {
        Map<String,Object> assistant=null;
        try(InputStream input= Files.newInputStream(Paths.get("../yudao-server/src/main/resources/application.yaml"))) {
            for(Object document:new Yaml().loadAll(input)) {
                if(!(document instanceof Map)) continue;
                Object erp=((Map<?,?>)document).get("erp");
                if(erp instanceof Map && ((Map<?,?>)erp).get("assistant") instanceof Map)
                    assistant=(Map<String,Object>)((Map<?,?>)erp).get("assistant");
            }
        }
        assertNotNull(assistant);
        Map<String,Object> v2=(Map<String,Object>)assistant.get("v2");
        assertEquals("V2",v2.get("mode"));
        for(String key:Arrays.asList("gray-tenant-ids","gray-user-ids","gray-department-ids","gray-entries",
                "gray-capabilities","fallback-entries","fallback-capabilities","disabled-entries","disabled-capabilities"))
            assertTrue(v2.get(key) instanceof List && ((List<?>)v2.get(key)).isEmpty(),key);
        assertEquals(Boolean.TRUE,v2.get("fallback-on-error"));
        assertEquals(Boolean.TRUE,v2.get("knowledge-enabled"));
        assertEquals(Boolean.TRUE,v2.get("mixed-enabled"));
        assertEquals(Boolean.TRUE,((Map<String,Object>)assistant.get("orchestration")).get("text-to-sql-enabled"));
    }

    @Test
    void shadowComparisonUsesAnswerFingerprintPermissionFailureAndPerformanceWithoutPersistingRows() {
        AssistantExecutionEnvelope envelope=envelope("query_sale_documents");
        Map<String,Object> first=result("100.00","runtime-a");
        Map<String,Object> second=result("100.00","runtime-b");
        AssistantUnifiedResult v1=AssistantUnifiedResult.success("V1",envelope,first,100L);
        AssistantUnifiedResult v2=AssistantUnifiedResult.success("V2",envelope,second,250L);

        Map<String,Object> matched=AssistantShadowComparator.compare(v1,v2,3.0D,5000L);

        assertEquals(v1.getResultFingerprint(),v2.getResultFingerprint(),"运行时 messageId 不应造成答案差异");
        assertEquals(64,v1.getResultFingerprint().length());
        assertTrue(Boolean.TRUE.equals(matched.get("answerMatched")));
        assertTrue(Boolean.TRUE.equals(matched.get("permissionMatched")));
        assertTrue(Boolean.TRUE.equals(matched.get("failureMatched")));
        assertTrue(Boolean.TRUE.equals(matched.get("performanceWithinThreshold")));
        assertTrue(Boolean.TRUE.equals(matched.get("releaseEligible")));
        assertFalse(matched.toString().contains("100.00"),"审计比较不得保存业务答案");

        AssistantUnifiedResult changed=AssistantUnifiedResult.success("V2",envelope,result("101.00","runtime-c"),8000L);
        Map<String,Object> mismatch=AssistantShadowComparator.compare(v1,changed,3.0D,5000L);
        assertFalse(Boolean.TRUE.equals(mismatch.get("answerMatched")));
        assertFalse(Boolean.TRUE.equals(mismatch.get("performanceWithinThreshold")));
        assertFalse(Boolean.TRUE.equals(mismatch.get("releaseEligible")));
    }

    @Test
    void rolloutSummarySeparatesEnginesFallbacksFailuresLatencyAndShadowMismatches() {
        AssistantStore store=new AssistantStore();AssistantExecutionCodec codec=new AssistantExecutionCodec();
        ReflectionTestUtils.setField(store,"codec",codec);
        AssistantExecutionEnvelope v1=envelope("query_sale_documents");v1.setEngine("V1");
        AssistantExecutionAudit a1=new AssistantExecutionAudit();a1.setSelectedEngine("V1");a1.setEntryFallback(true);
        Map<String,Object> shadow=new LinkedHashMap<>();shadow.put("executed",true);shadow.put("matched",false);
        shadow.put("releaseEligible",false);shadow.put("permissionMatched",false);shadow.put("answerMatched",false);
        a1.setShadowComparison(shadow);v1.setExecutionAudit(a1);
        AssistantExecutionEnvelope v2=envelope("query_sale_documents");v2.setEngine("V2");
        AssistantExecutionAudit a2=new AssistantExecutionAudit();a2.setSelectedEngine("V2");a2.setAutomaticFallback(true);
        a2.setCapabilityFallback(true);v2.setExecutionAudit(a2);
        List<Map<String,Object>> rows=Arrays.asList(row(codec.write(v1),"SUCCESS",100L),
                row(codec.write(v2),"QUERY_FAILED",900L));

        Map<String,Object> summary=store.summarizeRollout(rows);

        assertEquals(2,summary.get("sampleCount"));assertEquals(50D,summary.get("technicalFailureRate"));
        assertEquals(1,summary.get("entryFallbackCount"));assertEquals(1,summary.get("capabilityFallbackCount"));
        assertEquals(1,summary.get("automaticFallbackCount"));assertEquals(1,summary.get("shadowExecutedCount"));
        assertEquals(1,summary.get("permissionMismatchCount"));assertEquals(1,summary.get("answerMismatchCount"));
        Map<?,?> performance=(Map<?,?>)summary.get("performance");
        assertEquals(100L,((Map<?,?>)performance.get("V1")).get("p95Millis"));
        assertEquals(900L,((Map<?,?>)performance.get("V2")).get("p95Millis"));
    }

    private static AssistantExecutionEnvelope envelope(String tool) {
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();envelope.setToolName(tool);
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
        AssistantSemanticRequest request=new AssistantSemanticRequest();request.setCapabilityId(tool);
        request.setIntentId("DOCUMENT_QUERY");request.setBusinessObjects(Collections.singletonList("SALE_ORDER"));
        envelope.setSemanticRequest(request);return envelope;
    }
    private static Map<String,Object> result(String amount,String messageId) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("summary",Collections.singletonList(Collections.singletonMap("amount",amount)));
        result.put("rows",Collections.singletonList(Collections.singletonMap("amount",amount)));
        result.put("messageId",messageId);return result;
    }
    private static Map<String,Object> row(String plan,String status,long elapsed) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("plan_json",plan);row.put("status",status);
        row.put("elapsed_ms",elapsed);return row;
    }
}
