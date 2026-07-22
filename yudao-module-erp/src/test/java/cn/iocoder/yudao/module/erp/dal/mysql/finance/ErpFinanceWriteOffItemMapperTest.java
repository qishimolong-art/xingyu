package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ErpFinanceWriteOffItemMapperTest {

    @Test
    void selectPaymentPriceSumByBizIdAndBizType_returnsZeroWhenAggregateRowIsNull() {
        ErpFinancePaymentItemMapper mapper = mock(ErpFinancePaymentItemMapper.class, CALLS_REAL_METHODS);
        doReturn(Collections.singletonList(null)).when(mapper).selectMaps(any());

        BigDecimal result = mapper.selectPaymentPriceSumByBizIdAndBizType(1L, 10);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void selectReceiptPriceSumByBizIdAndBizType_returnsZeroWhenAggregateRowIsNull() {
        ErpFinanceReceiptItemMapper mapper = mock(ErpFinanceReceiptItemMapper.class, CALLS_REAL_METHODS);
        doReturn(Collections.singletonList(null)).when(mapper).selectMaps(any());

        BigDecimal result = mapper.selectReceiptPriceSumByBizIdAndBizType(1L, 10);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

}
