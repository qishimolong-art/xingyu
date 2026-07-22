package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

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
 * ERP warehouse sale department permission.
 */
@TableName("erp_warehouse_sale_dept_permission")
@KeySequence("erp_warehouse_sale_dept_permission_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpWarehouseSaleDeptPermissionDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * Warehouse id.
     */
    private Long warehouseId;

    /**
     * Sales department id.
     */
    private Long deptId;

}
