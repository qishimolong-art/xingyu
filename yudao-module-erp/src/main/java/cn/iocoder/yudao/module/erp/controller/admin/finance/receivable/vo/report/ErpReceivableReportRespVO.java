package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpReceivableReportRespVO {

    private Long customerId;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("联系电话")
    private String mobile;

    private Long deptId;

    @ExcelProperty("所属部门")
    private String deptName;

    private Long handlerId;

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty(value = "其他应收金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal otherReceivableAmount;

    @ExcelProperty(value = "已结金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal settledAmount;

    @ExcelProperty(value = "余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
