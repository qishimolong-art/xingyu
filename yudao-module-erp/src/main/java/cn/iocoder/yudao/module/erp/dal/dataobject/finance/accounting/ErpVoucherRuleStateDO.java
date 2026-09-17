package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("erp_voucher_rule_state")
public class ErpVoucherRuleStateDO extends BaseDO {
    @TableId private Long id;
    private String stateKey;
    private Long version;
    private String payload;
}
