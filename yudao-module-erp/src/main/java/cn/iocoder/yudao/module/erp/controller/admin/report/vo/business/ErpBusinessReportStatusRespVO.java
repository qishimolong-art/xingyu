package cn.iocoder.yudao.module.erp.controller.admin.report.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "ERP 统一经营报表状态 Response VO")
@Data
public class ErpBusinessReportStatusRespVO {

    @Schema(description = "状态口径版本", example = "2026-09-17-plan00")
    private String version;

    @Schema(description = "统一经营报表名称", example = "统一经营报表")
    private String reportName;

    @Schema(description = "统一口径说明")
    private List<String> globalNotes;

    @Schema(description = "计划 00-12 模块状态")
    private List<ModuleStatus> modules;

    @Schema(description = "ERP 统一经营报表模块状态")
    @Data
    public static class ModuleStatus {

        @Schema(description = "计划编码", example = "01")
        private String code;

        @Schema(description = "模块名称", example = "销售及客户经营")
        private String name;

        @Schema(description = "状态", example = "READONLY_AVAILABLE")
        private String status;

        @Schema(description = "当前可展示能力")
        private List<String> displayCapabilities;

        @Schema(description = "未完成项或不可用原因")
        private List<String> pendingItems;

        @Schema(description = "依赖 SQL")
        private List<String> dependencySql;

        @Schema(description = "依赖开关")
        private List<String> dependencySwitches;

        @Schema(description = "依赖权限")
        private List<String> dependencyPermissions;

        @Schema(description = "风险提示")
        private List<String> riskNotes;
    }
}
