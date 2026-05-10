package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 采购入库 DO
 *
 * @author 芋道源码
 */
@TableName(value = "erp_purchase_in")
@KeySequence("erp_purchase_in_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购入库单号
     */
    private String no;
    /**
     * 入库状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}
     */
    private Integer status;
    /**
     * 供应商编号
     *
     * 关联 {@link ErpSupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 结算账户编号
     *
     * 关联 {@link ErpAccountDO#getId()}
     */
    private Long accountId;
    /**
     * 入库时间
     */
    private LocalDateTime inTime;

    /**
     * 采购订单编号
     *
     * 关联 {@link ErpPurchaseOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 采购订单号
     *
     * 冗余 {@link ErpPurchaseOrderDO#getNo()}
     */
    private String orderNo;

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
     * 已支付金额，单位：元
     *
     * 目的：和 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO} 结合，记录已支付金额
     */
    private BigDecimal paymentPrice;

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

    // ========== 汽配扩展字段 ==========

    // ========== 系统信息 ==========
    /**
     * 采购员（用户名或显示名，前端文本）
     */
    private String purchaser;
    /**
     * 开票类型（收据/不开票/专票/普票）
     */
    private String invoiceType;
    /**
     * 运输方式（快递/物流/自提）
     */
    private String transportMethod;
    /**
     * 结算方式（挂账/现结/月结）
     */
    private String settleMethod;
    /**
     * 进货区
     */
    private String purchaseArea;
    /**
     * 记账员
     */
    private String accountant;
    /**
     * 浮动率（默认 1.0）
     */
    private BigDecimal floatRate;
    /**
     * 件数（默认 0）
     */
    private Integer packageCount;
    /**
     * 厂家单号
     */
    private String factoryOrderNo;
    /**
     * 开单方式（正常单/样品单/赠品单）
     */
    private String orderMethod;

    // ========== 运费信息 ==========
    /**
     * 运费类型 1
     */
    private String freightType1;
    /**
     * 运费类型 2
     */
    private String freightType2;
    /**
     * 运费对象 1
     */
    private String freightObject1;
    /**
     * 运费对象 2
     */
    private String freightObject2;
    /**
     * 物流公司
     */
    private String logisticsCompany;

    // ========== 供应商信息 ==========
    /**
     * 经办人（默认当前用户）
     */
    private String handler;
    /**
     * 税率（百分比）
     */
    private BigDecimal taxRate;
    /**
     * 部门 ID
     */
    private Long deptId;
    /**
     * 采购折让金额（默认 0.00）
     */
    private BigDecimal purchaseDiscount;
    /**
     * 优先级（正常件/紧急件）
     */
    private String priority;
    /**
     * 卸货员
     */
    private String unloader;
    /**
     * 浮动记录
     */
    private String floatRecord;
    /**
     * 收货单位
     */
    private String receiveUnit;
    /**
     * 总运费 1（默认 0.00）
     */
    private BigDecimal totalFreight1;
    /**
     * 总运费 2（默认 0.00）
     */
    private BigDecimal totalFreight2;
    /**
     * 付款日期
     */
    private LocalDateTime paymentDate;
    /**
     * 是否发票（复选）
     */
    private Boolean hasInvoice;

    // ========== 其他信息 ==========
    /**
     * 所属经营
     */
    private String businessEntity;

}