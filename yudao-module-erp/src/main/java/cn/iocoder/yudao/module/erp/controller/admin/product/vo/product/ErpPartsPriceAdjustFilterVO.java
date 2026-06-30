package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "配件价格调整 - 筛选条件 VO")
@Data
public class ErpPartsPriceAdjustFilterVO {

    @Schema(description = "配件编码")
    private String code;

    @Schema(description = "配件名称")
    private String name;

    @Schema(description = "商品分类编号")
    private Long categoryId;

    @Schema(description = "商品分类精确匹配", example = "false")
    private Boolean categoryExact;

    @Schema(description = "适用车型")
    private String vehicleModel;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "产地")
    private String originPlace;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "仓库编号")
    private Long warehouseId;

}
