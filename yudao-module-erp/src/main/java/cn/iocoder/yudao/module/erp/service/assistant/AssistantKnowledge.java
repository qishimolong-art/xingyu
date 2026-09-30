package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class AssistantKnowledge {
    @Resource private AssistantProperties properties;
    private Map<String,Map<String,Object>> entries = new LinkedHashMap<>();
    private String version;
    @PostConstruct
    public void load() throws Exception {
        byte[] bytes;
        try(java.io.InputStream in = new ClassPathResource("assistant/knowledge.json").getInputStream()) {
            bytes = StreamUtils.copyToByteArray(in);
        }
        String expected;
        try(java.io.InputStream in = new ClassPathResource("assistant/knowledge.sha256").getInputStream()) {
            expected = StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        }
        StringBuilder actual=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)) actual.append(String.format("%02x",b));
        if(!actual.toString().equals(expected)) throw new IllegalStateException("Assistant knowledge checksum mismatch");
        entries = new ObjectMapper().readValue(bytes,new TypeReference<LinkedHashMap<String,Map<String,Object>>>(){});
        version=expected.substring(0,16);
    }
    public String version() { return version; }
    public Map<String,Object> get(AssistantPlan.Metric metric) {
        Map<String,Object> item=new LinkedHashMap<>(entries.get(metric.name())); item.put("version",version); return item;
    }
    public void requirePublished(AssistantPlan.Metric metric) {
        Map<String,Object> item=entries.get(metric.name());
        if(item==null || !"VERIFIED".equals(item.get("status")) || !properties.getPublishedMetrics().contains(metric.name()))
            throw new AssistantFailure("UNPUBLISHED","该指标尚未完成当前环境的核验发布");
    }
    public void requirePublished(AssistantPlan plan) {
        requirePublished(plan.getMetric());
        Object groups=entries.get(plan.getMetric().name()).get("groups");
        if(!(groups instanceof List) || !((List<?>)groups).contains(plan.getGroup().name()))
            throw new IllegalArgumentException("该指标的此分组方式尚未核验开放，请选择已开放的统计方式");
    }
    public List<Map<String,Object>> catalog() {
        List<Map<String,Object>> result=new ArrayList<>();
        for(AssistantPlan.Metric m:AssistantPlan.Metric.values()) {
            Map<String,Object> item=get(m); item.put("id",m.name());
            item.put("published",properties.getPublishedMetrics().contains(m.name()) && "VERIFIED".equals(item.get("status")));
            result.add(item);
        }
        return result;
    }

    /** Small keyword index over published definitions; never sends the complete vault. */
    public List<Map<String,Object>> retrieve(String question,AssistantPlan previous,List<Map<String,Object>> available) {
        String[][] keywords={{"SALE","销售","卖","出库"},{"PURCHASE","采购","进货","入库"},
            {"STOCK","库存","仓"},{"STOCK_SKU","sku","种配件"},{"RECEIPT","收款","回款"},
            {"PAYMENT","付款","支付"},{"RECEIVABLE","应收","客户余额"},{"PAYABLE","应付","供应商余额"}};
        Set<String> selected=new HashSet<>();String normalized=question.toLowerCase(Locale.ROOT);
        for(String[] row:keywords) for(int i=1;i<row.length;i++) if(normalized.contains(row[i])) selected.add(row[0]);
        if(previous!=null && previous.getMetric()!=null) selected.add(previous.getMetric().name());
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> entry:available) if(Boolean.TRUE.equals(entry.get("published"))) {
            Map<String,Object> item=new LinkedHashMap<>();item.put("id",entry.get("id"));item.put("title",entry.get("title"));
            if(selected.contains(entry.get("id"))) item.put("description",entry.get("description"));
            result.add(item);
        }
        if(result.isEmpty()) throw new AssistantFailure("UNPUBLISHED","当前没有已核验发布且有权限的指标");
        return result;
    }
}
