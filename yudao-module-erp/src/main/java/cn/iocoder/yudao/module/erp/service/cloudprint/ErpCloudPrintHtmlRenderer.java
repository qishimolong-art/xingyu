package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ErpCloudPrintHtmlRenderer {

    private static final int DEFAULT_HTML_WIDTH = 210;
    private static final int DEFAULT_PDF_WIDTH = 241;
    private static final int DEFAULT_PDF_HEIGHT = 140;
    private static final String PDF_FONT_FAMILY = "CloudPrintCjk";
    private static final int DEFAULT_ROW_HEIGHT = 34;
    private static final int MIN_ROW_HEIGHT = 28;
    private static final double PAPER_PX_PER_MM = 96D / 25.4D;
    private static final Pattern CELL_PATTERN = Pattern.compile("^([A-Z]+)(\\d+)$");
    private static final Pattern FIELD_PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{\\s*([^}]+?)\\s*}");
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&(#\\d+|#x[0-9A-Fa-f]+|[A-Za-z][A-Za-z0-9]+);");
    private static final Pattern TOKEN_SPAN_PATTERN = Pattern.compile(
            "<span\\b([^>]*)class=([\"'])(?=[^\"']*\\btoken\\b)[^\"']*\\2([^>]*)>([\\s\\S]*?)</span>",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> CHINESE_FONT_PATHS = Arrays.asList(
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/simsun.ttf",
            "C:/Windows/Fonts/msyh.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/arphic/uming.ttc"
    );
    private static final Map<String, String> FALLBACK_FIELD_CODE_BY_NAME = buildFallbackFieldCodeByName();
    private static final Map<String, String> XHTML_ENTITY_REPLACEMENT = buildXhtmlEntityReplacement();

    public byte[] renderSaleOutPdf(Map<String, Object> data, Integer pageWidth, Integer pageHeight) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            String html = buildSaleOutHtml(data,
                    pageWidth != null && pageWidth > 0 ? pageWidth : DEFAULT_PDF_WIDTH,
                    pageHeight != null && pageHeight > 0 ? pageHeight : DEFAULT_PDF_HEIGHT,
                    true);
            PdfRendererBuilder builder = new PdfRendererBuilder()
                    .withHtmlContent(html, null)
                    .toStream(outputStream);
            registerChineseFont(builder);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成销售单云打印 PDF 失败", ex);
        }
    }

    public byte[] renderSaleOutTemplatePdf(String templateJson, String paperConfig,
                                           Map<String, Object> data, Integer pageWidth, Integer pageHeight) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Map<String, Object> template = JsonUtils.parseObject(templateJson,
                    new TypeReference<Map<String, Object>>() {});
            if (template == null || !(template.get("cells") instanceof List)) {
                throw new IllegalArgumentException("销售单打印模板内容为空或格式不正确");
            }
            Map<String, Object> paper = mergePaperConfig(template.get("paper"), paperConfig, pageWidth, pageHeight);
            String html = buildTemplateHtml(template, data, paper);
            PdfRendererBuilder builder = new PdfRendererBuilder()
                    .withHtmlContent(html, null)
                    .toStream(outputStream);
            registerChineseFont(builder);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("按打印模板生成销售单云打印 PDF 失败", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public byte[] renderSaleOut(Map<String, Object> data) {
        return buildSaleOutHtml(data, DEFAULT_HTML_WIDTH, null, false).getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private String buildTemplateHtml(Map<String, Object> template, Map<String, Object> printData,
                                     Map<String, Object> paper) {
        List<Map<String, Object>> cells = (List<Map<String, Object>>) template.get("cells");
        List<String> columns = getColumns(template);
        int rowCount = Math.max(intValue(template.get("rowCount"), 1), maxCellRow(cells));
        Map<String, Object> rowHeights = mapValue(template.get("rowHeights"));
        RenderBounds bounds = renderBounds(cells, columns);
        List<String> renderColumns = columns.subList(bounds.minColumnIndex, bounds.maxColumnIndex + 1);
        List<DetailBlock> detailBlocks = detailBlocks(cells);
        List<RenderRow> renderRows = buildRenderRows(bounds.minRow, rowCount, detailBlocks, printData);
        List<List<RenderRow>> pages = paginateRenderRows(cells, renderRows, detailBlocks, rowHeights, paper, printData);
        Map<String, Map<String, Object>> cellMap = new LinkedHashMap<>();
        for (Map<String, Object> cell : cells) {
            String cellId = stringValue(cell.get("cell"));
            if (isRenderableCell(cell) && !cellId.isEmpty()) {
                cellMap.put(cellId, cell);
            }
        }

        StringBuilder html = new StringBuilder(16384);
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"utf-8\"/><style>");
        appendTemplateStyle(html, paper);
        html.append("</style></head><body>");
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            appendTemplatePage(html, template, pages.get(pageIndex), renderColumns, rowHeights, cellMap, printData);
        }
        html.append("</body></html>");
        return html.toString();
    }

    private void appendTemplatePage(StringBuilder html, Map<String, Object> template, List<RenderRow> pageRows,
                                    List<String> renderColumns, Map<String, Object> rowHeights,
                                    Map<String, Map<String, Object>> cellMap, Map<String, Object> printData) {
        html.append("<div class=\"print-page\"><table class=\"print-sheet\"><colgroup>");
        for (String column : renderColumns) {
            html.append("<col style=\"width:").append(escapeCssSize(columnWidth(template, column))).append(";\"/>");
        }
        html.append("</colgroup><tbody>");
        for (RenderRow renderRow : pageRows) {
            html.append("<tr style=\"height:")
                    .append(escapeCssSize(rowHeight(rowHeights, renderRow.sourceRow)))
                    .append(";min-height:")
                    .append(escapeCssSize(rowHeight(rowHeights, renderRow.sourceRow)))
                    .append(";\">");
            for (int columnIndex = 0; columnIndex < renderColumns.size(); columnIndex++) {
                if (isCoveredBySpan(cellMap, renderColumns, renderRow.sourceRow, columnIndex)) {
                    continue;
                }
                String cellId = renderColumns.get(columnIndex) + renderRow.sourceRow;
                Map<String, Object> cell = cellMap.get(cellId);
                if (cell == null) {
                    html.append("<td class=\"print-cell print-cell-empty\"></td>");
                    continue;
                }
                int colspan = Math.min(intValue(cell.get("colspan"), 1), renderColumns.size() - columnIndex);
                int rowspan = intValue(cell.get("rowspan"), 1);
                html.append("<td class=\"").append(escapeAttribute(cellClassName(cell))).append("\"");
                if (colspan > 1) {
                    html.append(" colspan=\"").append(colspan).append("\"");
                }
                int finalRowspan = Math.min(rowspan, remainingRows(pageRows, renderRow.sourceRow));
                if (finalRowspan > 1) {
                    html.append(" rowspan=\"").append(finalRowspan).append("\"");
                }
                String style = cellStyle(cell);
                if (!style.isEmpty()) {
                    html.append(" style=\"").append(escapeAttribute(style)).append("\"");
                }
                html.append(">")
                        .append(resolveCellHtml(cell, printData, renderRow.itemIndex))
                        .append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table></div>");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergePaperConfig(Object templatePaper, String paperConfig,
                                                 Integer pageWidth, Integer pageHeight) {
        Map<String, Object> paper = new LinkedHashMap<>();
        paper.put("width", pageWidth != null && pageWidth > 0 ? pageWidth : DEFAULT_PDF_WIDTH);
        paper.put("height", pageHeight != null && pageHeight > 0 ? pageHeight : DEFAULT_PDF_HEIGHT);
        paper.put("orientation", "portrait");
        paper.put("margins", defaultMargins());
        Map<String, Object> savedPaper = mapValue(templatePaper);
        if (paperConfig != null && !paperConfig.isEmpty()) {
            Map<String, Object> configPaper = JsonUtils.parseObject(paperConfig,
                    new TypeReference<Map<String, Object>>() {});
            if (configPaper != null) {
                savedPaper.putAll(configPaper);
            }
        }
        paper.putAll(savedPaper);
        if (!savedPaper.containsKey("margins")) {
            paper.put("margins", defaultMargins());
        }
        return paper;
    }

    private Map<String, Object> defaultMargins() {
        Map<String, Object> margins = new LinkedHashMap<>();
        margins.put("top", 4);
        margins.put("right", 6);
        margins.put("bottom", 4);
        margins.put("left", 6);
        return margins;
    }

    private void appendTemplateStyle(StringBuilder html, Map<String, Object> paper) {
        Map<String, Object> margins = mapValue(paper.get("margins"));
        double width = doubleValue(paper.get("width"), DEFAULT_PDF_WIDTH);
        double height = doubleValue(paper.get("height"), DEFAULT_PDF_HEIGHT);
        if ("landscape".equals(stringValue(paper.get("orientation"))) && width < height) {
            double nextWidth = height;
            height = width;
            width = nextWidth;
        }
        html.append("@page{size:").append(formatNumber(width)).append("mm ")
                .append(formatNumber(height)).append("mm;margin:")
                .append(formatNumber(doubleValue(margins.get("top"), 4))).append("mm ")
                .append(formatNumber(doubleValue(margins.get("right"), 6))).append("mm ")
                .append(formatNumber(doubleValue(margins.get("bottom"), 4))).append("mm ")
                .append(formatNumber(doubleValue(margins.get("left"), 6))).append("mm;}");
        html.append("body{font-family:")
                .append(PDF_FONT_FAMILY)
                .append(",SimSun,'Microsoft YaHei',sans-serif;font-size:10px;color:#000;margin:0;}");
        html.append(".print-page{width:100%;box-sizing:border-box;page-break-after:always;}");
        html.append(".print-page:last-child{page-break-after:auto;}");
        html.append(".print-sheet{width:100%;border-collapse:collapse;table-layout:fixed;}");
        html.append(".print-cell{box-sizing:border-box;padding:2px 3px;line-height:1.25;vertical-align:middle;word-wrap:break-word;white-space:normal;}");
        html.append(".print-cell-empty{padding:0;}");
        html.append(".table-border,.manual-border{border:1px solid #000;}");
        html.append(".border-top{border-top:1px solid #000;}.border-right{border-right:1px solid #000;}");
        html.append(".border-bottom,.line-bottom{border-bottom:1px solid #000;}.border-left{border-left:1px solid #000;}");
        html.append(".bold{font-weight:700;}.blue-title,.contract-title{font-weight:700;font-size:16px;text-align:center;}");
        html.append(".align-left{text-align:left;}.align-center{text-align:center;}.align-right{text-align:right;}");
        html.append(".token{font:inherit;color:inherit;}");
    }

    private String resolveCellHtml(Map<String, Object> cell, Map<String, Object> printData, int itemIndex) {
        String code = normalizeFieldCode(stringValue(cell.get("code")));
        String html = stringValue(cell.get("html"));
        Map<String, Object> placeholderCodes = mapValue(cell.get("placeholderCodes"));
        String name = stringValue(cell.get("name"));
        if (!name.isEmpty() && !code.isEmpty()) {
            placeholderCodes.put(name, code);
        }
        if (html.contains("${")) {
            return normalizeTemplateHtml(renderFieldPlaceholders(html, placeholderCodes, printData, itemIndex));
        }
        if (!code.isEmpty()) {
            return escape(resolvePrintValue(code, printData, itemIndex));
        }
        return normalizeTemplateHtml(html);
    }

    private String renderFieldPlaceholders(String html, Map<String, Object> placeholderCodes,
                                           Map<String, Object> printData, int itemIndex) {
        Matcher matcher = FIELD_PLACEHOLDER_PATTERN.matcher(html);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String code = resolvePlaceholderCode(matcher.group(1), placeholderCodes);
            String value = code.isEmpty() ? matcher.group(0) : escape(resolvePrintValue(code, printData, itemIndex));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return TOKEN_SPAN_PATTERN.matcher(rendered.toString()).replaceAll("$4");
    }

    @SuppressWarnings("unchecked")
    private Object resolvePrintValue(String code, Map<String, Object> printData, int itemIndex) {
        String normalized = normalizeFieldCode(code);
        if (normalized.isEmpty() || printData == null) {
            return "";
        }
        if (normalized.startsWith("items.")) {
            List<Map<String, Object>> items = printData.get("items") instanceof List
                    ? (List<Map<String, Object>>) printData.get("items") : Collections.emptyList();
            Map<String, Object> item = itemIndex >= 0 && itemIndex < items.size() ? items.get(itemIndex) : null;
            Object value = value(item, normalized);
            return value != null ? value : value(item, normalized.substring("items.".length()));
        }
        Object value = value(mapValue(printData.get("main")), normalized);
        if (value != null) {
            return value;
        }
        value = value(mapValue(printData.get("system")), normalized);
        if (value != null) {
            return value;
        }
        value = value(mapValue(printData.get("currentUser")), normalized);
        if (value != null) {
            return value;
        }
        return value(mapValue(printData.get("print")), normalized);
    }

    private String resolvePlaceholderCode(String raw, Map<String, Object> placeholderCodes) {
        String normalized = normalizeFieldCode(raw);
        if (normalized.isEmpty()) {
            return "";
        }
        Object mapped = placeholderCodes.get(normalized);
        if (mapped != null) {
            return normalizeFieldCode(String.valueOf(mapped));
        }
        String fallback = FALLBACK_FIELD_CODE_BY_NAME.get(normalized);
        if (fallback != null) {
            return normalizeFieldCode(fallback);
        }
        return normalized.contains(".") ? normalized : "";
    }

    private String normalizeFieldCode(String code) {
        String raw = code == null ? "" : code.trim();
        if (raw.startsWith("${") && raw.endsWith("}")) {
            raw = raw.substring(2, raw.length() - 1).trim();
        }
        return raw;
    }

    private List<DetailBlock> detailBlocks(List<Map<String, Object>> cells) {
        List<DetailBlock> blocks = new ArrayList<>();
        for (Map<String, Object> cell : cells) {
            if (!isItemFieldCell(cell)) {
                continue;
            }
            CellParts parts = cellParts(cell);
            int start = parts.row;
            int end = start + intValue(cell.get("rowspan"), 1) - 1;
            mergeBlock(blocks, start, end);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map<String, Object> cell : cells) {
                if (!isRenderableCell(cell)) {
                    continue;
                }
                CellParts parts = cellParts(cell);
                int start = parts.row;
                int end = start + intValue(cell.get("rowspan"), 1) - 1;
                for (DetailBlock block : blocks) {
                    if (start <= block.end && end >= block.start) {
                        int nextStart = Math.min(block.start, start);
                        int nextEnd = Math.max(block.end, end);
                        if (nextStart != block.start || nextEnd != block.end) {
                            block.start = nextStart;
                            block.end = nextEnd;
                            changed = true;
                        }
                    }
                }
            }
            changed = mergeAdjacentBlocks(blocks) || changed;
        }
        return blocks;
    }

    private List<RenderRow> buildRenderRows(int startRow, int rowCount, List<DetailBlock> blocks,
                                            Map<String, Object> printData) {
        int itemCount = printData != null && printData.get("items") instanceof List
                ? Math.max(1, ((List<?>) printData.get("items")).size()) : 1;
        List<RenderRow> rows = new ArrayList<>();
        for (int row = Math.max(1, startRow); row <= rowCount; row++) {
            DetailBlock block = findBlockStartingAt(blocks, row);
            if (block != null) {
                for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                    for (int sourceRow = block.start; sourceRow <= block.end; sourceRow++) {
                        rows.add(new RenderRow(sourceRow, itemIndex, true));
                    }
                }
                row = block.end;
                continue;
            }
            rows.add(new RenderRow(row, 0, false));
        }
        return rows;
    }

    private List<List<RenderRow>> paginateRenderRows(List<Map<String, Object>> cells, List<RenderRow> renderRows,
                                                     List<DetailBlock> blocks, Map<String, Object> rowHeights,
                                                     Map<String, Object> paper, Map<String, Object> printData) {
        double capacity = paperContentHeightPx(paper);
        int itemCount = printItemCount(printData);
        if (blocks.isEmpty() || itemCount <= 0) {
            return splitRowsByHeight(renderRows, rowHeights, capacity);
        }

        DetailBlock firstBlock = blocks.get(0);
        DetailBlock lastBlock = blocks.get(blocks.size() - 1);
        Set<Integer> headerRows = detectDetailHeaderRows(cells, firstBlock);
        List<RenderRow> headerPlans = filterRows(renderRows, false, headerRows, null, null);
        List<List<RenderRow>> detailPlansByItem = new ArrayList<>();
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            detailPlansByItem.add(filterRows(renderRows, true, null, itemIndex, itemIndex));
        }
        int lastRenderableRow = lastRenderableSourceRow(cells);
        int topBoundary = headerRows.isEmpty() ? firstBlock.start : Collections.min(headerRows);
        List<RenderRow> topPlans = filterRows(renderRows, false, null, null, topBoundary - 1);
        List<RenderRow> bottomPlans = new ArrayList<>();
        for (RenderRow row : renderRows) {
            if (!row.detail && row.sourceRow > lastBlock.end && row.sourceRow <= lastRenderableRow) {
                bottomPlans.add(row);
            }
        }

        List<List<RenderRow>> pages = new ArrayList<>();
        int nextItemIndex = 0;
        PageBuildResult first = createDetailPage(topPlans, headerPlans, detailPlansByItem, bottomPlans,
                nextItemIndex, rowHeights, capacity, true);
        pages.add(first.rows);
        nextItemIndex = first.nextItemIndex;
        boolean bottomPlaced = first.bottomPlaced;

        while (nextItemIndex < detailPlansByItem.size()) {
            PageBuildResult next = createDetailPage(Collections.emptyList(), headerPlans, detailPlansByItem,
                    bottomPlans, nextItemIndex, rowHeights, capacity, true);
            pages.add(next.rows);
            nextItemIndex = next.nextItemIndex;
            bottomPlaced = next.bottomPlaced;
        }
        if (!bottomPlaced && !bottomPlans.isEmpty()) {
            pages.addAll(splitRowsByHeight(bottomPlans, rowHeights, capacity));
        }
        return nonEmptyPages(pages);
    }

    private PageBuildResult createDetailPage(List<RenderRow> topPlans, List<RenderRow> headerPlans,
                                             List<List<RenderRow>> detailPlansByItem, List<RenderRow> bottomPlans,
                                             int startItemIndex, Map<String, Object> rowHeights,
                                             double capacity, boolean allowBottom) {
        List<RenderRow> page = new ArrayList<>(topPlans);
        page.addAll(headerPlans);
        double used = planHeight(page, rowHeights);
        int nextItemIndex = startItemIndex;
        int detailCount = 0;
        while (nextItemIndex < detailPlansByItem.size()) {
            List<RenderRow> group = detailPlansByItem.get(nextItemIndex);
            double groupHeight = planHeight(group, rowHeights);
            if (detailCount > 0 && used + groupHeight > capacity) {
                break;
            }
            if (detailCount == 0 && used + groupHeight > capacity && page.size() > topPlans.size() + headerPlans.size()) {
                break;
            }
            page.addAll(group);
            used += groupHeight;
            nextItemIndex++;
            detailCount++;
            if (used >= capacity) {
                break;
            }
        }
        if (detailCount == 0 && nextItemIndex < detailPlansByItem.size()) {
            List<RenderRow> forcedGroup = detailPlansByItem.get(nextItemIndex);
            page.addAll(forcedGroup);
            used += planHeight(forcedGroup, rowHeights);
            nextItemIndex++;
        }
        if (allowBottom && nextItemIndex >= detailPlansByItem.size()) {
            double bottomHeight = planHeight(bottomPlans, rowHeights);
            if (used + bottomHeight <= capacity) {
                page.addAll(bottomPlans);
                return new PageBuildResult(page, nextItemIndex, true);
            }
        }
        return new PageBuildResult(page, nextItemIndex, false);
    }

    private List<List<RenderRow>> splitRowsByHeight(List<RenderRow> rows, Map<String, Object> rowHeights,
                                                   double capacity) {
        List<List<RenderRow>> pages = new ArrayList<>();
        List<RenderRow> current = new ArrayList<>();
        double used = 0;
        for (RenderRow row : rows) {
            double height = rowHeightValue(rowHeights, row.sourceRow);
            if (!current.isEmpty() && used + height > capacity) {
                pages.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(row);
            used += height;
        }
        if (!current.isEmpty() || pages.isEmpty()) {
            pages.add(current);
        }
        return nonEmptyPages(pages);
    }

    private List<List<RenderRow>> nonEmptyPages(List<List<RenderRow>> pages) {
        List<List<RenderRow>> result = new ArrayList<>();
        for (List<RenderRow> page : pages) {
            if (!page.isEmpty()) {
                result.add(page);
            }
        }
        if (result.isEmpty()) {
            result.add(Collections.emptyList());
        }
        return result;
    }

    private List<RenderRow> filterRows(List<RenderRow> rows, boolean detail, Set<Integer> sourceRows,
                                       Integer itemIndex, Integer maxSourceRow) {
        List<RenderRow> result = new ArrayList<>();
        for (RenderRow row : rows) {
            if (row.detail != detail) {
                continue;
            }
            if (sourceRows != null && !sourceRows.contains(row.sourceRow)) {
                continue;
            }
            if (itemIndex != null && row.itemIndex != itemIndex) {
                continue;
            }
            if (maxSourceRow != null && row.sourceRow > maxSourceRow) {
                continue;
            }
            result.add(row);
        }
        return result;
    }

    private Set<Integer> detectDetailHeaderRows(List<Map<String, Object>> cells, DetailBlock firstBlock) {
        Set<Integer> rows = new HashSet<>();
        if (firstBlock == null) {
            return rows;
        }
        for (int row = firstBlock.start - 1; row >= 1; row--) {
            boolean hasHeaderCell = false;
            for (Map<String, Object> cell : cells) {
                if (!isRenderableCell(cell)) {
                    continue;
                }
                CellRowRange range = cellRowRange(cell);
                if (range.start <= row && row <= range.end && hasTableBorderClass(cell) && !isItemFieldCell(cell)) {
                    hasHeaderCell = true;
                    break;
                }
            }
            if (!hasHeaderCell) {
                break;
            }
            rows.add(row);
        }
        return rows;
    }

    private boolean hasTableBorderClass(Map<String, Object> cell) {
        for (String className : classes(cell)) {
            if ("table-border".equals(className) || "manual-border".equals(className)
                    || "border-top".equals(className) || "border-right".equals(className)
                    || "border-bottom".equals(className) || "border-left".equals(className)
                    || "line-bottom".equals(className)) {
                return true;
            }
        }
        return false;
    }

    private int lastRenderableSourceRow(List<Map<String, Object>> cells) {
        int maxRow = 1;
        for (Map<String, Object> cell : cells) {
            if (!isRenderableCell(cell)) {
                continue;
            }
            maxRow = Math.max(maxRow, cellRowRange(cell).end);
        }
        return maxRow;
    }

    private CellRowRange cellRowRange(Map<String, Object> cell) {
        CellParts parts = cellParts(cell);
        return new CellRowRange(parts.row, parts.row + intValue(cell.get("rowspan"), 1) - 1);
    }

    private double planHeight(List<RenderRow> rows, Map<String, Object> rowHeights) {
        double total = 0;
        for (RenderRow row : rows) {
            total += rowHeightValue(rowHeights, row.sourceRow);
        }
        return total;
    }

    private double rowHeightValue(Map<String, Object> rowHeights, int row) {
        return Math.max(MIN_ROW_HEIGHT, cssSizeToNumber(rowHeight(rowHeights, row), DEFAULT_ROW_HEIGHT));
    }

    private double paperContentHeightPx(Map<String, Object> paper) {
        Map<String, Object> margins = mapValue(paper.get("margins"));
        double width = doubleValue(paper.get("width"), DEFAULT_PDF_WIDTH);
        double height = doubleValue(paper.get("height"), DEFAULT_PDF_HEIGHT);
        if ("landscape".equals(stringValue(paper.get("orientation"))) && width < height) {
            height = width;
        }
        double contentHeight = mmToPx(height)
                - mmToPx(doubleValue(margins.get("top"), 4))
                - mmToPx(doubleValue(margins.get("bottom"), 4));
        return Math.max(1, contentHeight);
    }

    @SuppressWarnings("unchecked")
    private int printItemCount(Map<String, Object> printData) {
        return printData != null && printData.get("items") instanceof List
                ? ((List<Map<String, Object>>) printData.get("items")).size() : 0;
    }

    private RenderBounds renderBounds(List<Map<String, Object>> cells, List<String> columns) {
        int minRow = Integer.MAX_VALUE;
        int minColumn = Integer.MAX_VALUE;
        int maxColumn = 0;
        for (Map<String, Object> cell : cells) {
            if (!isRenderableCell(cell)) {
                continue;
            }
            CellParts parts = cellParts(cell);
            int columnIndex = columns.indexOf(parts.column);
            if (columnIndex < 0) {
                continue;
            }
            minRow = Math.min(minRow, parts.row);
            minColumn = Math.min(minColumn, columnIndex);
            maxColumn = Math.max(maxColumn, columnIndex + intValue(cell.get("colspan"), 1) - 1);
        }
        if (minRow == Integer.MAX_VALUE) {
            return new RenderBounds(1, 0, Math.max(0, columns.size() - 1));
        }
        return new RenderBounds(minRow, Math.max(0, minColumn), Math.min(columns.size() - 1, maxColumn));
    }

    private boolean isRenderableCell(Map<String, Object> cell) {
        if (cell == null || stringValue(cell.get("cell")).isEmpty()) {
            return false;
        }
        return !stringValue(cell.get("html")).replaceAll("<br\\s*/?>", "").trim().isEmpty()
                || !stringValue(cell.get("text")).trim().isEmpty()
                || !stringValue(cell.get("name")).isEmpty()
                || !stringValue(cell.get("code")).isEmpty()
                || intValue(cell.get("colspan"), 1) > 1
                || intValue(cell.get("rowspan"), 1) > 1
                || hasVisibleClass(cell);
    }

    private boolean isItemFieldCell(Map<String, Object> cell) {
        String code = normalizeFieldCode(stringValue(cell.get("code")));
        String html = stringValue(cell.get("html"));
        if (code.startsWith("items.") || html.contains("${items.")) {
            return true;
        }
        for (Object value : mapValue(cell.get("placeholderCodes")).values()) {
            if (normalizeFieldCode(String.valueOf(value)).startsWith("items.")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasVisibleClass(Map<String, Object> cell) {
        Set<String> visibleClasses = new HashSet<>(Arrays.asList(
                "bold", "logo", "blue-title", "contract-title", "line-bottom",
                "table-border", "manual-border", "border-top", "border-right", "border-bottom", "border-left"));
        for (String className : classes(cell)) {
            if (visibleClasses.contains(className)) {
                return true;
            }
        }
        return false;
    }

    private void mergeBlock(List<DetailBlock> blocks, int start, int end) {
        for (DetailBlock block : blocks) {
            if (start <= block.end + 1 && end >= block.start - 1) {
                block.start = Math.min(block.start, start);
                block.end = Math.max(block.end, end);
                return;
            }
        }
        blocks.add(new DetailBlock(start, end));
        blocks.sort((left, right) -> Integer.compare(left.start, right.start));
    }

    private boolean mergeAdjacentBlocks(List<DetailBlock> blocks) {
        for (int index = 0; index < blocks.size() - 1; index++) {
            DetailBlock current = blocks.get(index);
            DetailBlock next = blocks.get(index + 1);
            if (current.end + 1 < next.start) {
                continue;
            }
            current.end = Math.max(current.end, next.end);
            blocks.remove(index + 1);
            return true;
        }
        return false;
    }

    private DetailBlock findBlockStartingAt(List<DetailBlock> blocks, int row) {
        for (DetailBlock block : blocks) {
            if (block.start == row) {
                return block;
            }
        }
        return null;
    }

    private int remainingRows(List<RenderRow> renderRows, int sourceRow) {
        int count = 0;
        for (RenderRow row : renderRows) {
            if (row.sourceRow >= sourceRow) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private boolean isCoveredBySpan(Map<String, Map<String, Object>> cellMap, List<String> columns,
                                    int row, int columnIndex) {
        for (Map<String, Object> cell : cellMap.values()) {
            CellParts parts = cellParts(cell);
            int startColumn = columns.indexOf(parts.column);
            if (startColumn < 0 || (parts.row == row && startColumn == columnIndex)) {
                continue;
            }
            int endColumn = startColumn + intValue(cell.get("colspan"), 1) - 1;
            int endRow = parts.row + intValue(cell.get("rowspan"), 1) - 1;
            if (parts.row <= row && row <= endRow && startColumn <= columnIndex && columnIndex <= endColumn) {
                return true;
            }
        }
        return false;
    }

    private String cellClassName(Map<String, Object> cell) {
        List<String> classes = new ArrayList<>();
        classes.add("print-cell");
        classes.addAll(classes(cell));
        String textAlign = stringValue(cell.get("textAlign"));
        if (!textAlign.isEmpty() && classes.stream().noneMatch(name -> name.startsWith("align-"))) {
            classes.add("align-" + textAlign);
        }
        return String.join(" ", classes);
    }

    private String cellStyle(Map<String, Object> cell) {
        StringBuilder style = new StringBuilder();
        String fontSize = stringValue(cell.get("fontSize"));
        if (fontSize.matches("^\\d+(\\.\\d+)?(px|pt|mm|em|rem|%)$")) {
            style.append("font-size:").append(fontSize).append(";");
        }
        String textAlign = stringValue(cell.get("textAlign"));
        if (textAlign.matches("left|center|right")) {
            style.append("text-align:").append(textAlign).append(";");
        }
        return style.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> classes(Map<String, Object> cell) {
        Object rawClasses = cell.get("classes");
        if (!(rawClasses instanceof List)) {
            return Collections.emptyList();
        }
        List<String> classes = new ArrayList<>();
        for (Object rawClass : (List<Object>) rawClasses) {
            String className = stringValue(rawClass);
            if (className.matches("[A-Za-z0-9_-]+")) {
                classes.add(className);
            }
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    private List<String> getColumns(Map<String, Object> template) {
        Object rawColumns = template.get("columns");
        if (rawColumns instanceof List && !((List<?>) rawColumns).isEmpty()) {
            List<String> columns = new ArrayList<>();
            for (Object column : (List<Object>) rawColumns) {
                String value = stringValue(column);
                if (!value.isEmpty()) {
                    columns.add(value);
                }
            }
            if (!columns.isEmpty()) {
                return columns;
            }
        }
        return Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    private int maxCellRow(List<Map<String, Object>> cells) {
        int maxRow = 1;
        for (Map<String, Object> cell : cells) {
            CellParts parts = cellParts(cell);
            maxRow = Math.max(maxRow, parts.row + intValue(cell.get("rowspan"), 1) - 1);
        }
        return maxRow;
    }

    private CellParts cellParts(Map<String, Object> cell) {
        String cellId = stringValue(cell.get("cell"));
        Matcher matcher = CELL_PATTERN.matcher(cellId);
        return matcher.matches() ? new CellParts(matcher.group(1), Integer.parseInt(matcher.group(2)))
                : new CellParts("A", 1);
    }

    private String columnWidth(Map<String, Object> template, String column) {
        return stringValue(mapValue(template.get("columnWidths")).getOrDefault(column, "90px"));
    }

    private String rowHeight(Map<String, Object> rowHeights, int row) {
        return stringValue(rowHeights.getOrDefault(String.valueOf(row), DEFAULT_ROW_HEIGHT + "px"));
    }

    private String normalizeTemplateHtml(String html) {
        if (html == null) {
            return "";
        }
        String normalized = html.replaceAll("(?i)<br\\s*/?>", "<br />");
        Matcher matcher = ENTITY_PATTERN.matcher(normalized);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String entityName = matcher.group(1);
            String wholeEntity = matcher.group(0);
            String replacement = xhtmlEntityReplacement(entityName, wholeEntity);
            if (replacement == null) {
                throw new IllegalArgumentException("打印模板包含 PDF 渲染不支持的 HTML 实体：" + wholeEntity);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String escapeAttribute(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String escapeCssSize(String value) {
        String normalized = stringValue(value);
        return normalized.matches("^\\d+(\\.\\d+)?(px|pt|mm|cm|%)?$")
                ? normalized
                : DEFAULT_ROW_HEIGHT + "px";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number) {
            return Math.max(1, ((Number) value).intValue());
        }
        try {
            return Math.max(1, Integer.parseInt(String.valueOf(value)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double doubleValue(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double cssSizeToNumber(String value, double defaultValue) {
        String normalized = stringValue(value);
        Matcher matcher = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)").matcher(normalized);
        if (!matcher.find()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double mmToPx(double value) {
        return Math.round(value * PAPER_PX_PER_MM);
    }

    private String formatNumber(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0).toPlainString() : decimal.toPlainString();
    }

    private static Map<String, String> buildFallbackFieldCodeByName() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("序号", "items.seq");
        map.put("零件编码", "items.productCode");
        map.put("产品编码", "items.productCode");
        map.put("零件名称", "items.productName");
        map.put("产品名称", "items.productName");
        map.put("规格", "items.standard");
        map.put("车型", "items.vehicleModel");
        map.put("单位", "items.productUnitName");
        map.put("数量", "items.count");
        map.put("件数", "items.pieceCount");
        map.put("单价", "items.productPrice");
        map.put("金额", "items.totalPrice");
        map.put("产品金额", "items.totalProductPrice");
        map.put("仓库", "items.warehouseName");
        map.put("备注", "items.remark");
        map.put("合计数", "document.totalCount");
        map.put("合计数量", "document.totalCount");
        map.put("合计总金额", "document.totalAmount");
        map.put("大写金额", "document.totalAmountUpper");
        map.put("打印时间", "print.now");
        map.put("当前用户", "currentUser.nickname");
        return map;
    }

    private static Map<String, String> buildXhtmlEntityReplacement() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("nbsp", "&#160;");
        map.put("times", "&#215;");
        map.put("divide", "&#247;");
        map.put("middot", "&#183;");
        map.put("copy", "&#169;");
        map.put("reg", "&#174;");
        map.put("trade", "&#8482;");
        map.put("mdash", "&#8212;");
        map.put("ndash", "&#8211;");
        map.put("hellip", "&#8230;");
        map.put("laquo", "&#171;");
        map.put("raquo", "&#187;");
        map.put("lsquo", "&#8216;");
        map.put("rsquo", "&#8217;");
        map.put("ldquo", "&#8220;");
        map.put("rdquo", "&#8221;");
        return map;
    }

    private String xhtmlEntityReplacement(String entityName, String wholeEntity) {
        if (entityName.startsWith("#")) {
            return wholeEntity;
        }
        if ("amp".equals(entityName) || "lt".equals(entityName) || "gt".equals(entityName)
                || "quot".equals(entityName) || "apos".equals(entityName)) {
            return wholeEntity;
        }
        return XHTML_ENTITY_REPLACEMENT.get(entityName);
    }

    @SuppressWarnings("unchecked")
    private String buildSaleOutHtml(Map<String, Object> data, int pageWidth, Integer pageHeight, boolean pdfMode) {
        Map<String, Object> document = (Map<String, Object>) data.get("document");
        Map<String, Object> customer = (Map<String, Object>) data.get("customer");
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        StringBuilder html = new StringBuilder(8192);
        if (!pdfMode) {
            html.append("<!doctype html>");
        }
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"utf-8\"/><style>");
        appendStyle(html, pageWidth, pageHeight, pdfMode);
        html.append("</style></head><body>");
        html.append("<h1>销售单</h1><table class=\"meta\">");
        row(html, "单号", value(document, "no"), "客户", value(customer, "name"));
        row(html, "日期", value(document, "outTime"), "部门", value(document, "deptName"));
        row(html, "收货人", value(document, "receiverName"), "电话", value(document, "receiverPhone"));
        row(html, "地址/备注", first(value(document, "customerAddress"), value(document, "remark")), "业务员", value(document, "saleUserName"));
        html.append("</table><table><thead><tr>");
        String[] headers = {"序号", "编码", "名称", "规格", "单位", "数量", "单价", "金额", "备注"};
        for (String header : headers) {
            html.append("<th>").append(escape(header)).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                html.append("<tr>");
                cell(html, i + 1, "center");
                cell(html, itemValue(item, "productCode"), "");
                cell(html, itemValue(item, "productName"), "");
                cell(html, first(itemValue(item, "standard"), itemValue(item, "vehicleModel")), "");
                cell(html, itemValue(item, "productUnitName"), "center");
                cell(html, itemValue(item, "count"), "right");
                cell(html, itemValue(item, "productPrice"), "right");
                cell(html, first(itemValue(item, "totalProductPrice"), itemValue(item, "totalPrice")), "right");
                cell(html, itemValue(item, "remark"), "");
                html.append("</tr>");
            }
        }
        html.append("</tbody></table><table class=\"summary\">");
        row(html, "合计数量", value(document, "totalCount"), "合计金额", first(value(document, "totalAmount"), value(document, "totalPrice")));
        row(html, "大写金额", first(value(document, "totalAmountUpper"), value(document, "totalPriceUpper")), "开单员", value(document, "billerName"));
        html.append("</table></body></html>");
        return html.toString();
    }

    private void appendStyle(StringBuilder html, int pageWidth, Integer pageHeight, boolean pdfMode) {
        if (!pdfMode) {
            html.append("@page{size:210mm auto;margin:6mm 8mm;}");
            html.append("body{font-family:SimSun,'Microsoft YaHei',sans-serif;font-size:12px;color:#000;}");
            html.append("h1{font-size:18px;text-align:center;margin:0 0 8px;}");
            html.append("table{width:100%;border-collapse:collapse;}");
            html.append("td,th{border:1px solid #000;padding:3px 4px;line-height:1.35;}");
            html.append(".meta td{border:0;padding:2px 4px}.right{text-align:right}.center{text-align:center}.summary{margin-top:6px}");
            return;
        }
        html.append("@page{size:").append(pageWidth).append("mm ");
        html.append(pageHeight != null && pageHeight > 0 ? pageHeight + "mm" : "auto");
        html.append(";margin:4mm 6mm;}");
        html.append("body{font-family:");
        html.append(PDF_FONT_FAMILY).append(",");
        html.append("SimSun,'Microsoft YaHei',sans-serif;font-size:10px;color:#000;margin:0;}");
        html.append("h1{font-size:16px;text-align:center;margin:0 0 5px;}");
        html.append("table{width:100%;border-collapse:collapse;table-layout:fixed;}");
        html.append("td,th{border:1px solid #000;padding:2px 3px;line-height:1.25;word-wrap:break-word;}");
        html.append("th{font-weight:700}.meta td{border:0;padding:1px 3px}.right{text-align:right}.center{text-align:center}.summary{margin-top:4px}");
    }

    private void registerChineseFont(PdfRendererBuilder builder) {
        for (String path : CHINESE_FONT_PATHS) {
            File fontFile = new File(path);
            if (fontFile.isFile()) {
                builder.useFont(fontFile, PDF_FONT_FAMILY);
                return;
            }
        }
    }

    private void row(StringBuilder html, String label1, Object value1, String label2, Object value2) {
        html.append("<tr><td>").append(escape(label1)).append("：</td><td>").append(escape(value1))
                .append("</td><td>").append(escape(label2)).append("：</td><td>").append(escape(value2)).append("</td></tr>");
    }

    private void cell(StringBuilder html, Object value, String className) {
        html.append("<td");
        if (className != null && !className.isEmpty()) {
            html.append(" class=\"").append(className).append("\"");
        }
        html.append(">").append(escape(value)).append("</td>");
    }

    private Object value(Map<String, Object> map, String key) {
        return map == null ? null : map.get(key);
    }

    private Object itemValue(Map<String, Object> item, String key) {
        Object prefixedValue = value(item, "items." + key);
        return prefixedValue != null && !"".equals(prefixedValue) ? prefixedValue : value(item, key);
    }

    private Object first(Object... values) {
        for (Object value : values) {
            if (value != null && !"".equals(value)) {
                return value;
            }
        }
        return "";
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return HtmlUtils.htmlEscape(String.valueOf(value));
    }

    private static class RenderBounds {
        private final int minRow;
        private final int minColumnIndex;
        private final int maxColumnIndex;

        private RenderBounds(int minRow, int minColumnIndex, int maxColumnIndex) {
            this.minRow = minRow;
            this.minColumnIndex = minColumnIndex;
            this.maxColumnIndex = maxColumnIndex;
        }
    }

    private static class DetailBlock {
        private int start;
        private int end;

        private DetailBlock(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class CellRowRange {
        private final int start;
        private final int end;

        private CellRowRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class PageBuildResult {
        private final List<RenderRow> rows;
        private final int nextItemIndex;
        private final boolean bottomPlaced;

        private PageBuildResult(List<RenderRow> rows, int nextItemIndex, boolean bottomPlaced) {
            this.rows = rows;
            this.nextItemIndex = nextItemIndex;
            this.bottomPlaced = bottomPlaced;
        }
    }

    private static class RenderRow {
        private final int sourceRow;
        private final int itemIndex;
        private final boolean detail;

        private RenderRow(int sourceRow, int itemIndex, boolean detail) {
            this.sourceRow = sourceRow;
            this.itemIndex = itemIndex;
            this.detail = detail;
        }
    }

    private static class CellParts {
        private final String column;
        private final int row;

        private CellParts(String column, int row) {
            this.column = column;
            this.row = row;
        }
    }

}
