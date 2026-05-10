package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购调价单 Response VO")
@Data
public class ErpPurchasePriceAdjustRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "调价单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String no;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "日期")
    private LocalDateTime adjustDate;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "部门")
    private String dept;

    @Schema(description = "调价人")
    private String adjustUser;

    @Schema(description = "调价类型")
    private String adjustType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "调价总金额")
    private BigDecimal totalAdjustPrice;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "调价明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "编号")
        private Long id;

        @Schema(description = "采购单号")
        private String purchaseInNo;

        @Schema(description = "配件编码")
        private String partCode;

        @Schema(description = "配件名称")
        private String partName;

        @Schema(description = "车型")
        private String vehicleModel;

        @Schema(description = "规格")
        private String standard;

        @Schema(description = "特征码")
        private String featureCode;

        @Schema(description = "产地")
        private String originPlace;

        @Schema(description = "品牌")
        private String brand;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "图号")
        private String drawingNo;

        @Schema(description = "入库数")
        private BigDecimal inCount;

        @Schema(description = "进价(原价)")
        private BigDecimal oldPrice;

        @Schema(description = "调后价")
        private BigDecimal newPrice;

        @Schema(description = "调价金额")
        private BigDecimal adjustPrice;

        @Schema(description = "货架")
        private String shelf;

        @Schema(description = "产品ID")
        private Long productId;

        @Schema(description = "关联采购入库项ID")
        private Long purchaseInItemId;

    }

}
