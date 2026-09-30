package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantPresentationTest {

    @Test
    void stockRankingUsesRowsForHeadlineAndHidesUnrelatedSummary() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);
        plan.setPeriod("CURRENT");plan.setGroup(AssistantPlan.Group.PRODUCT);plan.setLimit(1);
        assertEquals(AssistantPlan.PresentationMode.RANKING,AssistantPresentation.prepare("哪个产品的库存最多",plan));
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("summary",Collections.singletonList(row("合计",152092,"瓶",null)));
        result.put("rows",Arrays.asList(row("产品A",20,"件",1),row("产品B",20,"箱",1)));

        AssistantPresentation.apply(null,plan,result);

        Map<?,?> presentation=(Map<?,?>)result.get("presentation");
        assertEquals("RANKING",presentation.get("mode"));
        assertEquals(false,presentation.get("showSummary"));
        assertEquals(true,presentation.get("showChart"));
        assertEquals(true,presentation.get("showTable"));
        assertEquals("库存最多的产品有 2 个：产品A（20 件）、产品B（20 箱）。",presentation.get("headline"));
    }

    @Test
    void totalsTrendsDistributionsAndDetailsKeepTheirOwnBlocks() {
        AssistantPlan total=new AssistantPlan();total.setMetric(AssistantPlan.Metric.STOCK);total.setPeriod("CURRENT");
        assertEquals(AssistantPlan.PresentationMode.SUMMARY,AssistantPresentation.prepare("当前库存是多少",total));
        AssistantPlan trend=new AssistantPlan();trend.setMetric(AssistantPlan.Metric.SALE);trend.setPeriod("THIS_MONTH");trend.setGroup(AssistantPlan.Group.DAY);
        assertEquals(AssistantPlan.PresentationMode.TREND,AssistantPresentation.prepare("本月销售趋势",trend));
        AssistantPlan distribution=new AssistantPlan();distribution.setMetric(AssistantPlan.Metric.STOCK);distribution.setPeriod("CURRENT");distribution.setGroup(AssistantPlan.Group.WAREHOUSE);
        assertEquals(AssistantPlan.PresentationMode.DISTRIBUTION,AssistantPresentation.prepare("指定产品在各仓库的库存",distribution));
        Map<String,Object> detail=new LinkedHashMap<>();detail.put("summary",Collections.emptyList());detail.put("rows",Collections.singletonList(Collections.singletonMap("no","CG001")));
        AssistantPresentation.apply("最近的采购订单",null,detail);
        assertEquals("DETAIL",((Map<?,?>)detail.get("presentation")).get("mode"));
    }

    @Test
    void explicitTopNUsesRankingRangeHeadline() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);
        plan.setPeriod("CURRENT");plan.setGroup(AssistantPlan.Group.PRODUCT);plan.setLimit(10);
        AssistantPresentation.prepare("库存最多的前10个产品",plan);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("summary",Collections.emptyList());
        result.put("rows",Arrays.asList(row("产品A",20,"件",1),row("产品B",18,"箱",2)));

        AssistantPresentation.apply(null,plan,result);

        assertEquals("产品库存数量前10名，共返回 2 个结果。",
                ((Map<?,?>)result.get("presentation")).get("headline"));
    }

    @Test
    void topNHeadlineUsesMetricAndBusinessSubjectForAllVerifiedMetrics() {
        Object[][] cases={
                {AssistantPlan.Metric.STOCK,AssistantPlan.Group.PRODUCT,"产品库存数量前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.STOCK_SKU,AssistantPlan.Group.WAREHOUSE,"仓库 SKU 数量前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.SALE,AssistantPlan.Group.PARTY,"客户销售金额前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.PURCHASE,AssistantPlan.Group.PARTY,"供应商采购金额前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.RECEIPT,AssistantPlan.Group.PARTY,"客户收款金额前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.PAYMENT,AssistantPlan.Group.PARTY,"供应商付款金额前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.RECEIVABLE,AssistantPlan.Group.PARTY,"客户应收余额前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.PAYABLE,AssistantPlan.Group.PARTY,"供应商应付余额前10名，共返回 2 个结果。"},
                {AssistantPlan.Metric.SALE,AssistantPlan.Group.DEPT,"部门销售金额前10名，共返回 2 个结果。"}
        };
        for(Object[] item:cases) {
            AssistantPlan plan=ranking((AssistantPlan.Metric)item[0],(AssistantPlan.Group)item[1],10,AssistantPlan.RankOrder.DESC);
            AssistantPresentation.prepare("排名前10",plan);
            Map<String,Object> result=result(row("第一名",20,"元",1),row("第二名",18,"元",2));

            AssistantPresentation.apply(null,plan,result);

            assertEquals(item[2],((Map<?,?>)result.get("presentation")).get("headline"),item[0]+" / "+item[1]);
        }
    }

    @Test
    void receivableRankingUsesBalanceWordingForUniqueTieAndAscendingResults() {
        AssistantPlan unique=ranking(AssistantPlan.Metric.RECEIVABLE,AssistantPlan.Group.PARTY,1,AssistantPlan.RankOrder.DESC);
        AssistantPresentation.prepare("哪个客户欠钱最多",unique);
        Map<String,Object> uniqueResult=result(row("客户A",156301,"元",1));
        AssistantPresentation.apply(null,unique,uniqueResult);
        assertEquals("应收余额最高的客户是客户A，应收余额为156301 元。",
                ((Map<?,?>)uniqueResult.get("presentation")).get("headline"));

        AssistantPlan tie=ranking(AssistantPlan.Metric.RECEIVABLE,AssistantPlan.Group.PARTY,1,AssistantPlan.RankOrder.DESC);
        AssistantPresentation.prepare("哪个客户欠钱最多",tie);
        Map<String,Object> tieResult=result(row("客户A",20,"元",1),row("客户B",20,"元",1));
        AssistantPresentation.apply(null,tie,tieResult);
        assertEquals("应收余额最高的客户有 2 个：客户A（20 元）、客户B（20 元）。",
                ((Map<?,?>)tieResult.get("presentation")).get("headline"));

        AssistantPlan ascending=ranking(AssistantPlan.Metric.RECEIVABLE,AssistantPlan.Group.PARTY,10,AssistantPlan.RankOrder.ASC);
        AssistantPresentation.prepare("应收余额最低的后10个客户",ascending);
        Map<String,Object> ascendingResult=result(row("客户A",1,"元",1),row("客户B",2,"元",2));
        AssistantPresentation.apply(null,ascending,ascendingResult);
        assertEquals("客户应收余额后10名，共返回 2 个结果。",
                ((Map<?,?>)ascendingResult.get("presentation")).get("headline"));
    }

    @Test
    void emptyRankingDoesNotProduceHeadline() {
        AssistantPlan plan=ranking(AssistantPlan.Metric.RECEIVABLE,AssistantPlan.Group.PARTY,10,AssistantPlan.RankOrder.DESC);
        AssistantPresentation.prepare("哪个客户欠钱最多",plan);
        Map<String,Object> result=result();

        AssistantPresentation.apply(null,plan,result);

        assertFalse(((Map<?,?>)result.get("presentation")).containsKey("headline"));
    }

    private static AssistantPlan ranking(AssistantPlan.Metric metric,AssistantPlan.Group group,int limit,
                                         AssistantPlan.RankOrder order) {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(metric);plan.setGroup(group);plan.setLimit(limit);
        plan.setRankOrder(order);plan.setPeriod(plan.current()?"CURRENT":"THIS_MONTH");return plan;
    }

    private static Map<String,Object> result(Map<String,Object>... rows) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("summary",Collections.emptyList());
        result.put("rows",Arrays.asList(rows));return result;
    }

    private static Map<String,Object> row(String label,int amount,String unit,Integer rank) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("label",label);row.put("amount",amount);row.put("unit",unit);
        if(rank!=null) row.put("unitRank",rank);return row;
    }
}
