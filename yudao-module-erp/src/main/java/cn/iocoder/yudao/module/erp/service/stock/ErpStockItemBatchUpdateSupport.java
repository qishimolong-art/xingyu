package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
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
public class ErpStockItemBatchUpdateSupport {

    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    public void validateFieldPermission(String module, ErrorCode errorCode) {
        if (fieldPermissionMasker.isFieldHidden(module, "item_warehouseId")) {
            throw exception(errorCode);
        }
    }

    public ErpWarehouseDO validateTargetWarehouse(Long warehouseId) {
        List<ErpWarehouseDO> warehouses = DataPermissionUtils.executeIgnore(() ->
                warehouseService.validWarehouseList(Collections.singleton(warehouseId)));
        warehouseService.validateCurrentUserWarehousePermission(Collections.singleton(warehouseId));
        return CollUtil.getFirst(warehouses);
    }

    public void validateDocumentDeptAllowed(Long deptId, ErpWarehouseDO warehouse, String module, ErrorCode errorCode) {
        if (deptId == null || warehouse == null) {
            return;
        }
        Set<Long> allowedDeptIds = getAvailableDeptIds(warehouse, module);
        if (!allowedDeptIds.contains(deptId)) {
            throw exception(errorCode);
        }
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
        Set<Long> deptIds = getWarehouseAllowedDeptIds(warehouse);
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptySet();
        }
        applyDeptDataPermission(deptIds, module);
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptySet();
        }
        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(deptIds), DeptRespDTO::getId);
        deptIds.removeIf(deptId -> {
            DeptRespDTO dept = deptMap.get(deptId);
            return dept == null || !CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus());
        });
        return deptIds;
    }

    private Set<Long> getWarehouseAllowedDeptIds(ErpWarehouseDO warehouse) {
        if (warehouse == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        if (warehouse.getDeptId() != null) {
            deptIds.add(warehouse.getDeptId());
        }
        deptIds.addAll(warehouseService.getWarehouseSaleDeptIds(warehouse.getId()));
        return deptIds;
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
