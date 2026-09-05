package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportTrendRespVO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.report.ErpSaleReportService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 销售报表")
@RestController
@RequestMapping("/erp/sale-report")
@Validated
public class ErpSaleReportController {

    @Resource
    private ErpSaleReportService saleReportService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @GetMapping("/summary")
    @Operation(summary = "获得销售报表汇总")
    @PreAuthorize("@ss.hasPermission('erp:sale-report:query')")
    public CommonResult<ErpSaleReportSummaryRespVO> getSaleReportSummary(@Valid ErpSaleReportPageReqVO reqVO) {
        return success(saleReportService.getSaleReportSummary(reqVO));
    }

    @GetMapping("/trend")
    @Operation(summary = "获得销售报表趋势")
    @PreAuthorize("@ss.hasPermission('erp:sale-report:query')")
    public CommonResult<List<ErpSaleReportTrendRespVO>> getSaleReportTrend(@Valid ErpSaleReportPageReqVO reqVO) {
        return success(saleReportService.getSaleReportTrend(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售报表分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-report:query')")
    public CommonResult<PageResult<ErpSaleReportRespVO>> getSaleReportPage(@Valid ErpSaleReportPageReqVO reqVO) {
        return success(saleReportService.getSaleReportPage(reqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得销售报表明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-report:query')")
    public CommonResult<List<ErpSaleReportDetailRespVO>> getSaleReportDetailList(
            @Valid ErpSaleReportDetailReqVO reqVO) {
        return success(saleReportService.getSaleReportDetailList(reqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获得销售报表可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:sale-report:query')")
    public CommonResult<List<DeptSimpleRespVO>> getSaleReportDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_sale_report"));
    }

    @GetMapping("/export")
    @Operation(summary = "导出销售报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleReport(@Valid ErpSaleReportPageReqVO reqVO,
                                 HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "销售报表.xls", "数据", ErpSaleReportExportRespVO.class,
                saleReportService.getSaleReportPage(reqVO).getList().stream()
                        .map(this::toExportVO).collect(Collectors.toList()));
    }

    @GetMapping("/export-detail")
    @Operation(summary = "导出销售报表明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleReportDetail(@Valid ErpSaleReportDetailReqVO reqVO,
                                       HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "销售报表明细.xls", "明细", ErpSaleReportDetailExportRespVO.class,
                saleReportService.getSaleReportDetailList(reqVO).stream()
                        .map(this::toDetailExportVO).collect(Collectors.toList()));
    }

    private ErpSaleReportExportRespVO toExportVO(ErpSaleReportRespVO source) {
        ErpSaleReportExportRespVO target = new ErpSaleReportExportRespVO();
        target.setCustomerName(source.getCustomerName());
        target.setContact(source.getContact());
        target.setMobile(source.getMobile());
        target.setDeptName(source.getDeptName());
        target.setSaleUserName(source.getSaleUserName());
        target.setDocCount(source.getDocCount());
        target.setSaleCount(source.getSaleCount());
        target.setSaleAmount(formatExportAmount(source.getSaleAmount()));
        target.setReturnCount(source.getReturnCount());
        target.setReturnAmount(formatExportAmount(source.getReturnAmount()));
        target.setNetAmount(formatExportAmount(source.getNetAmount()));
        target.setLastBizTime(source.getLastBizTime());
        return target;
    }

    private ErpSaleReportDetailExportRespVO toDetailExportVO(ErpSaleReportDetailRespVO source) {
        ErpSaleReportDetailExportRespVO target = new ErpSaleReportDetailExportRespVO();
        target.setDocType(source.getDocType());
        target.setDocDate(source.getDocDate());
        target.setDocNo(source.getDocNo());
        target.setBizCount(source.getBizCount());
        target.setSaleAmount(formatExportAmount(source.getSaleAmount()));
        target.setReturnAmount(formatExportAmount(source.getReturnAmount()));
        target.setNetAmount(formatExportAmount(source.getNetAmount()));
        return target;
    }

    private String formatExportAmount(BigDecimal value) {
        return value == null ? "****" : value.stripTrailingZeros().toPlainString();
    }
}
