package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 银行转账单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpFinanceTransferRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "转账单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "YHZZ202605000001")
    @ExcelProperty("转账单号")
    private String no;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    @DictFormat(cn.iocoder.yudao.module.erp.enums.DictTypeConstants.AUDIT_STATUS)
    private Integer status;

    @Schema(description = "凭证号", example = "记-202607-000001")
    @ExcelProperty("凭证号")
    private String voucherNo;

    @Schema(description = "转账时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("转账时间")
    private LocalDateTime transferTime;

    @Schema(description = "转出账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long outAccountId;

    @Schema(description = "转出账户名称", example = "工商银行")
    @ExcelProperty("转出账户")
    private String outAccountName;

    @Schema(description = "转入账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long inAccountId;

    @Schema(description = "转入账户名称", example = "现金账户")
    @ExcelProperty("转入账户")
    private String inAccountName;

    @Schema(description = "转账金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @ExcelProperty("转账金额")
    private BigDecimal transferPrice;

    @Schema(description = "汇率", example = "1")
    @ExcelProperty("汇率")
    private BigDecimal exchangeRate;

    @Schema(description = "手续费", example = "1.00")
    @ExcelProperty("手续费")
    private BigDecimal feePrice;

    @Schema(description = "费用项目", example = "银行手续费")
    @ExcelProperty("费用项目")
    private String feeExpenseCategory;

    @Schema(description = "财务人员编号", example = "100")
    private Long financeUserId;

    @Schema(description = "财务人员名称", example = "张三")
    @ExcelProperty("财务人员")
    private String financeUserName;

    @Schema(description = "所属部门编号", example = "100")
    private Long deptId;

    @Schema(description = "所属部门名称", example = "财务部")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "备注", example = "同行转账")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "附件 URL", example = "https://example.com/file.pdf")
    private String fileUrl;

    @Schema(description = "创建人", example = "1")
    private String creator;

    @Schema(description = "创建人名称", example = "管理员")
    @ExcelProperty("创建人")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改人", example = "1")
    private String updater;

    @Schema(description = "修改人名称", example = "管理员")
    @ExcelProperty("修改人")
    private String updaterName;

    @Schema(description = "修改时间")
    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "审核人", example = "审核员")
    @ExcelProperty("审核人")
    private String auditorName;

    @Schema(description = "审核日期")
    @ExcelProperty("审核日期")
    private LocalDateTime auditTime;

}
