package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErpPaymentDetailReportRepositoryTest {

    @Test
    void businessIdIsResponseOnlyAndDoesNotEnterExcel() throws Exception {
        assertTrue(Row.class.isAnnotationPresent(ExcelIgnoreUnannotated.class));
        assertFalse(Row.class.getDeclaredField("businessId").isAnnotationPresent(ExcelProperty.class));
    }

    @Test
    void shouldBuildManualAndAccountingVoucherFilters() throws Exception {
        Filter filter = filter();
        filter.setPaymentVoucherNo(" SUPPLIER-REF ");
        filter.setVoucherNo(" ACCOUNTING-REF ");
        ErpPaymentDetailReportRepository repository = new ErpPaymentDetailReportRepository();
        ErpPaymentDetailReportRepository.Scope scope = new ErpPaymentDetailReportRepository.Scope(
                1L, new ErpFinanceVisibleScope(true, Collections.emptyList(), null));

        Method method = ErpPaymentDetailReportRepository.class.getDeclaredMethod(
                "reportSql", Filter.class, ErpPaymentDetailReportRepository.Scope.class);
        method.setAccessible(true);
        Object sql = method.invoke(repository, filter, scope);
        Field cteField = sql.getClass().getDeclaredField("cte");
        Field whereField = sql.getClass().getDeclaredField("displayWhere");
        Field paramsField = sql.getClass().getDeclaredField("params");
        cteField.setAccessible(true);
        whereField.setAccessible(true);
        paramsField.setAccessible(true);
        String cte = (String) cteField.get(sql);
        String where = (String) whereField.get(sql);
        @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) paramsField.get(sql);

        assertTrue(cte.contains("p.payment_voucher_no"));
        assertTrue(cte.contains("v.voucher_no"));
        assertTrue(where.contains("report.payment_voucher_no LIKE :paymentVoucherNo"));
        assertTrue(where.contains("report.voucher_no LIKE :voucherNo"));
        assertEquals("%SUPPLIER-REF%", params.get("paymentVoucherNo"));
        assertEquals("%ACCOUNTING-REF%", params.get("voucherNo"));
    }

    @Test
    void shouldQueryEffectiveItemsAndKeepWholePaymentAllocationStable() throws Exception {
        String url = System.getenv("ERP_REPORT_TEST_DB_URL");
        assumeTrue(url != null && !url.trim().isEmpty(), "未配置报表集成测试数据库，跳过真实 SQL 校验");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getenv("ERP_REPORT_TEST_DB_USER"), System.getenv("ERP_REPORT_TEST_DB_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long tenantId = jdbc.queryForObject("SELECT tenant_id FROM erp_finance_payment_item"
                + " WHERE deleted=b'0' AND write_off_status=1 LIMIT 1", Long.class);
        assertNotNull(tenantId);

        ErpPaymentDetailReportRepository repository = new ErpPaymentDetailReportRepository();
        Field jdbcField = ErpPaymentDetailReportRepository.class.getDeclaredField("jdbcTemplate");
        jdbcField.setAccessible(true);
        jdbcField.set(repository, jdbc);
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpPaymentDetailReportRepository.Scope scope =
                new ErpPaymentDetailReportRepository.Scope(tenantId, all);
        Filter filter = filter();

        long count = repository.count(filter, scope);
        List<Row> rows = repository.page(filter, scope, 0, 500);
        Summary summary = repository.summary(filter, scope);
        assertEquals(count, summary.getRowCount());
        assertEquals(Math.min(count, 500), rows.size());
        assertTrue(rows.stream().allMatch(row -> row.getBusinessTypeCode() >= 11
                && row.getBusinessTypeCode() <= 14));
        assertTrue(rows.stream().allMatch(row -> row.getBusinessId() != null && row.getBusinessId() > 0),
                "付款明细必须返回业务单据 ID，供报表详情下钻使用");

        long invalidRows = jdbc.queryForObject("SELECT COUNT(*) FROM erp_finance_payment_item i"
                + " INNER JOIN erp_finance_payment p ON p.id=i.payment_id AND p.tenant_id=i.tenant_id"
                + " WHERE i.tenant_id=? AND i.id IN (" + placeholders(rows.size()) + ")"
                + " AND (i.deleted<>b'0' OR i.write_off_status<>1 OR p.deleted<>b'0' OR p.status<>20)",
                args(tenantId, rows), Long.class);
        assertEquals(0L, invalidRows);

        Map<Long, List<Row>> byPayment = rows.stream().collect(Collectors.groupingBy(Row::getPaymentId));
        for (Map.Entry<Long, List<Row>> entry : byPayment.entrySet()) {
            BigDecimal allocationTotal = entry.getValue().stream().map(Row::getSettlementAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (allocationTotal.compareTo(BigDecimal.ZERO) == 0) {
                assertTrue(entry.getValue().stream().allMatch(row -> row.getActualPaymentAmount() == null));
                continue;
            }
            BigDecimal headerActual = jdbc.queryForObject("SELECT payment_price FROM erp_finance_payment WHERE id=?",
                    BigDecimal.class, entry.getKey());
            BigDecimal allocatedActual = entry.getValue().stream().map(Row::getActualPaymentAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, headerActual.setScale(2).compareTo(allocatedActual), "paymentId=" + entry.getKey());
        }
        BigDecimal pageSettlement = rows.stream().map(Row::getSettlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pageActual = rows.stream().map(Row::getActualPaymentAmount)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (count <= 500) {
            assertEquals(0, pageSettlement.compareTo(summary.getSettlementAmount()));
            assertEquals(0, pageActual.compareTo(summary.getActualPaymentAmount()));
        }

        if (!rows.isEmpty()) {
            Row selected = rows.get(0);
            Filter displayFilter = filter();
            displayFilter.setBusinessNo(selected.getBusinessNo());
            List<Row> filtered = repository.page(displayFilter, scope, 0, 500);
            Row sameRow = filtered.stream().filter(row -> selected.getPaymentItemId().equals(row.getPaymentItemId()))
                    .findFirst().orElseThrow(IllegalStateException::new);
            if (selected.getActualPaymentAmount() == null) assertNull(sameRow.getActualPaymentAmount());
            else assertEquals(0, selected.getActualPaymentAmount().compareTo(sameRow.getActualPaymentAmount()),
                    "业务单号筛选不得重新分摊实付金额");
        }

        List<Map<String, Object>> allocationCases = jdbc.queryForList("WITH inputs AS ("
                + "SELECT 1 payment_id,1 item_id,CAST(60 AS DECIMAL(18,6)) settlement_amount,"
                + "CAST(89 AS DECIMAL(18,6)) payment_actual UNION ALL SELECT 1,2,40,89"
                + " UNION ALL SELECT 2,3,-100,-90 UNION ALL SELECT 3,4,50,20 UNION ALL SELECT 3,5,-50,20),"
                + "raw AS (SELECT inputs.*,SUM(settlement_amount) OVER(PARTITION BY payment_id) allocation_total,"
                + "ROW_NUMBER() OVER(PARTITION BY payment_id ORDER BY item_id DESC) allocation_rank FROM inputs),"
                + "allocated AS (SELECT raw.*,CASE WHEN allocation_total=0 THEN NULL WHEN allocation_rank=1 THEN"
                + " ROUND(payment_actual-COALESCE(SUM(CASE WHEN allocation_rank>1 THEN ROUND(payment_actual*"
                + "settlement_amount/allocation_total,6) ELSE 0 END) OVER(PARTITION BY payment_id),0),6) ELSE"
                + " ROUND(payment_actual*settlement_amount/allocation_total,6) END actual_amount FROM raw)"
                + " SELECT payment_id,SUM(actual_amount) actual_amount,COUNT(actual_amount) allocated_count"
                + " FROM allocated GROUP BY payment_id ORDER BY payment_id");
        assertEquals(0, decimal(allocationCases.get(0).get("actual_amount")).compareTo(new BigDecimal("89.00")));
        assertEquals(0, decimal(allocationCases.get(1).get("actual_amount")).compareTo(new BigDecimal("-90.00")));
        assertNull(allocationCases.get(2).get("actual_amount"));
        assertEquals(0L, ((Number) allocationCases.get(2).get("allocated_count")).longValue());
    }

    private static Filter filter() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(500);
        filter.setOrderField("paymentDate");
        filter.setOrderDirection("desc");
        return filter;
    }

    private static String placeholders(int size) {
        return size == 0 ? "NULL" : String.join(",", Collections.nCopies(size, "?"));
    }

    private static Object[] args(Long tenantId, List<Row> rows) {
        Map<Integer, Object> args = new LinkedHashMap<>();
        args.put(0, tenantId);
        for (int index = 0; index < rows.size(); index++) args.put(index + 1, rows.get(index).getPaymentItemId());
        return args.values().toArray();
    }

    private static BigDecimal decimal(Object value) {
        return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value));
    }
}
