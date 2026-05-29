package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpReceivableOtherItemMapper extends BaseMapperX<ErpReceivableOtherItemDO> {
    default List<ErpReceivableOtherItemDO> selectListByReceivableId(Long receivableId) {
        return selectList(ErpReceivableOtherItemDO::getReceivableId, receivableId);
    }
    default List<ErpReceivableOtherItemDO> selectListByReceivableIds(Collection<Long> receivableIds) {
        return selectList(ErpReceivableOtherItemDO::getReceivableId, receivableIds);
    }
}
