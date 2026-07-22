package cn.iocoder.yudao.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.converters.longconverter.LongStringConverter;
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
                .registerWriteHandler(new SelectSheetWriteHandler(head))
                .registerConverter(new LongStringConverter())
                .includeColumnFieldNames(includeColumnFieldNames)
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
                .registerWriteHandler(new SelectSheetWriteHandler(head))
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
                .registerWriteHandler(new SelectSheetWriteHandler(head))
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
            List<T> rows = FastExcelFactory.read(normalizedInputStream, head, null)
                    .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                    .doReadAllSync();
            LAST_OPERATION.set(new ExcelOperationContext("READ",
                    file == null ? null : file.getOriginalFilename(), rows == null ? 0 : rows.size(), null));
            return rows;
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
        if (expectedHeaders.contains(normalizedValue)) {
            cell.setCellValue(normalizedValue);
            return;
        }
        String alias = HEADER_ALIASES.get(normalizedValue);
        if (alias != null && expectedHeaders.contains(alias)) {
            cell.setCellValue(alias);
        }
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
        return aliases;
    }

    public static class ExcelOperationContext {

        private final String operation;
        private final String filename;
        private final Integer dataCount;
        private final Set<String> includeColumnFieldNames;

        public ExcelOperationContext(String operation, String filename, Integer dataCount,
                                     Set<String> includeColumnFieldNames) {
            this.operation = operation;
            this.filename = filename;
            this.dataCount = dataCount;
            this.includeColumnFieldNames = includeColumnFieldNames;
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

    }

}
