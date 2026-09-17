package cn.iocoder.yudao.module.erp.service.stock.report;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.*;
import lombok.Data;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/** 分页、全部合计、导出共用本仓储的过滤SQL。所有值均参数绑定。 */
@Repository
public class ErpStockReportV2Repository {
    @Resource private JdbcTemplate jdbcTemplate;

    @Data
    public static class Scope {
        private long tenantId;
        private long userId;
        private boolean all;
        private boolean self;
        private Set<Long> deptIds = Collections.emptySet();
        private Set<Long> warehouseIds = Collections.emptySet();
        private Set<Long> wholeWarehouseIds = Collections.emptySet();
        private Set<Long> selfWarehouseIds = Collections.emptySet();
    }

    public void checkSchema() {
        jdbcTemplate.queryForList("SELECT tenant_id,stock_id,product_id,warehouse_id,origin_kind,available_from FROM erp_stock_dual_cost_origin WHERE 1=0");
        jdbcTemplate.queryForList("SELECT tenant_id,stock_id,stock_dept_id,quantity,opening_quantity,opening_financial_amount,"
                + "opening_settlement_amount,cutover_at,updated_at,legacy_record_id,legacy_cost_price,legacy_cost_amount "
                + "FROM erp_stock_dual_cost_balance WHERE 1=0");
        jdbcTemplate.queryForList("SELECT id,tenant_id,stock_id,product_id,warehouse_id,stock_dept_id,accounting_dept_id,"
                + "posted_by,biz_type,biz_id,biz_item_id,biz_no,batch_no,source_biz_type,source_biz_id,source_biz_item_id,"
                + "reversal_posting_id,posted_at,biz_date,quantity,financial_movement,settlement_movement,"
                + "balance_quantity,financial_balance,settlement_balance FROM erp_stock_dual_cost_posting WHERE 1=0");
    }

    public Set<Long> warehouseIds(long tenant, Collection<Long> departmentIds, boolean all) {
        Map<String,Object> args = new HashMap<>(); args.put("tenant",tenant); args.put("depts",departmentIds);
        if (!all && departmentIds.isEmpty()) return Collections.emptySet();
        // 报表保留停用/逻辑删除仓库历史，仅按原stock模块的仓库归属权限确定范围。
        return new LinkedHashSet<>(jdbc().queryForList("SELECT id FROM erp_warehouse WHERE tenant_id=:tenant"
                + (all ? "" : " AND dept_id IN (:depts)"), args, Long.class));
    }

