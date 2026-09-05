package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpSaleReportDetailRespVO {

    private String rowKey;
    private Long customerId;
    private String customerName;
    private String contact;
    private String mobile;
    private Long deptId;
    private String deptName;
    private Long saleUserId;
    private String saleUserName;

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

    @ExcelProperty("出库金额")
    private BigDecimal saleAmount;

    @ExcelProperty("退货金额")
    private BigDecimal returnAmount;

    @ExcelProperty("销售净额")
    private BigDecimal netAmount;
}
