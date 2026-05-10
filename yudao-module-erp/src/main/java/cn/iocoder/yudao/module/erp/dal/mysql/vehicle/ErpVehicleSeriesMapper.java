package cn.iocoder.yudao.module.erp.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleSeriesDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 车系 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpVehicleSeriesMapper extends BaseMapperX<ErpVehicleSeriesDO> {

    default List<ErpVehicleSeriesDO> selectListByBrandId(Long brandId) {
        return selectList(new LambdaQueryWrapperX<ErpVehicleSeriesDO>()
                .eq(ErpVehicleSeriesDO::getBrandId, brandId)
                .orderByAsc(ErpVehicleSeriesDO::getSort));
    }

}
