package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购单据整单明细匹配 Request VO")
@Data
public class ErpPurchaseItemMatchReqVO {

    @Schema(description = "主单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主单编号不能为空")
    private Long documentId;

    @Schema(description = "页大小", defaultValue = "10")
    @Min(value = 1, message = "页大小不能小于 1")
    @Max(value = 200, message = "页大小不能大于 200")
    private Integer pageSize = 10;

    @Schema(description = "待匹配键，单次最多 200 个", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "待匹配键不能为空")
    @Size(max = 200, message = "单次最多匹配 200 个键")
    private List<Key> keys;

    @Schema(description = "编辑态待删除明细编号")
    private List<Long> excludedItemIds;

    @Data
    public static class Key {
        @Schema(description = "商品编号")
        private Long productId;
        @Schema(description = "仓库编号")
        private Long warehouseId;
        @Schema(description = "赠品标识")
        private Boolean gift;
        @Schema(description = "来源入库明细编号")
        private Long sourceItemId;
    }
}

