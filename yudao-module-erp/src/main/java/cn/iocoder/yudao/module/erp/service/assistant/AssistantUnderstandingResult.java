package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;
import java.time.*;
import java.util.*;

/** 模型只描述语义；此类型有意不包含身份、对象 ID、权限或 SQL。 */
@Data
public class AssistantUnderstandingResult {
    private int schemaVersion = 1;
    private AssistantSemanticRequest.QueryMode queryMode;
    private List<Subtask> tasks = new ArrayList<>();
    /** 服务端填写的审计信息，不接受模型输入。 */
    private long elapsedMillis;

    @Data
    public static class Subtask {
        private String id;
        private String title;
        private String capability;
        /** 服务端记录模型识别到但当前入口未开放的能力。 */
        private String restrictedCapability;
        private String question;
        private String contextMode = "NEW_TOPIC";
        private List<String> inheritedFields = new ArrayList<>();
        private List<String> dependsOn = new ArrayList<>();
        private List<AssistantSemanticRequest.EntityMention> entities = new ArrayList<>();
        private Conditions conditions = new Conditions();
        private List<String> missingFields = new ArrayList<>();
        private String clarification;

        /** 空文案表示没有澄清要求；同时兼容已保存的旧语义结果。 */
        public boolean needsClarification() {
            return !missingFields.isEmpty() || capability != null && capability.startsWith("clarify")
                    || clarification != null && !clarification.trim().isEmpty();
        }

        public String entity(String type) {
            for (AssistantSemanticRequest.EntityMention entity : entities)
                if (type.equals(entity.getEntityType())) return entity.getKeyword();
            return null;
        }
    }

    @Data
    public static class Conditions {
        private AssistantPlan.Metric metric;
        private String group = "NONE";
        private String period;
        private String startDate;
        private String endDate;
        private AssistantPlan.SalesMeasure salesMeasure = AssistantPlan.SalesMeasure.AMOUNT;
        private AssistantPlan.RankOrder rankOrder = AssistantPlan.RankOrder.DESC;
        private int limit = 10;
        private AssistantPlan.PresentationMode presentationMode = AssistantPlan.PresentationMode.SUMMARY;
        private String stockMode = "ALL";
        private String priceField;
        private boolean priceRequested;
        private String archiveType;
        private String statusMode = "ENABLED";
        private String keyword;
        private String brand;
        private List<String> businessTypes = new ArrayList<>();
        private List<String> subtypes = new ArrayList<>();
        private String status;
        private String documentNo;
        /** NONE、COUNT、SUM_AMOUNT、SUM_QUANTITY。 */
        private String aggregation = "NONE";
        /** ALL、UNFINISHED、FULFILLED、RETURNED。 */
        private String fulfillment = "ALL";
        /** ALL、INBOUND、OUTBOUND。 */
        private String movement = "ALL";
        private String quantityOperator;
        private java.math.BigDecimal quantity;
        private boolean leafOnly;
        private boolean includeChildren = true;
        /** USERS、DEPARTMENTS、RELATION 或 UNVERIFIED。 */
        private String organizationView;

        public LocalDateTime[] range() {
            if (period == null || "CURRENT".equals(period)) return null;
            AssistantPlan plan = new AssistantPlan();
            plan.setMetric(AssistantPlan.Metric.SALE);
            plan.setPeriod(period); plan.setStartDate(startDate); plan.setEndDate(endDate);
            return plan.range(Clock.system(ZoneId.of("Asia/Shanghai")));
        }
    }
}
