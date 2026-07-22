package cn.iocoder.yudao.module.erp.service.stock.bo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Permission scope used by one transfer-out query request.
 */
public class ErpStockTransferOutPermissionScope {

    private final boolean all;
    private final Set<Long> deptIds;

    public ErpStockTransferOutPermissionScope(boolean all, Collection<Long> deptIds) {
        this.all = all;
        if (deptIds == null) {
            this.deptIds = Collections.emptySet();
            return;
        }
        Set<Long> normalizedDeptIds = new LinkedHashSet<>();
        deptIds.forEach(deptId -> {
            if (deptId != null) {
                normalizedDeptIds.add(deptId);
            }
        });
        this.deptIds = Collections.unmodifiableSet(normalizedDeptIds);
    }

    public boolean isAll() {
        return all;
    }

    public Set<Long> getDeptIds() {
        return deptIds;
    }

}
