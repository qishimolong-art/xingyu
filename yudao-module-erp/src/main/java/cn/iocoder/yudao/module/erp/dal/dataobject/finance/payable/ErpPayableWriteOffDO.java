package cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_payable_writeoff")
@KeySequence("erp_payable_writeoff_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPayableWriteOffDO extends BaseDO {

    @TableId
    private Long id;
    private Long supplierId;
    private Integer bizType;
    private Long bizId;
    private String bizNo;
    private BigDecimal writeOffAmount;
    private String remark;
    private LocalDateTime writeOffTime;
    private Long operatorUserId;

}
