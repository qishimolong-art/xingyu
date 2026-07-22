package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface ErpWarehouseMoveItemMapper extends BaseMapperX<ErpWarehouseMoveItemDO> {

    default List<ErpWarehouseMoveItemDO> selectListByMoveId(Long moveId) {
        return selectList(ErpWarehouseMoveItemDO::getMoveId, moveId);
    }

    default List<ErpWarehouseMoveItemDO> selectListByMoveIds(Collection<Long> moveIds) {
        if (moveIds == null || moveIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(ErpWarehouseMoveItemDO::getMoveId, moveIds);
    }

    default int deleteByMoveId(Long moveId) {
        return delete(ErpWarehouseMoveItemDO::getMoveId, moveId);
    }

}
