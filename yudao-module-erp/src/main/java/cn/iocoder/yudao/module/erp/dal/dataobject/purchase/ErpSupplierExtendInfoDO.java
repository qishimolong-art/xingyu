package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@TableName("erp_supplier_extend_info")
@KeySequence("erp_supplier_extend_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierExtendInfoDO extends BaseDO {

    @TableId
    private Long id;
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
