package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应付明细 Response VO")
@ExcelIgnoreUnannotated
@Data
public class ErpPayableDetailRespVO {

    @Schema(description = "单据类型", example = "采购入库")
    @ExcelProperty("单据类型")
    private String docType;

    @Schema(description = "业务类型")
    private Integer bizType;

    @Schema(description = "业务单据编号")
    private Long bizId;

    @Schema(description = "单据日期")
    @ExcelProperty("单据日期")
    private LocalDateTime docDate;

    @Schema(description = "单据号", example = "CGRK202605270001")
    @ExcelProperty("单据编号")
    private String docNo;

    @Schema(description = "采购入库单是否被调过价")
    private Boolean priceAdjusted;

    @Schema(description = "部门 ID", example = "1")
    private Long deptId;

    @Schema(description = "部门名称", example = "采购部")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "上次余额")
    @ExcelProperty("上次余额")
    private BigDecimal prevBalance;

    @Schema(description = "应付款")
    @ExcelProperty("应付款")
    private BigDecimal increaseAmount;

    @Schema(description = "付款金额")
    @ExcelProperty("付款金额")
    private BigDecimal paymentAmount;

    @Schema(description = "核销金额")
    private BigDecimal writeOffAmount;

    @Schema(description = "业务单据已关联的有效付款核销金额")
    private BigDecimal allocatedAmount;

    @Schema(description = "核销状态判断的单据基准金额")
    private BigDecimal writeOffBaseAmount;

    @Schema(description = "余额")
    @ExcelProperty("余额")
    private BigDecimal balance;
}
