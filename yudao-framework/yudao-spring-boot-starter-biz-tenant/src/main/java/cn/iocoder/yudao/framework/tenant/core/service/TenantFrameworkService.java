package cn.iocoder.yudao.framework.tenant.core.service;

import java.util.List;

/**
 * Tenant 框架 Service 接口，定义获取租户信息
 *
 * @author 芋道源码
 */
public interface TenantFrameworkService {

    /**
     * 获得所有租户
     *
     * @return 租户编号数组
     */
    List<Long> getTenantIds();

    /**
     * 校验租户是否合法
     *
     * @param id 租户编号
     */
    void validTenant(Long id);

    /**
     * 校验平台用户是否可以临时访问目标租户。
     */
    void validTenantVisit(Long userId, Long loginTenantId, Long visitTenantId);

}
