package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 仅在显式提供 ERP_REPORT_DB_* 环境变量时连接目标开发库，默认跳过。
 */
class ErpPurchaseInDetailReportRepositoryIntegrationTest {

    @Test
    void reportSql_usesAllocationLedgerForAllThreeSourceTypes() {
        ErpPurchaseInDetailReportRepository repository = new ErpPurchaseInDetailReportRepository();
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, List.of(), null);
        ErpPurchaseInDetailReportRepository.Scope scope =
                new ErpPurchaseInDetailReportRepository.Scope(1L, all, all, all, all);

        Object sql = ReflectionTestUtils.invokeMethod(repository, "reportSql", new Filter(), scope);
        String cte = (String) ReflectionTestUtils.getField(sql, "cte");

        assertNotNull(cte);
        assertTrue(cte.contains("GROUP BY pia.source_type,pia.source_item_id"));
        assertTrue(cte.contains("inv.source_type=10 AND inv.source_item_id=ifi.id"));
        assertTrue(cte.contains("inv.source_type=20 AND inv.source_item_id=pai.id"));
        assertTrue(cte.contains("inv.source_type=30 AND inv.source_item_id=pri.id"));
        assertFalse(cte.contains("inv.source_in_item_id"));
        assertFalse(cte.contains("'不适用'"));
    }

    @Test
    void queryApprovedThreeDocumentTypesAndCalculatedFields() {
        String url = System.getenv("ERP_REPORT_DB_URL");
        String username = System.getenv("ERP_REPORT_DB_USER");
        String password = System.getenv("ERP_REPORT_DB_PASSWORD");
        Assumptions.assumeTrue(url != null && username != null && password != null);

        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        ErpPurchaseInDetailReportRepository repository = new ErpPurchaseInDetailReportRepository();
        ReflectionTestUtils.setField(repository, "jdbcTemplate", new JdbcTemplate(dataSource));
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, List.of(), null);
        ErpPurchaseInDetailReportRepository.Scope scope =
                new ErpPurchaseInDetailReportRepository.Scope(1L, all, all, all, all);
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(500);

        long count = repository.count(filter, scope);
        Summary summary = repository.summary(filter, scope);
        List<Row> rows = repository.page(filter, scope, 0, 500);

        assertTrue(count > 0);
        assertEquals(count, summary.getRowCount());
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(row -> row.getRowKey() != null && row.getDocumentType() != null));
        assertTrue(rows.stream().anyMatch(row -> "PURCHASE_IN".equals(row.getDocumentTypeCode())));
        assertTrue(rows.stream().anyMatch(row -> "PURCHASE_RETURN".equals(row.getDocumentTypeCode())));
        assertTrue(rows.stream().anyMatch(row -> "PURCHASE_ADJUST".equals(row.getDocumentTypeCode())));
        assertTrue(rows.stream().filter(row -> "PURCHASE_RETURN".equals(row.getDocumentTypeCode()))
                .allMatch(row -> row.getCount().compareTo(BigDecimal.ZERO) <= 0
                        && row.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0));
        assertTrue(rows.stream().filter(row -> "PURCHASE_ADJUST".equals(row.getDocumentTypeCode()))
                .allMatch(row -> row.getCount().compareTo(BigDecimal.ZERO) == 0));
        assertTrue(rows.stream().filter(row -> "PURCHASE_IN".equals(row.getDocumentTypeCode()))
                .allMatch(row -> row.getInvoiceStatus() != null));
        assertNotNull(summary.getFreightAmount());
        Set<Long> productIds = new LinkedHashSet<>();
        rows.stream().map(Row::getProductId).filter(java.util.Objects::nonNull).limit(10).forEach(productIds::add);
        assertFalse(repository.priceEvents(productIds, scope).isEmpty());
    }
}
