package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.*;
import cn.iocoder.yudao.module.erp.service.sale.returncost.ErpSaleReturnCurrentCostService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/erp/sale-return-current-cost")
@Validated
public class ErpSaleReturnCurrentCostController {
    @Resource private ErpSaleReturnCurrentCostService service;

    @GetMapping("/preview")
    @PreAuthorize("@ss.hasPermission('erp:sale-return-current-cost:query') or @ss.hasPermission('erp:sale-return:update-status')")
    public CommonResult<Header> preview(@RequestParam Long id) { return success(service.preview(id)); }

    @GetMapping("/item-page")
    @PreAuthorize("@ss.hasPermission('erp:sale-return-current-cost:query') or @ss.hasPermission('erp:sale-return:update-status')")
    public CommonResult<PageResult<Row>> itemPage(@RequestParam Long id,
            @RequestParam String expectedSourceSignature, @RequestParam String expectedBasisSignature,
            @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "20") int pageSize) {
        return success(service.itemPage(id, expectedSourceSignature, expectedBasisSignature, pageNo, pageSize));
    }

    @PostMapping("/confirm")
    @PreAuthorize("@ss.hasPermission('erp:sale-return-current-cost:confirm') and (@ss.hasPermission('erp:sale-return-current-cost:query') or @ss.hasPermission('erp:sale-return:update-status'))")
    public CommonResult<Header> confirm(@Valid @RequestBody ConfirmRequest request) { return success(service.confirm(request)); }
}
