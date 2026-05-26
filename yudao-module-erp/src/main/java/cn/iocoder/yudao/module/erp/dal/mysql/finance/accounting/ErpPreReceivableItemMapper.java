package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 预收账款单明细 Mapper
 */
@Mapper
public interface ErpPreReceivableItemMapper extends BaseMapperX<ErpPreReceivableItemDO> {

    default List<ErpPreReceivableItemDO> selectListByPreReceivableId(Long preReceivableId) {
        return selectList(ErpPreReceivableItemDO::getPreReceivableId, preReceivableId);
    }

    default List<ErpPreReceivableItemDO> selectListByPreReceivableIds(Collection<Long> preReceivableIds) {
        return selectList(ErpPreReceivableItemDO::getPreReceivableId, preReceivableIds);
    }

}
