package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Computes and briefly caches the page capability presentation for the current identity. */
@Service
public class AssistantReadinessService {

    private static final int MAX_CACHE_ENTRIES = 1_000;

    @Resource private AssistantStore store;
    @Resource private AssistantQueryService queries;
    @Resource private AssistantKnowledge knowledge;
    @Resource private AssistantProperties properties;
    @Resource private AssistantReadOnly reader;
    @Resource private AssistantOrchestrator orchestrator;
    @Resource private AssistantSemanticCatalog semanticCatalog;
    @Resource private AssistantEntityResolver entityResolver;
    @Resource private AssistantSemanticKnowledge semanticKnowledge;
    @Resource private AssistantCompanyKnowledge companyKnowledge;
    @Resource private AssistantEngineRouter engineRouter;
    @Resource private AssistantCapabilityRegistry capabilityRegistry;
    @Resource private AssistantPhoneticNormalizer phoneticNormalizer;
    @Resource private PermissionApi permissions;

    private volatile Cache<IdentityKey, ReadinessValue> cache;
    private volatile int configuredCacheSeconds = -1;

    public Map<String, Object> get(boolean refresh) {
        IdentityKey key = currentIdentity();
        Cache<IdentityKey, ReadinessValue> currentCache = cache();
        if (refresh) {
            currentCache.invalidate(key);
        }
        try {
            ReadinessValue value = currentCache.get(key,
                    () -> permissions.executeInReadSnapshot(this::compute));
            if (!value.cacheable) {
                currentCache.invalidate(key);
            }
            return value.body;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("智能问数能力检查失败", cause);
        }
    }

    private Cache<IdentityKey, ReadinessValue> cache() {
        int seconds = Math.max(0, properties.getReadinessCacheSeconds());
        Cache<IdentityKey, ReadinessValue> local = cache;
        if (local != null && configuredCacheSeconds == seconds) {
            return local;
        }
        synchronized (this) {
            if (cache == null || configuredCacheSeconds != seconds) {
                cache = CacheBuilder.newBuilder()
                        .maximumSize(MAX_CACHE_ENTRIES)
                        .expireAfterWrite(Math.max(1, seconds), TimeUnit.SECONDS)
                        .build();
                configuredCacheSeconds = seconds;
            }
            return cache;
        }
    }

    private IdentityKey currentIdentity() {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        return new IdentityKey(TenantContextHolder.getTenantId(),
                user == null ? null : user.getTenantId(),
                user == null ? null : user.getVisitTenantId(),
                user == null ? null : user.getId());
    }

