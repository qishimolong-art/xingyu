package cn.iocoder.yudao.module.product.controller.app.spu;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.stock.ErpMallStockApi;
import cn.iocoder.yudao.module.product.controller.app.spu.vo.AppProductSpuDetailRespVO;
import cn.iocoder.yudao.module.product.controller.app.spu.vo.AppProductSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.app.spu.vo.AppProductSpuRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.brand.ProductBrandDO;
import cn.iocoder.yudao.module.product.dal.dataobject.category.ProductCategoryDO;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.enums.spu.ProductSpuStatusEnum;
import cn.iocoder.yudao.module.product.service.brand.ProductBrandService;
import cn.iocoder.yudao.module.product.service.category.ProductCategoryService;
import cn.iocoder.yudao.module.product.service.history.ProductBrowseHistoryService;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SPU_NOT_ENABLE;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SPU_NOT_EXISTS;

@Tag(name = "用户 APP - 商品 SPU")
@RestController
@RequestMapping("/product/spu")
@Validated
public class AppProductSpuController {

    @Resource
    private ProductSpuService productSpuService;
    @Resource
    private ProductSkuService productSkuService;
    @Resource
    private ProductBrowseHistoryService productBrowseHistoryService;
    @Resource
    private ProductCategoryService productCategoryService;
    @Resource
    private ProductBrandService productBrandService;
    @Resource
    private ErpCustomerMemberApi customerMemberApi;
    @Resource
    private ErpMallStockApi mallStockApi;

    @GetMapping("/list-by-ids")
    @Operation(summary = "获得商品 SPU 列表")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<List<AppProductSpuRespVO>> getSpuList(@RequestParam("ids") Set<Long> ids) {
        List<ProductSpuDO> list = productSpuService.getSpuList(ids);
        if (CollUtil.isEmpty(list)) {
            return success(Collections.emptyList());
        }

        // 拼接返回
        list.forEach(spu -> spu.setSalesCount(spu.getSalesCount() + spu.getVirtualSalesCount()));
        List<AppProductSpuRespVO> voList = BeanUtils.toBean(list, AppProductSpuRespVO.class);
        fillErpAvailableStock(voList);
        fillPriceVisibility(voList, isPriceVisible());
        return success(voList);
    }

    @GetMapping("/page")
    @Operation(summary = "获得商品 SPU 分页")
    public CommonResult<PageResult<AppProductSpuRespVO>> getSpuPage(@Valid AppProductSpuPageReqVO pageVO) {
        boolean priceVisible = isPriceVisible();
        if (!priceVisible && AppProductSpuPageReqVO.SORT_FIELD_PRICE.equals(pageVO.getSortField())) {
            pageVO.setSortField(null);
            pageVO.setSortAsc(null);
        }
        PageResult<ProductSpuDO> pageResult = productSpuService.getSpuPage(pageVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }

        // 拼接返回
        pageResult.getList().forEach(spu -> spu.setSalesCount(spu.getSalesCount() + spu.getVirtualSalesCount()));
        PageResult<AppProductSpuRespVO> voPageResult = BeanUtils.toBean(pageResult, AppProductSpuRespVO.class);
        fillErpAvailableStock(voPageResult.getList());
        fillPriceVisibility(voPageResult.getList(), priceVisible);
        return success(voPageResult);
    }

