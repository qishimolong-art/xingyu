package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpStockSelectPriceConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpStockSelectPriceConfigMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ErpStockSelectPriceConfigServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockSelectPriceConfigServiceImpl service;

    @Mock
    private ErpFieldConfigMapper fieldConfigMapper;
    @Mock
    private ErpStockSelectPriceConfigMapper stockSelectPriceConfigMapper;
    @Mock
    private PermissionApi permissionApi;

    @Test
    void getSceneHiddenPriceFields_shouldHideFieldsOutsideSaleAllowlist() {
        mockPriceFields();
        when(stockSelectPriceConfigMapper.selectListByBizType("sale"))
                .thenReturn(Collections.singletonList(relation("salePrice", "sale")));

        Set<String> result = service.getSceneHiddenPriceFields("sale");

        assertEquals(new LinkedHashSet<>(Arrays.asList("vipPrice", "col_vipPrice")), result);
    }

    @Test
    void getSceneHiddenPriceFields_shouldIgnoreNonBusinessStockScene() {
        assertTrue(service.getSceneHiddenPriceFields(null).isEmpty());
        assertTrue(service.getSceneHiddenPriceFields("stock").isEmpty());
    }

    @Test
    void getEffectiveVisibleFields_shouldIntersectSceneAndLoginDepartmentPricePermission() {
        mockPriceFields();
        when(stockSelectPriceConfigMapper.selectListByBizType("sale")).thenReturn(Arrays.asList(
                relation("salePrice", "sale"), relation("vipPrice", "sale")));
        when(permissionApi.getCurrentUserHiddenFields("erp_product", 20L))
                .thenReturn(Arrays.asList("salePrice", "col_salePrice"));

        List<String> result = service.getEffectiveVisibleFields("sale", 20L);

        assertEquals(Collections.singletonList("vipPrice"), result);
    }

    @Test
    void getEffectiveVisibleFields_purchaseMustUseLoginDepartmentPermissionContext() {
        mockPriceFields();
        when(stockSelectPriceConfigMapper.selectListByBizType("purchase")).thenReturn(Arrays.asList(
                relation("salePrice", "purchase"), relation("vipPrice", "purchase")));
        when(permissionApi.getCurrentUserHiddenFields("erp_product", 999L))
                .thenReturn(Collections.singletonList("vipPrice"));

        List<String> result = service.getEffectiveVisibleFields("purchase", 999L);

        assertEquals(Collections.singletonList("salePrice"), result);
    }

    @Test
    void getEffectiveVisibleFields_shouldNormalizeLegacyPurchasePriceKey() {
        when(permissionApi.getFieldDefinitions("erp_product", "price_info")).thenReturn(Arrays.asList(
                definition("purchase_price", "采购价", 10),
                definition("salePrice", "销售价", 20)));
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Arrays.asList(
                field(1L, "purchasePrice", "采购价", "SYSTEM", 10),
                field(2L, "salePrice", "销售价", "SYSTEM", 20)));
        when(stockSelectPriceConfigMapper.selectListByBizType("purchase")).thenReturn(Arrays.asList(
                relation("purchase_price", "purchase"), relation("salePrice", "purchase")));
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());

        List<String> result = service.getEffectiveVisibleFields("purchase", null);

        assertEquals(Arrays.asList("purchasePrice", "salePrice"), result);
    }

    @Test
    void getConfig_shouldNormalizeLegacyPurchasePriceRelation() {
        when(permissionApi.getFieldDefinitions("erp_product", "price_info")).thenReturn(Collections.singletonList(
                definition("purchase_price", "采购价", 10)));
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Collections.singletonList(
                field(1L, "purchasePrice", "采购价", "SYSTEM", 10)));
        when(stockSelectPriceConfigMapper.selectListByBizType("sale")).thenReturn(Collections.emptyList());
        when(stockSelectPriceConfigMapper.selectListByBizType("purchase"))
                .thenReturn(Collections.singletonList(relation("purchase_price", "purchase")));

        ErpStockSelectPriceConfigRespVO result = service.getConfig();

        assertEquals(1, result.getFields().size());
        assertEquals("purchasePrice", result.getFields().get(0).getFieldKey());
        assertTrue(result.getFields().get(0).getPurchaseVisible());
    }

    @Test
    void getEffectiveVisibleFields_stockShouldOnlyApplyProductPricePermission() {
        mockPriceFields();
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.singletonList("vipPrice"));

        List<String> result = service.getEffectiveVisibleFields("stock", null);

        assertEquals(Collections.singletonList("salePrice"), result);
    }

    private void mockPriceFields() {
        when(permissionApi.getFieldDefinitions("erp_product", "price_info")).thenReturn(Arrays.asList(
                definition("salePrice", "销售价", 10),
                definition("vipPrice", "会员价", 20)));
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Arrays.asList(
                field(2L, "vipPrice", "会员价", "CUSTOM", 20),
                new ErpFieldConfigDO().setId(3L).setModuleKey("erp_product")
                        .setFieldName("brand").setFieldLabel("品牌")
                        .setFieldGroup("base_info").setVisible(true).setSort(30)));
    }

    private FieldDefinitionRespDTO definition(String key, String label, Integer sort) {
        FieldDefinitionRespDTO definition = new FieldDefinitionRespDTO();
        definition.setFieldKey(key);
        definition.setFieldLabel(label);
        definition.setSort(sort);
        return definition;
    }

    private ErpFieldConfigDO field(Long id, String key, String label, String source, Integer sort) {
        return new ErpFieldConfigDO().setId(id).setModuleKey("erp_product")
                .setFieldName(key).setFieldLabel(label).setFieldSource(source)
                .setFieldGroup("price_info").setVisible(true).setSort(sort);
    }

    private ErpStockSelectPriceConfigDO relation(String fieldKey, String bizType) {
        return new ErpStockSelectPriceConfigDO().setFieldKey(fieldKey).setBizType(bizType);
    }

}
