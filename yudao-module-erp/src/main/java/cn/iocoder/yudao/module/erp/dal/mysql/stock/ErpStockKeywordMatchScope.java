package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 单个库存关键词片段在各搜索维度中的命中范围。
 */
@Data
@AllArgsConstructor
public class ErpStockKeywordMatchScope {

    private Collection<Long> productIds;
    private Collection<Long> warehouseIds;
    private Map<Long, Set<Long>> batchStockKeyMap;

}
