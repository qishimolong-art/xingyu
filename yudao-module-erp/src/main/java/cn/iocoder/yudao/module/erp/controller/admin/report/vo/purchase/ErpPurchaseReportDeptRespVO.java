package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPurchaseReportDeptRespVO {

    private Long deptId;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("供应商数")
    private Long supplierCount;

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
