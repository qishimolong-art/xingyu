package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.module.erp.service.assistant.wecom.AssistantWeComResultFormatter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantWeComResultFormatterTest {

    private final AssistantWeComResultFormatter formatter = new AssistantWeComResultFormatter();

    @Test
    void onlyOutputsReturnedColumnsAndSafeDefaults() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", "A|产品");
        row.put("amount", 12);
        row.put("internal_cost", 999);
        row.put("sql", "select secret");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", Collections.singletonList(row));
        result.put("summary", Collections.emptyList());
        result.put("scope", "当前权限");
        result.put("basis", Collections.singletonList("已审核业务单据"));

        String markdown = formatter.result(result, "按已审核口径统计");

        assertTrue(markdown.contains("A\\|产品"));
        assertTrue(markdown.contains("12"));
        assertFalse(markdown.contains("999"));
        assertFalse(markdown.contains("select secret"));
        assertTrue(markdown.contains("当前权限"));
        assertTrue(markdown.contains("依据**：已审核业务单据"));
    }

    @Test
    void rejectsForbiddenColumnsEvenWhenReturnedDefinitionRequestsThem() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", "安全数据");
        row.put("sql", "select hidden_secret");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", Collections.singletonList(row));
        result.put("columns", Arrays.asList(
                column("label", "名称"), column("sql", "内部 SQL")));

        String markdown = formatter.result(result, null);

        assertTrue(markdown.contains("安全数据"));
        assertFalse(markdown.contains("hidden_secret"));
        assertFalse(markdown.contains("内部 SQL"));
    }

    @Test
    void limitsRowsAndUtf8Length() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", index + "号" + "很长的中文内容".repeat(500));
            row.put("amount", index);
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("summary", Collections.emptyList());

        String markdown = formatter.result(result, "解释");

        assertTrue(markdown.contains("仅展示前 10 行"));
        assertTrue(markdown.getBytes(StandardCharsets.UTF_8).length <= 20_480);
        assertFalse(Character.isHighSurrogate(markdown.charAt(markdown.length() - 1)));
    }

    @Test
    void createsNumberedPendingChoice() {
        AssistantPlan plan = new AssistantPlan();
        plan.setClarification("请选择仓库");
        plan.setPendingField("warehouse");
        plan.setPendingIds(Arrays.asList(11L, 22L));
        plan.setChoices(Arrays.asList("成都仓", "重庆仓"));

        AssistantWeComResultFormatter.ChoiceResult result = formatter.clarification(plan);

        assertTrue(result.hasChoices());
        assertTrue(result.getContent().contains("1. 成都仓"));
        assertEquals(Arrays.asList(11L, 22L), result.getIds());
    }

    @Test
    void emptyResultHasExplicitExplanation() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", Collections.emptyList());
        result.put("summary", Collections.emptyList());
        String markdown = formatter.result(result, null);
        assertTrue(markdown.contains("未查询到当前权限范围内符合条件的数据"));
        assertTrue(markdown.contains("统计口径"));
    }

    @Test
    void formatsNumericCellsAndQueryTimeForWeCom() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("label", "合计");
        summary.put("amount", new BigDecimal("10830.000000"));
        summary.put("unit", "元");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", Collections.singletonList(summary));
        result.put("rows", Collections.emptyList());
        result.put("queriedAt", "2026-10-08T16:18:32.875037895");

        String markdown = formatter.result(result, null);

        assertTrue(markdown.contains("|合计|10830.00|元|"));
        assertFalse(markdown.contains("10830.000000"));
        assertTrue(markdown.contains("查询时间：2026-10-08 16:18:32"));
        assertFalse(markdown.contains("2026-10-08T16:18:32.875037895"));
    }

    @Test
    void formatsLocalDateTimeAndRoundsNumericCellsToTwoDecimals() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", "测试对象");
        row.put("amount", new BigDecimal("12.345"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", Collections.singletonList(row));
        result.put("summary", Collections.emptyList());
        result.put("queriedAt", LocalDateTime.of(2026, 10, 9, 8, 7, 6, 123_000_000));

        String markdown = formatter.result(result, null);

        assertTrue(markdown.contains("|测试对象|12.35|"));
        assertTrue(markdown.contains("查询时间：2026-10-09 08:07:06"));
    }

    @Test
    void productPriceStockUsesCompactDedicatedTableAndWarehouseSummary() {
        Map<String,Object> first=new LinkedHashMap<>();first.put("productId",1L);first.put("productName","黑霸王|K60");
        first.put("prices",Arrays.asList(price("股份价","369.000000"),price("零售价","399")));
        List<Map<String,Object>> stocks=new ArrayList<>();
        int[] amounts={415,251,29,10,10,5,-1};
        for(int index=0;index<amounts.length;index++) stocks.add(stock(index+1,"W"+(index+1),amounts[index]));
        first.put("warehouseStocks",stocks);
        Map<String,Object> second=new LinkedHashMap<>();second.put("productId",2L);second.put("productName","无库存产品");
        second.put("prices",Collections.emptyList());second.put("warehouseStocks",Collections.emptyList());
        Map<String,Object> third=new LinkedHashMap<>();third.put("productId",3L);third.put("productName","无价格权限产品");
        third.put("priceDisplayText","当前账号没有可见的产品价格字段");third.put("prices",Collections.emptyList());
        third.put("warehouseStocks",Collections.singletonList(stock(8,"成都",3)));
        Map<String,Object> result=new LinkedHashMap<>();result.put("toolName","query_product_stock");
        result.put("productResults",Arrays.asList(first,second,third));result.put("productMatchTruncated",true);
        result.put("scope","当前权限");result.put("queriedAt","2026-10-09T08:07:06.123");

        String markdown=formatter.result(result,null);

        assertTrue(markdown.contains("|序号|产品|价格|仓库库存|"));
        assertTrue(markdown.contains("黑霸王\\|K60"));
        assertTrue(markdown.contains("股份价 369.00；零售价 399.00"));
        assertTrue(markdown.contains("W1 415.00；W2 251.00；W3 29.00；W4 10.00；W5 10.00；其他2仓 4.00"));
        assertTrue(markdown.contains("无库存产品|当前账号可见的价格尚未维护|无库存"));
        assertTrue(markdown.contains("无价格权限产品|当前账号没有可见的产品价格字段|成都 3.00"));
        assertTrue(markdown.contains("已展示前 10 个产品"));
        assertFalse(markdown.contains("|对象|数值|单位|"));
    }

    @Test
    void accountBalanceUsesPartyDepartmentTableAndKeepsNegativeAmounts() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.RECEIVABLE);
        Map<String,Object> total=new LinkedHashMap<>();total.put("amount",new BigDecimal("1944.000000"));total.put("unit","元");
        Map<String,Object> first=account(1L,"客户|A",11L,"甘孜*分公司","10830");
        Map<String,Object> second=account(1L,"客户|A",12L,"兴宇路通","-8886");
        Map<String,Object> result=new LinkedHashMap<>();result.put("plan",plan);
        result.put("summary",Collections.singletonList(total));result.put("accountResults",Arrays.asList(first,second));
        result.put("accountPartyTruncated",true);result.put("scope","当前权限");

        String markdown=formatter.result(result,null);

        assertTrue(markdown.contains("|序号|客户|单据所属部门|应收余额|"));
        assertTrue(markdown.contains("**应收余额合计**：1944.00 元"));
        assertTrue(markdown.contains("|1|客户\\|A|甘孜\\*分公司|10830.00|"));
        assertTrue(markdown.contains("|2|客户\\|A|兴宇路通|-8886.00|"));
        assertTrue(markdown.contains("已展示前 10 个客户及其部门明细"));
        assertFalse(markdown.contains("|对象|数值|单位|"));
    }

    @Test
    void payableZeroTotalWithoutDepartmentRowsExplainsOffsetResult() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.PAYABLE);
        Map<String,Object> total=new LinkedHashMap<>();total.put("amount",BigDecimal.ZERO);total.put("unit","元");
        Map<String,Object> result=new LinkedHashMap<>();result.put("plan",plan);
        result.put("summary",Collections.singletonList(total));result.put("accountResults",Collections.emptyList());

        String markdown=formatter.result(result,null);

        assertTrue(markdown.contains("**应付余额合计**：0.00 元"));
        assertTrue(markdown.contains("当前没有非零部门余额"));
        assertFalse(markdown.contains("|序号|供应商|"));
    }

    @Test
    void resultShowsPhoneticCorrectionBeforeBusinessData() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("correctionNotice","已按同音/近音识别：“金典表现”→“经典表现”");
        result.put("rows",Collections.emptyList());result.put("summary",Collections.emptyList());

        String markdown=formatter.result(result,null);

        assertTrue(markdown.contains("已按同音/近音识别"));
        assertTrue(markdown.contains("金典表现"));
        assertTrue(markdown.contains("经典表现"));
    }

    @Test
    void companyKnowledgeAnswerDoesNotPretendToBeAnEmptyDataQuery() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolName", AssistantCompanyKnowledge.TOOL_NAME);
        result.put("answerText", "兴宇公司知识回答");
        result.put("basis", Arrays.asList("《四川兴宇公司概况》", "《兴宇品牌与产品线》"));

        String markdown = formatter.result(result, "兴宇公司知识回答");

        assertTrue(markdown.contains("公司知识回答"));
        assertTrue(markdown.contains("四川兴宇公司概况"));
        assertTrue(markdown.contains("公司公共知识"));
        assertFalse(markdown.contains("未查询到当前权限范围内符合条件的数据"));
        assertFalse(markdown.contains("统计口径"));
    }

    @Test
    void mixedAnswerShowsKnowledgeDataSourcesAndPartialWarning() {
        Map<String, Object> row = new LinkedHashMap<>();row.put("label", "黑霸王");row.put("amount", 12);row.put("unit", "桶");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queryType", "MIXED");result.put("toolName", AssistantMixedQuery.TOOL_NAME);
        result.put("answerText", "黑霸王产品介绍。[S1]");result.put("basis", Arrays.asList("[S1]《黑霸王产品总览》", "实时数据：当前账面库存"));
        result.put("warnings", Collections.singletonList("部分知识仍待核实"));
        result.put("summary", Collections.singletonList(row));result.put("rows", Collections.emptyList());
        result.put("scope", "当前 ERP 用户实时权限与公司公共知识");

        String markdown = formatter.result(result, "黑霸王产品介绍。[S1]");

        assertTrue(markdown.contains("公司知识与实时数据"));
        assertTrue(markdown.contains("黑霸王产品介绍"));
        assertTrue(markdown.contains("12"));
        assertTrue(markdown.contains("待核实"));
        assertTrue(markdown.contains("当前 ERP 用户实时权限"));
    }

    private static Map<String, Object> column(String key, String title) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("key", key);
        column.put("title", title);
        return column;
    }

    private static Map<String,Object> price(String title,String value) {
        Map<String,Object> price=new LinkedHashMap<>();price.put("title",title);
        price.put("value",new BigDecimal(value));price.put("unit","元");return price;
    }

    private static Map<String,Object> stock(long id,String name,int amount) {
        Map<String,Object> stock=new LinkedHashMap<>();stock.put("warehouseId",id);stock.put("warehouseName",name);
        stock.put("amount",new BigDecimal(amount));return stock;
    }

    private static Map<String,Object> account(long partyId,String partyName,Long deptId,String deptName,String amount) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("partyId",partyId);row.put("partyName",partyName);
        row.put("deptId",deptId);row.put("deptName",deptName);row.put("amount",new BigDecimal(amount));row.put("unit","元");
        return row;
    }
}
