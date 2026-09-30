package cn.iocoder.yudao.module.erp.controller.app.cloudprint;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.config.SwPrintProperties;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpCloudPrintCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErpCloudPrintCallbackControllerTest {

    private MockMvc mockMvc;
    private ErpCloudPrintCallbackService callbackService;

    @BeforeEach
    void setUp() {
        callbackService = mock(ErpCloudPrintCallbackService.class);
        ErpCloudPrintCallbackController controller = new ErpCloudPrintCallbackController();
        ReflectionTestUtils.setField(controller, "callbackService", callbackService);
        SwPrintProperties properties = new SwPrintProperties();
        properties.setCallbackPathToken("callback-token");
        WebProperties webProperties = new WebProperties();
        webProperties.getAppApi().setPrefix("");
        ErpCloudPrintCallbackFilter filter = new ErpCloudPrintCallbackFilter();
        ReflectionTestUtils.setField(filter, "properties", properties);
        ReflectionTestUtils.setField(filter, "webProperties", webProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).addFilters(filter).build();
    }

    @Test
    void validCallback_shouldReturnVendorOkJson() throws Exception {
        String body = "{\"method\":\"devStatus\",\"devid\":\"SW1\",\"code\":0}";

        mockMvc.perform(post("/erp/print/callback/callback-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(callbackService).receiveCallback("callback-token", body);
    }

    @Test
    void doubleEncodedCallback_shouldKeepRawBodyAndReturnVendorOkJson() throws Exception {
        String body = "\"{\\\"method\\\":\\\"devStatus\\\",\\\"devid\\\":\\\"SW1\\\",\\\"code\\\":101}\"";

        mockMvc.perform(post("/erp/print/callback/callback-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"));

        verify(callbackService).receiveCallback("callback-token", body);
    }

    @Test
    void invalidToken_shouldReturn404() throws Exception {
        mockMvc.perform(post("/erp/print/callback/wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingToken_shouldReturn404() throws Exception {
        mockMvc.perform(post("/erp/print/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidTokenWithWrongMethod_shouldStillReturn404() throws Exception {
        mockMvc.perform(get("/erp/print/callback/wrong"))
                .andExpect(status().isNotFound());
    }

    @Test
    void oversizedBody_shouldReturn413BeforeController() throws Exception {
        String body = repeat('x', ErpCloudPrintCallbackConstants.MAX_BODY_BYTES + 1);

        mockMvc.perform(post("/erp/print/callback/callback-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void persistFailure_shouldReturn500() throws Exception {
        doThrow(new IllegalStateException("db down")).when(callbackService)
                .receiveCallback("callback-token", "{}");

        mockMvc.perform(post("/erp/print/callback/callback-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("ERROR"));
    }

    @Test
    void wrongMethod_shouldReturn405() throws Exception {
        mockMvc.perform(get("/erp/print/callback/callback-token"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void wrongContentType_shouldReturn415() throws Exception {
        mockMvc.perform(post("/erp/print/callback/callback-token")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

}
