package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 报价订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSaleQuoteRespVO {

    @Schema(description = "编号")
    @ExcelProperty("编号")
    private Long id;
    @Schema(description = "报价单号")
    @ExcelProperty("报价单号")
    private String no;
    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;
    @Schema(description = "客户编号")
    private Long customerId;
    @Schema(description = "客户名称")
    @ExcelProperty("客户名称")
    private String customerName;
    private Long accountId;
    private Long saleUserId;
    private Long deptId;
    @Schema(description = "报价时间")
    @ExcelProperty("报价时间")
    private LocalDateTime quoteTime;
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;
    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;
    private BigDecimal totalProductPrice;
    private BigDecimal totalTaxPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountPrice;
    private BigDecimal otherPrice;
    private Integer sourceType;
    private Long sourceId;
    private String sourceNo;
    private String fileUrl;
    @ExcelProperty("备注")
    private String remark;
    private String creator;
    private String creatorName;
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    private List<Item> items;
    @ExcelProperty("产品信息")
    private String productNames;

    @Data
    public static class Item {
        private Long id;
        private Long productId;
        private Long productUnitId;
        private Long warehouseId;
        private BigDecimal productPrice;
        private BigDecimal count;
        private BigDecimal convertedCount;
        private BigDecimal totalPrice;
        private BigDecimal taxPercent;
        private BigDecimal taxPrice;
        private String warehousePosition;
        private String drawingNo;
        private String batchNo;
        private String barCode;
        private String brand;
        private String vehicleModel;
        private String originPlace;
        private String standard;
        private String remark;
        private String productName;
        private String productBarCode;
        private String productUnitName;
    }

}
