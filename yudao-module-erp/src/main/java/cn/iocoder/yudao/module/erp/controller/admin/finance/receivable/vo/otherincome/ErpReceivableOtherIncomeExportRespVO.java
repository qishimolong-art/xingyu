package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpReceivableOtherIncomeExportRespVO {

    @ExcelProperty("单据编号")
    private String no;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("业务时间")
    private LocalDateTime bizTime;

    @ExcelProperty("收款账户")
    private String accountName;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelProperty("收入类型")
    private String incomeType;

    @ExcelProperty(value = "收入合计", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalAmount;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty("往来单位")
    private String party;

    @ExcelProperty("关联业务")
    private String relatedBiz;

    @ExcelProperty("单据类型")
    private String docType;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("主表附件")
    private String fileUrl;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("审核人")
    private String auditorName;

    @ExcelProperty("审核时间")
    private LocalDateTime auditTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("项目名称")
    private String itemName;

    @ExcelProperty(value = "收入金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemAmount;

    @ExcelProperty("发票号")
    private String itemInvoiceNo;

    @ExcelProperty("项目往来单位")
    private String itemParty;

    @ExcelProperty("明细客户")
    private String itemCustomerName;

    @ExcelProperty("项目部门")
    private String itemDeptName;

    @ExcelProperty("业务日期")
    private LocalDateTime itemBizDate;

    @ExcelProperty("项目经手人")
    private String itemHandlerName;

    @ExcelProperty("数量")
    private Integer itemQty;

    @ExcelProperty("运费类型")
    private String itemFreightType;

    @ExcelProperty("明细备注")
    private String itemRemark;

    @ExcelProperty("明细附件")
    private String itemFileUrl;
}
