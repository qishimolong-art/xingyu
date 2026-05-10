package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseBranchDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 仓库分店关联 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpWarehouseBranchMapper extends BaseMapperX<ErpWarehouseBranchDO> {

    default List<ErpWarehouseBranchDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(ErpWarehouseBranchDO::getWarehouseId, warehouseId);
    }

    default void deleteByWarehouseId(Long warehouseId) {
        delete(ErpWarehouseBranchDO::getWarehouseId, warehouseId);
    }

}
