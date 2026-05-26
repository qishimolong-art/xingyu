package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 会计科目导入结果（预检 / 实际导入共用）
 */
@Data
@Schema(description = "管理后台 - ERP 会计科目导入结果 Resp VO")
public class ErpAccountingSubjectImportRespVO {

    /** 重复（已存在）的科目编码列表 */
    @Schema(description = "已存在的科目编码列表")
    private List<String> existedCodes = new ArrayList<>();

    /** 新增的科目编码列表 */
    @Schema(description = "新增的科目编码列表")
    private List<String> createdCodes = new ArrayList<>();

    /** 被覆盖更新的科目编码列表 */
    @Schema(description = "被覆盖更新的科目编码列表")
    private List<String> updatedCodes = new ArrayList<>();

    /** 跳过的科目编码列表 */
    @Schema(description = "被跳过的科目编码列表")
    private List<String> skippedCodes = new ArrayList<>();

    /** 失败明细：编码 → 失败原因 */
    @Schema(description = "失败明细")
    private List<FailureItem> failures = new ArrayList<>();

    /** 是否为预检（dryRun=true 时不真写库） */
    @Schema(description = "是否为预检")
    private Boolean dryRun;

    @Data
    public static class FailureItem {
        private String subjectCode;
        private String reason;

        public FailureItem(String code, String reason) {
            this.subjectCode = code;
            this.reason = reason;
        }
    }

}
