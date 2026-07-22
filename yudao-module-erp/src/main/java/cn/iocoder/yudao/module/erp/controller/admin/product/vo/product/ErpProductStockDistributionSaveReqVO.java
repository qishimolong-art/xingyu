package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 配件库存分发保存 Request VO")
@Data
public class ErpProductStockDistributionSaveReqVO {

    @Schema(description = "配件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "配件编号不能为空")
    private Long productId;

    @Schema(description = "分发仓库编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "分发仓库不能为空")
    private List<Long> warehouseIds;

}
