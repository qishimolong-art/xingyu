package cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_receivable_writeoff")
@KeySequence("erp_receivable_writeoff_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpReceivableWriteOffDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;
    private Integer bizType;
    private Long bizId;
    private String bizNo;
    private BigDecimal writeOffAmount;
    private String remark;
    private LocalDateTime writeOffTime;
    private Long operatorUserId;

}
