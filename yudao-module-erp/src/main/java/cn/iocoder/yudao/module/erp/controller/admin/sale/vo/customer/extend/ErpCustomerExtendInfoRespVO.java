package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 客户拓展信息（结构化） Response VO")
@Data
public class ErpCustomerExtendInfoRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "客户编号")
    private Long customerId;

    // ====== 1) 财务与结算信息 ======
    @Schema(description = "预收款金额")
    private BigDecimal advanceAmount;
    @Schema(description = "铺底金额")
    private BigDecimal baseAmount;
    @Schema(description = "结算账户编号")
    private Long settleAccountId;
    @Schema(description = "开户行2")
    private String bankName2;
    @Schema(description = "账户2")
    private String bankAccount2;
    @Schema(description = "结账日期（1-31）")
    private Integer settleDay;
    @Schema(description = "售价上浮率（%）")
    private BigDecimal priceMarkupRate;
    @Schema(description = "优惠价格级别")
    private Integer discountPriceLevel;

    // ====== 2) 销售与业绩信息 ======
    @Schema(description = "月销量目标")
    private BigDecimal monthlySaleTarget;
    @Schema(description = "月营业规模")
    private BigDecimal monthlyBusinessScale;
    @Schema(description = "总资产")
    private BigDecimal totalAssets;
    @Schema(description = "积分系数")
    private BigDecimal pointsRate;
    @Schema(description = "送货费用")
    private BigDecimal deliveryFee;

    // ====== 3) 法人与企业信息 ======
    @Schema(description = "企业法人")
    private String legalPerson;
    @Schema(description = "法人电话")
    private String legalPersonPhone;
    @Schema(description = "老板")
    private String boss;
    @Schema(description = "老板电话")
    private String bossPhone;
    @Schema(description = "经营项目")
    private String businessScope;
    @Schema(description = "身份证号码")
    private String idCardNo;
    @Schema(description = "电子邮件（拓展）")
    private String extEmail;

    // ====== 4) 采购退货联系人信息 ======
    @Schema(description = "采购退货联系人")
    private String purchaseReturnContact;
    @Schema(description = "采购退货电话")
    private String purchaseReturnPhone;
    @Schema(description = "采购退货地址")
    private String purchaseReturnAddress;

    // ====== 5) 系统与业务开关 ======
    @Schema(description = "对应连锁")
    private Long chainGroupId;
    @Schema(description = "替代编码")
    private String alternateCode;
    @Schema(description = "总经销")
    private Long generalDistributorId;
    @Schema(description = "是否生成出仓单")
    private Boolean generateOutBill;
    @Schema(description = "是否生成入仓单")
    private Boolean generateInBill;
    @Schema(description = "自动确认收货：0=不自动 3=3天 7=7天 15=15天")
    private Integer autoConfirmReceiveDays;
    @Schema(description = "内部说明")
    private String internalDescription;
    @Schema(description = "不参与备货")
    private Boolean excludeStocking;
    @Schema(description = "对账客服编号")
    private Long reconcileServiceUserId;
    @Schema(description = "易加网账号")
    private String yijiaWangAccount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
