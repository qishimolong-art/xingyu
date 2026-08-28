package cn.iocoder.yudao.module.erp.controller.admin.mall;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.mall.vo.ErpMallCategorySyncStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.mall.vo.ErpMallProductSyncStatusRespVO;
import cn.iocoder.yudao.module.erp.service.mall.ErpMallProductSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 同步商城商品")
@RestController
@RequestMapping("/erp/mall-sync")
@Validated
public class ErpMallProductSyncController {

    @Resource
    private ErpMallProductSyncService mallProductSyncService;

    @PostMapping("/category/all")
    @Operation(summary = "同步全部 ERP 分类到商城")
    @PreAuthorize("@ss.hasPermission('erp:mall-sync:category')")
    public CommonResult<Integer> syncAllCategories() {
        return success(mallProductSyncService.syncAllCategories());
    }

    @PostMapping("/category")
    @Operation(summary = "同步指定 ERP 分类到商城")
    @Parameter(name = "id", description = "ERP 分类编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:mall-sync:category')")
    public CommonResult<Boolean> syncCategory(@RequestParam("id") Long id) {
        mallProductSyncService.syncCategory(id);
        return success(true);
    }

    @PostMapping("/product/all")
    @Operation(summary = "同步全部 ERP 产品到商城")
    @PreAuthorize("@ss.hasPermission('erp:mall-sync:product')")
    public CommonResult<Integer> syncAllProducts() {
        return success(mallProductSyncService.syncAllProducts());
    }

    @PostMapping("/product")
    @Operation(summary = "同步指定 ERP 产品到商城")
    @Parameter(name = "id", description = "ERP 产品编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:mall-sync:product')")
    public CommonResult<Boolean> syncProduct(@RequestParam("id") Long id) {
        mallProductSyncService.syncProduct(id);
        return success(true);
    }

    @GetMapping("/category-status")
    @Operation(summary = "查询 ERP 分类同步商城状态")
    @PreAuthorize("@ss.hasPermission('erp:mall-sync:category')")
    public CommonResult<List<ErpMallCategorySyncStatusRespVO>> getCategorySyncStatus(
            @RequestParam(value = "syncStatus", required = false) Integer syncStatus) {
        return success(mallProductSyncService.getCategorySyncStatus(syncStatus));
    }

    @GetMapping("/product-status")
    @Operation(summary = "查询 ERP 产品同步商城状态")
    @PreAuthorize("@ss.hasPermission('erp:mall-sync:product')")
    public CommonResult<List<ErpMallProductSyncStatusRespVO>> getProductSyncStatus(
            @RequestParam(value = "syncStatus", required = false) Integer syncStatus) {
        return success(mallProductSyncService.getProductSyncStatus(syncStatus));
    }

}
