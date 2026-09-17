package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
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

    public PageResult<DeptSimpleRespVO> getAvailableDeptSimplePage(Long supplierId, String formKey,
                                                                   PageParam pageParam) {
        ErpSupplierDO supplier = supplierService.validateSupplier(supplierId);
        Set<Long> availableDeptIds = getAvailableDeptIds(supplier, formKey);
        if (CollUtil.isEmpty(availableDeptIds)) {
            return PageResult.empty();
        }
        PageResult<DeptRespDTO> page = deptApi.getDeptSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageParam.getKeyword(), availableDeptIds, pageParam);
        return new PageResult<>(toDeptSimpleRespVOList(page.getList()), page.getTotal());
    }

    public List<DeptSimpleRespVO> getDataPermissionDeptSimpleList(String formKey) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), formKey);
        if (Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            return toDeptSimpleRespVOList(deptApi.getDeptListByStatus(CommonStatusEnum.ENABLE.getStatus()));
        }

        Set<Long> permissionDeptIds = resolvePermissionDeptIds(permission);
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
        Set<Long> availableDeptIds = getAvailableDeptIds(supplier, formKey);
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptyList();
        }

        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(availableDeptIds), DeptRespDTO::getId);
        return availableDeptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .collect(Collectors.toList());
    }

    private Set<Long> getAvailableDeptIds(ErpSupplierDO supplier, String formKey) {
        Set<Long> supplierDeptIds = new LinkedHashSet<>();
        if (supplier.getDeptId() != null) {
            supplierDeptIds.add(supplier.getDeptId());
        }
        supplierDeptIds.addAll(supplierService.getSupplierDeptMap(Collections.singleton(supplier.getId()))
                .getOrDefault(supplier.getId(), Collections.emptyList()));
        if (CollUtil.isEmpty(supplierDeptIds)) {
            return Collections.emptySet();
        }

        Set<Long> availableDeptIds = new LinkedHashSet<>(supplierDeptIds);
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), formKey);
        if (!Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            Set<Long> permissionDeptIds = resolvePermissionDeptIds(permission);
            if (CollUtil.isEmpty(permissionDeptIds)) {
                return Collections.emptySet();
            }
            availableDeptIds.retainAll(permissionDeptIds);
        }
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptySet();
        }
        return availableDeptIds;
    }

    private List<DeptSimpleRespVO> toDeptSimpleRespVOList(List<DeptRespDTO> depts) {
        return depts.stream()
                .map(dept -> new DeptSimpleRespVO(dept.getId(), dept.getName(), dept.getParentId()))
                .collect(Collectors.toList());
    }

    private Set<Long> resolvePermissionDeptIds(DeptDataPermissionRespDTO permission) {
        if (permission == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        if (permission.getDeptIds() != null) {
            deptIds.addAll(permission.getDeptIds());
        }
        if (!Boolean.TRUE.equals(permission.getSelf())) {
            return deptIds;
        }
        Long loginUserId = getLoginUserId();
        if (loginUserId != null) {
            Set<Long> userDeptIds = permissionApi.getDeptIdsByUserId(loginUserId);
            if (userDeptIds != null) {
                deptIds.addAll(userDeptIds);
            }
        }
        return deptIds;
    }

}
