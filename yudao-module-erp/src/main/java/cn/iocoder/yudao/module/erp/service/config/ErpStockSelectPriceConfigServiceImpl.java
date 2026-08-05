package cn.iocoder.yudao.module.erp.service.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpStockSelectPriceConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpStockSelectPriceConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldSourceEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_SELECT_PRICE_CONFIG_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_SELECT_PRICE_DUPLICATE_FIELD;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_SELECT_PRICE_INVALID_BIZ_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_SELECT_PRICE_INVALID_FIELD;

@Service
@Validated
public class ErpStockSelectPriceConfigServiceImpl implements ErpStockSelectPriceConfigService {

    private static final String PRODUCT_MODULE = "erp_product";
    private static final String PRICE_FIELD_GROUP = "price_info";
    private static final String BIZ_TYPE_STOCK = "stock";

    @Resource
    private ErpFieldConfigMapper fieldConfigMapper;
    @Resource
    private ErpStockSelectPriceConfigMapper stockSelectPriceConfigMapper;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public ErpStockSelectPriceConfigRespVO getConfig() {
        return buildConfig(loadSnapshot());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ErpStockSelectPriceConfigUpdateReqVO reqVO) {
        fieldConfigMapper.selectListByModuleAndGroupForUpdate(
                TenantContextHolder.getRequiredTenantId(), PRODUCT_MODULE, PRICE_FIELD_GROUP);
        ConfigSnapshot snapshot = loadSnapshot();
        if (!Objects.equals(reqVO.getConfigVersion(), calculateVersion(snapshot))) {
            throw exception(STOCK_SELECT_PRICE_CONFIG_CHANGED);
        }
        Map<String, PriceField> fieldMap = snapshot.fields.stream()
                .collect(Collectors.toMap(PriceField::getFieldKey, item -> item, (a, b) -> a,
                        LinkedHashMap::new));
        Set<String> submittedKeys = new HashSet<>();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String operator = String.valueOf(loginUserId != null ? loginUserId : 0L);
        for (ErpStockSelectPriceConfigUpdateReqVO.Item item : reqVO.getItems()) {
            String fieldKey = StrUtil.trim(item.getFieldKey());
            if (!submittedKeys.add(fieldKey)) {
                throw exception(STOCK_SELECT_PRICE_DUPLICATE_FIELD, fieldKey);
            }
            if (!fieldMap.containsKey(fieldKey)) {
                throw exception(STOCK_SELECT_PRICE_INVALID_FIELD, fieldKey);
            }
            updateRelation(fieldKey, BIZ_TYPE_SALE, item.getSaleVisible(), operator, tenantId);
            updateRelation(fieldKey, BIZ_TYPE_PURCHASE, item.getPurchaseVisible(), operator, tenantId);
        }
    }

    @Override
    public Set<String> getSceneHiddenPriceFields(String bizType) {
        if (!isSupportedBizType(bizType)) {
            return Collections.emptySet();
        }
        List<PriceField> fields = getActivePriceFields();
        Set<String> allowedKeys = stockSelectPriceConfigMapper.selectListByBizType(bizType).stream()
                .map(ErpStockSelectPriceConfigDO::getFieldKey)
                .collect(Collectors.toSet());
        Set<String> hiddenFields = new LinkedHashSet<>();
        for (PriceField field : fields) {
            if (!allowedKeys.contains(field.getFieldKey())) {
                hiddenFields.add(field.getFieldKey());
                hiddenFields.add("col_" + field.getFieldKey());
            }
        }
        return hiddenFields;
    }

    @Override
    public List<String> getEffectiveVisibleFields(String bizType, Long businessDeptId) {
        validateEffectiveFieldsBizType(bizType);
        // Stock selection price fields are filtered by the current login user's department.
        // businessDeptId is the sales/customer business department and must not affect this visibility.
        List<String> permissionHidden = permissionApi.getCurrentUserHiddenFields(PRODUCT_MODULE);
        Set<String> hiddenFields = new HashSet<>(permissionHidden != null
                ? permissionHidden : Collections.emptyList());
        hiddenFields.addAll(getSceneHiddenPriceFields(bizType));
        return getActivePriceFields().stream()
                .map(PriceField::getFieldKey)
                .filter(fieldKey -> !hiddenFields.contains(fieldKey)
                        && !hiddenFields.contains("col_" + fieldKey))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByFieldKeys(Collection<String> fieldKeys) {
        if (CollUtil.isEmpty(fieldKeys)) {
            return;
        }
        Set<String> normalizedKeys = fieldKeys.stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .filter(key -> !key.startsWith("col_"))
                .collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(normalizedKeys)) {
            stockSelectPriceConfigMapper.physicalDeleteByFieldKeys(normalizedKeys,
                    TenantContextHolder.getRequiredTenantId());
        }
    }

    private void updateRelation(String fieldKey, String bizType, Boolean visible,
                                String operator, Long tenantId) {
        if (Boolean.TRUE.equals(visible)) {
            stockSelectPriceConfigMapper.restoreOrInsert(fieldKey, bizType, operator, tenantId);
        } else {
            stockSelectPriceConfigMapper.softDelete(fieldKey, bizType, operator, tenantId);
        }
    }

