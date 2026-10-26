package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpArchiveMergeReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchDisableReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptCreditRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptCreditSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaleDeptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerBusinessInfoMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpExportCaptchaService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerBusinessLicenseOcrService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerQueryService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerExportTaskService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerImportTaskService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerCreditStatusBO;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerSaleStatsBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerExportTaskStartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;

import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.hutool.core.collection.CollUtil;

@Tag(name = "管理后台 - ERP 客户")
@RestController
@RequestMapping("/erp/customer")
@Validated
public class ErpCustomerController {

    private static final String FIELD_PERMISSION_MODULE = "erp_customer";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpCustomerQueryService customerQueryService;
    @Resource
    private ErpCustomerExportTaskService customerExportTaskService;
    @Resource
    private ErpCustomerImportTaskService customerImportTaskService;
    @Resource
    private ErpCustomerBusinessLicenseOcrService customerBusinessLicenseOcrService;

    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpCustomerBusinessInfoMapper customerBusinessInfoMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;
    @Resource
    private ErpExportCaptchaService exportCaptchaService;
    @Resource
    private DeptApi deptApi;

    @Resource private cn.iocoder.yudao.module.erp.dal.mysql.base.ErpBaseDataMapper baseDataMapper;
    @Resource private AdminUserApi adminUserApi;
    @Resource private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource private ErpBaseDataService baseDataService;
    @Resource private ErpImportExportRecordService importExportRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:create')")
    public CommonResult<Long> createCustomer(@Valid @RequestBody ErpCustomerSaveReqVO createReqVO) {
        return success(customerService.createCustomer(createReqVO));
    }

