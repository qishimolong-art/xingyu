package cn.iocoder.yudao.module.erp.controller.admin.chain;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderItemDO;
import cn.iocoder.yudao.module.erp.service.chain.ErpChainOrderService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
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

@Tag(name = "管理后台 - ERP 连锁开单")
@RestController
@RequestMapping("/erp/chain-order")
@Validated
public class ErpChainOrderController {

    @Resource
    private ErpChainOrderService chainOrderService;
    @Resource
    private ErpProductService productService;

    @PostMapping("/create")
    @Operation(summary = "创建连锁开单")
    @PreAuthorize("@ss.hasPermission('erp:chain-order:create')")
    public CommonResult<Long> createChainOrder(@Valid @RequestBody ErpChainOrderSaveReqVO createReqVO) {
        return success(chainOrderService.createChainOrder(createReqVO));
    }

    @PutMapping("/approve")
    @Operation(summary = "审核连锁开单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:chain-order:approve')")
    public CommonResult<Boolean> approveChainOrder(@RequestParam("id") Long id) {
        chainOrderService.approveChainOrder(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消连锁开单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:chain-order:cancel')")
    public CommonResult<Boolean> cancelChainOrder(@RequestParam("id") Long id) {
        chainOrderService.cancelChainOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得连锁开单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:chain-order:query')")
    public CommonResult<ErpChainOrderRespVO> getChainOrder(@RequestParam("id") Long id) {
        ErpChainOrderDO chainOrder = chainOrderService.getChainOrder(id);
        if (chainOrder == null) {
            return success(null);
        }
        List<ErpChainOrderItemDO> items = chainOrderService.getChainOrderItemListByOrderId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(items, ErpChainOrderItemDO::getProductId));
        ErpChainOrderRespVO respVO = BeanUtils.toBean(chainOrder, ErpChainOrderRespVO.class);
        respVO.setItems(BeanUtils.toBean(items, ErpChainOrderRespVO.Item.class, item ->
                findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得连锁开单分页")
    @PreAuthorize("@ss.hasPermission('erp:chain-order:query')")
    public CommonResult<PageResult<ErpChainOrderRespVO>> getChainOrderPage(@Valid ErpChainOrderPageReqVO pageReqVO) {
        PageResult<ErpChainOrderDO> pageResult = chainOrderService.getChainOrderPage(pageReqVO);
        return success(buildChainOrderVOPageResult(pageResult));
    }

    private PageResult<ErpChainOrderRespVO> buildChainOrderVOPageResult(PageResult<ErpChainOrderDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1. 明细
        List<ErpChainOrderItemDO> itemList = chainOrderService.getChainOrderItemListByOrderIds(
                convertSet(pageResult.getList(), ErpChainOrderDO::getId));
        Map<Long, List<ErpChainOrderItemDO>> itemMap = convertMultiMap(itemList, ErpChainOrderItemDO::getChainOrderId);
        // 2. 产品信息
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(itemList, ErpChainOrderItemDO::getProductId));
        // 3. 拼接
        return BeanUtils.toBean(pageResult, ErpChainOrderRespVO.class, order -> {
            order.setItems(BeanUtils.toBean(itemMap.get(order.getId()), ErpChainOrderRespVO.Item.class, item ->
                    findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
        });
    }

}
