package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
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
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableOtherStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService;
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
import java.time.LocalDate;
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
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.parseDate;

@Tag(name = "ERP 应收调账")
@RestController
@RequestMapping("/erp/receivable-other")
@Validated
public class ErpReceivableOtherController {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_other";
    public static final Set<String> RECEIVABLE_OTHER_IMPORT_TEMPLATE_FIELDS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("customerId", "voucherNo", "settledAmount", "deptId",
                    "receivableAmount", "project", "sourceType", "handlerId", "receivableType", "costAmount",
                    "remark", "isPaperNote", "paperNoteDesc", "sourceNo", "fileUrl")));

    @Resource
    private ErpReceivableOtherService receivableOtherService;
    @Resource
    private ErpCustomerService customerService;
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
    @Operation(summary = "创建应收调账")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:create')")
    public CommonResult<Long> create(@Valid @RequestBody ErpReceivableOtherSaveReqVO reqVO) {
        return success(receivableOtherService.createReceivableOther(reqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建应收调账草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:create')")
    public CommonResult<Long> createDraft(@RequestBody ErpReceivableOtherDraftSaveReqVO reqVO) {
        return success(receivableOtherService.createReceivableOtherDraft(reqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交应收调账")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:create') and " +
            "@ss.hasPermission('erp:receivable-other:update-status')")
    public CommonResult<Long> createAndSubmit(@Valid @RequestBody ErpReceivableOtherSaveReqVO reqVO) {
        return success(receivableOtherService.createAndSubmitReceivableOther(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改应收调账")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpReceivableOtherSaveReqVO reqVO) {
        receivableOtherService.updateReceivableOther(reqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新应收调账草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:update')")
    public CommonResult<Boolean> updateDraft(@RequestBody ErpReceivableOtherDraftSaveReqVO reqVO) {
        receivableOtherService.updateReceivableOtherDraft(reqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交应收调账草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:update') and " +
            "@ss.hasPermission('erp:receivable-other:update-status')")
    public CommonResult<Boolean> updateAndSubmit(@Valid @RequestBody ErpReceivableOtherSaveReqVO reqVO) {
        receivableOtherService.updateAndSubmitReceivableOther(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交应收调账草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:update-status')")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        receivableOtherService.submitReceivableOther(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "更新应收调账单备注")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:update')")
    public CommonResult<Boolean> updateRemark(@Valid @RequestBody ErpFinanceUpdateRemarkReqVO reqVO) {
        receivableOtherService.updateReceivableOtherRemark(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改应收调账状态")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        receivableOtherService.updateReceivableOtherStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除应收调账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        receivableOtherService.deleteReceivableOther(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取应收调账")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:query')")
    public CommonResult<ErpReceivableOtherRespVO> get(@RequestParam("id") Long id) {
        ErpReceivableOtherDO db = receivableOtherService.getReceivableOther(id);
        if (db == null) {
            return success(null);
        }
        ErpReceivableOtherRespVO vo = BeanUtils.toBean(db, ErpReceivableOtherRespVO.class);
        fillExtend(vo);
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, vo);
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获取应收调账分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:query')")
    public CommonResult<PageResult<ErpReceivableOtherRespVO>> page(@Valid ErpReceivableOtherPageReqVO reqVO) {
        PageResult<ErpReceivableOtherDO> pageResult = receivableOtherService.getReceivableOtherPage(reqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpReceivableOtherDO::getCustomerId));
        Map<Long, AdminUserRespDTO> userMap = getUserMap(pageResult.getList(), customerMap);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpReceivableOtherDO::getDeptId));
        return success(maskPageResult(BeanUtils.toBean(pageResult, ErpReceivableOtherRespVO.class, vo -> {
            fillExtend(vo, customerMap, userMap, deptMap);
        })));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get receivable other data permission dept simple list")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:query')")
    public CommonResult<List<DeptSimpleRespVO>> getReceivableOtherDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_receivable_other"));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "Get receivable other data permission dept simple page")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getReceivableOtherDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage("erp_receivable_other", pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "Get user page for receivable other filter")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(adminUserApi, pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出应收调账 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpReceivableOtherPageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpReceivableOtherDO> pageResult = receivableOtherService.getReceivableOtherPage(reqVO);
        PageResult<ErpReceivableOtherRespVO> voPage = buildPageResult(pageResult);
        List<ErpReceivableOtherExportRespVO> rows =
                BeanUtils.toBean(voPage.getList(), ErpReceivableOtherExportRespVO.class);
        rows.forEach(row -> {
            row.setIncreaseReceivableAmount(row.getReceivableAmount());
            row.setStatusName(formatStatusName(row.getStatus()));
        });
        ExcelUtils.write(response, "应收调账.xls", "数据", ErpReceivableOtherExportRespVO.class,
                rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得应收调账导入模板")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "应收调账导入模板.xls", "应收调账",
                ErpReceivableOtherImportExcelVO.class,
                Collections.singletonList(new ErpReceivableOtherImportExcelVO()),
                RECEIVABLE_OTHER_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入应收调账")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:import')")
    public CommonResult<ErpFinanceImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        java.util.List<ErpReceivableOtherImportExcelVO> list = ExcelUtils.read(file, ErpReceivableOtherImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        for (int i = 0; i < list.size(); i++) {
            ErpReceivableOtherImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getCustomerId(), row.getReceivableAmount(), row.getRemark())) {
                continue;
            }
            try {
                ErpReceivableOtherSaveReqVO reqVO = BeanUtils.toBean(row, ErpReceivableOtherSaveReqVO.class);
                reqVO.setBizTime(parseDate(row.getBizTime(), LocalDate.now()));
                receivableOtherService.createReceivableOther(reqVO);
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getRemark(), failureReason(ex));
            }
        }
        return success(result);
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载应收调账导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, "erp_receivable_other", response);
    }

    private void fillExtend(ErpReceivableOtherRespVO vo) {
        if (vo.getCustomerId() != null) {
            ErpCustomerDO customer = customerService.getCustomer(vo.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getName());
                vo.setCustomerContact(customer.getContact());
                vo.setCustomerMobile(customer.getMobile());
                vo.setSaleUserId(customer.getSaleUserId());
                if (customer.getSaleUserId() != null) {
                    AdminUserRespDTO saleUser = adminUserApi.getUser(customer.getSaleUserId());
                    if (saleUser != null) {
                        vo.setSaleUserName(saleUser.getNickname());
                    }
                }
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
        fillAuditInfo(vo);
    }

    private PageResult<ErpReceivableOtherRespVO> buildPageResult(PageResult<ErpReceivableOtherDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpReceivableOtherDO::getCustomerId));
        Map<Long, AdminUserRespDTO> userMap = getUserMap(pageResult.getList(), customerMap);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpReceivableOtherDO::getDeptId));
        return maskPageResult(BeanUtils.toBean(pageResult, ErpReceivableOtherRespVO.class, vo -> {
            fillExtend(vo, customerMap, userMap, deptMap);
        }));
    }

    private PageResult<ErpReceivableOtherRespVO> maskPageResult(PageResult<ErpReceivableOtherRespVO> pageResult) {
        pageResult.getList().forEach(item -> fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, item));
        return pageResult;
    }

    private void fillExtend(ErpReceivableOtherRespVO vo, Map<Long, ErpCustomerDO> customerMap,
                            Map<Long, AdminUserRespDTO> userMap, Map<Long, DeptRespDTO> deptMap) {
        MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> {
            vo.setCustomerName(customer.getName());
            vo.setCustomerContact(customer.getContact());
            vo.setCustomerMobile(customer.getMobile());
            vo.setSaleUserId(customer.getSaleUserId());
            MapUtils.findAndThen(userMap, customer.getSaleUserId(),
                    user -> vo.setSaleUserName(user.getNickname()));
        });
        MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
        MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
        MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getUpdater()), user -> vo.setUpdaterName(user.getNickname()));
        MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
        fillAuditInfo(vo);
    }

    private Map<Long, AdminUserRespDTO> getUserMap(List<ErpReceivableOtherDO> rows,
                                                    Map<Long, ErpCustomerDO> customerMap) {
        Set<Long> userIds = new HashSet<>(convertListByFlatMap(rows,
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()),
                        NumberUtils.parseLong(item.getUpdater()))));
        customerMap.values().stream().map(ErpCustomerDO::getSaleUserId)
                .filter(java.util.Objects::nonNull).forEach(userIds::add);
        return adminUserApi.getUserMap(userIds);
    }

    private void fillAuditInfo(ErpReceivableOtherRespVO vo) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(vo.getStatus())) {
            return;
        }
        vo.setAuditorName(vo.getUpdaterName());
        vo.setAuditTime(vo.getUpdateTime());
    }

    private String formatStatusName(Integer status) {
        for (ErpReceivableOtherStatusEnum statusEnum : ErpReceivableOtherStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return null;
    }
}
