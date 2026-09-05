package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportRankRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportTrendRespVO;
import cn.iocoder.yudao.module.erp.service.report.ErpSystemReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 系统报表")
@RestController
@RequestMapping("/erp/system-report")
@Validated
public class ErpSystemReportController {

    @Resource
    private ErpSystemReportService systemReportService;

    @GetMapping("/{reportType}/options")
    @Operation(summary = "获得系统报表筛选项")
    @PreAuthorize("@ss.hasPermission('erp:system-report:query')")
    public CommonResult<Map<String, Object>> getOptions(@PathVariable("reportType") String reportType,
                                                        @Valid ErpSystemReportReqVO reqVO) {
        return success(systemReportService.getOptions(reportType, reqVO));
    }

    @GetMapping("/{reportType}/summary")
    @Operation(summary = "获得系统报表汇总")
    @PreAuthorize("@ss.hasPermission('erp:system-report:query')")
    public CommonResult<ErpSystemReportSummaryRespVO> getSummary(@PathVariable("reportType") String reportType,
                                                                 @Valid ErpSystemReportReqVO reqVO) {
        return success(systemReportService.getSummary(reportType, reqVO));
    }

    @GetMapping("/{reportType}/trend")
    @Operation(summary = "获得系统报表趋势")
    @PreAuthorize("@ss.hasPermission('erp:system-report:query')")
    public CommonResult<List<ErpSystemReportTrendRespVO>> getTrend(@PathVariable("reportType") String reportType,
                                                                   @Valid ErpSystemReportReqVO reqVO) {
        return success(systemReportService.getTrend(reportType, reqVO));
    }

    @GetMapping("/{reportType}/rank")
    @Operation(summary = "获得系统报表排行")
    @PreAuthorize("@ss.hasPermission('erp:system-report:query')")
    public CommonResult<List<ErpSystemReportRankRespVO>> getRank(@PathVariable("reportType") String reportType,
                                                                 @Valid ErpSystemReportReqVO reqVO) {
        return success(systemReportService.getRank(reportType, reqVO));
    }

    @GetMapping("/{reportType}/page")
    @Operation(summary = "获得系统报表分页")
    @PreAuthorize("@ss.hasPermission('erp:system-report:query')")
    public CommonResult<PageResult<Map<String, Object>>> getPage(@PathVariable("reportType") String reportType,
                                                                 @Valid ErpSystemReportReqVO reqVO) {
        return success(systemReportService.getPage(reportType, reqVO));
    }

    @GetMapping("/{reportType}/export")
    @Operation(summary = "导出系统报表")
    @PreAuthorize("@ss.hasPermission('erp:system-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void export(@PathVariable("reportType") String reportType,
                       @Valid ErpSystemReportReqVO reqVO,
                       HttpServletResponse response) throws IOException {
        List<ErpSystemReportExportRespVO> rows = systemReportService.getExportList(reportType, reqVO).stream()
                .map(this::toExportVO)
                .collect(Collectors.toList());
        ExcelUtils.write(response, "ERP系统报表.xlsx", "数据", ErpSystemReportExportRespVO.class, rows);
    }

    private ErpSystemReportExportRespVO toExportVO(Map<String, Object> row) {
        return new ErpSystemReportExportRespVO()
                .setName(text(first(row, "name", "supplierName", "customerName", "productName")))
                .setCode(text(first(row, "code", "productCode")))
                .setDeptName(text(row.get("deptName")))
                .setUserName(text(row.get("userName")))
                .setWarehouseName(text(row.get("warehouseName")))
                .setCategoryName(text(row.get("categoryName")))
                .setDocType(text(row.get("docType")))
                .setDocDate(text(row.get("docDate")))
                .setDocNo(text(row.get("docNo")))
                .setBizCount(number(first(row, "bizCount", "inCount", "saleCount", "stockQty")))
                .setInAmount(number(first(row, "inAmount", "saleAmount")))
                .setReturnAmount(number(first(row, "returnAmount", "outAmount")))
                .setNetAmount(number(first(row, "netAmount", "stockAmount")))
                .setPendingQty(number(first(row, "pendingQty", "gapQty")))
                .setRisk(text(row.get("risk")));
    }

    private Object first(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return value.toString().replace('T', ' ');
        }
        return String.valueOf(value);
    }

    private String number(Object value) {
        if (value == null) {
            return "****";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

}
