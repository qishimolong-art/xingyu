package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.DeviceInfo;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.SwResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpCloudPrintHtmlRendererTest {

    private final ErpCloudPrintHtmlRenderer renderer = new ErpCloudPrintHtmlRenderer();

    @Test
    void renderSaleOutPdf_shouldGeneratePdfBytes() {
        byte[] pdf = renderer.renderSaleOutPdf(buildPrintData(), 241, 140);

        assertTrue(pdf.length > 1024);
        assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), copyOf(pdf, 4));
    }

    @Test
    void renderSaleOut_shouldKeepHtmlFallback() {
        byte[] htmlBytes = renderer.renderSaleOut(buildPrintData());

        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        assertTrue(html.contains("销售单"));
        assertTrue(html.contains("云打印测试客户"));
    }

    @Test
    void renderSaleOut_shouldSupportPrefixedItemFields() {
        byte[] htmlBytes = renderer.renderSaleOut(buildPrefixedItemPrintData());

        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        assertTrue(html.contains("PX001"));
        assertTrue(html.contains("前缀字段配件"));
        assertTrue(html.contains("前缀规格"));
        assertTrue(html.contains("246.8"));
    }

    @Test
    void renderSaleOutTemplatePdf_shouldGeneratePdfFromTemplateJson() {
        byte[] pdf = renderer.renderSaleOutTemplatePdf(buildTemplateJson(), buildPaperConfigJson(),
                buildPrefixedItemPrintData(), 241, 140);

        assertTrue(pdf.length > 1024);
        assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), copyOf(pdf, 4));
    }

    @Test
    void renderSaleOutTemplatePdf_shouldSupportXhtmlEntitiesAndMultiplication() {
        byte[] pdf = renderer.renderSaleOutTemplatePdf(buildEntityTemplateJson(), buildPaperConfigJson(),
                buildManyItemsPrintData(1, "埃斯红9全合成SQ 5W-30 12×1L & A"), 241, 140);

        assertTrue(pdf.length > 1024);
        assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), copyOf(pdf, 4));
    }

    @Test
    void renderSaleOutTemplatePdf_shouldPaginateDetailRowsByPaperHeight() throws IOException {
        byte[] pdf = renderer.renderSaleOutTemplatePdf(buildTemplateJson(), buildPaperConfigJson(),
                buildManyItemsPrintData(80, "多页明细配件"), 241, 140);

        assertTrue(pdf.length > 1024);
        assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), copyOf(pdf, 4));
        try (PDDocument document = PDDocument.load(pdf)) {
            assertTrue(document.getNumberOfPages() > 1);
        }
    }

    @Test
    void isDeviceOnline_shouldSupportSwAiotOnlineState() {
        DeviceInfo online = new DeviceInfo();
        online.setState("online");
        online.setStatus(0);

        DeviceInfo offline = new DeviceInfo();
        offline.setState("offline");
        offline.setStatus(0);

        assertTrue(ErpCloudPrintServiceImpl.isDeviceOnline(online));
        assertFalse(ErpCloudPrintServiceImpl.isDeviceOnline(offline));
    }

    @Test
    void parseDeviceInfo_shouldSupportStringDevType() {
        String responseBody = "{\"code\":200,\"success\":true,\"message\":\"success\","
                + "\"data\":{\"devid\":\"SW_TEST\",\"devType\":\"SW\",\"nickname\":\"成都\","
                + "\"devicename\":\"DEVICE_TEST\",\"state\":\"online\",\"taskcnt\":0,"
                + "\"typename\":\"标拓针打\",\"status\":0},\"time\":\"2026-09-23 08:37:56\"}";

        SwResponse<DeviceInfo> response = JsonUtils.parseObject(responseBody,
                new TypeReference<SwResponse<DeviceInfo>>() {});

        assertEquals(200, response.getCode());
        assertEquals("SW", response.getData().getDevType());
        assertTrue(ErpCloudPrintServiceImpl.isDeviceOnline(response.getData()));
    }

    private Map<String, Object> buildPrintData() {
        Map<String, Object> document = new HashMap<>();
        document.put("no", "XS-TEST-001");
        document.put("outTime", "2026-09-23 10:00:00");
        document.put("deptName", "销售部");
        document.put("receiverName", "张三");
        document.put("receiverPhone", "13800000000");
        document.put("customerAddress", "成都市测试地址");
        document.put("saleUserName", "业务员A");
        document.put("totalCount", new BigDecimal("2.00"));
        document.put("totalAmount", new BigDecimal("246.80"));
        document.put("totalAmountUpper", "贰佰肆拾陆元捌角");
        document.put("billerName", "开单员B");

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "云打印测试客户");

        Map<String, Object> item = new HashMap<>();
        item.put("productCode", "P001");
        item.put("productName", "测试配件");
        item.put("standard", "标准规格");
        item.put("productUnitName", "件");
        item.put("count", new BigDecimal("2.00"));
        item.put("productPrice", new BigDecimal("123.40"));
        item.put("totalProductPrice", new BigDecimal("246.80"));
        item.put("remark", "空字段兼容");

        Map<String, Object> data = new HashMap<>();
        data.put("document", document);
        data.put("customer", customer);
        data.put("items", Collections.singletonList(item));
        return data;
    }

    private Map<String, Object> buildPrefixedItemPrintData() {
        Map<String, Object> data = buildPrintData();
        Map<String, Object> item = new HashMap<>();
        item.put("items.productCode", "PX001");
        item.put("items.productName", "前缀字段配件");
        item.put("items.standard", "前缀规格");
        item.put("items.productUnitName", "件");
        item.put("items.count", "2");
        item.put("items.productPrice", "123.4");
        item.put("items.totalPrice", "246.8");
        item.put("items.remark", "真实打印数据字段");
        data.put("items", Collections.singletonList(item));
        return data;
    }

    private Map<String, Object> buildManyItemsPrintData(int count, String productName) {
        Map<String, Object> data = buildPrintData();
        List<Map<String, Object>> items = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            Map<String, Object> item = new HashMap<>();
            item.put("items.seq", String.valueOf(index));
            item.put("items.productCode", "PX" + String.format("%03d", index));
            item.put("items.productName", productName + index);
            item.put("items.standard", "12×1L");
            item.put("items.productUnitName", "件");
            item.put("items.count", "1");
            item.put("items.totalPrice", "10.00");
            item.put("items.remark", "分页测试");
            items.add(item);
        }
        data.put("items", items);
        return data;
    }

    private String buildTemplateJson() {
        Map<String, Object> template = new HashMap<>();
        template.put("version", 1);
        template.put("columns", java.util.Arrays.asList("A", "B", "C", "D"));
        template.put("rowCount", 6);
        template.put("columnWidths", Map.of("A", "42px", "B", "120px", "C", "72px", "D", "88px"));
        template.put("rowHeights", Map.of("1", "30px", "2", "26px", "3", "26px", "4", "26px", "5", "26px", "6", "26px"));
        template.put("paper", Map.of(
                "size", "continuous_half",
                "width", 241,
                "height", 140,
                "orientation", "portrait",
                "margins", Map.of("top", 4, "right", 6, "bottom", 4, "left", 6)));
        template.put("cells", java.util.Arrays.asList(
                cell("A1", 4, 1, "销售单 ${document.no}", "", Map.of("document.no", "document.no"), "center", "16px", "bold"),
                cell("A2", 1, 1, "客户", "", Collections.emptyMap(), "left", "", "table-border"),
                cell("B2", 3, 1, "${customerName}", "document.customerName", Map.of("customerName", "customer.name"), "left", "", "table-border"),
                cell("A3", 1, 1, "序号", "", Collections.emptyMap(), "center", "", "table-border"),
                cell("B3", 1, 1, "产品名称", "", Collections.emptyMap(), "center", "", "table-border"),
                cell("C3", 1, 1, "数量", "", Collections.emptyMap(), "center", "", "table-border"),
                cell("D3", 1, 1, "金额", "", Collections.emptyMap(), "center", "", "table-border"),
                cell("A4", 1, 1, "${序号}", "items.seq", Map.of("序号", "items.seq"), "center", "", "table-border"),
                cell("B4", 1, 1, "${产品名称}", "items.productName", Map.of("产品名称", "items.productName"), "left", "", "table-border"),
                cell("C4", 1, 1, "${数量}", "items.count", Map.of("数量", "items.count"), "right", "", "table-border"),
                cell("D4", 1, 1, "${金额}", "items.totalPrice", Map.of("金额", "items.totalPrice"), "right", "", "table-border"),
                cell("A5", 2, 1, "合计数量", "", Collections.emptyMap(), "right", "", "table-border"),
                cell("C5", 2, 1, "${合计数量}", "document.totalCount", Map.of("合计数量", "document.totalCount"), "right", "", "table-border")));
        return JsonUtils.toJsonString(template);
    }

    private String buildEntityTemplateJson() {
        Map<String, Object> template = new HashMap<>();
        template.put("version", 1);
        template.put("columns", java.util.Arrays.asList("A", "B"));
        template.put("rowCount", 3);
        template.put("columnWidths", Map.of("A", "90px", "B", "180px"));
        template.put("rowHeights", Map.of("1", "34px", "2", "34px", "3", "34px"));
        template.put("paper", Map.of(
                "size", "continuous_half",
                "width", 241,
                "height", 140,
                "orientation", "portrait",
                "margins", Map.of("top", 4, "right", 6, "bottom", 4, "left", 6)));
        template.put("cells", java.util.Arrays.asList(
                cell("A1", 2, 1, "实体测试&nbsp;12&times;1L&middot;PDF", "", Collections.emptyMap(), "center", "", "bold"),
                cell("A2", 1, 1, "产品", "", Collections.emptyMap(), "left", "", "table-border"),
                cell("B2", 1, 1, "${产品名称}", "items.productName", Map.of("产品名称", "items.productName"), "left", "", "table-border"),
                cell("A3", 1, 1, "规格", "", Collections.emptyMap(), "left", "", "table-border"),
                cell("B3", 1, 1, "${规格}", "items.standard", Map.of("规格", "items.standard"), "left", "", "table-border")));
        return JsonUtils.toJsonString(template);
    }

    private String buildPaperConfigJson() {
        return JsonUtils.toJsonString(Map.of(
                "width", 241,
                "height", 140,
                "orientation", "portrait",
                "margins", Map.of("top", 4, "right", 6, "bottom", 4, "left", 6)));
    }

    private Map<String, Object> cell(String cell, int colspan, int rowspan, String html, String code,
                                     Map<String, String> placeholderCodes, String textAlign, String fontSize,
                                     String... classes) {
        Map<String, Object> result = new HashMap<>();
        result.put("cell", cell);
        result.put("colspan", colspan);
        result.put("rowspan", rowspan);
        result.put("html", html);
        result.put("text", html.replaceAll("<[^>]+>", ""));
        result.put("code", code);
        result.put("placeholderCodes", placeholderCodes);
        result.put("textAlign", textAlign);
        result.put("fontSize", fontSize);
        result.put("classes", java.util.Arrays.asList(classes));
        return result;
    }

    private byte[] copyOf(byte[] bytes, int length) {
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return result;
    }

}
