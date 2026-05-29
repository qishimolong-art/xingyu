package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 付款单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpFinancePaymentItemMapper extends BaseMapperX<ErpFinancePaymentItemDO> {

    default List<ErpFinancePaymentItemDO> selectListByPaymentId(Long paymentId) {
        return selectList(ErpFinancePaymentItemDO::getPaymentId, paymentId);
    }

    default List<ErpFinancePaymentItemDO> selectListByPaymentIds(Collection<Long> paymentIds) {
        return selectList(ErpFinancePaymentItemDO::getPaymentId, paymentIds);
    }

    default BigDecimal selectPaymentPriceSumByBizIdAndBizType(Long bizId, Integer bizType) {
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinancePaymentItemDO>()
                .select("SUM(payment_price) AS payment_price_sum")
                .eq("biz_id", bizId)
                .eq("biz_type", bizType));
        // 获得数量
        if (CollUtil.isEmpty(result)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(MapUtil.getDouble(result.get(0), "payment_price_sum", 0D));
    }

    default Map<Long, BigDecimal> selectPaymentPriceSumMapByBizIdsAndBizType(Collection<Long> bizIds, Integer bizType) {
        if (CollUtil.isEmpty(bizIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinancePaymentItemDO>()
                .select("biz_id, SUM(payment_price) AS payment_price_sum")
                .eq("biz_type", bizType)
                .in("biz_id", bizIds)
                .groupBy("biz_id"));
        Map<Long, BigDecimal> resultMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            Number bizId = (Number) row.get("biz_id");
            if (bizId == null) {
                continue;
            }
            resultMap.put(bizId.longValue(), toBigDecimal(row.get("payment_price_sum")));
        }
        return resultMap;
    }

    static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }

}
