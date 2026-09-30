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
    private int conversationDays = 30;
    private int auditDays = 90;
    private List<String> publishedMetrics = new ArrayList<>();
    private Orchestration orchestration = new Orchestration();
    private EntitySearch entitySearch = new EntitySearch();
    private ReadOnly readOnly = new ReadOnly();
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
    @Data public static class EntitySearch {
        /** Kept off by default; local/gray environments opt in after regression checks. */
        private boolean enabled = false;
        private int maxCandidates = 20;
    }
}
