package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpPurchaseCostConfirmationModels.*;
import cn.iocoder.yudao.module.erp.service.purchase.cost.ErpPurchaseCostConfirmationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/erp/purchase-cost-confirmation")
@Validated
public class ErpPurchaseCostConfirmationController {
    @Resource private ErpPurchaseCostConfirmationService service;
    @GetMapping({"/preview","/confirmation"})
    @PreAuthorize("@ss.hasPermission('erp:purchase-cost-confirmation:query')")
    public CommonResult<Header> preview(@RequestParam Long purchaseInId) {
        return success(service.preview(purchaseInId));
    }
    @GetMapping("/item-page")
    @PreAuthorize("@ss.hasPermission('erp:purchase-cost-confirmation:query')")
    public CommonResult<PageResult<Item>> itemPage(@RequestParam Long purchaseInId,
            @RequestParam String expectedSignature,@RequestParam(defaultValue="1") int pageNo,
            @RequestParam(defaultValue="20") int pageSize) {
        return success(service.itemPage(purchaseInId,expectedSignature,pageNo,pageSize));
    }
    @PostMapping("/confirm")
    @PreAuthorize("@ss.hasPermission('erp:purchase-cost-confirmation:query') and @ss.hasPermission('erp:purchase-cost-confirmation:confirm')")
    public CommonResult<Header> confirm(@Valid @RequestBody ConfirmRequest request) {
        return success(service.confirm(request));
    }
}
