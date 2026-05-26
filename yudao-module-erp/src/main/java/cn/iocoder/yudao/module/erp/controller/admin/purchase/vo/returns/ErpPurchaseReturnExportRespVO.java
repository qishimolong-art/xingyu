package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseReturnExportRespVO {

    @ExcelProperty("退货单号")
    private String no;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("退货时间")
    private LocalDateTime returnTime;

    @ExcelProperty("退货模式")
    private Integer returnMode;

    @ExcelProperty("单据状态")
    private Integer status;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("总数量")
    private BigDecimal totalCount;

    @ExcelProperty("总金额")
    private BigDecimal totalPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("原入库单")
    private String sourceInNo;

    @ExcelProperty("退货数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
