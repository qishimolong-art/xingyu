package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentBizWriteOffItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinancePaymentService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;

@Tag(name = "管理后台 - ERP 付款单")
@RestController
@RequestMapping("/erp/finance-payment")
@Validated
public class ErpFinancePaymentController {

    public static final Set<String> PAYMENT_IMPORT_TEMPLATE_FIELDS = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList("financeUserName", "deptName", "supplierName", "accountName",
                    "discountPrice", "totalPrice", "paymentPrice", "remark", "bizType", "bizNo", "paidPrice",
                    "itemPaymentPrice", "itemRemark")));

    @Resource
    private ErpFinancePaymentService financePaymentService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpFinanceSettlementImportResolver settlementImportResolver;
    @Resource
    private ErpImportExportRecordService importExportRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建付款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:create') and " +
            "@ss.hasPermission('erp:finance-payment:update-status')")
    public CommonResult<Long> createFinancePayment(@Valid @RequestBody ErpFinancePaymentSaveReqVO createReqVO) {
        return success(financePaymentService.createFinancePayment(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建付款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:create')")
    public CommonResult<Long> createFinancePaymentDraft(
            @RequestBody ErpFinancePaymentDraftSaveReqVO createReqVO) {
        return success(financePaymentService.createFinancePaymentDraft(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交付款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:create') and " +
            "@ss.hasPermission('erp:finance-payment:update-status')")
    public CommonResult<Long> createAndSubmitFinancePayment(
            @Valid @RequestBody ErpFinancePaymentSaveReqVO createReqVO) {
        return success(financePaymentService.createAndSubmitFinancePayment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新付款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:update')")
    public CommonResult<Boolean> updateFinancePayment(@Valid @RequestBody ErpFinancePaymentSaveReqVO updateReqVO) {
        financePaymentService.updateFinancePayment(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新付款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:update')")
    public CommonResult<Boolean> updateFinancePaymentDraft(
            @RequestBody ErpFinancePaymentDraftSaveReqVO updateReqVO) {
        financePaymentService.updateFinancePaymentDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交付款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:update') and " +
            "@ss.hasPermission('erp:finance-payment:update-status')")
    public CommonResult<Boolean> updateAndSubmitFinancePayment(
            @Valid @RequestBody ErpFinancePaymentSaveReqVO updateReqVO) {
        financePaymentService.updateAndSubmitFinancePayment(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交付款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:update-status')")
    public CommonResult<Boolean> submitFinancePayment(@RequestParam("id") Long id) {
        financePaymentService.submitFinancePayment(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "更新付款单备注")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:update')")
    public CommonResult<Boolean> updateFinancePaymentRemark(
            @Valid @RequestBody ErpFinanceUpdateRemarkReqVO updateReqVO) {
        financePaymentService.updateFinancePaymentRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "审核付款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:update-status')")
    public CommonResult<Boolean> approveFinancePayment(@RequestParam("id") Long id,
                                                       @RequestParam(value = "status", defaultValue = "20") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        financePaymentService.approveFinancePayment(id);
        return success(true);
    }

    @GetMapping("/writeoff-candidates")
    @Operation(summary = "获得付款单可核销业务单据")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:writeoff')")
    public CommonResult<List<ErpFinancePaymentWriteOffCandidateRespVO>> getWriteOffCandidates(
            @RequestParam("paymentId") Long paymentId) {
        return success(financePaymentService.getWriteOffCandidates(paymentId));
    }

    @GetMapping("/form-candidates")
    @Operation(summary = "获得新增/编辑付款单候选业务单据")
    @PreAuthorize("@ss.hasAnyPermissions('erp:finance-payment:create', 'erp:finance-payment:update')")
    public CommonResult<List<ErpFinancePaymentFormCandidateRespVO>> getFormCandidates(
            @Valid ErpFinancePaymentFormCandidateReqVO reqVO) {
        return success(financePaymentService.getFormCandidates(reqVO));
    }

    @PostMapping("/writeoff")
    @Operation(summary = "付款单后续核销")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:writeoff')")
    public CommonResult<Boolean> writeOffFinancePayment(
            @Valid @RequestBody ErpFinancePaymentWriteOffReqVO reqVO) {
        financePaymentService.writeOffFinancePayment(reqVO);
        return success(true);
    }

    @PostMapping("/writeoff-reverse")
    @Operation(summary = "撤销付款核销")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:writeoff-reverse')")
    public CommonResult<Boolean> reverseFinancePaymentWriteOff(
            @Valid @RequestBody ErpFinancePaymentWriteOffReverseReqVO reqVO) {
        financePaymentService.reverseFinancePaymentWriteOff(reqVO);
        return success(true);
    }

    @GetMapping("/writeoff-items")
    @Operation(summary = "获得付款单按业务单据核销明细")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<List<ErpFinancePaymentBizWriteOffItemRespVO>> getWriteOffItemsByBiz(
            @RequestParam("bizType") Integer bizType,
            @RequestParam("bizId") Long bizId) {
        List<ErpFinancePaymentItemDO> items = financePaymentService.getFinancePaymentItemListByBiz(bizType, bizId);
        if (CollUtil.isEmpty(items)) {
            return success(Collections.emptyList());
        }
        List<ErpFinancePaymentDO> payments = financePaymentService.getFinancePaymentList(
                convertSet(items, ErpFinancePaymentItemDO::getPaymentId));
        Map<Long, ErpFinancePaymentDO> paymentMap = convertMap(payments, ErpFinancePaymentDO::getId);
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(payments, ErpFinancePaymentDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(payments, ErpFinancePaymentDO::getFinanceUserId));
        return success(convertList(items, item -> buildBizWriteOffItem(item, paymentMap, accountMap, userMap)));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除付款单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:delete')")
    public CommonResult<Boolean> deleteFinancePayment(@RequestParam("ids") List<Long> ids) {
        financePaymentService.deleteFinancePayment(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得付款单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<ErpFinancePaymentRespVO> getFinancePayment(@RequestParam("id") Long id,
                                                                   @RequestParam(value = "includeItems", defaultValue = "true")
                                                                   Boolean includeItems) {
        ErpFinancePaymentDO payment = financePaymentService.getFinancePayment(id);
        if (payment == null) {
            return success(null);
        }
        List<ErpFinancePaymentItemDO> paymentItemList = Boolean.FALSE.equals(includeItems)
                ? Collections.emptyList() : financePaymentService.getFinancePaymentItemListByPaymentId(id);
        ErpFinancePaymentRespVO respVO = BeanUtils.toBean(payment, ErpFinancePaymentRespVO.class,
                financePaymentVO -> financePaymentVO.setItems(buildFinancePaymentItems(paymentItemList)));
        if (!Boolean.FALSE.equals(includeItems)) {
            fillWriteOffSummary(Collections.singletonList(respVO));
        }
        fillFinancePaymentNames(Collections.singletonList(respVO));
        fieldPermissionMasker.maskFormWithItems("erp_finance_payment", respVO);
        return success(respVO);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得付款单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<PageResult<ErpFinancePaymentRespVO.Item>> getFinancePaymentItemPage(
            @Valid ErpFinancePaymentItemPageReqVO pageReqVO) {
        PageResult<ErpFinancePaymentRespVO.Item> result = BeanUtils.toBean(
                financePaymentService.getFinancePaymentItemPage(pageReqVO),
                ErpFinancePaymentRespVO.Item.class);
        if (!Boolean.FALSE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields("erp_finance_payment", result.getList());
        }
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得付款单分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<PageResult<ErpFinancePaymentRespVO>> getFinancePaymentPage(@Valid ErpFinancePaymentPageReqVO pageReqVO) {
        PageResult<ErpFinancePaymentDO> pageResult = financePaymentService.getFinancePaymentPage(pageReqVO);
        return success(Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildFinancePaymentVOPageResultWithoutItems(pageResult)
                : buildFinancePaymentVOPageResult(pageResult));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get finance payment data permission dept simple list")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<List<DeptSimpleRespVO>> getFinancePaymentDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_finance_payment"));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "Get finance payment data permission dept simple page")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getFinancePaymentDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage("erp_finance_payment", pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "Get finance payment user simple page")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getFinancePaymentUserSimplePage(@Valid PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出付款单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFinancePaymentExcel(@Valid ErpFinancePaymentPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        List<ErpFinancePaymentDO> exportList = getFinancePaymentExportList(pageReqVO);
        List<ErpFinancePaymentItemDO> paymentItemList = financePaymentService.getFinancePaymentItemListByPaymentIds(
                convertSet(exportList, ErpFinancePaymentDO::getId));
        Map<Long, List<ErpFinancePaymentItemDO>> financePaymentItemMap = convertMultiMap(paymentItemList,
                ErpFinancePaymentItemDO::getPaymentId);
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(exportList, ErpFinancePaymentDO::getSupplierId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(exportList, ErpFinancePaymentDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(exportList,
                payment -> Stream.of(NumberUtils.parseLong(payment.getCreator()), payment.getFinanceUserId())));
        ExcelUtils.write(response, "付款单.xls", "数据", ErpFinancePaymentExportRespVO.class,
                buildFinancePaymentExportList(exportList, financePaymentItemMap, supplierMap, accountMap, userMap));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得付款单导入模板")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "付款单导入模板.xls", "付款单",
                ErpFinancePaymentImportExcelVO.class,
                Collections.singletonList(new ErpFinancePaymentImportExcelVO()),
                PAYMENT_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入付款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:import')")
    public CommonResult<ErpFinanceImportRespVO> importFinancePayment(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpFinancePaymentImportExcelVO> list = ExcelUtils.read(file, ErpFinancePaymentImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        ErpFinanceSettlementImportResolver.PaymentImportContext importContext =
                settlementImportResolver.buildPaymentContext();
        for (int i = 0; i < list.size(); i++) {
            ErpFinancePaymentImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getFinanceUserName(), row.getFinanceUserId(),
                    row.getDeptName(), row.getDeptId(), row.getSupplierName(), row.getSupplierId(),
                    row.getAccountName(), row.getAccountId(), row.getTotalPrice(), row.getPaymentPrice(),
                    row.getBizType(), row.getBizNo(), row.getBizId(), row.getItemPaymentPrice())) {
                continue;
            }
            try {
                financePaymentService.createFinancePayment(
                        settlementImportResolver.buildPaymentSaveReqVO(row, importContext));
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getPaymentTime(), failureReason(ex));
            }
        }
        return success(result);
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载付款单导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:finance-payment:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, "erp_finance_payment", response);
    }

    private PageResult<ErpFinancePaymentRespVO> buildFinancePaymentVOPageResult(PageResult<ErpFinancePaymentDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpFinancePaymentItemDO> paymentItemList = financePaymentService.getFinancePaymentItemListByPaymentIds(
                convertSet(pageResult.getList(), ErpFinancePaymentDO::getId));
        Map<Long, List<ErpFinancePaymentItemDO>> financePaymentItemMap = convertMultiMap(paymentItemList,
                ErpFinancePaymentItemDO::getPaymentId);
        PageResult<ErpFinancePaymentRespVO> result = BeanUtils.toBean(pageResult,
                ErpFinancePaymentRespVO.class, payment -> {
            payment.setItems(buildFinancePaymentItems(financePaymentItemMap.get(payment.getId())));
        });
        fillFinancePaymentNames(result.getList());
        fillWriteOffSummary(result.getList());
        return result;
    }

    private PageResult<ErpFinancePaymentRespVO> buildFinancePaymentVOPageResultWithoutItems(PageResult<ErpFinancePaymentDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        PageResult<ErpFinancePaymentRespVO> result = BeanUtils.toBean(pageResult,
                ErpFinancePaymentRespVO.class, payment -> payment.setItems(Collections.emptyList()));
        fillFinancePaymentNames(result.getList());
        fillWriteOffSummaryByAggregate(result.getList());
        return result;
    }

    private List<ErpFinancePaymentRespVO.Item> buildFinancePaymentItems(List<ErpFinancePaymentItemDO> items) {
        return BeanUtils.toBean(items, ErpFinancePaymentRespVO.Item.class);
    }

    private ErpFinancePaymentBizWriteOffItemRespVO buildBizWriteOffItem(
            ErpFinancePaymentItemDO item,
            Map<Long, ErpFinancePaymentDO> paymentMap,
            Map<Long, ErpAccountDO> accountMap,
            Map<Long, AdminUserRespDTO> userMap) {
        ErpFinancePaymentBizWriteOffItemRespVO result = BeanUtils.toBean(item,
                ErpFinancePaymentBizWriteOffItemRespVO.class);
        result.setPaymentItemId(item.getId()).setPaymentId(item.getPaymentId());
        ErpFinancePaymentDO payment = paymentMap.get(item.getPaymentId());
        if (payment == null) {
            return result;
        }
        result.setPaymentNo(payment.getNo()).setPaymentTime(payment.getPaymentTime());
        MapUtils.findAndThen(accountMap, payment.getAccountId(),
                account -> result.setAccountName(account.getName()));
        MapUtils.findAndThen(userMap, payment.getFinanceUserId(),
                user -> result.setFinanceUserName(user.getNickname()));
        return result;
    }

    private void fillWriteOffSummary(List<ErpFinancePaymentRespVO> rows) {
        for (ErpFinancePaymentRespVO row : rows) {
            List<ErpFinancePaymentRespVO.Item> items = row.getItems() == null
                    ? Collections.emptyList() : row.getItems();
            BigDecimal allocatedPrice = items.stream()
                    .filter(item -> ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus().equals(item.getWriteOffStatus()))
                    .map(ErpFinancePaymentRespVO.Item::getPaymentPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPrice = row.getTotalPrice() == null ? BigDecimal.ZERO : row.getTotalPrice();
            BigDecimal unallocatedPrice = totalPrice.subtract(allocatedPrice);
            row.setAllocatedPrice(allocatedPrice).setUnallocatedPrice(unallocatedPrice)
                    .setWriteOffCount((int) items.stream()
                            .filter(item -> ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus()
                                    .equals(item.getWriteOffStatus())).count());
            if (allocatedPrice.compareTo(BigDecimal.ZERO) != 0
                    && (totalPrice.compareTo(BigDecimal.ZERO) == 0
                    || allocatedPrice.signum() != totalPrice.signum()
                    || allocatedPrice.abs().compareTo(totalPrice.abs()) > 0)) {
                row.setWriteOffStatus(3);
            } else if (allocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
                row.setWriteOffStatus(0);
            } else if (allocatedPrice.abs().compareTo(totalPrice.abs()) == 0) {
                row.setWriteOffStatus(2);
            } else {
                row.setWriteOffStatus(1);
            }
        }
    }

    private void fillWriteOffSummaryByAggregate(List<ErpFinancePaymentRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Set<Long> paymentIds = convertSet(rows, ErpFinancePaymentRespVO::getId);
        Map<Long, BigDecimal> allocatedMap = financePaymentService.getEffectivePaymentPriceSumMapByPaymentIds(paymentIds);
        Map<Long, Long> countMap = financePaymentService.getEffectivePaymentItemCountMapByPaymentIds(paymentIds);
        for (ErpFinancePaymentRespVO row : rows) {
            BigDecimal allocatedPrice = allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO);
            BigDecimal totalPrice = row.getTotalPrice() == null ? BigDecimal.ZERO : row.getTotalPrice();
            BigDecimal unallocatedPrice = totalPrice.subtract(allocatedPrice);
            row.setAllocatedPrice(allocatedPrice).setUnallocatedPrice(unallocatedPrice)
                    .setWriteOffCount(countMap.getOrDefault(row.getId(), 0L).intValue());
            if (allocatedPrice.compareTo(BigDecimal.ZERO) != 0
                    && (totalPrice.compareTo(BigDecimal.ZERO) == 0
                    || allocatedPrice.signum() != totalPrice.signum()
                    || allocatedPrice.abs().compareTo(totalPrice.abs()) > 0)) {
                row.setWriteOffStatus(3);
            } else if (allocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
                row.setWriteOffStatus(0);
            } else if (allocatedPrice.abs().compareTo(totalPrice.abs()) == 0) {
                row.setWriteOffStatus(2);
            } else {
                row.setWriteOffStatus(1);
            }
        }
    }

    private void fillFinancePaymentNames(List<ErpFinancePaymentRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(rows, ErpFinancePaymentRespVO::getSupplierId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(rows, ErpFinancePaymentRespVO::getAccountId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(rows, ErpFinancePaymentRespVO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(rows,
                row -> Stream.of(parseUserId(row.getCreator()), parseUserId(row.getUpdater()), row.getFinanceUserId())));
        Map<Long, String> voucherNoMap = new HashMap<>();
        List<ErpVoucherDO> vouchers = voucherMapper.selectList(new LambdaQueryWrapperX<ErpVoucherDO>()
                .eq(ErpVoucherDO::getSourceBizType, ErpVoucherSourceBizTypeEnum.PAYMENT.getType())
                .in(ErpVoucherDO::getSourceBizId, convertSet(rows, ErpFinancePaymentRespVO::getId))
                .orderByDesc(ErpVoucherDO::getId));
        for (ErpVoucherDO voucher : vouchers) {
            voucherNoMap.putIfAbsent(voucher.getSourceBizId(), voucher.getVoucherNo());
        }
        for (ErpFinancePaymentRespVO row : rows) {
            MapUtils.findAndThen(supplierMap, row.getSupplierId(), supplier -> {
                row.setSupplierName(supplier.getName());
                row.setSettleMethod(supplier.getSettleMethod());
                row.setBankName(supplier.getBankName());
            });
            MapUtils.findAndThen(accountMap, row.getAccountId(), account -> {
                row.setAccountName(account.getName());
                if (account.getBankName() != null && !account.getBankName().isEmpty()) {
                    row.setBankName(account.getBankName());
                }
            });
            MapUtils.findAndThen(deptMap, row.getDeptId(), dept -> row.setDeptName(dept.getName()));
            MapUtils.findAndThen(userMap, parseUserId(row.getCreator()), user -> row.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, parseUserId(row.getUpdater()), user -> row.setUpdaterName(user.getNickname()));
            MapUtils.findAndThen(userMap, row.getFinanceUserId(), user -> row.setFinanceUserName(user.getNickname()));
            if (ErpAuditStatus.APPROVE.getStatus().equals(row.getStatus())) {
                row.setAuditorName(row.getUpdaterName());
                row.setAuditTime(row.getUpdateTime());
            }
            row.setVoucherNo(voucherNoMap.get(row.getId()));
            if (row.getPrintCount() == null) {
                row.setPrintCount(0);
            }
        }
    }

    private List<ErpFinancePaymentDO> getFinancePaymentExportList(ErpFinancePaymentPageReqVO pageReqVO) {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return financePaymentService.getFinancePaymentPage(pageReqVO).getList();
    }

    private List<ErpFinancePaymentExportRespVO> buildFinancePaymentExportList(List<ErpFinancePaymentDO> list,
                                                                              Map<Long, List<ErpFinancePaymentItemDO>> financePaymentItemMap,
                                                                              Map<Long, ErpSupplierDO> supplierMap,
                                                                              Map<Long, ErpAccountDO> accountMap,
                                                                              Map<Long, AdminUserRespDTO> userMap) {
        List<ErpFinancePaymentExportRespVO> rows = new ArrayList<>();
        for (ErpFinancePaymentDO payment : list) {
            List<ErpFinancePaymentItemDO> items = financePaymentItemMap.getOrDefault(payment.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildFinancePaymentExportRow(payment, null, true, supplierMap, accountMap, userMap));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                rows.add(buildFinancePaymentExportRow(payment, items.get(i), i == 0, supplierMap, accountMap, userMap));
            }
        }
        return rows;
    }

    private ErpFinancePaymentExportRespVO buildFinancePaymentExportRow(ErpFinancePaymentDO payment,
                                                                       ErpFinancePaymentItemDO item,
                                                                       boolean fillMainFields,
                                                                       Map<Long, ErpSupplierDO> supplierMap,
                                                                       Map<Long, ErpAccountDO> accountMap,
                                                                       Map<Long, AdminUserRespDTO> userMap) {
        ErpFinancePaymentExportRespVO row = fillMainFields
                ? BeanUtils.toBean(payment, ErpFinancePaymentExportRespVO.class)
                : new ErpFinancePaymentExportRespVO();
        if (fillMainFields) {
            MapUtils.findAndThen(supplierMap, payment.getSupplierId(), supplier -> row.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(accountMap, payment.getAccountId(), account -> row.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, payment.getFinanceUserId(), user -> row.setFinanceUserName(user.getNickname()));
            MapUtils.findAndThen(userMap, parseUserId(payment.getCreator()), user -> row.setCreatorName(user.getNickname()));
        }
        if (item == null) {
            return row;
        }
        row.setBizTypeName(getBizTypeName(item.getBizType()));
        row.setBizNo(item.getBizNo());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemPaidPrice(item.getPaidPrice());
        row.setItemPaymentPrice(item.getPaymentPrice());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private String getBizTypeName(Integer bizType) {
        if (bizType == null) {
            return null;
        }
        for (ErpBizTypeEnum value : ErpBizTypeEnum.values()) {
            if (value.getType().equals(bizType)) {
                return value.getName();
            }
        }
        return String.valueOf(bizType);
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
