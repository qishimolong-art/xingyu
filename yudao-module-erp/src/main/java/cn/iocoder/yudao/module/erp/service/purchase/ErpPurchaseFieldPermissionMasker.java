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
import java.util.Set;

/**
 * Masks purchase form response fields by current user's field permissions.
 */
@Component
public class ErpPurchaseFieldPermissionMasker {

    @Resource
    private PermissionApi permissionApi;

    public Set<String> getHiddenFieldSet(String module) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(module);
        if (CollUtil.isEmpty(hiddenFields)) {
            return Collections.emptySet();
        }
        return new HashSet<>(hiddenFields);
    }

    public boolean isFieldHidden(String module, String fieldKey) {
        Set<String> hiddenFieldSet = getHiddenFieldSet(module);
        return hiddenFieldSet.contains(fieldKey) || hiddenFieldSet.contains("col_" + fieldKey);
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
