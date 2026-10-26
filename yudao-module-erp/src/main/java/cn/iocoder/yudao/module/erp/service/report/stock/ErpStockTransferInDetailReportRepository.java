package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 调拨入库逐明细查询；分页、汇总均以同一条受权限约束的明细 SQL 为准。 */
@Repository
public class ErpStockTransferInDetailReportRepository {

    private static final int TRANSFER_IN = 20;
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
                        + "COALESCE(SUM(report.cost_amount),0) total_cost_amount " + sql.body,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    Sql sql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenantId", scope.tenantId);
        sql.params.put("transferIn", TRANSFER_IN);
        sql.params.put("approved", APPROVED);
        String permission = permission(scope.permissionScope, sql.params);
        sql.body = "FROM (SELECT sm.id move_id,smi.id item_id,smi.product_id,"
                + "smi.from_warehouse_id,smi.to_warehouse_id,sm.move_time move_date,sm.no document_no,"
                + "p.code product_code,p.name product_name,p.vehicle_model,p.origin_place,"
                + "fw.name from_warehouse_name,smi.count,smi.product_price,"
                + "COALESCE(smi.total_price,ROUND(smi.count*smi.product_price,6)) total_amount,smi.product_price cost_unit_price,"
                + "COALESCE(smi.total_price,ROUND(smi.count*smi.product_price,6)) cost_amount,u.nickname creator_name,"
                + "tw.name to_warehouse_name,p.standard,p.drawing_no,sm.creator "
                + "FROM erp_stock_move_item smi INNER JOIN erp_stock_move sm ON sm.id=smi.move_id "
                + "AND sm.tenant_id=smi.tenant_id AND sm.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=smi.product_id AND p.tenant_id=smi.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_warehouse fw ON fw.id=smi.from_warehouse_id AND fw.tenant_id=smi.tenant_id AND fw.deleted=b'0' "
                + "LEFT JOIN erp_warehouse tw ON tw.id=smi.to_warehouse_id AND tw.tenant_id=smi.tenant_id AND tw.deleted=b'0' "
                + "LEFT JOIN system_users u ON u.id=sm.creator AND u.tenant_id=sm.tenant_id AND u.deleted=b'0' "
                + "WHERE smi.tenant_id=:tenantId AND smi.deleted=b'0' "
                + "AND sm.transfer_direction=:transferIn AND sm.status=:approved" + permission
                + ") report WHERE 1=1";
        range(sql, "report.move_date", "moveDate", filter.getMoveDateFrom(), filter.getMoveDateTo());
        like(sql, "report.document_no", "documentNo", filter.getDocumentNo());
        equal(sql, "report.from_warehouse_id", "fromWarehouseId", filter.getFromWarehouseId());
        equal(sql, "report.to_warehouse_id", "toWarehouseId", filter.getToWarehouseId());
        range(sql, "report.count", "count", filter.getCountMin(), filter.getCountMax());
        range(sql, "report.product_price", "price", filter.getPriceMin(), filter.getPriceMax());
        range(sql, "report.total_amount", "amount", filter.getAmountMin(), filter.getAmountMax());
        equal(sql, "report.creator", "creatorId", filter.getCreatorId() == null ? null
                : String.valueOf(filter.getCreatorId()));
        like(sql, "report.vehicle_model", "vehicleModel", filter.getVehicleModel());
        like(sql, "report.origin_place", "originPlace", filter.getOriginPlace());
        like(sql, "report.standard", "standard", filter.getStandard());
        like(sql, "report.drawing_no", "drawingNo", filter.getDrawingNo());
        productKeyword(sql, filter.getProductKeyword(), scope.productKeywordColumns);
        return sql;
    }

    private String permission(ErpStockTransferOutPermissionScope scope, Map<String, Object> params) {
        if (scope != null && scope.isAll()) {
            return "";
        }
        if (scope == null || scope.getDeptIds().isEmpty()) {
            return " AND 1=0";
        }
        params.put("deptIds", scope.getDeptIds());
        return " AND (sm.dept_id IN (:deptIds) OR sm.from_dept_id IN (:deptIds) OR sm.to_dept_id IN (:deptIds)"
                + " OR smi.from_dept_id IN (:deptIds) OR smi.to_dept_id IN (:deptIds))";
    }

    private void productKeyword(Sql sql, String keyword, List<String> columns) {
        if (!StringUtils.hasText(keyword) || columns == null || columns.isEmpty()) {
            return;
        }
        sql.params.put("productKeyword", "%" + keyword.trim().replaceAll("\\s+", "%") + "%");
        List<String> predicates = new ArrayList<>();
        for (String column : columns) {
            predicates.add("report." + column + " LIKE :productKeyword");
        }
        sql.body += " AND (" + String.join(" OR ", predicates) + ")";
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("moveDate", "report.move_date");
        fields.put("documentNo", "report.document_no");
        fields.put("productCode", "report.product_code");
        fields.put("productName", "report.product_name");
        fields.put("vehicleModel", "report.vehicle_model");
        fields.put("originPlace", "report.origin_place");
        fields.put("fromWarehouseName", "report.from_warehouse_name");
        fields.put("count", "report.count");
        fields.put("productPrice", "report.product_price");
        fields.put("totalAmount", "report.total_amount");
        fields.put("costUnitPrice", "report.cost_unit_price");
        fields.put("costAmount", "report.cost_amount");
        fields.put("creatorName", "report.creator_name");
        fields.put("toWarehouseName", "report.to_warehouse_name");
        fields.put("standard", "report.standard");
        fields.put("drawingNo", "report.drawing_no");
        String field = fields.get(filter.getOrderField());
        if (field == null) {
            return " ORDER BY report.move_date DESC,report.document_no DESC,report.item_id ASC";
        }
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction
                + ",report.move_date DESC,report.document_no DESC,report.item_id ASC";
    }

    private void like(Sql sql, String column, String name, String value) {
        if (!StringUtils.hasText(value)) return;
        sql.body += " AND " + column + " LIKE :" + name;
        sql.params.put(name, "%" + value.trim() + "%");
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null) return;
        sql.body += " AND " + column + "=:" + name;
        sql.params.put(name, value);
    }

    private void range(Sql sql, String column, String name, Object min, Object max) {
        if (min != null) {
            sql.body += " AND " + column + ">=:" + name + "Min";
            sql.params.put(name + "Min", min);
        }
        if (max != null) {
            sql.body += " AND " + column + "<=:" + name + "Max";
            sql.params.put(name + "Max", max);
        }
    }

    public static final class Scope {
        private final long tenantId;
        private final ErpStockTransferOutPermissionScope permissionScope;
        private final List<String> productKeywordColumns;

        public Scope(long tenantId, ErpStockTransferOutPermissionScope permissionScope,
                     List<String> productKeywordColumns) {
            this.tenantId = tenantId;
            this.permissionScope = permissionScope;
            this.productKeywordColumns = productKeywordColumns;
        }
    }

    static final class Sql {
        private String body;
        private final Map<String, Object> params = new HashMap<>();
    }
}
