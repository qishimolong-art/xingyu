package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.UserPriceFieldDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.UserPriceFieldMapper;
import cn.iocoder.yudao.module.system.service.user.dto.UserPriceFieldConfigDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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

/**
 * User price field visibility service.
 */
@Service
public class UserPriceFieldServiceImpl implements UserPriceFieldService {

    private static final String PRODUCT_MODULE = "erp_product";
    private static final String PRICE_FIELD_GROUP = "price_info";

    private static final Map<String, String> LEGACY_PRICE_FIELD_CODE_MAP = new LinkedHashMap<String, String>() {{
        put("branch_price", "referencePrice");
        put("wholesale_price", "wholesalePrice");
        put("retail_price", "retailPrice");
        put("cost_price", "purchasePrice");
        put("last_purchase_price", "lastPurchasePrice");
        put("gross_profit_rate", "grossProfitRate");
        put("backup_price1", "backupPrice1");
    }};

    private static final Map<String, String> DEFAULT_PRICE_FIELD_LABEL_MAP = new LinkedHashMap<String, String>() {{
        put("purchasePrice", "采购价");
        put("salePrice", "销售价");
        put("minPrice", "最低价");
        put("referencePrice", "参考价");
        put("retailPrice", "零售价");
        put("lastPurchasePrice", "最后一次采购价");
        put("grossProfitRate", "毛利率");
        put("backupPrice1", "备用价1");
        put("wholesalePrice", "批发价");
        put("sharePrice", "股份价");
    }};

    @Resource
    private UserPriceFieldMapper userPriceFieldMapper;
    @Resource
    private FieldDefinitionMapper fieldDefinitionMapper;

    @Override
    public List<UserPriceFieldDO> getUserPriceFields(Long userId) {
        return userPriceFieldMapper.selectListByUserId(userId);
    }

