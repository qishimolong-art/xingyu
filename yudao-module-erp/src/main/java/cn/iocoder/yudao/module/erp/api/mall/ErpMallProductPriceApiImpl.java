package cn.iocoder.yudao.module.erp.api.mall;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_PRODUCT_PRICE_MAPPING_MULTIPLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_PRODUCT_PRICE_MAPPING_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_PRODUCT_PRICE_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;

/**
 * ERP mall product price API implementation.
 */
@Service
@Validated
public class ErpMallProductPriceApiImpl implements ErpMallProductPriceApi {

    @Resource
    private ErpMallProductMappingMapper productMappingMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ProductSpuService productSpuService;
    @Resource
    private ProductSkuService productSkuService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMallSpuRetailPrice(Long mallSpuId, Integer price) {
        if (price == null || price < 0) {
            throw exception(MALL_PRODUCT_PRICE_NEGATIVE);
        }
        Long erpProductId = getUniqueErpProductId(mallSpuId);
        ErpProductDO product = productMapper.selectById(erpProductId);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }

        productMapper.updateById(new ErpProductDO()
                .setId(erpProductId)
                .setRetailPrice(BigDecimal.valueOf(price, 2)));
        productSpuService.updateSpuPrice(mallSpuId, price);
        productSkuService.updateSkuPriceBySpuId(mallSpuId, price);
    }

    private Long getUniqueErpProductId(Long mallSpuId) {
        List<ErpMallProductMappingDO> mappings = productMappingMapper.selectListByMallSpuId(mallSpuId);
        if (CollUtil.isEmpty(mappings)) {
            throw exception(MALL_PRODUCT_PRICE_MAPPING_NOT_EXISTS);
        }
        Set<Long> erpProductIds = mappings.stream()
                .map(ErpMallProductMappingDO::getErpProductId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (erpProductIds.isEmpty()) {
            throw exception(MALL_PRODUCT_PRICE_MAPPING_NOT_EXISTS);
        }
        if (erpProductIds.size() > 1) {
            throw exception(MALL_PRODUCT_PRICE_MAPPING_MULTIPLE);
        }
        return erpProductIds.iterator().next();
    }

}
