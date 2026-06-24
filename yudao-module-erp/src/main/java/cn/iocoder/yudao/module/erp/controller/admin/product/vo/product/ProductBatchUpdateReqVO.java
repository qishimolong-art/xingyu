package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 产品批量修改 Request VO")
@Data
public class ProductBatchUpdateReqVO {

    @Schema(description = "产品编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "产品编号列表不能为空")
    private List<Long> ids;

    @Schema(description = "产品分类编号", example = "11161")
    private Long categoryId;

    @Schema(description = "单位编号", example = "8869")
    private Long unitId;

    @Schema(description = "默认仓库编号", example = "1")
    private Long defaultWarehouseId;

    @Schema(description = "产品状态", example = "0")
    private Integer status;

    @Schema(description = "产品备注")
    private String remark;

}
