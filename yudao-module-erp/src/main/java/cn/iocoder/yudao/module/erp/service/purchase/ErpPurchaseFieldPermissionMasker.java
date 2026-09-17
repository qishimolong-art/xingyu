package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Masks purchase form response fields by current user's field permissions.
 */
@Component
public class ErpPurchaseFieldPermissionMasker {

    private static final String PRODUCT_PRICE_PERMISSION_MODULE = "erp_product";
    private static final Set<String> PURCHASE_PRICE_RELATED_FIELDS = new HashSet<String>() {{
        add("totalProductPrice");
        add("discountPrice");
        add("totalPrice");
        add("totalAmount");
        add("taxPrice");
        add("taxAmount");
        add("taxExclusiveAmount");
        add("totalTaxPrice");
        add("productPrice");
        add("productPurchasePrice");
        add("salePrice");
        add("lastSalePrice");
        add("minPrice");
        add("referencePrice");
        add("retailPrice");
        add("lastPurchasePrice");
        add("grossProfitRate");
        add("backupPrice1");
        add("wholesalePrice");
        add("sharePrice");
        add("originalProductPrice");
        add("oldPrice");
        add("newPrice");
        add("adjustPrice");
        add("totalAdjustPrice");
        add("item_productPrice");
        add("item_productPurchasePrice");
        add("item_salePrice");
        add("item_lastSalePrice");
        add("item_minPrice");
        add("item_referencePrice");
        add("item_retailPrice");
        add("item_totalProductPrice");
        add("item_totalPrice");
        add("item_taxPrice");
        add("item_totalTaxPrice");
        add("item_lastPurchasePrice");
        add("item_grossProfitRate");
        add("item_backupPrice1");
        add("item_wholesalePrice");
        add("item_sharePrice");
        add("item_originalProductPrice");
        add("item_oldPrice");
        add("item_newPrice");
        add("item_adjustPrice");
        add("select_productPrice");
        add("select_totalProductPrice");
        add("select_totalPrice");
        add("select_taxPrice");
        add("select_col_productPrice");
        add("select_col_totalProductPrice");
        add("select_col_totalPrice");
        add("select_col_taxPrice");
    }};

    @Resource
    private PermissionApi permissionApi;

