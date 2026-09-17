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
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptBizWriteOffItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceReceiptService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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

@Tag(name = "管理后台 - ERP 收款单")
@RestController
@RequestMapping("/erp/finance-receipt")
@Validated
public class ErpFinanceReceiptController {

    public static final Set<String> RECEIPT_IMPORT_TEMPLATE_FIELDS = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList("financeUserName", "deptName", "customerName", "accountName",
                    "discountPrice", "totalPrice", "receiptPrice", "remark", "bizType", "bizNo",
                    "receiptedPrice", "itemReceiptPrice", "itemRemark")));

    @Resource
    private ErpFinanceReceiptService financeReceiptService;
    @Resource
    private ErpCustomerService customerService;
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
    private ErpCustomerDeptPermissionService customerDeptPermissionService;
    @Resource
    private ErpFinanceSettlementImportResolver settlementImportResolver;
    @Resource
    private ErpImportExportRecordService importExportRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:create') and " +
            "@ss.hasPermission('erp:finance-receipt:update-status')")
    public CommonResult<Long> createFinanceReceipt(@Valid @RequestBody ErpFinanceReceiptSaveReqVO createReqVO) {
        return success(financeReceiptService.createFinanceReceipt(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建收款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:create')")
    public CommonResult<Long> createFinanceReceiptDraft(
            @RequestBody ErpFinanceReceiptDraftSaveReqVO createReqVO) {
        return success(financeReceiptService.createFinanceReceiptDraft(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:create') and " +
            "@ss.hasPermission('erp:finance-receipt:update-status')")
    public CommonResult<Long> createAndSubmitFinanceReceipt(
            @Valid @RequestBody ErpFinanceReceiptSaveReqVO createReqVO) {
        return success(financeReceiptService.createAndSubmitFinanceReceipt(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update')")
    public CommonResult<Boolean> updateFinanceReceipt(@Valid @RequestBody ErpFinanceReceiptSaveReqVO updateReqVO) {
        financeReceiptService.updateFinanceReceipt(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新收款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update')")
    public CommonResult<Boolean> updateFinanceReceiptDraft(
            @RequestBody ErpFinanceReceiptDraftSaveReqVO updateReqVO) {
        financeReceiptService.updateFinanceReceiptDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交收款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update') and " +
            "@ss.hasPermission('erp:finance-receipt:update-status')")
    public CommonResult<Boolean> updateAndSubmitFinanceReceipt(
            @Valid @RequestBody ErpFinanceReceiptSaveReqVO updateReqVO) {
        financeReceiptService.updateAndSubmitFinanceReceipt(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交收款单草稿")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update-status')")
    public CommonResult<Boolean> submitFinanceReceipt(@RequestParam("id") Long id) {
        financeReceiptService.submitFinanceReceipt(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "更新收款单备注")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update')")
    public CommonResult<Boolean> updateFinanceReceiptRemark(
            @Valid @RequestBody ErpFinanceUpdateRemarkReqVO updateReqVO) {
        financeReceiptService.updateFinanceReceiptRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "审核收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update-status')")
    public CommonResult<Boolean> approveFinanceReceipt(@RequestParam("id") Long id,
                                                       @RequestParam(value = "status", defaultValue = "20") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        financeReceiptService.approveFinanceReceipt(id);
        return success(true);
    }

    @GetMapping("/writeoff-candidates")
    @Operation(summary = "获得收款单可核销业务单据")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:writeoff')")
    public CommonResult<List<ErpFinanceReceiptWriteOffCandidateRespVO>> getWriteOffCandidates(
            @RequestParam("receiptId") Long receiptId) {
        return success(financeReceiptService.getWriteOffCandidates(receiptId));
    }

    @GetMapping("/form-candidates")
    @Operation(summary = "获得新增/编辑收款单候选业务单据")
    @PreAuthorize("@ss.hasAnyPermissions('erp:finance-receipt:create', 'erp:finance-receipt:update')")
    public CommonResult<List<ErpFinanceReceiptFormCandidateRespVO>> getFormCandidates(
            @Valid ErpFinanceReceiptFormCandidateReqVO reqVO) {
        return success(financeReceiptService.getFormCandidates(reqVO));
    }

    @PostMapping("/writeoff")
    @Operation(summary = "收款单后续核销")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:writeoff')")
    public CommonResult<Boolean> writeOffFinanceReceipt(
            @Valid @RequestBody ErpFinanceReceiptWriteOffReqVO reqVO) {
        financeReceiptService.writeOffFinanceReceipt(reqVO);
        return success(true);
    }

    @PostMapping("/writeoff-reverse")
    @Operation(summary = "撤销收款核销")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:writeoff-reverse')")
    public CommonResult<Boolean> reverseFinanceReceiptWriteOff(
            @Valid @RequestBody ErpFinanceReceiptWriteOffReverseReqVO reqVO) {
        financeReceiptService.reverseFinanceReceiptWriteOff(reqVO);
        return success(true);
    }

    @GetMapping("/writeoff-items")
    @Operation(summary = "获得收款单按业务单据核销明细")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<List<ErpFinanceReceiptBizWriteOffItemRespVO>> getWriteOffItemsByBiz(
            @RequestParam("bizType") Integer bizType,
            @RequestParam("bizId") Long bizId) {
        List<ErpFinanceReceiptItemDO> items = financeReceiptService.getFinanceReceiptItemListByBiz(bizType, bizId);
        if (CollUtil.isEmpty(items)) {
            return success(Collections.emptyList());
        }
        List<ErpFinanceReceiptDO> receipts = financeReceiptService.getFinanceReceiptList(
                convertSet(items, ErpFinanceReceiptItemDO::getReceiptId));
        Map<Long, ErpFinanceReceiptDO> receiptMap = convertMap(receipts, ErpFinanceReceiptDO::getId);
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(receipts, ErpFinanceReceiptDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(receipts, ErpFinanceReceiptDO::getFinanceUserId));
        return success(convertList(items, item -> buildBizWriteOffItem(item, receiptMap, accountMap, userMap)));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收款单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:delete')")
    public CommonResult<Boolean> deleteFinanceReceipt(@RequestParam("ids") List<Long> ids) {
        financeReceiptService.deleteFinanceReceipt(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收款单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<ErpFinanceReceiptRespVO> getFinanceReceipt(@RequestParam("id") Long id,
                                                                   @RequestParam(value = "includeItems", defaultValue = "true")
                                                                   Boolean includeItems) {
        ErpFinanceReceiptDO receipt = financeReceiptService.getFinanceReceipt(id);
        if (receipt == null) {
            return success(null);
        }
        List<ErpFinanceReceiptItemDO> receiptItemList = Boolean.FALSE.equals(includeItems)
                ? Collections.emptyList() : financeReceiptService.getFinanceReceiptItemListByReceiptId(id);
        ErpFinanceReceiptRespVO respVO = BeanUtils.toBean(receipt, ErpFinanceReceiptRespVO.class,
                financeReceiptVO -> financeReceiptVO.setItems(buildFinanceReceiptItems(receiptItemList)));
        if (!Boolean.FALSE.equals(includeItems)) {
            fillWriteOffSummary(Collections.singletonList(respVO));
        }
        fillFinanceReceiptNames(Collections.singletonList(respVO));
        fieldPermissionMasker.maskFormWithItems("erp_finance_receipt", respVO);
        return success(respVO);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得收款单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<PageResult<ErpFinanceReceiptRespVO.Item>> getFinanceReceiptItemPage(
            @Valid ErpFinanceReceiptItemPageReqVO pageReqVO) {
        PageResult<ErpFinanceReceiptRespVO.Item> result = BeanUtils.toBean(
                financeReceiptService.getFinanceReceiptItemPage(pageReqVO),
                ErpFinanceReceiptRespVO.Item.class);
        if (!Boolean.FALSE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields("erp_finance_receipt", result.getList());
        }
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得收款单分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<PageResult<ErpFinanceReceiptRespVO>> getFinanceReceiptPage(@Valid ErpFinanceReceiptPageReqVO pageReqVO) {
        PageResult<ErpFinanceReceiptDO> pageResult = financeReceiptService.getFinanceReceiptPage(pageReqVO);
        return success(Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildFinanceReceiptVOPageResultWithoutItems(pageResult)
                : buildFinanceReceiptVOPageResult(pageResult));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get finance receipt data permission dept simple list")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<List<DeptSimpleRespVO>> getFinanceReceiptDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_finance_receipt"));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "Get finance receipt data permission dept simple page")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getFinanceReceiptDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage("erp_finance_receipt", pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "Get finance receipt user simple page")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getFinanceReceiptUserSimplePage(@Valid PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/customer-dept-simple-list")
    @Operation(summary = "获取客户对当前用户可用的收款部门精简列表")
    @Parameter(name = "customerId", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<List<DeptSimpleRespVO>> getCustomerAvailableDeptSimpleList(
            @RequestParam("customerId") Long customerId) {
        return success(customerDeptPermissionService.getAvailableDeptSimpleList(customerId, "erp_finance_receipt"));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出收款单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFinanceReceiptExcel(@Valid ErpFinanceReceiptPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        List<ErpFinanceReceiptDO> exportList = getFinanceReceiptExportList(pageReqVO);
        List<ErpFinanceReceiptItemDO> receiptItemList = financeReceiptService.getFinanceReceiptItemListByReceiptIds(
                convertSet(exportList, ErpFinanceReceiptDO::getId));
        Map<Long, List<ErpFinanceReceiptItemDO>> financeReceiptItemMap = convertMultiMap(receiptItemList,
                ErpFinanceReceiptItemDO::getReceiptId);
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(exportList, ErpFinanceReceiptDO::getCustomerId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(exportList, ErpFinanceReceiptDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(exportList,
                receipt -> Stream.of(NumberUtils.parseLong(receipt.getCreator()), receipt.getFinanceUserId())));
        ExcelUtils.write(response, "收款单.xls", "数据", ErpFinanceReceiptExportRespVO.class,
                buildFinanceReceiptExportList(exportList, financeReceiptItemMap, customerMap, accountMap, userMap));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得收款单导入模板")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "收款单导入模板.xls", "收款单",
                ErpFinanceReceiptImportExcelVO.class,
                Collections.singletonList(new ErpFinanceReceiptImportExcelVO()),
                RECEIPT_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:import') and " +
            "@ss.hasPermission('erp:finance-receipt:update-status')")
    public CommonResult<ErpFinanceImportRespVO> importFinanceReceipt(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpFinanceReceiptImportExcelVO> list = ExcelUtils.read(file, ErpFinanceReceiptImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        ErpFinanceSettlementImportResolver.ReceiptImportContext importContext =
                settlementImportResolver.buildReceiptContext();
        for (int i = 0; i < list.size(); i++) {
            ErpFinanceReceiptImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getFinanceUserName(), row.getFinanceUserId(),
                    row.getDeptName(), row.getDeptId(), row.getCustomerName(), row.getCustomerId(),
                    row.getAccountName(), row.getAccountId(), row.getTotalPrice(), row.getReceiptPrice(),
                    row.getBizType(), row.getBizNo(), row.getBizId(), row.getItemReceiptPrice())) {
                continue;
            }
            try {
                financeReceiptService.createFinanceReceipt(
                        settlementImportResolver.buildReceiptSaveReqVO(row, importContext));
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getReceiptTime(), failureReason(ex));
            }
        }
        return success(result);
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载收款单导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:import') and " +
            "@ss.hasPermission('erp:finance-receipt:update-status')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, "erp_finance_receipt", response);
    }

    private PageResult<ErpFinanceReceiptRespVO> buildFinanceReceiptVOPageResult(PageResult<ErpFinanceReceiptDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpFinanceReceiptItemDO> receiptItemList = financeReceiptService.getFinanceReceiptItemListByReceiptIds(
                convertSet(pageResult.getList(), ErpFinanceReceiptDO::getId));
        Map<Long, List<ErpFinanceReceiptItemDO>> financeReceiptItemMap = convertMultiMap(receiptItemList,
                ErpFinanceReceiptItemDO::getReceiptId);
        PageResult<ErpFinanceReceiptRespVO> result = BeanUtils.toBean(pageResult,
                ErpFinanceReceiptRespVO.class, receipt -> {
            receipt.setItems(buildFinanceReceiptItems(financeReceiptItemMap.get(receipt.getId())));
        });
        fillFinanceReceiptNames(result.getList());
        fillWriteOffSummary(result.getList());
        return result;
    }

    private PageResult<ErpFinanceReceiptRespVO> buildFinanceReceiptVOPageResultWithoutItems(PageResult<ErpFinanceReceiptDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        PageResult<ErpFinanceReceiptRespVO> result = BeanUtils.toBean(pageResult,
                ErpFinanceReceiptRespVO.class, receipt -> receipt.setItems(Collections.emptyList()));
        fillFinanceReceiptNames(result.getList());
        fillWriteOffSummaryByAggregate(result.getList());
        return result;
    }

    private List<ErpFinanceReceiptRespVO.Item> buildFinanceReceiptItems(List<ErpFinanceReceiptItemDO> items) {
        return BeanUtils.toBean(items, ErpFinanceReceiptRespVO.Item.class);
    }

    private ErpFinanceReceiptBizWriteOffItemRespVO buildBizWriteOffItem(
            ErpFinanceReceiptItemDO item,
            Map<Long, ErpFinanceReceiptDO> receiptMap,
            Map<Long, ErpAccountDO> accountMap,
            Map<Long, AdminUserRespDTO> userMap) {
        ErpFinanceReceiptBizWriteOffItemRespVO result = BeanUtils.toBean(item,
                ErpFinanceReceiptBizWriteOffItemRespVO.class);
        result.setReceiptItemId(item.getId()).setReceiptId(item.getReceiptId());
        ErpFinanceReceiptDO receipt = receiptMap.get(item.getReceiptId());
        if (receipt == null) {
            return result;
        }
        result.setReceiptNo(receipt.getNo()).setReceiptTime(receipt.getReceiptTime());
        MapUtils.findAndThen(accountMap, receipt.getAccountId(),
                account -> result.setAccountName(account.getName()));
        MapUtils.findAndThen(userMap, receipt.getFinanceUserId(),
                user -> result.setFinanceUserName(user.getNickname()));
        return result;
    }

    private void fillWriteOffSummary(List<ErpFinanceReceiptRespVO> rows) {
        for (ErpFinanceReceiptRespVO row : rows) {
            List<ErpFinanceReceiptRespVO.Item> items = row.getItems() == null
                    ? Collections.emptyList() : row.getItems();
            BigDecimal allocatedPrice = items.stream()
                    .filter(item -> ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus().equals(item.getWriteOffStatus()))
                    .map(ErpFinanceReceiptRespVO.Item::getReceiptPrice)
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

    private void fillWriteOffSummaryByAggregate(List<ErpFinanceReceiptRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Set<Long> receiptIds = convertSet(rows, ErpFinanceReceiptRespVO::getId);
        Map<Long, BigDecimal> allocatedMap = financeReceiptService.getEffectiveReceiptPriceSumMapByReceiptIds(receiptIds);
        Map<Long, Long> countMap = financeReceiptService.getEffectiveReceiptItemCountMapByReceiptIds(receiptIds);
        for (ErpFinanceReceiptRespVO row : rows) {
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

    private void fillFinanceReceiptNames(List<ErpFinanceReceiptRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(rows, ErpFinanceReceiptRespVO::getCustomerId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(rows, ErpFinanceReceiptRespVO::getAccountId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(rows, ErpFinanceReceiptRespVO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(rows,
                row -> Stream.of(parseUserId(row.getCreator()), parseUserId(row.getUpdater()), row.getFinanceUserId())));
        Map<Long, String> voucherNoMap = new HashMap<>();
        List<ErpVoucherDO> vouchers = voucherMapper.selectList(new LambdaQueryWrapperX<ErpVoucherDO>()
                .eq(ErpVoucherDO::getSourceBizType, ErpVoucherSourceBizTypeEnum.RECEIPT.getType())
                .in(ErpVoucherDO::getSourceBizId, convertSet(rows, ErpFinanceReceiptRespVO::getId))
                .orderByDesc(ErpVoucherDO::getId));
        for (ErpVoucherDO voucher : vouchers) {
            voucherNoMap.putIfAbsent(voucher.getSourceBizId(), voucher.getVoucherNo());
        }
        for (ErpFinanceReceiptRespVO row : rows) {
            MapUtils.findAndThen(customerMap, row.getCustomerId(), customer -> {
                row.setCustomerName(customer.getName());
                row.setSettleMethod(getCustomerSettleMethodName(customer.getSettleMethod()));
                row.setBankName(customer.getBankName());
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
        }
    }

    private String getCustomerSettleMethodName(Integer settleMethod) {
        if (settleMethod == null) {
            return null;
        }
        switch (settleMethod) {
            case 1:
                return "现金";
            case 2:
                return "挂账";
            case 3:
                return "汇款";
            case 4:
                return "网上支付";
            default:
                return String.valueOf(settleMethod);
        }
    }

    private List<ErpFinanceReceiptDO> getFinanceReceiptExportList(ErpFinanceReceiptPageReqVO pageReqVO) {
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            return financeReceiptService.getFinanceReceiptList(pageReqVO.getIds());
        }
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return financeReceiptService.getFinanceReceiptPage(pageReqVO).getList();
    }

    private List<ErpFinanceReceiptExportRespVO> buildFinanceReceiptExportList(List<ErpFinanceReceiptDO> list,
                                                                              Map<Long, List<ErpFinanceReceiptItemDO>> financeReceiptItemMap,
                                                                              Map<Long, ErpCustomerDO> customerMap,
                                                                              Map<Long, ErpAccountDO> accountMap,
                                                                              Map<Long, AdminUserRespDTO> userMap) {
        List<ErpFinanceReceiptExportRespVO> rows = new ArrayList<>();
        for (ErpFinanceReceiptDO receipt : list) {
            List<ErpFinanceReceiptItemDO> items = financeReceiptItemMap.getOrDefault(receipt.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildFinanceReceiptExportRow(receipt, null, true, customerMap, accountMap, userMap));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                rows.add(buildFinanceReceiptExportRow(receipt, items.get(i), i == 0, customerMap, accountMap, userMap));
            }
        }
        return rows;
    }

    private ErpFinanceReceiptExportRespVO buildFinanceReceiptExportRow(ErpFinanceReceiptDO receipt,
                                                                       ErpFinanceReceiptItemDO item,
                                                                       boolean fillMainFields,
                                                                       Map<Long, ErpCustomerDO> customerMap,
                                                                       Map<Long, ErpAccountDO> accountMap,
                                                                       Map<Long, AdminUserRespDTO> userMap) {
        ErpFinanceReceiptExportRespVO row = fillMainFields
                ? BeanUtils.toBean(receipt, ErpFinanceReceiptExportRespVO.class)
                : new ErpFinanceReceiptExportRespVO();
        if (fillMainFields) {
            MapUtils.findAndThen(customerMap, receipt.getCustomerId(), customer -> row.setCustomerName(customer.getName()));
            MapUtils.findAndThen(accountMap, receipt.getAccountId(), account -> row.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, receipt.getFinanceUserId(), user -> row.setFinanceUserName(user.getNickname()));
            MapUtils.findAndThen(userMap, parseUserId(receipt.getCreator()), user -> row.setCreatorName(user.getNickname()));
        }
        if (item == null) {
            return row;
        }
        row.setBizTypeName(getBizTypeName(item.getBizType()));
        row.setBizNo(item.getBizNo());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemReceiptedPrice(item.getReceiptedPrice());
        row.setItemReceiptPrice(item.getReceiptPrice());
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
