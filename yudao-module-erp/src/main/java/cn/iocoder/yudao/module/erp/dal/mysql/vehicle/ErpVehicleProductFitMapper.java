package cn.iocoder.yudao.module.erp.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleProductFitDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 车型配件适配 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpVehicleProductFitMapper extends BaseMapperX<ErpVehicleProductFitDO> {

    default List<ErpVehicleProductFitDO> selectListByVehicleModelId(Long vehicleModelId) {
        return selectList(ErpVehicleProductFitDO::getVehicleModelId, vehicleModelId);
    }

    default List<ErpVehicleProductFitDO> selectListByProductId(Long productId) {
        return selectList(ErpVehicleProductFitDO::getProductId, productId);
    }

    default ErpVehicleProductFitDO selectByModelAndProduct(Long vehicleModelId, Long productId) {
        return selectOne(new LambdaQueryWrapperX<ErpVehicleProductFitDO>()
                .eq(ErpVehicleProductFitDO::getVehicleModelId, vehicleModelId)
                .eq(ErpVehicleProductFitDO::getProductId, productId));
    }

}
