package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpStockCheckTypeExcelColumnSelectFunction;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpStockImportTemplateTest {

    private static final String PRODUCT_CODE_CHOICE_HEADER = "配件编码（三选一）";
    private static final String PRODUCT_NAME_CHOICE_HEADER = "配件名称（三选一）";
    private static final String FACTORY_CODE_CHOICE_HEADER = "厂家编码（三选一）";

    @Test
    void getStockInImportTemplate_hidesBizTimeColumn() throws Exception {
        Set<String> headers = writeAndReadHeaders(new ErpStockInController()::getImportTemplate);

        assertTrue(headers.contains("供应商名称"));
        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("业务时间"));
        assertFalse(headers.contains("单据日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getStockOutImportTemplate_hidesBizTimeColumn() throws Exception {
        Set<String> headers = writeAndReadHeaders(new ErpStockOutController()::getImportTemplate);

        assertTrue(headers.contains("客户名称"));
        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("业务时间"));
        assertFalse(headers.contains("单据日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getStockTransferOutImportTemplate_hidesBizTimeColumn() throws Exception {
        Set<String> headers = writeAndReadHeaders(
                new ErpStockTransferOutController()::getStockTransferOutImportTemplate);

        assertTrue(headers.contains("调入部门"));
        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("业务时间"));
        assertFalse(headers.contains("调拨日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getStockCheckImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        Set<String> headers = writeAndReadHeaders(new ErpStockCheckController()::getImportTemplate);

        assertTrue(headers.contains("盘点类型"));
        assertTrue(headers.contains("所属仓库"));
        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("业务时间"));
        assertFalse(headers.contains("盘点日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getStockCheckImportTemplate_addsCheckTypeSelectValidation() throws Exception {
        byte[] content = writeTemplate(new ErpStockCheckController()::getImportTemplate);

        assertHasValidationAtHeader(content, "盘点类型");
    }

    @Test
    void getWarehouseMoveImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        Set<String> headers = writeAndReadHeaders(new ErpWarehouseMoveController()::getWarehouseMoveImportTemplate);

        assertTrue(headers.contains("移出仓库"));
        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("业务时间"));
        assertFalse(headers.contains("移货日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void stockImportTemplates_showProductIdentityChoiceComments() throws Exception {
        assertHasChoiceHeaderComments(writeTemplate(new ErpStockInController()::getImportTemplate));
        assertHasChoiceHeaderComments(writeTemplate(new ErpStockOutController()::getImportTemplate));
        assertHasChoiceHeaderComments(writeTemplate(new ErpStockTransferOutController()::getStockTransferOutImportTemplate));
        assertHasChoiceHeaderComments(writeTemplate(new ErpStockCheckController()::getImportTemplate));
        assertHasChoiceHeaderComments(writeTemplate(new ErpWarehouseMoveController()::getWarehouseMoveImportTemplate));
    }

    @Test
    void stockImportBizTime_isNotRequiredForOldTemplateCompatibility() throws Exception {
        ExcelRequired required = ErpStockImportExcelVO.class.getDeclaredField("bizTime")
                .getAnnotation(ExcelRequired.class);

        assertNull(required);
    }

    private Set<String> writeAndReadHeaders(TemplateWriter writer) throws Exception {
        return readFirstRowHeaders(writeTemplate(writer));
    }

    private byte[] writeTemplate(TemplateWriter writer) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            ApplicationContext applicationContext = mock(ApplicationContext.class);
            springUtil.when(SpringUtil::getApplicationContext).thenReturn(applicationContext);
            when(applicationContext.getBeansOfType(ExcelColumnSelectFunction.class))
                    .thenReturn(buildSelectFunctionMap());
            writer.write(response);
        }
        return response.getContentAsByteArray();
    }

    private Map<String, ExcelColumnSelectFunction> buildSelectFunctionMap() {
        Map<String, ExcelColumnSelectFunction> result = new LinkedHashMap<>();
        result.put("checkType", selectFunction(ErpStockCheckTypeExcelColumnSelectFunction.NAME, "盘数量", "盘成本"));
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
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
        }
        return headers;
    }

    private void assertHasValidationAtHeader(byte[] content, String header) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            int columnIndex = findHeaderColumn(workbook.getSheetAt(0), header);
            assertTrue(hasValidationAt(workbook.getSheetAt(0), columnIndex));
        }
    }

    private void assertHasChoiceHeaderComments(byte[] content) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            assertHasChoiceHeaderComment(workbook, PRODUCT_CODE_CHOICE_HEADER);
            assertHasChoiceHeaderComment(workbook, PRODUCT_NAME_CHOICE_HEADER);
            assertHasChoiceHeaderComment(workbook, FACTORY_CODE_CHOICE_HEADER);
        }
    }

    private void assertHasChoiceHeaderComment(Workbook workbook, String header) {
        Row headerRow = workbook.getSheetAt(0).getRow(0);
        int columnIndex = findHeaderColumn(workbook.getSheetAt(0), header);
        Cell cell = headerRow.getCell(columnIndex);
        assertTrue(cell.getCellComment().getString().getString().contains("三选一字段"));
    }

    private int findHeaderColumn(org.apache.poi.ss.usermodel.Sheet sheet, String header) {
        Row headerRow = sheet.getRow(0);
        for (Cell cell : headerRow) {
            if (header.equals(cell.getStringCellValue())) {
                return cell.getColumnIndex();
            }
        }
        throw new AssertionError("Header not found: " + header);
    }

    private boolean hasValidationAt(org.apache.poi.ss.usermodel.Sheet sheet, int columnIndex) {
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

    private interface TemplateWriter {

        void write(MockHttpServletResponse response) throws IOException;

    }

}
