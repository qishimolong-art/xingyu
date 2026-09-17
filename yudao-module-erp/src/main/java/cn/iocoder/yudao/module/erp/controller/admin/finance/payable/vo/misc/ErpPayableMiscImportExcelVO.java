package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpPayableMiscImportExcelVO {

    @ExcelRequired
    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelRequired
    @ExcelProperty("账户")
    private String accountName;

    @ExcelRequired
    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("供应商ID")
    private Long supplierId;

    @ExcelProperty("账户ID")
    private Long accountId;

    @ExcelProperty("部门ID")
    private Long deptId;

    @ExcelRequired
    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("附件URL")
    private String fileUrl;

}
