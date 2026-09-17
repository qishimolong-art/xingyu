package cn.iocoder.yudao.module.erp.service.sale.returncost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

@Repository
public class ErpSaleReturnCurrentCostRepository {
    @Resource private JdbcTemplate jdbcTemplate;

    public void checkSchema() {
        jdbcTemplate.queryForList("SELECT tenant_id,return_id,latest_confirmation_id,posted FROM erp_sale_return_current_cost_state WHERE 1=0");
        jdbcTemplate.queryForList("SELECT id,tenant_id,return_id,revision,request_key,request_hash,source_signature,stock_signature,evidence,confirmed_by,confirmed_at,consumed_at FROM erp_sale_return_current_cost_confirmation WHERE 1=0");
        jdbcTemplate.queryForList("SELECT id,tenant_id,confirmation_id,return_item_id,financial_amount,settlement_amount,evidence FROM erp_sale_return_current_cost_line WHERE 1=0");
        jdbcTemplate.queryForList("SELECT id,tenant_id,return_id,return_item_id,posting_id,confirmation_id,confirmation_revision,basis_signature,source_signature,stock_signature,cost_source,quantity,financial_amount,settlement_amount,stock_basis,posted_at,approved_by FROM erp_sale_return_current_cost_posting_link WHERE 1=0");
    }

    public Map<String, Object> latest(long tenant, long returnId) {
        Map<String, Object> state = state(tenant, returnId);
        if (state == null || state.get("latest_confirmation_id") == null) {
            return null;
        }
        long id = ((Number) state.get("latest_confirmation_id")).longValue();
        Map<String, Object> result = first("SELECT * FROM erp_sale_return_current_cost_confirmation WHERE id=? AND tenant_id=? FOR UPDATE", id, tenant);
        if (result == null || ((Number) result.get("return_id")).longValue() != returnId) {
            throw new IllegalStateException("无单退货成本确认定位与实际证据不一致，请核对");
        }
        return result;
    }

    /** 同单父锁已串行；只读明确存在的版本，绝不锁尚未使用的请求键空范围。 */
    public Map<String, Object> byRequest(long tenant, long returnId, long expectedRevision, String key) {
        Map<String, Object> latest = latest(tenant, returnId);
        long candidateRevision = expectedRevision + 1;
        if (latest == null || candidateRevision < 1 || candidateRevision > ((Number) latest.get("revision")).longValue()) {
            return null;
        }
        Map<String, Object> candidate = first("SELECT * FROM erp_sale_return_current_cost_confirmation "
                + "WHERE tenant_id=? AND return_id=? AND revision=? FOR UPDATE", tenant, returnId, candidateRevision);
        if (candidate == null) {
            throw new IllegalStateException("无单退货成本确认版本链不完整，请核对");
        }
        return key.equals(candidate.get("request_key")) ? candidate : null;
    }

    public List<Map<String, Object>> lines(long tenant, long id, List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> values = new ArrayList<>();
        values.add(tenant);
        values.add(id);
        values.addAll(itemIds);
        // 完整签名已匹配，此版本每个源行都应存在；使用完整唯一键集合当前读，避免 RR 旧快照漏行。
        return jdbcTemplate.queryForList("SELECT * FROM erp_sale_return_current_cost_line FORCE INDEX(uk_sale_current_line) "
                + "WHERE tenant_id=? AND confirmation_id=? AND return_item_id IN (" + placeholders(itemIds) + ") FOR UPDATE", values.toArray());
    }

    /** 必须在真实源父行锁之后、库存锁之前调用；GET 不得调用。 */
    public void ensureState(long tenant, long returnId) {
        jdbcTemplate.update("INSERT INTO erp_sale_return_current_cost_state (tenant_id,return_id) VALUES (?,?) "
                + "ON DUPLICATE KEY UPDATE return_id=VALUES(return_id)", tenant, returnId);
    }

    public void setLatest(long tenant, long returnId, long confirmationId) {
        if (jdbcTemplate.update("UPDATE erp_sale_return_current_cost_state SET latest_confirmation_id=? WHERE tenant_id=? AND return_id=?",
                confirmationId, tenant, returnId) != 1) {
            throw new IllegalStateException("无单退货确认缺少同事务定位行");
        }
    }

    public void markPosted(long tenant, long returnId) {
        jdbcTemplate.update("UPDATE erp_sale_return_current_cost_state SET posted=b'1' WHERE tenant_id=? AND return_id=?", tenant, returnId);
        Map<String, Object> state = state(tenant, returnId);
        if (state == null || !posted(state)) {
            throw new IllegalStateException("无单退货过账缺少同事务定位行");
        }
    }

    private Map<String, Object> state(long tenant, long returnId) {
        return first("SELECT * FROM erp_sale_return_current_cost_state WHERE tenant_id=? AND return_id=? FOR UPDATE", tenant, returnId);
    }

    private boolean posted(Map<String, Object> state) {
        Object value = state.get("posted");
        return Boolean.TRUE.equals(value) || (value instanceof Number && ((Number) value).intValue() == 1);
    }

    private String placeholders(List<Long> ids) {
        return ids.stream().map(ignored -> "?").collect(Collectors.joining(","));
    }

    public Map<String, Object> posting(long tenant, long id) {
        return first("SELECT * FROM erp_stock_dual_cost_posting WHERE tenant_id=? AND id=?", tenant, id);
    }

    public long linkCount(long tenant, long returnId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM erp_sale_return_current_cost_posting_link WHERE tenant_id=? AND return_id=?", Long.class, tenant, returnId);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> postingLinks(long tenant, long returnId, List<Long> itemIds) {
        Map<String, Object> state = state(tenant, returnId);
        if (state == null || !posted(state) || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> values = new ArrayList<>();
        values.add(tenant);
        values.add(returnId);
        values.addAll(itemIds);
        List<Map<String, Object>> links = jdbcTemplate.queryForList("SELECT l.*,p.id AS actual_id,p.quantity AS actual_quantity,"
                + "p.financial_movement AS actual_financial,p.settlement_movement AS actual_settlement,"
                + "p.biz_id AS actual_return_id,p.biz_item_id AS actual_item_id,p.biz_type AS actual_type,"
                + "p.product_id AS actual_product,p.warehouse_id AS actual_warehouse,p.accounting_dept_id AS actual_dept,"
                + "p.source_biz_id AS actual_source,p.reversal_posting_id AS actual_reversal "
                + "FROM erp_sale_return_current_cost_posting_link l FORCE INDEX(uk_sale_current_return_item) LEFT JOIN erp_stock_dual_cost_posting p "
                + "ON p.tenant_id=l.tenant_id AND p.id=l.posting_id WHERE l.tenant_id=? AND l.return_id=? "
                + "AND l.return_item_id IN (" + placeholders(itemIds) + ") FOR UPDATE", values.toArray());
        if (links.size() != itemIds.size()) {
            throw new IllegalStateException("无单退货已过账定位与完整实际行不一致，请核对");
        }
        return links;
    }

    public int execute(String sql, Object... values) {
        return jdbcTemplate.update(sql, values);
    }

    public long insert(String sql, Object... values) {
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            return statement;
        }, holder);
        if (holder.getKey() == null) { throw new IllegalStateException("无单退货成本证据未生成ID"); }
        return holder.getKey().longValue();
    }

    private Map<String, Object> first(String sql, Object... values) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, values);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
