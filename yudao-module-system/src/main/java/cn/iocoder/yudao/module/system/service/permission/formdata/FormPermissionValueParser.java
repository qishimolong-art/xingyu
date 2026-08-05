package cn.iocoder.yudao.module.system.service.permission.formdata;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.enums.permission.FormPermissionFieldValueTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class FormPermissionValueParser {

    public Set<Long> parse(Object value, String valueType) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (value == null) {
            return userIds;
        }
        if (FormPermissionFieldValueTypeEnum.SINGLE_ID.getCode().equals(valueType)) {
            addLongValue(userIds, value);
        } else if (FormPermissionFieldValueTypeEnum.CSV_IDS.getCode().equals(valueType)) {
            parseCsv(userIds, value);
        } else if (FormPermissionFieldValueTypeEnum.JSON_IDS.getCode().equals(valueType)) {
            parseJson(userIds, value);
        }
        userIds.remove(null);
        return userIds;
    }

    private void parseCsv(Set<Long> userIds, Object value) {
        String text = String.valueOf(value);
        if (StrUtil.isBlank(text)) {
            return;
        }
        for (String item : text.split(",")) {
            addLongValue(userIds, item);
        }
    }

    private void parseJson(Set<Long> userIds, Object value) {
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                addLongValue(userIds, item);
            }
            return;
        }
        String text = String.valueOf(value);
        if (StrUtil.isBlank(text)) {
            return;
        }
        try {
            List<Object> values = JsonUtils.parseArray(text, Object.class);
            if (CollUtil.isEmpty(values)) {
                return;
            }
            for (Object item : values) {
                addLongValue(userIds, item);
            }
        } catch (Exception ex) {
            log.warn("[parseJson][表单数据权限字段值解析失败，value({})]", text, ex);
        }
    }

    private void addLongValue(Set<Long> userIds, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number) {
            userIds.add(((Number) value).longValue());
            return;
        }
        String text = String.valueOf(value).trim();
        if (StrUtil.isBlank(text)) {
            return;
        }
        try {
            userIds.add(Long.valueOf(text));
        } catch (NumberFormatException ignored) {
            log.warn("[addLongValue][跳过非法用户编号({})]", text);
        }
    }

}
