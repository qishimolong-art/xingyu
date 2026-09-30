package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;
import java.util.*;

/** Persisted v2 execution state. Legacy AssistantPlan JSON remains readable. */
@Data
public class AssistantExecutionEnvelope {
    public enum RouteType { VERIFIED_METRIC, BUSINESS_TOOL, TEXT_TO_SQL }
    private int schemaVersion = 2;
    private String question;
    private RouteType routeType;
    private AssistantPlan legacyPlan;
    private String toolName;
    private Map<String,Object> arguments = new LinkedHashMap<>();
    private List<Map<String,Object>> toolCalls = new ArrayList<>();
    private Map<String,Object> resultMetadata = new LinkedHashMap<>();
    /** Compatible structured semantic result used by audit, clarification and rerun. */
    private Map<String,Object> semanticResult = new LinkedHashMap<>();
    private String clarification;

    public static AssistantExecutionEnvelope metric(String question, AssistantPlan plan) {
        AssistantExecutionEnvelope value=new AssistantExecutionEnvelope();
        value.setQuestion(AssistantModelClient.redact(question));value.setRouteType(RouteType.VERIFIED_METRIC);
        value.setToolName("query_verified_metric");value.setLegacyPlan(plan);return value;
    }
}
