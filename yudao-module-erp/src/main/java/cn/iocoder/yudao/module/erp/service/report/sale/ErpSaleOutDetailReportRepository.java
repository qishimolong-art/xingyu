package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.PriceEvent;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Summary;
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
public class ErpSaleOutDetailReportRepository {

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
        return jdbc().queryForObject(sql.cte + summarySelect() + sql.body,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private String summarySelect() {
        return " SELECT COUNT(*) AS row_count,"
                + "COALESCE(SUM(report.count),0) AS total_count,"
                + "COALESCE(SUM(report.total_amount),0) AS total_amount,"
                + "COALESCE(SUM(report.invoice_amount),0) AS invoice_amount,"
                + "COALESCE(SUM(report.freight_amount),0) AS freight_amount,"
                + "COALESCE(SUM(CASE WHEN report.document_type_code IN ('SALE_OUT','SALE_RETURN') "
                + "AND report.sale_cost_amount IS NULL THEN 1 ELSE 0 END),0) AS missing_sale_cost_count,"
                + "CASE WHEN COALESCE(SUM(CASE WHEN report.document_type_code IN ('SALE_OUT','SALE_RETURN') "
                + "AND report.sale_cost_amount IS NULL THEN 1 ELSE 0 END),0)>0 THEN NULL "
                + "ELSE COALESCE(SUM(report.sale_cost_amount),0) END AS total_sale_cost_amount ";
    }

    public List<PriceEvent> priceEvents(Collection<Long> customerIds, Collection<Long> productIds, Scope scope) {
        if (customerIds == null || customerIds.isEmpty() || productIds == null || productIds.isEmpty()) return List.of();
        Map<String, Object> params = new HashMap<>();
        params.put("tenant", scope.tenantId);
        params.put("customerIds", customerIds);
        params.put("productIds", productIds);
        String outScope = documentScope(scope.outScope, "so.dept_id", "so.creator", "eventOut", params);
        String adjustScope = documentScope(scope.adjustScope, "sa.dept_id", "sa.creator", "eventAdjust", params);
        String outCustomerScope = customerScope(scope.customerScope, "c", "eventOutCustomer", params);
        String adjustCustomerScope = customerScope(scope.customerScope, "c", "eventAdjustCustomer", params);
        String sql = "SELECT event.customer_id,event.product_id,event.event_time,event.document_type_code,event.item_id,event.price FROM ("
                + "SELECT so.customer_id,soi.product_id,so.approve_time AS event_time,'SALE_OUT' AS document_type_code,"
                + "soi.id AS item_id,COALESCE(soi.original_product_price,soi.product_price) AS price "
                + "FROM erp_sale_out_items soi INNER JOIN erp_sale_out so ON so.id=soi.out_id "
                + "AND so.tenant_id=soi.tenant_id AND so.deleted=b'0' AND so.status=20 "
                + "INNER JOIN erp_customer c ON c.id=so.customer_id AND c.tenant_id=so.tenant_id AND c.deleted=b'0' "
                + "WHERE soi.tenant_id=:tenant AND soi.deleted=b'0' AND so.approve_time IS NOT NULL "
                + "AND so.customer_id IN (:customerIds) AND soi.product_id IN (:productIds)" + outScope + outCustomerScope
                + " UNION ALL "
                + "SELECT sa.customer_id,sai.product_id,sa.approve_time,'SALE_ADJUST',sai.id,sai.new_price "
                + "FROM erp_sale_price_adjust_item sai INNER JOIN erp_sale_price_adjust sa "
                + "ON sa.id=sai.adjust_id AND sa.tenant_id=sai.tenant_id AND sa.deleted=b'0' AND sa.status=20 "
                + "INNER JOIN erp_customer c ON c.id=sa.customer_id AND c.tenant_id=sa.tenant_id AND c.deleted=b'0' "
                + "WHERE sai.tenant_id=:tenant AND sai.deleted=b'0' AND sa.approve_time IS NOT NULL "
                + "AND sa.customer_id IN (:customerIds) AND sai.product_id IN (:productIds)" + adjustScope + adjustCustomerScope
                + ") event ORDER BY event.customer_id,event.product_id,event.event_time,event.document_type_code,event.item_id";
        return jdbc().query(sql, params, new BeanPropertyRowMapper<>(PriceEvent.class));
    }

    private Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String outScope = documentScope(scope.outScope, "so.dept_id", "so.creator", "outDoc", sql.params);
        String returnScope = documentScope(scope.returnScope, "sr.dept_id", "sr.creator", "returnDoc", sql.params);
        String adjustScope = documentScope(scope.adjustScope, "sa.dept_id", "sa.creator", "adjustDoc", sql.params);
        String outCustomerScope = customerScope(scope.customerScope, "c", "outCustomer", sql.params);
        String returnCustomerScope = customerScope(scope.customerScope, "c", "returnCustomer", sql.params);
        String adjustCustomerScope = customerScope(scope.customerScope, "c", "adjustCustomer", sql.params);

        sql.cte = "WITH out_weight AS ("
                + "SELECT soi.*,so.freight,so.invoice_amount,COALESCE(sc.freight_type,sq.freight_type) AS source_freight_type,"
                + "SUM(GREATEST(COALESCE(soi.count,0),0)) OVER(PARTITION BY soi.out_id) AS freight_total_count,"
                + "SUM(GREATEST(COALESCE(soi.count,0)*COALESCE(soi.original_product_price,soi.product_price,0),0)) "
                + "OVER(PARTITION BY soi.out_id) AS invoice_total_amount,"
                + "ROW_NUMBER() OVER(PARTITION BY soi.out_id ORDER BY soi.id DESC) AS tail_rank "
                + "FROM erp_sale_out_items soi INNER JOIN erp_sale_out so ON so.id=soi.out_id "
                + "AND so.tenant_id=soi.tenant_id AND so.deleted=b'0' "
                + "LEFT JOIN erp_sale_cart sc ON so.source_type=30 AND sc.id=so.source_id AND sc.tenant_id=so.tenant_id AND sc.deleted=b'0' "
                + "LEFT JOIN erp_sale_quote sq ON so.source_type=20 AND sq.id=so.source_id AND sq.tenant_id=so.tenant_id AND sq.deleted=b'0' "
                + "WHERE soi.tenant_id=:tenant AND soi.deleted=b'0'),"
                + "out_alloc AS (SELECT ow.*,"
                + "CASE WHEN ow.freight>0 AND ow.freight_total_count>0 THEN ROUND(ow.freight*GREATEST(COALESCE(ow.count,0),0)/ow.freight_total_count,2) END AS provisional_freight,"
                + "CASE WHEN ow.invoice_amount>0 AND ow.invoice_total_amount>0 THEN ROUND(ow.invoice_amount*GREATEST(COALESCE(ow.count,0)*COALESCE(ow.original_product_price,ow.product_price,0),0)/ow.invoice_total_amount,2) END AS provisional_invoice "
                + "FROM out_weight ow),"
                + "out_final AS (SELECT oa.*,"
                + "CASE WHEN oa.provisional_freight IS NULL THEN NULL WHEN oa.tail_rank=1 THEN oa.freight-(SUM(oa.provisional_freight) OVER(PARTITION BY oa.out_id)-oa.provisional_freight) ELSE oa.provisional_freight END AS allocated_freight,"
                + "CASE WHEN oa.provisional_invoice IS NULL THEN NULL WHEN oa.tail_rank=1 THEN oa.invoice_amount-(SUM(oa.provisional_invoice) OVER(PARTITION BY oa.out_id)-oa.provisional_invoice) ELSE oa.provisional_invoice END AS allocated_invoice "
                + "FROM out_alloc oa),"
                + "report_union AS (" + outSelect(outScope, outCustomerScope)
                + " UNION ALL " + returnSelect(returnScope, returnCustomerScope)
                + " UNION ALL " + adjustSelect(adjustScope, adjustCustomerScope) + ") ";
        sql.body = "FROM report_union report WHERE 1=1";
        appendFilters(sql, filter);
        return sql;
    }

