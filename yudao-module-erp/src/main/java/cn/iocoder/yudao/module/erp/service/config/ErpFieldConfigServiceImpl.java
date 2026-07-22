package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigCreateCustomReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldSourceEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldTypeEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionCreateOrUpdateReqDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FIELD_CONFIG_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FIELD_CONFIG_FIELD_NAME_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FIELD_CONFIG_MODULE_KEY_INVALID;

/**
 * ERP 字段配置 Service 实现类
 *
 * @author Claude
 */
@Service
@Validated
public class ErpFieldConfigServiceImpl implements ErpFieldConfigService {

    private static final String ERP_PRODUCT_MODULE = "erp_product";
    private static final String ERP_PRODUCT_TABLE = "erp_product";
    private static final String CUSTOM_COLUMN_PREFIX = "ext_";
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");
    private static final Pattern PHYSICAL_COLUMN_PATTERN = Pattern.compile("^ext_[a-z][a-z0-9_]{0,63}$");
    private static final Set<String> FIELD_GROUPS = new HashSet<>(Arrays.asList(
            "base_info", "price_info", "extend_info", "stock_info", "image_info", "detail_info"));

    @Resource
    private ErpFieldConfigMapper fieldConfigMapper;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public List<ErpFieldConfigDO> getFieldConfigListByModule(String moduleKey) {
        validateModuleKey(moduleKey);
        List<ErpFieldConfigDO> list = fieldConfigMapper.selectListByModuleKey(moduleKey);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(ErpFieldConfigBatchUpdateReqVO reqVO) {
        String moduleKey = reqVO.getModuleKey();
        validateModuleKey(moduleKey);

        if (reqVO.getItems() != null) {
            Set<String> seen = new HashSet<>();
            for (ErpFieldConfigBatchUpdateReqVO.Item item : reqVO.getItems()) {
                String fieldName = normalize(item.getFieldName());
                if (!StringUtils.hasText(fieldName)) {
                    throw exception(FIELD_CONFIG_FIELD_NAME_EMPTY);
                }
                item.setFieldName(fieldName);
                item.setFieldLabel(normalize(item.getFieldLabel()));
                if (!seen.add(fieldName)) {
                    throw exception(FIELD_CONFIG_DUPLICATE, moduleKey, fieldName);
                }
            }
        }

        validateSystemFieldUpdateScope(moduleKey, reqVO.getItems());

        Set<String> deletedCustomFields = normalizeDeletedCustomFields(reqVO.getDeletedCustomFields());
        if (!deletedCustomFields.isEmpty()) {
            Set<String> retainedFields = reqVO.getItems() == null ? Collections.emptySet() : reqVO.getItems().stream()
                    .map(ErpFieldConfigBatchUpdateReqVO.Item::getFieldName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            deletedCustomFields.removeAll(retainedFields);
            deleteCustomFieldConfigs(moduleKey, deletedCustomFields);
        }

        List<ErpFieldConfigDO> existList = fieldConfigMapper.selectListByModuleKey(moduleKey);
        List<ErpFieldConfigDO> systemConfigs = existList == null ? Collections.emptyList() : existList.stream()
                .filter(item -> !isCustomField(item))
                .collect(Collectors.toList());
        if (!systemConfigs.isEmpty()) {
            List<Long> ids = CollectionUtils.convertList(systemConfigs, ErpFieldConfigDO::getId);
            fieldConfigMapper.physicalDeleteByIds(ids);
        }

        if (reqVO.getItems() == null || reqVO.getItems().isEmpty()) {
            return;
        }
        for (ErpFieldConfigBatchUpdateReqVO.Item item : reqVO.getItems()) {
            if (ErpFieldConfigFieldSourceEnum.CUSTOM.getSource().equals(item.getFieldSource())) {
                updateCustomFieldConfig(moduleKey, item);
            }
        }
        List<ErpFieldConfigDO> insertList = CollectionUtils.convertList(reqVO.getItems().stream()
                .filter(item -> !ErpFieldConfigFieldSourceEnum.CUSTOM.getSource().equals(item.getFieldSource()))
                .collect(Collectors.toList()), item -> {
            ErpFieldConfigDO configDO = BeanUtils.toBean(item, ErpFieldConfigDO.class);
            configDO.setModuleKey(moduleKey);
            if (configDO.getRequired() == null) {
                configDO.setRequired(Boolean.FALSE);
            }
            if (configDO.getVisible() == null) {
                configDO.setVisible(Boolean.TRUE);
            }
            if (configDO.getListVisible() == null) {
                configDO.setListVisible(Boolean.FALSE);
            }
            if (configDO.getSearchable() == null) {
                configDO.setSearchable(Boolean.FALSE);
            }
            if (configDO.getReadonly() == null) {
                configDO.setReadonly(Boolean.FALSE);
            }
            configDO.setFieldSource(ErpFieldConfigFieldSourceEnum.SYSTEM.getSource());
            return configDO;
        });
        if (!insertList.isEmpty()) {
            fieldConfigMapper.insertBatch(insertList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpFieldConfigDO createCustomField(ErpFieldConfigCreateCustomReqVO reqVO) {
        String moduleKey = normalize(reqVO.getModuleKey());
        validateModuleKey(moduleKey);
        if (!ERP_PRODUCT_MODULE.equals(moduleKey)) {
            throw exception(FIELD_CONFIG_MODULE_KEY_INVALID, moduleKey);
        }
        String fieldName = normalize(reqVO.getFieldName());
        validateCustomFieldName(fieldName);
        if (fieldConfigMapper.selectByModuleKeyAndFieldName(moduleKey, fieldName) != null) {
            throw exception(FIELD_CONFIG_DUPLICATE, moduleKey, fieldName);
        }

        String fieldType = normalize(reqVO.getFieldType());
        if (!ErpFieldConfigFieldTypeEnum.isValid(fieldType)) {
            throw exception(FIELD_CONFIG_MODULE_KEY_INVALID, fieldType);
        }
        String physicalColumn = buildPhysicalColumn(fieldName);
        if (!PHYSICAL_COLUMN_PATTERN.matcher(physicalColumn).matches()) {
            throw exception(FIELD_CONFIG_FIELD_NAME_EMPTY);
        }
        if (Objects.equals(fieldConfigMapper.selectColumnCount(ERP_PRODUCT_TABLE, physicalColumn), 0L)) {
            fieldConfigMapper.addColumn(ERP_PRODUCT_TABLE, physicalColumn, buildColumnDefinition(reqVO, fieldType));
        } else {
            throw exception(FIELD_CONFIG_DUPLICATE, moduleKey, physicalColumn);
        }

        ErpFieldConfigDO config = BeanUtils.toBean(reqVO, ErpFieldConfigDO.class);
        config.setModuleKey(moduleKey);
        config.setFieldName(fieldName);
        config.setFieldLabel(normalize(reqVO.getFieldLabel()));
        config.setFieldType(fieldType);
        config.setFieldSource(ErpFieldConfigFieldSourceEnum.CUSTOM.getSource());
        config.setPhysicalColumn(physicalColumn);
        config.setFieldGroup(normalizeFieldGroup(reqVO.getFieldGroup()));
        config.setRequired(Boolean.TRUE.equals(reqVO.getRequired()));
        config.setVisible(reqVO.getVisible() == null || Boolean.TRUE.equals(reqVO.getVisible()));
        config.setListVisible(Boolean.TRUE.equals(reqVO.getListVisible()));
        config.setSearchable(Boolean.TRUE.equals(reqVO.getSearchable()));
        config.setReadonly(Boolean.TRUE.equals(reqVO.getReadonly()));
        config.setSort(reqVO.getSort() == null ? resolveCustomFieldSort(moduleKey) : reqVO.getSort());
        fieldConfigMapper.insert(config);
        syncFieldDefinition(config);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetFieldConfig(String moduleKey) {
        validateModuleKey(moduleKey);
        List<ErpFieldConfigDO> existList = fieldConfigMapper.selectListByModuleKey(moduleKey);
        List<ErpFieldConfigDO> systemConfigs = existList == null ? Collections.emptyList() : existList.stream()
                .filter(item -> !isCustomField(item))
                .collect(Collectors.toList());
        if (systemConfigs.isEmpty()) {
            return;
        }
        List<Long> ids = CollectionUtils.convertList(systemConfigs, ErpFieldConfigDO::getId);
        fieldConfigMapper.physicalDeleteByIds(ids);
    }

    @Override
    public List<ErpFieldConfigModuleEnum> listAllModules() {
        return Arrays.asList(ErpFieldConfigModuleEnum.values());
    }

    private void validateModuleKey(String moduleKey) {
        if (!ErpFieldConfigModuleEnum.isValid(moduleKey)) {
            throw exception(FIELD_CONFIG_MODULE_KEY_INVALID, moduleKey);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isCustomField(ErpFieldConfigDO config) {
        return config != null && ErpFieldConfigFieldSourceEnum.CUSTOM.getSource().equals(config.getFieldSource());
    }

    private void validateSystemFieldUpdateScope(String moduleKey, List<ErpFieldConfigBatchUpdateReqVO.Item> items) {
        if (ERP_PRODUCT_MODULE.equals(moduleKey)) {
            return;
        }
        List<ErpFieldConfigDO> existList = fieldConfigMapper.selectListByModuleKey(moduleKey);
        Set<String> existingFields = existList == null ? Collections.emptySet() : existList.stream()
                .map(ErpFieldConfigDO::getFieldName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (items == null || items.isEmpty()) {
            if (!existingFields.isEmpty()) {
                throw exception(FIELD_CONFIG_MODULE_KEY_INVALID, moduleKey);
            }
            return;
        }
        for (ErpFieldConfigBatchUpdateReqVO.Item item : items) {
            if (!existingFields.contains(item.getFieldName())) {
                throw exception(FIELD_CONFIG_MODULE_KEY_INVALID, moduleKey + ":" + item.getFieldName());
            }
        }
    }

    private void updateCustomFieldConfig(String moduleKey, ErpFieldConfigBatchUpdateReqVO.Item item) {
        ErpFieldConfigDO existing = fieldConfigMapper.selectByModuleKeyAndFieldName(moduleKey, item.getFieldName());
        if (!isCustomField(existing)) {
            return;
        }
        existing.setFieldLabel(normalize(item.getFieldLabel()));
        existing.setRequired(Boolean.TRUE.equals(item.getRequired()));
        existing.setVisible(item.getVisible() == null || Boolean.TRUE.equals(item.getVisible()));
        existing.setListVisible(Boolean.TRUE.equals(item.getListVisible()));
        existing.setSearchable(Boolean.TRUE.equals(item.getSearchable()));
        existing.setReadonly(Boolean.TRUE.equals(item.getReadonly()));
        existing.setFieldGroup(normalizeFieldGroup(item.getFieldGroup()));
        existing.setSort(item.getSort());
        fieldConfigMapper.updateById(existing);
        syncFieldDefinition(existing);
    }

    private Set<String> normalizeDeletedCustomFields(List<String> deletedCustomFields) {
        if (deletedCustomFields == null || deletedCustomFields.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String deletedCustomField : deletedCustomFields) {
            String fieldName = normalize(deletedCustomField);
            if (!StringUtils.hasText(fieldName)) {
                continue;
            }
            validateCustomFieldName(fieldName);
            result.add(fieldName);
        }
        return result;
    }

    private void deleteCustomFieldConfigs(String moduleKey, Set<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return;
        }
        List<ErpFieldConfigDO> deleteList = fieldNames.stream()
                .map(fieldName -> fieldConfigMapper.selectByModuleKeyAndFieldName(moduleKey, fieldName))
                .filter(this::isCustomField)
                .collect(Collectors.toList());
        if (deleteList.isEmpty()) {
            return;
        }
        fieldConfigMapper.physicalDeleteByIds(CollectionUtils.convertList(deleteList, ErpFieldConfigDO::getId));

        List<String> fieldKeys = new ArrayList<>();
        for (ErpFieldConfigDO config : deleteList) {
            fieldKeys.add(config.getFieldName());
            fieldKeys.add("col_" + config.getFieldName());
        }
        permissionApi.deleteFieldDefinitions(moduleKey, fieldKeys);
    }

    private void validateCustomFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName) || !FIELD_NAME_PATTERN.matcher(fieldName).matches()
                || fieldName.startsWith("ext_") || fieldName.startsWith("col_")) {
            throw exception(FIELD_CONFIG_FIELD_NAME_EMPTY);
        }
    }

    private String buildPhysicalColumn(String fieldName) {
        StringBuilder builder = new StringBuilder(CUSTOM_COLUMN_PREFIX);
        for (int i = 0; i < fieldName.length(); i++) {
            char ch = fieldName.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append('_').append(Character.toLowerCase(ch));
            } else {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString().replaceAll("_+", "_");
    }

    private String normalizeFieldGroup(String fieldGroup) {
        String group = normalize(fieldGroup);
        return FIELD_GROUPS.contains(group) ? group : "extend_info";
    }

    private String buildColumnDefinition(ErpFieldConfigCreateCustomReqVO reqVO, String fieldType) {
        if (ErpFieldConfigFieldTypeEnum.TEXT.getType().equals(fieldType)) {
            int maxLength = reqVO.getMaxLength() == null ? 255 : Math.max(1, Math.min(reqVO.getMaxLength(), 500));
            return "varchar(" + maxLength + ") NULL";
        }
        if (ErpFieldConfigFieldTypeEnum.TEXTAREA.getType().equals(fieldType)) {
            return "text NULL";
        }
        if (ErpFieldConfigFieldTypeEnum.INTEGER.getType().equals(fieldType)) {
            return "bigint NULL";
        }
        if (ErpFieldConfigFieldTypeEnum.DECIMAL.getType().equals(fieldType)) {
            int precision = reqVO.getDecimalPrecision() == null ? 18 : Math.max(1, Math.min(reqVO.getDecimalPrecision(), 30));
            int scale = reqVO.getDecimalScale() == null ? 2 : Math.max(0, Math.min(reqVO.getDecimalScale(), 10));
            if (scale >= precision) {
                scale = Math.max(0, precision - 1);
            }
            return "decimal(" + precision + "," + scale + ") NULL";
        }
        if (ErpFieldConfigFieldTypeEnum.DATE.getType().equals(fieldType)) {
            return "date NULL";
        }
        if (ErpFieldConfigFieldTypeEnum.DATETIME.getType().equals(fieldType)) {
            return "datetime NULL";
        }
        if (ErpFieldConfigFieldTypeEnum.BOOLEAN.getType().equals(fieldType)) {
            return "bit(1) NULL";
        }
        throw exception(FIELD_CONFIG_MODULE_KEY_INVALID, fieldType);
    }

    private Integer resolveCustomFieldSort(String moduleKey) {
        List<ErpFieldConfigDO> list = fieldConfigMapper.selectListByModuleKey(moduleKey);
        return list.stream()
                .map(ErpFieldConfigDO::getSort)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 10;
    }

    private void syncFieldDefinition(ErpFieldConfigDO config) {
        FieldDefinitionCreateOrUpdateReqDTO formField = new FieldDefinitionCreateOrUpdateReqDTO();
        formField.setModule(config.getModuleKey());
        formField.setFieldKey(config.getFieldName());
        formField.setFieldLabel(config.getFieldLabel());
        formField.setFieldGroup(config.getFieldGroup());
        formField.setSort(config.getSort());

        FieldDefinitionCreateOrUpdateReqDTO listField = new FieldDefinitionCreateOrUpdateReqDTO();
        listField.setModule(config.getModuleKey());
        listField.setFieldKey("col_" + config.getFieldName());
        listField.setFieldLabel(config.getFieldLabel());
        listField.setFieldGroup("list_col");
        listField.setSort(config.getSort());
        permissionApi.createOrUpdateFieldDefinitions(Arrays.asList(formField, listField));
    }

}
