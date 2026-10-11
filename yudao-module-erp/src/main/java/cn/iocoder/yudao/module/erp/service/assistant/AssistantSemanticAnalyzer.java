package cn.iocoder.yudao.module.erp.service.assistant;

import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V2 的确定性语义前置层。它只把问题归一化为受控结构，不查询业务数据，
 * 也不接受模型生成的 id、权限范围或 SQL。
 */
@Component
public class AssistantSemanticAnalyzer {

    private static final Pattern DATE_RANGE = Pattern.compile(
            "(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?\\s*(?:至|到|~|～|-)\\s*"
                    + "(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?");
    private static final Pattern YEAR_MONTH = Pattern.compile("(20\\d{2})年(\\d{1,2})月");
    private static final Pattern ARABIC_LIMIT = Pattern.compile("(?:前|后|top)(\\d{1,2})", Pattern.CASE_INSENSITIVE);

    public void analyze(AssistantSemanticRequest request, AssistantExecutionEnvelope previous) {
        analyze(request, previous, request.getQuestion());
    }

    public void analyze(AssistantSemanticRequest request, AssistantExecutionEnvelope previous, String rawQuestion) {
        analyzeFrame(request, previous, rawQuestion);
        finalizeRouting(request, previous, rawQuestion);
    }

    /** 先构造与工具选择无关的语义框架，避免由预选能力反向决定用户意图。 */
    public void analyzeFrame(AssistantSemanticRequest request, AssistantExecutionEnvelope previous,
                             String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion;
        String compact = compact(question);
        AssistantIntentGeneralizer.Decision generalized = AssistantIntentGeneralizer.analyze(question);
        request.setDomain(generalized.getDomain().name());
        request.setOperation(generalized.getOperation().name());
        request.setAggregation(generalized.getAggregation());
        request.setEntityRequirement(generalized.getEntityRequirement());
        request.setRoutingConfidence(generalized.getConfidence());
        request.setRoutingReason(generalized.getReason());
        AssistantCompanyKnowledgeIntent.Analysis knowledgeIntent =
                AssistantCompanyKnowledgeIntent.analyze(question);
        if (knowledgeIntent.getTopic() != AssistantCompanyKnowledgeIntent.Topic.NONE
                || knowledgeIntent.isExplicitRealtime()) {
            request.setKnowledgeIntent(knowledgeIntent.toAuditMap());
        } else {
            request.setKnowledgeIntent(new LinkedHashMap<String, Object>());
        }
        request.setMetric(generalized.getMetric() == null ? metric(compact) : generalized.getMetric());
        request.setBusinessObjects(businessObjects(compact));
        request.setDimensions(dimensions(compact));
        request.setDocumentStatuses(documentStatuses(compact));
        AssistantPlan previousPlan = previous == null ? null : previous.getLegacyPlan();
        request.setTimeRange(timeRange(question, request.getMetric(), previousPlan));
        request.setSort(sort(compact));
    }

    /** 能力确定后只补充能力契约相关字段和上下文判定，不重新解释指标与领域。 */
    public void finalizeRouting(AssistantSemanticRequest request, AssistantExecutionEnvelope previous,
                                String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion;
        String compact = compact(question);
        request.setIntentId(intent(request.getCapabilityId(), compact));
        request.setEntityMentions(entityMentions(question, request.getCapabilityId()));
        request.setFilters(filters(request));
        decideContext(request, previous, question);
    }

    /** V2 只有在语义层明确判定为连续追问时才可以看到上一轮执行状态。 */
    public AssistantExecutionEnvelope executionContext(AssistantSemanticRequest request,
                                                       AssistantExecutionEnvelope previous) {
        return request != null && request.getContext() != null
                && "INHERIT".equals(request.getContext().getMode()) ? previous : null;
    }

    public boolean needsContextClarification(AssistantSemanticRequest request) {
        return request != null && request.getContext() != null
                && "MISSING_CONTEXT".equals(request.getContext().getMode())
                && !request.getMissingFields().isEmpty();
    }

