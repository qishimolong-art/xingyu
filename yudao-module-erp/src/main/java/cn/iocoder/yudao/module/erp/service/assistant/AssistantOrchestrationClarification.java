package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.Map;

public class AssistantOrchestrationClarification extends AssistantFailure {
    private final AssistantExecutionEnvelope envelope;
    private final Map<String,Object> response;
    public AssistantOrchestrationClarification(AssistantExecutionEnvelope envelope,Map<String,Object> response) {
        super("CLARIFY",String.valueOf(response.get("clarification")));this.envelope=envelope;this.response=response;
    }
    public AssistantExecutionEnvelope getEnvelope(){return envelope;}
    public Map<String,Object> getResponse(){return response;}
}
