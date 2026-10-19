package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockInDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockInDetailReportModels.Summary;
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

/** 其他入库逐明细查询；分页、汇总和导出共用同一受权限约束的 SQL。 */
@Repository
public class ErpStockInDetailReportRepository {

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
                        + "COALESCE(SUM(report.total_amount),0) total_amount " + sql.body,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    Sql sql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenantId", scope.tenantId);
        sql.params.put("approved", APPROVED);
        String permission = permission(scope, sql.params);
        sql.body = "FROM (SELECT si.id stock_in_id,sii.id item_id,sii.warehouse_id,si.supplier_id related_party_id,"
                + "sii.product_id,p.category_id,si.dept_id,si.creator business_user_id,"
                + "w.name warehouse_name,si.no document_no,supplier.name related_party_name,p.code product_code,"
                + "p.drawing_no,p.name product_name,p.vehicle_model,sii.count,sii.product_price,"
                + "sii.total_price total_amount,p.brand,p.origin_place,p.standard,category.name category_name,"
                + "stock.shelf,si.approve_time,si.in_time document_time,sii.remark item_remark,"
                + "dept.name dept_name,business_user.nickname business_user_name,si.creator "
                + "FROM erp_stock_in_item sii "
                + "INNER JOIN erp_stock_in si ON si.id=sii.in_id AND si.tenant_id=sii.tenant_id "
                + "AND si.deleted=b'0' "
                + "LEFT JOIN erp_warehouse w ON w.id=sii.warehouse_id AND w.tenant_id=sii.tenant_id "
                + "AND w.deleted=b'0' "
                + "LEFT JOIN erp_supplier supplier ON supplier.id=si.supplier_id AND supplier.tenant_id=si.tenant_id "
                + "AND supplier.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=sii.product_id AND p.tenant_id=sii.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_product_category category ON category.id=p.category_id "
                + "AND category.tenant_id=sii.tenant_id AND category.deleted=b'0' "
                + "LEFT JOIN erp_stock stock ON stock.id=(SELECT MAX(stock_match.id) FROM erp_stock stock_match "
                + "WHERE stock_match.tenant_id=sii.tenant_id AND stock_match.product_id=sii.product_id "
                + "AND stock_match.warehouse_id=sii.warehouse_id AND stock_match.deleted=b'0') "
                + "LEFT JOIN system_dept dept ON dept.id=si.dept_id AND dept.tenant_id=si.tenant_id "
                + "AND dept.deleted=b'0' "
                + "LEFT JOIN system_users business_user ON business_user.id=si.creator "
                + "AND business_user.tenant_id=si.tenant_id AND business_user.deleted=b'0' "
                + "WHERE sii.tenant_id=:tenantId AND sii.deleted=b'0' AND si.status=:approved"
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
            predicates.add("si.dept_id IN (:scopeDeptIds)");
        }
        if (scope.self && scope.userId != null) {
            params.put("scopeCreator", String.valueOf(scope.userId));
            predicates.add("si.creator=:scopeCreator");
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
        fields.put("count", "report.count");
        fields.put("productPrice", "report.product_price");
        fields.put("totalAmount", "report.total_amount");
        fields.put("brand", "report.brand");
        fields.put("originPlace", "report.origin_place");
        fields.put("standard", "report.standard");
        fields.put("categoryName", "report.category_name");
        fields.put("shelf", "report.shelf");
        fields.put("approveTime", "report.approve_time");
        fields.put("documentTime", "report.document_time");
        fields.put("itemRemark", "report.item_remark");
        fields.put("deptName", "report.dept_name");
        fields.put("businessUserName", "report.business_user_name");
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
