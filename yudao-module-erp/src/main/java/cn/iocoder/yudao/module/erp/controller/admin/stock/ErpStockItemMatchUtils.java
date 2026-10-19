package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchRespVO;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存单据整单明细匹配工具。匹配页码始终以排除待删除行后的 {@code id ASC} 为准。
 */
@UtilityClass
public class ErpStockItemMatchUtils {

    public static <T> ErpStockItemMatchRespVO match(List<T> sourceItems,
                                                    ErpStockItemMatchReqVO reqVO,
                                                    Function<T, Long> idGetter,
                                                    BiPredicate<T, ErpStockItemMatchReqVO.Key> matcher) {
        Set<Long> excludedIds = reqVO.getExcludedItemIds() == null
                ? Collections.emptySet()
                : reqVO.getExcludedItemIds().stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        List<T> items = sourceItems == null ? Collections.emptyList() : sourceItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> !excludedIds.contains(idGetter.apply(item)))
                .sorted(Comparator.comparing(idGetter, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        ErpStockItemMatchRespVO result = new ErpStockItemMatchRespVO();
        for (int keyIndex = 0; keyIndex < reqVO.getKeys().size(); keyIndex++) {
            ErpStockItemMatchReqVO.Key key = reqVO.getKeys().get(keyIndex);
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                T item = items.get(itemIndex);
                if (matcher.test(item, key)) {
                    result.getMatches().add(new ErpStockItemMatchRespVO.Match(
                            keyIndex, idGetter.apply(item), itemIndex / pageSize + 1));
                    break;
                }
            }
        }
        return result;
    }

    public static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

}
