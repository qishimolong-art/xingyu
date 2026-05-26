package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpOtherReceivableItemMapper extends BaseMapperX<ErpOtherReceivableItemDO> {

    default List<ErpOtherReceivableItemDO> selectListByReceivableId(Long receivableId) {
        return selectList(ErpOtherReceivableItemDO::getReceivableId, receivableId);
    }

    default List<ErpOtherReceivableItemDO> selectListByReceivableIds(Collection<Long> receivableIds) {
        return selectList(new LambdaQueryWrapperX<ErpOtherReceivableItemDO>()
                .in(ErpOtherReceivableItemDO::getReceivableId, receivableIds));
    }

    default void deleteByReceivableId(Long receivableId) {
        delete(ErpOtherReceivableItemDO::getReceivableId, receivableId);
    }

}
