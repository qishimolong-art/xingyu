package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * ERP customer department credit config.
 */
@TableName("erp_customer_dept_credit")
@KeySequence("erp_customer_dept_credit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpCustomerDeptCreditDO extends BaseDO {

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

    /**
     * Whether credit control is enabled for this customer department.
     */
    private Boolean creditEnabled;

    /**
     * Credit limit.
     */
    private BigDecimal creditLimit;

    /**
     * Credit term days.
     */
    private Integer creditTermDays;

    /**
     * Remark.
     */
    private String remark;

}
