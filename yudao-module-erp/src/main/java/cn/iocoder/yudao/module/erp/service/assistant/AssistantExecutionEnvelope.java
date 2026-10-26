package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;
import java.util.*;

/** Persisted execution state. Legacy AssistantPlan JSON remains readable. */
@Data
public class AssistantExecutionEnvelope {
    public enum RouteType { VERIFIED_METRIC, BUSINESS_TOOL, TEXT_TO_SQL, KNOWLEDGE, MIXED }
    private int schemaVersion = 4;
    private String question;
    /** 用户原始问法；不得被同音归一化覆盖。 */
    private String originalQuestion;
    /** 实际用于路由和查询规划的受控归一化问法。 */
    private String normalizedQuestion;
    private List<Map<String,Object>> corrections = new ArrayList<>();
    private RouteType routeType;
    private AssistantPlan legacyPlan;
    private String toolName;
    private Map<String,Object> arguments = new LinkedHashMap<>();
    private List<Map<String,Object>> toolCalls = new ArrayList<>();
    private Map<String,Object> resultMetadata = new LinkedHashMap<>();
    /** Compatible structured semantic result used by audit, clarification and rerun. */
    private Map<String,Object> semanticResult = new LinkedHashMap<>();
    private String clarification;
    private AssistantSemanticRequest semanticRequest;
    private String engine = "V1";
    private AssistantExecutionAudit executionAudit;
    private AssistantUnderstandingResult understanding;
    private AssistantUnderstandingResult.Subtask understandingTask;
    private List<AssistantExecutionEnvelope> subtasks = new ArrayList<>();

    public static AssistantExecutionEnvelope metric(String question, AssistantPlan plan) {
        AssistantExecutionEnvelope value=new AssistantExecutionEnvelope();
        value.setQuestion(AssistantModelClient.redact(question));value.setRouteType(RouteType.VERIFIED_METRIC);
        value.setToolName("query_verified_metric");value.setLegacyPlan(plan);return value;
    }
}
