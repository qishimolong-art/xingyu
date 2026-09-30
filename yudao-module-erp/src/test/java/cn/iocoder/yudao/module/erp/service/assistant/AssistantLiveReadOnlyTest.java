package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Explicit opt-in against the configured local read-only connection; performs SELECT only. */
@EnabledIfSystemProperty(named="assistant.readOnlyLive",matches="true")
class AssistantLiveReadOnlyTest {

    @Test
    @SuppressWarnings("unchecked")
    void departmentLookupUsesTheSameJavaReadOnlyExecutorAsRuntime() throws Exception {
        AssistantSemanticReadOnly reader=reader();
        String sql="SELECT e.id,e.name name,NULL code,"+
                "CASE WHEN parent.name IS NULL THEN e.name ELSE CONCAT(parent.name,' / ',e.name) END description,"+
                "'NAME' matchedBy FROM system_dept e LEFT JOIN system_dept parent "+
                "ON parent.id=e.parent_id AND parent.tenant_id=e.tenant_id AND parent.deleted=0 "+
                "WHERE e.tenant_id=? AND e.deleted=0 AND e.status=0 AND (e.name=?) ORDER BY e.id LIMIT 21";
        try {
            List<Map<String,Object>> rows=reader.query(sql,Arrays.asList(1L,"甘孜分公司"),21);
            assertEquals(1,rows.size());assertEquals("甘孜分公司",rows.get(0).get("name"));
        } finally {
            reader.close();
        }
    }

    @Test
    void customerAndProductKeywordsMatchCurrentBusinessMasterData() throws Exception {
        AssistantSemanticReadOnly reader=reader();
        try {
            Map<String,Object> customer=entityContext("CUSTOMER","EXACT_NAME","理塘众鑫进口汽修");
            List<Map<String,Object>> exact=query(reader,customer);
            assertEquals(1,exact.size());assertEquals("理塘众鑫进口汽修",exact.get(0).get("name"));

            customer.put("entityMatchMode","FUZZY");customer.put("entityKeyword","理塘众鑫");
            List<Map<String,Object>> partial=query(reader,customer);
            assertTrue(partial.size()>=2);assertTrue(partial.stream().allMatch(row->String.valueOf(row.get("name")).contains("理塘众鑫")));

            Map<String,Object> product=entityContext("PRODUCT","TOKEN_FUZZY","美孚 CF 4L");
            for(String flag:Arrays.asList("searchCode","searchFactoryCode","searchBarCode","searchPinyin","searchWubi",
                    "searchVehicleModel","searchStandard","searchRemark","searchBrand","searchOeNumber","searchOriginPlace",
                    "searchFeatureCode","searchDrawingNo","searchShelf")) product.put(flag,true);
            List<Map<String,Object>> products=query(reader,product);
            assertTrue(products.size()>1);
            assertTrue(products.stream().allMatch(row->row.get("id")!=null&&row.get("name")!=null));
            assertTrue(products.stream().anyMatch(row->{
                String name=String.valueOf(row.get("name")).toUpperCase();
                return name.contains("美孚黑霸王CF-4")&&name.contains("4L");
            }));
        } finally {
            reader.close();
        }
    }

    private static AssistantSemanticReadOnly reader() throws Exception {
        Map<String,Object> assistant=null;
        try(InputStream input=Files.newInputStream(Paths.get("../yudao-server/src/main/resources/application.yaml"))) {
            for(Object document:new Yaml().loadAll(input)) {
                if(!(document instanceof Map)) continue;
                Object erp=((Map<?,?>)document).get("erp");
                if(erp instanceof Map && ((Map<?,?>)erp).get("assistant") instanceof Map)
                    assistant=(Map<String,Object>)((Map<?,?>)erp).get("assistant");
            }
        }
        if(assistant==null) throw new IllegalStateException("assistant settings not found");
        Map<String,Object> readOnly=(Map<String,Object>)assistant.get("read-only");
        AssistantProperties properties=new AssistantProperties();
        properties.getReadOnly().setUrl(String.valueOf(readOnly.get("url")));
        properties.getReadOnly().setUsername(String.valueOf(readOnly.get("username")));
        properties.getReadOnly().setPassword(String.valueOf(readOnly.get("password")));
        AssistantSemanticReadOnly reader=new AssistantSemanticReadOnly();
        ReflectionTestUtils.setField(reader,"properties",properties);
        return reader;
    }

    private static Map<String,Object> entityContext(String type,String mode,String keyword) {
        Map<String,Object> context=new HashMap<>();context.put("tenantId",1L);context.put("entityType",type);
        context.put("entityMatchMode",mode);context.put("entityKeyword",keyword);context.put("entityAll",true);
        context.put("rowLimit",21);context.put("showCode",false);context.put("searchShortName",true);
        context.put("searchPinyin",true);context.put("searchWubi",true);return context;
    }

    private static List<Map<String,Object>> query(AssistantSemanticReadOnly reader,Map<String,Object> context) {
        cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantEntitySql.Query query=
                cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantEntitySql.select(context);
        return reader.query(query.getSql(),query.getParameters(),query.getMaximumRows());
    }
}
