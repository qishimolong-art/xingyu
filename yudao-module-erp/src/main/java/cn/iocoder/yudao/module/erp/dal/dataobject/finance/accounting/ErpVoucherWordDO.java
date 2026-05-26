package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 凭证字字典 DO
 *
 * @author Claude
 */
@TableName("erp_voucher_word")
@KeySequence("erp_voucher_word_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVoucherWordDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 凭证字代码（"记"）
     */
    private String code;
    /**
     * 名称（"记账凭证"）
     */
    private String name;
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
