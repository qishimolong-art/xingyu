package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldSourceEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

public class ErpSalePriceLevelPricePickerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePriceLevelPricePicker pricePicker;

    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpFieldConfigMapper fieldConfigMapper;

    @Test
    public void testPickProductPriceMap_sharePrice() {
        ErpProductDO product = new ErpProductDO()
                .setId(1L)
                .setSalePrice(new BigDecimal("20.00"))
                .setSharePrice(new BigDecimal("18.88"));
        when(productMapper.selectByIds(anyCollection())).thenReturn(List.of(product));

        Map<Long, BigDecimal> result = pricePicker.pickProductPriceMap(List.of(1L), 10);

        assertEquals(new BigDecimal("18.88"), result.get(1L));
    }

    @Test
    public void testPickProductPriceMap_orderPriceUsesCustomFieldAndFallbackSalePrice() {
        ErpProductDO firstProduct = new ErpProductDO()
                .setId(1L)
                .setSalePrice(new BigDecimal("20.00"));
        ErpProductDO secondProduct = new ErpProductDO()
                .setId(2L)
                .setSalePrice(null)
                .setRetailPrice(new BigDecimal("30.00"))
                .setReferencePrice(new BigDecimal("40.00"));
        ErpFieldConfigDO orderPriceField = new ErpFieldConfigDO()
                .setModuleKey(ErpFieldConfigModuleEnum.ERP_PRODUCT.getKey())
                .setFieldSource(ErpFieldConfigFieldSourceEnum.CUSTOM.getSource())
                .setFieldGroup("price_info")
                .setFieldLabel("订货价")
                .setFieldName("orderPrice")
                .setPhysicalColumn("ext_order_price");
        Map<String, Object> firstRow = new HashMap<>();
        firstRow.put("id", 1L);
        firstRow.put("ext_order_price", "66.60");
        Map<String, Object> secondRow = new HashMap<>();
        secondRow.put("id", 2L);
        secondRow.put("ext_order_price", "");
        when(productMapper.selectByIds(anyCollection())).thenReturn(Arrays.asList(firstProduct, secondProduct));
        when(fieldConfigMapper.selectListByModuleKey(ErpFieldConfigModuleEnum.ERP_PRODUCT.getKey()))
                .thenReturn(List.of(orderPriceField));
        when(productMapper.selectCustomFieldMaps(anyCollection(), anyCollection()))
                .thenReturn(Arrays.asList(firstRow, secondRow));

        Map<Long, BigDecimal> result = pricePicker.pickProductPriceMap(Arrays.asList(1L, 2L), 11);

        assertEquals(new BigDecimal("66.60"), result.get(1L));
        assertEquals(new BigDecimal("30.00"), result.get(2L));
    }

}