    private String outSelect(String documentScope, String partyScope) {
        return "SELECT CONCAT('OUT-',ofi.id) AS row_key,ofi.id AS item_id,so.id AS business_id,"
                + "COALESCE(so.order_id,so.source_id) AS source_business_id,"
                + "CASE WHEN so.order_id IS NOT NULL THEN 'SALE_ORDER' "
                + "WHEN so.source_type=10 THEN 'SALE_ORDER' WHEN so.source_type=20 THEN 'SALE_QUOTE' "
                + "WHEN so.source_type=30 THEN 'SALE_CART' WHEN so.source_type=40 THEN 'SALE_PRICE_ADJUST' "
                + "WHEN so.source_type=50 THEN 'PURCHASE_IN' WHEN so.source_type=60 THEN 'MALL_ORDER' "
                + "WHEN so.source_type=70 THEN 'SALE_RETURN' "
                + "WHEN NULLIF(so.order_no,'') IS NOT NULL THEN 'SALE_ORDER' END AS source_document_type_code,"
                + "ofi.product_id,so.customer_id,"
                + "'SALE_OUT' AS document_type_code,'销售出库' AS document_type,so.approve_time AS approve_date,"
                + "so.no AS document_no,COALESCE(NULLIF(so.source_no,''),so.order_no) AS source_no,so.third_party_no,"
                + "p.code AS product_code,p.name AS product_name,COALESCE(NULLIF(ofi.vehicle_model,''),p.vehicle_model) AS vehicle_model,"
                + "COALESCE(NULLIF(ofi.feature_code,''),p.feature_code) AS feature_code,COALESCE(ofi.count,0) AS count,"
                + "COALESCE(ofi.original_product_price,ofi.product_price) AS product_price,"
                + "CASE WHEN ofi.allocated_freight IS NULL THEN COALESCE(ofi.original_product_price,ofi.product_price) "
                + "ELSE ROUND((COALESCE(ofi.count,0)*COALESCE(ofi.original_product_price,ofi.product_price,0)+ofi.allocated_freight)/NULLIF(ofi.count,0),6) END AS freight_included_price,"
                + "ROUND(COALESCE(ofi.count,0)*COALESCE(ofi.original_product_price,ofi.product_price,0),2) AS total_amount,"
                + "ofi.allocated_invoice AS invoice_amount,"
                + "CASE WHEN COALESCE(ofi.invoice_amount,0)<=0 THEN '未开票' WHEN ofi.invoice_amount>=so.total_product_price THEN '已开票' ELSE '部分开票' END AS invoice_status,"
                + "cost_record.cost_price AS sale_cost_price,"
                + "CASE WHEN cost_record.cost_price IS NULL THEN NULL "
                + "ELSE ROUND(COALESCE(ofi.count,0)*cost_record.cost_price,2) END AS sale_cost_amount,"
                + "NULL AS gross_profit_amount,NULL AS gross_profit_rate,"
                + "COALESCE(NULLIF(ofi.origin_place,''),p.origin_place) AS origin_place,COALESCE(NULLIF(ofi.brand,''),p.brand) AS brand,"
                + "c.name AS customer_name,c.code AS customer_code,c.area_id AS customer_area_id,d.name AS dept_name,"
                + "w.name AS warehouse_name,ofi.warehouse_position,su.nickname AS sale_user_name,"
                + "COALESCE(NULLIF(ofi.standard,''),p.standard) AS standard,COALESCE(NULLIF(ofi.drawing_no,''),p.drawing_no) AS drawing_no,"
                + "u.name AS unit_name,CASE WHEN ofi.gift_flag=b'1' THEN '是' ELSE '否' END AS gift_flag_text,"
                + "cu.nickname AS creator_name,ofi.remark AS item_remark,so.remark AS document_remark,p.factory_code,"
                + "NULL AS factory_name,NULL AS factory_model,CASE WHEN ofi.allocated_freight IS NULL THEN NULL ELSE ofi.source_freight_type END AS freight_type,"
                + "ofi.allocated_freight AS freight_amount,so.dept_id AS dept_id,ofi.warehouse_id AS warehouse_id "
                + "FROM out_final ofi INNER JOIN erp_sale_out so ON so.id=ofi.out_id AND so.tenant_id=ofi.tenant_id "
                + "AND so.deleted=b'0' AND so.status=20 INNER JOIN erp_customer c ON c.id=so.customer_id "
                + "AND c.tenant_id=so.tenant_id AND c.deleted=b'0' " + commonJoins("ofi", "so")
                + stockRecordCostJoin("ofi", "so", 50)
                + " WHERE ofi.tenant_id=:tenant" + documentScope + partyScope;
    }

