package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpSaleReportExportRespVO {

    @ExcelProperty("客户")
    private String customerName;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("联系电话")
    private String mobile;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("销售员")
    private String saleUserName;

    @ExcelProperty("单据数")
    private Long docCount;

    @ExcelProperty("出库数量")
    @cn.idev.excel.annotation.format.NumberFormat("0.######")
    private BigDecimal saleCount;

    @ExcelProperty(value = "出库金额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String saleAmount;

    @ExcelProperty("退货数量")
    @cn.idev.excel.annotation.format.NumberFormat("0.######")
    private BigDecimal returnCount;

    @ExcelProperty(value = "退货金额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String returnAmount;

    @ExcelProperty(value = "销售净额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String netAmount;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
