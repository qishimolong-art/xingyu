package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategorySaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductCategoryService;
import cn.iocoder.yudao.module.erp.service.product.ErpSimpleArchiveExportTaskService;
import cn.iocoder.yudao.module.erp.service.product.ErpSimpleArchiveImportTaskService;
import cn.iocoder.yudao.module.erp.service.product.ErpSimpleArchiveModule;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 商品分类")
@RestController
@RequestMapping("/erp/product-category")
@Validated
public class ErpProductCategoryController {

    private static final Set<String> PRODUCT_CATEGORY_IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "parentCode", "name", "code", "status"));

    @Resource
    private ErpProductCategoryService productCategoryService;
    @Resource
    private ErpSimpleArchiveImportTaskService archiveImportTaskService;
    @Resource
    private ErpSimpleArchiveExportTaskService archiveExportTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建商品分类")
    @PreAuthorize("@ss.hasPermission('erp:product-category:create')")
    public CommonResult<Long> createProductCategory(@Valid @RequestBody ErpProductCategorySaveReqVO createReqVO) {
        return success(productCategoryService.createProductCategory(createReqVO));
    }

    @GetMapping("/next-code")
    @Operation(summary = "获得下一个商品分类编码")
    @PreAuthorize("@ss.hasPermission('erp:product-category:create')")
    public CommonResult<String> getNextProductCategoryCode(@RequestParam(value = "parentCode", required = false) String parentCode) {
        return success(productCategoryService.getNextProductCategoryCode(parentCode));
    }

    @PutMapping("/update")
    @Operation(summary = "更新商品分类")
    @PreAuthorize("@ss.hasPermission('erp:product-category:update')")
    public CommonResult<Boolean> updateProductCategory(@RequestBody ErpProductCategorySaveReqVO updateReqVO) {
        productCategoryService.updateProductCategory(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量更新商品分类")
    @PreAuthorize("@ss.hasPermission('erp:product-category:update')")
    public CommonResult<Boolean> batchUpdateProductCategory(@Valid @RequestBody ErpProductCategoryBatchUpdateReqVO updateReqVO) {
        productCategoryService.batchUpdateProductCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除商品分类")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:product-category:delete')")
    public CommonResult<Boolean> deleteProductCategory(@RequestParam("id") Long id) {
        productCategoryService.deleteProductCategory(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除商品分类")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('erp:product-category:delete')")
    public CommonResult<Boolean> deleteProductCategoryList(@RequestParam("ids") List<Long> ids) {
        productCategoryService.deleteProductCategoryList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得商品分类")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:product-category:query')")
    public CommonResult<ErpProductCategoryRespVO> getProductCategory(@RequestParam("id") Long id) {
        ErpProductCategoryDO category = productCategoryService.getProductCategory(id);
        return success(BeanUtils.toBean(category, ErpProductCategoryRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得商品分类列表")
    @PreAuthorize("@ss.hasPermission('erp:product-category:query')")
    public CommonResult<List<ErpProductCategoryRespVO>> getProductCategoryList(@Valid ErpProductCategoryListReqVO listReqVO) {
        List<ErpProductCategoryDO> list = productCategoryService.getProductCategoryList(listReqVO);
        return success(BeanUtils.toBean(list, ErpProductCategoryRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得商品分类精简列表", description = "只包含被开启的分类，主要用于前端的下拉选项")
    public CommonResult<List<ErpProductCategoryRespVO>> getProductCategorySimpleList() {
        List<ErpProductCategoryDO> list = productCategoryService.getProductCategoryList(
                new ErpProductCategoryListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        return success(convertList(list, category -> new ErpProductCategoryRespVO()
                .setId(category.getId()).setName(category.getName()).setParentId(category.getParentId())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出商品分类 Excel")
    @PreAuthorize("@ss.hasPermission('erp:product-category:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductCategoryExcel(@Valid ErpProductCategoryListReqVO listReqVO,
              HttpServletResponse response) throws IOException {
        List<ErpProductCategoryDO> list = productCategoryService.getProductCategoryList(listReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "商品分类.xls", "数据", ErpProductCategoryRespVO.class,
                        BeanUtils.toBean(list, ErpProductCategoryRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得商品分类导入模板")
    @PreAuthorize("@ss.hasPermission('erp:product-category:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "商品分类导入模板.xls", "商品分类",
                ErpProductCategoryImportExcelVO.class,
                Collections.singletonList(new ErpProductCategoryImportExcelVO()),
                PRODUCT_CATEGORY_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入商品分类")
    @PreAuthorize("@ss.hasPermission('erp:product-category:import')")
    public CommonResult<ErpProductCategoryImportRespVO> importProductCategory(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpProductCategoryImportExcelVO> list = ExcelUtils.read(file, ErpProductCategoryImportExcelVO.class);
        return success(productCategoryService.importProductCategoryList(list));
    }

    @PostMapping("/import-task/start")
    @PreAuthorize("@ss.hasPermission('erp:product-category:import')")
    public CommonResult<ErpArchiveImportTaskStartRespVO> startImportTask(@RequestParam("file") MultipartFile file)
            throws IOException {
        return success(archiveImportTaskService.startTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY, file));
    }

    @GetMapping("/import-task/active")
    @PreAuthorize("@ss.hasPermission('erp:product-category:import')")
    public CommonResult<ErpArchiveImportTaskRespVO> getActiveImportTask() {
        return success(archiveImportTaskService.getActiveTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY));
    }

    @GetMapping("/import-task/get")
    @PreAuthorize("@ss.hasPermission('erp:product-category:import')")
    public CommonResult<ErpArchiveImportTaskRespVO> getImportTask(@RequestParam("id") Long id) {
        return success(archiveImportTaskService.getTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY, id));
    }

    @GetMapping("/import-failure-details/download")
    @PreAuthorize("@ss.hasPermission('erp:product-category:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        archiveImportTaskService.downloadFailureDetails(ErpSimpleArchiveModule.PRODUCT_CATEGORY, recordId, response);
    }

    @PostMapping("/export-task/start")
    @PreAuthorize("@ss.hasPermission('erp:product-category:export')")
    public CommonResult<ErpArchiveExportTaskStartRespVO> startExportTask(
            @Valid @RequestBody ErpProductCategoryListReqVO reqVO) {
        return success(archiveExportTaskService.startCategoryTask(reqVO));
    }

    @GetMapping("/export-task/active")
    @PreAuthorize("@ss.hasPermission('erp:product-category:export')")
    public CommonResult<ErpArchiveExportTaskRespVO> getActiveExportTask() {
        return success(archiveExportTaskService.getActiveTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY));
    }

    @GetMapping("/export-task/get")
    @PreAuthorize("@ss.hasPermission('erp:product-category:export')")
    public CommonResult<ErpArchiveExportTaskRespVO> getExportTask(@RequestParam("id") Long id) {
        return success(archiveExportTaskService.getTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY, id));
    }

    @GetMapping("/export-task/history")
    @PreAuthorize("@ss.hasPermission('erp:product-category:export')")
    public CommonResult<PageResult<ErpArchiveExportTaskRespVO>> getExportHistory(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return success(archiveExportTaskService.getHistory(
                ErpSimpleArchiveModule.PRODUCT_CATEGORY, pageNo, pageSize));
    }

    @GetMapping("/export-task/download")
    @PreAuthorize("@ss.hasPermission('erp:product-category:export')")
    public void downloadExportTask(@RequestParam("id") Long id,
                                   HttpServletResponse response) throws IOException {
        archiveExportTaskService.downloadTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY, id, response);
    }

}
