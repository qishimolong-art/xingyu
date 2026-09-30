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
                register(new Dataset(value.path("name").asText(),value.path("title").asText(),
                        value.path("permission").asText(),value.path("module").asText(),columns));
            }
        } catch(Exception e) {throw new IllegalStateException("Unable to load semantic dataset package",e);}
    }

    private static byte[] readAll(InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int length;
        while((length=input.read(buffer))!=-1) out.write(buffer,0,length);return out.toByteArray();
    }
    private static String sha256(byte[] value) throws Exception {
        StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(value)) result.append(String.format("%02x",b));return result.toString();
    }
    private void register(Dataset dataset) {
        if(dataset.name.isEmpty() || datasets.put(dataset.name,dataset)!=null) throw new IllegalStateException("Duplicate semantic dataset: "+dataset.name);
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
        Dataset(String name,String title,String permission,String module,Map<String,String> columns) {
            this.name=name;this.title=title;this.permission=permission;this.module=module;this.columns=Collections.unmodifiableMap(new LinkedHashMap<>(columns));
        }
        public String getName(){return name;} public String getTitle(){return title;} public String getPermission(){return permission;}
        public String getModule(){return module;} public Map<String,String> getColumns(){return columns;}
    }
}
