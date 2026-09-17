package cn.iocoder.yudao.module.erp.controller.admin.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ErpPurchaseCostConfirmationModels {
    private ErpPurchaseCostConfirmationModels() { }
    @Data public static class Header {
        private Long purchaseInId;
        private String purchaseNo;
        private String sourceSignature;
        private Integer latestRevision;
        private Long confirmationId;
        private String status;
        private String reason;
        private Integer sourceStatus;
        private boolean canConfirm;
        private String confirmBlockedReason;
        private Integer sourceItemCount;
        private String priceBasis = "INCLUSIVE_UNCONFIRMED";
        private String taxStatus = "UNKNOWN";
        private String ruleVersion = "MANUAL_LINE_AMOUNT_V1";
        private boolean costMasked;
        private String evidence;
        private String feeTreatment;
        private Long confirmedBy;
        @JsonSerialize(using=ToStringSerializer.class) private LocalDateTime confirmedAt;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal rawProductAmount;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal discountAmount;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal feeAmount;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal freight1;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal freight2;
    }
    @Data public static class Item {
        private String sourceSignature;
        private Long sourceItemId;
        private Long productId;
        private String productCode;
        private String productName;
        private Long warehouseId;
        private Boolean gift;
        private String evidence;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal quantity;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal rawUnitPrice;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal rawLineAmount;
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal confirmedNetTotalAmount;
    }
    @Data public static class ConfirmRequest {
        @NotNull @Positive private Long purchaseInId;
        @NotBlank @Size(max=64) private String expectedSignature;
        @NotNull @Min(0) private Integer expectedRevision;
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,80}") private String requestKey;
        @NotBlank @Size(max=1000) private String evidence;
        @NotBlank @Size(max=1000) private String feeTreatment;
        @NotEmpty @Valid private List<ConfirmItem> items;
    }
    @Data public static class ConfirmItem {
        @NotNull @Positive private Long sourceItemId;
        @NotNull @DecimalMin("0") @Digits(integer=18,fraction=6) private BigDecimal confirmedNetTotalAmount;
        @NotBlank @Size(max=1000) private String evidence;
    }
}
