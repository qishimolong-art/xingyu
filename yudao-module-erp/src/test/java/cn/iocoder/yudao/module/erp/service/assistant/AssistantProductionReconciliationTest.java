package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.dal.mysql.report.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantSql;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.*;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in read-only reconciliation. No model calls, no write datasource, no publication.
 * Reference rows are independently aggregated in Java from existing report sources / raw ledgers.
 * Bounded to the previously supplied conversation owner, known warehouse, and current period.
 */
@EnabledIfSystemProperty(named="assistant.productionReconcile", matches="true")
class AssistantProductionReconciliationTest {
    private final AssistantReadOnly reader=new AssistantReadOnly();
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private int referenceId;
    private SqlSession session() {
        return ((ThreadLocal<SqlSession>)ReflectionTestUtils.getField(reader,"sessions")).get();
    }
    private List<Map<String,Object>> reference(String sql,Map<String,Object> args) {
        Configuration config=session().getConfiguration();
        String id="reconcile.reference"+(++referenceId);
        SqlSource source=new XMLLanguageDriver().createSqlSource(config,"<script>"+sql+"</script>",Map.class);
        MappedStatement ms=new MappedStatement.Builder(config,id,source,SqlCommandType.SELECT)
            .timeout(10).resultMaps(Collections.singletonList(new ResultMap.Builder(config,id,Map.class,Collections.emptyList()).build())).build();
        config.addMappedStatement(ms);
        try {return session().selectList(id,args);}
        catch(RuntimeException error) {
            Throwable root=error;while(root.getCause()!=null)root=root.getCause();
            evidence.put("errorType",root.getClass().getSimpleName());
            if(root.getClass().getSimpleName().equals("ParseException"))
                evidence.put("parserError",root.getMessage());
            if(root instanceof SQLException) {
                evidence.put("sqlErrorCode",((SQLException)root).getErrorCode());
                evidence.put("sqlState",((SQLException)root).getSQLState());
                String message=root.getMessage();
                if(message!=null && (message.startsWith("Unknown column") || message.matches("Table '[a-zA-Z0-9_.-]+' doesn't exist")))
                    evidence.put("schemaError",message);
            }
            throw error;
        }
    }
    private long count(String sql) throws SQLException {
        try(Statement s=session().getConnection().createStatement()) {
            s.setQueryTimeout(10);try(ResultSet r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}
        }
    }
    private void verifyScope() throws SQLException {
        // Fail closed if the confirmed user/role or form/field scope changes.
        assertEquals(1,count("SELECT COUNT(*) FROM erp_assistant_conversation WHERE id='bd4e5cba-bc71-416a-bdee-790c7f9bb446' AND user_id=1 AND tenant_id=1 AND deleted=0"));
        assertEquals(1,count("SELECT COUNT(*) FROM system_users WHERE id=1 AND tenant_id=1 AND status=0 AND deleted=0"));
        assertEquals(1,count("SELECT COUNT(*) FROM system_user_role ur JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id WHERE ur.user_id=1 AND ur.tenant_id=1 AND ur.deleted=0 AND r.deleted=0 AND r.status=0 AND r.code='super_admin' AND r.data_scope=1"));
        assertEquals(0,count("SELECT COUNT(*) FROM system_role_form_data_scope s JOIN system_user_role ur ON ur.role_id=s.role_id AND ur.tenant_id=s.tenant_id WHERE ur.user_id=1 AND ur.tenant_id=1 AND ur.deleted=0 AND s.deleted=0 AND s.data_scope NOT IN (0,1)"));
        assertEquals(0,count("SELECT COUNT(*) FROM system_role_field_permission f JOIN system_user_role ur ON ur.role_id=f.role_id AND ur.tenant_id=f.tenant_id WHERE ur.user_id=1 AND ur.tenant_id=1 AND ur.deleted=0 AND f.deleted=0 AND f.hidden=1"));
        assertEquals(3,count("SELECT COUNT(DISTINCT price_field_code) FROM system_user_price_field WHERE user_id=1 AND deleted=0 AND visible=1 AND price_field_code IN ('salePrice','purchasePrice','lastPurchasePrice')"));
        assertEquals(1,count("SELECT COUNT(*) FROM erp_warehouse WHERE id=8 AND name='蛟龙港仓' AND tenant_id=1 AND status=0 AND deleted=0"));
    }
    private Map<String,Object> context(AssistantPlan plan,LocalDateTime now) {
        Map<String,Object> a=new HashMap<>();a.put("plan",plan);a.put("tenantId",1L);a.put("userId","1");
        LocalDateTime start=plan.getMetric()==AssistantPlan.Metric.RECEIPT || plan.getMetric()==AssistantPlan.Metric.PAYMENT
            ? now.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay():now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        a.put("start",start);a.put("end",now);LocalDateTime[] range={start,now.minusNanos(1000)};
        ErpSaleReportPageReqVO sale=new ErpSaleReportPageReqVO();sale.setBizTime(range);
        ErpPurchaseReportPageReqVO purchase=new ErpPurchaseReportPageReqVO();purchase.setBizTime(range);
        a.put("reqVO",plan.getMetric()==AssistantPlan.Metric.SALE?sale:plan.getMetric()==AssistantPlan.Metric.PURCHASE?purchase:
            plan.getMetric()==AssistantPlan.Metric.RECEIVABLE?new ErpReceivableAccountPageReqVO():new ErpPayableAccountPageReqVO());
        for(String prefix:Arrays.asList("party","document","doc","")) {
            a.put(prefix.isEmpty()?"all":prefix+"All",true);a.put(prefix.isEmpty()?"deptIds":prefix+"DeptIds",Collections.emptyList());
            a.put(prefix.isEmpty()?"selfUserId":prefix+"SelfUserId",null);
        }
        a.put("stockAll",true);a.put("stockDeptIds",Collections.emptyList());a.put("stockSelfIds",Collections.emptyList());
        a.put("mode","summary");a.put("rowLimit",101);return a;
    }
    private String baseline(AssistantPlan plan) {
        AssistantPlan.Metric metric=plan.getMetric();
        switch(metric) {
            case SALE: case PURCHASE:
                boolean sale=metric==AssistantPlan.Metric.SALE;
                boolean product=plan.getGroup()==AssistantPlan.Group.PRODUCT;
                String raw=AssistantSql.annotation(sale?ErpSaleReportMapper.class:ErpPurchaseReportMapper.class,product?"selectProductRows":"selectRows");
                String dimensions=product?"q.productId,NULL deptId,NULL partyId,NULL day":
                    "NULL productId,q.deptId,q."+(sale?"customerId":"supplierId")+" partyId,DATE(q.docDate) day";
                return "SELECT "+(product?"CAST(q.productId AS CHAR)":"q.rowKey")+" rowId,"+dimensions+",'元' unit,q.netAmount amount,q."+(sale?"saleAmount":"purchaseAmount")+" gross,q.returnAmount refund,"+(product?0:1)+" documents,0 pending FROM ("+raw+") q";
            case RECEIVABLE: case PAYABLE:
                boolean receive=metric==AssistantPlan.Metric.RECEIVABLE;
                String account=receive?new ErpReceivableAccountMapper.SqlProvider().selectList().replace("<script>","").replace("</script>",""):
                    AssistantSql.annotation(ErpPayableAccountMapper.class,"selectList");
                return "SELECT q."+(receive?"customerId":"supplierId")+" partyId,q.deptId,'元' unit,q."+(receive?"receivableBalance":"balance")+" amount,0 gross,0 refund,0 pending,1 documents FROM ("+account+") q";
            case STOCK: case STOCK_SKU:
                return "SELECT CAST(s.id AS CHAR) rowId,p.id productId,w.id warehouseId,w.dept_id deptId,COALESCE(u.name,'未设置单位') unit,COALESCE(s.count,0) amount,0 gross,0 refund,0 pending,1 documents FROM erp_stock s JOIN erp_product p ON p.id=s.product_id AND p.deleted=0 AND p.tenant_id=s.tenant_id JOIN erp_warehouse w ON w.id=s.warehouse_id AND w.deleted=0 AND w.tenant_id=s.tenant_id LEFT JOIN erp_product_unit u ON u.id=p.unit_id AND u.deleted=0 AND u.tenant_id=s.tenant_id WHERE s.deleted=0 AND s.tenant_id=#{tenantId} AND s.warehouse_id=8";
            case RECEIPT: case PAYMENT:
                String kind=metric==AssistantPlan.Metric.RECEIPT?"receipt":"payment";
                String party=kind.equals("receipt")?"customer":"supplier";
                // Raw header fields. The reference applies state rules in Java, avoiding repeated SQL CASE logic.
                return "SELECT t.id,CAST(t.id AS CHAR) rowId,t.status,t.dept_id deptId,p.id partyId,DATE(t."+kind+"_time) day,'元' unit,t."+kind+"_price actual,t.total_price total,t.discount_price discount FROM erp_finance_"+kind+" t JOIN erp_"+party+" p ON p.id=t."+party+"_id AND p.tenant_id=t.tenant_id AND p.deleted=0 WHERE t.tenant_id=#{tenantId} AND t.deleted=0 AND t.status IN (10,20) AND t."+kind+"_time &gt;= #{start} AND t."+kind+"_time &lt; #{end}";
            default:throw new IllegalArgumentException();
        }
    }
    private BigDecimal decimal(Object value){return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());}
    private void explain(Map<String,Object> args,Map<String,Object> comparison) {
        MappedStatement statement=session().getConfiguration().getMappedStatement("cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantQueryMapper.select");
        BoundSql bound=statement.getBoundSql(args);
        TenantLineInnerInterceptor tenant=new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(TenantContextHolder.getRequiredTenantId());}});
        String sql=tenant.parserSingle(bound.getSql(),null);
        List<Map<String,Object>> rows=new ArrayList<>();
        try(PreparedStatement prepared=session().getConnection().prepareStatement("EXPLAIN "+sql)) {
            prepared.setQueryTimeout(10);
            new org.apache.ibatis.scripting.defaults.DefaultParameterHandler(statement,args,bound).setParameters(prepared);
            try(ResultSet result=prepared.executeQuery()) {
                while(result.next()) {Map<String,Object> row=new LinkedHashMap<>();for(String key:Arrays.asList("table","type","key","rows","Extra"))row.put(key,result.getObject(key));rows.add(row);}
            }
        } catch(SQLException e){throw new IllegalStateException("Bounded EXPLAIN failed",e);}
        comparison.put("explain",rows);
    }
    private void compareStockModes(AssistantPlan plan,Map<String,Object> args,List<Map<String,Object>> raw,Map<String,Object> comparison) {
        boolean sku=plan.getMetric()==AssistantPlan.Metric.STOCK_SKU;
        List<String> modes=sku?Arrays.asList("ALL","POSITIVE","NEGATIVE"):Arrays.asList("ALL","POSITIVE","NEGATIVE","AVAILABLE");
        for(String mode:modes) {
            plan.setStockMode(mode);
            List<Map<String,Object>> rows=new ArrayList<>();
            if(mode.equals("AVAILABLE")) {
                String reserved=cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper.occupiedCountExpression().replace("erp_stock.","s.").replace("&","&amp;").replace("<","&lt;");
                String sql=baseline(plan).replace("COALESCE(s.count,0) amount","COALESCE(s.count,0) amount,COALESCE("+reserved+",0) reserved");
                for(Map<String,Object> row:reference(sql+" LIMIT 20001",args)) {
                    Map<String,Object> copy=new HashMap<>(row);copy.put("amount",decimal(row.get("amount")).subtract(decimal(row.get("reserved"))));rows.add(copy);
                }
                assertTrue(rows.size()<=20000);
            } else for(Map<String,Object> row:raw) {
                int sign=decimal(row.get("amount")).signum();
                if(sku || mode.equals("ALL") || mode.equals("POSITIVE") && sign>0 || mode.equals("NEGATIVE") && sign<0)rows.add(row);
            }
            Map<String,Map<String,BigDecimal>> expected;
            if(sku) {
                Map<String,BigDecimal> sums=new HashMap<>();for(Map<String,Object> row:rows)sums.merge(String.valueOf(row.get("productId")),decimal(row.get("amount")),BigDecimal::add);
                long n=sums.values().stream().filter(v->mode.equals("ALL") || mode.equals("POSITIVE") && v.signum()>0 || mode.equals("NEGATIVE") && v.signum()<0).count();
                expected=new TreeMap<>();if(n>0){Map<String,BigDecimal> count=new TreeMap<>();count.put("amount",BigDecimal.valueOf(n));count.put("documents",BigDecimal.valueOf(n));expected.put("种",count);}
            } else expected=aggregate(rows,plan.getMetric(),true);
            args.put("mode","summary");Map<String,Map<String,BigDecimal>> actual=aggregate(reader.select(args),plan.getMetric(),false);
            assertEquals(expected.keySet(),actual.keySet(),mode);
            for(String unit:expected.keySet())for(String field:expected.get(unit).keySet())assertEquals(0,expected.get(unit).get(field).compareTo(actual.get(unit).get(field)),mode+":"+field);
        }
        comparison.put("verifiedStockModes",modes);plan.setStockMode(sku?"POSITIVE":"ALL");
    }
    private void compareDetailAndFilters(AssistantPlan plan,Map<String,Object> args,List<Map<String,Object>> raw,Map<String,Object> comparison) {
        Map<String,BigDecimal> detail=new TreeMap<>();
        for(Map<String,Object> row:raw) {
            String id=String.valueOf(row.get(plan.getMetric()==AssistantPlan.Metric.STOCK_SKU?"productId":"rowId"));
            BigDecimal value=decimal(row.get("amount"));
            if(row.containsKey("actual"))value=((Number)row.get("status")).intValue()==20?decimal(row.get("actual")):BigDecimal.ZERO;
            detail.merge(id,value,BigDecimal::add);
        }
        if(plan.getMetric()==AssistantPlan.Metric.STOCK_SKU)detail.entrySet().removeIf(e->e.getValue().signum()<=0);
        args.put("mode","count");assertEquals(detail.size(),((Number)reader.select(args).get(0).get("total")).intValue());
        List<String> ids=new ArrayList<>(detail.keySet());
        for(int offset:new LinkedHashSet<>(Arrays.asList(0,Math.max(0,((ids.size()-1)/20)*20)))) {
            args.put("mode","details");args.put("pageSize",20);args.put("offset",offset);
            List<Map<String,Object>> rows=reader.select(args);assertEquals(Math.min(20,Math.max(0,ids.size()-offset)),rows.size());
            for(int i=0;i<rows.size();i++) {
                String id=String.valueOf(rows.get(i).get("rowId"));assertEquals(ids.get(offset+i),id);
                assertEquals(0,detail.get(id).compareTo(decimal(rows.get(i).get("amount"))));
            }
        }
        comparison.put("detailPagesMatched",true);args.put("mode","summary");
        List<String> checked=new ArrayList<>();
        for(String dimension:Arrays.asList("deptId","partyId","productId")) {
            if(dimension.equals("productId") && plan.getMetric()!=AssistantPlan.Metric.STOCK && plan.getMetric()!=AssistantPlan.Metric.STOCK_SKU)continue;
            Object id=raw.stream().map(r->r.get(dimension)).filter(v->v instanceof Number).findFirst().orElse(null);if(id==null)continue;
            Long value=((Number)id).longValue();
            if(dimension.equals("deptId"))plan.setDepartmentId(value);else if(dimension.equals("partyId"))plan.setPartyId(value);else plan.setProductId(value);
            List<Map<String,Object>> subset=new ArrayList<>();for(Map<String,Object> row:raw)if(id.equals(row.get(dimension)))subset.add(row);
            Map<String,Map<String,BigDecimal>> expected=aggregate(subset,plan.getMetric(),true),actual=aggregate(reader.select(args),plan.getMetric(),false);
            assertEquals(expected.keySet(),actual.keySet());
            for(String unit:expected.keySet())for(String field:expected.get(unit).keySet())assertEquals(0,expected.get(unit).get(field).compareTo(actual.get(unit).get(field)));
            plan.setDepartmentId(null);plan.setPartyId(null);plan.setProductId(null);checked.add(dimension);
        }
        comparison.put("verifiedFilters",checked);
        if(plan.getMetric()==AssistantPlan.Metric.RECEIPT || plan.getMetric()==AssistantPlan.Metric.PAYMENT) {
            String kind=plan.getMetric()==AssistantPlan.Metric.RECEIPT?"receipt":"payment",party=kind.equals("receipt")?"customer":"supplier";
            String itemSql="SELECT i."+kind+"_price amount FROM erp_finance_"+kind+"_item i JOIN erp_finance_"+kind+" t ON t.id=i."+kind+"_id AND t.tenant_id=i.tenant_id JOIN erp_"+party+" p ON p.id=t."+party+"_id AND p.tenant_id=t.tenant_id AND p.deleted=0 WHERE i.deleted=0 AND i.write_off_status=1 AND t.deleted=0 AND t.status=20 AND t."+kind+"_time &gt;= #{start} AND t."+kind+"_time &lt; #{end} LIMIT 20001";
            List<Map<String,Object>> items=reference(itemSql,args);assertTrue(items.size()<=20000);
            BigDecimal expected=items.stream().map(r->decimal(r.get("amount"))).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal actual=reader.select(args).stream().map(r->decimal(r.get("writeOff"))).reduce(BigDecimal.ZERO,BigDecimal::add);
            assertEquals(0,expected.compareTo(actual));comparison.put("writeOffMatched",true);
        }
    }
    private List<Map<String,Object>> dataset(AssistantPlan plan,Map<String,Object> args) {
        String sql=baseline(plan);
        long count=((Number)reference("SELECT COUNT(1) n FROM ("+sql+") bounded",args).get(0).get("n")).longValue();
        assertTrue(count<=20000,"Reference row cap exceeded; narrow scope before continuing");
        return reference(sql+" LIMIT 20001",args);
    }
    private void compareGroups(AssistantPlan plan,Map<String,Object> args,List<Map<String,Object>> raw,Map<String,Object> comparison) {
        List<String> verified=new ArrayList<>();verified.add("NONE");
        List<AssistantPlan.Group> groups=(plan.getMetric()==AssistantPlan.Metric.STOCK || plan.getMetric()==AssistantPlan.Metric.STOCK_SKU)
            ?Arrays.asList(AssistantPlan.Group.PRODUCT,AssistantPlan.Group.WAREHOUSE,AssistantPlan.Group.DEPT)
            :plan.current()?Arrays.asList(AssistantPlan.Group.PARTY,AssistantPlan.Group.DEPT)
            :Arrays.asList(AssistantPlan.Group.DAY,AssistantPlan.Group.PARTY,AssistantPlan.Group.DEPT);
        if(plan.getMetric()==AssistantPlan.Metric.SALE || plan.getMetric()==AssistantPlan.Metric.PURCHASE) {
            groups=new ArrayList<>(groups);groups.add(AssistantPlan.Group.PRODUCT);
        }
        for(AssistantPlan.Group group:groups) {
            plan.setGroup(group);String field=group==AssistantPlan.Group.DEPT?"deptId":group==AssistantPlan.Group.DAY?"day":group.name().toLowerCase(Locale.ROOT)+"Id";
            List<Map<String,Object>> rows=group==AssistantPlan.Group.PRODUCT && (plan.getMetric()==AssistantPlan.Metric.SALE || plan.getMetric()==AssistantPlan.Metric.PURCHASE)?dataset(plan,args):raw;
            Map<String,List<Map<String,Object>>> buckets=new HashMap<>();
            for(Map<String,Object> row:rows)buckets.computeIfAbsent(String.valueOf(row.get(field)),k->new ArrayList<>()).add(row);
            Map<String,Map<String,Map<String,BigDecimal>>> expected=new HashMap<>();
            for(Map.Entry<String,List<Map<String,Object>>> bucket:buckets.entrySet()) {
                if(plan.getMetric()==AssistantPlan.Metric.STOCK) {
                    Map<String,BigDecimal> sum=new TreeMap<>();
                    for(Map<String,Object> row:bucket.getValue())for(String value:Arrays.asList("amount","gross","refund","pending","documents"))
                        sum.merge(value,decimal(row.get(value)),BigDecimal::add);
                    String unit="数量";
                    if(group==AssistantPlan.Group.PRODUCT) {
                        Set<String> units=bucket.getValue().stream().map(row->String.valueOf(row.get("unit"))).collect(java.util.stream.Collectors.toSet());
                        assertEquals(1,units.size(),"A product must have one display unit");unit=units.iterator().next();
                    }
                    Map<String,Map<String,BigDecimal>> grouped=new TreeMap<>();grouped.put(unit,sum);expected.put(bucket.getKey(),grouped);
                } else expected.put(bucket.getKey(),aggregate(bucket.getValue(),plan.getMetric(),true));
            }
            args.put("mode","groups");args.put("rowLimit",10);
            List<Map<String,Object>> actual=reader.select(args);
            Map<String,List<String>> ordered=new HashMap<>();
            for(String id:expected.keySet())for(String unit:expected.get(id).keySet())ordered.computeIfAbsent(unit,k->new ArrayList<>()).add(id);
            Set<String> top=new HashSet<>();
            if(plan.getMetric()==AssistantPlan.Metric.STOCK) {
                List<String> ids=new ArrayList<>(expected.keySet());
                ids.sort((a,b)->{
                    String aUnit=expected.get(a).keySet().iterator().next(),bUnit=expected.get(b).keySet().iterator().next();
                    int amount=expected.get(b).get(bUnit).get("amount").compareTo(expected.get(a).get(aUnit).get("amount"));
                    if(amount!=0)return amount;
                    if(a.equals("null") || b.equals("null"))return a.equals(b)?0:a.equals("null")?-1:1;
                    return new BigDecimal(a).compareTo(new BigDecimal(b));
                });
                BigDecimal previous=null;int rank=0;
                for(String id:ids) {
                    String unit=expected.get(id).keySet().iterator().next();BigDecimal amount=expected.get(id).get(unit).get("amount");
                    if(previous==null || amount.compareTo(previous)!=0){rank++;previous=amount;}
                    if(rank<=10)top.add(id+"|"+unit);
                }
            } else if(plan.getMetric()==AssistantPlan.Metric.STOCK_SKU && group!=AssistantPlan.Group.DAY) {
                // Production SQL uses DENSE_RANK and deliberately retains ties. Mirror that
                // behavior here instead of truncating an equal-amount SKU group at ten rows.
                for(String unit:ordered.keySet()) {
                    List<String> ids=ordered.get(unit);
                    ids.sort((a,b)->{
                        int amount=expected.get(b).get(unit).get("amount").compareTo(expected.get(a).get(unit).get("amount"));
                        if(amount!=0)return amount;
                        if(a.equals("null") || b.equals("null"))return a.equals(b)?0:a.equals("null")?-1:1;
                        return new BigDecimal(a).compareTo(new BigDecimal(b));
                    });
                    BigDecimal previous=null;int rank=0,rowCount=0;
                    for(String id:ids) {
                        BigDecimal amount=expected.get(id).get(unit).get("amount");
                        if(previous==null || amount.compareTo(previous)!=0){rank++;previous=amount;}
                        if(rank>10 || rowCount>=201)break;
                        top.add(id+"|"+unit);rowCount++;
                    }
                }
            } else for(String unit:ordered.keySet()) {
                List<String> ids=ordered.get(unit);
                ids.sort((a,b)->{
                    if(group==AssistantPlan.Group.DAY)return a.compareTo(b);
                    int amount=expected.get(b).get(unit).get("amount").compareTo(expected.get(a).get(unit).get("amount"));
                    if(amount!=0)return amount;
                    if(a.equals("null") || b.equals("null"))return a.equals(b)?0:a.equals("null")?-1:1;
                    return new BigDecimal(a).compareTo(new BigDecimal(b));
                });
                int maximum=group==AssistantPlan.Group.DAY?367:10;
                for(String id:ids.subList(0,Math.min(maximum,ids.size())))top.add(id+"|"+unit);
            }
            if(group==AssistantPlan.Group.DAY){args.put("rowLimit",367);actual=reader.select(args);}
            Set<String> actualKeys=new HashSet<>();
            for(Map<String,Object> row:actual) {
                String id=String.valueOf(row.get("groupId")),unit=String.valueOf(row.get("unit"));actualKeys.add(id+"|"+unit);
                assertTrue(expected.containsKey(id),"Unexpected grouped identity");
                for(Map.Entry<String,BigDecimal> entry:expected.get(id).get(unit).entrySet())
                    assertEquals(0,entry.getValue().compareTo(decimal(row.get(entry.getKey()))),plan.getMetric()+":"+group+":"+entry.getKey());
            }
            assertEquals(top,actualKeys,plan.getMetric()+":"+group+" ranking/trend");verified.add(group.name());
        }
        comparison.put("verifiedGroups",verified);plan.setGroup(AssistantPlan.Group.NONE);args.put("mode","summary");args.put("rowLimit",101);
    }
    private Map<String,Map<String,BigDecimal>> aggregate(List<Map<String,Object>> rows,AssistantPlan.Metric metric,boolean expected) {
        Map<String,Map<String,BigDecimal>> sums=new TreeMap<>();
        if(expected && metric==AssistantPlan.Metric.STOCK_SKU) {
            Map<String,BigDecimal> products=new HashMap<>();
            for(Map<String,Object> row:rows)products.merge(String.valueOf(row.get("productId")),decimal(row.get("amount")),BigDecimal::add);
            long n=products.values().stream().filter(v->v.signum()>0).count();
            if(n>0){Map<String,BigDecimal> sum=new TreeMap<>();sum.put("amount",BigDecimal.valueOf(n));sum.put("documents",BigDecimal.valueOf(n));sums.put("种",sum);}return sums;
        }
        for(Map<String,Object> row:rows) {
            Map<String,BigDecimal> sum=sums.computeIfAbsent(String.valueOf(row.get("unit")),k->new TreeMap<>());
            if(expected && (metric==AssistantPlan.Metric.RECEIPT || metric==AssistantPlan.Metric.PAYMENT)) {
                boolean approved=((Number)row.get("status")).intValue()==20;
                row=new HashMap<>(row);row.put("amount",approved?row.get("actual"):0);row.put("gross",approved?row.get("total"):0);
                row.put("refund",approved?row.get("discount"):0);row.put("pending",approved?0:row.get("actual"));row.put("documents",approved?1:0);
            }
            for(String field:metric==AssistantPlan.Metric.STOCK_SKU?Arrays.asList("amount","documents"):Arrays.asList("amount","gross","refund","pending","documents"))
                sum.merge(field,decimal(row.get(field)),BigDecimal::add);
        }
        return sums;
    }
    private void compareAccountDetails(AssistantPlan plan,Map<String,Object> args,List<Map<String,Object>> accounts,Map<String,Object> comparison) {
        if(accounts.isEmpty()){comparison.put("ledgerSample","no-account");return;}
        boolean receive=plan.getMetric()==AssistantPlan.Metric.RECEIVABLE;
        String party=receive?"customer":"supplier",account=receive?"receivable":"payable",cash=receive?"receipt":"payment";
        plan.setPartyId(((Number)accounts.get(0).get("partyId")).longValue());
        String original=receive?cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper.RECEIVABLE_ACCOUNT_SALE_AMOUNT_EXPRESSION:
            cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper.ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION;
        String[][] sources={
            {receive?"sale_out":"purchase_in",receive?"out_time":"in_time",original," AND t.status=20"},
            {receive?"sale_return":"purchase_return","return_time","-t.total_price"," AND t.status=20"},
            {receive?"sale_price_adjust":"purchase_price_adjust",receive?"adjust_date":"adjust_time","t.total_adjust_price"," AND t.status=20"},
            {"finance_"+cash,cash+"_time","-IF(EXISTS(SELECT 1 FROM erp_"+account+"_other x WHERE x.source_id=t.id AND x.tenant_id=t.tenant_id AND x.deleted=0 AND x.status=20 AND x.source_type='"+(receive?"收款单优惠":"付款单折让")+"'),t."+cash+"_price,t.total_price)"," AND t.status=20"},
            {account+"_other","biz_time","t."+account+"_amount"," AND t.status=20"},
            {account+"_writeoff","write_off_time","-t.write_off_amount",""}
        };
        Map<String,BigDecimal> expected=new HashMap<>();Map<String,String> days=new HashMap<>();
        for(String[] src:sources) {
            List<Map<String,Object>> rows=reference("SELECT t.id,DATE(t."+src[1]+") day,"+src[2]+" amount FROM erp_"+src[0]+" t WHERE t.deleted=0 AND t.tenant_id=#{tenantId} AND t."+party+"_id=#{plan.partyId}"+src[3]+" LIMIT 20001",args);
            assertTrue(rows.size()<=20000,"Account ledger cap exceeded");
            for(Map<String,Object> row:rows){String id=src[0]+"-"+row.get("id");expected.put(id,decimal(row.get("amount")));days.put(id,String.valueOf(row.get("day")));}
        }
        List<String> ordered=new ArrayList<>(expected.keySet());ordered.sort((a,b)->{int date=days.get(b).compareTo(days.get(a));return date==0?a.compareTo(b):date;});
        args.put("mode","count");assertEquals(ordered.size(),((Number)reader.select(args).get(0).get("total")).intValue());
        for(int offset:new LinkedHashSet<>(Arrays.asList(0,Math.max(0,((ordered.size()-1)/20)*20)))) {
            args.put("mode","details");args.put("pageSize",20);args.put("offset",offset);List<Map<String,Object>> actual=reader.select(args);
            assertEquals(Math.min(20,Math.max(0,ordered.size()-offset)),actual.size());
            for(int i=0;i<actual.size();i++){String id=String.valueOf(actual.get(i).get("rowId"));assertEquals(ordered.get(offset+i),id);assertEquals(0,expected.get(id).compareTo(decimal(actual.get(i).get("amount"))));}
        }
        comparison.put("ledgerPagesMatched",true);comparison.put("ledgerRows",ordered.size());
        comparison.put("ledgerMeaning","Transactions and write-off evidence are shown separately; their sum is not the current account balance formula.");
        plan.setPartyId(null);args.put("mode","summary");
    }
    @Test void compareBoundedProductionSummaries() throws Exception {
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.WARN);
        evidence.put("environment","production");evidence.put("businessReconciliation",false);
        evidence.put("scope",Collections.singletonMap("tenantId",1));evidence.put("userId",1);
        evidence.put("status","running");evidence.put("publicationChanged",false);
        List<Map<String,Object>> comparisons=new ArrayList<>();evidence.put("comparisons",comparisons);
        Map<?,?> settings=null;
        try(java.io.InputStream in=Files.newInputStream(Paths.get("../yudao-server/src/main/resources/application.yaml"))) {
            for(Object doc:new org.yaml.snakeyaml.Yaml().loadAll(in))if(doc instanceof Map && ((Map)doc).get("erp") instanceof Map)settings=(Map)((Map)((Map)doc).get("erp")).get("assistant");
        }
        Map<?,?> ro=(Map<?,?>)settings.get("read-only");String url=(String)ro.get("url");
        assertTrue(url.startsWith("jdbc:mysql://47.108.161.110:3306/ruoyi-vue-pro?"));assertEquals("select_only",ro.get("username"));
        AssistantProperties properties=new AssistantProperties();properties.getReadOnly().setUrl(url);properties.getReadOnly().setUsername((String)ro.get("username"));properties.getReadOnly().setPassword((String)ro.get("password"));
        MybatisConfiguration config=new MybatisConfiguration();MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(TenantContextHolder.getRequiredTenantId());}}));config.addInterceptor(interceptor);
        ReflectionTestUtils.setField(reader,"properties",properties);ReflectionTestUtils.setField(reader,"sqlSessionFactory",new SqlSessionFactoryBuilder().build(config));
        TenantContextHolder.setTenantId(1L);
        try {
            reader.probe();evidence.put("javaReadOnlyProbe",true);
            reader.snapshot(()->{
                try{verifyScope();}catch(SQLException e){throw new IllegalStateException("Scope metadata check failed",e);}
                LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));evidence.put("verifiedAt",now.toString());
                for(AssistantPlan.Metric metric:Arrays.asList(AssistantPlan.Metric.STOCK,AssistantPlan.Metric.STOCK_SKU,AssistantPlan.Metric.SALE,AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.PURCHASE,AssistantPlan.Metric.PAYMENT,AssistantPlan.Metric.RECEIVABLE,AssistantPlan.Metric.PAYABLE)) {
                    if(!Arrays.asList(System.getProperty("assistant.reconcileMetrics","STOCK,STOCK_SKU,SALE,RECEIPT,PURCHASE,PAYMENT,RECEIVABLE,PAYABLE").split(",")).contains(metric.name()))continue;
                    Map<String,Object> comparison=new LinkedHashMap<>();comparison.put("metric",metric.name());comparisons.add(comparison);
                    AssistantPlan plan=new AssistantPlan();plan.setMetric(metric);plan.setPeriod(plan.current()?"CURRENT":metric==AssistantPlan.Metric.RECEIPT || metric==AssistantPlan.Metric.PAYMENT?"THIS_WEEK":"THIS_MONTH");
                    if(metric==AssistantPlan.Metric.STOCK || metric==AssistantPlan.Metric.STOCK_SKU)plan.setWarehouseId(8L);
                    if(metric==AssistantPlan.Metric.STOCK_SKU)plan.setStockMode("POSITIVE");
                    Map<String,Object> args=context(plan,now);List<Map<String,Object>> raw=dataset(plan,args);
                    comparison.put("referenceRows",raw.size());
                    long started=System.nanoTime();
                    Map<String,Map<String,BigDecimal>> expected=aggregate(raw,metric,true),actual=aggregate(reader.select(args),metric,false);
                    comparison.put("summaryElapsedMs",(System.nanoTime()-started)/1000000);
                    comparison.put("period",plan.getPeriod());comparison.put("expected",expected);comparison.put("actual",actual);
                    assertEquals(expected.keySet(),actual.keySet(),metric.name());
                    for(String unit:expected.keySet())for(String field:expected.get(unit).keySet())
                        assertEquals(0,expected.get(unit).get(field).compareTo(actual.get(unit).get(field)),metric+":"+unit+":"+field);
                    comparison.put("matched",true);
                    if(metric==AssistantPlan.Metric.STOCK || metric==AssistantPlan.Metric.STOCK_SKU)compareStockModes(plan,args,raw,comparison);
                    compareGroups(plan,args,raw,comparison);
                    if(metric!=AssistantPlan.Metric.RECEIVABLE && metric!=AssistantPlan.Metric.PAYABLE)compareDetailAndFilters(plan,args,raw,comparison);
                    else compareAccountDetails(plan,args,raw,comparison);
                    explain(args,comparison);
                }
                return true;
            });
            evidence.put("status","summary-passed");
            evidence.put("remaining","Application permission regression and authenticated browser acceptance must be linked before publication; this preliminary file alone cannot publish metrics.");
        } finally {
            reader.close();TenantContextHolder.clear();
            if("running".equals(evidence.get("status"))) evidence.put("status","failed");
            Path output=Paths.get("../../修改文档/0921-0927/0927/智能问数/正式库汇总初步对账.json");Files.createDirectories(output.getParent());
            if(Files.exists(output))Files.copy(output,output.resolveSibling("正式库对账历史-"+System.currentTimeMillis()+".json"));
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.toFile(),evidence);
        }
    }
}
