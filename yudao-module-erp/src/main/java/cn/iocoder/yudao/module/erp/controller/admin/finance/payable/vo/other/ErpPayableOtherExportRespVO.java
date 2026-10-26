package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPayableOtherExportRespVO {

    @ExcelProperty("单据编号")
    private String no;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("开单日期")
    private LocalDate bizTime;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("供应商联系人")
    private String supplierContact;

    @ExcelProperty("供应商电话")
    private String supplierMobile;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelProperty(value = "已结金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal settledAmount;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty(value = "应付金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal payableAmount;

    @ExcelProperty(value = "未付金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal outstandingAmount;

    @ExcelProperty("调账项目")
    private String project;

    @ExcelProperty("来源类型")
    private String sourceType;

    @ExcelProperty("来源单号")
    private String sourceNo;

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("附件")
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
}
