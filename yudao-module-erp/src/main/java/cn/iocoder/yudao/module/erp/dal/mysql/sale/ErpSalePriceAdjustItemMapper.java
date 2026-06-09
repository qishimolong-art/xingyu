package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 销售调价单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpSalePriceAdjustItemMapper extends BaseMapperX<ErpSalePriceAdjustItemDO> {

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustId(Long adjustId) {
        return selectList(ErpSalePriceAdjustItemDO::getAdjustId, adjustId);
    }

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustIds(Collection<Long> adjustIds) {
        return selectList(ErpSalePriceAdjustItemDO::getAdjustId, adjustIds);
    }

    default List<ErpSalePriceAdjustItemDO> selectListBySaleOutItemIds(Collection<Long> saleOutItemIds) {
        return selectList(ErpSalePriceAdjustItemDO::getSaleOutItemId, saleOutItemIds);
    }

    default void deleteByAdjustId(Long adjustId) {
        delete(ErpSalePriceAdjustItemDO::getAdjustId, adjustId);
    }

}