    public Map<String, Object> contextClarification(AssistantSemanticRequest request) {
        if (request.getMissingFields().contains("calendarMonth")) {
            return AssistantClarification.missing("MISSING_CALENDAR_MONTH", "请确认该日期是哪个月",
                    request.getMissingFields(), Arrays.asList("上个月", "本月"));
        }
        String text = request.getMissingFields().stream().anyMatch(value -> value.startsWith("entity"))
                ? "请补充要查询的产品、客户、供应商、仓库、部门或用户名称"
                : "这句话依赖上一轮条件，但当前没有可继承的有效上下文，请补充完整查询条件";
        return AssistantClarification.missing("MISSING_CONTEXT", text, request.getMissingFields(),
                Arrays.asList("补充业务对象名称或编码", "重新输入完整问题"));
    }

    /** 把运行期确认的指标、实体类型和时间写回统一请求，模型值不能覆盖服务端确认值。 */
    public void mergeRuntime(AssistantSemanticRequest request, AssistantExecutionEnvelope envelope) {
        if (request == null || envelope == null) return;
        Map<String, Object> semantic = envelope.getSemanticResult();
        AssistantPlan plan = envelope.getLegacyPlan();
        if (plan != null) {
            if (plan.getMetric() != null) request.setMetric(plan.getMetric().name());
            if (plan.getGroup() != null && plan.getGroup() != AssistantPlan.Group.NONE) {
                addUnique(request.getDimensions(), plan.getGroup().name());
            }
            if (plan.getPeriod() != null) {
                AssistantSemanticRequest.TimeRange range = request.getTimeRange();
                if (range == null) range = new AssistantSemanticRequest.TimeRange();
                range.setPeriod(plan.getPeriod());
                range.setStartDate(plan.getStartDate());
                range.setEndDate(plan.getEndDate());
                if (range.getSource() == null) range.setSource("SERVER_NORMALIZED");
                request.setTimeRange(range);
            }
        }
        Object resolvedTypes = semantic.get("resolvedEntityTypes");
        if (resolvedTypes instanceof Collection) {
            for (Object value : (Collection<?>) resolvedTypes) addUnique(request.getBusinessObjects(), String.valueOf(value));
        }
        Object grouping = semantic.get("grouping");
        if (grouping != null && !"NONE".equals(String.valueOf(grouping))) {
            addUnique(request.getDimensions(), String.valueOf(grouping));
        }
        semantic.put("intentId", request.getIntentId());
        semantic.put("domain", request.getDomain());
        semantic.put("operation", request.getOperation());
        semantic.put("aggregation", request.getAggregation());
        semantic.put("entityRequirement", request.getEntityRequirement());
        semantic.put("routingConfidence", request.getRoutingConfidence());
        semantic.put("routingReason", request.getRoutingReason());
        semantic.put("knowledgeIntent", new LinkedHashMap<>(request.getKnowledgeIntent()));
        semantic.put("businessObjects", new ArrayList<>(request.getBusinessObjects()));
        semantic.put("normalizedMetric", request.getMetric());
        semantic.put("dimensions", new ArrayList<>(request.getDimensions()));
        semantic.put("documentStatuses", new ArrayList<>(request.getDocumentStatuses()));
        semantic.put("normalizedTimeRange", request.getTimeRange());
        semantic.put("normalizedSort", request.getSort());
        semantic.put("contextDecision", request.getContext());
        semantic.put("missingFields", new ArrayList<>(request.getMissingFields()));
    }

