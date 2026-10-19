package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantSemanticKnowledge;

import java.util.*;

/** Prepared master-data lookup SQL, separate from verified metric implementations. */
public final class AssistantEntitySql {
    private AssistantEntitySql() {}

    public static Query select(Map<String,Object> context) {
        String type=String.valueOf(context.get("entityType"));
        String match=String.valueOf(context.get("entityMatchMode"));
        String table,name,code="NULL",description="NULL",base="";
        switch(type) {
            case "PRODUCT":
                table="erp_product e";name="e.name";code=flag(context,"showCode")?"e.code":"NULL";
                base=" AND (e.merged_flag IS NULL OR e.merged_flag=0)";break;
            case "CUSTOMER":
                table="erp_customer e";name="e.name";code=flag(context,"showCode")?"e.code":"NULL";
                description=flag(context,"searchShortName")?"NULLIF(e.short_name,'')":"NULL";
                base=" AND (e.merged_flag IS NULL OR e.merged_flag=0)";break;
            case "SUPPLIER":
                table="erp_supplier e";name="e.name";code=flag(context,"showCode")?"e.code":"NULL";
                description="OLD_CODE".equals(match) && flag(context,"searchOldCode")
                        ?"CASE WHEN e.old_code IS NOT NULL AND e.old_code<>'' THEN CONCAT('旧编码：',e.old_code) ELSE NULL END"
                        :flag(context,"searchShortName")?"NULLIF(e.short_name,'')":"NULL";
                base=" AND (e.merged_flag IS NULL OR e.merged_flag=0)";break;
            case "WAREHOUSE":
                table="erp_warehouse e";name="e.name";code=flag(context,"showCode")?"e.warehouse_code":"NULL";break;
            case "DEPARTMENT":
                table="system_dept e LEFT JOIN system_dept parent ON parent.id=e.parent_id AND parent.tenant_id=e.tenant_id AND parent.deleted=0";
                name="e.name";description="CASE WHEN parent.name IS NULL THEN e.name ELSE CONCAT(parent.name,' / ',e.name) END";break;
            case "SALESPERSON":
                table="system_users e";name="e.nickname";description="'业务员'";break;
            case "PURCHASER":
                table="system_users e";name="e.nickname";description="'采购员'";break;
            case "USER":
                table="system_users e";name="e.nickname";break;
            default: throw new IllegalArgumentException("Unsupported entity type");
        }
        List<Object> parameters=new ArrayList<>();parameters.add(context.get("tenantId"));
        String scope=scope(context,type,parameters);
        String predicate=predicate(context,type,match,parameters);
        String order=order(context,type,match,name,parameters);
        int limit=Math.max(1,Math.min(21,number(context.get("rowLimit"),21)));
        String matchedBy="EXACT_NAME".equals(match)?"NAME":"FUZZY".equals(match)?"FUZZY_NAME":match;
        String sql="SELECT e.id,"+name+" name,"+code+" code,"+description+" description,'"+matchedBy+"' matchedBy FROM "+table
                +" WHERE e.tenant_id=? AND e.deleted=0 AND e.status=0"+base+scope+" AND ("+predicate+") "+order+" LIMIT "+limit;
        return new Query(sql,parameters,limit);
    }

    private static String predicate(Map<String,Object> context,String type,String match,List<Object> parameters) {
        if("ID".equals(match)) {parameters.add(context.get("entityId"));return "e.id=?";}
        List<String> terms=new ArrayList<>();Object keyword=context.get("entityKeyword");
        if("CODE".equals(match) && !"PRODUCT".equals(type) && flag(context,"searchCode")) {
            add(terms,parameters,"UPPER(COALESCE(e."+("WAREHOUSE".equals(type)?"warehouse_code":"code")+",''))=UPPER(?)",keyword);
        } else if("OLD_CODE".equals(match) && "SUPPLIER".equals(type) && flag(context,"searchOldCode")) {
            add(terms,parameters,"UPPER(COALESCE(e.old_code,''))=UPPER(?)",keyword);
        } else if("EXACT_NAME".equals(match)) {
            add(terms,parameters,userType(type)?"e.nickname=?":"e.name=?",keyword);
            if(flag(context,"searchShortName")) add(terms,parameters,"e.short_name=?",keyword);
        } else if("BRAND".equals(match) && "PRODUCT".equals(type)) {
            add(terms,parameters,"UPPER(COALESCE(e.name,'')) LIKE CONCAT('%',UPPER(?),'%')",keyword);
        } else if("TOKEN_FUZZY".equals(match) && "PRODUCT".equals(type)) {
            return productTokenPredicate(context,parameters);
        } else if("PINYIN".equals(match) && !"PRODUCT".equals(type) && flag(context,"searchPinyin")) {
            add(terms,parameters,"UPPER(COALESCE(e.pinyin_code,'')) LIKE CONCAT('%',UPPER(?),'%')",keyword);
        } else if("FUZZY".equals(match)) {
            add(terms,parameters,userType(type)?"e.nickname LIKE CONCAT('%',?,'%')":"e.name LIKE CONCAT('%',?,'%')",keyword);
            if("PRODUCT".equals(type)) {
                String productKeyword=String.valueOf(keyword);
                String compactKeyword=AssistantSemanticKnowledge.compactProductUnitSpacing(productKeyword);
                ErpKeywordQuery.KeywordSearch parsed=ErpKeywordQuery.parseKeywordSearch(compactKeyword);
                if(!ErpKeywordQuery.shouldAppendTokenProductItemCondition(parsed)) {
                    Set<String> patterns=new LinkedHashSet<>();
                    for(String variant:AssistantSemanticKnowledge.productUnitVariants(compactKeyword)) {
                        if(!variant.equals(productKeyword.trim())) patterns.add("%"+variant+"%");
                        String ordered=AssistantSemanticKnowledge.productUnitAwareOrderedPattern(compactKeyword,variant);
                        if(ordered!=null && !ordered.equals(variant)) patterns.add("%"+ordered+"%");
                    }
                    for(String pattern:patterns)
                        terms.add(likeAny(productNameColumn(),pattern,parameters));
                }
            }
            if(flag(context,"searchShortName")) add(terms,parameters,"e.short_name LIKE CONCAT('%',?,'%')",keyword);
            if(!"PRODUCT".equals(type) && flag(context,"searchPinyin"))
                add(terms,parameters,"e.pinyin_code LIKE CONCAT(?,'%')",keyword);
            if(!"PRODUCT".equals(type) && flag(context,"searchWubi"))
                add(terms,parameters,"e.wubi_code LIKE CONCAT(?,'%')",keyword);
        }
        return terms.isEmpty()?"1=0":String.join(" OR ",terms);
    }

