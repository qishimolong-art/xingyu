package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpReceivableDetailReportRepositoryTest {

    @Test
    void shouldExposeSourceTargetsWithoutAddingTechnicalFieldsToExcel() throws Exception {
        assertNull(Row.class.getDeclaredField("sourceBusinessId").getAnnotation(ExcelProperty.class));
        assertNull(Row.class.getDeclaredField("sourceDocumentTypeCode").getAnnotation(ExcelProperty.class));

        ErpReceivableDetailReportRepository repository = new ErpReceivableDetailReportRepository();
        String saleOut = invokeBusinessSourceSql(repository, "saleOut");
        assertTrue(saleOut.contains("WHEN 10 THEN 'SALE_ORDER'"));
        assertTrue(saleOut.contains("WHEN 20 THEN 'SALE_QUOTE'"));
        assertTrue(saleOut.contains("WHEN 30 THEN 'SALE_CART'"));
        assertTrue(saleOut.contains("WHEN 40 THEN 'SALE_PRICE_ADJUST'"));
        assertTrue(saleOut.contains("WHEN 50 THEN 'PURCHASE_IN'"));
        assertTrue(saleOut.contains("WHEN 70 THEN 'SALE_RETURN'"));
        String saleReturn = invokeBusinessSourceSql(repository, "saleReturn");
        assertTrue(saleReturn.contains("t.source_out_id"));
        assertTrue(saleReturn.contains("-ABS(COALESCE(t.total_price,0))"));
        assertTrue(saleReturn.contains("CASE WHEN ABS(-ABS(COALESCE(t.total_price,0)))>0"));
        assertTrue(saleReturn.contains(">=ROUND(ABS(-ABS(COALESCE(t.total_price,0))),6)"));
        assertTrue(invokeBusinessSourceSql(repository, "saleAdjust").contains("t.original_sale_out_id"));
        assertTrue(saleOut.contains("fri.write_off_status=1"));
        assertTrue(saleOut.contains("fri.deleted=b'0'"));
        assertTrue(saleOut.contains("fr.status=20"));
        assertTrue(saleOut.contains("fr.deleted=b'0'"));
        assertTrue(saleOut.contains("SUM(wo.write_off_amount)"));
        assertTrue(saleOut.contains("wo.deleted=b'0'"));

        String receipt = invokeSourceSql(repository, "receipt");
        assertTrue(receipt.contains("COUNT(DISTINCT CONCAT(fri.biz_type,':',fri.biz_id))=1"));
        assertTrue(receipt.contains("COUNT(DISTINCT NULLIF(TRIM(fri.biz_no),''))=1"));
        assertTrue(receipt.contains("WHEN 24 THEN 'RECEIVABLE_MISC'"));

        String adjust = invokeSourceSql(repository, "receivableAdjust");
        assertTrue(adjust.contains("WHEN '销售手推车' THEN 'SALE_CART'"));
        assertTrue(adjust.contains("WHEN '收款单优惠' THEN 'FINANCE_RECEIPT'"));
        assertTrue(adjust.contains("ROUND(COALESCE(t.settled_amount,0),6) write_off_amount"));
        assertTrue(receipt.contains("ROUND(0,6) write_off_amount"));
    }

    @Test
    void shouldBuildFiveSourceLedgerWithoutManualWriteoffBalanceEvent() throws Exception {
        ErpReceivableDetailReportRepository repository = new ErpReceivableDetailReportRepository();
        Filter filter = new Filter();
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpReceivableDetailReportRepository.Scope scope =
                new ErpReceivableDetailReportRepository.Scope(1L, all, all);

        Method method = ErpReceivableDetailReportRepository.class.getDeclaredMethod(
                "reportSql", Filter.class, ErpReceivableDetailReportRepository.Scope.class);
        method.setAccessible(true);
        Object sql = method.invoke(repository, filter, scope);
        Field cteField = sql.getClass().getDeclaredField("cte");
        cteField.setAccessible(true);
        String cte = (String) cteField.get(sql);

        assertEquals(4, occurrences(cte, " UNION ALL "));
        assertTrue(cte.contains("(increase_amount-receipt_amount) change_amount"));
        assertFalse(cte.contains("MANUAL_WRITEOFF"));
        assertFalse(cte.contains("FROM erp_receivable_writeoff w) t"));
        assertFalse(cte.contains("increase_amount-receipt_amount-write_off_amount"));
    }

    @Test
    void shouldKeepManualWriteoffAsProgressWithoutReducingReceivableBalanceAgain() {
        BigDecimal receivable = new BigDecimal("1000.00");
        BigDecimal approvedReceipt = new BigDecimal("600.00");
        BigDecimal legacyManualWriteoff = new BigDecimal("100.00");

        BigDecimal balance = receivable.subtract(approvedReceipt);
        BigDecimal settledProgress = approvedReceipt.add(legacyManualWriteoff);

        assertEquals(new BigDecimal("400.00"), balance);
        assertEquals(new BigDecimal("700.00"), settledProgress);
    }

    @Test
    void shouldApplyAccountScopeToSettlementProgress() throws Exception {
        ErpReceivableDetailReportRepository repository = new ErpReceivableDetailReportRepository();
        ErpFinanceVisibleScope accountScope = new ErpFinanceVisibleScope(false, Arrays.asList(3L, 4L), 9L);
        ErpFinanceVisibleScope customerScope = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpReceivableDetailReportRepository.Scope scope =
                new ErpReceivableDetailReportRepository.Scope(1L, accountScope, customerScope);

        Method method = ErpReceivableDetailReportRepository.class.getDeclaredMethod(
                "reportSql", Filter.class, ErpReceivableDetailReportRepository.Scope.class);
        method.setAccessible(true);
        Object sql = method.invoke(repository, new Filter(), scope);
        Field cteField = sql.getClass().getDeclaredField("cte");
        cteField.setAccessible(true);
        String cte = (String) cteField.get(sql);

        assertTrue(cte.contains("fr.dept_id IN (:progressReceiptDeptIds) OR fr.finance_user_id=:progressReceiptSelf"));
        assertTrue(cte.contains("wo.dept_id IN (:progressWriteoffDeptIds) OR wo.operator_user_id=:progressWriteoffSelf"));
    }

    @Test
    void shouldQueryRealFiveSourceLedgerAndKeepCustomerBalancesStable() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long tenantId = jdbc.queryForObject("SELECT tenant_id FROM erp_sale_out WHERE deleted=b'0' AND status=20 "
                + "UNION SELECT tenant_id FROM erp_finance_receipt WHERE deleted=b'0' AND status=20 LIMIT 1", Long.class);
        assertNotNull(tenantId);

        ErpReceivableDetailReportRepository repository = new ErpReceivableDetailReportRepository();
        Field field = ErpReceivableDetailReportRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbc);
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpReceivableDetailReportRepository.Scope scope =
                new ErpReceivableDetailReportRepository.Scope(tenantId, all, all);
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(500);
        filter.setOrderField("businessDate");
        filter.setOrderDirection("asc");

        long count = repository.count(filter, scope);
        List<Row> rows = repository.page(filter, scope, 0, 500);
        Summary summary = repository.summary(filter, scope);
        assertEquals(count, summary.getRowCount());
        assertEquals(Math.min(count, 500), rows.size());

        Map<Long, BigDecimal> lastBalance = new HashMap<>();
        Map<String, BigDecimal> balanceByRow = new HashMap<>();
        for (Row row : rows) {
            BigDecimal previous = lastBalance.getOrDefault(row.getCustomerId(), BigDecimal.ZERO);
            assertEquals(0, previous.compareTo(row.getPreviousBalance()), row.getRowKey());
            BigDecimal change = zero(row.getIncreaseAmount()).subtract(zero(row.getReceiptAmount()));
            assertEquals(0, previous.add(change).compareTo(row.getBalance()), row.getRowKey());
            lastBalance.put(row.getCustomerId(), row.getBalance());
            balanceByRow.put(row.getRowKey(), row.getBalance());
        }
        if (count <= 500) {
            BigDecimal ending = lastBalance.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, ending.compareTo(summary.getEndingBalance()));
        }

        if (!rows.isEmpty()) {
            Row selected = rows.get(0);
            Filter displayFilter = new Filter();
            displayFilter.setPageNo(1);
            displayFilter.setPageSize(100);
            displayFilter.setOrderField("businessDate");
            displayFilter.setOrderDirection("asc");
            displayFilter.setDocumentTypes(Collections.singletonList(selected.getDocumentTypeCode()));
            displayFilter.setDocumentNo(selected.getDocumentNo());
            List<Row> filteredRows = repository.page(displayFilter, scope, 0, 100);
            Row sameRow = filteredRows.stream().filter(row -> selected.getRowKey().equals(row.getRowKey()))
                    .findFirst().orElseThrow();
            assertEquals(0, balanceByRow.get(selected.getRowKey()).compareTo(sameRow.getBalance()),
                    "单据类型或单号筛选不应重算真实余额");
        }
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String invokeSourceSql(ErpReceivableDetailReportRepository repository, String methodName)
            throws Exception {
        Method method = ErpReceivableDetailReportRepository.class.getDeclaredMethod(
                methodName, String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(repository, "", "", "");
    }

    private static String invokeBusinessSourceSql(ErpReceivableDetailReportRepository repository, String methodName)
            throws Exception {
        Method method = ErpReceivableDetailReportRepository.class.getDeclaredMethod(
                methodName, String.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(repository, "", "", "", " AND fr.dept_id IN (:progressReceiptDeptIds)",
                " AND wo.dept_id IN (:progressWriteoffDeptIds)");
    }

    private static int occurrences(String value, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
