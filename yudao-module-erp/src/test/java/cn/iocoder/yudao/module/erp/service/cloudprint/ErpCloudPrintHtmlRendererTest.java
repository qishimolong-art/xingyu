package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.DeviceInfo;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.SwResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpCloudPrintHtmlRendererTest {

    private final ErpCloudPrintHtmlRenderer renderer = new ErpCloudPrintHtmlRenderer();

    @Test
    void senderAliases_shouldRenderBothOldAndNewBindings() {
        Map<String, Object> data = Map.of("main", Map.of(
                "document.senderName", "张三、李四", "document.checkerName", "张三、李四"));
        for (String code : List.of("document.senderName", "document.checkerName")) {
            Map<String, Object> cell = Map.of("name", "发货人", "code", code,
                    "html", "<span>${发货人}</span>",
                    "placeholderCodes", new HashMap<>(Map.of("发货人", "document.senderName")));
            String html = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    renderer, "resolveCellHtml", cell, data, 0);
            assertTrue(html.contains("张三、李四"));
            assertFalse(html.contains("${"));
        }
    }

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
    void resolveCellHtml_shouldFormatAmountFieldsWithTwoDecimals() {
        Map<String, Object> data = buildPrefixedItemPrintData();

        String productPrice = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "resolveCellHtml",
                Map.of("code", "items.productPrice", "html", ""), data, 0);
        String totalPrice = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "resolveCellHtml",
                Map.of("code", "items.totalPrice", "html", ""), data, 0);
        String count = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "resolveCellHtml",
                Map.of("code", "items.count", "html", ""), data, 0);
        Map<String, Object> main = new HashMap<>();
        main.put("document.totalFreight1", "12");
        main.put("document.extraFee", "2.5");
        main.put("document.grossProfitRate", "13");
        data.put("main", main);
        String freight = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "resolveCellHtml",
                Map.of("code", "document.totalFreight1", "html", ""), data, 0);
        String extraFee = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "resolveCellHtml",
                Map.of("code", "document.extraFee", "html", ""), data, 0);
        String grossProfitRate = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "resolveCellHtml",
                Map.of("code", "document.grossProfitRate", "html", ""), data, 0);

        assertEquals("123.40", productPrice);
        assertEquals("246.80", totalPrice);
        assertEquals("2", count);
        assertEquals("12.00", freight);
        assertEquals("2.50", extraFee);
        assertEquals("13", grossProfitRate);
    }

    @Test
    void renderSaleOutTemplatePdf_shouldGeneratePdfFromTemplateJson() {
        byte[] pdf = renderer.renderSaleOutTemplatePdf(buildTemplateJson(), buildPaperConfigJson(),
                buildPrefixedItemPrintData(), 241, 140);

        assertTrue(pdf.length > 1024);
        assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), copyOf(pdf, 4));
    }

    @Test
    void cellStyle_shouldMapOnlyWhitelistedTemplateFonts() {
        Map<String, String> expectedAliases = Map.of(
                "simsun", "CloudPrintSimSun",
                "simhei", "CloudPrintSimHei",
                "microsoft_yahei", "CloudPrintMicrosoftYaHei",
                "kaiti", "CloudPrintKaiTi",
                "fangsong", "CloudPrintFangSong");

        expectedAliases.forEach((fontFamily, alias) -> {
            String style = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    renderer, "cellStyle", Map.of("fontFamily", fontFamily));
            assertTrue(style.contains("font-family:'" + alias + "','CloudPrintCjk';"));
        });
        String invalidStyle = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "cellStyle", Map.of("fontFamily", "url(https://invalid.example/font.ttf)"));
        assertFalse(invalidStyle.contains("font-family"));
    }

    @Test
    void fontRegistry_shouldExposeAllTemplateFontsAndRejectUnknownKey() {
        assertEquals(List.of("simsun", "simhei", "microsoft_yahei", "kaiti", "fangsong"),
                ErpCloudPrintFontRegistry.templateKeys());
        assertTrue(ErpCloudPrintFontRegistry.read(
                ErpCloudPrintFontRegistry.resolveFontFile("simhei", 700)).length > 1024);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ErpCloudPrintFontRegistry.resolveFontFile("unknown", 400));
    }

    @Test
    void bundledFontResources_shouldMatchPinnedHashesAndIncludeLicenses() throws IOException {
        Map<String, String> hashes = Map.ofEntries(
                Map.entry("LXGWWenKai-Light.ttf", "526ec70cbb0118e871d481f8179e03ff045f0e4d72d080dcca87950c4ab27cca"),
                Map.entry("LXGWWenKai-Regular.ttf", "39ad71264b588165b469e35e6afb162a378dacd1f95348160240ba9038ac3009"),
                Map.entry("LXGWWenKaiGB-Medium.ttf", "b885c51ec0d3f325974013801dfcefda1a9ba0bf385c607cf5f2582dafa2e5ab"),
                Map.entry("LXGWWenKaiGB-Regular.ttf", "295568c131648062107543aa159c97dd49564be791136c2abf74cad83eba3f7f"),
                Map.entry("NotoSansSC-Bold.ttf", "9db05b8ad1cdd8f3408c994e2bd0b89d75e59cce9e731dc1d80a9bd6a8e71a66"),
                Map.entry("NotoSansSC-Regular.ttf", "f073bb2c48e94be777e0ff18fabfb4e37e747df7394b294cb3206ae31edc1c5d"),
                Map.entry("NotoSansSC-Medium.ttf", "73bf4d2027a3e58f35509d5d1fda30738176618aa79cc83c4e0c8d2b09894178"),
                Map.entry("NotoSerifSC-Bold.ttf", "7757703c59eae2ab6b40b857d3747b4b48b9b5ca435e20dcb21651cdcbce9acb"),
                Map.entry("NotoSerifSC-Regular.ttf", "43eecf78d8453095c2afacc7adee52fdb44c9082f6b64124be7dfedde48f855a"));
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            try (InputStream input = ErpCloudPrintHtmlRendererTest.class.getResourceAsStream(
                    "/fonts/cloud-print/" + entry.getKey())) {
                assertTrue(input != null, "字体资源缺失：" + entry.getKey());
                assertEquals(entry.getValue(), cn.hutool.crypto.digest.DigestUtil.sha256Hex(input));
            }
        }
        for (String license : List.of("OFL-LXGWWenKai.txt", "OFL-LXGWWenKaiGB.txt",
                "OFL-NotoSansSC.txt", "OFL-NotoSerifSC.txt")) {
            assertTrue(ErpCloudPrintHtmlRendererTest.class.getResourceAsStream(
                    "/fonts/cloud-print/licenses/" + license) != null, "许可证缺失：" + license);
        }
        assertTrue(ErpCloudPrintHtmlRendererTest.class.getResourceAsStream(
                "/fonts/cloud-print/THIRD-PARTY-FONTS.md") != null);
    }

    @Test
    void renderSaleOutTemplatePdf_shouldEmbedFiveFontsWithoutHashCharacters() throws IOException {
        for (String fontFamily : ErpCloudPrintFontRegistry.templateKeys()) {
            byte[] pdf = renderer.renderSaleOutTemplatePdf(buildFontTemplateJson(fontFamily),
                    buildPaperConfigJson(), buildPrintData(), 241, 140);
            try (PDDocument document = PDDocument.load(pdf)) {
                String text = new PDFTextStripper().getText(document);
                assertTrue(text.contains("中文数字123符号×龘"), fontFamily + " 未正确提取中文");
                assertFalse(text.contains("#"), fontFamily + " 出现井号替换字符");
                boolean embeddedWithToUnicode = false;
                for (PDPage page : document.getPages()) {
                    for (org.apache.pdfbox.cos.COSName name : page.getResources().getFontNames()) {
                        PDFont font = page.getResources().getFont(name);
                        embeddedWithToUnicode |= font.isEmbedded()
                                && font.getCOSObject().containsKey(org.apache.pdfbox.cos.COSName.TO_UNICODE);
                    }
                }
                assertTrue(embeddedWithToUnicode, fontFamily + " 未嵌入字体或缺少 ToUnicode");
            }
        }
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
    void versionTwoTemplate_shouldRepeatOnlyExplicitMultiRowBody() {
        Map<String, Object> template = buildVersionTwoTemplate();
        Map<String, Object> data = buildManyItemsPrintData(2, "显式明细");

        String html = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "buildTemplateHtml", template, data, versionTwoPaper());

        assertEquals(1, occurrenceCount(html, "组件外备注"));
        assertEquals(2, occurrenceCount(html, "每条第二行"));
        assertEquals(1, occurrenceCount(html, "显式明细1"));
        assertEquals(1, occurrenceCount(html, "显式明细2"));
        assertEquals(1, occurrenceCount(html, "表尾唯一"));
    }

    @Test
    void versionTwoTemplate_shouldOmitBodyWhenItemsAreEmpty() {
        Map<String, Object> template = buildVersionTwoTemplate();
        Map<String, Object> data = buildPrintData();
        data.put("items", Collections.emptyList());

        String html = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "buildTemplateHtml", template, data, versionTwoPaper());

        assertEquals(1, occurrenceCount(html, "产品名称表头"));
        assertEquals(0, occurrenceCount(html, "每条第二行"));
        assertEquals(1, occurrenceCount(html, "表尾唯一"));
    }

    @Test
    void versionTwoTemplate_shouldRenderCellsOutsideComponentOnceOnSameBodyRow() {
        Map<String, Object> template = buildPartialWidthVersionTwoTemplate();

        String html = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "buildTemplateHtml", template,
                buildManyItemsPrintData(2, "局部组件明细"), versionTwoPaper());

        assertEquals(1, occurrenceCount(html, "循环行外侧备注"));
        assertEquals(1, occurrenceCount(html, "局部组件明细1"));
        assertEquals(1, occurrenceCount(html, "局部组件明细2"));

        Map<String, Object> emptyData = buildPrintData();
        emptyData.put("items", Collections.emptyList());
        String emptyHtml = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "buildTemplateHtml", template, emptyData, versionTwoPaper());
        assertEquals(1, occurrenceCount(emptyHtml, "循环行外侧备注"));
        assertEquals(0, occurrenceCount(emptyHtml, "${产品名称}"));
    }

    @Test
    void versionTwoTemplatePdf_shouldRepeatHeaderAndRenderFooterOnce() throws IOException {
        byte[] pdf = renderer.renderSaleOutTemplatePdf(JsonUtils.toJsonString(buildVersionTwoTemplate()),
                JsonUtils.toJsonString(versionTwoPaper()),
                buildManyItemsPrintData(80, "版本二分页明细"), 120, 60);

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(document.getNumberOfPages() > 1);
            assertEquals(document.getNumberOfPages(), occurrenceCount(text, "产品名称表头"));
            assertEquals(1, occurrenceCount(text, "表尾唯一"));
            assertEquals(1, occurrenceCount(text, "组件外备注"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void versionTwoTemplate_shouldRenderPageContextForEveryPage() {
        Map<String, Object> template = buildVersionTwoTemplate();
        List<Map<String, Object>> cells = new ArrayList<>((List<Map<String, Object>>) template.get("cells"));
        cells.set(1, cell("A2", 2, 1,
                "第${当前页}页/共${总页数}页·${页码}", "", Map.of(
                        "当前页", "print.pageNo",
                        "总页数", "print.pageCount",
                        "页码", "print.pageText"), "right", "", "table-border"));
        template.put("cells", cells);

        String html = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "buildTemplateHtml", template,
                buildManyItemsPrintData(80, "页码测试明细"), versionTwoPaper());

        int pageCount = occurrenceCount(html, "<div class=\"print-page\">");
        assertTrue(pageCount > 1);
        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            String pageText = "第" + pageNo + "页/共" + pageCount + "页";
            assertTrue(html.contains(pageText + "·" + pageText));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void versionTwoTemplate_shouldKeepDocumentHeaderOnFirstPageAndPageContextOnContinuationPages() {
        Map<String, Object> template = buildVersionTwoTemplate();
        Map<String, Object> detailTable = new LinkedHashMap<>((Map<String, Object>) template.get("detailTable"));
        detailTable.put("headerRows", Map.of("start", 1, "end", 2));
        template.put("detailTable", detailTable);
        List<Map<String, Object>> cells = new ArrayList<>((List<Map<String, Object>>) template.get("cells"));
        cells.set(0, cell("A1", 1, 1, "仅第一页单据标题", "", Collections.emptyMap(), "left", "", ""));
        cells.add(1, cell("B1", 1, 1, "${页码}", "print.pageText",
                Map.of("页码", "print.pageText"), "right", "", ""));
        template.put("cells", cells);

        String html = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                renderer, "buildTemplateHtml", template,
                buildManyItemsPrintData(80, "续页抬头测试明细"), versionTwoPaper());

        int pageCount = occurrenceCount(html, "<div class=\"print-page\">");
        assertTrue(pageCount > 1);
        assertEquals(1, occurrenceCount(html, "仅第一页单据标题"));
        assertEquals(pageCount, occurrenceCount(html, "产品名称表头"));
        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            assertTrue(html.contains("第" + pageNo + "页/共" + pageCount + "页"));
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

    private Map<String, Object> buildVersionTwoTemplate() {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("version", 2);
        template.put("columns", List.of("A", "B"));
        template.put("rowCount", 5);
        template.put("columnWidths", Map.of("A", "60px", "B", "150px"));
        template.put("rowHeights", Map.of(
                "1", "28px", "2", "28px", "3", "28px", "4", "28px", "5", "28px"));
        template.put("paper", versionTwoPaper());
        template.put("detailTable", Map.of(
                "id", "detail-table",
                "dataSource", "items",
                "startColumn", "A",
                "endColumn", "B",
                "headerRows", Map.of("start", 2, "end", 2),
                "bodyRows", Map.of("start", 3, "end", 4),
                "footerRows", Map.of("start", 5, "end", 5),
                "repeatHeader", true));
        template.put("cells", List.of(
                cell("A1", 2, 1, "组件外备注", "", Collections.emptyMap(), "left", "", ""),
                cell("A2", 2, 1, "产品名称表头", "", Collections.emptyMap(), "center", "", "table-border"),
                cell("A3", 2, 1, "${产品名称}", "items.productName",
                        Map.of("产品名称", "items.productName"), "left", "", "table-border"),
                cell("A4", 2, 1, "每条第二行", "", Collections.emptyMap(), "left", "", "table-border"),
                cell("A5", 2, 1, "表尾唯一", "", Collections.emptyMap(), "left", "", "table-border")));
        return template;
    }

    private Map<String, Object> buildPartialWidthVersionTwoTemplate() {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("version", 2);
        template.put("columns", List.of("A", "B", "C"));
        template.put("rowCount", 4);
        template.put("columnWidths", Map.of("A", "60px", "B", "100px", "C", "100px"));
        template.put("rowHeights", Map.of("1", "28px", "2", "28px", "3", "28px", "4", "28px"));
        template.put("paper", versionTwoPaper());
        template.put("detailTable", Map.of(
                "id", "detail-table",
                "dataSource", "items",
                "startColumn", "A",
                "endColumn", "B",
                "headerRows", Map.of("start", 2, "end", 2),
                "bodyRows", Map.of("start", 3, "end", 3),
                "footerRows", Map.of("start", 4, "end", 4),
                "repeatHeader", true));
        template.put("cells", List.of(
                cell("A1", 3, 1, "局部组件", "", Collections.emptyMap(), "left", ""),
                cell("A2", 2, 1, "产品名称表头", "", Collections.emptyMap(), "center", "", "table-border"),
                cell("A3", 2, 1, "${产品名称}", "items.productName",
                        Map.of("产品名称", "items.productName"), "left", "", "table-border"),
                cell("C3", 1, 1, "循环行外侧备注", "", Collections.emptyMap(), "left", ""),
                cell("A4", 2, 1, "表尾唯一", "", Collections.emptyMap(), "left", "", "table-border")));
        return template;
    }

    private Map<String, Object> versionTwoPaper() {
        return Map.of(
                "size", "custom",
                "width", 120,
                "height", 60,
                "orientation", "portrait",
                "margins", Map.of("top", 4, "right", 4, "bottom", 4, "left", 4));
    }

    private int occurrenceCount(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
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
                cellWithFont("A1", 4, 1, "销售单 ${document.no}", "", Map.of("document.no", "document.no"),
                        "center", "16px", "kaiti", "bold"),
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

    private String buildFontTemplateJson(String fontFamily) {
        Map<String, Object> template = new HashMap<>();
        template.put("version", 1);
        template.put("columns", List.of("A"));
        template.put("rowCount", 2);
        template.put("columnWidths", Map.of("A", "300px"));
        template.put("rowHeights", Map.of("1", "34px", "2", "34px"));
        template.put("paper", Map.of("width", 241, "height", 140, "orientation", "portrait",
                "margins", Map.of("top", 4, "right", 6, "bottom", 4, "left", 6)));
        template.put("cells", List.of(
                cellWithFont("A1", 1, 1, "中文数字123符号&times;龘", "", Collections.emptyMap(),
                        "left", "12px", fontFamily),
                cellWithFont("A2", 1, 1, "中文数字123符号&times;龘", "", Collections.emptyMap(),
                        "left", "12px", fontFamily, "bold")));
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

    private Map<String, Object> cellWithFont(String cell, int colspan, int rowspan, String html, String code,
                                             Map<String, String> placeholderCodes, String textAlign, String fontSize,
                                             String fontFamily, String... classes) {
        Map<String, Object> result = cell(cell, colspan, rowspan, html, code, placeholderCodes,
                textAlign, fontSize, classes);
        result.put("fontFamily", fontFamily);
        return result;
    }

    private byte[] copyOf(byte[] bytes, int length) {
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return result;
    }

}