    private String returnSelect(String documentScope, String partyScope) {
        return "SELECT CONCAT('RETURN-',sri.id),sri.id,sr.id,"
                + "CASE WHEN sr.return_mode=10 THEN sr.source_out_id END,"
                + "CASE WHEN sr.return_mode=10 THEN 'SALE_OUT' END,sri.product_id,sr.customer_id,'SALE_RETURN','销售退货',"
                + "sr.approve_time,sr.no,CASE WHEN sr.return_mode=10 THEN sr.source_out_no END,source_out.third_party_no,"
                + "p.code,p.name,p.vehicle_model,p.feature_code,-COALESCE(sri.count,0),sri.product_price,sri.product_price,"
                + "ROUND(-COALESCE(sri.count,0)*COALESCE(sri.product_price,0),2),NULL,'不适用',"
                + "cost_record.cost_price,CASE WHEN cost_record.cost_price IS NULL THEN NULL "
                + "ELSE ROUND(-COALESCE(sri.count,0)*cost_record.cost_price,2) END,NULL,NULL,"
                + "p.origin_place,p.brand,c.name,c.code,c.area_id,d.name,w.name,sri.warehouse_position,su.nickname,p.standard,"
                + "p.drawing_no,u.name,'否',cu.nickname,sri.remark,sr.remark,p.factory_code,NULL,NULL,NULL,NULL,sr.dept_id,sri.warehouse_id "
                + "FROM erp_sale_return_items sri INNER JOIN erp_sale_return sr ON sr.id=sri.return_id "
                + "AND sr.tenant_id=sri.tenant_id AND sr.deleted=b'0' AND sr.status=20 "
                + "INNER JOIN erp_customer c ON c.id=sr.customer_id AND c.tenant_id=sr.tenant_id AND c.deleted=b'0' "
                + "LEFT JOIN erp_sale_out source_out ON source_out.id=sr.source_out_id AND source_out.tenant_id=sr.tenant_id AND source_out.deleted=b'0' "
                + commonJoins("sri", "sr") + stockRecordCostJoin("sri", "sr", 60)
                + " WHERE sri.tenant_id=:tenant AND sri.deleted=b'0'" + documentScope + partyScope;
    }

