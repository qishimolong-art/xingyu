package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldSourceEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_LEVEL_PERMISSION_DENIED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ErpSalePriceLevelPermissionValidatorTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePriceLevelPermissionValidator validator;

    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpFieldConfigMapper fieldConfigMapper;

    @Test
    void validateSelectablePriceLevel_salePriceVisibleAllows() {
        when(permissionApi.getCurrentUserHiddenFields(eq("erp_product"), eq(20L)))
                .thenReturn(Collections.singletonList("purchasePrice"));

        validator.validateSelectablePriceLevel(8, 20L);
    }

    @Test
    void validateSelectablePriceLevel_salePriceHiddenThrows() {
        when(permissionApi.getCurrentUserHiddenFields(eq("erp_product"), eq(20L)))
                .thenReturn(Collections.singletonList("col_sale_price"));

        assertServiceException(() -> validator.validateSelectablePriceLevel(8, 20L),
                SALE_PRICE_LEVEL_PERMISSION_DENIED);
    }

    @Test
    void validateSelectablePriceLevel_orderPriceCustomFieldHiddenThrows() {
        when(permissionApi.getCurrentUserHiddenFields(eq("erp_product"), eq(20L)))
                .thenReturn(Collections.singletonList("col_orderPriceCustom"));
        when(fieldConfigMapper.selectListByModuleKey(eq(ErpFieldConfigModuleEnum.ERP_PRODUCT.getKey())))
                .thenReturn(List.of(new ErpFieldConfigDO()
                        .setFieldSource(ErpFieldConfigFieldSourceEnum.CUSTOM.getSource())
                        .setFieldGroup("price_info")
                        .setFieldLabel("订货价")
                        .setFieldName("orderPriceCustom")));

        assertServiceException(() -> validator.validateSelectablePriceLevel(11, 20L),
                SALE_PRICE_LEVEL_PERMISSION_DENIED);
    }

    @Test
    void validateSelectablePriceLevel_invalidLevelThrows() {
        assertServiceException(() -> validator.validateSelectablePriceLevel(99, 20L),
                SALE_PRICE_LEVEL_PERMISSION_DENIED);
    }

}
