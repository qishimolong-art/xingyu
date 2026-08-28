package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportRespVO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableReportService;
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
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 应付报表")
@RestController
@RequestMapping("/erp/payable-report")
@Validated
public class ErpPayableReportController {

    @Resource
    private ErpPayableReportService payableReportService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @GetMapping("/page")
    @Operation(summary = "获得应付报表分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-report:query')")
    public CommonResult<PageResult<ErpPayableReportRespVO>> getPayableReportPage(
            @Valid ErpPayableReportPageReqVO reqVO) {
        return success(payableReportService.getPayableReportPage(reqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获得应付报表可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:payable-report:query')")
    public CommonResult<List<DeptSimpleRespVO>> getPayableReportDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_finance_payable_report"));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得供应商其他应付明细")
    @PreAuthorize("@ss.hasPermission('erp:payable-report:query')")
    public CommonResult<List<ErpPayableReportDetailRespVO>> getPayableReportDetailList(
            @Valid ErpPayableReportDetailReqVO reqVO) {
        return success(payableReportService.getPayableReportDetailList(reqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出应付报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPayableReport(@Valid ErpPayableReportPageReqVO reqVO,
                                    HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "应付报表.xls", "数据", ErpPayableReportRespVO.class,
                payableReportService.getPayableReportPage(reqVO).getList());
    }

    @GetMapping("/export-detail")
    @Operation(summary = "导出供应商其他应付明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPayableReportDetail(@Valid ErpPayableReportDetailReqVO reqVO,
                                          HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "应付报表明细.xls", "明细", ErpPayableReportDetailRespVO.class,
                payableReportService.getPayableReportDetailList(reqVO));
    }
}
