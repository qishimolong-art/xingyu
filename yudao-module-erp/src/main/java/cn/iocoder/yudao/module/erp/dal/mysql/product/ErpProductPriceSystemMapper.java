package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductPriceSystemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface ErpProductPriceSystemMapper extends BaseMapperX<ErpProductPriceSystemDO> {

    default List<ErpProductPriceSystemDO> selectListByProductId(Long productId) {
        return selectList(ErpProductPriceSystemDO::getProductId, productId);
    }

    default List<ErpProductPriceSystemDO> selectListByProductIds(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<ErpProductPriceSystemDO>()
                .in(ErpProductPriceSystemDO::getProductId, productIds));
    }

    default List<ErpProductPriceSystemDO> selectListByProductIdsAndPriceSystemId(Collection<Long> productIds, Long priceSystemId) {
        if (CollUtil.isEmpty(productIds) || priceSystemId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<ErpProductPriceSystemDO>()
                .in(ErpProductPriceSystemDO::getProductId, productIds)
                .eq(ErpProductPriceSystemDO::getPriceSystemId, priceSystemId));
    }

    default int deleteByProductId(Long productId) {
        return delete(ErpProductPriceSystemDO::getProductId, productId);
    }
}
