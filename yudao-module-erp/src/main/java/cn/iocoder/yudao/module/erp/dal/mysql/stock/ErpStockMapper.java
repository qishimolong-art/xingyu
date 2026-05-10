package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 产品库存 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockMapper extends BaseMapperX<ErpStockDO> {

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO) {
        return selectPage(reqVO, (Collection<Long>) null);
    }

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO, Collection<Long> productIdFilter) {
        LambdaQueryWrapperX<ErpStockDO> wrapper = new LambdaQueryWrapperX<ErpStockDO>()
                .eqIfPresent(ErpStockDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStockDO::getWarehouseId, reqVO.getWarehouseId())
                .geIfPresent(ErpStockDO::getCount, reqVO.getCountMin())
                .leIfPresent(ErpStockDO::getCount, reqVO.getCountMax());
        if (productIdFilter != null) {
            if (productIdFilter.isEmpty()) {
                // 产品过滤命中 0 条 → 直接返回空分页，而不是查所有
                return PageResult.empty(0L);
            }
            wrapper.in(ErpStockDO::getProductId, productIdFilter);
        }
        // 库存数筛选
        if (reqVO.getCountFilter() != null) {
            if (reqVO.getCountFilter() == 1) {
                wrapper.gt(ErpStockDO::getCount, BigDecimal.ZERO);
            } else if (reqVO.getCountFilter() == 2) {
                wrapper.eq(ErpStockDO::getCount, BigDecimal.ZERO);
            }
        }
        if (Boolean.TRUE.equals(reqVO.getPositiveCountOnly())) {
            wrapper.gt(ErpStockDO::getCount, BigDecimal.ZERO);
        }
        wrapper.orderByDesc(ErpStockDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default ErpStockDO selectByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        return selectOne(ErpStockDO::getProductId, productId,
                ErpStockDO::getWarehouseId, warehouseId);
    }

    default int updateCountIncrement(Long id, BigDecimal count, boolean negativeEnable) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id);
        if (count.compareTo(BigDecimal.ZERO) > 0) {
            updateWrapper.setSql("count = count + " + count);
        } else if (count.compareTo(BigDecimal.ZERO) < 0) {
            if (!negativeEnable) {
                updateWrapper.ge(ErpStockDO::getCount, count.abs());
            }
            updateWrapper.setSql("count = count - " + count.abs());
        }
        return update(null, updateWrapper);
    }

    /**
     * 原子更新库存的 count 与 costPrice、costAmount
     * 采用乐观锁：仅当 count == expectedOldCount 时才更新，避免并发写成本时覆盖
     *
     * @return 更新行数（0 表示并发冲突，需上层重试）
     */
    default int updateCountAndCost(Long id, BigDecimal expectedOldCount, BigDecimal newCount,
                                   BigDecimal newCostPrice, BigDecimal newCostAmount, boolean negativeEnable) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .eq(ErpStockDO::getCount, expectedOldCount)
                .set(ErpStockDO::getCount, newCount)
                .set(ErpStockDO::getCostPrice, newCostPrice)
                .set(ErpStockDO::getCostAmount, newCostAmount);
        if (!negativeEnable && newCount.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        return update(null, updateWrapper);
    }

    default BigDecimal selectSumByProductId(Long productId) {
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("SUM(count) AS sum_count")
                .eq("product_id", productId));
        // 获得数量
        if (CollUtil.isEmpty(result)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(MapUtil.getDouble(result.get(0), "sum_count", 0D));
    }

    /**
     * 按 product_id 批量聚合库存数量
     *
     * @param productIds 产品编号集合
     * @return Map&lt;productId, sum(count)&gt;
     */
    default Map<Long, BigDecimal> selectSumMapByProductIds(Collection<Long> productIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (CollUtil.isEmpty(productIds)) {
            return map;
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("product_id AS product_id, SUM(count) AS sum_count")
                .in("product_id", productIds)
                .groupBy("product_id"));
        if (CollUtil.isEmpty(rows)) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Long productId = MapUtil.getLong(row, "product_id");
            if (productId == null) {
                continue;
            }
            map.put(productId, BigDecimal.valueOf(MapUtil.getDouble(row, "sum_count", 0D)));
        }
        return map;
    }

}