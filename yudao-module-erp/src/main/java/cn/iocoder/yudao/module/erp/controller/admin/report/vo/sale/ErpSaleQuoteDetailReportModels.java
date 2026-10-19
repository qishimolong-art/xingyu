package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

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

public final class ErpSaleQuoteDetailReportModels {

    private ErpSaleQuoteDetailReportModels() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] quoteDate;
        private String documentNo;
        private String sourceNo;
        private Integer orderStatus;
        private Integer outStatus;
        private Long customerId;
        private Long saleUserId;
        private Long deptId;
        private Long warehouseId;
        private String productKeyword;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getQuoteDateFrom() {
            return quoteDate == null || quoteDate.length == 0 ? null : quoteDate[0];
        }

        public LocalDateTime getQuoteDateTo() {
            return quoteDate == null || quoteDate.length < 2 ? null : quoteDate[1];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long itemId;
        private Long quoteId;
        private Long sourceBusinessId;
        private String sourceDocumentTypeCode;
        private Long productId;
        private Long warehouseId;
        private Long customerAreaId;

        @ExcelProperty(value = "报价日期", index = 0)
        private LocalDateTime quoteDate;
        @ExcelProperty(value = "单号", index = 1)
        private String documentNo;
        @ExcelProperty(value = "来源单号", index = 2)
        private String sourceNo;
        @ExcelProperty(value = "订单状态", index = 3)
        private String orderStatus;
        @ExcelProperty(value = "出库状态", index = 4)
        private String outStatus;
        @ExcelProperty(value = "客户名称", index = 5)
        private String customerName;
        @ExcelProperty(value = "客户编码", index = 6)
        private String customerCode;
        @ExcelProperty(value = "客户区域", index = 7)
        private String customerRegion;
        @ExcelProperty(value = "部门", index = 8)
        private String deptName;
        @ExcelProperty(value = "销售员", index = 9)
        private String saleUserName;
        @ExcelProperty(value = "制单人", index = 10)
        private String creatorName;
        @ExcelProperty(value = "预计交货日期", index = 11)
        private LocalDateTime expectedDeliveryDate;
        @ExcelProperty(value = "价格类型", index = 12)
        private String priceType;
        @ExcelProperty(value = "零件编码", index = 13)
        private String productCode;
        @ExcelProperty(value = "零件名称", index = 14)
        private String productName;
        @ExcelProperty(value = "适用车型", index = 15)
        private String vehicleModel;
        @ExcelProperty(value = "特征码", index = 16)
        private String featureCode;
        @ExcelProperty(value = "图号", index = 17)
        private String drawingNo;
        @ExcelProperty(value = "规格", index = 18)
        private String standard;
        @ExcelProperty(value = "品牌", index = 19)
        private String brand;
        @ExcelProperty(value = "产地", index = 20)
        private String originPlace;
        @ExcelProperty(value = "单位", index = 21)
        private String unitName;
        @ExcelProperty(value = "仓库", index = 22)
        private String warehouseName;
        @ExcelProperty(value = "货架位", index = 23)
        private String warehousePosition;
        @ExcelProperty(value = "是否赠品", index = 24)
        private String giftFlagText;
        @ExcelProperty(value = "数量", index = 25)
        private BigDecimal count;
        @ExcelProperty(value = "单价", index = 26)
        private BigDecimal productPrice;
        @ExcelProperty(value = "金额", index = 27)
        private BigDecimal totalPrice;
        @ExcelProperty(value = "下订时库存数", index = 28)
        private BigDecimal stockCountAtOrder;
        @ExcelProperty(value = "当前库存数", index = 29)
        private BigDecimal stockCount;
        @ExcelProperty(value = "可用库存数", index = 30)
        private BigDecimal availableCount;
        @ExcelProperty(value = "库存上限", index = 31)
        private Integer stockMax;
        @ExcelProperty(value = "库存下限", index = 32)
        private Integer stockMin;
        @ExcelProperty(value = "标准库存", index = 33)
        private Integer stockStandard;
        @ExcelProperty(value = "已转出数", index = 34)
        private BigDecimal convertedCount;
        @ExcelProperty(value = "实际出库数", index = 35)
        private BigDecimal actualOutCount;
        @ExcelProperty(value = "未出数", index = 36)
        private BigDecimal pendingOutCount;
        @ExcelProperty(value = "单据备注", index = 37)
        private String documentRemark;

        private Integer rawStatus;
        private Boolean priceVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal totalPrice;
        private BigDecimal convertedCount;
        private BigDecimal actualOutCount;
        private BigDecimal pendingOutCount;
        private Boolean priceVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class ItemProgress {
        private Long itemId;
        private BigDecimal convertedCount;
        private BigDecimal actualOutCount;
    }
}
