package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ERP supplier department relation.
 */
@TableName("erp_supplier_dept")
@KeySequence("erp_supplier_dept_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSupplierDeptDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * Supplier id.
     */
    private Long supplierId;

    /**
     * Department id.
     */
    private Long deptId;

}
