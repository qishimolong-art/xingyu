package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryRespVO;
import cn.iocoder.yudao.module.erp.service.purchase.history.ErpPurchaseHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/erp/purchase-history")
@Validated
public class ErpPurchaseHistoryController {

    @Resource
    private ErpPurchaseHistoryService purchaseHistoryService;

    @GetMapping("/page")
    @Operation(summary = "获得采购入库与采购退货统一历史分页")
    @PreAuthorize("@ss.hasAnyPermissions('erp:purchase-in:query', 'erp:purchase-return:query')")
    public CommonResult<PageResult<ErpPurchaseHistoryRespVO>> getPurchaseHistoryPage(
            @Valid ErpPurchaseHistoryPageReqVO pageReqVO) {
        return success(purchaseHistoryService.getPage(pageReqVO));
    }
}