    public Set<String> getHiddenFieldSet(String module) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module);
        Set<String> result = CollUtil.isEmpty(hiddenFields) ? new HashSet<>() : new HashSet<>(hiddenFields);
        if (!PRODUCT_PRICE_PERMISSION_MODULE.equals(module)) {
            Set<String> productPriceHiddenFields = getProductPriceHiddenFieldSet();
            if (isPurchasePriceHidden(productPriceHiddenFields)) {
                result.addAll(PURCHASE_PRICE_RELATED_FIELDS);
            }
            addProductPriceHiddenFields(result, productPriceHiddenFields);
        }
        return result.isEmpty() ? Collections.emptySet() : result;
    }

    public boolean isFieldHidden(String module, String fieldKey) {
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        return hiddenFieldSet.contains(fieldKey) || hiddenFieldSet.contains("col_" + fieldKey);
    }

    public void clearHiddenFields(String module, Object target) {
        if (target == null) {
            return;
        }
        maskBean(target, getHiddenFieldSet(module), "");
    }

    public void clearHiddenFieldsExcept(String module, Object target, Set<String> retainedFields) {
        if (target == null) {
            return;
        }
        maskBean(target, getHiddenFieldSetExcept(module, retainedFields), "");
    }

    public void clearHiddenItemFields(String module, Collection<?> targetItems) {
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        for (Object targetItem : targetItems) {
            maskBean(targetItem, hiddenFieldSet, "item_");
        }
    }

    public void clearHiddenItemFieldsExcept(String module, Collection<?> targetItems, Set<String> retainedFields) {
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSetExcept(module, retainedFields);
        for (Object targetItem : targetItems) {
            maskBean(targetItem, hiddenFieldSet, "item_");
        }
    }

    public void preserveHiddenFields(String module, Object target, Object source) {
        if (target == null || source == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        copyHiddenFields(target, source, hiddenFieldSet, "");
    }

    public void preserveHiddenItemFields(String module, Collection<?> targetItems, Collection<?> sourceItems) {
        if (CollUtil.isEmpty(targetItems) || CollUtil.isEmpty(sourceItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        Map<Object, Object> sourceItemMap = sourceItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> getFieldValue(item, "id") != null)
                .collect(Collectors.toMap(item -> getFieldValue(item, "id"), Function.identity(), (a, b) -> a));
        for (Object targetItem : targetItems) {
            Object id = getFieldValue(targetItem, "id");
            if (id == null) {
                continue;
            }
            Object sourceItem = sourceItemMap.get(id);
            if (sourceItem != null) {
                copyHiddenFields(targetItem, sourceItem, hiddenFieldSet, "item_");
            }
        }
    }

    public void mask(String module, Object vo) {
        if (vo == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(vo, hiddenFieldSet, "");
        Object items = getFieldValue(vo, "items");
        if (!(items instanceof Collection<?>)) {
            return;
        }
        Collection<?> collection = (Collection<?>) items;
        for (Object item : collection) {
            maskBean(item, hiddenFieldSet, "item_");
        }
    }

    public void maskList(String module, Collection<?> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        for (Object vo : list) {
            maskBean(vo, hiddenFieldSet, "");
            Object items = getFieldValue(vo, "items");
            if (!(items instanceof Collection<?>)) {
                continue;
            }
            for (Object item : (Collection<?>) items) {
                maskBean(item, hiddenFieldSet, "item_");
            }
        }
    }

    private void maskBean(Object bean, Set<String> hiddenFields, String prefix) {
        if (bean == null) {
            return;
        }
        Class<?> current = bean.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().isPrimitive()) {
                    continue;
                }
                if (!isHiddenField(hiddenFields, prefix, field.getName())) {
                    continue;
                }
                setFieldValue(bean, field, null);
            }
            current = current.getSuperclass();
        }
    }

    private void copyHiddenFields(Object target, Object source, Set<String> hiddenFields, String prefix) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field targetField : current.getDeclaredFields()) {
                if (targetField.getType().isPrimitive()
                        || !isHiddenField(hiddenFields, prefix, targetField.getName())) {
                    continue;
                }
                Field sourceField = findField(source.getClass(), targetField.getName());
                if (sourceField != null) {
                    setFieldValue(target, targetField, getFieldValue(source, sourceField));
                }
            }
            current = current.getSuperclass();
        }
    }

    private Object getFieldValue(Object bean, String fieldName) {
        Field field = findField(bean.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(bean);
        } catch (IllegalAccessException ignored) {
            return null;
        }
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

    private Set<String> getProductPriceHiddenFieldSet() {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(PRODUCT_PRICE_PERMISSION_MODULE);
        return CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);
    }

    private void addProductPriceHiddenFields(Set<String> result, Set<String> productHiddenFields) {
        addMappedProductPriceHiddenField(result, productHiddenFields, "purchasePrice", "productPurchasePrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "salePrice", "salePrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "lastSalePrice", "lastSalePrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "minPrice", "minPrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "referencePrice", "referencePrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "retailPrice", "retailPrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "lastPurchasePrice", "lastPurchasePrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "grossProfitRate", "grossProfitRate");
        addMappedProductPriceHiddenField(result, productHiddenFields, "backupPrice1", "backupPrice1");
        addMappedProductPriceHiddenField(result, productHiddenFields, "wholesalePrice", "wholesalePrice");
        addMappedProductPriceHiddenField(result, productHiddenFields, "sharePrice", "sharePrice");
    }

    private void addMappedProductPriceHiddenField(Set<String> result, Set<String> productHiddenFields,
                                                  String productField, String itemField) {
        if (!isHiddenField(productHiddenFields, "", productField)) {
            return;
        }
        result.add(itemField);
        result.add("item_" + itemField);
    }

    private Set<String> getHiddenFieldSetExcept(String module, Set<String> retainedFields) {
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet) || CollUtil.isEmpty(retainedFields)) {
            return hiddenFieldSet;
        }
        Set<String> result = new HashSet<>(hiddenFieldSet);
        result.removeAll(retainedFields);
        retainedFields.forEach(field -> result.remove("col_" + field));
        return result.isEmpty() ? Collections.emptySet() : result;
    }

    private boolean isPurchasePriceHidden(Set<String> hiddenFields) {
        return isHiddenField(hiddenFields, "", "lastPurchasePrice")
                || isHiddenField(hiddenFields, "", "purchasePrice");
    }

    private boolean isHiddenField(Set<String> hiddenFields, String prefix, String fieldName) {
        return hiddenFields.contains(prefix + fieldName) || hiddenFields.contains("col_" + prefix + fieldName);
    }
}
