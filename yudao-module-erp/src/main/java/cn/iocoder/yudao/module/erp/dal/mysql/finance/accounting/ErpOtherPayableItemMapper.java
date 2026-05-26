package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpOtherPayableItemMapper extends BaseMapperX<ErpOtherPayableItemDO> {

    default List<ErpOtherPayableItemDO> selectListByPayableId(Long payableId) {
        return selectList(ErpOtherPayableItemDO::getPayableId, payableId);
    }

    default List<ErpOtherPayableItemDO> selectListByPayableIds(Collection<Long> payableIds) {
        return selectList(ErpOtherPayableItemDO::getPayableId, payableIds);
    }

    default int deleteByPayableId(Long payableId) {
        return delete(ErpOtherPayableItemDO::getPayableId, payableId);
    }

}
