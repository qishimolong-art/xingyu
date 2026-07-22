package cn.iocoder.yudao.module.erp.dal.dataobject.product;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ERP product department relation.
 */
@TableName("erp_product_dept")
@KeySequence("erp_product_dept_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpProductDeptDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * Product id.
     */
    private Long productId;

    /**
     * Department id.
     */
    private Long deptId;

}
