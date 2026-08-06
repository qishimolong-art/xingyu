package cn.iocoder.yudao.module.erp.service.purchase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
