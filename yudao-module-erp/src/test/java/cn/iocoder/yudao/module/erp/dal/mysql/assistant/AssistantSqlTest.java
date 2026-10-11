package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantPlan;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Arrays;
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

    @Test
    void productResultTableFiltersResolvedProductsAndAggregatesWarehouses() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);
        plan.setPeriod("CURRENT");plan.setIncludeProductPrices(true);plan.setProductResultTable(true);
        plan.setProductIds(Arrays.asList(9L,3L));
        Map<String,Object> context=context(plan);context.put("mode","productResults");

        String sql=AssistantSql.select(context);

        assertTrue(sql.contains("collection='plan.productIds'"));
        assertTrue(sql.contains("GROUP BY productId,warehouseId"));
        assertTrue(sql.contains("SUM(amount) amount"));
        assertTrue(sql.contains("MAX(sharePrice) sharePrice"));
    }

    @Test
    void accountPartiesRankByPartyTotalBeforeDepartmentExpansion() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.PAYABLE);
        plan.setPeriod("CURRENT");plan.setGroup(AssistantPlan.Group.PARTY);
        plan.setRankOrder(AssistantPlan.RankOrder.ASC);
        Map<String,Object> values=context(plan);values.put("mode","accountParties");values.put("rowLimit",11);

        String sql=AssistantSql.select(values);

        assertTrue(sql.contains("GROUP BY partyId ORDER BY amount ASC,partyId LIMIT #{rowLimit}"));
        assertTrue(sql.contains("SUM(amount) amount"));
    }

    @Test
    void accountDepartmentResultsUseBusinessDocumentDepartmentsWithoutCountingWriteoffs() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.RECEIVABLE);
        plan.setPeriod("CURRENT");plan.setPartyId(9L);
        Map<String,Object> values=context(plan);values.put("mode","accountResults");
        values.put("accountPartyIds",Arrays.asList(9L));values.put("rowLimit",2001);

        String sql=AssistantSql.select(values);

        assertTrue(sql.contains("erp_sale_out"));
        assertTrue(sql.contains("erp_sale_return"));
        assertTrue(sql.contains("erp_sale_price_adjust"));
        assertTrue(sql.contains("erp_finance_receipt"));
        assertTrue(sql.contains("erp_receivable_other"));
        assertTrue(sql.contains("erp_receivable_misc"));
        assertFalse(sql.contains("erp_receivable_writeoff"));
        assertTrue(sql.contains("collection='accountPartyIds'"));
        assertTrue(sql.contains("GROUP BY partyId,deptId HAVING SUM(amount)&lt;&gt;0"));
        assertTrue(sql.contains("COALESCE(MAX(deptName),'未归属部门')"));
    }

    @Test
    void accountDepartmentResultDynamicSqlCanBeBoundWithoutDatabase() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.PAYABLE);
        plan.setPeriod("CURRENT");
        Map<String,Object> values=context(plan);values.put("mode","accountResults");
        values.put("accountPartyIds",Arrays.asList(3L,5L));values.put("rowLimit",2001);
        values.put("documentAll",true);values.put("partyAll",true);

        BoundSql boundSql=new XMLLanguageDriver().createSqlSource(new org.apache.ibatis.session.Configuration(),
                AssistantSql.select(values),Map.class).getBoundSql(values);

        String normalizedSql=boundSql.getSql().replaceAll("\\s+"," ");
        assertTrue(normalizedSql.contains("partyId IN ( ? , ? )"));
        assertFalse(boundSql.getSql().contains("<foreach"));
    }

    @Test
    void salespersonSalesAmountAndQuantityUseVerifiedPersonAndNetDocumentSources() {
        AssistantPlan amount=new AssistantPlan();amount.setMetric(AssistantPlan.Metric.SALE);
        amount.setPeriod("THIS_MONTH");amount.setGroup(AssistantPlan.Group.SALESPERSON);
        amount.setSalesperson("张三");amount.setSalespersonId(42L);
        String amountSql=AssistantSql.select(context(amount));
        assertTrue(amountSql.contains("h.sale_user_id salespersonId"));
        assertTrue(amountSql.contains("plan.salespersonId"));
        assertTrue(amountSql.contains("COALESCE(salespersonName,'未指定业务员')"));
        assertTrue(amountSql.contains("erp_sale_out"));assertTrue(amountSql.contains("erp_sale_return"));

        AssistantPlan quantity=new AssistantPlan();quantity.setMetric(AssistantPlan.Metric.SALE);
        quantity.setPeriod("THIS_MONTH");quantity.setGroup(AssistantPlan.Group.SALESPERSON);
        quantity.setSalesMeasure(AssistantPlan.SalesMeasure.QUANTITY);
        String quantitySql=AssistantSql.select(context(quantity));
        assertTrue(quantitySql.contains("erp_sale_out_items"));
        assertTrue(quantitySql.contains("erp_sale_return_items"));
        assertTrue(quantitySql.contains("COALESCE(u.name,'未设置单位') unit"));
        assertTrue(quantitySql.contains("IFNULL(i.count,0)"));
        assertTrue(quantitySql.contains("GROUP BY COALESCE(salespersonId,0),COALESCE(salespersonName,'未指定业务员'),unit"));
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
