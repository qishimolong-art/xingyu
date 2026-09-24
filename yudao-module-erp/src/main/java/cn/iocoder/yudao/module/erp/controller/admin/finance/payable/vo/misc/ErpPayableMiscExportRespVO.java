package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPayableMiscExportRespVO {

    @ExcelProperty("单号")
    private String no;

    @ExcelProperty("日期")
    private LocalDateTime bizTime;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("账户")
    private String accountName;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("原金额")
    private BigDecimal amount;

    @ExcelProperty("已付金额")
    private BigDecimal settledAmount;

    @ExcelProperty("剩余金额")
    private BigDecimal balanceAmount;

    @ExcelProperty("来源类型")
    private String sourceType;

    @ExcelProperty("来源单号")
    private String sourceNo;

    @ExcelProperty("冲减原单号")
    private String sourceMiscNo;

    @ExcelProperty("状态")
    private String statusName;

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

}
