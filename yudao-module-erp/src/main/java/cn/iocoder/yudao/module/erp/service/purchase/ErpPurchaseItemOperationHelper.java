package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 采购单据子表增量操作工具。
 */
final class ErpPurchaseItemOperationHelper {

    static final String OPERATION_INSERT = "insert";
    static final String OPERATION_UPDATE = "update";
    static final String OPERATION_DELETE = "delete";

    private ErpPurchaseItemOperationHelper() {
    }

    static <T> boolean useIncrementalItems(List<T> items, Function<T, String> operationGetter,
                                           ErrorCode invalidOperationCode) {
        if (CollUtil.isEmpty(items)) {
            return true;
        }
        boolean hasOperation = false;
        boolean hasEmptyOperation = false;
        for (T item : items) {
            String operation = StrUtil.trimToEmpty(operationGetter.apply(item));
            if (StrUtil.isBlank(operation)) {
                hasEmptyOperation = true;
                continue;
            }
            normalizeOperation(operation, invalidOperationCode);
            hasOperation = true;
        }
        if (hasOperation && hasEmptyOperation) {
            throw exception(invalidOperationCode);
        }
        return hasOperation;
    }

    static <REQ, OLD> RequestChangeSet<REQ> buildRequestChangeSet(
            List<REQ> items,
            List<OLD> oldItems,
            Class<REQ> requestClass,
            Function<REQ, Long> itemIdGetter,
            BiConsumer<REQ, Long> itemIdSetter,
            Function<REQ, String> operationGetter,
            Function<OLD, Long> oldItemIdGetter,
            ErrorCode invalidOperationCode,
            ErrorCode itemNotExistsCode) {
        List<REQ> finalItems = BeanUtils.toBean(oldItems, requestClass);
        Set<Long> updateIds = new LinkedHashSet<>();
        Set<Long> deleteIds = new LinkedHashSet<>();
        List<REQ> insertItems = new ArrayList<>();
        Map<Long, OLD> oldItemMap = convertMap(oldItems, oldItemIdGetter);
        if (CollUtil.isEmpty(items)) {
            return new RequestChangeSet<>(finalItems, insertItems, updateIds, deleteIds);
        }

        for (REQ item : items) {
            String operation = normalizeOperation(operationGetter.apply(item), invalidOperationCode);
            if (OPERATION_INSERT.equals(operation)) {
                REQ insertItem = BeanUtils.toBean(item, requestClass);
                itemIdSetter.accept(insertItem, null);
                insertItems.add(insertItem);
                continue;
            }
            Long itemId = itemIdGetter.apply(item);
            if (itemId == null || !oldItemMap.containsKey(itemId)) {
                throw exception(itemNotExistsCode);
            }
            if (OPERATION_UPDATE.equals(operation)) {
                replaceFinalItem(finalItems, item, itemIdGetter, itemNotExistsCode);
                updateIds.add(itemId);
                continue;
            }
            finalItems.removeIf(finalItem -> Objects.equals(itemIdGetter.apply(finalItem), itemId));
            deleteIds.add(itemId);
            updateIds.remove(itemId);
        }
        finalItems.addAll(insertItems);
        return new RequestChangeSet<>(finalItems, insertItems, updateIds, deleteIds);
    }

    private static String normalizeOperation(String operation, ErrorCode invalidOperationCode) {
        String normalized = StrUtil.trimToEmpty(operation).toLowerCase(Locale.ROOT);
        if (OPERATION_INSERT.equals(normalized) || OPERATION_UPDATE.equals(normalized)
                || OPERATION_DELETE.equals(normalized)) {
            return normalized;
        }
        throw exception(invalidOperationCode);
    }

    private static <REQ> void replaceFinalItem(List<REQ> finalItems, REQ item,
                                               Function<REQ, Long> itemIdGetter,
                                               ErrorCode itemNotExistsCode) {
        Long itemId = itemIdGetter.apply(item);
        for (int i = 0; i < finalItems.size(); i++) {
            if (Objects.equals(itemIdGetter.apply(finalItems.get(i)), itemId)) {
                finalItems.set(i, item);
                return;
            }
        }
        throw exception(itemNotExistsCode);
    }

    static final class RequestChangeSet<REQ> {

        private final List<REQ> finalItems;
        private final List<REQ> insertItems;
        private final Set<Long> updateIds;
        private final Set<Long> deleteIds;

        private RequestChangeSet(List<REQ> finalItems, List<REQ> insertItems,
                                 Set<Long> updateIds, Set<Long> deleteIds) {
            this.finalItems = finalItems;
            this.insertItems = insertItems;
            this.updateIds = updateIds;
            this.deleteIds = deleteIds;
        }

        List<REQ> getFinalItems() {
            return finalItems;
        }

        Set<Long> getUpdateIds() {
            return updateIds;
        }

        Set<Long> getDeleteIds() {
            return deleteIds;
        }
    }
}
