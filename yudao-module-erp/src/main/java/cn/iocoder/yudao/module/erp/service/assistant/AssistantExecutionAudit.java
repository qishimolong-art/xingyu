package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** 随执行信封持久化的双引擎审计模型。 */
@Data
public class AssistantExecutionAudit {
    private String configuredMode;
    private String selectedEngine;
    private String decisionReason;
    private boolean capabilityFallback;
    private boolean entryFallback;
    private boolean automaticFallback;
    private String fallbackCode;
    private AssistantUnifiedResult foreground;
    private Map<String, Object> shadowComparison = new LinkedHashMap<>();
}
