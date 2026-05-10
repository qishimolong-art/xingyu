package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 采购订单导入解析响应 VO
 */
@Schema(description = "管理后台 - ERP 采购订单导入解析 Response VO")
@Data
public class ErpPurchaseOrderImportRespVO {

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "采购员（用户ID）")
    private Long purchaser;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "税率(%)")
    private BigDecimal taxPercent;

    @Schema(description = "订货日期")
    private LocalDateTime orderDate;

    @Schema(description = "采购周期(天)")
    private Integer purchaseCycle;

    @Schema(description = "到货日期")
    private LocalDateTime arrivalDate;

    @Schema(description = "送货方式")
    private String deliveryMethod;

    @Schema(description = "采购方式")
    private String purchaseType;

    @Schema(description = "结算方式")
    private String settleMethod;

    @Schema(description = "开票类型")
    private String invoiceType;

    @Schema(description = "厂家单号")
    private String factoryOrderNo;

    @Schema(description = "收货地址")
    private String receiveAddress;

    @Schema(description = "订货公司")
    private String orderCompany;

    @Schema(description = "备注")
    private String remark;

}
