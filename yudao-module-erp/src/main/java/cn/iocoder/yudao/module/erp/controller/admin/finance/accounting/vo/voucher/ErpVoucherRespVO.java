package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 凭证 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpVoucherRespVO {

    @Schema(description = "凭证编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("凭证编号")
    private Long id;

    @Schema(description = "凭证字", requiredMode = Schema.RequiredMode.REQUIRED, example = "记")
    @ExcelProperty("凭证字")
    private String voucherWord;

    @Schema(description = "凭证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "记-202605-000001")
    @ExcelProperty("凭证号")
    private String voucherNo;

    @Schema(description = "凭证日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-05-14")
    @ExcelProperty("凭证日期")
    private LocalDate voucherDate;

    @Schema(description = "归属年", example = "2026")
    @ExcelProperty("归属年")
    private Integer periodYear;

    @Schema(description = "归属月", example = "5")
    @ExcelProperty("归属月")
    private Integer periodMonth;

    @Schema(description = "附件张数", example = "1")
    private Integer attachmentCount;

    @Schema(description = "摘要", example = "差旅费报销")
    @ExcelProperty("摘要")
    private String summary;

    @Schema(description = "借方合计", example = "100.00")
    @ExcelProperty("借方合计")
    private BigDecimal totalDebit;

    @Schema(description = "贷方合计", example = "100.00")
    @ExcelProperty("贷方合计")
    private BigDecimal totalCredit;

    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("审核状态")
    private Integer auditStatus;

    @Schema(description = "凭证来源类型", example = "1")
    private Integer sourceType;

    @Schema(description = "业务来源类型", example = "1")
    private Integer sourceBizType;

    @Schema(description = "业务单据 ID", example = "1024")
    private Long sourceBizId;

    @Schema(description = "业务单号", example = "CGRK202605000001")
    private String sourceBizNo;

    @Schema(description = "业务单据金额", example = "100.00")
    @ExcelProperty("业务金额")
    private BigDecimal sourceBizAmount;

    @Schema(description = "是否已生成业务单据", example = "false")
    private Boolean generateBusinessDoc;

    @Schema(description = "辅助：项目 ID", example = "1024")
    private Long projectId;

    @Schema(description = "辅助：部门 ID", example = "1024")
    private Long deptId;

    @Schema(description = "辅助：客户 ID", example = "1024")
    private Long customerId;

    @Schema(description = "辅助：个人（用户 ID）", example = "1024")
    private Long personUserId;

    @Schema(description = "制单人 ID", example = "100")
    private Long makerUserId;

    @Schema(description = "制单人姓名", example = "张三")
    @ExcelProperty("制单人")
    private String makerUserName;

    @Schema(description = "记账人", example = "张三")
    private String bookkeeper;

    @Schema(description = "出纳", example = "李四")
    private String cashier;

    @Schema(description = "主管", example = "王五")
    private String supervisor;

    @Schema(description = "审核人 ID", example = "100")
    private Long auditorUserId;

    @Schema(description = "审核人姓名", example = "李四")
    @ExcelProperty("审核人")
    private String auditorUserName;

    @Schema(description = "审核日期")
    @ExcelProperty("审核日期")
    private LocalDateTime auditTime;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "凭证分录列表")
    private List<ErpVoucherItemRespVO> items;

}
