package cn.iocoder.yudao.module.erp.controller.admin.autoorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpAutoOrderRuleDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.autoorder.ErpPurchaseSuggestionPageReqVO;
import cn.iocoder.yudao.module.erp.service.autoorder.ErpAutoOrderService;
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

@Tag(name = "管理后台 - ERP 自动订货")
@RestController
@RequestMapping("/erp/auto-order")
@Validated
public class ErpAutoOrderController {

    @Resource
    private ErpAutoOrderService autoOrderService;

    // ========== 订货规则 ==========

    @PostMapping("/rule/create")
    @Operation(summary = "创建订货规则")
    @PreAuthorize("@ss.hasPermission('erp:auto-order:create')")
    public CommonResult<Long> createRule(@Valid @RequestBody ErpAutoOrderRuleDO rule) {
        return success(autoOrderService.createRule(rule));
    }

    @PutMapping("/rule/update")
    @Operation(summary = "更新订货规则")
    @PreAuthorize("@ss.hasPermission('erp:auto-order:update')")
    public CommonResult<Boolean> updateRule(@Valid @RequestBody ErpAutoOrderRuleDO rule) {
        autoOrderService.updateRule(rule);
        return success(true);
    }

    @DeleteMapping("/rule/delete")
    @Operation(summary = "删除订货规则")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:auto-order:delete')")
    public CommonResult<Boolean> deleteRule(@RequestParam("id") Long id) {
        autoOrderService.deleteRule(id);
        return success(true);
    }

    @GetMapping("/rule/get")
    @Operation(summary = "获得订货规则")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:auto-order:query')")
    public CommonResult<ErpAutoOrderRuleDO> getRule(@RequestParam("id") Long id) {
        return success(autoOrderService.getRule(id));
    }

    @GetMapping("/rule/list")
    @Operation(summary = "获得订货规则列表")
    @PreAuthorize("@ss.hasPermission('erp:auto-order:query')")
    public CommonResult<List<ErpAutoOrderRuleDO>> getRuleList(@RequestParam(value = "status", required = false) Integer status) {
        return success(autoOrderService.getRuleList(status));
    }

    // ========== 采购建议单 ==========

    @PostMapping("/suggestion/generate")
    @Operation(summary = "生成采购建议单")
    @PreAuthorize("@ss.hasPermission('erp:auto-order:create')")
    public CommonResult<Long> generateSuggestion(@RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        return success(autoOrderService.generateSuggestion(warehouseId));
    }

    @PutMapping("/suggestion/confirm")
    @Operation(summary = "确认采购建议单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:auto-order:update')")
    public CommonResult<Boolean> confirmSuggestion(@RequestParam("id") Long id) {
        autoOrderService.confirmSuggestion(id);
        return success(true);
    }

    @PutMapping("/suggestion/generate-order")
    @Operation(summary = "采购建议单转采购订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:auto-order:update')")
    public CommonResult<Boolean> generatePurchaseOrder(@RequestParam("id") Long id) {
        autoOrderService.generatePurchaseOrder(id);
        return success(true);
    }

    @GetMapping("/suggestion/get")
    @Operation(summary = "获得采购建议单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:auto-order:query')")
    public CommonResult<ErpPurchaseSuggestionDO> getSuggestion(@RequestParam("id") Long id) {
        return success(autoOrderService.getSuggestion(id));
    }

    @GetMapping("/suggestion/page")
    @Operation(summary = "获得采购建议单分页")
    @PreAuthorize("@ss.hasPermission('erp:auto-order:query')")
    public CommonResult<PageResult<ErpPurchaseSuggestionDO>> getSuggestionPage(@Valid ErpPurchaseSuggestionPageReqVO pageReqVO) {
        return success(autoOrderService.getSuggestionPage(pageReqVO));
    }

    @GetMapping("/suggestion/item-list")
    @Operation(summary = "获得采购建议单明细")
    @Parameter(name = "suggestionId", description = "建议单编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:auto-order:query')")
    public CommonResult<List<ErpPurchaseSuggestionItemDO>> getSuggestionItemList(@RequestParam("suggestionId") Long suggestionId) {
        return success(autoOrderService.getSuggestionItemList(suggestionId));
    }

}
