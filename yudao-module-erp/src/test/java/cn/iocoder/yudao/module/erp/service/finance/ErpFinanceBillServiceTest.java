package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountTransactionRespVO;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpFinanceBillServiceTest {

    @Test
    void accountTransactionBalanceIsFilledFromCurrentBalanceBackwards() {
        ErpFinanceBillService service = new ErpFinanceBillService();
        ErpAccountService accountService = mock(ErpAccountService.class);
        ErpAccountBalanceBO balance = new ErpAccountBalanceBO();
        balance.setAccountId(1L);
        balance.setCurrentBalance(new BigDecimal("170"));
        when(accountService.getAccountBalanceMap(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonMap(1L, balance));
        ReflectionTestUtils.setField(service, "accountService", accountService);

        ErpAccountTransactionRespVO payment = buildRow("FK-1",
                ErpFinanceBillService.BILL_TYPE_PAYMENT, "30", LocalDateTime.of(2026, 9, 15, 10, 0));
        ErpAccountTransactionRespVO receipt = buildRow("SK-1",
                ErpFinanceBillService.BILL_TYPE_RECEIPT, "100", LocalDateTime.of(2026, 9, 16, 10, 0));
        List<ErpAccountTransactionRespVO> rows = Arrays.asList(payment, receipt);

        ReflectionTestUtils.invokeMethod(service, "fillAccountTransactionBalances", rows, 1L);

        assertThat(receipt.getBalance()).isEqualByComparingTo("170");
        assertThat(payment.getBalance()).isEqualByComparingTo("70");
    }

    private ErpAccountTransactionRespVO buildRow(String no, String billType, String amount, LocalDateTime time) {
        ErpAccountTransactionRespVO row = new ErpAccountTransactionRespVO();
        row.setNo(no);
        row.setBillType(billType);
        row.setAmount(new BigDecimal(amount));
        row.setTransactionTime(time);
        return row;
    }

}
