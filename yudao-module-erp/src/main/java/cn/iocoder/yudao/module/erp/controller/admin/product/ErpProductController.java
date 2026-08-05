package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpArchiveMergeReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductBatchDisableReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductStockDistributionBatchSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductStockDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductStockDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchUpdatePriceFieldsReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchAdjustPriceReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchAdjustStockLimitsReqVO;
import cn.iocoder.yudao.module.erp.service.common.ErpExportCaptchaService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
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
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_FIELD_NO_PERMISSION;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_EXPORT_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PRODUCT_TYPE;

@Tag(name = "管理后台 - ERP 产品")
@RestController
@RequestMapping("/erp/product")
@Validated
public class ErpProductController {

    private static final Set<String> PRODUCT_IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "code", "name", "barCode", "categoryName", "batchNoEnabled", "unitName", "status", "defaultWarehouseName", "vehicleModel", "factoryCode",
            "purchasePrice", "salePrice", "minPrice", "standard", "remark", "expiryDay", "weight",
            "referencePrice", "retailPrice", "lastPurchasePrice", "grossProfitRate", "backupPrice1",
            "wholesalePrice", "sharePrice", "stockMax", "stockMin", "stockStandard", "packageQty", "mainImage",
            "detailContent"));
    private static final Set<String> PRODUCT_EXPORT_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "id", "code", "deptName", "productCode", "name", "barCode", "categoryName", "batchNoEnabled",
            "unitName", "status", "standard", "remark", "expiryDay", "weight", "purchasePrice", "salePrice",
            "minPrice", "vehicleModel", "factoryCode", "sharePrice", "currentStock", "lockCount", "createTime",
            "creatorName", "updateTime", "updaterName"));

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpExportCaptchaService exportCaptchaService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private PermissionApi permissionApi;

    @PostMapping("/create")
    @Operation(summary = "创建产品")
    @PreAuthorize("@ss.hasPermission('erp:product:create')")
    public CommonResult<Long> createProduct(@Valid @RequestBody ProductSaveReqVO createReqVO) {
        return success(productService.createProduct(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新产品")
    @PreAuthorize("@ss.hasPermission('erp:product:update')")
    public CommonResult<Boolean> updateProduct(@RequestBody ProductSaveReqVO updateReqVO) {
        productService.updateProduct(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量修改产品")
    @PreAuthorize("@ss.hasPermission('erp:product:update')")
    public CommonResult<Boolean> batchUpdateProduct(@Valid @RequestBody ProductBatchUpdateReqVO updateReqVO) {
        productService.batchUpdateProduct(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-disable")
    @Operation(summary = "批量停用产品")
    @PreAuthorize("@ss.hasPermission('erp:product:update')")
    public CommonResult<Boolean> batchDisableProduct(@Valid @RequestBody ErpProductBatchDisableReqVO reqVO) {
        productService.batchDisableProduct(reqVO.getIds());
        return success(true);
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复配件")
    @PreAuthorize("@ss.hasPermission('erp:product:update')")
    public CommonResult<Boolean> restoreProduct(@Valid @RequestBody ErpProductBatchDisableReqVO reqVO) {
        productService.restoreProduct(reqVO.getIds());
        return success(true);
    }

    @PutMapping("/merge")
    @Operation(summary = "合并配件")
    @PreAuthorize("@ss.hasPermission('erp:product:merge')")
    public CommonResult<Boolean> mergeProduct(@Valid @RequestBody ErpArchiveMergeReqVO reqVO) {
        productService.mergeProduct(reqVO.getSourceId(), reqVO.getKeepId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产品")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:product:delete')")
    public CommonResult<Boolean> deleteProduct(@RequestParam("id") Long id) {
        productService.deleteProduct(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得产品")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<ErpProductRespVO> getProduct(@RequestParam("id") Long id) {
        return success(productService.getProductDetail(id));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得产品完整详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<ErpProductRespVO> getProductDetail(@RequestParam("id") Long id) {
        return success(productService.getProductArchiveDetail(id));
    }

    @GetMapping("/stock-distribution")
    @Operation(summary = "获得配件库存分发")
    @Parameter(name = "productId", description = "配件编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<ErpProductStockDistributionRespVO> getProductStockDistribution(
            @RequestParam("productId") Long productId) {
        return success(productService.getProductStockDistribution(productId));
    }

    @PutMapping("/stock-distribution")
    @Operation(summary = "更新配件库存分发")
    @PreAuthorize("@ss.hasPermission('erp:product:stock-distribute')")
    public CommonResult<Boolean> updateProductStockDistribution(
            @Valid @RequestBody ErpProductStockDistributionSaveReqVO reqVO) {
        productService.updateProductStockDistribution(reqVO);
        return success(true);
    }

    @PutMapping("/stock-distribution/batch")
    @Operation(summary = "批量更新配件库存分发")
    @PreAuthorize("@ss.hasPermission('erp:product:stock-distribute')")
    public CommonResult<Boolean> batchUpdateProductStockDistribution(
            @Valid @RequestBody ErpProductStockDistributionBatchSaveReqVO reqVO) {
        productService.batchUpdateProductStockDistribution(reqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得产品分页")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<PageResult<ErpProductRespVO>> getProductPage(@Valid ErpProductPageReqVO pageReqVO) {
        return success(productService.getProductVOPage(pageReqVO));
    }

    @GetMapping("/price-adjust-page")
    @Operation(summary = "获得配件价格调整列表")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<PageResult<ErpProductRespVO>> getPartsPriceAdjustPage(@Valid ErpProductPageReqVO pageReqVO) {
        return success(productService.getProductVOPage(pageReqVO, false));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得产品精简列表")
    public CommonResult<List<ErpProductRespVO>> getProductSimpleList() {
        List<ErpProductRespVO> list = productService.getProductVOListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, product -> new ErpProductRespVO().setId(product.getId())
                .setCode(product.getCode())
                .setProductCode(product.getCode())
                .setName(product.getName())
                .setBarCode(product.getBarCode())
                .setCategoryId(product.getCategoryId())
                .setCategoryName(product.getCategoryName())
                .setUnitId(product.getUnitId())
                .setUnitName(product.getUnitName())
                .setPurchasePrice(product.getPurchasePrice())
                .setSalePrice(product.getSalePrice())
                .setMinPrice(product.getMinPrice())
                .setSharePrice(product.getSharePrice())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品 Excel")
    @PreAuthorize("@ss.hasPermission('erp:product:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductExcel(@Valid ErpProductPageReqVO pageReqVO,
                                   @RequestParam(value = "captchaCode", required = false) String captchaCode,
                                   @RequestParam(value = "verifyCode", required = false) String verifyCode,
                                   HttpServletResponse response) throws IOException {
        exportCaptchaService.validate(captchaCode, verifyCode);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpProductRespVO> pageResult = productService.getProductVOPage(pageReqVO);
        operateLogService.record(ERP_PRODUCT_TYPE, ERP_EXPORT_SUB_TYPE, 0L,
                "导出配件信息，导出数量：" + pageResult.getList().size(), "产品导出");
        ExcelUtils.write(response, "产品.xls", "数据", ErpProductRespVO.class, pageResult.getList(),
                filterVisibleExcelFields(PRODUCT_EXPORT_FIELDS));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获取产品导入模板")
    @PreAuthorize("@ss.hasPermission('erp:product:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "产品导入模板.xls", "产品", ErpProductImportExcelVO.class,
                Collections.singletonList(new ErpProductImportExcelVO()),
                filterVisibleExcelFields(PRODUCT_IMPORT_TEMPLATE_FIELDS));
    }

    @PostMapping("/import")
    @Operation(summary = "导入产品")
    @PreAuthorize("@ss.hasPermission('erp:product:import')")
    public CommonResult<ErpProductImportRespVO> importProduct(@RequestParam("file") MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String filename = originalFilename.toLowerCase();
        List<ErpProductImportExcelVO> list = filename.endsWith(".csv")
                ? productService.parseCsvImport(new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
                : ExcelUtils.read(file, ErpProductImportExcelVO.class);
        return success(productService.importProductList(list));
    }

    private Set<String> filterVisibleExcelFields(Set<String> candidateFields) {
        Set<String> hiddenFields = new HashSet<>(permissionApi.getCurrentUserHiddenFields("erp_product"));
        Set<String> visibleFields = candidateFields.stream()
                .filter(field -> !isFieldHidden(hiddenFields, getPermissionFieldKey(field)))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (visibleFields.isEmpty()) {
            throw exception(PRODUCT_FIELD_NO_PERMISSION, "可导入/导出字段");
        }
        return visibleFields;
    }

    private String getPermissionFieldKey(String excelField) {
        switch (excelField) {
            case "productCode": return "code";
            case "deptName": return "deptIds";
            case "categoryName": return "categoryId";
            case "unitName": return "unitId";
            default: return excelField;
        }
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

    @PutMapping("/update-shelf")
    @Operation(summary = "批量修改产品货架位")
    @PreAuthorize("@ss.hasPermission('erp:product:update')")
    public CommonResult<Boolean> updateProductsShelf(@RequestParam("ids") List<Long> ids,
                                                     @RequestParam("shelf") String shelf) {
        productService.updateProductsShelf(ids, shelf);
        return success(true);
    }

    @PutMapping("/batch-update-price-fields")
    @Operation(summary = "列表直接编辑保存价格/库存字段（无需口令）")
    @PreAuthorize("@ss.hasPermission('erp:product:update')")
    public CommonResult<Boolean> batchUpdatePriceFields(
            @RequestBody @Valid List<ErpPartsBatchUpdatePriceFieldsReqVO> reqList) {
        productService.batchUpdatePriceFields(reqList);
        return success(true);
    }

    @PutMapping("/batch-adjust-price")
    @Operation(summary = "批量调整配件价格")
    @PreAuthorize("@ss.hasPermission('erp:parts:adjust-price')")
    public CommonResult<Integer> batchAdjustPrice(
            @RequestBody @Valid ErpPartsBatchAdjustPriceReqVO reqVO) {
        int count = productService.batchAdjustPrice(reqVO);
        return success(count);
    }

    @PutMapping("/batch-adjust-stock-limits")
    @Operation(summary = "批量调整配件库存上下限（需口令）")
    @PreAuthorize("@ss.hasPermission('erp:parts:adjust-price')")
    public CommonResult<Integer> batchAdjustStockLimits(
            @RequestBody @Valid ErpPartsBatchAdjustStockLimitsReqVO reqVO) {
        int count = productService.batchAdjustStockLimits(reqVO);
        return success(count);
    }

    @GetMapping("/duplicate-shelf-ids")
    @Operation(summary = "查询货架位重复的产品 ID")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<List<Long>> getDuplicateShelfProductIds() {
        return success(productService.findDuplicateShelfProductIds());
    }

    @GetMapping("/empty-shelf-ids")
    @Operation(summary = "查询空置货架位的产品 ID")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<List<Long>> getEmptyShelfProductIds() {
        return success(productService.findEmptyShelfProductIds());
    }

}
