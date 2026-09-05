package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpYesNoExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpPurchaseImportTemplateTest {

    private static final String PRODUCT_CODE_CHOICE_HEADER = "配件编码（三选一）";
    private static final String PRODUCT_NAME_CHOICE_HEADER = "配件名称（三选一）";
    private static final String FACTORY_CODE_CHOICE_HEADER = "厂家编码（三选一）";

    @Test
    void getPurchaseOrderImportTemplate_doesNotExposeDocumentDateColumn() throws Exception {
        ErpPurchaseOrderController controller = prepareController(new ErpPurchaseOrderController());

        Set<String> headers = writeAndReadHeaders(controller::getImportTemplate);

        assertTrue(headers.contains("供应商"));
        assertTrue(headers.contains("赠品"));
        assertFalse(headers.contains("采购日期"));
        assertFalse(headers.contains("创建时间"));
    }

    @Test
    void getPurchaseOrderImportTemplate_addsGiftSelectValidation() throws Exception {
        ErpPurchaseOrderController controller = prepareController(new ErpPurchaseOrderController());

        byte[] content = writeTemplate(controller::getImportTemplate);

        assertHasValidationAtHeader(content, "赠品");
    }

    @Test
    void getPurchaseOrderDetailImportTemplate_addsGiftSelectValidation() throws Exception {
        ErpPurchaseOrderController controller = prepareController(new ErpPurchaseOrderController());

        byte[] content = writeTemplate(controller::getDetailImportTemplate);

        assertHasValidationAtHeader(content, "赠品");
    }

    @Test
    void getPurchaseInOrderImportTemplate_hidesInTimeColumn() throws Exception {
        ErpPurchaseInController controller = prepareController(new ErpPurchaseInController());

        Set<String> headers = writeAndReadHeaders(controller::getOrderImportTemplate);

        assertTrue(headers.contains("供应商"));
        assertFalse(headers.contains("入库时间"));
    }

    @Test
    void getPurchaseInOrderImportTemplate_addsGiftSelectValidation() throws Exception {
        ErpPurchaseInController controller = prepareController(new ErpPurchaseInController());

        byte[] content = writeTemplate(controller::getOrderImportTemplate);

        assertHasValidationAtHeader(content, "赠品");
    }

    @Test
    void getPurchaseReturnOrderImportTemplate_hidesReturnTimeColumn() throws Exception {
        ErpPurchaseReturnController controller = prepareController(new ErpPurchaseReturnController());

        Set<String> headers = writeAndReadHeaders(controller::getOrderImportTemplate);

        assertTrue(headers.contains("供应商"));
        assertFalse(headers.contains("退货时间"));
    }

    @Test
    void getPurchasePriceAdjustOrderImportTemplate_hidesAdjustTimeColumn() throws Exception {
        ErpPurchasePriceAdjustController controller = prepareController(new ErpPurchasePriceAdjustController());

        Set<String> headers = writeAndReadHeaders(controller::getOrderImportTemplate);

        assertTrue(headers.contains("供应商"));
        assertFalse(headers.contains("调价时间"));
    }

    @Test
    void getPurchaseInvoiceImportTemplate_hidesInvoiceDateColumn() throws Exception {
        ErpPurchaseInvoiceController controller = prepareController(new ErpPurchaseInvoiceController());

        Set<String> headers = writeAndReadHeaders(controller::getImportTemplate);

        assertTrue(headers.contains("供应商"));
        assertFalse(headers.contains("开票日期"));
    }

    @Test
    void purchaseImportTemplates_showProductIdentityChoiceHeaders() throws Exception {
        ErpPurchaseOrderController orderController = prepareController(new ErpPurchaseOrderController());
        assertHasProductChoiceHeaders(writeTemplate(orderController::getImportTemplate));
        assertHasProductChoiceHeaders(writeTemplate(orderController::getDetailImportTemplate));

        ErpPurchaseInController inController = prepareController(new ErpPurchaseInController());
        assertHasProductChoiceHeaders(writeTemplate(inController::getImportTemplate));
        assertHasProductChoiceHeaders(writeTemplate(inController::getOrderImportTemplate));

        ErpPurchaseReturnController returnController = prepareController(new ErpPurchaseReturnController());
        assertHasProductChoiceHeaders(writeTemplate(returnController::getImportTemplate));
        assertHasProductChoiceHeaders(writeTemplate(returnController::getOrderImportTemplate));

        ErpPurchaseInvoiceController invoiceController = prepareController(new ErpPurchaseInvoiceController());
        assertHasProductChoiceHeaders(writeTemplate(invoiceController::getImportTemplate));

        ErpPurchasePriceAdjustController priceAdjustController =
                prepareController(new ErpPurchasePriceAdjustController());
        assertHasProductChoiceHeaders(writeTemplate(priceAdjustController::getImportTemplate));
        assertHasProductChoiceHeaders(writeTemplate(priceAdjustController::getOrderImportTemplate));
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
            SheetHeader sheetHeader = findHeader(workbook.getSheetAt(0), header);
            assertTrue(hasValidationAt(workbook.getSheetAt(0), sheetHeader.columnIndex));
        }
    }

    private void assertHasProductChoiceHeaders(byte[] content) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            assertHasChoiceHeader(workbook, PRODUCT_CODE_CHOICE_HEADER);
            assertHasChoiceHeader(workbook, PRODUCT_NAME_CHOICE_HEADER);
            assertHasChoiceHeader(workbook, FACTORY_CODE_CHOICE_HEADER);
        }
    }

    private void assertHasChoiceHeader(Workbook workbook, String header) {
        Cell cell = findHeaderCell(workbook.getSheetAt(0), header);
        assertNotNull(cell.getCellComment());
        assertTrue(cell.getCellComment().getString().getString().contains("三选一字段"));
    }

    private SheetHeader findHeader(org.apache.poi.ss.usermodel.Sheet sheet, String header) {
        return new SheetHeader(findHeaderCell(sheet, header).getColumnIndex());
    }

    private Cell findHeaderCell(org.apache.poi.ss.usermodel.Sheet sheet, String header) {
        Row headerRow = sheet.getRow(0);
        for (Cell cell : headerRow) {
            if (header.equals(cell.getStringCellValue())) {
                return cell;
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

    private static class SheetHeader {

        private final int columnIndex;

        private SheetHeader(int columnIndex) {
            this.columnIndex = columnIndex;
        }

    }

    private interface TemplateWriter {

        void write(MockHttpServletResponse response) throws IOException;

    }

}
