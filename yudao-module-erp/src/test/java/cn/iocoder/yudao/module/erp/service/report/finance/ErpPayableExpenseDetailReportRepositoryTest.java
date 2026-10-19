package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpPayableExpenseDetailReportRepositoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildAllFiltersScopedConditionsAndWhitelistedSort() throws Exception {
        Filter filter = new Filter();
        filter.setPageNo(2);
        filter.setPageSize(20);
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 31, 23, 59, 59);
        filter.setApproveDate(new LocalDateTime[]{from, to});
        filter.setAccountId(21L);
        filter.setDocumentNo(" FYZF ");
        filter.setItemName(" 差旅 ");
        filter.setAmountMin(new BigDecimal("1.23"));
        filter.setAmountMax(new BigDecimal("99.87"));
        filter.setVoucherNo(" PZ ");
        filter.setSummary(" 摘要 ");
        filter.setHandlerId(22L);
        filter.setParty(" 对象 ");
        filter.setBillingDeptId(23L);
        filter.setDeptId(24L);
        filter.setBusinessUserId(25L);
        filter.setOrderField("amount");
        filter.setOrderDirection("asc");
        ErpFinanceVisibleScope visibleScope = new ErpFinanceVisibleScope(false, Arrays.asList(31L, 32L), 33L);
        ErpPayableExpenseDetailReportRepository.Scope scope =
                new ErpPayableExpenseDetailReportRepository.Scope(9L, visibleScope);

        ErpPayableExpenseDetailReportRepository repository = new ErpPayableExpenseDetailReportRepository();
        Method reportSqlMethod = ErpPayableExpenseDetailReportRepository.class
                .getDeclaredMethod("reportSql", Filter.class, ErpPayableExpenseDetailReportRepository.Scope.class);
        reportSqlMethod.setAccessible(true);
        Object sql = reportSqlMethod.invoke(repository, filter, scope);
        Field textField = sql.getClass().getDeclaredField("fromWhere");
        Field paramsField = sql.getClass().getDeclaredField("params");
        textField.setAccessible(true);
        paramsField.setAccessible(true);
        String text = (String) textField.get(sql);
        Map<String, Object> params = (Map<String, Object>) paramsField.get(sql);

        assertTrue(text.contains("e.tenant_id=:tenant AND e.deleted=b'0' AND e.status=20"));
        assertFalse(text.contains("source_type"), "费用明细报表必须包含收款单优惠自动费用");
        assertTrue(text.contains("i.deleted=b'0'"));
        assertTrue(text.contains("e.approve_time IS NOT NULL"));
        assertTrue(text.contains("e.dept_id IN (:mainDeptIds) OR e.handler_id=:mainSelf"));
        assertTrue(text.contains("i.dept_id IN (:itemDeptIds) OR i.handler_id=:itemSelf"));
        assertTrue(text.contains("e.approve_time>=:approveFrom AND e.approve_time<=:approveTo"));
        assertTrue(text.contains("e.account_id=:accountId"));
        assertTrue(text.contains("e.no LIKE :documentNo"));
        assertTrue(text.contains("i.item_name LIKE :itemName"));
        assertTrue(text.contains("i.amount>=:amountMin AND i.amount<=:amountMax"));
        assertTrue(text.contains("e.voucher_no LIKE :voucherNo"));
        assertTrue(text.contains("e.remark LIKE :summary"));
        assertTrue(text.contains("e.handler_id=:handlerId"));
        assertTrue(text.contains("e.party LIKE :party"));
        assertTrue(text.contains("e.dept_id=:billingDeptId"));
        assertTrue(text.contains("i.dept_id=:deptId"));
        assertTrue(text.contains("i.handler_id=:businessUserId"));
        assertEquals(9L, params.get("tenant"));
        assertEquals(from, params.get("approveFrom"));
        assertEquals(to, params.get("approveTo"));
        assertEquals("%FYZF%", params.get("documentNo"));
        assertEquals("%差旅%", params.get("itemName"));
        assertEquals("%PZ%", params.get("voucherNo"));
        assertEquals("%摘要%", params.get("summary"));
        assertEquals("%对象%", params.get("party"));
        assertEquals(Arrays.asList(31L, 32L), params.get("mainDeptIds"));
        assertEquals(Arrays.asList(31L, 32L), params.get("itemDeptIds"));
        assertEquals(33L, params.get("mainSelf"));
        assertEquals(33L, params.get("itemSelf"));

        Method orderByMethod = ErpPayableExpenseDetailReportRepository.class
                .getDeclaredMethod("orderBy", Filter.class);
        orderByMethod.setAccessible(true);
        assertEquals(" ORDER BY i.amount ASC,e.approve_time DESC,e.no DESC,i.id ASC",
                orderByMethod.invoke(repository, filter));
        filter.setOrderField("amount; DROP TABLE erp_payable_expense");
        assertEquals(" ORDER BY e.approve_time DESC,e.no DESC,i.id ASC",
                orderByMethod.invoke(repository, filter));
    }

    @Test
    void shouldQueryApprovedItemsWithStrictMappingsFiltersSummaryAndIsolation() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource source = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                Long expenseId = jdbc.queryForObject("SELECT e.id FROM erp_payable_expense e WHERE e.deleted=b'0' "
                        + "AND EXISTS (SELECT 1 FROM erp_payable_expense_item i WHERE i.expense_id=e.id "
                        + "AND i.tenant_id=e.tenant_id AND i.deleted=b'0') ORDER BY e.id LIMIT 1", Long.class);
                String documentNo = jdbc.queryForObject(
                        "SELECT no FROM erp_payable_expense WHERE id=?", String.class, expenseId);
                Long tenantId = jdbc.queryForObject(
                        "SELECT tenant_id FROM erp_payable_expense WHERE id=?", Long.class, expenseId);
                jdbc.update("UPDATE erp_payable_expense SET status=20,approve_time=? WHERE id=?",
                        LocalDateTime.of(2026, 10, 14, 9, 30), expenseId);
                jdbc.update("INSERT INTO erp_payable_expense_item(expense_id,item_name,amount,invoice_no,party,"
                                + "dept_id,biz_date,handler_id,qty,expense_category,remark,file_url,creator,create_time,"
                                + "updater,update_time,deleted,tenant_id) SELECT expense_id,CONCAT(item_name,'-第二项'),"
                                + "2.34,invoice_no,party,dept_id,biz_date,handler_id,qty,expense_category,remark,file_url,"
                                + "creator,NOW(),updater,NOW(),b'0',tenant_id FROM erp_payable_expense_item "
                                + "WHERE expense_id=? AND deleted=b'0' ORDER BY id LIMIT 1", expenseId);
                jdbc.update("INSERT INTO erp_payable_expense_item(expense_id,item_name,amount,create_time,update_time,"
                                + "deleted,tenant_id) VALUES(?, '逻辑删除项', 999, NOW(), NOW(), b'1', ?)",
                        expenseId, tenantId);

                ErpPayableExpenseDetailReportRepository repository = repository(jdbc);
                ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
                ErpPayableExpenseDetailReportRepository.Scope scope =
                        new ErpPayableExpenseDetailReportRepository.Scope(tenantId, all);
                Filter filter = filter(documentNo);

                long count = repository.count(filter, scope);
                List<Row> rows = repository.page(filter, scope, 0, 10);
                Summary summary = repository.summary(filter, scope);
                assertEquals(2L, count);
                assertEquals(2, rows.size());
                assertEquals(2L, summary.getRowCount());
                assertEquals(0, rows.stream().map(Row::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .compareTo(summary.getAmount()));
                assertEquals("2026-10-14", rows.get(0).getApproveDate());
                assertNull(rows.get(0).getType());
                assertNull(rows.get(0).getInvoice());
                assertNull(rows.get(0).getOccurrenceDate());
                assertTrue(rows.get(0).getItemId() < rows.get(1).getItemId());
                assertEquals(1, repository.page(filter, scope, 0, 1).size());
                assertEquals(rows.get(1).getItemId(), repository.page(filter, scope, 1, 1).get(0).getItemId());
                assertEquals(0L, repository.count(filter,
                        new ErpPayableExpenseDetailReportRepository.Scope(Long.MAX_VALUE, all)));
                ErpFinanceVisibleScope denied =
                        new ErpFinanceVisibleScope(false, Collections.singletonList(Long.MAX_VALUE), null);
                assertEquals(0L, repository.count(filter,
                        new ErpPayableExpenseDetailReportRepository.Scope(tenantId, denied)));

                Row original = rows.stream().filter(row -> !row.getItemName().endsWith("-第二项"))
                        .findFirst().orElseThrow(IllegalStateException::new);
                String mainDept = jdbc.queryForObject("SELECT d.name FROM erp_payable_expense e "
                        + "LEFT JOIN system_dept d ON d.id=e.dept_id AND d.deleted=b'0' WHERE e.id=?", String.class,
                        expenseId);
                String itemDept = jdbc.queryForObject("SELECT d.name FROM erp_payable_expense_item i "
                        + "LEFT JOIN system_dept d ON d.id=i.dept_id AND d.deleted=b'0' WHERE i.id=?", String.class,
                        original.getItemId());
                assertEquals(mainDept, original.getBillingDeptName());
                assertEquals(itemDept, original.getDeptName());

                Filter amountFilter = filter(documentNo);
                amountFilter.setAmountMin(new BigDecimal("2.34"));
                amountFilter.setAmountMax(new BigDecimal("2.34"));
                List<Row> amountRows = repository.page(amountFilter, scope, 0, 10);
                assertEquals(1, amountRows.size());
                assertTrue(amountRows.get(0).getItemName().endsWith("-第二项"));

                Filter amountSort = filter(documentNo);
                amountSort.setOrderField("amount");
                amountSort.setOrderDirection("desc");
                List<Row> amountSortedRows = repository.page(amountSort, scope, 0, 10);
                assertTrue(amountSortedRows.get(0).getAmount().compareTo(amountSortedRows.get(1).getAmount()) >= 0);

                jdbc.update("UPDATE erp_payable_expense SET status=10 WHERE id=?", expenseId);
                assertEquals(0L, repository.count(filter, scope));
                jdbc.update("UPDATE erp_payable_expense SET status=20,approve_time=NULL WHERE id=?", expenseId);
                assertEquals(0L, repository.count(filter, scope));
            } finally {
                connection.rollback();
            }
        }
    }

    private ErpPayableExpenseDetailReportRepository repository(JdbcTemplate jdbc) throws Exception {
        ErpPayableExpenseDetailReportRepository repository = new ErpPayableExpenseDetailReportRepository();
        Field field = ErpPayableExpenseDetailReportRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbc);
        return repository;
    }

    private Filter filter(String documentNo) {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(10);
        filter.setDocumentNo(documentNo);
        return filter;
    }
}
