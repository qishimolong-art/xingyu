package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrRespVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpCustomerBusinessLicenseOcrServiceImplTest {

    private final ErpCustomerBusinessLicenseOcrServiceImpl service = new ErpCustomerBusinessLicenseOcrServiceImpl();

    @Test
    void normalizeImage_shouldStripDataUrlPrefix() {
        assertEquals("abc123", service.normalizeImage("data:image/png;base64,abc123"));
        assertEquals("https://example.com/license.png",
                service.normalizeImage(" https://example.com/license.png "));
    }

    @Test
    void parseResponse_shouldMapAliyunBusinessLicenseFields() {
        String body = "{"
                + "\"success\":true,"
                + "\"message\":\"ok\","
                + "\"reg_num\":\"91330100MA00000000\","
                + "\"name\":\"Hangzhou Test Technology Co., Ltd.\","
                + "\"address\":\"No. 1 Test Road\","
                + "\"person\":\"Zhang San\","
                + "\"type\":\"Limited Liability Company\","
                + "\"capital\":\"1000万人民币\","
                + "\"establish_date\":\"2024-01-02\","
                + "\"valid_period\":\"长期\","
                + "\"business_scope\":\"software development\""
                + "}";

        ErpCustomerBusinessLicenseOcrRespVO result = service.parseResponse(body);

        assertTrue(result.getSuccess());
        assertEquals("ok", result.getMessage());
        assertEquals("91330100MA00000000", result.getUnifiedCreditCode());
        assertEquals("91330100MA00000000", result.getTaxNo());
        assertEquals("Hangzhou Test Technology Co., Ltd.", result.getInvoiceCompany());
        assertEquals("No. 1 Test Road", result.getInvoiceAddress());
        assertEquals("Zhang San", result.getLegalPerson());
        assertEquals("Limited Liability Company", result.getCompanyType());
        assertEquals("1000万人民币", result.getRegisteredCapital());
        assertEquals("2024-01-02", result.getEstablishDate());
        assertEquals("长期", result.getValidPeriod());
        assertEquals("software development", result.getBusinessScope());
        assertEquals("91330100MA00000000", result.getData().get("reg_num"));
        assertEquals("Hangzhou Test Technology Co., Ltd.", result.getRaw().get("name"));
    }

    @Test
    void parseResponse_shouldMapNestedResultFields() {
        String body = "{"
                + "\"code\":200,"
                + "\"msg\":\"success\","
                + "\"result\":{"
                + "\"creditCode\":\"91330100MA11111111\","
                + "\"companyName\":\"Nested Test Co., Ltd.\","
                + "\"registeredAddress\":\"Nested Address\""
                + "}"
                + "}";

        ErpCustomerBusinessLicenseOcrRespVO result = service.parseResponse(body);

        assertTrue(result.getSuccess());
        assertEquals("success", result.getMessage());
        assertEquals("91330100MA11111111", result.getUnifiedCreditCode());
        assertEquals("91330100MA11111111", result.getTaxNo());
        assertEquals("Nested Test Co., Ltd.", result.getInvoiceCompany());
        assertEquals("Nested Address", result.getInvoiceAddress());
    }

    @Test
    void parseResponse_shouldMapDeepNestedFields() {
        String body = "{"
                + "\"success\":true,"
                + "\"data\":{"
                + "\"blocks\":[{"
                + "\"companyName\":\"Deep Nested Co., Ltd.\","
                + "\"creditCode\":\"91330100MA22222222\","
                + "\"business\":\"retail; wholesale\""
                + "}],"
                + "\"extra\":{\"legalPerson\":\"Li Si\"}"
                + "}"
                + "}";

        ErpCustomerBusinessLicenseOcrRespVO result = service.parseResponse(body);

        assertTrue(result.getSuccess());
        assertEquals("91330100MA22222222", result.getUnifiedCreditCode());
        assertEquals("91330100MA22222222", result.getTaxNo());
        assertEquals("Deep Nested Co., Ltd.", result.getInvoiceCompany());
        assertEquals("Li Si", result.getLegalPerson());
        assertEquals("retail; wholesale", result.getBusinessScope());
    }

    @Test
    void parseResponse_shouldExposeAliyunErrorCodeMessage() {
        String body = "{"
                + "\"error_code\":\"400\","
                + "\"error_msg\":\"invalid image\""
                + "}";

        ErpCustomerBusinessLicenseOcrRespVO result = service.parseResponse(body);

        assertFalse(result.getSuccess());
        assertEquals("invalid image", result.getMessage());
    }

    @Test
    void parseResponse_shouldAcceptTextSuccessStatus() {
        String body = "{"
                + "\"status\":\"success\","
                + "\"message\":\"ok\","
                + "\"data\":{\"name\":\"Status Success Co., Ltd.\"}"
                + "}";

        ErpCustomerBusinessLicenseOcrRespVO result = service.parseResponse(body);

        assertTrue(result.getSuccess());
        assertEquals("Status Success Co., Ltd.", result.getInvoiceCompany());
    }

}
