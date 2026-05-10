package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "管理后台 - ERP 销售手推车")
@RestController
@RequestMapping("/erp/sale-cart")
@Validated
public class ErpSaleCartController {

    @Resource
    private ErpSaleCartService saleCartService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;

    @PostMapping("/create")
    @Operation(summary = "创建销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:create')")
    public CommonResult<Long> createSaleCart(@Valid @RequestBody ErpSaleCartSaveReqVO createReqVO) {
        return success(saleCartService.createSaleCart(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> updateSaleCart(@Valid @RequestBody ErpSaleCartSaveReqVO updateReqVO) {
        saleCartService.updateSaleCart(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:submit')")
    public CommonResult<Boolean> submitSaleCart(@RequestParam("id") Long id) {
        saleCartService.submitSaleCart(id);
        return success(true);
    }

    @PutMapping("/first-approve")
    @Operation(summary = "初审销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:first-approve')")
    public CommonResult<Boolean> firstApproveSaleCart(@RequestParam("id") Long id) {
        saleCartService.firstApproveSaleCart(id);
        return success(true);
    }

    @PutMapping("/final-approve")
    @Operation(summary = "终审销售手推车，并自动生成销售单")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:final-approve')")
    public CommonResult<Long> finalApproveSaleCart(@RequestParam("id") Long id) {
        return success(saleCartService.finalApproveSaleCart(id));
    }

    @PostMapping("/convert-quote")
    @Operation(summary = "销售手推车转报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:convert-quote')")
    public CommonResult<Long> convertSaleCartToQuote(@RequestParam("id") Long id) {
        return success(saleCartService.convertToQuote(id));
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<ErpSaleCartRespVO> getSaleCart(@RequestParam("id") Long id) {
        ErpSaleCartDO cart = saleCartService.getSaleCart(id);
        if (cart == null) {
            return success(null);
        }
        return success(buildSaleCartRespVO(cart, saleCartService.getSaleCartItemListByCartId(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售手推车分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<PageResult<ErpSaleCartRespVO>> getSaleCartPage(@Valid ErpSaleCartPageReqVO pageReqVO) {
        PageResult<ErpSaleCartDO> pageResult = saleCartService.getSaleCartPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        List<ErpSaleCartItemDO> itemList = saleCartService.getSaleCartItemListByCartIds(
                convertSet(pageResult.getList(), ErpSaleCartDO::getId));
        Map<Long, List<ErpSaleCartItemDO>> itemMap = convertMultiMap(itemList, ErpSaleCartItemDO::getCartId);
        return success(BeanUtils.toBean(pageResult, ErpSaleCartRespVO.class,
                cart -> fillRelation(cart, itemMap.get(cart.getId()))));
    }

    private ErpSaleCartRespVO buildSaleCartRespVO(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        return BeanUtils.toBean(cart, ErpSaleCartRespVO.class, vo -> fillRelation(vo, items));
    }

    private void fillRelation(ErpSaleCartRespVO vo, List<ErpSaleCartItemDO> items) {
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(convertSet(items, ErpSaleCartItemDO::getProductId));
        vo.setItems(BeanUtils.toBean(items, ErpSaleCartRespVO.Item.class,
                item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
        vo.setProductNames(CollUtil.join(vo.getItems(), "，", ErpSaleCartRespVO.Item::getProductName));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(convertSet(Collections.singletonList(vo), ErpSaleCartRespVO::getCustomerId));
        MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
    }

}
