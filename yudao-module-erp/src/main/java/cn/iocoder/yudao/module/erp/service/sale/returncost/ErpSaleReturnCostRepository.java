package cn.iocoder.yudao.module.erp.service.sale.returncost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/** 所有核算查询显式限定租户；追加记录不依赖可变销售价格。 */
@Repository
public class ErpSaleReturnCostRepository {
    @Resource
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> lockSourcePostings(long tenant, long outId, long itemId) {
        return jdbcTemplate.queryForList("SELECT * FROM erp_stock_dual_cost_posting"
                + " WHERE tenant_id=? AND biz_type=50 AND biz_id=? AND biz_item_id=? ORDER BY id FOR UPDATE", tenant, outId, itemId);
    }

    public Map<String, Object> lockProgress(long tenant, long sourcePostingId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM erp_sale_return_cost_progress"
                + " WHERE tenant_id=? AND source_posting_id=? FOR UPDATE", tenant, sourcePostingId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 用逐行当前读求和，外层事务的旧RR快照不得漏掉刚提交的退货。 */
    public List<Map<String, Object>> lockAllocations(long tenant, long sourcePostingId) {
        return jdbcTemplate.queryForList("SELECT a.*,p.id AS actual_posting_id,p.biz_type AS actual_biz_type,"
                + "p.biz_id AS actual_biz_id,p.biz_item_id AS actual_item_id,p.reversal_posting_id AS actual_source_id,"
                + "p.product_id AS actual_product_id,p.accounting_dept_id AS actual_accounting_dept_id,"
                + "p.source_biz_type AS actual_source_biz_type,p.source_biz_id AS actual_source_biz_id,"
                + "p.source_biz_item_id AS actual_source_item_id,"
                + "p.quantity AS actual_quantity,p.financial_movement AS actual_financial,p.settlement_movement AS actual_settlement"
                + " FROM erp_sale_return_cost_allocation a LEFT JOIN erp_stock_dual_cost_posting p"
                + " ON p.tenant_id=a.tenant_id AND p.id=a.return_posting_id"
                + " WHERE a.tenant_id=? AND a.source_posting_id=? ORDER BY a.id FOR UPDATE", tenant, sourcePostingId);
    }

    /**
     * 旧已审核退货缺少分摊时不能只按新累计量继续退货。
     * 启用后所有审核持原销售锁且必须写齐分摊，历史无链集合不会新增。
     * 此处不锁其他退货父行，避免其已持父锁等待原销售锁时出现锁序反转。
     * 切换不允许旧新核算进程并行写同一账套；并发新退货由 allocation 当前读校验。
     */
    public boolean hasUnmappedApprovedReturn(long tenant, long outId, long excludedReturnId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM erp_sale_return r"
                + " JOIN erp_sale_return_items i ON i.return_id=r.id AND i.tenant_id=r.tenant_id AND i.deleted=0"
                + " LEFT JOIN erp_sale_return_cost_allocation a ON a.tenant_id=i.tenant_id AND a.return_id=r.id AND a.return_item_id=i.id"
                + " WHERE r.tenant_id=? AND r.source_out_id=? AND r.status=20 AND r.deleted=0 AND r.id<>? AND a.id IS NULL",
                Integer.class, tenant, outId, excludedReturnId);
        return count != null && count > 0;
    }

    public Map<String, Object> posting(long tenant, long postingId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM erp_stock_dual_cost_posting"
                + " WHERE tenant_id=? AND id=?", tenant, postingId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 已持原销售父锁、尚未持库存锁；只锁追加事件，不锁别的退货业务父行。 */
    public List<Map<String, Object>> lockReturnStatusAllocations(long tenant, long outId) {
        return jdbcTemplate.queryForList("SELECT a.quantity,p.id AS original_posting_id,"
                + "r.id AS return_posting_id,r.quantity AS actual_quantity,r.biz_type AS actual_type,"
                + "r.reversal_posting_id AS actual_source_id"
                + " FROM erp_stock_dual_cost_posting p JOIN erp_sale_return_cost_allocation a"
                + " ON a.tenant_id=p.tenant_id AND a.source_posting_id=p.id"
                + " LEFT JOIN erp_stock_dual_cost_posting r ON r.tenant_id=a.tenant_id AND r.id=a.return_posting_id"
                + " WHERE p.tenant_id=? AND p.biz_type=50 AND p.biz_id=? ORDER BY p.id,a.id FOR UPDATE", tenant, outId);
    }

    public long allocationCount(long tenant, long returnId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM erp_sale_return_cost_allocation"
                + " WHERE tenant_id=? AND return_id=?", Long.class, tenant, returnId);
        return count == null ? 0 : count;
    }

    public int execute(String sql, Object... values) {
        return jdbcTemplate.update(sql, values);
    }
}
