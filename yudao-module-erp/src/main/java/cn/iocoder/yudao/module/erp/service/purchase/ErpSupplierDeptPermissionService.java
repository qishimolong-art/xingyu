package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * Supplier department availability helper for purchase documents.
 */
@Service
@Validated
public class ErpSupplierDeptPermissionService {

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    public List<DeptSimpleRespVO> getAvailableDeptSimpleList(Long supplierId, String formKey) {
        ErpSupplierDO supplier = supplierService.validateSupplier(supplierId);
        return toDeptSimpleRespVOList(getAvailableDeptList(supplier, formKey));
    }

    public List<DeptSimpleRespVO> getDataPermissionDeptSimpleList(String formKey) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), formKey);
        if (Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            return toDeptSimpleRespVOList(deptApi.getDeptListByStatus(CommonStatusEnum.ENABLE.getStatus()));
        }

        Set<Long> permissionDeptIds = permission != null ? permission.getDeptIds() : Collections.emptySet();
        if (CollUtil.isEmpty(permissionDeptIds)) {
            return Collections.emptyList();
        }

        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(permissionDeptIds), DeptRespDTO::getId);
        List<DeptRespDTO> depts = permissionDeptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .collect(Collectors.toList());
        return toDeptSimpleRespVOList(depts);
    }

    public boolean hasAvailableDept(ErpSupplierDO supplier, Long deptId, String formKey) {
        if (supplier == null || deptId == null) {
            return true;
        }
        return getAvailableDeptList(supplier, formKey).stream()
                .anyMatch(dept -> deptId.equals(dept.getId()));
    }

    public List<DeptRespDTO> getAvailableDeptList(ErpSupplierDO supplier, String formKey) {
        Set<Long> supplierDeptIds = new LinkedHashSet<>();
        if (supplier.getDeptId() != null) {
            supplierDeptIds.add(supplier.getDeptId());
        }
        supplierDeptIds.addAll(supplierService.getSupplierDeptMap(Collections.singleton(supplier.getId()))
                .getOrDefault(supplier.getId(), Collections.emptyList()));
        if (CollUtil.isEmpty(supplierDeptIds)) {
            return Collections.emptyList();
        }

        Set<Long> availableDeptIds = new LinkedHashSet<>(supplierDeptIds);
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), formKey);
        if (!Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            Set<Long> permissionDeptIds = permission != null ? permission.getDeptIds() : Collections.emptySet();
            if (CollUtil.isEmpty(permissionDeptIds)) {
                return Collections.emptyList();
            }
            availableDeptIds.retainAll(permissionDeptIds);
        }
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptyList();
        }

        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(availableDeptIds), DeptRespDTO::getId);
        return availableDeptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .collect(Collectors.toList());
    }

    private List<DeptSimpleRespVO> toDeptSimpleRespVOList(List<DeptRespDTO> depts) {
        return depts.stream()
                .map(dept -> new DeptSimpleRespVO(dept.getId(), dept.getName(), dept.getParentId()))
                .collect(Collectors.toList());
    }

}
