package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Versioned logical datasets. Physical table names are intentionally absent from model metadata. */
@Component
public class AssistantSemanticCatalog {
    @Resource private AssistantProperties properties;
    private final Map<String,Dataset> datasets=new LinkedHashMap<>();
    private final String packageChecksum;

    public AssistantSemanticCatalog() {
        try {
            ClassPathResource resource=new ClassPathResource("assistant/semantic-datasets.json");
            byte[] bytes;
            try(InputStream input=resource.getInputStream()){bytes=readAll(input);}
            String actual=sha256(bytes);
            String expected;
            try(InputStream input=new ClassPathResource("assistant/semantic-datasets.sha256").getInputStream()) {
                expected=new String(readAll(input),StandardCharsets.UTF_8).trim().split("\\s+")[0];
            }
            if(!actual.equalsIgnoreCase(expected)) throw new IllegalStateException("Semantic package checksum mismatch");
            packageChecksum=actual;
            JsonNode root=new ObjectMapper().readTree(bytes);
            for(JsonNode value:root.path("datasets")) {
                Map<String,String> columns=new LinkedHashMap<>();
                Iterator<Map.Entry<String,JsonNode>> fields=value.path("columns").fields();
                while(fields.hasNext()){Map.Entry<String,JsonNode> field=fields.next();columns.put(field.getKey(),field.getValue().asText());}
                Set<String> operations=textSet(value.path("allowedOperations"));
                List<String> relationships=new ArrayList<>(textSet(value.path("relationships")));
                JsonNode cost=value.path("costPolicy");
                Set<String> selectiveColumns=textSet(cost.path("selectiveColumns"));
                Set<String> timeColumns=textSet(cost.path("timeColumns"));
                register(new Dataset(value.path("name").asText(),value.path("title").asText(),
                        value.path("permission").asText(),value.path("module").asText(),columns,
                        operations,relationships,cost.path("maxRows").asInt(200),cost.path("maxOffset").asInt(1000),
                        cost.path("maxSelectedColumns").asInt(12),cost.path("maxGroupColumns").asInt(3),
                        cost.path("queryTimeoutSeconds").asInt(10),cost.path("requiresSelectiveFilter").asBoolean(false),
                        selectiveColumns,timeColumns,cost.path("maxTimeRangeDays").asInt(0)));
            }
        } catch(Exception e) {throw new IllegalStateException("Unable to load semantic dataset package",e);}
    }

    private static LinkedHashSet<String> textSet(JsonNode values) {
        LinkedHashSet<String> result=new LinkedHashSet<>();
        if(values!=null && values.isArray()) for(JsonNode value:values) {
            String text=value.asText("").trim();if(!text.isEmpty()) result.add(text);
        }
        return result;
    }