    private ConfigSnapshot loadSnapshot() {
        List<PriceField> fields = getActivePriceFields();
        Set<String> fieldKeys = fields.stream().map(PriceField::getFieldKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ErpStockSelectPriceConfigDO> relations = fieldKeys.isEmpty()
                ? new ArrayList<>() : new ArrayList<>(stockSelectPriceConfigMapper.selectListByFieldKeys(fieldKeys));
        relations.sort(Comparator.comparing(ErpStockSelectPriceConfigDO::getFieldKey)
                .thenComparing(ErpStockSelectPriceConfigDO::getBizType));
        return new ConfigSnapshot(fields, relations);
    }

    private List<PriceField> getActivePriceFields() {
        Map<String, ErpFieldConfigDO> configMap = fieldConfigMapper
                .selectListByModuleKey(PRODUCT_MODULE).stream()
                .collect(Collectors.toMap(ErpFieldConfigDO::getFieldName, item -> item,
                        (a, b) -> a, LinkedHashMap::new));
        return permissionApi.getFieldDefinitions(PRODUCT_MODULE, PRICE_FIELD_GROUP).stream()
                .filter(definition -> {
                    ErpFieldConfigDO config = configMap.get(definition.getFieldKey());
                    return config == null || !Boolean.FALSE.equals(config.getVisible());
                })
                .map(definition -> {
                    ErpFieldConfigDO config = configMap.get(definition.getFieldKey());
                    String source = config != null
                            && ErpFieldConfigFieldSourceEnum.CUSTOM.getSource().equals(config.getFieldSource())
                            ? ErpFieldConfigFieldSourceEnum.CUSTOM.getSource()
                            : ErpFieldConfigFieldSourceEnum.SYSTEM.getSource();
                    return new PriceField(definition.getFieldKey(), definition.getFieldLabel(),
                            source, definition.getSort());
                })
                .collect(Collectors.toList());
    }

    private ErpStockSelectPriceConfigRespVO buildConfig(ConfigSnapshot snapshot) {
        Map<String, Set<String>> bizTypesByField = snapshot.relations.stream()
                .collect(Collectors.groupingBy(ErpStockSelectPriceConfigDO::getFieldKey,
                        LinkedHashMap::new,
                        Collectors.mapping(ErpStockSelectPriceConfigDO::getBizType, Collectors.toSet())));
        List<ErpStockSelectPriceConfigRespVO.Field> fields = snapshot.fields.stream().map(field -> {
            Set<String> bizTypes = bizTypesByField.getOrDefault(field.getFieldKey(), Collections.emptySet());
            ErpStockSelectPriceConfigRespVO.Field result = new ErpStockSelectPriceConfigRespVO.Field();
            result.setFieldKey(field.getFieldKey());
            result.setFieldLabel(field.getFieldLabel());
            result.setFieldSource(field.getFieldSource());
            result.setSort(field.getSort());
            result.setSaleVisible(bizTypes.contains(BIZ_TYPE_SALE));
            result.setPurchaseVisible(bizTypes.contains(BIZ_TYPE_PURCHASE));
            return result;
        }).collect(Collectors.toList());
        ErpStockSelectPriceConfigRespVO result = new ErpStockSelectPriceConfigRespVO();
        result.setConfigVersion(calculateVersion(snapshot));
        result.setFields(fields);
        return result;
    }

    private String calculateVersion(ConfigSnapshot snapshot) {
        StringBuilder canonical = new StringBuilder();
        snapshot.fields.forEach(field -> canonical.append("F|")
                .append(field.getFieldKey()).append('|')
                .append(StrUtil.nullToEmpty(field.getFieldLabel())).append('|')
                .append(StrUtil.nullToEmpty(field.getFieldSource())).append('|')
                .append(field.getSort()).append('\n'));
        snapshot.relations.forEach(relation -> canonical.append("R|")
                .append(relation.getFieldKey()).append('|')
                .append(relation.getBizType()).append('\n'));
        return DigestUtil.sha256Hex(canonical.toString());
    }

    private boolean isSupportedBizType(String bizType) {
        return BIZ_TYPE_SALE.equals(bizType) || BIZ_TYPE_PURCHASE.equals(bizType);
    }

    private void validateBizType(String bizType) {
        if (!isSupportedBizType(bizType)) {
            throw exception(STOCK_SELECT_PRICE_INVALID_BIZ_TYPE, bizType);
        }
    }

    private void validateEffectiveFieldsBizType(String bizType) {
        if (!isSupportedBizType(bizType) && !BIZ_TYPE_STOCK.equals(bizType)) {
            throw exception(STOCK_SELECT_PRICE_INVALID_BIZ_TYPE, bizType);
        }
    }

    private static final class ConfigSnapshot {
        private final List<PriceField> fields;
        private final List<ErpStockSelectPriceConfigDO> relations;

        private ConfigSnapshot(List<PriceField> fields,
                               List<ErpStockSelectPriceConfigDO> relations) {
            this.fields = fields;
            this.relations = relations;
        }
    }

    private static final class PriceField {
        private final String fieldKey;
        private final String fieldLabel;
        private final String fieldSource;
        private final Integer sort;

        private PriceField(String fieldKey, String fieldLabel, String fieldSource, Integer sort) {
            this.fieldKey = fieldKey;
            this.fieldLabel = fieldLabel;
            this.fieldSource = fieldSource;
            this.sort = sort;
        }

        private String getFieldKey() {
            return fieldKey;
        }

        private String getFieldLabel() {
            return fieldLabel;
        }

        private String getFieldSource() {
            return fieldSource;
        }

        private Integer getSort() {
            return sort;
        }
    }

}
