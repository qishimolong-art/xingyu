package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Masks sale form response fields by current user's field permissions.
 */
@Component
public class ErpSaleFieldPermissionMasker {

    private static final Integer UNKNOWN_PRICE_LEVEL_CACHE_KEY = Integer.MIN_VALUE;
    private static final Set<String> SALE_DETAIL_VISIBLE_PRICE_FIELDS = new HashSet<>(Arrays.asList(
            "productPrice", "salePrice", "lastSalePrice", "originalProductPrice", "oldPrice", "newPrice",
            "totalProductPrice", "totalPrice", "taxPrice", "totalTaxPrice", "discountPrice", "allowancePrice",
            "feeAmount", "otherPrice", "freightAmount", "depositPrice", "receiptPrice", "refundPrice",
            "reductionAmount", "afterReductionPrice", "afterReductionAmount", "actualSaleAmount",
            "totalOriginalPrice", "totalAdjustedPrice", "adjustPrice", "totalAdjustPrice", "adjustAmount",
            "extraFee", "invoiceAmount", "billAmount", "cancelAmount", "afterCancelAmount"));

    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpCustomerMapper customerMapper;

    public void maskForm(String module, Object vo) {
        if (vo == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module, vo);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(vo, hiddenFieldSet, "", false);
    }

    public void maskForms(String module, Collection<?> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, Integer> customerPriceLevelCache = new HashMap<>();
        Map<Integer, Set<String>> hiddenFieldCache = new HashMap<>();
        list.forEach(item -> {
            Set<String> hiddenFieldSet = getHiddenFieldSet(module, item, customerPriceLevelCache, hiddenFieldCache);
            if (CollUtil.isNotEmpty(hiddenFieldSet)) {
                maskBean(item, hiddenFieldSet, "", false);
            }
        });
    }

    public void maskFormWithItems(String module, Object vo) {
        if (vo == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module, vo);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(vo, hiddenFieldSet, "", false);
        maskItems(vo, hiddenFieldSet);
    }

    public void maskSaleDetailFormWithItems(String module, Object vo) {
        if (vo == null) {
            return;
        }
        Set<String> hiddenFieldSet = withoutSaleDetailVisiblePriceFields(getHiddenFieldSet(module, vo));
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(vo, hiddenFieldSet, "", false);
        maskItems(vo, hiddenFieldSet);
    }

    public void maskSaleDetailFormsWithItems(String module, Collection<?> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, Integer> customerPriceLevelCache = new HashMap<>();
        Map<Integer, Set<String>> hiddenFieldCache = new HashMap<>();
        for (Object item : list) {
            Set<String> hiddenFieldSet = withoutSaleDetailVisiblePriceFields(
                    getHiddenFieldSet(module, item, customerPriceLevelCache, hiddenFieldCache));
            if (CollUtil.isEmpty(hiddenFieldSet)) {
                continue;
            }
            maskBean(item, hiddenFieldSet, "", false);
            maskItems(item, hiddenFieldSet);
        }
    }

    public void maskFormsWithItems(String module, Collection<?> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, Integer> customerPriceLevelCache = new HashMap<>();
        Map<Integer, Set<String>> hiddenFieldCache = new HashMap<>();
        for (Object item : list) {
            Set<String> hiddenFieldSet = getHiddenFieldSet(module, item, customerPriceLevelCache, hiddenFieldCache);
            if (CollUtil.isEmpty(hiddenFieldSet)) {
                continue;
            }
            maskBean(item, hiddenFieldSet, "", false);
            maskItems(item, hiddenFieldSet);
        }
    }

    public void maskExportRows(String module, Collection<?> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, Integer> customerPriceLevelCache = new HashMap<>();
        Map<Integer, Set<String>> hiddenFieldCache = new HashMap<>();
        rows.forEach(row -> {
            Set<String> hiddenFieldSet = getHiddenFieldSet(module, row, customerPriceLevelCache, hiddenFieldCache);
            if (CollUtil.isNotEmpty(hiddenFieldSet)) {
                maskBean(row, hiddenFieldSet, "", true);
            }
        });
    }

    public void maskSaleDetailExportRows(String module, Collection<?> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, Integer> customerPriceLevelCache = new HashMap<>();
        Map<Integer, Set<String>> hiddenFieldCache = new HashMap<>();
        rows.forEach(row -> {
            Set<String> hiddenFieldSet = withoutSaleDetailVisiblePriceFields(
                    getHiddenFieldSet(module, row, customerPriceLevelCache, hiddenFieldCache));
            if (CollUtil.isNotEmpty(hiddenFieldSet)) {
                maskBean(row, hiddenFieldSet, "", true);
            }
        });
    }

    public void maskSelectRows(String module, Collection<?> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, Integer> customerPriceLevelCache = new HashMap<>();
        Map<Integer, Set<String>> hiddenFieldCache = new HashMap<>();
        rows.forEach(row -> {
            Set<String> hiddenFieldSet = getHiddenFieldSet(module, row, customerPriceLevelCache, hiddenFieldCache);
            if (CollUtil.isNotEmpty(hiddenFieldSet)) {
                maskBean(row, hiddenFieldSet, "select_", false);
            }
        });
    }

