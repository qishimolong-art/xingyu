package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 产品新增/修改 Request VO")
@Data
public class ProductSaveReqVO {

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "15672")
    private Long id;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "产品名称不能为空")
    private String name;

    @Schema(description = "配件编码", example = "P000001")
    private String code;

    @Schema(description = "所属部门编号", example = "100")
    private Long deptId;

    @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "X110")
    @NotEmpty(message = "产品条码不能为空")
    private String barCode;

    @Schema(description = "产品分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11161")
    @NotNull(message = "产品分类编号不能为空")
    private Long categoryId;

    @Schema(description = "单位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8869")
    @NotNull(message = "单位编号不能为空")
    private Long unitId;

    @Schema(description = "产品状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "产品状态不能为空")
    private Integer status;

    @Schema(description = "产品规格", example = "红色")
    private String standard;

    @Schema(description = "产品备注", example = "你猜")
    private String remark;

    @Schema(description = "保质期天数", example = "10")
    private Integer expiryDay;

    @Schema(description = "基础重量（kg）", example = "1.00")
    private BigDecimal weight;

    @Schema(description = "采购价格，单位：元", example = "10.30")
    private BigDecimal purchasePrice;

    @Schema(description = "销售价格，单位：元", example = "74.32")
    private BigDecimal salePrice;

    @Schema(description = "最低价格，单位：元", example = "161.87")
    private BigDecimal minPrice;

    // ========== 配件信息管理扩展字段 ==========
    @Schema(description = "默认仓库编号", example = "1")
    private Long defaultWarehouseId;

    @Schema(description = "适用车型", example = "宝马 X5")
    private String vehicleModel;

    @Schema(description = "厂家编码", example = "FCT-001")
    private String factoryCode;

    @Schema(description = "参考价", example = "100.00")
    private BigDecimal referencePrice;

    @Schema(description = "零售价", example = "120.00")
    private BigDecimal retailPrice;

    @Schema(description = "毛利率（百分比整数）", example = "20")
    private Integer grossProfitRate;

    @Schema(description = "备用价1", example = "95.00")
    private BigDecimal backupPrice1;

    @Schema(description = "批发价", example = "80.00")
    private BigDecimal wholesalePrice;

    @Schema(description = "库存上限", example = "1000")
    private Integer stockMax;

    @Schema(description = "库存下限", example = "10")
    private Integer stockMin;

    @Schema(description = "标准库存", example = "200")
    private Integer stockStandard;

    @Schema(description = "包装数", example = "1")
    private Integer packageQty;

    @Schema(description = "主图 URL", example = "https://xxx/img.jpg")
    private String mainImage;

    @Schema(description = "配件详情页 Markdown", example = "# 商品介绍 ...")
    private String detailContent;

    @Schema(description = "通用件列表")
    @Valid
    private List<Universal> universals;

    @Schema(description = "通用件子项")
    @Data
    public static class Universal {

        @Schema(description = "通用件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "P000002")
        @NotEmpty(message = "通用件编码不能为空")
        private String universalCode;

        @Schema(description = "通用件名称", example = "通用刹车片")
        private String universalName;

        @Schema(description = "适用车型", example = "奔驰 GLC")
        private String universalVehicle;

    }

}
