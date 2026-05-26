package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 预收款单 DO
 */
@TableName("erp_pre_receipt")
@KeySequence("erp_pre_receipt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPreReceiptDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 预收款单号
     */
    private String no;
    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}
     */
    private Integer status;
    /**
     * 业务时间
     */
    private LocalDateTime bizTime;
    /**
     * 往来类型：1=客户 2=供应商 3=员工
     */
    private Integer partyType;
    /**
     * 往来方 ID
     */
    private Long partyId;
    /**
     * 往来方名称
     */
    private String partyName;
    /**
     * 结算账户编号
     */
    private Long accountId;
    /**
     * 合计金额
     */
    private BigDecimal totalAmount;
    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;
    /**
     * 实收金额
     */
    private BigDecimal actualAmount;
    /**
     * 备注
     */
    private String remark;
    /**
     * 附件 URL
     */
    private String fileUrl;

}
