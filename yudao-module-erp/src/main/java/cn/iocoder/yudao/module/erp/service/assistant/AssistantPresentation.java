package cn.iocoder.yudao.module.erp.service.assistant;

import java.math.BigDecimal;
import java.util.*;

/** Builds deterministic result presentation hints from the normalized query intent. */
public final class AssistantPresentation {
    private AssistantPresentation() {}

    public static AssistantPlan.PresentationMode prepare(String question, AssistantPlan plan) {
        AssistantPlan.PresentationMode mode=resolve(question,plan,null);
        if(plan!=null) plan.setPresentationMode(mode);
        return mode;
    }

    public static boolean skipsSummary(AssistantPlan plan) {
        return plan!=null && plan.getPresentationMode()==AssistantPlan.PresentationMode.RANKING;
    }

    public static void apply(String question,AssistantPlan plan,Map<String,Object> result) {
        AssistantPlan.PresentationMode mode=resolve(question,plan,result);
        if(plan!=null) plan.setPresentationMode(mode);
        List<?> summary=list(result.get("summary")),rows=list(result.get("rows"));
        Map<String,Object> presentation=new LinkedHashMap<>();
        presentation.put("mode",mode.name());
        presentation.put("showSummary",mode!=AssistantPlan.PresentationMode.RANKING
                && mode!=AssistantPlan.PresentationMode.DETAIL && !summary.isEmpty());
        presentation.put("showChart",Arrays.asList(AssistantPlan.PresentationMode.RANKING,
                AssistantPlan.PresentationMode.TREND,AssistantPlan.PresentationMode.DISTRIBUTION).contains(mode)
                && !rows.isEmpty());
        presentation.put("showTable",!rows.isEmpty()
                && (mode!=AssistantPlan.PresentationMode.SUMMARY || summary.isEmpty()));
        String headline=rankingHeadline(plan,rows);
        if(headline!=null) presentation.put("headline",headline);
        result.put("presentation",presentation);
    }

    private static AssistantPlan.PresentationMode resolve(String question,AssistantPlan plan,Map<String,Object> result) {
        if(plan!=null && question==null && plan.getPresentationMode()!=null) return plan.getPresentationMode();
        String text=question==null?"":question.replaceAll("\\s+","");
        if(plan!=null) {
            if(isRanking(text) && plan.getGroup()!=AssistantPlan.Group.NONE && plan.getGroup()!=AssistantPlan.Group.DAY)
                return AssistantPlan.PresentationMode.RANKING;
            if(plan.getGroup()==AssistantPlan.Group.DAY) return AssistantPlan.PresentationMode.TREND;
            if(plan.getGroup()!=AssistantPlan.Group.NONE) return AssistantPlan.PresentationMode.DISTRIBUTION;
            return AssistantPlan.PresentationMode.SUMMARY;
        }
        if(isRanking(text)) return AssistantPlan.PresentationMode.RANKING;
        if(text.matches(".*(?:趋势|按天|每天|每日|逐日).*")) return AssistantPlan.PresentationMode.TREND;
        if(text.matches(".*(?:按|每个|各个|分别)(?:仓库|部门|客户|供应商|产品|配件|状态).*"))
            return AssistantPlan.PresentationMode.DISTRIBUTION;
        if(result!=null && !list(result.get("rows")).isEmpty() && list(result.get("summary")).isEmpty())
            return AssistantPlan.PresentationMode.DETAIL;
        return AssistantPlan.PresentationMode.SUMMARY;
    }

    private static boolean isRanking(String text) {
        return text.matches(".*(?:排名|排行|最多|最高|最大|最少|最低|最小|前[0-9一二三四五六七八九十]+|后[0-9一二三四五六七八九十]+).*" );
    }

    private static String rankingHeadline(AssistantPlan plan,List<?> rows) {
        if(plan==null || plan.getPresentationMode()!=AssistantPlan.PresentationMode.RANKING || rows.isEmpty()) return null;
        List<Map<?,?>> first=new ArrayList<>();
        for(Object value:rows) {
            if(!(value instanceof Map)) continue;
            Map<?,?> row=(Map<?,?>)value;Object rank=row.containsKey("unitRank")?row.get("unitRank"):row.get("unit_rank");
            if(rank==null && first.isEmpty() || rank!=null && "1".equals(number(rank))) first.add(row);
            else if(rank==null) break;
        }
        if(first.isEmpty()) return null;
        String direction=direction(plan);
        String subject=subject(plan);
        String metric=metric(plan);
        if(plan.getLimit()>1) {
            String range=plan.getRankOrder()==AssistantPlan.RankOrder.ASC?"后":"前";
            return rankingLabel(plan,subject)+range+plan.getLimit()+"名，共返回 "+rows.size()+" 个结果。";
        }
        if(first.size()==1) {
            Map<?,?> row=first.get(0);
            String valueLabel=plan.getMetric()==AssistantPlan.Metric.STOCK?"当前库存":metric;
            return metric+direction+"的"+subject+"是"+value(row,"label","未命名")+"，"+valueLabel+"为"
                    +value(row,"amount","0")+unit(row)+"。";
        }
        List<String> values=new ArrayList<>();
        for(Map<?,?> row:first) values.add(value(row,"label","未命名")+"（"+value(row,"amount","0")+unit(row)+"）");
        return metric+direction+"的"+subject+"有 "+first.size()+" 个："+String.join("、",values)+"。";
    }

    private static String direction(AssistantPlan plan) {
        if(plan.getRankOrder()!=AssistantPlan.RankOrder.ASC)
            return plan.getMetric()==AssistantPlan.Metric.STOCK || plan.getMetric()==AssistantPlan.Metric.STOCK_SKU?"最多":"最高";
        return plan.getMetric()==AssistantPlan.Metric.STOCK || plan.getMetric()==AssistantPlan.Metric.STOCK_SKU?"最少":"最低";
    }

    private static String subject(AssistantPlan plan) {
        switch(plan.getGroup()) {
            case PRODUCT:return "产品";
            case WAREHOUSE:return "仓库";
            case DEPT:return "部门";
            case PARTY:
                return Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.RECEIPT,
                        AssistantPlan.Metric.RECEIVABLE).contains(plan.getMetric())?"客户":"供应商";
            default:return "对象";
        }
    }

    private static String metric(AssistantPlan plan) {
        switch(plan.getMetric()) {
            case STOCK:return "库存";
            case STOCK_SKU:return "SKU 数量";
            case SALE:return "销售金额";
            case PURCHASE:return "采购金额";
            case RECEIPT:return "收款金额";
            case PAYMENT:return "付款金额";
            case RECEIVABLE:return "应收余额";
            case PAYABLE:return "应付余额";
            default:return "数值";
        }
    }

    private static String rankingLabel(AssistantPlan plan,String subject) {
        if(plan.getMetric()==AssistantPlan.Metric.STOCK) return subject+"库存数量";
        if(plan.getMetric()==AssistantPlan.Metric.STOCK_SKU) return subject+" SKU 数量";
        return subject+metric(plan);
    }

    private static String unit(Map<?,?> row) {
        Object unit=row.get("unit");return unit==null||String.valueOf(unit).trim().isEmpty()?"":" "+unit;
    }
    private static String value(Map<?,?> row,String key,String fallback) {
        Object value=row.get(key);return value==null?fallback:number(value);
    }
    private static String number(Object value) {
        if(value instanceof Number) try {return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();}
        catch(NumberFormatException ignored) {}
        return String.valueOf(value);
    }
    private static List<?> list(Object value) {return value instanceof List?(List<?>)value:Collections.emptyList();}
}