    @Override
    public List<UserPriceFieldConfigDTO> getUserPriceFieldConfigs(Long userId) {
        List<FieldDefinitionDO> definitions = getProductPriceDefinitions();
        List<UserPriceFieldDO> savedFields = userPriceFieldMapper.selectListByUserId(userId);
        Map<String, UserPriceFieldDO> savedFieldMap = buildSavedFieldMap(savedFields, definitions);
        Set<String> visibleCodes = savedFields.isEmpty() ? definitions.stream()
                .map(FieldDefinitionDO::getFieldKey)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                : getVisibleProductFieldKeys(savedFields, definitions);

        return definitions.stream().map(definition -> {
            UserPriceFieldDO savedField = savedFieldMap.get(definition.getFieldKey());
            UserPriceFieldConfigDTO dto = new UserPriceFieldConfigDTO();
            dto.setId(savedField != null ? savedField.getId() : null);
            dto.setUserId(userId);
            dto.setPriceFieldCode(definition.getFieldKey());
            dto.setPriceFieldLabel(definition.getFieldLabel());
            dto.setVisible(visibleCodes.contains(definition.getFieldKey()));
            dto.setSort(definition.getSort());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserPriceFields(Long userId, List<String> fieldCodes) {
        userPriceFieldMapper.deleteByUserId(userId, TenantContextHolder.getRequiredTenantId());

        List<FieldDefinitionDO> definitions = getProductPriceDefinitions();
        Set<String> catalogCodes = definitions.stream()
                .map(FieldDefinitionDO::getFieldKey)
                .collect(Collectors.toSet());
        Set<String> visibleCodes = fieldCodes == null ? Collections.emptySet() : fieldCodes.stream()
                .map(this::normalizePriceFieldCode)
                .filter(StrUtil::isNotBlank)
                .filter(catalogCodes::contains)
                .collect(Collectors.toSet());
        List<UserPriceFieldDO> records = definitions.stream()
                .map(definition -> UserPriceFieldDO.builder()
                        .userId(userId)
                        .priceFieldCode(definition.getFieldKey())
                        .visible(visibleCodes.contains(definition.getFieldKey()))
                        .build())
                .collect(Collectors.toList());
        records.forEach(userPriceFieldMapper::insert);
    }

    @Override
    public List<String> getHiddenProductPriceFields(Long userId) {
        List<UserPriceFieldDO> priceFields = userPriceFieldMapper.selectListByUserId(userId);
        if (priceFields.isEmpty()) {
            return Collections.emptyList();
        }
        List<FieldDefinitionDO> definitions = getProductPriceDefinitions();
        Set<String> visibleCodes = getVisibleProductFieldKeys(priceFields, definitions);
        List<String> hiddenFields = new ArrayList<>();
        definitions.forEach(definition -> {
            String fieldKey = definition.getFieldKey();
            if (!visibleCodes.contains(fieldKey)) {
                hiddenFields.add(fieldKey);
                hiddenFields.add("col_" + fieldKey);
            }
        });
        return hiddenFields.stream().distinct().collect(Collectors.toList());
    }

    private Set<String> getVisibleProductFieldKeys(List<UserPriceFieldDO> priceFields,
                                                   List<FieldDefinitionDO> definitions) {
        Set<String> catalogCodes = definitions.stream()
                .map(FieldDefinitionDO::getFieldKey)
                .collect(Collectors.toSet());
        return priceFields.stream()
                .filter(item -> Boolean.TRUE.equals(item.getVisible()))
                .map(UserPriceFieldDO::getPriceFieldCode)
                .map(this::normalizePriceFieldCode)
                .filter(StrUtil::isNotBlank)
                .filter(catalogCodes::contains)
                .collect(Collectors.toSet());
    }

    private Map<String, UserPriceFieldDO> buildSavedFieldMap(List<UserPriceFieldDO> savedFields,
                                                             List<FieldDefinitionDO> definitions) {
        Set<String> catalogCodes = definitions.stream()
                .map(FieldDefinitionDO::getFieldKey)
                .collect(Collectors.toSet());
        Map<String, UserPriceFieldDO> savedFieldMap = new LinkedHashMap<>();
        savedFields.forEach(item -> {
            String fieldKey = normalizePriceFieldCode(item.getPriceFieldCode());
            if (StrUtil.isNotBlank(fieldKey) && catalogCodes.contains(fieldKey)) {
                savedFieldMap.put(fieldKey, item);
            }
        });
        return savedFieldMap;
    }

    private String normalizePriceFieldCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        String trimmedCode = StrUtil.trim(code);
        return LEGACY_PRICE_FIELD_CODE_MAP.getOrDefault(trimmedCode, trimmedCode);
    }

    private List<FieldDefinitionDO> getProductPriceDefinitions() {
        List<FieldDefinitionDO> definitions = fieldDefinitionMapper.selectListByModuleAndGroup(
                PRODUCT_MODULE, PRICE_FIELD_GROUP);
        if (definitions == null || definitions.isEmpty()) {
            return buildDefaultPriceDefinitions();
        }
        return definitions.stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.isNotBlank(item.getFieldKey()))
                .sorted(Comparator
                        .comparing(FieldDefinitionDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(FieldDefinitionDO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.collectingAndThen(Collectors.toList(), items -> {
                    Set<String> seenKeys = new HashSet<>();
                    return items.stream()
                            .filter(item -> seenKeys.add(item.getFieldKey()))
                            .collect(Collectors.toList());
                }));
    }

    private List<FieldDefinitionDO> buildDefaultPriceDefinitions() {
        List<FieldDefinitionDO> definitions = new ArrayList<>();
        int sort = 1;
        for (Map.Entry<String, String> entry : DEFAULT_PRICE_FIELD_LABEL_MAP.entrySet()) {
            FieldDefinitionDO definition = new FieldDefinitionDO();
            definition.setModule(PRODUCT_MODULE);
            definition.setFieldGroup(PRICE_FIELD_GROUP);
            definition.setFieldKey(entry.getKey());
            definition.setFieldLabel(entry.getValue());
            definition.setSort(sort++);
            definitions.add(definition);
        }
        return definitions;
    }

}
