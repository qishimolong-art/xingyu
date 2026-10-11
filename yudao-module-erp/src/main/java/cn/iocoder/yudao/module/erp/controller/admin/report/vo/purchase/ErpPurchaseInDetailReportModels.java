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
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 配件入库明细查询报表模型。
 */
public final class ErpPurchaseInDetailReportModels {

    private ErpPurchaseInDetailReportModels() {
    }

    public static final String TYPE_IN = "PURCHASE_IN";
    public static final String TYPE_RETURN = "PURCHASE_RETURN";
    public static final String TYPE_ADJUST = "PURCHASE_ADJUST";

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] approveDate;
        private List<String> documentTypes;
        private String documentNo;
        private String sourceNo;
        private String factoryOrderNo;
        private Long supplierId;
        private String purchaserKeyword;
        private String productKeyword;
        private Long warehouseId;
        private String invoiceStatus;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getApproveDateFrom() {
            return approveDate == null || approveDate.length == 0 ? null : approveDate[0];
        }

        public LocalDateTime getApproveDateTo() {
            return approveDate == null || approveDate.length < 2 ? null : approveDate[1];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private String rowKey;
        private Long itemId;
        private Long businessId;
        private Long productId;
        private Long warehouseId;
        private Long supplierId;
        private String documentTypeCode;

        @ExcelProperty(value = "审核日期", index = 0)
        private LocalDateTime approveDate;
        @ExcelProperty(value = "单号", index = 1)
        private String documentNo;
        @ExcelProperty(value = "单据类型", index = 2)
        private String documentType;
        @ExcelProperty(value = "来源单号", index = 3)
        private String sourceNo;
        @ExcelProperty(value = "零件编码", index = 4)
        private String productCode;
        @ExcelProperty(value = "零件名称", index = 5)
        private String productName;
        @ExcelProperty(value = "适用车型", index = 6)
        private String vehicleModel;
        @ExcelProperty(value = "特征码", index = 7)
        private String featureCode;
        @ExcelProperty(value = "数量", index = 8)
        private BigDecimal count;
        @ExcelProperty(value = "单价", index = 9)
        private BigDecimal productPrice;
        @ExcelProperty(value = "含运费单价", index = 10)
        private BigDecimal freightIncludedPrice;
        @ExcelProperty(value = "上次采购价", index = 11)
        private BigDecimal previousPurchasePrice;
        @ExcelProperty(value = "总金额", index = 12)
        private BigDecimal totalAmount;
        @ExcelProperty(value = "税务开票金额", index = 13)
        private BigDecimal invoiceAmount;
        @ExcelProperty(value = "开票状态", index = 14)
        private String invoiceStatus;
        @ExcelProperty(value = "产地", index = 15)
        private String originPlace;
        @ExcelProperty(value = "厂家单号", index = 16)
        private String factoryOrderNo;
        @ExcelProperty(value = "品牌", index = 17)
        private String brand;
        @ExcelProperty(value = "供应商", index = 18)
        private String supplierName;
        @ExcelProperty(value = "供应商编码", index = 19)
        private String supplierCode;
        @ExcelProperty(value = "仓库", index = 20)
        private String warehouseName;
        @ExcelProperty(value = "货架位", index = 21)
        private String warehousePosition;
        @ExcelProperty(value = "业务员", index = 22)
        private String purchaserName;
        @ExcelProperty(value = "规格", index = 23)
        private String standard;
        @ExcelProperty(value = "图号", index = 24)
        private String drawingNo;
        @ExcelProperty(value = "单位", index = 25)
        private String unitName;
        @ExcelProperty(value = "建立人", index = 26)
        private String creatorName;
        @ExcelProperty(value = "备注", index = 27)
        private String itemRemark;
        @ExcelProperty(value = "单据备注", index = 28)
        private String documentRemark;
        @ExcelProperty(value = "厂家编码", index = 29)
        private String factoryCode;
        @ExcelProperty(value = "厂家名称", index = 30)
        private String factoryName;
        @ExcelProperty(value = "厂家型号", index = 31)
        private String factoryModel;
        @ExcelProperty(value = "供应商区域", index = 32)
        private String supplierRegion;
        @ExcelProperty(value = "运费类型1", index = 33)
        private String freightType1;
        @ExcelProperty(value = "运费1", index = 34)
        private BigDecimal freight1;

        private Boolean priceVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal totalAmount;
        private BigDecimal invoiceAmount;
        private BigDecimal freightAmount;
        private Boolean priceVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class PriceEvent {
        private Long productId;
        private LocalDateTime eventTime;
        private String documentTypeCode;
        private Long itemId;
        private BigDecimal price;
    }
}
