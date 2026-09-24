package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpReceivableAccountExportRespVO {

    @ExcelProperty("客户")
    private String customerName;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("业务员")
    private String saleUserName;

    @ExcelProperty("销售出库")
    private BigDecimal saleOutAmount;

    @ExcelProperty("销售退货")
    private BigDecimal saleReturnAmount;

    @ExcelProperty("调价金额")
    private BigDecimal priceAdjustAmount;

    @ExcelProperty("其他应收")
    private BigDecimal miscReceivableAmount;

    @ExcelProperty("已收款")
    private BigDecimal receiptAmount;

    @ExcelProperty("已核销")
    private BigDecimal writeOffAmount;

    @ExcelProperty("应收余额")
    private BigDecimal receivableBalance;

    @ExcelProperty("预收余额")
    private BigDecimal preAdvanceAmount;

    @ExcelProperty("授信余额")
    private BigDecimal creditBalance;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
