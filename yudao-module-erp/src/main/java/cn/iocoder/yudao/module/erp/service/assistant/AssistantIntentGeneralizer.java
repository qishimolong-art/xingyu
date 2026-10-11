package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.Locale;

/**
 * 与语序无关的高置信语义特征归一层。
 *
 * <p>这里只处理可以由业务词、疑问操作和对象类别共同确认的语义，不读取业务数据，
 * 也不尝试从整句话猜测具体实体。不能高置信确认的问题继续交给受约束模型路由。</p>
 */
final class AssistantIntentGeneralizer {

    enum Domain { INVENTORY, STOCK_MOVEMENT, SALE, PURCHASE, FINANCE, COMPANY_KNOWLEDGE, UNKNOWN }
    enum Operation { METRIC, DOCUMENT, FULFILLMENT, COUNT_DISTINCT, LIST, RANK, KNOWLEDGE_ANSWER, UNKNOWN }

    static final class Decision {
        private final Domain domain;
        private final Operation operation;
        private final String capabilityId;
        private final String metric;
        private final String aggregation;
        private final String entityRequirement;
        private final double confidence;
        private final String reason;

        private Decision(Domain domain, Operation operation, String capabilityId, String metric,
                         String aggregation, String entityRequirement, double confidence, String reason) {
            this.domain = domain;
            this.operation = operation;
            this.capabilityId = capabilityId;
            this.metric = metric;
            this.aggregation = aggregation;
            this.entityRequirement = entityRequirement;
            this.confidence = confidence;
            this.reason = reason;
        }

        Domain getDomain() { return domain; }
        Operation getOperation() { return operation; }
        String getCapabilityId() { return capabilityId; }
        String getMetric() { return metric; }
        String getAggregation() { return aggregation; }
        String getEntityRequirement() { return entityRequirement; }
        double getConfidence() { return confidence; }
        String getReason() { return reason; }
        boolean resolved() { return capabilityId != null; }
    }

    private AssistantIntentGeneralizer() {
    }

