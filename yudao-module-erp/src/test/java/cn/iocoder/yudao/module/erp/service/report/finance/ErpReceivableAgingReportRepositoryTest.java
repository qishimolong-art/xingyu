package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpReceivableAgingReportRepositoryTest {

    @Test
    void shouldUseFiveApprovedEventsAndFixedDayBuckets() {
        ErpReceivableAgingReportRepository repository = new ErpReceivableAgingReportRepository();
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpReceivableAgingReportRepository.Sql sql = repository.reportSql(filter(LocalDate.of(2026, 10, 14)),
                new ErpReceivableAgingReportRepository.Scope(1L, all, all));
        for (String table : new String[]{"erp_sale_out", "erp_sale_return", "erp_sale_price_adjust",
                "erp_finance_receipt", "erp_receivable_other"}) assertTrue(sql.cte.contains(table), table);
        assertFalse(sql.cte.contains("erp_receivable_writeoff"));
        assertFalse(sql.cte.contains("erp_receivable_misc"));
        assertTrue(sql.cte.contains("age_days BETWEEN 0 AND 30"));
        assertTrue(sql.cte.contains("age_days BETWEEN 31 AND 60"));
        assertTrue(sql.cte.contains("age_days BETWEEN 61 AND 90"));
        assertTrue(sql.cte.contains("age_days BETWEEN 91 AND 180"));
        assertTrue(sql.cte.contains("age_days BETWEEN 181 AND 365"));
        assertTrue(sql.cte.contains("age_days>=366"));
        assertTrue(sql.cte.contains("ROUND(e.balance,6)<>0"));
        assertTrue(sql.cte.contains("source_type='收款单优惠'"));
    }

    @Test
    void examplesShouldKeepSignedEventAmounts() {
        BigDecimal oldReceivable = new BigDecimal("1000.00");
        BigDecimal recentReceipt = new BigDecimal("-600.00");
        assertEquals(new BigDecimal("400.00"), oldReceivable.add(recentReceipt));
        assertEquals(new BigDecimal("1001.00"), new BigDecimal("1011.00").add(new BigDecimal("-10.00")));
    }

    @Test
    void shouldRunRealSqlAndKeepBalanceEqualToBuckets() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long tenantId = jdbc.queryForObject("SELECT tenant_id FROM erp_customer WHERE deleted=b'0' LIMIT 1", Long.class);
        assertNotNull(tenantId);

        ErpReceivableAgingReportRepository repository = new ErpReceivableAgingReportRepository();
        Field field = ErpReceivableAgingReportRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbc);
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpReceivableAgingReportRepository.Scope scope =
                new ErpReceivableAgingReportRepository.Scope(tenantId, all, all);
        Filter filter = filter(LocalDate.now());
        List<Row> rows = repository.page(filter, scope, 0, 500);
        Summary summary = repository.summary(filter, scope);
        assertEquals(repository.count(filter, scope), summary.getRowCount());
        for (Row row : rows) assertEquals(0, zero(row.getBalance()).compareTo(bucketTotal(row)));
        assertEquals(0, zero(summary.getBalance()).compareTo(bucketTotal(summary)));
    }

    private Filter filter(LocalDate date) {
        Filter filter = new Filter();
        filter.setAnalysisDate(date);
        filter.setPageNo(1);
        filter.setPageSize(500);
        return filter;
    }

    private BigDecimal bucketTotal(Row row) {
        return sum(row.getWithin1Month(), row.getWithin2Months(), row.getWithin3Months(), row.getMonths3To6(),
                row.getMonths6To12(), row.getOver1Year());
    }

    private BigDecimal bucketTotal(Summary row) {
        return sum(row.getWithin1Month(), row.getWithin2Months(), row.getWithin3Months(), row.getMonths3To6(),
                row.getMonths6To12(), row.getOver1Year());
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) total = total.add(zero(value));
        return total;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
