package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLockDO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 库存占用 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpStockLockMapper extends BaseMapperX<ErpStockLockDO> {

    default List<ErpStockLockDO> selectListByBiz(Integer bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<ErpStockLockDO>()
                .eq(ErpStockLockDO::getBizType, bizType)
                .eq(ErpStockLockDO::getBizId, bizId)
                .eq(ErpStockLockDO::getStatus, 1));
    }

    default List<ErpStockLockDO> selectListByProductAndWarehouse(Long productId, Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<ErpStockLockDO>()
                .eq(ErpStockLockDO::getProductId, productId)
                .eq(ErpStockLockDO::getWarehouseId, warehouseId)
                .eq(ErpStockLockDO::getStatus, 1));
    }

}
