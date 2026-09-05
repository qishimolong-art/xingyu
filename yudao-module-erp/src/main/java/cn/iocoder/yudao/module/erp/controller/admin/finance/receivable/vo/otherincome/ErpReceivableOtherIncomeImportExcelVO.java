package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpReceivableOtherIncomeImportExcelVO {

    @ExcelProperty("业务时间")
    private String bizTime;

    @ExcelRequired
    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelRequired
    @ExcelProperty("结算账户ID")
    private Long accountId;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelRequired
    @ExcelProperty("收入类型")
    private String incomeType;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelRequired
    @ExcelProperty("经手人ID")
    private Long handlerId;

    @ExcelProperty("往来单位")
    private String party;

    @ExcelProperty("相关业务")
    private String relatedBiz;

    @ExcelProperty("单据类型")
    private String docType;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("附件URL")
    private String fileUrl;

    @ExcelRequired
    @ExcelProperty("收入项目")
    private String itemName;

    @ExcelRequired
    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("发票号")
    private String invoiceNo;

    @ExcelProperty("明细往来单位")
    private String itemParty;

    @ExcelProperty("明细客户ID")
    private Long itemCustomerId;

    @ExcelProperty("明细部门ID")
    private Long itemDeptId;

    @ExcelProperty("明细业务时间")
    private String itemBizDate;

    @ExcelProperty("明细经手人ID")
    private Long itemHandlerId;

    @ExcelProperty("数量")
    private Integer qty;

    @ExcelProperty("运费类型")
    private String freightType;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
