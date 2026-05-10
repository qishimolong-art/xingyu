package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
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

@Tag(name = "管理后台 - ERP 报价订单")
@RestController
@RequestMapping("/erp/sale-quote")
@Validated
public class ErpSaleQuoteController {

    @Resource
    private ErpSaleQuoteService saleQuoteService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;

    @PostMapping("/create")
    @Operation(summary = "创建报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<Long> createSaleQuote(@Valid @RequestBody ErpSaleQuoteSaveReqVO createReqVO) {
        return success(saleQuoteService.createSaleQuote(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<Boolean> updateSaleQuote(@Valid @RequestBody ErpSaleQuoteSaveReqVO updateReqVO) {
        saleQuoteService.updateSaleQuote(updateReqVO);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核报价订单，并自动生成销售单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:approve')")
    public CommonResult<Long> approveSaleQuote(@RequestParam("id") Long id) {
        return success(saleQuoteService.approveSaleQuote(id));
    }

    @PostMapping("/convert-cart")
    @Operation(summary = "报价订单转销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:convert-cart')")
    public CommonResult<Long> convertSaleQuoteToCart(@Valid @RequestBody ErpSaleQuoteConvertCartReqVO reqVO) {
        return success(saleQuoteService.convertToCart(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报价订单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:delete')")
    public CommonResult<Boolean> deleteSaleQuote(@RequestParam("ids") List<Long> ids) {
        saleQuoteService.deleteSaleQuote(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<ErpSaleQuoteRespVO> getSaleQuote(@RequestParam("id") Long id) {
        ErpSaleQuoteDO quote = saleQuoteService.getSaleQuote(id);
        if (quote == null) {
            return success(null);
        }
        return success(buildSaleQuoteRespVO(quote, saleQuoteService.getSaleQuoteItemListByQuoteId(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得报价订单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<ErpSaleQuoteRespVO>> getSaleQuotePage(@Valid ErpSaleQuotePageReqVO pageReqVO) {
        PageResult<ErpSaleQuoteDO> pageResult = saleQuoteService.getSaleQuotePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        List<ErpSaleQuoteItemDO> itemList = saleQuoteService.getSaleQuoteItemListByQuoteIds(
                convertSet(pageResult.getList(), ErpSaleQuoteDO::getId));
        Map<Long, List<ErpSaleQuoteItemDO>> itemMap = convertMultiMap(itemList, ErpSaleQuoteItemDO::getQuoteId);
        return success(BeanUtils.toBean(pageResult, ErpSaleQuoteRespVO.class,
                quote -> fillRelation(quote, itemMap.get(quote.getId()))));
    }

    private ErpSaleQuoteRespVO buildSaleQuoteRespVO(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
        return BeanUtils.toBean(quote, ErpSaleQuoteRespVO.class, vo -> fillRelation(vo, items));
    }

    private void fillRelation(ErpSaleQuoteRespVO vo, List<ErpSaleQuoteItemDO> items) {
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(convertSet(items, ErpSaleQuoteItemDO::getProductId));
        vo.setItems(BeanUtils.toBean(items, ErpSaleQuoteRespVO.Item.class,
                item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
        vo.setProductNames(CollUtil.join(vo.getItems(), "，", ErpSaleQuoteRespVO.Item::getProductName));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(convertSet(Collections.singletonList(vo), ErpSaleQuoteRespVO::getCustomerId));
        MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
    }

}
