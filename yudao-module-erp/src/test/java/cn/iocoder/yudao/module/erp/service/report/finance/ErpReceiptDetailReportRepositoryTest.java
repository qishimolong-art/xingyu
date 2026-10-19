package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpReceiptDetailReportRepositoryTest {

    @Test
    void shouldBuildFourSourceEffectiveWriteOffAndDeterministicRoundingSql() throws Exception {
        ErpReceiptDetailReportRepository repository = new ErpReceiptDetailReportRepository();
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(20);
        filter.setPaymentVoucherNo(" CUSTOMER-REF ");
        filter.setVoucherNo(" ACCOUNTING-REF ");
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpReceiptDetailReportRepository.Scope scope =
                new ErpReceiptDetailReportRepository.Scope(1L, all, all);

        Method method = ErpReceiptDetailReportRepository.class
                .getDeclaredMethod("reportSql", Filter.class, ErpReceiptDetailReportRepository.Scope.class);
        method.setAccessible(true);
        Object sql = method.invoke(repository, filter, scope);
        Field cteField = sql.getClass().getDeclaredField("cte");
        Field whereField = sql.getClass().getDeclaredField("where");
        Field paramsField = sql.getClass().getDeclaredField("params");
        cteField.setAccessible(true);
        whereField.setAccessible(true);
        paramsField.setAccessible(true);
        String cte = (String) cteField.get(sql);
        String where = (String) whereField.get(sql);
        @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) paramsField.get(sql);

        assertTrue(cte.contains("erp_sale_out"));
        assertTrue(cte.contains("erp_sale_return"));
        assertTrue(cte.contains("erp_sale_price_adjust"));
        assertTrue(cte.contains("erp_receivable_misc"));
        assertTrue(cte.contains("r.status=20"));
        assertTrue(cte.contains("i.write_off_status=1"));
        assertTrue(cte.contains("SUM(settlement_amount) OVER(PARTITION BY receipt_id)"));
        assertTrue(cte.contains("MAX(receipt_item_id) OVER(PARTITION BY receipt_id)"));
        assertTrue(cte.contains("receipt_actual-SUM"));
        assertTrue(cte.contains("CAST(NULL AS DECIMAL(20,2)) cost_amount"));
        assertTrue(cte.contains("r.id receipt_id"));
        assertTrue(cte.contains("s.id business_id"));
        assertTrue(cte.contains("r.payment_voucher_no"));
        assertTrue(cte.contains("v.source_biz_type=6"));
        assertTrue(where.contains("report.payment_voucher_no LIKE :paymentVoucherNo"));
        assertTrue(where.contains("report.voucher_no LIKE :voucherNo"));
        assertEquals("%CUSTOMER-REF%", params.get("paymentVoucherNo"));
        assertEquals("%ACCOUNTING-REF%", params.get("voucherNo"));

        ErpFinanceVisibleScope self = new ErpFinanceVisibleScope(false, Collections.emptyList(), 9L);
        Object scopedSql = method.invoke(repository, filter,
                new ErpReceiptDetailReportRepository.Scope(1L, self, self));
        String scopedCte = (String) cteField.get(scopedSql);
        assertTrue(scopedCte.contains("r.finance_user_id=:receiptSelf"));
        assertTrue(scopedCte.contains("c.sale_user_id=:customerSelf"));
    }

    @Test
    void shouldQueryRealReceiptDetailsWhenIntegrationDatabaseConfigured() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long tenantId = jdbc.queryForObject("SELECT tenant_id FROM erp_finance_receipt "
                + "WHERE deleted=b'0' AND status=20 LIMIT 1", Long.class);
        assertNotNull(tenantId);

        ErpReceiptDetailReportRepository repository = new ErpReceiptDetailReportRepository();
        Field field = ErpReceiptDetailReportRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbc);
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpReceiptDetailReportRepository.Scope scope =
                new ErpReceiptDetailReportRepository.Scope(tenantId, all, all);
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(500);

        long count = repository.count(filter, scope);
        List<Row> rows = repository.page(filter, scope, 0, 500);
        Summary summary = repository.summary(filter, scope);
        assertEquals(count, summary.getRowCount());
        assertEquals(Math.min(count, 500), rows.size());
        BigDecimal settlement = rows.stream().map(Row::getSettlementAmount)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actual = rows.stream().map(Row::getActualReceiptAmount)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (count <= 500) {
            assertEquals(0, settlement.compareTo(summary.getSettlementAmount()));
            assertEquals(0, actual.compareTo(summary.getActualReceiptAmount()));
        }
        rows.forEach(row -> {
            assertNotNull(row.getReceiptId());
            assertTrue(row.getReceiptId() > 0);
            assertNotNull(row.getBusinessId());
            assertTrue(row.getBusinessId() > 0);
            assertTrue(row.getCostAmount() == null);
            assertTrue(row.getGrossProfit() == null);
            assertTrue(row.getGrossProfitRate() == null);
        });
    }
}
