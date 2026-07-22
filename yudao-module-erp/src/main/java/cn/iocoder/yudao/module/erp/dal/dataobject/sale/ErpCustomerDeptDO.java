package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ERP customer department relation.
 */
@TableName("erp_customer_dept")
@KeySequence("erp_customer_dept_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpCustomerDeptDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * Customer id.
     */
    private Long customerId;

    /**
     * Department id.
     */
    private Long deptId;

}
