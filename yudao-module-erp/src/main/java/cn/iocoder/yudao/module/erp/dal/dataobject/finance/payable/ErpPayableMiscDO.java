package cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable;

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
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_payable_misc")
@KeySequence("erp_payable_misc_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpPayableMiscDO extends BaseDO {

    @TableId
    private Long id;

    private String no;

    private Integer status;

    private LocalDateTime bizTime;

    private Long supplierId;

    private Long accountId;

    private BigDecimal amount;

    private String remark;

    private String fileUrl;

    private Long deptId;

    private Long handlerId;

}
