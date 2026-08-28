package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.ProductFieldConfigDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ProductFieldConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductPriceFieldCatalogServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ProductPriceFieldCatalogService service;

    @Mock
    private ProductFieldConfigMapper productFieldConfigMapper;
    @Mock
    private FieldDefinitionMapper fieldDefinitionMapper;

    @Test
    void getPriceFields_shouldUseErpFieldConfigPriceGroup() {
        when(productFieldConfigMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Arrays.asList(config("purchasePrice", "采购价", 180, 1L),
                        config("salePrice", "销售价", 190, 2L),
                        config("minPrice", "最低价", 200, 3L),
                        config("referencePrice", "参考价", 210, 4L),
                        config("customDealerPrice", "订货价", 440, 5L)));

        List<FieldDefinitionDO> fields = service.getPriceFields();

        assertEquals(Arrays.asList("purchasePrice", "salePrice", "minPrice", "referencePrice", "customDealerPrice"),
                Arrays.asList(fields.get(0).getFieldKey(), fields.get(1).getFieldKey(), fields.get(2).getFieldKey(),
                        fields.get(3).getFieldKey(), fields.get(4).getFieldKey()));
        assertEquals("采购价", fields.get(0).getFieldLabel());
        assertEquals("订货价", fields.get(4).getFieldLabel());
        verify(fieldDefinitionMapper, never()).selectListByModuleAndGroup("erp_product", "price_info");
    }

    @Test
    void getPriceFields_shouldFallbackToLegacyDefinitionsWhenErpFieldConfigIsEmpty() {
        when(productFieldConfigMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Collections.emptyList());
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Arrays.asList(definition("purchasePrice", "采购价", 1, 1L),
                        definition("referencePrice", "参考价", 10, 2L)));

        List<FieldDefinitionDO> fields = service.getPriceFields();

        assertEquals(Arrays.asList("purchasePrice", "referencePrice"),
                Arrays.asList(fields.get(0).getFieldKey(), fields.get(1).getFieldKey()));
    }

    @Test
    void getPriceFields_shouldDeduplicatePriceAliasesFromConfig() {
        when(productFieldConfigMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Arrays.asList(config("purchase_price", "历史采购价", 100, 1L),
                        config("purchasePrice", "采购价", 180, 2L),
                        config("referencePrice", "参考价", 210, 3L)));

        List<FieldDefinitionDO> fields = service.getPriceFields();

        assertEquals(Arrays.asList("purchase_price", "referencePrice"),
                Arrays.asList(fields.get(0).getFieldKey(), fields.get(1).getFieldKey()));
        assertEquals("历史采购价", fields.get(0).getFieldLabel());
    }

    private static ProductFieldConfigDO config(String fieldName, String label, Integer sort, Long id) {
        ProductFieldConfigDO config = new ProductFieldConfigDO();
        config.setId(id);
        config.setModuleKey("erp_product");
        config.setFieldGroup("price_info");
        config.setFieldName(fieldName);
        config.setFieldLabel(label);
        config.setSort(sort);
        return config;
    }

    private static FieldDefinitionDO definition(String fieldKey, String label, Integer sort, Long id) {
        FieldDefinitionDO definition = new FieldDefinitionDO();
        definition.setId(id);
        definition.setModule("erp_product");
        definition.setFieldGroup("price_info");
        definition.setFieldKey(fieldKey);
        definition.setFieldLabel(label);
        definition.setSort(sort);
        return definition;
    }

}
