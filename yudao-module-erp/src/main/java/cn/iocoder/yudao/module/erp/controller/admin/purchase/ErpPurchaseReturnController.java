package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseItemMatchRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnRefundSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购退货")
@RestController
@RequestMapping("/erp/purchase-return")
@Validated
public class ErpPurchaseReturnController {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_return";
    private static final String DEPT_SELECTION_PERMISSION_FORM_KEY = "system_dept";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productCode",
            "productName", "productName",
            "warehouseId", "warehouseName",
            "count", "count",
            "item_count", "count",
            "itemCount", "count",
            "productPrice", "productPrice",
            "item_remark", "remark",
            "itemRemark", "remark");
    private static final Map<String, String> ORDER_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "supplierId", "supplierName",
            "supplierName", "supplierName",
            "remark", "remark",
            "productId", "productCode",
            "productName", "productName",
            "warehouseId", "warehouseName",
            "warehouseName", "warehouseName",
            "count", "itemCount",
            "item_count", "itemCount",
            "itemCount", "itemCount",
            "productPrice", "productPrice",
            "batchNo", "batchNo",
            "item_batchNo", "batchNo",
            "item_remark", "itemRemark",
            "itemRemark", "itemRemark");
    private static final Set<String> ORDER_IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "no", "supplierName", "remark", "productCode", "productName", "factoryCode", "warehouseName", "itemCount", "productPrice",
            "batchNo", "itemRemark"));

    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;

    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpPurchaseItemPriceReferenceFiller itemPriceReferenceFiller;
    @Resource
    private ErpFieldConfigService fieldConfigService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建采购退货")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<Long> createPurchaseReturn(@Valid @RequestBody ErpPurchaseReturnSaveReqVO createReqVO) {
        return success(purchaseReturnService.createPurchaseReturn(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<Long> createPurchaseReturnDraft(
            @RequestBody ErpPurchaseReturnDraftCreateReqVO createReqVO) {
        return success(purchaseReturnService.createPurchaseReturnDraft(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购退货")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> updatePurchaseReturn(@Valid @RequestBody ErpPurchaseReturnSaveReqVO updateReqVO) {
        purchaseReturnService.updatePurchaseReturn(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update-items")
    @Operation(summary = "批量修改采购退货明细仓库/部门")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> batchUpdatePurchaseReturnItems(
            @Valid @RequestBody ErpPurchaseReturnItemBatchUpdateReqVO updateReqVO) {
        purchaseReturnService.batchUpdatePurchaseReturnItems(updateReqVO);
        return success(true);
    }

    @GetMapping("/warehouse-dept-simple-list")
    @Operation(summary = "获得采购退货目标仓库可用部门列表")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<List<DeptSimpleRespVO>> getWarehouseAvailableDeptSimpleList(
            @RequestParam("warehouseId") Long warehouseId) {
        return success(purchaseReturnService.getWarehouseAvailableDeptSimpleList(warehouseId));
    }

    @GetMapping("/warehouse-dept-simple-page")
    @Operation(summary = "获得采购退货目标仓库可用部门分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getWarehouseAvailableDeptSimplePage(
            @RequestParam("warehouseId") Long warehouseId, @Valid PageParam pageReqVO) {
        return success(purchaseReturnService.getWarehouseAvailableDeptSimplePage(warehouseId, pageReqVO));
    }

    @GetMapping("/supplier-dept-simple-list")
    @Operation(summary = "获得供应商对当前用户可用的采购退货部门精简列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<List<DeptSimpleRespVO>> getSupplierAvailableDeptSimpleList(
            @RequestParam("supplierId") Long supplierId) {
        return success(purchaseReturnService.getSupplierAvailableDeptSimpleList(supplierId));
    }

    @GetMapping("/supplier-dept-simple-page")
    @Operation(summary = "获得供应商对当前用户可用的采购退货部门分页")
    @Parameter(name = "supplierId", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getSupplierAvailableDeptSimplePage(
            @RequestParam("supplierId") Long supplierId, @Valid PageParam pageReqVO) {
        return success(purchaseReturnService.getSupplierAvailableDeptSimplePage(supplierId, pageReqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "鑾峰緱褰撳墠鐢ㄦ埛鍙煡璇㈢殑閲囪喘閫€璐ч儴闂ㄧ簿绠€鍒楄〃")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(supplierDeptPermissionService.getDataPermissionDeptSimpleList(
                DEPT_SELECTION_PERMISSION_FORM_KEY));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获得当前用户可查询的采购退货部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage(DEPT_SELECTION_PERMISSION_FORM_KEY, pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获得采购退货用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(pageReqVO));
    }

    @PutMapping("/update-draft")
    @Operation(summary = "保存采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> updatePurchaseReturnDraft(
            @RequestBody ErpPurchaseReturnDraftUpdateReqVO updateReqVO) {
        purchaseReturnService.updatePurchaseReturnDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update') and " +
            "@ss.hasPermission('erp:purchase-return:update-status')")
    public CommonResult<Boolean> updateAndSubmitPurchaseReturnDraft(
            @Valid @RequestBody ErpPurchaseReturnDraftUpdateReqVO updateReqVO) {
        purchaseReturnService.updateAndSubmitPurchaseReturnDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update-status')")
    public CommonResult<Boolean> submitPurchaseReturn(@RequestParam("id") Long id) {
        purchaseReturnService.submitPurchaseReturn(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改采购退货备注")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> updatePurchaseReturnRemark(
            @Valid @RequestBody ErpPurchaseUpdateRemarkReqVO updateReqVO) {
        purchaseReturnService.updatePurchaseReturnRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购退货的状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update-status')")
    public CommonResult<Boolean> updatePurchaseReturnStatus(@RequestParam("id") Long id,
                                                            @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        purchaseReturnService.updatePurchaseReturnStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购退货")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:delete')")
    public CommonResult<Boolean> deletePurchaseReturn(@RequestParam("ids") List<Long> ids) {
        purchaseReturnService.deletePurchaseReturn(ids);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "解析采购退货导入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<ErpPurchaseReturnImportRespVO> importPurchaseReturn(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseReturnImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseReturnImportExcelVO.class);
        return success(purchaseReturnService.importPurchaseReturnItems(list));
    }

    @PostMapping("/import-order")
    @Operation(summary = "Import purchase return order")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<ErpPurchaseImportResultRespVO> importPurchaseReturnOrder(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseReturnOrderImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseReturnOrderImportExcelVO.class);
        return success(purchaseReturnService.importPurchaseReturnOrderList(list));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载采购退货导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId, HttpServletResponse response)
            throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购退货明细导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseReturnImportExcelVO example = new ErpPurchaseReturnImportExcelVO();
        example.setProductCode("P000001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setWarehouseName("主仓");
        example.setRemark("示例");
        ExcelUtils.writeImportTemplate(response, "采购退货明细导入模板.xls", "采购退货明细", ErpPurchaseReturnImportExcelVO.class,
                java.util.Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_RETURN, ErpPurchaseReturnImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get-order-import-template")
    @Operation(summary = "Get purchase return order import template")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public void getOrderImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseReturnOrderImportExcelVO example = new ErpPurchaseReturnOrderImportExcelVO();
        example.setNo("CGTH20260811000002");
        example.setSupplierName("示例供应商");
        example.setReturnTime("2026-08-11");
        example.setRemark("整单备注");
        example.setProductCode("P000001");
        example.setWarehouseName("主仓库");
        example.setItemCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setBatchNo("BATCH-001");
        example.setItemRemark("明细备注");

        ErpPurchaseReturnOrderImportExcelVO secondItem = new ErpPurchaseReturnOrderImportExcelVO();
        secondItem.setProductCode("P000002");
        secondItem.setWarehouseName("主仓库");
        secondItem.setItemCount(new BigDecimal("2"));
        secondItem.setProductPrice(new BigDecimal("20.00"));

        ExcelUtils.writeImportTemplate(response, "采购退货导入模板.xls", "采购退货",
                ErpPurchaseReturnOrderImportExcelVO.class, java.util.Arrays.asList(example, secondItem),
                ORDER_IMPORT_TEMPLATE_FIELDS,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_RETURN, ErpPurchaseReturnOrderImportExcelVO.class,
                        ORDER_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购退货")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<ErpPurchaseReturnRespVO> getPurchaseReturn(@RequestParam("id") Long id,
                                                                   @RequestParam(value = "mask", defaultValue = "true") Boolean mask,
                                                                   @RequestParam(value = "includeItems", required = false,
                                                                           defaultValue = "true") Boolean includeItems) {
        ErpPurchaseReturnDO purchaseReturn = purchaseReturnService.getPurchaseReturn(id);
        if (purchaseReturn == null) {
            return success(null);
        }
        List<ErpPurchaseReturnItemDO> purchaseReturnItemList = Boolean.TRUE.equals(includeItems)
                ? purchaseReturnService.getPurchaseReturnItemListByReturnId(id) : java.util.Collections.emptyList();
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(java.util.Collections.singletonList(purchaseReturn)));
        DeptRespDTO dept = purchaseReturn.getDeptId() == null ? null : deptApi.getDept(purchaseReturn.getDeptId());
        ErpSupplierDO supplier = purchaseReturn.getSupplierId() == null
                ? null : supplierService.getSupplier(purchaseReturn.getSupplierId());
        ErpPurchaseReturnRespVO respVO = BeanUtils.toBean(purchaseReturn, ErpPurchaseReturnRespVO.class, purchaseReturnVO -> {
            if (Boolean.TRUE.equals(includeItems)) {
                purchaseReturnVO.setItems(buildPurchaseReturnItemVOList(purchaseReturnItemList, id));
                purchaseReturnVO.setItemCount(purchaseReturnItemList.size());
            }
                if (dept != null) {
                    purchaseReturnVO.setDeptName(dept.getName());
                }
                if (supplier != null) {
                    purchaseReturnVO.setSupplierName(supplier.getName());
                }
                fillUserNames(purchaseReturnVO, userMap);
        });
        if (Boolean.TRUE.equals(mask)) {
            fieldPermissionMasker.mask("erp_purchase_return", respVO);
        }
        return success(respVO);
    }

    @GetMapping("/payment-summary")
    @Operation(summary = "获得采购退货退款核销摘要")
    @Parameter(name = "id", description = "采购退货编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<ErpPurchaseReturnRefundSummaryRespVO> getPurchaseReturnPaymentSummary(
            @RequestParam("id") Long id) {
        ErpPurchaseReturnDO purchaseReturn = purchaseReturnService.getPurchaseReturn(id);
        if (purchaseReturn == null) {
            return success(null);
        }
        BigDecimal refundableAmount = zeroIfNull(purchaseReturn.getTotalPrice());
        BigDecimal refundedAmount = zeroIfNull(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                id, ErpBizTypeEnum.PURCHASE_RETURN.getType())).abs();
        BigDecimal unrefundedAmount = refundableAmount.subtract(refundedAmount);
        return success(new ErpPurchaseReturnRefundSummaryRespVO()
                .setId(id)
                .setRefundableAmount(refundableAmount)
                .setRefundedAmount(refundedAmount)
                .setUnrefundedAmount(unrefundedAmount)
                .setRefundStatus(calculatePurchaseReturnRefundStatus(refundableAmount, refundedAmount)));
    }

    private Integer calculatePurchaseReturnRefundStatus(BigDecimal refundableAmount, BigDecimal refundedAmount) {
        BigDecimal normalizedRefundableAmount = zeroIfNull(refundableAmount).abs();
        BigDecimal normalizedRefundedAmount = zeroIfNull(refundedAmount).abs();
        if (normalizedRefundedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        if (normalizedRefundableAmount.compareTo(BigDecimal.ZERO) == 0
                || normalizedRefundedAmount.compareTo(normalizedRefundableAmount) < 0) {
            return 1;
        }
        return 2;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得采购退货明细分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<PageResult<ErpPurchaseReturnRespVO.Item>> getPurchaseReturnItemPage(
            @Valid ErpPurchaseReturnItemPageReqVO pageReqVO) {
        PageResult<ErpPurchaseReturnItemDO> pageResult = purchaseReturnService.getPurchaseReturnItemPage(pageReqVO);
        PageResult<ErpPurchaseReturnRespVO.Item> respResult = new PageResult<>(
                buildPurchaseReturnItemVOList(pageResult.getList(), pageReqVO.getReturnId()), pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, respResult.getList());
        }
        return success(respResult);
    }

    @PostMapping("/item-page")
    @Operation(summary = "获得采购退货明细分页（编辑态）")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<PageResult<ErpPurchaseReturnRespVO.Item>> getPurchaseReturnItemPageForEdit(
            @Valid @RequestBody ErpPurchaseReturnItemPageReqVO pageReqVO) {
        return getPurchaseReturnItemPage(pageReqVO);
    }

    @PostMapping("/item-match")
    @Operation(summary = "匹配采购退货整单明细")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<ErpPurchaseItemMatchRespVO> matchPurchaseReturnItems(
            @Valid @RequestBody ErpPurchaseItemMatchReqVO reqVO) {
        purchaseReturnService.getPurchaseReturn(reqVO.getDocumentId());
        Set<Long> excludedIds = new java.util.HashSet<>(CollUtil.emptyIfNull(reqVO.getExcludedItemIds()));
        List<ErpPurchaseReturnItemDO> items = purchaseReturnService.getPurchaseReturnItemListByReturnId(reqVO.getDocumentId())
                .stream().filter(item -> !excludedIds.contains(item.getId()))
                .sorted(java.util.Comparator.comparing(ErpPurchaseReturnItemDO::getId)).collect(Collectors.toList());
        ErpPurchaseItemMatchRespVO result = new ErpPurchaseItemMatchRespVO();
        for (int keyIndex = 0; keyIndex < reqVO.getKeys().size(); keyIndex++) {
            ErpPurchaseItemMatchReqVO.Key key = reqVO.getKeys().get(keyIndex);
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                ErpPurchaseReturnItemDO item = items.get(itemIndex);
                if (java.util.Objects.equals(key.getProductId(), item.getProductId())
                        && java.util.Objects.equals(key.getWarehouseId(), item.getWarehouseId())) {
                    result.getMatches().add(new ErpPurchaseItemMatchRespVO.Match(keyIndex, item.getId(),
                            itemIndex / reqVO.getPageSize() + 1));
                    break;
                }
            }
        }
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购退货分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<PageResult<ErpPurchaseReturnRespVO>> getPurchaseReturnPage(@Valid ErpPurchaseReturnPageReqVO pageReqVO) {
        PageResult<ErpPurchaseReturnDO> pageResult = purchaseReturnService.getPurchaseReturnPage(pageReqVO);
        return success(Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildPurchaseReturnVOPageResultWithoutItems(pageResult)
                : buildPurchaseReturnVOPageResult(pageResult));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得采购退货查询汇总")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<ErpPurchaseReturnSummaryRespVO> getPurchaseReturnSummary(
            @Valid ErpPurchaseReturnPageReqVO pageReqVO) {
        ErpPurchaseReturnSummaryRespVO summary = purchaseReturnService.getPurchaseReturnSummary(pageReqVO);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, summary);
        return success(summary);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购退货 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseReturnExcel(@Valid ErpPurchaseReturnPageReqVO pageReqVO,
                                          @RequestParam(value = "fields", required = false) String fields,
                                          HttpServletResponse response) throws IOException {
        List<ErpPurchaseReturnRespVO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = buildPurchaseReturnVOPageResult(new PageResult<>(
                    purchaseReturnService.getPurchaseReturnList(pageReqVO.getIds()),
                    (long) pageReqVO.getIds().size())).getList();
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = buildPurchaseReturnVOPageResult(purchaseReturnService.getPurchaseReturnPage(pageReqVO)).getList();
        }
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpPurchaseReturnExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "采购退货.xls", "数据", ErpPurchaseReturnExportRespVO.class,
                buildPurchaseReturnExportList(list), includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得采购退货导出字段")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getPurchaseReturnExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpPurchaseReturnExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    private PageResult<ErpPurchaseReturnRespVO> buildPurchaseReturnVOPageResult(PageResult<ErpPurchaseReturnDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchaseReturnItemDO> purchaseReturnItemList = purchaseReturnService.getPurchaseReturnItemListByReturnIds(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getId));
        Map<Long, List<ErpPurchaseReturnItemDO>> purchaseReturnItemMap = convertMultiMap(purchaseReturnItemList, ErpPurchaseReturnItemDO::getReturnId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(purchaseReturnItemList, ErpPurchaseReturnItemDO::getProductId)));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));
        PageResult<ErpPurchaseReturnRespVO> respResult = BeanUtils.toBean(pageResult, ErpPurchaseReturnRespVO.class, purchaseReturn -> {
            List<ErpPurchaseReturnItemDO> itemList = purchaseReturnItemMap.getOrDefault(purchaseReturn.getId(), java.util.Collections.emptyList());
            List<ErpPurchaseReturnRespVO.Item> respItems = BeanUtils.toBean(itemList, ErpPurchaseReturnRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductCode(product.getCode()).setProductBarCode(product.getBarCode())
                            .setProductUnitName(product.getUnitName()).setWeight(product.getWeight())
                            .setStandard(product.getStandard()).setFeatureCode(product.getFeatureCode())
                            .setFactoryCode(product.getFactoryCode()).setLastPurchasePrice(product.getLastPurchasePrice())
                            .setDrawingNo(item.getDrawingNo() != null ? item.getDrawingNo() : product.getDrawingNo())
                            .setBrand(item.getBrand() != null ? item.getBrand() : product.getBrand())
                            .setVehicleModel(item.getVehicleModel() != null ? item.getVehicleModel() : product.getVehicleModel())
                            .setOriginPlace(item.getOriginPlace() != null ? item.getOriginPlace() : product.getOriginPlace())
                            .setPackageQty(item.getPackageQty() == null || item.getPackageQty() <= 0
                                    ? product.getPackageQty() : item.getPackageQty())
                            .setBatchNoEnabled(product.getBatchNoEnabled())));
            itemPriceReferenceFiller.fill(respItems);
            purchaseReturn.setItems(respItems);
            purchaseReturn.setItemCount(itemList.size());
            purchaseReturn.setProductNames(CollUtil.join(purchaseReturn.getItems(), "，", ErpPurchaseReturnRespVO.Item::getProductName));
            MapUtils.findAndThen(supplierMap, purchaseReturn.getSupplierId(), supplier -> purchaseReturn.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, purchaseReturn.getDeptId(), dept -> purchaseReturn.setDeptName(dept.getName()));
            fillUserNames(purchaseReturn, userMap);
        });
        fieldPermissionMasker.maskList(FIELD_PERMISSION_MODULE, respResult.getList());
        return respResult;
    }

    private PageResult<ErpPurchaseReturnRespVO> buildPurchaseReturnVOPageResultWithoutItems(
            PageResult<ErpPurchaseReturnDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> returnIds = convertSet(pageResult.getList(), ErpPurchaseReturnDO::getId);
        Map<Long, Integer> itemCountMap = purchaseReturnItemMapper.selectItemCountMapByReturnIds(returnIds);
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));
        PageResult<ErpPurchaseReturnRespVO> respResult = BeanUtils.toBean(pageResult, ErpPurchaseReturnRespVO.class,
                purchaseReturn -> {
                    purchaseReturn.setItemCount(itemCountMap.getOrDefault(purchaseReturn.getId(), 0));
                    MapUtils.findAndThen(supplierMap, purchaseReturn.getSupplierId(),
                            supplier -> purchaseReturn.setSupplierName(supplier.getName()));
                    MapUtils.findAndThen(deptMap, purchaseReturn.getDeptId(),
                            dept -> purchaseReturn.setDeptName(dept.getName()));
                    fillUserNames(purchaseReturn, userMap);
                });
        fieldPermissionMasker.maskList(FIELD_PERMISSION_MODULE, respResult.getList());
        return respResult;
    }

    private PageResult<UserSimpleRespVO> buildUserSimplePage(PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = page.getList().stream()
                .map(user -> new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private List<ErpPurchaseReturnRespVO.Item> buildPurchaseReturnItemVOList(List<ErpPurchaseReturnItemDO> itemList,
                                                                            Long currentReturnId) {
        if (CollUtil.isEmpty(itemList)) {
            return new ArrayList<>();
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(itemList, ErpPurchaseReturnItemDO::getProductId)));
        Set<Long> productIds = convertSet(itemList, ErpPurchaseReturnItemDO::getProductId);
        Set<Long> warehouseIds = convertSet(itemList, ErpPurchaseReturnItemDO::getWarehouseId);
        productIds.remove(null);
        warehouseIds.remove(null);
        Map<String, ErpStockDO> stockMap = CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds)
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> stockService.getStockMap(productIds, warehouseIds));
        Map<Long, ErpPurchaseInItemDO> inItemMap = java.util.Collections.emptyMap();
        Map<Long, BigDecimal> returnedMap = java.util.Collections.emptyMap();
        Set<Long> sourceInItemIds = convertSet(itemList, ErpPurchaseReturnItemDO::getSourceInItemId);
        sourceInItemIds.remove(null);
        if (CollUtil.isNotEmpty(sourceInItemIds)) {
            List<ErpPurchaseInItemDO> inItems = purchaseInItemMapper.selectBatchIds(sourceInItemIds);
            inItemMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                    inItems, ErpPurchaseInItemDO::getId);
            returnedMap = purchaseReturnItemMapper.selectProcessingAndApprovedReturnedCountMapBySourceInItemIdsExcludeReturn(
                    sourceInItemIds, currentReturnId);
        }
        Map<Long, ErpPurchaseInItemDO> finalInItemMap = inItemMap;
        Map<Long, BigDecimal> finalReturnedMap = returnedMap;
        List<ErpPurchaseReturnRespVO.Item> respItems = BeanUtils.toBean(itemList, ErpPurchaseReturnRespVO.Item.class, item -> {
            ErpStockDO stock = stockMap.get(item.getProductId() + "_" + item.getWarehouseId());
            item.setStockCount(stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO);
            MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                    .setProductCode(product.getCode()).setProductBarCode(product.getBarCode())
                    .setProductUnitName(product.getUnitName()).setWeight(product.getWeight())
                    .setStandard(product.getStandard()).setFeatureCode(product.getFeatureCode())
                    .setFactoryCode(product.getFactoryCode()).setLastPurchasePrice(product.getLastPurchasePrice())
                    .setDrawingNo(item.getDrawingNo() != null ? item.getDrawingNo() : product.getDrawingNo())
                    .setBrand(item.getBrand() != null ? item.getBrand() : product.getBrand())
                    .setVehicleModel(item.getVehicleModel() != null ? item.getVehicleModel() : product.getVehicleModel())
                    .setOriginPlace(item.getOriginPlace() != null ? item.getOriginPlace() : product.getOriginPlace())
                    .setPackageQty(item.getPackageQty() == null || item.getPackageQty() <= 0
                            ? product.getPackageQty() : item.getPackageQty())
                    .setBatchNoEnabled(product.getBatchNoEnabled()));
            if (item.getSourceInItemId() != null) {
                ErpPurchaseInItemDO inItem = finalInItemMap.get(item.getSourceInItemId());
                if (inItem != null) {
                    item.setInCount(inItem.getCount());
                    BigDecimal otherReturned = finalReturnedMap.getOrDefault(item.getSourceInItemId(), BigDecimal.ZERO);
                    item.setReturnableCount(inItem.getCount().subtract(otherReturned).max(BigDecimal.ZERO));
                }
            }
        });
        itemPriceReferenceFiller.fill(respItems);
        return respItems;
    }

    private List<ErpPurchaseReturnExportRespVO> buildPurchaseReturnExportList(List<ErpPurchaseReturnRespVO> list) {
        Set<Long> warehouseIds = new LinkedHashSet<>();
        Set<Long> itemDeptIds = new LinkedHashSet<>();
        Set<Long> productIds = new LinkedHashSet<>();
        Set<Long> sourceInItemIds = new LinkedHashSet<>();
        for (ErpPurchaseReturnRespVO purchaseReturn : list) {
            for (ErpPurchaseReturnRespVO.Item item : CollUtil.emptyIfNull(purchaseReturn.getItems())) {
                if (item.getWarehouseId() != null) {
                    warehouseIds.add(item.getWarehouseId());
                }
                if (item.getDeptId() != null) {
                    itemDeptIds.add(item.getDeptId());
                }
                if (item.getProductId() != null) {
                    productIds.add(item.getProductId());
                }
                if (item.getSourceInItemId() != null) {
                    sourceInItemIds.add(item.getSourceInItemId());
                }
            }
        }
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(warehouseIds)
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
        Map<Long, DeptRespDTO> itemDeptMap = CollUtil.isEmpty(itemDeptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(itemDeptIds);
        Map<String, ErpStockDO> stockMap = CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds)
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> stockService.getStockMap(productIds, warehouseIds));
        Map<Long, ErpPurchaseInItemDO> sourceInItemMap = CollUtil.isEmpty(sourceInItemIds)
                ? Collections.emptyMap()
                : cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                        purchaseInItemMapper.selectBatchIds(sourceInItemIds), ErpPurchaseInItemDO::getId);
        Map<Long, BigDecimal> returnedCountMap = CollUtil.isEmpty(sourceInItemIds)
                ? Collections.emptyMap()
                : purchaseReturnItemMapper.selectProcessingAndApprovedReturnedCountMapBySourceInItemIdsExcludeReturn(
                        sourceInItemIds, null);
        List<ErpPurchaseReturnExportRespVO> rows = new ArrayList<>();
        for (ErpPurchaseReturnRespVO purchaseReturn : list) {
            if (CollUtil.isEmpty(purchaseReturn.getItems())) {
                rows.add(buildPurchaseReturnExportRow(purchaseReturn, null, null, null,
                        null, null, null, true));
                continue;
            }
            // 导出与编辑页一样排除本单占用，同来源多行须一起归还本单额度。
            Map<Long, BigDecimal> currentCounts = new LinkedHashMap<>();
            if (ErpAuditStatus.PROCESS.getStatus().equals(purchaseReturn.getStatus())
                    || ErpAuditStatus.APPROVE.getStatus().equals(purchaseReturn.getStatus())) {
                for (ErpPurchaseReturnRespVO.Item item : purchaseReturn.getItems()) {
                    if (item.getSourceInItemId() != null && item.getCount() != null) {
                        currentCounts.merge(item.getSourceInItemId(), item.getCount(), BigDecimal::add);
                    }
                }
            }
            for (int i = 0; i < purchaseReturn.getItems().size(); i++) {
                ErpPurchaseReturnRespVO.Item item = purchaseReturn.getItems().get(i);
                ErpStockDO stock = stockMap.get(item.getProductId() + "_" + item.getWarehouseId());
                rows.add(buildPurchaseReturnExportRow(purchaseReturn,
                        item,
                        warehouseMap.get(item.getWarehouseId()),
                        itemDeptMap.get(item.getDeptId()),
                        stock,
                        sourceInItemMap.get(item.getSourceInItemId()),
                        returnedCountMap.getOrDefault(item.getSourceInItemId(), BigDecimal.ZERO)
                                .subtract(currentCounts.getOrDefault(item.getSourceInItemId(), BigDecimal.ZERO)),
                        i == 0));
            }
        }
        return rows;
    }

    private ErpPurchaseReturnExportRespVO buildPurchaseReturnExportRow(ErpPurchaseReturnRespVO purchaseReturn,
                                                                       ErpPurchaseReturnRespVO.Item item,
                                                                       ErpWarehouseDO warehouse,
                                                                       DeptRespDTO itemDept,
                                                                       ErpStockDO stock,
                                                                       ErpPurchaseInItemDO sourceInItem,
                                                                       BigDecimal returnedCount,
                                                                       boolean fillOrderFields) {
        ErpPurchaseReturnExportRespVO row = fillOrderFields
                ? BeanUtils.toBean(purchaseReturn, ErpPurchaseReturnExportRespVO.class)
                : new ErpPurchaseReturnExportRespVO();
        if (fillOrderFields) {
            row.setSupplierName(purchaseReturn.getSupplierName());
            row.setReturnTime(purchaseReturn.getReturnTime());
            row.setReturnMode(purchaseReturn.getReturnMode());
            row.setStatus(purchaseReturn.getStatus());
            row.setPurchaserName(purchaseReturn.getPurchaserName());
            row.setHandlerName(purchaseReturn.getHandlerName());
            row.setTotalTaxAmount(purchaseReturn.getTotalTaxPrice());
            row.setMainTaxPercent(purchaseReturn.getTaxRate());
            row.setCreatorName(purchaseReturn.getCreatorName());
            row.setUpdaterName(purchaseReturn.getUpdaterName());
            row.setUpdateTime(purchaseReturn.getUpdateTime());
            row.setRemark(purchaseReturn.getRemark());
        }
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setItemDeptName(itemDept != null ? itemDept.getName() : null);
        row.setStockCount(stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO);
        row.setInCount(sourceInItem != null ? sourceInItem.getCount() : item.getInCount());
        if (sourceInItem != null) {
            BigDecimal otherReturned = returnedCount == null ? BigDecimal.ZERO : returnedCount;
            row.setReturnableCount(sourceInItem.getCount().subtract(otherReturned).max(BigDecimal.ZERO));
        } else {
            row.setReturnableCount(item.getReturnableCount());
        }
        row.setWeight(item.getWeight());
        row.setPackageQty(item.getPackageQty());
        row.setWholeQty(item.getWholeQty());
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setLastPurchasePrice(item.getLastPurchasePrice());
        row.setItemTotalPrice(item.getProductPrice() == null || item.getCount() == null
                ? null : item.getProductPrice().multiply(item.getCount()));
        row.setSourceInNo(item.getSourceInNo());
        row.setItemTaxPercent(item.getTaxPercent());
        row.setItemTaxPrice(item.getTaxPrice());
        row.setWarehouseName(warehouse != null ? warehouse.getName() : null);
        row.setWarehousePosition(item.getWarehousePosition());
        row.setBatchNo(item.getBatchNo());
        row.setBrand(item.getBrand());
        row.setStandard(item.getStandard());
        row.setFeatureCode(item.getFeatureCode());
        row.setFactoryCode(item.getFactoryCode());
        row.setPartCode(item.getPartCode());
        row.setPartName(item.getPartName());
        row.setVehicleModel(item.getVehicleModel());
        row.setOriginPlace(item.getOriginPlace());
        row.setProductBarCode(item.getProductBarCode());
        row.setDrawingNo(item.getDrawingNo());
        row.setBarCode(item.getBarCode());
        row.setBusinessEntity(item.getBusinessEntity());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private Set<Long> collectUserIds(List<ErpPurchaseReturnDO> list) {
        Set<Long> userIds = new LinkedHashSet<>();
        list.forEach(purchaseReturn -> {
            addUserId(userIds, purchaseReturn.getCreator());
            addUserId(userIds, purchaseReturn.getUpdater());
            addUserId(userIds, purchaseReturn.getPurchaser());
            if (purchaseReturn.getHandler() != null) {
                userIds.add(purchaseReturn.getHandler());
            }
        });
        return userIds;
    }

    private void fillUserNames(ErpPurchaseReturnRespVO purchaseReturn, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(purchaseReturn.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> purchaseReturn.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(purchaseReturn.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> purchaseReturn.setUpdaterName(user.getNickname()));
        }
        Long purchaserId = parseUserId(purchaseReturn.getPurchaser());
        if (purchaserId != null) {
            MapUtils.findAndThen(userMap, purchaserId, user -> purchaseReturn.setPurchaserName(user.getNickname()));
        }
        if (purchaseReturn.getHandler() != null) {
            MapUtils.findAndThen(userMap, purchaseReturn.getHandler(),
                    user -> purchaseReturn.setHandlerName(user.getNickname()));
        }
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
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("supplierName", "main");
        map.put("deptName", "main");
        map.put("returnTime", "main");
        map.put("returnMode", "main");
        map.put("purchaserName", "main");
        map.put("handlerName", "main");
        map.put("status", "main");
        map.put("totalCount", "main");
        map.put("totalProductPrice", "main");
        map.put("totalTaxAmount", "main");
        map.put("discountPercent", "main");
        map.put("discountPrice", "main");
        map.put("otherPrice", "main");
        map.put("refundPrice", "main");
        map.put("totalPrice", "main");
        map.put("mainTaxPercent", "main");
        map.put("invoiceType", "main");
        map.put("transportMethod", "main");
        map.put("settleMethod", "main");
        map.put("freightAmount", "main");
        map.put("logisticsCompany", "main");
        map.put("factoryOrderNo", "main");
        map.put("priority", "main");
        map.put("orderMethod", "main");
        map.put("createTime", "system");
        map.put("fileUrl", "main");
        map.put("creatorName", "system");
        map.put("updaterName", "system");
        map.put("updateTime", "system");
        map.put("feeAmount", "main");
        map.put("remark", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("itemDeptName", "detail");
        map.put("stockCount", "detail");
        map.put("inCount", "detail");
        map.put("returnableCount", "detail");
        map.put("weight", "detail");
        map.put("packageQty", "detail");
        map.put("wholeQty", "detail");
        map.put("sourceInNo", "detail");
        map.put("itemCount", "detail");
        map.put("productPrice", "detail");
        map.put("lastPurchasePrice", "detail");
        map.put("itemTotalPrice", "detail");
        map.put("itemTaxPercent", "detail");
        map.put("itemTaxPrice", "detail");
        map.put("warehouseName", "detail");
        map.put("warehousePosition", "detail");
        map.put("batchNo", "detail");
        map.put("brand", "detail");
        map.put("standard", "detail");
        map.put("featureCode", "detail");
        map.put("factoryCode", "detail");
        map.put("partCode", "detail");
        map.put("partName", "detail");
        map.put("vehicleModel", "detail");
        map.put("originPlace", "detail");
        map.put("productBarCode", "detail");
        map.put("drawingNo", "detail");
        map.put("barCode", "detail");
        map.put("businessEntity", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("supplierName", "supplierId");
        map.put("deptName", "deptId");
        map.put("purchaserName", "purchaser");
        map.put("handlerName", "handler");
        map.put("totalProductPrice", "totalProductPrice");
        map.put("totalTaxAmount", "totalTaxPrice");
        map.put("mainTaxPercent", "taxRate");
        map.put("discountPercent", "discountPercent");
        map.put("discountPrice", "discountPrice");
        map.put("otherPrice", "otherPrice");
        map.put("refundPrice", "refundPrice");
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productName");
        map.put("productUnitName", "item_productUnitName");
        map.put("itemDeptName", "item_deptId");
        map.put("stockCount", "item_stockCount");
        map.put("inCount", "item_inCount");
        map.put("returnableCount", "item_returnableCount");
        map.put("weight", "item_weight");
        map.put("packageQty", "item_packageQty");
        map.put("wholeQty", "item_wholeQty");
        map.put("itemCount", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("lastPurchasePrice", "item_lastPurchasePrice");
        map.put("itemTotalPrice", "item_totalProductPrice");
        map.put("itemTaxPercent", "item_taxPercent");
        map.put("itemTaxPrice", "item_taxPrice");
        map.put("warehouseName", "item_warehouseId");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("batchNo", "item_batchNo");
        map.put("brand", "item_brand");
        map.put("standard", "item_standard");
        map.put("featureCode", "item_featureCode");
        map.put("factoryCode", "item_factoryCode");
        map.put("partCode", "item_partCode");
        map.put("partName", "item_partName");
        map.put("vehicleModel", "item_vehicleModel");
        map.put("originPlace", "item_originPlace");
        map.put("productBarCode", "item_productBarCode");
        map.put("drawingNo", "item_drawingNo");
        map.put("barCode", "item_barCode");
        map.put("businessEntity", "item_businessEntity");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
