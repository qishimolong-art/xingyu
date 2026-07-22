package cn.iocoder.yudao.framework.common.biz.system.logger;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;

/**
 * 操作日志内容构建工具。
 */
public final class OperateLogContentUtils {

    private static final int MAX_VALUE_LENGTH = 300;
    private static final int MAX_ACTION_LENGTH = 3500;
    private static final Set<String> IDENTITY_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "id", "name", "nickname", "username", "code", "no"));
    private static final Set<String> IGNORED_FIELDS = new HashSet<>(Arrays.asList(
            "class", "creator", "createTime", "updater", "updateTime", "deleted", "tenantId"));
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "oldPassword", "newPassword", "token", "accessToken", "refreshToken", "secret", "secretKey"));

    private OperateLogContentUtils() {
    }

    public static String buildCreateAction(String type, Object newObj, Long bizId, String fallbackCode) {
        return limit("创建" + normalizeType(type) + "，" + buildIdentity(newObj, bizId, fallbackCode)
                + buildInitialValues(newObj));
    }

    public static String buildErpFormCreateAction(Object newObj, Long bizId, String fallbackCode) {
        return limit(buildErpFormIdentity(newObj, bizId, fallbackCode));
    }

    public static String buildUpdateAction(String type, Object oldObj, Object newObj, Long bizId, String fallbackCode) {
        StringBuilder builder = new StringBuilder();
        builder.append("更新").append(normalizeType(type)).append("，")
                .append(buildIdentity(firstNonNull(newObj, oldObj), bizId, fallbackCode));
        List<String> changes = buildChanges(oldObj, newObj);
        if (changes.isEmpty()) {
            builder.append("；修改内容：未检测到字段变化");
        } else {
            builder.append("；修改内容：").append(String.join("；", changes));
        }
        return limit(builder.toString());
    }

    public static String buildErpFormUpdateAction(Object oldObj, Object newObj, Long bizId, String fallbackCode) {
        StringBuilder builder = new StringBuilder();
        builder.append(buildErpFormIdentity(firstNonNull(newObj, oldObj), bizId, fallbackCode));
        builder.append("\n变更明细：");
        List<String> changes = buildErpFormChanges(oldObj, newObj);
        if (changes.isEmpty()) {
            builder.append("\n无字段变化。");
        } else {
            builder.append("\n").append(String.join("\n", changes));
        }
        return limit(builder.toString());
    }

    public static String buildDeleteAction(String type, Object oldObj, Long bizId, String fallbackCode) {
        return limit("删除" + normalizeType(type) + "，" + buildIdentity(oldObj, bizId, fallbackCode));
    }

    public static String buildErpFormDeleteAction(Object oldObj, Long bizId, String fallbackCode) {
        return limit(buildErpFormIdentity(oldObj, bizId, fallbackCode));
    }

    public static String buildStatusAction(String type, Object oldObj, Object newObj, Long bizId,
                                           String fallbackCode, boolean approve) {
        String action = approve ? "审核" : "反审核";
        return limit(action + normalizeType(type) + "，"
                + buildIdentity(firstNonNull(newObj, oldObj), bizId, fallbackCode)
                + buildStatusChange(oldObj, newObj));
    }

    public static String buildAction(String action, Object obj, Long bizId, String fallbackCode) {
        return limit(action + "，" + buildIdentity(obj, bizId, fallbackCode));
    }

    public static String buildErpFormAction(String normalizedSubType, Object obj, Long bizId,
                                            String fallbackCode, String originalAction) {
        if (StrUtil.startWith(originalAction, "数据库编号：")) {
            return limit(originalAction);
        }
        String convertedAction = convertExistingErpAction(normalizedSubType, bizId, fallbackCode, originalAction);
        if (StrUtil.isNotBlank(convertedAction)) {
            return limit(convertedAction);
        }
        StringBuilder builder = new StringBuilder(buildErpFormIdentity(obj, bizId, fallbackCode));
        if ("修改".equals(normalizedSubType) && StrUtil.isNotBlank(originalAction)) {
            builder.append("\n变更明细：\n操作内容：")
                    .append(formatValue(originalAction))
                    .append("。");
        }
        return limit(builder.toString());
    }

    private static String convertExistingErpAction(String normalizedSubType, Long bizId,
                                                   String fallbackCode, String originalAction) {
        if (StrUtil.isBlank(originalAction)) {
            return null;
        }
        String id = firstMatchedValue(originalAction, "数据库编号：", "编号：");
        String name = firstMatchedValue(originalAction, "名称：");
        String code = firstMatchedValue(originalAction, "编码：", "单据编号：");
        if (StrUtil.isBlank(id) && StrUtil.isBlank(name) && StrUtil.isBlank(code)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("数据库编号：").append(StrUtil.blankToDefault(id, formatValue(bizId)));
        builder.append("\n名称：").append(StrUtil.blankToDefault(name, formatValue(firstNonNull(fallbackCode, code))));
        builder.append("\n编码：").append(StrUtil.blankToDefault(code, formatValue(fallbackCode)));
        if ("修改".equals(normalizedSubType)) {
            String changes = extractChangeDetail(originalAction);
            builder.append("\n变更明细：");
            builder.append(StrUtil.isBlank(changes) ? "\n无字段变化。" : "\n" + normalizeChangeDetail(changes));
        }
        return builder.toString();
    }

    private static String firstMatchedValue(String text, String... labels) {
        for (String label : labels) {
            String value = extractValue(text, label);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String extractValue(String text, String label) {
        int index = -1;
        int fromIndex = 0;
        while (fromIndex < text.length()) {
            index = text.indexOf(label, fromIndex);
            if (index < 0) {
                return null;
            }
            if (!"编号：".equals(label) || index < 2 || !"单据".equals(text.substring(index - 2, index))) {
                break;
            }
            fromIndex = index + label.length();
        }
        int start = index + label.length();
        int end = findNextDelimiter(text, start);
        return trimValue(text.substring(start, end));
    }

    private static int findNextDelimiter(String text, int start) {
        int end = text.length();
        for (String delimiter : Arrays.asList("\n", "；", "，", ",")) {
            int index = text.indexOf(delimiter, start);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return end;
    }

    private static String trimValue(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("[。；,，]+$", "");
    }

    private static String extractChangeDetail(String originalAction) {
        int index = originalAction.indexOf("变更明细：");
        if (index < 0) {
            index = originalAction.indexOf("修改内容：");
        }
        if (index < 0) {
            return null;
        }
        int start = originalAction.indexOf('：', index);
        return start < 0 ? null : originalAction.substring(start + 1).trim();
    }

    private static String normalizeChangeDetail(String detail) {
        if (StrUtil.isBlank(detail)) {
            return "";
        }
        String normalized = detail.replace("；", "；\n")
                .replaceAll("；\\s*\\n", "；\n")
                .trim();
        if (!normalized.endsWith("。") && !normalized.endsWith("；")) {
            normalized = normalized + "。";
        }
        return normalized;
    }

    public static List<String> buildChanges(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) {
            return Collections.emptyList();
        }
        Map<String, Object> oldValues = readProperties(oldObj);
        Map<String, Object> newValues = readProperties(newObj);
        List<String> changes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            String field = entry.getKey();
            if (shouldSkipField(field)) {
                continue;
            }
            Object oldValue = oldValues.get(field);
            Object newValue = entry.getValue();
            if (valueEquals(oldValue, newValue)) {
                continue;
            }
            changes.add("字段【" + field + "】修改前【" + formatValue(oldValue) + "】修改后【" + formatValue(newValue) + "】");
        }
        return changes;
    }

    public static List<String> buildErpFormChanges(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) {
            return Collections.emptyList();
        }
        Map<String, Object> oldValues = readProperties(oldObj);
        Map<String, Object> newValues = readProperties(newObj);
        List<String> changes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            String field = entry.getKey();
            if (shouldSkipField(field)) {
                continue;
            }
            Object oldValue = oldValues.get(field);
            Object newValue = entry.getValue();
            if (valueEquals(oldValue, newValue)) {
                continue;
            }
            changes.add(field + "：" + formatValue(oldValue) + "->" + formatValue(newValue));
        }
        for (int i = 0; i < changes.size(); i++) {
            changes.set(i, changes.get(i) + (i == changes.size() - 1 ? "。" : "；"));
        }
        return changes;
    }

    public static String buildIdentity(Object obj, Long bizId, String fallbackCode) {
        Map<String, Object> values = readProperties(obj);
        Long id = bizId != null ? bizId : toLong(values.get("id"));
        StringBuilder builder = new StringBuilder();
        if (id != null) {
            builder.append("编号：").append(id);
        }
        appendIdentity(builder, "名称", firstNonNull(values.get("name"), values.get("nickname"), values.get("username")));
        appendIdentity(builder, "编码", firstNonNull(values.get("code"), fallbackCode));
        appendIdentity(builder, "单据编号", firstNonNull(values.get("no"), fallbackCode));
        if (builder.length() == 0 && StrUtil.isNotBlank(fallbackCode)) {
            builder.append("编码：").append(fallbackCode);
        }
        return builder.length() == 0 ? "编号：未知" : builder.toString();
    }

    public static String buildErpFormIdentity(Object obj, Long bizId, String fallbackCode) {
        Map<String, Object> values = readProperties(obj);
        Long id = bizId != null ? bizId : toLong(values.get("id"));
        Object name = firstNonNull(values.get("name"), values.get("nickname"), values.get("username"), values.get("no"), fallbackCode);
        Object code = firstNonNull(values.get("code"), values.get("warehouseCode"), values.get("no"), fallbackCode);
        return "数据库编号：" + formatValue(id)
                + "\n名称：" + formatValue(name)
                + "\n编码：" + formatValue(code);
    }

    private static String buildInitialValues(Object newObj) {
        Map<String, Object> values = readProperties(newObj);
        if (values.isEmpty()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (shouldSkipField(entry.getKey()) || IDENTITY_FIELDS.contains(entry.getKey())) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            items.add("字段【" + entry.getKey() + "】值【" + formatValue(value) + "】");
        }
        return items.isEmpty() ? "" : "；初始内容：" + String.join("；", items);
    }

    private static String buildStatusChange(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) {
            return "";
        }
        Map<String, Object> oldValues = readProperties(oldObj);
        Map<String, Object> newValues = readProperties(newObj);
        Object oldStatus = oldValues.get("status");
        Object newStatus = newValues.get("status");
        if (valueEquals(oldStatus, newStatus)) {
            return "";
        }
        return "；修改内容：字段【status】修改前【" + formatValue(oldStatus) + "】修改后【" + formatValue(newStatus) + "】";
    }

    private static Map<String, Object> readProperties(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> values = new TreeMap<>();
        try {
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                if (descriptor.getReadMethod() == null) {
                    continue;
                }
                String name = descriptor.getName();
                if (SENSITIVE_FIELDS.contains(name)) {
                    values.put(name, "已脱敏");
                    continue;
                }
                values.put(name, descriptor.getReadMethod().invoke(obj));
            }
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ignored) {
            return Collections.emptyMap();
        }
        return values;
    }

    private static boolean shouldSkipField(String field) {
        return IGNORED_FIELDS.contains(field) || SENSITIVE_FIELDS.contains(field);
    }

    private static boolean valueEquals(Object oldValue, Object newValue) {
        if (oldValue instanceof BigDecimal && newValue instanceof BigDecimal) {
            return ((BigDecimal) oldValue).compareTo((BigDecimal) newValue) == 0;
        }
        return Objects.equals(oldValue, newValue);
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "空";
        }
        String text;
        if (value instanceof Date || value instanceof Number || value instanceof CharSequence || value instanceof Boolean) {
            text = String.valueOf(value);
        } else {
            text = JsonUtils.toJsonString(value);
        }
        if (text == null) {
            text = String.valueOf(value);
        }
        text = text.replace("\r", " ").replace("\n", " ");
        return StrUtil.maxLength(text, MAX_VALUE_LENGTH);
    }

    private static String normalizeType(String type) {
        return StrUtil.blankToDefault(type, "数据");
    }

    private static void appendIdentity(StringBuilder builder, String label, Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("，");
        }
        builder.append(label).append("：").append(value);
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private static String limit(String action) {
        return StrUtil.maxLength(action, MAX_ACTION_LENGTH);
    }

}
