package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_pre_payment")
@KeySequence("erp_pre_payment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPrePaymentDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private Integer status;
    private LocalDateTime bizTime;
    /**
     * 往来单位类型：1=客户 2=供应商 3=员工
     */
    private Integer partyType;
    private Long partyId;
    private String partyName;
    private Long accountId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal actualAmount;
    private String remark;
    private String fileUrl;

}
