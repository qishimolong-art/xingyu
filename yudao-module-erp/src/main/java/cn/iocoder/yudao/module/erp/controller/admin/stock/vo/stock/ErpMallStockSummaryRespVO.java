package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

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

}
