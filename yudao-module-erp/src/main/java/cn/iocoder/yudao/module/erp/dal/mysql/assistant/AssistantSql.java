package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantPlan;
import cn.iocoder.yudao.module.erp.dal.mysql.report.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.apache.ibatis.annotations.Select;
import java.lang.reflect.Method;
import java.util.*;

/** Only server-owned SQL and enum-selected columns. Values always use MyBatis bindings. */
public class AssistantSql {
    public static String annotation(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.isAnnotationPresent(Select.class))
                return unwrap(String.join(" ", method.getAnnotation(Select.class).value()));
        }
        throw new IllegalStateException("Missing verified report source: " + name);
    }
    private static String unwrap(String sql) { return sql.replace("<script>", "").replace("</script>", ""); }

    public static String source(AssistantPlan plan) {
        switch (plan.getMetric()) {
            case SALE: case PURCHASE:
                boolean sale = plan.getMetric() == AssistantPlan.Metric.SALE;
                boolean product = plan.getGroup() == AssistantPlan.Group.PRODUCT;
                String raw = annotation(sale ? ErpSaleReportMapper.class : ErpPurchaseReportMapper.class,
                        product ? "selectProductRows" : "selectRows");
                String party = sale ? "customer" : "supplier";
                String gross = sale ? "saleAmount" : "purchaseAmount";
                return "SELECT " + (product ? "CAST(q.productId AS CHAR)" : "q.rowKey") + " rowId, "
                    + (product ? "q.productName" : "q.docNo") + " name, '元' unit, q.netAmount amount, q." + gross + " gross, q.returnAmount refund, "
                    + (product ? "0" : "1") + " documents, 0 pending, "
                    + (product ? "NULL partyId, NULL partyName, NULL deptId, NULL deptName, NULL day, q.productId, q.productName" :
                        "q." + party + "Id partyId, q." + party + "Name partyName, q.deptId, q.deptName, DATE(q.docDate) day, NULL productId, NULL productName")
                    + ", NULL warehouseId, NULL warehouseName FROM (" + raw + ") q";
            case RECEIVABLE: case PAYABLE:
                boolean receivable = plan.getMetric() == AssistantPlan.Metric.RECEIVABLE;
                String account = receivable ? unwrap(new ErpReceivableAccountMapper.SqlProvider().selectList())
                        : annotation(ErpPayableAccountMapper.class, "selectList");
                String prefix = receivable ? "customer" : "supplier";
                return "SELECT CONCAT(q." + prefix + "Id,'-',COALESCE(q.deptId,0)) rowId, q." + prefix + "Name name, '元' unit, q."
                    + (receivable ? "receivableBalance" : "balance") + " amount, 0 gross, 0 refund, 0 pending, 1 documents, "
                    + "q." + prefix + "Id partyId, q." + prefix + "Name partyName, q.deptId, q.deptName, NULL day, NULL productId, NULL productName, NULL warehouseId, NULL warehouseName FROM (" + account + ") q";
            case STOCK: case STOCK_SKU: return stock(plan);
            case RECEIPT: case PAYMENT: return cash(plan);
            default: throw new IllegalArgumentException("Unsupported metric");
        }
    }

    static String scope(String alias, String prefix, String selfColumn) {
        return " <if test='!" + prefix + "All'> AND (1=0 "
            + "<if test='" + prefix + "DeptIds != null and " + prefix + "DeptIds.size() > 0'> OR " + alias + ".dept_id IN <foreach collection='" + prefix + "DeptIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach></if>"
            + "<if test='" + prefix + "SelfUserId != null'> OR " + alias + "." + selfColumn + "=#{" + prefix + "SelfUserId}</if>) </if>";
    }
    private static String cash(AssistantPlan plan) {
        boolean receipt = plan.getMetric() == AssistantPlan.Metric.RECEIPT;
        String kind = receipt ? "receipt" : "payment", party = receipt ? "customer" : "supplier";
        return "SELECT CAST(t.id AS CHAR) rowId, t.no name, '元' unit, "
            + "CASE WHEN t.status=20 THEN t." + kind + "_price ELSE 0 END amount, "
            + "CASE WHEN t.status=20 THEN t.total_price ELSE 0 END gross, CASE WHEN t.status=20 THEN t.discount_price ELSE 0 END refund, "
            + "CASE WHEN t.status=20 THEN COALESCE((SELECT SUM(i."+kind+"_price) FROM erp_finance_"+kind+"_item i WHERE i."+kind+"_id=t.id AND i.tenant_id=t.tenant_id AND i.deleted=0 AND i.write_off_status=1),0) ELSE 0 END writeOff, "
            + "CASE WHEN t.status=10 THEN t." + kind + "_price ELSE 0 END pending, CASE WHEN t.status=20 THEN 1 ELSE 0 END documents, "
            + "p.id partyId,p.name partyName,t.dept_id deptId,d.name deptName,DATE(t." + kind + "_time) day, NULL productId,NULL productName,NULL warehouseId,NULL warehouseName "
            + "FROM erp_finance_" + kind + " t JOIN erp_" + party + " p ON p.id=t." + party + "_id AND p.deleted=0 AND p.tenant_id=t.tenant_id "
            + "LEFT JOIN system_dept d ON d.id=t.dept_id AND d.deleted=0 AND d.tenant_id=t.tenant_id "
            + "WHERE t.deleted=0 AND t.tenant_id=#{tenantId} AND t.status IN (10,20) AND t." + kind + "_time &gt;= #{start} AND t." + kind + "_time &lt; #{end} "
            + scope("t", "document", "finance_user_id") + scope("p", "party", receipt ? "sale_user_id" : "creator");
    }
    private static String stock(AssistantPlan plan) {
        String quantity = "COALESCE(erp_stock.count,0)";
        if ("AVAILABLE".equals(plan.getStockMode())) quantity += " - COALESCE(" + ErpStockMapper.occupiedCountExpression().replace("&", "&amp;").replace("<", "&lt;") + ",0)";
        String prices = plan.isIncludeProductPrices() ? priceColumns("p") : "";
        if(plan.isIncludeProductPrices()) return "SELECT COALESCE(CAST(erp_stock.id AS CHAR),CONCAT('product-',p.id)) rowId,p.name name,COALESCE(u.name,'未设置单位') unit," + quantity + " amount,0 gross,0 refund,0 pending,CASE WHEN erp_stock.id IS NULL THEN 0 ELSE 1 END documents, "
            + "NULL partyId,NULL partyName,w.dept_id deptId,d.name deptName,NULL day,p.id productId,p.name productName,w.id warehouseId,w.name warehouseName" + prices + " "
            + "FROM erp_product p LEFT JOIN erp_stock erp_stock ON erp_stock.product_id=p.id AND erp_stock.deleted=0 AND erp_stock.tenant_id=p.tenant_id "
            + "<if test='!stockAll'> AND (1=0 <if test='stockDeptIds.size()>0'> OR erp_stock.warehouse_id IN <foreach collection='stockDeptIds' item='wid' open='(' separator=',' close=')'>#{wid}</foreach></if>"
            + "<if test='stockSelfIds.size()>0'> OR (erp_stock.creator=#{userId} AND erp_stock.warehouse_id IN <foreach collection='stockSelfIds' item='wid' open='(' separator=',' close=')'>#{wid}</foreach>)</if>)</if>"
            + " LEFT JOIN erp_warehouse w ON w.id=erp_stock.warehouse_id AND w.deleted=0 AND w.tenant_id=p.tenant_id "
            + "LEFT JOIN erp_product_unit u ON u.id=p.unit_id AND u.deleted=0 AND u.tenant_id=p.tenant_id "
            + "LEFT JOIN system_dept d ON d.id=w.dept_id AND d.deleted=0 AND d.tenant_id=p.tenant_id "
            + "WHERE p.deleted=0 AND p.tenant_id=#{tenantId} ";
        return "SELECT CAST(erp_stock.id AS CHAR) rowId,p.name name,COALESCE(u.name,'未设置单位') unit," + quantity + " amount,0 gross,0 refund,0 pending,1 documents, "
            + "NULL partyId,NULL partyName,w.dept_id deptId,d.name deptName,NULL day,p.id productId,p.name productName,w.id warehouseId,w.name warehouseName "
            + "FROM erp_stock erp_stock JOIN erp_product p ON p.id=erp_stock.product_id AND p.deleted=0 AND p.tenant_id=erp_stock.tenant_id "
            + "JOIN erp_warehouse w ON w.id=erp_stock.warehouse_id AND w.deleted=0 AND w.tenant_id=erp_stock.tenant_id "
            + "LEFT JOIN erp_product_unit u ON u.id=p.unit_id AND u.deleted=0 AND u.tenant_id=erp_stock.tenant_id "
            + "LEFT JOIN system_dept d ON d.id=w.dept_id AND d.deleted=0 AND d.tenant_id=erp_stock.tenant_id "
            + "WHERE erp_stock.deleted=0 AND erp_stock.tenant_id=#{tenantId} "
            + "<if test='!stockAll'> AND (1=0 <if test='stockDeptIds.size()>0'> OR erp_stock.warehouse_id IN <foreach collection='stockDeptIds' item='wid' open='(' separator=',' close=')'>#{wid}</foreach></if>"
            + "<if test='stockSelfIds.size()>0'> OR (erp_stock.creator=#{userId} AND erp_stock.warehouse_id IN <foreach collection='stockSelfIds' item='wid' open='(' separator=',' close=')'>#{wid}</foreach>)</if>)</if>";
    }

    private static String priceColumns(String alias) {
        return ","+alias+".purchase_price purchasePrice,"+alias+".sale_price salePrice,"+alias+".min_price minPrice,"
            + alias+".reference_price referencePrice,"+alias+".retail_price retailPrice,"+alias+".last_purchase_price lastPurchasePrice,"
            + alias+".wholesale_price wholesalePrice,"+alias+".share_price sharePrice";
    }

    public static String filtered(AssistantPlan plan) {
        return filter(plan,source(plan));
    }
    private static String filter(AssistantPlan plan,String source) {
        return filter(plan,source,true);
    }
    private static String filter(AssistantPlan plan,String source,boolean applyStockMode) {
        String sql = "SELECT v.* FROM (" + source + ") v WHERE 1=1";
        for (String field : Arrays.asList("warehouse", "party", "product", "department")) {
            String col = "department".equals(field) ? "deptName" : field + "Name";
            String idCol="department".equals(field)?"deptId":field+"Id";
            sql += " <choose><when test='plan."+field+"Id != null'> AND v."+idCol+"=#{plan."+field+"Id}</when><otherwise><if test='plan." + field + " != null'> AND v." + col + "=#{plan." + field + "}</if></otherwise></choose>";
        }
        if(applyStockMode && plan.getMetric()==AssistantPlan.Metric.STOCK) {
            if("NEGATIVE".equals(plan.getStockMode())) sql+=" AND v.amount&lt;0";
            if("POSITIVE".equals(plan.getStockMode())) sql+=" AND v.amount&gt;0";
        }
        return sql;
    }

    public static String select(Map<String,Object> context) {
        AssistantPlan plan = (AssistantPlan) context.get("plan");
        plan.validate();
        String mode = (String) context.get("mode");
        boolean aggregateStockRanking=plan.getMetric()==AssistantPlan.Metric.STOCK && "groups".equals(mode)
                && plan.getGroup()!=AssistantPlan.Group.NONE;
        // Ranking modes apply POSITIVE/NEGATIVE after the selected dimension is aggregated. Filtering
        // individual warehouse rows first would change a product's or warehouse's net stock.
        String sql = filter(plan,source(plan),!aggregateStockRanking);
        boolean account=plan.getMetric()==AssistantPlan.Metric.RECEIVABLE || plan.getMetric()==AssistantPlan.Metric.PAYABLE;
        if(account && ("details".equals(mode) || "count".equals(mode))) {
            sql=filter(plan,AssistantAccountSql.source(plan));
            if("count".equals(mode)) return "<script>SELECT COUNT(*) total FROM ("+sql+") v</script>";
            return "<script>SELECT rowId,name,docType,unit,amount,day,deptName,partyName FROM ("+sql+") v ORDER BY day DESC,rowId LIMIT #{pageSize} OFFSET #{offset}</script>";
        }
        if("permissionDepartments".equals(mode)) {
            if(account) return "<script>SELECT DISTINCT deptId FROM ("+AssistantAccountSql.source(plan)+") v LIMIT 201</script>";
            if(plan.getGroup()==AssistantPlan.Group.PRODUCT && (plan.getMetric()==AssistantPlan.Metric.SALE || plan.getMetric()==AssistantPlan.Metric.PURCHASE)) {
                AssistantPlan header=new AssistantPlan();header.setMetric(plan.getMetric());
                return "<script>SELECT DISTINCT deptId FROM ("+source(header)+") v LIMIT 201</script>";
            }
            // Check all departments before name resolution: a fuzzy name can resolve outside an exact filter.
            return "<script>SELECT DISTINCT deptId FROM ("+source(plan)+") v LIMIT 201</script>";
        }
        if ("candidates".equals(mode)) {
            String field = (String) context.get("candidateField");
            if (!Arrays.asList("warehouse", "party", "product", "dept").contains(field)) throw new IllegalArgumentException();
            return "<script>SELECT DISTINCT v." + field + "Id id,v." + field + "Name name FROM (" + source(plan)
                + ") v WHERE v." + field + "Name LIKE CONCAT('%',#{candidateName},'%') ORDER BY id LIMIT 21</script>";
        }
        if(plan.getMetric()==AssistantPlan.Metric.STOCK_SKU && ("details".equals(mode) || "count".equals(mode))) {
            sql="SELECT CAST(productId AS CHAR) rowId,MAX(productName) name,MAX(unit) unit,SUM(amount) amount,0 gross,0 refund,0 pending,SUM(documents) documents,NULL day,NULL deptName,NULL partyName,MAX(productName) productName,NULL warehouseName FROM ("+sql+") s GROUP BY productId";
            if("POSITIVE".equals(plan.getStockMode())) sql+=" HAVING SUM(amount)&gt;0";
            if("NEGATIVE".equals(plan.getStockMode())) sql+=" HAVING SUM(amount)&lt;0";
        }
        if ("details".equals(mode)) return "<script>SELECT rowId,name,unit,amount,gross,refund,pending,documents,day,deptName,partyName,productName,warehouseName FROM ("
            + sql + ") v ORDER BY rowId LIMIT #{pageSize} OFFSET #{offset}</script>";
        if ("count".equals(mode)) return "<script>SELECT COUNT(*) total FROM (" + sql + ") v</script>";
        String dimension = "'合计'", key = "'all'";
        if (!"summary".equals(mode)) {
            switch (plan.getGroup()) {
                case DAY: dimension = "CAST(day AS CHAR)"; key = "day"; break;
                case PARTY: dimension = "partyName"; key = "partyId"; break;
                case PRODUCT: dimension = "productName"; key = "productId"; break;
                case DEPT: dimension = "deptName"; key = "deptId"; break;
                case WAREHOUSE: dimension = "warehouseName"; key = "warehouseId"; break;
                default: break;
            }
        }
        if (plan.getMetric() == AssistantPlan.Metric.STOCK_SKU) {
            // Count products AFTER summing all visible stock rows within each selected grouping.
            sql = "SELECT " + key + " groupId," + dimension + " groupName,productId,SUM(amount) quantity FROM (" + sql + ") s GROUP BY " + key + "," + dimension + ",productId";
            String having = "POSITIVE".equals(plan.getStockMode()) ? " HAVING SUM(amount)&gt;0" : "NEGATIVE".equals(plan.getStockMode()) ? " HAVING SUM(amount)&lt;0" : "";
            sql += having;
            String skuGrouped="SELECT groupId,groupName label,'种' unit,COUNT(*) amount,COUNT(*) documents FROM (" + sql + ") v GROUP BY groupId,groupName";
            if(!"summary".equals(mode)) {
                String order=plan.getRankOrder()==AssistantPlan.RankOrder.ASC?"ASC":"DESC";
                return "<script>SELECT * FROM (SELECT g.*,DENSE_RANK() OVER(ORDER BY amount "+order+") unitRank FROM ("+skuGrouped+") g) ranked WHERE unitRank &lt;= #{rowLimit} ORDER BY unitRank,groupId LIMIT 201</script>";
            }
            return "<script>"+skuGrouped+" ORDER BY amount DESC,groupId LIMIT #{rowLimit}</script>";
        }
        String allocated=(plan.getMetric()==AssistantPlan.Metric.RECEIPT || plan.getMetric()==AssistantPlan.Metric.PAYMENT)?",SUM(writeOff) writeOff":"";
        String priceAggregates=plan.isIncludeProductPrices()?priceAggregates():"";
        String grouped="SELECT " + key + " groupId," + dimension + " label,unit,SUM(amount) amount,SUM(gross) gross,SUM(refund) refund,SUM(pending) pending,SUM(documents) documents "
            + allocated + priceAggregates + " FROM (" + sql + ") v GROUP BY " + key + "," + dimension + ",unit";
        if(plan.getMetric()==AssistantPlan.Metric.STOCK && !"summary".equals(mode)) {
            boolean productRanking=plan.getGroup()==AssistantPlan.Group.PRODUCT;
            String stockUnit=productRanking?"MAX(unit)":"'数量'";
            String stockLabel="MAX("+dimension+")";
            grouped="SELECT " + key + " groupId," + stockLabel + " label," + stockUnit + " unit,"
                    + "SUM(amount) amount,SUM(gross) gross,SUM(refund) refund,SUM(pending) pending,SUM(documents) documents "
                    + priceAggregates + " FROM (" + sql + ") v GROUP BY " + key;
            if("POSITIVE".equals(plan.getStockMode())) grouped+=" HAVING SUM(amount)&gt;0";
            if("NEGATIVE".equals(plan.getStockMode())) grouped+=" HAVING SUM(amount)&lt;0";
            String order=plan.getRankOrder()==AssistantPlan.RankOrder.ASC?"ASC":"DESC";
            return "<script>SELECT * FROM (SELECT g.*,DENSE_RANK() OVER(ORDER BY amount "+order+") unitRank FROM ("+grouped+") g) ranked WHERE unitRank &lt;= #{rowLimit} ORDER BY unitRank,groupId LIMIT 201</script>";
        }
        return "<script>"+grouped+" ORDER BY "
            + (plan.getGroup() == AssistantPlan.Group.DAY && !"summary".equals(mode) ? "groupId ASC" : "amount DESC,groupId") + " LIMIT #{rowLimit}</script>";
    }

    private static String priceAggregates() {
        return ",MAX(purchasePrice) purchasePrice,MAX(salePrice) salePrice,MAX(minPrice) minPrice,MAX(referencePrice) referencePrice,"
            + "MAX(retailPrice) retailPrice,MAX(lastPurchasePrice) lastPurchasePrice,MAX(wholesalePrice) wholesalePrice,MAX(sharePrice) sharePrice";
    }
}
