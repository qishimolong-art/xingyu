package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;
import java.util.*;

@Data
public class AssistantToolCall {
    private String id;
    private String name;
    private Map<String,Object> arguments = new LinkedHashMap<>();
}
