package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.MapUtils.findAndThen;

@Tag(name = "管理后台 - ERP 采购调价单")
@RestController
@RequestMapping("/erp/purchase-price-adjust")
@Validated
public class ErpPurchasePriceAdjustController {

    @Resource
    private ErpPurchasePriceAdjustService purchasePriceAdjustService;
    @Resource
    private ErpSupplierService supplierService;

    @PostMapping("/create")
    @Operation(summary = "创建采购调价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create')")
    public CommonResult<Long> createPurchasePriceAdjust(@Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO createReqVO) {
        return success(purchasePriceAdjustService.createPurchasePriceAdjust(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购调价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update')")
    public CommonResult<Boolean> updatePurchasePriceAdjust(@Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO updateReqVO) {
        purchasePriceAdjustService.updatePurchasePriceAdjust(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购调价单状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update-status')")
    public CommonResult<Boolean> updatePurchasePriceAdjustStatus(@RequestParam("id") Long id,
                                                                  @RequestParam("status") Integer status) {
        purchasePriceAdjustService.updatePurchasePriceAdjustStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购调价单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:delete')")
    public CommonResult<Boolean> deletePurchasePriceAdjust(@RequestParam("ids") List<Long> ids) {
        purchasePriceAdjustService.deletePurchasePriceAdjust(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购调价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<ErpPurchasePriceAdjustRespVO> getPurchasePriceAdjust(@RequestParam("id") Long id) {
        ErpPurchasePriceAdjustDO adjust = purchasePriceAdjustService.getPurchasePriceAdjust(id);
        if (adjust == null) {
            return success(null);
        }
        List<ErpPurchasePriceAdjustItemDO> items = purchasePriceAdjustService.getPurchasePriceAdjustItemListByAdjustId(id);
        ErpPurchasePriceAdjustRespVO respVO = BeanUtils.toBean(adjust, ErpPurchasePriceAdjustRespVO.class);
        respVO.setItems(BeanUtils.toBean(items, ErpPurchasePriceAdjustRespVO.Item.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购调价单分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<PageResult<ErpPurchasePriceAdjustRespVO>> getPurchasePriceAdjustPage(@Valid ErpPurchasePriceAdjustPageReqVO pageReqVO) {
        PageResult<ErpPurchasePriceAdjustDO> pageResult = purchasePriceAdjustService.getPurchasePriceAdjustPage(pageReqVO);
        return success(buildPriceAdjustVOPageResult(pageResult));
    }

    private PageResult<ErpPurchasePriceAdjustRespVO> buildPriceAdjustVOPageResult(PageResult<ErpPurchasePriceAdjustDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1. 调价明细
        List<ErpPurchasePriceAdjustItemDO> itemList = purchasePriceAdjustService.getPurchasePriceAdjustItemListByAdjustIds(
                convertSet(pageResult.getList(), ErpPurchasePriceAdjustDO::getId));
        Map<Long, List<ErpPurchasePriceAdjustItemDO>> itemMap = convertMultiMap(itemList, ErpPurchasePriceAdjustItemDO::getAdjustId);
        // 2. 供应商信息
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchasePriceAdjustDO::getSupplierId));
        // 3. 拼接
        return BeanUtils.toBean(pageResult, ErpPurchasePriceAdjustRespVO.class, adjust -> {
            adjust.setItems(BeanUtils.toBean(itemMap.get(adjust.getId()), ErpPurchasePriceAdjustRespVO.Item.class));
            findAndThen(supplierMap, adjust.getSupplierId(), supplier -> adjust.setSupplierName(supplier.getName()));
        });
    }

}
