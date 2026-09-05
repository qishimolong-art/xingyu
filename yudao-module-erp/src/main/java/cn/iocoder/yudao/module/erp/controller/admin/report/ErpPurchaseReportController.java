package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDeptExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDeptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportProductExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportTrendRespVO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.report.ErpPurchaseReportService;
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

@Tag(name = "ERP 采购报表")
@RestController
@RequestMapping("/erp/purchase-report")
@Validated
public class ErpPurchaseReportController {

    @Resource
    private ErpPurchaseReportService purchaseReportService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @GetMapping("/summary")
    @Operation(summary = "获得采购报表汇总")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<ErpPurchaseReportSummaryRespVO> getPurchaseReportSummary(
            @Valid ErpPurchaseReportPageReqVO reqVO) {
        return success(purchaseReportService.getPurchaseReportSummary(reqVO));
    }

    @GetMapping("/trend")
    @Operation(summary = "获得采购报表趋势")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<List<ErpPurchaseReportTrendRespVO>> getPurchaseReportTrend(
            @Valid ErpPurchaseReportPageReqVO reqVO) {
        return success(purchaseReportService.getPurchaseReportTrend(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购报表分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<PageResult<ErpPurchaseReportRespVO>> getPurchaseReportPage(
            @Valid ErpPurchaseReportPageReqVO reqVO) {
        return success(purchaseReportService.getPurchaseReportPage(reqVO));
    }

    @GetMapping("/product-page")
    @Operation(summary = "获得采购商品报表分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<PageResult<ErpPurchaseReportProductRespVO>> getPurchaseReportProductPage(
            @Valid ErpPurchaseReportPageReqVO reqVO) {
        return success(purchaseReportService.getPurchaseReportProductPage(reqVO));
    }

    @GetMapping("/dept-page")
    @Operation(summary = "获得采购部门报表分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<PageResult<ErpPurchaseReportDeptRespVO>> getPurchaseReportDeptPage(
            @Valid ErpPurchaseReportPageReqVO reqVO) {
        return success(purchaseReportService.getPurchaseReportDeptPage(reqVO));
    }

    @GetMapping("/detail-page")
    @Operation(summary = "获得采购报表明细分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<PageResult<ErpPurchaseReportDetailRespVO>> getPurchaseReportDetailPage(
            @Valid ErpPurchaseReportPageReqVO reqVO) {
        return success(purchaseReportService.getPurchaseReportDetailPage(reqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得采购报表明细")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<List<ErpPurchaseReportDetailRespVO>> getPurchaseReportDetailList(
            @Valid ErpPurchaseReportDetailReqVO reqVO) {
        return success(purchaseReportService.getPurchaseReportDetailList(reqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获得采购报表可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:query')")
    public CommonResult<List<DeptSimpleRespVO>> getPurchaseReportDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_purchase_report"));
    }

    @GetMapping("/export")
    @Operation(summary = "导出采购报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseReport(@Valid ErpPurchaseReportPageReqVO reqVO,
                                     HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "采购报表.xls", "数据", ErpPurchaseReportExportRespVO.class,
                purchaseReportService.getPurchaseReportPage(reqVO).getList().stream()
                        .map(this::toExportVO).collect(Collectors.toList()));
    }

    @GetMapping("/export-product")
    @Operation(summary = "导出采购商品报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseReportProduct(@Valid ErpPurchaseReportPageReqVO reqVO,
                                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "采购商品报表.xls", "数据", ErpPurchaseReportProductExportRespVO.class,
                purchaseReportService.getPurchaseReportProductPage(reqVO).getList().stream()
                        .map(this::toProductExportVO).collect(Collectors.toList()));
    }

    @GetMapping("/export-dept")
    @Operation(summary = "导出采购部门报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseReportDept(@Valid ErpPurchaseReportPageReqVO reqVO,
                                         HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "采购部门报表.xls", "数据", ErpPurchaseReportDeptExportRespVO.class,
                purchaseReportService.getPurchaseReportDeptPage(reqVO).getList().stream()
                        .map(this::toDeptExportVO).collect(Collectors.toList()));
    }

    @GetMapping("/export-detail")
    @Operation(summary = "导出采购报表明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseReportDetail(@Valid ErpPurchaseReportPageReqVO reqVO,
                                           HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "采购报表明细.xls", "明细", ErpPurchaseReportDetailExportRespVO.class,
                purchaseReportService.getPurchaseReportDetailPage(reqVO).getList().stream()
                        .map(this::toDetailExportVO).collect(Collectors.toList()));
    }

    private ErpPurchaseReportExportRespVO toExportVO(ErpPurchaseReportRespVO source) {
        ErpPurchaseReportExportRespVO target = new ErpPurchaseReportExportRespVO();
        target.setSupplierName(source.getSupplierName());
        target.setContact(source.getContact());
        target.setMobile(source.getMobile());
        target.setDeptName(source.getDeptName());
        target.setPurchaserName(source.getPurchaserName());
        target.setDocCount(source.getDocCount());
        target.setPurchaseCount(source.getPurchaseCount());
        target.setPurchaseAmount(formatExportAmount(source.getPurchaseAmount()));
        target.setReturnCount(source.getReturnCount());
        target.setReturnAmount(formatExportAmount(source.getReturnAmount()));
        target.setNetAmount(formatExportAmount(source.getNetAmount()));
        target.setLastBizTime(source.getLastBizTime());
        return target;
    }

    private ErpPurchaseReportDetailExportRespVO toDetailExportVO(ErpPurchaseReportDetailRespVO source) {
        ErpPurchaseReportDetailExportRespVO target = new ErpPurchaseReportDetailExportRespVO();
        target.setDocType(source.getDocType());
        target.setDocDate(source.getDocDate());
        target.setDocNo(source.getDocNo());
        target.setSupplierName(source.getSupplierName());
        target.setPurchaserName(source.getPurchaserName());
        target.setBizCount(source.getBizCount());
        target.setPurchaseAmount(formatExportAmount(source.getPurchaseAmount()));
        target.setReturnAmount(formatExportAmount(source.getReturnAmount()));
        target.setNetAmount(formatExportAmount(source.getNetAmount()));
        return target;
    }

    private ErpPurchaseReportProductExportRespVO toProductExportVO(ErpPurchaseReportProductRespVO source) {
        ErpPurchaseReportProductExportRespVO target = new ErpPurchaseReportProductExportRespVO();
        target.setProductCode(source.getProductCode());
        target.setProductName(source.getProductName());
        target.setBarCode(source.getBarCode());
        target.setUnitName(source.getUnitName());
        target.setBrand(source.getBrand());
        target.setStandard(source.getStandard());
        target.setVehicleModel(source.getVehicleModel());
        target.setDrawingNo(source.getDrawingNo());
        target.setSupplierCount(source.getSupplierCount());
        target.setDocCount(source.getDocCount());
        target.setPurchaseCount(source.getPurchaseCount());
        target.setPurchaseAmount(formatExportAmount(source.getPurchaseAmount()));
        target.setReturnCount(source.getReturnCount());
        target.setReturnAmount(formatExportAmount(source.getReturnAmount()));
        target.setNetAmount(formatExportAmount(source.getNetAmount()));
        target.setLastBizTime(source.getLastBizTime());
        return target;
    }

    private ErpPurchaseReportDeptExportRespVO toDeptExportVO(ErpPurchaseReportDeptRespVO source) {
        ErpPurchaseReportDeptExportRespVO target = new ErpPurchaseReportDeptExportRespVO();
        target.setDeptName(source.getDeptName());
        target.setSupplierCount(source.getSupplierCount());
        target.setDocCount(source.getDocCount());
        target.setPurchaseCount(source.getPurchaseCount());
        target.setPurchaseAmount(formatExportAmount(source.getPurchaseAmount()));
        target.setReturnCount(source.getReturnCount());
        target.setReturnAmount(formatExportAmount(source.getReturnAmount()));
        target.setNetAmount(formatExportAmount(source.getNetAmount()));
        target.setLastBizTime(source.getLastBizTime());
        return target;
    }

    private String formatExportAmount(BigDecimal value) {
        return value == null ? "****" : value.stripTrailingZeros().toPlainString();
    }
}
