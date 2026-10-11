package cn.iocoder.yudao.module.erp.service.assistant;
import java.util.*;
public class AssistantEntityChoice extends AssistantFailure {
    private final String field;
    private final List<Map<String,Object>> candidates;
    public AssistantEntityChoice(String field,List<Map<String,Object>> candidates) {
        super("CLARIFY_ENTITY","请从有权限的候选对象中选择");this.field=field;this.candidates=candidates;
    }
    public Map<String,Object> response() {
        String text=getMessage();String original=null;boolean phonetic=!candidates.isEmpty();boolean near=false;
        for(Map<String,Object> candidate:candidates) {
            String type=String.valueOf(candidate.get("matchType"));phonetic&=type.contains("PHONETIC");near|=type.contains("NEAR");
            if(original==null && candidate.get("correctedFrom")!=null) original=String.valueOf(candidate.get("correctedFrom"));
        }
        if(phonetic && original!=null) text="已按"+(near?"常见近音":"同音")+"识别“"+original+"”，请从有权限的候选对象中选择";
        Map<String,Object> result=AssistantClarification.entityChoice(text,candidates);
        if(phonetic && original!=null) result.put("correctionNotice",text);
        result.put("field",field);return result;
    }
}
