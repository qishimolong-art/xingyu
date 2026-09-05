package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.parseDate;

@Tag(name = "ERP 费用支付")
@RestController
@RequestMapping("/erp/payable-expense")
@Validated
public class ErpPayableExpenseController {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payable_expense";
    public static final Set<String> PAYABLE_EXPENSE_IMPORT_TEMPLATE_FIELDS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("settleMethod", "accountId", "voucherNo", "expenseType", "deptId",
                    "handlerId", "party", "relatedBiz", "docType", "remark", "fileUrl", "itemName", "amount",
                    "invoiceNo", "itemParty", "itemDeptId", "itemBizDate", "itemHandlerId", "qty",
                    "expenseCategory", "itemRemark")));

    @Resource
    private ErpPayableExpenseService payableExpenseService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:create')")
    public CommonResult<Long> create(@Valid @RequestBody ErpPayableExpenseSaveReqVO reqVO) {
        return success(payableExpenseService.createPayableExpense(reqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建费用支付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:create')")
    public CommonResult<Long> createDraft(@RequestBody ErpPayableExpenseDraftSaveReqVO reqVO) {
        return success(payableExpenseService.createPayableExpenseDraft(reqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:create')"
            + " and @ss.hasPermission('erp:payable-expense:update-status')")
    public CommonResult<Long> createAndSubmit(@Valid @RequestBody ErpPayableExpenseSaveReqVO reqVO) {
        return success(payableExpenseService.createAndSubmitPayableExpense(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpPayableExpenseSaveReqVO reqVO) {
        payableExpenseService.updatePayableExpense(reqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "修改费用支付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update')")
    public CommonResult<Boolean> updateDraft(@RequestBody ErpPayableExpenseDraftSaveReqVO reqVO) {
        payableExpenseService.updatePayableExpenseDraft(reqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "修改并提交费用支付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update')"
            + " and @ss.hasPermission('erp:payable-expense:update-status')")
    public CommonResult<Boolean> updateAndSubmit(@Valid @RequestBody ErpPayableExpenseSaveReqVO reqVO) {
        payableExpenseService.updateAndSubmitPayableExpense(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交费用支付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update-status')")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        payableExpenseService.submitPayableExpense(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "更新支出单备注")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update')")
    public CommonResult<Boolean> updateRemark(@Valid @RequestBody ErpFinanceUpdateRemarkReqVO reqVO) {
        payableExpenseService.updatePayableExpenseRemark(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改费用支付状态")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        payableExpenseService.updatePayableExpenseStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除费用支付")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:delete')")
    public CommonResult<Boolean> delete(@RequestParam("ids") List<Long> ids) {
        payableExpenseService.deletePayableExpense(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:query')")
    public CommonResult<ErpPayableExpenseRespVO> get(@RequestParam("id") Long id,
                                                     @RequestParam(value = "includeItems", defaultValue = "true")
                                                     Boolean includeItems) {
        ErpPayableExpenseDO db = payableExpenseService.getPayableExpense(id);
        if (db == null) {
            return success(null);
        }
        ErpPayableExpenseRespVO vo = BeanUtils.toBean(db, ErpPayableExpenseRespVO.class);
        vo.setItems(Boolean.FALSE.equals(includeItems) ? Collections.emptyList()
                : buildPayableExpenseItems(payableExpenseService.getPayableExpenseItemListByExpenseId(id)));
        fillExtend(vo);
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, vo);
        return success(vo);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获取费用支付明细分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:query')")
    public CommonResult<PageResult<ErpPayableExpenseRespVO.Item>> itemPage(
            @Valid ErpPayableExpenseItemPageReqVO pageReqVO) {
        PageResult<ErpPayableExpenseItemDO> pageResult = payableExpenseService.getPayableExpenseItemPage(pageReqVO);
        PageResult<ErpPayableExpenseRespVO.Item> result = BeanUtils.toBean(pageResult,
                ErpPayableExpenseRespVO.Item.class);
        result.setList(buildPayableExpenseItems(pageResult.getList()));
        if (!Boolean.FALSE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, result.getList());
        }
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获取费用支付分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:query')")
    public CommonResult<PageResult<ErpPayableExpenseRespVO>> page(@Valid ErpPayableExpensePageReqVO reqVO) {
        PageResult<ErpPayableExpenseDO> pageResult = payableExpenseService.getPayableExpensePage(reqVO);
        return success(maskPageResult(buildPageResult(pageResult)));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get payable expense data permission dept simple list")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:query')")
    public CommonResult<List<DeptSimpleRespVO>> getPayableExpenseDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_payable_expense"));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出费用支付 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpPayableExpensePageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpPayableExpenseRespVO> voPage = maskPageResult(
                buildPageResult(payableExpenseService.getPayableExpensePage(reqVO)));
        List<ErpPayableExpenseExportRespVO> rows = new ArrayList<>();
        for (ErpPayableExpenseRespVO expense : voPage.getList()) {
            List<ErpPayableExpenseRespVO.Item> items = expense.getItems() == null ? Collections.emptyList() : expense.getItems();
            if (CollUtil.isEmpty(items)) {
                rows.add(buildExportRow(expense, null, true));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                rows.add(buildExportRow(expense, items.get(i), i == 0));
            }
        }
        ExcelUtils.write(response, "费用支付.xls", "数据", ErpPayableExpenseExportRespVO.class, rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得费用支付导入模板")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "费用支付导入模板.xls", "费用支付",
                ErpPayableExpenseImportExcelVO.class,
                Collections.singletonList(new ErpPayableExpenseImportExcelVO()),
                PAYABLE_EXPENSE_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:import')")
    public CommonResult<ErpFinanceImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPayableExpenseImportExcelVO> list = ExcelUtils.read(file, ErpPayableExpenseImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        for (int i = 0; i < list.size(); i++) {
            ErpPayableExpenseImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getSettleMethod(), row.getAccountId(),
                    row.getExpenseType(), row.getHandlerId(), row.getItemName(), row.getAmount())) {
                continue;
            }
            try {
                ErpPayableExpenseSaveReqVO reqVO = BeanUtils.toBean(row, ErpPayableExpenseSaveReqVO.class);
                reqVO.setBizTime(parseDate(row.getBizTime(), LocalDate.now()));
                ErpPayableExpenseSaveReqVO.Item item = new ErpPayableExpenseSaveReqVO.Item();
                item.setItemName(row.getItemName());
                item.setAmount(row.getAmount());
                item.setInvoiceNo(row.getInvoiceNo());
                item.setParty(row.getItemParty());
                item.setDeptId(row.getItemDeptId());
                item.setBizDate(parseDate(row.getItemBizDate(), null));
                item.setHandlerId(row.getItemHandlerId());
                item.setQty(row.getQty());
                item.setExpenseCategory(row.getExpenseCategory());
                item.setRemark(row.getItemRemark());
                reqVO.setItems(Collections.singletonList(item));
                payableExpenseService.createPayableExpense(reqVO);
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getItemName(), failureReason(ex));
            }
        }
        return success(result);
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载费用支付导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, "erp_payable_expense", response);
    }

    private PageResult<ErpPayableExpenseRespVO> buildPageResult(PageResult<ErpPayableExpenseDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPayableExpenseItemDO> itemList = payableExpenseService.getPayableExpenseItemListByExpenseIds(
                convertSet(pageResult.getList(), ErpPayableExpenseDO::getId));
        Map<Long, List<ErpPayableExpenseItemDO>> itemMap = convertMultiMap(itemList,
                ErpPayableExpenseItemDO::getExpenseId);
        Set<Long> accountIds = convertSet(pageResult.getList(), ErpPayableExpenseDO::getAccountId);
        accountIds.remove(null);
        Map<Long, ErpAccountDO> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap() : accountService.getAccountMap(accountIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(pageResult.getList(),
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()),
                        NumberUtils.parseLong(item.getUpdater()))));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(CollectionUtils.convertSet(pageResult.getList(),
                ErpPayableExpenseDO::getDeptId));
        return BeanUtils.toBean(pageResult, ErpPayableExpenseRespVO.class, vo -> {
            vo.setItems(buildPayableExpenseItems(itemMap.get(vo.getId())));
            MapUtils.findAndThen(accountMap, vo.getAccountId(), account -> vo.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getUpdater()), user -> vo.setUpdaterName(user.getNickname()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillItemExtend(vo);
        });
    }

    private PageResult<ErpPayableExpenseRespVO> maskPageResult(PageResult<ErpPayableExpenseRespVO> pageResult) {
        pageResult.getList().forEach(item -> fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, item));
        return pageResult;
    }

    private List<ErpPayableExpenseRespVO.Item> buildPayableExpenseItems(List<ErpPayableExpenseItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(CollectionUtils.convertSet(items,
                ErpPayableExpenseItemDO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(CollectionUtils.convertSet(items,
                ErpPayableExpenseItemDO::getHandlerId));
        return BeanUtils.toBean(items, ErpPayableExpenseRespVO.Item.class, item -> {
            MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
            MapUtils.findAndThen(userMap, item.getHandlerId(), user -> item.setHandlerName(user.getNickname()));
        });
    }

    private ErpPayableExpenseExportRespVO buildExportRow(ErpPayableExpenseRespVO expense,
                                                         ErpPayableExpenseRespVO.Item item,
                                                         boolean fillMainFields) {
        ErpPayableExpenseExportRespVO row = fillMainFields
                ? BeanUtils.toBean(expense, ErpPayableExpenseExportRespVO.class)
                : new ErpPayableExpenseExportRespVO();
        if (item == null) {
            return row;
        }
        row.setItemName(item.getItemName());
        row.setItemAmount(item.getAmount());
        row.setItemInvoiceNo(item.getInvoiceNo());
        row.setItemParty(item.getParty());
        row.setItemDeptName(item.getDeptName());
        row.setItemBizDate(item.getBizDate());
        row.setItemHandlerName(item.getHandlerName());
        row.setItemQty(item.getQty());
        row.setItemExpenseCategory(item.getExpenseCategory());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private void fillExtend(ErpPayableExpenseRespVO vo) {
        if (vo.getAccountId() != null) {
            ErpAccountDO account = accountService.getAccount(vo.getAccountId());
            if (account != null) {
                vo.setAccountName(account.getName());
            }
        }
        if (vo.getHandlerId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(vo.getHandlerId());
            if (user != null) {
                vo.setHandlerName(user.getNickname());
            }
        }
        if (vo.getCreator() != null) {
            try {
                AdminUserRespDTO user = adminUserApi.getUser(Long.parseLong(vo.getCreator()));
                if (user != null) {
                    vo.setCreatorName(user.getNickname());
                }
            } catch (Exception ignored) {
            }
        }
        if (vo.getUpdater() != null) {
            try {
                AdminUserRespDTO user = adminUserApi.getUser(Long.parseLong(vo.getUpdater()));
                if (user != null) {
                    vo.setUpdaterName(user.getNickname());
                }
            } catch (Exception ignored) {
            }
        }
        if (vo.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(vo.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        fillItemExtend(vo);
    }

    private void fillItemExtend(ErpPayableExpenseRespVO vo) {
        if (CollUtil.isEmpty(vo.getItems())) {
            return;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(CollectionUtils.convertSet(vo.getItems(),
                ErpPayableExpenseRespVO.Item::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(CollectionUtils.convertSet(vo.getItems(),
                ErpPayableExpenseRespVO.Item::getHandlerId));
        vo.getItems().forEach(item -> {
            MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
            MapUtils.findAndThen(userMap, item.getHandlerId(), user -> item.setHandlerName(user.getNickname()));
        });
    }

}
