package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomerAssistantToolRegistryTest {
    @Test
    void shouldExposeOnlyCustomerSafeTools() {
        AssistantProperties properties = new AssistantProperties();
        List<Map<String,Object>> tools=new CustomerAssistantToolRegistry(properties).definitions();
        Set<String> names=tools.stream().map(tool->(Map<?,?>)tool.get("function"))
                .map(function->String.valueOf(function.get("name"))).collect(Collectors.toSet());
        assertEquals(Set.of("query_customer_price","query_mall_stock","query_customer_orders","clarify_customer_intent"),names);
    }

    @Test
    void shouldApplyCapabilityConfigurationToRegistrationAndExecution() {
        AssistantProperties properties = new AssistantProperties();
        properties.getCustomerApp().setAllowedCapabilities(List.of("MALL_STOCK", "CLARIFICATION"));
        CustomerAssistantToolRegistry registry = new CustomerAssistantToolRegistry(properties);

        Set<String> names = registry.definitions().stream().map(tool -> (Map<?, ?>) tool.get("function"))
                .map(function -> String.valueOf(function.get("name"))).collect(Collectors.toSet());
        assertEquals(Set.of("query_mall_stock", "clarify_customer_intent"), names);
        assertFalse(registry.isAllowed(CustomerAssistantToolRegistry.PRICE));
        assertFalse(registry.isAllowed("query_any_sql"));
    }
}
