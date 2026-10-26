package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPurchaseReportProductExportRespVO {

    @ExcelProperty("配件编码")
    private String productCode;

    @ExcelProperty("配件名称")
    private String productName;

    @ExcelProperty("条形码")
    private String barCode;

    @ExcelProperty("单位")
    private String unitName;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("适用车型")
    private String vehicleModel;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("供应商数")
    private Long supplierCount;

    @ExcelProperty("单据数")
    private Long docCount;

    @ExcelProperty("入库数量")
    @cn.idev.excel.annotation.format.NumberFormat("0.######")
    private BigDecimal purchaseCount;

    @ExcelProperty(value = "入库金额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String purchaseAmount;

    @ExcelProperty("退货数量")
    @cn.idev.excel.annotation.format.NumberFormat("0.######")
    private BigDecimal returnCount;

    @ExcelProperty(value = "退货金额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String returnAmount;

    @ExcelProperty(value = "采购净额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String netAmount;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
