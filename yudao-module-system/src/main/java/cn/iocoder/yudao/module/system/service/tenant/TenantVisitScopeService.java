package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.tenant.TenantRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.TenantVisitScopeRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.TenantVisitScopeSaveReqVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.UserTenantVisitScopeDO;

public interface TenantVisitScopeService {

    String PERMISSION_TENANT_VISIT = "system:tenant:visit";
    String PERMISSION_TENANT_VISIT_SCOPE = "system:tenant:visit-scope";
    String SCOPE_ALL = "ALL";
    String SCOPE_CUSTOM = "CUSTOM";
    String ROLE_CODE_TENANT_VISIT_USER = "tenant_visit_user";
    long SYSTEM_TENANT_ID = 1L;

    TenantVisitScopeRespVO getUserTenantVisitScope(Long userId);

    void saveUserTenantVisitScope(Long userId, TenantVisitScopeSaveReqVO scope);

    void validateUserTenantVisitScopes(Collection<Long> userIds, TenantVisitScopeSaveReqVO scope);

    Long getManagedTenantVisitRoleId();

    Set<Long> preserveManagedTenantVisitRole(Long userId, Collection<Long> roleIds);

    List<TenantRespVO> getCurrentUserVisitTenantList(Long userId, Long loginTenantId);

    void validateTenantVisit(Long userId, Long loginTenantId, Long visitTenantId);

    boolean hasEffectiveTenantVisitPermission(Long userId);

    void reconcileUserScope(Long userId);

    void reconcileUserScopes(Collection<Long> userIds);

    void reconcileUserScopesByRoleId(Long roleId);

    void reconcileAllUserScopes();

    void clearUserScope(Long userId);

    UserTenantVisitScopeDO getUserScopeFromCache(Long userId);

}
