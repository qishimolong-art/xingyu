package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.report.stock.ErpStockProfitLossDetailReportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/erp/business-report/stock-profit-loss-detail")
@Validated
public class ErpStockProfitLossDetailReportController {

    @Resource
    private ErpStockProfitLossDetailReportService reportService;

    @GetMapping("/page")
    @Operation(summary = "获得配件盈亏明细表分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:query')")
    public CommonResult<PageResult<Row>> getPage(@Valid Filter filter) {
        return success(reportService.getPage(filter));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得配件盈亏明细表汇总")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:query')")
    public CommonResult<Summary> getSummary(@Valid Filter filter) {
        return success(reportService.getSummary(filter));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出配件盈亏明细表")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid Filter filter, HttpServletResponse response) throws IOException {
        reportService.export(filter, response);
    }
}
