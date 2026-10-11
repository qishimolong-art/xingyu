package cn.iocoder.yudao.module.erp.service.assistant;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import java.util.*;

/**
 * V2 能力注册中心。这里登记的是执行边界，不取代各业务 Service 内已有的数据和字段权限校验。
 */
@Component
public class AssistantCapabilityRegistry {

    @Resource private AssistantToolRegistry tools;
    private final Map<String, AssistantCapability> capabilities = createCapabilities();

    public AssistantCapability getRequired(String id) {
        AssistantCapability capability = capabilities.get(id);
        if (capability == null) {
            throw new AssistantFailure("CAPABILITY_UNAVAILABLE", "查询能力尚未登记或未开放");
        }
        return capability;
    }

    public Optional<AssistantCapability> find(String id) {
        return Optional.ofNullable(capabilities.get(id));
    }

    public List<AssistantCapability> all() {
        return Collections.unmodifiableList(new ArrayList<>(capabilities.values()));
    }

    @PostConstruct
    public void initialize() {
        validateAgainstToolRegistry();
    }

    /** 启动和测试时检查工具与能力清单未发生漂移。 */
    public void validateAgainstToolRegistry() {
        for (AssistantCapability capability : capabilities.values()) {
            if (!capability.isVirtual() && !tools.contains(capability.getExecutor())) {
                throw new IllegalStateException("能力执行器未在工具中心登记: " + capability.getId());
            }
        }
        for (String tool : tools.names()) {
            if (!capabilities.containsKey(tool)) {
                throw new IllegalStateException("工具未在能力中心登记: " + tool);
            }
        }
    }

    private static Map<String, AssistantCapability> createCapabilities() {
        Map<String, AssistantCapability> result = new LinkedHashMap<>();
        register(result, capability("query_verified_metric", "METRIC", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_master_data_stats", "MASTER_DATA", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("resolve_business_entity", "ENTITY", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_product_stock", "PRODUCT_STOCK", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_inventory_details", "INVENTORY", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_stock_movements", "STOCK_MOVEMENT", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_purchase_documents", "PURCHASE", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_purchase_fulfillment", "PURCHASE", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_sale_documents", "SALE", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_sale_fulfillment", "SALE", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_cash_documents", "FINANCE", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("query_account_balance", "FINANCE", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability(AssistantStage5BusinessQuery.MASTER_TOOL, "MASTER_DATA", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability(AssistantStage5BusinessQuery.ORGANIZATION_TOOL, "ORGANIZATION", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability(AssistantStage5BusinessQuery.STOCK_TOOL, "STOCK_DOCUMENT", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability(AssistantStage5BusinessQuery.SALE_TOOL, "SALE_DOCUMENT", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability(AssistantStage5BusinessQuery.PURCHASE_TOOL, "PURCHASE_DOCUMENT", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability(AssistantStage5BusinessQuery.FINANCE_TOOL, "FINANCE_DOCUMENT", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability(AssistantCompanyKnowledge.TOOL_NAME, "COMPANY_KNOWLEDGE", AssistantSemanticRequest.QueryMode.KNOWLEDGE));
        register(result, capability("query_semantic_schema", "SEMANTIC_DATASET", AssistantSemanticRequest.QueryMode.DATA));
        register(result, capability("execute_semantic_query", "SEMANTIC_DATASET", AssistantSemanticRequest.QueryMode.DATA));
        register(result, virtualCapability("model_routed", "ROUTING", AssistantSemanticRequest.QueryMode.DATA));
        register(result, virtualCapability(AssistantToolRegistry.CLARIFY_INTENT_TOOL, "ROUTING",
                AssistantSemanticRequest.QueryMode.DATA));
        register(result, virtualCapability(AssistantMixedQuery.TOOL_NAME, "MIXED", AssistantSemanticRequest.QueryMode.MIXED));
        return Collections.unmodifiableMap(result);
    }

    private static AssistantCapability capability(String id, String domain,
                                                   AssistantSemanticRequest.QueryMode queryMode) {
        return AssistantCapability.builder().id(id).domain(domain).queryMode(queryMode).executor(id)
                .requiredPermission("erp:assistant:query+DYNAMIC_SOURCE_PERMISSION")
                .rowLimit(200).timeoutMillis(60_000).auditable(true)
                .outputField("status").outputField("summary").outputField("rows").outputField("sources")
                .build();
    }

    private static AssistantCapability virtualCapability(String id, String domain,
                                                          AssistantSemanticRequest.QueryMode queryMode) {
        AssistantCapability capability = capability(id, domain, queryMode);
        capability.setVirtual(true);
        return capability;
    }

    private static void register(Map<String, AssistantCapability> target, AssistantCapability capability) {
        if (target.put(capability.getId(), capability) != null) {
            throw new IllegalStateException("重复的智能问数能力: " + capability.getId());
        }
    }
}
