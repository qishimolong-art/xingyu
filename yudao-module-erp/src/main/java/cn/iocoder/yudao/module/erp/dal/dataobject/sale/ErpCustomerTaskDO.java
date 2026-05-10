package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("erp_customer_task")
@KeySequence("erp_customer_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCustomerTaskDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;
    private Integer year;
    private Integer month;
    private String taskLevel;
    private BigDecimal taskAmount;
    private String remark;

}
