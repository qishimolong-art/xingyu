package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.report.finance.ErpOtherIncomeReportService;
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
@RequestMapping("/erp/other-income-report")
@Validated
public class ErpOtherIncomeReportController {

    private static final String QUERY_PERMISSION = "@ss.hasPermission('erp:other-income-report:query')"
            + " && @ss.hasPermission('erp:receivable-other-income:query')";
    private static final String EXPORT_PERMISSION = "@ss.hasPermission('erp:other-income-report:export')"
            + " && @ss.hasPermission('erp:receivable-other-income:query')"
            + " && @ss.hasPermission('erp:receivable-other-income:export')";

    @Resource private ErpOtherIncomeReportService reportService;

    @GetMapping("/page")
    @Operation(summary = "获得其他收入查询报表分页")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<PageResult<Row>> getPage(@Valid Filter filter) {
        return success(reportService.getPage(filter));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得其他收入查询报表合计")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<Summary> getSummary(@Valid Filter filter) {
        return success(reportService.getSummary(filter));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出其他收入查询报表")
    @PreAuthorize(EXPORT_PERMISSION)
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid Filter filter, HttpServletResponse response) throws IOException {
        reportService.export(filter, response);
    }
}
