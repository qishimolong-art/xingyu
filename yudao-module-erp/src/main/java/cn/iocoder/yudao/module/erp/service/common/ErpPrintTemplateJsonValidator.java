package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ERP 打印模板 JSON 校验器。
 *
 * <p>版本 1 保持历史兼容；版本 2 使用唯一的 detailTable 明细表组件。</p>
 */
public final class ErpPrintTemplateJsonValidator {

    private static final Pattern CELL_PATTERN = Pattern.compile("^([A-Z]+)(\\d+)$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{\\s*([^}]+?)\\s*}");

    private ErpPrintTemplateJsonValidator() {
    }

    public static void validate(String templateJson) {
        Map<String, Object> template;
        try {
            template = JsonUtils.parseObject(templateJson, new TypeReference<Map<String, Object>>() { });
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("打印模板 JSON 格式不正确", ex);
        }
        validate(template);
    }

    @SuppressWarnings("unchecked")
    public static void validate(Map<String, Object> template) {
        if (template == null || intValue(template.get("version"), 1) < 2) {
            return;
        }
        List<String> columns = stringList(template.get("columns"));
        if (!(template.get("cells") instanceof List)) {
            throw invalid("cells 必须是数组");
        }
        List<Map<String, Object>> cells = new ArrayList<>();
        for (Object item : (List<?>) template.get("cells")) {
            if (!(item instanceof Map)) {
                throw invalid("cells 中存在非对象项");
            }
            cells.add((Map<String, Object>) item);
        }
        int rowCount = intValue(template.get("rowCount"), 0);
        if (columns.isEmpty() || rowCount < 1) {
            throw invalid("列定义或行数为空");
        }

        Object detailValue = template.get("detailTable");
        Map<String, Object> detailTable = detailValue instanceof Map
                ? (Map<String, Object>) detailValue : null;
        if (detailValue != null && detailTable == null) {
            throw invalid("detailTable 必须是单个对象");
        }

        DetailRange range = detailTable == null ? null : parseDetailRange(detailTable, columns, rowCount);
        List<String> placementErrors = new ArrayList<>();
        for (Map<String, Object> cell : cells) {
            validateCellBounds(cell, columns, rowCount);
            CellRect rect = cellRect(cell, columns);
            Set<String> codes = fieldCodes(cell);
            boolean hasDetailField = codes.stream().anyMatch(ErpPrintTemplateJsonValidator::isDetailField);
            boolean hasMainField = codes.stream().anyMatch(code -> !isDetailField(code));
            if (hasDetailField && (range == null || !range.containsBody(rect))) {
                placementErrors.add(stringValue(cell.get("cell")) + " 的明细字段位于循环体外");
            }
            if (hasMainField && range != null && range.intersectsBody(rect)) {
                placementErrors.add(stringValue(cell.get("cell")) + " 的主表或系统字段位于循环体内");
            }
            if (range != null && range.crossesSectionBoundary(rect)) {
                placementErrors.add(stringValue(cell.get("cell")) + " 的合并单元格跨越明细表区段或边界");
            }
        }
        if (!placementErrors.isEmpty()) {
            throw invalid(String.join("；", placementErrors));
        }
    }

    private static DetailRange parseDetailRange(Map<String, Object> detailTable, List<String> columns, int rowCount) {
        if (!"detail-table".equals(stringValue(detailTable.get("id")))) {
            throw invalid("明细表 id 必须为 detail-table");
        }
        if (!"items".equals(stringValue(detailTable.get("dataSource")))) {
            throw invalid("明细表数据集必须为 items");
        }
        Object repeatHeader = detailTable.get("repeatHeader");
        if (repeatHeader != null && !(repeatHeader instanceof Boolean)) {
            throw invalid("repeatHeader 必须是布尔值");
        }
        int left = columns.indexOf(stringValue(detailTable.get("startColumn")));
        int right = columns.indexOf(stringValue(detailTable.get("endColumn")));
        if (left < 0 || right < left) {
            throw invalid("明细表列范围无效");
        }
        RowRange header = parseOptionalRowRange(detailTable.get("headerRows"), "表头", rowCount);
        RowRange body = parseRequiredRowRange(detailTable.get("bodyRows"), "循环体", rowCount);
        RowRange footer = parseOptionalRowRange(detailTable.get("footerRows"), "表尾", rowCount);
        if (header != null && header.end + 1 != body.start) {
            throw invalid("表头与循环体必须连续且不能重叠");
        }
        if (footer != null && body.end + 1 != footer.start) {
            throw invalid("循环体与表尾必须连续且不能重叠");
        }
        return new DetailRange(left, right, header, body, footer);
    }

