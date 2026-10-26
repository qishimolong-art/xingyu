package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
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

    @ExcelProperty(value = "铺底金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal baseAmount;

    @ExcelProperty(value = "信用额度", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal creditLimit;

    @ExcelProperty("信用期限（天）")
    private Integer creditTermDays;

    @ExcelProperty(value = "销售出库", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal saleOutAmount;

    @ExcelProperty(value = "销售退货", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal saleReturnAmount;

    @ExcelProperty(value = "调价金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal priceAdjustAmount;

    @ExcelProperty(value = "其他应收", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal miscReceivableAmount;

    @ExcelProperty(value = "已收款", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal receiptAmount;

    @ExcelProperty(value = "已核销", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal writeOffAmount;

    @ExcelProperty(value = "应收余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal receivableBalance;

    @ExcelProperty(value = "预收余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal preAdvanceAmount;

    @ExcelProperty(value = "授信余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal creditBalance;

    @ExcelProperty(value = "此前应收", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal openingReceivableBalance;

    @ExcelProperty(value = "应收合计", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalReceivable;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
