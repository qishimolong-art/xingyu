package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpFinanceTransferImportExcelVO {

    @ExcelRequired
    @ExcelProperty("转账时间")
    private String transferTime;

    @ExcelRequired
    @ExcelProperty("转出账户ID")
    private Long outAccountId;

    @ExcelRequired
    @ExcelProperty("转入账户ID")
    private Long inAccountId;

    @ExcelRequired
    @ExcelProperty("转账金额")
    private BigDecimal transferPrice;

    @ExcelRequired
    @ExcelProperty("汇率")
    private BigDecimal exchangeRate;

    @ExcelProperty("手续费")
    private BigDecimal feePrice;

    @ExcelProperty("费用项目")
    private String feeExpenseCategory;

    @ExcelProperty("财务人员ID")
    private Long financeUserId;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("附件URL")
    private String fileUrl;
}
