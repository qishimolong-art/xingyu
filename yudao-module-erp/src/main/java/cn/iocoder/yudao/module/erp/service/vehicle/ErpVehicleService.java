package cn.iocoder.yudao.module.erp.service.vehicle;

import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleBrandDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleModelDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleProductFitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleSeriesDO;

import java.util.List;

/**
 * ERP 车型适配 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpVehicleService {

    // ========== 品牌 ==========
    Long createBrand(ErpVehicleBrandDO brand);
    void updateBrand(ErpVehicleBrandDO brand);
    void deleteBrand(Long id);
    ErpVehicleBrandDO getBrand(Long id);
    List<ErpVehicleBrandDO> getBrandList(Integer status);

    // ========== 车系 ==========
    Long createSeries(ErpVehicleSeriesDO series);
    void updateSeries(ErpVehicleSeriesDO series);
    void deleteSeries(Long id);
    ErpVehicleSeriesDO getSeries(Long id);
    List<ErpVehicleSeriesDO> getSeriesListByBrandId(Long brandId);

    // ========== 车型 ==========
    Long createModel(ErpVehicleModelDO model);
    void updateModel(ErpVehicleModelDO model);
    void deleteModel(Long id);
    ErpVehicleModelDO getModel(Long id);
    List<ErpVehicleModelDO> getModelListBySeriesId(Long seriesId);

    // ========== 配件适配 ==========
    Long createProductFit(ErpVehicleProductFitDO fit);
    void deleteProductFit(Long id);
    List<ErpVehicleProductFitDO> getProductFitListByModelId(Long vehicleModelId);
    List<ErpVehicleProductFitDO> getProductFitListByProductId(Long productId);

}
