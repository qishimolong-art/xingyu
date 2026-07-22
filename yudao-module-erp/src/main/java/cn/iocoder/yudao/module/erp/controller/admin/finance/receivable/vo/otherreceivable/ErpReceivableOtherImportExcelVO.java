package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpReceivableOtherImportExcelVO {

    @ExcelRequired
    @ExcelProperty("业务日期")
    private String bizTime;

    @ExcelRequired
    @ExcelProperty("客户ID")
    private Long customerId;

    @ExcelProperty("凭证号")
    private String voucherNo;

    @ExcelProperty("已结金额")
    private BigDecimal settledAmount;

    @ExcelProperty("部门ID")
    private Long deptId;

    @ExcelRequired
    @ExcelProperty("应收金额")
    private BigDecimal receivableAmount;

    @ExcelProperty("调账项目")
    private String project;

    @ExcelProperty("来源类型")
    private String sourceType;

    @ExcelProperty("经手人ID")
    private Long handlerId;

    @ExcelProperty("应收类型")
    private String receivableType;

    @ExcelProperty("成本金额")
    private BigDecimal costAmount;

    @ExcelRequired
    @ExcelProperty("调账原因备注")
    private String remark;

    @ExcelProperty("是否纸质单据")
    private Boolean isPaperNote;

    @ExcelProperty("纸质单据说明")
    private String paperNoteDesc;

    @ExcelProperty("来源单号")
    private String sourceNo;

    @ExcelProperty("附件URL")
    private String fileUrl;
}
