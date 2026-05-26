package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 销售出库 DO
 *
 * @author 芋道源码
 */
@TableName(value = "erp_sale_out")
@KeySequence("erp_sale_out_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSaleOutDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 销售出库单号
     */
    private String no;
    /**
     * 出库状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}
     */
    private Integer status;
    /**
     * 客户编号
     *
     * 关联 {@link ErpCustomerDO#getId()}
     */
    private Long customerId;
    /**
     * 结算账户编号
     *
     * 关联 {@link ErpAccountDO#getId()}
     */
    private Long accountId;
    /**
     * 销售员编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long saleUserId;
    /**
     * 出库时间
     */
    private LocalDateTime outTime;

    /**
     * 销售订单编号
     *
     * 关联 {@link ErpSaleOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 销售订单号
     *
     * 冗余 {@link ErpSaleOrderDO#getNo()}
     */
    private String orderNo;

    /**
     * 业务来源类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum}
     */
    private Integer sourceType;
    /**
     * 来源单据编号
     */
    private Long sourceId;
    /**
     * 来源单据号
     */
    private String sourceNo;

    /**
     * 合计数量
     */
    private BigDecimal totalCount;
    /**
     * 最终合计价格，单位：元
     *
     * totalPrice = totalProductPrice + totalTaxPrice - discountPrice + otherPrice
     */
    private BigDecimal totalPrice;
    /**
     * 已收款金额，单位：元
     *
     * 目的：和 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO} 结合，记录已收款金额
     */
    private BigDecimal receiptPrice;

    /**
     * 合计产品价格，单位：元
     */
    private BigDecimal totalProductPrice;
    /**
     * 合计税额，单位：元
     */
    private BigDecimal totalTaxPrice;
    /**
     * 优惠率，百分比
     */
    private BigDecimal discountPercent;
    /**
     * 优惠金额，单位：元
     *
     * discountPrice = (totalProductPrice + totalTaxPrice) * discountPercent
     */
    private BigDecimal discountPrice;
    /**
     * 其它金额，单位：元
     */
    private BigDecimal otherPrice;

    /**
     * 附件地址
     */
    private String fileUrl;
    /**
     * 备注
     */
    private String remark;

    /**
     * 是否因销售调价作废
     */
    private Boolean adjusted;
    /**
     * 调价来源销售单编号
     */
    private Long adjustSourceOutId;
    /**
     * 调价重做后的新销售单编号
     */
    private Long adjustNewOutId;
    /**
     * 销售调价单编号
     */
    private Long adjustPriceAdjustId;

    // ========== 业务扩展字段 ==========

    /**
     * 结算状态（0=未结算, 1=部分结算, 2=已结算）
     */
    private Integer settleStatus;
    /**
     * 订单类型
     */
    private String orderType;
    /**
     * 额外费用
     */
    private BigDecimal extraFee;
    /**
     * 优先级
     */
    private String priority;
    /**
     * 客户签收状态（0=未签收, 1=已签收）
     */
    private Integer signStatus;
    /**
     * 签收图片
     */
    private String signImageUrl;

    // ========== 物流信息 ==========

    /**
     * 送货方式
     */
    private String deliveryMethod;
    /**
     * 发货方
     */
    private String shipper;
    /**
     * 收货人
     */
    private String receiverName;
    /**
     * 收货电话
     */
    private String receiverPhone;
    /**
     * 配送单号
     */
    private String deliveryNo;
    /**
     * 物流单号
     */
    private String logisticsNo;
    /**
     * 物流公司
     */
    private String logisticsCompany;
    /**
     * 发货人
     */
    private String senderName;
    /**
     * 保险公司
     */
    private String insuranceCompany;
    /**
     * 第三方单号
     */
    private String thirdPartyNo;
    /**
     * 第三方上游单号
     */
    private String thirdPartyUpstreamNo;

    // ========== 财务信息 ==========

    /**
     * 结算方式
     */
    private String settleMethod;
    /**
     * 开票金额
     */
    private BigDecimal invoiceAmount;
    /**
     * 减收金额
     */
    private BigDecimal reductionAmount;
    /**
     * 减后金额
     */
    private BigDecimal afterReductionAmount;
    /**
     * 票据金额
     */
    private BigDecimal billAmount;
    /**
     * 运费
     */
    private BigDecimal freight;
    /**
     * 票据类型
     */
    private String billType;
    /**
     * 票据号
     */
    private String billNo;

    // ========== 取消信息 ==========

    /**
     * 取消数量
     */
    private BigDecimal cancelCount;
    /**
     * 取消金额
     */
    private BigDecimal cancelAmount;
    /**
     * 取消后金额
     */
    private BigDecimal afterCancelAmount;

    // ========== 人员/部门 ==========

    /**
     * 审核人编号
     */
    private Long auditorId;
    /**
     * 部门编号
     */
    private Long deptId;

    // ========== 时间 ==========

    /**
     * 总重
     */
    private BigDecimal totalWeight;
    /**
     * 审核时间
     */
    private LocalDateTime approveTime;
    /**
     * 打印时间
     */
    private LocalDateTime printTime;
    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;
    /**
     * 来源单制单日期
     */
    private LocalDateTime sourceCreateTime;

    // ========== 其他 ==========

    /**
     * 内部说明
     */
    private String internalNote;
    /**
     * VIN
     */
    private String vin;
    /**
     * 打印次数
     */
    private Integer printCount;

    /**
     * 退货状态（0=未退货, 1=部分退货, 2=已退货）
     */
    private Integer returnStatus;

}
