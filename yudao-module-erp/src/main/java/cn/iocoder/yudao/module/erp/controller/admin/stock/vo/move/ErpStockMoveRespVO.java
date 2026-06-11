package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.DictTypeConstants.AUDIT_STATUS;

@Schema(description = "Admin - ERP stock move Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockMoveRespVO {

    @Schema(description = "Stock move id", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
    @ExcelProperty("调拨编号")
    private Long id;

    @Schema(description = "Stock move no", requiredMode = Schema.RequiredMode.REQUIRED, example = "S123")
    @ExcelProperty("调拨单号")
    private String no;

    @Schema(description = "Department id", example = "100")
    @ExcelProperty("所属部门ID")
    private Long deptId;

    @Schema(description = "Department name")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "Move time", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("调拨时间")
    private LocalDateTime moveTime;

    @Schema(description = "Source type", example = "30")
    private Integer sourceType;

    @Schema(description = "Source document id", example = "1024")
    private Long sourceId;

    @Schema(description = "Source document no", example = "SC202606100001")
    private String sourceNo;

    @Schema(description = "Total count", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;

    @Schema(description = "Total price", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @Schema(description = "Status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(AUDIT_STATUS)
    private Integer status;

    @Schema(description = "Remark", example = "remark")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "Attachment URL", example = "https://www.iocoder.cn/1.doc")
    private String fileUrl;

    @Schema(description = "Creator", example = "admin")
    private String creator;

    @Schema(description = "Creator name", example = "admin")
    private String creatorName;

    private String updater;

    @Schema(description = "Create time", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "Updater name")
    private String updaterName;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;

    @Schema(description = "Move items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "Product names", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    @Schema(description = "Product codes")
    @ExcelProperty("产品编码")
    private String productCodes;

    @Data
    public static class Item {

        @Schema(description = "Move item id", example = "11756")
        private Long id;

        @Schema(description = "From warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long fromWarehouseId;

        @Schema(description = "To warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "888")
        private Long toWarehouseId;

        @Schema(description = "Product id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "Product price", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "Product count", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal count;

        @Schema(description = "Remark", example = "remark")
        private String remark;

        @Schema(description = "Product name", requiredMode = Schema.RequiredMode.REQUIRED, example = "product")
        private String productName;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Product bar code", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;

        @Schema(description = "Product unit name", requiredMode = Schema.RequiredMode.REQUIRED, example = "pcs")
        private String productUnitName;

        @Schema(description = "Stock count", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal stockCount;

    }

}
