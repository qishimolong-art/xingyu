package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpReceivableOtherExportRespVO {

    @ExcelProperty("单号")
    private String no;

    private Integer status;

    @ExcelProperty("日期")
    private LocalDate bizTime;

    @ExcelProperty(value = "应收金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal receivableAmount;

    @ExcelProperty(value = "增加应收", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal increaseReceivableAmount;

    @ExcelProperty(value = "已结金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal settledAmount;

    @ExcelProperty("来源类型")
    private String sourceType;

    @ExcelProperty("应收类型")
    private String receivableType;

    @ExcelProperty(value = "成本金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal costAmount;

    @ExcelProperty("来源单号")
    private String sourceNo;

    @ExcelProperty("纸质欠条")
    private Boolean isPaperNote;

    @ExcelProperty("欠条说明")
    private String paperNoteDesc;

    @ExcelProperty("附件")
    private String fileUrl;

    @ExcelProperty("状态")
    private String statusName;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("业务员")
    private String saleUserName;

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

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("项目")
    private String project;

    @ExcelProperty("所属部门")
    private String deptName;

}
