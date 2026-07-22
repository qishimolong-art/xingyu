package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class ErpAccountImportExcelVO {

    @ExcelRequired
    @ExcelProperty("账户名称")
    private String name;

    @ExcelRequired
    @ExcelProperty("账户类型")
    private Integer accountType;

    @ExcelProperty("开户行")
    private String bankName;

    @ExcelProperty("银行账号")
    private String bankAccount;

    @ExcelProperty("账户编码")
    private String no;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("排序")
    private Integer sort;
}
