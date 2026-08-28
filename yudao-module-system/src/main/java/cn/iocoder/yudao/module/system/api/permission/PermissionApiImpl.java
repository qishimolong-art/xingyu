package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionCreateOrUpdateReqDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 权限 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class PermissionApiImpl implements PermissionApi {

    @Resource
    private PermissionService permissionService;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private FieldDefinitionMapper fieldDefinitionMapper;

    @Override
    public Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        return permissionService.getUserRoleIdListByRoleId(roleIds);
    }

    @Override
    public List<String> getCurrentUserHiddenFields(String module) {
        return permissionService.getCurrentUserHiddenFields(module);
    }

    @Override
    public List<String> getCurrentUserHiddenFields(String module, Long businessDeptId) {
        return permissionService.getCurrentUserHiddenFields(module, businessDeptId);
    }

    @Override
    public List<String> getCurrentUserHiddenFields(String module, Long businessDeptId,
                                                   boolean includeProductPricePermission) {
        return permissionService.getCurrentUserHiddenFields(module, businessDeptId, includeProductPricePermission);
    }

    @Override
    public List<String> getCurrentUserHiddenFields(String module, Long businessDeptId,
                                                   boolean includeProductPricePermission, Integer customerPriceLevel) {
        return permissionService.getCurrentUserHiddenFields(module, businessDeptId,
                includeProductPricePermission, customerPriceLevel);
    }

    @Override
    public void createOrUpdateFieldDefinitions(List<FieldDefinitionCreateOrUpdateReqDTO> definitions) {
        permissionService.createOrUpdateFieldDefinitions(definitions);
    }

    @Override
    public void deleteFieldDefinitions(String module, List<String> fieldKeys) {
        permissionService.deleteFieldDefinitions(module, fieldKeys);
    }

    @Override
    public List<FieldDefinitionRespDTO> getFieldDefinitions(String module, String fieldGroup) {
        return CollectionUtils.convertList(
                fieldDefinitionMapper.selectListByModuleAndGroup(module, fieldGroup),
                definition -> {
                    FieldDefinitionRespDTO result = new FieldDefinitionRespDTO();
                    result.setFieldKey(definition.getFieldKey());
                    result.setFieldLabel(definition.getFieldLabel());
                    result.setSort(definition.getSort());
                    return result;
                });
    }

    @Override
    public List<FieldDefinitionRespDTO> getFieldDefinitions(String module) {
        return CollectionUtils.convertList(
                fieldDefinitionMapper.selectListByModule(module),
                definition -> {
                    FieldDefinitionRespDTO result = new FieldDefinitionRespDTO();
                    result.setFieldKey(definition.getFieldKey());
                    result.setFieldLabel(definition.getFieldLabel());
                    result.setSort(definition.getSort());
                    return result;
                });
    }

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        return permissionService.hasAnyPermissions(userId, permissions);
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        return permissionService.hasAnyRoles(userId, roles);
    }

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        return permissionService.getDeptDataPermission(userId);
    }

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId, String formKey) {
        return permissionService.getDeptDataPermission(userId, formKey);
    }

    @Override
    public Set<Long> getDeptIdsByUserId(Long userId) {
        return adminUserService.getUserDeptIdListByUserId(userId);
    }

    @Override
    public Set<Long> getUserIdsByDeptIds(Collection<Long> deptIds) {
        return CollectionUtils.convertSet(adminUserService.getUserListByDeptIds(deptIds), AdminUserDO::getId);
    }

}
