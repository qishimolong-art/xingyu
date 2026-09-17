package cn.iocoder.yudao.module.erp.service.stock.cost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.sql.Statement;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** 此核算写入仓储不对外暴露查询；原生 SQL 始终显式约束租户及有效记录。 */
@Repository
public class ErpDualCostLedgerRepository {
    private static final String INSERT_CURSOR_BODY = "UPDATE erp_stock SET legacy_record_cursor_id=GREATEST(legacy_record_cursor_id,NEW.id) "
            + "WHERE tenant_id=NEW.tenant_id AND product_id=NEW.product_id AND warehouse_id=NEW.warehouse_id AND deleted=b'0' AND NEW.deleted=b'0'";
    private static final String CURSOR_GUARD_BODY = "BEGIN IF NEW.legacy_record_cursor_id<OLD.legacy_record_cursor_id THEN "
            + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='stock record cursor cannot decrease'; END IF; END";
    @Resource
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> lockStock(long tenant, long product, long warehouse) {
        return one("SELECT id, dept_id, count, cost_price, cost_amount FROM erp_stock WHERE tenant_id=? AND product_id=? "
                + "AND warehouse_id=? AND deleted=0 FOR UPDATE", tenant, product, warehouse);
    }

    public Map<String, Object> lockBalance(long tenant, long stock) {
        return one("SELECT * FROM erp_stock_dual_cost_balance WHERE tenant_id=? AND stock_id=? FOR UPDATE", tenant, stock);
    }

    public Map<String, Object> findPosting(long tenant, String key) {
        // 仅在 INSERT 已明确触发该动作唯一键冲突之后调用，不能用于锁定尚不存在的动作。
        return one("SELECT request_hash,rule_version FROM erp_stock_dual_cost_posting WHERE tenant_id=? AND action_key=? FOR UPDATE", tenant, key);
    }

    public long latestLegacyRecordId(long tenant, long product, long warehouse) {
        assertLegacyCursorReady();
        Map<String, Object> stock = one("SELECT id,legacy_record_cursor_id FROM erp_stock WHERE tenant_id=? "
                + "AND product_id=? AND warehouse_id=? AND deleted=0 FOR UPDATE", tenant, product, warehouse);
        if (stock == null || !(stock.get("legacy_record_cursor_id") instanceof Number)) {
            throw new IllegalStateException("库存流水游标缺少真实库存父行或尚未核对");
        }
        long id = ((Number) stock.get("legacy_record_cursor_id")).longValue();
        if (id < 0) {
            throw new IllegalStateException("库存流水游标无效，请核对");
        }
        if (id == 0) {
            return 0L; // 迁移/新库存历史核验及单调触发器证明的零起点，不锁空流水范围。
        }
        Map<String, Object> record = one("SELECT id,tenant_id,product_id,warehouse_id,deleted FROM erp_stock_record WHERE id=? AND tenant_id=? FOR UPDATE", id, tenant);
        if (record == null || !sameLong(record.get("tenant_id"), tenant)
                || !sameLong(record.get("product_id"), product) || !sameLong(record.get("warehouse_id"), warehouse)
                || !(Boolean.FALSE.equals(record.get("deleted")) || sameLong(record.get("deleted"), 0))) {
            throw new IllegalStateException("库存流水游标指向缺失、删除或其他维度的记录，请核对");
        }
        return id;
    }

    /** 人工期初只读核对真实历史，当前事务不会插入库存流水；不可用于普通过账路径。 */
    public long verifyLegacyCursorForOpening(long tenant, long product, long warehouse) {
        long cursor = latestLegacyRecordId(tenant, product, warehouse);
        Map<String, Object> actual = one("SELECT id FROM erp_stock_record WHERE tenant_id=? AND product_id=? "
                + "AND warehouse_id=? AND deleted=0 ORDER BY id DESC LIMIT 1 FOR UPDATE", tenant, product, warehouse);
        long latest = actual == null ? 0L : ((Number) actual.get("id")).longValue();
        if (cursor != latest) {
            throw new IllegalStateException("库存流水游标与期初真实历史不一致，禁止将缺失记录当零");
        }
        return cursor;
    }

