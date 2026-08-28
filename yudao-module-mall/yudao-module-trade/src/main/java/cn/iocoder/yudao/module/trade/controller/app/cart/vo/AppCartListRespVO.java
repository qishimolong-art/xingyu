package cn.iocoder.yudao.module.trade.controller.app.cart.vo;

import cn.iocoder.yudao.module.trade.controller.app.base.sku.AppProductSkuBaseRespVO;
import cn.iocoder.yudao.module.trade.controller.app.base.spu.AppProductSpuBaseRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "用户 App - 用户的购物列表 Response VO")
@Data
public class AppCartListRespVO {

    @Schema(description = "是否已授权 ERP 客户", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean authorized;

    @Schema(description = "是否允许查看价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean priceVisible;

    @Schema(description = "是否允许下订", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean orderEnabled;

    /**
     * 有效的购物项数组
     */
    private List<Cart> validList;

    /**
     * 无效的购物项数组
     */
    private List<Cart> invalidList;

    @Schema(description = "购物项")
    @Data
    public static class Cart {

        @Schema(description = "购物项的编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long id;

        @Schema(description = "ERP 客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        private Long customerId;

        @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long deptId;

        @Schema(description = "商品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer count;

        @Schema(description = "是否选中", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        private Boolean selected;

        @Schema(description = "ERP 库存记录编号", example = "2048")
        private Long stockId;

        @Schema(description = "ERP 产品编号", example = "2048")
        private Long erpProductId;

        @Schema(description = "ERP 仓库编号", example = "1")
        private Long warehouseId;

        @Schema(description = "ERP 仓库名称", example = "主仓")
        private String warehouseName;

        @Schema(description = "ERP 库存是否可购买", example = "true")
        private Boolean stockAvailable;

        @Schema(description = "ERP 可用库存")
        private BigDecimal stockAvailableCount;

        @Schema(description = "ERP 库存状态文案", example = "现货")
        private String stockStatusText;

        /**
         * 商品 SPU
         */
        private AppProductSpuBaseRespVO spu;
        /**
         * 商品 SKU
         */
        private AppProductSkuBaseRespVO sku;

    }

}