    @GetMapping("/vehicle-models")
    @Operation(summary = "获得商品 SPU 车型筛选项列表")
    public CommonResult<List<String>> getVehicleModelList(@Valid AppProductSpuPageReqVO pageVO) {
        return success(productSpuService.getAppVehicleModelList(pageVO));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得商品 SPU 明细")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<AppProductSpuDetailRespVO> getSpuDetail(@RequestParam("id") Long id) {
        // 获得商品 SPU
        ProductSpuDO spu = productSpuService.getSpu(id);
        if (spu == null) {
            throw exception(SPU_NOT_EXISTS);
        }
        if (!ProductSpuStatusEnum.isEnable(spu.getStatus())) {
            throw exception(SPU_NOT_ENABLE, spu.getName());
        }
        // 获得商品 SKU
        List<ProductSkuDO> skus = productSkuService.getSkuListBySpuId(spu.getId());

        // 增加浏览量
        productSpuService.updateBrowseCount(id, 1);
        // 保存浏览记录
        productBrowseHistoryService.createBrowseHistory(getLoginUserId(), id);

        // 拼接返回
        spu.setSalesCount(spu.getSalesCount() + spu.getVirtualSalesCount());
        AppProductSpuDetailRespVO spuVO = BeanUtils.toBean(spu, AppProductSpuDetailRespVO.class)
                .setSkus(BeanUtils.toBean(skus, AppProductSpuDetailRespVO.Sku.class));
        fillErpAvailableStock(spuVO);
        fillPartParamNames(spuVO, spu);
        fillPriceVisibility(spuVO, isPriceVisible());
        return success(spuVO);
    }

    private boolean isPriceVisible() {
        return customerMemberApi.isCustomerMemberAuthorized(getLoginUserId());
    }

    private void fillErpAvailableStock(List<AppProductSpuRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, BigDecimal> stockMap = mallStockApi.getMallSpuAvailableStockMap(
                list.stream().map(AppProductSpuRespVO::getId).collect(Collectors.toSet()));
        list.forEach(spu -> spu.setStock(toStockInteger(stockMap.get(spu.getId()))));
    }

    private void fillErpAvailableStock(AppProductSpuDetailRespVO spu) {
        Map<Long, BigDecimal> spuStockMap = mallStockApi.getMallSpuAvailableStockMap(Collections.singleton(spu.getId()));
        spu.setStock(toStockInteger(spuStockMap.get(spu.getId())));
        if (CollUtil.isEmpty(spu.getSkus())) {
            return;
        }
        Map<Long, BigDecimal> skuStockMap = mallStockApi.getMallSkuAvailableStockMap(spu.getId(),
                spu.getSkus().stream().map(AppProductSpuDetailRespVO.Sku::getId).collect(Collectors.toSet()));
        spu.getSkus().forEach(sku -> sku.setStock(toStockInteger(skuStockMap.get(sku.getId()))));
    }

    private Integer toStockInteger(BigDecimal stock) {
        if (stock == null || stock.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal normalized = stock.setScale(0, RoundingMode.DOWN);
        if (normalized.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return normalized.intValue();
    }

    private void fillPriceVisibility(List<AppProductSpuRespVO> list, boolean priceVisible) {
        list.forEach(spu -> fillPriceVisibility(spu, priceVisible));
    }

    private void fillPriceVisibility(AppProductSpuRespVO spu, boolean priceVisible) {
        spu.setPriceVisible(priceVisible);
        if (!priceVisible) {
            spu.setPrice(null).setMarketPrice(null);
        }
    }

    private void fillPriceVisibility(AppProductSpuDetailRespVO spu, boolean priceVisible) {
        spu.setPriceVisible(priceVisible);
        if (priceVisible) {
            return;
        }
        spu.setPrice(null).setMarketPrice(null);
        if (spu.getSkus() != null) {
            spu.getSkus().forEach(sku -> sku.setPrice(null).setMarketPrice(null).setVipPrice(null));
        }
    }

    private void fillPartParamNames(AppProductSpuDetailRespVO spuVO, ProductSpuDO spu) {
        ProductCategoryDO category = spu.getCategoryId() == null ? null : productCategoryService.getCategory(spu.getCategoryId());
        if (category != null) {
            spuVO.setCategoryName(category.getName());
        }
        ProductBrandDO brand = spu.getBrandId() == null ? null : productBrandService.getBrand(spu.getBrandId());
        if (brand != null) {
            spuVO.setBrandName(brand.getName());
        }
    }

}
