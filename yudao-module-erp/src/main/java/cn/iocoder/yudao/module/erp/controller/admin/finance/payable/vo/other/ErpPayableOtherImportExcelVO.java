package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpPayableOtherImportExcelVO {

    @ExcelProperty("业务日期")
    private String bizTime;

    @ExcelRequired
    @ExcelProperty("供应商ID")
    private Long supplierId;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelProperty("已结金额")
    private BigDecimal settledAmount;

    @ExcelProperty("部门ID")
    private Long deptId;

    @ExcelRequired
    @ExcelProperty("应付金额")
    private BigDecimal payableAmount;

    @ExcelProperty("调账项目")
    private String project;

    @ExcelProperty("来源类型")
    private String sourceType;

    @ExcelProperty("经手人ID")
    private Long handlerId;

    @ExcelRequired
    @ExcelProperty("调账原因备注")
    private String remark;

    @ExcelProperty("附件URL")
    private String fileUrl;
}
