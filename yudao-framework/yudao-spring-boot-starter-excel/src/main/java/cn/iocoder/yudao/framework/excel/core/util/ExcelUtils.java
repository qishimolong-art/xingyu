package cn.iocoder.yudao.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.idev.excel.exception.ExcelAnalysisException;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.hutool.core.io.IoUtil;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.excel.core.handler.RequiredHeaderStyleWriteHandler;
import cn.iocoder.yudao.framework.excel.core.handler.SelectSheetWriteHandler;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Excel 工具类
 *
 * @author 芋道源码
 */
public class ExcelUtils {

    private static final Pattern REQUIRED_HEADER_PREFIX_PATTERN = Pattern.compile("^\\s*[*\\uff0a]\\s*");
    private static final Map<String, String> HEADER_ALIASES = buildHeaderAliases();
    private static final ThreadLocal<ExcelOperationContext> LAST_OPERATION = new ThreadLocal<>();

    /**
     * 将列表以 Excel 响应给前端
     *
     * @param response  响应
     * @param filename  文件名
     * @param sheetName Excel sheet 名
     * @param head      Excel head 头
     * @param data      数据列表哦
     * @param <T>       泛型，保证 head 和 data 类型的一致性
     * @throws IOException 写入失败的情况
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data) throws IOException {
        // 输出 Excel
        setLastWriteOperation(filename, data, null);
        if (response != null) {
            writeResponse(response, filename, writeToBytes(sheetName, head, data, null, false, null));
            return;
        }
        FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy()) // 基于 column 长度，自动适配。最大 255 宽度
                .registerWriteHandler(new SelectSheetWriteHandler(head)) // 基于固定 sheet 实现下拉框
                .registerConverter(new LongStringConverter()) // 避免 Long 类型丢失精度
                .sheet(sheetName).doWrite(data);
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data, Set<String> includeColumnFieldNames) throws IOException {
        if (includeColumnFieldNames == null || includeColumnFieldNames.isEmpty()) {
            write(response, filename, sheetName, head, data);
            return;
        }
        setLastWriteOperation(filename, data, includeColumnFieldNames);
        if (response != null) {
            writeResponse(response, filename, writeToBytes(sheetName, head, data, includeColumnFieldNames, false, null));
            return;
        }
        FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerWriteHandler(new SelectSheetWriteHandler(head, includeColumnFieldNames))
                .registerConverter(new LongStringConverter())
                .includeColumnFieldNames(includeColumnFieldNames)
                .sheet(sheetName).doWrite(data);
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    /**
     * 使用动态表头导出 Excel，适合在原模板列后追加运行时列。
     */
    public static void writeDynamic(HttpServletResponse response, String filename, String sheetName,
                                    List<List<String>> head, List<List<Object>> data) throws IOException {
        setLastWriteOperation(filename, data, null);
        if (response != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            FastExcelFactory.write(outputStream)
                    .head(head)
                    .autoCloseStream(false)
                    .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                    .registerConverter(new LongStringConverter())
                    .sheet(sheetName).doWrite(data);
            writeResponse(response, filename, outputStream.toByteArray());
            return;
        }
        FastExcelFactory.write(response.getOutputStream())
                .head(head)
                .autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerConverter(new LongStringConverter())
                .sheet(sheetName).doWrite(data);
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    /**
     * 将导入模板以 Excel 响应给前端，并对 {@code @ExcelRequired} 字段的表头做必填标注。
     */
    public static <T> void writeImportTemplate(HttpServletResponse response, String filename, String sheetName,
                                               Class<T> head, List<T> data) throws IOException {
        writeImportTemplate(response, filename, sheetName, head, data, null, null);
    }

    public static <T> void writeImportTemplate(HttpServletResponse response, String filename, String sheetName,
                                               Class<T> head, List<T> data, Set<String> includeColumnFieldNames) throws IOException {
        writeImportTemplate(response, filename, sheetName, head, data, includeColumnFieldNames, null);
    }

    public static <T> void writeImportTemplate(HttpServletResponse response, String filename, String sheetName,
                                               Class<T> head, List<T> data, Set<String> includeColumnFieldNames,
                                               Set<String> requiredColumnFieldNames) throws IOException {
        if (response != null) {
            writeResponse(response, filename,
                    writeToBytes(sheetName, head, data, includeColumnFieldNames, true, requiredColumnFieldNames));
            return;
        }
        cn.idev.excel.write.builder.ExcelWriterBuilder builder = FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerWriteHandler(new SelectSheetWriteHandler(head, includeColumnFieldNames))
                .registerWriteHandler(new RequiredHeaderStyleWriteHandler(requiredColumnFieldNames))
                .registerConverter(new LongStringConverter());
        if (includeColumnFieldNames != null && !includeColumnFieldNames.isEmpty()) {
            builder.includeColumnFieldNames(includeColumnFieldNames);
        }
        builder.sheet(sheetName).doWrite(data);
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    private static <T> byte[] writeToBytes(String sheetName, Class<T> head, List<T> data,
                                           Set<String> includeColumnFieldNames, boolean importTemplate,
                                           Set<String> requiredColumnFieldNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ExcelWriterBuilder builder = FastExcelFactory.write(outputStream, head)
                .autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerWriteHandler(new SelectSheetWriteHandler(head, includeColumnFieldNames))
                .registerConverter(new LongStringConverter());
        if (importTemplate) {
            builder.registerWriteHandler(new RequiredHeaderStyleWriteHandler(requiredColumnFieldNames));
        }
        if (includeColumnFieldNames != null && !includeColumnFieldNames.isEmpty()) {
            builder.includeColumnFieldNames(includeColumnFieldNames);
        }
        builder.sheet(sheetName).doWrite(data);
        return outputStream.toByteArray();
    }

    private static void writeResponse(HttpServletResponse response, String filename, byte[] content) throws IOException {
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        IoUtil.write(response.getOutputStream(), false, content);
    }

    public static <T> List<T> read(MultipartFile file, Class<T> head) throws IOException {
        // 参考 https://t.zsxq.com/zM77F 帖子，增加 try 处理，兼容 windows 场景
        try (InputStream inputStream = file.getInputStream();
             InputStream normalizedInputStream = normalizeRequiredHeaders(inputStream, head)) {
            List<T> rows;
            try {
                rows = FastExcelFactory.read(normalizedInputStream, head, null)
                        .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                        .doReadAllSync();
            } catch (ExcelDataConvertException ex) {
                throw new IllegalArgumentException(formatReadConvertError(ex), ex);
            } catch (ExcelAnalysisException ex) {
                ExcelDataConvertException convertException = findCause(ex, ExcelDataConvertException.class);
                if (convertException != null) {
                    throw new IllegalArgumentException(formatReadConvertError(convertException), ex);
                }
                throw ex;
            }
            LAST_OPERATION.set(new ExcelOperationContext("READ",
                    file == null ? null : file.getOriginalFilename(), rows == null ? 0 : rows.size(), null,
                    head == null ? null : head.getName(), rows == null ? null : new ArrayList<>(rows)));
            return rows;
        }
    }

    public static int countFirstSheetDataRows(MultipartFile file, int headRowNumber) throws IOException {
        if (file == null) {
            return 0;
        }
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() <= 0) {
                return 0;
            }
            Sheet sheet = workbook.getSheetAt(0);
            int firstDataRowIndex = Math.max(headRowNumber, 0);
            int lastNonBlankRowIndex = -1;
            for (int rowIndex = firstDataRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                if (hasNonBlankCell(sheet.getRow(rowIndex))) {
                    lastNonBlankRowIndex = rowIndex;
                }
            }
            return lastNonBlankRowIndex < firstDataRowIndex ? 0 : lastNonBlankRowIndex - firstDataRowIndex + 1;
        }
    }

