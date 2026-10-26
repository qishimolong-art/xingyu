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

    @ExcelProperty(value = "上笔余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal prevBalance;

    @ExcelProperty(value = "增加金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal increaseAmount;

    @ExcelProperty(value = "付款金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal paymentAmount;

    private BigDecimal writeOffAmount;

    @ExcelProperty(value = "已付金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal allocatedAmount;

    private BigDecimal writeOffBaseAmount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("附件")
    private String fileUrl;

    @ExcelProperty(value = "余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;
}
