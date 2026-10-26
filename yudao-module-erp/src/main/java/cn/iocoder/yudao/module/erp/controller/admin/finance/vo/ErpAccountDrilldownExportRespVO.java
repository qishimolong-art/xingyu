package cn.iocoder.yudao.module.erp.controller.admin.finance.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpAccountDrilldownExportRespVO {

    @ExcelProperty(value = "单据类型", index = 0)
    private String docType;

    @ExcelProperty(value = "日期", index = 1)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime docDate;

    @ExcelProperty(value = "单号", index = 2)
    private String docNo;

    @ExcelProperty(value = "本笔变动", index = 3, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal changeAmount;

    @ExcelProperty(value = "上笔累计", index = 4, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal prevBalance;

    @ExcelProperty(value = "累计金额", index = 5, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;

    @ExcelProperty(value = "所属部门", index = 6)
    private String deptName;

    @ExcelProperty(value = "审核人/业务员", index = 7)
    private String operatorName;

    @ExcelProperty(value = "备注", index = 8)
    private String remark;

    @ExcelProperty(value = "凭证号", index = 9)
    private String voucherNo;
}