    @PostMapping("/business-license/recognize")
    @Operation(summary = "识别客户营业执照")
    @PreAuthorize("@ss.hasPermission('erp:customer:create') or @ss.hasPermission('erp:customer:update')")
    public CommonResult<ErpCustomerBusinessLicenseOcrRespVO> recognizeBusinessLicense(
            @Valid @RequestBody ErpCustomerBusinessLicenseOcrReqVO reqVO) {
        return success(customerBusinessLicenseOcrService.recognize(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateCustomer(@Valid @RequestBody ErpCustomerSaveReqVO updateReqVO) {
        customerService.updateCustomer(updateReqVO);
        return success(true);
    }

    @GetMapping("/dept-distribution")
    @Operation(summary = "获得客户部门分配")
    @Parameter(name = "id", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:customer:dept-distribute')")
    public CommonResult<ErpCustomerDeptDistributionRespVO> getCustomerDeptDistribution(@RequestParam("id") Long id) {
        return success(customerService.getCustomerDeptDistribution(id));
    }

    @PutMapping("/dept-distribution")
    @Operation(summary = "更新客户部门分配")
    @PreAuthorize("@ss.hasPermission('erp:customer:dept-distribute')")
    public CommonResult<Boolean> updateCustomerDeptDistribution(
            @Valid @RequestBody ErpCustomerDeptDistributionSaveReqVO reqVO) {
        customerService.updateCustomerDeptDistribution(reqVO);
        return success(true);
    }

    @GetMapping("/dept-credit")
    @Operation(summary = "获得客户部门授信")
    @Parameter(name = "id", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:customer:dept-credit')")
    public CommonResult<ErpCustomerDeptCreditRespVO> getCustomerDeptCredit(@RequestParam("id") Long id) {
        return success(customerService.getCustomerDeptCredit(id));
    }

    @PutMapping("/dept-credit")
    @Operation(summary = "更新客户部门授信")
    @PreAuthorize("@ss.hasPermission('erp:customer:dept-credit')")
    public CommonResult<Boolean> updateCustomerDeptCredit(@Valid @RequestBody ErpCustomerDeptCreditSaveReqVO reqVO) {
        customerService.updateCustomerDeptCredit(reqVO);
        return success(true);
    }

    @PutMapping("/merge")
    @Operation(summary = "合并客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:merge')")
    public CommonResult<Boolean> mergeCustomer(@Valid @RequestBody ErpArchiveMergeReqVO reqVO) {
        customerService.mergeCustomer(reqVO.getSourceId(), reqVO.getKeepId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:delete')")
    public CommonResult<Boolean> deleteCustomer(@RequestParam("id") Long id) {
        customerService.deleteCustomer(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除客户")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:delete')")
    public CommonResult<Boolean> deleteCustomerList(@RequestParam("ids") List<Long> ids) {
        customerService.deleteCustomerList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerRespVO> getCustomer(@RequestParam("id") Long id) {
        ErpCustomerDO customer = customerService.getCustomer(id);
        if (customer == null) throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS);
        ErpCustomerRespVO respVO = BeanUtils.toBean(customer, ErpCustomerRespVO.class);
        fillCustomerExtra(Collections.singletonList(respVO));
        applyCreditStatus(respVO, customerService.getCustomerCreditStatus(id));
        maskCustomerFields(Collections.singletonList(respVO));
        fillBusinessInfoSynced(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/sale-price-level")
    @Operation(summary = "获得销售单据使用的客户价格级别")
    @Parameter(name = "id", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<Integer> getCustomerSalePriceLevel(@RequestParam("id") Long id) {
        ErpCustomerDO customer = customerService.getCustomer(id);
        return success(customer != null ? customer.getPriceLevel() : null);
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerRespVO>> getCustomerPage(@Valid ErpCustomerPageReqVO pageReqVO) {
        return success(customerQueryService.buildCustomerPage(customerService.getCustomerPage(pageReqVO)));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得客户精简列表", description = "只包含被开启的客户，主要用于前端的下拉选项")
    public CommonResult<List<ErpCustomerRespVO>> getCustomerSimpleList() {
        List<ErpCustomerDO> list = customerService.getCustomerListByStatus(CommonStatusEnum.ENABLE.getStatus());
        List<ErpCustomerRespVO> respList = convertList(list, this::buildCustomerSimpleRespVO);
        fillCustomerExtra(respList);
        maskCustomerFields(respList);
        return success(respList);
    }

    @GetMapping("/simple-page")
    @Operation(summary = "获得客户精简分页", description = "只包含被开启的客户，主要用于前端的下拉选项")
    public CommonResult<PageResult<ErpCustomerRespVO>> getCustomerSimplePage(@Valid ErpCustomerPageReqVO pageReqVO) {
        PageResult<ErpCustomerDO> pageResult = customerService.getCustomerPageByStatus(
                pageReqVO, CommonStatusEnum.ENABLE.getStatus());
        List<ErpCustomerRespVO> respList = convertList(pageResult.getList(), this::buildCustomerSimpleRespVO);
        fillCustomerExtra(respList);
        maskCustomerFields(respList);
        return success(new PageResult<>(respList, pageResult.getTotal()));
    }

    @GetMapping("/sale-simple-list")
    @Operation(summary = "获得销售可选客户精简列表", description = "包含启用客户，并标记超过白条授信限制的客户为禁选")
    public CommonResult<List<ErpCustomerRespVO>> getSaleCustomerSimpleList() {
        List<ErpCustomerDO> list = customerService.getCustomerListByStatus(CommonStatusEnum.ENABLE.getStatus());
        List<ErpCustomerRespVO> respList = convertList(list, customer -> buildCustomerSimpleRespVO(customer)
                .setCreditEnabled(customer.getCreditEnabled())
                .setCreditLimit(customer.getCreditLimit()).setCreditTermDays(customer.getCreditTermDays()));
        fillCustomerExtra(respList);
        fillSaleCustomerCreditStatus(respList);
        maskCustomerFields(respList);
        return success(respList);
    }

    @GetMapping("/sale-simple-page")
    @Operation(summary = "获得销售可选客户精简分页", description = "包含启用客户，并标记超过白条授信限制的客户为禁选")
    public CommonResult<PageResult<ErpCustomerRespVO>> getSaleCustomerSimplePage(@Valid ErpCustomerPageReqVO pageReqVO) {
        PageResult<ErpCustomerDO> pageResult = customerService.getCustomerPageByStatus(
                pageReqVO, CommonStatusEnum.ENABLE.getStatus());
        List<ErpCustomerRespVO> respList = convertList(pageResult.getList(), customer -> buildCustomerSimpleRespVO(customer)
                .setCreditEnabled(customer.getCreditEnabled())
                .setCreditLimit(customer.getCreditLimit()).setCreditTermDays(customer.getCreditTermDays()));
        fillCustomerExtra(respList);
        fillSaleCustomerCreditStatus(respList);
        maskCustomerFields(respList);
        return success(new PageResult<>(respList, pageResult.getTotal()));
    }

    private void fillSaleCustomerCreditStatus(List<ErpCustomerRespVO> respList) {
        Map<Long, ErpCustomerCreditStatusBO> creditStatusMap = customerService.getCustomerCreditStatusMap(
                convertList(respList, ErpCustomerRespVO::getId));
        respList.forEach(vo -> {
            ErpCustomerCreditStatusBO status = creditStatusMap.get(vo.getId());
            applyCreditStatus(vo, status);
            vo.setDisabled(status != null && Boolean.TRUE.equals(status.getBlocked()));
        });
    }

    private ErpCustomerRespVO buildCustomerSimpleRespVO(ErpCustomerDO customer) {
        return new ErpCustomerRespVO()
                .setId(customer.getId())
                .setCode(customer.getCode())
                .setName(customer.getName())
                .setShortName(customer.getShortName())
                .setContact(customer.getContact())
                .setMobile(customer.getMobile())
                .setTelephone(customer.getTelephone())
                .setPinyinCode(customer.getPinyinCode())
                .setWubiCode(customer.getWubiCode())
                .setMemberCode(customer.getMemberCode())
                .setPlatformCode(customer.getPlatformCode());
    }

    @GetMapping("/sale-dept-list")
    @Operation(summary = "获得客户销售可选部门列表")
    @Parameter(name = "customerId", description = "客户编号", required = true, example = "1024")
    public CommonResult<List<ErpCustomerSaleDeptRespVO>> getCustomerSaleDeptList(
            @RequestParam("customerId") Long customerId) {
        List<Long> deptIds = customerService.getCustomerSaleDeptIds(customerId);
        if (CollUtil.isEmpty(deptIds)) {
            return success(Collections.emptyList());
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        return success(deptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .map(dept -> BeanUtils.toBean(dept, ErpCustomerSaleDeptRespVO.class))
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出客户 Excel")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCustomerExcel(@Valid ErpCustomerPageReqVO pageReqVO,
              @RequestParam(value = "captchaCode", required = false) String captchaCode,
              @RequestParam(value = "verifyCode", required = false) String verifyCode,
              @RequestParam(value = "fields", required = false) String fields,
              HttpServletResponse response) throws IOException {
        exportCaptchaService.validate(captchaCode, verifyCode);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpCustomerExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ErpCustomerPageReqVO query = BeanUtils.toBean(pageReqVO, ErpCustomerPageReqVO.class);
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            query = new ErpCustomerPageReqVO();
            query.setIds(pageReqVO.getIds());
            query.setOrderField(pageReqVO.getOrderField());
            query.setOrderDirection(pageReqVO.getOrderDirection());
        }
        final ErpCustomerPageReqVO exportQuery = query;
        ErpCustomerExportSupport.write(response, includeFields, pageNo -> {
            exportQuery.setPageNo(pageNo);
            exportQuery.setPageSize(500);
            List<ErpCustomerRespVO> rows = customerQueryService.buildCustomerExportList(
                    customerService.getCustomerPage(exportQuery).getList());
            return convertList(rows, ErpCustomerExportSupport::toExportRow);
        });
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get customer export fields")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getCustomerExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpCustomerExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    @PostMapping("/export-task/start")
    @Operation(summary = "创建客户异步导出任务")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    public CommonResult<ErpCustomerExportTaskStartRespVO> startExportTask(
            @Valid @RequestBody ErpCustomerExportTaskStartReqVO reqVO) {
        exportCaptchaService.validate(reqVO.getCaptchaCode(), reqVO.getVerifyCode());
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpCustomerExportRespVO.class,
                reqVO.getFields(), fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE),
                EXPORT_FIELD_PERMISSION_MAP);
        ErpCustomerPageReqVO query = BeanUtils.toBean(reqVO, ErpCustomerPageReqVO.class);
        if (CollUtil.isNotEmpty(reqVO.getIds())) {
            query = new ErpCustomerPageReqVO();
            query.setIds(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(reqVO.getIds())));
            query.setOrderField(reqVO.getOrderField());
            query.setOrderDirection(reqVO.getOrderDirection());
        }
        query.setPageNo(1);
        query.setPageSize(500);
        return success(customerExportTaskService.startTask(query, includeFields));
    }

    @GetMapping("/export-task/active")
    @Operation(summary = "获得当前用户正在执行的客户导出任务")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    public CommonResult<ErpCustomerExportTaskRespVO> getActiveExportTask() {
        return success(customerExportTaskService.getActiveTask());
    }

    @GetMapping("/export-task/get")
    @Operation(summary = "获得客户导出任务进度")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    public CommonResult<ErpCustomerExportTaskRespVO> getExportTask(@RequestParam("id") Long id) {
        return success(customerExportTaskService.getTask(id));
    }

    @GetMapping("/export-task/history")
    @Operation(summary = "获得当前用户的客户导出记录")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    public CommonResult<PageResult<ErpCustomerExportTaskRespVO>> getExportTaskHistory(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return success(customerExportTaskService.getHistory(pageNo, pageSize));
    }

    @GetMapping("/export-task/download")
    @Operation(summary = "下载客户异步导出文件")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    public void downloadExportTask(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        customerExportTaskService.downloadTask(id, response);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得客户导入模板")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<ErpCustomerImportExcelVO> list = Collections.singletonList(new ErpCustomerImportExcelVO());
        ExcelUtils.writeImportTemplate(response, "客户导入模板.xls", "客户", ErpCustomerImportExcelVO.class, list, null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.CUSTOMER, ErpCustomerImportExcelVO.class,
                        ErpImportTemplateRequiredFieldUtils.aliasMap(
                                "name", "name",
                                "code", "code",
                                "deptId", "deptName",
                                "deptIds", "deptNames",
                                "contact", "contact",
                                "mobile", "mobile",
                                "telephone", "telephone",
                                "address", "address")));
    }

    @PostMapping("/import")
    @Operation(summary = "导入客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public CommonResult<ErpCustomerImportRespVO> importCustomer(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpCustomerImportExcelVO> list = ExcelUtils.read(file, ErpCustomerImportExcelVO.class);
        return success(customerService.importCustomerList(list));
    }

    @PostMapping("/import-task/start")
    @Operation(summary = "创建客户异步导入任务")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public CommonResult<ErpCustomerImportTaskStartRespVO> startImportTask(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(customerImportTaskService.startTask(file));
    }

    @GetMapping("/import-task/active")
    @Operation(summary = "获得当前用户正在执行的客户导入任务")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public CommonResult<ErpCustomerImportTaskRespVO> getActiveImportTask() {
        return success(customerImportTaskService.getActiveTask());
    }

    @GetMapping("/import-task/get")
    @Operation(summary = "获得客户导入任务进度")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public CommonResult<ErpCustomerImportTaskRespVO> getImportTask(@RequestParam("id") Long id) {
        return success(customerImportTaskService.getTask(id));
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量编辑客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> batchUpdateCustomer(@Valid @RequestBody ErpCustomerBatchUpdateReqVO reqVO) {
        customerService.batchUpdateCustomer(reqVO);
        return success(true);
    }

    @PutMapping("/batch-disable")
    @Operation(summary = "批量停用客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> batchDisableCustomer(@Valid @RequestBody ErpCustomerBatchDisableReqVO reqVO) {
        customerService.batchDisableCustomer(reqVO.getIds());
        return success(true);
    }

    @PutMapping("/restore")
    @Operation(summary = "还原停用客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> restoreCustomer(@Valid @RequestBody ErpCustomerBatchDisableReqVO reqVO) {
        customerService.restoreCustomer(reqVO.getIds());
        return success(true);
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "main");
        map.put("code", "main");
        map.put("name", "main");
        map.put("shortName", "main");
        map.put("customerType", "main");
        map.put("enterpriseMatchStatus", "main");
        map.put("contact", "main");
        map.put("mobile", "main");
        map.put("telephone", "main");
        map.put("email", "main");
        map.put("address", "main");
        map.put("remark", "main");
        map.put("status", "main");
        map.put("sort", "main");
        map.put("taxNo", "finance_info");
        map.put("settleMethod", "finance_info");
        map.put("taxPercent", "finance_info");
        map.put("bankName", "finance_info");
        map.put("bankAccount", "finance_info");
        map.put("bankAddress", "finance_info");
        map.put("saleUserId", "main");
        map.put("deptName", "main");
        map.put("deptNames", "main");
        map.put("allowMultiDept", "main");
        map.put("priceLevel", "main");
        map.put("routeId", "main");
        map.put("createTime", "system");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("deptName", "deptId");
        map.put("deptNames", "deptIds");
        return map;
    }

    private void fillCustomerExtra(List<ErpCustomerRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> userIds = new java.util.HashSet<>();
        list.forEach(row -> {
            if (row.getSaleUserId() != null) userIds.add(row.getSaleUserId());
            if (row.getDeveloperUserId() != null) userIds.add(row.getDeveloperUserId());
            for (String id : java.util.Arrays.asList(row.getCreator(), row.getUpdater())) {
                if (id != null && id.matches("[0-9]+")) userIds.add(Long.valueOf(id));
            }
        });
        Map<Long, AdminUserRespDTO> users = userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        list.forEach(row -> {
            AdminUserRespDTO saleUser = users.get(row.getSaleUserId());
            AdminUserRespDTO developer = users.get(row.getDeveloperUserId());
            row.setSaleUserName(saleUser == null ? null : saleUser.getNickname());
            row.setDeveloperUserName(developer == null ? null : developer.getNickname());
            AdminUserRespDTO creator = row.getCreator() != null && row.getCreator().matches("[0-9]+") ? users.get(Long.valueOf(row.getCreator())) : null;
            AdminUserRespDTO updater = row.getUpdater() != null && row.getUpdater().matches("[0-9]+") ? users.get(Long.valueOf(row.getUpdater())) : null;
            row.setCreatorName(creator == null ? row.getCreator() : creator.getNickname());
            row.setUpdaterName(updater == null ? row.getUpdater() : updater.getNickname());
        });
        Set<Long> optionIds = new java.util.HashSet<>();
        list.forEach(row -> { if (row.getRouteId() != null) optionIds.add(row.getRouteId()); if (row.getAreaId() != null) optionIds.add(row.getAreaId()); if (row.getFreightExplainId() != null) optionIds.add(row.getFreightExplainId()); });
        Map<Long, ErpBaseDataDO> optionMap = optionIds.isEmpty() ? Collections.emptyMap() : convertMap(baseDataMapper.selectByIds(optionIds), ErpBaseDataDO::getId);
        Map<String, Integer> typeCodes = new LinkedHashMap<>();
        typeCodes.put("car_owner", 1); typeCodes.put("repair_factory", 2); typeCodes.put("terminal_customer", 3); typeCodes.put("customer_supplier", 4); typeCodes.put("supplier", 5); typeCodes.put("store", 6); typeCodes.put("dealer", 7);
        Map<String, Integer> settleCodes = new LinkedHashMap<>();
        settleCodes.put("cash", 1); settleCodes.put("credit", 2); settleCodes.put("remittance", 3); settleCodes.put("online_payment", 4);
        Set<String> codes = new java.util.HashSet<>(typeCodes.keySet()); codes.addAll(settleCodes.keySet());
        Map<Integer, String> types = new LinkedHashMap<>(); Map<Integer, String> settlements = new LinkedHashMap<>();
        baseDataMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpBaseDataDO>()
                .in(ErpBaseDataDO::getType, java.util.Arrays.asList("customer_type", "settle_method")).in(ErpBaseDataDO::getCode, codes))
                .forEach(option -> {
                    Integer value = ("customer_type".equals(option.getType()) ? typeCodes : settleCodes).get(option.getCode());
                    if (value != null) ("customer_type".equals(option.getType()) ? types : settlements).put(value, option.getName());
                });
        list.forEach(row -> {
            ErpBaseDataDO route = optionMap.get(row.getRouteId()); ErpBaseDataDO area = optionMap.get(row.getAreaId());
            row.setRouteName(route == null ? null : route.getName()); row.setAreaName(area == null ? null : area.getName());
            row.setCustomerTypeName(types.get(row.getCustomerType())); row.setSettleMethodName(settlements.get(row.getSettleMethod()));
        });
        Map<Long, List<Long>> customerDeptMap =
                customerService.getCustomerDeptMap(convertList(list, ErpCustomerRespVO::getId));
        Set<Long> deptIds = new java.util.HashSet<>();
        list.forEach(customer -> {
            if (customer.getDeptId() != null) {
                deptIds.add(customer.getDeptId());
            }
            List<Long> currentDeptIds =
                    customerDeptMap.getOrDefault(customer.getId(), Collections.emptyList());
            customer.setDeptIds(currentDeptIds);
            deptIds.addAll(currentDeptIds);
        });
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap =
                CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        list.forEach(customer -> {
            DeptRespDTO dept = deptMap.get(customer.getDeptId());
            if (dept != null) {
                customer.setDeptName(dept.getName());
            }
            customer.setDeptNames(customer.getDeptIds().stream()
                    .map(deptMap::get)
                    .filter(item -> item != null && item.getName() != null)
                    .map(DeptRespDTO::getName)
                    .collect(java.util.stream.Collectors.joining("、")));
        });
    }

    private void applyCreditStatus(ErpCustomerRespVO vo, ErpCustomerCreditStatusBO status) {
        if (vo == null || status == null) {
            return;
        }
        vo.setReceivableBalance(status.getReceivableBalance());
        vo.setEarliestUnpaidDate(status.getEarliestUnpaidDate());
        vo.setDebtDays(status.getDebtDays());
        vo.setCreditAmountExceeded(status.getAmountExceeded());
        vo.setCreditTermExceeded(status.getTermExceeded());
        vo.setCreditBlocked(status.getBlocked());
        vo.setCreditBlockedReason(status.getBlockedReason());
        vo.setDisabled(Boolean.TRUE.equals(status.getBlocked()));
    }

    private void fillBusinessInfoSynced(List<ErpCustomerRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> customerIds = convertSet(list, ErpCustomerRespVO::getId);
        Set<Long> syncedCustomerIds = convertSet(
                customerBusinessInfoMapper.selectListByCustomerIds(customerIds),
                ErpCustomerBusinessInfoDO::getCustomerId);
        list.forEach(customer -> customer.setBusinessInfoSynced(syncedCustomerIds.contains(customer.getId())));
    }


    private void maskCustomerFields(List<ErpCustomerRespVO> rows) {
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, rows);
        rows.forEach(row -> {
            if (row.getSaleUserId() == null) row.setSaleUserName(null);
            if (row.getDeveloperUserId() == null) row.setDeveloperUserName(null);
            if (row.getRouteId() == null) row.setRouteName(null);
            if (row.getAreaId() == null) row.setAreaName(null);
            if (row.getCustomerType() == null) row.setCustomerTypeName(null);
            if (row.getSettleMethod() == null) row.setSettleMethodName(null);
            if (row.getCreator() == null) row.setCreatorName(null);
            if (row.getUpdater() == null) row.setUpdaterName(null);
        });
    }
    private static void boundSelectorPageSize(PageParam reqVO) {
        if (reqVO.getPageSize() == null || reqVO.getPageSize() <= 0 || reqVO.getPageSize() > 100) reqVO.setPageSize(100);
    }

    @GetMapping("/form-dept-simple-page")
    @PreAuthorize("@ss.hasPermission('erp:customer:create') or @ss.hasPermission('erp:customer:update') or @ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<DeptRespDTO>> getFormDeptSimplePage(@Valid PageParam reqVO,
            @RequestParam(value = "selectedId", required = false) Long selectedId) {
        boundSelectorPageSize(reqVO);
        return success(deptApi.getDeptSimplePage(CommonStatusEnum.ENABLE.getStatus(), reqVO.getKeyword(),
                selectedId == null ? null : Collections.singleton(selectedId), reqVO));
    }

    @GetMapping("/dept-simple-page")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam reqVO) {
        boundSelectorPageSize(reqVO);
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, reqVO));
    }

    @GetMapping("/assignable-users-page")
    @PreAuthorize("@ss.hasPermission('erp:customer:create') or @ss.hasPermission('erp:customer:update') or @ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<AdminUserRespDTO>> getAssignableUsersPage(@Valid PageParam reqVO) {
        boundSelectorPageSize(reqVO);
        return success(adminUserApi.getUserSimplePage(CommonStatusEnum.ENABLE.getStatus(), reqVO.getKeyword(), reqVO));
    }

    @GetMapping("/merge-simple-page")
    @PreAuthorize("@ss.hasPermission('erp:customer:merge')")
    public CommonResult<PageResult<ErpCustomerRespVO>> getMergeSimplePage(@Valid ErpCustomerPageReqVO reqVO) {
        boundSelectorPageSize(reqVO);
        // The old merge selector used simple-list: keep enabled-only candidates.
        PageResult<ErpCustomerDO> page = customerService.getCustomerPageByStatus(reqVO, CommonStatusEnum.ENABLE.getStatus());
        return success(new PageResult<>(BeanUtils.toBean(page.getList(), ErpCustomerRespVO.class), page.getTotal()));
    }

    @GetMapping("/import-failure-details/download")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    @GetMapping("/form-dept-options")
    @PreAuthorize("@ss.hasPermission('erp:customer:create') or @ss.hasPermission('erp:customer:update') or @ss.hasPermission('erp:customer:query') or @ss.hasPermission('erp:customer:dept-distribute')")
    public CommonResult<List<DeptRespDTO>> getFormDeptOptions() {
        return success(deptApi.getDeptListByStatus(null));
    }

    @GetMapping("/base-options-page")
    @PreAuthorize("@ss.hasPermission('erp:customer:create') or @ss.hasPermission('erp:customer:update') or @ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpBaseDataRespVO>> getBaseOptionsPage(@Valid PageParam reqVO,
            @RequestParam("type") @javax.validation.constraints.Pattern(regexp = "sales_area|customer_category|customer_type|relation_type|settle_method|transport_method|freight_type|freight_explain|route|invoice_type|delivery_method|price_level") String type) {
        boundSelectorPageSize(reqVO);
        ErpBaseDataPageReqVO query = BeanUtils.toBean(reqVO, ErpBaseDataPageReqVO.class);
        query.setType(type);
        query.setStatus(CommonStatusEnum.ENABLE.getStatus());
        query.setName(reqVO.getKeyword());
        query.setKeyword(null);
        java.util.List<String> codes = customerOptionCodes(type);
        PageResult<ErpBaseDataDO> page = codes.isEmpty() ? baseDataService.getBaseDataPage(query)
                : baseDataMapper.selectPage(query, new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpBaseDataDO>()
                    .eq(ErpBaseDataDO::getType, type).eq(ErpBaseDataDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                    .likeIfPresent(ErpBaseDataDO::getName, reqVO.getKeyword()).in(ErpBaseDataDO::getCode, codes)
                    .orderByAsc(ErpBaseDataDO::getSort).orderByDesc(ErpBaseDataDO::getId));
        return success(new PageResult<>(convertList(page.getList(), item -> {
            ErpBaseDataRespVO option = new ErpBaseDataRespVO().setId(item.getId()).setName(item.getName()).setCode(item.getCode());

            option.setCode(item.getCode());
            return option;
        }), page.getTotal()));
    }

    private java.util.List<String> customerOptionCodes(String type) {
        switch (type) {
            case "customer_category": return java.util.Arrays.asList("customer", "customer_supplier");
            case "customer_type": return java.util.Arrays.asList("car_owner", "customer_supplier", "dealer", "repair_factory", "store", "supplier", "terminal_customer");
            case "settle_method": return java.util.Arrays.asList("cash", "credit", "remittance", "online_payment");
            case "invoice_type": return java.util.Arrays.asList("receipt", "normal_invoice", "vat_invoice");
            case "freight_type": return java.util.Arrays.asList("pay_for_customer", "other", "self_pay", "customer_pay", "delivery_subsidy");
            case "delivery_method": return java.util.Arrays.asList("customer_pickup", "delivery_service", "logistics_consignment", "door_delivery");
            default: return Collections.emptyList();
        }
    }
}
