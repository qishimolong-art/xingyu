package cn.iocoder.yudao.module.erp.service.finance.accounting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
/** 旧硬编码公式的测试已由 ErpVoucherRuleCalculatorTest 的新业务规则用例替代。 */
class ErpAutoVoucherBuilderTest {
    @Test void retiredEntryPointsFailClosed() {
        ErpAutoVoucherBuilder b = new ErpAutoVoucherBuilder();
        assertThrows(IllegalStateException.class, () -> b.buildPurchaseInItems(null,null));
        assertThrows(IllegalStateException.class, () -> b.buildPurchaseReturnItems(null,null));
        assertThrows(IllegalStateException.class, () -> b.buildSaleOutItems(null,null,null));
        assertThrows(IllegalStateException.class, () -> b.buildSaleReturnItems(null,null,null));
        assertThrows(IllegalStateException.class, () -> b.buildStockInItems(null,null));
        assertThrows(IllegalStateException.class, () -> b.buildStockOutItems(null,null));
        assertThrows(IllegalStateException.class, () -> b.buildOtherReceivableItems(null));
        assertThrows(IllegalStateException.class, () -> b.buildOtherPayableItems(null));
        assertThrows(IllegalStateException.class, () -> b.buildPreReceiptItems(null));
        assertThrows(IllegalStateException.class, () -> b.buildPrePaymentItems(null));
        assertThrows(IllegalStateException.class, () -> b.buildPreReceivableItems(null));
    }
}
