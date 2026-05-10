package cn.iocoder.yudao.module.erp.controller.admin.vehicle;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleBrandDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleModelDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleProductFitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.vehicle.ErpVehicleSeriesDO;
import cn.iocoder.yudao.module.erp.service.vehicle.ErpVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 车型适配")
@RestController
@RequestMapping("/erp/vehicle")
@Validated
public class ErpVehicleController {

    @Resource
    private ErpVehicleService vehicleService;

    // ========== 品牌 ==========

    @PostMapping("/brand/create")
    @Operation(summary = "创建车型品牌")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:create')")
    public CommonResult<Long> createBrand(@Valid @RequestBody ErpVehicleBrandDO brand) {
        return success(vehicleService.createBrand(brand));
    }

    @PutMapping("/brand/update")
    @Operation(summary = "更新车型品牌")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:update')")
    public CommonResult<Boolean> updateBrand(@Valid @RequestBody ErpVehicleBrandDO brand) {
        vehicleService.updateBrand(brand);
        return success(true);
    }

    @DeleteMapping("/brand/delete")
    @Operation(summary = "删除车型品牌")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:delete')")
    public CommonResult<Boolean> deleteBrand(@RequestParam("id") Long id) {
        vehicleService.deleteBrand(id);
        return success(true);
    }

    @GetMapping("/brand/list")
    @Operation(summary = "获得车型品牌列表")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:query')")
    public CommonResult<List<ErpVehicleBrandDO>> getBrandList(@RequestParam(value = "status", required = false) Integer status) {
        return success(vehicleService.getBrandList(status));
    }

    // ========== 车系 ==========

    @PostMapping("/series/create")
    @Operation(summary = "创建车系")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:create')")
    public CommonResult<Long> createSeries(@Valid @RequestBody ErpVehicleSeriesDO series) {
        return success(vehicleService.createSeries(series));
    }

    @PutMapping("/series/update")
    @Operation(summary = "更新车系")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:update')")
    public CommonResult<Boolean> updateSeries(@Valid @RequestBody ErpVehicleSeriesDO series) {
        vehicleService.updateSeries(series);
        return success(true);
    }

    @DeleteMapping("/series/delete")
    @Operation(summary = "删除车系")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:delete')")
    public CommonResult<Boolean> deleteSeries(@RequestParam("id") Long id) {
        vehicleService.deleteSeries(id);
        return success(true);
    }

    @GetMapping("/series/list")
    @Operation(summary = "获得车系列表")
    @Parameter(name = "brandId", description = "品牌编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:query')")
    public CommonResult<List<ErpVehicleSeriesDO>> getSeriesList(@RequestParam("brandId") Long brandId) {
        return success(vehicleService.getSeriesListByBrandId(brandId));
    }

    // ========== 车型 ==========

    @PostMapping("/model/create")
    @Operation(summary = "创建车型")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:create')")
    public CommonResult<Long> createModel(@Valid @RequestBody ErpVehicleModelDO model) {
        return success(vehicleService.createModel(model));
    }

    @PutMapping("/model/update")
    @Operation(summary = "更新车型")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:update')")
    public CommonResult<Boolean> updateModel(@Valid @RequestBody ErpVehicleModelDO model) {
        vehicleService.updateModel(model);
        return success(true);
    }

    @DeleteMapping("/model/delete")
    @Operation(summary = "删除车型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:delete')")
    public CommonResult<Boolean> deleteModel(@RequestParam("id") Long id) {
        vehicleService.deleteModel(id);
        return success(true);
    }

    @GetMapping("/model/list")
    @Operation(summary = "获得车型列表")
    @Parameter(name = "seriesId", description = "车系编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:query')")
    public CommonResult<List<ErpVehicleModelDO>> getModelList(@RequestParam("seriesId") Long seriesId) {
        return success(vehicleService.getModelListBySeriesId(seriesId));
    }

    // ========== 配件适配 ==========

    @PostMapping("/product-fit/create")
    @Operation(summary = "创建车型配件适配")
    @PreAuthorize("@ss.hasPermission('erp:vehicle:create')")
    public CommonResult<Long> createProductFit(@Valid @RequestBody ErpVehicleProductFitDO fit) {
        return success(vehicleService.createProductFit(fit));
    }

    @DeleteMapping("/product-fit/delete")
    @Operation(summary = "删除车型配件适配")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:delete')")
    public CommonResult<Boolean> deleteProductFit(@RequestParam("id") Long id) {
        vehicleService.deleteProductFit(id);
        return success(true);
    }

    @GetMapping("/product-fit/list-by-model")
    @Operation(summary = "获得车型下的配件列表")
    @Parameter(name = "vehicleModelId", description = "车型编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:query')")
    public CommonResult<List<ErpVehicleProductFitDO>> getProductFitListByModel(@RequestParam("vehicleModelId") Long vehicleModelId) {
        return success(vehicleService.getProductFitListByModelId(vehicleModelId));
    }

    @GetMapping("/product-fit/list-by-product")
    @Operation(summary = "获得产品适配的车型列表")
    @Parameter(name = "productId", description = "产品编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vehicle:query')")
    public CommonResult<List<ErpVehicleProductFitDO>> getProductFitListByProduct(@RequestParam("productId") Long productId) {
        return success(vehicleService.getProductFitListByProductId(productId));
    }

}
