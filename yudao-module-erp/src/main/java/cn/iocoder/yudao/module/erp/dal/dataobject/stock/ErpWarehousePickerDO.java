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
 * ERP warehouse picker assignment.
 */
@TableName("erp_warehouse_picker")
@KeySequence("erp_warehouse_picker_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpWarehousePickerDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * Warehouse id.
     */
    private Long warehouseId;

    /**
     * Picker user id.
     */
    private Long userId;

}
