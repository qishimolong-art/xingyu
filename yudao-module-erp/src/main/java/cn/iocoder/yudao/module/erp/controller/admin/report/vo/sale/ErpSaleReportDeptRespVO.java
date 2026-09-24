package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpSaleReportDeptRespVO {

    private Long deptId;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("客户数")
    private Long customerCount;

    @ExcelProperty("单据数")
    private Long docCount;

    @ExcelProperty("出库数量")
    private BigDecimal saleCount;

    @ExcelProperty("出库金额")
    private BigDecimal saleAmount;

    @ExcelProperty("退货数量")
    private BigDecimal returnCount;

    @ExcelProperty("退货金额")
    private BigDecimal returnAmount;

    @ExcelProperty("销售净额")
    private BigDecimal netAmount;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
