package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductPriceSystemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductPriceSystemMapper;
import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 产品-价格体系关联 Service 实现类
 *
 * @author Claude
 */
@Service
@Validated
public class ErpProductPriceSystemServiceImpl implements ErpProductPriceSystemService {

    @Resource
    private ErpProductPriceSystemMapper productPriceSystemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProductPrices(Long productId, Map<Long, BigDecimal> priceSystemPrices) {
        if (productId == null) {
            return;
        }
        // 1. 空 Map 视为清空
        if (CollUtil.isEmpty(priceSystemPrices)) {
            productPriceSystemMapper.deleteByProductId(productId);
            return;
        }
        // 2. 先全量删除原有关联
        productPriceSystemMapper.deleteByProductId(productId);
        // 3. 收集非空价格并批量插入（null 代表清除，不插入）
        List<ErpProductPriceSystemDO> insertList = new ArrayList<>(priceSystemPrices.size());
        for (Map.Entry<Long, BigDecimal> entry : priceSystemPrices.entrySet()) {
            Long priceSystemId = entry.getKey();
            BigDecimal price = entry.getValue();
            if (priceSystemId == null || price == null) {
                continue;
            }
            insertList.add(ErpProductPriceSystemDO.builder()
                    .productId(productId)
                    .priceSystemId(priceSystemId)
                    .price(price)
                    .build());
        }
        if (CollUtil.isNotEmpty(insertList)) {
            productPriceSystemMapper.insertBatch(insertList);
        }
    }

    @Override
    public Map<Long, BigDecimal> getProductPriceMap(Long productId) {
        if (productId == null) {
            return Collections.emptyMap();
        }
        List<ErpProductPriceSystemDO> list = productPriceSystemMapper.selectListByProductId(productId);
        return convertMap(list, ErpProductPriceSystemDO::getPriceSystemId, ErpProductPriceSystemDO::getPrice);
    }

    @Override
    public Map<Long, BigDecimal> getProductPriceMap(Collection<Long> productIds, Long priceSystemId) {
        if (CollUtil.isEmpty(productIds) || priceSystemId == null) {
            return Collections.emptyMap();
        }
        List<ErpProductPriceSystemDO> list = productPriceSystemMapper
                .selectListByProductIdsAndPriceSystemId(productIds, priceSystemId);
        return convertMap(list, ErpProductPriceSystemDO::getProductId, ErpProductPriceSystemDO::getPrice);
    }

    @Override
    public void deleteByProductId(Long productId) {
        if (productId == null) {
            return;
        }
        productPriceSystemMapper.deleteByProductId(productId);
    }

}
