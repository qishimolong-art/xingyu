package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("erp_other_receivable_item")
@KeySequence("erp_other_receivable_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpOtherReceivableItemDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 其他应收单 ID
     */
    private Long receivableId;
    /**
     * 摘要
     */
    private String summary;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 备注
     */
    private String remark;

}
