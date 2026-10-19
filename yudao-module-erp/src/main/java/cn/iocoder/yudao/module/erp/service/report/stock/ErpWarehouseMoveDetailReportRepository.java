package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpWarehouseMoveDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpWarehouseMoveDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpWarehouseMoveDetailReportModels.Summary;
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

/** 仓库移货逐明细查询；分页、汇总和导出共用同一受权限约束的 SQL。 */
@Repository
public class ErpWarehouseMoveDetailReportRepository {

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
                        + "COALESCE(SUM(report.cost_amount),0) total_cost_amount,"
                        + "COALESCE(SUM(report.move_amount),0) total_move_amount " + sql.body,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    Sql sql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenantId", scope.tenantId);
        sql.params.put("approved", APPROVED);
        String permission = permission(scope, sql.params);
        sql.body = "FROM (SELECT wm.id move_id,wmi.id item_id,wmi.product_id,"
                + "wmi.from_warehouse_id,wmi.to_warehouse_id,p.code product_code,wm.no document_no,"
                + "wm.approve_time,p.name product_name,p.brand,p.vehicle_model,p.origin_place,"
                + "pu.name unit_name,wmi.count,wmi.cost_price,wmi.cost_amount,"
                + "wmi.product_price move_price,wmi.total_price move_amount,"
                + "fw.name from_warehouse_name,wmi.from_shelf,tw.name to_warehouse_name,wmi.to_shelf,"
                + "creator.nickname creator_name,approver.nickname approve_user_name,"
                + "p.standard,p.drawing_no,p.feature_code,wmi.remark item_remark,wm.creator "
                + "FROM erp_warehouse_move_item wmi "
                + "INNER JOIN erp_warehouse_move wm ON wm.id=wmi.move_id AND wm.tenant_id=wmi.tenant_id "
                + "AND wm.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=wmi.product_id AND p.tenant_id=wmi.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_product_unit pu ON pu.id=COALESCE(wmi.product_unit_id,p.unit_id) "
                + "AND pu.tenant_id=wmi.tenant_id AND pu.deleted=b'0' "
                + "LEFT JOIN erp_warehouse fw ON fw.id=wmi.from_warehouse_id "
                + "AND fw.tenant_id=wmi.tenant_id AND fw.deleted=b'0' "
                + "LEFT JOIN erp_warehouse tw ON tw.id=wmi.to_warehouse_id "
                + "AND tw.tenant_id=wmi.tenant_id AND tw.deleted=b'0' "
                + "LEFT JOIN system_users creator ON creator.id=wm.creator "
                + "AND creator.tenant_id=wm.tenant_id AND creator.deleted=b'0' "
                + "LEFT JOIN system_users approver ON approver.id=wm.approve_user_id "
                + "AND approver.tenant_id=wm.tenant_id AND approver.deleted=b'0' "
                + "WHERE wmi.tenant_id=:tenantId AND wmi.deleted=b'0' AND wm.status=:approved"
                + permission + ") report WHERE 1=1";
        range(sql, "report.approve_time", "approveTime", filter.getApproveTimeFrom(), filter.getApproveTimeTo());
        equal(sql, "report.from_warehouse_id", "fromWarehouseId", filter.getFromWarehouseId());
        equal(sql, "report.to_warehouse_id", "toWarehouseId", filter.getToWarehouseId());
        keyword(sql, filter.getKeyword(), scope.keywordColumns);
        return sql;
    }

    private String permission(Scope scope, Map<String, Object> params) {
        if (scope.all) return "";
        List<String> predicates = new ArrayList<>();
        if (scope.deptIds != null && !scope.deptIds.isEmpty()) {
            params.put("deptIds", scope.deptIds);
            predicates.add("wm.dept_id IN (:deptIds)");
        }
        if (scope.self && scope.userId != null) {
            params.put("selfCreator", String.valueOf(scope.userId));
            predicates.add("wm.creator=:selfCreator");
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
        fields.put("productCode", "report.product_code");
        fields.put("documentNo", "report.document_no");
        fields.put("approveTime", "report.approve_time");
        fields.put("productName", "report.product_name");
        fields.put("brand", "report.brand");
        fields.put("vehicleModel", "report.vehicle_model");
        fields.put("originPlace", "report.origin_place");
        fields.put("unitName", "report.unit_name");
        fields.put("count", "report.count");
        fields.put("costPrice", "report.cost_price");
        fields.put("costAmount", "report.cost_amount");
        fields.put("movePrice", "report.move_price");
        fields.put("moveAmount", "report.move_amount");
        fields.put("fromWarehouseName", "report.from_warehouse_name");
        fields.put("fromShelf", "report.from_shelf");
        fields.put("toWarehouseName", "report.to_warehouse_name");
        fields.put("toShelf", "report.to_shelf");
        fields.put("creatorName", "report.creator_name");
        fields.put("approveUserName", "report.approve_user_name");
        fields.put("standard", "report.standard");
        fields.put("drawingNo", "report.drawing_no");
        fields.put("featureCode", "report.feature_code");
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
