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
 * ERP 采购退货 DO
 *
 * @author 芋道源码
 */
@TableName(value = "erp_purchase_return")
@KeySequence("erp_purchase_return_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseReturnDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购退货单号
     */
    private String no;
    /**
     * 退货状态
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
     * 退货时间
     */
    private LocalDateTime returnTime;

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
     * 已退款金额，单位：元
     *
     * 目的：和 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO} 结合，记录已支付金额
     */
    private BigDecimal refundPrice;

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
    /**
     * 退货类型(入库单退货等)
     */
    private String returnType;
    /**
     * 采购员
     */
    private String purchaser;
    /**
     * 开票类型
     */
    private String invoiceType;
    /**
     * 运输方式
     */
    private String transportMethod;
    /**
     * 结算方式
     */
    private String settleMethod;
    /**
     * 件数
     */
    private Integer packageCount;
    /**
     * 运费金额
     */
    private BigDecimal freightAmount;
    /**
     * 物流公司
     */
    private String logisticsCompany;
    /**
     * 单据来源(正常退货等)
     */
    private String docSource;
    /**
     * 厂家单号
     */
    private String factoryOrderNo;
    /**
     * 制单人
     */
    private String maker;
    /**
     * 部门
     */
    private String dept;
    /**
     * 发货区
     */
    private String shippingArea;
    /**
     * 仓库类型
     */
    private String warehouseType;
    /**
     * 运费类型
     */
    private String freightType;
    /**
     * 物流单号
     */
    private String logisticsNo;
    /**
     * 优先级
     */
    private String priority;
    /**
     * 开单方式(正常单等)
     */
    private String orderMethod;

    // ========== 三期：双模式字段 ==========
    /**
     * 退货模式（10=按单退货 BY_ORDER，20=按库存退货 BY_STOCK）
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnModeEnum}
     */
    private Integer returnMode;

    // ========== 八期：扩展字段 ==========
    /**
     * 税率
     */
    private BigDecimal taxRate;
    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 经办人（制单人，用户 ID）
     */
    private Long handler;

}