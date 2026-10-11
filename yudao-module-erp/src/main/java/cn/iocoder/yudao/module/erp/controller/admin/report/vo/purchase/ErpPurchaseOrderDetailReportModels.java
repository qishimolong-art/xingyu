package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

public final class ErpPurchaseOrderDetailReportModels {

    private ErpPurchaseOrderDetailReportModels() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] orderDate;
        private String orderNo;
        private String factoryOrderNo;
        private Long supplierId;
        private Long purchaserId;
        private String productKeyword;
        private Long warehouseId;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getOrderDateFrom() {
            return orderDate == null || orderDate.length == 0 ? null : orderDate[0];
        }

        public LocalDateTime getOrderDateTo() {
            return orderDate == null || orderDate.length < 2 ? null : orderDate[1];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long itemId;
        private Long orderId;
        private Long productId;
        private Long warehouseId;

        @ExcelProperty(value = "订货日期", index = 0)
        private LocalDateTime orderDate;
        @ExcelProperty(value = "厂家单号", index = 1)
        private String factoryOrderNo;
        @ExcelProperty(value = "零件名称", index = 2)
        private String productName;
        @ExcelProperty(value = "供应商", index = 3)
        private String supplierName;
        @ExcelProperty(value = "适用车型", index = 4)
        private String vehicleModel;
        @ExcelProperty(value = "零件编码", index = 5)
        private String productCode;
        @ExcelProperty(value = "仓库", index = 6)
        private String warehouseName;
        @ExcelProperty(value = "货架位", index = 7)
        private String warehousePosition;
        @ExcelProperty(value = "图号", index = 8)
        private String drawingNo;
        @ExcelProperty(value = "规格", index = 9)
        private String standard;
        @ExcelProperty(value = "品牌", index = 10)
        private String brand;
        @ExcelProperty(value = "产地", index = 11)
        private String originPlace;
        @ExcelProperty(value = "采购员", index = 12)
        private String purchaserName;
        @ExcelProperty(value = "制单人", index = 13)
        private String creatorName;
        @ExcelProperty(value = "单号", index = 14)
        private String orderNo;
        @ExcelProperty(value = "数量", index = 15)
        private BigDecimal count;
        @ExcelProperty(value = "单价", index = 16)
        private BigDecimal productPrice;
        @ExcelProperty(value = "金额", index = 17)
        private BigDecimal totalPrice;
        @ExcelProperty(value = "在途数", index = 18)
        private BigDecimal inTransitCount;
        @ExcelProperty(value = "未入数", index = 19)
        private BigDecimal pendingInCount;
        @ExcelProperty(value = "下订时库存数", index = 20)
        private BigDecimal stockCountAtOrder;
        @ExcelProperty(value = "库存上限", index = 21)
        private Integer stockMax;
        @ExcelProperty(value = "库存下限", index = 22)
        private Integer stockMin;
        @ExcelProperty(value = "标准库存", index = 23)
        private Integer stockStandard;
        @ExcelProperty(value = "订单在途数", index = 24)
        private BigDecimal orderInTransitCount;
        @ExcelProperty(value = "预计到货日期", index = 25)
        private LocalDateTime expectedArrivalDate;
        @ExcelProperty(value = "单据备注", index = 26)
        private String documentRemark;
        @ExcelProperty(value = "特征码", index = 27)
        private String featureCode;

        private Boolean priceVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal totalPrice;
        private BigDecimal inTransitCount;
        private BigDecimal pendingInCount;
        private BigDecimal orderInTransitCount;
        private Boolean priceVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class ProductWarehousePair {
        private Long productId;
        private Long warehouseId;
    }
}
