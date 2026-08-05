package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigUpdateReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpStockSelectPriceConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErpStockSelectPriceConfigControllerTest {

    private ErpStockSelectPriceConfigService configService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        configService = mock(ErpStockSelectPriceConfigService.class);
        ErpStockSelectPriceConfigController controller = new ErpStockSelectPriceConfigController();
        ReflectionTestUtils.setField(controller, "configService", configService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json;charset=UTF-8"})
    void updateConfig_shouldAcceptJsonContentType(String contentType) throws Exception {
        mockMvc.perform(put("/erp/stock-select-price-config/update")
                        .contentType(MediaType.parseMediaType(contentType))
                        .content("{\"configVersion\":\"v1\",\"items\":[{\"fieldKey\":\"salePrice\","
                                + "\"saleVisible\":true,\"purchaseVisible\":false}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(configService).updateConfig(argThat(reqVO ->
                "v1".equals(reqVO.getConfigVersion())
                        && reqVO.getItems().size() == 1
                        && "salePrice".equals(reqVO.getItems().get(0).getFieldKey())
                        && Boolean.TRUE.equals(reqVO.getItems().get(0).getSaleVisible())
                        && Boolean.FALSE.equals(reqVO.getItems().get(0).getPurchaseVisible())));
    }

    @Test
    void updateConfig_shouldRejectMissingRequiredField() throws Exception {
        mockMvc.perform(put("/erp/stock-select-price-config/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configVersion\":\"v1\",\"items\":[{\"fieldKey\":\"salePrice\","
                                + "\"purchaseVisible\":false}]}"))
                .andExpect(status().isBadRequest());
    }

}
