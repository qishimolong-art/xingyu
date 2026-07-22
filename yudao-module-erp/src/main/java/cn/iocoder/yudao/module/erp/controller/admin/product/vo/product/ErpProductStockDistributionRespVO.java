package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 配件库存分发 Response VO")
@Data
public class ErpProductStockDistributionRespVO {

    @Schema(description = "配件编号", example = "1024")
    private Long productId;

    @Schema(description = "配件编码", example = "P000001")
    private String productCode;

    @Schema(description = "配件名称", example = "前刹车片")
    private String productName;

    @Schema(description = "已分发仓库编号列表")
    private List<Long> distributedWarehouseIds;

    @Schema(description = "可分发仓库列表")
    private List<WarehouseItem> warehouses;

    @Schema(description = "管理后台 - ERP 配件库存分发仓库项")
    @Data
    public static class WarehouseItem {

        @Schema(description = "仓库编号", example = "1")
        private Long warehouseId;

        @Schema(description = "仓库编码", example = "WH001")
        private String warehouseCode;

        @Schema(description = "仓库名称", example = "总仓")
        private String warehouseName;

        @Schema(description = "部门编号", example = "100")
        private Long deptId;

        @Schema(description = "部门名称", example = "总部")
        private String deptName;

        @Schema(description = "是否已分发")
        private Boolean distributed;

        @Schema(description = "是否默认仓库")
        private Boolean defaultWarehouse;

        @Schema(description = "库存行编号", example = "2048")
        private Long stockId;

        @Schema(description = "当前库存")
        private BigDecimal count;

        @Schema(description = "占用数量")
        private BigDecimal lockCount;

        @Schema(description = "是否允许取消分发")
        private Boolean removable;

        @Schema(description = "不可取消原因")
        private String disabledReason;

    }

}
