package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.ProductFieldConfigDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ProductFieldConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductPriceFieldCatalogService {

    public static final String PRODUCT_MODULE = "erp_product";
    public static final String PRICE_FIELD_GROUP = "price_info";

    @Resource
    private ProductFieldConfigMapper productFieldConfigMapper;
    @Resource
    private FieldDefinitionMapper fieldDefinitionMapper;

    public List<FieldDefinitionDO> getPriceFields() {
        List<ProductFieldConfigDO> configs = productFieldConfigMapper.selectListByModuleAndGroup(
                PRODUCT_MODULE, PRICE_FIELD_GROUP);
        if (configs != null && !configs.isEmpty()) {
            return buildDefinitions(configs);
        }
        return buildDefinitionsFromLegacyCatalog(fieldDefinitionMapper.selectListByModuleAndGroup(
                PRODUCT_MODULE, PRICE_FIELD_GROUP));
    }

    public List<FieldDefinitionDO> getPriceFieldsForUpdate(Long tenantId) {
        List<ProductFieldConfigDO> configs = productFieldConfigMapper.selectListByModuleAndGroupForUpdate(
                tenantId, PRODUCT_MODULE, PRICE_FIELD_GROUP);
        if (configs != null && !configs.isEmpty()) {
            return buildDefinitions(configs);
        }
        return buildDefinitionsFromLegacyCatalog(fieldDefinitionMapper.selectListByModuleAndGroupForUpdate(
                tenantId, PRODUCT_MODULE, PRICE_FIELD_GROUP));
    }

    private List<FieldDefinitionDO> buildDefinitions(List<ProductFieldConfigDO> configs) {
        return configs.stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.isNotBlank(item.getFieldName()))
                .sorted(Comparator
                        .comparing(ProductFieldConfigDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductFieldConfigDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDefinition)
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::deduplicate));
    }

    private FieldDefinitionDO toDefinition(ProductFieldConfigDO config) {
        FieldDefinitionDO definition = new FieldDefinitionDO();
        definition.setId(config.getId());
        definition.setModule(PRODUCT_MODULE);
        definition.setFieldGroup(PRICE_FIELD_GROUP);
        definition.setFieldKey(config.getFieldName());
        definition.setFieldLabel(config.getFieldLabel());
        definition.setSort(config.getSort());
        return definition;
    }

    private List<FieldDefinitionDO> buildDefinitionsFromLegacyCatalog(List<FieldDefinitionDO> definitions) {
        if (definitions == null) {
            return java.util.Collections.emptyList();
        }
        return definitions.stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.isNotBlank(item.getFieldKey()))
                .sorted(Comparator
                        .comparing(FieldDefinitionDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(FieldDefinitionDO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::deduplicate));
    }

    private List<FieldDefinitionDO> deduplicate(List<FieldDefinitionDO> definitions) {
        Set<String> seenKeys = new HashSet<>();
        return definitions.stream()
                .filter(item -> seenKeys.add(ProductPriceFieldKeys.normalize(item.getFieldKey())))
                .collect(Collectors.toList());
    }

}
