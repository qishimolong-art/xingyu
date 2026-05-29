package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购票据 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseInvoiceRespVO {

    @Schema(description = "编号", example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "票据单号", example = "CGPJ202605270001")
    @ExcelProperty("票据单号")
    private String no;

    @Schema(description = "状态", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "上海供应商")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "开票日期")
    @ExcelProperty("开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "票据类型", example = "增值税专用发票")
    @ExcelProperty("票据类型")
    private String invoiceType;

    @Schema(description = "发票号", example = "033001900111")
    @ExcelProperty("发票号")
    private String invoiceNo;

    @Schema(description = "发票张数", example = "1")
    @ExcelProperty("发票张数")
    private Integer invoiceCount;

    @Schema(description = "不含税金额", example = "1000")
    @ExcelProperty("不含税金额")
    private BigDecimal taxExclusiveAmount;

    @Schema(description = "税额", example = "130")
    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计", example = "1130")
    @ExcelProperty("价税合计")
    private BigDecimal totalAmount;

    @Schema(description = "部门编号", example = "10")
    private Long deptId;

    @Schema(description = "经手人用户编号", example = "1")
    private Long handlerId;

    @Schema(description = "备注", example = "首批采购发票")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "附件地址")
    @ExcelProperty("附件地址")
    private String fileUrl;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人名称")
    @ExcelProperty("创建人")
    private String creatorName;

    @Schema(description = "修改人")
    private String updater;

    @Schema(description = "修改人名称")
    private String updaterName;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "产品信息")
    @ExcelProperty("产品信息")
    private String productNames;

    @Schema(description = "明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        private Long id;
        private Long sourceInId;
        private String sourceInNo;
        private Long sourceInItemId;
        private Long productId;
        private String productCode;
        private String productName;
        private String productUnitName;
        private String productBarCode;
        private BigDecimal count;
        private BigDecimal productPrice;
        private BigDecimal taxExclusivePrice;
        private BigDecimal taxPercent;
        private BigDecimal taxPrice;
        private BigDecimal totalPrice;
        private String remark;
    }

}
