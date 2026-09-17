package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购票据新增/修改 Request VO")
@Data
public class ErpPurchaseInvoiceSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "开票日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    @Schema(description = "票据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "增值税专用发票")
    @NotBlank(message = "票据类型不能为空")
    private String invoiceType;

    @Schema(description = "发票号", requiredMode = Schema.RequiredMode.REQUIRED, example = "033001900111")
    @NotBlank(message = "发票号不能为空")
    private String invoiceNo;

    @Schema(description = "发票张数", example = "1")
    private Integer invoiceCount;

    @Schema(description = "部门编号", example = "10")
    private Long deptId;

    @Schema(description = "经手人用户编号", example = "1")
    private Long handlerId;

    @Schema(description = "备注", example = "首批采购发票")
    private String remark;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn/demo.pdf")
    private String fileUrl;

    @Schema(description = "来源入库单编号列表，用于大单分页带入明细")
    private List<Long> sourceInIds;

    @Schema(description = "排除的来源入库单明细编号列表，用于大单分页带入后删除明细")
    private List<Long> excludedSourceInItemIds;

    @Schema(description = "票据明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "明细编号", example = "1")
        private Long id;

        @Schema(description = "明细操作类型：insert 新增，update 修改，delete 删除", example = "update")
        private String operation;

        @Schema(description = "来源入库单编号", example = "100")
        private Long sourceInId;

        @Schema(description = "来源入库单号", example = "CGRK202605270001")
        private String sourceInNo;

        @Schema(description = "来源入库单明细编号", example = "200")
        private Long sourceInItemId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "300")
        @NotNull(message = "产品不能为空")
        private Long productId;

        @Schema(description = "产品编码", example = "P000001")
        private String productCode;

        @Schema(description = "产品名称", example = "机油滤芯")
        private String productName;

        @Schema(description = "产品单位名称", example = "个")
        private String productUnitName;

        @Schema(description = "产品条码", example = "6901234567890")
        private String productBarCode;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "数量不能为空")
        private BigDecimal count;

        @Schema(description = "不含税单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.50")
        @NotNull(message = "产品单价不能为空")
        private BigDecimal productPrice;

        @Schema(description = "税率(%)", example = "13")
        private BigDecimal taxPercent;

        @Schema(description = "备注", example = "票据拆分")
        private String remark;
    }

}
