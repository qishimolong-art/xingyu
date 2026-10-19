package cn.iocoder.yudao.module.erp.service.assistant.customer;

import lombok.Data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class CustomerAssistantPrincipal {
    private Long tenantId;
    private Long memberUserId;
    private Long customerId;
    private String customerName;
    private Integer priceLevel;
    private boolean priceVisible;
    private boolean orderEnabled;
    private Set<Long> departmentIds;

    public CustomerAssistantPrincipal(Long tenantId, Long memberUserId, Long customerId, String customerName,
                                      Integer priceLevel, boolean priceVisible, boolean orderEnabled,
                                      Set<Long> departmentIds) {
        this.tenantId = tenantId;
        this.memberUserId = memberUserId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.priceLevel = priceLevel;
        this.priceVisible = priceVisible;
        this.orderEnabled = orderEnabled;
        this.departmentIds = departmentIds == null ? Collections.emptySet() : new LinkedHashSet<>(departmentIds);
    }

    public Set<Long> getDepartmentIds() {
        return departmentIds == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(departmentIds));
    }
}
