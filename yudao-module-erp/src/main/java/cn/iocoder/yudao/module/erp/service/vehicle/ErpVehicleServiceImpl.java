package cn.iocoder.yudao.module.erp.service.vehicle;

import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleBrandDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleModelDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleProductFitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleSeriesDO;
import cn.iocoder.yudao.module.erp.dal.mysql.vehicle.ErpVehicleBrandMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.vehicle.ErpVehicleModelMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.vehicle.ErpVehicleProductFitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.vehicle.ErpVehicleSeriesMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 车型适配 Service 实现类
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpVehicleServiceImpl implements ErpVehicleService {

    @Resource
    private ErpVehicleBrandMapper vehicleBrandMapper;
    @Resource
    private ErpVehicleSeriesMapper vehicleSeriesMapper;
    @Resource
    private ErpVehicleModelMapper vehicleModelMapper;
    @Resource
    private ErpVehicleProductFitMapper vehicleProductFitMapper;

    // ========== 品牌 ==========

    @Override
    public Long createBrand(ErpVehicleBrandDO brand) {
        vehicleBrandMapper.insert(brand);
        return brand.getId();
    }

    @Override
    public void updateBrand(ErpVehicleBrandDO brand) {
        validateBrandExists(brand.getId());
        vehicleBrandMapper.updateById(brand);
    }

    @Override
    public void deleteBrand(Long id) {
        validateBrandExists(id);
        vehicleBrandMapper.deleteById(id);
    }

    @Override
    public ErpVehicleBrandDO getBrand(Long id) {
        return vehicleBrandMapper.selectById(id);
    }

    @Override
    public List<ErpVehicleBrandDO> getBrandList(Integer status) {
        return vehicleBrandMapper.selectListByStatus(status);
    }

    private void validateBrandExists(Long id) {
        if (vehicleBrandMapper.selectById(id) == null) {
            throw exception(VEHICLE_BRAND_NOT_EXISTS);
        }
    }

    // ========== 车系 ==========

    @Override
    public Long createSeries(ErpVehicleSeriesDO series) {
        vehicleSeriesMapper.insert(series);
        return series.getId();
    }

    @Override
    public void updateSeries(ErpVehicleSeriesDO series) {
        validateSeriesExists(series.getId());
        vehicleSeriesMapper.updateById(series);
    }

    @Override
    public void deleteSeries(Long id) {
        validateSeriesExists(id);
        vehicleSeriesMapper.deleteById(id);
    }

    @Override
    public ErpVehicleSeriesDO getSeries(Long id) {
        return vehicleSeriesMapper.selectById(id);
    }

    @Override
    public List<ErpVehicleSeriesDO> getSeriesListByBrandId(Long brandId) {
        return vehicleSeriesMapper.selectListByBrandId(brandId);
    }

    private void validateSeriesExists(Long id) {
        if (vehicleSeriesMapper.selectById(id) == null) {
            throw exception(VEHICLE_SERIES_NOT_EXISTS);
        }
    }

    // ========== 车型 ==========

    @Override
    public Long createModel(ErpVehicleModelDO model) {
        vehicleModelMapper.insert(model);
        return model.getId();
    }

    @Override
    public void updateModel(ErpVehicleModelDO model) {
        validateModelExists(model.getId());
        vehicleModelMapper.updateById(model);
    }

    @Override
    public void deleteModel(Long id) {
        validateModelExists(id);
        vehicleModelMapper.deleteById(id);
    }

    @Override
    public ErpVehicleModelDO getModel(Long id) {
        return vehicleModelMapper.selectById(id);
    }

    @Override
    public List<ErpVehicleModelDO> getModelListBySeriesId(Long seriesId) {
        return vehicleModelMapper.selectListBySeriesId(seriesId);
    }

    private void validateModelExists(Long id) {
        if (vehicleModelMapper.selectById(id) == null) {
            throw exception(VEHICLE_MODEL_NOT_EXISTS);
        }
    }

    // ========== 配件适配 ==========

    @Override
    public Long createProductFit(ErpVehicleProductFitDO fit) {
        // 校验是否已存在
        ErpVehicleProductFitDO existFit = vehicleProductFitMapper.selectByModelAndProduct(
                fit.getVehicleModelId(), fit.getProductId());
        if (existFit != null) {
            throw exception(VEHICLE_PRODUCT_FIT_EXISTS);
        }
        vehicleProductFitMapper.insert(fit);
        return fit.getId();
    }

    @Override
    public void deleteProductFit(Long id) {
        if (vehicleProductFitMapper.selectById(id) == null) {
            throw exception(VEHICLE_PRODUCT_FIT_NOT_EXISTS);
        }
        vehicleProductFitMapper.deleteById(id);
    }

    @Override
    public List<ErpVehicleProductFitDO> getProductFitListByModelId(Long vehicleModelId) {
        return vehicleProductFitMapper.selectListByVehicleModelId(vehicleModelId);
    }

    @Override
    public List<ErpVehicleProductFitDO> getProductFitListByProductId(Long productId) {
        return vehicleProductFitMapper.selectListByProductId(productId);
    }

}
