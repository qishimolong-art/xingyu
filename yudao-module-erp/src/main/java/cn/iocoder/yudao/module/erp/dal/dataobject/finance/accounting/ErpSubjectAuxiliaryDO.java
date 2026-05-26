package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 会计科目辅助核算关联 DO
 *
 * 一个科目可挂多种辅助核算（N:M）。凭证录入时按所选科目反查，决定弹出哪几个核算项下拉。
 *
 * @author Claude
 */
@TableName("erp_subject_auxiliary")
@KeySequence("erp_subject_auxiliary_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSubjectAuxiliaryDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 关联 {@link ErpAccountingSubjectDO#getId()}
     */
    private Long subjectId;
    /**
     * 冗余科目编码，方便排查
     */
    private String subjectCode;
    /**
     * 辅助核算类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAuxiliaryTypeEnum}
     */
    private String auxiliaryType;
    /**
     * 排序
     */
    private Integer sort;

}
