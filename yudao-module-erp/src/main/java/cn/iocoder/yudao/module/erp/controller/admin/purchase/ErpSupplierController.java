package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
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
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportTaskStartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportTaskStartRespVO;
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
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierExportTaskService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierImportTaskService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierQueryService;
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
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpBaseDataService baseDataService;
    @Resource
    private ErpFinanceFieldPermissionMasker baseDataFieldPermissionMasker;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpSupplierQueryService supplierQueryService;
    @Resource
    private ErpSupplierExportTaskService supplierExportTaskService;
    @Resource
    private ErpSupplierImportTaskService supplierImportTaskService;
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
        supplierQueryService.fillSupplierExtra(Collections.singletonList(respVO));
        maskSupplierFields(Collections.singletonList(respVO));
        fillBusinessInfoSynced(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商分页")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<PageResult<ErpSupplierRespVO>> getSupplierPage(@Valid ErpSupplierPageReqVO pageReqVO) {
        PageResult<ErpSupplierDO> pageResult = supplierService.getSupplierPage(pageReqVO);
        PageResult<ErpSupplierRespVO> respPage = BeanUtils.toBean(pageResult, ErpSupplierRespVO.class);
        supplierQueryService.fillSupplierExtra(respPage.getList());
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
        return success(buildSupplierSimpleRespList(list));
    }

    @GetMapping("/simple-page")
    @Operation(summary = "获得供应商精简分页", description = "只包含被开启的供应商，主要用于前端的下拉分页选项")
    public CommonResult<PageResult<ErpSupplierRespVO>> getSupplierSimplePage(@Valid ErpSupplierPageReqVO pageReqVO) {
        PageResult<ErpSupplierDO> pageResult =
                supplierService.getSupplierPageByStatus(pageReqVO, CommonStatusEnum.ENABLE.getStatus());
        return success(new PageResult<>(buildSupplierSimpleRespList(pageResult.getList()), pageResult.getTotal()));
    }

    private List<ErpSupplierRespVO> buildSupplierSimpleRespList(List<ErpSupplierDO> list) {
        return convertList(list, supplier -> new ErpSupplierRespVO()
                .setId(supplier.getId())
                .setName(supplier.getName())
                .setCode(supplier.getCode())
                .setShortName(supplier.getShortName())
                .setContact(supplier.getContact())
                .setMobile(supplier.getMobile())
                .setTelephone(supplier.getTelephone())
                .setPinyinCode(supplier.getPinyinCode())
                .setWubiCode(supplier.getWubiCode()));
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
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpSupplierExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ErpSupplierPageReqVO query = BeanUtils.toBean(pageReqVO, ErpSupplierPageReqVO.class);
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            query = new ErpSupplierPageReqVO();
            query.setIds(pageReqVO.getIds());
            query.setOrderField(pageReqVO.getOrderField());
            query.setOrderDirection(pageReqVO.getOrderDirection());
        }
        final ErpSupplierPageReqVO exportQuery = query;
        ErpSupplierExportSupport.write(response, includeFields, pageNo -> {
            exportQuery.setPageNo(pageNo);
            exportQuery.setPageSize(500);
            return convertList(supplierQueryService.buildSupplierRespList(
                            supplierService.getSupplierPage(exportQuery).getList()),
                    ErpSupplierExportSupport::toExportRow);
        });
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get supplier export fields")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getSupplierExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpSupplierExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    @PostMapping("/export-task/start")
    @Operation(summary = "创建供应商异步导出任务")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    public CommonResult<ErpSupplierExportTaskStartRespVO> startExportTask(
            @Valid @RequestBody ErpSupplierExportTaskStartReqVO reqVO) {
        exportCaptchaService.validate(reqVO.getCaptchaCode(), reqVO.getVerifyCode());
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpSupplierExportRespVO.class,
                reqVO.getFields(), fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE),
                EXPORT_FIELD_PERMISSION_MAP);
        ErpSupplierPageReqVO query = BeanUtils.toBean(reqVO, ErpSupplierPageReqVO.class);
        if (CollUtil.isNotEmpty(reqVO.getIds())) {
            query = new ErpSupplierPageReqVO();
            query.setIds(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(reqVO.getIds())));
            query.setOrderField(reqVO.getOrderField());
            query.setOrderDirection(reqVO.getOrderDirection());
        }
        query.setPageNo(1);
        query.setPageSize(500);
        return success(supplierExportTaskService.startTask(query, includeFields));
    }

    @GetMapping("/export-task/active")
    @Operation(summary = "获得当前用户正在执行的供应商导出任务")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    public CommonResult<ErpSupplierExportTaskRespVO> getActiveExportTask() {
        return success(supplierExportTaskService.getActiveTask());
    }

    @GetMapping("/export-task/get")
    @Operation(summary = "获得供应商导出任务进度")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    public CommonResult<ErpSupplierExportTaskRespVO> getExportTask(@RequestParam("id") Long id) {
        return success(supplierExportTaskService.getTask(id));
    }

    @GetMapping("/export-task/history")
    @Operation(summary = "获得当前用户的供应商导出记录")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    public CommonResult<PageResult<ErpSupplierExportTaskRespVO>> getExportTaskHistory(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return success(supplierExportTaskService.getHistory(pageNo, pageSize));
    }

    @GetMapping("/export-task/download")
    @Operation(summary = "下载供应商异步导出文件")
    @PreAuthorize("@ss.hasPermission('erp:supplier:export')")
    public void downloadExportTask(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        supplierExportTaskService.downloadTask(id, response);
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
    public CommonResult<ErpSupplierImportRespVO> importSupplier(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSupplierImportExcelVO> list = ExcelUtils.read(file, ErpSupplierImportExcelVO.class);
        return success(supplierService.importSupplierList(list));
    }

    @PostMapping("/import-task/start")
    @Operation(summary = "创建供应商异步导入任务")
    @PreAuthorize("@ss.hasPermission('erp:supplier:import')")
    public CommonResult<ErpSupplierImportTaskStartRespVO> startImportTask(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(supplierImportTaskService.startTask(file));
    }

    @GetMapping("/import-task/active")
    @Operation(summary = "获得当前用户正在执行的供应商导入任务")
    @PreAuthorize("@ss.hasPermission('erp:supplier:import')")
    public CommonResult<ErpSupplierImportTaskRespVO> getActiveImportTask() {
        return success(supplierImportTaskService.getActiveTask());
    }

    @GetMapping("/import-task/get")
    @Operation(summary = "获得供应商导入任务进度")
    @PreAuthorize("@ss.hasPermission('erp:supplier:import')")
    public CommonResult<ErpSupplierImportTaskRespVO> getImportTask(@RequestParam("id") Long id) {
        return success(supplierImportTaskService.getTask(id));
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

    private void maskSupplierFields(List<ErpSupplierRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        // Resolve permissions once for the whole page, rather than querying them for every supplier.
        fieldPermissionMasker.maskList(FIELD_PERMISSION_MODULE, list);
        list.forEach(supplier -> {
            if (supplier.getPurchaser() == null) supplier.setPurchaserName(null);
        });
    }

    private void fillBusinessInfoSynced(List<ErpSupplierRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> supplierIds = convertSet(list, ErpSupplierRespVO::getId);
        Set<Long> syncedSupplierIds = convertSet(
                supplierBusinessInfoMapper.selectSyncedSupplierIds(supplierIds),
                ErpSupplierBusinessInfoDO::getSupplierId);
        list.forEach(supplier -> supplier.setBusinessInfoSynced(syncedSupplierIds.contains(supplier.getId())));
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("deptName", "deptId");
        map.put("deptNames", "deptIds");
        map.put("creatorName", "creator");
        map.put("updaterName", "updater");
        return map;
    }


    private static void boundSelectorPageSize(PageParam reqVO) {
        if (reqVO.getPageSize() == null || reqVO.getPageSize() <= 0 || reqVO.getPageSize() > 100) reqVO.setPageSize(100);
    }

    @GetMapping("/form-dept-simple-page")
    @PreAuthorize("@ss.hasPermission('erp:supplier:create') or @ss.hasPermission('erp:supplier:update') or @ss.hasPermission('erp:supplier:query')")
    public CommonResult<PageResult<DeptRespDTO>> getFormDeptSimplePage(@Valid PageParam reqVO,
            @RequestParam(value = "selectedId", required = false) Long selectedId) {
        boundSelectorPageSize(reqVO);
        return success(deptApi.getDeptSimplePage(CommonStatusEnum.ENABLE.getStatus(), reqVO.getKeyword(),
                selectedId == null ? null : Collections.singleton(selectedId), reqVO));
    }

    @GetMapping("/dept-simple-page")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam reqVO) {
        boundSelectorPageSize(reqVO);
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, reqVO));
    }

    @GetMapping("/assignable-users-page")
    @PreAuthorize("@ss.hasPermission('erp:supplier:create') or @ss.hasPermission('erp:supplier:update') or @ss.hasPermission('erp:supplier:query')")
    public CommonResult<PageResult<AdminUserRespDTO>> getAssignableUsersPage(@Valid PageParam reqVO) {
        boundSelectorPageSize(reqVO);
        return success(adminUserApi.getUserSimplePage(CommonStatusEnum.ENABLE.getStatus(), reqVO.getKeyword(), reqVO));
    }

    @GetMapping("/merge-simple-page")
    @PreAuthorize("@ss.hasPermission('erp:supplier:merge')")
    public CommonResult<PageResult<ErpSupplierRespVO>> getMergeSimplePage(@Valid ErpSupplierPageReqVO reqVO) {
        boundSelectorPageSize(reqVO);
        // The old merge selector used simple-list: keep enabled-only candidates.
        PageResult<ErpSupplierDO> page = supplierService.getSupplierPageByStatus(reqVO, CommonStatusEnum.ENABLE.getStatus());
        return success(new PageResult<>(buildSupplierSimpleRespList(page.getList()), page.getTotal()));
    }

    @GetMapping("/import-failure-details/download")
    @PreAuthorize("@ss.hasPermission('erp:supplier:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    @GetMapping("/form-dept-options")
    @PreAuthorize("@ss.hasPermission('erp:supplier:create') or @ss.hasPermission('erp:supplier:update') or @ss.hasPermission('erp:supplier:query') or @ss.hasPermission('erp:supplier:dept-distribute')")
    public CommonResult<List<DeptRespDTO>> getFormDeptOptions() {
        return success(deptApi.getDeptListByStatus(null));
    }

    @GetMapping("/base-options-page")
    @PreAuthorize("@ss.hasPermission('erp:supplier:create') or @ss.hasPermission('erp:supplier:update') or @ss.hasPermission('erp:supplier:query')")
    public CommonResult<PageResult<ErpBaseDataRespVO>> getBaseOptionsPage(@Valid PageParam reqVO,
            @RequestParam("type") @javax.validation.constraints.Pattern(regexp = "region|supplier_category|settle_method|transport_method|freight_type|logistics_company|invoice_type") String type) {
        boundSelectorPageSize(reqVO);
        ErpBaseDataPageReqVO query = BeanUtils.toBean(reqVO, ErpBaseDataPageReqVO.class);
        query.setType(type);
        query.setStatus(CommonStatusEnum.ENABLE.getStatus());
        query.setName(reqVO.getKeyword());
        query.setKeyword(null);
        PageResult<ErpBaseDataDO> page = baseDataService.getBaseDataPage(query);
        return success(new PageResult<>(convertList(page.getList(), item -> {
            ErpBaseDataRespVO option = new ErpBaseDataRespVO().setId(item.getId()).setName(item.getName()).setCode(item.getCode());
            baseDataFieldPermissionMasker.maskForm("erp_base_data", option);
            option.setCode(item.getCode());
            return option;
        }), page.getTotal()));
    }
}
