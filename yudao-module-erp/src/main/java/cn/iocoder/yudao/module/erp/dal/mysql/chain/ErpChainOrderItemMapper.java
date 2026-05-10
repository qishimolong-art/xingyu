package cn.iocoder.yudao.module.erp.dal.mysql.chain;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 连锁开单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpChainOrderItemMapper extends BaseMapperX<ErpChainOrderItemDO> {

    default List<ErpChainOrderItemDO> selectListByChainOrderId(Long chainOrderId) {
        return selectList(ErpChainOrderItemDO::getChainOrderId, chainOrderId);
    }

    default List<ErpChainOrderItemDO> selectListByChainOrderIds(Collection<Long> chainOrderIds) {
        return selectList(ErpChainOrderItemDO::getChainOrderId, chainOrderIds);
    }

    default void deleteByChainOrderId(Long chainOrderId) {
        delete(ErpChainOrderItemDO::getChainOrderId, chainOrderId);
    }

}
