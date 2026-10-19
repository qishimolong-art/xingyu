package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ErpPurchaseInvoiceDetailReportRepository {
    @Resource private NamedParameterJdbcTemplate jdbc;

    public long count(Filter filter, Scope scope) {
        Sql sql = sql(filter, scope);
        Long value = jdbc.queryForObject(sql.cte + " SELECT COUNT(*) " + sql.body, sql.params, Long.class);
        return value == null ? 0 : value;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = sql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc.query(sql.cte + " SELECT report.* " + sql.body + orderBy(filter)
                        + " LIMIT :limit OFFSET :offset", sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = sql(filter, scope);
        return jdbc.queryForObject(sql.cte + " SELECT COUNT(*) row_count,"
                        + "COALESCE(SUM(report.count),0) total_count,"
                        + "COALESCE(SUM(report.total_amount),0) total_amount " + sql.body,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql sql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String permission = scopeCondition(scope.visibleScope, sql.params);
        sql.cte = "WITH payment_agg AS ("
                + "SELECT fpi.biz_type,fpi.biz_id,SUM(COALESCE(fpi.payment_price,0)) paid_amount "
                + "FROM erp_finance_payment_item fpi INNER JOIN erp_finance_payment fp ON fp.id=fpi.payment_id "
                + "AND fp.tenant_id=fpi.tenant_id AND fp.deleted=b'0' AND fp.status=20 "
                + "WHERE fpi.tenant_id=:tenant AND fpi.deleted=b'0' AND fpi.write_off_status=1 "
                + "AND fpi.biz_type=11 GROUP BY fpi.biz_type,fpi.biz_id),"
                + "adjust_payment_agg AS (SELECT fpi.biz_id,SUM(COALESCE(fpi.payment_price,0)) paid_amount "
                + "FROM erp_finance_payment_item fpi WHERE fpi.tenant_id=:tenant AND fpi.deleted=b'0' "
                + "AND fpi.biz_type=13 GROUP BY fpi.biz_id),"
                + "in_settlement AS (SELECT pin.id,pin.tenant_id,CASE WHEN EXISTS (SELECT 1 "
                + "FROM erp_purchase_in_items psi WHERE psi.deleted=b'0' AND psi.tenant_id=pin.tenant_id "
                + "AND psi.in_id=pin.id AND psi.original_product_price IS NOT NULL) THEN "
                + "(COALESCE((SELECT SUM(ROUND(COALESCE(psi.original_product_price,psi.product_price)*psi.count,2)) "
                + "FROM erp_purchase_in_items psi WHERE psi.deleted=b'0' AND psi.tenant_id=pin.tenant_id AND psi.in_id=pin.id),0) "
                + "-ROUND(COALESCE((SELECT SUM(ROUND(COALESCE(psi.original_product_price,psi.product_price)*psi.count,2)) "
                + "FROM erp_purchase_in_items psi WHERE psi.deleted=b'0' AND psi.tenant_id=pin.tenant_id AND psi.in_id=pin.id),0) "
                + "*COALESCE(pin.discount_percent,0)/100,2)+COALESCE(pin.fee_amount,pin.other_price,0)) "
                + "ELSE pin.total_price END settlement_total FROM erp_purchase_in pin "
                + "WHERE pin.tenant_id=:tenant AND pin.deleted=b'0'),"
                + "report AS (SELECT ii.id item_id,inv.id invoice_id,ii.product_id,inv.supplier_id,ii.source_type,ii.source_doc_id,"
                + "inv.create_time create_date,inv.no document_no,s.name supplier_name,inv.invoice_date,"
                + "inv.invoice_type,inv.invoice_no,"
                + "CASE WHEN ii.source_type=30 THEN COALESCE(NULLIF(pri.part_code,''),p.code) "
                + "WHEN ii.source_type=20 THEN COALESCE(NULLIF(pai.product_code,''),p.code) ELSE p.code END product_code,"
                + "CASE WHEN ii.source_type=30 THEN COALESCE(NULLIF(pri.part_name,''),p.name) "
                + "WHEN ii.source_type=20 THEN COALESCE(NULLIF(pai.product_name,''),p.name) ELSE p.name END product_name,"
                + "COALESCE(NULLIF(ii.product_unit_name,''),u.name) unit_name,ii.count,ii.product_price,ii.total_price total_amount,"
                + "CASE WHEN ii.source_type IS NULL THEN NULL WHEN COALESCE(CASE ii.source_type "
                + "WHEN 10 THEN pay.paid_amount WHEN 20 THEN adjust_pay.paid_amount WHEN 30 THEN pr.refund_price END,0)=0 THEN '未结算' "
                + "WHEN ABS(COALESCE(CASE ii.source_type WHEN 10 THEN pay.paid_amount WHEN 20 THEN adjust_pay.paid_amount "
                + "WHEN 30 THEN pr.refund_price END,0))>=ABS(CASE ii.source_type WHEN 10 THEN ins.settlement_total "
                + "WHEN 20 THEN pa.total_adjust_price WHEN 30 THEN pr.total_price END) THEN '已结算' ELSE '部分结算' END settlement_status,"
                + "NULL tax_rate,"
                + "CASE WHEN ii.source_type=30 THEN COALESCE(NULLIF(pri.vehicle_model,''),p.vehicle_model) "
                + "WHEN ii.source_type=20 THEN COALESCE(NULLIF(pai.vehicle_model,''),p.vehicle_model) "
                + "WHEN ii.source_type=10 THEN COALESCE(NULLIF(pii.vehicle_model,''),p.vehicle_model) ELSE p.vehicle_model END vehicle_model,"
                + "CASE WHEN ii.source_type=30 THEN COALESCE(NULLIF(pri.brand,''),p.brand) "
                + "WHEN ii.source_type=20 THEN COALESCE(NULLIF(pai.brand,''),p.brand) "
                + "WHEN ii.source_type=10 THEN COALESCE(NULLIF(pii.brand,''),p.brand) ELSE p.brand END brand,"
                + "CASE WHEN ii.source_type=30 THEN COALESCE(NULLIF(pri.origin_place,''),p.origin_place) "
                + "WHEN ii.source_type=20 THEN COALESCE(NULLIF(pai.origin_place,''),p.origin_place) "
                + "WHEN ii.source_type=10 THEN COALESCE(NULLIF(pii.origin_place,''),p.origin_place) ELSE p.origin_place END origin_place,"
                + "CASE WHEN ii.source_type=30 THEN COALESCE(NULLIF(pri.drawing_no,''),p.drawing_no) "
                + "WHEN ii.source_type=20 THEN COALESCE(NULLIF(pai.drawing_no,''),p.drawing_no) "
                + "WHEN ii.source_type=10 THEN COALESCE(NULLIF(pii.drawing_no,''),p.drawing_no) ELSE p.drawing_no END drawing_no,"
                + "CASE WHEN ii.source_type=20 THEN COALESCE(NULLIF(pai.standard,''),p.standard) ELSE p.standard END standard,"
                + "NULL volume,CASE WHEN ii.source_type=30 THEN COALESCE(pri.weight,p.weight) "
                + "WHEN ii.source_type=20 THEN COALESCE(pai.weight,p.weight) ELSE p.weight END weight,"
                + "NULL tax_category_code,NULL tax_product_name,NULL tax_product_model,NULL tax_unit,"
                + "inv.approve_time approve_date,ii.remark item_remark,"
                + "CASE WHEN ii.source_type IS NULL THEN NULL ELSE COALESCE(ii.source_doc_no,ii.source_in_no) END business_no,"
                + "CASE ii.source_type WHEN 10 THEN pin.approve_time WHEN 20 THEN pa.approve_time "
                + "WHEN 30 THEN pr.approve_time END business_approve_date "
                + "FROM erp_purchase_invoice_item ii INNER JOIN erp_purchase_invoice inv ON inv.id=ii.invoice_id "
                + "AND inv.tenant_id=ii.tenant_id AND inv.deleted=b'0' AND inv.status=20 AND inv.approve_time IS NOT NULL "
                + "INNER JOIN erp_supplier s ON s.id=inv.supplier_id AND s.tenant_id=inv.tenant_id AND s.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=ii.product_id AND p.tenant_id=ii.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_product_unit u ON u.id=p.unit_id AND u.tenant_id=p.tenant_id AND u.deleted=b'0' "
                + "LEFT JOIN erp_purchase_in pin ON ii.source_type=10 AND pin.id=ii.source_doc_id AND pin.tenant_id=ii.tenant_id AND pin.deleted=b'0' "
                + "LEFT JOIN in_settlement ins ON ins.id=pin.id AND ins.tenant_id=pin.tenant_id "
                + "LEFT JOIN erp_purchase_in_items pii ON ii.source_type=10 AND pii.id=ii.source_item_id AND pii.tenant_id=ii.tenant_id AND pii.deleted=b'0' "
                + "LEFT JOIN erp_purchase_price_adjust pa ON ii.source_type=20 AND pa.id=ii.source_doc_id AND pa.tenant_id=ii.tenant_id AND pa.deleted=b'0' "
                + "LEFT JOIN erp_purchase_price_adjust_item pai ON ii.source_type=20 AND pai.id=ii.source_item_id AND pai.tenant_id=ii.tenant_id AND pai.deleted=b'0' "
                + "LEFT JOIN erp_purchase_return pr ON ii.source_type=30 AND pr.id=ii.source_doc_id AND pr.tenant_id=ii.tenant_id AND pr.deleted=b'0' "
                + "LEFT JOIN erp_purchase_return_items pri ON ii.source_type=30 AND pri.id=ii.source_item_id AND pri.tenant_id=ii.tenant_id AND pri.deleted=b'0' "
                + "LEFT JOIN payment_agg pay ON ii.source_type=10 AND pay.biz_type=11 AND pay.biz_id=ii.source_doc_id "
                + "LEFT JOIN adjust_payment_agg adjust_pay ON ii.source_type=20 AND adjust_pay.biz_id=ii.source_doc_id "
                + "WHERE ii.tenant_id=:tenant AND ii.deleted=b'0'" + permission + ") ";
        sql.body = "FROM report WHERE 1=1";
        filters(sql, filter);
        return sql;
    }

    private void filters(Sql sql, Filter f) {
        between(sql, "report.approve_date", "approve", f.getApproveDateFrom(), f.getApproveDateTo());
        between(sql, "report.create_date", "create", f.getCreateDateFrom(), f.getCreateDateTo());
        between(sql, "report.invoice_date", "invoice", f.getInvoiceDateFrom(), f.getInvoiceDateTo());
        between(sql, "report.business_approve_date", "businessApprove", f.getBusinessApproveDateFrom(), f.getBusinessApproveDateTo());
        like(sql, "report.document_no", "documentNo", f.getDocumentNo());
        equal(sql, "report.supplier_id", "supplierId", f.getSupplierId());
        like(sql, "report.invoice_no", "invoiceNo", f.getInvoiceNo());
        equal(sql, "report.invoice_type", "invoiceType", f.getInvoiceType());
        like(sql, "report.business_no", "businessNo", f.getBusinessNo());
        equal(sql, "report.settlement_status", "settlementStatus", f.getSettlementStatus());
        if (text(f.getProductKeyword())) {
            sql.body += " AND (report.product_code LIKE :productKeyword OR report.product_name LIKE :productKeyword)";
            sql.params.put("productKeyword", "%" + f.getProductKeyword().trim() + "%");
        }
        range(sql, "report.count", "count", f.getCountMin(), f.getCountMax());
        range(sql, "report.product_price", "price", f.getPriceMin(), f.getPriceMax());
        range(sql, "report.total_amount", "amount", f.getAmountMin(), f.getAmountMax());
        like(sql, "report.vehicle_model", "vehicleModel", f.getVehicleModel());
        like(sql, "report.brand", "brand", f.getBrand());
        like(sql, "report.origin_place", "originPlace", f.getOriginPlace());
        like(sql, "report.drawing_no", "drawingNo", f.getDrawingNo());
        like(sql, "report.standard", "standard", f.getStandard());
        range(sql, "report.weight", "weight", f.getWeightMin(), f.getWeightMax());
        like(sql, "report.item_remark", "itemRemark", f.getItemRemark());
    }

    private String orderBy(Filter f) {
        Map<String, String> fields = new HashMap<>();
        fields.put("createDate", "report.create_date"); fields.put("documentNo", "report.document_no");
        fields.put("supplierName", "report.supplier_name"); fields.put("invoiceDate", "report.invoice_date");
        fields.put("invoiceType", "report.invoice_type"); fields.put("invoiceNo", "report.invoice_no");
        fields.put("productCode", "report.product_code"); fields.put("productName", "report.product_name");
        fields.put("count", "report.count"); fields.put("productPrice", "report.product_price");
        fields.put("totalAmount", "report.total_amount"); fields.put("settlementStatus", "report.settlement_status");
        fields.put("approveDate", "report.approve_date"); fields.put("businessNo", "report.business_no");
        fields.put("businessApproveDate", "report.business_approve_date");
        String requested = fields.get(f.getOrderField());
        if (requested == null) return " ORDER BY report.approve_date DESC,report.document_no DESC,report.item_id ASC";
        String direction = "asc".equalsIgnoreCase(f.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + requested + direction + ",report.approve_date DESC,report.document_no DESC,report.item_id ASC";
    }

    private String scopeCondition(ErpFinanceVisibleScope scope, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder value = new StringBuilder(" AND ("); boolean any = false;
        if (!scope.getDeptIds().isEmpty()) { value.append("inv.dept_id IN (:scopeDeptIds)"); params.put("scopeDeptIds", scope.getDeptIds()); any = true; }
        if (scope.getSelfUserId() != null) { if (any) value.append(" OR "); value.append("inv.handler_id=:scopeUserId"); params.put("scopeUserId", scope.getSelfUserId()); any = true; }
        return any ? value.append(')').toString() : " AND 1=0";
    }

    private void between(Sql sql, String column, String key, Object from, Object to) {
        if (from != null) { sql.body += " AND " + column + ">=:" + key + "From"; sql.params.put(key + "From", from); }
        if (to != null) { sql.body += " AND " + column + "<=:" + key + "To"; sql.params.put(key + "To", to); }
    }
    private void range(Sql sql, String column, String key, Object min, Object max) { between(sql, column, key, min, max); }
    private void like(Sql sql, String column, String key, String value) { if (text(value)) { sql.body += " AND " + column + " LIKE :" + key; sql.params.put(key, "%" + value.trim() + "%"); } }
    private void equal(Sql sql, String column, String key, Object value) { if (value != null && (!(value instanceof String) || text((String) value))) { sql.body += " AND " + column + "=:" + key; sql.params.put(key, value instanceof String ? ((String) value).trim() : value); } }
    private boolean text(String value) { return value != null && !value.trim().isEmpty(); }

    public static class Scope {
        private final long tenantId; private final ErpFinanceVisibleScope visibleScope;
        public Scope(long tenantId, ErpFinanceVisibleScope visibleScope) { this.tenantId = tenantId; this.visibleScope = visibleScope; }
    }
    private static class Sql { private String cte; private String body; private final Map<String, Object> params = new HashMap<>(); }
}
