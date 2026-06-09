package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement.ErpFinanceBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement.ErpFinanceBillRespVO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 收付款统一查询")
@RestController
@RequestMapping("/erp/finance-bill")
@Validated
public class ErpFinanceBillController {

    @Resource
    private ErpFinanceBillService financeBillService;

    @GetMapping("/page")
    @Operation(summary = "获得收付款统一分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query') or @ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<PageResult<ErpFinanceBillRespVO>> getFinanceBillPage(@Valid ErpFinanceBillPageReqVO pageReqVO) {
        return success(financeBillService.getFinanceBillPage(pageReqVO));
    }

}
