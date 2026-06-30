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
 * ERP user warehouse permission DO.
 */
@TableName("erp_user_warehouse_permission")
@KeySequence("erp_user_warehouse_permission_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpUserWarehousePermissionDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * User id.
     */
    private Long userId;

    /**
     * Warehouse id.
     */
    private Long warehouseId;

}
