package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitRespVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public enum ErpSimpleArchiveModule {

    PRODUCT_CATEGORY("erp_product_category", "配件分类", "配件分类", "配件分类",
            ErpProductCategoryImportExcelVO.class, ErpProductCategoryRespVO.class,
            fields("id", "parentId", "name", "code", "sort", "status", "createTime"), false),
    PRODUCT_BRAND("erp_product_brand", "配件品牌", "配件品牌", "配件品牌",
            ErpProductBrandImportExcelVO.class, ErpProductBrandRespVO.class,
            fields("id", "name", "status", "sort", "createTime"), true),
    PRODUCT_UNIT("erp_product_unit", "配件单位", "配件单位", "配件单位",
            ErpProductUnitImportExcelVO.class, ErpProductUnitRespVO.class,
            fields("id", "name", "status", "createTime"), true);

    private final String moduleKey;
    private final String moduleName;
    private final String fileNamePrefix;
    private final String sheetName;
    private final Class<?> importClass;
    private final Class<?> exportClass;
    private final Set<String> exportFields;
    private final boolean csvSupported;

    ErpSimpleArchiveModule(String moduleKey, String moduleName, String fileNamePrefix, String sheetName,
                           Class<?> importClass, Class<?> exportClass, Set<String> exportFields,
                           boolean csvSupported) {
        this.moduleKey = moduleKey;
        this.moduleName = moduleName;
        this.fileNamePrefix = fileNamePrefix;
        this.sheetName = sheetName;
        this.importClass = importClass;
        this.exportClass = exportClass;
        this.exportFields = exportFields;
        this.csvSupported = csvSupported;
    }

    private static Set<String> fields(String... fields) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(fields)));
    }

    public String getModuleKey() { return moduleKey; }
    public String getModuleName() { return moduleName; }
    public String getFileNamePrefix() { return fileNamePrefix; }
    public String getSheetName() { return sheetName; }
    public Class<?> getImportClass() { return importClass; }
    public Class<?> getExportClass() { return exportClass; }
    public Set<String> getExportFields() { return exportFields; }
    public boolean isCsvSupported() { return csvSupported; }

}
