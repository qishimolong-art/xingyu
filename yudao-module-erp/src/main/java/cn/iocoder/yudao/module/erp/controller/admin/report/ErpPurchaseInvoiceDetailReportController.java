package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.report.purchase.ErpPurchaseInvoiceDetailReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "ERP 采购票据明细查询")
@RestController
@RequestMapping("/erp/purchase-invoice-detail-report")
@Validated
public class ErpPurchaseInvoiceDetailReportController {
    private static final String QUERY = "@ss.hasPermission('erp:purchase-invoice-detail-report:query')"
            + " && @ss.hasPermission('erp:purchase-invoice:query')";
    private static final String EXPORT_PERMISSION = "@ss.hasPermission('erp:purchase-invoice-detail-report:export')"
            + " && @ss.hasPermission('erp:purchase-invoice:export')";

    @Resource private ErpPurchaseInvoiceDetailReportService service;

    @GetMapping("/page") @Operation(summary = "获得采购票据明细分页") @PreAuthorize(QUERY)
    public CommonResult<PageResult<Row>> page(@Valid Filter filter) { return success(service.getPage(filter)); }

    @GetMapping("/summary") @Operation(summary = "获得采购票据明细汇总") @PreAuthorize(QUERY)
    public CommonResult<Summary> summary(@Valid Filter filter) { return success(service.getSummary(filter)); }

    @GetMapping("/export-excel") @Operation(summary = "导出采购票据明细")
    @PreAuthorize(EXPORT_PERMISSION) @ApiAccessLog(operateType = EXPORT)
    public void export(@Valid Filter filter, HttpServletResponse response) throws IOException {
        service.export(filter, response);
    }
}
