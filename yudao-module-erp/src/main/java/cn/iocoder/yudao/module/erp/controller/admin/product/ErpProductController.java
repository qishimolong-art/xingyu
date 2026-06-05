package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductSaveReqVO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
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
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 产品")
@RestController
@RequestMapping("/erp/product")
@Validated
public class ErpProductController {

    @Resource
    private ErpProductService productService;

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
        return success(productService.getProductDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得产品分页")
    @PreAuthorize("@ss.hasPermission('erp:product:query')")
    public CommonResult<PageResult<ErpProductRespVO>> getProductPage(@Valid ErpProductPageReqVO pageReqVO) {
        return success(productService.getProductVOPage(pageReqVO));
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
                .setMinPrice(product.getMinPrice())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品 Excel")
    @PreAuthorize("@ss.hasPermission('erp:product:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductExcel(@Valid ErpProductPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpProductRespVO> pageResult = productService.getProductVOPage(pageReqVO);
        ExcelUtils.write(response, "产品.xls", "数据", ErpProductRespVO.class, pageResult.getList());
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获取产品导入模板")
    @PreAuthorize("@ss.hasPermission('erp:product:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "产品导入模板.xls", "产品", ErpProductImportExcelVO.class,
                Collections.singletonList(new ErpProductImportExcelVO()));
    }

    @PostMapping("/import")
    @Operation(summary = "导入产品")
    @PreAuthorize("@ss.hasPermission('erp:product:import')")
    public CommonResult<ErpProductImportRespVO> importProduct(@RequestParam("file") MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        List<ErpProductImportExcelVO> list = filename.endsWith(".csv")
                ? productService.parseCsvImport(new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
                : ExcelUtils.read(file, ErpProductImportExcelVO.class);
        return success(productService.importProductList(list));
    }

    @PutMapping("/update-shelf")
    @Operation(summary = "批量修改产品货架位")
    @PreAuthorize("@ss.hasPermission('erp:product:update')")
    public CommonResult<Boolean> updateProductsShelf(@RequestParam("ids") List<Long> ids,
                                                     @RequestParam("shelf") String shelf) {
        productService.updateProductsShelf(ids, shelf);
        return success(true);
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
