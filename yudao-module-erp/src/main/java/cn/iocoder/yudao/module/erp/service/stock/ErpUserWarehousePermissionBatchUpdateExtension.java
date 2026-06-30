package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserBatchUpdateReqVO;
import cn.iocoder.yudao.module.system.service.user.AdminUserBatchUpdateExtension;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * Batch update extension for ERP user warehouse permissions.
 */
@Service
public class ErpUserWarehousePermissionBatchUpdateExtension implements AdminUserBatchUpdateExtension {

    private static final String WAREHOUSE_PERMISSION_UPDATE = "erp:warehouse-permission:update";

    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public void validate(UserBatchUpdateReqVO reqVO, Set<Long> userIds) {
        if (!Boolean.TRUE.equals(reqVO.getUpdateWarehousePermissions())) {
            return;
        }
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || !permissionApi.hasAnyPermissions(loginUserId, WAREHOUSE_PERMISSION_UPDATE)) {
            throw exception(FORBIDDEN);
        }
        validateMode(reqVO.getWarehousePermissionMode());
        Set<Long> warehouseIds = normalizeWarehouseIds(reqVO.getWarehouseIds());
        if (!UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_REPLACE.equals(reqVO.getWarehousePermissionMode())
                && CollUtil.isEmpty(warehouseIds)) {
            throw invalidParamException("请选择仓库");
        }
        warehouseService.validWarehouseList(warehouseIds);
        validateAssignableWarehouses(warehouseIds);
        reqVO.setWarehouseIds(warehouseIds);
    }

    @Override
    public void update(Long userId, UserBatchUpdateReqVO reqVO) {
        if (!Boolean.TRUE.equals(reqVO.getUpdateWarehousePermissions())) {
            return;
        }
        Set<Long> warehouseIds = normalizeWarehouseIds(reqVO.getWarehouseIds());
        switch (reqVO.getWarehousePermissionMode()) {
            case UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_REPLACE:
                warehouseService.updateUserWarehousePermissions(userId, warehouseIds);
                break;
            case UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_ADD:
                warehouseService.updateUserWarehousePermissions(userId, mergeUserWarehouseIds(userId, warehouseIds));
                break;
            case UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_REMOVE:
                warehouseService.updateUserWarehousePermissions(userId, removeUserWarehouseIds(userId, warehouseIds));
                break;
            default:
                throw invalidParamException("Warehouse permission update mode is invalid");
        }
    }

    private void validateMode(String mode) {
        if (!UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_REPLACE.equals(mode)
                && !UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_ADD.equals(mode)
                && !UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_REMOVE.equals(mode)) {
            throw invalidParamException("仓库权限修改模式不正确");
        }
    }

    private void validateAssignableWarehouses(Set<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return;
        }
        if (warehouseService.hasCurrentUserAllWarehousePermission()) {
            return;
        }
        Set<Long> assignableWarehouseIds = warehouseService.getAssignableWarehouseList().stream()
                .map(warehouse -> warehouse == null ? null : warehouse.getId())
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!assignableWarehouseIds.containsAll(warehouseIds)) {
            throw exception(FORBIDDEN);
        }
    }

    private Set<Long> mergeUserWarehouseIds(Long userId, Set<Long> warehouseIds) {
        Set<Long> mergedWarehouseIds = new LinkedHashSet<>(warehouseService.getUserWarehouseIds(userId));
        mergedWarehouseIds.addAll(warehouseIds);
        return mergedWarehouseIds;
    }

    private Set<Long> removeUserWarehouseIds(Long userId, Set<Long> warehouseIds) {
        Set<Long> remainWarehouseIds = new LinkedHashSet<>(warehouseService.getUserWarehouseIds(userId));
        remainWarehouseIds.removeAll(warehouseIds);
        return remainWarehouseIds;
    }

    private Set<Long> normalizeWarehouseIds(Collection<Long> warehouseIds) {
        if (warehouseIds == null) {
            return Collections.emptySet();
        }
        return warehouseIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

}
