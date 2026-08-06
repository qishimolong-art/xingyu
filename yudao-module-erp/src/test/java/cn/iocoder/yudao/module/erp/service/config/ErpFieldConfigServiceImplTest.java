package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigCreateCustomReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldSourceEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionCreateOrUpdateReqDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FIELD_CONFIG_FIELD_NAME_GENERATE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"deprecation", "unchecked"})
class ErpFieldConfigServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFieldConfigServiceImpl service;

    @Mock
    private ErpFieldConfigMapper fieldConfigMapper;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpStockSelectPriceConfigService stockSelectPriceConfigService;

    @Test
    void createCustomField_shouldGenerateFieldNameAndIgnoreRequestFieldName() {
        ErpFieldConfigCreateCustomReqVO reqVO = buildReqVO();
        reqVO.setFieldName("manualCode");
        when(fieldConfigMapper.selectColumnCount(eq("erp_product"), startsWith("ext_custom_"))).thenReturn(0L);
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Collections.emptyList());

        ErpFieldConfigDO result = service.createCustomField(reqVO);

        assertTrue(result.getFieldName().startsWith("custom_"));
        assertNotEquals("manualCode", result.getFieldName());
        assertEquals("ext_" + result.getFieldName(), result.getPhysicalColumn());

        ArgumentCaptor<ErpFieldConfigDO> configCaptor = ArgumentCaptor.forClass(ErpFieldConfigDO.class);
        verify(fieldConfigMapper).insert(configCaptor.capture());
        assertEquals(result.getFieldName(), configCaptor.getValue().getFieldName());
        verify(fieldConfigMapper).addColumn(eq("erp_product"), eq(result.getPhysicalColumn()), anyString());

        ArgumentCaptor<List<FieldDefinitionCreateOrUpdateReqDTO>> definitionCaptor = ArgumentCaptor.forClass(List.class);
        verify(permissionApi).createOrUpdateFieldDefinitions(definitionCaptor.capture());
        assertEquals(result.getFieldName(), definitionCaptor.getValue().get(0).getFieldKey());
        assertEquals("col_" + result.getFieldName(), definitionCaptor.getValue().get(1).getFieldKey());
    }

    @Test
    void createCustomField_shouldFailWhenGeneratedFieldNameAlwaysConflicts() {
        ErpFieldConfigCreateCustomReqVO reqVO = buildReqVO();
        when(fieldConfigMapper.selectColumnCount(eq("erp_product"), startsWith("ext_custom_"))).thenReturn(1L);

        assertServiceException(() -> service.createCustomField(reqVO), FIELD_CONFIG_FIELD_NAME_GENERATE_FAILED);

        verify(fieldConfigMapper, never()).addColumn(anyString(), anyString(), anyString());
        verify(fieldConfigMapper, never()).insert(org.mockito.ArgumentMatchers.any(ErpFieldConfigDO.class));
        verify(permissionApi, never()).createOrUpdateFieldDefinitions(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void batchUpdate_shouldUpdateCustomFieldLabelAndSyncFieldDefinitions() {
        ErpFieldConfigDO existing = buildCustomFieldDO();
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Collections.emptyList());
        when(fieldConfigMapper.selectByModuleKeyAndFieldName("erp_product", "custom_quality")).thenReturn(existing);

        ErpFieldConfigBatchUpdateReqVO reqVO = new ErpFieldConfigBatchUpdateReqVO();
        reqVO.setModuleKey("erp_product");
        reqVO.setItems(Collections.singletonList(buildBatchItem("质检等级")));

        service.batchUpdate(reqVO);

        ArgumentCaptor<ErpFieldConfigDO> configCaptor = ArgumentCaptor.forClass(ErpFieldConfigDO.class);
        verify(fieldConfigMapper).updateById(configCaptor.capture());
        ErpFieldConfigDO updated = configCaptor.getValue();
        assertEquals("质检等级", updated.getFieldLabel());
        assertEquals("custom_quality", updated.getFieldName());
        assertEquals("ext_custom_quality", updated.getPhysicalColumn());

        ArgumentCaptor<List<FieldDefinitionCreateOrUpdateReqDTO>> definitionCaptor = ArgumentCaptor.forClass(List.class);
        verify(permissionApi).createOrUpdateFieldDefinitions(definitionCaptor.capture());
        assertEquals("custom_quality", definitionCaptor.getValue().get(0).getFieldKey());
        assertEquals("质检等级", definitionCaptor.getValue().get(0).getFieldLabel());
        assertEquals("col_custom_quality", definitionCaptor.getValue().get(1).getFieldKey());
        assertEquals("质检等级", definitionCaptor.getValue().get(1).getFieldLabel());
    }

    @Test
    void batchUpdate_shouldUpdateSystemFieldLabelByRebuildingConfig() {
        ErpFieldConfigDO existing = buildSystemFieldDO();
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Collections.singletonList(existing));

        ErpFieldConfigBatchUpdateReqVO reqVO = new ErpFieldConfigBatchUpdateReqVO();
        reqVO.setModuleKey("erp_product");
        reqVO.setItems(Collections.singletonList(buildSystemBatchItem("采购参考价")));

        service.batchUpdate(reqVO);

        ArgumentCaptor<Collection<Long>> deleteIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(fieldConfigMapper).physicalDeleteByIds(deleteIdsCaptor.capture());
        assertEquals(Collections.singletonList(12L), deleteIdsCaptor.getValue());

        ArgumentCaptor<List<ErpFieldConfigDO>> insertCaptor = ArgumentCaptor.forClass(List.class);
        verify(fieldConfigMapper).insertBatch(insertCaptor.capture());
        ErpFieldConfigDO inserted = insertCaptor.getValue().get(0);
        assertEquals("erp_product", inserted.getModuleKey());
        assertEquals("referencePrice", inserted.getFieldName());
        assertEquals("采购参考价", inserted.getFieldLabel());
        assertEquals("reference_price", inserted.getPhysicalColumn());
        assertEquals(ErpFieldConfigFieldSourceEnum.SYSTEM.getSource(), inserted.getFieldSource());
        verify(permissionApi, never()).createOrUpdateFieldDefinitions(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void batchUpdate_shouldDeleteCustomFieldConfigAndRelatedDefinitions() {
        ErpFieldConfigDO existing = buildCustomFieldDO();
        when(fieldConfigMapper.selectByModuleKeyAndFieldName("erp_product", "custom_quality")).thenReturn(existing);
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Collections.emptyList());

        ErpFieldConfigBatchUpdateReqVO reqVO = new ErpFieldConfigBatchUpdateReqVO();
        reqVO.setModuleKey("erp_product");
        reqVO.setItems(Collections.emptyList());
        reqVO.setDeletedCustomFields(Collections.singletonList("custom_quality"));

        service.batchUpdate(reqVO);

        ArgumentCaptor<Collection<Long>> deleteIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(fieldConfigMapper).physicalDeleteByIds(deleteIdsCaptor.capture());
        assertEquals(Collections.singletonList(11L), deleteIdsCaptor.getValue());

        ArgumentCaptor<List<String>> fieldKeysCaptor = ArgumentCaptor.forClass(List.class);
        verify(permissionApi).deleteFieldDefinitions(eq("erp_product"), fieldKeysCaptor.capture());
        assertEquals(Arrays.asList("custom_quality", "col_custom_quality"), fieldKeysCaptor.getValue());

        ArgumentCaptor<Collection<String>> stockFieldKeysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(stockSelectPriceConfigService).deleteByFieldKeys(stockFieldKeysCaptor.capture());
        assertEquals(Collections.singleton("custom_quality"), stockFieldKeysCaptor.getValue());
        verify(fieldConfigMapper, never()).addColumn(anyString(), anyString(), anyString());
    }

    @Test
    void batchUpdate_shouldNotDeleteCustomFieldWhenItIsStillRetainedInItems() {
        ErpFieldConfigDO existing = buildCustomFieldDO();
        when(fieldConfigMapper.selectListByModuleKey("erp_product")).thenReturn(Collections.emptyList());
        when(fieldConfigMapper.selectByModuleKeyAndFieldName("erp_product", "custom_quality")).thenReturn(existing);

        ErpFieldConfigBatchUpdateReqVO reqVO = new ErpFieldConfigBatchUpdateReqVO();
        reqVO.setModuleKey("erp_product");
        reqVO.setItems(Collections.singletonList(buildBatchItem("质量等级")));
        reqVO.setDeletedCustomFields(Collections.singletonList(" custom_quality "));

        service.batchUpdate(reqVO);

        verify(fieldConfigMapper, never()).physicalDeleteByIds(org.mockito.ArgumentMatchers.anyCollection());
        verify(permissionApi, never()).deleteFieldDefinitions(eq("erp_product"), org.mockito.ArgumentMatchers.anyList());
        verify(stockSelectPriceConfigService, never()).deleteByFieldKeys(org.mockito.ArgumentMatchers.anyCollection());
        verify(fieldConfigMapper).updateById(org.mockito.ArgumentMatchers.any(ErpFieldConfigDO.class));
    }

    private ErpFieldConfigCreateCustomReqVO buildReqVO() {
        ErpFieldConfigCreateCustomReqVO reqVO = new ErpFieldConfigCreateCustomReqVO();
        reqVO.setModuleKey("erp_product");
        reqVO.setFieldLabel("供应商等级");
        reqVO.setFieldType("TEXT");
        reqVO.setFieldGroup("base_info");
        reqVO.setVisible(Boolean.TRUE);
        reqVO.setRequired(Boolean.FALSE);
        return reqVO;
    }

    private ErpFieldConfigBatchUpdateReqVO.Item buildBatchItem(String fieldLabel) {
        ErpFieldConfigBatchUpdateReqVO.Item item = new ErpFieldConfigBatchUpdateReqVO.Item();
        item.setFieldName("custom_quality");
        item.setFieldLabel(fieldLabel);
        item.setFieldSource(ErpFieldConfigFieldSourceEnum.CUSTOM.getSource());
        item.setPhysicalColumn("ext_custom_quality");
        item.setFieldType("TEXT");
        item.setFieldGroup("base_info");
        item.setRequired(Boolean.FALSE);
        item.setVisible(Boolean.TRUE);
        item.setListVisible(Boolean.TRUE);
        item.setSearchable(Boolean.FALSE);
        item.setReadonly(Boolean.FALSE);
        item.setSort(30);
        return item;
    }

    private ErpFieldConfigBatchUpdateReqVO.Item buildSystemBatchItem(String fieldLabel) {
        ErpFieldConfigBatchUpdateReqVO.Item item = new ErpFieldConfigBatchUpdateReqVO.Item();
        item.setFieldName("referencePrice");
        item.setFieldLabel(fieldLabel);
        item.setFieldSource(ErpFieldConfigFieldSourceEnum.SYSTEM.getSource());
        item.setPhysicalColumn("reference_price");
        item.setFieldType("DECIMAL");
        item.setFieldGroup("price_info");
        item.setRequired(Boolean.FALSE);
        item.setVisible(Boolean.TRUE);
        item.setListVisible(Boolean.TRUE);
        item.setSearchable(Boolean.FALSE);
        item.setReadonly(Boolean.FALSE);
        item.setSort(10);
        return item;
    }

    private ErpFieldConfigDO buildCustomFieldDO() {
        ErpFieldConfigDO config = new ErpFieldConfigDO();
        config.setId(11L);
        config.setModuleKey("erp_product");
        config.setFieldName("custom_quality");
        config.setFieldLabel("质量等级");
        config.setFieldSource(ErpFieldConfigFieldSourceEnum.CUSTOM.getSource());
        config.setPhysicalColumn("ext_custom_quality");
        config.setFieldType("TEXT");
        config.setFieldGroup("base_info");
        config.setRequired(Boolean.FALSE);
        config.setVisible(Boolean.TRUE);
        config.setListVisible(Boolean.FALSE);
        config.setSearchable(Boolean.FALSE);
        config.setReadonly(Boolean.FALSE);
        config.setSort(20);
        return config;
    }

    private ErpFieldConfigDO buildSystemFieldDO() {
        ErpFieldConfigDO config = new ErpFieldConfigDO();
        config.setId(12L);
        config.setModuleKey("erp_product");
        config.setFieldName("referencePrice");
        config.setFieldLabel("参考价");
        config.setFieldSource(ErpFieldConfigFieldSourceEnum.SYSTEM.getSource());
        config.setPhysicalColumn("reference_price");
        config.setFieldType("DECIMAL");
        config.setFieldGroup("price_info");
        config.setRequired(Boolean.FALSE);
        config.setVisible(Boolean.TRUE);
        config.setListVisible(Boolean.TRUE);
        config.setSearchable(Boolean.FALSE);
        config.setReadonly(Boolean.FALSE);
        config.setSort(10);
        return config;
    }

}