    static Decision analyze(String question) {
        String text = compact(question);
        if (text.isEmpty()) return unknown();

        boolean inventory = containsAny(text, "库存", "有货", "现货", "缺货", "无货");
        boolean productClass = containsAny(text, "产品", "配件", "商品", "sku");
        boolean movement = containsAny(text, "流水", "出入库", "入库记录", "出库记录", "库存变动", "调拨", "盘点");
        boolean variety = containsAny(text, "多少种", "有几种", "几种", "种数", "种类", "品种")
                || text.matches(".*sku(?:数|数量|个数|种数).*" );
        boolean genericCount = containsAny(text, "多少个", "有几个", "几个", "数量", "个数", "总数");
        boolean list = containsAny(text, "哪些", "哪一些", "都有", "列出", "列表", "清单", "明细", "分布");
        boolean ranking = containsAny(text, "最多", "最少", "最高", "最低", "最大", "最小", "排名", "排行")
                || text.matches(".*(?:前|后)[0-9一二三四五六七八九十]+.*");

        if (movement) {
            return new Decision(Domain.STOCK_MOVEMENT, Operation.DOCUMENT, "query_stock_movements",
                    "STOCK", "NONE", "OPTIONAL", 0.96D, "STOCK_MOVEMENT_QUERY");
        }
        if (inventory && productClass && (variety || genericCount && !list && !ranking)) {
            return new Decision(Domain.INVENTORY, Operation.COUNT_DISTINCT, "query_verified_metric",
                    "STOCK_SKU", "COUNT_DISTINCT", "NONE", 0.99D,
                    variety ? "INVENTORY_PRODUCT_VARIETY_COUNT" : "INVENTORY_GENERIC_PRODUCT_COUNT");
        }
        if (inventory && productClass && ranking) {
            return new Decision(Domain.INVENTORY, Operation.RANK, "query_verified_metric",
                    "STOCK", "NONE", "NONE", 0.98D, "INVENTORY_PRODUCT_RANKING");
        }
        if (inventory && list) {
            return new Decision(Domain.INVENTORY, Operation.LIST, "query_inventory_details",
                    "STOCK", "NONE", "NONE", 0.97D, "INVENTORY_LIST");
        }

        AssistantCompanyKnowledgeIntent.Analysis companyKnowledge =
                AssistantCompanyKnowledgeIntent.analyze(question);
        if (companyKnowledge.isKnowledge()) {
            return new Decision(Domain.COMPANY_KNOWLEDGE, Operation.KNOWLEDGE_ANSWER,
                    AssistantCompanyKnowledge.TOOL_NAME, null, "NONE", "NONE", companyKnowledge.getConfidence(),
                    companyKnowledge.getReason());
        }

        boolean saleBusinessDocument = containsAny(text, "销售报价", "报价单", "销售开单", "销售出库",
                "销售退货", "拣货配送", "销售调价", "销售价格调整");
        if (saleBusinessDocument)
            return new Decision(Domain.SALE, Operation.DOCUMENT, AssistantStage5BusinessQuery.SALE_TOOL,
                    null, "NONE", "OPTIONAL", 0.97D, "SALE_BUSINESS_DOCUMENT");
        boolean saleDocument = containsAny(text, "销售订单", "销售单", "客户订单");
        boolean saleFulfillment = saleDocument && containsAny(text, "发货", "未发货", "未发完", "没发完", "剩余", "履约", "未完成");
        if (saleDocument) {
            return new Decision(Domain.SALE, saleFulfillment ? Operation.FULFILLMENT : Operation.DOCUMENT,
                    saleFulfillment ? "query_sale_fulfillment" : "query_sale_documents",
                    "SALE", "NONE", "OPTIONAL", 0.97D,
                    saleFulfillment ? "SALE_FULFILLMENT" : "SALE_DOCUMENT");
        }
        boolean saleMetric = containsAny(text, "销售", "销量", "销售额", "卖出", "卖了", "卖掉", "成交");
        boolean productPrice = containsAny(text, "销售价", "售价", "报价", "价格", "多少钱一个")
                && !text.contains("销售额");
        if (saleMetric && !productPrice && !containsAny(text, "销售话术", "怎么卖", "如何卖")) {
            return new Decision(Domain.SALE, Operation.METRIC, "query_verified_metric",
                    "SALE", "SUM", "OPTIONAL", 0.93D, "SALE_METRIC");
        }

        boolean purchaseBusinessDocument = containsAny(text, "采购入库", "采购退货", "采购发票", "采购调价", "采购价格调整");
        if (purchaseBusinessDocument)
            return new Decision(Domain.PURCHASE, Operation.DOCUMENT, AssistantStage5BusinessQuery.PURCHASE_TOOL,
                    null, "NONE", "OPTIONAL", 0.97D, "PURCHASE_BUSINESS_DOCUMENT");
        boolean purchaseDocument = containsAny(text, "采购订单", "采购单", "订货单");
        boolean purchaseFulfillment = purchaseDocument && containsAny(text, "到货", "未到货", "剩余", "履约", "未完成");
        if (purchaseDocument) {
            return new Decision(Domain.PURCHASE, purchaseFulfillment ? Operation.FULFILLMENT : Operation.DOCUMENT,
                    purchaseFulfillment ? "query_purchase_fulfillment" : "query_purchase_documents",
                    "PURCHASE", "NONE", "OPTIONAL", 0.97D,
                    purchaseFulfillment ? "PURCHASE_FULFILLMENT" : "PURCHASE_DOCUMENT");
        }
        boolean purchaseMetric = containsAny(text, "采购额", "采购金额", "采购量", "进货金额", "进货量", "进货花了", "买进");
        if (purchaseMetric) {
            return new Decision(Domain.PURCHASE, Operation.METRIC, "query_verified_metric",
                    "PURCHASE", "SUM", "OPTIONAL", 0.93D, "PURCHASE_METRIC");
        }

        if (containsAny(text, "收款单", "付款单", "应收调账", "应付调账", "核销记录", "往来对冲"))
            return new Decision(Domain.FINANCE, Operation.DOCUMENT, AssistantStage5BusinessQuery.FINANCE_TOOL,
                    null, "NONE", "OPTIONAL", 0.97D, "FINANCE_BUSINESS_DOCUMENT");
        if (containsAny(text, "应收", "客户欠款", "客户余额", "欠我们"))
            return finance("RECEIVABLE", "RECEIVABLE_METRIC");
        if (containsAny(text, "应付", "供应商余额", "欠供应商"))
            return finance("PAYABLE", "PAYABLE_METRIC");
        if (containsAny(text, "收款", "回款", "收回来", "收了多少钱"))
            return finance("RECEIPT", "RECEIPT_METRIC");
        if (containsAny(text, "付款", "付出去", "付了多少钱"))
            return finance("PAYMENT", "PAYMENT_METRIC");
        return unknown();
    }

