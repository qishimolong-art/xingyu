package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ERP 销售直接开单禁用部门 DO
 */
@TableName("erp_sale_direct_forbidden_dept")
@KeySequence("erp_sale_direct_forbidden_dept_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSaleDirectForbiddenDeptDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 禁止直接做销售单据的部门 ID
     */
    private Long deptId;

}
