package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Summary;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpOtherIncomeReportRepositoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildAllFiltersScopedConditionsAndWhitelistedSort() throws Exception {
        Filter filter = new Filter();
        filter.setPageNo(2);
        filter.setPageSize(20);
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 31, 23, 59, 59);
        filter.setApproveDate(new LocalDateTime[]{from, to});
        filter.setOccurrenceDate(new LocalDateTime[]{from, to});
        filter.setAccountId(21L);
        filter.setDocumentNo(" QTSR ");
        filter.setItemName(" 礼金 ");
        filter.setAmountMin(new BigDecimal("1.23"));
        filter.setAmountMax(new BigDecimal("99.87"));
        filter.setVoucherNo(" PZ ");
        filter.setSummary(" 摘要 ");
        filter.setBillingDeptId(23L);
        filter.setProjectDeptId(24L);
        filter.setHandlerId(25L);
        filter.setParty(" 对象 ");
        filter.setInvoice(" FP ");
        filter.setOrderField("amount");
        filter.setOrderDirection("asc");
        ErpFinanceVisibleScope visibleScope = new ErpFinanceVisibleScope(false, Arrays.asList(31L, 32L), 33L);
        ErpOtherIncomeReportRepository.Scope scope = new ErpOtherIncomeReportRepository.Scope(9L, visibleScope);

        ErpOtherIncomeReportRepository repository = new ErpOtherIncomeReportRepository();
        Method reportSqlMethod = ErpOtherIncomeReportRepository.class
                .getDeclaredMethod("reportSql", Filter.class, ErpOtherIncomeReportRepository.Scope.class);
        reportSqlMethod.setAccessible(true);
        Object sql = reportSqlMethod.invoke(repository, filter, scope);
        Field textField = sql.getClass().getDeclaredField("fromWhere");
        Field paramsField = sql.getClass().getDeclaredField("params");
        textField.setAccessible(true);
        paramsField.setAccessible(true);
        String text = (String) textField.get(sql);
        Map<String, Object> params = (Map<String, Object>) paramsField.get(sql);

        assertTrue(text.contains("e.tenant_id=:tenant AND e.deleted=b'0' AND e.status=20"));
        assertTrue(!text.contains("source_type"), "其他收入报表应包含付款单折让自动记录");
        assertTrue(text.contains("i.tenant_id=e.tenant_id AND i.deleted=b'0'"));
        assertTrue(text.contains("e.approve_time IS NOT NULL"));
        assertTrue(text.contains("e.dept_id IN (:mainDeptIds) OR e.handler_id=:mainSelf"));
        assertTrue(text.contains("i.dept_id IN (:itemDeptIds) OR i.handler_id=:itemSelf"));
        assertTrue(text.contains("e.approve_time>=:approveFrom AND e.approve_time<=:approveTo"));
        assertTrue(text.contains("i.biz_date>=:occurrenceFrom AND i.biz_date<=:occurrenceTo"));
        assertTrue(text.contains("e.account_id=:accountId"));
        assertTrue(text.contains("e.no LIKE :documentNo"));
        assertTrue(text.contains("i.item_name LIKE :itemName"));
        assertTrue(text.contains("i.amount>=:amountMin AND i.amount<=:amountMax"));
        assertTrue(text.contains("e.voucher_no LIKE :voucherNo"));
        assertTrue(text.contains("i.remark LIKE :summary"));
        assertTrue(text.contains("e.dept_id=:billingDeptId"));
        assertTrue(text.contains("i.dept_id=:projectDeptId"));
        assertTrue(text.contains("e.handler_id=:handlerId"));
        assertTrue(text.contains("e.party LIKE :party"));
        assertTrue(text.contains("i.invoice_no LIKE :invoice"));
        assertEquals(9L, params.get("tenant"));
        assertEquals(from, params.get("approveFrom"));
        assertEquals(to, params.get("occurrenceTo"));
        assertEquals("%QTSR%", params.get("documentNo"));
        assertEquals("%摘要%", params.get("summary"));
        assertEquals(Arrays.asList(31L, 32L), params.get("mainDeptIds"));
        assertEquals(Arrays.asList(31L, 32L), params.get("itemDeptIds"));
        assertEquals(33L, params.get("mainSelf"));
        assertEquals(33L, params.get("itemSelf"));

        Method orderByMethod = ErpOtherIncomeReportRepository.class.getDeclaredMethod("orderBy", Filter.class);
        orderByMethod.setAccessible(true);
        assertEquals(" ORDER BY i.amount ASC,e.approve_time DESC,e.id DESC,i.id DESC",
                orderByMethod.invoke(repository, filter));
        filter.setOrderField("amount; DROP TABLE erp_receivable_other_income");
        assertEquals(" ORDER BY e.approve_time DESC,e.id DESC,i.id DESC", orderByMethod.invoke(repository, filter));
    }

    @Test
    void shouldQueryApprovedItemsWithStrictMappingsSummaryAndIsolation() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource source = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                Long incomeId = jdbc.queryForObject("SELECT e.id FROM erp_receivable_other_income e "
                        + "JOIN erp_receivable_other_income_item i ON i.income_id=e.id AND i.tenant_id=e.tenant_id "
                        + "AND i.deleted=b'0' WHERE e.deleted=b'0' GROUP BY e.id HAVING COUNT(*)=1 ORDER BY e.id LIMIT 1",
                        Long.class);
                Long tenantId = jdbc.queryForObject(
                        "SELECT tenant_id FROM erp_receivable_other_income WHERE id=?", Long.class, incomeId);
                String documentNo = jdbc.queryForObject(
                        "SELECT no FROM erp_receivable_other_income WHERE id=?", String.class, incomeId);
                jdbc.update("UPDATE erp_receivable_other_income SET status=20,approve_time=?,voucher_no='PZ-ONLY-MAIN',"
                                + "remark='主单备注不得作为摘要',party='主单对象',source_type='付款单折让',"
                                + "source_id=id,source_no=no WHERE id=?",
                        LocalDateTime.of(2026, 10, 14, 9, 30), incomeId);
                jdbc.update("UPDATE erp_receivable_other_income_item SET item_name='礼金收入',amount=12.345,"
                                + "invoice_no='FP-001',biz_date=?,remark='明细摘要' WHERE income_id=? AND deleted=b'0'",
                        LocalDateTime.of(2026, 10, 10, 8, 0), incomeId);
                jdbc.update("INSERT INTO erp_receivable_other_income_item(income_id,item_name,amount,invoice_no,party,"
                                + "customer_id,dept_id,biz_date,handler_id,qty,freight_type,remark,file_url,creator,"
                                + "create_time,updater,update_time,deleted,tenant_id) SELECT income_id,'补贴收入',2.344,"
                                + "'FP-002',party,customer_id,dept_id,?,handler_id,qty,freight_type,'第二条摘要',file_url,"
                                + "creator,NOW(),updater,NOW(),b'0',tenant_id FROM erp_receivable_other_income_item "
                                + "WHERE income_id=? AND deleted=b'0' LIMIT 1",
                        LocalDateTime.of(2026, 10, 11, 8, 0), incomeId);
                jdbc.update("INSERT INTO erp_receivable_other_income_item(income_id,item_name,amount,create_time,"
                                + "update_time,deleted,tenant_id) VALUES(?,'逻辑删除项',999,NOW(),NOW(),b'1',?)",
                        incomeId, tenantId);

                ErpOtherIncomeReportRepository repository = repository(jdbc);
                ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
                ErpOtherIncomeReportRepository.Scope scope = new ErpOtherIncomeReportRepository.Scope(tenantId, all);
                Filter filter = filter(documentNo);
                List<Row> rows = repository.page(filter, scope, 0, 10);
                Summary summary = repository.summary(filter, scope);

                assertEquals(2L, repository.count(filter, scope));
                assertEquals(2, rows.size());
                assertEquals(2L, summary.getRowCount());
                assertEquals(0, new BigDecimal("14.69").compareTo(summary.getAmount()));
                Row first = rows.get(0);
                assertEquals(incomeId, first.getIncomeId());
                assertEquals(incomeId, rows.get(1).getIncomeId());
                assertEquals("2026-10-14", first.getApproveDate());
                assertEquals("PZ-ONLY-MAIN", first.getVoucherNo());
                assertEquals("主单对象", first.getParty());
                assertEquals("第二条摘要", first.getSummary());
                assertEquals("FP-002", first.getInvoice());
                assertEquals("2026-10-11", first.getOccurrenceDate());
                assertNull(first.getType());
                assertEquals(1, repository.page(filter, scope, 0, 1).size());
                assertEquals(rows.get(1).getItemId(), repository.page(filter, scope, 1, 1).get(0).getItemId());
                assertEquals(0L, repository.count(filter,
                        new ErpOtherIncomeReportRepository.Scope(Long.MAX_VALUE, all)));
                ErpFinanceVisibleScope denied =
                        new ErpFinanceVisibleScope(false, Collections.singletonList(Long.MAX_VALUE), null);
                assertEquals(0L, repository.count(filter,
                        new ErpOtherIncomeReportRepository.Scope(tenantId, denied)));

                Filter detailSummary = filter(documentNo);
                detailSummary.setSummary("明细摘要");
                assertEquals(1L, repository.count(detailSummary, scope));
                Filter mainSummary = filter(documentNo);
                mainSummary.setSummary("主单备注不得作为摘要");
                assertEquals(0L, repository.count(mainSummary, scope));

                Filter invoice = filter(documentNo);
                invoice.setInvoice("FP-001");
                assertEquals(1L, repository.count(invoice, scope));
                Filter occurrence = filter(documentNo);
                occurrence.setOccurrenceDate(new LocalDateTime[]{LocalDateTime.of(2026, 10, 11, 0, 0),
                        LocalDateTime.of(2026, 10, 11, 23, 59, 59)});
                assertEquals(1L, repository.count(occurrence, scope));

                jdbc.update("UPDATE erp_receivable_other_income SET status=10 WHERE id=?", incomeId);
                assertEquals(0L, repository.count(filter, scope));
                jdbc.update("UPDATE erp_receivable_other_income SET status=20,approve_time=NULL WHERE id=?", incomeId);
                assertEquals(0L, repository.count(filter, scope));
            } finally {
                connection.rollback();
            }
        }
    }

    private ErpOtherIncomeReportRepository repository(JdbcTemplate jdbc) throws Exception {
        ErpOtherIncomeReportRepository repository = new ErpOtherIncomeReportRepository();
        Field field = ErpOtherIncomeReportRepository.class.getDeclaredField("jdbcTemplate");
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
