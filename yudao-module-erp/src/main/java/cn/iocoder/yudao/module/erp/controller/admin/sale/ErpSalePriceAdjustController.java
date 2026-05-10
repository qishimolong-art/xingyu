package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
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

@Tag(name = "管理后台 - ERP 销售调价单")
@RestController
@RequestMapping("/erp/sale-price-adjust")
@Validated
public class ErpSalePriceAdjustController {

    @Resource
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Resource
    private ErpCustomerService customerService;

    @PostMapping("/create")
    @Operation(summary = "创建销售调价单")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:create')")
    public CommonResult<Long> createSalePriceAdjust(@Valid @RequestBody ErpSalePriceAdjustSaveReqVO createReqVO) {
        return success(salePriceAdjustService.createSalePriceAdjust(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售调价单")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:update')")
    public CommonResult<Boolean> updateSalePriceAdjust(@Valid @RequestBody ErpSalePriceAdjustSaveReqVO updateReqVO) {
        salePriceAdjustService.updateSalePriceAdjust(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新销售调价单状态")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:update-status')")
    public CommonResult<Boolean> updateSalePriceAdjustStatus(@RequestParam("id") Long id,
                                                              @RequestParam("status") Integer status) {
        salePriceAdjustService.updateSalePriceAdjustStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售调价单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:delete')")
    public CommonResult<Boolean> deleteSalePriceAdjust(@RequestParam("ids") List<Long> ids) {
        salePriceAdjustService.deleteSalePriceAdjust(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售调价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:query')")
    public CommonResult<ErpSalePriceAdjustRespVO> getSalePriceAdjust(@RequestParam("id") Long id) {
        ErpSalePriceAdjustDO adjust = salePriceAdjustService.getSalePriceAdjust(id);
        if (adjust == null) {
            return success(null);
        }
        List<ErpSalePriceAdjustItemDO> items = salePriceAdjustService.getSalePriceAdjustItemListByAdjustId(id);
        ErpSalePriceAdjustRespVO respVO = BeanUtils.toBean(adjust, ErpSalePriceAdjustRespVO.class);
        respVO.setItems(BeanUtils.toBean(items, ErpSalePriceAdjustRespVO.Item.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售调价单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:query')")
    public CommonResult<PageResult<ErpSalePriceAdjustRespVO>> getSalePriceAdjustPage(@Valid ErpSalePriceAdjustPageReqVO pageReqVO) {
        PageResult<ErpSalePriceAdjustDO> pageResult = salePriceAdjustService.getSalePriceAdjustPage(pageReqVO);
        return success(buildSalePriceAdjustVOPageResult(pageResult));
    }

    private PageResult<ErpSalePriceAdjustRespVO> buildSalePriceAdjustVOPageResult(PageResult<ErpSalePriceAdjustDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpSalePriceAdjustItemDO> itemList = salePriceAdjustService.getSalePriceAdjustItemListByAdjustIds(
                convertSet(pageResult.getList(), ErpSalePriceAdjustDO::getId));
        Map<Long, List<ErpSalePriceAdjustItemDO>> itemMap = convertMultiMap(itemList, ErpSalePriceAdjustItemDO::getAdjustId);
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSalePriceAdjustDO::getCustomerId));
        return BeanUtils.toBean(pageResult, ErpSalePriceAdjustRespVO.class, adjust -> {
            adjust.setItems(BeanUtils.toBean(itemMap.get(adjust.getId()), ErpSalePriceAdjustRespVO.Item.class));
            findAndThen(customerMap, adjust.getCustomerId(), customer -> adjust.setCustomerName(customer.getName()));
        });
    }

}
