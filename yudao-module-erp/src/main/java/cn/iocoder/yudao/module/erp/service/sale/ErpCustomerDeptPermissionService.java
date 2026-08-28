package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
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
 * Customer department availability helper for finance and sale documents.
 */
@Service
@Validated
public class ErpCustomerDeptPermissionService {

    @Resource
    private ErpCustomerService customerService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    public List<DeptSimpleRespVO> getAvailableDeptSimpleList(Long customerId, String formKey) {
        return toDeptSimpleRespVOList(getAvailableDeptList(customerId, formKey));
    }

    public List<DeptRespDTO> getCustomerAppAvailableDeptList(Long customerId) {
        Set<Long> availableDeptIds = new LinkedHashSet<>(
                customerService.getCustomerSaleDeptIdsIgnoreDataPermission(customerId));
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptyList();
        }

        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(availableDeptIds), DeptRespDTO::getId);
        return availableDeptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .collect(Collectors.toList());
    }

    public boolean hasAvailableDept(Long customerId, Long deptId, String formKey) {
        if (customerId == null || deptId == null) {
            return true;
        }
        return getAvailableDeptList(customerId, formKey).stream()
                .anyMatch(dept -> deptId.equals(dept.getId()));
    }

    private List<DeptRespDTO> getAvailableDeptList(Long customerId, String formKey) {
        Set<Long> availableDeptIds = new LinkedHashSet<>(customerService.getCustomerSaleDeptIds(customerId));
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptyList();
        }

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
