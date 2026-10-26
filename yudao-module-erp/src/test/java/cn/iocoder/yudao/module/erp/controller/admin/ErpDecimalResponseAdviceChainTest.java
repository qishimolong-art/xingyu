package cn.iocoder.yudao.module.erp.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.web.core.handler.GlobalResponseBodyHandler;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.erp.framework.web.StockDecimalResponseAdvice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uses Spring MVC's real advice discovery/sorting and JSON conversion, without a database. */
class ErpDecimalResponseAdviceChainTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"/erp/receivable-account/page", "/erp/payable-account/page", "/erp/stock-in/get"})
    void decimalResponsesKeepPrecisionAndOriginalAccessLogRegardlessOfRegistrationOrder(String path) throws Exception {
        for (boolean decimalFirst : new boolean[]{true, false}) {
            MvcResult result = mvc(decimalFirst).perform(get(path).queryParam("decimalMode", "string"))
                    .andExpect(status().isOk()).andReturn();
            JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
            JsonNode row = body.path("data").path("list").get(0);

            assertEquals(0, body.path("code").intValue());
            assertEquals(1, body.path("data").path("total").longValue());
            assertEquals("999999999999999999.123456", row.path("amount").textValue());
            assertEquals("-5.055556", row.path("adjustments").get(0).textValue());
            assertEquals("0.000001", row.path("adjustments").get(1).path("balance").textValue());
            assertTrue(row.path("id").isIntegralNumber());
            assertTrue(row.path("status").isIntegralNumber());
            assertTrue(row.path("enabled").isBoolean());
            assertEquals("****", row.path("maskedPrice").textValue());
            assertTrue(row.path("empty").isNull());

            CommonResult<?> logged = WebFrameworkUtils.getCommonResult(result.getRequest());
            assertNotNull(logged, "Access log must retain the original CommonResult before JSON conversion");
            PageResult<?> page = (PageResult<?>) logged.getData();
            assertEquals(new BigDecimal("999999999999999999.123456"),
                    ((Map<?, ?>) page.getList().get(0)).get("amount"));
        }
    }

    @Test
    void defaultAndUnknownDecimalModesKeepNumericJson() throws Exception {
        for (String query : new String[]{"", "?decimalMode=number"}) {
            MvcResult result = mvc(true).perform(get("/erp/receivable-account/page" + query))
                    .andExpect(status().isOk()).andReturn();
            JsonNode row = mapper.readTree(result.getResponse().getContentAsString()).path("data").path("list").get(0);
            assertTrue(row.path("amount").isNumber());
            assertNotNull(WebFrameworkUtils.getCommonResult(result.getRequest()));
        }
    }

    @Test
    void errorResponsesKeepCodeMessageAndAccessLog() throws Exception {
        MvcResult result = mvc(true).perform(get("/erp/decimal-test/error").queryParam("decimalMode", "string"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1001, body.path("code").intValue());
        assertEquals("validation failed", body.path("msg").textValue());
        assertTrue(body.path("data").isNull());
        assertEquals(1001, WebFrameworkUtils.getCommonResult(result.getRequest()).getCode());
    }

    private MockMvc mvc(boolean decimalFirst) {
        Object decimal = new StockDecimalResponseAdvice(mapper);
        Object global = new GlobalResponseBodyHandler();
        return MockMvcBuilders.standaloneSetup(new FixtureController())
                .setControllerAdvice(decimalFirst ? new Object[]{decimal, global} : new Object[]{global, decimal})
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
    }

    @RestController
    public static class FixtureController {

        @GetMapping({"/erp/receivable-account/page", "/erp/payable-account/page", "/erp/stock-in/get"})
        public CommonResult<PageResult<Map<String, Object>>> page() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", 390L);
            row.put("status", 1);
            row.put("enabled", true);
            row.put("amount", new BigDecimal("999999999999999999.123456"));
            row.put("adjustments", Arrays.asList(new BigDecimal("-5.055556"),
                    Collections.singletonMap("balance", new BigDecimal("0.000001"))));
            row.put("maskedPrice", "****");
            row.put("empty", null);
            return CommonResult.success(new PageResult<>(Collections.singletonList(row), 1L));
        }

        @GetMapping("/erp/decimal-test/error")
        public CommonResult<Object> error() {
            return CommonResult.error(1001, "validation failed");
        }
    }
}
