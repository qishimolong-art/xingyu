package cn.iocoder.yudao.module.erp.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleBrandDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 车型品牌 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpVehicleBrandMapper extends BaseMapperX<ErpVehicleBrandDO> {

    default List<ErpVehicleBrandDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpVehicleBrandDO>()
                .eqIfPresent(ErpVehicleBrandDO::getStatus, status)
                .orderByAsc(ErpVehicleBrandDO::getSort));
    }

}
