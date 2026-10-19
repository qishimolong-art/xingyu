package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.report.sale.ErpSalePickDeliveryDetailReportService;
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
@RequestMapping("/erp/sale-delivery-detail-report")
@Validated
public class ErpSaleDeliveryDetailReportController {
    private static final String QUERY_PERMISSION = "@ss.hasPermission('erp:sale-delivery-detail-report:query')"
            + " && @ss.hasPermission('erp:sale-delivery:query')";
    private static final String EXPORT_PERMISSION = "@ss.hasPermission('erp:sale-delivery-detail-report:export')"
            + " && @ss.hasPermission('erp:sale-delivery:export')";
    @Resource private ErpSalePickDeliveryDetailReportService reportService;

    @GetMapping("/page")
    @Operation(summary = "获得送货明细报表分页")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<PageResult<Row>> getPage(@Valid Filter filter) {
        return success(reportService.getPage(filter, true));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得送货明细报表合计")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<Summary> getSummary(@Valid Filter filter) {
        return success(reportService.getSummary(filter, true));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出送货明细报表")
    @PreAuthorize(EXPORT_PERMISSION)
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid Filter filter, HttpServletResponse response) throws IOException {
        reportService.export(filter, true, response);
    }
}
