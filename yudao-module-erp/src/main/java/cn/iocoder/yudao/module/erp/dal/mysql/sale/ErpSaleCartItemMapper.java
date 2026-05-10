package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 销售手推车项 Mapper
 */
@Mapper
public interface ErpSaleCartItemMapper extends BaseMapperX<ErpSaleCartItemDO> {

    default List<ErpSaleCartItemDO> selectListByCartId(Long cartId) {
        return selectList(ErpSaleCartItemDO::getCartId, cartId);
    }

    default List<ErpSaleCartItemDO> selectListByCartIds(Collection<Long> cartIds) {
        return selectList(ErpSaleCartItemDO::getCartId, cartIds);
    }

    default int deleteByCartId(Long cartId) {
        return delete(ErpSaleCartItemDO::getCartId, cartId);
    }

}
