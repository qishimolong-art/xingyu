package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpReceivableOtherExportRespVO {

    @ExcelProperty("单据编号")
    private String no;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("业务日期")
    private LocalDate bizTime;

    @ExcelProperty("客户")
    private String customerName;

    @ExcelProperty("客户联系人")
    private String customerContact;

    @ExcelProperty("客户电话")
    private String customerMobile;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelProperty("已结金额")
    private BigDecimal settledAmount;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("应收金额")
    private BigDecimal receivableAmount;

    @ExcelProperty("调账项目")
    private String project;

    @ExcelProperty("来源类型")
    private String sourceType;

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty("应收类型")
    private String receivableType;

    @ExcelProperty("成本金额")
    private BigDecimal costAmount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("纸质单据")
    private Boolean isPaperNote;

    @ExcelProperty("单据说明")
    private String paperNoteDesc;

    @ExcelProperty("来源单号")
    private String sourceNo;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
