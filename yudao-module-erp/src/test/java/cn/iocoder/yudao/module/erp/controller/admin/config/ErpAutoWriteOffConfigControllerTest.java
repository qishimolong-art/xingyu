package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigRespVO;
import cn.iocoder.yudao.module.erp.service.config.ErpAutoWriteOffConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErpAutoWriteOffConfigControllerTest {

    private ErpAutoWriteOffConfigService configService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        configService = mock(ErpAutoWriteOffConfigService.class);
        ErpAutoWriteOffConfigController controller = new ErpAutoWriteOffConfigController();
        ReflectionTestUtils.setField(controller, "configService", configService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getConfig_shouldReturnConfiguredAndEffectiveDepartmentIds() throws Exception {
        ErpAutoWriteOffConfigRespVO config = new ErpAutoWriteOffConfigRespVO();
        config.setDisabledDeptIds(Arrays.asList(10L, 12L));
        config.setEffectiveDisabledDeptIds(Arrays.asList(10L, 11L, 12L));
        when(configService.getConfig()).thenReturn(config);

        mockMvc.perform(get("/erp/auto-write-off-config/get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.disabledDeptIds[0]").value(10))
                .andExpect(jsonPath("$.data.effectiveDisabledDeptIds[1]").value(11));
    }

    @Test
    void updateConfig_shouldAcceptJsonContentType() throws Exception {
        mockMvc.perform(put("/erp/auto-write-off-config/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabledDeptIds\":[12,10,12]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(configService).updateConfig(argThat(reqVO ->
                reqVO.getDisabledDeptIds().equals(Arrays.asList(12L, 10L, 12L))));
    }

    @Test
    void updateConfig_shouldRejectMissingDisabledDeptIds() throws Exception {
        mockMvc.perform(put("/erp/auto-write-off-config/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConfig_shouldAllowFinanceQueryPermissionsForFormPreview() throws Exception {
        Method method = ErpAutoWriteOffConfigController.class.getMethod("getConfig");

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertEquals("@ss.hasAnyPermissions('erp:auto-write-off-config:query', "
                + "'erp:finance-payment:query', 'erp:finance-receipt:query')", annotation.value());
    }

}
