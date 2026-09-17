package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 凭证分录明细 DO
 *
 * @author Claude
 */
@TableName(value = "erp_voucher_item", autoResultMap = true)
@KeySequence("erp_voucher_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVoucherItemDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 关联 {@link ErpVoucherDO#getId()}
     */
    private Long voucherId;
    /**
     * 行号
     */
    private Integer lineNo;
    /**
     * 该行摘要
     */
    private String summary;
    /**
     * 关联 {@link ErpAccountingSubjectDO#getId()}
     */
    private Long subjectId;
    /**
     * 科目编码（冗余）
     */
    private String subjectCode;
    /**
     * 科目名称（冗余）
     */
    private String subjectName;
    /**
     * 辅助核算类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAuxiliaryTypeEnum}
     */
    private String auxiliaryType;
    /**
     * 辅助核算 ID（按 auxiliaryType 解释）
     */
    private Long auxiliaryId;
    /**
     * 辅助核算名称（冗余）
     */
    private String auxiliaryName;
    /**
     * 借方金额
     */
    private BigDecimal debitAmount;
    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;

    @com.baomidou.mybatisplus.annotation.TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private java.util.List<cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Auxiliary> auxiliaries;
}
