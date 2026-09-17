package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.DictTypeConstants.AUDIT_STATUS;

@Schema(description = "管理后台 - ERP 库存盘点单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockCheckRespVO {

    @Schema(description = "盘点编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
    @ExcelProperty("盘点编号")
    private Long id;

    @Schema(description = "盘点单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "S123")
    @ExcelProperty("盘点单号")
    private String no;

    @Schema(description = "所属部门", example = "100")
    @ExcelProperty("所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "盘点时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("盘点时间")
    private LocalDateTime checkTime;

    @Schema(description = "盘点类型：1 盘数量，2 盘成本", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("盘点类型")
    private Integer checkType;

    @Schema(description = "合计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;

    @Schema(description = "合计金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(AUDIT_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "附件 URL", example = "https://www.iocoder.cn/1.doc")
    private String fileUrl;

    @Schema(description = "创建人", example = "芋道")
    private String creator;
    @Schema(description = "创建人名称", example = "芋道")
    private String creatorName;

    private String updater;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改人名称")
    private String updaterName;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "打印次数")
    @ExcelProperty("打印次数")
    private Integer printCount;

    @Schema(description = "最后打印时间")
    @ExcelProperty("打印时间")
    private LocalDateTime printTime;

    @Schema(description = "项数")
    private Integer itemCount;

    @Schema(description = "仓库信息")
    private String warehouseNames;

    @Schema(description = "盘点项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    @Schema(description = "产品编码")
    @ExcelProperty("产品编码")
    private String productCodes;

    @Data
    public static class Item {

        @Schema(description = "盘点项编号", example = "11756")
        private Long id;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long warehouseId;

        @Schema(description = "仓库名称")
        private String warehouseName;

        @Schema(description = "仓库所属部门 ID", example = "100")
        private Long warehouseDeptId;

        @Schema(description = "仓库所属部门名称")
        private String warehouseDeptName;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "批次号", example = "BATCH-001")
        private String batchNo;

        @Schema(description = "产品资料采购价/库存采购价", example = "88.00")
        private BigDecimal productPurchasePrice;
        @Schema(description = "销售价", example = "120.00")
        private BigDecimal salePrice;
        @Schema(description = "最近销售价", example = "118.00")
        private BigDecimal lastSalePrice;
        @Schema(description = "最低价", example = "95.00")
        private BigDecimal minPrice;
        @Schema(description = "参考价", example = "100.00")
        private BigDecimal referencePrice;
        @Schema(description = "零售价", example = "128.00")
        private BigDecimal retailPrice;
        @Schema(description = "最后采购价", example = "86.00")
        private BigDecimal lastPurchasePrice;
        @Schema(description = "毛利率", example = "20")
        private Integer grossProfitRate;
        @Schema(description = "备用价1", example = "98.00")
        private BigDecimal backupPrice1;
        @Schema(description = "批发价", example = "98.00")
        private BigDecimal wholesalePrice;
        @Schema(description = "股份价", example = "90.00")
        private BigDecimal sharePrice;
        @Schema(description = "价格是否可见", example = "true")
        private Boolean priceVisible;

        @Schema(description = "产品单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "账面数量（当前库存）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "账面数量不能为空")
        private BigDecimal stockCount;

        @Schema(description = "产品单位编号")
        private Long productUnitId;

        @Schema(description = "包装数", example = "12")
        private Integer packageQty;

        @Schema(description = "单重", example = "1.25")
        private BigDecimal weight;

        @Schema(description = "总重", example = "125.00")
        private BigDecimal totalWeight;

        @Schema(description = "实际数量（实际库存）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "实际数量不能为空")
        private BigDecimal actualCount;

        @Schema(description = "调整数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "调整数量不能为空")
        private BigDecimal count;

        @Schema(description = "备注", example = "随便")
        private String remark;

        // ========== 关联字段 ==========

        @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "巧克力")
        private String productName;
        @Schema(description = "产品编码")
        private String productCode;
        @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;
        @Schema(description = "产品单位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "件")
        private String productUnitName;
        @Schema(description = "是否启用批次号")
        private Boolean batchNoEnabled;

    }

}
