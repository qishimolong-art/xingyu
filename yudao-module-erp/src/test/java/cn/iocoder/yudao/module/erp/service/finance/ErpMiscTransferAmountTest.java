package cn.iocoder.yudao.module.erp.service.finance;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ErpMiscTransferAmountTest {
    @ParameterizedTest
    @CsvSource({"1000,1000,300,700", "1000,700,700,0", "1000,700,800,0",
            "-1000,-1000,-300,-700", "-1000,-700,-800,0", "1000,0,0,0"})
    void signedRemainingNeverFlipsDirection(String original, String balance, String pending, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(ErpMiscTransferAmount.available(
                new BigDecimal(original), new BigDecimal(balance), new BigDecimal(pending))));
    }
}
