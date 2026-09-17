package cn.iocoder.yudao.module.erp.service.stock.cost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 仅本事务成功创建且无历史的维度可建立NEW_DIMENSION；现有零库存不是初始化依据。 */
@Service
public class ErpStockDimensionService {
    @Value("${erp.reporting.dual-cost-enabled:false}")
    private boolean enabled;
    @Value("${erp.reporting.dual-cost-cutover:}")
    private String cutover;
    @Resource
    private JdbcTemplate jdbcTemplate;

    public boolean isEnabled() {
        return enabled;
    }

    /** 所有身份变更在主表写入前调用。低频档案/维度变更同租户串行，普通过账不取此锁。 */
    public void lockIdentityChanges() {
        if (!enabled) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw error("库存身份变更必须在业务事务内执行");
        }
        long tenant = TenantContextHolder.getRequiredTenantId();
        jdbcTemplate.update("INSERT IGNORE INTO erp_stock_dual_cost_dimension_mutex"
                + "(tenant_id,product_id,warehouse_id) VALUES (?,0,0)", tenant);
        jdbcTemplate.queryForList("SELECT tenant_id FROM erp_stock_dual_cost_dimension_mutex"
                + " WHERE tenant_id=? AND product_id=0 AND warehouse_id=0 FOR UPDATE", tenant);
    }

    /** false意味着旧路径，且本方法未读取任何新表。 */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY, rollbackFor = Exception.class)
    public boolean initializeDimensions(Collection<ErpStockDO> requested) {
        if (!enabled) {
            return false;
        }
        lockIdentityChanges();
        LocalDateTime start = validCutover();
        Long operator = getLoginUserId();
        if (operator == null) {
            throw error("新库存初始化必须由已登录人员在业务事务中执行");
        }
        long tenant = TenantContextHolder.getRequiredTenantId();
        List<ErpStockDO> dimensions = ordered(requested);
        reserveDimensions(dimensions);
        for (ErpStockDO dimension : dimensions) {
            initialize(tenant, dimension.getProductId(), dimension.getWarehouseId(), start, operator);
        }
        return true;
    }

    /** 批量入口先完整收集所有维度，预锁后才修改任意主表/明细；不在这里创建库存。 */
    public void reserveDimensions(Collection<ErpStockDO> requested) {
        if (!enabled) {
            return;
        }
        lockIdentityChanges();
        long tenant = TenantContextHolder.getRequiredTenantId();
        List<ErpStockDO> dimensions = ordered(requested);
        // 先取得全部维度mutex，再访问库存；复合主键和InnoDB行锁随业务事务释放。
        for (ErpStockDO dimension : dimensions) {
            jdbcTemplate.update("INSERT IGNORE INTO erp_stock_dual_cost_dimension_mutex"
                            + "(tenant_id,product_id,warehouse_id) VALUES (?,?,?)",
                    tenant, dimension.getProductId(), dimension.getWarehouseId());
            jdbcTemplate.queryForList("SELECT tenant_id FROM erp_stock_dual_cost_dimension_mutex"
                            + " WHERE tenant_id=? AND product_id=? AND warehouse_id=? FOR UPDATE",
                    tenant, dimension.getProductId(), dimension.getWarehouseId());
        }
    }

    private void initialize(long tenant, long product, long warehouse, LocalDateTime start, long operator) {
        Map<String, Object> location = one("SELECT id,dept_id,status FROM erp_warehouse"
                + " WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE", tenant, warehouse);
        if (location == null || location.get("dept_id") == null
                || new BigDecimal(location.get("dept_id").toString()).signum() <= 0) {
            throw error("库存仓库必须存在且归属部门已明确");
        }
        Map<String, Object> goods = one("SELECT id,status FROM erp_product"
                + " WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE", tenant, product);
        if (goods == null) {
            throw error("库存商品档案不存在");
        }
        List<Map<String, Object>> stocks = jdbcTemplate.queryForList(
                "SELECT id,dept_id,count,cost_price,cost_amount,deleted FROM erp_stock"
                        + " WHERE tenant_id=? AND product_id=? AND warehouse_id=? FOR UPDATE", tenant, product, warehouse);
        if (!stocks.isEmpty()) {
            if (stocks.size() != 1 || deleted(stocks.get(0).get("deleted"))) {
                throw error("该库存维度已有旧记录或软删历史，禁止自动建立零起点");
            }
            Map<String, Object> stock = stocks.get(0);
            if (!same(stock.get("dept_id"), location.get("dept_id"))) {
                throw error("库存归属与仓库归属不一致，禁止由初始化静默改部门");
            }
            Map<String, Object> balance = one("SELECT stock_id,cutover_at FROM erp_stock_dual_cost_balance"
                    + " WHERE tenant_id=? AND stock_id=? FOR UPDATE", tenant, stock.get("id"));
            if (balance == null || !start.equals(time(balance.get("cutover_at")))) {
                throw error("已有库存尚未核对当前切换期初，不能以零库存自动补齐");
            }
            return;
        }
        // 档案停用不影响已有库存身份；只有真正创建新维度时才要求商品和仓库可用。
        if (!same(location.get("status"), 0) || !same(goods.get("status"), 0)) {
            throw error("新库存维度的商品和仓库必须处于启用状态");
        }
        // 这里只核对身份是否孤立，不锁全租户正常余额。gate保护本段身份增删；普通过账只更新已有余额。
        // 目标stock及目标balance仍使用上方当前读，避免外层RR旧快照看不到并发初始化赢家。
        if (!jdbcTemplate.queryForList("SELECT b.stock_id FROM erp_stock_dual_cost_balance b"
                + " LEFT JOIN erp_stock s ON s.id=b.stock_id AND s.tenant_id=b.tenant_id"
                + " WHERE b.tenant_id=? AND s.id IS NULL LIMIT 1", tenant).isEmpty()) {
            throw error("存在无法定位库存维度的期初余额，请先核对；禁止自动建立零起点");
        }
        // 不过滤deleted。批次数量是业务表聚合视图，并不存在独立batch_quantity表；已过账批次由原库存流水留证。
        for (String table : Arrays.asList("erp_stock_record", "erp_stock_lock", "erp_stock_dual_cost_posting", "erp_stock_dual_cost_origin")) {
            if (!jdbcTemplate.queryForList("SELECT product_id FROM " + table
                    + " WHERE tenant_id=? AND product_id=? AND warehouse_id=? LIMIT 1", tenant, product, warehouse).isEmpty()) {
                throw error("该商品仓库存在库存历史，禁止自动建立零起点");
            }
        }
        LocalDateTime current = LocalDateTime.now();
        final LocalDateTime createdAt = current.withNano(current.getNano() / 1000 * 1000);
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement("INSERT INTO erp_stock"
                    + "(tenant_id,product_id,warehouse_id,dept_id,count,lock_count,cost_price,cost_amount,"
                    + "creator,updater,create_time,update_time,deleted) VALUES (?,?,?,?,0,0,0,0,?,?,?,?,0)", Statement.RETURN_GENERATED_KEYS);
            Object[] values = {tenant, product, warehouse, location.get("dept_id"), String.valueOf(operator),
                    String.valueOf(operator), createdAt, createdAt};
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            return ps;
        },key);
        long stockId = Objects.requireNonNull(key.getKey()).longValue();
        String action = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO erp_stock_dual_cost_origin"
                        + "(tenant_id,stock_id,product_id,warehouse_id,origin_kind,available_from,created_by,action_key,history_check_version,created_at)"
                        + " VALUES (?,?,?,?,'NEW_DIMENSION',?,?,?,'NO_STOCK_HISTORY_V1',?)",
                tenant, stockId, product, warehouse, createdAt, operator, action, createdAt);
        jdbcTemplate.update("INSERT INTO erp_stock_dual_cost_balance"
                        + "(tenant_id,stock_id,quantity,financial_amount,settlement_amount,opening_quantity,opening_financial_amount,"
                        + "opening_settlement_amount,cutover_at,confirmed_by,confirmed_at,evidence,updated_at,legacy_record_id,"
                        + "legacy_cost_price,legacy_cost_amount,stock_dept_id) VALUES (?,?,0,0,0,0,0,0,?,?,?,?,?,0,0,0,?)",
                tenant, stockId, start, operator, createdAt, "NEW_DIMENSION:" + action, createdAt, location.get("dept_id"));
    }

    /** 名称/备注不涉及本方法；调用方必须传字段权限保留后的实际值。 */
    public void assertWarehouseDepartmentChange(Long warehouseId, Long effectiveDeptId) {
        if (!enabled || effectiveDeptId == null) {
            return;
        }
        lockIdentityChanges();
        long tenant = TenantContextHolder.getRequiredTenantId();
        Map<String, Object> warehouse = one("SELECT dept_id FROM erp_warehouse"
                + " WHERE tenant_id=? AND id=? FOR UPDATE", tenant, warehouseId);
        if (warehouse != null && !same(warehouse.get("dept_id"), effectiveDeptId)
                && hasHistory("warehouse_id", warehouseId)) {
            throw error("该仓库已有库存核算身份，变更归属部门须使用受控归属调整流程");
        }
    }

    public void assertProductIdentityChange(Long productId) {
        if (!enabled) {
            return;
        }
        lockIdentityChanges();
        one("SELECT id FROM erp_product WHERE tenant_id=? AND id=? FOR UPDATE",
                TenantContextHolder.getRequiredTenantId(), productId);
        if (hasHistory("product_id", productId)) {
            throw error("该商品已有库存核算身份，禁止删除或合并改写历史维度");
        }
    }

    public void assertStockRemoval(Long stockId) {
        if (enabled) {
            lockIdentityChanges();
            throw error("新核算启用后不能删除库存分发身份，请使用受控停用流程");
        }
    }

    public void assertLegacyInsertAllowed() {
        if (enabled) {
            throw error("统一过账应已锁定实物库存，禁止旧写入分支自动创建缺失库存");
        }
    }

    private boolean hasHistory(String column, Long id) {
        long tenant = TenantContextHolder.getRequiredTenantId();
        // 身份调整也可能由已建立旧RR快照的外层业务调用，确切目标库存须作当前读。
        if (!jdbcTemplate.queryForList("SELECT " + column + " FROM erp_stock"
                + " WHERE tenant_id=? AND " + column + "=? LIMIT 1 FOR UPDATE", tenant, id).isEmpty()) {
            return true;
        }
        for (String table : Arrays.asList("erp_stock_record", "erp_stock_lock", "erp_stock_dual_cost_posting", "erp_stock_dual_cost_origin")) {
            if (!jdbcTemplate.queryForList("SELECT " + column + " FROM " + table
                    + " WHERE tenant_id=? AND " + column + "=? LIMIT 1", tenant, id).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private LocalDateTime validCutover() {
        try {
            LocalDateTime value = LocalDateTime.parse(cutover);
            if (value.getNano() != 0) {
                throw error("切换时间须使用与人工期初一致的整秒精度");
            }
            if (value.isAfter(LocalDateTime.now())) {
                throw error("尚未到核算切换时间");
            }
            return value;
        } catch (java.time.DateTimeException | NullPointerException e) {
            throw error("新库存初始化前必须配置有效切换时间");
        }
    }

    static List<ErpStockDO> ordered(Collection<ErpStockDO> requested) {
        if (requested == null) {
            return Collections.emptyList();
        }
        Map<String, ErpStockDO> unique = new HashMap<>();
        for (ErpStockDO value : requested) {
            if (value == null || value.getProductId() == null || value.getWarehouseId() == null) {
                continue;
            }
            if (value.getProductId() <= 0 || value.getWarehouseId() <= 0) {
                throw error("库存维度编号无效");
            }
            unique.put(value.getProductId() + ":" + value.getWarehouseId(), value);
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(ErpStockDO::getProductId).thenComparing(ErpStockDO::getWarehouseId))
                .collect(Collectors.toList());
    }

    private static boolean same(Object left, Object right) {
        return left == null || right == null ? left == right
                : new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) == 0;
    }

    private static boolean deleted(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof byte[]) {
            return ((byte[]) value)[0] != 0;
        }
        return value != null && Integer.parseInt(value.toString()) != 0;
    }

    private static LocalDateTime time(Object value) {
        return value instanceof java.sql.Timestamp ? ((java.sql.Timestamp) value).toLocalDateTime() : (LocalDateTime) value;
    }

    private static ServiceException error(String reason) {
        return new ServiceException(409, reason);
    }
}
