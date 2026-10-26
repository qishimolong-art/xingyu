package cn.iocoder.yudao.module.erp.controller.admin.common;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpCloudPrintFontRegistry;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpPrintWebFontRegistry;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ErpPrintFontControllerTest {
    private AnnotationConfigApplicationContext context;
    private ErpPrintController controller;
    private PermissionStub permissions;

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        controller = context.getBean(ErpPrintController.class);
        permissions = context.getBean(PermissionStub.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", "", Collections.emptyList()));
        permissions.allowed = Set.of("erp:stock-transfer-out:print");
    }

    @AfterEach
    void close() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    private ServletWebRequest request(String etag) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/erp/print/font");
        if (etag != null) request.addHeader("If-None-Match", etag);
        return new ServletWebRequest(request, new MockHttpServletResponse());
    }

    @Test
    void manifestAndResource_shouldUseMatchingHashesAndConditionalPrivateCache() {
        var manifestResponse = controller.getFontManifest(request(null));
        assertEquals(200, manifestResponse.getStatusCodeValue());
        assertTrue(manifestResponse.getHeaders().getCacheControl().contains("no-cache"));
        assertTrue(manifestResponse.getHeaders().getCacheControl().contains("private"));
        var manifest = manifestResponse.getBody().getData();
        assertEquals(9, manifest.getResources().size());
        assertEquals(304, controller.getFontManifest(request(manifestResponse.getHeaders().getETag())).getStatusCodeValue());
        for (String id : manifest.getResources().keySet()) {
            var response = controller.getWebFont(id, request(null));
            assertEquals("font/woff2", response.getHeaders().getContentType().toString());
            assertTrue(response.getHeaders().getCacheControl().contains("immutable"));
            assertEquals(id, cn.hutool.crypto.digest.DigestUtil.sha256Hex(response.getBody()));
            assertEquals(manifest.getResources().get(id).getSize(), response.getBody().length);
            assertSame(response.getBody(), controller.getWebFont(id, request(null)).getBody());
            var unchanged = controller.getWebFont(id, request("W/\"" + id + "\""));
            assertEquals(304, unchanged.getStatusCodeValue());
            assertNull(unchanged.getBody());
        }
    }

    @Test
    void httpRoutes_shouldReturnManifestAndWoff2AndHonorIfNoneMatch() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        String id = ErpPrintWebFontRegistry.manifest().getFamilies().get("simhei").get("400");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/erp/print/font/manifest"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.version").isNotEmpty());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/erp/print/font/resource/" + id + ".woff2"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("font/woff2"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/erp/print/font/resource/" + id + ".woff2")
                        .header("If-None-Match", "\"" + id + "\""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotModified());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/erp/print/font/resource/unknown.woff2"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void unknownResource_shouldNotReadArbitraryClasspathFiles() {
        for (String id : Arrays.asList("../manifest", "manifest.json", "A".repeat(64), "0".repeat(64))) {
            assertEquals(404, controller.getWebFont(id, request(null)).getStatusCodeValue());
        }
    }

    @Test
    void legacyTtfEndpoint_shouldRetainMappingAndSevenDayCache() {
        var response = controller.getFontFile("simhei", 400);
        assertEquals("font/ttf", response.getHeaders().getContentType().toString());
        assertEquals("max-age=604800, private", response.getHeaders().getCacheControl());
        assertArrayEquals(ErpCloudPrintFontRegistry.read(ErpCloudPrintFontRegistry.resolveFontFile("simhei", 400)), response.getBody());
    }

    @Test
    void readPermissions_shouldNotGrantTemplateMutationAndDeniedUsersCannotRead() {
        String id = ErpPrintWebFontRegistry.manifest().getFamilies().get("simhei").get("400");
        for (String permission : Set.of("erp:purchase-order:print", "erp:sale-out:print", "erp:finance-payment:print", "erp:stock-transfer-out:print")) {
            permissions.allowed = Set.of(permission);
            assertEquals(200, controller.getFontManifest(request(null)).getStatusCodeValue());
            assertEquals(200, controller.getWebFont(id, request(null)).getStatusCodeValue());
            assertThrows(AccessDeniedException.class, () -> controller.setDefaultTemplate(34L));
        }
        permissions.allowed = Set.of("erp:purchase-order:print-template");
        assertEquals(200, controller.getWebFont(id, request(null)).getStatusCodeValue());
        permissions.allowed = Set.of();
        assertThrows(AccessDeniedException.class, () -> controller.getFontManifest(request(null)));
        assertThrows(AccessDeniedException.class, () -> controller.getWebFont(id, request(null)));
        assertThrows(AccessDeniedException.class, () -> controller.getFontFile("simhei", 400));
    }

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    static class Config {
        @Bean ErpPrintController controller() { return new ErpPrintController(); }
        @Bean ErpPrintService printService() { return mock(ErpPrintService.class); }
        @Bean(name = "ss") PermissionStub permissions() { return new PermissionStub(); }
    }

    public static class PermissionStub implements SecurityFrameworkService {
        Set<String> allowed = Set.of();
        public boolean hasPermission(String permission) { return allowed.contains(permission); }
        public boolean hasAnyPermissions(String... values) { return Arrays.stream(values).anyMatch(allowed::contains); }
        public boolean hasRole(String role) { return false; }
        public boolean hasAnyRoles(String... roles) { return false; }
        public boolean hasScope(String scope) { return false; }
        public boolean hasAnyScopes(String... scopes) { return false; }
    }
}
