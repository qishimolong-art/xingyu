package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 三个入口共用的 V2 语义请求。原始问题只用于本次执行；结构化字段用于能力选择、
 * 上下文隔离、审计和 V1/V2 影子对比，不能用于扩大数据权限。
 */
@Data
public class AssistantSemanticRequest {

    public enum Source { WEB_PAGE, WEB_DRAWER, WECOM, RERUN, UNKNOWN }
    public enum QueryMode { DATA, KNOWLEDGE, MIXED }

    private int schemaVersion = 4;
    private AssistantUnderstandingResult understanding;
    private String requestId;
    private String conversationId;
    private String question;
    private String originalQuestion;
    private String normalizedQuestion;
    private List<Map<String, Object>> phoneticCorrections = new ArrayList<>();
    private Source source = Source.UNKNOWN;
    private String currentPage;
    private Long tenantId;
    private Long userId;
    /** 仅用于灰度归属和审计；不能代替数据权限范围。 */
    private Long departmentId;
    private QueryMode queryMode = QueryMode.DATA;
    private String capabilityId;
    /** 与具体能力解耦的语义领域，例如 INVENTORY、COMPANY_KNOWLEDGE。 */
    private String domain;
    /** 用户要求的操作，例如 COUNT_DISTINCT、LIST、RANK。 */
    private String operation;
    /** NONE、COUNT、COUNT_DISTINCT 或 SUM。 */
    private String aggregation;
    /** NONE、OPTIONAL 或 REQUIRED；类别级统计不得被当成 REQUIRED 实体查询。 */
    private String entityRequirement;
    /** 高置信确定性路由的综合置信度；未命中时为 0。 */
    private double routingConfidence;
    private String routingReason;
    /** 公司知识内部语义框架；只用于受控路由、上下文和审计。 */
    private Map<String, Object> knowledgeIntent = new LinkedHashMap<>();
    /** 归一化意图，例如 METRIC_QUERY、DOCUMENT_QUERY、ENTITY_LOOKUP。 */
    private String intentId;
    /** 只记录对象类型；对象 id 和名称仍由有权限的实体解析器确认。 */
    private List<String> businessObjects = new ArrayList<>();
    private String metric;
    private List<String> dimensions = new ArrayList<>();
    private List<SemanticFilter> filters = new ArrayList<>();
    private TimeRange timeRange;
    private List<String> documentStatuses = new ArrayList<>();
    private SortSpec sort;
    private List<EntityMention> entityMentions = new ArrayList<>();
    private ContextDecision context = new ContextDecision();
    private List<String> missingFields = new ArrayList<>();
    /** 兼容阶段 2 已持久化的轻量上下文字段。 */
    private Map<String, Object> inheritedContext = new LinkedHashMap<>();

    @Data
    public static class SemanticFilter {
        private String field;
        private String operator;
        private Object value;
        private String source;
    }

    @Data
    public static class TimeRange {
        private String period;
        private String startDate;
        private String endDate;
        /** CURRENT、EXPLICIT 或 INHERITED。 */
        private String source;
    }

    @Data
    public static class SortSpec {
        private String field;
        private String direction;
        private Integer limit;
        private String source;
    }

    @Data
    public static class EntityMention {
        private String entityType;
        private String keyword;
        private String role;
        private String extractionMethod;
    }

    @Data
    public static class ContextDecision {
        /** NEW_TOPIC、INHERIT 或 MISSING_CONTEXT。 */
        private String mode = "NEW_TOPIC";
        private String reason = "NO_PREVIOUS_CONTEXT";
        private String sourceRequestId;
        private List<String> references = new ArrayList<>();
        private List<InheritedField> inheritedFields = new ArrayList<>();
    }

    @Data
    public static class InheritedField {
        private String field;
        private String source;
        private String reason;
    }
}
