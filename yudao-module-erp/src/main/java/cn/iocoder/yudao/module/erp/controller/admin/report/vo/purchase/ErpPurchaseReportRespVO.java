package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPurchaseReportRespVO {

    private Long supplierId;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("联系电话")
    private String mobile;

    private Long deptId;

    @ExcelProperty("所属部门")
    private String deptName;

    private String purchaser;

    @ExcelProperty("采购员")
    private String purchaserName;

    @ExcelProperty("单据数")
    private Long docCount;

    @ExcelProperty("入库数量")
    private BigDecimal purchaseCount;

    @ExcelProperty("入库金额")
    private BigDecimal purchaseAmount;

    @ExcelProperty("退货数量")
    private BigDecimal returnCount;

    @ExcelProperty("退货金额")
    private BigDecimal returnAmount;

    @ExcelProperty("采购净额")
    private BigDecimal netAmount;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
