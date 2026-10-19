package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpPayableDetailReportRepositoryTest {

    @Test
    void shouldQueryRealFiveSourceLedgerAndKeepSupplierBalancesStable() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long tenantId = jdbc.queryForObject("SELECT tenant_id FROM erp_purchase_in WHERE deleted=b'0' AND status=20 "
                + "UNION SELECT tenant_id FROM erp_finance_payment WHERE deleted=b'0' AND status=20 LIMIT 1", Long.class);
        assertNotNull(tenantId);

        ErpPayableDetailReportRepository repository = new ErpPayableDetailReportRepository();
        Field field = ErpPayableDetailReportRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbc);
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpPayableDetailReportRepository.Scope scope =
                new ErpPayableDetailReportRepository.Scope(tenantId, all, all);
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(500);
        filter.setOrderField("documentDate");
        filter.setOrderDirection("asc");

        long count = repository.count(filter, scope);
        List<Row> rows = repository.page(filter, scope, 0, 500);
        Summary summary = repository.summary(filter, scope);
        assertEquals(count, summary.getRowCount());
        assertEquals(Math.min(count, 500), rows.size());

        Map<Long, BigDecimal> lastBalance = new HashMap<>();
        Map<String, BigDecimal> balanceByRow = new HashMap<>();
        for (Row row : rows) {
            BigDecimal previous = lastBalance.getOrDefault(row.getSupplierId(), BigDecimal.ZERO);
            assertEquals(0, previous.compareTo(row.getPreviousBalance()), row.getRowKey());
            BigDecimal change = zero(row.getIncreasePayable()).subtract(zero(row.getPaymentAmount()));
            assertEquals(0, previous.add(change).compareTo(row.getEndingPayable()), row.getRowKey());
            lastBalance.put(row.getSupplierId(), row.getEndingPayable());
            balanceByRow.put(row.getRowKey(), row.getEndingPayable());
        }
        if (count <= 500) {
            BigDecimal ending = lastBalance.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, ending.compareTo(summary.getEndingPayable()));
        }

        if (!rows.isEmpty()) {
            Row selected = rows.get(0);
            Filter displayFilter = new Filter();
            displayFilter.setPageNo(1);
            displayFilter.setPageSize(100);
            displayFilter.setOrderField("documentDate");
            displayFilter.setOrderDirection("asc");
            displayFilter.setDocumentTypes(Collections.singletonList(selected.getDocumentTypeCode()));
            displayFilter.setDocumentNo(selected.getDocumentNo());
            List<Row> filteredRows = repository.page(displayFilter, scope, 0, 100);
            Row sameRow = filteredRows.stream().filter(row -> selected.getRowKey().equals(row.getRowKey()))
                    .findFirst().orElseThrow(IllegalStateException::new);
            assertEquals(0, balanceByRow.get(selected.getRowKey()).compareTo(sameRow.getEndingPayable()),
                    "单据类型或单号筛选不应重算真实余额");
        }
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
