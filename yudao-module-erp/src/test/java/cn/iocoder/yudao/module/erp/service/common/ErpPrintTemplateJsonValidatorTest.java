package cn.iocoder.yudao.module.erp.service.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErpPrintTemplateJsonValidatorTest {

    @Test
    void validate_shouldKeepVersionOneCompatible() {
        assertDoesNotThrow(() -> ErpPrintTemplateJsonValidator.validate(Map.of(
                "version", 1,
                "cells", List.of(cell("A2", "items.name")))));
    }

    @Test
    void validate_shouldAcceptVersionTwoDetailTable() {
        assertDoesNotThrow(() -> ErpPrintTemplateJsonValidator.validate(validTemplate()));
    }

    @Test
    void validate_shouldRejectMissingBodyAndOverlappingSections() {
        Map<String, Object> missingBody = validTemplate();
        detailTable(missingBody).remove("bodyRows");
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(missingBody));

        Map<String, Object> overlap = validTemplate();
        detailTable(overlap).put("headerRows", Map.of("start", 2, "end", 3));
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(overlap));
    }

    @Test
    void validate_shouldRejectInvalidComponentIdentityAndRepeatHeaderType() {
        Map<String, Object> invalidId = validTemplate();
        detailTable(invalidId).put("id", "another-detail");
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(invalidId));

        Map<String, Object> invalidRepeatHeader = validTemplate();
        detailTable(invalidRepeatHeader).put("repeatHeader", "true");
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(invalidRepeatHeader));
    }

    @Test
    void validate_shouldRejectDetailOutsideBodyAndMainInsideBody() {
        Map<String, Object> detailOutside = validTemplate();
        cells(detailOutside).add(cell("A5", "items.remark"));
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(detailOutside));

        Map<String, Object> mainInside = validTemplate();
        cells(mainInside).add(cell("B3", "document.no"));
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(mainInside));
    }

    @Test
    void validate_shouldRejectMixedPlaceholderOutsideBodyAndCrossBoundaryMerge() {
        Map<String, Object> mixedOutside = validTemplate();
        Map<String, Object> mixed = cell("B5", "");
        mixed.put("html", "备注：${商品名称}");
        mixed.put("placeholderCodes", Map.of("商品名称", "items.productName"));
        cells(mixedOutside).add(mixed);
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(mixedOutside));

        Map<String, Object> crossed = validTemplate();
        Map<String, Object> merged = cell("A2", "");
        merged.put("rowspan", 2);
        cells(crossed).add(merged);
        assertThrows(IllegalArgumentException.class,
                () -> ErpPrintTemplateJsonValidator.validate(crossed));
    }

    @Test
    void validate_shouldAllowVersionTwoWithoutDetailTableWhenNoItemFieldExists() {
        Map<String, Object> template = validTemplate();
        template.remove("detailTable");
        cells(template).removeIf(cell -> String.valueOf(cell.get("code")).startsWith("items."));
        assertDoesNotThrow(() -> ErpPrintTemplateJsonValidator.validate(template));
    }

    private Map<String, Object> validTemplate() {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("version", 2);
        template.put("columns", List.of("A", "B", "C"));
        template.put("rowCount", 5);
        template.put("detailTable", new LinkedHashMap<>(Map.of(
                "id", "detail-table",
                "dataSource", "items",
                "startColumn", "A",
                "endColumn", "C",
                "headerRows", Map.of("start", 2, "end", 2),
                "bodyRows", Map.of("start", 3, "end", 4),
                "footerRows", Map.of("start", 5, "end", 5),
                "repeatHeader", true)));
        template.put("cells", new ArrayList<>(List.of(
                cell("A1", "document.no"),
                cell("A3", "items.productName"),
                cell("A5", "document.totalCount"))));
        return template;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> detailTable(Map<String, Object> template) {
        return (Map<String, Object>) template.get("detailTable");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cells(Map<String, Object> template) {
        return (List<Map<String, Object>>) template.get("cells");
    }

    private Map<String, Object> cell(String cellId, String code) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("cell", cellId);
        cell.put("code", code);
        cell.put("html", code.isEmpty() ? "静态文字" : "${" + code + "}");
        cell.put("placeholderCodes", code.isEmpty() ? Map.of() : Map.of(code, code));
        cell.put("colspan", 1);
        cell.put("rowspan", 1);
        return cell;
    }
}