    private String adjustSelect(String documentScope, String partyScope) {
        return "SELECT CONVERT(CONCAT('ADJUST-',sai.id) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "sai.id,sa.id,source_out.id,'SALE_OUT',sai.product_id,sa.customer_id,'SALE_ADJUST','销售调价',sa.approve_time,"
                + "CONVERT(sa.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(sai.sale_out_no,''),sa.original_sale_out_no) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(source_out.third_party_no USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(sai.part_code,''),p.code) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(sai.part_name,''),p.name) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(sai.vehicle_model,''),source_item.vehicle_model,p.vehicle_model) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(p.feature_code USING utf8mb4) COLLATE utf8mb4_unicode_ci,0,sai.old_price,sai.old_price,"
                + "ROUND(COALESCE(sai.adjust_price,0),2),NULL,'不适用',cost_record.cost_price,"
                + "CAST(0 AS DECIMAL(20,2)),NULL,NULL,"
                + "CONVERT(COALESCE(NULLIF(sai.origin_place,''),source_item.origin_place,p.origin_place) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(sai.brand,''),source_item.brand,p.brand) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "c.name,c.code,c.area_id,d.name,w.name,CONVERT(source_item.warehouse_position USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "su.nickname,CONVERT(COALESCE(NULLIF(source_item.standard,''),p.standard) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(source_item.drawing_no,''),p.drawing_no) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(COALESCE(NULLIF(sai.unit,''),u.name) USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CASE WHEN source_item.gift_flag=b'1' THEN '是' ELSE '否' END,cu.nickname,"
                + "CONVERT(sai.item_remark USING utf8mb4) COLLATE utf8mb4_unicode_ci,"
                + "CONVERT(sa.remark USING utf8mb4) COLLATE utf8mb4_unicode_ci,p.factory_code,NULL,NULL,NULL,NULL,"
                + "sa.dept_id,source_item.warehouse_id "
                + "FROM erp_sale_price_adjust_item sai INNER JOIN erp_sale_price_adjust sa ON sa.id=sai.adjust_id "
                + "AND sa.tenant_id=sai.tenant_id AND sa.deleted=b'0' AND sa.status=20 "
                + "INNER JOIN erp_customer c ON c.id=sa.customer_id AND c.tenant_id=sa.tenant_id AND c.deleted=b'0' "
                + "LEFT JOIN erp_sale_out_items source_item ON source_item.id=sai.sale_out_item_id AND source_item.tenant_id=sai.tenant_id AND source_item.deleted=b'0' "
                + "LEFT JOIN erp_sale_out source_out ON source_out.id=source_item.out_id AND source_out.tenant_id=sai.tenant_id AND source_out.deleted=b'0' "
                + "LEFT JOIN system_dept d ON d.id=sa.dept_id AND d.tenant_id=sa.tenant_id AND d.deleted=b'0' "
                + "LEFT JOIN system_users su ON su.id=sa.adjust_user_id AND su.tenant_id=sa.tenant_id AND su.deleted=b'0' "
                + "LEFT JOIN system_users cu ON cu.id=sa.creator AND cu.tenant_id=sa.tenant_id AND cu.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=sai.product_id AND p.tenant_id=sai.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_product_unit u ON u.id=source_item.product_unit_id AND u.tenant_id=sai.tenant_id AND u.deleted=b'0' "
                + "LEFT JOIN erp_warehouse w ON w.id=source_item.warehouse_id AND w.tenant_id=sai.tenant_id AND w.deleted=b'0' "
                + stockRecordCostJoin("source_item", "source_out", 50)
                + " WHERE sai.tenant_id=:tenant AND sai.deleted=b'0'" + documentScope + partyScope;
    }

    private String commonJoins(String itemAlias, String documentAlias) {
        return " LEFT JOIN system_dept d ON d.id=" + documentAlias + ".dept_id AND d.tenant_id=" + itemAlias + ".tenant_id AND d.deleted=b'0'"
                + " LEFT JOIN system_users su ON su.id=" + documentAlias + ".sale_user_id AND su.tenant_id=" + itemAlias + ".tenant_id AND su.deleted=b'0'"
                + " LEFT JOIN system_users cu ON cu.id=" + documentAlias + ".creator AND cu.tenant_id=" + itemAlias + ".tenant_id AND cu.deleted=b'0'"
                + " LEFT JOIN erp_product p ON p.id=" + itemAlias + ".product_id AND p.tenant_id=" + itemAlias + ".tenant_id AND p.deleted=b'0'"
                + " LEFT JOIN erp_product_unit u ON u.id=" + itemAlias + ".product_unit_id AND u.tenant_id=" + itemAlias + ".tenant_id AND u.deleted=b'0'"
                + " LEFT JOIN erp_warehouse w ON w.id=" + itemAlias + ".warehouse_id AND w.tenant_id=" + itemAlias + ".tenant_id AND w.deleted=b'0'";
    }

    private String stockRecordCostJoin(String itemAlias, String documentAlias, int bizType) {
        return " LEFT JOIN erp_stock_record cost_record ON cost_record.id=(SELECT MAX(cost_candidate.id)"
                + " FROM erp_stock_record cost_candidate WHERE cost_candidate.tenant_id=" + itemAlias + ".tenant_id"
                + " AND cost_candidate.deleted=b'0' AND cost_candidate.biz_type=" + bizType
                + " AND cost_candidate.biz_id=" + documentAlias + ".id AND cost_candidate.biz_item_id=" + itemAlias + ".id"
                + " AND cost_candidate.product_id=" + itemAlias + ".product_id"
                + " AND cost_candidate.warehouse_id=" + itemAlias + ".warehouse_id)"
                + " AND cost_record.tenant_id=" + itemAlias + ".tenant_id AND cost_record.deleted=b'0'";
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
        like(sql, "report.third_party_no", "thirdPartyNo", filter.getThirdPartyNo());
        equal(sql, "report.customer_id", "customerId", filter.getCustomerId());
        equal(sql, "report.dept_id", "deptId", filter.getDeptId());
        equal(sql, "report.warehouse_id", "warehouseId", filter.getWarehouseId());
        like(sql, "report.sale_user_name", "saleUserKeyword", filter.getSaleUserKeyword());
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
        fields.put("customerName", "report.customer_name");
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

    private String customerScope(ErpFinanceVisibleScope scope, String alias, String prefix,
                                 Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(alias).append(".dept_id IN (:").append(prefix).append("DeptIds) OR (")
                    .append(alias).append(".allow_multi_dept=b'1' AND EXISTS (SELECT 1 FROM erp_customer_dept ecd ")
                    .append("WHERE ecd.tenant_id=").append(alias).append(".tenant_id AND ecd.customer_id=")
                    .append(alias).append(".id AND ecd.deleted=b'0' AND ecd.dept_id IN (:")
                    .append(prefix).append("DeptIds)))");
            params.put(prefix + "DeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(alias).append(".creator=:").append(prefix).append("Self");
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
        private final ErpFinanceVisibleScope outScope;
        private final ErpFinanceVisibleScope returnScope;
        private final ErpFinanceVisibleScope adjustScope;
        private final ErpFinanceVisibleScope customerScope;

        public Scope(long tenantId, ErpFinanceVisibleScope outScope, ErpFinanceVisibleScope returnScope,
                     ErpFinanceVisibleScope adjustScope, ErpFinanceVisibleScope customerScope) {
            this.tenantId = tenantId;
            this.outScope = outScope;
            this.returnScope = returnScope;
            this.adjustScope = adjustScope;
            this.customerScope = customerScope;
        }
    }

    private static class Sql {
        private String cte;
        private String body;
        private final Map<String, Object> params = new HashMap<>();
    }
}
