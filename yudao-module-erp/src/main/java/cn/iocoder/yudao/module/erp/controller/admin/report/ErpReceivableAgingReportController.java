package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.DetailFilter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.DetailRow;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.report.finance.ErpReceivableAgingReportService;
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
@RequestMapping("/erp/receivable-aging-report")
@Validated
public class ErpReceivableAgingReportController {

    private static final String QUERY_PERMISSION = "@ss.hasPermission('erp:receivable-aging-report:query')"
            + " && @ss.hasPermission('erp:receivable-account:query')";
    private static final String EXPORT_PERMISSION = "@ss.hasPermission('erp:receivable-aging-report:export')"
            + " && @ss.hasPermission('erp:receivable-account:query')";

    @Resource
    private ErpReceivableAgingReportService reportService;

    @GetMapping("/page")
    @Operation(summary = "获得应收账龄分析分页")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<PageResult<Row>> getPage(@Valid Filter filter) {
        return success(reportService.getPage(filter));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得应收账龄分析汇总")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<Summary> getSummary(@Valid Filter filter) {
        return success(reportService.getSummary(filter));
    }

    @GetMapping("/detail-page")
    @Operation(summary = "获得应收账龄金额构成明细")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<PageResult<DetailRow>> getDetailPage(@Valid DetailFilter filter) {
        return success(reportService.getDetailPage(filter));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出应收账龄分析")
    @PreAuthorize(EXPORT_PERMISSION)
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid Filter filter, HttpServletResponse response) throws IOException {
        reportService.export(filter, response);
    }
}