    private static String order(Map<String,Object> context,String type,String match,String name,List<Object> parameters) {
        if ("PRODUCT".equals(type) || !"PINYIN".equals(match) || !flag(context,"searchPinyin"))
            return "ORDER BY e.id";
        Object keyword=context.get("entityKeyword");
        parameters.add(keyword);parameters.add(keyword);parameters.add(keyword);
        return "ORDER BY CASE WHEN UPPER(COALESCE(e.pinyin_code,''))=UPPER(?) THEN 0 "
                +"WHEN UPPER(COALESCE(e.pinyin_code,'')) LIKE CONCAT(UPPER(?),'%') THEN 1 ELSE 2 END,"
                +"LOCATE(UPPER(?),UPPER(COALESCE(e.pinyin_code,''))),CHAR_LENGTH("+name+"),e.id";
    }

    /** Product name-only search that keeps both character order and cross-token order. */
    private static String productTokenPredicate(Map<String,Object> context,List<Object> parameters) {
        String rawKeyword=String.valueOf(context.get("entityKeyword"));
        String keyword=AssistantSemanticKnowledge.compactProductUnitSpacing(rawKeyword);
        ErpKeywordQuery.KeywordSearch parsed=ErpKeywordQuery.parseKeywordSearch(keyword);
        if(!ErpKeywordQuery.shouldAppendTokenProductItemCondition(parsed)) {
            if(keyword.equals(rawKeyword.trim()) || keyword.isEmpty()) return "1=0";
        }
        List<String> columns=productNameColumn();
        if(columns.isEmpty()) return "1=0";
        Set<String> orderedPatterns=new LinkedHashSet<>();
        List<String> sourceTokens=productKeywordTokens(keyword);
        for(String variant:AssistantSemanticKnowledge.productUnitVariants(keyword)) {
            List<String> tokens=productKeywordTokens(variant);
            if(tokens.isEmpty()) continue;
            String compact=String.join("",tokens);
            orderedPatterns.add("%"+String.join("%",tokens)+"%");
            String ordered=orderedProductTokenPattern(sourceTokens,tokens);
            if(ordered!=null && !ordered.equals(compact)) orderedPatterns.add("%"+ordered+"%");
        }
        List<String> alternatives=new ArrayList<>();
        for(String pattern:orderedPatterns) alternatives.add(likeAny(columns,pattern,parameters));
        return alternatives.isEmpty()?"1=0":"("+String.join(" OR ",alternatives)+")";
    }

    private static String orderedProductTokenPattern(List<String> sourceTokens,List<String> candidateTokens) {
        if(sourceTokens.size()!=candidateTokens.size())
            return orderedCharacterPattern(String.join("",candidateTokens));
        List<String> orderedTokens=new ArrayList<>();
        for(int i=0;i<candidateTokens.size();i++) {
            String ordered=AssistantSemanticKnowledge.productUnitAwareOrderedPattern(
                    sourceTokens.get(i),candidateTokens.get(i));
            if(ordered!=null && !ordered.isEmpty()) orderedTokens.add(ordered);
        }
        return String.join("%",orderedTokens);
    }

    private static List<String> productKeywordTokens(String keyword) {
        ErpKeywordQuery.KeywordSearch parsed=ErpKeywordQuery.parseKeywordSearch(keyword);
        if(ErpKeywordQuery.shouldAppendTokenProductItemCondition(parsed)) return parsed.tokens();
        String value=keyword==null?"":keyword.trim();
        return value.isEmpty()?Collections.emptyList():Collections.singletonList(value);
    }

