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

    @Test
    void effectivePaymentPriceSql_onlyCountsEffectiveApprovedRowsInCurrentTenant() {
        String sql = ErpFinancePaymentItemMapper.effectivePaymentPriceSql(11);

        assertThat(sql).contains("fpi.biz_type = 11", "fpi.write_off_status = 1",
                "fp.status = 20", "fp.deleted = 0",
                "fpi.tenant_id = t.tenant_id", "fp.tenant_id = t.tenant_id");
    }

    @Test
    void effectiveReceiptPriceSql_onlyCountsEffectiveApprovedRowsInCurrentTenant() {
        String sql = ErpFinanceReceiptItemMapper.effectiveReceiptPriceSql(21);

        assertThat(sql).contains("fri.biz_type = 21", "fri.write_off_status = 1",
                "fr.status = 20", "fr.deleted = 0",
                "fri.tenant_id = t.tenant_id", "fr.tenant_id = t.tenant_id");
    }

}
