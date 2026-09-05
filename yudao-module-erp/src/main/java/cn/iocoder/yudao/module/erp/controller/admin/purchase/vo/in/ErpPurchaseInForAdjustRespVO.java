package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购入库单 - 供采购调价"按入库单"方式选择使用的列表 VO
 *
 * 仅返回调价选单需要的字段，避免拉完整 {@link ErpPurchaseInRespVO}
 */
@Schema(description = "管理后台 - ERP 采购入库单列表（供调价选择） Response VO")
@Data
public class ErpPurchaseInForAdjustRespVO {

    @Schema(description = "入库单编号", example = "17386")
    private Long id;

    @Schema(description = "入库单号", example = "PI20260512001")
    private String no;

    @Schema(description = "供应商编号", example = "1024")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "芋道供应商")
    private String supplierName;

    @Schema(description = "入库时间")
    private LocalDateTime inTime;

    /**
     * 审核人 ID。
     * 注意：{@link cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO} 未显式记录审核人，
     * 这里取 {@code updater}（最后一次状态变更人）作为近似
     */
    @Schema(description = "审核人编号（近似值：取 updater）", example = "1")
    private Long auditorId;

    @Schema(description = "审核人姓名", example = "芋道源码")
    private String auditorName;

    /**
     * 审批时间。
     * 注意：DO 未显式记录审批时间，这里取 {@code updateTime}（最后一次状态变更时间）作为近似
     */
    @Schema(description = "审批时间（近似值：取 updateTime）")
    private LocalDateTime approveTime;

    @Schema(description = "合计产品价格", example = "1000.00")
    private BigDecimal totalProductPrice;

    @Schema(description = "合计税额", example = "130.00")
    private BigDecimal totalTaxPrice;

    @Schema(description = "最终合计价格", example = "1130.00")
    private BigDecimal totalPrice;

    @Schema(description = "明细行数", example = "5")
    private Integer itemCount;

    @Schema(description = "是否被调价过（前端显示\"（调）\"）", example = "false")
    private Boolean adjusted;

    @Schema(description = "是否已完成采购票据开具", example = "false")
    private Boolean hasInvoice;

    @Schema(description = "开票类型", example = "普票")
    private String invoiceType;

    @Schema(description = "备注", example = "首次入库")
    private String remark;

}
