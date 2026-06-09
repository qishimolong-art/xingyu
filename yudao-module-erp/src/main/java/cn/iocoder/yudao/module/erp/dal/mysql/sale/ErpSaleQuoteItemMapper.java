package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 报价订单项 Mapper
 */
@Mapper
public interface ErpSaleQuoteItemMapper extends BaseMapperX<ErpSaleQuoteItemDO> {

    default List<ErpSaleQuoteItemDO> selectListByQuoteId(Long quoteId) {
        return selectList(ErpSaleQuoteItemDO::getQuoteId, quoteId);
    }

    default List<ErpSaleQuoteItemDO> selectListByQuoteIds(Collection<Long> quoteIds) {
        return selectList(ErpSaleQuoteItemDO::getQuoteId, quoteIds);
    }

    default int deleteByQuoteId(Long quoteId) {
        return delete(ErpSaleQuoteItemDO::getQuoteId, quoteId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleQuoteItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpSaleQuoteItemDO::getWarehouseId, warehouseId);
    }

}
