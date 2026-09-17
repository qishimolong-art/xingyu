package cn.iocoder.yudao.module.erp.controller.app.stock.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "用户 App - ERP 商城商品库存选项分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppErpMallStockOptionPageReqVO extends PageParam {

    @Schema(description = "商城 SPU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "商城 SPU 编号不能为空")
    private Long spuId;

    @Schema(description = "商城 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "商城 SKU 编号不能为空")
    private Long skuId;

    @Schema(description = "用户经度（GCJ-02）", example = "104.0668")
    private BigDecimal userLongitude;

    @Schema(description = "用户纬度（GCJ-02）", example = "30.5728")
    private BigDecimal userLatitude;

}
