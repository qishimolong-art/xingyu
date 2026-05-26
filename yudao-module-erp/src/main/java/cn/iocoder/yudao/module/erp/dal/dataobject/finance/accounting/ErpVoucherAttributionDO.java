package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 凭证归属（跨月调整）DO
 *
 * @author Claude
 */
@TableName("erp_voucher_attribution")
@KeySequence("erp_voucher_attribution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVoucherAttributionDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 业务单据类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum}
     */
    private Integer bizType;
    /**
     * 业务单 ID
     */
    private Long bizId;
    /**
     * 业务单号（冗余）
     */
    private String bizNo;
    /**
     * 业务发生日期
     */
    private LocalDateTime bizDate;
    /**
     * 原始金额
     */
    private BigDecimal bizAmount;
    /**
     * 折让金额
     */
    private BigDecimal discountAmount;
    /**
     * 实收金额
     */
    private BigDecimal receivedAmount;
    /**
     * 制单日期（实际生成日期）
     */
    private LocalDate voucherMakeDate;
    /**
     * 归属年
     */
    private Integer attributionYear;
    /**
     * 归属月（1-12）
     */
    private Integer attributionMonth;
    /**
     * 归属状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAttributionStatusEnum}
     */
    private Integer attributionStatus;
    /**
     * 生成的凭证 ID（关联 {@link ErpVoucherDO#getId()}）
     */
    private Long voucherId;
    /**
     * 交易单位（客户/供应商/项目名）
     */
    private String transactionParty;
    /**
     * 业务员 ID
     */
    private Long handlerUserId;
    /**
     * 审核人 ID
     */
    private Long auditorUserId;
    /**
     * 确认时间
     */
    private LocalDateTime auditTime;
    /**
     * 结算方式
     */
    private String settleMethod;
    /**
     * 部门 ID
     */
    private Long deptId;
    /**
     * 运输方式
     */
    private String shippingMethod;
    /**
     * 应收确认
     */
    private Boolean receivableConfirmed;
    /**
     * 是否开票
     */
    private Boolean invoiceIssued;
    /**
     * 发货状态
     */
    private Integer shipStatus;
    /**
     * 摘要
     */
    private String summary;
    /**
     * 备注
     */
    private String remark;

}
