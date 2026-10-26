package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.module.erp.service.assistant.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

/** 复用语义协议，但只执行客户工具，绝不借用后台员工身份。 */
@Service
public class CustomerAssistantSemanticExecutor {
    @Resource private CustomerAssistantToolRegistry tools;
    @Resource private CustomerAssistantQueryService queries;
    private final ObjectMapper json = new ObjectMapper();

    public AssistantUnderstandingResult savedUnderstanding(Map<String,Object> previous) {
        try { return json.convertValue(previous.get("understanding"), AssistantUnderstandingResult.class); }
        catch (Exception ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    public Map<String,Object> execute(AssistantUnderstandingResult understanding, Map<String,Object> previous,
                                      CustomerAssistantPrincipal principal, String choiceField, Long choiceId) {
        if (understanding == null) throw new AssistantFailure("INVALID_CHOICE", "候选项已失效，请重新提问");
        List<Map<String,Object>> components = new ArrayList<>(); Map<String,Object> states = new LinkedHashMap<>();
        Map<?,?> oldStates = previous.get("states") instanceof Map ? (Map<?,?>)previous.get("states") : Collections.emptyMap();
        Set<String> completed = new HashSet<>(); int usable = 0;
        for (AssistantUnderstandingResult.Subtask task : understanding.getTasks()) {
            AssistantTask.checkCurrent();
            Map<String,Object> component = new LinkedHashMap<>(), state = new LinkedHashMap<>();
            component.put("id", task.getId()); component.put("title", task.getTitle()); component.put("type", "DATA");
            component.put("toolName", task.getCapability()); states.put(task.getId(), state);
            try {
                if (!completed.containsAll(task.getDependsOn())) throw new AssistantFailure("DEPENDENCY_UNAVAILABLE", "请先完成依赖任务的确认");
                if (!task.getCapability().startsWith("clarify") && !tools.isAllowed(task.getCapability()))
                    throw new AssistantFailure("CAPABILITY_FORBIDDEN", "该能力未向客户开放，请转人工客服");
                if (task.needsClarification())
                    throw new CustomerAssistantQueryService.ProductChoice(AssistantClarification.missing("MISSING_PARAMETER",
                            task.getClarification() == null || task.getClarification().trim().isEmpty()
                                    ? "请补充商品或订单信息" : task.getClarification(), task.getMissingFields(), Collections.emptyList()));
                Map<String,Object> args = new LinkedHashMap<>();
                String keyword = task.entity("PRODUCT"); args.put("keyword", keyword);
                Map<?,?> old = oldStates.get(task.getId()) instanceof Map ? (Map<?,?>)oldStates.get(task.getId()) : Collections.emptyMap();
                if (choiceId != null && ("task:" + task.getId() + ":productId").equals(choiceField)) {
                    boolean allowed = false;
                    Object pending = old.get("pendingCandidates");
                    if (pending instanceof Collection) for (Object item : (Collection<?>)pending)
                        if (item instanceof Map && ((Map<?,?>)item).get("id") instanceof Number
                                && ((Number)((Map<?,?>)item).get("id")).longValue() == choiceId) allowed = true;
                    if (!allowed) throw new AssistantFailure("INVALID_CHOICE", "候选项已失效，请重新提问");
                    args.put("productId", choiceId);
                } else if (Objects.equals(keyword, old.get("keyword")) && old.get("productId") instanceof Number
                        && (choiceId != null || "INHERIT".equals(task.getContextMode()))) args.put("productId", old.get("productId"));
                state.put("keyword", keyword);
                AssistantUnderstandingResult.Conditions c = task.getConditions();
                if (task.getEntities().stream().anyMatch(e -> !"PRODUCT".equals(e.getEntityType()))
                        || c.getKeyword()!=null || c.getBrand()!=null || c.getStatus()!=null || c.getQuantityOperator()!=null
                        || c.isPriceRequested() && !CustomerAssistantToolRegistry.PRICE.equals(task.getCapability())
                        || c.getPriceField()!=null || !c.getBusinessTypes().isEmpty() || !c.getSubtypes().isEmpty()
                        || !"NONE".equals(c.getGroup()) || !"ALL".equals(c.getMovement()) || !"ALL".equals(c.getStockMode())
                        || !Arrays.asList("ALL","UNFINISHED").contains(c.getFulfillment())
                        || !CustomerAssistantToolRegistry.ORDER.equals(task.getCapability()) && (c.getDocumentNo()!=null || c.getPeriod()!=null && !"CURRENT".equals(c.getPeriod())))
                    throw new AssistantFailure("CAPABILITY_FORBIDDEN", "客户查询不支持这些内部条件，请调整问题或转人工客服");
                if (CustomerAssistantToolRegistry.ORDER.equals(task.getCapability()) && task.entity("PRODUCT")!=null)
                    throw new AssistantFailure("CAPABILITY_FORBIDDEN", "客户订单暂不支持商品筛选，未执行忽略商品的订单查询");
                args.put("orderNo", c.getDocumentNo()); args.put("unfinishedOnly", "UNFINISHED".equals(c.getFulfillment()));
                args.put("startDate", c.getStartDate()); args.put("endDate", c.getEndDate()); args.put("period", c.getPeriod());
                Map<String,Object> result;
                if (CustomerAssistantToolRegistry.PRICE.equals(task.getCapability())) result = queries.price(principal, args);
                else if (CustomerAssistantToolRegistry.STOCK.equals(task.getCapability())) result = queries.stock(principal, args);
                else if (CustomerAssistantToolRegistry.ORDER.equals(task.getCapability())) result = queries.orders(principal, args);
                else throw new AssistantFailure("CAPABILITY_FORBIDDEN", "该能力未向客户开放");
                Object productId = result.remove("_productId");
                if (productId != null) state.put("productId", productId);
                component.put("result", result); component.put("status", result.get("status"));
                completed.add(task.getId()); usable++;
            } catch (CustomerAssistantQueryService.ProductChoice choice) {
                Map<String,Object> response = choice.getResponse(); Object candidates = response.get("candidates");
                if (candidates instanceof List) {
                    state.put("pendingCandidates", candidates);
                    for (Object candidate : (List<?>)candidates) if (candidate instanceof Map)
                        ((Map<String,Object>)candidate).put("field", "task:" + task.getId() + ":productId");
                }
                component.put("status", "CLARIFY"); component.put("clarification", response);
            } catch (AssistantFailure failure) {
                if (Arrays.asList("CANCELLED","TIMEOUT").contains(failure.getCode())) throw failure;
                component.put("status", failure.getCode()); component.put("message", failure.getMessage());
            }
            components.add(component);
        }
        Map<String,Object> result = new LinkedHashMap<>();
        if (components.size() == 1 && components.get(0).get("result") instanceof Map) result.putAll((Map<String,Object>)components.get(0).get("result"));
        else {
            result.put("tasks", components); result.put("summary", Collections.emptyList()); result.put("rows", Collections.emptyList());
            result.put("columns", Collections.emptyList());
            result.put("status", usable == components.size() ? "SUCCESS" : usable > 0 ? "PARTIAL_SUCCESS"
                    : components.size() == 1 ? components.get(0).get("status") : "TASKS_INCOMPLETE");
            if (components.size() == 1 && components.get(0).get("clarification") instanceof Map) {
                result.putAll((Map<String,Object>)components.get(0).get("clarification")); result.put("status", "CLARIFY");
            }
            result.put("warnings", Collections.singletonList("请查看各任务的结果或提示"));
        }
        Map<String,Object> execution = new LinkedHashMap<>(); execution.put("understanding", understanding); execution.put("states", states);
        result.put("_execution", execution); return result;
    }
}
