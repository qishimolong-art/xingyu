package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Model output is a constrained query description, never SQL. */
@Data
public class AssistantPlan {
    public enum Metric { SALE, PURCHASE, STOCK, STOCK_SKU, RECEIPT, PAYMENT, RECEIVABLE, PAYABLE }
    public enum Group { NONE, DAY, PARTY, PRODUCT, DEPT, WAREHOUSE }
    public enum RankOrder { DESC, ASC }
    public enum PresentationMode { SUMMARY, RANKING, TREND, DISTRIBUTION, DETAIL }
    public enum PriceQueryMode { NONE, ALL, SPECIFIC }
    private Metric metric;
    private Group group = Group.NONE;
    private String period;
    private String startDate;
    private String endDate;
    private String warehouse;
    private String party;
    private String product;
    private String department;
    private Long warehouseId;
    private Long partyId;
    private Long productId;
    private Long departmentId;
    private String stockMode = "ALL";
    private int limit = 10;
    private RankOrder rankOrder = RankOrder.DESC;
    // Server-owned display intent. It is normalized from the current question and is not trusted from model output.
    private PresentationMode presentationMode;
    private String clarification;
    private List<String> choices = new ArrayList<>();
    // Persisted server-only state; never accepted from the model or HTTP query input.
    private boolean includeProductPrices;
    /** Server-owned price intent. NONE with includeProductPrices=true is retained as a legacy ALL query. */
    private PriceQueryMode priceQueryMode = PriceQueryMode.NONE;
    /** Requested product price field when priceQueryMode is SPECIFIC. */
    private String priceField;
    private String pendingField;
    private List<Long> pendingIds = new ArrayList<>();
    /** Model-only structured candidates. The orchestrator validates and clears them before persistence. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ModelEntityMention> entityMentions = new ArrayList<>();

    @Data
    public static class ModelEntityMention {
        private String entityType;
        private String keyword;
        private String role;
    }

    public boolean current() {
        return metric == Metric.STOCK || metric == Metric.STOCK_SKU
                || metric == Metric.RECEIVABLE || metric == Metric.PAYABLE;
    }

    public void validate() {
        if (metric == null || group == null || rankOrder == null || limit < 1 || limit > 50)
            throw new IllegalArgumentException("请明确查询指标，排名最多支持 50 项");
        if (priceQueryMode == null)
            throw new IllegalArgumentException("价格查询类型无效");
        if (priceQueryMode == PriceQueryMode.SPECIFIC && (priceField == null || priceField.trim().isEmpty()))
            throw new IllegalArgumentException("请明确要查询的价格类型");
        if (!Arrays.asList("ALL", "POSITIVE", "NEGATIVE", "AVAILABLE").contains(stockMode))
            throw new IllegalArgumentException("库存口径无效");
        if (current() && (group == Group.DAY || (period != null && !"CURRENT".equals(period))))
            throw new IllegalArgumentException("该指标只支持当前余额，请选择当前时点");
        if (!current() && !Arrays.asList("TODAY", "YESTERDAY", "THIS_WEEK", "LAST_WEEK", "THIS_MONTH", "LAST_MONTH",
                "THIS_YEAR", "LAST_YEAR", "CUSTOM").contains(period))
            throw new IllegalArgumentException("请明确统计时间");
        if (group == Group.WAREHOUSE && metric != Metric.STOCK && metric != Metric.STOCK_SKU)
            throw new IllegalArgumentException("该指标尚未开放仓库维度");
        if (group == Group.PRODUCT && !Arrays.asList(Metric.STOCK, Metric.STOCK_SKU, Metric.SALE, Metric.PURCHASE).contains(metric))
            throw new IllegalArgumentException("该指标尚未开放配件维度");
        if (group == Group.PARTY && (metric == Metric.STOCK || metric == Metric.STOCK_SKU))
            throw new IllegalArgumentException("库存不支持往来单位维度");
        if (group == Group.PRODUCT && (metric == Metric.SALE || metric == Metric.PURCHASE) && (party != null || department != null))
            throw new IllegalArgumentException("配件排名暂不支持同时筛选往来单位或部门，请分别查询");
        if (party != null && (metric == Metric.STOCK || metric == Metric.STOCK_SKU))
            throw new IllegalArgumentException("库存不支持往来单位筛选");
        if (metric == Metric.STOCK_SKU && "AVAILABLE".equals(stockMode))
            throw new IllegalArgumentException("可用库存SKU口径尚未发布，请选择账面库存SKU");
        if (warehouse != null && metric != Metric.STOCK && metric != Metric.STOCK_SKU)
            throw new IllegalArgumentException("该指标尚未开放仓库筛选");
        if (product != null && metric != Metric.STOCK && metric != Metric.STOCK_SKU && group != Group.PRODUCT)
            throw new IllegalArgumentException("按配件筛选时请选择配件统计口径");
        for (String filter : Arrays.asList(warehouse, party, product, department))
            if (filter != null && (filter.trim().isEmpty() || filter.length() > 80))
                throw new IllegalArgumentException("筛选名称长度须为 1 至 80 个字符");
    }

    public LocalDateTime[] range(Clock clock) {
        validate();
        if (current()) return null;
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Shanghai")));
        LocalDate day = now.toLocalDate();
        LocalDateTime start;
        LocalDateTime end = now;
        switch (period) {
            case "TODAY": start = day.atStartOfDay(); break;
            case "YESTERDAY": start = day.minusDays(1).atStartOfDay(); end = day.atStartOfDay(); break;
            case "THIS_WEEK": start = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(); break;
            case "LAST_WEEK": end = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(); start = end.minusWeeks(1); break;
            case "THIS_MONTH": start = day.withDayOfMonth(1).atStartOfDay(); break;
            case "LAST_MONTH": end = day.withDayOfMonth(1).atStartOfDay(); start = end.minusMonths(1); break;
            case "THIS_YEAR": start = day.withDayOfYear(1).atStartOfDay(); break;
            case "LAST_YEAR": end = day.withDayOfYear(1).atStartOfDay(); start = end.minusYears(1); break;
            case "CUSTOM": start = LocalDate.parse(startDate).atStartOfDay(); end = LocalDate.parse(endDate).plusDays(1).atStartOfDay(); if(end.isAfter(now)) end = now; break;
            default: throw new IllegalArgumentException("统计时间无效");
        }
        if (!start.isBefore(end) || Duration.between(start, end).toDays() > 366)
            throw new IllegalArgumentException("请选择不超过 366 天的有效时间范围");
        return new LocalDateTime[]{start, end};
    }
}
