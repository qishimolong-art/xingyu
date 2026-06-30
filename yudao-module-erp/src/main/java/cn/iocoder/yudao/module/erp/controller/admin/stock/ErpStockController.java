package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductPriceSystemService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 产品库存")
@RestController
@RequestMapping("/erp/stock")
@Validated
public class ErpStockController {

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpProductPriceSystemService productPriceSystemService;
    @Resource
    private DeptApi deptApi;

    @GetMapping("/get")
    @Operation(summary = "获得产品库存")
    @Parameters({
            @Parameter(name = "id", description = "编号", example = "1"), // 方案一：传递 id
            @Parameter(name = "productId", description = "产品编号", example = "10"), // 方案二：传递 productId + warehouseId
            @Parameter(name = "warehouseId", description = "仓库编号", example = "2")
    })
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<ErpStockRespVO> getStock(@RequestParam(value = "id", required = false) Long id,
                                                 @RequestParam(value = "productId", required = false) Long productId,
                                                 @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        ErpStockDO stock = id != null ? stockService.getStock(id) : stockService.getStock(productId, warehouseId);
        if (stock == null) {
            return success(null);
        }
        return success(buildStockVOPageResult(new PageResult<>(Collections.singletonList(stock), 1L), null).getList().get(0));
    }

    @GetMapping("/get-count")
    @Operation(summary = "获得产品库存数量")
    @Parameters({
            @Parameter(name = "productId", description = "产品编号", example = "10"),
            @Parameter(name = "warehouseId", description = "仓库编号", example = "2")
    })
    public CommonResult<BigDecimal> getStockCount(@RequestParam("productId") Long productId,
                                                  @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        return success(warehouseId != null ? stockService.getStockCount(productId, warehouseId)
                : stockService.getStockCount(productId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得产品库存分页")
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<PageResult<ErpStockRespVO>> getStockPage(@Valid ErpStockPageReqVO pageReqVO) {
        PageResult<ErpStockDO> pageResult = stockService.getStockPage(pageReqVO);
        return success(buildStockVOPageResult(pageResult, pageReqVO.getPriceSystemId()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品库存 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockExcel(@Valid ErpStockPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockRespVO> list = buildStockVOPageResult(stockService.getStockPage(pageReqVO),
                pageReqVO.getPriceSystemId()).getList();
        // 导出 Excel
        ExcelUtils.write(response, "产品库存.xls", "数据", ErpStockRespVO.class, list);
    }

    @PutMapping("/adjust")
    @Operation(summary = "手动调整库存数量（盘点调整）")
    @PreAuthorize("@ss.hasPermission('erp:stock:adjust')")
    public CommonResult<BigDecimal> adjustStock(@Valid @RequestBody ErpStockAdjustReqVO reqVO) {
        return success(stockService.adjustStock(reqVO));
    }

    private PageResult<ErpStockRespVO> buildStockVOPageResult(PageResult<ErpStockDO> pageResult, Long priceSystemId) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> productIds = convertSet(pageResult.getList(), ErpStockDO::getProductId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(
                () -> productService.getProductVOMap(productIds));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(
                () -> warehouseService.getWarehouseMap(convertSet(pageResult.getList(), ErpStockDO::getWarehouseId)));
        // 聚合数据：占用数 / 未入数
        Map<Long, BigDecimal> occupiedMap = saleOrderItemMapper.selectOccupiedCountMap(productIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        Map<Long, BigDecimal> pendingInMap = purchaseOrderItemMapper.selectPendingInCountMap(productIds);
        // 价格体系
        Map<Long, BigDecimal> priceMap = priceSystemId != null
                ? productPriceSystemService.getProductPriceMap(productIds, priceSystemId)
                : Collections.emptyMap();

        return BeanUtils.toBean(pageResult, ErpStockRespVO.class, stock -> {
            MapUtils.findAndThen(productMap, stock.getProductId(), product -> {
                stock.setProductName(product.getName())
                        .setCategoryName(product.getCategoryName())
                        .setUnitId(product.getUnitId())
                        .setUnitName(product.getUnitName());
                // 扩展字段
                stock.setProductCode(product.getCode())
                        .setDrawingNo(product.getDrawingNo())
                        .setStandard(product.getStandard())
                        .setFeatureCode(product.getFeatureCode())
                        .setVehicleModel(product.getVehicleModel())
                        .setBrand(product.getBrand())
                        .setOriginPlace(product.getOriginPlace())
                        .setShelf(product.getShelf())
                        .setCategoryId(product.getCategoryId())
                        .setLastPurchasePrice(product.getLastPurchasePrice())
                        .setOeNumber(product.getOeNumber())
                        .setFactoryCode(product.getFactoryCode())
                        .setProductBarCode(product.getBarCode())
                        .setSalePrice(product.getSalePrice())
                        .setReferencePrice(product.getReferencePrice())
                        .setRetailPrice(product.getRetailPrice())
                        .setBackupPrice1(product.getBackupPrice1())
                        .setWholesalePrice(product.getWholesalePrice())
                        .setStockMax(product.getStockMax())
                        .setStockMin(product.getStockMin())
                        .setStockStandard(product.getStockStandard())
                        .setPackageQty(product.getPackageQty())
                        .setBatchNoEnabled(product.getBatchNoEnabled())
                        .setWeight(product.getWeight());
            });
            MapUtils.findAndThen(warehouseMap, stock.getWarehouseId(), warehouse -> {
                stock.setWarehouseName(warehouse.getName()).setDeptId(warehouse.getDeptId());
                MapUtils.findAndThen(deptMap, warehouse.getDeptId(), dept -> stock.setDeptName(dept.getName()));
            });
            // 聚合字段
            BigDecimal occupied = occupiedMap.getOrDefault(stock.getProductId(), BigDecimal.ZERO);
            BigDecimal pending = pendingInMap.getOrDefault(stock.getProductId(), BigDecimal.ZERO);
            stock.setOccupiedCount(occupied)
                    .setPendingInCount(pending)
                    .setInTransitCount(pending);
            // 价格体系
            if (priceSystemId != null) {
                BigDecimal unitPrice = priceMap.get(stock.getProductId());
                stock.setCurrentPrice(unitPrice);
                if (unitPrice != null && stock.getCount() != null) {
                    stock.setCurrentPriceAmount(unitPrice.multiply(stock.getCount()));
                }
            }
        });
    }

}
