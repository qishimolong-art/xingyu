package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpReceivableMiscImportExcelVO {

    @ExcelRequired
    @ExcelProperty("客户")
    private String customerName;

    @ExcelRequired
    @ExcelProperty("账户")
    private String accountName;

    @ExcelRequired
    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("客户ID")
    private Long customerId;

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
