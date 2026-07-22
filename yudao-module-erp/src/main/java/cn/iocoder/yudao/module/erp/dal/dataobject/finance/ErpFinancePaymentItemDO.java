package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 付款项 DO
 *
 * @author 芋道源码
 */
@TableName("erp_finance_payment_item")
@KeySequence("erp_finance_payment_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFinancePaymentItemDO extends BaseDO {

    /**
     * 入库项编号
     */
    @TableId
    private Long id;
    /**
     * 付款单编号
     *
     * 关联 {@link ErpFinancePaymentDO#getId()}
     */
    private Long paymentId;

    /**
     * 业务类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum} 的采购入库、退货
     */
    private Integer bizType;
    /**
     * 业务编号
     *
     * 例如说：{@link ErpPurchaseInDO#getId()}
     */
    private Long bizId;
    /**
     * 业务单号
     *
     * 例如说：{@link ErpPurchaseInDO#getNo()}
     */
    private String bizNo;

    /**
     * 应付金额，单位：分
     */
    private BigDecimal totalPrice;
    /**
     * 已付金额，单位：分
     */
    private BigDecimal paidPrice;
    /**
     * 本次付款，单位：分
     */
    private BigDecimal paymentPrice;

    /**
     * 核销状态：0 待生效、1 已生效、2 已撤销
     */
    private Integer writeOffStatus;
    /**
     * 核销生效时间
     */
    private LocalDateTime writeOffTime;
    /**
     * 核销操作人
     */
    private Long writeOffUserId;
    /**
     * 撤销时间
     */
    private LocalDateTime reverseTime;
    /**
     * 撤销操作人
     */
    private Long reverseUserId;
    /**
     * 撤销原因
     */
    private String reverseReason;
    /**
     * 备注
     */
    private String remark;

}