    private static List<String> productNameColumn() {
        return Collections.singletonList("e.name");
    }

    /** Every product-name term keeps the shared ordered-character compatibility, including 530 -> 5%3%0. */
    private static String orderedCharacterPattern(String value) {
        return value==null?null:ErpKeywordQuery.orderedCharactersPattern(value);
    }

    private static String likeAny(List<String> columns,String value,List<Object> parameters) {
        List<String> terms=new ArrayList<>();
        for(String column:columns) {
            terms.add("UPPER(COALESCE("+column+",'')) LIKE UPPER(?)");parameters.add(value);
        }
        return String.join(" OR ",terms);
    }

    private static String scope(Map<String,Object> context,String type,List<Object> parameters) {
        if(flag(context,"entityAll")) return "";
        Collection<?> dept=values(context.get("entityDeptIds"));Collection<?> warehouse=values(context.get("entityWarehouseIds"));
        List<String> terms=new ArrayList<>();
        if("PRODUCT".equals(type)) {
            if(!dept.isEmpty()) {
                terms.add("e.dept_id IN "+placeholders(dept,parameters));
                terms.add("EXISTS (SELECT 1 FROM erp_product_dept epd WHERE epd.product_id=e.id AND epd.tenant_id=e.tenant_id AND epd.deleted=0 AND epd.dept_id IN "+placeholders(dept,parameters)+")");
                terms.add("EXISTS (SELECT 1 FROM erp_stock es JOIN erp_warehouse ew ON ew.id=es.warehouse_id AND ew.tenant_id=es.tenant_id AND ew.deleted=0 WHERE es.product_id=e.id AND es.tenant_id=e.tenant_id AND es.deleted=0 AND ew.dept_id IN "+placeholders(dept,parameters)+")");
            }
            if(!warehouse.isEmpty()) terms.add("EXISTS (SELECT 1 FROM erp_stock es WHERE es.product_id=e.id AND es.tenant_id=e.tenant_id AND es.deleted=0 AND es.warehouse_id IN "+placeholders(warehouse,parameters)+")");
            addSelf(terms,parameters,"e.creator",context.get("entitySelfUserId"));
        } else if("CUSTOMER".equals(type) || "SUPPLIER".equals(type)) {
            String party="CUSTOMER".equals(type)?"customer":"supplier";
            if(!dept.isEmpty()) {
                terms.add("e.dept_id IN "+placeholders(dept,parameters));
                terms.add("(e.allow_multi_dept=1 AND EXISTS (SELECT 1 FROM erp_"+party+"_dept ed WHERE ed."+party+"_id=e.id AND ed.tenant_id=e.tenant_id AND ed.deleted=0 AND ed.dept_id IN "+placeholders(dept,parameters)+"))");
            }
            addSelf(terms,parameters,"e.creator",context.get("entitySelfUserId"));
        } else if("WAREHOUSE".equals(type)) {
            if(!warehouse.isEmpty()) terms.add("e.id IN "+placeholders(warehouse,parameters));
        } else if("DEPARTMENT".equals(type)) {
            if(!dept.isEmpty()) terms.add("e.id IN "+placeholders(dept,parameters));
            Object self=context.get("entitySelfDeptId");if(self!=null) add(terms,parameters,"e.id=?",self);
        } else if(userType(type)) {
            if(!dept.isEmpty()) {
                terms.add("e.dept_id IN "+placeholders(dept,parameters));
                if("USER".equals(type)) terms.add("EXISTS (SELECT 1 FROM system_user_dept sud WHERE sud.user_id=e.id AND sud.tenant_id=e.tenant_id AND sud.deleted=0 AND sud.dept_id IN "+placeholders(dept,parameters)+")");
            }
            addSelf(terms,parameters,"e.id",context.get("entitySelfUserId"));
        }
        return " AND ("+(terms.isEmpty()?"1=0":String.join(" OR ",terms))+")";
    }

    private static void addSelf(List<String> terms,List<Object> parameters,String column,Object value) {
        if(value!=null) add(terms,parameters,column+"=?",value);
    }
    private static boolean userType(String type) {return "SALESPERSON".equals(type) || "PURCHASER".equals(type) || "USER".equals(type);}
    private static void add(List<String> terms,List<Object> parameters,String sql,Object value) {terms.add(sql);parameters.add(value);}
    private static String placeholders(Collection<?> values,List<Object> parameters) {
        parameters.addAll(values);return "("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
    }
    private static Collection<?> values(Object value) {return value instanceof Collection?(Collection<?>)value:Collections.emptyList();}
    private static boolean flag(Map<String,Object> context,String key) {return Boolean.TRUE.equals(context.get(key));}
    private static int number(Object value,int fallback) {return value instanceof Number?((Number)value).intValue():fallback;}

    public static final class Query {
        private final String sql;private final List<Object> parameters;private final int maximumRows;
        Query(String sql,List<Object> parameters,int maximumRows) {this.sql=sql;this.parameters=parameters;this.maximumRows=maximumRows;}
        public String getSql(){return sql;}public List<Object> getParameters(){return parameters;}public int getMaximumRows(){return maximumRows;}
    }
}
