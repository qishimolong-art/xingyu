package cn.iocoder.yudao.module.erp.controller.admin.chain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 连锁开单 Response VO")
@Data
public class ErpChainOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "连锁开单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String no;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "总公司租户ID")
    private Long hqTenantId;

    @Schema(description = "分公司租户ID")
    private Long branchTenantId;

    @Schema(description = "终端客户ID")
    private Long customerId;

    @Schema(description = "总公司出库仓库ID")
    private Long hqWarehouseId;

    @Schema(description = "分公司入库仓库ID")
    private Long branchWarehouseId;

    @Schema(description = "分公司类型(1有仓 2无仓)")
    private Integer branchType;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;

    @Schema(description = "审核时间")
    private LocalDateTime approveTime;

    @Schema(description = "合计数量")
    private BigDecimal totalCount;

    @Schema(description = "合计金额")
    private BigDecimal totalPrice;

    @Schema(description = "总公司销售出库单ID")
    private Long hqSaleOutId;

    @Schema(description = "分公司采购入库单ID")
    private Long branchPurchaseInId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "连锁开单明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "编号")
        private Long id;

        @Schema(description = "产品编号")
        private Long productId;

        @Schema(description = "产品名称")
        private String productName;

        @Schema(description = "产品条码")
        private String productBarCode;

        @Schema(description = "产品单位名称")
        private String productUnitName;

        @Schema(description = "产品单位编号")
        private Long productUnitId;

        @Schema(description = "产品单价")
        private BigDecimal productPrice;

        @Schema(description = "数量")
        private BigDecimal count;

        @Schema(description = "总价")
        private BigDecimal totalPrice;

        @Schema(description = "备注")
        private String remark;

    }

}
