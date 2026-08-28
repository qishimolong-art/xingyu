package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购调价单新增/修改 Request VO")
@Data
public class ErpPurchasePriceAdjustSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "调价日期")
    private LocalDateTime adjustTime;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "调价人（系统用户 ID）", example = "1")
    private Long adjuster;

    @Schema(description = "调价类型：10=按入库单调价 20=添加明细", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "调价类型不能为空")
    private Integer adjustType;

    @Schema(description = "备注", example = "单价填错纠正")
    private String remark;

    @Schema(description = "调价明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "调价明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "调价项编号", example = "1")
        private Long id;

        @Schema(description = "采购入库单编号", example = "1")
        private Long inId;

        @Schema(description = "采购入库单号", example = "CGRK20260508000001")
        private String inNo;

        @Schema(description = "采购入库项编号", example = "1")
        private Long inItemId;

        @Schema(description = "批次号")
        private String batchNo;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "产品不能为空")
        private Long productId;

        @Schema(description = "仓库编号", example = "1")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "调价前单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.5")
        @NotNull(message = "调价前单价不能为空")
        private BigDecimal oldPrice;

        @Schema(description = "调价后单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "13.5")
        @NotNull(message = "调价后单价不能为空")
        private BigDecimal newPrice;

        @Schema(description = "入库数量快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
        @NotNull(message = "入库数量不能为空")
        private BigDecimal count;

        @Schema(description = "调价比率（按入库单方式填；添加明细方式为空）", example = "1.08")
        private BigDecimal adjustRatio;

        // ========== 产品冗余字段 ==========
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
