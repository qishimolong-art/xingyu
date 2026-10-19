package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ErpAccountMapperBalanceSqlTest {

    @Test
    void accountBalanceExcludesNonCashReceiptAndPaymentDiscountDocuments() throws Exception {
        Method method = ErpAccountMapper.class.getMethod("selectAccountBalanceList", java.util.Collection.class);
        Select select = method.getAnnotation(Select.class);
        String sql = String.join(" ", select.value());

        assertThat(sql).contains("FROM erp_payable_expense");
        assertThat(sql).contains("source_type IS NULL OR source_type &lt;&gt; '收款单优惠'");
        assertThat(sql).contains("FROM erp_receivable_other_income");
        assertThat(sql).contains("source_type IS NULL OR source_type &lt;&gt; '付款单折让'");
    }

}
