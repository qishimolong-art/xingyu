package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 产品单位")
@RestController
@RequestMapping("/erp/product-unit")
@Validated
public class ErpProductUnitController {

    private static final Set<String> PRODUCT_UNIT_IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "name", "status"));

    @Resource
    private ErpProductUnitService productUnitService;
    @Resource
    private ErpSimpleArchiveImportTaskService archiveImportTaskService;
    @Resource
    private ErpSimpleArchiveExportTaskService archiveExportTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建产品单位")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:create')")
    public CommonResult<Long> createProductUnit(@Valid @RequestBody ErpProductUnitSaveReqVO createReqVO) {
        return success(productUnitService.createProductUnit(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新产品单位")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:update')")
    public CommonResult<Boolean> updateProductUnit(@RequestBody ErpProductUnitSaveReqVO updateReqVO) {
        productUnitService.updateProductUnit(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量修改产品单位")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:update')")
    public CommonResult<Boolean> batchUpdateProductUnit(@Valid @RequestBody ErpProductUnitBatchUpdateReqVO updateReqVO) {
        productUnitService.batchUpdateProductUnit(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产品单位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:product-unit:delete')")
    public CommonResult<Boolean> deleteProductUnit(@RequestParam("id") Long id) {
        productUnitService.deleteProductUnit(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除产品单位")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('erp:product-unit:delete')")
    public CommonResult<Boolean> deleteProductUnitList(@RequestParam("ids") List<Long> ids) {
        productUnitService.deleteProductUnitList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得产品单位")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:query')")
    public CommonResult<ErpProductUnitRespVO> getProductUnit(@RequestParam("id") Long id) {
        ErpProductUnitDO productUnit = productUnitService.getProductUnit(id);
        return success(BeanUtils.toBean(productUnit, ErpProductUnitRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得产品单位分页")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:query')")
    public CommonResult<PageResult<ErpProductUnitRespVO>> getProductUnitPage(@Valid ErpProductUnitPageReqVO pageReqVO) {
        PageResult<ErpProductUnitDO> pageResult = productUnitService.getProductUnitPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpProductUnitRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得产品单位精简列表", description = "只包含被开启的单位，主要用于前端的下拉选项")
    public CommonResult<List<ErpProductUnitRespVO>> getProductUnitSimpleList() {
        List<ErpProductUnitDO> list = productUnitService.getProductUnitListByStatusForCurrentUser(
                CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, unit -> new ErpProductUnitRespVO().setId(unit.getId()).setName(unit.getName())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品单位 Excel")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductUnitExcel(@Valid ErpProductUnitPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpProductUnitDO> list = productUnitService.getProductUnitPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "产品单位.xls", "数据", ErpProductUnitRespVO.class,
                        BeanUtils.toBean(list, ErpProductUnitRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得产品单位导入模板")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "产品单位导入模板.xls", "产品单位",
                ErpProductUnitImportExcelVO.class,
                Collections.singletonList(new ErpProductUnitImportExcelVO()),
                PRODUCT_UNIT_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入产品单位")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:import')")
    public CommonResult<ErpProductUnitImportRespVO> importProductUnit(@RequestParam("file") MultipartFile file)
            throws Exception {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        List<ErpProductUnitImportExcelVO> list = filename.endsWith(".csv")
                ? productUnitService.parseCsvImport(new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                : ExcelUtils.read(file, ErpProductUnitImportExcelVO.class);
        return success(productUnitService.importProductUnitList(list));
    }

    @PostMapping("/import-task/start")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:import')")
    public CommonResult<ErpArchiveImportTaskStartRespVO> startImportTask(@RequestParam("file") MultipartFile file)
            throws IOException {
        return success(archiveImportTaskService.startTask(ErpSimpleArchiveModule.PRODUCT_UNIT, file));
    }

    @GetMapping("/import-task/active")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:import')")
    public CommonResult<ErpArchiveImportTaskRespVO> getActiveImportTask() {
        return success(archiveImportTaskService.getActiveTask(ErpSimpleArchiveModule.PRODUCT_UNIT));
    }

    @GetMapping("/import-task/get")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:import')")
    public CommonResult<ErpArchiveImportTaskRespVO> getImportTask(@RequestParam("id") Long id) {
        return success(archiveImportTaskService.getTask(ErpSimpleArchiveModule.PRODUCT_UNIT, id));
    }

    @GetMapping("/import-failure-details/download")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        archiveImportTaskService.downloadFailureDetails(ErpSimpleArchiveModule.PRODUCT_UNIT, recordId, response);
    }

    @PostMapping("/export-task/start")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:export')")
    public CommonResult<ErpArchiveExportTaskStartRespVO> startExportTask(
            @Valid @RequestBody ErpProductUnitPageReqVO reqVO) {
        return success(archiveExportTaskService.startUnitTask(reqVO));
    }

    @GetMapping("/export-task/active")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:export')")
    public CommonResult<ErpArchiveExportTaskRespVO> getActiveExportTask() {
        return success(archiveExportTaskService.getActiveTask(ErpSimpleArchiveModule.PRODUCT_UNIT));
    }

    @GetMapping("/export-task/get")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:export')")
    public CommonResult<ErpArchiveExportTaskRespVO> getExportTask(@RequestParam("id") Long id) {
        return success(archiveExportTaskService.getTask(ErpSimpleArchiveModule.PRODUCT_UNIT, id));
    }

    @GetMapping("/export-task/history")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:export')")
    public CommonResult<PageResult<ErpArchiveExportTaskRespVO>> getExportHistory(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return success(archiveExportTaskService.getHistory(ErpSimpleArchiveModule.PRODUCT_UNIT, pageNo, pageSize));
    }

    @GetMapping("/export-task/download")
    @PreAuthorize("@ss.hasPermission('erp:product-unit:export')")
    public void downloadExportTask(@RequestParam("id") Long id,
                                   HttpServletResponse response) throws IOException {
        archiveExportTaskService.downloadTask(ErpSimpleArchiveModule.PRODUCT_UNIT, id, response);
    }

}
