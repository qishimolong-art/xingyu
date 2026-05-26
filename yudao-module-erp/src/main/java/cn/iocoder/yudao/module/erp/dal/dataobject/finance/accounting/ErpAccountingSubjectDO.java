package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 会计科目 DO（合并期初余额）
 *
 * @author Claude
 */
@TableName("erp_accounting_subject")
@KeySequence("erp_accounting_subject_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAccountingSubjectDO extends BaseDO {

    /**
     * 自增主键
     */
    @TableId
    private Long id;
    /**
     * 科目编码（如 1001 / 100201）
     */
    private String subjectCode;
    /**
     * 科目名称
     */
    private String subjectName;
    /**
     * 简称
     */
    private String shortName;
    /**
     * 科目大类
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpSubjectCategoryEnum}
     */
    private Integer subjectCategory;
    /**
     * 父科目编码（根节点为 NULL）
     */
    private String parentCode;
    /**
     * 层级（1=一级 2=二级 ...）
     */
    private Integer subjectLevel;
    /**
     * 是否末级（凭证只能选末级科目）
     */
    private Boolean isLeaf;
    /**
     * 余额方向：1=借 2=贷
     */
    private Integer balanceDirection;
    /**
     * 凭证类型：1=客户 2=连锁
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpSubjectVoucherTypeEnum}
     */
    private Integer voucherType;
    /**
     * 期初余额
     */
    private BigDecimal openingBalance;
    /**
     * 是否启用
     */
    private Boolean enable;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

}
