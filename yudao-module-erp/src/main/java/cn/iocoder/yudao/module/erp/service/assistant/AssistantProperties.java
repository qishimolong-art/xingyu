package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** Disabled until both deployment and metric verification have been completed. */
@Data
@Component
@ConfigurationProperties(prefix = "erp.assistant")
public class AssistantProperties {
    private boolean enabled = false;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-flash";
    @lombok.ToString.Exclude
    private String apiKey = "";
    private int modelTimeoutSeconds = 20;
    /** 部署级回滚开关；启用后语义失败不允许单次请求自动回退。 */
    private boolean semanticUnderstandingEnabled = true;
    private int conversationDays = 30;
    private int auditDays = 90;
    /** Page capability presentation cache; business queries always authorize in real time. */
    private int readinessCacheSeconds = 60;
    private List<String> publishedMetrics = new ArrayList<>();
    private Orchestration orchestration = new Orchestration();
    private V2 v2 = new V2();
    private EntitySearch entitySearch = new EntitySearch();
    private Phonetic phonetic = new Phonetic();
    private ReadOnly readOnly = new ReadOnly();
    private CustomerApp customerApp = new CustomerApp();
    @Data public static class Orchestration {
        /** LEGACY, GRAY or NEW. Production keeps LEGACY until acceptance is complete. */
        private String mode = "LEGACY";
        private List<Long> grayUserIds = new ArrayList<>();
        private int maxToolRounds = 5;
        private boolean textToSqlEnabled = false;
        /** Published only after schema, permission and business reconciliation checks. */
        private List<String> publishedDatasets = new ArrayList<>();
    }
    @Data public static class ReadOnly {
        private String url = "";
        private String username = "";
        @lombok.ToString.Exclude private String password = "";
    }
    @Data public static class V2 {
        /** V1、V2_SHADOW、V2_GRAY 或 V2；缺少配置时安全回退到 V1。 */
        private String mode = "V1";
        private List<Long> grayTenantIds = new ArrayList<>();
        private List<Long> grayUserIds = new ArrayList<>();
        private List<Long> grayDepartmentIds = new ArrayList<>();
        private List<String> grayEntries = new ArrayList<>();
        private List<String> grayCapabilities = new ArrayList<>();
        /** 命中的入口或能力无条件走 V1，分别用于入口级和能力级回退。 */
        private List<String> fallbackEntries = new ArrayList<>();
        private List<String> fallbackCapabilities = new ArrayList<>();
        /** 临时关闭指定入口或能力；关闭发生在任何数据读取之前。 */
        private List<String> disabledEntries = new ArrayList<>();
        private List<String> disabledCapabilities = new ArrayList<>();
        /** 公司知识和混合路由可独立于纯数据查询关闭。 */
        private boolean knowledgeEnabled = true;
        private boolean mixedEnabled = true;
        /** V2 前台执行失败且尚未返回结果时，自动用 V1 重试。 */
        private boolean fallbackOnError = true;
        /** 影子结果只有同时超过倍率和绝对增量时才判定为性能退化。 */
        private double maxShadowLatencyRatio = 3.0D;
        private long maxShadowLatencyDeltaMillis = 5000L;
    }
    @Data public static class EntitySearch {
        /** Kept off by default; local/gray environments opt in after regression checks. */
        private boolean enabled = false;
        private int maxCandidates = 20;
    }
    @Data public static class Phonetic {
        /** 同音识别仅改变语义理解，不绕过实体、字段或数据权限。 */
        private boolean enabled = true;
        /** 受控启用平翘舌、前后鼻音、n/l、f/h 等常见近音。 */
        private boolean nearSoundEnabled = true;
        private int maxCorrections = 4;
        private int maxInitialVariants = 8;
    }
    @Data public static class CustomerApp {
        /** Production remains disabled until customer-account acceptance is complete. */
        private boolean enabled = false;
        private int maxRows = 20;
        private int orderDefaultDays = 90;
        private List<String> allowedCapabilities = new ArrayList<>(java.util.Arrays.asList(
                "CUSTOMER_PRICE", "MALL_STOCK", "CUSTOMER_ORDER", "CLARIFICATION"));
    }
}
