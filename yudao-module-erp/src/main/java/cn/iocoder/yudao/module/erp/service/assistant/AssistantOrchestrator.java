package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Hybrid routing: verified metric first, dedicated tools second, semantic SQL last. */
@Service
public class AssistantOrchestrator {
    @Resource private AssistantProperties properties;
    @Resource private AssistantModelClient model;
    @Resource private AssistantKnowledge knowledge;
    @Resource private AssistantQueryService queries;
    @Resource private AssistantToolRegistry registry;
    @Resource private AssistantSemanticCatalog semanticCatalog;
    @Resource private AssistantSemanticQueryService semantic;
    @Resource private AssistantEntityResolver entities;
    @Resource private AssistantSemanticKnowledge semanticKnowledge;
    @Resource private AssistantMasterDataQueryService masterData;

    public boolean enabledForCurrentUser() {
        String mode=properties.getOrchestration().getMode();
        if("NEW".equalsIgnoreCase(mode)) return true;
        Long user=SecurityFrameworkUtils.getLoginUserId();
        return "GRAY".equalsIgnoreCase(mode) && user!=null && properties.getOrchestration().getGrayUserIds().contains(user);
    }

    public Outcome execute(String question,AssistantExecutionEnvelope previous,List<Map<String,Object>> metricCatalog) {
        AssistantSemanticKnowledge.Match semanticMatch=semanticKnowledge.match(question);
        if(semanticMatch.unverified()) {
            AssistantExecutionEnvelope rejected=new AssistantExecutionEnvelope();
            rejected.setQuestion(AssistantModelClient.redact(question));rejected.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
            rejected.setToolName("capability_unverified");initializeSemanticResult(rejected,question,"capability_unverified");
            applySemanticKnowledge(rejected,semanticMatch);
            throw new AssistantFailure("CAPABILITY_UNVERIFIED",unverifiedCapabilityText(question)).attachEnvelope(rejected);
        }
        AssistantPlan previousPlan=previous==null?null:previous.getLegacyPlan();
        Map<String,Object> inheritedEntity=resolvedEntity(previous,question);
        Map<String,Object> inheritedEntities=resolvedEntities(previous,question);
        String currentEntityKeyword=currentIndependentEntityKeyword(question);
        if(isShortEntityMetricFollowup(question) && inheritedEntity==null) {
            if(hasPendingEntityChoice(previous))
                throw new AssistantOrchestrationClarification(previous,pendingEntityChoiceResponse(previous));
            throw new AssistantFailure("CLARIFY","请先说明要查询哪个产品、客户或部门的销量");
        }
        String deterministic=deterministicPeriodContinuation(question,previousPlan)
                ?"query_verified_metric":deterministicTool(question);
        AssistantToolCall call;
        if(deterministic!=null) {
            call=new AssistantToolCall();call.setId(UUID.randomUUID().toString());call.setName(deterministic);
            Map<String,Object> arguments=new LinkedHashMap<>();
            if(AssistantMasterDataQueryService.TOOL_NAME.equals(deterministic)) {
                AssistantMasterDataQueryService.Query query=masterData.parse(question);
                arguments.put("question",question);arguments.put("archiveType",query.getArchiveType().name());
                arguments.put("statusMode",query.getStatusMode().name());
            } else if("resolve_business_entity".equals(deterministic)) arguments.put("keyword",bareEntityKeyword(question));
            else arguments.put("question",question);
            call.setArguments(arguments);
        }
        else {
            List<String> available=availableTools();
            if(available.isEmpty()) throw new AssistantFailure("FORBIDDEN","当前账号没有可用的问数工具");
            call=model.chooseTool(question,AssistantModelClient.isContinuationQuestion(question)?context(previous):Collections.emptyMap(),registry.definitions(available));
        }
        registry.validate(call);
        if(coveredMetric(question,call.getName())) call.setName("query_verified_metric");
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();envelope.setQuestion(AssistantModelClient.redact(question));envelope.setToolName(call.getName());envelope.setArguments(safeArguments(call.getArguments()));
        envelope.setRouteType(routeType(call.getName()));
        initializeSemanticResult(envelope,question,call.getName());
        applySemanticKnowledge(envelope,semanticMatch);
        Map<String,Object> orchestrationTrace=queryTrace(envelope);orchestrationTrace.put("stage","ROUTING");
        if(AssistantModelClient.isStockRankingQuestion(question)) orchestrationTrace.put("entityResolutionSkipped",true);
        record(envelope,call.getId(),call.getName(),safeArguments(call.getArguments()));
        try {
        orchestrationTrace.put("stage","ENTITY_RESOLUTION");
        if(!AssistantMasterDataQueryService.TOOL_NAME.equals(call.getName()) && entities!=null && entities.enabled()) {
            if(currentEntityKeyword!=null && inheritedEntity==null) {
                resolveEntityOrClarify(envelope,null,currentEntityKeyword,"QUERY");
                Map<String,Object> current=primaryResolvedEntity(envelope);
                if(current!=null) {
                    current.put("source","CURRENT");current.put("keyword",currentEntityKeyword);
                    putResolvedEntity(envelope,current,true);
                }
            } else if(inheritedEntity!=null) {
                inheritedEntity=revalidateInheritedEntity(inheritedEntity);
                putResolvedEntity(envelope,inheritedEntity,true);
                recordResolvedMention(envelope,inheritedEntity,inheritedSource(inheritedEntity),false,"CONTEXT_REFERENCE");
                if(inheritedEntities!=null) for(Object item:inheritedEntities.values())
                    if(item instanceof Map) {
                        Map<String,Object> inherited=new LinkedHashMap<>((Map<String,Object>)item);
                        if(sameEntity(inheritedEntity,inherited)) inherited=inheritedEntity;
                        else inherited=revalidateInheritedEntity(inherited);
                        putResolvedEntity(envelope,inherited,false);recordResolvedMention(envelope,inherited,inheritedSource(inherited),false,"CONTEXT_REFERENCE");
                    }
            } else {
                String bareKeyword=bareEntityKeyword(question);
                if(bareKeyword!=null && !Arrays.asList("query_product_stock","resolve_business_entity").contains(call.getName()))
                    resolveEntityOrClarify(envelope,null,bareKeyword,"BARE");
            }
            resolveCurrentEntityMentions(envelope,question,call.getName());
            resolveStockCompoundEntities(envelope,question,call.getName());
            resolveSupplementaryModelMentions(envelope,question,call.getName());
            ensureExplicitReferencesResolved(envelope,question);
        }
        orchestrationTrace.put("stage","PLAN_BUILD");
        Map<String,Object> result;
        switch(call.getName()) {
            case "query_master_data_stats":
                AssistantMasterDataQueryService.Query masterQuery=masterData.parseArguments(call.getArguments());
                if(masterQuery==null || masterData.parse(question)==null)
                    throw new AssistantFailure("MODEL_INVALID","基础档案统计问题或参数无效");
                envelope.getArguments().put("archiveType",masterQuery.getArchiveType().name());
                envelope.getArguments().put("statusMode",masterQuery.getStatusMode().name());
                envelope.getSemanticResult().put("intentId","MASTER_DATA_STATS");
                envelope.getSemanticResult().put("capabilityId","MASTER_DATA_STATS");
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
                result=masterData.execute(masterQuery);break;
            case "query_verified_metric":
                AssistantPlan plan=deterministicVerifiedPlan(question,primaryResolvedEntity(envelope),previousPlan);
                boolean deterministicPlan=plan!=null;
                if(plan==null) plan=model.parse(question,AssistantModelClient.continuationContext(question,previousPlan),knowledge.retrieve(question,previousPlan,metricCatalog));
                AssistantModelClient.normalizePlan(question,AssistantModelClient.continuationContext(question,previousPlan),plan);
                Map<String,Object> planTrace=queryTrace(envelope);planTrace.put("planSource",deterministicPlan?"DETERMINISTIC":"MODEL");
                planTrace.put("normalizedMetric",plan.getMetric()==null?null:plan.getMetric().name());planTrace.put("normalizedPeriod",plan.getPeriod());
                resolveModelPlanMentions(envelope,question,plan);
                AssistantPresentation.prepare(question,plan);
                applyResolvedEntity(envelope,plan);
                capturePlanSemantics(envelope,plan);validateToolEntityContract(envelope,call.getName(),plan);
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC);envelope.setLegacyPlan(plan);
                addRankingTrace(question,envelope,plan);
                if(plan.getClarification()!=null && !plan.getClarification().trim().isEmpty()) {envelope.setClarification(plan.getClarification());return Outcome.clarify(envelope,plan);}
                result=executePlan(envelope,plan);decorate(result,envelope,"已核验指标");break;
            case "query_product_stock":
                AssistantPlan stock=productStockPlan(question,call.getArguments());
                if(entities!=null && entities.enabled() && envelope.getArguments().get("resolvedEntity")==null && stock.getProduct()!=null)
                    resolveEntityOrClarify(envelope,"PRODUCT",stock.getProduct(),"QUERY");
                AssistantModelClient.normalizePlan(question,null,stock);envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);envelope.setLegacyPlan(stock);
                AssistantPresentation.prepare(question,stock);
                applyResolvedEntity(envelope,stock);
                capturePlanSemantics(envelope,stock);validateToolEntityContract(envelope,call.getName(),stock);
                if(stock.getClarification()!=null && !stock.getClarification().trim().isEmpty()){envelope.setClarification(stock.getClarification());return Outcome.clarify(envelope,stock);}
                result=executePlan(envelope,stock);decorate(result,envelope,"产品价格/库存工具");break;
            case "query_inventory_details":
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
                result=dedicatedSemanticTool(question,"inventory_current",call.getName(),envelope);break;
            case "query_cash_documents": case "query_account_balance":
                AssistantPlan finance=model.parse(question,AssistantModelClient.continuationContext(question,previousPlan),knowledge.retrieve(question,previousPlan,metricCatalog));
                resolveModelPlanMentions(envelope,question,finance);
                AssistantPresentation.prepare(question,finance);
                applyResolvedEntity(envelope,finance);
                capturePlanSemantics(envelope,finance);validateToolEntityContract(envelope,call.getName(),finance);
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);envelope.setLegacyPlan(finance);
                if(finance.getClarification()!=null && !finance.getClarification().trim().isEmpty()){envelope.setClarification(finance.getClarification());return Outcome.clarify(envelope,finance);}
                result=executePlan(envelope,finance);decorate(result,envelope,"财务业务工具");break;
            case "resolve_business_entity":
                String bareKeyword=stringValue(call.getArguments().get("keyword"));
                if(bareKeyword==null) bareKeyword=bareEntityKeyword(question);
                resolveEntityOrClarify(envelope,stringValue(call.getArguments().get("entityType")),bareKeyword,"BARE_DEFAULT");
                Map<String,Object> resolved=primaryResolvedEntity(envelope);
                if(resolved!=null && "PRODUCT".equals(resolved.get("entityType"))) {
                    Map<String,Object> productArgument=new LinkedHashMap<>();productArgument.put("product",resolved.get("name"));
                    AssistantPlan defaultProduct=productStockPlan(String.valueOf(resolved.get("name"))+" 价格和库存",productArgument);
                    applyResolvedEntity(envelope,defaultProduct);AssistantPresentation.prepare(question,defaultProduct);
                    capturePlanSemantics(envelope,defaultProduct);validateToolEntityContract(envelope,"query_product_stock",defaultProduct);
                    envelope.setToolName("query_product_stock");envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
                    envelope.setLegacyPlan(defaultProduct);result=executePlan(envelope,defaultProduct);decorate(result,envelope,"产品价格/库存工具");break;
                }
                throw new AssistantOrchestrationClarification(envelope,purposeResponse(resolved));
            case "query_purchase_documents": case "query_purchase_fulfillment":
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
                result=dedicatedSemanticTool(question,"purchase_orders",call.getName(),envelope);break;
            case "query_sale_documents": case "query_sale_fulfillment":
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
                result=dedicatedSemanticTool(question,saleOrderDataset(question,envelope),call.getName(),envelope);break;
            case "query_stock_movements":
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
                result=dedicatedSemanticTool(question,"stock_movements",call.getName(),envelope);break;
            case "query_semantic_schema": case "execute_semantic_query":
                if(!properties.getOrchestration().isTextToSqlEnabled()) throw new AssistantFailure("TEXT_TO_SQL_DISABLED","智能自由查询尚未向当前环境开放");
                envelope.setRouteType(AssistantExecutionEnvelope.RouteType.TEXT_TO_SQL);
                result=semanticFallback(question,envelope);break;
            default: throw new AssistantFailure("MODEL_INVALID","未注册的查询工具");
        }
        captureRankedEntity(envelope,result);
        AssistantPresentation.apply(question,envelope.getLegacyPlan(),result);
        applySemanticResultStatus(envelope,result);
        envelope.getResultMetadata().put("status",result.get("status"));envelope.getResultMetadata().put("queryType",result.get("queryType"));
        envelope.getResultMetadata().put("presentation",result.get("presentation"));
        Object queryTrace=result.remove("queryTrace");if(queryTrace instanceof Map) envelope.getResultMetadata().put("queryTrace",queryTrace);
        else orchestrationTrace.put("stage","COMPLETED");
        return Outcome.result(envelope,result);
        } catch(AssistantFailure failure) {
            orchestrationTrace.put("failureCode",failure.getCode());
            envelope.getSemanticResult().put("outcomeStatus",outcomeStatus(failure.getCode()));
            throw failure.attachEnvelope(envelope);
        }
    }

    public Outcome resumeChoice(AssistantExecutionEnvelope envelope,String field,Long id,List<Map<String,Object>> metricCatalog) {
        return resumeChoice(envelope,field,id,null,metricCatalog);
    }

    public Outcome resumeChoice(AssistantExecutionEnvelope envelope,String field,Long id,String question,List<Map<String,Object>> metricCatalog) {
        if(envelope==null || envelope.getArguments()==null || field==null)
            throw new AssistantFailure("INVALID_CHOICE","候选项已失效，请重新提问");
        if(field.startsWith("resolved:")) return resumeResolvedChoice(envelope,field,id,question,metricCatalog);
        if(!field.startsWith("entity:")) throw new AssistantFailure("INVALID_CHOICE","候选项已失效，请重新提问");
        AssistantEntityResolver.EntityType type;
        try {type=AssistantEntityResolver.EntityType.valueOf(field.substring("entity:".length()));}
        catch(Exception e){throw new AssistantFailure("INVALID_CHOICE","候选类型无效");}
        List<Map<String,Object>> pending=listOfMaps(envelope.getArguments().get("pendingEntities"));
        boolean allowed=false;
        for(Map<String,Object> candidate:pending) if(type.name().equals(candidate.get("entityType"))
                && candidate.get("id") instanceof Number && ((Number)candidate.get("id")).longValue()==id) {allowed=true;break;}
        if(!allowed) throw new AssistantFailure("INVALID_CHOICE","候选项已失效，请重新提问");
        Map<String,Object> selected=entities.validate(type,id);
        String slot=stringValue(envelope.getArguments().get("pendingEntitySlot"));
        String pendingKeyword=stringValue(envelope.getArguments().get("pendingEntityKeyword"));
        String pendingMode=stringValue(envelope.getArguments().get("pendingMode"));
        selected.put("source","CLARIFICATION");if(pendingKeyword!=null) selected.put("keyword",pendingKeyword);
        envelope.getArguments().remove("pendingEntities");envelope.getArguments().remove("pendingEntitySlot");
        envelope.getArguments().remove("pendingEntityKeyword");envelope.getArguments().remove("pendingMode");
        putResolvedEntity(envelope,selected,slot==null || envelope.getArguments().get("resolvedEntity")==null);
        recordMention(envelope,type.name(),pendingKeyword==null?String.valueOf(selected.get("name")):pendingKeyword,type.name(),"CLARIFICATION",true,
                String.valueOf(selected.get("matchType")),"RESOLVED");
        markMentionResolved(envelope,type.name(),pendingKeyword==null?String.valueOf(selected.get("name")):pendingKeyword,selected);
        if("QUERY".equals(pendingMode)) return execute(envelope.getQuestion()+"；补充：使用已选择对象",envelope,metricCatalog);
        if("BARE_DEFAULT".equals(pendingMode) && type==AssistantEntityResolver.EntityType.PRODUCT)
            return execute("这个产品；补充：查询价格和当前库存",envelope,metricCatalog);
        throw new AssistantOrchestrationClarification(envelope,purposeResponse(selected));
    }

    private Outcome resumeResolvedChoice(AssistantExecutionEnvelope envelope,String field,Long id,String question,List<Map<String,Object>> metricCatalog) {
        AssistantEntityResolver.EntityType type;
        try {type=AssistantEntityResolver.EntityType.valueOf(field.substring("resolved:".length()));}
        catch(Exception e){throw new AssistantFailure("INVALID_CHOICE","候选类型无效");}
        Map<String,Object> existing=resolvedEntityByType(envelope,type.name());
        if(existing==null || !(existing.get("id") instanceof Number) || ((Number)existing.get("id")).longValue()!=id)
            throw new AssistantFailure("INVALID_CHOICE","候选项已失效，请重新提问");
        String next=question==null || question.trim().isEmpty()
                ?envelope.getQuestion()+"；补充：使用已选择对象"
                :question;
        return execute(next,envelope,metricCatalog);
    }

    private void resolveEntityOrClarify(AssistantExecutionEnvelope envelope,String typeValue,String keyword,String mode) {
        resolveEntityOrClarify(envelope,typeValue,keyword,mode,null,true);
    }

    private void resolveEntityOrClarify(AssistantExecutionEnvelope envelope,String typeValue,String keyword,String mode,String slot,boolean primary) {
        if(keyword==null || keyword.trim().isEmpty()) throw new AssistantFailure("CLARIFY","请提供要查找的对象关键词");
        List<Map<String,Object>> matches;
        try {
            if(typeValue==null) matches=entities.resolveAcrossTypes(keyword);
            else matches=entities.resolveType(AssistantEntityResolver.EntityType.valueOf(typeValue.toUpperCase(Locale.ROOT)),keyword);
        } catch(IllegalArgumentException e) {
            throw new AssistantFailure("MODEL_INVALID","模型返回了不支持的对象类型");
        } catch(AssistantFailure failure) {
            markMentionStatus(envelope,typeValue,keyword,"FORBIDDEN");
            if(Arrays.asList("FORBIDDEN","FIELD_FORBIDDEN").contains(failure.getCode()))
                throw new AssistantFailure("NO_PERMISSION","没有权限查询该业务对象或相关字段");
            throw failure;
        }
        if(matches.isEmpty()) {
            markMentionStatus(envelope,typeValue,keyword,"NOT_FOUND");
            throw new AssistantFailure("ENTITY_NOT_FOUND",entityNotFoundText(typeValue));
        }
        if(matches.size()>20) throw new AssistantFailure("CLARIFY_ENTITY","匹配对象较多，请填写更完整的名称或完整编码");
        if(matches.size()==1) {
            putResolvedEntity(envelope,matches.get(0),primary);
            markMentionResolved(envelope,typeValue,keyword,matches.get(0));
            if("BARE".equals(mode)) throw new AssistantOrchestrationClarification(envelope,purposeResponse(matches.get(0)));
            return;
        }
        markMentionStatus(envelope,typeValue,keyword,"CHOICE_REQUIRED");
        for(Map<String,Object> candidate:matches) candidate.put("field","entity:"+candidate.get("entityType"));
        envelope.getArguments().put("pendingEntities",matches);envelope.getArguments().put("pendingMode",mode);
        envelope.getArguments().put("pendingEntityKeyword",keyword);if(slot!=null) envelope.getArguments().put("pendingEntitySlot",slot);
        Map<String,Object> response=new LinkedHashMap<>();response.put("status","CHOICE_REQUIRED");response.put("clarification","匹配到多个有权限的业务对象，请选择");
        response.put("field","entity");response.put("candidates",matches);
        throw new AssistantOrchestrationClarification(envelope,response);
    }

    private void resolveCurrentEntityMentions(AssistantExecutionEnvelope envelope,String question,String tool) {
        for(AssistantSemanticExtractor.Reference reference:AssistantSemanticExtractor.extract(question,tool)) {
            recordMention(envelope,reference.type.name(),reference.keyword,reference.role,"CURRENT",true,reference.matchType,"DISCOVERED");
            Map<String,Object> existing=resolvedEntityByType(envelope,reference.type.name());
            boolean currentAlreadyResolved=existing!=null && Arrays.asList("CURRENT","CLARIFICATION").contains(existing.get("source"))
                    && reference.keyword.equals(existing.get("keyword"));
            if(currentAlreadyResolved) continue;
            Map<String,Object> primary=resolvedEntityByType(envelope,reference.type.name());
            boolean replacePrimary=envelope.getArguments().get("resolvedEntity")==null
                    || primary!=null && reference.type.name().equals(((Map<?,?>)envelope.getArguments().get("resolvedEntity")).get("entityType"));
            resolveEntityOrClarify(envelope,reference.type.name(),reference.keyword,"QUERY",reference.type.name(),replacePrimary);
            Map<String,Object> resolved=resolvedEntityByType(envelope,reference.type.name());
            if(resolved!=null) {
                resolved.put("source","CURRENT");resolved.put("keyword",reference.keyword);
                putResolvedEntity(envelope,resolved,replacePrimary);
            }
        }
    }

    private void resolveStockCompoundEntities(AssistantExecutionEnvelope envelope,String question,String tool) {
        if(!supportsStockCompoundEntities(tool,question)) return;
        if(resolvedEntityByType(envelope,"PRODUCT")==null) {
            String code=extractLeadingProductCode(question);
            if(code!=null) resolveEntityOrClarify(envelope,"PRODUCT",code,"QUERY","PRODUCT",envelope.getArguments().get("resolvedEntity")==null);
        }
        String warehouse=extractWarehouseName(question);
        if(warehouse!=null && resolvedEntityByType(envelope,"WAREHOUSE")==null)
            resolveEntityOrClarify(envelope,"WAREHOUSE",warehouse,"QUERY","WAREHOUSE",envelope.getArguments().get("resolvedEntity")==null);
    }

    private static void ensureExplicitReferencesResolved(AssistantExecutionEnvelope envelope,String question) {
        String text=question==null?"":question.replaceAll("\\s+","");
        String[][] definitions={
                {"PRODUCT","产品","(?:这个|该|此|某)(?:产品|配件|商品)|(?:产品|配件)(?:的)?(?:出入库|库存流水|库存(?:是多少|多少|情况))"},
                {"CUSTOMER","客户","(?:这个|该|此|某)客户|客户(?:的)?(?:收款|应收|欠款|欠钱)"},
                {"SUPPLIER","供应商","(?:这个|该|此|某)供应商|供应商(?:的)?(?:付款|应付|采购|供货)"},
                {"WAREHOUSE","仓库","(?:这个|该|此|某)仓库|仓库(?:的)?(?:库存|出入库|库存流水)"},
                {"DEPARTMENT","部门","(?:这个|该|此|某)部门|部门(?:的)?(?:销售|采购|库存|收款|付款|应收|应付)"},
                {"SALESPERSON","业务员","(?:这个|该|此|某)(?:业务员|销售员)|(?:业务员|销售员)(?:的)?(?:销量|销售|客户欠款|欠款排行)"}
        };
        for(String[] definition:definitions) {
            if(resolvedEntityByType(envelope,definition[0])!=null || !text.matches(".*(?:"+definition[2]+").*")) continue;
            if(text.matches(".*(?:哪个|哪些|什么|各个|各|每个|所有|全部|按)(?:的)?"+definition[1]+".*")) continue;
            recordMention(envelope,definition[0],definition[1],definition[0],"CURRENT",true,"REFERENCE","NEEDS_CLARIFICATION");
            throw new AssistantFailure("CLARIFY","无法确认问题中的"+definition[1]+"，请补充明确名称或编码");
        }
    }

    private static Map<String,Object> purposeResponse(Map<String,Object> entity) {
        String type=String.valueOf(entity.get("entityType"));List<String> choices=new ArrayList<>();
        if("PRODUCT".equals(type)) choices.addAll(Arrays.asList("查询价格/当前库存","按仓库查询当前库存","查询库存流水"));
        else if("CUSTOMER".equals(type)) choices.addAll(Arrays.asList("查询本月销售","查询本月收款","查询当前应收"));
        else if("SUPPLIER".equals(type)) choices.addAll(Arrays.asList("查询本月采购","查询本月付款","查询当前应付"));
        else if("WAREHOUSE".equals(type)) choices.addAll(Arrays.asList("查询当前库存","查询库存流水"));
        else choices.addAll(Arrays.asList("查询本月销售","查询本月采购","查询当前库存"));
        Map<String,Object> selected=new LinkedHashMap<>();
        selected.put("id",entity.get("id"));selected.put("entityType",type);selected.put("name",entity.get("name"));selected.put("field","resolved:"+type);
        Map<String,Object> response=new LinkedHashMap<>();response.put("clarification","已识别“"+entity.get("name")+"”，请选择要查询的内容");
        response.put("choices",choices);response.put("selectedEntity",selected);return response;
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> resolvedEntity(AssistantExecutionEnvelope previous,String question) {
        if(previous==null || previous.getArguments()==null) return null;
        Object value=previous.getArguments().get("resolvedEntity");
        if(!(value instanceof Map)) return null;
        String q=question==null?"":question;
        Map<String,Object> previousEntity=(Map<String,Object>)value;
        if(q.contains("补充：")) return new LinkedHashMap<>(previousEntity);
        String previousType=stringValue(previousEntity.get("entityType"));
        boolean currentReplacesSameType=AssistantSemanticExtractor.extract(question,null).stream()
                .anyMatch(reference->reference.type.name().equals(previousType));
        boolean repeatsPreviousEntity=mentionsPreviousEntity(q,previousEntity);
        String currentKeyword=currentIndependentEntityKeyword(question);
        if(currentKeyword!=null && !repeatsPreviousEntity) return null;
        if(bareEntityKeyword(question)!=null) return null;
        return !currentReplacesSameType && (AssistantModelClient.shouldInheritEntityContext(question) || repeatsPreviousEntity)
                ?new LinkedHashMap<>(previousEntity):null;
    }

    private static boolean mentionsPreviousEntity(String question,Map<String,Object> previousEntity) {
        String normalizedQuestion=normalizeEntityText(question);
        for(String field:Arrays.asList("keyword","name")) {
            Object raw=previousEntity.get(field);if(raw==null) continue;
            String value=normalizeEntityText(String.valueOf(raw));
            if(value.length()>=2 && normalizedQuestion.contains(value)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> resolvedEntities(AssistantExecutionEnvelope previous,String question) {
        if(previous==null || previous.getArguments()==null || question==null || !question.contains("补充：")) return null;
        Object value=previous.getArguments().get("resolvedEntities");
        return value instanceof Map?new LinkedHashMap<>((Map<String,Object>)value):null;
    }

    @SuppressWarnings("unchecked") private static List<Map<String,Object>> listOfMaps(Object value) {
        if(!(value instanceof Collection)) return Collections.emptyList();List<Map<String,Object>> result=new ArrayList<>();
        for(Object item:(Collection<?>)value) if(item instanceof Map) result.add((Map<String,Object>)item);return result;
    }

    private Map<String,Object> revalidateInheritedEntity(Map<String,Object> inherited) {
        String type=stringValue(inherited.get("entityType"));Object rawId=inherited.get("id");
        if(type==null || !(rawId instanceof Number)) throw new AssistantFailure("CLARIFY","历史业务对象已失效，请重新指定查询对象");
        try {
            Map<String,Object> current=entities.validate(AssistantEntityResolver.EntityType.valueOf(type),((Number)rawId).longValue());
            if(current==null) throw new AssistantFailure("NO_PERMISSION","历史业务对象已不可访问，请重新指定查询对象");
            Object source=inherited.get("source");Object keyword=inherited.get("keyword");
            current.put("source","CLARIFICATION".equals(source)?"CLARIFICATION":"CONTEXT");
            if(keyword!=null) current.put("keyword",keyword);
            return current;
        } catch(IllegalArgumentException failure) {
            throw new AssistantFailure("CLARIFY","历史业务对象类型无效，请重新指定查询对象");
        } catch(AssistantFailure failure) {
            if(Arrays.asList("FORBIDDEN","FIELD_FORBIDDEN").contains(failure.getCode()))
                throw new AssistantFailure("NO_PERMISSION","历史业务对象已不可访问，请重新指定查询对象");
            throw failure;
        }
    }

    private static String inheritedSource(Map<String,Object> entity) {
        return "CLARIFICATION".equals(entity.get("source"))?"CLARIFICATION":"CONTEXT";
    }

    private static boolean sameEntity(Map<String,Object> left,Map<String,Object> right) {
        return left!=null && right!=null && Objects.equals(left.get("entityType"),right.get("entityType"))
                && Objects.equals(String.valueOf(left.get("id")),String.valueOf(right.get("id")));
    }

    private void resolveModelPlanMentions(AssistantExecutionEnvelope envelope,String question,AssistantPlan plan) {
        if(plan==null || entities==null || !entities.enabled()) return;
        LinkedHashMap<String,AssistantPlan.ModelEntityMention> mentions=new LinkedHashMap<>();
        if(plan.getEntityMentions()!=null) for(AssistantPlan.ModelEntityMention mention:plan.getEntityMentions())
            if(mention!=null && mention.getEntityType()!=null) mentions.putIfAbsent(mention.getEntityType(),mention);
        addPlanMention(mentions,"PRODUCT",plan.getProduct(),"PRODUCT_FILTER");
        addPlanMention(mentions,"WAREHOUSE",plan.getWarehouse(),"WAREHOUSE_FILTER");
        addPlanMention(mentions,"DEPARTMENT",plan.getDepartment(),"DEPARTMENT_FILTER");
        if(plan.getParty()!=null) {
            String partyType=Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.RECEIVABLE).contains(plan.getMetric())
                    ?"CUSTOMER":Arrays.asList(AssistantPlan.Metric.PURCHASE,AssistantPlan.Metric.PAYMENT,AssistantPlan.Metric.PAYABLE).contains(plan.getMetric())?"SUPPLIER":null;
            if(partyType!=null) addPlanMention(mentions,partyType,plan.getParty(),partyType+"_FILTER");
        }
        plan.setEntityMentions(new ArrayList<>());
        resolveModelMentions(envelope,question,mentions.values());
    }

    private void resolveSupplementaryModelMentions(AssistantExecutionEnvelope envelope,String question,String tool) {
        if(!needsSupplementaryModelMentions(envelope,question,tool)) return;
        List<AssistantPlan.ModelEntityMention> mentions=model.extractEntityMentions(question,tool,resolvedEntityTypes(envelope));
        if(mentions==null || mentions.isEmpty())
            throw new AssistantFailure("CLARIFY","问题中可能包含具体业务对象，但无法确认其角色，请补充客户、供应商或部门名称");
        resolveModelMentions(envelope,question,mentions);
    }

    private static boolean needsSupplementaryModelMentions(AssistantExecutionEnvelope envelope,String question,String tool) {
        String expectedType;
        if(Arrays.asList("query_purchase_documents","query_purchase_fulfillment").contains(tool)) expectedType="SUPPLIER";
        else if(Arrays.asList("query_sale_documents","query_sale_fulfillment").contains(tool)) expectedType="CUSTOMER";
        else return false;
        if(resolvedEntityByType(envelope,expectedType)!=null || resolvedEntityByType(envelope,"DEPARTMENT")!=null) return false;
        String text=question==null?"":question.replaceAll("\\s+","").replaceFirst("^(?:请问|帮我查一下|帮我查询|查询一下|查询|查看一下|查看|查一下)","");
        Matcher matcher=Pattern.compile("^(.{2,80}?)(?:的)?(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月)?(?:的)?(?:采购|销售)(?:订单|单)").matcher(text);
        if(!matcher.find()) return false;
        String prefix=matcher.group(1);
        return !prefix.matches(".*(?:多少|几个|哪些|所有|全部|最近|当前|按|每个|各个|各).*");
    }

    private void resolveModelMentions(AssistantExecutionEnvelope envelope,String question,Collection<AssistantPlan.ModelEntityMention> mentions) {
        String normalizedQuestion=normalizeEntityText(question);
        for(AssistantPlan.ModelEntityMention mention:mentions) {
            AssistantEntityResolver.EntityType type;
            try {type=AssistantEntityResolver.EntityType.valueOf(mention.getEntityType());}
            catch(Exception failure){throw new AssistantFailure("MODEL_INVALID","模型返回了不支持的对象类型");}
            String keyword=mention.getKeyword()==null?null:mention.getKeyword().trim();
            Map<String,Object> existing=resolvedEntityByType(envelope,type.name());
            if(existing!=null && !"CONTEXT".equals(existing.get("source"))) {
                if(keyword!=null && !Objects.equals(normalizeEntityText(keyword),normalizeEntityText(String.valueOf(existing.get("keyword")))))
                    recordEntityConflict(envelope,type.name(),keyword,existing);
                continue;
            }
            if(!AssistantSemanticExtractor.isConcreteKeyword(type,keyword) || !normalizedQuestion.contains(normalizeEntityText(keyword))) {
                recordMention(envelope,type.name(),keyword==null?type.name():keyword,safeModelRole(mention,type),"CURRENT",true,"MODEL_NATURAL","NEEDS_CLARIFICATION");
                throw new AssistantFailure("CLARIFY","无法从原问题核对模型识别的"+entityTypeText(Collections.singleton(type.name()))+"，请补充明确名称或编码");
            }
            recordMention(envelope,type.name(),keyword,safeModelRole(mention,type),"CURRENT",true,"MODEL_NATURAL","DISCOVERED");
            boolean primary=envelope.getArguments().get("resolvedEntity")==null
                    || existing!=null && type.name().equals(((Map<?,?>)envelope.getArguments().get("resolvedEntity")).get("entityType"));
            resolveEntityOrClarify(envelope,type.name(),keyword,"QUERY",type.name(),primary);
            Map<String,Object> resolved=resolvedEntityByType(envelope,type.name());
            if(resolved!=null) {resolved.put("source","CURRENT");resolved.put("keyword",keyword);putResolvedEntity(envelope,resolved,primary);}
        }
    }

    private static void addPlanMention(Map<String,AssistantPlan.ModelEntityMention> mentions,String type,String keyword,String role) {
        if(keyword==null || keyword.trim().isEmpty() || mentions.containsKey(type)) return;
        AssistantPlan.ModelEntityMention mention=new AssistantPlan.ModelEntityMention();mention.setEntityType(type);mention.setKeyword(keyword);mention.setRole(role);
        mentions.put(type,mention);
    }

    private static String safeModelRole(AssistantPlan.ModelEntityMention mention,AssistantEntityResolver.EntityType type) {
        String role=mention.getRole();return role!=null && role.matches("[A-Za-z_]{1,40}")?role:type.name();
    }

    @SuppressWarnings("unchecked")
    private static void recordEntityConflict(AssistantExecutionEnvelope envelope,String type,String modelKeyword,Map<String,Object> serverEntity) {
        Object raw=envelope.getSemanticResult().get("conflicts");List<Map<String,Object>> conflicts;
        if(raw instanceof List) conflicts=(List<Map<String,Object>>)raw;
        else {conflicts=new ArrayList<>();envelope.getSemanticResult().put("conflicts",conflicts);}
        Map<String,Object> conflict=new LinkedHashMap<>();conflict.put("entityType",type);conflict.put("modelKeyword",AssistantModelClient.redact(modelKeyword));
        conflict.put("serverKeyword",AssistantModelClient.redact(String.valueOf(serverEntity.get("keyword"))));conflict.put("decision","SERVER_EXPLICIT_WINS");conflicts.add(conflict);
    }

    private static String normalizeEntityText(String value) {
        return value==null?"":value.replaceAll("[\\s，,。；;：:\"'“”‘’《》【】]","").toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked") private static void applyResolvedEntity(AssistantExecutionEnvelope envelope,AssistantPlan plan) {
        Object all=envelope.getArguments().get("resolvedEntities");
        if(all instanceof Map) for(Object item:((Map<?,?>)all).values()) if(item instanceof Map) applyResolvedEntityToPlan(plan,(Map<String,Object>)item);
        Object value=envelope.getArguments().get("resolvedEntity");if(!(value instanceof Map)) return;
        applyResolvedEntityToPlan(plan,(Map<String,Object>)value);
    }

    private static void applyResolvedEntityToPlan(AssistantPlan plan,Map<String,Object> entity) {
        Object rawId=entity.get("id");if(!(rawId instanceof Number)) return;
        Long id=((Number)rawId).longValue();String type=String.valueOf(entity.get("entityType")),name=String.valueOf(entity.get("name"));
        switch(type) {
            case "PRODUCT": plan.setProduct(name);plan.setProductId(id);break;
            case "WAREHOUSE": plan.setWarehouse(name);plan.setWarehouseId(id);break;
            case "DEPARTMENT": plan.setDepartment(name);plan.setDepartmentId(id);break;
            case "CUSTOMER":
                if(!Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.RECEIVABLE).contains(plan.getMetric())) throw new AssistantFailure("CLARIFY","该指标不能按客户查询");
                plan.setParty(name);plan.setPartyId(id);break;
            case "SUPPLIER":
                if(!Arrays.asList(AssistantPlan.Metric.PURCHASE,AssistantPlan.Metric.PAYMENT,AssistantPlan.Metric.PAYABLE).contains(plan.getMetric())) throw new AssistantFailure("CLARIFY","该指标不能按供应商查询");
                plan.setParty(name);plan.setPartyId(id);break;
            default: break;
        }
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> primaryResolvedEntity(AssistantExecutionEnvelope envelope) {
        Object value=envelope.getArguments().get("resolvedEntity");
        return value instanceof Map?(Map<String,Object>)value:null;
    }

    private static boolean isShortEntityMetricFollowup(String question) {
        String text=question==null?"":question.replaceAll("\\s+","");
        return AssistantModelClient.isShortMetricFollowup(text);
    }

    private static String currentIndependentEntityKeyword(String question) {
        if(question==null) return null;
        String text=question.trim().replaceAll("[？?。！!]+$","").replaceAll("\\s+"," ");
        Matcher matcher=Pattern.compile("^(.{1,80}?)(?:的)?(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月)?(?:的)?(?:销量|销售量|销售情况|卖了多少)(?:呢|吗)?$").matcher(text);
        if(!matcher.find()) return null;
        String keyword=matcher.group(1).trim()
                .replaceFirst("^(?:请问|帮我查一下|帮我查询|查询一下|查询|查看一下|查看|查一下)","")
                .replaceFirst("^(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月)","")
                .replaceFirst("(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月)$","")
                .replaceFirst("的$","").trim();
        if(keyword.matches("(?:这个|该|此|上面|刚才|刚刚|前面|它)(?:产品|配件|客户|供应商|仓库|部门)?")
                || keyword.matches(".*(?:客户|供应商|产品|配件|仓库|部门)(?:名称)?(?:为|是|[:：])?.*")) return null;
        return standaloneEntityToken(keyword)?keyword:null;
    }

    private static String bareEntityKeyword(String question) {
        if(question==null) return null;
        String value=question.trim().replaceAll("[？?。！!]+$","");
        if(value.length()>2) value=value.replaceFirst("(?:呢|啊|呀)$","").trim();
        if(!standaloneEntityToken(value)) return null;
        String compact=value.replaceAll("\\s+","");
        // A bare object is a name/code only, not a complete natural-language query.
        if(compact.matches(".*(?:销售|销量|销售量|销售情况|采购|库存|出入库|流水|调拨|盘点|"
                + "收款|回款|付款|应收|应付|欠款|欠钱|价格|报价|查价|多少钱|多少元|最后采购价|上次采购价|最近采购价|最新采购价|"
                + "股份价|零售价|销售价|售价|批发价|参考价|最低价|采购价|有货|缺货|查询|查看|情况|"
                + "这个|该|此|某|产品|配件|客户|供应商|仓库|部门|分公司|事业部|办事处).*") ) return null;
        return value;
    }

    private static boolean standaloneEntityToken(String value) {
        if(value==null || value.isEmpty() || value.length()>80
                || !value.matches("[\\p{L}\\p{N}_./\\-·*+ ]+")) return false;
        String compact=value.replaceAll("\\s+","");
        if(compact.matches("(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月|当前|现在|"
                + "销量|销售量|销售情况|销售|采购|库存|当前库存|库存情况|收款|回款|付款|应收|应付|欠款|欠钱|"
                + "价格|报价|多少钱|多少元|有货|缺货|继续|再看|查询|查看|产品|配件|客户|供应商|仓库|部门)")) return false;
        return !compact.matches(".*(?:哪个|哪些|什么|多少|为何|为什么|怎么|排名|排行|最多|最少|最高|最低|前十|后十).*");
    }

    private static boolean hasPendingEntityChoice(AssistantExecutionEnvelope previous) {
        return previous!=null && previous.getArguments()!=null
                && !listOfMaps(previous.getArguments().get("pendingEntities")).isEmpty();
    }

    private static Map<String,Object> pendingEntityChoiceResponse(AssistantExecutionEnvelope previous) {
        List<Map<String,Object>> candidates=listOfMaps(previous.getArguments().get("pendingEntities"));
        Map<String,Object> response=new LinkedHashMap<>();response.put("status","CHOICE_REQUIRED");
        response.put("clarification","请先选择要查询的业务对象，再继续询问销量");
        response.put("field","entity");response.put("candidates",candidates);return response;
    }

    private static boolean supportsStockCompoundEntities(String tool,String question) {
        if("query_stock_movements".equals(tool) || "query_product_stock".equals(tool)) return true;
        String text=question==null?"":question.replaceAll("\\s+","");
        return "query_verified_metric".equals(tool) && text.contains("库存");
    }

    private static String extractLeadingProductCode(String question) {
        if(question==null) return null;
        String text=question.trim().replaceAll("\\s+","");
        Matcher matcher=Pattern.compile("^([A-Za-z0-9][A-Za-z0-9_./\\-]{1,79})(?:在|的|$)").matcher(text);
        if(!matcher.find()) return null;
        String code=matcher.group(1);
        return code.matches(".*\\d.*")?code:null;
    }

    private static String extractWarehouseName(String question) {
        if(question==null) return null;
        String text=question.replaceAll("\\s+","");
        String[] patterns={
                "(?:在|到|从)([^，。,.?？；;]{1,30}?仓)(?:里|中|内)?(?:的)?(?:库存流水|出入库|库存变动|调拨记录|盘点记录|库存)",
                "(?:这个|该|此)?(?:产品|配件|它)?([^，。,.?？；;]{1,30}?仓)(?:的)?(?:库存流水|出入库|库存变动|调拨记录|盘点记录)"
        };
        for(String pattern:patterns) {
            Matcher matcher=Pattern.compile(pattern).matcher(text);
            if(!matcher.find()) continue;
            String value=matcher.group(1).replaceFirst("^(?:在|到|从|这个|该|此|产品|配件|它)+","").trim();
            return value.isEmpty()?null:value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void putResolvedEntity(AssistantExecutionEnvelope envelope,Map<String,Object> entity,boolean primary) {
        if(entity==null) return;
        Map<String,Object> copy=new LinkedHashMap<>(entity);Object type=copy.get("entityType");
        if(type!=null) {
            Map<String,Object> all=new LinkedHashMap<>();
            Object raw=envelope.getArguments().get("resolvedEntities");
            if(raw instanceof Map) all.putAll((Map<String,Object>)raw);
            all.put(String.valueOf(type),copy);
            envelope.getArguments().put("resolvedEntities",all);
        }
        if(primary) envelope.getArguments().put("resolvedEntity",copy);
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> resolvedEntityByType(AssistantExecutionEnvelope envelope,String type) {
        if(envelope==null || envelope.getArguments()==null) return null;
        Object raw=envelope.getArguments().get("resolvedEntities");
        if(raw instanceof Map) {
            Object value=((Map<?,?>)raw).get(type);
            if(value instanceof Map) return new LinkedHashMap<>((Map<String,Object>)value);
        }
        Object primary=envelope.getArguments().get("resolvedEntity");
        if(primary instanceof Map && type.equals(((Map<?,?>)primary).get("entityType"))) return new LinkedHashMap<>((Map<String,Object>)primary);
        return null;
    }

    private static void initializeSemanticResult(AssistantExecutionEnvelope envelope,String question,String tool) {
        Map<String,Object> semantic=envelope.getSemanticResult();
        semantic.put("intent",tool);semantic.put("metricOrTool",tool);
        semantic.put("entityMentions",new ArrayList<Map<String,Object>>());
        semantic.put("filters",new ArrayList<Map<String,Object>>());
        semantic.put("contextReferences",AssistantSemanticExtractor.contextReferences(question));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> semanticMentions(AssistantExecutionEnvelope envelope) {
        Object raw=envelope.getSemanticResult().get("entityMentions");
        if(raw instanceof List) return (List<Map<String,Object>>)raw;
        List<Map<String,Object>> mentions=new ArrayList<>();envelope.getSemanticResult().put("entityMentions",mentions);return mentions;
    }

    private static void recordMention(AssistantExecutionEnvelope envelope,String type,String keyword,String role,String source,
                                      boolean explicit,String matchType,String status) {
        if(type==null || keyword==null) return;
        for(Map<String,Object> mention:semanticMentions(envelope))
            if(type.equals(mention.get("entityType")) && keyword.equals(mention.get("keyword")) && source.equals(mention.get("source"))) return;
        Map<String,Object> mention=new LinkedHashMap<>();mention.put("entityType",type);mention.put("keyword",AssistantModelClient.redact(keyword));
        mention.put("role",role);mention.put("source",source);mention.put("explicit",explicit);mention.put("extractionMethod",matchType);
        mention.put("matchType",matchType);mention.put("status",status);
        semanticMentions(envelope).add(mention);
    }

    private static void recordResolvedMention(AssistantExecutionEnvelope envelope,Map<String,Object> entity,String source,boolean explicit,String matchType) {
        if(entity==null || entity.get("entityType")==null) return;
        String type=String.valueOf(entity.get("entityType"));String keyword=String.valueOf(entity.get("name"));
        recordMention(envelope,type,keyword,type,source,explicit,matchType,"RESOLVED");
        markMentionResolved(envelope,type,keyword,entity);
    }

    private static void markMentionStatus(AssistantExecutionEnvelope envelope,String type,String keyword,String status) {
        if(type==null || keyword==null) return;
        List<Map<String,Object>> mentions=semanticMentions(envelope);
        for(int i=mentions.size()-1;i>=0;i--) {
            Map<String,Object> mention=mentions.get(i);
            if(type.equals(mention.get("entityType")) && AssistantModelClient.redact(keyword).equals(mention.get("keyword"))) {
                mention.put("status",status);return;
            }
        }
    }

    private static void markMentionResolved(AssistantExecutionEnvelope envelope,String type,String keyword,Map<String,Object> entity) {
        if(type==null && entity!=null) type=String.valueOf(entity.get("entityType"));
        markMentionStatus(envelope,type,keyword,"RESOLVED");
        for(int i=semanticMentions(envelope).size()-1;i>=0;i--) {
            Map<String,Object> mention=semanticMentions(envelope).get(i);
            if(!Objects.equals(type,mention.get("entityType")) || !Objects.equals(AssistantModelClient.redact(keyword),mention.get("keyword"))) continue;
            mention.put("resolvedId",entity.get("id"));mention.put("resolvedName",entity.get("name"));
            if(entity.get("matchField")!=null) mention.put("matchField",entity.get("matchField"));
            if(entity.get("matchType")!=null) mention.put("matchType",entity.get("matchType"));return;
        }
    }

    private static void capturePlanSemantics(AssistantExecutionEnvelope envelope,AssistantPlan plan) {
        if(plan==null) return;
        Map<String,Object> semantic=envelope.getSemanticResult();semantic.put("metricOrTool",plan.getMetric()==null?envelope.getToolName():plan.getMetric().name());
        semantic.put("resolvedEntityTypes",new ArrayList<>(resolvedEntityTypes(envelope)));
        semantic.put("grouping",plan.getGroup()==null?null:plan.getGroup().name());
        Map<String,Object> ranking=new LinkedHashMap<>();ranking.put("order",plan.getRankOrder()==null?null:plan.getRankOrder().name());ranking.put("limit",plan.getLimit());semantic.put("ranking",ranking);
        List<Map<String,Object>> filters=new ArrayList<>();
        if(plan.getPeriod()!=null) filters.add(filter("period",plan.getPeriod()));
        if(plan.getStockMode()!=null) filters.add(filter("stockMode",plan.getStockMode()));
        semantic.put("filters",filters);
        Map<String,Object> timeRange=new LinkedHashMap<>();timeRange.put("period",plan.getPeriod());
        if(plan.getStartDate()!=null) timeRange.put("start",plan.getStartDate());
        if(plan.getEndDate()!=null) timeRange.put("end",plan.getEndDate());
        semantic.put("timeRange",timeRange);
    }

    private static Map<String,Object> filter(String field,Object value) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("field",field);result.put("value",value);return result;
    }

    private static void applySemanticResultStatus(AssistantExecutionEnvelope envelope,Map<String,Object> result) {
        if(!"EMPTY".equals(result.get("status"))) {
            envelope.getSemanticResult().put("outcomeStatus","RESOLVED");return;
        }
        envelope.getSemanticResult().put("status","NO_DATA");envelope.getSemanticResult().put("outcomeStatus","NO_DATA");
        result.put("semanticStatus","NO_DATA");
        result.put("emptyText",semanticMentions(envelope).isEmpty()
                ?"在指定条件和当前权限范围内没有查询到相关数据"
                :"已确认查询对象，但在指定条件和当前权限范围内没有业务记录");
    }

    private static String outcomeStatus(String code) {
        if(code==null) return "FAILED";
        if(Arrays.asList("CLARIFY","CLARIFY_ENTITY","CHOICE_REQUIRED","INVALID_CHOICE").contains(code)) return "CHOICE_REQUIRED";
        if("ENTITY_NOT_FOUND".equals(code)) return "ENTITY_NOT_FOUND";
        if(Arrays.asList("FORBIDDEN","FIELD_FORBIDDEN","NO_PERMISSION").contains(code)) return "NO_PERMISSION";
        if("CAPABILITY_UNVERIFIED".equals(code) || "DATASET_UNPUBLISHED".equals(code)) return "CAPABILITY_UNVERIFIED";
        return "FAILED";
    }

    private static String entityNotFoundText(String type) {
        if("PRODUCT".equalsIgnoreCase(type)) return "没有找到匹配的产品，请检查产品名称、编码或规格";
        if("CUSTOMER".equalsIgnoreCase(type)) return "没有找到匹配的客户，请检查客户名称或编码";
        if("SUPPLIER".equalsIgnoreCase(type)) return "没有找到匹配的供应商，请检查供应商名称或编码";
        if("WAREHOUSE".equalsIgnoreCase(type)) return "没有找到匹配的仓库，请检查仓库名称或编码";
        if("DEPARTMENT".equalsIgnoreCase(type)) return "没有找到匹配的部门，请检查部门名称";
        if("SALESPERSON".equalsIgnoreCase(type)) return "没有找到匹配的业务员，请检查业务员名称";
        return "指定范围内未找到有权限的业务对象";
    }

    private void validateToolEntityContract(AssistantExecutionEnvelope envelope,String tool,AssistantPlan plan) {
        Set<String> types=resolvedEntityTypes(envelope);
        if(types.isEmpty()) return;
        if(types.contains("SALESPERSON"))
            throw new AssistantFailure("CAPABILITY_UNVERIFIED","已识别业务员，但按业务员筛选销量或欠款的统计能力尚未完成业务对账，未执行删除业务员条件的查询");
        AssistantToolRegistry.ExecutionContract contract=registry.executionContract(tool,plan==null?null:plan.getMetric());
        if(contract.supportsEntities(types)) return;
        Set<String> supported=contract.getSupportedEntityTypes();
        Set<String> unsupported=new LinkedHashSet<>(types);unsupported.removeAll(supported);
        throw new AssistantFailure("DATASET_UNPUBLISHED","已经识别到"+entityTypeText(types)+"，但当前查询尚未开放“"+entityTypeText(unsupported)+"”组合条件，未执行删减条件后的查询");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> resolvedEntityTypes(AssistantExecutionEnvelope envelope) {
        Set<String> result=new LinkedHashSet<>();Object raw=envelope.getArguments().get("resolvedEntities");
        if(raw instanceof Map) for(Object item:((Map<?,?>)raw).values()) if(item instanceof Map && ((Map<?,?>)item).get("entityType")!=null)
            result.add(String.valueOf(((Map<?,?>)item).get("entityType")));
        Object primary=envelope.getArguments().get("resolvedEntity");if(primary instanceof Map && ((Map<?,?>)primary).get("entityType")!=null)
            result.add(String.valueOf(((Map<?,?>)primary).get("entityType")));
        return result;
    }

    private static String entityTypeText(Collection<String> types) {
        List<String> names=new ArrayList<>();
        for(String type:types) names.add("PRODUCT".equals(type)?"产品":"CUSTOMER".equals(type)?"客户":"SUPPLIER".equals(type)?"供应商":"WAREHOUSE".equals(type)?"仓库":"DEPARTMENT".equals(type)?"部门":"SALESPERSON".equals(type)?"业务员":type);
        return String.join("、",names);
    }

    /** High-frequency business tools use server-owned templates. Model SQL is reserved for the fallback route. */
    private Map<String,Object> dedicatedSemanticTool(String question,String dataset,String tool,AssistantExecutionEnvelope envelope) {
        validateToolEntityContract(envelope,tool,null);
        if(requiresSemanticFallback(question,dataset,envelope)) {
            if(!properties.getOrchestration().isTextToSqlEnabled())
                throw new AssistantFailure("TEXT_TO_SQL_DISABLED","该问题包含专用工具尚未覆盖的期间或对象筛选，智能查询尚未向当前环境开放");
            envelope.setRouteType(AssistantExecutionEnvelope.RouteType.TEXT_TO_SQL);envelope.setToolName("execute_semantic_query");
            return semanticFallback(question,envelope);
        }
        semantic.validateDatasetAccess(dataset);record(envelope,null,"query_semantic_schema",Collections.singletonMap("dataset",dataset));
        DedicatedQuery query=dedicatedQuery(question,dataset,tool,envelope);
        record(envelope,null,"execute_semantic_query",Collections.singletonMap("template",query.template));
        Map<String,Object> result=semantic.execute(query.sql,query.parameters);
        envelope.getArguments().put("sql",query.sql);
        if(query.detailSql!=null) envelope.getArguments().put("detailSql",query.detailSql);
        envelope.getArguments().put("parameters",safeArguments(query.parameters));
        envelope.getArguments().put("template",query.template);
        result.put("queryType","BUSINESS_TOOL");result.put("toolName",tool);result.put("title",toolTitle(tool));
        if("query_inventory_details".equals(tool))
            result.put("basis",Collections.singletonList("当前账面库存数量；不包含库存金额、成本或历史时点库存"));
        else if(Arrays.asList("query_sale_documents","query_sale_fulfillment").contains(tool))
            result.put("basis",Collections.singletonList("已审核销售订单；订单金额不等于已审核销售出库扣除退货后的销售金额"));
        return result;
    }

    private DedicatedQuery dedicatedQuery(String question,String dataset,String tool,AssistantExecutionEnvelope envelope) {
        String text=question==null?"":question.replaceAll("\\s+","");
        Map<String,Object> parameters=new LinkedHashMap<>();
        List<String> filters=new ArrayList<>();
        String timeColumn="stock_movements".equals(dataset)?"biz_date":"inventory_current".equals(dataset)?null:"order_time";
        if(timeColumn!=null) addTimeFilter(text,timeColumn,filters,parameters);
        addResolvedEntityFilter(dataset,envelope,filters,parameters);
        addDocumentNumberFilter(text,dataset,filters,parameters);
        boolean count=asksForDocumentCount(text) && !"query_stock_movements".equals(tool);
        String alias="q";
        String select;
        int limit;
        if("query_purchase_documents".equals(tool)) {
            String group=documentGroup(text,"supplier_name");
            String scalar=documentScalar(text);
            select=group==null?(count?"COUNT(q.id) document_count":scalar==null?"q.no,q.order_time,q.supplier_name,q.dept_name,q.total_count,q.total_price,q.status":scalar)
                    :group+","+documentAggregate(text);limit=group!=null?10:count?0:20;
        } else if("query_sale_documents".equals(tool)) {
            if("sale_order_items".equals(dataset)) {
                String group=saleItemGroup(text);
                String scalar=saleItemScalar(text,count);
                select=group==null?(scalar==null?"q.order_no,q.order_time,q.customer_name,q.salesperson_name,q.dept_name,q.product_name,q.warehouse_name,q.unit,q.order_quantity,q.unit_price,q.line_amount,q.out_quantity,q.return_quantity,q.remaining_quantity":scalar)
                        :group+","+saleItemAggregate(text);limit=group!=null?10:isScalarAggregate(select)?0:50;
            } else {
                String group=documentGroup(text,"customer_name");
                String scalar=documentScalar(text);
                select=group==null?(count?"COUNT(q.id) document_count":scalar==null?"q.no,q.order_time,q.customer_name,q.salesperson_name,q.dept_name,q.total_count,q.total_price,q.status":scalar)
                        :group+","+documentAggregate(text);limit=group!=null?10:count?0:20;
            }
        } else if("query_purchase_fulfillment".equals(tool)) {
            String group=documentGroup(text,"supplier_name");
            select=group==null?(count?"COUNT(q.id) document_count":"q.no,q.order_time,q.supplier_name,q.dept_name,q.total_count,q.in_count,q.return_count,q.remaining_count")
                    :group+",SUM(q.total_count) total_quantity,SUM(q.in_count) fulfilled_quantity,SUM(q.return_count) return_quantity,SUM(q.remaining_count) remaining_quantity";
            addFulfillmentFilter(text,filters,"in_count","return_count","remaining_count");limit=group!=null?10:count?0:50;
        } else if("query_sale_fulfillment".equals(tool)) {
            if("sale_order_items".equals(dataset)) {
                String group=saleItemGroup(text);
                select=group==null?(count?"COUNT(DISTINCT q.order_id) document_count":"q.order_no,q.order_time,q.customer_name,q.salesperson_name,q.dept_name,q.product_name,q.warehouse_name,q.unit,q.order_quantity,q.out_quantity,q.return_quantity,q.remaining_quantity")
                        :group+",SUM(q.order_quantity) total_quantity,SUM(q.out_quantity) fulfilled_quantity,SUM(q.return_quantity) return_quantity,SUM(q.remaining_quantity) remaining_quantity";
                addFulfillmentFilter(text,filters,"out_quantity","return_quantity","remaining_quantity");limit=group!=null?10:count?0:50;
            } else {
                String group=documentGroup(text,"customer_name");
                select=group==null?(count?"COUNT(q.id) document_count":"q.no,q.order_time,q.customer_name,q.salesperson_name,q.dept_name,q.total_count,q.out_count,q.return_count,q.remaining_count")
                        :group+",SUM(q.total_count) total_quantity,SUM(q.out_count) fulfilled_quantity,SUM(q.return_count) return_quantity,SUM(q.remaining_count) remaining_quantity";
                addFulfillmentFilter(text,filters,"out_count","return_count","remaining_count");limit=group!=null?10:count?0:50;
            }
        } else if("query_inventory_details".equals(tool)) {
            String group=inventoryGroup(text);
            select=group==null?"q.product_name,q.warehouse_name,q.dept_name,q.unit,q.quantity"
                    :group+",SUM(q.quantity) total_quantity";
            addInventoryFilter(text,filters,parameters);limit=group!=null?20:50;
        } else if("query_stock_movements".equals(tool)) {
            select="q.product_name,q.warehouse_name,q.dept_name,q.unit,q.quantity,q.balance_quantity,q.biz_type,q.biz_no,q.biz_date";
            limit=20;
        } else throw new AssistantFailure("MODEL_INVALID","未注册的专用查询模板");
        StringBuilder sql=new StringBuilder("SELECT ").append(select).append(" FROM ").append(dataset).append(' ').append(alias);
        if(!filters.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ",filters));
        String groupExpression=groupExpression(select);
        String detailSql=null;
        if(groupExpression!=null) sql.append(" GROUP BY ").append(groupExpression).append(" ORDER BY ").append(aggregateAlias(select)).append(" DESC LIMIT ").append(limit);
        else if(!count && !isScalarAggregate(select)) {
            if(timeColumn!=null) sql.append(" ORDER BY q.").append(timeColumn).append(" DESC");
            else sql.append(" ORDER BY q.product_name,q.warehouse_name");
            if(Arrays.asList("query_stock_movements","query_inventory_details").contains(tool)) detailSql=sql.toString();
            sql.append(" LIMIT ").append(limit);
        }
        return new DedicatedQuery(sql.toString(),detailSql,parameters,tool+"-v1");
    }

    @SuppressWarnings("unchecked")
    private static void addResolvedEntityFilter(String dataset,AssistantExecutionEnvelope envelope,List<String> filters,Map<String,Object> parameters) {
        if(Arrays.asList("inventory_current","stock_movements","sale_order_items").contains(dataset)) {
            Object rawAll=envelope.getArguments().get("resolvedEntities");Set<String> usedColumns=new LinkedHashSet<>();boolean added=false;
            if(rawAll instanceof Map) for(Object item:((Map<?,?>)rawAll).values())
                if(item instanceof Map) added|=addResolvedEntityFilter(dataset,(Map<String,Object>)item,filters,parameters,usedColumns,false);
            if(added) return;
        }
        Object raw=envelope.getArguments().get("resolvedEntity");if(!(raw instanceof Map)) return;
        addResolvedEntityFilter(dataset,(Map<String,Object>)raw,filters,parameters,new LinkedHashSet<>(),true);
    }

    private static boolean addResolvedEntityFilter(String dataset,Map<String,Object> entity,List<String> filters,Map<String,Object> parameters,
                                                   Set<String> usedColumns,boolean strict) {
        Object id=entity.get("id");if(!(id instanceof Number)) return false;
        String type=String.valueOf(entity.get("entityType")),column=null;
        if("purchase_orders".equals(dataset) && "SUPPLIER".equals(type)) column="supplier_id";
        else if("sale_orders".equals(dataset) && "CUSTOMER".equals(type)) column="customer_id";
        else if("sale_orders".equals(dataset) && "SALESPERSON".equals(type)) column="salesperson_id";
        else if("sale_order_items".equals(dataset) && "CUSTOMER".equals(type)) column="customer_id";
        else if("sale_order_items".equals(dataset) && "PRODUCT".equals(type)) column="product_id";
        else if("sale_order_items".equals(dataset) && "WAREHOUSE".equals(type)) column="warehouse_id";
        else if("sale_order_items".equals(dataset) && "SALESPERSON".equals(type)) column="salesperson_id";
        else if(Arrays.asList("inventory_current","stock_movements").contains(dataset) && "PRODUCT".equals(type)) column="product_id";
        else if(Arrays.asList("inventory_current","stock_movements").contains(dataset) && "WAREHOUSE".equals(type)) column="warehouse_id";
        else if("DEPARTMENT".equals(type)) column="dept_id";
        else if(("PRODUCT".equals(type) && Arrays.asList("purchase_orders","sale_orders").contains(dataset)))
            throw new AssistantFailure("DATASET_UNPUBLISHED","按产品查询采购或销售记录需要订单明细语义数据集，当前尚未核验发布");
        else {
            if(strict) throw new AssistantFailure("CLARIFY","所选业务对象不适用于当前查询");
            return false;
        }
        if(!usedColumns.add(column)) return false;
        String parameter="p"+(parameters.size()+1);parameters.put(parameter,id);filters.add("q."+column+"=:"+parameter);
        return true;
    }

    private static boolean requiresSemanticFallback(String question,String dataset,AssistantExecutionEnvelope envelope) {
        String text=question==null?"":question.replaceAll("\\s+","");
        if(text.matches(".*(?:今年|去年|近[一二三四五六七八九十0-9]+(?:天|周|个月|月|年)|最近[一二三四五六七八九十0-9]+(?:天|周|个月|月|年)|\\d{4}[-年/]\\d{1,2}).*")) return true;
        if("purchase_orders".equals(dataset) && text.matches(".*供应商.+(?:的采购|采购订单).*" ) && !text.matches(".*(?:按|每个|各个)供应商.*"))
            return resolvedEntityByType(envelope,"SUPPLIER")==null;
        return "sale_orders".equals(dataset) && text.matches(".*客户.+(?:的销售|销售订单).*" ) && !text.matches(".*(?:按|每个|各个)客户.*")
                && resolvedEntityByType(envelope,"CUSTOMER")==null;
    }

    private static String documentGroup(String text,String partyColumn) {
        if(text.matches(".*(?:按|每个|各个)(?:供应商|客户).*")) return "q."+partyColumn;
        if(text.matches(".*(?:按|每个|各个)部门.*")) return "q.dept_name";
        if(text.matches(".*(?:按天|每天|每日|趋势).*")) return "DATE(q.order_time) period";
        if(text.matches(".*(?:按|每个|各个)状态.*")) return "q.status";
        if(text.matches(".*(?:按|每个|各个)(?:产品|配件).*"))
            throw new AssistantFailure("DATASET_UNPUBLISHED","采购或销售订单按产品统计需要订单明细语义数据集，当前尚未核验发布");
        return null;
    }

    private static String saleItemGroup(String text) {
        if(text.matches(".*(?:按|每个|各个)客户.*")) return "q.customer_name";
        if(text.matches(".*(?:按|每个|各个)(?:产品|配件|商品).*")) return "q.product_name";
        if(text.matches(".*(?:按|每个|各个)(?:业务员|销售员).*")) return "q.salesperson_name";
        if(text.matches(".*(?:按|每个|各个)仓库.*")) return "q.warehouse_name";
        if(text.matches(".*(?:按|每个|各个)部门.*")) return "q.dept_name";
        if(text.matches(".*(?:按天|每天|每日|趋势).*")) return "DATE(q.order_time) period";
        return null;
    }

    private static String saleItemAggregate(String text) {
        if(text.matches(".*(?:金额|订单额|销售额|多少钱).*")) return "SUM(q.line_amount) total_amount";
        if(text.matches(".*(?:已发货|已出库).*")) return "SUM(q.out_quantity) fulfilled_quantity";
        if(text.matches(".*(?:退货).*")) return "SUM(q.return_quantity) return_quantity";
        if(text.matches(".*(?:未发货|剩余|未完成).*")) return "SUM(q.remaining_quantity) remaining_quantity";
        if(text.matches(".*(?:订货数量|商品数量|产品数量|配件数量|销量|销售量).*")) return "SUM(q.order_quantity) total_quantity";
        return "COUNT(DISTINCT q.order_id) document_count";
    }

    private static String saleItemScalar(String text,boolean count) {
        if(count) return "COUNT(DISTINCT q.order_id) document_count";
        if(text.matches(".*(?:总金额|订单金额|订单额).*")) return "SUM(q.line_amount) total_amount";
        return null;
    }

    private static String inventoryGroup(String text) {
        if(text.matches(".*(?:按|每个|各个|各)仓库.*|.*仓库(?:分布|汇总|排名|排行).*")) return "q.warehouse_name,q.unit";
        if(text.matches(".*(?:按|每个|各个|各)(?:产品|配件|商品).*|.*(?:产品|配件)(?:分布|汇总|排名|排行).*")) return "q.product_name,q.unit";
        if(text.matches(".*(?:按|每个|各个|各)部门.*")) return "q.dept_name,q.unit";
        return null;
    }

    private static void addInventoryFilter(String text,List<String> filters,Map<String,Object> parameters) {
        if(text.matches(".*(?:负库存|库存(?:数量)?(?:小于|<)0|库存小于零).*")) filters.add("q.quantity<0");
        else if(text.matches(".*(?:正库存|有库存|库存(?:数量)?(?:大于|>)0|库存大于零).*")) filters.add("q.quantity>0");
        Matcher below=Pattern.compile("库存(?:数量)?(?:低于|少于|小于)([0-9]+(?:\\.[0-9]+)?)").matcher(text);
        if(below.find()) {String parameter="p"+(parameters.size()+1);parameters.put(parameter,new java.math.BigDecimal(below.group(1)));filters.add("q.quantity<:"+parameter);}
    }

    private static void addDocumentNumberFilter(String text,String dataset,List<String> filters,Map<String,Object> parameters) {
        if(!Arrays.asList("sale_orders","sale_order_items").contains(dataset)) return;
        Matcher matcher=Pattern.compile("(?:销售)?订单(?:号|编号)?(?:为|是|[:：])?([A-Za-z0-9_-]{4,60})",Pattern.CASE_INSENSITIVE).matcher(text);
        if(!matcher.find()) return;
        String parameter="p"+(parameters.size()+1);parameters.put(parameter,matcher.group(1));
        filters.add("q."+("sale_order_items".equals(dataset)?"order_no":"no")+"=:"+parameter);
    }

    private static String saleOrderDataset(String question,AssistantExecutionEnvelope envelope) {
        String text=question==null?"":question.replaceAll("\\s+","");
        Set<String> types=resolvedEntityTypes(envelope);
        if(types.contains("PRODUCT") || types.contains("WAREHOUSE") || text.matches(".*(?:产品|配件|商品|明细|业务员|销售员).*")) return "sale_order_items";
        return "sale_orders";
    }

    private static String documentAggregate(String text) {
        if(text.matches(".*(?:平均|均价).*(?:金额|销售额|采购额|多少钱).*|.*(?:金额|销售额|采购额|多少钱).*(?:平均|均价).*")) return "AVG(q.total_price) average_amount";
        if(text.matches(".*(?:最高|最大).*(?:金额|销售额|采购额).*|.*(?:金额|销售额|采购额).*(?:最高|最大).*")) return "MAX(q.total_price) maximum_amount";
        if(text.matches(".*(?:最低|最小).*(?:金额|销售额|采购额).*|.*(?:金额|销售额|采购额).*(?:最低|最小).*")) return "MIN(q.total_price) minimum_amount";
        if(text.matches(".*(?:金额|销售额|采购额|多少钱).*")) return "SUM(q.total_price) total_amount";
        if(text.matches(".*(?:订货数量|商品数量|产品数量|配件数量).*")) return "SUM(q.total_count) total_quantity";
        return "COUNT(q.id) document_count";
    }

    private static String documentScalar(String text) {
        if(!text.matches(".*(?:金额|销售额|采购额|多少钱|订货数量|商品数量|产品数量|配件数量).*")) return null;
        return documentAggregate(text);
    }

    private static boolean isScalarAggregate(String select){return select.matches("(?:COUNT|SUM|AVG|MIN|MAX)\\(.*");}

    private static String groupExpression(String select) {
        Matcher aggregate=Pattern.compile(",(?=(?:COUNT|SUM|AVG|MIN|MAX)\\()",Pattern.CASE_INSENSITIVE).matcher(select);
        if(!aggregate.find()) return null;
        String group=select.substring(0,aggregate.start());int alias=group.lastIndexOf(' ');
        return alias>0 && group.substring(0,alias).contains("(")?group.substring(0,alias):group;
    }

    private static String aggregateAlias(String select) {
        if(select.endsWith(" total_amount")) return "total_amount";
        if(select.endsWith(" average_amount")) return "average_amount";
        if(select.endsWith(" maximum_amount")) return "maximum_amount";
        if(select.endsWith(" minimum_amount")) return "minimum_amount";
        if(select.endsWith(" total_quantity")) return "total_quantity";
        if(select.endsWith(" fulfilled_quantity")) return "fulfilled_quantity";
        if(select.endsWith(" return_quantity")) return "return_quantity";
        if(select.endsWith(" remaining_quantity")) return "remaining_quantity";
        return "document_count";
    }

    private static void addFulfillmentFilter(String text,List<String> filters,String fulfilled,String returned,String remaining) {
        if(text.matches(".*(?:未到货|没有到货|尚未到货|未发货|没有发货|尚未发货|剩余|未完成).*")) filters.add("q."+remaining+">0");
        else if(text.contains("退货")) filters.add("q."+returned+">0");
        else if(text.matches(".*(?:已到货|已发货).*")) filters.add("q."+fulfilled+">0");
    }

    private static boolean asksForDocumentCount(String text) {
        return text.matches(".*(?:几个|多少个|多少张|多少笔|单据数|订单数|数量).*订单.*")
                || text.matches(".*订单.*(?:几个|多少个|多少张|多少笔|单据数|订单数).*");
    }

    private static void addTimeFilter(String text,String column,List<String> filters,Map<String,Object> parameters) {
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));LocalDate day=now.toLocalDate();LocalDateTime start=null,end=now;
        if(text.contains("今天") || text.contains("今日")) start=day.atStartOfDay();
        else if(text.contains("昨天") || text.contains("昨日")) {start=day.minusDays(1).atStartOfDay();end=day.atStartOfDay();}
        else if(text.contains("上周")) {end=day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();start=end.minusWeeks(1);}
        else if(text.contains("本周") || text.contains("这周")) start=day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        else if(text.contains("上月")) {end=day.withDayOfMonth(1).atStartOfDay();start=end.minusMonths(1);}
        else if(text.contains("本月") || text.contains("这个月")) start=day.withDayOfMonth(1).atStartOfDay();
        if(start==null) return;
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        parameters.put("p1",start.format(formatter));parameters.put("p2",end.format(formatter));
        filters.add("q."+column+">=:p1");filters.add("q."+column+"<:p2");
    }

    private static AssistantPlan productStockPlan(String question,Map<String,Object> arguments) {
        String product=stringValue(arguments.get("product"));
        if(product==null) product=extractProduct(question);
        if(product==null) throw new AssistantFailure("CLARIFY","请说明要查询的产品名称、编码或型号");
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");plan.setProduct(product);
        applyProductPriceIntent(question,plan);
        String text=question==null?"":question.replaceAll("\\s+","");
        boolean grouped=Boolean.TRUE.equals(arguments.get("groupByWarehouse")) || text.matches(".*(?:各个仓库|各仓库|每个仓库|所有仓库|分别在哪些仓库).*" );
        if(grouped){plan.setGroup(AssistantPlan.Group.WAREHOUSE);plan.setLimit(50);}
        String warehouse=stringValue(arguments.get("warehouse"));if(warehouse!=null) plan.setWarehouse(warehouse);
        String mode=stringValue(arguments.get("stockMode"));plan.setStockMode(mode==null?"ALL":mode);
        return plan;
    }

    private static void applyProductPriceIntent(String question,AssistantPlan plan) {
        String text=question==null?"":question.replaceAll("\\s+","").toLowerCase(Locale.ROOT);
        String field=requestedProductPriceField(text);
        if(field!=null) {
            plan.setIncludeProductPrices(true);plan.setPriceQueryMode(AssistantPlan.PriceQueryMode.SPECIFIC);plan.setPriceField(field);
        } else if(text.matches(".*(?:价格|报价|查价|多少钱|多少元).*")) {
            plan.setIncludeProductPrices(true);plan.setPriceQueryMode(AssistantPlan.PriceQueryMode.ALL);
        } else {
            plan.setIncludeProductPrices(false);plan.setPriceQueryMode(AssistantPlan.PriceQueryMode.NONE);
        }
    }

    private static String requestedProductPriceField(String text) {
        if(text.matches(".*(?:最后采购价|上次采购价|最近采购价|最新采购价).*")) return "lastPurchasePrice";
        if(text.contains("股份价")) return "sharePrice";
        if(text.contains("批发价")) return "wholesalePrice";
        if(text.contains("零售价")) return "retailPrice";
        if(text.contains("参考价")) return "referencePrice";
        if(text.contains("最低价")) return "minPrice";
        if(text.contains("销售价") || text.contains("售价")) return "salePrice";
        if(text.contains("采购价")) return "purchasePrice";
        return null;
    }

    private static String extractProduct(String question) {
        if(question==null) return null;String value=question.trim();
        if(AssistantModelClient.isStockRankingQuestion(value)) return null;
        value=value.replaceFirst("^(?:请问|帮我查一下|帮我查询|查询一下|查询|查看一下|查看)","");
        value=value.replaceFirst("^(?:这个|这款|产品|配件|型号|规格)","");
        value=value.replaceFirst("(?:在)?(?:各个|各|每个|所有)仓库(?:中|里的|内)?(?:的)?(?:当前)?(?:库存情况|库存数量|库存量|库存多少|库存)$","");
        value=value.replaceFirst("(?:当前)?(?:库存情况|库存数量|库存量|库存多少|库存)$","");
        value=value.replaceAll("(?:的)?(?:最后采购价|上次采购价|最近采购价|最新采购价|股份价|批发价|零售价|参考价|最低价|销售价|采购价|售价|价格|报价|多少钱|多少元|有货吗|有没有货|有货没|缺货吗|库存|库存吗|查价)$","");
        value=value.replaceAll("^(?:最后采购价|上次采购价|最近采购价|最新采购价|股份价|批发价|零售价|参考价|最低价|销售价|采购价|售价|价格|报价|多少钱|多少元)$","");
        value=value.replaceAll("[？?。！!]+$","").trim();
        return value.isEmpty()?null:value;
    }

    private static String stringValue(Object value){if(!(value instanceof String))return null;String text=((String)value).trim();return text.isEmpty()?null:text;}

    private static final class DedicatedQuery {
        final String sql,detailSql,template;final Map<String,Object> parameters;
        DedicatedQuery(String sql,String detailSql,Map<String,Object> parameters,String template){this.sql=sql;this.detailSql=detailSql;this.parameters=parameters;this.template=template;}
    }

    private Map<String,Object> executePlan(AssistantExecutionEnvelope envelope,AssistantPlan plan) {
        try {
            queryTrace(envelope).put("stage","QUERY");
            if(entities!=null) entities.preparePlan(plan);
            return queries.execute(plan);
        } catch(AssistantEntityChoice choice) {throw new AssistantOrchestrationClarification(envelope,choice.response());}
    }
    private Map<String,Object> semanticFallback(String question,AssistantExecutionEnvelope envelope) {
        List<String> names=subjects(question);if(names.isEmpty()) throw new AssistantFailure("CLARIFY","请明确要查询采购、销售、库存还是资金往来数据");
        validateDatasetEntityContract(envelope,names);
        for(String name:names) semantic.validateDatasetAccess(name);
        record(envelope,null,"query_semantic_schema",Collections.singletonMap("datasets",names));
        Map<String,Object> constraint=entityConstraint(envelope);
        String modelQuestion=constraint==null?question:question+"\n业务对象已由服务端按ID解析，SQL不得添加对象名称或编码条件，服务端会强制注入对象ID过滤。";
        List<Map<String,Object>> schema=semantic.schema(names);Map<String,Object> proposal=model.semanticSql(modelQuestion,schema,null);Map<String,Object> result;
        try {record(envelope,null,"execute_semantic_query",Collections.singletonMap("attempt",1));result=executeSemantic(proposal,constraint);}
        catch(AssistantFailure first) {if(!"SQL_REJECTED".equals(first.getCode())&&!"MODEL_INVALID".equals(first.getCode()))throw first;proposal=model.semanticSql(modelQuestion,schema,first.getMessage());record(envelope,null,"execute_semantic_query",Collections.singletonMap("attempt",2));result=executeSemantic(proposal,constraint);}
        envelope.getArguments().put("sql",proposal.get("sql"));envelope.getArguments().put("parameters",safeArguments(map(proposal.get("parameters"))));return result;
    }
    private Map<String,Object> executeSemantic(Map<String,Object> proposal,Map<String,Object> constraint) {
        String sql=String.valueOf(proposal.get("sql"));Map<String,Object> parameters=map(proposal.get("parameters"));
        return constraint==null?semantic.execute(sql,parameters):semantic.execute(sql,parameters,constraint);
    }
    public Map<String,Object> rerun(AssistantExecutionEnvelope envelope,List<Map<String,Object>> catalog) {
        if(envelope==null) throw new AssistantFailure("INVALID_RERUN","历史查询上下文已失效，请重新提问");
        return execute(envelope.getQuestion(),envelope,catalog).getResult();
    }
    public Map<String,Object> details(AssistantExecutionEnvelope envelope,int page,int pageSize) {
        if(page<1 || page>10_000 || pageSize<1 || pageSize>100) throw new AssistantFailure("INVALID_PAGE","分页参数无效");
        if(AssistantMasterDataQueryService.TOOL_NAME.equals(envelope.getToolName()))
            throw new AssistantFailure("DETAIL_UNAVAILABLE","基础档案数量统计暂不提供名单明细");
        if(envelope.getLegacyPlan()!=null) return queries.details(envelope.getLegacyPlan(),page,pageSize);
        Map<String,Object> params=map(envelope.getArguments().get("parameters"));
        Object detailSql=envelope.getArguments().get("detailSql");
        if(detailSql instanceof String && !((String)detailSql).trim().isEmpty())
            return semantic.serverOwnedDetails((String)detailSql,params,entityConstraint(envelope),page,pageSize);
        return semantic.details(String.valueOf(envelope.getArguments().get("sql")),params,entityConstraint(envelope),page,pageSize);
    }
    public void validateAccess(AssistantExecutionEnvelope envelope) {
        if(envelope!=null && AssistantMasterDataQueryService.TOOL_NAME.equals(envelope.getToolName())) {
            masterData.validateAccess(masterData.parseArguments(envelope.getArguments()));return;
        }
        if(envelope.getLegacyPlan()!=null){queries.validateAccess(envelope.getLegacyPlan());return;}
        validateResolvedEntities(envelope);
        if(envelope.getArguments().get("sql")==null) return;
        String sql=String.valueOf(envelope.getArguments().get("sql"));semantic.validateAccess(sql);
    }

    @SuppressWarnings("unchecked") private void validateResolvedEntities(AssistantExecutionEnvelope envelope) {
        if(envelope==null || envelope.getArguments()==null || entities==null) return;
        Set<String> checked=new LinkedHashSet<>();Object rawAll=envelope.getArguments().get("resolvedEntities");
        if(rawAll instanceof Map) for(Object item:((Map<?,?>)rawAll).values())
            if(item instanceof Map) validateResolvedEntity((Map<String,Object>)item,checked);
        Object raw=envelope.getArguments().get("resolvedEntity");
        if(raw instanceof Map) validateResolvedEntity((Map<String,Object>)raw,checked);
    }

    private void validateResolvedEntity(Map<String,Object> entity,Set<String> checked) {
        Object id=entity.get("id");Object type=entity.get("entityType");
        if(!(id instanceof Number) || type==null) return;
        String key=type+":"+id;if(!checked.add(key)) return;
        entities.validate(AssistantEntityResolver.EntityType.valueOf(String.valueOf(type)),((Number)id).longValue());
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> entityConstraint(AssistantExecutionEnvelope envelope) {
        if(envelope==null || envelope.getArguments()==null) return null;
        List<Map<String,Object>> entities=new ArrayList<>();Object all=envelope.getArguments().get("resolvedEntities");
        if(all instanceof Map) for(Object value:((Map<?,?>)all).values()) if(value instanceof Map) entities.add(new LinkedHashMap<>((Map<String,Object>)value));
        if(entities.size()>1) {Map<String,Object> result=new LinkedHashMap<>();result.put("entities",entities);return result;}
        if(entities.size()==1) return entities.get(0);
        Object value=envelope.getArguments().get("resolvedEntity");return value instanceof Map?new LinkedHashMap<>((Map<String,Object>)value):null;
    }

    private void validateDatasetEntityContract(AssistantExecutionEnvelope envelope,Collection<String> datasets) {
        Set<String> types=resolvedEntityTypes(envelope);if(types.isEmpty()) return;
        for(String dataset:datasets) {
            if(registry.datasetContract(dataset).supportsEntities(types)) return;
        }
        throw new AssistantFailure("DATASET_UNPUBLISHED","已经识别到"+entityTypeText(types)+"，但当前语义数据集不支持该对象组合，未生成删减条件的查询");
    }

    public List<String> availableTools() {
        List<String> result=new ArrayList<>();boolean verified=false;
        for(AssistantPlan.Metric metric:AssistantPlan.Metric.values()) try {queries.authorize(metric);verified=true;}catch(AssistantFailure ignored){}
        if(verified) result.add("query_verified_metric");
        try {queries.authorize(AssistantPlan.Metric.STOCK);result.add("query_product_stock");}catch(AssistantFailure ignored){}
        try {semantic.validateDatasetAccess("inventory_current");result.add("query_inventory_details");}catch(AssistantFailure ignored){}
        try {semantic.validateDatasetAccess("stock_movements");result.add("query_stock_movements");}catch(AssistantFailure ignored){}
        try {semantic.validateDatasetAccess("purchase_orders");result.add("query_purchase_documents");result.add("query_purchase_fulfillment");}catch(AssistantFailure ignored){}
        try {semantic.validateDatasetAccess("sale_orders");semantic.validateDatasetAccess("sale_order_items");result.add("query_sale_documents");result.add("query_sale_fulfillment");}catch(AssistantFailure ignored){}
        try {queries.authorize(AssistantPlan.Metric.RECEIPT);result.add("query_cash_documents");}catch(AssistantFailure ignored){}
        try {queries.authorize(AssistantPlan.Metric.RECEIVABLE);result.add("query_account_balance");}catch(AssistantFailure ignored){}
        if(masterData!=null && masterData.hasAnyAvailableType()) result.add(AssistantMasterDataQueryService.TOOL_NAME);
        if(!result.isEmpty()) result.add("resolve_business_entity");
        if(properties.getOrchestration().isTextToSqlEnabled() && !semanticCatalog.publishedNames().isEmpty()) {
            result.add("query_semantic_schema");result.add("execute_semantic_query");
        }
        return result;
    }

    private String deterministicTool(String question) {
        String q=question.replaceAll("\\s+","").toLowerCase(Locale.ROOT);
        if(masterData!=null && masterData.parse(question)!=null) return AssistantMasterDataQueryService.TOOL_NAME;
        if(bareEntityKeyword(question)!=null) return "resolve_business_entity";
        if(q.matches(".*(?:库存流水|出入库(?:情况|明细|记录)?|库存变动|调拨记录|盘点记录).*")) return "query_stock_movements";
        if(q.contains("采购") && q.matches(".*(?:订单|采购单|下单|订货|到货|未到货).*")) return q.matches(".*(?:到货|未到货|剩余).*" )?"query_purchase_fulfillment":"query_purchase_documents";
        if(q.matches(".*(?:销售订单|销售单|客户订单|订单.*客户|客户.*订单|销售.*(?:下单|订货|发货|未发货)).*")) return q.matches(".*(?:发货|未发货|剩余|未完成).*" )?"query_sale_fulfillment":"query_sale_documents";
        if(AssistantModelClient.isStockRankingQuestion(question)) return "query_verified_metric";
        if(genericStockSkuCountQuestion(q)) return "query_verified_metric";
        if(productPriceOrAvailabilityQuestion(q)) return "query_product_stock";
        if(q.contains("库存") && AssistantSemanticExtractor.extract(question,"query_product_stock").stream()
                .anyMatch(reference->reference.type==AssistantEntityResolver.EntityType.PRODUCT)) return "query_product_stock";
        if(q.contains("库存") && q.matches(".*(?:明细|清单|列表|哪些|分布|按仓库|按产品|按配件|低于|少于).*")) return "query_inventory_details";
        if(q.contains("库存") && q.matches(".*(?:产品|配件|型号|编码|各个仓库|各仓库|每个仓库).*")) return "query_product_stock";
        if(q.matches(".*(?:销售|销量|销售额|采购|库存|收款|回款|收了(?:多少|多少钱|多少款)?(?:钱|款)?回来|付款|应收|应付|欠款|欠钱|客户余额).*")) return "query_verified_metric";
        AssistantSemanticKnowledge.Match semanticMatch=semanticKnowledge.match(question);
        if("PRODUCT_PRICE_STOCK".equals(semanticMatch.getIntentId())) return "query_product_stock";
        if(Arrays.asList("SALE","RECEIVABLE").contains(semanticMatch.getIntentId())) return "query_verified_metric";
        return null;
    }
    /** Clear metric/period questions do not need a model round-trip. */
    private static AssistantPlan deterministicVerifiedPlan(String question,Map<String,Object> inheritedEntity,AssistantPlan previousPlan) {
        String text=question==null?"":question.replaceAll("\\s+","").toLowerCase(Locale.ROOT);
        if(genericStockSkuCountQuestion(text)) {
            AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK_SKU);
            plan.setPeriod("CURRENT");plan.setGroup(AssistantPlan.Group.NONE);plan.setStockMode("POSITIVE");return plan;
        }
        AssistantPlan balance=contextualBalancePlan(text,inheritedEntity);
        if(balance!=null) return balance;
        AssistantPlan contextualSale=contextualSalePlan(text,inheritedEntity);
        if(contextualSale!=null) return contextualSale;
        String period=AssistantModelClient.explicitRelativePeriod(text);
        AssistantPlan.Metric metric=deterministicPeriodMetric(text);
        if(metric!=null && period!=null) {
            AssistantPlan plan=new AssistantPlan();plan.setMetric(metric);plan.setPeriod(period);
            plan.setGroup(isDailyTrend(text)?AssistantPlan.Group.DAY:resolvedMetricGroup(metric,inheritedEntity));return plan;
        }
        if(period!=null && deterministicPeriodContinuation(question,previousPlan)) return copyWithPeriod(previousPlan,period);
        return null;
    }

    private static boolean genericStockSkuCountQuestion(String text) {
        return text.matches(".*(?:库存中|库存里|库存内)(?:有)?(?:多少|几个|几种)(?:个|种)?(?:产品|配件).*" )
                || text.matches(".*有库存的(?:产品|配件)(?:数|数量|个数|种数).*" );
    }

    private static AssistantPlan.Metric deterministicPeriodMetric(String text) {
        if(text.matches(".*(?:销售总金额|销售总额|销售金额|销售额).*")) return AssistantPlan.Metric.SALE;
        if(text.matches(".*(?:采购总金额|采购总额|采购金额|采购额).*")) return AssistantPlan.Metric.PURCHASE;
        if(text.matches(".*(?:(?:实际)?收款|回款|收了(?:多少|多少钱|多少款)?(?:钱|款)?回来|收回(?:多少|多少钱|多少款)?(?:钱|款)?).*")) return AssistantPlan.Metric.RECEIPT;
        if(text.matches(".*(?:实际)?付款.*")) return AssistantPlan.Metric.PAYMENT;
        return null;
    }

    private static boolean deterministicPeriodContinuation(String question,AssistantPlan previousPlan) {
        String text=question==null?"":question.replaceAll("\\s+","");
        if(previousPlan==null || AssistantModelClient.explicitRelativePeriod(text)==null
                || !AssistantModelClient.isContinuationQuestion(question)) return false;
        return Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.PURCHASE,
                AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.PAYMENT).contains(previousPlan.getMetric());
    }

    private static AssistantPlan copyWithPeriod(AssistantPlan source,String period) {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(source.getMetric());plan.setGroup(source.getGroup());plan.setPeriod(period);
        plan.setWarehouse(source.getWarehouse());plan.setParty(source.getParty());plan.setProduct(source.getProduct());plan.setDepartment(source.getDepartment());
        plan.setWarehouseId(source.getWarehouseId());plan.setPartyId(source.getPartyId());plan.setProductId(source.getProductId());plan.setDepartmentId(source.getDepartmentId());
        plan.setStockMode(source.getStockMode());plan.setLimit(source.getLimit());plan.setRankOrder(source.getRankOrder());return plan;
    }

    private static boolean isDailyTrend(String text) {
        return text.matches(".*(?:趋势|每日|每天|按天|按日).*" );
    }

    private static AssistantPlan.Group resolvedMetricGroup(AssistantPlan.Metric metric,Map<String,Object> entity) {
        if(entity==null) return AssistantPlan.Group.NONE;
        String type=stringValue(entity.get("entityType"));
        if("PRODUCT".equals(type) && (metric==AssistantPlan.Metric.SALE || metric==AssistantPlan.Metric.PURCHASE)) return AssistantPlan.Group.PRODUCT;
        if("DEPARTMENT".equals(type)) return AssistantPlan.Group.DEPT;
        return AssistantPlan.Group.NONE;
    }

    /** A resolved customer/supplier plus a current-balance phrase is fully deterministic. */
    private static AssistantPlan contextualBalancePlan(String text,Map<String,Object> resolvedEntity) {
        if(resolvedEntity==null) return null;
        String type=stringValue(resolvedEntity.get("entityType"));
        AssistantPlan.Metric metric;
        if("CUSTOMER".equals(type) && text.matches(".*(?:欠款|欠钱|应收|客户余额).*") )
            metric=AssistantPlan.Metric.RECEIVABLE;
        else if("SUPPLIER".equals(type) && text.matches(".*(?:应付|供应商余额).*") )
            metric=AssistantPlan.Metric.PAYABLE;
        else return null;
        AssistantPlan plan=new AssistantPlan();plan.setMetric(metric);plan.setGroup(AssistantPlan.Group.NONE);
        plan.setPeriod("CURRENT");return plan;
    }

    /** 已确认对象后的简短销量追问未给期间时使用年初至查询时刻。 */
    private static AssistantPlan contextualSalePlan(String text,Map<String,Object> inheritedEntity) {
        if(inheritedEntity==null || !text.matches(".*(?:销量|销售量|销售情况|卖了多少).*")) return null;
        // 明确给出自定义时间时仍交给模型解析，避免把“8 月份销量”等问题误套为本月。
        if(hasCustomSalesPeriod(text)) return null;
        String type=stringValue(inheritedEntity.get("entityType"));
        AssistantPlan.Group group;
        if("PRODUCT".equals(type)) group=AssistantPlan.Group.PRODUCT;
        else if("CUSTOMER".equals(type)) group=AssistantPlan.Group.PARTY;
        else if("DEPARTMENT".equals(type)) group=AssistantPlan.Group.DEPT;
        else return null;
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.SALE);plan.setGroup(group);
        String period=salesPeriod(text);
        if(period!=null) plan.setPeriod(period); else applyYearToDate(plan);
        return plan;
    }

    private static boolean hasCustomSalesPeriod(String text) {
        if(AssistantModelClient.explicitRelativePeriod(text)!=null) return false;
        return text.matches(".*(?:今年|去年|前年|上半年|下半年|第[一二三四1234]季度|"
                + "(?:近|最近|过去)[0-9一二三四五六七八九十百]+(?:天|周|个月|月)|"
                + "\\d{4}(?:年|[-/])\\d{1,2}|\\d{1,2}月份?|[一二三四五六七八九十]{1,3}月份?).*");
    }

    private static String salesPeriod(String text) {
        return AssistantModelClient.explicitRelativePeriod(text);
    }

    private static void applyYearToDate(AssistantPlan plan) {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        plan.setPeriod("CUSTOM");plan.setStartDate(today.withDayOfYear(1).toString());plan.setEndDate(today.toString());
    }

    private void applySemanticKnowledge(AssistantExecutionEnvelope envelope,AssistantSemanticKnowledge.Match match) {
        Map<String,Object> semantic=envelope.getSemanticResult();
        semantic.put("intentId",match.getIntentId());semantic.put("capabilityId",match.getCapabilityId());
        semantic.put("knowledgeVersion",match.getIntentId().equals("NO_INTENT")?null:semanticKnowledge.version());
        semantic.put("expectedEntityTypes",match.getEntityTypes());semantic.put("defaultPeriod",match.getDefaultPeriod());
        semantic.put("outcomeStatus",match.getOutcomeStatus());
    }

    private static String unverifiedCapabilityText(String question) {
        String text=question==null?"":question;
        if(text.matches(".*(?:账龄).*")) return "账龄查询已识别，但统计口径和业务对账尚未完成，暂不返回数值";
        if(text.matches(".*(?:跨账套|其他账套).*")) return "跨账套欠款已识别，但账套范围、权限和口径尚未核验，暂不返回数值";
        if(text.matches(".*(?:库存金额|库存成本|存货金额|存货成本|库存货值).*")) return "库存金额已识别，但成本版本、字段权限和库存时点口径尚未完成业务对账，暂不返回数值";
        return "毛利或利润分析已识别，但成本、退货、调价和权限口径尚未核验，暂不返回数值";
    }
    private static boolean productPriceOrAvailabilityQuestion(String q) {
        if(q.matches(".*(?:销售额|采购额|销量|销售量|欠款|欠钱|客户余额|应收|应付|收款|付款|订单|单据|报表|排名|最多|最少).*")) return false;
        boolean priceAsk=q.matches(".*(?:价格|报价|查价|多少钱|多少元|最后采购价|上次采购价|最近采购价|最新采购价|股份价|零售价|销售价|售价|批发价|参考价|最低价|采购价).*");
        boolean availabilityAsk=q.matches(".*(?:有没有货|有货|缺货|库存).*");
        boolean productish=q.matches(".*(?:产品|配件|型号|规格|编码|机油|防冻液|齿轮油|轮胎|美孚|黑霸王|速霸|力霸|突破|每日保护|全效|孚配|xhp|gl-|\\d+w|dot|k\\d+|x\\d+|[a-z]\\d{4,}).*");
        boolean directCode=q.matches("^[a-z]\\d{4,}$");
        return productish && (priceAsk || availabilityAsk || q.matches(".*(?:每日保护|全效|力霸|孚配|美孚|黑霸王|速霸|突破|xhp|gl-|\\d+w|dot|k\\d+|x\\d+).*"))
                || directCode
                || priceAsk && q.matches("^(?:价格|报价|查价|多少钱|多少元|最后采购价|上次采购价|最近采购价|最新采购价|股份价|零售价|销售价|售价|批发价|参考价|最低价|采购价)$")
                || availabilityAsk && q.matches("^(?:有没有货|有货|缺货)$");
    }
    private boolean coveredMetric(String question,String chosen) {return "query_verified_metric".equals(deterministicTool(question)) && !"query_verified_metric".equals(chosen);}
    private static List<String> subjects(String question) {List<String> r=new ArrayList<>();String q=question.replaceAll("\\s+","");if(q.contains("采购"))r.add("purchase_orders");if(q.contains("销售")||q.contains("客户")&&q.contains("订单"))r.add(q.matches(".*(?:产品|配件|商品|明细|业务员|销售员).*" )?"sale_order_items":"sale_orders");if(q.contains("库存")||q.contains("仓库"))r.add("inventory_current");if(q.contains("流水")||q.contains("出入库")||q.contains("调拨")||q.contains("盘点"))r.add("stock_movements");return r;}
    private static Map<String,Object> context(AssistantExecutionEnvelope previous){if(previous==null)return Collections.emptyMap();Map<String,Object> value=new LinkedHashMap<>();value.put("routeType",previous.getRouteType());value.put("toolName",previous.getToolName());if(previous.getLegacyPlan()!=null)value.put("metric",previous.getLegacyPlan().getMetric());return value;}
    private static AssistantExecutionEnvelope.RouteType routeType(String tool) {
        if("query_verified_metric".equals(tool)) return AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC;
        if("query_semantic_schema".equals(tool) || "execute_semantic_query".equals(tool)) return AssistantExecutionEnvelope.RouteType.TEXT_TO_SQL;
        return AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL;
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> queryTrace(AssistantExecutionEnvelope envelope) {
        Object value=envelope.getResultMetadata().get("queryTrace");
        if(value instanceof Map) return (Map<String,Object>)value;
        Map<String,Object> trace=new LinkedHashMap<>();envelope.getResultMetadata().put("queryTrace",trace);return trace;
    }
    private static void addRankingTrace(String question,AssistantExecutionEnvelope envelope,AssistantPlan plan) {
        if(!AssistantModelClient.isStockRankingQuestion(question)) return;
        Map<String,Object> trace=queryTrace(envelope);trace.put("ranking",true);trace.put("dimension",plan.getGroup().name());
        trace.put("rankOrder",plan.getRankOrder().name());trace.put("limit",plan.getLimit());trace.put("entityResolutionSkipped",true);
    }
    @SuppressWarnings("unchecked") private static void captureRankedEntity(AssistantExecutionEnvelope envelope,Map<String,Object> result) {
        AssistantPlan plan=envelope.getLegacyPlan();
        if(plan==null || plan.getPresentationMode()!=AssistantPlan.PresentationMode.RANKING) return;
        String type=plan.getGroup()==AssistantPlan.Group.PRODUCT?"PRODUCT":plan.getGroup()==AssistantPlan.Group.WAREHOUSE?"WAREHOUSE":null;
        if(type==null) return;
        Object rawRows=result.get("rows");if(!(rawRows instanceof List)) return;
        List<Map<String,Object>> topRows=new ArrayList<>();
        for(Object item:(List<?>)rawRows) {
            if(!(item instanceof Map)) continue;
            Map<String,Object> row=(Map<String,Object>)item;
            Object rank=row.containsKey("unitRank")?row.get("unitRank"):row.get("unit_rank");
            if(rank==null && topRows.isEmpty() || rank!=null && "1".equals(String.valueOf(rank))) topRows.add(row);
            else if(rank==null) break;
        }
        if(topRows.size()!=1) return;
        Map<String,Object> row=topRows.get(0);Object id=row.get("groupId"),name=row.get("label");
        if(!(id instanceof Number) || name==null || String.valueOf(name).trim().isEmpty()) return;
        Map<String,Object> entity=new LinkedHashMap<>();entity.put("id",((Number)id).longValue());
        entity.put("name",String.valueOf(name));entity.put("entityType",type);entity.put("matchType","RANK_TOP");
        putResolvedEntity(envelope,entity,true);
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value){return value instanceof Map?new LinkedHashMap<>((Map<String,Object>)value):new LinkedHashMap<>();}
    private static Map<String,Object> safeArguments(Map<String,Object> values){
        Map<String,Object> result=new LinkedHashMap<>();
        for(Map.Entry<String,Object> item:values.entrySet()) result.put(item.getKey(),safeArgument(item.getValue()));
        return result;
    }
    private static Object safeArgument(Object value){
        if(value instanceof String) return AssistantModelClient.redact((String)value);
        if(value instanceof Map) {
            Map<String,Object> nested=new LinkedHashMap<>();
            for(Map.Entry<?,?> item:((Map<?,?>)value).entrySet()) nested.put(String.valueOf(item.getKey()),safeArgument(item.getValue()));
            return nested;
        }
        if(value instanceof Collection) {
            List<Object> nested=new ArrayList<>();for(Object item:(Collection<?>)value) nested.add(safeArgument(item));return nested;
        }
        return value;
    }
    private void record(AssistantExecutionEnvelope envelope,String id,String name,Map<String,Object> arguments) {
        if(envelope.getToolCalls().size()>=Math.max(1,properties.getOrchestration().getMaxToolRounds()))
            throw new AssistantFailure("TOOL_LIMIT","本次问题需要的查询步骤过多，请缩小查询范围");
        Map<String,Object> call=new LinkedHashMap<>();call.put("id",id==null?UUID.randomUUID().toString():id);call.put("name",name);call.put("arguments",arguments);envelope.getToolCalls().add(call);
    }
    private void decorate(Map<String,Object> result,AssistantExecutionEnvelope envelope,String basis){result.put("queryType",envelope.getRouteType().name());result.put("toolName",envelope.getToolName());result.put("title","query_product_stock".equals(envelope.getToolName())?"产品价格/库存查询":((Map<?,?>)result.get("knowledge")).get("title"));result.putIfAbsent("columns",Collections.emptyList());result.put("unit",null);result.put("timeRange",Arrays.asList(result.get("start"),result.get("end")));result.put("basis",Collections.singletonList(basis));result.put("traceId",UUID.randomUUID().toString());}
    private static String toolTitle(String tool){if(tool.contains("purchase"))return "采购订单查询";if(tool.contains("sale"))return "销售订单查询";if("query_inventory_details".equals(tool))return "当前库存明细查询";if(tool.contains("stock"))return "库存流水查询";return "业务查询";}

    public static final class Outcome {
        private final AssistantExecutionEnvelope envelope;private final Map<String,Object> result;private final AssistantPlan clarification;
        private Outcome(AssistantExecutionEnvelope e,Map<String,Object> r,AssistantPlan c){envelope=e;result=r;clarification=c;}
        static Outcome result(AssistantExecutionEnvelope e,Map<String,Object> r){return new Outcome(e,r,null);}static Outcome clarify(AssistantExecutionEnvelope e,AssistantPlan c){return new Outcome(e,null,c);}
        public AssistantExecutionEnvelope getEnvelope(){return envelope;}public Map<String,Object> getResult(){return result;}public AssistantPlan getClarification(){return clarification;}
    }
}
