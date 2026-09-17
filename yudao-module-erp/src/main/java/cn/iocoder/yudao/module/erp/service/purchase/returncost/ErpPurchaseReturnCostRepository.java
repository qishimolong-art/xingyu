package cn.iocoder.yudao.module.erp.service.purchase.returncost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.util.*;

@Repository
public class ErpPurchaseReturnCostRepository {
    @Resource private JdbcTemplate jdbcTemplate;

    /** 所有来源量写入口先锁来源父，再锁退货父；当前读包含历史，不锁序反转。 */
    public List<Map<String,Object>> relatedReturns(long tenant, boolean saleTrace, long itemId,
                                                   Long excludedReturnId, boolean includeProcessing) {
        String field = saleTrace ? "source_sale_return_item_id" : "source_in_item_id";
        String status = saleTrace ? "" : includeProcessing ? " AND r.status IN (10,20)" : " AND r.status=20";
        return jdbcTemplate.queryForList("SELECT i.id,i.return_id,i.product_id,i.count,i."+field+" AS source_item_id,"
                + "r.status,r.supplier_id FROM erp_purchase_return_items i STRAIGHT_JOIN erp_purchase_return r"
                + " ON r.tenant_id=i.tenant_id AND r.id=i.return_id AND r.deleted=0"
                + " WHERE i.tenant_id=? AND i.deleted=0 AND i."+field+"=? AND i.return_id<>?"+status
                + " ORDER BY i.id FOR UPDATE",tenant,itemId,excludedReturnId==null ? -1L : excludedReturnId);
    }

    public Map<String,Object> posting(long tenant,long id) {
        List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT * FROM erp_stock_dual_cost_posting WHERE tenant_id=? AND id=?",tenant,id);
        return rows.isEmpty()?null:rows.get(0);
    }

    public long linkCount(long tenant,long returnId) {
        Long count=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM erp_purchase_return_posting_link WHERE tenant_id=? AND return_id=?",Long.class,tenant,returnId);
        return count==null?0:count;
    }

    public int execute(String sql,Object...args) { return jdbcTemplate.update(sql,args); }
}
