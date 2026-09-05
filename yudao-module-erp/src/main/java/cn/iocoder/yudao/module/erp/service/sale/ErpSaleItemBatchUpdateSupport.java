package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Component
public class ErpSaleItemBatchUpdateSupport {

    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    public void validateFieldPermission(String module, boolean updateWarehouse, boolean updateDept,
                                        ErrorCode errorCode) {
        validateFieldPermission(module, updateWarehouse, updateDept, false, errorCode);
    }

    public void validateFieldPermission(String module, boolean updateWarehouse, boolean updateDept,
                                        boolean updateProductPrice, ErrorCode errorCode) {
        if (updateWarehouse && fieldPermissionMasker.isFieldHidden(module, "item_warehouseId")) {
            throw exception(errorCode);
        }
        if (updateDept && (fieldPermissionMasker.isFieldHidden(module, "item_deptId")
                || fieldPermissionMasker.isFieldHidden(module, "item_warehouseDeptName"))) {
            throw exception(errorCode);
        }
        if (updateProductPrice && (fieldPermissionMasker.isFieldHidden(module, "item_productPrice")
                || fieldPermissionMasker.isFieldHidden(module, "productPrice"))) {
            throw exception(errorCode);
        }
    }

    public ErpWarehouseDO validateTargetWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        return CollUtil.getFirst(warehouseService.validSaleWarehouseList(Collections.singleton(warehouseId)));
    }

    public Long resolveTargetDeptId(ErpWarehouseDO targetWarehouse, Long deptId, String module,
                                    ErrorCode errorCode) {
        if (targetWarehouse == null) {
            if (deptId != null) {
                validateDeptVisible(deptId, module, errorCode);
            }
            return deptId;
        }
        Set<Long> availableDeptIds = getAvailableDeptIds(targetWarehouse, module);
        if (deptId != null) {
            if (!availableDeptIds.contains(deptId)) {
                throw exception(errorCode);
            }
            return deptId;
        }
        if (availableDeptIds.size() == 1) {
            return availableDeptIds.iterator().next();
        }
        throw exception(errorCode);
    }

    public void validateWarehouseDept(Long warehouseId, Long deptId, String module, ErrorCode errorCode) {
        if (isCurrentUserSelectableWarehouseOwnerDept(warehouseId, deptId)) {
            validateDeptEnabled(deptId, errorCode);
            return;
        }
        validateDeptVisible(deptId, module, errorCode);
        warehouseService.validateWarehouseSaleAllowedForDept(warehouseId, deptId);
    }

    public List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId, String module) {
        ErpWarehouseDO warehouse = validateTargetWarehouse(warehouseId);
        if (warehouse == null) {
            return Collections.emptyList();
        }
        Set<Long> availableDeptIds = getAvailableDeptIds(warehouse, module);
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptyList();
        }
        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(availableDeptIds), DeptRespDTO::getId);
        return availableDeptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .map(dept -> new DeptSimpleRespVO(dept.getId(), dept.getName(), dept.getParentId()))
                .collect(Collectors.toList());
    }

    private Set<Long> getAvailableDeptIds(ErpWarehouseDO warehouse, String module) {
        Set<Long> availableDeptIds = getWarehouseAllowedDeptIds(warehouse, module);
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptySet();
        }
        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(availableDeptIds), DeptRespDTO::getId);
        availableDeptIds.removeIf(deptId -> {
            DeptRespDTO dept = deptMap.get(deptId);
            return dept == null || !CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus());
        });
        return availableDeptIds;
    }

    private Set<Long> getWarehouseAllowedDeptIds(ErpWarehouseDO warehouse, String module) {
        if (warehouse == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        if (warehouse.getDeptId() != null) {
            deptIds.add(warehouse.getDeptId());
        }
        Set<Long> saleDeptIds = getWarehouseSaleDeptIds(warehouse);
        if (CollUtil.isNotEmpty(saleDeptIds)) {
            applyDeptDataPermission(saleDeptIds, module);
            deptIds.addAll(saleDeptIds);
        }
        return deptIds;
    }

    private void validateDeptVisible(Long deptId, String module, ErrorCode errorCode) {
        if (deptId == null) {
            throw exception(errorCode);
        }
        Set<Long> availableDeptIds = new LinkedHashSet<>();
        availableDeptIds.add(deptId);
        applyDeptDataPermission(availableDeptIds, module);
        if (!availableDeptIds.contains(deptId)) {
            throw exception(errorCode);
        }
        validateDeptEnabled(deptId, errorCode);
    }

    private void validateDeptEnabled(Long deptId, ErrorCode errorCode) {
        DeptRespDTO dept = CollUtil.getFirst(deptApi.getDeptList(Collections.singleton(deptId)));
        if (dept == null || !CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus())) {
            throw exception(errorCode);
        }
    }

    private boolean isCurrentUserSelectableWarehouseOwnerDept(Long warehouseId, Long deptId) {
        if (warehouseId == null || deptId == null) {
            return false;
        }
        ErpWarehouseDO warehouse;
        try {
            warehouse = CollUtil.getFirst(warehouseService.validSaleWarehouseList(Collections.singleton(warehouseId)));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        return warehouse != null && deptId.equals(warehouse.getDeptId());
    }

    private Set<Long> getWarehouseSaleDeptIds(ErpWarehouseDO warehouse) {
        if (warehouse.getId() == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(warehouseService.getWarehouseSaleDeptIds(warehouse.getId()));
    }

    private void applyDeptDataPermission(Set<Long> deptIds, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), module);
        if (Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            return;
        }
        Set<Long> permissionDeptIds = permission != null ? permission.getDeptIds() : Collections.emptySet();
        if (CollUtil.isEmpty(permissionDeptIds)) {
            deptIds.clear();
            return;
        }
        deptIds.retainAll(permissionDeptIds);
    }

}
