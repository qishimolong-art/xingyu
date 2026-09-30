package cn.iocoder.yudao.module.erp.service.assistant;
import java.util.*;
public class AssistantEntityChoice extends AssistantFailure {
    private final String field;
    private final List<Map<String,Object>> candidates;
    public AssistantEntityChoice(String field,List<Map<String,Object>> candidates) {
        super("CLARIFY_ENTITY","请从有权限的候选对象中选择");this.field=field;this.candidates=candidates;
    }
    public Map<String,Object> response() {
        Map<String,Object> result=new LinkedHashMap<>();result.put("clarification",getMessage());result.put("field",field);result.put("candidates",candidates);return result;
    }
}