    private static void decideContext(AssistantSemanticRequest request, AssistantExecutionEnvelope previous,
                                      String question) {
        AssistantSemanticRequest.ContextDecision decision = new AssistantSemanticRequest.ContextDecision();
        List<String> references = AssistantSemanticExtractor.contextReferences(question);
        decision.setReferences(new ArrayList<>(references));
        request.setContext(decision);

        if (AssistantKnowledgeContext.isFollowUp(question, previous)) {
            decision.setMode("INHERIT");
            decision.setReason("KNOWLEDGE_FOLLOW_UP");
            AssistantSemanticRequest previousRequest = previous.getSemanticRequest();
            decision.setSourceRequestId(previousRequest == null ? null : previousRequest.getRequestId());
            inherit(decision, "knowledgeTopic", "PREVIOUS_KNOWLEDGE", "ELLIPTICAL_KNOWLEDGE_FOLLOW_UP");
            request.getInheritedContext().put("capabilityId", AssistantCompanyKnowledge.TOOL_NAME);
            request.getInheritedContext().put("routeType", AssistantExecutionEnvelope.RouteType.KNOWLEDGE.name());
            if (request.getKnowledgeIntent().isEmpty() && previousRequest != null
                    && previousRequest.getKnowledgeIntent() != null
                    && !previousRequest.getKnowledgeIntent().isEmpty()) {
                request.setKnowledgeIntent(new LinkedHashMap<>(previousRequest.getKnowledgeIntent()));
                request.getKnowledgeIntent().put("reason", "INHERITED_KNOWLEDGE_TOPIC");
            }
            return;
        }

        boolean continuation = AssistantModelClient.isContinuationQuestion(question);
        boolean entityReference = references.stream().anyMatch(value -> !"换成".equals(value));
        boolean concreteCurrentEntity = !request.getEntityMentions().isEmpty();
        AssistantPlan previousPlan = previous == null ? null : previous.getLegacyPlan();
        boolean dayOnly = AssistantCalendarDate.isDayOnly(question);
        if (dayOnly && request.getTimeRange() == null && !AssistantCalendarDate.isSingleDay(previousPlan)) {
            decision.setMode("MISSING_CONTEXT");
            decision.setReason(previous == null ? "NO_PREVIOUS_DATE" : "PREVIOUS_DATE_NOT_SINGLE_DAY");
            request.getMissingFields().add("calendarMonth");
            return;
        }
        if (!continuation && !entityReference) {
            decision.setMode("NEW_TOPIC");
            decision.setReason(concreteCurrentEntity ? "CURRENT_ENTITY_STARTS_NEW_TOPIC" : "COMPLETE_CURRENT_QUESTION");
            return;
        }

        Set<String> previousTypes = previousEntityTypes(previous);
        Set<String> expectedTypes = expectedReferenceTypes(references);
        boolean compatible = expectedTypes.isEmpty() ? !previousTypes.isEmpty()
                : previousTypes.stream().anyMatch(expectedTypes::contains);
        if (previous == null || entityReference && !compatible) {
            decision.setMode("MISSING_CONTEXT");
            decision.setReason(previous == null ? "NO_PREVIOUS_TURN" : "ENTITY_REFERENCE_TYPE_MISMATCH");
            request.getMissingFields().add(entityReference ? "entity" : "context");
            return;
        }

        // A concrete entity in the current question is always resolved again and must not silently reuse an old id.
        if (concreteCurrentEntity && !entityReference) {
            decision.setMode("NEW_TOPIC");
            decision.setReason("CURRENT_ENTITY_OVERRIDES_PREVIOUS");
            return;
        }

        decision.setMode("INHERIT");
        boolean metricSwitch = dayOnly && previousPlan != null && request.getMetric() != null
                && previousPlan.getMetric() != null && !request.getMetric().equals(previousPlan.getMetric().name());
        decision.setReason(metricSwitch ? "DATE_YEAR_MONTH_FOLLOW_UP"
                : entityReference ? "EXPLICIT_CONTEXT_REFERENCE" : "ELLIPTICAL_FOLLOW_UP");
        AssistantSemanticRequest previousRequest = previous.getSemanticRequest();
        decision.setSourceRequestId(previousRequest == null ? null : previousRequest.getRequestId());

        if (!metricSwitch && !previousTypes.isEmpty()) {
            inherit(decision, "businessObject", "PREVIOUS_RESOLVED_ENTITY", decision.getReason());
            request.getInheritedContext().put("entityTypes", new ArrayList<>(previousTypes));
            for(String type:previousTypes) addUnique(request.getBusinessObjects(),type);
        }
        if (request.getMetric() == null && previousPlan != null && previousPlan.getMetric() != null) {
            request.setMetric(previousPlan.getMetric().name());
            inherit(decision, "metric", "PREVIOUS_PLAN", "CURRENT_METRIC_OMITTED");
            request.getInheritedContext().put("metric", previousPlan.getMetric().name());
        }
        if (request.getTimeRange() == null && previousPlan != null && previousPlan.getPeriod() != null
                && canInheritPeriod(request.getMetric(),previousPlan.getPeriod())) {
            AssistantSemanticRequest.TimeRange inherited = new AssistantSemanticRequest.TimeRange();
            inherited.setPeriod(previousPlan.getPeriod());
            inherited.setStartDate(previousPlan.getStartDate());
            inherited.setEndDate(previousPlan.getEndDate());
            inherited.setSource("INHERITED");
            request.setTimeRange(inherited);
            inherit(decision, "timeRange", "PREVIOUS_PLAN", "CURRENT_TIME_OMITTED");
            request.getInheritedContext().put("period", previousPlan.getPeriod());
        }
        if (!metricSwitch && request.getDimensions().isEmpty() && previousPlan != null && previousPlan.getGroup() != null
                && previousPlan.getGroup() != AssistantPlan.Group.NONE) {
            request.getDimensions().add(previousPlan.getGroup().name());
            inherit(decision, "dimensions", "PREVIOUS_PLAN", "CURRENT_DIMENSION_OMITTED");
        }
        if (previous.getToolName() != null) request.getInheritedContext().put("capabilityId", previous.getToolName());
        if (previous.getRouteType() != null) request.getInheritedContext().put("routeType", previous.getRouteType().name());
    }

