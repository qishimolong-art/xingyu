package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpPayableAgingReportRepositoryTest {

    @Test
    void shouldBuildRollingMonthBoundariesForLeapDay() {
        Map<String, Object> params = new HashMap<>();
        ErpPayableAgingReportRepository.applyBoundaries(params, LocalDate.of(2024, 2, 29));
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), params.get("cutoff_exclusive"));
        assertEquals(LocalDate.of(2024, 1, 29), params.get("month01_start"));
        assertEquals(LocalDate.of(2023, 2, 28), params.get("month12_start"));
        assertEquals(LocalDate.of(2022, 2, 28), params.get("month24_start"));
        assertEquals(LocalDate.of(2021, 2, 28), params.get("month36_start"));
    }

    @Test
    void shouldUseOnlyFiveApprovedPayableSourcesAndKeepZeroSuppliers() {
        ErpPayableAgingReportRepository repository = new ErpPayableAgingReportRepository();
        Filter filter = filter(LocalDate.of(2026, 10, 14));
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpPayableAgingReportRepository.Sql sql = repository.reportSql(filter,
                new ErpPayableAgingReportRepository.Scope(1L, all, all));
        for (String table : new String[]{"erp_purchase_in", "erp_purchase_return",
                "erp_purchase_price_adjust", "erp_payable_other", "erp_finance_payment"}) {
            assertTrue(sql.cte.contains(table), table);
        }
        assertFalse(sql.cte.contains("erp_payable_writeoff"));
        assertFalse(sql.cte.contains("erp_pre_payment"));
        assertTrue(sql.cte.contains("t.status=20"));
        assertTrue(sql.cte.contains("visible_suppliers vs LEFT JOIN aged"));
        assertTrue(sql.cte.contains("event_date>=:month12_start AND event_date<:month11_start"));
        assertTrue(sql.cte.contains("event_date<:month36_start"));
    }

    @Test
    void shouldQueryRealSupplierRowsAndKeepBalanceEqualToAllBuckets() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long tenantId = jdbc.queryForObject("SELECT tenant_id FROM erp_supplier WHERE deleted=b'0' LIMIT 1", Long.class);
        assertNotNull(tenantId);

        ErpPayableAgingReportRepository repository = new ErpPayableAgingReportRepository();
        Field field = ErpPayableAgingReportRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbc);
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpPayableAgingReportRepository.Scope scope =
                new ErpPayableAgingReportRepository.Scope(tenantId, all, all);
        Filter filter = filter(LocalDate.now());

        long count = repository.count(filter, scope);
        Long supplierCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM erp_supplier WHERE tenant_id=? AND deleted=b'0'", Long.class, tenantId);
        assertEquals(supplierCount == null ? 0L : supplierCount, count);
        List<Row> rows = repository.page(filter, scope, 0, 500);
        Summary summary = repository.summary(filter, scope);
        assertEquals(count, summary.getRowCount());
        for (Row row : rows) {
            assertEquals(0, zero(row.getBalance()).compareTo(bucketTotal(row)), row.getSupplierName());
        }
        assertEquals(0, zero(summary.getBalance()).compareTo(bucketTotal(summary)));
    }

    private Filter filter(LocalDate date) {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(500);
        filter.setAsOfDate(date);
        return filter;
    }

    private BigDecimal bucketTotal(Row row) {
        return sum(row.getMonth01(), row.getMonth02(), row.getMonth03(), row.getMonth04(), row.getMonth05(),
                row.getMonth06(), row.getMonth07(), row.getMonth08(), row.getMonth09(), row.getMonth10(),
                row.getMonth11(), row.getMonth12(), row.getWithinTwoYears(), row.getWithinThreeYears(),
                row.getOverThreeYears());
    }

    private BigDecimal bucketTotal(Summary summary) {
        return sum(summary.getMonth01(), summary.getMonth02(), summary.getMonth03(), summary.getMonth04(),
                summary.getMonth05(), summary.getMonth06(), summary.getMonth07(), summary.getMonth08(),
                summary.getMonth09(), summary.getMonth10(), summary.getMonth11(), summary.getMonth12(),
                summary.getWithinTwoYears(), summary.getWithinThreeYears(), summary.getOverThreeYears());
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
