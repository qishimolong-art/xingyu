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

    @PostMapping("/create")
    @Operation(summary = "创建客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:create')")
    public CommonResult<Long> createCustomer(@Valid @RequestBody ErpCustomerSaveReqVO createReqVO) {
        return success(customerService.createCustomer(createReqVO));
    }

    @PostMapping("/business-license/recognize")
    @Operation(summary = "识别客户营业执照")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
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
        ErpCustomerRespVO respVO = BeanUtils.toBean(customer, ErpCustomerRespVO.class);
        fillCustomerExtra(Collections.singletonList(respVO));
        applyCreditStatus(respVO, customerService.getCustomerCreditStatus(id));
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, respVO);
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
        PageResult<ErpCustomerDO> pageResult = customerService.getCustomerPage(pageReqVO);
        PageResult<ErpCustomerRespVO> respResult = BeanUtils.toBean(pageResult, ErpCustomerRespVO.class);
        fillCustomerExtra(respResult.getList());
        // 批量聚合销售统计（最近销售日期 / 累计销售额 / 应收余额）
        if (CollUtil.isNotEmpty(respResult.getList())) {
            java.util.Collection<Long> customerIds = convertList(respResult.getList(), ErpCustomerRespVO::getId);
            java.util.Map<Long, ErpCustomerSaleStatsBO> statsMap = convertMap(
                    saleOutMapper.selectSaleStatsByCustomerIds(customerIds),
                    ErpCustomerSaleStatsBO::getCustomerId);
            respResult.getList().forEach(vo -> {
                ErpCustomerSaleStatsBO stats = statsMap.get(vo.getId());
                if (stats != null) {
                    if (stats.getLastSaleTime() != null) {
                        vo.setLastSaleDate(stats.getLastSaleTime().toLocalDate());
                    }
                    vo.setTotalSaleAmount(stats.getTotalSaleAmount());
                    vo.setReceivableBalance(stats.getReceivableBalance());
                }
            });
            java.util.Map<Long, ErpCustomerCreditStatusBO> creditStatusMap =
                    customerService.getCustomerCreditStatusMap(customerIds);
            respResult.getList().forEach(vo -> applyCreditStatus(vo, creditStatusMap.get(vo.getId())));
        }
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respResult.getList());
        fillBusinessInfoSynced(respResult.getList());
        return success(respResult);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得客户精简列表", description = "只包含被开启的客户，主要用于前端的下拉选项")
    public CommonResult<List<ErpCustomerRespVO>> getCustomerSimpleList() {
        List<ErpCustomerDO> list = customerService.getCustomerListByStatus(CommonStatusEnum.ENABLE.getStatus());
        List<ErpCustomerRespVO> respList = convertList(list, this::buildCustomerSimpleRespVO);
        fillCustomerExtra(respList);
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respList);
        return success(respList);
    }

    @GetMapping("/simple-page")
    @Operation(summary = "获得客户精简分页", description = "只包含被开启的客户，主要用于前端的下拉选项")
    public CommonResult<PageResult<ErpCustomerRespVO>> getCustomerSimplePage(@Valid ErpCustomerPageReqVO pageReqVO) {
        PageResult<ErpCustomerDO> pageResult = customerService.getCustomerPageByStatus(
                pageReqVO, CommonStatusEnum.ENABLE.getStatus());
        List<ErpCustomerRespVO> respList = convertList(pageResult.getList(), this::buildCustomerSimpleRespVO);
        fillCustomerExtra(respList);
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respList);
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
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respList);
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
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respList);
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
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpCustomerDO> list = customerService.getCustomerPage(pageReqVO).getList();
        List<ErpCustomerRespVO> rows = BeanUtils.toBean(list, ErpCustomerRespVO.class);
        fillCustomerExtra(rows);
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, rows);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpCustomerRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        // 导出 Excel
        ExcelUtils.write(response, "客户.xls", "数据", ErpCustomerRespVO.class, rows, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get customer export fields")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getCustomerExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpCustomerRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
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

}
