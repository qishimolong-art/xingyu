package cn.iocoder.yudao.module.erp.service.report.trade;

import cn.iocoder.yudao.module.erp.controller.admin.report.trade.ErpTradeReportModels.Filter;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;

/** 仅在明确租户与交集权限内读取真实posting；不从当前源金额生成历史交易额。 */
@Repository
public class ErpTradeReportRepository {
    @Resource private JdbcTemplate jdbcTemplate;
    public void checkSchema() {
        jdbcTemplate.queryForList("SELECT posting_id,signature,gross_status,net_amount,sale_user_name,accounting_dept_name,stock_dept_name,warehouse_name,snapshot_json FROM erp_business_report_item_snapshot WHERE 1=0");
        jdbcTemplate.queryForList("SELECT id,posted_at,accounting_dept_id,financial_movement FROM erp_stock_dual_cost_posting WHERE 1=0");
    }
    public static final Map<String,Integer> TYPES;
    public static final Map<String,String> GROUPS;
    static {
        Map<String,Integer> types=new LinkedHashMap<>();types.put("SALE_OUT",50);types.put("SALE_RETURN",60);types.put("PURCHASE_IN",70);types.put("PURCHASE_RETURN",80);TYPES=Collections.unmodifiableMap(types);
        Map<String,String> g=new LinkedHashMap<>();g.put("CUSTOMER","s.party_id");g.put("SUPPLIER","s.party_id");g.put("PRODUCT","p.product_id");g.put("OE","s.oe_number");g.put("BRAND","s.brand");g.put("CATEGORY","s.category_id");g.put("ACCOUNTING_DEPT","p.accounting_dept_id");g.put("STOCK_DEPT","p.stock_dept_id");g.put("WAREHOUSE","p.warehouse_id");g.put("SALE_USER","s.sale_user_id");g.put("PURCHASER","s.purchaser");GROUPS=Collections.unmodifiableMap(g);
    }
    public static final class Query {
        final String from,where; final List<Object> args; final boolean sale;
        Boolean currentCostLinksAvailable;
        Query(String from,String where,List<Object> args,boolean sale){this.from=from;this.where=where;this.args=args;this.sale=sale;}
    }
    public Query prepare(boolean sale,long tenant,Filter f,ErpFinanceVisibleScope document,ErpFinanceVisibleScope party) {
        String header=sale?"erp_sale_out":"erp_purchase_in", returns=sale?"erp_sale_return":"erp_purchase_return";
        String partyTable=sale?"erp_customer":"erp_supplier", partyField=sale?"customer_id":"supplier_id";
        int positive=sale?50:70,negative=sale?60:80;
        String from=" FROM erp_stock_dual_cost_posting p LEFT JOIN erp_business_report_item_snapshot s ON s.tenant_id=p.tenant_id AND s.posting_id=p.id "
                +"LEFT JOIN "+header+" h ON h.tenant_id=p.tenant_id AND h.id=p.biz_id AND p.biz_type="+positive+" "
                +"LEFT JOIN "+returns+" r ON r.tenant_id=p.tenant_id AND r.id=p.biz_id AND p.biz_type="+negative+" "
                +"LEFT JOIN "+partyTable+" c ON c.tenant_id=p.tenant_id AND c.id=COALESCE(s.party_id,h."+partyField+",r."+partyField+") ";
        List<Object> args=new ArrayList<>();args.add(tenant);args.add(Timestamp.valueOf(f.getPostedFrom()));args.add(Timestamp.valueOf(f.getPostedTo()));
        StringBuilder where=new StringBuilder(" WHERE p.tenant_id=? AND p.posted_at>=? AND p.posted_at<? AND p.biz_type IN ("+positive+","+negative+")");
        scope(where,args,document,"p.accounting_dept_id","COALESCE(s.source_creator COLLATE utf8mb4_unicode_ci,h.creator COLLATE utf8mb4_unicode_ci,r.creator COLLATE utf8mb4_unicode_ci)",null);
        scope(where,args,party,"c.dept_id","c.creator",sale?"erp_customer_dept":"erp_supplier_dept");
        eq(where,args,"p.product_id",f.getProductId());eq(where,args,"s.party_id",sale?f.getCustomerId():f.getSupplierId());
        eq(where,args,"s.sale_user_id",f.getSaleUserId());eq(where,args,"s.category_id",f.getCategoryId());
        eq(where,args,"s.oe_number",f.getOeNumber());eq(where,args,"s.brand",f.getBrand());eq(where,args,"s.purchaser",f.getPurchaser());
        eq(where,args,"s.no_original_sale",f.getNoOriginalSale());
        if(f.getBizNo()!=null&&!f.getBizNo().isEmpty()){where.append(" AND p.biz_no LIKE ?");args.add("%"+f.getBizNo()+"%");}
        in(where,args,"p.accounting_dept_id",f.getAccountingDeptIds());in(where,args,"p.stock_dept_id",f.getStockDeptIds());in(where,args,"p.warehouse_id",f.getWarehouseIds());
        if(f.getBusinessTypes()!=null&&!f.getBusinessTypes().isEmpty()){List<Integer> values=new ArrayList<>();for(String type:f.getBusinessTypes())values.add(TYPES.get(type));in(where,args,"p.biz_type",values);}
        return new Query(from,where.toString(),args,sale);
    }
    private static void scope(StringBuilder w,List<Object> a,ErpFinanceVisibleScope s,String dept,String creator,String relation) {
        if(s==null||!s.hasAccess()){w.append(" AND 1=0");return;}if(s.isAll())return;
        w.append(" AND (1=0");
        if(!s.getDeptIds().isEmpty()){
            w.append(" OR ").append(dept).append(" IN (").append(marks(s.getDeptIds().size())).append(')');a.addAll(s.getDeptIds());
            if(relation!=null){String key=relation.contains("customer")?"customer_id":"supplier_id";
                w.append(" OR (c.allow_multi_dept=1 AND EXISTS(SELECT 1 FROM ").append(relation)
                        .append(" rel WHERE rel.tenant_id=c.tenant_id AND rel.").append(key).append("=c.id AND rel.deleted=0 AND rel.dept_id IN (")
                        .append(marks(s.getDeptIds().size())).append(")))");a.addAll(s.getDeptIds());}
        }
        if(s.getSelfUserId()!=null){w.append(" OR ").append(creator).append("=?");a.add(s.getSelfUserId().toString());}w.append(')');
    }
    public long count(Query q,String group) {
        String sql=group==null?"SELECT COUNT(*)"+q.from+q.where:"SELECT COUNT(*) FROM (SELECT "+GROUPS.get(group)+q.from+q.where+" GROUP BY "+GROUPS.get(group)+") groups_count";
        return jdbcTemplate.queryForObject(sql,Long.class,q.args.toArray());
    }
    public List<Map<String,Object>> rows(Query q,Filter f,String group,long offset,int limit) {
        List<Object> args=new ArrayList<>(q.args);args.add(limit);args.add(offset);
        if(group!=null){String key=GROUPS.get(group);return jdbcTemplate.queryForList("SELECT "+key+" AS group_key,MAX("+label(group)+") AS group_label,"+aggregates(q.sale)+q.from+q.where+" GROUP BY "+key+" ORDER BY "+key+" ASC LIMIT ? OFFSET ?",args.toArray());}
        String order="postingId".equals(f.getOrderField())?"p.id":"p.posted_at";
        String direction="asc".equals(f.getOrderDirection())?" ASC":" DESC";
        List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT p.id AS posting_id,p.biz_type,p.biz_id,p.biz_item_id,p.biz_no,p.biz_date,p.posted_at,p.product_id,p.warehouse_id,p.stock_dept_id,p.accounting_dept_id,"
                +"p.quantity,p.financial_movement,p.settlement_movement,p.source_biz_type,p.source_biz_id,p.source_biz_item_id,p.reversal_posting_id,"
                +"s.posting_id AS snapshot_id,s.party_id,s.party_name,s.sale_user_id,s.sale_user_name,s.accounting_dept_name,s.stock_dept_name,s.warehouse_name,s.purchaser,s.product_code,s.product_name,s.oe_number,s.brand,s.category_id,s.unit_name,s.specification,"
                +"s.raw_gross_amount,s.gross_amount,s.net_amount,s.gross_status,s.original_biz_id,s.original_biz_item_id,s.original_biz_no,s.no_original_sale,s.snapshot_json"
                +q.from+q.where+" ORDER BY "+order+direction+",p.id"+direction+" LIMIT ? OFFSET ?",args.toArray());
        attachCurrentReturnCostSources(q,rows);
        return rows;
    }
    /** 仅补已经通过报表权限过滤的实际posting依据；不读取当前库存或敏感stock_basis。 */
    private void attachCurrentReturnCostSources(Query q,List<Map<String,Object>> rows) {
        if(!q.sale)return;
        Map<Long,Map<String,Object>> selected=new HashMap<>();
        for(Map<String,Object> row:rows)if(((Number)row.get("biz_type")).intValue()==60
                && (Boolean.TRUE.equals(row.get("no_original_sale")) || "1".equals(String.valueOf(row.get("no_original_sale")))))
            selected.put(((Number)row.get("posting_id")).longValue(),row);
        if(selected.isEmpty())return;
        // 旧Trade迁移可没有该可选表；只对明确不存在的表降级，其他SQL/连接错误照常抛出。
        if(q.currentCostLinksAvailable==null)q.currentCostLinksAvailable=jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_return_current_cost_posting_link'",Long.class)>0;
        if(!q.currentCostLinksAvailable)return;
        List<Object> args=new ArrayList<>();args.add(q.args.get(0));args.addAll(selected.keySet());
        for(Map<String,Object> link:jdbcTemplate.queryForList("SELECT posting_id,return_id,return_item_id,cost_source FROM erp_sale_return_current_cost_posting_link WHERE tenant_id=? AND posting_id IN ("+marks(selected.size())+")",args.toArray())) {
            Map<String,Object> row=selected.get(((Number)link.get("posting_id")).longValue());
            if(row!=null&&Objects.equals(String.valueOf(row.get("biz_id")),String.valueOf(link.get("return_id")))
                    &&Objects.equals(String.valueOf(row.get("biz_item_id")),String.valueOf(link.get("return_item_id")))
                    &&Arrays.asList("CURRENT_AVERAGE","MANUAL_CONFIRMED").contains(link.get("cost_source")))row.put("current_cost_source",link.get("cost_source"));
        }
    }
    public Map<String,Object> summary(Query q){return jdbcTemplate.queryForMap("SELECT "+aggregates(q.sale)+q.from+q.where,q.args.toArray());}
    private static String aggregates(boolean sale) {
        int pos=sale?50:70, neg=sale?60:80;String sign=sale?"-":"";
        return "COUNT(*) AS row_count,COUNT(DISTINCT CONCAT(p.biz_type,':',p.biz_id)) AS document_count,"
                +"COALESCE(SUM(CASE WHEN p.biz_type="+pos+" THEN ABS(p.quantity) ELSE 0 END),0) AS positive_quantity,"
                +"COALESCE(SUM(CASE WHEN p.biz_type="+neg+" THEN ABS(p.quantity) ELSE 0 END),0) AS return_quantity,"
                +"COALESCE(SUM("+sign+"p.quantity),0) AS net_quantity,"
                +"COALESCE(SUM(CASE WHEN s.posting_id IS NULL THEN 1 ELSE 0 END),0) AS missing_snapshot_count,"
                +"COALESCE(SUM(CASE WHEN s.gross_amount IS NULL THEN 1 ELSE 0 END),0) AS missing_gross_count,"
                +"CASE WHEN COUNT(*)=COUNT(s.raw_gross_amount) THEN COALESCE(SUM(CASE WHEN p.biz_type="+pos+" THEN s.raw_gross_amount ELSE -s.raw_gross_amount END),0) END AS raw_gross_amount,"
                +"CASE WHEN COUNT(*)=COUNT(s.gross_amount) THEN COALESCE(SUM(CASE WHEN p.biz_type="+pos+" THEN s.gross_amount ELSE -s.gross_amount END),0) END AS gross_amount,"
                +"CASE WHEN COALESCE(SUM(CASE WHEN p.biz_type="+pos+" AND s.gross_amount IS NULL THEN 1 ELSE 0 END),0)=0 THEN COALESCE(SUM(CASE WHEN p.biz_type="+pos+" THEN s.gross_amount ELSE 0 END),0) END AS positive_gross_amount,"
                +"CASE WHEN COALESCE(SUM(CASE WHEN p.biz_type="+neg+" AND s.gross_amount IS NULL THEN 1 ELSE 0 END),0)=0 THEN COALESCE(SUM(CASE WHEN p.biz_type="+neg+" THEN s.gross_amount ELSE 0 END),0) END AS return_gross_amount,"
                +"CASE WHEN COUNT(*)=COUNT(s.net_amount) THEN COALESCE(SUM(CASE WHEN p.biz_type="+pos+" THEN s.net_amount ELSE -s.net_amount END),0) END AS net_amount,"
                +"COALESCE(SUM("+sign+"p.financial_movement),0) AS financial_cost,COALESCE(SUM("+sign+"p.settlement_movement),0) AS settlement_cost,"
                +"COALESCE(SUM(CASE WHEN p.biz_type="+pos+" THEN ABS(p.financial_movement) ELSE 0 END),0) AS positive_financial_cost,"
                +"COALESCE(SUM(CASE WHEN p.biz_type="+neg+" THEN ABS(p.financial_movement) ELSE 0 END),0) AS return_financial_cost,"
                +"COALESCE(SUM(CASE WHEN p.biz_type="+pos+" THEN ABS(p.settlement_movement) ELSE 0 END),0) AS positive_settlement_cost,"
                +"COALESCE(SUM(CASE WHEN p.biz_type="+neg+" THEN ABS(p.settlement_movement) ELSE 0 END),0) AS return_settlement_cost";
    }
    public List<Long> departments(Query q){List<Object>a=new ArrayList<>(q.args);a.addAll(q.args);return jdbcTemplate.queryForList("SELECT DISTINCT p.accounting_dept_id"+q.from+q.where+" UNION SELECT DISTINCT p.stock_dept_id"+q.from+q.where,Long.class,a.toArray());}
    public List<Map<String,Object>> options(Query q,String group,String keyword,int limit,int offset){String col=GROUPS.get(group);List<Object>a=new ArrayList<>(q.args);String extra=optionSearch(group,keyword,a);a.add(limit);a.add(offset);
        return jdbcTemplate.queryForList("SELECT "+col+" AS value,MAX("+label(group)+") AS label,COUNT(*) AS row_count"+q.from+q.where+extra+" GROUP BY "+col+" ORDER BY "+col+" LIMIT ? OFFSET ?",a.toArray());}
    public long optionCount(Query q,String group,String keyword){List<Object>a=new ArrayList<>(q.args);String extra=optionSearch(group,keyword,a);return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (SELECT "+GROUPS.get(group)+q.from+q.where+extra+" GROUP BY "+GROUPS.get(group)+") counted_options",Long.class,a.toArray());}
    private static String optionSearch(String group,String keyword,List<Object> args){if(keyword==null||keyword.isEmpty())return "";args.add("%"+keyword+"%");args.add("%"+keyword+"%");return " AND (CAST("+GROUPS.get(group)+" AS CHAR) LIKE ? OR "+label(group)+" LIKE ?)";}
    private static String label(String group){if("PRODUCT".equals(group))return "s.product_name";if("CUSTOMER".equals(group)||"SUPPLIER".equals(group))return "s.party_name";if("ACCOUNTING_DEPT".equals(group))return "s.accounting_dept_name";if("STOCK_DEPT".equals(group))return "s.stock_dept_name";if("WAREHOUSE".equals(group))return "s.warehouse_name";if("SALE_USER".equals(group))return "s.sale_user_name";return "CAST("+GROUPS.get(group)+" AS CHAR)";}
    private static void eq(StringBuilder w,List<Object>a,String col,Object v){if(v!=null){w.append(" AND ").append(col).append("=?");a.add(v);}}
    private static void in(StringBuilder w,List<Object>a,String col,Collection<?> v){if(v!=null&&!v.isEmpty()){w.append(" AND ").append(col).append(" IN (").append(marks(v.size())).append(')');a.addAll(v);}}
    private static String marks(int size){return String.join(",",Collections.nCopies(size,"?"));}
}
