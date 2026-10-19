package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantProperties;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class CustomerAssistantContextService {
    @Resource private AssistantProperties properties;
    @Resource private ErpCustomerMemberApi customerMemberApi;
    @Resource private ErpCustomerDeptPermissionService customerDeptPermissionService;

    public CustomerAssistantPrincipal current() {
        if (!properties.getCustomerApp().isEnabled())
            throw new AssistantFailure("DISABLED", "智能客服问数功能尚未开放");
        Long memberUserId = getLoginUserId();
        if (memberUserId == null) throw new AssistantFailure("FORBIDDEN", "请先登录");
        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.getCustomerMemberAuth(memberUserId);
        if (auth == null || !Boolean.TRUE.equals(auth.getAuthorized()) || auth.getCustomerId() == null)
            throw new AssistantFailure("CUSTOMER_UNBOUND", "当前账号尚未绑定有效客户，请联系业务人员或转人工客服");
        LinkedHashSet<Long> deptIds = customerDeptPermissionService.getCustomerAppAvailableDeptList(auth.getCustomerId())
                .stream().map(DeptRespDTO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (deptIds.isEmpty()) throw new AssistantFailure("NO_AVAILABLE_DEPARTMENT", "当前客户暂无可用销售部门，请联系业务人员或转人工客服");
        return new CustomerAssistantPrincipal(TenantContextHolder.getRequiredTenantId(), memberUserId,
                auth.getCustomerId(), auth.getCustomerName(), auth.getPriceLevel(),
                Boolean.TRUE.equals(auth.getPriceVisible()), Boolean.TRUE.equals(auth.getOrderEnabled()), deptIds);
    }
}
