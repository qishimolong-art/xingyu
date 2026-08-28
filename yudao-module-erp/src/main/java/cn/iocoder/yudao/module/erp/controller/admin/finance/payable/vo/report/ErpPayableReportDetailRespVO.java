package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPayableReportDetailRespVO {

    @ExcelProperty("单据类型")
    private String docType;

    private Integer bizType;
    private Long bizId;

    @ExcelProperty("单据日期")
    private LocalDateTime docDate;

    @ExcelProperty("单据编号")
    private String docNo;

    @ExcelProperty("上笔余额")
    private BigDecimal prevBalance;

    @ExcelProperty("增加金额")
    private BigDecimal increaseAmount;

    @ExcelProperty("付款金额")
    private BigDecimal paymentAmount;

    private BigDecimal writeOffAmount;

    @ExcelProperty("已付金额")
    private BigDecimal allocatedAmount;

    private BigDecimal writeOffBaseAmount;

    @ExcelProperty("余额")
    private BigDecimal balance;
}