    public static ExcelOperationContext getLastOperation() {
        return LAST_OPERATION.get();
    }

    public static void clearLastOperation() {
        LAST_OPERATION.remove();
    }

    private static <T> void setLastWriteOperation(String filename, List<T> data, Set<String> includeColumnFieldNames) {
        LAST_OPERATION.set(new ExcelOperationContext("WRITE", filename, data == null ? 0 : data.size(),
                includeColumnFieldNames));
    }

    private static <T> InputStream normalizeRequiredHeaders(InputStream inputStream, Class<T> head) throws IOException {
        Set<String> expectedHeaders = getExcelHeaderNames(head);
        if (expectedHeaders.isEmpty()) {
            return inputStream;
        }
        try (Workbook workbook = WorkbookFactory.create(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (workbook.getNumberOfSheets() > 0) {
                Sheet sheet = workbook.getSheetAt(0);
                Row row = sheet.getRow(0);
                if (row != null) {
                    for (Cell cell : row) {
                        normalizeRequiredHeaderCell(cell, expectedHeaders);
                    }
                }
            }
            workbook.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    private static void normalizeRequiredHeaderCell(Cell cell, Set<String> expectedHeaders) {
        if (cell == null || cell.getCellType() != CellType.STRING) {
            return;
        }
        String value = cell.getStringCellValue();
        if (value == null) {
            return;
        }
        String normalizedValue = REQUIRED_HEADER_PREFIX_PATTERN.matcher(value).replaceFirst("");
        String matchedHeader = resolveExpectedHeader(normalizedValue, expectedHeaders);
        if (matchedHeader != null) {
            cell.setCellValue(matchedHeader);
        }
    }

    private static String resolveExpectedHeader(String header, Set<String> expectedHeaders) {
        if (expectedHeaders.contains(header)) {
            return header;
        }
        String alias = HEADER_ALIASES.get(header);
        if (alias != null && expectedHeaders.contains(alias)) {
            return alias;
        }
        if (alias != null) {
            String choiceAlias = findChoiceHeader(alias, expectedHeaders);
            if (choiceAlias != null) {
                return choiceAlias;
            }
        }
        return findChoiceHeader(header, expectedHeaders);
    }

    private static String findChoiceHeader(String header, Set<String> expectedHeaders) {
        String choiceHeader = header + "\uff08\u4e09\u9009\u4e00\uff09";
        if (expectedHeaders.contains(choiceHeader)) {
            return choiceHeader;
        }
        choiceHeader = header + "\uff08\u4e8c\u9009\u4e00\uff09";
        if (expectedHeaders.contains(choiceHeader)) {
            return choiceHeader;
        }
        return null;
    }

    private static boolean hasNonBlankCell(Row row) {
        if (row == null || row.getFirstCellNum() < 0 || row.getLastCellNum() < 0) {
            return false;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (!isBlankCell(cell)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlankCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty();
    }

    private static Set<String> getExcelHeaderNames(Class<?> head) {
        Set<String> headers = new HashSet<>();
        for (Field field : head.getDeclaredFields()) {
            ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
            if (excelProperty == null) {
                continue;
            }
            for (String value : excelProperty.value()) {
                if (value != null && !value.isEmpty()) {
                    headers.add(value);
                }
            }
        }
        return headers;
    }

    private static Map<String, String> buildHeaderAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("供应商名称", "供应商");
        aliases.put("仓库", "所属仓库");
        aliases.put("产品编码", "配件编码");
        aliases.put("产品名称", "配件名称");
        aliases.put("配件编码", "产品编码");
        aliases.put("配件名称", "产品名称");
        aliases.put("产品编码\uff08\u4e8c\u9009\u4e00\uff09", "配件编码");
        aliases.put("产品名称\uff08\u4e8c\u9009\u4e00\uff09", "配件名称");
        aliases.put("配件编码\uff08\u4e8c\u9009\u4e00\uff09", "配件编码");
        aliases.put("配件名称\uff08\u4e8c\u9009\u4e00\uff09", "配件名称");
        aliases.put("产品编码\uff08\u4e09\u9009\u4e00\uff09", "配件编码");
        aliases.put("产品名称\uff08\u4e09\u9009\u4e00\uff09", "配件名称");
        aliases.put("配件编码\uff08\u4e09\u9009\u4e00\uff09", "配件编码");
        aliases.put("配件名称\uff08\u4e09\u9009\u4e00\uff09", "配件名称");
        aliases.put("厂家编码\uff08\u4e8c\u9009\u4e00\uff09", "厂家编码");
        aliases.put("厂家编码\uff08\u4e09\u9009\u4e00\uff09", "厂家编码");
        return aliases;
    }

    private static String formatReadConvertError(ExcelDataConvertException ex) {
        Integer rowIndex = ex.getRowIndex();
        Integer columnIndex = ex.getColumnIndex();
        String rowText = rowIndex == null ? "未知行" : "第 " + (rowIndex + 1) + " 行";
        String columnText = resolveHeaderName(ex.getExcelContentProperty());
        if (columnText == null && columnIndex != null) {
            columnText = "第 " + (columnIndex + 1) + " 列";
        }
        String fieldText = columnText == null ? "" : "，字段【" + columnText + "】";
        return rowText + fieldText + "格式不正确，请检查单元格内容；日期请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss";
    }

    private static String resolveHeaderName(ExcelContentProperty contentProperty) {
        if (contentProperty == null || contentProperty.getField() == null) {
            return null;
        }
        ExcelProperty excelProperty = contentProperty.getField().getAnnotation(ExcelProperty.class);
        if (excelProperty == null || excelProperty.value().length == 0) {
            return contentProperty.getField().getName();
        }
        String[] values = excelProperty.value();
        return values[values.length - 1];
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> targetClass) {
        Throwable current = throwable;
        while (current != null) {
            if (targetClass.isInstance(current)) {
                return targetClass.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    public static class ExcelOperationContext {

        private final String operation;
        private final String filename;
        private final Integer dataCount;
        private final Set<String> includeColumnFieldNames;
        private final String headClassName;
        private final List<?> readRows;

        public ExcelOperationContext(String operation, String filename, Integer dataCount,
                                     Set<String> includeColumnFieldNames) {
            this(operation, filename, dataCount, includeColumnFieldNames, null, null);
        }

        public ExcelOperationContext(String operation, String filename, Integer dataCount,
                                     Set<String> includeColumnFieldNames, String headClassName, List<?> readRows) {
            this.operation = operation;
            this.filename = filename;
            this.dataCount = dataCount;
            this.includeColumnFieldNames = includeColumnFieldNames;
            this.headClassName = headClassName;
            this.readRows = readRows;
        }

        public String getOperation() {
            return operation;
        }

        public String getFilename() {
            return filename;
        }

        public Integer getDataCount() {
            return dataCount;
        }

        public Set<String> getIncludeColumnFieldNames() {
            return includeColumnFieldNames;
        }

        public String getHeadClassName() {
            return headClassName;
        }

        public List<?> getReadRows() {
            return readRows;
        }

    }

}
