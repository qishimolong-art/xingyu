package cn.iocoder.yudao.module.erp.service.sale;

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
 * Masks sale form response fields by current user's field permissions.
 */
@Component
public class ErpSaleFieldPermissionMasker {

    @Resource
    private PermissionApi permissionApi;

    public void maskForm(String module, Object vo) {
        if (vo == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(vo, hiddenFieldSet, "", false);
    }

    public void maskForms(String module, Collection<?> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        list.forEach(item -> maskBean(item, hiddenFieldSet, "", false));
    }

    public void maskFormWithItems(String module, Object vo) {
        if (vo == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(vo, hiddenFieldSet, "", false);
        maskItems(vo, hiddenFieldSet);
    }

    public void maskFormsWithItems(String module, Collection<?> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        for (Object item : list) {
            maskBean(item, hiddenFieldSet, "", false);
            maskItems(item, hiddenFieldSet);
        }
    }

    public void maskExportRows(String module, Collection<?> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        rows.forEach(row -> maskBean(row, hiddenFieldSet, "", true));
    }

    public void maskSelectRows(String module, Collection<?> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        rows.forEach(row -> maskBean(row, hiddenFieldSet, "select_", false));
    }

    public void clearHiddenFields(String module, Object target) {
        if (target == null) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        maskBean(target, hiddenFieldSet, "", false);
    }

    public void clearHiddenItemFields(String module, Collection<?> targetItems) {
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        if (CollUtil.isEmpty(hiddenFieldSet)) {
            return;
        }
        targetItems.forEach(item -> maskBean(item, hiddenFieldSet, "item_", false));
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
        if (CollUtil.isEmpty(targetItems)) {
            return;
        }
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
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
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module);
        if (CollUtil.isEmpty(hiddenFields)) {
            return Collections.emptySet();
        }
        return new HashSet<>(hiddenFields);
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
