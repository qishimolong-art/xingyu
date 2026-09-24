package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErpPurchaseInvoiceOcrServiceImplTest {

    private final ErpPurchaseInvoiceOcrServiceImpl service = new ErpPurchaseInvoiceOcrServiceImpl();

    @Test
    void parseFactoryOrderNo_shouldUseFirstRemarkToken() {
        assertEquals("S250801001", service.parseFactoryOrderNo("S250801001 其他备注"));
        assertEquals("S250801001", service.parseFactoryOrderNo("  S250801001\t其他备注"));
        assertEquals("S260727090788", service.parseFactoryOrderNo("S260727090788 8027393083 2026-09-03 根据协议"));
    }

    @Test
    void parseFactoryOrderNo_shouldHandleMergedAuxiliaryNoBeforeDate() {
        assertEquals("S260727090788",
                service.parseFactoryOrderNo("S2607270907888027393083 2026-09-03根据协议,贵公司向我司采购的产品可能享受折扣"));
        assertEquals("S260727090788",
                service.parseFactoryOrderNo("S26072709078880273930832026-09-03根据协议,贵公司向我司采购的产品可能享受折扣"));
        assertEquals("S250801001",
                service.parseFactoryOrderNo("S2508010018027393083 2026-09-03根据协议,贵公司向我司采购的产品可能享受折扣"));
    }

    @Test
    void parseFactoryOrderNo_shouldReturnNullWhenRemarkDoesNotStartWithFactoryOrderNo() {
        assertNull(service.parseFactoryOrderNo(null));
        assertNull(service.parseFactoryOrderNo(""));
        assertNull(service.parseFactoryOrderNo("其他备注 S250801001"));
    }

    @Test
    void parseInternalFileLocation_shouldSupportAbsoluteLocalUrl() {
        ErpPurchaseInvoiceOcrServiceImpl.InternalFileLocation result = service.parseInternalFileLocation(
                "http://127.0.0.1:48080/admin-api/infra/file/-1/get/erp/purchase-invoice/20260921/a.jpg");

        assertNotNull(result);
        assertEquals(-1L, result.getConfigId());
        assertEquals("erp/purchase-invoice/20260921/a.jpg", result.getPath());
    }

    @Test
    void parseInternalFileLocation_shouldSupportRelativeAndEncodedUrl() {
        ErpPurchaseInvoiceOcrServiceImpl.InternalFileLocation result = service.parseInternalFileLocation(
                "/admin-api/infra/file/4/get/erp/purchase-invoice/%E5%8F%91%E7%A5%A8.jpg?token=1");

        assertNotNull(result);
        assertEquals(4L, result.getConfigId());
        assertEquals("erp/purchase-invoice/发票.jpg", result.getPath());
    }

    @Test
    void buildAliyunApiGatewaySignatureHeaders_shouldUseAppKeySecretSignature() {
        Map<String, String> result = ErpPurchaseInvoiceOcrServiceImpl.buildAliyunApiGatewaySignatureHeaders(
                "key", "secret", "POST",
                "https://dgfp.market.alicloudapi.com/ocrservice/invoice?b=2&a=1",
                "application/json; charset=utf-8", "application/json; charset=utf-8",
                "1234567890", "nonce");

        assertEquals("key", result.get("X-Ca-Key"));
        assertEquals("nonce", result.get("X-Ca-Nonce"));
        assertEquals("HmacSHA256", result.get("X-Ca-Signature-Method"));
        assertEquals("1234567890", result.get("X-Ca-Timestamp"));
        assertEquals("x-ca-key,x-ca-nonce,x-ca-signature-method,x-ca-timestamp",
                result.get("X-Ca-Signature-Headers"));
        assertEquals("HWl6XISnQHQHxevQ1JxRAiYyZOE86OJAN/9k/x7/yB4=", result.get("X-Ca-Signature"));
    }

    @Test
    void applyInvoiceOcrAuth_shouldPreferAppCodeWhenBothCredentialsExist() {
        ReflectionTestUtils.setField(service, "appKey", "key");
        ReflectionTestUtils.setField(service, "appSecret", "secret");
        ReflectionTestUtils.setField(service, "appCode", "code");
        ReflectionTestUtils.setField(service, "endpoint", "https://dgfp.market.alicloudapi.com/ocrservice/invoice");
        HttpRequest request = HttpRequest.post("https://dgfp.market.alicloudapi.com/ocrservice/invoice");

        ReflectionTestUtils.invokeMethod(service, "applyInvoiceOcrAuth", request);

        assertEquals("APPCODE code", request.header("Authorization"));
        assertNull(request.header("X-Ca-Key"));
    }

}