    public void maskSaleDetailSelectRows(String module, Collection<?> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, Integer> customerPriceLevelCache = new HashMap<>();
        Map<Integer, Set<String>> hiddenFieldCache = new HashMap<>();
        rows.forEach(row -> {
            Set<String> hiddenFieldSet = withoutSaleDetailVisiblePriceFields(
                    getHiddenFieldSet(module, row, customerPriceLevelCache, hiddenFieldCache));
            if (CollUtil.isNotEmpty(hiddenFieldSet)) {
                maskBean(row, hiddenFieldSet, "select_", false);
            }
        });
    }

    public void clearHiddenFields(String module, Object target) {
        if (target == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module, target);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(target, hiddenFieldSet, "", false);
    }

    public void clearSaleDetailHiddenFields(String module, Object target) {
        if (target == null) {
            return;
        }
        Set<String> hiddenFieldSet = getSaleDetailHiddenFieldSet(module, target);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(target, hiddenFieldSet, "", false);
    }

    public void clearHiddenItemFields(String module, Collection<?> targetItems) {
        clearHiddenItemFields(module, null, targetItems);
    }

    public void clearHiddenItemFields(String module, Object context, Collection<?> targetItems) {
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module, context);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        targetItems.forEach(item -> maskBean(item, hiddenFieldSet, "item_", false));
    }

    public void clearSaleDetailHiddenItemFields(String module, Object context, Collection<?> targetItems) {
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getSaleDetailHiddenFieldSet(module, context);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        targetItems.forEach(item -> maskBean(item, hiddenFieldSet, "item_", false));
    }

    public void preserveHiddenFields(String module, Object target, Object source) {
        if (target == null || source == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module, target);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        copyHiddenFields(target, source, hiddenFieldSet, "");
    }

    public void preserveSaleDetailHiddenFields(String module, Object target, Object source) {
        if (target == null || source == null) {
            return;
        }
        Set<String> hiddenFieldSet = getSaleDetailHiddenFieldSet(module, target);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        copyHiddenFields(target, source, hiddenFieldSet, "");
    }

    public void preserveHiddenItemFields(String module, Collection<?> targetItems, Collection<?> sourceItems) {
        preserveHiddenItemFields(module, null, targetItems, sourceItems);
    }

    public void preserveHiddenItemFields(String module, Object context,
                                         Collection<?> targetItems, Collection<?> sourceItems) {
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module, context);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        Map<Object, Object> sourceItemMap = CollUtil.isEmpty(sourceItems) ? Collections.emptyMap()
                : sourceItems.stream()
                        .filter(Objects::nonNull)
                        .filter(item -> getFieldValue(item, "id") != null)
                        .collect(Collectors.toMap(item -> getFieldValue(item, "id"), Function.identity(), (a, b) -> a));
        for (Object targetItem : targetItems) {
            Object id = getFieldValue(targetItem, "id");
            Object sourceItem = id == null ? null : sourceItemMap.get(id);
            if (sourceItem == null) {
                maskBean(targetItem, hiddenFieldSet, "item_", false);
                continue;
            }
            copyHiddenFields(targetItem, sourceItem, hiddenFieldSet, "item_");
        }
    }

    public void preserveSaleDetailHiddenItemFields(String module, Object context,
                                                   Collection<?> targetItems, Collection<?> sourceItems) {
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getSaleDetailHiddenFieldSet(module, context);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        Map<Object, Object> sourceItemMap = CollUtil.isEmpty(sourceItems) ? Collections.emptyMap()
                : sourceItems.stream()
                        .filter(Objects::nonNull)
                        .filter(item -> getFieldValue(item, "id") != null)
                        .collect(Collectors.toMap(item -> getFieldValue(item, "id"), Function.identity(), (a, b) -> a));
        for (Object targetItem : targetItems) {
            Object id = getFieldValue(targetItem, "id");
            Object sourceItem = id == null ? null : sourceItemMap.get(id);
            if (sourceItem == null) {
                maskBean(targetItem, hiddenFieldSet, "item_", false);
                continue;
            }
            copyHiddenFields(targetItem, sourceItem, hiddenFieldSet, "item_");
        }
    }

    public boolean isFieldHidden(String module, String fieldKey) {
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        return hiddenFieldSet.contains(fieldKey) || hiddenFieldSet.contains("col_" + fieldKey);
    }

    public Set<String> getHiddenFieldSet(String module) {
        return getHiddenFieldSetByPriceLevel(module, null);
    }

    public Set<String> getHiddenFieldSet(String module, Integer customerPriceLevel) {
        return getHiddenFieldSetByPriceLevel(module, customerPriceLevel);
    }

    public Set<String> getHiddenFieldSet(String module, boolean includeProductPricePermission) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module, null,
                includeProductPricePermission, null);
        return CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);
    }

    public Set<String> getHiddenFieldSet(String module, Object context) {
        return getHiddenFieldSet(module, context, new HashMap<>(), new HashMap<>());
    }

    private Set<String> getHiddenFieldSet(String module, Object context,
                                          Map<Long, Integer> customerPriceLevelCache,
                                          Map<Integer, Set<String>> hiddenFieldCache) {
        Integer customerPriceLevel = resolveCustomerPriceLevel(context, customerPriceLevelCache);
        Integer cacheKey = customerPriceLevel == null ? UNKNOWN_PRICE_LEVEL_CACHE_KEY : customerPriceLevel;
        return hiddenFieldCache.computeIfAbsent(cacheKey, key -> getHiddenFieldSetByPriceLevel(module,
                UNKNOWN_PRICE_LEVEL_CACHE_KEY.equals(key) ? null : key));
    }

    private Set<String> getHiddenFieldSetByPriceLevel(String module, Integer customerPriceLevel) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module, null, true, customerPriceLevel);
        return CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);
    }

    private Set<String> getSaleDetailHiddenFieldSet(String module, Object context) {
        return withoutSaleDetailVisiblePriceFields(getHiddenFieldSet(module, context));
    }

    private Set<String> withoutSaleDetailVisiblePriceFields(Set<String> hiddenFields) {
        if (CollUtil.isEmpty(hiddenFields)) {
            return Collections.emptySet();
        }
        return hiddenFields.stream()
                .filter(field -> !isSaleDetailVisiblePriceField(field))
                .collect(Collectors.toSet());
    }

    private boolean isSaleDetailVisiblePriceField(String fieldKey) {
        if (fieldKey == null) {
            return false;
        }
        String normalized = fieldKey;
        boolean changed;
        do {
            changed = false;
            for (String prefix : Arrays.asList("select_col_", "select_", "col_", "item_")) {
                if (normalized.startsWith(prefix)) {
                    normalized = normalized.substring(prefix.length());
                    changed = true;
                }
            }
        } while (changed);
        return SALE_DETAIL_VISIBLE_PRICE_FIELDS.contains(normalized);
    }

    private Integer resolveCustomerPriceLevel(Object context, Map<Long, Integer> customerPriceLevelCache) {
        Integer priceLevel = toInteger(getFieldValue(context, "priceLevel"));
        if (priceLevel != null) {
            return priceLevel;
        }
        Long customerId = toLong(getFieldValue(context, "customerId"));
        if (customerId == null) {
            return null;
        }
        return customerPriceLevelCache.computeIfAbsent(customerId, this::getCustomerPriceLevel);
    }

    private Integer getCustomerPriceLevel(Long customerId) {
        ErpCustomerDO customer = DataPermissionUtils.executeIgnore(() -> customerMapper.selectById(customerId));
        return customer == null ? null : customer.getPriceLevel();
    }

    private Integer toInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private void maskItems(Object vo, Set<String> hiddenFields) {
        Object items = getFieldValue(vo, "items");
        if (!(items instanceof Collection<?>)) {
            return;
        }
        ((Collection<?>) items).forEach(item -> maskBean(item, hiddenFields, "item_", false));
    }

    private void copyHiddenFields(Object target, Object source, Set<String> hiddenFields, String prefix) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field targetField : current.getDeclaredFields()) {
                if (targetField.getType().isPrimitive() || !shouldMask(hiddenFields, prefix, targetField.getName(), false)) {
                    continue;
                }
                Field sourceField = findField(source.getClass(), targetField.getName());
                if (sourceField == null) {
                    continue;
                }
                setFieldValue(target, targetField, getFieldValue(source, sourceField));
            }
            current = current.getSuperclass();
        }
    }

    private void maskBean(Object bean, Set<String> hiddenFields, String prefix, boolean exportRow) {
        if (bean == null) {
            return;
        }
        Class<?> current = bean.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().isPrimitive() || !shouldMask(hiddenFields, prefix, field.getName(), exportRow)) {
                    continue;
                }
                setFieldValue(bean, field, null);
            }
            current = current.getSuperclass();
        }
    }

    private boolean shouldMask(Set<String> hiddenFields, String prefix, String fieldName, boolean exportRow) {
        if (hiddenFields.contains(prefix + fieldName)) {
            return true;
        }
        if ("select_".equals(prefix) && hiddenFields.contains("select_col_" + fieldName)) {
            return true;
        }
        if (prefix.isEmpty() && hiddenFields.contains("col_" + fieldName)) {
            return true;
        }
        if (!exportRow) {
            return false;
        }
        if (hiddenFields.contains("item_" + fieldName)) {
            return true;
        }
        if (fieldName.startsWith("item") && fieldName.length() > 4) {
            String itemFieldName = Character.toLowerCase(fieldName.charAt(4)) + fieldName.substring(5);
            return hiddenFields.contains("item_" + itemFieldName);
        }
        return false;
    }

    private Object getFieldValue(Object bean, String fieldName) {
        Field field = findField(bean.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        return getFieldValue(bean, field);
    }

    private Object getFieldValue(Object bean, Field field) {
        try {
            field.setAccessible(true);
            return field.get(bean);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private void setFieldValue(Object bean, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(bean, value);
        } catch (IllegalAccessException ignored) {
            // Ignore fields that cannot be masked reflectively.
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

}
