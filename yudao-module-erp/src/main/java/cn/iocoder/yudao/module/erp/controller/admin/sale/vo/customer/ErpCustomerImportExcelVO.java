package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

/**
 * ERP 客户导入 Excel VO
 */
@Data
public class ErpCustomerImportExcelVO {

    @ExcelRequired
    @ExcelProperty("客户名称")
    private String name;

    @ExcelProperty("客户编码")
    private String code;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("联系电话")
    private String telephone;

    @ExcelProperty("地区编号")
    private Long areaId;

    @ExcelProperty("详细地址")
    private String detailAddress;

    @ExcelProperty("税号")
    private String taxNo;

    @ExcelProperty("开户行")
    private String bankName;

    @ExcelProperty("银行账号")
    private String bankAccount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("排序")
    private Integer sort;

    @ExcelProperty("状态")
    private Integer status;

}
