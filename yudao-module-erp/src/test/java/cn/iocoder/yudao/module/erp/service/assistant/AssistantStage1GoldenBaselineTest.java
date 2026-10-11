package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 阶段 1：V1 结果基线和后续 V2 可复用的黄金问题契约。 */
class AssistantStage1GoldenBaselineTest {

    private static final String GOLDEN_RESOURCE = "/assistant/assistant_stage1_golden_v1.psv";
    private static final Set<String> SCENARIOS = new LinkedHashSet<>(Arrays.asList(
            "POSITIVE", "BOUNDARY", "NO_PERMISSION", "FAILURE"));
    private static final Set<String> REQUIRED_CAPABILITIES = new LinkedHashSet<>(Arrays.asList(
            "product_archive", "product_category", "product_brand", "product_unit", "price_system", "base_data_dictionary",
            "customer_archive", "supplier_archive", "system_user", "system_department", "user_department_relation",
            "sale_metric", "sale_order", "sale_quote_cart", "sale_out_return", "sale_pick_delivery", "sale_price_adjust",
            "purchase_metric", "purchase_order", "purchase_in_return", "purchase_invoice_ocr", "purchase_price_adjust",
            "stock_current", "stock_movement", "stock_documents", "stock_operations", "warehouse_archive",
            "receipt_payment", "account_transfer", "receivable_payable", "receivable_misc_income", "payable_misc_expense",
            "adjustment_writeoff_offset", "company_knowledge", "mixed_question"));

