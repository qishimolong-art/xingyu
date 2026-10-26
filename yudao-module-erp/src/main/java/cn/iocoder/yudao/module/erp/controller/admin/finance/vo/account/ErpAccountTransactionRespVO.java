package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 账户收付款流水 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpAccountTransactionRespVO {

    @Schema(description = "单据编号")
    @ExcelProperty("单据号")
    private String no;

    @Schema(description = "账单类型：receipt-收款，payment-付款")
    private String billType;

    @Schema(description = "单据类型名称")
    @ExcelProperty("业务类型")
    private String billTypeName;

    @Schema(description = "金额")
    @ExcelProperty(value = "交易金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal amount;

    @Schema(description = "该笔交易后的账户余额")
    @ExcelProperty(value = "账户余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;

    @Schema(description = "交易时间")
    @ExcelProperty("交易时间")
    private LocalDateTime transactionTime;

    @Schema(description = "关联业务单据号")
    @ExcelProperty("来源单号")
    private String bizNo;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

}
