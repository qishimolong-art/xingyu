package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;

import java.util.Collection;
import java.util.Collections;

/** 财务报表显式数据权限范围。 */
public class ErpFinanceVisibleScope {

    private final boolean all;
    private final Collection<Long> deptIds;
    private final Long selfUserId;

    public ErpFinanceVisibleScope(boolean all, Collection<Long> deptIds, Long selfUserId) {
        this.all = all;
        this.deptIds = deptIds == null ? Collections.emptyList() : deptIds;
        this.selfUserId = selfUserId;
    }

    public static ErpFinanceVisibleScope from(DeptDataPermissionRespDTO permission, Long loginUserId) {
        if (permission == null || loginUserId == null) {
            return null;
        }
        return new ErpFinanceVisibleScope(Boolean.TRUE.equals(permission.getAll()), permission.getDeptIds(),
                Boolean.TRUE.equals(permission.getSelf()) ? loginUserId : null);
    }

    public boolean hasAccess() {
        return all || !deptIds.isEmpty() || selfUserId != null;
    }

    public boolean isAll() { return all; }
    public Collection<Long> getDeptIds() { return deptIds; }
    public Long getSelfUserId() { return selfUserId; }
}
