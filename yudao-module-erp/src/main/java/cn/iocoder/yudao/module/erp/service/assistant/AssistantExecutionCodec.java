package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;

/** Backward compatible persistence codec for legacy AssistantPlan and v2 envelopes. */
@Component
public class AssistantExecutionCodec {
    private final ObjectMapper json=new ObjectMapper();
    public AssistantExecutionEnvelope read(String value,String question) {
        if(value==null || value.trim().isEmpty()) return null;
        try {
            JsonNode root=json.readTree(value);
            if(root.has("routeType") || root.has("schemaVersion")) return json.treeToValue(root,AssistantExecutionEnvelope.class);
            return AssistantExecutionEnvelope.metric(question,json.treeToValue(root,AssistantPlan.class));
        } catch(Exception e) {throw new AssistantFailure("INVALID_HISTORY","历史查询格式无效，请重新提问");}
    }
    public String write(AssistantExecutionEnvelope value) {
        try{return value==null?null:json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}
    }
}
