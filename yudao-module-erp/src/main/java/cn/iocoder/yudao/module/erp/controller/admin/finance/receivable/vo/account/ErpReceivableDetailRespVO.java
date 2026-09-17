package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应收账款明细 Response VO")
@ExcelIgnoreUnannotated
@Data
public class ErpReceivableDetailRespVO {

    @ExcelProperty("单据类型")
    private String docType;

    private Integer bizType;

    private Long bizId;

    @ExcelProperty("单据日期")
    private LocalDateTime docDate;

    @ExcelProperty("单据编号")
    private String docNo;

    private Long deptId;

    private Integer returnStatus;

    private Boolean priceAdjusted;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("上笔余额")
    private BigDecimal prevBalance;

    @ExcelProperty("增加应收")
    private BigDecimal increaseAmount;

    @ExcelProperty("其他应收")
    private BigDecimal otherReceivableAmount;

    @ExcelProperty("收款金额")
    private BigDecimal receiptAmount;

    private BigDecimal writeOffAmount;

    private BigDecimal allocatedAmount;

    private BigDecimal writeOffBaseAmount;

    @ExcelProperty("余额")
    private BigDecimal balance;
}
