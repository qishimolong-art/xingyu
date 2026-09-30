package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Compiles logical datasets into scoped server-owned sources and executes only after AST validation. */
@Service
public class AssistantSemanticQueryService {
    @Resource private AssistantSemanticCatalog catalog;
    @Resource private AssistantSemanticSqlGuard guard;
    @Resource private AssistantSemanticReadOnly reader;
    @Resource private AssistantToolPermissionService permissions;

    public void validateDatasetAccess(String dataset) {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put("tenantId",TenantContextHolder.getRequiredTenantId());
        values.put("userId",String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        source(dataset,values,catalog.requirePublished(dataset));
    }

    public List<Map<String,Object>> schema(Collection<String> names) {
        List<Map<String,Object>> result=new ArrayList<>();int count=0;
        for(String name:names) {
            AssistantSemanticCatalog.Dataset dataset=catalog.requirePublished(name);
            Map<String,String> fields=permissions.visibleFields(dataset.getPermission(),dataset.getModule(),dataset.getColumns());
            Map<String,Object> row=new LinkedHashMap<>();row.put("name",dataset.getName());row.put("title",dataset.getTitle());row.put("columns",fields);result.add(row);
            if(++count>=8) break;
        }
        return result;
    }

    public void validateAccess(String sql) {
        AssistantSemanticSqlGuard.Validated checked=guard.validate(sql);
        if(checked.getDatasets().size()!=1) throw new AssistantFailure("SQL_REJECTED","首版智能查询一次只允许一个语义数据集");
        String dataset=checked.getDatasets().iterator().next();AssistantSemanticCatalog.Dataset definition=catalog.requirePublished(dataset);
        permissions.authorize(definition.getPermission(),definition.getModule(),checked.getColumns());Map<String,Object> values=new LinkedHashMap<>();
        values.put("tenantId",TenantContextHolder.getRequiredTenantId());values.put("userId",String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        source(dataset,values,catalog.requirePublished(dataset));
    }

    public Map<String,Object> execute(String sql,Map<String,Object> modelParameters) {
        return execute(sql,modelParameters,null);
    }

    public Map<String,Object> execute(String sql,Map<String,Object> modelParameters,Map<String,Object> entityConstraint) {
        PreparedSemanticQuery prepared=prepare(sql,modelParameters,entityConstraint,false);
        BoundSql bound=bind(replaceDataset(prepared.checked.getSql(),prepared.dataset,source(prepared.dataset,prepared.values,prepared.definition)),prepared.values);
        List<Map<String,Object>> rows=reader.query(bound.sql,bound.parameters,200);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("queryType","TEXT_TO_SQL");result.put("toolName","execute_semantic_query");
        result.put("title",prepared.definition.getTitle()+"智能查询");result.put("summary",Collections.emptyList());result.put("rows",rows);
        result.put("columns",columns(rows,prepared.definition));result.put("unit",null);result.put("timeRange",null);
        result.put("scope","当前登录用户有权限查看的数据");
        result.put("basis",Collections.singletonList(prepared.definition.getTitle()));result.put("traceId",UUID.randomUUID().toString());
        result.put("queriedAt",LocalDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        result.put("status",rows.isEmpty()?"EMPTY":"SUCCESS");
        Map<String,Object> trace=new LinkedHashMap<>();trace.put("sql",prepared.checked.getSql());trace.put("datasets",prepared.checked.getDatasets());
        trace.put("fingerprint",prepared.checked.getFingerprint());trace.put("validation","PASSED");trace.put("rowCount",rows.size());
        List<String> injected=new ArrayList<>(Arrays.asList("tenant","deleted","approved","business-permission","field-permission","data-permission"));
        if(entityConstraint!=null) injected.add("resolved-entity");trace.put("injectedScopes",injected);
        result.put("queryTrace",trace);return result;
    }

    public Map<String,Object> details(String sql,Map<String,Object> modelParameters,Map<String,Object> entityConstraint,int page,int pageSize) {
        return details(sql,modelParameters,entityConstraint,page,pageSize,false);
    }

    public Map<String,Object> serverOwnedDetails(String sql,Map<String,Object> modelParameters,Map<String,Object> entityConstraint,int page,int pageSize) {
        return details(sql,modelParameters,entityConstraint,page,pageSize,true);
    }

    private Map<String,Object> details(String sql,Map<String,Object> modelParameters,Map<String,Object> entityConstraint,int page,int pageSize,boolean serverOwnedPage) {
        if(page<1 || page>10_000 || pageSize<1 || pageSize>100) throw new AssistantFailure("INVALID_PAGE","分页参数无效");
        PreparedSemanticQuery prepared=prepare(sql,modelParameters,entityConstraint,serverOwnedPage);
        BoundSql bound=bind(replaceDataset(prepared.checked.getSql(),prepared.dataset,source(prepared.dataset,prepared.values,prepared.definition)),prepared.values);
        List<Map<String,Object>> countRows=reader.query("SELECT COUNT(*) total FROM ("+bound.sql+") assistant_detail_count",bound.parameters,1);
        Object total=countRows.isEmpty()?0:countRows.get(0).get("total");
        List<Object> pageParameters=new ArrayList<>(bound.parameters);
        pageParameters.add(pageSize);pageParameters.add((page-1)*pageSize);
        List<Map<String,Object>> rows=reader.query("SELECT * FROM ("+bound.sql+") assistant_detail_page LIMIT ? OFFSET ?",pageParameters,pageSize);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("list",rows);result.put("total",total);result.put("columns",columns(rows,prepared.definition));return result;
    }

    private PreparedSemanticQuery prepare(String sql,Map<String,Object> modelParameters,Map<String,Object> entityConstraint,boolean serverOwnedPage) {
        AssistantSemanticSqlGuard.Validated checked=serverOwnedPage?guard.validateServerOwnedPage(sql):guard.validate(sql);
        if(checked.getDatasets().size()!=1) throw new AssistantFailure("SQL_REJECTED","首版智能查询一次只允许一个语义数据集");
        String dataset=checked.getDatasets().iterator().next();
        AssistantSemanticCatalog.Dataset definition=catalog.requirePublished(dataset);
        permissions.authorize(definition.getPermission(),definition.getModule(),checked.getColumns());
        Map<String,Object> values=new LinkedHashMap<>();
        values.put("tenantId",TenantContextHolder.getRequiredTenantId());
        values.put("userId",String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        Map<String,Object> submitted=modelParameters==null?Collections.emptyMap():modelParameters;
        if(!submitted.keySet().equals(checked.getParameters())) throw new AssistantFailure("MODEL_INVALID","查询参数与SQL占位符不一致");
        for(Map.Entry<String,Object> item:submitted.entrySet()) {
            if(!item.getKey().matches("p[0-9]{1,2}") || !safeValue(item.getValue())) throw new AssistantFailure("MODEL_INVALID","模型查询参数无效");
            values.put(item.getKey(),item.getValue());
        }
        if(entityConstraint!=null) values.put("resolvedEntity",entityConstraint);
        return new PreparedSemanticQuery(checked,dataset,definition,values);
    }

    private String source(String name,Map<String,Object> values,AssistantSemanticCatalog.Dataset dataset) {
        switch(name) {
            case "purchase_orders":
                permissions.authorize(dataset.getPermission(),dataset.getModule(),Collections.emptySet());
                permissions.documentScope(values,"document","erp_purchase_order",false);
                permissions.documentScope(values,"party","erp_supplier",true);
                return "SELECT o.id,o.no,o.status,o.supplier_id,s.name supplier_name,o.dept_id,d.name dept_name,o.order_time,"
                        +"o.total_count,o.total_price,o.in_count,o.return_count,GREATEST(o.total_count-COALESCE(o.in_count,0)+COALESCE(o.return_count,0),0) remaining_count "
                        +"FROM erp_purchase_order o JOIN erp_supplier s ON s.id=o.supplier_id AND s.tenant_id=o.tenant_id AND s.deleted=0 "
                        +"LEFT JOIN system_dept d ON d.id=o.dept_id AND d.tenant_id=o.tenant_id AND d.deleted=0 "
                        +"WHERE o.tenant_id=:tenantId AND o.deleted=0 AND o.status=20"+scope("o","document","purchaser",values)+scope("s","party","creator",values)+entityScope(name,values);
            case "sale_orders":
                permissions.authorize(dataset.getPermission(),dataset.getModule(),Collections.emptySet());
                permissions.documentScope(values,"document","erp_sale_order",false);
                permissions.documentScope(values,"party","erp_customer",true);
                return "SELECT o.id,o.no,o.status,o.customer_id,c.name customer_name,o.sale_user_id salesperson_id,su.nickname salesperson_name,o.dept_id,d.name dept_name,o.order_time,"
                        +"o.total_count,o.total_price,o.out_count,o.return_count,GREATEST(o.total_count-COALESCE(o.out_count,0)+COALESCE(o.return_count,0),0) remaining_count "
                        +"FROM erp_sale_order o JOIN erp_customer c ON c.id=o.customer_id AND c.tenant_id=o.tenant_id AND c.deleted=0 "
                        +"LEFT JOIN system_users su ON su.id=o.sale_user_id AND su.tenant_id=o.tenant_id AND su.deleted=0 "
                        +"LEFT JOIN system_dept d ON d.id=o.dept_id AND d.tenant_id=o.tenant_id AND d.deleted=0 "
                        +"WHERE o.tenant_id=:tenantId AND o.deleted=0 AND o.status=20"+scope("o","document","sale_user_id",values)+scope("c","party","sale_user_id",values)+entityScope(name,values);
            case "sale_order_items":
                permissions.authorize(dataset.getPermission(),dataset.getModule(),Collections.emptySet());
                permissions.documentScope(values,"document","erp_sale_order",false);
                permissions.documentScope(values,"party","erp_customer",true);
                return "SELECT i.id item_id,o.id order_id,o.no order_no,o.status,o.order_time,o.customer_id,c.name customer_name,"
                        +"o.sale_user_id salesperson_id,su.nickname salesperson_name,o.dept_id,d.name dept_name,"
                        +"i.product_id,p.name product_name,i.warehouse_id,w.name warehouse_name,COALESCE(u.name,'未设置单位') unit,"
                        +"i.count order_quantity,i.product_price unit_price,i.total_price line_amount,i.out_count out_quantity,i.return_count return_quantity,"
                        +"GREATEST(i.count-COALESCE(i.out_count,0)+COALESCE(i.return_count,0),0) remaining_quantity,i.gift_flag "
                        +"FROM erp_sale_order_items i JOIN erp_sale_order o ON o.id=i.order_id AND o.tenant_id=i.tenant_id AND o.deleted=0 "
                        +"JOIN erp_customer c ON c.id=o.customer_id AND c.tenant_id=o.tenant_id AND c.deleted=0 "
                        +"JOIN erp_product p ON p.id=i.product_id AND p.tenant_id=i.tenant_id AND p.deleted=0 "
                        +"LEFT JOIN erp_warehouse w ON w.id=i.warehouse_id AND w.tenant_id=i.tenant_id AND w.deleted=0 "
                        +"LEFT JOIN erp_product_unit u ON u.id=i.product_unit_id AND u.tenant_id=i.tenant_id AND u.deleted=0 "
                        +"LEFT JOIN system_users su ON su.id=o.sale_user_id AND su.tenant_id=o.tenant_id AND su.deleted=0 "
                        +"LEFT JOIN system_dept d ON d.id=o.dept_id AND d.tenant_id=o.tenant_id AND d.deleted=0 "
                        +"WHERE i.tenant_id=:tenantId AND i.deleted=0 AND o.status=20"+scope("o","document","sale_user_id",values)+scope("c","party","sale_user_id",values)+entityScope(name,values);
            case "inventory_current":
                permissions.authorize(dataset.getPermission(),dataset.getModule(),Collections.emptySet());
                permissions.stockScope(values);
                return "SELECT p.id product_id,p.name product_name,w.id warehouse_id,w.name warehouse_name,w.dept_id,d.name dept_name,COALESCE(u.name,'未设置单位') unit,COALESCE(st.count,0) quantity "
                        +"FROM erp_stock st JOIN erp_product p ON p.id=st.product_id AND p.tenant_id=st.tenant_id AND p.deleted=0 "
                        +"JOIN erp_warehouse w ON w.id=st.warehouse_id AND w.tenant_id=st.tenant_id AND w.deleted=0 "
                        +"LEFT JOIN erp_product_unit u ON u.id=p.unit_id AND u.tenant_id=st.tenant_id AND u.deleted=0 "
                        +"LEFT JOIN system_dept d ON d.id=w.dept_id AND d.tenant_id=st.tenant_id AND d.deleted=0 "
                        +"WHERE st.tenant_id=:tenantId AND st.deleted=0"+stockScope("st",values)+entityScope(name,values);
            case "stock_movements":
                permissions.authorize(dataset.getPermission(),dataset.getModule(),Collections.emptySet());
                permissions.stockScope(values);
                return "SELECT r.id,p.id product_id,p.name product_name,w.id warehouse_id,w.name warehouse_name,r.dept_id,d.name dept_name,COALESCE(u.name,'未设置单位') unit,"
                        +"r.count quantity,r.total_count balance_quantity,r.biz_type,r.biz_no,r.biz_date "
                        +"FROM erp_stock_record r JOIN erp_product p ON p.id=r.product_id AND p.tenant_id=r.tenant_id AND p.deleted=0 "
                        +"JOIN erp_warehouse w ON w.id=r.warehouse_id AND w.tenant_id=r.tenant_id AND w.deleted=0 "
                        +"LEFT JOIN erp_product_unit u ON u.id=r.product_unit_id AND u.tenant_id=r.tenant_id AND u.deleted=0 "
                        +"LEFT JOIN system_dept d ON d.id=r.dept_id AND d.tenant_id=r.tenant_id AND d.deleted=0 "
                        +"WHERE r.tenant_id=:tenantId AND r.deleted=0"+stockScope("r",values)+entityScope(name,values);
            default: throw new AssistantFailure("DATASET_UNPUBLISHED","该数据集尚未开放执行");
        }
    }
    private static boolean safeValue(Object value) {
        return value==null || value instanceof Number || value instanceof Boolean || value instanceof String && ((String)value).length()<=120;
    }
    private static String scope(String alias,String prefix,String selfColumn,Map<String,Object> values) {
        if(Boolean.TRUE.equals(values.get(prefix+"All"))) return "";
        List<String> terms=new ArrayList<>();Collection<?> ids=(Collection<?>)values.get(prefix+"DeptIds");
        if(ids!=null && !ids.isEmpty()) terms.add(alias+".dept_id IN ("+names(values,prefix+"Dept",ids)+")");
        if(values.get(prefix+"SelfUserId")!=null) {values.put(prefix+"Self",values.get(prefix+"SelfUserId"));terms.add(alias+"."+selfColumn+"=:"+prefix+"Self");}
        return " AND ("+String.join(" OR ",terms.isEmpty()?Collections.singletonList("1=0"):terms)+")";
    }
    private static String stockScope(String alias,Map<String,Object> values) {
        if(Boolean.TRUE.equals(values.get("stockAll"))) return "";
        List<String> terms=new ArrayList<>();Collection<?> dept=(Collection<?>)values.get("stockDeptIds"),self=(Collection<?>)values.get("stockSelfIds");
        if(dept!=null && !dept.isEmpty()) terms.add(alias+".warehouse_id IN ("+names(values,"stockDept",dept)+")");
        if(self!=null && !self.isEmpty()) terms.add("("+alias+".creator=:userId AND "+alias+".warehouse_id IN ("+names(values,"stockSelf",self)+"))");
        return " AND ("+String.join(" OR ",terms.isEmpty()?Collections.singletonList("1=0"):terms)+")";
    }

    @SuppressWarnings("unchecked")
    private static String entityScope(String dataset,Map<String,Object> values) {
        Object raw=values.get("resolvedEntity");if(!(raw instanceof Map)) return "";
        Map<String,Object> constraint=(Map<String,Object>)raw;List<Map<String,Object>> entities=new ArrayList<>();
        Object all=constraint.get("entities");
        if(all instanceof Collection) for(Object item:(Collection<?>)all) if(item instanceof Map) entities.add((Map<String,Object>)item);
        if(entities.isEmpty()) entities.add(constraint);
        List<String> terms=new ArrayList<>();Set<String> columns=new LinkedHashSet<>();int index=0;
        for(Map<String,Object> entity:entities) {
            Object id=entity.get("id");if(!(id instanceof Number)) throw new AssistantFailure("INVALID_CHOICE","对象编号无效");
            String[] target=entityColumn(dataset,String.valueOf(entity.get("entityType")));
            if(target==null) throw new AssistantFailure("DATASET_UNPUBLISHED","当前语义数据集不支持所选业务对象组合");
            if(!columns.add(target[0]+"."+target[1])) continue;
            String parameter="resolvedEntityId"+(index++);values.put(parameter,id);terms.add(target[0]+"."+target[1]+"=:"+parameter);
        }
        return terms.isEmpty()?"":" AND "+String.join(" AND ",terms);
    }

    private static String[] entityColumn(String dataset,String type) {
        String column=null,alias=null;
        if("purchase_orders".equals(dataset)){alias="o";if("SUPPLIER".equals(type))column="supplier_id";else if("DEPARTMENT".equals(type))column="dept_id";}
        else if("sale_orders".equals(dataset)){alias="o";if("CUSTOMER".equals(type))column="customer_id";else if("DEPARTMENT".equals(type))column="dept_id";else if("SALESPERSON".equals(type))column="sale_user_id";}
        else if("sale_order_items".equals(dataset)){alias="i";if("PRODUCT".equals(type))column="product_id";else if("WAREHOUSE".equals(type))column="warehouse_id";else if("CUSTOMER".equals(type)){alias="o";column="customer_id";}else if("DEPARTMENT".equals(type)){alias="o";column="dept_id";}else if("SALESPERSON".equals(type)){alias="o";column="sale_user_id";}}
        else if("inventory_current".equals(dataset)){alias="st";if("PRODUCT".equals(type))column="product_id";else if("WAREHOUSE".equals(type))column="warehouse_id";else if("DEPARTMENT".equals(type)){alias="w";column="dept_id";}}
        else if("stock_movements".equals(dataset)){alias="r";if("PRODUCT".equals(type))column="product_id";else if("WAREHOUSE".equals(type))column="warehouse_id";else if("DEPARTMENT".equals(type))column="dept_id";}
        else return null;
        return column==null?null:new String[]{alias,column};
    }
    private static String names(Map<String,Object> values,String prefix,Collection<?> source) {
        List<String> names=new ArrayList<>();int i=0;for(Object value:source){String name=prefix+(i++);values.put(name,value);names.add(":"+name);}return String.join(",",names);
    }
    private static String replaceDataset(String sql,String dataset,String source) {
        Pattern pattern=Pattern.compile("(?i)\\bFROM\\s+"+Pattern.quote(dataset)+"\\s+([A-Za-z_][A-Za-z0-9_]*)");
        Matcher match=pattern.matcher(sql);if(!match.find()) throw new AssistantFailure("SQL_REJECTED","语义数据集必须使用别名");
        String alias=match.group(1);if(Arrays.asList("WHERE","GROUP","ORDER","LIMIT","HAVING").contains(alias.toUpperCase(Locale.ROOT))) throw new AssistantFailure("SQL_REJECTED","语义数据集必须使用别名");
        return match.replaceFirst(Matcher.quoteReplacement("FROM ("+source+") "+alias));
    }
    private static BoundSql bind(String sql,Map<String,Object> values) {
        Pattern pattern=Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)");Matcher matcher=pattern.matcher(sql);
        StringBuffer compiled=new StringBuffer();List<Object> params=new ArrayList<>();
        while(matcher.find()) {String name=matcher.group(1);if(!values.containsKey(name)) throw new AssistantFailure("MODEL_INVALID","缺少查询参数: "+name);params.add(values.get(name));matcher.appendReplacement(compiled,"?");}
        matcher.appendTail(compiled);return new BoundSql(compiled.toString(),params);
    }
    private static List<Map<String,Object>> columns(List<Map<String,Object>> rows,AssistantSemanticCatalog.Dataset dataset) {
        if(rows.isEmpty()) return Collections.emptyList();List<Map<String,Object>> result=new ArrayList<>();
        Map<String,String> common=new HashMap<>();common.put("document_count","单据数");common.put("record_count","记录数");
        common.put("period","日期");common.put("total_amount","金额");common.put("total_quantity","订货数量");
        common.put("average_amount","平均金额");common.put("maximum_amount","最高金额");common.put("minimum_amount","最低金额");
        common.put("fulfilled_quantity","已完成数量");common.put("return_quantity","退货数量");common.put("remaining_quantity","剩余数量");
        for(String key:rows.get(0).keySet()){Map<String,Object> item=new LinkedHashMap<>();item.put("key",key);
            item.put("title",dataset.getColumns().containsKey(key)?dataset.getColumns().get(key):common.getOrDefault(key,key));result.add(item);}return result;
    }
    private static final class BoundSql {final String sql;final List<Object> parameters;BoundSql(String sql,List<Object> parameters){this.sql=sql;this.parameters=parameters;}}
    private static final class PreparedSemanticQuery {
        final AssistantSemanticSqlGuard.Validated checked;final String dataset;final AssistantSemanticCatalog.Dataset definition;final Map<String,Object> values;
        PreparedSemanticQuery(AssistantSemanticSqlGuard.Validated checked,String dataset,AssistantSemanticCatalog.Dataset definition,Map<String,Object> values){this.checked=checked;this.dataset=dataset;this.definition=definition;this.values=values;}
    }
}
