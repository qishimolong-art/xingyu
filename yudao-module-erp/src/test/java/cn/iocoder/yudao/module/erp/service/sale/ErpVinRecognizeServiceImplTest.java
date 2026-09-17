package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.vin.ErpVinRecognizeRespVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpVinRecognizeServiceImplTest {

    private final ErpVinRecognizeServiceImpl service = new ErpVinRecognizeServiceImpl();

    @Test
    void parseResponse_shouldMapNewAliyunNestedResult() {
        String body = "{"
                + "\"success\":true,"
                + "\"code\":200,"
                + "\"msg\":\"success\","
                + "\"data\":{"
                + "\"order_no\":\"VIN202609170001\","
                + "\"result\":{"
                + "\"manufacturer\":\"测试厂商\","
                + "\"brand\":\"测试品牌\","
                + "\"cartype\":\"SUV\","
                + "\"name\":\"测试车型\","
                + "\"yeartype\":\"2026款\","
                + "\"vin\":\"LSVTEST1234567890\","
                + "\"iscorrect\":1,"
                + "\"carlist\":\"测试车型列表\""
                + "}"
                + "}"
                + "}";

        ErpVinRecognizeRespVO result = service.parseResponse("LSVTEST1234567890", body);

        assertTrue(result.getSuccess());
        assertEquals("success", result.getMessage());
        assertEquals("LSVTEST1234567890", result.getVin());
        assertEquals("测试厂商", result.getData().get("manufacturer"));
        assertEquals("测试品牌", result.getData().get("brand"));
        assertEquals("SUV", result.getData().get("cartype"));
        assertEquals("测试车型", result.getData().get("name"));
        assertEquals("2026款", result.getData().get("yeartype"));
        assertEquals("测试车型列表", result.getData().get("carlist"));
        assertNotNull(result.getRaw().get("data"));
        assertTrue(result.getModelList().isEmpty());
    }

    @Test
    void parseResponse_shouldExposeFailureCodeAndMessage() {
        String body = "{"
                + "\"success\":false,"
                + "\"code\":400,"
                + "\"msg\":\"vin invalid\""
                + "}";

        ErpVinRecognizeRespVO result = service.parseResponse("LSVTEST1234567890", body);

        assertFalse(result.getSuccess());
        assertEquals("vin invalid", result.getMessage());
        assertEquals(400, result.getRaw().get("code"));
    }

    @Test
    void parseResponse_shouldKeepLegacyDataAndModelListCompatible() {
        String body = "{"
                + "\"status\":\"success\","
                + "\"message\":\"ok\","
                + "\"data\":{"
                + "\"brand_name\":\"Legacy Brand\","
                + "\"model_list\":[{\"name\":\"Legacy Model\",\"year\":\"2025\"}]"
                + "}"
                + "}";

        ErpVinRecognizeRespVO result = service.parseResponse("LSVTEST1234567890", body);

        assertTrue(result.getSuccess());
        assertEquals("ok", result.getMessage());
        assertEquals("Legacy Brand", result.getData().get("brand_name"));
        assertFalse(result.getData().containsKey("model_list"));
        assertEquals(1, result.getModelList().size());
        assertEquals("Legacy Model", result.getModelList().get(0).get("name"));
    }

    @Test
    void normalizeVin_shouldTrimAndUpperCase() {
        assertEquals("LSVTEST1234567890", service.normalizeVin(" lsvtest1234567890 "));
    }

    @Test
    void recognize_shouldRejectInvalidVinBeforeCallingRemoteApi() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.recognize("LSVTESTI234567890"));

        assertEquals(1_030_209_000, exception.getCode());
    }

}
