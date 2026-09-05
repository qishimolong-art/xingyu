package cn.iocoder.yudao.framework.excel.core.util;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelColumnSelect;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExcelUtilsTest {

    @Test
    public void testWrite_whenExcelBuildFails_thenResponseStreamNotTouched() {
        TrackingMockHttpServletResponse response = new TrackingMockHttpServletResponse();

        assertThrows(RuntimeException.class, () -> ExcelUtils.write(response, "test.xlsx", "data",
                BrokenExportVO.class, Collections.singletonList(new BrokenExportVO("broken"))));

        assertFalse(response.isOutputStreamAccessed());
        assertNull(response.getHeader("Content-Disposition"));
    }

    @Test
    public void testRead_whenSuccess_thenRecordsReadContext() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildSimpleImportFile());

        List<SimpleImportVO> result = ExcelUtils.read(file, SimpleImportVO.class);
        ExcelUtils.ExcelOperationContext context = ExcelUtils.getLastOperation();

        assertEquals(1, result.size());
        assertEquals("\u6d4b\u8bd5\u4ea7\u54c1", result.get(0).getName());
        assertEquals("READ", context.getOperation());
        assertEquals("test.xlsx", context.getFilename());
        assertEquals(SimpleImportVO.class.getName(), context.getHeadClassName());
        assertEquals(1, context.getReadRows().size());
    }

    @Test
    public void testRead_whenOldProductHeaders_thenNormalizesToPartHeaders() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "product.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildOldProductHeaderImportFile());

        List<PartHeaderImportVO> result = ExcelUtils.read(file, PartHeaderImportVO.class);

        assertEquals(1, result.size());
        assertEquals("P0001", result.get(0).getProductCode());
        assertEquals("示例配件", result.get(0).getProductName());
    }

    @Test
    public void testRead_whenPreviousPartHeaders_thenNormalizesToChoiceHeaders() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "product.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildPreviousPartHeaderImportFile());

        List<PartHeaderImportVO> result = ExcelUtils.read(file, PartHeaderImportVO.class);

        assertEquals(1, result.size());
        assertEquals("P0002", result.get(0).getProductCode());
        assertEquals("上一版配件", result.get(0).getProductName());
    }

    @Test
    public void testCountFirstSheetDataRows_whenLastNonBlankRowIsLimitBoundary_thenCountsDataRowsAfterHeader() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rows.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildSparseImportFile(3001));

        assertEquals(3000, ExcelUtils.countFirstSheetDataRows(file, 1));
    }

    @Test
    public void testCountFirstSheetDataRows_whenMiddleBlankRows_thenCountsRowSpan() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rows.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildSparseImportFile(2, 5));

        assertEquals(4, ExcelUtils.countFirstSheetDataRows(file, 1));
    }

    @Test
    public void testCountFirstSheetDataRows_whenTrailingBlankRows_thenIgnoresTrailingRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rows.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildFileWithTrailingBlankRows());

        assertEquals(1, ExcelUtils.countFirstSheetDataRows(file, 1));
    }

    @Test
    public void testWriteDynamic_thenWritesDynamicHeadAndRows() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeDynamic(response, "\u9519\u8bef\u6570\u636e.xls", "\u9519\u8bef\u6570\u636e",
                Arrays.asList(Collections.singletonList("\u540d\u79f0"), Collections.singletonList("\u5931\u8d25\u539f\u56e0")),
                Collections.singletonList(Arrays.asList("\u6d4b\u8bd5\u4ea7\u54c1", "\u540d\u79f0\u91cd\u590d")));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("\u540d\u79f0", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("\u5931\u8d25\u539f\u56e0", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("\u6d4b\u8bd5\u4ea7\u54c1", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("\u540d\u79f0\u91cd\u590d", sheet.getRow(1).getCell(1).getStringCellValue());
        }
    }

    @Test
    public void testWriteImportTemplate_whenRequired_thenYellowHeaderWithoutPrefix() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "template.xls", "data", SimpleRequiredImportVO.class,
                Collections.singletonList(new SimpleRequiredImportVO()));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            Cell requiredCell = headerRow.getCell(0);
            Cell optionalCell = headerRow.getCell(1);
            assertEquals("\u540d\u79f0", requiredCell.getStringCellValue());
            assertEquals("\u5907\u6ce8", optionalCell.getStringCellValue());
            assertEquals(FillPatternType.SOLID_FOREGROUND, requiredCell.getCellStyle().getFillPattern());
            assertEquals(IndexedColors.YELLOW.getIndex(), requiredCell.getCellStyle().getFillForegroundColor());
            assertNotNull(requiredCell.getCellComment());
            assertNull(optionalCell.getCellComment());
        }
    }

    @Test
    public void testWriteImportTemplate_whenChoiceRequired_thenBlueHeaderWithComment() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "template.xls", "data", ChoiceRequiredImportVO.class,
                Collections.singletonList(new ChoiceRequiredImportVO()));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            Cell choiceCell = headerRow.getCell(0);
            assertEquals("配件编码（三选一）", choiceCell.getStringCellValue());
            assertEquals(FillPatternType.SOLID_FOREGROUND, choiceCell.getCellStyle().getFillPattern());
            assertEquals(IndexedColors.LIGHT_TURQUOISE.getIndex(), choiceCell.getCellStyle().getFillForegroundColor());
            assertNotNull(choiceCell.getCellComment());
            assertTrue(choiceCell.getCellComment().getString().getString().contains("三选一字段"));
        }
    }

    @Test
    public void testWriteImportTemplate_whenColumnSelect_thenCreatesHiddenDictSheetAndValidation() throws Exception {
        initDictData("test_select", "个", "箱");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "template.xls", "data", SimpleSelectImportVO.class,
                Collections.singletonList(new SimpleSelectImportVO()));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertNotNull(workbook.getSheet("字典sheet"));
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("字典sheet")));
            assertTrue(hasValidationAt(workbook.getSheetAt(0), 1));
        }
    }

    @Test
    public void testWriteImportTemplate_whenIncludeColumns_thenSelectValidationUsesVisibleColumnIndex() throws Exception {
        initDictData("test_select", "个", "箱");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Set<String> includeColumnFieldNames = new HashSet<>(Arrays.asList("name", "unitName"));

        ExcelUtils.writeImportTemplate(response, "template.xls", "data", SimpleSelectImportVO.class,
                Collections.singletonList(new SimpleSelectImportVO()), includeColumnFieldNames);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            assertEquals("\u540d\u79f0", headerRow.getCell(0).getStringCellValue());
            assertEquals("\u5355\u4f4d", headerRow.getCell(1).getStringCellValue());
            assertTrue(hasValidationAt(workbook.getSheetAt(0), 1));
            assertFalse(hasValidationAt(workbook.getSheetAt(0), 2));
        }
    }

    private void initDictData(String dictType, String... labels) {
        DictDataCommonApi dictDataApi = mock(DictDataCommonApi.class);
        List<DictDataRespDTO> dictDataList = Arrays.stream(labels)
                .map(label -> {
                    DictDataRespDTO dto = new DictDataRespDTO();
                    dto.setDictType(dictType);
                    dto.setLabel(label);
                    dto.setValue(label);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        DictFrameworkUtils.init(dictDataApi);
        DictFrameworkUtils.clearCache();
        when(dictDataApi.getDictDataList(eq(dictType))).thenReturn(dictDataList);
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

    private byte[] buildSimpleImportFile() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            sheet.createRow(0).createCell(0).setCellValue("\u540d\u79f0");
            sheet.createRow(1).createCell(0).setCellValue("\u6d4b\u8bd5\u4ea7\u54c1");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildOldProductHeaderImportFile() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("产品编码");
            header.createCell(1).setCellValue("产品名称");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("P0001");
            data.createCell(1).setCellValue("示例配件");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildPreviousPartHeaderImportFile() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("配件编码");
            header.createCell(1).setCellValue("配件名称");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("P0002");
            data.createCell(1).setCellValue("上一版配件");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildSparseImportFile(int... nonBlankRowNumbers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            sheet.createRow(0).createCell(0).setCellValue("\u540d\u79f0");
            for (int rowNumber : nonBlankRowNumbers) {
                sheet.createRow(rowNumber - 1).createCell(0).setCellValue("row-" + rowNumber);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildFileWithTrailingBlankRows() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            sheet.createRow(0).createCell(0).setCellValue("\u540d\u79f0");
            sheet.createRow(1).createCell(0).setCellValue("\u6d4b\u8bd5\u4ea7\u54c1");
            sheet.createRow(6);
            sheet.createRow(8).createCell(0).setCellStyle(workbook.createCellStyle());
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static class BrokenExportVO {

        @ExcelProperty(value = "\u540d\u79f0", converter = BrokenConverter.class)
        private final String name;

        private BrokenExportVO(String name) {
            this.name = name;
        }

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

    }

    public static class SimpleImportVO {

        @ExcelProperty("\u540d\u79f0")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class SimpleRequiredImportVO {

        @ExcelRequired
        @ExcelProperty("\u540d\u79f0")
        private String name;

        @ExcelProperty("\u5907\u6ce8")
        private String remark;

    }

    public static class ChoiceRequiredImportVO {

        @ExcelChoiceRequired
        @ExcelProperty("配件编码（三选一）")
        private String productCode;

    }

    public static class PartHeaderImportVO {

        @ExcelProperty("配件编码（三选一）")
        private String productCode;

        @ExcelProperty("配件名称（三选一）")
        private String productName;

        @ExcelProperty("厂家编码（三选一）")
        private String factoryCode;

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

    }

    public static class SimpleSelectImportVO {

        @ExcelProperty("\u540d\u79f0")
        private String name;

        @ExcelColumnSelect(dictType = "test_select")
        @ExcelProperty("\u5355\u4f4d")
        private String unitName;

        @ExcelProperty("\u5907\u6ce8")
        private String remark;

    }

    public static class BrokenConverter implements Converter<Object> {

        @Override
        public Class<?> supportJavaTypeKey() {
            return Object.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Object convertToJavaData(ReadCellData readCellData, ExcelContentProperty contentProperty,
                                        GlobalConfiguration globalConfiguration) {
            return null;
        }

        @Override
        public WriteCellData<?> convertToExcelData(Object object, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            throw new RuntimeException("mock excel convert failure");
        }

    }

    private static class TrackingMockHttpServletResponse extends MockHttpServletResponse {

        private boolean outputStreamAccessed;

        @Override
        public javax.servlet.ServletOutputStream getOutputStream() {
            outputStreamAccessed = true;
            return super.getOutputStream();
        }

        private boolean isOutputStreamAccessed() {
            return outputStreamAccessed;
        }

    }

}
