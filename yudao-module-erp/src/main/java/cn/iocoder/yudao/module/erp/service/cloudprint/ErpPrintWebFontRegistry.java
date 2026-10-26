package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.Data;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 浏览器压缩字体。映射由原 TTF 注册表生成，PDF 仍使用原始字体。 */
public final class ErpPrintWebFontRegistry {
    private static final String ROOT = "/fonts/cloud-print/web/";
    private static final Map<String, byte[]> BYTES = new ConcurrentHashMap<>();

    private ErpPrintWebFontRegistry() {
    }

    private static class Holder {
        private static final Manifest MANIFEST = loadManifest();
    }

    public static Manifest manifest() {
        return Holder.MANIFEST;
    }

    public static WebFont find(String sha256) {
        return sha256 != null && sha256.matches("[a-f0-9]{64}")
                ? manifest().getResources().get(sha256) : null;
    }

    public static byte[] read(WebFont font) {
        if (font == null || find(font.getSha256()) != font) {
            throw new IllegalArgumentException("未知的内置字体资源");
        }
        return BYTES.computeIfAbsent(font.getSha256(), key -> {
            byte[] bytes = readResource(font.getFileName());
            if (bytes.length != font.getSize() || !DigestUtil.sha256Hex(bytes).equals(key)) {
                throw new IllegalStateException("内置压缩字体校验失败：" + font.getFileName());
            }
            return bytes;
        });
    }

    private static byte[] readResource(String name) {
        try (java.io.InputStream input = ErpCloudPrintFontRegistry.open(ROOT + name)) {
            return input.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("读取内置压缩字体失败：" + name, ex);
        }
    }

    private static Manifest loadManifest() {
        Manifest manifest = JsonUtils.parseObject(readResource("manifest.json"), Manifest.class);
        if (manifest == null || manifest.getVersion() == null || manifest.getResources().size() != 9
                || !manifest.getFamilies().keySet().equals(new java.util.HashSet<>(ErpCloudPrintFontRegistry.templateKeys()))) {
            throw new IllegalStateException("内置压缩字体清单不完整");
        }
        manifest.getFamilies().forEach((family, weights) -> weights.forEach((weight, id) -> {
            WebFont font = manifest.getResources().get(id);
            String source = ErpCloudPrintFontRegistry.resolveFontFile(family, Integer.valueOf(weight)).getFileName();
            if (font == null || !source.equals(font.getSource()) || !id.equals(font.getSha256())
                    || !font.getFileName().equals(source.replace(".ttf", ".woff2"))
                    || !font.getUrl().equals("/erp/print/font/resource/" + id + ".woff2")) {
                throw new IllegalStateException("压缩字体与原字体注册表不匹配：" + family);
            }
        }));
        if (!manifest.getFallback().equals(manifest.getFamilies().get("microsoft_yahei"))) {
            throw new IllegalStateException("内置压缩字体备用映射不匹配");
        }
        return manifest;
    }

    @Data
    public static class Manifest {
        private String version;
        private Map<String, String> tools;
        private Map<String, Map<String, String>> families;
        private Map<String, String> fallback;
        private Map<String, WebFont> resources;
    }

    @Data
    public static class WebFont {
        private String sha256;
        private String fileName;
        private long size;
        private String source;
        private String sourceSha256;
        private String url;
    }
}
