package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 采购调价单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchasePriceAdjustItemMapper extends BaseMapperX<ErpPurchasePriceAdjustItemDO> {

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustId(Long adjustId) {
        return selectList(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustId);
    }

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustIds(Collection<Long> adjustIds) {
        return selectList(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustIds);
    }

    default void deleteByAdjustId(Long adjustId) {
        delete(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustId);
    }

}
