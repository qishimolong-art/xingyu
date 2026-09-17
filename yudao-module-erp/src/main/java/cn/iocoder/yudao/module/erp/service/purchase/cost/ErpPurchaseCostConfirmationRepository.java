package cn.iocoder.yudao.module.erp.service.purchase.cost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.sql.Statement;
import java.util.*;

/** 新表只在显式成本确认或启用后过账时访问；每条查询显式租户隔离。 */
@Repository
public class ErpPurchaseCostConfirmationRepository {
    @Resource private JdbcTemplate jdbcTemplate;
    public void checkSchema() {
        jdbcTemplate.queryForList("SELECT id,tenant_id,purchase_in_id,revision,request_key,request_hash,source_signature,"
                +"source_snapshot,rule_version,price_basis,tax_status,evidence,fee_treatment,confirmed_by,confirmed_at,consumed_at "
                +"FROM erp_purchase_cost_confirmation WHERE 1=0");
        jdbcTemplate.queryForList("SELECT id,tenant_id,confirmation_id,source_item_id,product_id,warehouse_id,quantity,"
                +"confirmed_net_total_amount,raw_unit_price,raw_line_amount,evidence FROM erp_purchase_cost_confirmation_line WHERE 1=0");
        jdbcTemplate.queryForList("SELECT id,tenant_id,confirmation_id,revision,source_item_id,action_key,financial_amount,settlement_amount FROM erp_purchase_cost_posting_link WHERE 1=0");
    }
    public Map<String,Object> latest(long tenant,long purchase,boolean lock) {
        return one("SELECT * FROM erp_purchase_cost_confirmation WHERE tenant_id=? AND purchase_in_id=? ORDER BY revision DESC LIMIT 1"
                +(lock?" FOR UPDATE":""),tenant,purchase);
    }
    public Map<String,Object> byRequest(long tenant,String key) {
        return one("SELECT * FROM erp_purchase_cost_confirmation WHERE tenant_id=? AND request_key=? FOR UPDATE",tenant,key);
    }
    public Map<String,Object> byId(long tenant,long id) {
        return one("SELECT * FROM erp_purchase_cost_confirmation WHERE tenant_id=? AND id=? FOR UPDATE",tenant,id);
    }
    public List<Map<String,Object>> lines(long tenant,long confirmation) {
        return jdbcTemplate.queryForList("SELECT * FROM erp_purchase_cost_confirmation_line WHERE tenant_id=? AND confirmation_id=? ORDER BY source_item_id",tenant,confirmation);
    }
    public List<Map<String,Object>> linesForItems(long tenant,long confirmation,Collection<Long> ids) {
        if(ids.isEmpty())return Collections.emptyList();
        List<Object> args=new ArrayList<>();args.add(tenant);args.add(confirmation);args.addAll(ids);
        return jdbcTemplate.queryForList("SELECT * FROM erp_purchase_cost_confirmation_line WHERE tenant_id=? AND confirmation_id=? AND source_item_id IN ("
                +String.join(",",Collections.nCopies(ids.size(),"?"))+")",args.toArray());
    }
    public Map<String,Object> line(long tenant,long confirmation,long item) {
        return one("SELECT * FROM erp_purchase_cost_confirmation_line WHERE tenant_id=? AND confirmation_id=? AND source_item_id=?",tenant,confirmation,item);
    }
    public long insert(String sql,Object...args) {
        GeneratedKeyHolder keys=new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]);
            return ps;
        },keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    public int execute(String sql,Object...args) { return jdbcTemplate.update(sql,args); }
    public long postedLineCount(long tenant,long confirmation) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM erp_purchase_cost_posting_link WHERE tenant_id=? AND confirmation_id=?",Long.class,tenant,confirmation);
    }
    private Map<String,Object> one(String sql,Object...args) {
        List<Map<String,Object>> rows=jdbcTemplate.queryForList(sql,args); return rows.isEmpty()?null:rows.get(0);
    }
}
