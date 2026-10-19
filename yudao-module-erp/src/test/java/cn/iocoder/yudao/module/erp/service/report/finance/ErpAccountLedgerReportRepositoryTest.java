package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpAccountLedgerReportRepositoryTest {

    @Test
    void shouldBuildFiveSourcesAccountScopeHistoryBalanceAndDisplayFilters() throws Exception {
        Filter filter = filter();
        filter.setBusinessNo(" TEST ");
        filter.setAccountId(9L);
        filter.setReceiverId(11L);
        filter.setPaymentVoucherNo(" CUSTOMER-REF ");
        filter.setVoucherNo(" ACCOUNTING-REF ");
        filter.setOrderField("balance");
        filter.setOrderDirection("asc");
        ErpFinanceVisibleScope visibleScope = new ErpFinanceVisibleScope(
                false, Collections.singletonList(7L), 8L);
        ErpAccountLedgerReportRepository.Scope scope =
                new ErpAccountLedgerReportRepository.Scope(1L, visibleScope);
        ErpAccountLedgerReportRepository repository = new ErpAccountLedgerReportRepository();

        Method method = ErpAccountLedgerReportRepository.class.getDeclaredMethod(
                "reportSql", Filter.class, ErpAccountLedgerReportRepository.Scope.class, boolean.class);
        method.setAccessible(true);
        Object sql = method.invoke(repository, filter, scope, true);
        Field cteField = sql.getClass().getDeclaredField("cte");
        Field whereField = sql.getClass().getDeclaredField("fromWhere");
        Field paramsField = sql.getClass().getDeclaredField("params");
        cteField.setAccessible(true);
        whereField.setAccessible(true);
        paramsField.setAccessible(true);
        String cte = (String) cteField.get(sql);
        String where = (String) whereField.get(sql);
        @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) paramsField.get(sql);

        for (String table : new String[]{"erp_finance_receipt", "erp_finance_payment",
                "erp_receivable_other_income", "erp_payable_expense", "erp_finance_transfer"}) {
            assertTrue(cte.contains(table));
        }
        assertEquals(2, occurrences(cte, "FROM erp_finance_transfer"));
        assertTrue(cte.contains("ROUND(COALESCE(t.transfer_price,0)+COALESCE(t.fee_price,0),2)"));
        assertTrue(cte.contains("ROUND(COALESCE(t.transfer_price,0),2),CAST(NULL AS DECIMAL(24,2))"));
        assertTrue(cte.contains("SUM(COALESCE(s.income_amount,0)-COALESCE(s.expense_amount,0))"));
        assertTrue(cte.contains("PARTITION BY s.account_id"));
        assertTrue(cte.contains("a.dept_id IN (:accountDeptIds) OR CAST(a.creator AS UNSIGNED)=:accountSelf"));
        assertTrue(cte.contains("s.account_id=:accountId"));
        assertTrue(cte.contains("r.payment_voucher_no"));
        assertTrue(cte.contains("p.payment_voucher_no"));
        assertTrue(cte.contains("oi.voucher_no"));
        assertTrue(cte.contains("e.voucher_no"));
        assertTrue(cte.contains("oi.source_type IS NULL OR oi.source_type<>'付款单折让'"));
        assertTrue(cte.contains("e.source_type IS NULL OR e.source_type<>'收款单优惠'"));
        assertTrue(cte.contains("v.source_biz_type=CASE s.source_type"));
        assertFalse(cte.contains("q.business_no LIKE"), "展示筛选必须位于余额 CTE 之后");
        assertTrue(where.contains("q.business_no LIKE :businessNo"));
        assertTrue(where.contains("q.receiver_id=:receiverId"));
        assertTrue(where.contains("q.payment_voucher_no LIKE :paymentVoucherNo"));
        assertTrue(where.contains("q.voucher_no LIKE :voucherNo"));
        assertEquals("%TEST%", params.get("businessNo"));
        assertEquals("%CUSTOMER-REF%", params.get("paymentVoucherNo"));
        assertEquals("%ACCOUNTING-REF%", params.get("voucherNo"));
        assertEquals(9L, params.get("accountId"));
    }

    @Test
    void shouldQueryApprovedRowsKeepBalancesStableAndMatchSummary() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource source = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(source);
        Long tenantId = jdbc.queryForObject("SELECT tenant_id FROM erp_account WHERE deleted=b'0' LIMIT 1",
                Long.class);
        ErpAccountLedgerReportRepository repository = repository(jdbc);
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpAccountLedgerReportRepository.Scope scope =
                new ErpAccountLedgerReportRepository.Scope(tenantId, all);
        Filter filter = filter();

        long count = repository.count(filter, scope);
        List<Row> rows = repository.page(filter, scope, 0, 500);
        Summary summary = repository.summary(filter, scope);
        assertEquals(count, summary.getRowCount());
        assertEquals(Math.min(count, 500), rows.size());
        assertEquals(0, rows.stream().map(Row::getIncomeAmount).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(summary.getIncomeAmount()));
        assertEquals(0, rows.stream().map(Row::getExpenseAmount).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(summary.getExpenseAmount()));
        assertEquals(0, summary.getIncomeAmount().subtract(summary.getExpenseAmount())
                .compareTo(summary.getNetAmount()));

        Map<String, String> expectedTypes = new HashMap<>();
        expectedTypes.put("receipt", "收款单");
        expectedTypes.put("payment", "付款单");
        expectedTypes.put("other_income", "其他收入");
        expectedTypes.put("expense", "费用支出");
        expectedTypes.put("transfer", "银行转账");
        for (Row row : rows) {
            assertTrue(row.getSourceType().equals("receipt") || row.getSourceType().equals("payment")
                    || row.getSourceType().equals("other_income") || row.getSourceType().equals("expense")
                    || row.getSourceType().equals("transfer"));
            assertEquals(expectedTypes.get(row.getSourceType()), row.getDocumentType());
            assertTrue("借方".equals(row.getDirection()) || "贷方".equals(row.getDirection()));
        }
        if (!rows.isEmpty()) {
            Row selected = rows.get(rows.size() / 2);
            Filter selectedFilter = filter();
            selectedFilter.setBusinessNo(selected.getBusinessNo());
            Row filtered = repository.page(selectedFilter, scope, 0, 500).stream()
                    .filter(row -> selected.getRowKey().equals(row.getRowKey())).findFirst()
                    .orElseThrow(IllegalStateException::new);
            assertEquals(0, selected.getBalance().compareTo(filtered.getBalance()),
                    "业务单号筛选不得重新计算历史余额");
        }

        List<Map<String, Object>> cases = jdbc.queryForList("WITH ledger_inputs AS ("
                + "SELECT 1 account_id,1 seq,CAST(-10 AS DECIMAL(18,2)) income_amount,"
                + "CAST(NULL AS DECIMAL(18,2)) expense_amount UNION ALL "
                + "SELECT 1,2,NULL,CAST(7 AS DECIMAL(18,2)) UNION ALL "
                + "SELECT 2,1,CAST(100 AS DECIMAL(18,2)),NULL UNION ALL "
                + "SELECT 2,2,NULL,CAST(103 AS DECIMAL(18,2))), calculated AS ("
                + "SELECT account_id,seq,SUM(COALESCE(income_amount,0)-COALESCE(expense_amount,0)) "
                + "OVER(PARTITION BY account_id ORDER BY seq ROWS UNBOUNDED PRECEDING) balance FROM ledger_inputs) "
                + "SELECT account_id,MAX(CASE WHEN seq=2 THEN balance END) balance FROM calculated "
                + "GROUP BY account_id ORDER BY account_id");
        assertEquals(0, decimal(cases.get(0).get("balance")).compareTo(new BigDecimal("-17.00")));
        assertEquals(0, decimal(cases.get(1).get("balance")).compareTo(new BigDecimal("-3.00")));
    }

    @Test
    void shouldExcludePaymentDiscountOtherIncomeFromLedgerAndBalanceContribution() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource source = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                Map<String, Object> account = jdbc.queryForMap(
                        "SELECT id,tenant_id FROM erp_account WHERE deleted=b'0' LIMIT 1");
                Long accountId = ((Number) account.get("id")).longValue();
                Long tenantId = ((Number) account.get("tenant_id")).longValue();
                long sourceId = Math.abs(System.nanoTime());
                String no = "AUTO-NONCASH-" + sourceId;
                jdbc.update("INSERT INTO erp_receivable_other_income(no,status,approve_time,biz_time,settle_method,"
                                + "account_id,income_type,total_amount,source_type,source_id,source_no,doc_type,remark,"
                                + "creator,create_time,updater,update_time,deleted,tenant_id) "
                                + "VALUES(?,20,NOW(),NOW(),'挂账',?,'其他',10,'付款单折让',?,?,'正常单据',"
                                + "'集成测试非现金其他收入','1',NOW(),'1',NOW(),b'0',?)",
                        no, accountId, sourceId, no, tenantId);

                ErpAccountLedgerReportRepository repository = repository(jdbc);
                ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
                ErpAccountLedgerReportRepository.Scope scope =
                        new ErpAccountLedgerReportRepository.Scope(tenantId, all);
                Filter filter = filter();
                filter.setBusinessNo(no);
                assertEquals(0L, repository.count(filter, scope));
                assertTrue(repository.page(filter, scope, 0, 10).isEmpty());
                BigDecimal balanceContribution = jdbc.queryForObject(
                        "SELECT COALESCE(SUM(total_amount),0) FROM erp_receivable_other_income "
                                + "WHERE no=? AND deleted=b'0' AND status=20 "
                                + "AND (source_type IS NULL OR source_type<>'付款单折让')",
                        BigDecimal.class, no);
                assertEquals(0, BigDecimal.ZERO.compareTo(balanceContribution));
            } finally {
                connection.rollback();
            }
        }
    }

    private ErpAccountLedgerReportRepository repository(JdbcTemplate jdbc) throws Exception {
        ErpAccountLedgerReportRepository repository = new ErpAccountLedgerReportRepository();
        Field field = ErpAccountLedgerReportRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbc);
        return repository;
    }

    private Filter filter() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(500);
        return filter;
    }

    private int occurrences(String text, String pattern) {
        return (text.length() - text.replace(pattern, "").length()) / pattern.length();
    }

    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value));
    }
}
