package cn.iocoder.yudao.module.erp.dal.mysql.mall;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpMallProductMappingMapper extends BaseMapperX<ErpMallProductMappingDO> {

    default ErpMallProductMappingDO selectByErpProductId(Long erpProductId) {
        return selectOne(ErpMallProductMappingDO::getErpProductId, erpProductId);
    }

    default List<ErpMallProductMappingDO> selectListByErpProductIds(Collection<Long> erpProductIds) {
        return selectList(new LambdaQueryWrapperX<ErpMallProductMappingDO>()
                .inIfPresent(ErpMallProductMappingDO::getErpProductId, erpProductIds));
    }

    default List<ErpMallProductMappingDO> selectListByMallSpuId(Long mallSpuId) {
        return selectList(new LambdaQueryWrapperX<ErpMallProductMappingDO>()
                .eqIfPresent(ErpMallProductMappingDO::getMallSpuId, mallSpuId));
    }

    default List<ErpMallProductMappingDO> selectListByMallSpuIds(Collection<Long> mallSpuIds) {
        return selectList(new LambdaQueryWrapperX<ErpMallProductMappingDO>()
                .inIfPresent(ErpMallProductMappingDO::getMallSpuId, mallSpuIds));
    }

    default List<ErpMallProductMappingDO> selectListBySyncStatus(Integer syncStatus) {
        return selectList(new LambdaQueryWrapperX<ErpMallProductMappingDO>()
                .eqIfPresent(ErpMallProductMappingDO::getSyncStatus, syncStatus)
                .orderByDesc(ErpMallProductMappingDO::getUpdateTime)
                .orderByDesc(ErpMallProductMappingDO::getId));
    }

}
