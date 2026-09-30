package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.module.erp.service.assistant.wecom.AssistantWeComResultFormatter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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

        String markdown = formatter.result(result, "按已审核口径统计");

        assertTrue(markdown.contains("A\\|产品"));
        assertTrue(markdown.contains("12"));
        assertFalse(markdown.contains("999"));
        assertFalse(markdown.contains("select secret"));
        assertTrue(markdown.contains("当前权限"));
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

    private static Map<String, Object> column(String key, String title) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("key", key);
        column.put("title", title);
        return column;
    }
}
