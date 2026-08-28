package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPayableExpenseExportRespVO {

    @ExcelProperty("单据编号")
    private String no;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("单据日期")
    private LocalDate bizTime;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("结算账户")
    private String accountName;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelProperty("类型")
    private String expenseBizType;

    @ExcelProperty("支出类型")
    private String expenseType;

    @ExcelProperty("总金额")
    private BigDecimal totalAmount;

    @ExcelProperty("开单部门")
    private String deptName;

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty("对象")
    private String party;

    @ExcelProperty("相关业务")
    private String relatedBiz;

    @ExcelProperty("单据类型")
    private String docType;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("项目名称")
    private String itemName;

    @ExcelProperty("金额")
    private BigDecimal itemAmount;

    @ExcelProperty("发票号")
    private String itemInvoiceNo;

    @ExcelProperty("明细对象")
    private String itemParty;

    @ExcelProperty("明细部门")
    private String itemDeptName;

    @ExcelProperty("发生日期")
    private LocalDate itemBizDate;

    @ExcelProperty("明细经手人")
    private String itemHandlerName;

    @ExcelProperty("数量")
    private Integer itemQty;

    @ExcelProperty("费用分类")
    private String itemExpenseCategory;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
