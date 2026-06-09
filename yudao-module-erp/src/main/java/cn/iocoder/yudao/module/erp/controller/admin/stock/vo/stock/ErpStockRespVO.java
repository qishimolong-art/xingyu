package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 库存 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17086")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "19614")
    private Long productId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2802")
    private Long warehouseId;

    @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "21935")
    @ExcelProperty("库存数量")
    private BigDecimal count;

    @Schema(description = "成本均价（移动加权平均），单位：元", example = "12.345678")
    @ExcelProperty("成本均价")
    private BigDecimal costPrice;

    @Schema(description = "成本金额 = 库存 × 成本均价，单位：元", example = "1234.5678")
    @ExcelProperty("成本金额")
    private BigDecimal costAmount;

    // ========== 产品信息 ==========

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "苹果")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "产品分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "水果")
    @ExcelProperty("产品分类")
    private String categoryName;

    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "个")
    @ExcelProperty("单位")
    private String unitName;

    // ========== 仓库信息 ==========

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "所属部门", example = "100")
    @ExcelProperty("所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    @ExcelProperty("所属部门")
    private String deptName;

    // ========== 产品扩展字段 ==========
    @Schema(description = "零件编码")
    @ExcelProperty("编码")
    private String productCode;
    @Schema(description = "图号")
    @ExcelProperty("图号")
    private String drawingNo;
    @Schema(description = "规格")
    @ExcelProperty("规格")
    private String standard;
    @Schema(description = "特征码")
    private String featureCode;
    @Schema(description = "适用车型")
    @ExcelProperty("适用车型")
    private String vehicleModel;
    @Schema(description = "品牌")
    @ExcelProperty("品牌")
    private String brand;
    @Schema(description = "产地")
    @ExcelProperty("产地")
    private String originPlace;
    @Schema(description = "货架")
    @ExcelProperty("货架")
    private String shelf;
    @Schema(description = "产品分类编号")
    private Long categoryId;
    @Schema(description = "最近采购价")
    @ExcelProperty("进价")
    private BigDecimal lastPurchasePrice;
    @Schema(description = "OE 编号")
    private String oeNumber;
    @Schema(description = "厂家编码")
    private String factoryCode;
    @Schema(description = "条形码")
    private String productBarCode;
    @Schema(description = "参考价")
    private BigDecimal referencePrice;
    @Schema(description = "零售价")
    private BigDecimal retailPrice;
    @Schema(description = "备用价1")
    private BigDecimal backupPrice1;
    @Schema(description = "批发价")
    private BigDecimal wholesalePrice;
    @Schema(description = "库存上限")
    private Integer stockMax;
    @Schema(description = "库存下限")
    private Integer stockMin;
    @Schema(description = "标准库存")
    private Integer stockStandard;
    @Schema(description = "包装数")
    private Integer packageQty;
    @Schema(description = "重量（kg）")
    private BigDecimal weight;

    // ========== 动态聚合字段 ==========
    @Schema(description = "占用数（已下销售单未出库数量）")
    @ExcelProperty("占用数")
    private BigDecimal occupiedCount;
    @Schema(description = "未入数（已下采购单未入库数量）")
    @ExcelProperty("未入数")
    private BigDecimal pendingInCount;
    @Schema(description = "在途数（= 未入数）")
    @ExcelProperty("在途数")
    private BigDecimal inTransitCount;

    // ========== 价格体系动态列（priceSystemId 有值时填充） ==========
    @Schema(description = "所选价格体系单价")
    @ExcelProperty("备用价1")
    private BigDecimal currentPrice;
    @Schema(description = "所选价格体系金额 = count × currentPrice")
    @ExcelProperty("备用价1金额")
    private BigDecimal currentPriceAmount;

}
