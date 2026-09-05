package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductBrandDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBrandService;
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

@Tag(name = "管理后台 - ERP 配件品牌")
@RestController
@RequestMapping("/erp/product-brand")
@Validated
public class ErpProductBrandController {

    private static final Set<String> PRODUCT_BRAND_IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "name", "status", "sort"));

    @Resource
    private ErpProductBrandService productBrandService;

    @PostMapping("/create")
    @Operation(summary = "创建配件品牌")
    @PreAuthorize("@ss.hasPermission('erp:product-brand:create')")
    public CommonResult<Long> createProductBrand(@Valid @RequestBody ErpProductBrandSaveReqVO createReqVO) {
        return success(productBrandService.createProductBrand(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新配件品牌")
    @PreAuthorize("@ss.hasPermission('erp:product-brand:update')")
    public CommonResult<Boolean> updateProductBrand(@RequestBody ErpProductBrandSaveReqVO updateReqVO) {
        productBrandService.updateProductBrand(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除配件品牌")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:product-brand:delete')")
    public CommonResult<Boolean> deleteProductBrand(@RequestParam("id") Long id) {
        productBrandService.deleteProductBrand(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除配件品牌")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('erp:product-brand:delete')")
    public CommonResult<Boolean> deleteProductBrandList(@RequestParam("ids") List<Long> ids) {
        productBrandService.deleteProductBrandList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得配件品牌")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:product-brand:query')")
    public CommonResult<ErpProductBrandRespVO> getProductBrand(@RequestParam("id") Long id) {
        ErpProductBrandDO productBrand = productBrandService.getProductBrand(id);
        return success(BeanUtils.toBean(productBrand, ErpProductBrandRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得配件品牌分页")
    @PreAuthorize("@ss.hasPermission('erp:product-brand:query')")
    public CommonResult<PageResult<ErpProductBrandRespVO>> getProductBrandPage(
            @Valid ErpProductBrandPageReqVO pageReqVO) {
        PageResult<ErpProductBrandDO> pageResult = productBrandService.getProductBrandPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpProductBrandRespVO.class));
    }

    @GetMapping("/list-all-simple")
    @Operation(summary = "获得配件品牌精简列表", description = "只包含被开启的品牌，主要用于前端的下拉选项")
    public CommonResult<List<ErpProductBrandRespVO>> getProductBrandSimpleList() {
        List<ErpProductBrandDO> list = productBrandService.getProductBrandListByStatusForCurrentUser(
                CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, brand -> new ErpProductBrandRespVO()
                .setId(brand.getId()).setName(brand.getName()).setStatus(brand.getStatus()).setSort(brand.getSort())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出配件品牌 Excel")
    @PreAuthorize("@ss.hasPermission('erp:product-brand:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductBrandExcel(@Valid ErpProductBrandPageReqVO pageReqVO,
                                        HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpProductBrandDO> list = productBrandService.getProductBrandPage(pageReqVO).getList();
        ExcelUtils.write(response, "配件品牌.xls", "数据", ErpProductBrandRespVO.class,
                BeanUtils.toBean(list, ErpProductBrandRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得配件品牌导入模板")
    @PreAuthorize("@ss.hasPermission('erp:product-brand:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "配件品牌导入模板.xls", "配件品牌",
                ErpProductBrandImportExcelVO.class,
                Collections.singletonList(new ErpProductBrandImportExcelVO()),
                PRODUCT_BRAND_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "导入配件品牌")
    @PreAuthorize("@ss.hasPermission('erp:product-brand:import')")
    public CommonResult<ErpProductBrandImportRespVO> importProductBrand(@RequestParam("file") MultipartFile file)
            throws Exception {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        List<ErpProductBrandImportExcelVO> list = filename.endsWith(".csv")
                ? productBrandService.parseCsvImport(new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                : ExcelUtils.read(file, ErpProductBrandImportExcelVO.class);
        return success(productBrandService.importProductBrandList(list));
    }

}
