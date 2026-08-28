package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportRespVO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableReportService;
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

@Tag(name = "ERP 应收报表")
@RestController
@RequestMapping("/erp/receivable-report")
@Validated
public class ErpReceivableReportController {

    @Resource
    private ErpReceivableReportService receivableReportService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @GetMapping("/page")
    @Operation(summary = "获得应收报表分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-report:query')")
    public CommonResult<PageResult<ErpReceivableReportRespVO>> getReceivableReportPage(
            @Valid ErpReceivableReportPageReqVO reqVO) {
        return success(receivableReportService.getReceivableReportPage(reqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获得应收报表可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:receivable-report:query')")
    public CommonResult<List<DeptSimpleRespVO>> getReceivableReportDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_finance_receivable_report"));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得客户其他应收明细")
    @PreAuthorize("@ss.hasPermission('erp:receivable-report:query')")
    public CommonResult<List<ErpReceivableReportDetailRespVO>> getReceivableReportDetailList(
            @Valid ErpReceivableReportDetailReqVO reqVO) {
        return success(receivableReportService.getReceivableReportDetailList(reqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出应收报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceivableReport(@Valid ErpReceivableReportPageReqVO reqVO,
                                       HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "应收报表.xls", "数据", ErpReceivableReportRespVO.class,
                receivableReportService.getReceivableReportPage(reqVO).getList());
    }

    @GetMapping("/export-detail")
    @Operation(summary = "导出客户其他应收明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-report:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceivableReportDetail(@Valid ErpReceivableReportDetailReqVO reqVO,
                                             HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "应收报表明细.xls", "明细", ErpReceivableReportDetailRespVO.class,
                receivableReportService.getReceivableReportDetailList(reqVO));
    }
}
