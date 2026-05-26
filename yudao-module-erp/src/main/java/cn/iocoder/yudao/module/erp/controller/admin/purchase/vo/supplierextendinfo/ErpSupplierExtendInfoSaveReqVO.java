package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextendinfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "管理后台 - ERP 供应商结构化拓展信息保存 Request VO")
@Data
public class ErpSupplierExtendInfoSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    private Long chainGroupId;
    private String bankName2;
    private String bankAccount2;
    private String extEmail;
    private String idCardNo;
    private String alternateCode;
    private BigDecimal baseAmount;
    private String legalPerson;
    private String legalPersonPhone;
    private String boss;
    private String bossPhone;
    private BigDecimal priceMarkupRate;
    private Boolean keySupplier;
    private Boolean allianceReplenishment;
    private String purchaseReturnContact;
    private String purchaseReturnAddress;
    private String purchaseReturnPhone;
    private Integer settleDay;
    private Boolean generateInBill;
    private String inboundMethod;
    private Boolean generateLabelTask;
    private String allianceOrderMatchRule;
    private String allianceBillExtractRule;
    private Long settleAccountId;
    private Integer scrapReturnCycleDays;
    private LocalDate scrapReturnNextDate;
    private BigDecimal scrapReturnThresholdAmount;
    private Boolean autoReplenishmentEnabled;
    private String autoReplenishmentFormula;
    private String autoReplenishmentCycle;
    private LocalTime autoReplenishmentTime;
    private LocalDate nextOrderDate;
    private Integer stockUpperDays;
    private Integer stockStandardDays;
    private Integer stockLowerDays;
    private String dailySaleCycle;
    private Integer dailySaleRecentDays;
    private Integer limitAdjustRecentMonths;
    private BigDecimal limitAdjustMonthlySales;

}
