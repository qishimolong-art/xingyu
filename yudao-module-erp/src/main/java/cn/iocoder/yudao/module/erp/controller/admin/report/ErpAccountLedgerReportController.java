package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.report.finance.ErpAccountLedgerReportService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 账户流水报表")
@RestController
@RequestMapping("/erp/account-ledger-report")
@Validated
public class ErpAccountLedgerReportController {

    private static final String QUERY_PERMISSION = "@ss.hasPermission('erp:account-ledger-report:query')"
            + " && @ss.hasPermission('erp:account:query')";
    private static final String EXPORT_PERMISSION = "@ss.hasPermission('erp:account-ledger-report:export')"
            + " && @ss.hasPermission('erp:account:query')";

    @Resource private ErpAccountLedgerReportService reportService;
    @Resource private AdminUserApi adminUserApi;

    @GetMapping("/page")
    @Operation(summary = "获得账户流水报表分页")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<PageResult<Row>> getPage(@Valid Filter filter) {
        return success(reportService.getPage(filter));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得账户流水报表合计")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<Summary> getSummary(@Valid Filter filter) {
        return success(reportService.getSummary(filter));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获得账户流水报表人员精简分页")
    @PreAuthorize(QUERY_PERMISSION)
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出账户流水报表")
    @PreAuthorize(EXPORT_PERMISSION)
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid Filter filter, HttpServletResponse response) throws IOException {
        reportService.export(filter, response);
    }
}
