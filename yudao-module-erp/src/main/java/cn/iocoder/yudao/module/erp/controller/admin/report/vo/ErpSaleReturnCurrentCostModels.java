package cn.iocoder.yudao.module.erp.controller.admin.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ErpSaleReturnCurrentCostModels {
    private ErpSaleReturnCurrentCostModels() { }

    @Data
    public static class Header {
        private Long id;
        private String no;
        private boolean enabled;
        private String status;
        private String reason;
        private Integer sourceStatus;
        private String sourceSignature;
        private String basisSignature;
        private int sourceItemCount;
        private int latestRevision;
        private Long confirmationId;
        private boolean costMasked;
        private boolean canConfirm;
        private boolean canApprove;
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal financialAmount;
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal settlementAmount;
    }

    @Data
    public static class Row {
        private Long sourceItemId;
        private Long productId;
        private String productCode;
        private String productName;
        private Long warehouseId;
        private String warehouseName;
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal quantity;
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal financialAmount;
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal settlementAmount;
        private String costSource;
        private String reason;
        private String sourceSignature;
        private String basisSignature;
    }

    @Data
    public static class ConfirmRequest {
        @NotNull @Positive private Long id;
        @NotBlank private String expectedSourceSignature;
        @NotBlank private String expectedBasisSignature;
        @NotNull @Min(0) private Integer expectedRevision;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") private String requestKey;
        @NotBlank @Size(max = 500) private String evidence;
        @NotEmpty @Valid private List<ConfirmItem> items;
    }

    @Data
    public static class ConfirmItem {
        @NotNull @Positive private Long sourceItemId;
        @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 6) private BigDecimal financialAmount;
        @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 6) private BigDecimal settlementAmount;
        @NotBlank @Size(max = 500) private String evidence;
    }
}
