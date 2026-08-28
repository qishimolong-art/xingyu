package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.UserPriceFieldDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.UserPriceFieldMapper;
import cn.iocoder.yudao.module.system.service.permission.ProductPriceFieldKeys;
import cn.iocoder.yudao.module.system.service.permission.ProductPriceFieldCatalogService;
import cn.iocoder.yudao.module.system.service.user.dto.UserPriceFieldConfigDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User price field visibility service.
 */
@Service
public class UserPriceFieldServiceImpl implements UserPriceFieldService {

    @Resource
    private UserPriceFieldMapper userPriceFieldMapper;
    @Resource
    private ProductPriceFieldCatalogService productPriceFieldCatalogService;

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
                .map(this::normalizePriceFieldCode)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                : getVisibleProductFieldKeys(savedFields, definitions);

        return definitions.stream().map(definition -> {
            String fieldKey = normalizePriceFieldCode(definition.getFieldKey());
            UserPriceFieldDO savedField = savedFieldMap.get(fieldKey);
            UserPriceFieldConfigDTO dto = new UserPriceFieldConfigDTO();
            dto.setId(savedField != null ? savedField.getId() : null);
            dto.setUserId(userId);
            dto.setPriceFieldCode(fieldKey);
            dto.setPriceFieldLabel(definition.getFieldLabel());
            dto.setVisible(visibleCodes.contains(fieldKey));
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
                .map(this::normalizePriceFieldCode)
                .collect(Collectors.toSet());
        Set<String> visibleCodes = fieldCodes == null ? Collections.emptySet() : fieldCodes.stream()
                .map(this::normalizePriceFieldCode)
                .filter(StrUtil::isNotBlank)
                .filter(catalogCodes::contains)
                .collect(Collectors.toSet());
        List<UserPriceFieldDO> records = definitions.stream()
                .map(definition -> {
                    String fieldKey = normalizePriceFieldCode(definition.getFieldKey());
                    return UserPriceFieldDO.builder()
                            .userId(userId)
                            .priceFieldCode(fieldKey)
                            .visible(visibleCodes.contains(fieldKey))
                            .build();
                })
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
            String fieldKey = normalizePriceFieldCode(definition.getFieldKey());
            if (!visibleCodes.contains(fieldKey)) {
                ProductPriceFieldKeys.addHiddenField(hiddenFields, fieldKey);
            }
        });
        return hiddenFields.stream().distinct().collect(Collectors.toList());
    }

    private Set<String> getVisibleProductFieldKeys(List<UserPriceFieldDO> priceFields,
                                                   List<FieldDefinitionDO> definitions) {
        Set<String> catalogCodes = definitions.stream()
                .map(FieldDefinitionDO::getFieldKey)
                .map(this::normalizePriceFieldCode)
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
                .map(this::normalizePriceFieldCode)
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
        return ProductPriceFieldKeys.normalize(code);
    }

    private List<FieldDefinitionDO> getProductPriceDefinitions() {
        return productPriceFieldCatalogService.getPriceFields();
    }

}
