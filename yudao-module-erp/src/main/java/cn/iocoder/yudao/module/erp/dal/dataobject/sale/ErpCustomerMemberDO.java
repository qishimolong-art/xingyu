package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ERP customer mini-app member authorization.
 */
@TableName("erp_customer_member")
@KeySequence("erp_customer_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpCustomerMemberDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * Customer id.
     */
    private Long customerId;

    /**
     * Mini-app member user id.
     */
    private Long memberUserId;

    /**
     * Mobile snapshot when authorization is created.
     */
    private String mobile;

    /**
     * Status.
     *
     * @see CommonStatusEnum
     */
    private Integer status;

    /**
     * Remark.
     */
    private String remark;

}