    private static String intent(String capability, String text) {
        if (AssistantCompanyKnowledge.TOOL_NAME.equals(capability)) return "COMPANY_KNOWLEDGE";
        if (AssistantMixedQuery.TOOL_NAME.equals(capability)) return "MIXED_QUERY";
        if ("query_master_data_stats".equals(capability)) return "MASTER_DATA_STATS";
        if ("resolve_business_entity".equals(capability)) return "ENTITY_LOOKUP";
        if (capability != null && capability.contains("fulfillment")) return "FULFILLMENT_QUERY";
        if (capability != null && capability.contains("documents")) return "DOCUMENT_QUERY";
        if ("query_stock_movements".equals(capability)) return "STOCK_MOVEMENT_QUERY";
        if (AssistantStage5BusinessQuery.MASTER_TOOL.equals(capability)) return "MASTER_DATA_DETAIL_QUERY";
        if (AssistantStage5BusinessQuery.ORGANIZATION_TOOL.equals(capability)) return "ORGANIZATION_QUERY";
        if (AssistantStage5BusinessQuery.STOCK_TOOL.equals(capability)) return "STOCK_DOCUMENT_QUERY";
        if (AssistantStage5BusinessQuery.SALE_TOOL.equals(capability)) return "SALE_BUSINESS_QUERY";
        if (AssistantStage5BusinessQuery.PURCHASE_TOOL.equals(capability)) return "PURCHASE_BUSINESS_QUERY";
        if (AssistantStage5BusinessQuery.FINANCE_TOOL.equals(capability)) return "FINANCE_BUSINESS_QUERY";
        if (Arrays.asList("query_verified_metric", "query_product_stock", "query_inventory_details",
                "query_cash_documents", "query_account_balance").contains(capability)) return "METRIC_QUERY";
        if (text.matches(".*(?:查询|查看|统计|多少|排名|排行|趋势|明细|情况).*")) return "QUERY";
        return "UNKNOWN";
    }

