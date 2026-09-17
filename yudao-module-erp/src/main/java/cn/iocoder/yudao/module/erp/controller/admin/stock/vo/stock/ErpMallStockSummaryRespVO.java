package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 商城商品库存汇总 Response VO")
@Data
@Accessors(chain = true)
public class ErpMallStockSummaryRespVO {

    @Schema(description = "商城 SPU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long spuId;

    @Schema(description = "是否已绑定 ERP 产品", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean mapped;

    @Schema(description = "绑定关系数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer mappingCount;

    @Schema(description = "ERP 可用库存汇总", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.00")
    private BigDecimal totalAvailableCount;

    @Schema(description = "ERP 当前库存汇总", requiredMode = Schema.RequiredMode.REQUIRED, example = "18.00")
    private BigDecimal totalCount;

    @Schema(description = "ERP 锁定库存汇总", requiredMode = Schema.RequiredMode.REQUIRED, example = "2.00")
    private BigDecimal totalLockCount;

    @Schema(description = "ERP 占用库存汇总", requiredMode = Schema.RequiredMode.REQUIRED, example = "3.00")
    private BigDecimal totalOccupiedCount;

    @Schema(description = "ERP 待入库存汇总", requiredMode = Schema.RequiredMode.REQUIRED, example = "5.00")
    private BigDecimal totalPendingInCount;

    @Schema(description = "ERP 在途库存汇总", requiredMode = Schema.RequiredMode.REQUIRED, example = "6.00")
    private BigDecimal totalInTransitCount;

    @Schema(description = "按仓库聚合的库存明细")
    private List<WarehouseStock> warehouseStocks;

    @Schema(description = "管理后台 - ERP 商城商品仓库库存 Response VO")
    @Data
    @Accessors(chain = true)
    public static class WarehouseStock {

        @Schema(description = "仓库编号", example = "1")
        private Long warehouseId;

        @Schema(description = "仓库名称", example = "成都总仓")
        private String warehouseName;

        @Schema(description = "所属部门编号", example = "101")
        private Long deptId;

        @Schema(description = "所属部门名称", example = "成都分公司")
        private String deptName;

        @Schema(description = "货架位", example = "A-01")
        private String shelf;

        @Schema(description = "当前库存", example = "18.00")
        private BigDecimal count;

        @Schema(description = "锁定库存", example = "2.00")
        private BigDecimal lockCount;

        @Schema(description = "占用库存", example = "3.00")
        private BigDecimal occupiedCount;

        @Schema(description = "待入库存", example = "5.00")
        private BigDecimal pendingInCount;

        @Schema(description = "在途库存", example = "6.00")
        private BigDecimal inTransitCount;

        @Schema(description = "可用库存", example = "16.00")
        private BigDecimal availableCount;

    }

}
