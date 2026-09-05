package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpProductCategoryCodeExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpProductUnitNameExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpProductImportExcelVOTest {

    @Test
    void writeImportTemplate_marksRequiredHeadersWithYellowWithoutPrefix() throws Exception {
        byte[] content = writeProductImportTemplate(null, null);

        Set<String> headers = readFirstRowHeaders(content);
        assertTrue(headers.contains("配件编码"));
        assertFalse(headers.contains("* 配件编码"));
        assertTrue(headers.contains("产品名称"));
        assertFalse(headers.contains("* 产品名称"));
        assertTrue(headers.contains("产品条码"));
        assertFalse(headers.contains("* 产品条码"));
        assertTrue(headers.contains("配件分类编码"));
        assertTrue(headers.contains("单位"));
        assertFalse(headers.contains("创建时间"));
        assertFalse(headers.contains("更新时间"));

        assertRequiredHeaderStyle(content, "配件编码");
        assertRequiredHeaderStyle(content, "产品名称");
        assertOptionalHeaderStyle(content, "产品条码");
    }

    @Test
    void getRequiredFields_mapsConfiguredDefaultWarehouseToYellowRequiredHeader() throws Exception {
        ErpFieldConfigService fieldConfigService = mock(ErpFieldConfigService.class);
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Arrays.asList(
                fieldConfig("defaultWarehouseId", true),
                fieldConfig("barCode", false)));

        Set<String> requiredFields = ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                ErpFieldConfigModuleEnum.ERP_PRODUCT, ErpProductImportExcelVO.class,
                ErpImportTemplateRequiredFieldUtils.aliasMap(
                        "defaultWarehouseId", "defaultWarehouseName"));

        assertTrue(requiredFields.contains("defaultWarehouseName"));
        assertFalse(requiredFields.contains("barCode"));

        byte[] content = writeProductImportTemplate(null, requiredFields);

        Set<String> headers = readFirstRowHeaders(content);
        assertTrue(headers.contains("默认仓库"));
        assertFalse(headers.contains("* 默认仓库"));
        assertFalse(headers.contains("* 产品条码"));
        assertRequiredHeaderStyle(content, "默认仓库");
        assertOptionalHeaderStyle(content, "产品条码");
    }

    @Test
    void writeImportTemplate_addsSelectValidationForCategoryCodeAndUnitName() throws Exception {
        byte[] content = writeProductImportTemplate(null, null);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertTrue(hasValidationAt(sheet, 3));
            assertTrue(hasValidationAt(sheet, 5));
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("字典sheet")));
        }
    }

    @Test
    void writeImportTemplate_whenIncludeColumns_thenSelectValidationFollowsVisibleColumns() throws Exception {
        Set<String> includeColumnFieldNames = new HashSet<>(Arrays.asList("code", "name", "categoryCode", "unitName"));

        byte[] content = writeProductImportTemplate(includeColumnFieldNames, null);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertTrue(hasValidationAt(sheet, 2));
            assertTrue(hasValidationAt(sheet, 3));
            assertFalse(hasValidationAt(sheet, 5));
        }
    }

    private byte[] writeProductImportTemplate(Set<String> includeColumnFieldNames,
                                              Set<String> requiredColumnFieldNames) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            ApplicationContext applicationContext = mock(ApplicationContext.class);
            springUtil.when(SpringUtil::getApplicationContext).thenReturn(applicationContext);
            when(applicationContext.getBeansOfType(ExcelColumnSelectFunction.class))
                    .thenReturn(buildSelectFunctionMap());
            ExcelUtils.writeImportTemplate(response, "产品导入模板.xls", "产品", ErpProductImportExcelVO.class,
                    Collections.singletonList(new ErpProductImportExcelVO()), includeColumnFieldNames,
                    requiredColumnFieldNames);
        }
        return response.getContentAsByteArray();
    }

    private Map<String, ExcelColumnSelectFunction> buildSelectFunctionMap() {
        Map<String, ExcelColumnSelectFunction> result = new LinkedHashMap<>();
        result.put("category", selectFunction(ErpProductCategoryCodeExcelColumnSelectFunction.NAME, "001", "001 001"));
        result.put("unit", selectFunction(ErpProductUnitNameExcelColumnSelectFunction.NAME, "个", "箱"));
        return result;
    }

    private ExcelColumnSelectFunction selectFunction(String name, String... options) {
        return new ExcelColumnSelectFunction() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public java.util.List<String> getOptions() {
                return Arrays.asList(options);
            }

        };
    }

    private Set<String> readFirstRowHeaders(byte[] content) throws Exception {
        Set<String> headers = new HashSet<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
        }
        return headers;
    }

    private void assertRequiredHeaderStyle(byte[] content, String header) throws Exception {
        assertTrue(isYellowRequiredHeader(content, header));
    }

    private void assertOptionalHeaderStyle(byte[] content, String header) throws Exception {
        assertFalse(isYellowRequiredHeader(content, header));
    }

    private boolean isYellowRequiredHeader(byte[] content, String header) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            for (Cell cell : headerRow) {
                if (header.equals(cell.getStringCellValue())) {
                    return cell.getCellStyle().getFillPattern() == FillPatternType.SOLID_FOREGROUND
                            && cell.getCellStyle().getFillForegroundColor() == IndexedColors.YELLOW.getIndex();
                }
            }
        }
        throw new AssertionError("Header not found: " + header);
    }

    private boolean hasValidationAt(Sheet sheet, int columnIndex) {
        for (DataValidation validation : sheet.getDataValidations()) {
            for (org.apache.poi.ss.util.CellRangeAddress address : validation.getRegions().getCellRangeAddresses()) {
                if (address.getFirstRow() == 1 && address.getLastRow() == 2000
                        && address.getFirstColumn() == columnIndex && address.getLastColumn() == columnIndex) {
                    return true;
                }
            }
        }
        return false;
    }

    private ErpFieldConfigDO fieldConfig(String fieldName, boolean required) {
        return ErpFieldConfigDO.builder()
                .fieldName(fieldName)
                .required(required)
                .build();
    }

}