    public void requireOpeningStockReferences(long tenant) {
        Long missing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_balance b "
                + "LEFT JOIN erp_stock s ON s.id=b.stock_id AND s.tenant_id=b.tenant_id "
                + "WHERE b.tenant_id=? AND s.id IS NULL", Long.class, tenant);
        if (missing != null && missing > 0) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(409,
                "期初关联库存记录缺失，无法可靠恢复维度，请先核对后再输出报表");
    }

    private NamedParameterJdbcTemplate jdbc() { return new NamedParameterJdbcTemplate(jdbcTemplate); }

    public long movementCount(Filter f, Scope s) {
        Sql sql = movement(f, s);
        return jdbc().queryForObject("SELECT COUNT(*) " + sql.text, sql.params, Long.class);
    }

    public List<MovementRow> movementPage(Filter f, Scope s, long offset, int limit) {
        Sql sql = movement(f, s);
        sql.params.put("offset", offset); sql.params.put("limit", limit);
        return jdbc().query("SELECT p.id AS posting_id,p.stock_id,p.product_id,p.warehouse_id,p.stock_dept_id,"
                        + "p.accounting_dept_id,p.biz_type,p.biz_id,p.biz_item_id,p.biz_no,p.batch_no,"
                        + "p.source_biz_type,p.source_biz_id,p.source_biz_item_id,p.reversal_posting_id,p.posted_at,p.biz_date,"
                        + "p.quantity,p.financial_movement,p.settlement_movement,p.balance_quantity,p.financial_balance,p.settlement_balance,"
                        + "pr.code AS product_code,pr.name AS product_name,w.name AS warehouse_name "
                        + ",CASE WHEN b.stock_id IS NULL THEN NULL ELSE COALESCE(o.origin_kind,'MANUAL_OPENING') END AS origin_kind,"
                        + "CASE WHEN b.stock_id IS NULL THEN NULL ELSE COALESCE(o.available_from,b.cutover_at) END AS available_from "
                        + sql.text + movementOrder(f) + " LIMIT :limit OFFSET :offset", sql.params,
                new BeanPropertyRowMapper<>(MovementRow.class));
    }

    public Summary movementSummary(Filter f, Scope s) {
        Sql sql = movement(f, s);
        return jdbc().queryForObject("SELECT COUNT(*) AS row_count,"
                        + "COALESCE(SUM(GREATEST(p.quantity,0)),0) AS in_quantity,"
                        + "COALESCE(SUM(GREATEST(-p.quantity,0)),0) AS out_quantity,"
                        + "COALESCE(SUM(GREATEST(p.financial_movement,0)),0) AS financial_in_amount,"
                        + "COALESCE(SUM(GREATEST(-p.financial_movement,0)),0) AS financial_out_amount,"
                        + "COALESCE(SUM(GREATEST(p.settlement_movement,0)),0) AS settlement_in_amount,"
                        + "COALESCE(SUM(GREATEST(-p.settlement_movement,0)),0) AS settlement_out_amount "
                        + sql.text, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    public Set<Long> permissionDepartments(Filter f, Scope s, boolean movement) {
        Sql sql = movement ? movement(f, s) : stockScope(f, s, false);
        String query = movement
                ? "SELECT DISTINCT p.stock_dept_id AS dept_id,p.accounting_dept_id AS accounting_dept_id " + sql.text
                : "SELECT DISTINCT COALESCE(b.stock_dept_id,s.dept_id) AS dept_id,NULL AS accounting_dept_id " + sql.text;
        Set<Long> result = new LinkedHashSet<>();
        jdbc().query(query, sql.params, rs -> {
            if (rs.getObject("dept_id") != null) result.add(rs.getLong("dept_id"));
            if (rs.getObject("accounting_dept_id") != null) result.add(rs.getLong("accounting_dept_id"));
        });
        return result;
    }

    public List<BalanceRow> balancePage(Filter f, Scope s, LocalDateTime cutover, long offset, int limit) {
        Sql sql = balance(f, s, cutover, false);
        sql.params.put("offset", offset); sql.params.put("limit", limit);
        String order = "stockId".equals(f.getOrderField()) || f.getOrderField() == null ? "stock_id"
                : "productId".equals(f.getOrderField()) ? "product_id" : "warehouse_id";
        return jdbc().query(sql.text + " ORDER BY " + order + direction(f) + ",stock_id" + direction(f)
                        + " LIMIT :limit OFFSET :offset", sql.params, new BeanPropertyRowMapper<>(BalanceRow.class));
    }

    public Summary balanceSummary(Filter f, Scope s, LocalDateTime cutover, boolean movementCoverage) {
        Sql sql = balance(f, s, cutover, movementCoverage);
        StringBuilder select = new StringBuilder("SELECT COUNT(*) AS row_count,"
                + "COALESCE(SUM(data_status='MISSING_OPENING'),0) AS missing_opening_count,"
                + "COALESCE(SUM(data_status='BEFORE_CUTOVER'),0) AS outside_coverage_count,"
                + "COALESCE(SUM(data_status='STALE_OPENING'),0) AS stale_count");
        for (String field : AMOUNT_FIELDS) select.append(",COALESCE(SUM(").append(field).append("),0) AS ").append(field);
        return jdbc().queryForObject(select + " FROM (" + sql.text + ") totals", sql.params,
                new BeanPropertyRowMapper<>(Summary.class));
    }

    public long balanceCount(Filter f, Scope s) {
        Sql sql=stockScope(f,s,false);
        return jdbc().queryForObject("SELECT COUNT(*) "+sql.text,sql.params,Long.class);
    }

    public List<BizTypeAmount> businessTypeAmounts(Filter f,Scope scope,Collection<Long> stockIds,boolean summary) {
        Sql stocks=stockScope(f,scope,false);
        String selected="SELECT s.id,s.tenant_id,COALESCE(o.available_from,b.cutover_at) AS cutover_at "+stocks.text;
        String sql="SELECT "+(summary?"NULL":"p.stock_id")+" AS stock_id,p.biz_type,SUM(p.quantity) AS quantity,"
                +"SUM(p.financial_movement) AS financial_movement,SUM(p.settlement_movement) AS settlement_movement "
                +"FROM erp_stock_dual_cost_posting p JOIN ("+selected+") selected ON selected.id=p.stock_id AND selected.tenant_id=p.tenant_id "
                +"WHERE p.tenant_id=:tenant AND p.posted_at>=:from AND p.posted_at<:to AND p.posted_at>=selected.cutover_at ";
        if(stockIds!=null) {
            if(stockIds.isEmpty()) return Collections.emptyList();
            sql+=" AND p.stock_id IN (:selectedStockIds) ";stocks.params.put("selectedStockIds",stockIds);
        }
        // 不按正负取绝对值、不筛零数量；冲销独立bizType和未知bizType均保留。
        sql+=" GROUP BY "+(summary?"":"p.stock_id,")+"p.biz_type ORDER BY "+(summary?"":"p.stock_id,")+"p.biz_type";
        return jdbc().query(sql,stocks.params,new BeanPropertyRowMapper<>(BizTypeAmount.class));
    }

    public ReportOptions reportOptions(Scope scope) {
        Map<String,Object> args=new HashMap<>();args.put("tenant",scope.getTenantId());args.put("warehouses",scope.getWarehouseIds());
        args.put("allowedDepts",scope.getDeptIds());args.put("user",scope.getUserId());
        args.put("wholeWarehouses",scope.getWholeWarehouseIds());args.put("selfWarehouses",scope.getSelfWarehouseIds());
        String warehouseClause=scope.getWarehouseIds().isEmpty()?"1=0":"w.id IN (:warehouses)";
        List<HistoryOption> warehouses=jdbc().query("SELECT w.id,w.name,w.status,w.deleted AS archived FROM erp_warehouse w "
                +"WHERE w.tenant_id=:tenant AND "+warehouseClause+" ORDER BY w.id",args,new BeanPropertyRowMapper<>(HistoryOption.class));
        String departmentClause=scope.isAll()?"1=1":scope.getDeptIds().isEmpty()?"1=0":"d.id IN (:allowedDepts)";
        // 此选项不依赖新账表，关闭状态也不访问未迁移表；仅提供有完整部门范围的筛选项。
        List<HistoryOption> departments=jdbc().query("SELECT DISTINCT d.id,d.name,d.status,d.deleted AS archived FROM system_dept d "
                +"WHERE d.tenant_id=:tenant AND "+departmentClause+" AND (EXISTS (SELECT 1 FROM erp_warehouse w "
                +"WHERE w.tenant_id=:tenant AND w.dept_id=d.id AND "+warehouseClause+") OR EXISTS (SELECT 1 FROM erp_stock s "
                +"WHERE s.tenant_id=:tenant AND s.dept_id=d.id AND "+stockWarehouseCondition(scope,"s.warehouse_id","s.creator")+")) ORDER BY d.id",
                args,new BeanPropertyRowMapper<>(HistoryOption.class));
        return new ReportOptions().setWarehouses(warehouses).setStockDepartments(departments);
    }

    public List<ProductOption> productOptions(String keyword, Scope scope, long offset, int limit) {
        return productOptions(keyword,null,scope,offset,limit);
    }

    public List<ProductOption> productOptions(String keyword, Long warehouseId, Scope scope, long offset, int limit) {
        Map<String, Object> args = new HashMap<>();
        args.put("tenant", scope.getTenantId()); args.put("warehouses", scope.getWarehouseIds());
        args.put("keyword", "%" + (keyword == null ? "" : keyword.trim()) + "%");
        args.put("offset", offset); args.put("limit", limit);
        args.put("wholeWarehouses",scope.getWholeWarehouseIds()); args.put("selfWarehouses",scope.getSelfWarehouseIds());
        args.put("user",scope.getUserId());
        String warehouses = stockWarehouseCondition(scope,"s.warehouse_id","s.creator");
        if(warehouseId!=null) { warehouses+=" AND s.warehouse_id=:requestedWarehouse";args.put("requestedWarehouse",warehouseId); }
        return jdbc().query("SELECT p.id,p.code,p.name FROM erp_product p WHERE p.tenant_id=:tenant AND p.deleted=0 "
                        + "AND (p.code LIKE :keyword OR p.name LIKE :keyword) AND EXISTS (SELECT 1 FROM erp_stock s "
                        + "WHERE s.product_id=p.id AND s.tenant_id=:tenant AND s.deleted=0 AND " + warehouses + ") "
                        + "ORDER BY p.id LIMIT :limit OFFSET :offset", args, new BeanPropertyRowMapper<>(ProductOption.class));
    }

    public long productOptionCount(String keyword, Scope scope) {
        return productOptionCount(keyword,null,scope);
    }

    public long productOptionCount(String keyword, Long warehouseId, Scope scope) {
        Map<String, Object> args = new HashMap<>();
        args.put("tenant", scope.getTenantId()); args.put("warehouses", scope.getWarehouseIds());
        args.put("keyword", "%" + (keyword == null ? "" : keyword.trim()) + "%");
        args.put("wholeWarehouses",scope.getWholeWarehouseIds()); args.put("selfWarehouses",scope.getSelfWarehouseIds());
        args.put("user",scope.getUserId());
        String warehouses = stockWarehouseCondition(scope,"s.warehouse_id","s.creator");
        if(warehouseId!=null) { warehouses+=" AND s.warehouse_id=:requestedWarehouse";args.put("requestedWarehouse",warehouseId); }
        return jdbc().queryForObject("SELECT COUNT(*) FROM erp_product p WHERE p.tenant_id=:tenant AND p.deleted=0 "
                        + "AND (p.code LIKE :keyword OR p.name LIKE :keyword) AND EXISTS (SELECT 1 FROM erp_stock s "
                        + "WHERE s.product_id=p.id AND s.tenant_id=:tenant AND s.deleted=0 AND " + warehouses + ")",
                args, Long.class);
    }

    private Sql movement(Filter f, Scope scope) {
        Sql sql = new Sql();
        sql.text = "FROM erp_stock_dual_cost_posting p "
                + "LEFT JOIN erp_stock ps ON ps.id=p.stock_id AND ps.tenant_id=p.tenant_id "
                + "LEFT JOIN erp_stock_dual_cost_balance b ON b.stock_id=p.stock_id AND b.tenant_id=p.tenant_id "
                + "LEFT JOIN erp_stock_dual_cost_origin o ON o.stock_id=p.stock_id AND o.tenant_id=p.tenant_id "
                + "LEFT JOIN erp_product pr ON pr.id=p.product_id AND pr.tenant_id=p.tenant_id AND pr.deleted=0 "
                + "LEFT JOIN erp_warehouse w ON w.id=p.warehouse_id AND w.tenant_id=p.tenant_id AND w.deleted=0 "
                + "WHERE p.tenant_id=:tenant AND p.posted_at>=:from AND p.posted_at<:to ";
        commonParams(sql, f, scope);
        sql.text += scopeCondition(sql, scope, "p.stock_dept_id", "p.posted_by", "p.warehouse_id", true);
        filter(sql, "p.product_id", "product", f.getProductId());
        filter(sql, "p.warehouse_id", "warehouse", f.getWarehouseId());
        filterList(sql, "p.stock_dept_id", "stockDepts", f.getStockDeptIds());
        filterList(sql, "p.accounting_dept_id", "accountingDepts", f.getAccountingDeptIds());
        filterList(sql, "p.biz_type", "bizTypes", f.getBizTypes());
        if (f.getBatchNo() != null && !f.getBatchNo().trim().isEmpty()) filter(sql, "p.batch_no", "batch", f.getBatchNo().trim());
        return sql;
    }

    private Sql stockScope(Filter f, Scope scope, boolean allowSelf) {
        Sql sql = new Sql();
        sql.text = "FROM erp_stock s LEFT JOIN erp_stock_dual_cost_balance b ON b.stock_id=s.id AND b.tenant_id=s.tenant_id "
                + "LEFT JOIN erp_stock_dual_cost_origin o ON o.stock_id=s.id AND o.tenant_id=s.tenant_id "
                + "WHERE s.tenant_id=:tenant AND (s.deleted=0 OR b.stock_id IS NOT NULL) "
                + "AND (o.origin_kind IS NULL OR o.origin_kind<>'NEW_DIMENSION' OR o.available_from<:to) ";
        commonParams(sql, f, scope);
        // 自有事件的完整性检查只限定其事件涉及的库存，不把本人范围扩展到整个部门。
        String own = allowSelf ? "CASE WHEN EXISTS(SELECT 1 FROM erp_stock_dual_cost_posting own "
                + "WHERE own.stock_id=s.id AND own.tenant_id=s.tenant_id AND own.posted_by=:user) THEN :user ELSE NULL END" : "NULL";
        sql.text += scopeCondition(sql, scope, "COALESCE(b.stock_dept_id,s.dept_id)", own, "s.warehouse_id", allowSelf);
        filter(sql, "s.product_id", "product", f.getProductId());
        filter(sql, "s.warehouse_id", "warehouse", f.getWarehouseId());
        filterList(sql, "COALESCE(b.stock_dept_id,s.dept_id)", "stockDepts", f.getStockDeptIds());
        return sql;
    }

    private Sql balance(Filter f, Scope scope, LocalDateTime cutoff, boolean allowSelf) {
        Sql stocks = stockScope(f, scope, allowSelf);
        stocks.params.put("cutover", cutoff);
        String state = "CASE WHEN b.stock_id IS NULL THEN 'MISSING_OPENING' "
                + "WHEN b.cutover_at<>:cutover OR b.cutover_at>:from THEN 'BEFORE_CUTOVER' "
                + "WHEN :to>b.updated_at AND (NOT(b.quantity<=>s.count) OR NOT(b.legacy_cost_price<=>s.cost_price) "
                + "OR NOT(b.legacy_cost_amount<=>s.cost_amount) OR NOT(b.stock_dept_id<=>s.dept_id) "
                + "OR b.legacy_record_id<>COALESCE((SELECT MAX(lr.id) FROM erp_stock_record lr "
                + "WHERE lr.tenant_id=s.tenant_id AND lr.product_id=s.product_id AND lr.warehouse_id=s.warehouse_id AND lr.deleted=0),0)) "
                + "THEN 'STALE_OPENING' ELSE 'READY' END";
        // 聚合仅扫描授权且过滤命中的库存，期间上界统一为排他。
        String events = "SELECT p.stock_id,p.tenant_id,"
                + "SUM(CASE WHEN p.posted_at<:from THEN p.quantity ELSE 0 END) prior_quantity,"
                + "SUM(CASE WHEN p.posted_at<:from THEN p.financial_movement ELSE 0 END) prior_financial,"
                + "SUM(CASE WHEN p.posted_at<:from THEN p.settlement_movement ELSE 0 END) prior_settlement,"
                + "SUM(CASE WHEN p.posted_at>=:from THEN GREATEST(p.quantity,0) ELSE 0 END) in_quantity,"
                + "SUM(CASE WHEN p.posted_at>=:from THEN GREATEST(-p.quantity,0) ELSE 0 END) out_quantity,"
                + "SUM(CASE WHEN p.posted_at>=:from THEN GREATEST(p.financial_movement,0) ELSE 0 END) financial_in_amount,"
                + "SUM(CASE WHEN p.posted_at>=:from THEN GREATEST(-p.financial_movement,0) ELSE 0 END) financial_out_amount,"
                + "SUM(CASE WHEN p.posted_at>=:from THEN GREATEST(p.settlement_movement,0) ELSE 0 END) settlement_in_amount,"
                + "SUM(CASE WHEN p.posted_at>=:from THEN GREATEST(-p.settlement_movement,0) ELSE 0 END) settlement_out_amount "
                + "FROM erp_stock_dual_cost_posting p JOIN (SELECT s.id,s.tenant_id,COALESCE(o.available_from,b.cutover_at) AS cutover_at " + stocks.text
                + ") selected ON selected.id=p.stock_id AND selected.tenant_id=p.tenant_id "
                + "WHERE p.tenant_id=:tenant AND p.posted_at>=selected.cutover_at AND p.posted_at<:to GROUP BY p.stock_id,p.tenant_id";
        String joins = " LEFT JOIN (" + events + ") e ON e.stock_id=s.id AND e.tenant_id=s.tenant_id "
                + "LEFT JOIN erp_product pr ON pr.id=s.product_id AND pr.tenant_id=s.tenant_id AND pr.deleted=0 "
                + "LEFT JOIN erp_warehouse w ON w.id=s.warehouse_id AND w.tenant_id=s.tenant_id AND w.deleted=0 ";
        String base = stocks.text.replace("WHERE s.tenant_id", joins + " WHERE s.tenant_id");
        String openingQ = "b.opening_quantity+COALESCE(e.prior_quantity,0)";
        String openingF = "b.opening_financial_amount+COALESCE(e.prior_financial,0)";
        String openingS = "b.opening_settlement_amount+COALESCE(e.prior_settlement,0)";
        stocks.text = "SELECT s.id stock_id,s.product_id,s.warehouse_id,COALESCE(b.stock_dept_id,s.dept_id) stock_dept_id,"
                + "pr.code product_code,pr.name product_name,w.name warehouse_name,b.cutover_at," + state + " data_status,"
                + "CASE WHEN b.stock_id IS NULL THEN NULL ELSE COALESCE(o.origin_kind,'MANUAL_OPENING') END origin_kind,"
                + "CASE WHEN b.stock_id IS NULL THEN NULL ELSE COALESCE(o.available_from,b.cutover_at) END available_from,"
                + openingQ + " opening_quantity,COALESCE(e.in_quantity,0) in_quantity,COALESCE(e.out_quantity,0) out_quantity,"
                + openingQ + "+COALESCE(e.in_quantity,0)-COALESCE(e.out_quantity,0) closing_quantity,"
                + openingF + " opening_financial_amount,COALESCE(e.financial_in_amount,0) financial_in_amount,"
                + "COALESCE(e.financial_out_amount,0) financial_out_amount," + openingF
                + "+COALESCE(e.financial_in_amount,0)-COALESCE(e.financial_out_amount,0) closing_financial_amount,"
                + openingS + " opening_settlement_amount,COALESCE(e.settlement_in_amount,0) settlement_in_amount,"
                + "COALESCE(e.settlement_out_amount,0) settlement_out_amount," + openingS
                + "+COALESCE(e.settlement_in_amount,0)-COALESCE(e.settlement_out_amount,0) closing_settlement_amount " + base;
        return stocks;
    }

    private String scopeCondition(Sql sql, Scope scope, String dept, String creator, String warehouse, boolean allowSelf) {
        String result = " AND " + stockWarehouseCondition(scope,warehouse,warehouse.startsWith("p.") ? "ps.creator" : "s.creator");
        if (!scope.isAll()) {
            String departments = scope.getDeptIds().isEmpty() ? "1=0" : dept + " IN (:allowedDepts)";
            result += " AND (" + departments + (allowSelf && scope.isSelf() ? " OR " + creator + "=:user" : "") + ") ";
        }
        return result;
    }

    private String stockWarehouseCondition(Scope scope,String warehouse,String creator) {
        String whole = scope.getWholeWarehouseIds().isEmpty() ? "1=0" : warehouse+" IN (:wholeWarehouses)";
        String own = scope.getSelfWarehouseIds().isEmpty() ? "1=0"
                : "("+warehouse+" IN (:selfWarehouses) AND "+creator+"=CAST(:user AS CHAR))";
        return "("+whole+" OR "+own+") ";
    }

    private void commonParams(Sql sql, Filter f, Scope scope) {
        sql.params.put("tenant", scope.getTenantId()); sql.params.put("user", scope.getUserId());
        sql.params.put("allowedDepts", scope.getDeptIds()); sql.params.put("warehouses", scope.getWarehouseIds());
        sql.params.put("wholeWarehouses",scope.getWholeWarehouseIds()); sql.params.put("selfWarehouses",scope.getSelfWarehouseIds());
        sql.params.put("from", f.getPostedFrom()); sql.params.put("to", f.getPostedTo());
    }
    private void filter(Sql sql, String column, String name, Object value) {
        if (value != null) { sql.text += " AND " + column + "=:" + name; sql.params.put(name, value); }
    }
    private void filterList(Sql sql, String column, String name, Collection<?> values) {
        if (values != null && !values.isEmpty()) { sql.text += " AND " + column + " IN (:" + name + ")"; sql.params.put(name, values); }
    }
    private String movementOrder(Filter f) {
        return "postingId".equals(f.getOrderField()) ? " ORDER BY p.id" + direction(f)
                : " ORDER BY p.posted_at" + direction(f) + ",p.id" + direction(f);
    }
    private String direction(Filter f) { return "desc".equalsIgnoreCase(f.getOrderDirection()) ? " DESC" : " ASC"; }
    private static class Sql { private String text; private final Map<String, Object> params = new HashMap<>(); }
    private static final String[] AMOUNT_FIELDS = {"opening_quantity","in_quantity","out_quantity","closing_quantity",
            "opening_financial_amount","financial_in_amount","financial_out_amount","closing_financial_amount",
            "opening_settlement_amount","settlement_in_amount","settlement_out_amount","closing_settlement_amount"};
}
