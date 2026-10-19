package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.report.sale.ErpSaleOutDetailReportService;
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
@RequestMapping("/erp/sale-out-detail-report")
@Validated
public class ErpSaleOutDetailReportController {

    private static final String QUERY_PERMISSION = "@ss.hasPermission('erp:sale-out-detail-report:query')"
            + " && @ss.hasPermission('erp:sale-out:query')"
            + " && @ss.hasPermission('erp:sale-return:query')"
            + " && @ss.hasPermission('erp:sale-price-adjust:query')";
    private static final String EXPORT_PERMISSION = "@ss.hasPermission('erp:sale-out-detail-report:export')"
            + " && @ss.hasPermission('erp:sale-out:export')"
            + " && @ss.hasPermission('erp:sale-return:export')"
            + " && @ss.hasPermission('erp:sale-price-adjust:query')";

    @Resource
    private ErpSaleOutDetailReportService reportService;

    @GetMapping("/page")
    @Operation(summary = "获得配件出库明细查询报表分页")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<PageResult<Row>> getPage(@Valid Filter filter) {
        return success(reportService.getPage(filter));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得配件出库明细查询报表合计")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<Summary> getSummary(@Valid Filter filter) {
        return success(reportService.getSummary(filter));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出配件出库明细查询报表")
    @PreAuthorize(EXPORT_PERMISSION)
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid Filter filter, HttpServletResponse response) throws IOException {
        reportService.export(filter, response);
    }
}