    private static String metric(String text) {
        if (text.matches(".*(?:应收|客户余额|客户欠款|欠钱).*")) return "RECEIVABLE";
        if (text.matches(".*(?:应付|供应商余额).*")) return "PAYABLE";
        if (text.matches(".*(?:收款|回款|收了多少钱|收回).*")) return "RECEIPT";
        if (text.matches(".*(?:付款|付了多少钱).*")) return "PAYMENT";
        if (AssistantIntentGeneralizer.isGenericStockSkuCount(text)) return "STOCK_SKU";
        if (text.matches(".*(?:库存|有货|缺货).*")) return "STOCK";
        if (text.matches(".*(?:采购|进货).*")) return "PURCHASE";
        if (text.matches(".*(?:销售|销量|销售额|销售了多少|卖了多少).*")) return "SALE";
        if (text.matches(".*(?:员工|职工|用户).*(?:数量|多少|几个|总数).*")) return "USER_COUNT";
        return null;
    }

    private static List<String> businessObjects(String text) {
        List<String> result = new ArrayList<>();
        if (text.matches(".*(?:产品|配件|商品|sku).*")) addUnique(result, "PRODUCT");
        if (text.contains("客户")) addUnique(result, "CUSTOMER");
        if (text.contains("供应商")) addUnique(result, "SUPPLIER");
        if (text.contains("仓库") || text.matches(".*[\\p{IsHan}a-z0-9]{1,20}仓(?:里|中|内|的|有|$).*")) addUnique(result, "WAREHOUSE");
        if (text.matches(".*(?:部门|分公司|事业部|办事处).*")) addUnique(result, "DEPARTMENT");
        if (text.matches(".*(?:业务员|销售员|业务经理).*")) addUnique(result, "SALESPERSON");
        if (text.matches(".*(?:采购员|采购人员|采购经办人).*")) addUnique(result, "PURCHASER");
        if (text.matches(".*(?:系统用户|用户|员工|职工).*")) addUnique(result, "USER");
        return result;
    }

    private static List<String> dimensions(String text) {
        List<String> result = new ArrayList<>();
        if (text.matches(".*(?:按天|每天|逐日|日趋势|日报).*")) addUnique(result, "DAY");
        if (text.matches(".*(?:按客户|各客户|每个客户|客户排名).*")) addUnique(result, "CUSTOMER");
        if (text.matches(".*(?:按供应商|各供应商|每个供应商|供应商排名).*")) addUnique(result, "SUPPLIER");
        if (text.matches(".*(?:按产品|按配件|各产品|各配件|产品排名|配件排名|(?:最多|最少).*(?:产品|配件|商品)).*")) addUnique(result, "PRODUCT");
        if (text.matches(".*(?:按仓库|各仓库|每个仓库|仓库排名).*")) addUnique(result, "WAREHOUSE");
        if (text.matches(".*(?:按部门|各部门|每个部门|部门排名).*")) addUnique(result, "DEPARTMENT");
        if (text.matches(".*(?:按业务员|各业务员|每个业务员|业务员排名|销售员排名).*")) addUnique(result, "SALESPERSON");
        if (text.matches(".*(?:按采购员|各采购员|每个采购员|采购员排名).*")) addUnique(result, "PURCHASER");
        if (text.matches(".*(?:按用户|按员工|各用户|各员工).*")) addUnique(result, "USER");
        return result;
    }

    private static List<String> documentStatuses(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (text.matches(".*(?:草稿|未提交).*")) result.add("DRAFT");
        if (text.matches(".*(?:待审核|审核中|审批中|待审批).*")) result.add("PROCESSING");
        if (text.matches(".*(?:已审核|审核通过|已审批|审批通过).*")) result.add("APPROVED");
        if (text.matches(".*(?:已作废|作废).*")) result.add("VOIDED");
        if (text.matches(".*(?:已完成|全部到货|全部发货|已结清).*")) result.add("COMPLETED");
        if (text.matches(".*(?:未完成|未到货|未发货|未结清|还没(?:有)?(?:发完|到齐|到完)|没有(?:发完|到齐|到完)|剩余).*")) result.add("UNCOMPLETED");
        if (text.matches(".*(?:启用|正常|有效).*")) result.add("ENABLED");
        if (text.matches(".*(?:停用|禁用|无效).*")) result.add("DISABLED");
        return new ArrayList<>(result);
    }

