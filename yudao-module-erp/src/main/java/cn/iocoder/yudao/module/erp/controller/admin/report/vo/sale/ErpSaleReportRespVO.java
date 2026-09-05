package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpSaleReportRespVO {

    private Long customerId;

    @ExcelProperty("客户")
    private String customerName;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("联系电话")
    private String mobile;

    private Long deptId;

    @ExcelProperty("所属部门")
    private String deptName;

    private Long saleUserId;

    @ExcelProperty("销售员")
    private String saleUserName;

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
