package cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpSettlementOffsetDetailExportRespVO {

    @ExcelProperty(value = "审批日期", index = 0)
    private LocalDateTime approveTime;

    @ExcelProperty(value = "往来类型", index = 1)
    private String direction;

    @ExcelProperty(value = "单据类型", index = 2)
    private String docType;

    @ExcelProperty(value = "单据编号", index = 3)
    private String docNo;

    @ExcelProperty(value = "所属部门", index = 4)
    private String deptName;

    @ExcelProperty(value = "上笔余额", index = 5, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal prevBalance;

    @ExcelProperty(value = "增加金额", index = 6, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal increaseAmount;

    @ExcelProperty(value = "减少金额", index = 7, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal decreaseAmount;

    @ExcelProperty(value = "余额", index = 8, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;

    @ExcelProperty(value = "单据时间", index = 9)
    private LocalDateTime docDate;
}
