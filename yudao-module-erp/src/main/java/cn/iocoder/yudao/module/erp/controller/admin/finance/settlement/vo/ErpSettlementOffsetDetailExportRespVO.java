package cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpSettlementOffsetDetailExportRespVO {

    @ExcelProperty("往来类型")
    private String direction;

    @ExcelProperty("单据类型")
    private String docType;

    @ExcelProperty("单据日期")
    private LocalDateTime docDate;

    @ExcelProperty("单据编号")
    private String docNo;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("上笔余额")
    private BigDecimal prevBalance;

    @ExcelProperty("增加金额")
    private BigDecimal increaseAmount;

    @ExcelProperty("减少金额")
    private BigDecimal decreaseAmount;

    @ExcelProperty("余额")
    private BigDecimal balance;
}
