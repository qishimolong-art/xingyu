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

    @ExcelProperty("单号")
    private String no;

    private Integer status;

    @ExcelProperty("日期")
    private LocalDate bizTime;

    @ExcelProperty("应收金额")
    private BigDecimal receivableAmount;

    @ExcelProperty("增加应收")
    private BigDecimal increaseReceivableAmount;

    @ExcelProperty("已结金额")
    private BigDecimal settledAmount;

    @ExcelProperty("来源类型")
    private String sourceType;

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

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("项目")
    private String project;

    @ExcelProperty("所属部门")
    private String deptName;

}