    @Test
    void stageOneCorpusContainsAtLeastThreeHundredUniqueQuestions() throws Exception {
        List<Row> curated = rows();
        Set<String> questions = curated.stream().map(row -> row.question).collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(curated.size(), questions.size(), "阶段 1 扩展问题不得重复");

        try (InputStream input = getClass().getResourceAsStream("/assistant/wecom_faq_route_fixture_20260928.json")) {
            assertNotNull(input, "企微真实高频问法基线缺失");
            @SuppressWarnings("unchecked")
            Map<String, Object> root = new ObjectMapper().readValue(input, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> faq = (List<Map<String, Object>>) root.get("rows");
            faq.forEach(row -> questions.add(String.valueOf(row.get("question"))));
        }

        assertTrue(curated.size() >= 120, "能力四象限问题应不少于 120 条");
        assertTrue(questions.size() >= 300, "阶段 1 黄金问题总数应不少于 300 条，实际为 " + questions.size());
    }

    @Test
    void everyCapabilityHasPositiveBoundaryPermissionAndFailureCases() throws Exception {
        Map<String, List<Row>> byCapability = rows().stream().collect(Collectors.groupingBy(
                row -> row.capabilityId, LinkedHashMap::new, Collectors.toList()));
        assertEquals(REQUIRED_CAPABILITIES, byCapability.keySet(), "阶段 0 范围对应的能力族不得遗漏或被替换");
        for (Map.Entry<String, List<Row>> entry : byCapability.entrySet()) {
            Set<String> actual = entry.getValue().stream().map(row -> row.scenario).collect(Collectors.toSet());
            assertEquals(SCENARIOS, actual, entry.getKey() + " 缺少四象限用例");
            assertEquals(4, entry.getValue().size(), entry.getKey() + " 每个阶段 1 能力应固定四条基线");
        }
    }

    @Test
    void corpusCoversAllRequiredAcceptanceScenarios() throws Exception {
        Set<String> tags = rows().stream().flatMap(row -> Arrays.stream(row.tags.split(",")))
                .collect(Collectors.toSet());
        assertTrue(tags.containsAll(Arrays.asList("normal", "time_status", "ranking", "follow_up",
                "no_data", "no_permission", "knowledge", "mixed", "safety", "sensitive")), tags.toString());
    }

    @Test
    void everyQuestionRecordsV1ObservationAndV2Expectation() throws Exception {
        Set<String> allowedV1Support = new HashSet<>(Arrays.asList(
                "SUPPORTED", "PARTIAL", "MODEL_DEPENDENT", "SPLIT_ONLY", "GAP"));
        Set<String> allowedTargetRoutes = new HashSet<>(Arrays.asList(
                "VERIFIED_METRIC", "BUSINESS_TOOL", "KNOWLEDGE", "MIXED"));
        for (Row row : rows()) {
            assertTrue(allowedV1Support.contains(row.v1Support), row.caseId);
            assertFalse(row.v1Route.isEmpty(), row.caseId);
            assertFalse(row.v1Outcome.isEmpty(), row.caseId);
            assertTrue(allowedTargetRoutes.contains(row.targetRoute), row.caseId);
            assertFalse(row.targetStatus.isEmpty(), row.caseId);
            assertTrue(Arrays.asList("AUTOMATED_TEST", "SOURCE_TRACE", "RUNTIME_REQUIRED").contains(row.evidence), row.caseId);
            if ("NO_PERMISSION".equals(row.scenario)
                    && !Arrays.asList("company_knowledge", "mixed_question").contains(row.capabilityId)) {
                assertEquals("NO_PERMISSION", row.targetStatus, row.caseId);
            }
        }
    }

    @Test
    void declaredV1ToolsStillExistInCurrentRegistry() throws Exception {
        AssistantToolRegistry registry = new AssistantToolRegistry();
        Set<String> specialRoutes = Collections.singleton("split_mixed_question");
        for (Row row : rows()) {
            if (row.v1Tool.isEmpty()) {
                assertEquals("GAP", row.v1Support, row.caseId + " 只有明确能力缺口可以没有 V1 工具");
                continue;
            }
            assertTrue(registry.contains(row.v1Tool) || specialRoutes.contains(row.v1Tool),
                    row.caseId + " 引用了未注册的 V1 工具 " + row.v1Tool);
        }
    }

    @Test
    void publishedMetricSemanticBaselineRemainsStable() {
        AssistantSemanticKnowledge semantic = new AssistantSemanticKnowledge();
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("本月销售额", "SALE");
        cases.put("本月采购额", "PURCHASE");
        // 当前 V1 语义层把库存问法先归入产品价格/库存工具，指标区分在后续确定性计划中完成。
        cases.put("当前库存数量", "PRODUCT_PRICE_STOCK");
        cases.put("当前库存商品种类", "PRODUCT_PRICE_STOCK");
        cases.put("本周收款", "RECEIPT");
        cases.put("本周付款", "PAYMENT");
        cases.put("当前应收余额", "RECEIVABLE");
        // PAYABLE 在 V1 语义卡中尚无确定性分支，当前会落到裸产品兜底；该缺口由阶段 1 固化，后续再纠正。
        cases.put("当前应付余额", "PRODUCT_PRICE_STOCK");
        cases.forEach((question, intent) -> assertEquals(intent, semantic.match(question).getIntentId(), question));
    }

    @Test
    void organizationKnowledgePhrasesAreNotBareProducts() {
        AssistantSemanticKnowledge semantic = new AssistantSemanticKnowledge();
        for (String question : new String[]{"公司的组织架构", "公司组织结构是什么", "兴宇的公司架构",
                "公司的部门负责人分别是谁", "谁是总经办的负责人", "市场部负责人是谁", "项目部由谁负责",
                "公司的人员组成", "具体的人员组成", "这些人分别负责什么", "财务部的主要职责",
                "市场部负责什么", "项目部主要做什么", "总经办有哪些职责", "公司各部门之间如何协作",
                "这些部门的负责人分别是谁", "谁负责传媒工作", "传媒工作由谁负责", "汪凯虎的工作",
                "汪凯虎负责什么", "组织架构经历过哪些变化", "经销商业务部如何划分区域",
                "总经办有哪些人", "总经办有那些人", "总经办都有谁", "总经办的人员组成",
                "传媒运营部有哪些人", "财务部成员有哪些"}) {
            assertNotEquals("PRODUCT_PRICE_STOCK", semantic.match(question).getIntentId(), question);
        }
        for (String question : new String[]{"客户负责人是谁", "供应商负责人是谁", "产品负责人是谁", "项目负责人是谁"}) {
            assertNotEquals("PRODUCT_PRICE_STOCK", semantic.match(question).getIntentId(), question);
        }
        assertEquals("PRODUCT_PRICE_STOCK", semantic.match("黑霸王").getIntentId());
        assertEquals("PRODUCT_PRICE_STOCK", semantic.match("经典表现库存").getIntentId());
        assertEquals("PRODUCT_PRICE_STOCK", semantic.match("145988 的库存和价格").getIntentId());
    }

    @Test
    void baselineContainsNoSecretMaterial() throws Exception {
        for (Row row : rows()) {
            String normalized = row.question.toLowerCase(Locale.ROOT);
            assertFalse(normalized.matches(".*(?:sk-[a-z0-9]{16,}|password\\s*[=:]|secret\\s*[=:]|token\\s*[=:]).*"), row.caseId);
        }
    }

    private List<Row> rows() throws Exception {
        InputStream input = getClass().getResourceAsStream(GOLDEN_RESOURCE);
        assertNotNull(input, GOLDEN_RESOURCE + " 缺失");
        List<Row> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            assertEquals("case_id|capability_id|domain|batch|scenario|question|previous_question|tags|v1_support|v1_route|v1_tool|v1_outcome|target_route|target_status|evidence", header);
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] values = line.split("\\|", -1);
                assertEquals(15, values.length, "黄金问题格式错误，行 " + lineNumber);
                result.add(new Row(values));
            }
        }
        return result;
    }

    private static final class Row {
        private final String caseId;
        private final String capabilityId;
        private final String scenario;
        private final String question;
        private final String tags;
        private final String v1Support;
        private final String v1Route;
        private final String v1Tool;
        private final String v1Outcome;
        private final String targetRoute;
        private final String targetStatus;
        private final String evidence;

        private Row(String[] values) {
            caseId = values[0];
            capabilityId = values[1];
            scenario = values[4];
            question = values[5];
            tags = values[7];
            v1Support = values[8];
            v1Route = values[9];
            v1Tool = values[10];
            v1Outcome = values[11];
            targetRoute = values[12];
            targetStatus = values[13];
            evidence = values[14];
            assertFalse(caseId.isEmpty());
            assertFalse(capabilityId.isEmpty());
            assertTrue(SCENARIOS.contains(scenario), caseId);
            assertFalse(question.isEmpty(), caseId);
            assertFalse(tags.isEmpty(), caseId);
        }
    }
}