    private ReadinessValue compute() {
        Map<String,Object> result=new LinkedHashMap<>();
        boolean enabled=properties.isEnabled();
        boolean modelConfigured=properties.getApiKey()!=null && !properties.getApiKey().trim().isEmpty();
        boolean readerConfigured=reader.configured();
        boolean transientFailure=false;
        String metricState="READY", metricText="可以查询已开放指标";
        if(!enabled) {metricState="DISABLED";metricText="智能问数尚未开放";}
        else if(!readerConfigured) {metricState="READ_ONLY_NOT_CONFIGURED";metricText="正式业务只读账号尚未配置";}
        else if(!modelConfigured) {metricState="MODEL_NOT_CONFIGURED";metricText="模型服务尚未配置";}
        else try {reader.probe();} catch(AssistantFailure e) {
            metricState=e.getCode();metricText=e.getMessage();transientFailure=true;
        }
        boolean auditReady=store.hybridAuditSchemaReady();
        String companyReason=!enabled?"智能问数尚未开放":!properties.getV2().isKnowledgeEnabled()?"公司知识问答已临时关闭":!modelConfigured?"模型服务尚未配置"
                :!companyKnowledge.available()?Optional.ofNullable(companyKnowledge.unavailableReason()).orElse("公司知识资源不可用")
                :!auditReady?"智能查询审计扩展尚未完成数据库迁移":null;
        boolean companyKnowledgeAvailable=companyReason==null;
        List<Map<String,Object>> metrics=new ArrayList<>();
        Set<AssistantPlan.Metric> authorizedMetrics=EnumSet.noneOf(AssistantPlan.Metric.class);
        int authorized=0,publishedCount=0,availableMetricCount=0;
        for(Map<String,Object> item:knowledge.catalog()) {
            Map<String,Object> metric=new LinkedHashMap<>();metric.put("id",item.get("id"));metric.put("title",item.get("title"));
            boolean published=Boolean.TRUE.equals(item.get("published"));
            String reason=published?metricState:"UNPUBLISHED",reasonText=published?metricText:"该指标尚未完成核验发布";
            boolean permitted=false;
            AssistantPlan.Metric metricType=AssistantPlan.Metric.valueOf((String)item.get("id"));
            try {queries.authorize(metricType);permitted=true;authorizedMetrics.add(metricType);authorized++;if(published)publishedCount++;}
            catch(AssistantFailure denied) {reason=denied.getCode();reasonText=denied.getMessage();}
            boolean available="READY".equals(metricState) && published && permitted;
            if(available) availableMetricCount++;
            metric.put("available",available);metric.put("reason",reason);metric.put("reasonText",reasonText);metrics.add(metric);
        }
        String state,text;
        if(!enabled) {state="DISABLED";text="智能问数尚未开放";}
        else if(!modelConfigured) {state=!readerConfigured?metricState:"MODEL_NOT_CONFIGURED";text=!readerConfigured?metricText:"模型服务尚未配置";}
        else if(availableMetricCount>0 || companyKnowledgeAvailable) {
            state="READY";text=availableMetricCount>0 && companyKnowledgeAvailable?"可以查询已开放指标、公司知识或混合问题"
                    :companyKnowledgeAvailable?"可以查询公司知识":"可以查询已开放指标";
        } else if(!"READY".equals(metricState)) {state=metricState;text=metricText;}
        else if(authorized==0) {state="FORBIDDEN";text="没有可使用的智能问数或公司知识能力";}
        else if(publishedCount==0) {state="UNPUBLISHED";text="业务指标和公司知识尚未发布";}
        else {state="KNOWLEDGE_UNAVAILABLE";text=companyReason;}
        result.put("status",state);result.put("text",text);result.put("metrics",metrics);result.put("knowledgeVersion",knowledge.version());
        result.put("orchestrationMode",properties.getOrchestration().getMode());result.put("newOrchestration",orchestrator.enabledForCurrentUser() && auditReady);
        result.put("hybridAuditReady",auditReady);
        result.put("tools",auditReady?orchestrator.availableTools(authorizedMetrics):Collections.emptyList());result.put("textToSql",auditReady && properties.getOrchestration().isTextToSqlEnabled());
        result.put("semanticVersion",semanticCatalog.version());result.put("datasets",semanticCatalog.publishedNames());
        result.put("semanticKnowledgeVersion",semanticKnowledge.version());
        result.put("companyKnowledgeAvailable",companyKnowledgeAvailable);
        result.put("companyKnowledgeVersion",companyKnowledge.version());
        result.put("companyKnowledgeReason",companyReason);
        result.put("companyKnowledgeStats",companyKnowledge.statistics());
        result.put("mixedQuestionAvailable",properties.getV2().isMixedEnabled() && companyKnowledgeAvailable && auditReady && readerConfigured);
        result.put("semanticIntents",semanticKnowledge.openedIntents());
        result.put("unverifiedCapabilities",semanticKnowledge.unverifiedCapabilities());
        if(engineRouter!=null) result.put("v2",engineRouter.readiness());
        if(capabilityRegistry!=null) result.put("registeredCapabilities",capabilityRegistry.all().stream()
                .map(AssistantCapability::getId).collect(Collectors.toList()));
        Map<String,Object> entitySearch=new LinkedHashMap<>();boolean entityEnabled=entityResolver!=null && entityResolver.enabled();entitySearch.put("enabled",entityEnabled);
        entitySearch.put("types",entityEnabled?entityResolver.availableTypes():Collections.emptyList());entitySearch.put("maxCandidates",Math.max(1,Math.min(20,properties.getEntitySearch().getMaxCandidates())));
        result.put("entitySearch",entitySearch);
        Map<String,Object> phonetic=new LinkedHashMap<>();
        boolean phoneticEnabled=phoneticNormalizer!=null && phoneticNormalizer.enabled();
        phonetic.put("enabled",phoneticEnabled);
        phonetic.put("version",phoneticNormalizer==null?null:phoneticNormalizer.version());
        phonetic.put("nearSoundEnabled",phoneticEnabled && properties.getPhonetic().isNearSoundEnabled());
        phonetic.put("entityTypes",phoneticEnabled?phoneticNormalizer.supportedEntityTypes():Collections.emptyList());
        phonetic.put("reason",phoneticEnabled?null:phoneticNormalizer==null?"同音识别组件未加载":phoneticNormalizer.unavailableReason());
        result.put("phonetic",phonetic);
        return new ReadinessValue(Collections.unmodifiableMap(result), !transientFailure);
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    private static final class IdentityKey {
        private final Long effectiveTenantId;
        private final Long loginTenantId;
        private final Long visitTenantId;
        private final Long userId;
    }

    @AllArgsConstructor
    private static final class ReadinessValue {
        private final Map<String, Object> body;
        private final boolean cacheable;
    }
}