    @SuppressWarnings("unchecked")
    private static RowRange parseOptionalRowRange(Object value, String label, int rowCount) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw invalid(label + "范围格式不正确");
        }
        return parseRowRange((Map<String, Object>) value, label, rowCount);
    }

    @SuppressWarnings("unchecked")
    private static RowRange parseRequiredRowRange(Object value, String label, int rowCount) {
        if (!(value instanceof Map)) {
            throw invalid(label + "范围不能为空");
        }
        return parseRowRange((Map<String, Object>) value, label, rowCount);
    }

    private static RowRange parseRowRange(Map<String, Object> value, String label, int rowCount) {
        int start = intValue(value.get("start"), 0);
        int end = intValue(value.get("end"), 0);
        if (start < 1 || end < start || end > rowCount) {
            throw invalid(label + "范围越界");
        }
        return new RowRange(start, end);
    }

    private static void validateCellBounds(Map<String, Object> cell, List<String> columns, int rowCount) {
        CellRect rect = cellRect(cell, columns);
        if (rect.left < 0 || rect.right >= columns.size() || rect.top < 1 || rect.bottom > rowCount) {
            throw invalid("单元格 " + stringValue(cell.get("cell")) + " 范围越界");
        }
    }

    private static CellRect cellRect(Map<String, Object> cell, List<String> columns) {
        Matcher matcher = CELL_PATTERN.matcher(stringValue(cell.get("cell")));
        if (!matcher.matches()) {
            return new CellRect(-1, -1, 0, 0);
        }
        int left = columns.indexOf(matcher.group(1));
        int top = Integer.parseInt(matcher.group(2));
        int colspan = Math.max(1, intValue(cell.get("colspan"), 1));
        int rowspan = Math.max(1, intValue(cell.get("rowspan"), 1));
        return new CellRect(left, left + colspan - 1, top, top + rowspan - 1);
    }

    private static Set<String> fieldCodes(Map<String, Object> cell) {
        Set<String> codes = new LinkedHashSet<>();
        String directCode = normalizeCode(stringValue(cell.get("code")));
        if (!directCode.isEmpty()) {
            codes.add(directCode);
        }
        Map<String, Object> placeholderCodes = mapValue(cell.get("placeholderCodes"));
        for (Object value : placeholderCodes.values()) {
            String code = normalizeCode(stringValue(value));
            if (!code.isEmpty()) {
                codes.add(code);
            }
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(stringValue(cell.get("html")));
        while (matcher.find()) {
            String raw = normalizeCode(matcher.group(1));
            String code = normalizeCode(stringValue(placeholderCodes.getOrDefault(raw, raw)));
            if (code.contains(".")) {
                codes.add(code);
            }
        }
        return codes;
    }

    private static boolean isDetailField(String code) {
        return normalizeCode(code).startsWith("items.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add(stringValue(item));
        }
        return result;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(stringValue(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalizeCode(String value) {
        String code = stringValue(value);
        if (code.startsWith("${") && code.endsWith("}")) {
            return code.substring(2, code.length() - 1).trim();
        }
        return code;
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("打印模板版本 2 校验失败：" + message);
    }

    private static final class RowRange {
        private final int start;
        private final int end;

        private RowRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class CellRect {
        private final int left;
        private final int right;
        private final int top;
        private final int bottom;

        private CellRect(int left, int right, int top, int bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    private static final class DetailRange {
        private final int left;
        private final int right;
        private final RowRange header;
        private final RowRange body;
        private final RowRange footer;

        private DetailRange(int left, int right, RowRange header, RowRange body, RowRange footer) {
            this.left = left;
            this.right = right;
            this.header = header;
            this.body = body;
            this.footer = footer;
        }

        private boolean containsBody(CellRect cell) {
            return cell.left >= left && cell.right <= right && cell.top >= body.start && cell.bottom <= body.end;
        }

        private boolean intersectsBody(CellRect cell) {
            return cell.left <= right && cell.right >= left && cell.top <= body.end && cell.bottom >= body.start;
        }

        private boolean crossesSectionBoundary(CellRect cell) {
            Set<String> sections = new LinkedHashSet<>();
            for (int row = cell.top; row <= cell.bottom; row++) {
                for (int column = cell.left; column <= cell.right; column++) {
                    sections.add(section(row, column));
                }
            }
            return sections.size() > 1 && sections.stream().anyMatch(section -> !"outside".equals(section));
        }

        private String section(int row, int column) {
            if (column < left || column > right) {
                return "outside";
            }
            if (header != null && row >= header.start && row <= header.end) {
                return "header";
            }
            if (row >= body.start && row <= body.end) {
                return "body";
            }
            if (footer != null && row >= footer.start && row <= footer.end) {
                return "footer";
            }
            return "outside";
        }
    }
}
