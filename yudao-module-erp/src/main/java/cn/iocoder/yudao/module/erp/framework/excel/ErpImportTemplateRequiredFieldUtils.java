package cn.iocoder.yudao.module.erp.framework.excel;

import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves required Excel template headers from ERP field configuration.
 */
public final class ErpImportTemplateRequiredFieldUtils {

    private ErpImportTemplateRequiredFieldUtils() {
    }

    public static Set<String> getRequiredFields(ErpFieldConfigService fieldConfigService,
                                                ErpFieldConfigModuleEnum moduleEnum,
                                                Class<?> importExcelClass,
                                                Map<String, String> fieldAliases) {
        if (fieldConfigService == null || moduleEnum == null || importExcelClass == null) {
            return Collections.emptySet();
        }
        Set<String> importFields = getFieldNames(importExcelClass);
        if (importFields.isEmpty()) {
            return Collections.emptySet();
        }
        Map<String, String> aliases = fieldAliases == null ? Collections.emptyMap() : fieldAliases;
        Set<String> result = new HashSet<>();
        for (ErpFieldConfigDO fieldConfig : fieldConfigService.getFieldConfigListByModule(moduleEnum.getKey())) {
            if (!Boolean.TRUE.equals(fieldConfig.getRequired())) {
                continue;
            }
            String fieldName = fieldConfig.getFieldName();
            String mappedFieldName = aliases.getOrDefault(fieldName, fieldName);
            if (importFields.contains(mappedFieldName)) {
                result.add(mappedFieldName);
            }
        }
        return result;
    }

    public static Map<String, String> aliasMap(String... pairs) {
        Map<String, String> map = new HashMap<>();
        if (pairs == null) {
            return map;
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        Set<String> names = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            names.add(field.getName());
        }
        return names;
    }

}
