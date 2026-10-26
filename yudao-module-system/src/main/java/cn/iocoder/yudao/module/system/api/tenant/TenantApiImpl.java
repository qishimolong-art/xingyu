package cn.iocoder.yudao.module.system.api.tenant;

import cn.iocoder.yudao.framework.common.biz.system.tenant.TenantCommonApi;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import cn.iocoder.yudao.module.system.service.tenant.TenantVisitScopeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 多租户的 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class TenantApiImpl implements TenantCommonApi {

    @Resource
    private TenantService tenantService;
    @Resource
    private TenantVisitScopeService tenantVisitScopeService;

    @Override
    public List<Long> getTenantIdList() {
        return tenantService.getTenantIdList();
    }

    @Override
    public void validateTenant(Long id) {
        tenantService.validTenant(id);
    }

    @Override
    public void validateTenantVisit(Long userId, Long loginTenantId, Long visitTenantId) {
        tenantVisitScopeService.validateTenantVisit(userId, loginTenantId, visitTenantId);
    }

}
