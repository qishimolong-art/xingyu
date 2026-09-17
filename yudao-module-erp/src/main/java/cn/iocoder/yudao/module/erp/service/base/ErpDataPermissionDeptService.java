package cn.iocoder.yudao.module.erp.service.base;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
@Validated
public class ErpDataPermissionDeptService {

    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    public List<DeptSimpleRespVO> getDeptSimpleList(String formKey) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), formKey);
        if (Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            return toDeptSimpleRespVOList(deptApi.getDeptListByStatus(CommonStatusEnum.ENABLE.getStatus()));
        }

        return getEnabledDeptSimpleList(permission != null ? permission.getDeptIds() : Collections.emptySet());
    }

    public PageResult<DeptSimpleRespVO> getDeptSimplePage(String formKey, PageParam pageParam) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), formKey);
        Collection<Long> deptIds = null;
        if (!Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            deptIds = resolvePermissionDeptIds(permission);
            if (CollUtil.isEmpty(deptIds)) {
                return PageResult.empty();
            }
        }
        PageResult<DeptRespDTO> page = deptApi.getDeptSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageParam.getKeyword(), deptIds, pageParam);
        return new PageResult<>(toDeptSimpleRespVOList(page.getList()), page.getTotal());
    }

    public List<DeptSimpleRespVO> getEnabledDeptSimpleList(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }

        List<Long> distinctDeptIds = deptIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(distinctDeptIds)) {
            return Collections.emptyList();
        }

        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(distinctDeptIds), DeptRespDTO::getId);
        List<DeptRespDTO> depts = distinctDeptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .collect(Collectors.toList());
        return toDeptSimpleRespVOList(depts);
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
        if (loginUserId == null) {
            return deptIds;
        }
        Set<Long> userDeptIds = permissionApi.getDeptIdsByUserId(loginUserId);
        if (userDeptIds != null) {
            deptIds.addAll(userDeptIds);
        }
        return deptIds;
    }

    private List<DeptSimpleRespVO> toDeptSimpleRespVOList(List<DeptRespDTO> depts) {
        return depts.stream()
                .map(dept -> new DeptSimpleRespVO(dept.getId(), dept.getName(), dept.getParentId()))
                .collect(Collectors.toList());
    }

}
