package cn.iocoder.yudao.module.erp.service.assistant.customer;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomerAssistantPrincipalTest {
    @Test
    void departmentScopeMustNotBeMutableByCaller() {
        LinkedHashSet<Long> source=new LinkedHashSet<>(Set.of(10L,20L));
        CustomerAssistantPrincipal principal=new CustomerAssistantPrincipal(1L,2L,3L,"客户",4,true,true,source);
        source.add(30L);
        assertEquals(Set.of(10L,20L),principal.getDepartmentIds());
        assertThrows(UnsupportedOperationException.class,()->principal.getDepartmentIds().add(40L));
    }
}
