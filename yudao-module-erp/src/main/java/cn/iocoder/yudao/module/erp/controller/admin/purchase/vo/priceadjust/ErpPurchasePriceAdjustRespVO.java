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

    @Schema(description = "调价单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CGTJ2026050801")
    private String no;

    @Schema(description = "状态：10=待审批 20=已通过 30=已拒绝", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "调价日期")
    private LocalDateTime adjustTime;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "调价人（系统用户 ID）")
    private Long adjuster;

    @Schema(description = "调价人名称")
    private String adjusterName;

    @Schema(description = "调价类型：10=按入库单调价 20=添加明细")
    private Integer adjustType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "调价总金额（可正可负）")
    private BigDecimal totalAdjustPrice;

    @Schema(description = "原单金额")
    private BigDecimal originalTotalPrice;

    @Schema(description = "调后金额")
    private BigDecimal adjustedTotalPrice;

    @Schema(description = "已结算金额")
    private BigDecimal paymentPrice;

    @Schema(description = "审批通过时间")
    private LocalDateTime approveTime;

    @Schema(description = "审核人名称")
    private String approverName;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "修改人")
    private String updater;

    @Schema(description = "修改人名称")
    private String updaterName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "调价明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "编号")
        private Long id;

        @Schema(description = "调价单编号")
        private Long adjustId;

        @Schema(description = "采购入库单编号")
        private Long inId;

        @Schema(description = "采购入库单号")
        private String inNo;

        @Schema(description = "采购入库项编号")
        private Long inItemId;

        @Schema(description = "批次号")
        private String batchNo;

        @Schema(description = "产品编号")
        private Long productId;

        @Schema(description = "仓库编号")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "调价前单价")
        private BigDecimal oldPrice;

        @Schema(description = "调价后单价")
        private BigDecimal newPrice;

        @Schema(description = "入库数量快照")
        private BigDecimal count;

        @Schema(description = "调价比率")
        private BigDecimal adjustRatio;

        @Schema(description = "调价金额")
        private BigDecimal adjustPrice;

        @Schema(description = "配件编码")
        private String productCode;

        @Schema(description = "配件名称")
        private String productName;

        @Schema(description = "单位名称")
        private String productUnitName;

        @Schema(description = "产品重量")
        private BigDecimal weight;

        @Schema(description = "包装数")
        private Integer packageQty;

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

        @Schema(description = "图号")
        private String drawingNo;

        @Schema(description = "货架位")
        private String warehousePosition;

    }

}