    private static AssistantSemanticRequest.TimeRange timeRange(String question, String metric, AssistantPlan previousPlan) {
        String text = compact(question);
        AssistantCalendarDate.Resolution calendar = AssistantCalendarDate.resolve(question, previousPlan);
        calendar.requireValid();
        if (calendar.isResolved()) return range("CUSTOM", calendar.getDate().toString(),
                calendar.getDate().toString(), calendar.getSource().name());
        if (calendar.isMissingContext()) return null;
        AssistantRelativePeriod.Resolution rolling = AssistantRelativePeriod.resolve(text);
        rolling.requireValid();
        if (rolling.isValid()) return range("CUSTOM", rolling.getStartDate().toString(),
                rolling.getEndDate().toString(), "EXPLICIT");
        String period = AssistantModelClient.explicitRelativePeriod(text);
        if (period != null) return range(period, null, null, "EXPLICIT");
        Matcher dates = DATE_RANGE.matcher(question);
        if (dates.find()) {
            try {
                LocalDate start = LocalDate.of(integer(dates, 1), integer(dates, 2), integer(dates, 3));
                LocalDate end = LocalDate.of(integer(dates, 4), integer(dates, 5), integer(dates, 6));
                if (!start.isAfter(end)) return range("CUSTOM", start.toString(), end.toString(), "EXPLICIT");
            } catch (DateTimeException ignored) {
                // Invalid dates remain unresolved and will be clarified by the plan validator.
            }
        }
        Matcher month = YEAR_MONTH.matcher(question);
        if (month.find()) {
            try {
                YearMonth value = YearMonth.of(integer(month, 1), integer(month, 2));
                return range("CUSTOM", value.atDay(1).toString(), value.atEndOfMonth().toString(), "EXPLICIT");
            } catch (DateTimeException ignored) {
                return null;
            }
        }
        if (Arrays.asList("STOCK", "STOCK_SKU", "RECEIVABLE", "PAYABLE").contains(metric)) {
            return range("CURRENT", null, null, "CURRENT");
        }
        return null;
    }

    private static AssistantSemanticRequest.SortSpec sort(String text) {
        boolean ranking = text.matches(".*(?:最多|最高|最大|最少|最低|最小|排名|排行|前[0-9一二三四五六七八九十]+|后[0-9一二三四五六七八九十]+).*" );
        if (!ranking) return null;
        AssistantSemanticRequest.SortSpec result = new AssistantSemanticRequest.SortSpec();
        result.setDirection(text.matches(".*(?:最少|最低|最小|后[0-9一二三四五六七八九十]+).*" ) ? "ASC" : "DESC");
        result.setLimit(limit(text));
        result.setSource("EXPLICIT");
        return result;
    }

    private static List<AssistantSemanticRequest.EntityMention> entityMentions(String question, String capability) {
        List<AssistantSemanticRequest.EntityMention> result = new ArrayList<>();
        for (AssistantSemanticExtractor.Reference reference : AssistantSemanticExtractor.extract(question, capability)) {
            AssistantSemanticRequest.EntityMention mention = new AssistantSemanticRequest.EntityMention();
            mention.setEntityType(reference.type.name());
            mention.setKeyword(AssistantModelClient.redact(reference.keyword));
            mention.setRole(reference.role);
            mention.setExtractionMethod(reference.matchType);
            result.add(mention);
        }
        return result;
    }

