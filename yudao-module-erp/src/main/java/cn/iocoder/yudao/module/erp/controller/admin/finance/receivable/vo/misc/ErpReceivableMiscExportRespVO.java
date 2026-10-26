package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpReceivableMiscExportRespVO {

    @ExcelProperty("单号")
    private String no;

    @ExcelProperty("日期")
    private LocalDateTime bizTime;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("账户")
    private String accountName;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty(value = "原金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal amount;

    @ExcelProperty(value = "已收金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal settledAmount;

    @ExcelProperty(value = "剩余金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balanceAmount;

    @ExcelProperty(value = "待审批占用", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal pendingTransferAmount;

    @ExcelProperty(value = "剩余可转金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal transferAvailableAmount;

    @ExcelProperty("系统冲减")
    private Boolean generatedOffset;

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

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

}
