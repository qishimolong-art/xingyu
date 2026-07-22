package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP 产品 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpProductRespVO {

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "15672")
    @ExcelProperty("产品编号")
    private Long id;

    @Schema(description = "配件编码", example = "P000001")
    @ExcelProperty("配件编码")
    private String code;

    @Schema(description = "所属部门编号", example = "100")
    private Long deptId;

    @Schema(description = "所属部门编号列表", example = "[100, 101]")
    private List<Long> deptIds;

    @Schema(description = "所属部门", example = "销售一部")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "所属部门名称列表", example = "销售部、采购部")
    private String deptNames;

    @Schema(description = "审计展示用开放部门名称列表", example = "总公司 / 销售部、总公司 / 采购部")
    private String openDeptNames;

    @Schema(description = "创建时所在部门编号", example = "100")
    private Long createDeptId;

    @Schema(description = "创建时所在部门", example = "总公司 / 销售部")
    private String createDeptName;

    @Schema(description = "产品编码", example = "P000001")
    @ExcelProperty("产品编码")
    private String productCode;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("产品名称")
    private String name;

    @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "X110")
    @ExcelProperty("产品条码")
    private String barCode;

    @Schema(description = "商品分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11161")
    private Long categoryId;
    @Schema(description = "商品分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "水果")
    @ExcelProperty("商品分类")
    private String categoryName;

    @Schema(description = "开启批次号", example = "false")
    @ExcelProperty("开启批次号")
    private Boolean batchNoEnabled;

    @Schema(description = "单位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8869")
    private Long unitId;
    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "个")
    @ExcelProperty("单位")
    private String unitName;

    @Schema(description = "产品状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("产品状态")
    private Integer status;

    @Schema(description = "产品规格", example = "红色")
    @ExcelProperty("产品规格")
    private String standard;

    @Schema(description = "产品备注", example = "你猜")
    @ExcelProperty("产品备注")
    private String remark;

    @Schema(description = "保质期天数", example = "10")
    @ExcelProperty("保质期天数")
    private Integer expiryDay;

    @Schema(description = "基础重量（kg）", example = "1.00")
    @ExcelProperty("基础重量（kg）")
    private BigDecimal weight;

    @Schema(description = "采购价格，单位：元", example = "10.30")
    @ExcelProperty("采购价格，单位：元")
    private BigDecimal purchasePrice;

    @Schema(description = "销售价格，单位：元", example = "74.32")
    @ExcelProperty("销售价格，单位：元")
    private BigDecimal salePrice;

    @Schema(description = "最低价格，单位：元", example = "161.87")
    @ExcelProperty("最低价格，单位：元")
    private BigDecimal minPrice;

    // ========== 配件信息管理扩展字段 ==========
    @Schema(description = "默认仓库编号", example = "1")
    private Long defaultWarehouseId;
    @Schema(description = "默认仓库名称", example = "主仓")
    private String defaultWarehouseName;

    @Schema(description = "适用车型", example = "宝马 X5")
    @ExcelProperty("适用车型")
    private String vehicleModel;

    @Schema(description = "厂家编码", example = "FCT-001")
    @ExcelProperty("厂家编码")
    private String factoryCode;

    // ========== 汽配扩展字段（与 DO 对齐） ==========
    @Schema(description = "品牌", example = "博世")
    private String brand;
    @Schema(description = "OE 编号", example = "OE-001")
    private String oeNumber;
    @Schema(description = "产地", example = "上海")
    private String originPlace;
    @Schema(description = "特征码", example = "FC-001")
    private String featureCode;
    @Schema(description = "图号", example = "DWG-001")
    private String drawingNo;
    @Schema(description = "货架位置", example = "A-01-01")
    private String shelf;

    @Schema(description = "参考价", example = "100.00")
    private BigDecimal referencePrice;
    @Schema(description = "零售价", example = "120.00")
    private BigDecimal retailPrice;
    @Schema(description = "最后一次采购入库价（只读）", example = "90.00")
    private BigDecimal lastPurchasePrice;
    @Schema(description = "毛利率（百分比整数）", example = "20")
    private Integer grossProfitRate;
    @Schema(description = "备用价1", example = "95.00")
    private BigDecimal backupPrice1;
    @Schema(description = "批发价", example = "80.00")
    private BigDecimal wholesalePrice;
    @Schema(description = "股份价", example = "88.00")
    @ExcelProperty("股份价")
    private BigDecimal sharePrice;

    @Schema(description = "库存上限", example = "1000")
    private Integer stockMax;
    @Schema(description = "库存下限", example = "10")
    private Integer stockMin;
    @Schema(description = "标准库存", example = "200")
    private Integer stockStandard;
    @Schema(description = "包装数", example = "1")
    private Integer packageQty;

    @Schema(description = "主图 URL")
    private String mainImage;
    @Schema(description = "配件详情页 Markdown")
    private String detailContent;

    @Schema(description = "是否已合并", example = "false")
    private Boolean mergedFlag;
    @Schema(description = "合并目标配件编号")
    private Long mergedTargetId;

    // ========== 运行时计算字段 ==========
    @Schema(description = "当前库存（实时聚合）", example = "120")
    @ExcelProperty("当前库存")
    private BigDecimal currentStock;
    @Schema(description = "占用数量", example = "12")
    @ExcelProperty("占用数量")
    private BigDecimal lockCount;
    @Schema(description = "在途库存（预留，暂为 0）", example = "0")
    private BigDecimal inTransitStock;
    @Schema(description = "可用库存（暂等于当前库存）", example = "120")
    private BigDecimal availableStock;
    @Schema(description = "是否低于安全库存", example = "false")
    private Boolean lowStockWarning;

    @Schema(description = "是否仅因销售仓库分配而只读可见", example = "false")
    private Boolean readonlyBySaleDistribution;

    @Schema(description = "通用件列表")
    private List<Universal> universals;

    @Schema(description = "自定义字段值，key 为字段编码")
    private Map<String, Object> customFields;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建人")
    @ExcelProperty("创建人")
    private String creatorName;

    @Schema(description = "更新时间")
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新人")
    @ExcelProperty("更新人")
    private String updaterName;

    @Schema(description = "通用件子项")
    @Data
    public static class Universal {

        @Schema(description = "子项 ID")
        private Long id;

        @Schema(description = "通用件编码", example = "P000002")
        private String universalCode;

        @Schema(description = "通用件名称", example = "通用刹车片")
        private String universalName;

        @Schema(description = "适用车型", example = "奔驰 GLC")
        private String universalVehicle;

    }

}
