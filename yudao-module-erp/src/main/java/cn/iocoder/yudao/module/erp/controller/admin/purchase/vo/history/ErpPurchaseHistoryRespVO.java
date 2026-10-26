package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 采购历史 Response VO")
@Data
@Accessors(chain = true)
public class ErpPurchaseHistoryRespVO {

    public static final String TYPE_IN = "PURCHASE_IN";
    public static final String TYPE_RETURN = "PURCHASE_RETURN";

    @Schema(description = "跨单据类型唯一行键", example = "PURCHASE_IN-1024")
    private String rowKey;
    @Schema(description = "业务单据编号", example = "1024")
    private Long businessId;
    @Schema(description = "业务明细编号", example = "2048")
    private Long itemId;
    @Schema(description = "单据类型编码", example = TYPE_IN)
    private String documentTypeCode;
    @Schema(description = "单据类型", example = "采购入库")
    private String documentType;
    @Schema(description = "单据编号", example = "CGRK20261021000001")
    private String documentNo;
    @Schema(description = "供应商编号", example = "1024")
    private Long supplierId;
    @Schema(description = "供应商名称", example = "示例供应商")
    private String supplierName;
    @Schema(description = "数量；采购退货为负数", example = "-2")
    private BigDecimal count;
    @Schema(description = "采购价格；采购退货仍为正数", example = "18.5")
    private BigDecimal productPrice;
    @Schema(description = "业务日期")
    private LocalDateTime businessTime;
    @Schema(description = "仓库名称", example = "主仓库")
    private String warehouseName;
    @Schema(description = "开单人", example = "管理员")
    private String creatorName;
    @Schema(description = "单据备注")
    private String remark;
}
