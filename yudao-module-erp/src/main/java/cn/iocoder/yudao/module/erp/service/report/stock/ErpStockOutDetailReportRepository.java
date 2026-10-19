package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockOutDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockOutDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockOutDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 其他出库逐明细查询；分页、汇总和导出共用同一受权限约束的 SQL。 */
@Repository
public class ErpStockOutDetailReportRepository {

    private static final int APPROVED = 20;

    @Resource
    private NamedParameterJdbcTemplate jdbc;

    public long count(Filter filter, Scope scope) {
        Sql sql = sql(filter, scope);
        Long value = jdbc.queryForObject("SELECT COUNT(*) " + sql.body, sql.params, Long.class);
        return value == null ? 0L : value;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = sql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc.query("SELECT report.* " + sql.body + orderBy(filter)
                        + " LIMIT :limit OFFSET :offset", sql.params,
                new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = sql(filter, scope);
        return jdbc.queryForObject("SELECT COUNT(*) row_count,"
                        + "COALESCE(SUM(report.count),0) total_count,"
                        + "COALESCE(SUM(report.total_amount),0) total_amount,"
                        + "CASE WHEN COALESCE(SUM(report.cost_missing),0)>0 THEN NULL "
                        + "ELSE COALESCE(SUM(report.cost_amount),0) END total_cost_amount,"
                        + "COALESCE(SUM(report.product_amount),0) total_product_amount,"
                        + "COALESCE(SUM(report.cancel_count),0) total_cancel_count,"
                        + "COALESCE(SUM(report.after_cancel_count),0) total_after_cancel_count,"
                        + "COALESCE(SUM(report.after_cancel_amount),0) total_after_cancel_amount,"
                        + "COALESCE(SUM(report.cost_missing),0) missing_cost_count " + sql.body,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    Sql sql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenantId", scope.tenantId);
        sql.params.put("approved", APPROVED);
        sql.params.put("otherOut", ErpStockRecordBizTypeEnum.OTHER_OUT.getType());
        String permission = permission(scope, sql.params);
        sql.body = "FROM (SELECT so.id stock_out_id,soi.id item_id,soi.warehouse_id,"
                + "so.customer_id related_party_id,soi.product_id,p.category_id,so.dept_id,"
                + "so.creator business_user_id,w.name warehouse_name,so.no document_no,"
                + "customer.name related_party_name,p.code product_code,p.drawing_no,p.name product_name,"
                + "p.vehicle_model,p.feature_code,soi.count,soi.product_price unit_price,"
                + "soi.total_price total_amount,sr.unit_price cost_unit_price,ABS(sr.total_price) cost_amount,"
                + "soi.product_price product_price,soi.total_price product_amount,soi.count cancel_count,"
                + "soi.count after_cancel_count,soi.total_price after_cancel_amount,so.out_time document_time,"
                + "so.approve_time,CAST(NULL AS CHAR) source_no,p.brand,p.origin_place,p.standard,"
                + "category.name product_category_name,stock.shelf,CAST(NULL AS CHAR) document_category,"
                + "creator.nickname business_user_name,creator.nickname creator_name,dept.name dept_name,"
                + "soi.remark item_remark,so.creator,"
                + "CASE WHEN sr.id IS NULL OR sr.unit_price IS NULL OR sr.total_price IS NULL THEN 1 ELSE 0 END cost_missing "
                + "FROM erp_stock_out_item soi "
                + "INNER JOIN erp_stock_out so ON so.id=soi.out_id AND so.tenant_id=soi.tenant_id "
                + "AND so.deleted=b'0' "
                + "LEFT JOIN erp_warehouse w ON w.id=soi.warehouse_id AND w.tenant_id=soi.tenant_id "
                + "AND w.deleted=b'0' "
                + "LEFT JOIN erp_customer customer ON customer.id=so.customer_id "
                + "AND customer.tenant_id=so.tenant_id AND customer.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=soi.product_id AND p.tenant_id=soi.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_product_category category ON category.id=p.category_id "
                + "AND category.tenant_id=soi.tenant_id AND category.deleted=b'0' "
                + "LEFT JOIN erp_stock stock ON stock.id=(SELECT MAX(stock_match.id) FROM erp_stock stock_match "
                + "WHERE stock_match.tenant_id=soi.tenant_id AND stock_match.product_id=soi.product_id "
                + "AND stock_match.warehouse_id=soi.warehouse_id AND stock_match.deleted=b'0') "
                + "LEFT JOIN erp_stock_record sr ON sr.id=(SELECT MAX(sr_match.id) FROM erp_stock_record sr_match "
                + "WHERE sr_match.tenant_id=soi.tenant_id AND sr_match.deleted=b'0' "
                + "AND sr_match.biz_type=:otherOut AND sr_match.biz_id=so.id AND sr_match.biz_item_id=soi.id) "
                + "LEFT JOIN system_dept dept ON dept.id=so.dept_id AND dept.tenant_id=so.tenant_id "
                + "AND dept.deleted=b'0' "
                + "LEFT JOIN system_users creator ON creator.id=so.creator "
                + "AND creator.tenant_id=so.tenant_id AND creator.deleted=b'0' "
                + "WHERE soi.tenant_id=:tenantId AND soi.deleted=b'0' AND so.status=:approved"
                + permission + ") report WHERE 1=1";
        range(sql, "report.approve_time", "approveTime", filter.getApproveTimeFrom(), filter.getApproveTimeTo());
        range(sql, "report.document_time", "documentTime", filter.getDocumentTimeFrom(), filter.getDocumentTimeTo());
        equal(sql, "report.warehouse_id", "warehouseId", filter.getWarehouseId());
        equal(sql, "report.related_party_id", "relatedPartyId", filter.getRelatedPartyId());
        equal(sql, "report.category_id", "categoryId", filter.getCategoryId());
        equal(sql, "report.dept_id", "deptId", filter.getDeptId());
        if (filter.getBusinessUserId() != null) {
            equal(sql, "report.creator", "businessUserId", String.valueOf(filter.getBusinessUserId()));
        }
        keyword(sql, filter.getKeyword(), scope.keywordColumns);
        return sql;
    }

    private String permission(Scope scope, Map<String, Object> params) {
        if (scope.all) return "";
        List<String> predicates = new ArrayList<>();
        if (scope.deptIds != null && !scope.deptIds.isEmpty()) {
            params.put("scopeDeptIds", scope.deptIds);
            predicates.add("so.dept_id IN (:scopeDeptIds)");
        }
        if (scope.self && scope.userId != null) {
            params.put("scopeCreator", String.valueOf(scope.userId));
            predicates.add("so.creator=:scopeCreator");
        }
        return predicates.isEmpty() ? " AND 1=0" : " AND (" + String.join(" OR ", predicates) + ")";
    }

    private void keyword(Sql sql, String keyword, List<String> columns) {
        if (!StringUtils.hasText(keyword) || columns == null || columns.isEmpty()) return;
        sql.params.put("keyword", "%" + keyword.trim().replaceAll("\\s+", "%") + "%");
        List<String> predicates = new ArrayList<>();
        for (String column : columns) predicates.add("report." + column + " LIKE :keyword");
        sql.body += " AND (" + String.join(" OR ", predicates) + ")";
    }

    String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("warehouseName", "report.warehouse_name");
        fields.put("documentNo", "report.document_no");
        fields.put("relatedPartyName", "report.related_party_name");
        fields.put("productCode", "report.product_code");
        fields.put("drawingNo", "report.drawing_no");
        fields.put("productName", "report.product_name");
        fields.put("vehicleModel", "report.vehicle_model");
        fields.put("featureCode", "report.feature_code");
        fields.put("count", "report.count");
        fields.put("unitPrice", "report.unit_price");
        fields.put("totalAmount", "report.total_amount");
        fields.put("costUnitPrice", "report.cost_unit_price");
        fields.put("costAmount", "report.cost_amount");
        fields.put("productPrice", "report.product_price");
        fields.put("productAmount", "report.product_amount");
        fields.put("cancelCount", "report.cancel_count");
        fields.put("afterCancelCount", "report.after_cancel_count");
        fields.put("afterCancelAmount", "report.after_cancel_amount");
        fields.put("documentTime", "report.document_time");
        fields.put("approveTime", "report.approve_time");
        fields.put("brand", "report.brand");
        fields.put("originPlace", "report.origin_place");
        fields.put("standard", "report.standard");
        fields.put("productCategoryName", "report.product_category_name");
        fields.put("shelf", "report.shelf");
        fields.put("businessUserName", "report.business_user_name");
        fields.put("creatorName", "report.creator_name");
        fields.put("deptName", "report.dept_name");
        fields.put("itemRemark", "report.item_remark");
        String field = fields.get(filter.getOrderField());
        if (field == null) {
            return " ORDER BY report.approve_time DESC,report.document_no DESC,report.item_id ASC";
        }
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction
                + ",report.approve_time DESC,report.document_no DESC,report.item_id ASC";
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null) return;
        sql.body += " AND " + column + "=:" + name;
        sql.params.put(name, value);
    }

    private void range(Sql sql, String column, String name, Object min, Object max) {
        if (min != null) {
            sql.body += " AND " + column + ">=:" + name + "From";
            sql.params.put(name + "From", min);
        }
        if (max != null) {
            sql.body += " AND " + column + "<=:" + name + "To";
            sql.params.put(name + "To", max);
        }
    }

    public static final class Scope {
        private final long tenantId;
        private final boolean all;
        private final Collection<Long> deptIds;
        private final boolean self;
        private final Long userId;
        private final List<String> keywordColumns;

        public Scope(long tenantId, boolean all, Collection<Long> deptIds, boolean self, Long userId,
                     List<String> keywordColumns) {
            this.tenantId = tenantId;
            this.all = all;
            this.deptIds = deptIds;
            this.self = self;
            this.userId = userId;
            this.keywordColumns = keywordColumns;
        }
    }

    static final class Sql {
        private String body;
        private final Map<String, Object> params = new HashMap<>();
    }
}
