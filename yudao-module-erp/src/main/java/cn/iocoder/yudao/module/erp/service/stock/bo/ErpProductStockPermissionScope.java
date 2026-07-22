package cn.iocoder.yudao.module.erp.service.stock.bo;

import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Product-stock data permission resolved to warehouse ownership.
 *
 * <p>Department warehouses allow every stock row in the warehouse. The self scope is stricter:
 * it only allows rows created by the current user in warehouses owned by the user's department.</p>
 */
@Data
@AllArgsConstructor
public class ErpProductStockPermissionScope {

    private boolean all;
    private Set<Long> departmentWarehouseIds;
    private Set<Long> selfWarehouseIds;
    private Long userId;

    public static ErpProductStockPermissionScope empty(Long userId) {
        return new ErpProductStockPermissionScope(false, Collections.emptySet(), Collections.emptySet(), userId);
    }

    public Set<Long> getVisibleWarehouseIds() {
        Set<Long> warehouseIds = new LinkedHashSet<>(departmentWarehouseIds);
        warehouseIds.addAll(selfWarehouseIds);
        return warehouseIds;
    }

    public boolean canAccessWarehouse(Long warehouseId) {
        return warehouseId != null && (all || departmentWarehouseIds.contains(warehouseId)
                || selfWarehouseIds.contains(warehouseId));
    }

    public boolean canAccessStock(ErpStockDO stock) {
        if (stock == null || stock.getWarehouseId() == null) {
            return false;
        }
        if (all || departmentWarehouseIds.contains(stock.getWarehouseId())) {
            return true;
        }
        return selfWarehouseIds.contains(stock.getWarehouseId()) && userId != null
                && Objects.equals(String.valueOf(userId), stock.getCreator());
    }

}
