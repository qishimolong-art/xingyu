package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpArchiveMergeReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierBatchDisableReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierDeptDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierDeptDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierBusinessInfoMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpExportCaptchaService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerBusinessLicenseOcrService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;

@Tag(name = "管理后台 - ERP 供应商")
@RestController
@RequestMapping("/erp/supplier")
@Validated
public class ErpSupplierController {

    private static final String FIELD_PERMISSION_MODULE = "erp_supplier";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;
    @Resource
    private ErpCustomerBusinessLicenseOcrService customerBusinessLicenseOcrService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;
    @Resource
    private ErpExportCaptchaService exportCaptchaService;
    @Resource
    private ErpSupplierBusinessInfoMapper supplierBusinessInfoMapper;

    @PostMapping("/create")
    @Operation(summary = "创建供应商")
    @PreAuthorize("@ss.hasPermission('erp:supplier:create')")
    public CommonResult<Long> createSupplier(@Valid @RequestBody ErpSupplierSaveReqVO createReqVO) {
        return success(supplierService.createSupplier(createReqVO));
    }

    @PostMapping("/business-license/recognize")
    @Operation(summary = "识别供应商营业执照")
    @PreAuthorize("@ss.hasPermission('erp:supplier:create')")
    public CommonResult<ErpCustomerBusinessLicenseOcrRespVO> recognizeBusinessLicense(
            @Valid @RequestBody ErpCustomerBusinessLicenseOcrReqVO reqVO) {
        return success(customerBusinessLicenseOcrService.recognize(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplier(@Valid @RequestBody ErpSupplierSaveReqVO updateReqVO) {
        supplierService.updateSupplier(updateReqVO);
        return success(true);
    }

    @GetMapping("/dept-distribution")
    @Operation(summary = "获得供应商部门分配")
    @Parameter(name = "id", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:supplier:dept-distribute')")
    public CommonResult<ErpSupplierDeptDistributionRespVO> getSupplierDeptDistribution(@RequestParam("id") Long id) {
        return success(supplierService.getSupplierDeptDistribution(id));
    }

    @PutMapping("/dept-distribution")
    @Operation(summary = "更新供应商部门分配")
    @PreAuthorize("@ss.hasPermission('erp:supplier:dept-distribute')")
    public CommonResult<Boolean> updateSupplierDeptDistribution(
            @Valid @RequestBody ErpSupplierDeptDistributionSaveReqVO reqVO) {
        supplierService.updateSupplierDeptDistribution(reqVO);
        return success(true);
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量编辑供应商")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> batchUpdateSupplier(@Valid @RequestBody ErpSupplierBatchUpdateReqVO reqVO) {
        supplierService.batchUpdateSupplier(reqVO);
        return success(true);
    }

    @PutMapping("/batch-disable")
    @Operation(summary = "批量停用供应商")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> batchDisableSupplier(@Valid @RequestBody ErpSupplierBatchDisableReqVO reqVO) {
        supplierService.batchDisableSupplier(reqVO.getIds());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新供应商开启状态（停用/启用）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @Parameter(name = "status", description = "状态（0=开启，1=停用）", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierStatus(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status) {
        supplierService.updateSupplierStatus(id, status);
        return success(true);
    }

    @PutMapping("/merge")
    @Operation(summary = "合并供应商")
    @PreAuthorize("@ss.hasPermission('erp:supplier:merge')")
    public CommonResult<Boolean> mergeSupplier(@Valid @RequestBody ErpArchiveMergeReqVO reqVO) {
        supplierService.mergeSupplier(reqVO.getSourceId(), reqVO.getKeepId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:delete')")
    public CommonResult<Boolean> deleteSupplier(@RequestParam("id") Long id) {
        supplierService.deleteSupplier(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除供应商")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:delete')")
    public CommonResult<Boolean> deleteSupplierList(@RequestParam("ids") List<Long> ids) {
        supplierService.deleteSupplierList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierRespVO> getSupplier(@RequestParam("id") Long id) {
        ErpSupplierDO supplier = supplierService.getSupplier(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        ErpSupplierRespVO respVO = BeanUtils.toBean(supplier, ErpSupplierRespVO.class);
        fillSupplierExtra(Collections.singletonList(respVO));
        fieldPermissionMasker.mask("erp_supplier", respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商分页")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<PageResult<ErpSupplierRespVO>> getSupplierPage(@Valid ErpSupplierPageReqVO pageReqVO) {
        PageResult<ErpSupplierDO> pageResult = supplierService.getSupplierPage(pageReqVO);
        PageResult<ErpSupplierRespVO> respPage = BeanUtils.toBean(pageResult, ErpSupplierRespVO.class);
        fillSupplierExtra(respPage.getList());
        maskSupplierFields(respPage.getList());
        fillBusinessInfoSynced(respPage.getList());
        return success(respPage);
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "鑾峰緱褰撳墠鐢ㄦ埛鍙煡璇㈢殑渚涘簲鍟嗛儴闂ㄧ簿绠€鍒楄〃")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(supplierDeptPermissionService.getDataPermissionDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得供应商精简列表", description = "只包含被开启的供应商，主要用于前端的下拉选项")
    public CommonResult<List<ErpSupplierRespVO>> getSupplierSimpleList() {
        List<ErpSupplierDO> list = supplierService.getSupplierListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, supplier -> new ErpSupplierRespVO().setId(supplier.getId()).setName(supplier.getName())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出供应商 Excel")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSupplierExcel(@Valid ErpSupplierPageReqVO pageReqVO,
                                    @RequestParam(value = "fields", required = false) String fields,
                                    @RequestParam(value = "captchaCode", required = false) String captchaCode,
                                    @RequestParam(value = "verifyCode", required = false) String verifyCode,
                                    HttpServletResponse response) throws IOException {
        exportCaptchaService.validate(captchaCode, verifyCode);
        List<ErpSupplierDO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = supplierService.getSupplierList(pageReqVO.getIds());
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = supplierService.getSupplierPage(pageReqVO).getList();
        }
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpSupplierRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        // 导出 Excel
        ExcelUtils.write(response, "供应商.xls", "数据", ErpSupplierRespVO.class,
                        buildSupplierRespList(list), includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get supplier export fields")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getSupplierExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpSupplierRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得供应商导入模板")
    @PreAuthorize("@ss.hasPermission('erp:supplier:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        Set<String> requiredFields = convertSet(
                fieldConfigService.getFieldConfigListByModule(ErpFieldConfigModuleEnum.SUPPLIER.getKey()),
                ErpFieldConfigDO::getFieldName,
                fieldConfig -> Boolean.TRUE.equals(fieldConfig.getRequired()));
        ExcelUtils.writeImportTemplate(response, "供应商导入模板.xls", "供应商", ErpSupplierImportExcelVO.class,
                Collections.singletonList(new ErpSupplierImportExcelVO()), null, requiredFields);
    }

    @PostMapping("/import")
    @Operation(summary = "导入供应商")
    @PreAuthorize("@ss.hasPermission('erp:supplier:import')")
    public CommonResult<Boolean> importSupplier(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSupplierImportExcelVO> list = ExcelUtils.read(file, ErpSupplierImportExcelVO.class);
        supplierService.importSupplierList(list);
        return success(true);
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "system");
        map.put("name", "main");
        map.put("shortName", "main");
        map.put("code", "main");
        map.put("deptName", "main");
        map.put("deptNames", "main");
        map.put("allowMultiDept", "main");
        map.put("contact", "main");
        map.put("mobile", "main");
        map.put("telephone", "main");
        map.put("email", "main");
        map.put("status", "main");
        map.put("sort", "main");
        map.put("remark", "main");
        map.put("region", "main");
        map.put("category", "main");
        map.put("purchaser", "main");
        map.put("settleMethod", "main");
        map.put("transportMethod", "main");
        map.put("freightType", "main");
        map.put("logisticsCompany", "main");
        map.put("invoiceType", "invoice_info");
        map.put("taxpayerId", "invoice_info");
        map.put("invoiceBank", "invoice_info");
        map.put("invoiceBankAccount", "invoice_info");
        map.put("invoiceAddress", "invoice_info");
        map.put("invoicePhone", "invoice_info");
        map.put("invoiceCompany", "invoice_info");
        map.put("account", "finance_info");
        map.put("bankName", "finance_info");
        map.put("bankAccount", "finance_info");
        map.put("bankAddress", "finance_info");
        map.put("taxNo", "finance_info");
        map.put("taxPercent", "finance_info");
        map.put("financePhone", "finance_info");
        map.put("memberCode", "finance_info");
        map.put("legalPerson", "finance_info");
        map.put("creditCode", "finance_info");
        map.put("createDeptName", "system");
        map.put("createTime", "system");
        map.put("creatorName", "system");
        map.put("updaterName", "system");
        map.put("updateTime", "system");
        return map;
    }

    private List<ErpSupplierRespVO> buildSupplierRespList(List<ErpSupplierDO> list) {
        List<ErpSupplierRespVO> respList = BeanUtils.toBean(list, ErpSupplierRespVO.class);
        fillSupplierExtra(respList);
        return respList;
    }

    private void fillSupplierExtra(List<ErpSupplierRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, List<Long>> supplierDeptMap = supplierService.getSupplierDeptMap(convertSet(list, ErpSupplierRespVO::getId));
        Set<Long> deptIds = new HashSet<>(convertSet(list, ErpSupplierRespVO::getDeptId));
        supplierDeptMap.values().forEach(ids -> {
            if (ids != null) {
                deptIds.addAll(ids);
            }
        });
        Set<Long> userIds = new HashSet<>();
        list.forEach(supplier -> {
            addUserId(userIds, supplier.getCreator());
            addUserId(userIds, supplier.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        list.forEach(supplier -> {
            Long creatorId = parseUserId(supplier.getCreator());
            AdminUserRespDTO creator = creatorId == null ? null : userMap.get(creatorId);
            if (supplier.getCreateDeptId() == null && creator != null) {
                supplier.setCreateDeptId(creator.getDeptId());
            }
            if (supplier.getCreateDeptId() != null) {
                deptIds.add(supplier.getCreateDeptId());
            }
        });
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = DataPermissionUtils.executeIgnore(() -> deptApi.getDeptMap(deptIds));
        list.forEach(supplier -> {
            List<Long> currentDeptIds = supplierDeptMap.getOrDefault(supplier.getId(), Collections.emptyList());
            supplier.setDeptIds(currentDeptIds);
            supplier.setDeptNames(currentDeptIds.stream()
                    .map(deptMap::get)
                    .filter(dept -> dept != null && dept.getName() != null)
                    .map(DeptRespDTO::getName)
                    .collect(java.util.stream.Collectors.joining("、")));
            MapUtils.findAndThen(deptMap, supplier.getDeptId(), dept -> supplier.setDeptName(dept.getName()));
            MapUtils.findAndThen(deptMap, supplier.getCreateDeptId(),
                    dept -> supplier.setCreateDeptName(dept.getName()));
            Long creatorId = parseUserId(supplier.getCreator());
            if (creatorId != null) {
                MapUtils.findAndThen(userMap, creatorId, user -> supplier.setCreatorName(user.getNickname()));
            }
            Long updaterId = parseUserId(supplier.getUpdater());
            if (updaterId != null) {
                MapUtils.findAndThen(userMap, updaterId, user -> supplier.setUpdaterName(user.getNickname()));
            }
        });
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void maskSupplierFields(List<ErpSupplierRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(supplier -> fieldPermissionMasker.mask(FIELD_PERMISSION_MODULE, supplier));
    }

    private void fillBusinessInfoSynced(List<ErpSupplierRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> supplierIds = convertSet(list, ErpSupplierRespVO::getId);
        Set<Long> syncedSupplierIds = convertSet(
                supplierBusinessInfoMapper.selectListBySupplierIds(supplierIds),
                ErpSupplierBusinessInfoDO::getSupplierId);
        list.forEach(supplier -> supplier.setBusinessInfoSynced(syncedSupplierIds.contains(supplier.getId())));
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("deptName", "deptId");
        map.put("deptNames", "deptIds");
        return map;
    }

}
