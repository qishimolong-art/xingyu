package cn.iocoder.yudao.module.erp.controller.admin.product.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 商品分类列表 Request VO")
@Data
public class ErpProductCategoryListReqVO {

    @Schema(description = "分类名称", example = "芋艿")
    private String name;

    @Schema(description = "开启状态", example = "1")
    private Integer status;

    @Schema(description = "排序字段", example = "sort")
    private String orderField;

    @Schema(description = "排序方向", example = "asc")
    private String orderDirection;

}
