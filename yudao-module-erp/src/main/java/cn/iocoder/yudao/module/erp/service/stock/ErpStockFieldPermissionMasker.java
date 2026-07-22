package cn.iocoder.yudao.module.erp.service.stock;

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
 * Masks stock form response fields by current user's field permissions.
 */
@Component
public class ErpStockFieldPermissionMasker {

    @Resource
    private PermissionApi permissionApi;

    public void maskFormWithItems(String module, Object vo) {
        if (vo == null) {
            return;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
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

    public void maskForm(String module, Object vo) {
        if (vo == null) {
            return;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        maskBean(vo, new HashSet<>(hiddenFields), "");
    }

    public void maskListColumns(String module, Object vo) {
        maskWithPrefix(module, vo, "col_");
    }

    public void maskListColumns(String module, Collection<?> list) {
        maskWithPrefix(module, list, "col_");
    }

    public void maskReportColumns(String module, Object vo) {
        maskWithPrefix(module, vo, "report_");
    }

    public void maskReportColumns(String module, Collection<?> list) {
        maskWithPrefix(module, list, "report_");
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
            if (sourceItem == null) {
                continue;
            }
            copyHiddenFields(targetItem, sourceItem, hiddenFieldSet, "item_");
        }
    }

    private void maskWithPrefix(String module, Object target, String prefix) {
        if (target == null) {
            return;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (target instanceof Collection<?>) {
            for (Object item : (Collection<?>) target) {
                maskBean(item, hiddenFieldSet, prefix);
            }
            return;
        }
        maskBean(target, hiddenFieldSet, prefix);
    }

    public Set<String> getHiddenFieldSet(String module) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module);
        if (CollUtil.isEmpty(hiddenFields)) {
            return Collections.emptySet();
        }
        return new HashSet<>(hiddenFields);
    }

    private void copyHiddenFields(Object target, Object source, Set<String> hiddenFields, String prefix) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field targetField : current.getDeclaredFields()) {
                if (targetField.getType().isPrimitive()) {
                    continue;
                }
                if (!hiddenFields.contains(prefix + targetField.getName())) {
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
                if (!hiddenFields.contains(prefix + field.getName())) {
                    continue;
                }
                setFieldValue(bean, field, null);
            }
            current = current.getSuperclass();
        }
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
