package cn.iocoder.yudao.module.erp.controller.admin.chain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 连锁开单新增/修改 Request VO")
@Data
public class ErpChainOrderSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "总公司租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "总公司租户ID不能为空")
    private Long hqTenantId;

    @Schema(description = "分公司租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分公司租户ID不能为空")
    private Long branchTenantId;

    @Schema(description = "终端客户ID(无仓分公司)")
    private Long customerId;

    @Schema(description = "总公司出库仓库ID")
    private Long hqWarehouseId;

    @Schema(description = "分公司入库仓库ID(有仓)")
    private Long branchWarehouseId;

    @Schema(description = "分公司类型(1有仓 2无仓)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分公司类型不能为空")
    private Integer branchType;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "连锁开单明细列表")
    @NotEmpty(message = "开单明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "明细编号", example = "1")
        private Long id;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "产品不能为空")
        private Long productId;

        @Schema(description = "产品单位编号")
        private Long productUnitId;

        @Schema(description = "产品单价")
        private BigDecimal productPrice;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "数量不能为空")
        private BigDecimal count;

        @Schema(description = "备注")
        private String remark;

    }

}