    private static List<AssistantSemanticRequest.SemanticFilter> filters(AssistantSemanticRequest request) {
        List<AssistantSemanticRequest.SemanticFilter> result = new ArrayList<>();
        for (AssistantSemanticRequest.EntityMention mention : request.getEntityMentions()) {
            AssistantSemanticRequest.SemanticFilter filter = new AssistantSemanticRequest.SemanticFilter();
            filter.setField("entity:" + mention.getEntityType());
            filter.setOperator("RESOLVE");
            filter.setValue(mention.getKeyword());
            filter.setSource("CURRENT_QUESTION");
            result.add(filter);
        }
        if (!request.getDocumentStatuses().isEmpty()) {
            AssistantSemanticRequest.SemanticFilter filter = new AssistantSemanticRequest.SemanticFilter();
            filter.setField("documentStatus");
            filter.setOperator("IN");
            filter.setValue(new ArrayList<>(request.getDocumentStatuses()));
            filter.setSource("CURRENT_QUESTION");
            result.add(filter);
        }
        return result;
    }

    private static Set<String> previousEntityTypes(AssistantExecutionEnvelope previous) {
        Set<String> result = new LinkedHashSet<>();
        if (previous == null || previous.getArguments() == null) return result;
        Object all = previous.getArguments().get("resolvedEntities");
        if (all instanceof Map) {
            for (Object value : ((Map<?, ?>) all).values()) {
                if (value instanceof Map && ((Map<?, ?>) value).get("entityType") != null) {
                    result.add(String.valueOf(((Map<?, ?>) value).get("entityType")));
                }
            }
        }
        Object primary = previous.getArguments().get("resolvedEntity");
        if (primary instanceof Map && ((Map<?, ?>) primary).get("entityType") != null) {
            result.add(String.valueOf(((Map<?, ?>) primary).get("entityType")));
        }
        return result;
    }

    private static Set<String> expectedReferenceTypes(Collection<String> references) {
        Set<String> result = new LinkedHashSet<>();
        for (String reference : references) {
            if (reference.contains("产品") || reference.contains("配件") || "它".equals(reference)) result.add("PRODUCT");
            else if (reference.contains("客户")) result.add("CUSTOMER");
            else if (reference.contains("供应商")) result.add("SUPPLIER");
            else if (reference.contains("仓库")) result.add("WAREHOUSE");
            else if (reference.contains("部门")) result.add("DEPARTMENT");
            else if (reference.contains("用户") || reference.contains("员工")) result.add("USER");
        }
        return result;
    }

    private static void inherit(AssistantSemanticRequest.ContextDecision decision, String field,
                                String source, String reason) {
        AssistantSemanticRequest.InheritedField value = new AssistantSemanticRequest.InheritedField();
        value.setField(field);
        value.setSource(source);
        value.setReason(reason);
        decision.getInheritedFields().add(value);
    }

    private static AssistantSemanticRequest.TimeRange range(String period, String start, String end, String source) {
        AssistantSemanticRequest.TimeRange result = new AssistantSemanticRequest.TimeRange();
        result.setPeriod(period);
        result.setStartDate(start);
        result.setEndDate(end);
        result.setSource(source);
        return result;
    }

    private static boolean canInheritPeriod(String metric,String period) {
        if(metric==null || period==null) return true;
        boolean currentMetric=Arrays.asList("STOCK","STOCK_SKU","RECEIVABLE","PAYABLE").contains(metric);
        return currentMetric?"CURRENT".equals(period):!"CURRENT".equals(period);
    }

    private static int integer(Matcher matcher, int group) {
        return Integer.parseInt(matcher.group(group));
    }

    private static int limit(String text) {
        Matcher matcher = ARABIC_LIMIT.matcher(text);
        if (matcher.find()) return Math.max(1, Math.min(50, Integer.parseInt(matcher.group(1))));
        String[] values = {"十", "九", "八", "七", "六", "五", "四", "三", "二", "一"};
        for (int i = 0; i < values.length; i++) {
            if (text.matches(".*(?:前|后)" + values[i] + ".*")) return 10 - i;
        }
        return 10;
    }

    private static void addUnique(List<String> values, String value) {
        if (value != null && !values.contains(value)) values.add(value);
    }

    private static String compact(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s\\u3000，,。；;：:？?！!]", "");
    }
}
