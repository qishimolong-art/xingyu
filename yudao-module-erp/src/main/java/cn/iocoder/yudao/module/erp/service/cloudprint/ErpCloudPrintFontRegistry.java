package cn.iocoder.yudao.module.erp.service.cloudprint;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 云打印内置字体注册表，浏览器字体接口和 PDF 渲染共用同一份资源。 */
public final class ErpCloudPrintFontRegistry {

    public static final String DEFAULT_FAMILY = "CloudPrintCjk";
    private static final String ROOT = "/fonts/cloud-print/";
    private static final Map<String, FontFamily> FAMILIES = buildFamilies();

    private ErpCloudPrintFontRegistry() {
    }

    public static void registerPdfFonts(PdfRendererBuilder builder) {
        register(builder, DEFAULT_FAMILY, resource("NotoSansSC-Regular.ttf"), 400);
        register(builder, DEFAULT_FAMILY, resource("NotoSansSC-Bold.ttf"), 700);
        for (FontFamily family : FAMILIES.values()) {
            register(builder, family.getPdfFamily(), family.getRegularResource(), 400);
            register(builder, family.getPdfFamily(), family.getBoldResource(), 700);
        }
    }

    public static FontFile resolveFontFile(String templateKey, Integer requestedWeight) {
        FontFamily family = FAMILIES.get(templateKey);
        if (family == null) {
            throw new IllegalArgumentException("不支持的打印字体：" + templateKey);
        }
        int weight = requestedWeight != null && requestedWeight >= 600 ? 700 : 400;
        String resource = weight == 700 ? family.getBoldResource() : family.getRegularResource();
        return new FontFile(templateKey, family.getPdfFamily(), weight, fileName(resource), resource);
    }

    public static String pdfFamily(String templateKey) {
        FontFamily family = FAMILIES.get(templateKey);
        return family == null ? DEFAULT_FAMILY : family.getPdfFamily();
    }

    public static boolean supports(String templateKey) {
        return FAMILIES.containsKey(templateKey);
    }

    public static List<String> templateKeys() {
        return Collections.unmodifiableList(new ArrayList<>(FAMILIES.keySet()));
    }

    public static InputStream open(String resource) {
        InputStream inputStream = ErpCloudPrintFontRegistry.class.getResourceAsStream(resource);
        if (inputStream == null) {
            throw new IllegalStateException("内置打印字体资源缺失：" + resource);
        }
        return inputStream;
    }

    public static byte[] read(FontFile fontFile) {
        try (InputStream inputStream = open(fontFile.getResource())) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("读取内置打印字体失败：" + fontFile.getFileName(), ex);
        }
    }

    private static void register(PdfRendererBuilder builder, String family, String resource, int weight) {
        builder.useFont(() -> open(resource), family, weight, FontStyle.NORMAL, true);
    }

    private static String resource(String fileName) {
        return ROOT + fileName;
    }

    private static String fileName(String resource) {
        return resource.substring(resource.lastIndexOf('/') + 1);
    }

    private static Map<String, FontFamily> buildFamilies() {
        Map<String, FontFamily> families = new LinkedHashMap<>();
        families.put("simsun", new FontFamily("CloudPrintSimSun",
                resource("NotoSerifSC-Regular.ttf"), resource("NotoSerifSC-Bold.ttf")));
        families.put("simhei", new FontFamily("CloudPrintSimHei",
                resource("NotoSansSC-Medium.ttf"), resource("NotoSansSC-Bold.ttf")));
        families.put("microsoft_yahei", new FontFamily("CloudPrintMicrosoftYaHei",
                resource("NotoSansSC-Regular.ttf"), resource("NotoSansSC-Medium.ttf")));
        families.put("kaiti", new FontFamily("CloudPrintKaiTi",
                resource("LXGWWenKaiGB-Regular.ttf"), resource("LXGWWenKaiGB-Medium.ttf")));
        families.put("fangsong", new FontFamily("CloudPrintFangSong",
                resource("LXGWWenKai-Light.ttf"), resource("LXGWWenKai-Regular.ttf")));
        return Collections.unmodifiableMap(families);
    }

    @Getter
    @AllArgsConstructor
    private static class FontFamily {
        private final String pdfFamily;
        private final String regularResource;
        private final String boldResource;
    }

    @Getter
    @AllArgsConstructor
    public static class FontFile {
        private final String templateKey;
        private final String pdfFamily;
        private final int weight;
        private final String fileName;
        private final String resource;
    }
}
