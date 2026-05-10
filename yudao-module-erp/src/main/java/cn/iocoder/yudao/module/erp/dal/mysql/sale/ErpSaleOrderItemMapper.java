package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 销售订单明项目 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOrderItemMapper extends BaseMapperX<ErpSaleOrderItemDO> {

    default List<ErpSaleOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(ErpSaleOrderItemDO::getOrderId, orderId);
    }

    default List<ErpSaleOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(ErpSaleOrderItemDO::getOrderId, orderIds);
    }

    default int deleteByOrderId(Long orderId) {
        return delete(ErpSaleOrderItemDO::getOrderId, orderId);
    }

    /**
     * 统计每个产品的"占用数"（= SUM(count - outCount)），按 product_id 分组
     */
    default Map<Long, BigDecimal> selectOccupiedCountMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(
                new QueryWrapper<ErpSaleOrderItemDO>()
                        .select("product_id, SUM(COALESCE(count,0) - COALESCE(out_count,0)) AS occupied")
                        .in("product_id", productIds)
                        .groupBy("product_id"));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long pid = (Long) row.get("product_id");
            Object val = row.get("occupied");
            BigDecimal amount = val == null ? BigDecimal.ZERO
                    : (val instanceof BigDecimal ? (BigDecimal) val : new BigDecimal(val.toString()));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                result.put(pid, amount);
            }
        }
        return result;
    }

}