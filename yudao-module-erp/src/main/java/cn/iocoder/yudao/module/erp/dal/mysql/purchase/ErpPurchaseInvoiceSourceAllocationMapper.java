package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceSourceAllocationDO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

@Mapper
public interface ErpPurchaseInvoiceSourceAllocationMapper
        extends BaseMapperX<ErpPurchaseInvoiceSourceAllocationDO> {

    default List<ErpPurchaseInvoiceSourceAllocationDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(ErpPurchaseInvoiceSourceAllocationDO::getInvoiceId, invoiceId);
    }

    default Map<Long, BigDecimal> selectAllocatedCountMap(Integer sourceType, Collection<Long> sourceItemIds) {
        if (sourceType == null || CollUtil.isEmpty(sourceItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpPurchaseInvoiceSourceAllocationDO>()
                .select("source_item_id", "SUM(allocated_count) AS allocated_count")
                .eq("source_type", sourceType)
                .in("source_item_id", sourceItemIds)
                .groupBy("source_item_id"));
        return convertMap(rows, row -> ((Number) row.get("source_item_id")).longValue(),
                row -> row.get("allocated_count") == null ? BigDecimal.ZERO
                        : new BigDecimal(row.get("allocated_count").toString()));
    }

    default List<ErpPurchaseInvoiceSourceAllocationDO> selectListBySourceItems(
            Integer sourceType, Collection<Long> sourceItemIds) {
        if (sourceType == null || CollUtil.isEmpty(sourceItemIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpPurchaseInvoiceSourceAllocationDO>()
                .eq(ErpPurchaseInvoiceSourceAllocationDO::getSourceType, sourceType)
                .in(ErpPurchaseInvoiceSourceAllocationDO::getSourceItemId, sourceItemIds));
    }
}
