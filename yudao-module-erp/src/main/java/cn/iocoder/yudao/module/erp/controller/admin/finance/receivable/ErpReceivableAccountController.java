package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailLegacyExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDrilldownReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountDrilldownPageRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountApprovalDrilldownExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountDrilldownExportRespVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableDrilldownTypeEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.ErpFinanceSimplePageUtils.buildUserSimplePage;

@Tag(name = "ERP 应收账款")
@RestController
@RequestMapping("/erp/receivable-account")
@Validated
public class ErpReceivableAccountController {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_account";

    @Resource
    private ErpReceivableAccountService receivableAccountService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @GetMapping("/page")
    @Operation(summary = "获得应收账款分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<PageResult<ErpReceivableAccountRespVO>> getReceivableAccountPage(@Valid ErpReceivableAccountPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(receivableAccountService.getReceivableAccountPage(pageReqVO), ErpReceivableAccountRespVO.class));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得应收账款查询汇总")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<ErpReceivableAccountSummaryRespVO> getReceivableAccountSummary(
            @Valid ErpReceivableAccountPageReqVO pageReqVO) {
        return success(receivableAccountService.getReceivableAccountSummary(pageReqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获取应收账款可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<List<DeptSimpleRespVO>> getReceivableAccountDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_finance_receivable_account"));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获取应收账款可搜索部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getReceivableAccountDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage("erp_finance_receivable_account", pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获取应收账款用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(adminUserApi, pageReqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得应收账款明细")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<List<ErpReceivableDetailRespVO>> getReceivableDetailList(@Valid ErpReceivableDetailReqVO reqVO) {
        return success(receivableAccountService.getReceivableDetailList(reqVO));
    }

    @GetMapping("/drilldown-page")
    @Operation(summary = "获得应收账款金额列下钻分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query') && "
            + "(!#reqVO.miscDetail || @ss.hasPermission('erp:receivable-misc:query'))")
    public CommonResult<ErpAccountDrilldownPageRespVO<ErpReceivableDetailRespVO>> getReceivableDrilldownPage(
            @Valid ErpReceivableDrilldownReqVO reqVO) {
        return success(receivableAccountService.getReceivableDrilldownPage(reqVO));
    }

    @PostMapping("/writeoff")
    @Operation(summary = "核销应收账款")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:writeoff')")
    public CommonResult<Long> writeOffReceivable(@Valid @RequestBody ErpReceivableWriteOffReqVO reqVO) {
        return success(receivableAccountService.writeOffReceivable(reqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出应收账款概览 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceivableAccount(@Valid ErpReceivableAccountPageReqVO reqVO,
                                        @RequestParam(value = "fields", required = false) String fields,
                                        HttpServletResponse response) throws IOException {
        Set<String> includeFields = resolveExportFields(ErpReceivableAccountExportRespVO.class, fields);
        ExcelUtils.write(response, "应收账款概览.xls", "概览", ErpReceivableAccountExportRespVO.class,
                receivableAccountService.getReceivableAccountList(reqVO).stream()
                        .map(this::toExportVO).collect(Collectors.toList()), includeFields);
    }

    public void exportReceivableAccount(ErpReceivableAccountPageReqVO reqVO,
                                        HttpServletResponse response) throws IOException {
        exportReceivableAccount(reqVO, null, response);
    }

    @GetMapping("/export-detail")
    @Operation(summary = "导出应收账款明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceivableDetail(@Valid ErpReceivableDetailReqVO reqVO,
                                       @RequestParam(value = "fields", required = false) String fields,
                                       HttpServletResponse response) throws IOException {
        List<ErpReceivableDetailRespVO> rows = receivableAccountService.getReceivableDetailList(reqVO);
        if (reqVO.isApproveTimeBasis()) {
            ExcelUtils.write(response, "应收账款明细.xls", "明细", ErpReceivableDetailRespVO.class, rows,
                    resolveExportFields(ErpReceivableDetailRespVO.class, fields));
            return;
        }
        ExcelUtils.write(response, "应收账款明细.xls", "明细", ErpReceivableDetailLegacyExportRespVO.class,
                BeanUtils.toBean(rows, ErpReceivableDetailLegacyExportRespVO.class),
                resolveExportFields(ErpReceivableDetailLegacyExportRespVO.class, fields));
    }

    public void exportReceivableDetail(ErpReceivableDetailReqVO reqVO,
                                       HttpServletResponse response) throws IOException {
        exportReceivableDetail(reqVO, null, response);
    }

    @GetMapping("/export-drilldown")
    @Operation(summary = "导出应收账款金额列下钻 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query') && "
            + "(!#reqVO.miscDetail || @ss.hasPermission('erp:receivable-misc:query'))")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceivableDrilldown(@Valid ErpReceivableDrilldownReqVO reqVO,
                                          @RequestParam(value = "fields", required = false) String fields,
                                          HttpServletResponse response) throws IOException {
        ErpReceivableDrilldownTypeEnum type = ErpReceivableDrilldownTypeEnum.fromValue(reqVO.getDetailType());
        String detailLabel = type.getLabel();
        List<ErpReceivableDetailRespVO> rows = receivableAccountService.getReceivableDrilldownList(reqVO);
        if (type == ErpReceivableDrilldownTypeEnum.BALANCE) {
            ExcelUtils.write(response, "应收账款-" + detailLabel + "明细.xls", detailLabel,
                    ErpReceivableDetailRespVO.class, rows,
                    resolveExportFields(ErpReceivableDetailRespVO.class, fields));
            return;
        }
        if (reqVO.isApproveTimeBasis()) {
            List<ErpAccountApprovalDrilldownExportRespVO> exports = BeanUtils.toBean(rows,
                    ErpAccountApprovalDrilldownExportRespVO.class);
            for (int i = 0; i < rows.size(); i++) {
                exports.get(i).setOperatorName(rows.get(i).getAuditorName());
            }
            ExcelUtils.write(response, "应收账款-" + detailLabel + "明细.xls", detailLabel,
                    ErpAccountApprovalDrilldownExportRespVO.class, exports,
                    resolveExportFields(ErpAccountApprovalDrilldownExportRespVO.class, fields));
            return;
        }
        List<ErpAccountDrilldownExportRespVO> exports = BeanUtils.toBean(rows, ErpAccountDrilldownExportRespVO.class);
        for (int i = 0; i < rows.size(); i++) {
            exports.get(i).setOperatorName(rows.get(i).getAuditorName());
        }
        ExcelUtils.write(response, "应收账款-" + detailLabel + "明细.xls", detailLabel,
                ErpAccountDrilldownExportRespVO.class, exports,
                resolveExportFields(ErpAccountDrilldownExportRespVO.class, fields));
    }

    public void exportReceivableDrilldown(ErpReceivableDrilldownReqVO reqVO,
                                          HttpServletResponse response) throws IOException {
        exportReceivableDrilldown(reqVO, null, response);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得应收账款导出字段")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<List<ErpExportFieldRespVO>> getExportFields(
            @RequestParam(value = "scope", defaultValue = "overview") String scope,
            @RequestParam(value = "detailType", required = false) String detailType,
            @RequestParam(value = "dateBasis", required = false) String dateBasis) {
        Class<?> head = resolveExportFieldHead(scope, detailType, dateBasis);
        return success(ErpExportFieldUtils.listFields(head, Collections.emptyMap(),
                fieldPermissionMasker.getHiddenFields(FIELD_PERMISSION_MODULE), Collections.emptyMap()));
    }

    public CommonResult<List<ErpExportFieldRespVO>> getExportFields(String scope) {
        return getExportFields(scope, null, null);
    }

    private Class<?> resolveExportFieldHead(String scope, String detailType, String dateBasis) {
        if ("overview".equals(scope)) {
            return ErpReceivableAccountExportRespVO.class;
        }
        if ("detail".equals(scope)) {
            return "approveTime".equals(dateBasis)
                    ? ErpReceivableDetailRespVO.class : ErpReceivableDetailLegacyExportRespVO.class;
        }
        ErpReceivableDrilldownTypeEnum type = ErpReceivableDrilldownTypeEnum.fromValue(detailType);
        if (type == null || type == ErpReceivableDrilldownTypeEnum.BALANCE) {
            return ErpReceivableDetailRespVO.class;
        }
        return "approveTime".equals(dateBasis)
                ? ErpAccountApprovalDrilldownExportRespVO.class : ErpAccountDrilldownExportRespVO.class;
    }

    private Set<String> resolveExportFields(Class<?> head, String fields) {
        return ErpExportFieldUtils.resolveIncludeFields(head, ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFields(FIELD_PERMISSION_MODULE), Collections.emptyMap());
    }

    private ErpReceivableAccountExportRespVO toExportVO(ErpReceivableAccountDO source) {
        ErpReceivableAccountExportRespVO target = new ErpReceivableAccountExportRespVO();
        target.setCustomerName(source.getCustomerName());
        target.setContact(source.getContact());
        target.setMobile(source.getMobile());
        target.setDeptName(source.getDeptName());
        target.setSaleUserName(source.getSaleUserName());
        target.setBaseAmount(source.getBaseAmount());
        target.setCreditLimit(source.getCreditLimit());
        target.setCreditTermDays(source.getCreditTermDays());
        target.setSaleOutAmount(source.getSaleOutAmount());
        target.setSaleReturnAmount(source.getSaleReturnAmount());
        target.setPriceAdjustAmount(source.getPriceAdjustAmount());
        target.setMiscReceivableAmount(source.getMiscReceivableAmount());
        target.setReceiptAmount(source.getReceiptAmount());
        target.setWriteOffAmount(source.getWriteOffAmount());
        target.setReceivableBalance(source.getReceivableBalance());
        target.setPreAdvanceAmount(source.getPreAdvanceAmount());
        target.setCreditBalance(source.getCreditBalance());
        target.setOpeningReceivableBalance(source.getOpeningReceivableBalance());
        target.setTotalReceivable(source.getTotalReceivable());
        target.setLastBizTime(source.getLastBizTime());
        return target;
    }
}
