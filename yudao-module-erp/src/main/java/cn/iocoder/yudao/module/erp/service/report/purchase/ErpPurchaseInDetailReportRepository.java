package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.PriceEvent;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ErpPurchaseInDetailReportRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        Long value = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) " + sql.body, sql.params, Long.class);
        return value == null ? 0L : value;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = reportSql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(sql.cte + " SELECT report.* " + sql.body + orderBy(filter)
                        + " LIMIT :limit OFFSET :offset", sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        return jdbc().queryForObject(sql.cte
                        + " SELECT COUNT(*) AS row_count,"
                        + "COALESCE(SUM(report.count),0) AS total_count,"
                        + "COALESCE(SUM(report.total_amount),0) AS total_amount,"
                        + "COALESCE(SUM(report.invoice_amount),0) AS invoice_amount,"
                        + "COALESCE(SUM(report.freight1),0) AS freight_amount " + sql.body,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    public List<PriceEvent> priceEvents(Collection<Long> productIds, Scope scope) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        Map<String, Object> params = new HashMap<>();
        params.put("tenant", scope.tenantId);
        params.put("productIds", productIds);
        String inScope = documentScope(scope.inScope, "pi.dept_id", "pi.creator", "eventIn", params);
        String adjustScope = documentScope(scope.adjustScope, "pa.dept_id", "pa.creator", "eventAdjust", params);
        String supplierInScope = supplierScope(scope.supplierScope, "s", "eventInSupplier", params);
        String supplierAdjustScope = supplierScope(scope.supplierScope, "s", "eventAdjustSupplier", params);
        String sql = "SELECT event.product_id,event.event_time,event.document_type_code,event.item_id,event.price FROM ("
                + "SELECT pii.product_id,pi.approve_time AS event_time,'PURCHASE_IN' AS document_type_code,"
                + "pii.id AS item_id,COALESCE(pii.original_product_price,pii.product_price) AS price "
                + "FROM erp_purchase_in_items pii INNER JOIN erp_purchase_in pi ON pi.id=pii.in_id "
                + "AND pi.tenant_id=pii.tenant_id AND pi.deleted=b'0' AND pi.status=20 "
                + "INNER JOIN erp_supplier s ON s.id=pi.supplier_id AND s.tenant_id=pi.tenant_id AND s.deleted=b'0' "
                + "WHERE pii.tenant_id=:tenant AND pii.deleted=b'0' AND pii.product_id IN (:productIds) "
                + "AND pi.approve_time IS NOT NULL" + inScope + supplierInScope
                + " UNION ALL "
                + "SELECT pai.product_id,pa.approve_time,'PURCHASE_ADJUST',pai.id,pai.new_price "
                + "FROM erp_purchase_price_adjust_item pai INNER JOIN erp_purchase_price_adjust pa "
                + "ON pa.id=pai.adjust_id AND pa.tenant_id=pai.tenant_id AND pa.deleted=b'0' AND pa.status=20 "
                + "INNER JOIN erp_supplier s ON s.id=pa.supplier_id AND s.tenant_id=pa.tenant_id AND s.deleted=b'0' "
                + "WHERE pai.tenant_id=:tenant AND pai.deleted=b'0' AND pai.product_id IN (:productIds) "
                + "AND pa.approve_time IS NOT NULL" + adjustScope + supplierAdjustScope
                + ") event ORDER BY event.product_id,event.event_time,event.document_type_code,event.item_id";
        return jdbc().query(sql, params, new BeanPropertyRowMapper<>(PriceEvent.class));
    }

    private Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String inScope = documentScope(scope.inScope, "pi.dept_id", "pi.creator", "inDoc", sql.params);
        String returnScope = documentScope(scope.returnScope, "pr.dept_id", "pr.creator", "returnDoc", sql.params);
        String adjustScope = documentScope(scope.adjustScope, "pa.dept_id", "pa.creator", "adjustDoc", sql.params);
        String inSupplierScope = supplierScope(scope.supplierScope, "s", "inSupplier", sql.params);
        String returnSupplierScope = supplierScope(scope.supplierScope, "s", "returnSupplier", sql.params);
        String adjustSupplierScope = supplierScope(scope.supplierScope, "s", "adjustSupplier", sql.params);

        sql.cte = "WITH in_weight AS ("
                + "SELECT pii.*,pi.total_freight1,pi.freight_type1,"
                + "SUM(GREATEST(COALESCE(pii.count,0),0)) OVER(PARTITION BY pii.in_id) AS freight_total_count,"
                + "ROW_NUMBER() OVER(PARTITION BY pii.in_id ORDER BY pii.id DESC) AS freight_tail_rank "
                + "FROM erp_purchase_in_items pii INNER JOIN erp_purchase_in pi ON pi.id=pii.in_id "
                + "AND pi.tenant_id=pii.tenant_id AND pi.deleted=b'0' "
                + "WHERE pii.tenant_id=:tenant AND pii.deleted=b'0'),"
                + "in_alloc AS (SELECT iw.*,CASE WHEN iw.total_freight1>0 AND iw.freight_total_count>0 "
                + "THEN ROUND(iw.total_freight1*GREATEST(COALESCE(iw.count,0),0)/iw.freight_total_count,2) END AS provisional_freight "
                + "FROM in_weight iw),"
                + "in_final AS (SELECT ia.*,CASE WHEN ia.provisional_freight IS NULL THEN NULL "
                + "WHEN ia.freight_tail_rank=1 THEN ia.total_freight1-(SUM(ia.provisional_freight) OVER(PARTITION BY ia.in_id)-ia.provisional_freight) "
                + "ELSE ia.provisional_freight END AS allocated_freight FROM in_alloc ia),"
                + "invoice_agg AS (SELECT pia.source_type,pia.source_item_id,"
                + "SUM(COALESCE(pia.allocated_count,0)) AS invoice_count,"
                + "SUM(COALESCE(pia.allocated_amount,0)) AS invoice_amount "
                + "FROM erp_purchase_invoice_source_allocation pia "
                + "WHERE pia.tenant_id=:tenant AND pia.deleted=b'0' "
                + "GROUP BY pia.source_type,pia.source_item_id),"
                + "report_union AS (" + inboundSelect(inScope, inSupplierScope)
                + " UNION ALL " + returnSelect(returnScope, returnSupplierScope)
                + " UNION ALL " + adjustSelect(adjustScope, adjustSupplierScope) + ") ";
        sql.body = "FROM report_union report WHERE 1=1";
        appendFilters(sql, filter);
        return sql;
    }

    private String inboundSelect(String documentScope, String supplierScope) {
        return "SELECT CONCAT('IN-',ifi.id) AS row_key,ifi.id AS item_id,pi.id AS business_id,"
                + "ifi.product_id,ifi.warehouse_id,'PURCHASE_IN' AS document_type_code,"
                + "pi.order_id AS source_business_id,'PURCHASE_ORDER' AS source_document_type_code,'采购入库' AS document_type,"
                + "pi.approve_time AS approve_date,pi.no AS document_no,pi.order_no AS source_no,"
                + "p.code AS product_code,p.name AS product_name,COALESCE(NULLIF(ifi.vehicle_model,''),p.vehicle_model) AS vehicle_model,"
                + "p.feature_code,COALESCE(ifi.count,0) AS count,COALESCE(ifi.original_product_price,ifi.product_price) AS product_price,"
                + "CASE WHEN ifi.allocated_freight IS NULL THEN COALESCE(ifi.original_product_price,ifi.product_price) "
                + "ELSE ROUND((COALESCE(ifi.count,0)*COALESCE(ifi.original_product_price,ifi.product_price)+ifi.allocated_freight)/NULLIF(ifi.count,0),6) END AS freight_included_price,"
                + "ROUND(COALESCE(ifi.count,0)*COALESCE(ifi.original_product_price,ifi.product_price),2) AS total_amount,"
                + "COALESCE(inv.invoice_amount,0) AS invoice_amount,"
                + "CASE WHEN COALESCE(inv.invoice_count,0)<=0 THEN '未开票' WHEN inv.invoice_count>=ifi.count THEN '已开票' ELSE '部分开票' END AS invoice_status,"
                + "COALESCE(NULLIF(ifi.origin_place,''),p.origin_place) AS origin_place,pi.factory_order_no,"
                + "COALESCE(NULLIF(ifi.brand,''),p.brand) AS brand,s.name AS supplier_name,s.code AS supplier_code,"
                + "w.name AS warehouse_name,ifi.warehouse_position,pi.purchaser AS purchaser_name,p.standard,"
                + "COALESCE(NULLIF(ifi.drawing_no,''),p.drawing_no) AS drawing_no,u.name AS unit_name,cu.nickname AS creator_name,"
                + "ifi.remark AS item_remark,pi.remark AS document_remark,p.factory_code,NULL AS factory_name,NULL AS factory_model,"
                + "s.region AS supplier_region,CASE WHEN ifi.allocated_freight IS NULL THEN NULL ELSE ifi.freight_type1 END AS freight_type1,"
                + "ifi.allocated_freight AS freight1,s.id AS supplier_id "
                + "FROM in_final ifi INNER JOIN erp_purchase_in pi ON pi.id=ifi.in_id AND pi.tenant_id=ifi.tenant_id "
                + "AND pi.deleted=b'0' AND pi.status=20 INNER JOIN erp_supplier s ON s.id=pi.supplier_id "
                + "AND s.tenant_id=pi.tenant_id AND s.deleted=b'0' LEFT JOIN invoice_agg inv ON inv.source_type=10 AND inv.source_item_id=ifi.id "
                + commonJoins("ifi") + " WHERE ifi.tenant_id=:tenant" + documentScope + supplierScope;
    }

    private String returnSelect(String documentScope, String supplierScope) {
        return "SELECT CONCAT('RETURN-',pri.id),pri.id,pr.id,pri.product_id,pri.warehouse_id,"
                + "'PURCHASE_RETURN',CASE WHEN pr.return_mode=10 THEN pri.source_in_id END,'PURCHASE_IN',"
                + "'采购退货',pr.approve_time,pr.no,"
                + "CASE WHEN pr.return_mode=10 THEN pri.source_in_no END,"
                + "COALESCE(NULLIF(pri.part_code,''),p.code),COALESCE(NULLIF(pri.part_name,''),p.name),"
                + "COALESCE(NULLIF(pri.vehicle_model,''),p.vehicle_model),p.feature_code,-COALESCE(pri.count,0),"
                + "pri.product_price,pri.product_price,ROUND(-COALESCE(pri.count,0)*COALESCE(pri.product_price,0),2),"
                + "COALESCE(inv.invoice_amount,0),"
                + "CASE WHEN COALESCE(inv.invoice_count,0)<=0 THEN '未开票' WHEN inv.invoice_count>=pri.count THEN '已开票' ELSE '部分开票' END,"
                + "COALESCE(NULLIF(pri.origin_place,''),p.origin_place),pr.factory_order_no,"
                + "COALESCE(NULLIF(pri.brand,''),p.brand),s.name,s.code,w.name,pri.warehouse_position,pr.purchaser,p.standard,"
                + "COALESCE(NULLIF(pri.drawing_no,''),p.drawing_no),u.name,cu.nickname,pri.remark,pr.remark,p.factory_code,"
                + "NULL,NULL,s.region,NULL,NULL,s.id "
                + "FROM erp_purchase_return_items pri INNER JOIN erp_purchase_return pr ON pr.id=pri.return_id "
                + "AND pr.tenant_id=pri.tenant_id AND pr.deleted=b'0' AND pr.status=20 "
                + "INNER JOIN erp_supplier s ON s.id=pr.supplier_id AND s.tenant_id=pr.tenant_id AND s.deleted=b'0' "
                + "LEFT JOIN invoice_agg inv ON inv.source_type=30 AND inv.source_item_id=pri.id "
                + commonJoins("pri") + " WHERE pri.tenant_id=:tenant AND pri.deleted=b'0'" + documentScope + supplierScope;
    }

    private String adjustSelect(String documentScope, String supplierScope) {
        return "SELECT CONVERT(CONCAT('ADJUST-',pai.id) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "pai.id,pa.id,pai.product_id,pai.warehouse_id,"
                + "'PURCHASE_ADJUST',pai.in_id,'PURCHASE_IN','采购调价',pa.approve_time,"
                + "CONVERT(pa.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(pai.in_no USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(pai.product_code,''),p.code) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(pai.product_name,''),p.name) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(pai.vehicle_model,''),p.vehicle_model) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(pai.feature_code,''),p.feature_code) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "0,pai.old_price,pai.old_price,ROUND(COALESCE(pai.adjust_price,0),2),COALESCE(inv.invoice_amount,0),"
                + "CASE WHEN COALESCE(inv.invoice_count,0)<=0 THEN '未开票' WHEN inv.invoice_count>=pai.count THEN '已开票' ELSE '部分开票' END,"
                + "CONVERT(COALESCE(NULLIF(pai.origin_place,''),p.origin_place) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "source_in.factory_order_no,"
                + "CONVERT(COALESCE(NULLIF(pai.brand,''),p.brand) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "s.name,s.code,w.name,CONVERT(pai.warehouse_position USING utf8mb4) COLLATE utf8mb4_unicode_ci,au.nickname,"
                + "CONVERT(COALESCE(NULLIF(pai.standard,''),p.standard) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(pai.drawing_no,''),p.drawing_no) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(pai.product_unit_name,''),u.name) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "cu.nickname,NULL,CONVERT(pa.remark USING utf8mb4) COLLATE utf8mb4_unicode_ci,p.factory_code,"
                + "NULL,NULL,s.region,NULL,NULL,s.id "
                + "FROM erp_purchase_price_adjust_item pai INNER JOIN erp_purchase_price_adjust pa ON pa.id=pai.adjust_id "
                + "AND pa.tenant_id=pai.tenant_id AND pa.deleted=b'0' AND pa.status=20 "
                + "INNER JOIN erp_supplier s ON s.id=pa.supplier_id AND s.tenant_id=pa.tenant_id AND s.deleted=b'0' "
                + "LEFT JOIN invoice_agg inv ON inv.source_type=20 AND inv.source_item_id=pai.id "
                + "LEFT JOIN erp_purchase_in source_in ON source_in.id=pai.in_id AND source_in.tenant_id=pai.tenant_id AND source_in.deleted=b'0' "
                + commonJoins("pai")
                + " LEFT JOIN system_users au ON au.id=pa.adjuster AND au.tenant_id=pa.tenant_id AND au.deleted=b'0' "
                + "WHERE pai.tenant_id=:tenant AND pai.deleted=b'0'" + documentScope + supplierScope;
    }

    private String commonJoins(String itemAlias) {
        return " LEFT JOIN erp_product p ON p.id=" + itemAlias + ".product_id AND p.tenant_id=" + itemAlias + ".tenant_id AND p.deleted=b'0'"
                + " LEFT JOIN erp_warehouse w ON w.id=" + itemAlias + ".warehouse_id AND w.tenant_id=" + itemAlias + ".tenant_id AND w.deleted=b'0'"
                + " LEFT JOIN erp_product_unit u ON u.id="
                + ("pai".equals(itemAlias) ? "p.unit_id" : itemAlias + ".product_unit_id")
                + " AND u.tenant_id=" + itemAlias + ".tenant_id AND u.deleted=b'0'"
                + " LEFT JOIN system_users cu ON cu.id="
                + ("ifi".equals(itemAlias) ? "pi.creator" : "pri".equals(itemAlias) ? "pr.creator" : "pa.creator")
                + " AND cu.tenant_id=" + itemAlias + ".tenant_id AND cu.deleted=b'0'";
    }

    private void appendFilters(Sql sql, Filter filter) {
        if (filter.getApproveDateFrom() != null) {
            sql.body += " AND report.approve_date>=:approveFrom";
            sql.params.put("approveFrom", filter.getApproveDateFrom());
        }
        if (filter.getApproveDateTo() != null) {
            sql.body += " AND report.approve_date<=:approveTo";
            sql.params.put("approveTo", filter.getApproveDateTo());
        }
        if (filter.getDocumentTypes() != null && !filter.getDocumentTypes().isEmpty()) {
            sql.body += " AND report.document_type_code IN (:documentTypes)";
            sql.params.put("documentTypes", filter.getDocumentTypes());
        }
        like(sql, "report.document_no", "documentNo", filter.getDocumentNo());
        like(sql, "report.source_no", "sourceNo", filter.getSourceNo());
        like(sql, "report.factory_order_no", "factoryOrderNo", filter.getFactoryOrderNo());
        equal(sql, "report.supplier_id", "supplierId", filter.getSupplierId());
        equal(sql, "report.warehouse_id", "warehouseId", filter.getWarehouseId());
        like(sql, "report.purchaser_name", "purchaserKeyword", filter.getPurchaserKeyword());
        equal(sql, "report.invoice_status", "invoiceStatus", filter.getInvoiceStatus());
        if (hasText(filter.getProductKeyword())) {
            sql.body += " AND (report.product_code LIKE :productKeyword OR report.product_name LIKE :productKeyword "
                    + "OR report.factory_code LIKE :productKeyword OR report.drawing_no LIKE :productKeyword "
                    + "OR report.feature_code LIKE :productKeyword)";
            sql.params.put("productKeyword", "%" + filter.getProductKeyword().trim() + "%");
        }
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("approveDate", "report.approve_date");
        fields.put("documentNo", "report.document_no");
        fields.put("documentType", "report.document_type");
        fields.put("productCode", "report.product_code");
        fields.put("productName", "report.product_name");
        fields.put("supplierName", "report.supplier_name");
        fields.put("warehouseName", "report.warehouse_name");
        fields.put("count", "report.count");
        fields.put("productPrice", "report.product_price");
        fields.put("totalAmount", "report.total_amount");
        String field = fields.getOrDefault(filter.getOrderField(), "report.approve_date");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",report.document_no" + direction + ",report.item_id" + direction;
    }

    private String documentScope(ErpFinanceVisibleScope scope, String deptColumn, String creatorColumn,
                                 String prefix, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(deptColumn).append(" IN (:").append(prefix).append("DeptIds)");
            params.put(prefix + "DeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(creatorColumn).append("=:").append(prefix).append("Self");
            params.put(prefix + "Self", String.valueOf(scope.getSelfUserId()));
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String supplierScope(ErpFinanceVisibleScope scope, String supplierAlias, String prefix,
                                 Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(supplierAlias).append(".dept_id IN (:").append(prefix).append("DeptIds)")
                    .append(" OR (").append(supplierAlias).append(".allow_multi_dept=b'1' AND EXISTS (")
                    .append("SELECT 1 FROM erp_supplier_dept esd WHERE esd.tenant_id=").append(supplierAlias).append(".tenant_id ")
                    .append("AND esd.supplier_id=").append(supplierAlias).append(".id AND esd.deleted=b'0' ")
                    .append("AND esd.dept_id IN (:").append(prefix).append("DeptIds)))");
            params.put(prefix + "DeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(supplierAlias).append(".creator=:").append(prefix).append("Self");
            params.put(prefix + "Self", String.valueOf(scope.getSelfUserId()));
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private void like(Sql sql, String column, String name, String value) {
        if (!hasText(value)) return;
        sql.body += " AND " + column + " LIKE :" + name;
        sql.params.put(name, "%" + value.trim() + "%");
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null || value instanceof String && !hasText((String) value)) return;
        sql.body += " AND " + column + "=:" + name;
        sql.params.put(name, value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private NamedParameterJdbcTemplate jdbc() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public static class Scope {
        private final long tenantId;
        private final ErpFinanceVisibleScope inScope;
        private final ErpFinanceVisibleScope returnScope;
        private final ErpFinanceVisibleScope adjustScope;
        private final ErpFinanceVisibleScope supplierScope;

        public Scope(long tenantId, ErpFinanceVisibleScope inScope, ErpFinanceVisibleScope returnScope,
                     ErpFinanceVisibleScope adjustScope, ErpFinanceVisibleScope supplierScope) {
            this.tenantId = tenantId;
            this.inScope = inScope;
            this.returnScope = returnScope;
            this.adjustScope = adjustScope;
            this.supplierScope = supplierScope;
        }
    }

    private static class Sql {
        private String cte;
        private String body;
        private final Map<String, Object> params = new HashMap<>();
    }
}
