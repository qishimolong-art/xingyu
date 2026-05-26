package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleReturnExportRespVO {

    @ExcelProperty("退货单号")
    private String no;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("退货时间")
    private LocalDateTime returnTime;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("合计数量")
    private BigDecimal totalCount;

    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("退货数量")
    private BigDecimal itemCount;

    @ExcelProperty("退货单价")
    private BigDecimal productPrice;

    @ExcelProperty("退货原因")
    private String returnReason;

    @ExcelProperty("仓位")
    private String warehousePosition;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
