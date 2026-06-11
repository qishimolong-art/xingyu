package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售手推车提交 Response VO")
@Data
public class ErpSaleCartSubmitRespVO {

    @Schema(description = "销售手推车编号", example = "1")
    private Long id;

    @Schema(description = "销售手推车单号", example = "XSST202606100001")
    private String no;

    @Schema(description = "提交后的状态", example = "20")
    private Integer status;

    @Schema(description = "是否存在库存不足")
    private Boolean stockInsufficient = false;

    @Schema(description = "库存短缺明细")
    private List<ShortageItem> shortageItems = new ArrayList<>();

    @Data
    public static class ShortageItem {

        @Schema(description = "产品编号", example = "100")
        private Long productId;

        @Schema(description = "产品编码", example = "P0001")
        private String productCode;

        @Schema(description = "产品名称", example = "刹车片")
        private String productName;

        @Schema(description = "目标仓库编号", example = "10")
        private Long warehouseId;

        @Schema(description = "目标仓库名称", example = "销售仓")
        private String warehouseName;

        @Schema(description = "需求数量", example = "10.00")
        private BigDecimal requiredCount;

        @Schema(description = "当前库存", example = "3.00")
        private BigDecimal stockCount;

        @Schema(description = "短缺数量", example = "7.00")
        private BigDecimal shortageCount;
    }

}
