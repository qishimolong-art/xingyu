package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.DictTypeConstants.AUDIT_STATUS;

@Schema(description = "管理后台 - ERP 其它入库单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockInRespVO {

    @Schema(description = "入库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
    @ExcelProperty("入库编号")
    private Long id;

    @Schema(description = "入库单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "S123")
    @ExcelProperty("入库单号")
    private String no;

    @Schema(description = "所属部门", example = "100")
    @ExcelProperty("所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "供应商编号", example = "3113")
    private Long supplierId;
    @Schema(description = "供应商名称", example = "芋道")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "仓库名称，多个仓库逗号分隔")
    private String warehouseNames;

    @Schema(description = "打印次数")
    private Integer printCount;

    @Schema(description = "最后打印时间")
    private LocalDateTime lastPrintTime;

    @Schema(description = "入库时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("入库时间")
    private LocalDateTime inTime;

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

    @Schema(description = "入库项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    @Schema(description = "产品编码")
    @ExcelProperty("产品编码")
    private String productCodes;

    @Data
    public static class Item {

        @Schema(description = "入库项编号", example = "11756")
        private Long id;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long warehouseId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

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

        @Schema(description = "产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal count;

        @Schema(description = "批次号", example = "BATCH-001")
        private String batchNo;

        @Schema(description = "产品单位编号")
        private Long productUnitId;

        @Schema(description = "包装数", example = "12")
        private Integer packageQty;

        @Schema(description = "单重", example = "1.25")
        private BigDecimal weight;

        @Schema(description = "总重", example = "125.00")
        private BigDecimal totalWeight;

        @Schema(description = "备注", example = "随便")
        private String remark;

        // ========== 关联字段 ==========

        @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "巧克力")
        private String productName;
        @Schema(description = "产品编码")
        private String productCode;
        @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;
        @Schema(description = "产品单位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
        private String productUnitName;

        @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal stockCount; // 该字段仅仅在“详情”和“编辑”时使用

    }

}
