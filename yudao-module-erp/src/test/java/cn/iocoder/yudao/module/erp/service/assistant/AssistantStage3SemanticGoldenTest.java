package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantStage3SemanticGoldenTest {

    private final AssistantSemanticAnalyzer analyzer = new AssistantSemanticAnalyzer();

    @Test
    void semanticGoldenQuestionsCoverCoreSynonymsContextSwitchAndMissingParameters() throws Exception {
        List<String[]> rows = rows();
        assertTrue(rows.size() >= 40, "阶段 3 语义黄金集不得少于 40 条");
        Set<String> contexts = new LinkedHashSet<>();
        Set<String> metrics = new LinkedHashSet<>();
        List<Executable> checks = new ArrayList<>();
        for (String[] row : rows) {
            String id = row[0];
            AssistantExecutionEnvelope previous = previous(row[3], row[4], row[5]);
            AssistantSemanticRequest request = new AssistantSemanticRequest();
            request.setQuestion(row[1]);
            request.setCapabilityId(row[2]);

            analyzer.analyze(request, previous);

            checks.add(() -> assertAll(id,
                    () -> assertEquals(value(row[6]), request.getIntentId(), id + " intent"),
                    () -> assertEquals(value(row[7]), request.getMetric(), id + " metric"),
                    () -> assertEquals(value(row[8]), request.getTimeRange() == null ? null : request.getTimeRange().getPeriod(), id + " period"),
                    () -> assertEquals(row[9], request.getContext().getMode(), id + " context"),
                    () -> assertEquals(values(row[10]), new LinkedHashSet<>(request.getBusinessObjects()), id + " objects"),
                    () -> assertEquals(values(row[11]), new LinkedHashSet<>(request.getDimensions()), id + " dimensions"),
                    () -> assertEquals(values(row[12]), new LinkedHashSet<>(request.getDocumentStatuses()), id + " statuses"),
                    () -> assertEquals(values(row[13]), new LinkedHashSet<>(request.getMissingFields()), id + " missing"),
                    () -> assertSort(id, row[14], request.getSort())));
            contexts.add(request.getContext().getMode());
            if (request.getMetric() != null) metrics.add(request.getMetric());
        }
        assertAll(checks);
        assertEquals(new LinkedHashSet<>(Arrays.asList("NEW_TOPIC", "INHERIT", "MISSING_CONTEXT")), contexts);
        assertTrue(metrics.containsAll(Arrays.asList("SALE", "PURCHASE", "STOCK", "STOCK_SKU",
                "RECEIPT", "PAYMENT", "RECEIVABLE", "PAYABLE", "USER_COUNT")));
    }

    private static void assertSort(String id,String expected,AssistantSemanticRequest.SortSpec actual) {
        if(expected.isEmpty()) {
            assertNull(actual,id+" sort");return;
        }
        String[] values=expected.split(":");
        assertNotNull(actual,id+" sort");
        assertEquals(values[0],actual.getDirection(),id+" sort direction");
        assertEquals(Integer.valueOf(values[1]),actual.getLimit(),id+" sort limit");
    }

    private static AssistantExecutionEnvelope previous(String metric,String period,String entityType) {
        if(metric.isEmpty() && period.isEmpty() && entityType.isEmpty()) return null;
        AssistantPlan plan=new AssistantPlan();
        if(!metric.isEmpty()) plan.setMetric(AssistantPlan.Metric.valueOf(metric));
        if(!period.isEmpty()) plan.setPeriod(period);
        AssistantExecutionEnvelope envelope=AssistantExecutionEnvelope.metric("上一轮",plan);
        AssistantSemanticRequest request=new AssistantSemanticRequest();request.setRequestId("previous");
        envelope.setSemanticRequest(request);
        if(!entityType.isEmpty()) {
            Map<String,Object> entity=new LinkedHashMap<>();entity.put("id",1L);entity.put("name","上一轮对象");entity.put("entityType",entityType);
            envelope.getArguments().put("resolvedEntity",entity);
            Map<String,Object> all=new LinkedHashMap<>();all.put(entityType,entity);envelope.getArguments().put("resolvedEntities",all);
        }
        return envelope;
    }

    private static Set<String> values(String value) {
        if(value==null || value.isEmpty()) return Collections.emptySet();
        return new LinkedHashSet<>(Arrays.asList(value.split(";")));
    }

    private static String value(String value) {
        return value==null || value.isEmpty()?null:value;
    }

    private static List<String[]> rows() throws Exception {
        InputStream input=AssistantStage3SemanticGoldenTest.class.getResourceAsStream(
                "/assistant/assistant_stage3_semantic_cases.psv");
        assertNotNull(input,"阶段 3 语义黄金集不存在");
        List<String[]> rows=new ArrayList<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while((line=reader.readLine())!=null) {
                if(line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parsed=line.split("\\|",-1);
                assertTrue(parsed.length==14 || parsed.length==15,"PSV 列数错误: "+line);
                String[] values=Arrays.copyOf(parsed,15);
                if(values[14]==null) values[14]="";
                rows.add(values);
            }
        }
        return rows;
    }
}
