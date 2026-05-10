package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 采购退货项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseReturnItemMapper extends BaseMapperX<ErpPurchaseReturnItemDO> {

    default List<ErpPurchaseReturnItemDO> selectListByReturnId(Long returnId) {
        return selectList(ErpPurchaseReturnItemDO::getReturnId, returnId);
    }

    default List<ErpPurchaseReturnItemDO> selectListByReturnIds(Collection<Long> returnIds) {
        return selectList(ErpPurchaseReturnItemDO::getReturnId, returnIds);
    }

    default int deleteByReturnId(Long returnId) {
        return delete(ErpPurchaseReturnItemDO::getReturnId, returnId);
    }

    /**
     * 基于采购订单编号，查询每个采购订单项的退货数量之和
     *
     * @param returnIds 入库订单项编号数组
     * @return key：采购订单项编号；value：退货数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpPurchaseReturnItemDO>()
                .select("order_item_id, SUM(count) AS sum_count")
                .groupBy("order_item_id")
                .in("return_id", returnIds));
        // 获得数量
        return convertMap(result, obj -> (Long) obj.get("order_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

    /**
     * 按 source_in_item_id 分组，查询每个原入库项已累计退货的数量（仅统计已审批单据）
     *
     * @param sourceInItemIds 原入库项 ID 集合
     * @return Map&lt;sourceInItemId, 已退数量合计&gt;
     */
    default Map<Long, BigDecimal> selectReturnedCountMapBySourceInItemIds(Collection<Long> sourceInItemIds) {
        if (CollUtil.isEmpty(sourceInItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpPurchaseReturnItemDO>()
                .select("source_in_item_id, SUM(count) AS sum_count")
                .groupBy("source_in_item_id")
                .in("source_in_item_id", sourceInItemIds));
        return convertMap(result,
                obj -> (Long) obj.get("source_in_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    /**
     * 按 source_in_item_id 分组，查询其他退货单（排除 excludeReturnId）的累计退货数量
     *
     * @param sourceInItemIds 原入库项 ID 集合
     * @param excludeReturnId 要排除的当前退货单 ID（新建场景传 null）
     * @return Map&lt;sourceInItemId, 其他退货单累计退货数量&gt;
     */
    default Map<Long, BigDecimal> selectReturnedCountMapBySourceInItemIdsExcludeReturn(
            Collection<Long> sourceInItemIds, Long excludeReturnId) {
        if (CollUtil.isEmpty(sourceInItemIds)) {
            return Collections.emptyMap();
        }
        QueryWrapper<ErpPurchaseReturnItemDO> wrapper = new QueryWrapper<ErpPurchaseReturnItemDO>()
                .select("source_in_item_id, SUM(count) AS sum_count")
                .groupBy("source_in_item_id")
                .in("source_in_item_id", sourceInItemIds);
        if (excludeReturnId != null) {
            wrapper.ne("return_id", excludeReturnId);
        }
        List<Map<String, Object>> result = selectMaps(wrapper);
        return convertMap(result,
                obj -> (Long) obj.get("source_in_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

}