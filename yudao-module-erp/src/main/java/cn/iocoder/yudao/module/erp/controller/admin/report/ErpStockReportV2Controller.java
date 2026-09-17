package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.*;
import cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@RestController
@RequestMapping("/erp/business-report/stock-v2")
@Validated
public class ErpStockReportV2Controller {
    @Resource private ErpStockReportV2Service reportService;

    @GetMapping("/status")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<Status> status() { return success(reportService.status()); }

    @GetMapping("/report-options")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<ReportOptions> reportOptions() { return success(reportService.reportOptions()); }

    @GetMapping("/movement-page")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<PageResult<MovementRow>> movementPage(@Valid Filter filter) { return success(reportService.movementPage(filter)); }

    @GetMapping("/movement-summary")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<Summary> movementSummary(@Valid Filter filter) { return success(reportService.movementSummary(filter)); }

    @GetMapping("/balance-page")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<PageResult<BalanceRow>> balancePage(@Valid Filter filter) { return success(reportService.balancePage(filter)); }

    @GetMapping("/balance-summary")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<Summary> balanceSummary(@Valid Filter filter) { return success(reportService.balanceSummary(filter)); }

    @GetMapping("/export-excel")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void export(@Valid Filter filter,@RequestParam String reportType,HttpServletResponse response) throws IOException {
        reportService.export(filter,reportType,response);
    }

    @GetMapping("/product-options")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query') || @ss.hasPermission('erp:report-stock-opening:confirm')")
    public CommonResult<PageResult<ProductOption>> productOptions(@RequestParam(required=false) String keyword,
            @RequestParam(required=false) Long warehouseId,
            @RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize) {
        return success(reportService.productOptions(keyword,warehouseId,pageNo,pageSize));
    }
}
