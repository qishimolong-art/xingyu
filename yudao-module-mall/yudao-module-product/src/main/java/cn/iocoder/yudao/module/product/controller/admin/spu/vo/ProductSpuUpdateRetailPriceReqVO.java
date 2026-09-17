package cn.iocoder.yudao.module.product.controller.admin.spu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 商品 SPU 小程序价格更新 Request VO")
@Data
public class ProductSpuUpdateRetailPriceReqVO {

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商品编号不能为空")
    private Long id;

    @Schema(description = "小程序价格，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1999")
    @NotNull(message = "小程序价格不能为空")
    @Min(value = 0, message = "小程序价格不能小于 0")
    private Integer price;

}
