package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 银行转账单 DO
 */
@TableName("erp_finance_transfer")
@KeySequence("erp_finance_transfer_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFinanceTransferDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 转账单号
     */
    private String no;

    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}
     */
    private Integer status;

    /**
     * 转账时间
     */
    private LocalDateTime transferTime;

    /**
     * 转出账户编号
     */
    private Long outAccountId;

    /**
     * 转入账户编号
     */
    private Long inAccountId;

    /**
     * 转账金额
     */
    private BigDecimal transferPrice;

    /**
     * 财务人员编号
     */
    private Long financeUserId;
    /**
     * Department id.
     */
    private Long deptId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 附件 URL
     */
    private String fileUrl;

}
