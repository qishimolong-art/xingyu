package cn.iocoder.yudao.module.erp.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleModelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 车型 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpVehicleModelMapper extends BaseMapperX<ErpVehicleModelDO> {

    default List<ErpVehicleModelDO> selectListBySeriesId(Long seriesId) {
        return selectList(new LambdaQueryWrapperX<ErpVehicleModelDO>()
                .eq(ErpVehicleModelDO::getSeriesId, seriesId)
                .orderByAsc(ErpVehicleModelDO::getSort));
    }

}
