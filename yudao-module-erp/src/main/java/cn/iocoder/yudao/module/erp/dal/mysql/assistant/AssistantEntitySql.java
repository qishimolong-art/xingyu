package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;

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
                if("FACTORY_CODE".equals(match) && flag(context,"searchFactoryCode"))
                    description="CASE WHEN e.factory_code IS NOT NULL AND e.factory_code<>'' THEN CONCAT('厂家编码：',e.factory_code) ELSE NULL END";
                else if("BAR_CODE".equals(match) && flag(context,"searchBarCode"))
                    description="CASE WHEN e.bar_code IS NOT NULL AND e.bar_code<>'' THEN CONCAT('条码：',e.bar_code) ELSE NULL END";
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
            default: throw new IllegalArgumentException("Unsupported entity type");
        }
        List<Object> parameters=new ArrayList<>();parameters.add(context.get("tenantId"));
        String scope=scope(context,type,parameters);
        String predicate=predicate(context,type,match,parameters);
        int limit=Math.max(1,Math.min(21,number(context.get("rowLimit"),21)));
        String matchedBy="EXACT_NAME".equals(match)?"NAME":"FUZZY".equals(match)?"FUZZY_NAME":match;
        String sql="SELECT e.id,"+name+" name,"+code+" code,"+description+" description,'"+matchedBy+"' matchedBy FROM "+table
                +" WHERE e.tenant_id=? AND e.deleted=0 AND e.status=0"+base+scope+" AND ("+predicate+") ORDER BY e.id LIMIT "+limit;
        return new Query(sql,parameters,limit);
    }

    private static String predicate(Map<String,Object> context,String type,String match,List<Object> parameters) {
        if("ID".equals(match)) {parameters.add(context.get("entityId"));return "e.id=?";}
        List<String> terms=new ArrayList<>();Object keyword=context.get("entityKeyword");
        if("CODE".equals(match) && flag(context,"searchCode")) {
            add(terms,parameters,"UPPER(COALESCE(e."+("WAREHOUSE".equals(type)?"warehouse_code":"code")+",''))=UPPER(?)",keyword);
        } else if("FACTORY_CODE".equals(match) && "PRODUCT".equals(type) && flag(context,"searchFactoryCode")) {
            add(terms,parameters,"UPPER(COALESCE(e.factory_code,''))=UPPER(?)",keyword);
        } else if("BAR_CODE".equals(match) && "PRODUCT".equals(type) && flag(context,"searchBarCode")) {
            add(terms,parameters,"UPPER(COALESCE(e.bar_code,''))=UPPER(?)",keyword);
        } else if("OLD_CODE".equals(match) && "SUPPLIER".equals(type) && flag(context,"searchOldCode")) {
            add(terms,parameters,"UPPER(COALESCE(e.old_code,''))=UPPER(?)",keyword);
        } else if("EXACT_NAME".equals(match)) {
            add(terms,parameters,"SALESPERSON".equals(type)?"e.nickname=?":"e.name=?",keyword);
            if(flag(context,"searchShortName")) add(terms,parameters,"e.short_name=?",keyword);
        } else if("TOKEN_FUZZY".equals(match) && "PRODUCT".equals(type)) {
            return productTokenPredicate(context,parameters);
        } else if("FUZZY".equals(match)) {
            add(terms,parameters,"SALESPERSON".equals(type)?"e.nickname LIKE CONCAT('%',?,'%')":"e.name LIKE CONCAT('%',?,'%')",keyword);
            if(flag(context,"searchShortName")) add(terms,parameters,"e.short_name LIKE CONCAT('%',?,'%')",keyword);
            if(flag(context,"searchPinyin")) add(terms,parameters,"e.pinyin_code LIKE CONCAT(?,'%')",keyword);
            if(flag(context,"searchWubi")) add(terms,parameters,"e.wubi_code LIKE CONCAT(?,'%')",keyword);
        }
        return terms.isEmpty()?"1=0":String.join(" OR ",terms);
    }

    /** Mirrors the product page's ordered-wildcard OR token-AND search using only visible fields. */
    private static String productTokenPredicate(Map<String,Object> context,List<Object> parameters) {
        String keyword=String.valueOf(context.get("entityKeyword"));
        ErpKeywordQuery.KeywordSearch parsed=ErpKeywordQuery.parseKeywordSearch(keyword);
        if(!ErpKeywordQuery.shouldAppendTokenProductItemCondition(parsed)) return "1=0";
        List<String> columns=productKeywordColumns(context);
        if(columns.isEmpty()) return "1=0";
        List<String> alternatives=new ArrayList<>();
        if(parsed.spaceDelimited()) {
            String ordered="%"+keyword.trim().replaceAll("\\s+","%")+"%";
            alternatives.add(likeAny(columns,ordered,parameters));
        }
        List<String> tokenGroups=new ArrayList<>();
        for(String token:parsed.tokens()) tokenGroups.add("("+likeAny(columns,"%"+token+"%",parameters)+")");
        if(!tokenGroups.isEmpty()) alternatives.add("("+String.join(" AND ",tokenGroups)+")");
        return alternatives.isEmpty()?"1=0":"("+String.join(" OR ",alternatives)+")";
    }

    private static List<String> productKeywordColumns(Map<String,Object> context) {
        List<String> columns=new ArrayList<>();columns.add("e.name");
        addColumn(columns,context,"searchCode","e.code");
        addColumn(columns,context,"searchPinyin","e.pinyin_code");
        addColumn(columns,context,"searchWubi","e.wubi_code");
        addColumn(columns,context,"searchBarCode","e.bar_code");
        addColumn(columns,context,"searchVehicleModel","e.vehicle_model");
        addColumn(columns,context,"searchFactoryCode","e.factory_code");
        addColumn(columns,context,"searchStandard","e.standard");
        addColumn(columns,context,"searchRemark","e.remark");
        addColumn(columns,context,"searchBrand","e.brand");
        addColumn(columns,context,"searchOeNumber","e.oe_number");
        addColumn(columns,context,"searchOriginPlace","e.origin_place");
        addColumn(columns,context,"searchFeatureCode","e.feature_code");
        addColumn(columns,context,"searchDrawingNo","e.drawing_no");
        addColumn(columns,context,"searchShelf","e.shelf");
        return columns;
    }

    private static void addColumn(List<String> columns,Map<String,Object> context,String flag,String column) {
        if(flag(context,flag)) columns.add(column);
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
        } else if("SALESPERSON".equals(type)) {
            if(!dept.isEmpty()) terms.add("e.dept_id IN "+placeholders(dept,parameters));
            addSelf(terms,parameters,"e.id",context.get("entitySelfUserId"));
        }
        return " AND ("+(terms.isEmpty()?"1=0":String.join(" OR ",terms))+")";
    }

    private static void addSelf(List<String> terms,List<Object> parameters,String column,Object value) {
        if(value!=null) add(terms,parameters,column+"=?",value);
    }
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
