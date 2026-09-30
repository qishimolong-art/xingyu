package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantPlan;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantSqlTest {

    @Test
    void productStockRankingAggregatesByProductWithoutUnitPartition() {
        AssistantPlan plan=stockRanking(AssistantPlan.Group.PRODUCT);
        plan.setStockMode("POSITIVE");

        String sql=AssistantSql.select(context(plan));

        assertTrue(sql.contains("MAX(productName) label,MAX(unit) unit"));
        assertTrue(sql.contains("GROUP BY productId HAVING SUM(amount)&gt;0"));
        assertTrue(sql.contains("DENSE_RANK() OVER(ORDER BY amount DESC)"));
        assertFalse(sql.contains("PARTITION BY unit"));
    }

    @Test
    void warehouseStockRankingUsesOneMixedQuantityRowPerWarehouse() {
        AssistantPlan plan=stockRanking(AssistantPlan.Group.WAREHOUSE);

        String sql=AssistantSql.select(context(plan));

        assertTrue(sql.contains("MAX(warehouseName) label,'数量' unit"));
        assertTrue(sql.contains("GROUP BY warehouseId"));
        assertTrue(sql.contains("DENSE_RANK() OVER(ORDER BY amount DESC)"));
        assertFalse(sql.contains("GROUP BY warehouseId,warehouseName,unit"));
        assertFalse(sql.contains("PARTITION BY unit"));
    }

    private static AssistantPlan stockRanking(AssistantPlan.Group group) {
        AssistantPlan plan=new AssistantPlan();
        plan.setMetric(AssistantPlan.Metric.STOCK);
        plan.setGroup(group);
        plan.setPeriod("CURRENT");
        plan.setStockMode("ALL");
        plan.setLimit(1);
        plan.setRankOrder(AssistantPlan.RankOrder.DESC);
        return plan;
    }

    private static Map<String,Object> context(AssistantPlan plan) {
        Map<String,Object> context=new HashMap<>();
        context.put("plan",plan);
        context.put("mode","groups");
        context.put("rowLimit",plan.getLimit());
        return context;
    }
}