    /** 新口径必须能证明列及两个采集/保护触发器均为已审阅定义。 */
    public void assertLegacyCursorReady() {
        Map<String, Object> column = one("SELECT DATA_TYPE,IS_NULLABLE,COLUMN_DEFAULT,COLUMN_TYPE,GENERATION_EXPRESSION "
                + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_stock' AND COLUMN_NAME='legacy_record_cursor_id'");
        if (column == null || !"bigint".equalsIgnoreCase(String.valueOf(column.get("DATA_TYPE")))
                || !"NO".equals(column.get("IS_NULLABLE")) || !"0".equals(String.valueOf(column.get("COLUMN_DEFAULT")))
                || String.valueOf(column.get("COLUMN_TYPE")).toLowerCase(Locale.ROOT).contains("unsigned")
                || (column.get("GENERATION_EXPRESSION") != null && !String.valueOf(column.get("GENERATION_EXPRESSION")).isEmpty())) {
            throw new IllegalStateException("库存流水游标迁移缺失或列定义冲突，新核算不可用");
        }
        requireTrigger("erp_stock_record_cursor_ai", "erp_stock_record", "AFTER", "INSERT", INSERT_CURSOR_BODY);
        requireTrigger("erp_stock_record_cursor_bu", "erp_stock", "BEFORE", "UPDATE", CURSOR_GUARD_BODY);
    }

    private void requireTrigger(String name, String table, String timing, String event, String body) {
        Map<String, Object> trigger = one("SELECT t.EVENT_OBJECT_TABLE,t.ACTION_TIMING,t.EVENT_MANIPULATION,t.ACTION_ORIENTATION,t.ACTION_STATEMENT,t.ACTION_ORDER,"
                + "(SELECT MAX(other.ACTION_ORDER) FROM information_schema.TRIGGERS other WHERE other.TRIGGER_SCHEMA=t.TRIGGER_SCHEMA "
                + "AND other.EVENT_OBJECT_TABLE=t.EVENT_OBJECT_TABLE AND other.ACTION_TIMING=t.ACTION_TIMING "
                + "AND other.EVENT_MANIPULATION=t.EVENT_MANIPULATION) AS LAST_ACTION_ORDER "
                + "FROM information_schema.TRIGGERS t WHERE t.TRIGGER_SCHEMA=DATABASE() AND t.TRIGGER_NAME=?", name);
        if (trigger == null || !table.equals(trigger.get("EVENT_OBJECT_TABLE"))
                || !timing.equals(trigger.get("ACTION_TIMING")) || !event.equals(trigger.get("EVENT_MANIPULATION"))
                || !"ROW".equals(trigger.get("ACTION_ORIENTATION"))
                || !sameLong(trigger.get("ACTION_ORDER"), ((Number) trigger.get("LAST_ACTION_ORDER")).longValue())
                || !normalizeSql(body).equals(normalizeSql(String.valueOf(trigger.get("ACTION_STATEMENT"))))) {
            throw new IllegalStateException("库存流水游标采集或保护触发器未就绪：" + name);
        }
    }

    private String normalizeSql(String sql) {
        return sql.replace("`", "").replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private boolean sameLong(Object value, long expected) {
        return value instanceof Number && ((Number) value).longValue() == expected;
    }

    public int execute(String sql, Object... arguments) {
        return jdbcTemplate.update(sql, arguments);
    }

    public long insertPosting(String sql, Object... arguments) {
        GeneratedKeyHolder generated = new GeneratedKeyHolder();
        int count = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            return statement;
        }, generated);
        if (count != 1 || generated.getKey() == null) {
            throw new IllegalStateException("库存过账未取得实际事件编号");
        }
        return generated.getKey().longValue();
    }

    private Map<String, Object> one(String sql, Object... arguments) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, arguments);
        if (rows.size() > 1) {
            throw new IllegalStateException("库存核算维度存在重复数据，需先核对");
        }
        return rows.isEmpty() ? null : rows.get(0);
    }
}
