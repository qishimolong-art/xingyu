package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUniversalDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ERP 配件通用件 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpProductUniversalMapper extends BaseMapperX<ErpProductUniversalDO> {

    default List<ErpProductUniversalDO> selectListByProductId(Long productId) {
        return selectList(ErpProductUniversalDO::getProductId, productId);
    }

    default List<ErpProductUniversalDO> selectListByProductIds(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpProductUniversalDO>()
                .in(ErpProductUniversalDO::getProductId, productIds));
    }

    default int deleteByProductId(Long productId) {
        return delete(ErpProductUniversalDO::getProductId, productId);
    }

}
