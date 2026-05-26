package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 客户拓展信息（结构化）DO
 *
 * 承载《客户v2》文档 Tab 6 分组 1-5 的结构化字段，一对一（customerId 唯一）。
 * 动态字段继续保留在 {@link ErpCustomerExtendDO}（KV 表）。
 */
@TableName("erp_customer_extend_info")
@KeySequence("erp_customer_extend_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCustomerExtendInfoDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;

    // ====== 1) 财务与结算信息（8） ======
    private BigDecimal advanceAmount;
    private BigDecimal baseAmount;
    private Long settleAccountId;
    private String bankName2;
    private String bankAccount2;
    private Integer settleDay;
    private BigDecimal priceMarkupRate;
    private Integer discountPriceLevel;

    // ====== 2) 销售与业绩信息（5） ======
    private BigDecimal monthlySaleTarget;
    private BigDecimal monthlyBusinessScale;
    private BigDecimal totalAssets;
    private BigDecimal pointsRate;
    private BigDecimal deliveryFee;

    // ====== 3) 法人与企业信息（7） ======
    private String legalPerson;
    private String legalPersonPhone;
    private String boss;
    private String bossPhone;
    private String businessScope;
    private String idCardNo;
    private String extEmail;

    // ====== 4) 采购退货联系人信息（3） ======
    private String purchaseReturnContact;
    private String purchaseReturnPhone;
    private String purchaseReturnAddress;

    // ====== 5) 系统与业务开关（9+） ======
    private Long chainGroupId;
    private String alternateCode;
    private Long generalDistributorId;
    private Boolean generateOutBill;
    private Boolean generateInBill;
    private Integer autoConfirmReceiveDays;
    private String internalDescription;
    private Boolean excludeStocking;
    private Long reconcileServiceUserId;
    private String yijiaWangAccount;

}
