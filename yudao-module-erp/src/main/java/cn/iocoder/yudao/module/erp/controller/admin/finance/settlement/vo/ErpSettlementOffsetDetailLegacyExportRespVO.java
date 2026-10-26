package cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 保留未指定 dateBasis 的旧明细导出列序。 */
@ExcelIgnoreUnannotated
@Data
public class ErpSettlementOffsetDetailLegacyExportRespVO {

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
    @ExcelProperty(value = "上笔余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal prevBalance;
    @ExcelProperty(value = "增加金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal increaseAmount;
    @ExcelProperty(value = "减少金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal decreaseAmount;
    @ExcelProperty(value = "余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;
}