    private static byte[] readAll(InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int length;
        while((length=input.read(buffer))!=-1) out.write(buffer,0,length);return out.toByteArray();
    }
    private static String sha256(byte[] value) throws Exception {
        StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(value)) result.append(String.format("%02x",b));return result.toString();
    }
    private void register(Dataset dataset) {
        if(dataset.name.isEmpty() || dataset.title.isEmpty() || dataset.permission.isEmpty() || dataset.module.isEmpty()
                || dataset.columns.isEmpty() || dataset.allowedOperations.isEmpty())
            throw new IllegalStateException("Incomplete semantic dataset: "+dataset.name);
        if(dataset.maxRows<1 || dataset.maxRows>200 || dataset.maxOffset<0 || dataset.maxOffset>10_000
                || dataset.maxSelectedColumns<1 || dataset.maxSelectedColumns>30 || dataset.maxGroupColumns<0
                || dataset.maxGroupColumns>8 || dataset.queryTimeoutSeconds<1 || dataset.queryTimeoutSeconds>10)
            throw new IllegalStateException("Invalid semantic dataset limits: "+dataset.name);
        if(!dataset.columns.keySet().containsAll(dataset.selectiveColumns)
                || !dataset.columns.keySet().containsAll(dataset.timeColumns)
                || !dataset.selectiveColumns.containsAll(dataset.timeColumns))
            throw new IllegalStateException("Invalid semantic dataset cost columns: "+dataset.name);
        if(dataset.requiresSelectiveFilter && dataset.selectiveColumns.isEmpty())
            throw new IllegalStateException("Selective filters are required but not configured: "+dataset.name);
        if(!dataset.timeColumns.isEmpty() && dataset.maxTimeRangeDays<1)
            throw new IllegalStateException("Time range limit is missing: "+dataset.name);
        if(datasets.put(dataset.name,dataset)!=null) throw new IllegalStateException("Duplicate semantic dataset: "+dataset.name);
    }
    public Dataset requirePublished(String name) {
        Dataset value=datasets.get(name);
        if(value==null || value.columns.isEmpty() || !properties.getOrchestration().getPublishedDatasets().contains(name))
            throw new AssistantFailure("DATASET_UNPUBLISHED","该业务数据集尚未完成核验发布");
        return value;
    }
    public Set<String> publishedNames() {
        Set<String> result=new LinkedHashSet<>();
        for(String name:properties.getOrchestration().getPublishedDatasets()) if(datasets.containsKey(name) && !datasets.get(name).columns.isEmpty()) result.add(name);
        return result;
    }
    public List<Map<String,Object>> schema(Collection<String> names) {
        List<Map<String,Object>> result=new ArrayList<>();int count=0;
        for(String name:names) {
            Dataset value=requirePublished(name);Map<String,Object> row=new LinkedHashMap<>();
            row.put("name",value.name);row.put("title",value.title);row.put("columns",value.columns);result.add(row);
            if(++count>=8) break;
        }
        return result;
    }
    public String version() {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");digest.update(packageChecksum.getBytes(StandardCharsets.UTF_8));
            for(String name:publishedNames()) digest.update(name.getBytes(StandardCharsets.UTF_8));
            StringBuilder value=new StringBuilder();for(byte b:digest.digest()) value.append(String.format("%02x",b));
            return value.substring(0,16);
        } catch(Exception e) {throw new IllegalStateException(e);}
    }
    public String packageChecksum(){return packageChecksum;}

    public static final class Dataset {
        private final String name,title,permission,module;
        private final Map<String,String> columns;
        private final Set<String> allowedOperations,selectiveColumns,timeColumns;
        private final List<String> relationships;
        private final int maxRows,maxOffset,maxSelectedColumns,maxGroupColumns,queryTimeoutSeconds,maxTimeRangeDays;
        private final boolean requiresSelectiveFilter;
        Dataset(String name,String title,String permission,String module,Map<String,String> columns,
                Set<String> allowedOperations,List<String> relationships,int maxRows,int maxOffset,
                int maxSelectedColumns,int maxGroupColumns,int queryTimeoutSeconds,boolean requiresSelectiveFilter,
                Set<String> selectiveColumns,Set<String> timeColumns,int maxTimeRangeDays) {
            this.name=name;this.title=title;this.permission=permission;this.module=module;
            this.columns=Collections.unmodifiableMap(new LinkedHashMap<>(columns));
            this.allowedOperations=Collections.unmodifiableSet(new LinkedHashSet<>(allowedOperations));
            this.relationships=Collections.unmodifiableList(new ArrayList<>(relationships));
            this.maxRows=maxRows;this.maxOffset=maxOffset;this.maxSelectedColumns=maxSelectedColumns;
            this.maxGroupColumns=maxGroupColumns;this.queryTimeoutSeconds=queryTimeoutSeconds;
            this.requiresSelectiveFilter=requiresSelectiveFilter;
            this.selectiveColumns=Collections.unmodifiableSet(new LinkedHashSet<>(selectiveColumns));
            this.timeColumns=Collections.unmodifiableSet(new LinkedHashSet<>(timeColumns));this.maxTimeRangeDays=maxTimeRangeDays;
        }
        public String getName(){return name;} public String getTitle(){return title;} public String getPermission(){return permission;}
        public String getModule(){return module;} public Map<String,String> getColumns(){return columns;}
        public Set<String> getAllowedOperations(){return allowedOperations;} public List<String> getRelationships(){return relationships;}
        public int getMaxRows(){return maxRows;} public int getMaxOffset(){return maxOffset;}
        public int getMaxSelectedColumns(){return maxSelectedColumns;} public int getMaxGroupColumns(){return maxGroupColumns;}
        public int getQueryTimeoutSeconds(){return queryTimeoutSeconds;} public boolean isRequiresSelectiveFilter(){return requiresSelectiveFilter;}
        public Set<String> getSelectiveColumns(){return selectiveColumns;} public Set<String> getTimeColumns(){return timeColumns;}
        public int getMaxTimeRangeDays(){return maxTimeRangeDays;}
        public Map<String,Object> modelPolicy(Collection<String> visibleColumns) {
            Set<String> visible=new LinkedHashSet<>(visibleColumns);Map<String,Object> result=new LinkedHashMap<>();
            result.put("allowedOperations",allowedOperations);result.put("relationships",relationships);
            result.put("maxRows",maxRows);result.put("maxOffset",maxOffset);
            result.put("maxSelectedColumns",maxSelectedColumns);result.put("maxGroupColumns",maxGroupColumns);
            result.put("requiresSelectiveFilter",requiresSelectiveFilter);
            List<String> filters=new ArrayList<>();for(String column:selectiveColumns) if(visible.contains(column)) filters.add(column);
            List<String> times=new ArrayList<>();for(String column:timeColumns) if(visible.contains(column)) times.add(column);
            result.put("selectiveColumns",filters);result.put("timeColumns",times);
            if(!times.isEmpty()) result.put("maxTimeRangeDays",maxTimeRangeDays);
            return result;
        }
    }
}
