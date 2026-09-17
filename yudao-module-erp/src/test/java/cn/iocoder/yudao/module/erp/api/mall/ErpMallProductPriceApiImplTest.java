package cn.iocoder.yudao.module.erp.api.mall;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_PRODUCT_PRICE_MAPPING_MULTIPLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_PRODUCT_PRICE_MAPPING_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_PRODUCT_PRICE_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ErpMallProductPriceApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpMallProductPriceApiImpl mallProductPriceApi;

    @Mock
    private ErpMallProductMappingMapper productMappingMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ProductSpuService productSpuService;
    @Mock
    private ProductSkuService productSkuService;

    @Test
    void testUpdateMallSpuRetailPrice_updatesErpRetailPriceAndMallDisplayPrice() {
        Long mallSpuId = 100L;
        Long erpProductId = 200L;
        when(productMappingMapper.selectListByMallSpuId(mallSpuId)).thenReturn(Arrays.asList(
                ErpMallProductMappingDO.builder().mallSpuId(mallSpuId).mallSkuId(1000L).erpProductId(erpProductId).build(),
                ErpMallProductMappingDO.builder().mallSpuId(mallSpuId).mallSkuId(1001L).erpProductId(erpProductId).build()));
        when(productMapper.selectById(erpProductId)).thenReturn(new ErpProductDO().setId(erpProductId));

        mallProductPriceApi.updateMallSpuRetailPrice(mallSpuId, 1234);

        ArgumentCaptor<ErpProductDO> productCaptor = ArgumentCaptor.forClass(ErpProductDO.class);
        verify(productMapper).updateById(productCaptor.capture());
        assertEquals(erpProductId, productCaptor.getValue().getId());
        assertEquals(new BigDecimal("12.34"), productCaptor.getValue().getRetailPrice());
        verify(productSpuService).updateSpuPrice(mallSpuId, 1234);
        verify(productSkuService).updateSkuPriceBySpuId(mallSpuId, 1234);
    }

    @Test
    void testUpdateMallSpuRetailPrice_noMapping() {
        Long mallSpuId = 100L;
        when(productMappingMapper.selectListByMallSpuId(mallSpuId)).thenReturn(Collections.emptyList());

        assertServiceException(() -> mallProductPriceApi.updateMallSpuRetailPrice(mallSpuId, 1234),
                MALL_PRODUCT_PRICE_MAPPING_NOT_EXISTS);
        verifyNoInteractions(productMapper, productSpuService, productSkuService);
    }

    @Test
    void testUpdateMallSpuRetailPrice_multipleErpProducts() {
        Long mallSpuId = 100L;
        when(productMappingMapper.selectListByMallSpuId(mallSpuId)).thenReturn(Arrays.asList(
                ErpMallProductMappingDO.builder().mallSpuId(mallSpuId).erpProductId(200L).build(),
                ErpMallProductMappingDO.builder().mallSpuId(mallSpuId).erpProductId(201L).build()));

        assertServiceException(() -> mallProductPriceApi.updateMallSpuRetailPrice(mallSpuId, 1234),
                MALL_PRODUCT_PRICE_MAPPING_MULTIPLE);
        verifyNoInteractions(productMapper, productSpuService, productSkuService);
    }

    @Test
    void testUpdateMallSpuRetailPrice_negativePrice() {
        assertServiceException(() -> mallProductPriceApi.updateMallSpuRetailPrice(100L, -1),
                MALL_PRODUCT_PRICE_NEGATIVE);
        verifyNoInteractions(productMappingMapper, productMapper, productSpuService, productSkuService);
    }

    @Test
    void testUpdateMallSpuRetailPrice_productMissing() {
        Long mallSpuId = 100L;
        Long erpProductId = 200L;
        when(productMappingMapper.selectListByMallSpuId(mallSpuId)).thenReturn(Collections.singletonList(
                ErpMallProductMappingDO.builder().mallSpuId(mallSpuId).erpProductId(erpProductId).build()));

        assertServiceException(() -> mallProductPriceApi.updateMallSpuRetailPrice(mallSpuId, 1234),
                PRODUCT_NOT_EXISTS);
        verify(productMapper).selectById(erpProductId);
        verify(productMapper, never()).updateById(org.mockito.ArgumentMatchers.any(ErpProductDO.class));
        verifyNoInteractions(productSpuService, productSkuService);
    }

}
