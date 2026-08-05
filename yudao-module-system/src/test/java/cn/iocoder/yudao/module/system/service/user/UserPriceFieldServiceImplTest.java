package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.UserPriceFieldDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.UserPriceFieldMapper;
import cn.iocoder.yudao.module.system.service.user.dto.UserPriceFieldConfigDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPriceFieldServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private UserPriceFieldServiceImpl service;

    @Mock
    private UserPriceFieldMapper userPriceFieldMapper;
    @Mock
    private FieldDefinitionMapper fieldDefinitionMapper;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void getUserPriceFieldConfigs_shouldUseProductPriceDefinitionsAndLegacyCodes() {
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Arrays.asList(field("referencePrice", "参考价", 1),
                        field("sharePrice", "股份价", 2)));
        when(userPriceFieldMapper.selectListByUserId(10L))
                .thenReturn(Collections.singletonList(record(10L, "branch_price", true)));

        List<UserPriceFieldConfigDTO> configs = service.getUserPriceFieldConfigs(10L);

        assertEquals(2, configs.size());
        assertEquals("referencePrice", configs.get(0).getPriceFieldCode());
        assertEquals("参考价", configs.get(0).getPriceFieldLabel());
        assertEquals(true, configs.get(0).getVisible());
        assertEquals("sharePrice", configs.get(1).getPriceFieldCode());
        assertEquals(false, configs.get(1).getVisible());
    }

    @Test
    void saveUserPriceFields_shouldPersistEveryProductPriceDefinition() {
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Arrays.asList(field("referencePrice", "参考价", 1),
                        field("sharePrice", "股份价", 2)));

        service.saveUserPriceFields(10L, Arrays.asList("branch_price", "sharePrice", "unknown"));

        verify(userPriceFieldMapper).deleteByUserId(10L, 1L);
        ArgumentCaptor<UserPriceFieldDO> captor = ArgumentCaptor.forClass(UserPriceFieldDO.class);
        verify(userPriceFieldMapper, times(2)).insert(captor.capture());
        assertEquals(Arrays.asList("referencePrice", "sharePrice"),
                Arrays.asList(captor.getAllValues().get(0).getPriceFieldCode(),
                        captor.getAllValues().get(1).getPriceFieldCode()));
        assertEquals(Arrays.asList(true, true),
                Arrays.asList(captor.getAllValues().get(0).getVisible(),
                        captor.getAllValues().get(1).getVisible()));
    }

    @Test
    void getHiddenProductPriceFields_shouldHideUnsavedNewDefinitions() {
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Arrays.asList(field("referencePrice", "参考价", 1),
                        field("sharePrice", "股份价", 2)));
        when(userPriceFieldMapper.selectListByUserId(10L))
                .thenReturn(Collections.singletonList(record(10L, "branch_price", true)));

        assertEquals(Arrays.asList("sharePrice", "col_sharePrice"),
                service.getHiddenProductPriceFields(10L));
    }

    private static FieldDefinitionDO field(String key, String label, int sort) {
        FieldDefinitionDO field = new FieldDefinitionDO();
        field.setId((long) sort);
        field.setModule("erp_product");
        field.setFieldGroup("price_info");
        field.setFieldKey(key);
        field.setFieldLabel(label);
        field.setSort(sort);
        return field;
    }

    private static UserPriceFieldDO record(Long userId, String code, Boolean visible) {
        UserPriceFieldDO record = new UserPriceFieldDO();
        record.setUserId(userId);
        record.setPriceFieldCode(code);
        record.setVisible(visible);
        return record;
    }

}
