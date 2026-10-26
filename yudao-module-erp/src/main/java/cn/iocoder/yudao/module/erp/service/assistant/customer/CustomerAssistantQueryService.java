package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantClarification;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantProperties;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantUnderstandingResult;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceLevelPricePicker;
import cn.iocoder.yudao.module.erp.service.stock.ErpMallStockService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockSummaryBO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerAssistantQueryService {
    @Resource private NamedParameterJdbcTemplate jdbc;
    @Resource private ErpSalePriceLevelPricePicker pricePicker;
    @Resource private ErpMallStockService mallStockService;
    @Resource private ErpSalePickDeliveryService pickDeliveryService;
    @Resource private AssistantProperties properties;

    public Map<String,Object> price(CustomerAssistantPrincipal p,Map<String,Object> args) {
        if(!p.isPriceVisible()) throw new AssistantFailure("PRICE_HIDDEN","当前账号不可查看价格，请转人工客服确认");
        if(Integer.valueOf(6).equals(p.getPriceLevel())) throw new AssistantFailure("PRICE_LEVEL_FORBIDDEN","当前客户价格需要人工确认，请转人工客服");
        Map<String,Object> product=oneProduct(p,args);
        Long id=number(product.get("id"));
        BigDecimal price=pricePicker.pickProductPriceMap(Collections.singleton(id),p.getPriceLevel()).get(id);
        if(price==null||price.compareTo(BigDecimal.ZERO)<=0) return result("EMPTY","暂未配置可展示的客户价，请转人工客服确认",Collections.emptyList(),priceColumns());
        Map<String,Object> row=productView(product);row.put("customerPrice",price);
        Map<String,Object> result=result("SUCCESS","已查询当前绑定客户适用的客户价",Collections.singletonList(row),priceColumns());
        result.put("_productId",id);return result;
    }

    public Map<String,Object> stock(CustomerAssistantPrincipal p,Map<String,Object> args) {
        Map<String,Object> product=oneProduct(p,args);Long spuId=number(product.get("mall_spu_id"));
        ErpMallStockSummaryBO stock=mallStockService.getMallStockSummaryDetail(spuId);
        if(stock==null||!Boolean.TRUE.equals(stock.getMapped())) throw new AssistantFailure("PRODUCT_NOT_AVAILABLE","该商品尚未开放商城查询");
        BigDecimal available=stock.getTotalAvailableCount()==null?BigDecimal.ZERO:stock.getTotalAvailableCount().max(BigDecimal.ZERO);
        Map<String,Object> row=productView(product);row.put("availableQuantity",available);
        Map<String,Object> result=result("SUCCESS","商城当前精确可售总量",Collections.singletonList(row),Arrays.asList(column("name","商品"),column("code","配件编码"),column("standard","规格"),column("availableQuantity","可售数量")));
        result.put("_productId",number(product.get("id")));return result;
    }

    public Map<String,Object> orders(CustomerAssistantPrincipal p,Map<String,Object> args) {
        if(!p.isOrderEnabled()) throw new AssistantFailure("ORDER_FORBIDDEN","当前账号未开通订单查询，请转人工客服");
        String orderNo=text(args.get("orderNo"));boolean unfinished=Boolean.TRUE.equals(args.get("unfinishedOnly"));
        int page=Math.max(1,Math.min(10000,intValue(args.get("page"),1)));int size=Math.max(1,Math.min(50,properties.getCustomerApp().getMaxRows()));
        MapSqlParameterSource params=new MapSqlParameterSource().addValue("tenantId",p.getTenantId()).addValue("customerId",p.getCustomerId())
                .addValue("deptIds",p.getDepartmentIds()).addValue("cartSource",30).addValue("approved",20).addValue("cancelled",90)
                .addValue("start",LocalDateTime.now().minusDays(Math.max(1,properties.getCustomerApp().getOrderDefaultDays())))
                .addValue("limit",size).addValue("offset",(page-1)*size);
        String dateFilter=StringUtils.hasText(orderNo)||unfinished?"":" AND c.cart_time>=:start";
        String oldDateFilter=StringUtils.hasText(orderNo)||unfinished?"":" AND o.order_time>=:start";
        if(args.get("period") instanceof String && !"CURRENT".equals(args.get("period"))) {
            AssistantUnderstandingResult.Conditions conditions=new AssistantUnderstandingResult.Conditions();
            conditions.setPeriod((String)args.get("period"));conditions.setStartDate(text(args.get("startDate")));conditions.setEndDate(text(args.get("endDate")));
            LocalDateTime[] range=conditions.range();params.addValue("from",range[0]).addValue("until",range[1]);
            dateFilter=" AND c.cart_time>=:from AND c.cart_time<:until";oldDateFilter=" AND o.order_time>=:from AND o.order_time<:until";
        }
        String sql="SELECT * FROM ("+
                "SELECT 'SALE_CART' source_kind,c.id,c.no,c.status,c.cart_time order_date,c.total_price amount,c.total_count ordered_count,"+
                "COALESCE((SELECT SUM(so.total_count) FROM erp_sale_out so WHERE so.tenant_id=c.tenant_id AND so.customer_id=c.customer_id AND so.source_type=:cartSource AND so.source_id=c.id AND so.status=:approved AND so.deleted=0),0) out_count,"+
                "COALESCE((SELECT SUM(sr.total_count) FROM erp_sale_return sr JOIN erp_sale_out so2 ON so2.id=sr.source_out_id AND so2.tenant_id=sr.tenant_id WHERE sr.tenant_id=c.tenant_id AND sr.customer_id=c.customer_id AND so2.source_type=:cartSource AND so2.source_id=c.id AND sr.status=:approved AND sr.deleted=0 AND so2.deleted=0),0) return_count " +
                "FROM erp_sale_cart c WHERE c.tenant_id=:tenantId AND c.customer_id=:customerId AND c.dept_id IN (:deptIds) AND c.deleted=0"+dateFilter+
                " UNION ALL SELECT 'LEGACY_ORDER',o.id,o.no,o.status,o.order_time,o.total_price,o.total_count,COALESCE(o.out_count,0),COALESCE(o.return_count,0) " +
                "FROM erp_sale_order o WHERE o.tenant_id=:tenantId AND o.customer_id=:customerId AND o.dept_id IN (:deptIds) AND o.deleted=0"+oldDateFilter+
                ") q WHERE 1=1";
        if(StringUtils.hasText(orderNo)){sql+=" AND q.no=:orderNo";params.addValue("orderNo",orderNo.trim());}
        if(unfinished) sql+=" AND q.status<>:cancelled AND GREATEST(q.ordered_count-q.out_count+q.return_count,0)>0";
        sql+=" ORDER BY q.order_date DESC,q.id DESC LIMIT :limit OFFSET :offset";
        List<Map<String,Object>> raw=jdbc.queryForList(sql,params);Map<Long,ErpSalePickDeliverySummaryRespVO> progress=pickProgress(raw);
        List<Map<String,Object>> rows=new ArrayList<>();
        for(Map<String,Object> source:raw){BigDecimal ordered=decimal(source.get("ordered_count"));BigDecimal out=decimal(source.get("out_count"));BigDecimal returned=decimal(source.get("return_count"));BigDecimal remaining=ordered.subtract(out).add(returned).max(BigDecimal.ZERO);
            ErpSalePickDeliverySummaryRespVO deliveryProgress="SALE_CART".equals(source.get("source_kind"))?progress.get(number(source.get("id"))):null;
            Map<String,Object> row=new LinkedHashMap<>();row.put("orderNo",source.get("no"));row.put("type","SALE_CART".equals(source.get("source_kind"))?"销售开单":"历史销售订单");row.put("date",source.get("order_date"));row.put("amount",source.get("amount"));row.put("status",status(source));row.put("orderedQuantity",ordered);row.put("shippedQuantity",out);row.put("returnedQuantity",returned);row.put("remainingQuantity",remaining);row.put("fulfillmentStage",stage(source,remaining,deliveryProgress));rows.add(row);}
        return result(rows.isEmpty()?"EMPTY":"SUCCESS",rows.isEmpty()?"未查询到符合条件的订单":"已查询当前客户授权部门内的订单",rows,orderColumns());
    }

    private Map<String,Object> oneProduct(CustomerAssistantPrincipal p,Map<String,Object> args) {
        Long productId=number(args.get("productId"));String keyword=text(args.get("keyword"));
        MapSqlParameterSource params=new MapSqlParameterSource().addValue("tenantId",p.getTenantId()).addValue("syncSuccess",0).addValue("enabled",0);
        String sql="SELECT p.id,p.name,p.code,p.standard,m.mall_spu_id FROM erp_product p JOIN erp_mall_product_mapping m ON m.erp_product_id=p.id AND m.tenant_id=p.tenant_id AND m.deleted=0 AND m.sync_status=:syncSuccess AND m.mall_spu_id IS NOT NULL WHERE p.tenant_id=:tenantId AND p.deleted=0 AND p.status=:enabled";
        if(productId!=null){sql+=" AND p.id=:productId";params.addValue("productId",productId);}else{
            if(!StringUtils.hasText(keyword)) throw new AssistantFailure("MISSING_PRODUCT","请提供商品名称、配件编码、厂商编码或规格");
            sql+=" AND (p.name LIKE :keyword ESCAPE '!' OR p.code LIKE :keyword ESCAPE '!' OR p.factory_code LIKE :keyword ESCAPE '!' OR p.standard LIKE :keyword ESCAPE '!')";
            params.addValue("keyword","%"+escapeLike(keyword.trim())+"%");
        }
        sql+=" ORDER BY CASE WHEN p.code=:exact THEN 0 WHEN p.name=:exact THEN 1 ELSE 2 END,p.id LIMIT 11";params.addValue("exact",StringUtils.hasText(keyword)?keyword.trim():"");
        List<Map<String,Object>> products=jdbc.queryForList(sql,params);
        if(products.isEmpty()) throw new AssistantFailure("PRODUCT_NOT_FOUND","未找到已启用且已开放商城查询的商品");
        if(products.size()>1){List<Map<String,Object>> candidates=products.stream().limit(10).map(this::candidate).collect(Collectors.toList());throw new ProductChoice(AssistantClarification.entityChoice("找到多个商品，请选择要查询的商品",candidates));}
        return products.get(0);
    }
    private Map<Long,ErpSalePickDeliverySummaryRespVO> pickProgress(List<Map<String,Object>> rows){List<Long> ids=rows.stream().filter(r->"SALE_CART".equals(r.get("source_kind"))).map(r->number(r.get("id"))).filter(Objects::nonNull).collect(Collectors.toList());return ids.isEmpty()?Collections.emptyMap():pickDeliveryService.getSummaryMapBySaleCartIds(ids);}
    private String stage(Map<String,Object> row,BigDecimal remaining,ErpSalePickDeliverySummaryRespVO progress){if(remaining.signum()==0)return "已完成";if(progress!=null&&progress.getDeliveryStatus()!=null&&progress.getDeliveryStatus()>0)return "配送中";if(progress!=null&&progress.getPickStatus()!=null&&progress.getPickStatus()>0)return "拣货中";if(decimal(row.get("out_count")).signum()>0)return "部分出库";int status=intValue(row.get("status"),0);if("SALE_CART".equals(row.get("source_kind"))){if(status<20)return "待提交";if(status<40)return "审核中";return "待履约";}return status>=20?"待履约":"审核中";}
    private String status(Map<String,Object> row){int s=intValue(row.get("status"),0);if("SALE_CART".equals(row.get("source_kind"))){if(s==90)return "已取消";if(s<20)return "待提交";if(s<40)return "审核中";if(s==40)return "已审核";if(s==50)return "履约中";return "已处理";}return s>=20?"已审核":"未审核";}
    private Map<String,Object> result(String status,String text,List<Map<String,Object>> rows,List<Map<String,Object>> columns){Map<String,Object> v=new LinkedHashMap<>();v.put("status",status);v.put("answerText",text);v.put("summary",Collections.emptyList());v.put("rows",rows);v.put("columns",columns);v.put("warnings",Collections.emptyList());return v;}
    private Map<String,Object> productView(Map<String,Object> p){Map<String,Object> v=new LinkedHashMap<>();v.put("name",p.get("name"));v.put("code",p.get("code"));v.put("standard",p.get("standard"));return v;}
    private Map<String,Object> candidate(Map<String,Object> p){Map<String,Object> v=productView(p);v.put("id",number(p.get("id")));v.put("label",String.valueOf(p.get("name"))+(p.get("standard")==null?"":" / "+p.get("standard")));return v;}
    private List<Map<String,Object>> priceColumns(){return Arrays.asList(column("name","商品"),column("code","配件编码"),column("standard","规格"),column("customerPrice","客户价"));}
    private List<Map<String,Object>> orderColumns(){return Arrays.asList(column("orderNo","订单号"),column("type","类型"),column("date","日期"),column("amount","金额"),column("status","状态"),column("orderedQuantity","订购数量"),column("shippedQuantity","已出库"),column("returnedQuantity","退货"),column("remainingQuantity","剩余"),column("fulfillmentStage","履约阶段"));}
    private Map<String,Object> column(String key,String title){Map<String,Object> v=new LinkedHashMap<>();v.put("key",key);v.put("title",title);return v;}
    private String escapeLike(String value){return value.replace("!","!!").replace("%","!%").replace("_","!_");}
    private String text(Object v){return v==null?null:String.valueOf(v);}
    private Long number(Object v){if(v instanceof Number)return ((Number)v).longValue();try{return v==null?null:Long.valueOf(String.valueOf(v));}catch(Exception e){return null;}}
    private int intValue(Object v,int fallback){if(v instanceof Number)return ((Number)v).intValue();try{return Integer.parseInt(String.valueOf(v));}catch(Exception e){return fallback;}}
    private BigDecimal decimal(Object v){if(v==null)return BigDecimal.ZERO;if(v instanceof BigDecimal)return (BigDecimal)v;return new BigDecimal(String.valueOf(v));}

    public static class ProductChoice extends RuntimeException {private final Map<String,Object> response;public ProductChoice(Map<String,Object> response){this.response=response;}public Map<String,Object> getResponse(){return response;}}
}