    static boolean isGenericStockSkuCount(String question) {
        Decision decision = analyze(question);
        return decision.getDomain() == Domain.INVENTORY
                && decision.getOperation() == Operation.COUNT_DISTINCT;
    }

    /**
     * 公司主体与外部品牌、厂家、渠道或合作伙伴之间的关系属于公司知识，且不依赖双方语序。
     * 实时金额、库存和单据等业务词优先保留给受控业务查询。
     */
    static boolean isCompanyRelationshipQuestion(String question) {
        AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(question);
        return analysis.isKnowledge()
                && analysis.getTopic() == AssistantCompanyKnowledgeIntent.Topic.RELATIONSHIP;
    }

    /** 统一知识检索中的第一人称公司指代，避免把“我们/咱们”当成无意义泛词。 */
    static String normalizeCompanySelfReference(String question) {
        return AssistantCompanyKnowledgeIntent.normalizeSelfReference(question);
    }

    /**
     * 提取关系问题中的另一方名称，用于保证知识证据确实提到了该品牌或合作伙伴。
     * 返回 null 表示问题只给出了“厂家/合作方”等泛称，不做具体对象过滤。
     */
    static String companyRelationshipCounterparty(String question) {
        return AssistantCompanyKnowledgeIntent.relationshipCounterparty(question);
    }

    /**
     * 只修正类别级问题被误送到单实体工具的情况；具体业务工具之间不在这里互相覆盖。
     */
    static String reconcileCapability(String question, String selectedCapability, boolean knowledgeAvailable) {
        Decision decision = analyze(question);
        if (!decision.resolved()) return selectedCapability;
        if (decision.getDomain() == Domain.COMPANY_KNOWLEDGE && !knowledgeAvailable) return selectedCapability;
        if (selectedCapability == null || "model_routed".equals(selectedCapability)) return decision.getCapabilityId();
        if (decision.getDomain() == Domain.COMPANY_KNOWLEDGE
                && !AssistantMixedQuery.TOOL_NAME.equals(selectedCapability)) {
            return decision.getCapabilityId();
        }
        if (decision.getDomain() == Domain.INVENTORY
                && ("query_product_stock".equals(selectedCapability)
                || "resolve_business_entity".equals(selectedCapability))) {
            return decision.getCapabilityId();
        }
        return selectedCapability;
    }

    private static Decision unknown() {
        return new Decision(Domain.UNKNOWN, Operation.UNKNOWN, null, null,
                "NONE", "OPTIONAL", 0D, "NO_HIGH_CONFIDENCE_SEMANTIC_ROUTE");
    }

    private static Decision finance(String metric,String reason) {
        return new Decision(Domain.FINANCE, Operation.METRIC, "query_verified_metric",
                metric, "SUM", "OPTIONAL", 0.93D, reason);
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static String compact(String question) {
        return question == null ? "" : question.replaceAll("[\\s，。！？、,.!?；;：:]", "")
                .toLowerCase(Locale.ROOT);
    }
}
