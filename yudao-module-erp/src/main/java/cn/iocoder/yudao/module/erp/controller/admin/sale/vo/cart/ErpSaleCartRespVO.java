package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleCartRespVO {

    @ExcelProperty("编号")
    private Long id;
    @ExcelProperty("手推车单号")
    private String no;
    @ExcelProperty("状态")
    private Integer status;
    private Long customerId;
    @ExcelProperty("客户名称")
    private String customerName;
    private Long accountId;
    private Long saleUserId;
    private Long deptId;
    @ExcelProperty("开单时间")
    private LocalDateTime cartTime;
    private Long firstAuditUserId;
    private LocalDateTime firstAuditTime;
    private Long finalAuditUserId;
    private LocalDateTime finalAuditTime;
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
        private BigDecimal totalPrice;
        private BigDecimal taxPercent;
        private BigDecimal taxPrice;
        private BigDecimal stockCount;
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
