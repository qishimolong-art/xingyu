package cn.iocoder.yudao.module.erp.controller.admin.report.business;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.business.ErpBusinessReportStatusRespVO;
import cn.iocoder.yudao.module.erp.service.report.business.ErpBusinessReportStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 统一经营报表状态")
@RestController
@RequestMapping("/erp/business-report")
@Validated
public class ErpBusinessReportStatusController {

    @Resource
    private ErpBusinessReportStatusService businessReportStatusService;

    @GetMapping("/status")
    @Operation(summary = "获得统一经营报表状态聚合")
    @PreAuthorize("@ss.hasAnyPermissions('erp:sale-report-v2:query', 'erp:purchase-report-v2:query', "
            + "'erp:stock-record:query', 'erp:stock-transfer-ledger:query', 'erp:receivable-report:query', "
            + "'erp:payable-report:query', 'erp:system-report:query', 'erp:finance-receipt:query', "
            + "'erp:finance-payment:query', 'erp:finance-transfer:query', 'erp:account:query', "
            + "'erp:voucher:query')")
    public CommonResult<ErpBusinessReportStatusRespVO> getStatus() {
        return success(businessReportStatusService.getStatus());
    }

}
