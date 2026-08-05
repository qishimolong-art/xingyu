package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigCreateCustomReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionCreateOrUpdateReqDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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

}
