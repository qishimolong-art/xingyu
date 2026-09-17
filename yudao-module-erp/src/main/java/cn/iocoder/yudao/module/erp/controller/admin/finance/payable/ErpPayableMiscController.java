package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

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
import cn.iocoder.yudao.module.erp.controller.admin.finance.ErpFinanceSettlementImportResolver.PayableMiscImportContext;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableMiscStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableMiscService;
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

@Tag(name = "ERP 其他应付")
@RestController
@RequestMapping("/erp/payable-misc")
@Validated
public class ErpPayableMiscController {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payable_misc";
    public static final Set<String> PAYABLE_MISC_IMPORT_TEMPLATE_FIELDS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("supplierName", "accountName", "deptName",
                    "amount", "remark", "fileUrl")));

    @Resource
    private ErpPayableMiscService payableMiscService;
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
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpFinanceSettlementImportResolver settlementImportResolver;

    @PostMapping("/create")
    @Operation(summary = "创建其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:create')")
    public CommonResult<Long> create(@Valid @RequestBody ErpPayableMiscSaveReqVO reqVO) {
        return success(payableMiscService.createPayableMisc(reqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:create')")
    public CommonResult<Long> createDraft(@RequestBody ErpPayableMiscDraftSaveReqVO reqVO) {
        return success(payableMiscService.createPayableMiscDraft(reqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:create') and " +
            "@ss.hasPermission('erp:payable-misc:update-status')")
    public CommonResult<Long> createAndSubmit(@Valid @RequestBody ErpPayableMiscSaveReqVO reqVO) {
        return success(payableMiscService.createAndSubmitPayableMisc(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpPayableMiscSaveReqVO reqVO) {
        payableMiscService.updatePayableMisc(reqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:update')")
    public CommonResult<Boolean> updateDraft(@RequestBody ErpPayableMiscDraftSaveReqVO reqVO) {
        payableMiscService.updatePayableMiscDraft(reqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:update') and " +
            "@ss.hasPermission('erp:payable-misc:update-status')")
    public CommonResult<Boolean> updateAndSubmit(@Valid @RequestBody ErpPayableMiscSaveReqVO reqVO) {
        payableMiscService.updateAndSubmitPayableMisc(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:update-status')")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        payableMiscService.submitPayableMisc(id);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改其他应付状态")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        payableMiscService.updatePayableMiscStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除其他应付")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        payableMiscService.deletePayableMisc(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:query')")
    public CommonResult<ErpPayableMiscRespVO> get(@RequestParam("id") Long id) {
        ErpPayableMiscDO db = payableMiscService.getPayableMisc(id);
        if (db == null) {
            return success(null);
        }
        ErpPayableMiscRespVO vo = BeanUtils.toBean(db, ErpPayableMiscRespVO.class);
        fillExtend(vo);
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, vo);
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获取其他应付分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:query')")
    public CommonResult<PageResult<ErpPayableMiscRespVO>> page(@Valid ErpPayableMiscPageReqVO reqVO) {
        return success(buildPageResult(payableMiscService.getPayableMiscPage(reqVO)));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获得其他应付可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:query')")
    public CommonResult<List<DeptSimpleRespVO>> getPayableMiscDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_payable_misc"));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获得其他应付可搜索部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getPayableMiscDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage("erp_payable_misc", pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获得其他应付用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(adminUserApi, pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出其他应付 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpPayableMiscPageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpPayableMiscRespVO> voPage =
                buildPageResult(payableMiscService.getPayableMiscPage(reqVO));
        List<ErpPayableMiscExportRespVO> rows =
                BeanUtils.toBean(voPage.getList(), ErpPayableMiscExportRespVO.class);
        rows.forEach(row -> row.setStatusName(formatStatusName(
                voPage.getList().stream().filter(item -> item.getNo().equals(row.getNo()))
                        .findFirst().map(ErpPayableMiscRespVO::getStatus).orElse(null))));
        ExcelUtils.write(response, "其他应付.xls", "数据", ErpPayableMiscExportRespVO.class, rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得其他应付导入模板")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "其他应付导入模板.xls", "其他应付",
                ErpPayableMiscImportExcelVO.class,
                Collections.singletonList(new ErpPayableMiscImportExcelVO()),
                PAYABLE_MISC_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-misc:import')")
    public CommonResult<ErpFinanceImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPayableMiscImportExcelVO> list = ExcelUtils.read(file, ErpPayableMiscImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        PayableMiscImportContext importContext = settlementImportResolver.buildPayableMiscContext();
        for (int i = 0; i < list.size(); i++) {
            ErpPayableMiscImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getSupplierName(), row.getAccountName(), row.getDeptName(),
                    row.getSupplierId(), row.getAccountId(), row.getDeptId(),
                    row.getAmount(), row.getRemark(), row.getFileUrl())) {
                continue;
            }
            try {
                payableMiscService.createPayableMisc(
                        settlementImportResolver.buildPayableMiscSaveReqVO(row, importContext));
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getSupplierName() == null ? String.valueOf(row.getSupplierId())
                        : row.getSupplierName(), failureReason(ex));
            }
        }
        return success(result);
    }

    private PageResult<ErpPayableMiscRespVO> buildPageResult(PageResult<ErpPayableMiscDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPayableMiscDO::getSupplierId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(pageResult.getList(), ErpPayableMiscDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = getUserMap(pageResult.getList());
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpPayableMiscDO::getDeptId));
        PageResult<ErpPayableMiscRespVO> result = BeanUtils.toBean(pageResult, ErpPayableMiscRespVO.class, vo ->
                fillExtend(vo, supplierMap, accountMap, userMap, deptMap));
        result.getList().forEach(item -> fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, item));
        return result;
    }

    private void fillExtend(ErpPayableMiscRespVO vo) {
        fillExtend(vo,
                vo.getSupplierId() == null ? Collections.emptyMap()
                        : Collections.singletonMap(vo.getSupplierId(), supplierService.getSupplier(vo.getSupplierId())),
                vo.getAccountId() == null ? Collections.emptyMap()
                        : Collections.singletonMap(vo.getAccountId(), accountService.getAccount(vo.getAccountId())),
                getUserMap(Collections.singletonList(BeanUtils.toBean(vo, ErpPayableMiscDO.class))),
                vo.getDeptId() == null ? Collections.emptyMap()
                        : Collections.singletonMap(vo.getDeptId(), deptApi.getDept(vo.getDeptId())));
    }

    private void fillExtend(ErpPayableMiscRespVO vo, Map<Long, ErpSupplierDO> supplierMap,
                            Map<Long, ErpAccountDO> accountMap, Map<Long, AdminUserRespDTO> userMap,
                            Map<Long, DeptRespDTO> deptMap) {
        MapUtils.findAndThen(supplierMap, vo.getSupplierId(), supplier -> {
            vo.setSupplierName(supplier.getName());
            vo.setSupplierContact(supplier.getContact());
            vo.setSupplierMobile(supplier.getMobile());
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

    private Map<Long, AdminUserRespDTO> getUserMap(List<ErpPayableMiscDO> rows) {
        Set<Long> userIds = new HashSet<>(convertListByFlatMap(rows,
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()),
                        NumberUtils.parseLong(item.getUpdater()))));
        return userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
    }

    private String formatStatusName(Integer status) {
        for (ErpPayableMiscStatusEnum statusEnum : ErpPayableMiscStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return null;
    }

}
