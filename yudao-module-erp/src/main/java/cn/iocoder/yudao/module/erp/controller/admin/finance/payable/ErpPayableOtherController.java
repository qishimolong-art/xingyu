package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

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
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.parseDate;

@Tag(name = "ERP 其他应付")
@RestController
@RequestMapping("/erp/payable-other")
@Validated
public class ErpPayableOtherController {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payable_other";
    public static final Set<String> PAYABLE_OTHER_IMPORT_TEMPLATE_FIELDS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("supplierId", "voucherNo", "settledAmount", "deptId",
                    "payableAmount", "project", "sourceType", "handlerId", "remark", "fileUrl")));

    @Resource
    private ErpPayableOtherService payableOtherService;
    @Resource
    private ErpSupplierService supplierService;
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
    @Operation(summary = "创建其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:create')")
    public CommonResult<Long> create(@Valid @RequestBody ErpPayableOtherSaveReqVO reqVO) {
        return success(payableOtherService.createPayableOther(reqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:create')")
    public CommonResult<Long> createDraft(@RequestBody ErpPayableOtherDraftSaveReqVO reqVO) {
        return success(payableOtherService.createPayableOtherDraft(reqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:create') and " +
            "@ss.hasPermission('erp:payable-other:update-status')")
    public CommonResult<Long> createAndSubmit(@Valid @RequestBody ErpPayableOtherSaveReqVO reqVO) {
        return success(payableOtherService.createAndSubmitPayableOther(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpPayableOtherSaveReqVO reqVO) {
        payableOtherService.updatePayableOther(reqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:update')")
    public CommonResult<Boolean> updateDraft(@RequestBody ErpPayableOtherDraftSaveReqVO reqVO) {
        payableOtherService.updatePayableOtherDraft(reqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:update') and " +
            "@ss.hasPermission('erp:payable-other:update-status')")
    public CommonResult<Boolean> updateAndSubmit(@Valid @RequestBody ErpPayableOtherSaveReqVO reqVO) {
        payableOtherService.updateAndSubmitPayableOther(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交其他应付草稿")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:update-status')")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        payableOtherService.submitPayableOther(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "更新其他应付单备注")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:update')")
    public CommonResult<Boolean> updateRemark(@Valid @RequestBody ErpFinanceUpdateRemarkReqVO reqVO) {
        payableOtherService.updatePayableOtherRemark(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改其他应付状态")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        payableOtherService.updatePayableOtherStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除其他应付")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:payable-other:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        payableOtherService.deletePayableOther(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:query')")
    public CommonResult<ErpPayableOtherRespVO> get(@RequestParam("id") Long id) {
        ErpPayableOtherDO db = payableOtherService.getPayableOther(id);
        if (db == null) {
            return success(null);
        }
        ErpPayableOtherRespVO vo = BeanUtils.toBean(db, ErpPayableOtherRespVO.class);
        fillExtend(vo);
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, vo);
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获取其他应付分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:query')")
    public CommonResult<PageResult<ErpPayableOtherRespVO>> page(@Valid ErpPayableOtherPageReqVO reqVO) {
        PageResult<ErpPayableOtherDO> pageResult = payableOtherService.getPayableOtherPage(reqVO);
        return success(maskPageResult(buildPageResult(pageResult)));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get payable other data permission dept simple list")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:query')")
    public CommonResult<java.util.List<DeptSimpleRespVO>> getPayableOtherDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_payable_other"));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出其他应付 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpPayableOtherPageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpPayableOtherRespVO> voPage = maskPageResult(buildPageResult(payableOtherService.getPayableOtherPage(reqVO)));
        ExcelUtils.write(response, "其他应付.xls", "数据", ErpPayableOtherExportRespVO.class,
                BeanUtils.toBean(voPage.getList(), ErpPayableOtherExportRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得其他应付导入模板")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "其他应付导入模板.xls", "其他应付",
                ErpPayableOtherImportExcelVO.class, Collections.singletonList(new ErpPayableOtherImportExcelVO()),
                PAYABLE_OTHER_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入其他应付")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:import')")
    public CommonResult<ErpFinanceImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        java.util.List<ErpPayableOtherImportExcelVO> list = ExcelUtils.read(file, ErpPayableOtherImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        for (int i = 0; i < list.size(); i++) {
            ErpPayableOtherImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getSupplierId(), row.getPayableAmount(), row.getRemark())) {
                continue;
            }
            try {
                ErpPayableOtherSaveReqVO reqVO = BeanUtils.toBean(row, ErpPayableOtherSaveReqVO.class);
                reqVO.setBizTime(parseDate(row.getBizTime(), LocalDate.now()));
                payableOtherService.createPayableOther(reqVO);
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getRemark(), failureReason(ex));
            }
        }
        return success(result);
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载其他应付导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:payable-other:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, "erp_payable_other", response);
    }

    private PageResult<ErpPayableOtherRespVO> buildPageResult(PageResult<ErpPayableOtherDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPayableOtherDO::getSupplierId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(pageResult.getList(),
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()),
                        NumberUtils.parseLong(item.getUpdater()))));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpPayableOtherDO::getDeptId));
        return BeanUtils.toBean(pageResult, ErpPayableOtherRespVO.class, vo -> {
            MapUtils.findAndThen(supplierMap, vo.getSupplierId(), supplier -> {
                vo.setSupplierName(supplier.getName());
                vo.setSupplierContact(supplier.getContact());
                vo.setSupplierMobile(supplier.getMobile());
            });
            MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getUpdater()), user -> vo.setUpdaterName(user.getNickname()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillAuditInfo(vo);
        });
    }

    private PageResult<ErpPayableOtherRespVO> maskPageResult(PageResult<ErpPayableOtherRespVO> pageResult) {
        pageResult.getList().forEach(item -> fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, item));
        return pageResult;
    }

    private void fillExtend(ErpPayableOtherRespVO vo) {
        if (vo.getSupplierId() != null) {
            ErpSupplierDO supplier = supplierService.getSupplier(vo.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getName());
                vo.setSupplierContact(supplier.getContact());
                vo.setSupplierMobile(supplier.getMobile());
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

    private void fillAuditInfo(ErpPayableOtherRespVO vo) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(vo.getStatus())) {
            return;
        }
        vo.setAuditorName(vo.getUpdaterName());
        vo.setAuditTime(vo.getUpdateTime());
    }

}
