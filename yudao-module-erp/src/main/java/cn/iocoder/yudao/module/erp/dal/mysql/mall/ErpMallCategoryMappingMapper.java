package cn.iocoder.yudao.module.erp.dal.mysql.mall;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallCategoryMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpMallCategoryMappingMapper extends BaseMapperX<ErpMallCategoryMappingDO> {

    default ErpMallCategoryMappingDO selectByErpCategoryId(Long erpCategoryId) {
        return selectOne(ErpMallCategoryMappingDO::getErpCategoryId, erpCategoryId);
    }

    default List<ErpMallCategoryMappingDO> selectListByErpCategoryIds(Collection<Long> erpCategoryIds) {
        return selectList(new LambdaQueryWrapperX<ErpMallCategoryMappingDO>()
                .inIfPresent(ErpMallCategoryMappingDO::getErpCategoryId, erpCategoryIds));
    }

    default List<ErpMallCategoryMappingDO> selectListBySyncStatus(Integer syncStatus) {
        return selectList(new LambdaQueryWrapperX<ErpMallCategoryMappingDO>()
                .eqIfPresent(ErpMallCategoryMappingDO::getSyncStatus, syncStatus)
                .orderByDesc(ErpMallCategoryMappingDO::getUpdateTime)
                .orderByDesc(ErpMallCategoryMappingDO::getId));
    }

}
