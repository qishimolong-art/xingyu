package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.ErpFinanceSettlementImportResolver;
import cn.iocoder.yudao.module.erp.controller.admin.finance.ErpFinanceSettlementImportResolver.ReceivableMiscImportContext;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableMiscStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpMiscTransferOffsetConstants;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableMiscService;
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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.ErpFinanceSimplePageUtils.buildUserSimplePage;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;

@Tag(name = "ERP 其他应收")
@RestController
@RequestMapping("/erp/receivable-misc")
@Validated
public class ErpReceivableMiscController {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_misc";
    public static final Set<String> RECEIVABLE_MISC_IMPORT_TEMPLATE_FIELDS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("customerName", "accountName", "deptName",
                    "amount", "remark", "fileUrl")));

    @Resource
    private ErpReceivableMiscService receivableMiscService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpReceivableMiscMapper receivableMiscMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpFinanceSettlementImportResolver settlementImportResolver;

    @PostMapping("/create")
    @Operation(summary = "创建其他应收")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:create')")
    public CommonResult<Long> create(@Valid @RequestBody ErpReceivableMiscSaveReqVO reqVO) {
        return success(receivableMiscService.createReceivableMisc(reqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建其他应收草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:create')")
    public CommonResult<Long> createDraft(@RequestBody ErpReceivableMiscDraftSaveReqVO reqVO) {
        return success(receivableMiscService.createReceivableMiscDraft(reqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交其他应收")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:create') and " +
            "@ss.hasPermission('erp:receivable-misc:update-status')")
    public CommonResult<Long> createAndSubmit(@Valid @RequestBody ErpReceivableMiscSaveReqVO reqVO) {
        return success(receivableMiscService.createAndSubmitReceivableMisc(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改其他应收")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpReceivableMiscSaveReqVO reqVO) {
        receivableMiscService.updateReceivableMisc(reqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新其他应收草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:update')")
    public CommonResult<Boolean> updateDraft(@RequestBody ErpReceivableMiscDraftSaveReqVO reqVO) {
        receivableMiscService.updateReceivableMiscDraft(reqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交其他应收草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:update') and " +
            "@ss.hasPermission('erp:receivable-misc:update-status')")
    public CommonResult<Boolean> updateAndSubmit(@Valid @RequestBody ErpReceivableMiscSaveReqVO reqVO) {
        receivableMiscService.updateAndSubmitReceivableMisc(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交其他应收草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:update-status')")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        receivableMiscService.submitReceivableMisc(id);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改其他应收状态")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        receivableMiscService.updateReceivableMiscStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除其他应收")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        receivableMiscService.deleteReceivableMisc(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取其他应收")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:query')")
    public CommonResult<ErpReceivableMiscRespVO> get(@RequestParam("id") Long id) {
        ErpReceivableMiscDO db = receivableMiscService.getReceivableMisc(id);
        if (db == null) {
            return success(null);
        }
        ErpReceivableMiscRespVO vo = BeanUtils.toBean(db, ErpReceivableMiscRespVO.class);
        fillExtend(vo);
        fillSettlement(vo, Collections.singletonMap(db.getId(), db),
                receivableMiscMapper.selectOffsetAmountSumMapBySourceMiscIds(Collections.singleton(db.getId()),
                        ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE));
        maskForm(vo);
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获取其他应收分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:query')")
    public CommonResult<PageResult<ErpReceivableMiscRespVO>> page(@Valid ErpReceivableMiscPageReqVO reqVO) {
        return success(buildPageResult(receivableMiscService.getReceivableMiscPage(reqVO)));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获得其他应收可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:query')")
    public CommonResult<List<DeptSimpleRespVO>> getReceivableMiscDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_receivable_misc"));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获得其他应收可搜索部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getReceivableMiscDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage("erp_receivable_misc", pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获得其他应收用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(adminUserApi, pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出其他应收 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpReceivableMiscPageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpReceivableMiscRespVO> voPage =
                buildPageResult(receivableMiscService.getReceivableMiscPage(reqVO));
        List<ErpReceivableMiscExportRespVO> rows =
                BeanUtils.toBean(voPage.getList(), ErpReceivableMiscExportRespVO.class);
        rows.forEach(row -> row.setStatusName(formatStatusName(
                voPage.getList().stream().filter(item -> item.getNo().equals(row.getNo()))
                        .findFirst().map(ErpReceivableMiscRespVO::getStatus).orElse(null))));
        ExcelUtils.write(response, "其他应收.xls", "数据", ErpReceivableMiscExportRespVO.class, rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得其他应收导入模板")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "其他应收导入模板.xls", "其他应收",
                ErpReceivableMiscImportExcelVO.class,
                Collections.singletonList(new ErpReceivableMiscImportExcelVO()),
                RECEIVABLE_MISC_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入其他应收")
    @PreAuthorize("@ss.hasPermission('erp:receivable-misc:import')")
    public CommonResult<ErpFinanceImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpReceivableMiscImportExcelVO> list = ExcelUtils.read(file, ErpReceivableMiscImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        ReceivableMiscImportContext importContext = settlementImportResolver.buildReceivableMiscContext();
        for (int i = 0; i < list.size(); i++) {
            ErpReceivableMiscImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getCustomerName(), row.getAccountName(), row.getDeptName(),
                    row.getCustomerId(), row.getAccountId(), row.getDeptId(),
                    row.getAmount(), row.getRemark(), row.getFileUrl())) {
                continue;
            }
            try {
                receivableMiscService.createReceivableMisc(
                        settlementImportResolver.buildReceivableMiscSaveReqVO(row, importContext));
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getCustomerName() == null ? String.valueOf(row.getCustomerId())
                        : row.getCustomerName(), failureReason(ex));
            }
        }
        return success(result);
    }

    private PageResult<ErpReceivableMiscRespVO> buildPageResult(PageResult<ErpReceivableMiscDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpReceivableMiscDO::getCustomerId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(pageResult.getList(), ErpReceivableMiscDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = getUserMap(pageResult.getList());
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpReceivableMiscDO::getDeptId));
        Set<Long> ids = convertSet(pageResult.getList(), ErpReceivableMiscDO::getId);
        Map<Long, BigDecimal> settledMap = receivableMiscMapper.selectOffsetAmountSumMapBySourceMiscIds(
                ids, ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE);
        Map<Long, ErpReceivableMiscDO> rowMap = pageResult.getList().stream()
                .collect(java.util.stream.Collectors.toMap(ErpReceivableMiscDO::getId, item -> item));
        PageResult<ErpReceivableMiscRespVO> result = BeanUtils.toBean(pageResult, ErpReceivableMiscRespVO.class, vo ->
                fillExtend(vo, customerMap, accountMap, userMap, deptMap));
        result.getList().forEach(item -> fillSettlement(item, rowMap, settledMap));
        result.getList().forEach(this::maskForm);
        return result;
    }

    private void fillSettlement(ErpReceivableMiscRespVO vo, Map<Long, ErpReceivableMiscDO> rowMap,
                                Map<Long, BigDecimal> settledMap) {
        BigDecimal settledAmount = amount(settledMap.get(vo.getId()));
        ErpReceivableMiscDO row = rowMap.get(vo.getId());
        BigDecimal originalAmount = amount(vo.getAmount());
        if (row != null) {
            originalAmount = amount(row.getAmount());
            vo.setGeneratedOffset(isGeneratedOffset(row));
        }
        vo.setSettledAmount(settledAmount);
        vo.setBalanceAmount(originalAmount.subtract(settledAmount));
    }

    private boolean isGeneratedOffset(ErpReceivableMiscDO row) {
        return ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE.equals(row.getSourceType())
                && row.getSourceItemId() != null;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void maskForm(ErpReceivableMiscRespVO vo) {
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, vo);
        if (vo.getAmount() == null) {
            vo.setSettledAmount(null);
            vo.setBalanceAmount(null);
        }
    }

    private void fillExtend(ErpReceivableMiscRespVO vo) {
        fillExtend(vo,
                vo.getCustomerId() == null ? Collections.emptyMap()
                        : Collections.singletonMap(vo.getCustomerId(), customerService.getCustomer(vo.getCustomerId())),
                vo.getAccountId() == null ? Collections.emptyMap()
                        : Collections.singletonMap(vo.getAccountId(), accountService.getAccount(vo.getAccountId())),
                getUserMap(Collections.singletonList(BeanUtils.toBean(vo, ErpReceivableMiscDO.class))),
                vo.getDeptId() == null ? Collections.emptyMap()
                        : Collections.singletonMap(vo.getDeptId(), deptApi.getDept(vo.getDeptId())));
    }

    private void fillExtend(ErpReceivableMiscRespVO vo, Map<Long, ErpCustomerDO> customerMap,
                            Map<Long, ErpAccountDO> accountMap, Map<Long, AdminUserRespDTO> userMap,
                            Map<Long, DeptRespDTO> deptMap) {
        MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> {
            vo.setCustomerName(customer.getName());
            vo.setCustomerContact(customer.getContact());
            vo.setCustomerMobile(customer.getMobile());
        });
        MapUtils.findAndThen(accountMap, vo.getAccountId(), account -> vo.setAccountName(account.getName()));
        MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
        MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
        MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getUpdater()), user -> vo.setUpdaterName(user.getNickname()));
        MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
        if (ErpAuditStatus.APPROVE.getStatus().equals(vo.getStatus())) {
            vo.setAuditorName(vo.getUpdaterName());
            vo.setAuditTime(vo.getUpdateTime());
        }
    }

    private Map<Long, AdminUserRespDTO> getUserMap(List<ErpReceivableMiscDO> rows) {
        Set<Long> userIds = new HashSet<>(convertListByFlatMap(rows,
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()),
                        NumberUtils.parseLong(item.getUpdater()))));
        return userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
    }

    private String formatStatusName(Integer status) {
        for (ErpReceivableMiscStatusEnum statusEnum : ErpReceivableMiscStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return null;
    }

}
