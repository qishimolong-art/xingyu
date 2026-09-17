package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 仅允许枚举内的真实单据，通过 MyBatis 租户及部门权限拦截器读取。 */
@Component
public class ErpVoucherSourceReader {
    @Resource private ApplicationContext applicationContext;
    @Data public static class Source {
        private int type;
        private Long id;
        private Map<String,Object> header;
        private List<Map<String,Object>> details = new ArrayList<>();
        private List<Map<String,Object>> stocks = new ArrayList<>();
        private Map<Long,Long> categories = new TreeMap<>();
        public String version() { return DigestUtil.sha256Hex(JsonUtils.toJsonString(this)); }
        public String no() { return Objects.toString(header.get("no"), ""); }
        public BigDecimal amount() { return decimal(header, type==6?"receiptPrice":type==12?"paymentPrice":type==18?"transferPrice":header.containsKey("actualAmount")?"actualAmount":"totalPrice"); }
        public LocalDate date() {
            for(String k:Arrays.asList("bizTime","inTime","outTime","returnTime","receiptTime","paymentTime","transferTime")) {
                Object v=header.get(k); if(v instanceof LocalDateTime) return ((LocalDateTime)v).toLocalDate();
                if(v instanceof java.time.Instant) return ((java.time.Instant)v).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
            throw problem("待补充：来源单据缺少业务日期");
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public Map<String,Object> one(String bean, Long id) {
        if(id==null) throw problem("待补充：缺少对象编号");
        Object value=applicationContext.getBean(bean,BaseMapperX.class).selectById(id);
        if(value==null) throw problem("对象不存在或无访问权限："+id);
        return new TreeMap<>(BeanUtil.beanToMap(value));
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public List<Map<String,Object>> list(String bean, QueryWrapper query) {
        List<Object> values=applicationContext.getBean(bean,BaseMapperX.class).selectList(query);
        List<Map<String,Object>> result=new ArrayList<>();
        for(Object v:values) result.add(new TreeMap<>(BeanUtil.beanToMap(v)));
        return result;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public cn.iocoder.yudao.framework.common.pojo.PageResult<Map<String,Object>> page(String bean,QueryWrapper query,Integer pageNo,Integer pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page page=new com.baomidou.mybatisplus.extension.plugins.pagination.Page(pageNo==null?1:pageNo,pageSize==null?10:pageSize);
        query.orderByDesc("id");
        applicationContext.getBean(bean,BaseMapperX.class).selectPage(page,query);
        List<Map<String,Object>> rows=new ArrayList<>();for(Object v:page.getRecords())rows.add(new TreeMap<>(BeanUtil.beanToMap(v)));
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(rows,page.getTotal());
    }
    public void lock(Integer type, Long id) {
        Map<Integer,String> beans=new HashMap<>();
        beans.put(2,"SaleOut");beans.put(3,"SaleReturn");beans.put(8,"PurchaseIn");beans.put(9,"PurchaseReturn");
        beans.put(4,"OtherReceivable");beans.put(10,"OtherPayable");beans.put(6,"FinanceReceipt");beans.put(12,"FinancePayment");
        beans.put(16,"StockOut");beans.put(17,"StockIn");beans.put(18,"FinanceTransfer");beans.put(21,"PreReceipt");beans.put(22,"PrePayment");beans.put(23,"PreReceivable");
        if(!beans.containsKey(type)||id==null) throw problem("不支持的凭证来源");
        if(list("erp"+beans.get(type)+"Mapper",new QueryWrapper<>().eq("id",id).last("FOR UPDATE")).isEmpty()) throw problem("来源不存在或无权限");
    }
    public Source read(Integer type, Long id) {
        if(type==null || !ErpVoucherTemplates.TYPES.contains(type)) throw problem("需人工处理：此来源尚未接入");
        String prefix, itemPrefix=null, fk=null; Integer stockType=null;
        switch(type) {
            case 2: prefix="SaleOut"; itemPrefix=prefix; fk="out_id"; stockType=50; break;
            case 3: prefix="SaleReturn"; itemPrefix=prefix; fk="return_id"; stockType=60; break;
            case 8: prefix="PurchaseIn"; itemPrefix=prefix; fk="in_id"; stockType=70; break;
            case 9: prefix="PurchaseReturn"; itemPrefix=prefix; fk="return_id"; stockType=80; break;
            case 16: prefix="StockOut"; itemPrefix=prefix; fk="out_id"; stockType=20; break;
            case 17: prefix="StockIn"; itemPrefix=prefix; fk="in_id"; stockType=10; break;
            case 4: prefix="OtherReceivable"; break;
            case 10: prefix="OtherPayable"; break;
            case 6: prefix="FinanceReceipt"; break;
            case 12: prefix="FinancePayment"; break;
            case 18: prefix="FinanceTransfer"; break;
            case 21: prefix="PreReceipt"; break;
            case 22: prefix="PrePayment"; break;
            case 23: prefix="PreReceivable"; break;
            default: throw problem("不支持的来源");
        }
        Source s=new Source(); s.setType(type); s.setId(id); s.setHeader(one("erp"+prefix+"Mapper",id));
        if(!Integer.valueOf(20).equals(s.getHeader().get("status"))) throw problem("单据尚未审核，不能生成凭证");
        if(itemPrefix!=null) {
            s.setDetails(list("erp"+itemPrefix+"ItemMapper",new QueryWrapper<>().eq(fk,id).orderByAsc("id")));
            for(Map<String,Object> line:s.getDetails()) {
                Long product=longValue(line,"productId");
                Map<String,Object> p=one("erpProductMapper",product);
                s.getCategories().put(product,longValue(p,"categoryId"));
            }
            s.setStocks(list("erpStockRecordMapper",new QueryWrapper<>().eq("biz_id",id).in("biz_type",stockType,stockType+1).orderByAsc("id")));
        }
        return s;
    }
    public static Long longValue(Map<String,Object> m,String key) {
        Object value=m.get(key); return value==null?null:Long.valueOf(value.toString());
    }
    public static BigDecimal decimal(Map<String,Object> m,String key) {
        Object value=m.get(key); if(value==null) throw problem("待补充：来源缺少金额 "+key);
        return new BigDecimal(value.toString());
    }
    public static BigDecimal optional(Map<String,Object> m,String key) {
        return m.get(key)==null?BigDecimal.ZERO:decimal(m,key);
    }
    public static ServiceException problem(String message) { return new ServiceException(1_030_090_001,message); }
}
