package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportExcelVO;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpYesNoExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpSaleImportTemplateTest {

    private static final String PRODUCT_CODE_CHOICE_HEADER = "配件编码（三选一）";
    private static final String PRODUCT_NAME_CHOICE_HEADER = "配件名称（三选一）";
    private static final String FACTORY_CODE_CHOICE_HEADER = "厂家编码（三选一）";

    @Test
    void getSaleOrderImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        ErpSaleOrderController controller = prepareController(new ErpSaleOrderController());

        Set<String> headers = writeAndReadHeaders(controller::exportImportTemplate);

        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("下单时间"));
        assertFalse(headers.contains("销售日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getSaleQuoteDetailImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        ErpSaleQuoteController controller = prepareController(new ErpSaleQuoteController());

        Set<String> headers = writeAndReadHeaders(controller::exportImportTemplate);

        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertTrue(headers.contains("赠品"));
        assertFalse(headers.contains("报价日期"));
        assertFalse(headers.contains("单据日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getSaleQuoteDetailImportTemplate_addsGiftSelectValidation() throws Exception {
        ErpSaleQuoteController controller = prepareController(new ErpSaleQuoteController());

        byte[] content = writeTemplate(controller::exportImportTemplate);

        assertHasValidationAtHeader(content, "赠品");
    }

    @Test
    void getSaleQuoteOrderImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        ErpSaleQuoteController controller = prepareController(new ErpSaleQuoteController());

        Set<String> headers = writeAndReadHeaders(controller::getOrderImportTemplate);

        assertTrue(headers.contains("客户名称"));
        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("报价日期"));
        assertFalse(headers.contains("单据日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getSaleQuoteOrderImportTemplate_addsGiftSelectValidation() throws Exception {
        ErpSaleQuoteController controller = prepareController(new ErpSaleQuoteController());

        byte[] content = writeTemplate(controller::getOrderImportTemplate);

        assertHasValidationAtHeader(content, "赠品");
    }

    @Test
    void getSaleReturnImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        ErpSaleReturnController controller = prepareController(new ErpSaleReturnController());

        Set<String> headers = writeAndReadHeaders(controller::exportImportTemplate);

        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("退货日期"));
        assertFalse(headers.contains("开单日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getSalePriceAdjustImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        ErpSalePriceAdjustController controller = prepareController(new ErpSalePriceAdjustController());

        Set<String> headers = writeAndReadHeaders(controller::getImportTemplate);

        assertTrue(headers.contains("销售单号"));
        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertFalse(headers.contains("调价日期"));
        assertFalse(headers.contains("单据日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getSaleCartImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        ErpSaleCartController controller = prepareController(new ErpSaleCartController());

        Set<String> headers = writeAndReadHeaders(controller::exportImportTemplate);

        assertTrue(headers.contains(PRODUCT_CODE_CHOICE_HEADER));
        assertTrue(headers.contains(PRODUCT_NAME_CHOICE_HEADER));
        assertTrue(headers.contains(FACTORY_CODE_CHOICE_HEADER));
        assertTrue(headers.contains("赠品"));
        assertFalse(headers.contains("手推车日期"));
        assertFalse(headers.contains("单据日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getSaleCartImportTemplate_addsGiftSelectValidation() throws Exception {
        ErpSaleCartController controller = prepareController(new ErpSaleCartController());

        byte[] content = writeTemplate(controller::exportImportTemplate);

        assertHasValidationAtHeader(content, "赠品");
    }

    @Test
    void getSaleCartImportTemplate_marksProductIdentityHeadersAsChoiceRequired() throws Exception {
        ErpSaleCartController controller = prepareController(new ErpSaleCartController());

        byte[] content = writeTemplate(controller::exportImportTemplate);

        assertChoiceHeader(content, PRODUCT_CODE_CHOICE_HEADER);
        assertChoiceHeader(content, PRODUCT_NAME_CHOICE_HEADER);
        assertChoiceHeader(content, FACTORY_CODE_CHOICE_HEADER);
    }

    @Test
    void saleCartOrderImportTemplate_addsGiftSelectValidation() throws Exception {
        byte[] content = writeTemplate(response -> ExcelUtils.writeImportTemplate(response,
                "销售手推车整单导入模板.xls", "销售手推车整单", ErpSaleCartOrderImportExcelVO.class,
                Collections.singletonList(new ErpSaleCartOrderImportExcelVO())));

        assertHasValidationAtHeader(content, "赠品");
    }

    @Test
    void readSaleQuoteImport_whenGiftUsesYesNo_thenConvertsToBoolean() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sale-quote.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildSaleQuoteGiftFile());

        List<ErpSaleQuoteImportExcelVO> rows = ExcelUtils.read(file, ErpSaleQuoteImportExcelVO.class);

        assertEquals(2, rows.size());
        assertTrue(rows.get(0).getGiftFlag());
        assertFalse(rows.get(1).getGiftFlag());
    }

    @Test
    void saleOrderOrderImportTime_isNotRequiredForOldTemplateCompatibility() throws Exception {
        ExcelRequired required = ErpSaleOrderOrderImportExcelVO.class.getDeclaredField("orderTime")
                .getAnnotation(ExcelRequired.class);

        assertNull(required);
    }

    private <T> T prepareController(T controller) {
        ErpFieldConfigService fieldConfigService = mock(ErpFieldConfigService.class);
        when(fieldConfigService.getFieldConfigListByModule(anyString())).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(controller, "fieldConfigService", fieldConfigService);
        return controller;
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
        result.put("yesNo", selectFunction(ErpYesNoExcelColumnSelectFunction.NAME, "是", "否"));
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

    private void assertChoiceHeader(byte[] content, String header) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            int columnIndex = findHeaderColumn(workbook.getSheetAt(0), header);
            Cell cell = workbook.getSheetAt(0).getRow(0).getCell(columnIndex);
            assertEquals(FillPatternType.SOLID_FOREGROUND, cell.getCellStyle().getFillPattern());
            assertEquals(IndexedColors.LIGHT_TURQUOISE.getIndex(), cell.getCellStyle().getFillForegroundColor());
            assertTrue(cell.getCellComment().getString().getString().contains("三选一字段"));
        }
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

    private byte[] buildSaleQuoteGiftFile() throws Exception {
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("报价订单");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("配件编码（二选一）");
            header.createCell(1).setCellValue("配件名称（二选一）");
            header.createCell(2).setCellValue("数量");
            header.createCell(3).setCellValue("单价");
            header.createCell(4).setCellValue("赠品");
            Row yesRow = sheet.createRow(1);
            yesRow.createCell(2).setCellValue(1);
            yesRow.createCell(4).setCellValue("是");
            Row noRow = sheet.createRow(2);
            noRow.createCell(2).setCellValue(1);
            noRow.createCell(4).setCellValue("否");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private interface TemplateWriter {

        void write(MockHttpServletResponse response) throws IOException;

    }

}
