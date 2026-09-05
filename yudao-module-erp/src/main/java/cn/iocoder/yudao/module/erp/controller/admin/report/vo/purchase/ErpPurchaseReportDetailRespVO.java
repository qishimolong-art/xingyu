package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPurchaseReportDetailRespVO {

    private String rowKey;
    private Long supplierId;
    private String supplierName;
    private String contact;
    private String mobile;
    private Long deptId;
    private String deptName;
    private String purchaser;
    private String purchaserName;

    @ExcelProperty("单据类型")
    private String docType;

    private Integer bizType;
    private Long bizId;

    @ExcelProperty("单据日期")
    private LocalDateTime docDate;

    @ExcelProperty("单据编号")
    private String docNo;

    @ExcelProperty("业务数量")
    private BigDecimal bizCount;

    @ExcelProperty("入库金额")
    private BigDecimal purchaseAmount;

    @ExcelProperty("退货金额")
    private BigDecimal returnAmount;

    @ExcelProperty("采购净额")
    private BigDecimal netAmount;
}
