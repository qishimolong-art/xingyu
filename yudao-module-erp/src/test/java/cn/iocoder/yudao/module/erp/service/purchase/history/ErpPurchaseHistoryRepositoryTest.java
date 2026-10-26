package cn.iocoder.yudao.module.erp.service.purchase.history;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryRespVO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseHistoryRepositoryTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseHistoryRepository repository;

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void countBuildsApprovedTenantSafeUnionWithIndependentDataScopes() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(3L);
        ErpPurchaseHistoryPageReqVO filter = filter();
        filter.setSupplierId(12L);
        filter.setBusinessTime(new LocalDateTime[]{
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59)
        });
        ErpFinanceVisibleScope inScope = new ErpFinanceVisibleScope(false, Arrays.asList(7L, 8L), 9L);
        ErpFinanceVisibleScope returnScope = new ErpFinanceVisibleScope(false, Collections.singleton(10L), 11L);

        assertEquals(3L, repository.count(filter,
                new ErpPurchaseHistoryRepository.Scope(1L, true, true, inScope, returnScope)));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForObject(sql.capture(), params.capture(), eq(Long.class));
        String value = sql.getValue();
        assertTrue(value.contains("erp_purchase_in_items"));
        assertTrue(value.contains("erp_purchase_return_items"));
        assertTrue(value.contains(" UNION ALL "));
        assertTrue(value.contains("CONCAT('PURCHASE_IN-',pii.id) AS row_key"));
        assertTrue(value.contains("CONCAT('PURCHASE_RETURN-',pri.id) AS row_key"));
        assertTrue(value.contains("pi.deleted=FALSE AND pi.status=20"));
        assertTrue(value.contains("pr.deleted=FALSE AND pr.status=20"));
        assertTrue(value.contains("pii.tenant_id=:tenantId AND pii.deleted=FALSE"));
        assertTrue(value.contains("pri.tenant_id=:tenantId AND pri.deleted=FALSE"));
        assertTrue(value.contains("-ABS(COALESCE(pri.count,0)) AS count"));
        assertTrue(value.contains("ABS(pri.product_price) AS product_price"));
        assertTrue(value.contains("pi.dept_id IN (:purchaseInDeptIds) OR pi.creator=:purchaseInSelfUserId"));
        assertTrue(value.contains("pr.dept_id IN (:purchaseReturnDeptIds) OR pr.creator=:purchaseReturnSelfUserId"));
        assertTrue(value.contains("history.supplier_id=:supplierId"));
        assertTrue(value.contains("history.business_time>=:businessTimeFrom"));
        assertTrue(value.contains("history.business_time<=:businessTimeTo"));
        assertEquals(1L, params.getValue().get("tenantId"));
        assertEquals(88L, params.getValue().get("productId"));
        assertEquals(12L, params.getValue().get("supplierId"));
    }

    @Test
    void pageCanQueryOnlyPermittedSourceAndRejectsOrderInjection() {
        when(jdbc.query(anyString(), anyMap(), org.mockito.ArgumentMatchers.<RowMapper<ErpPurchaseHistoryRespVO>>any()))
                .thenReturn(Collections.emptyList());
        ErpPurchaseHistoryPageReqVO filter = filter();
        filter.setOrderField("business_time desc; drop table erp_purchase_in");
        filter.setOrderDirection("asc");
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);

        repository.page(filter, new ErpPurchaseHistoryRepository.Scope(1L, true, false, all, null), 10, 10);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), anyMap(),
                org.mockito.ArgumentMatchers.<RowMapper<ErpPurchaseHistoryRespVO>>any());
        String value = sql.getValue();
        assertTrue(value.contains("erp_purchase_in_items"));
        assertFalse(value.contains("erp_purchase_return_items"));
        assertTrue(value.contains("ORDER BY history.business_time ASC,history.document_no ASC,history.item_id ASC"));
        assertTrue(value.contains("LIMIT :limit OFFSET :offset"));
        assertFalse(value.contains("drop table"));
    }

    @Test
    void documentTypeFilterCanLimitHistoryToPurchaseIn() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(2L);
        ErpPurchaseHistoryPageReqVO filter = filter();
        filter.setDocumentTypeCode("PURCHASE_IN");
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);

        assertEquals(2L, repository.count(filter,
                new ErpPurchaseHistoryRepository.Scope(1L, true, true, all, all)));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sql.capture(), anyMap(), eq(Long.class));
        assertTrue(sql.getValue().contains("erp_purchase_in_items"));
        assertFalse(sql.getValue().contains("erp_purchase_return_items"));
    }

    @Test
    void pageExecutesUnionBeforeGlobalCountSortAndPagination() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:purchase_history_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        createHistoryTablesAndRows(dataSource);
        ErpPurchaseHistoryRepository realRepository = new ErpPurchaseHistoryRepository();
        ReflectionTestUtils.setField(realRepository, "jdbc", new NamedParameterJdbcTemplate(dataSource));
        ErpFinanceVisibleScope all = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpPurchaseHistoryRepository.Scope scope =
                new ErpPurchaseHistoryRepository.Scope(1L, true, true, all, all);
        ErpPurchaseHistoryPageReqVO request = filter();

        assertEquals(3L, realRepository.count(request, scope));
        List<ErpPurchaseHistoryRespVO> firstPage = realRepository.page(request, scope, 0, 2);
        List<ErpPurchaseHistoryRespVO> secondPage = realRepository.page(request, scope, 2, 2);

        assertEquals(Arrays.asList("PURCHASE_IN-2", "PURCHASE_RETURN-1"),
                Arrays.asList(firstPage.get(0).getRowKey(), firstPage.get(1).getRowKey()));
        assertEquals("PURCHASE_IN-1", secondPage.get(0).getRowKey());
        assertEquals(new BigDecimal("-1.000000"), firstPage.get(1).getCount());
        assertEquals(new BigDecimal("11.000000"), firstPage.get(1).getProductPrice());
        assertEquals(3L, java.util.stream.Stream.concat(firstPage.stream(), secondPage.stream())
                .map(ErpPurchaseHistoryRespVO::getRowKey).distinct().count());

        request.setSupplierId(14L);
        request.setBusinessTime(new LocalDateTime[]{
                LocalDateTime.of(2026, 10, 2, 0, 0),
                LocalDateTime.of(2026, 10, 2, 23, 59, 59)
        });
        assertEquals(1L, realRepository.count(request, scope));
        List<ErpPurchaseHistoryRespVO> filtered = realRepository.page(request, scope, 0, 10);
        assertEquals("PURCHASE_RETURN", filtered.get(0).getDocumentTypeCode());
        assertEquals("CGTH-1", filtered.get(0).getDocumentNo());

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
    }

    private void createHistoryTablesAndRows(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE erp_purchase_in (id BIGINT PRIMARY KEY, tenant_id BIGINT,"
                    + " deleted BOOLEAN, status INT, no VARCHAR(50), supplier_id BIGINT, in_time TIMESTAMP,"
                    + " creator VARCHAR(50), dept_id BIGINT, remark VARCHAR(255))");
            statement.execute("CREATE TABLE erp_purchase_in_items (id BIGINT PRIMARY KEY, in_id BIGINT,"
                    + " tenant_id BIGINT, deleted BOOLEAN, product_id BIGINT, count DECIMAL(18,6),"
                    + " product_price DECIMAL(18,6), warehouse_id BIGINT)");
            statement.execute("CREATE TABLE erp_purchase_return (id BIGINT PRIMARY KEY, tenant_id BIGINT,"
                    + " deleted BOOLEAN, status INT, no VARCHAR(50), supplier_id BIGINT, return_time TIMESTAMP,"
                    + " creator VARCHAR(50), dept_id BIGINT, remark VARCHAR(255))");
            statement.execute("CREATE TABLE erp_purchase_return_items (id BIGINT PRIMARY KEY, return_id BIGINT,"
                    + " tenant_id BIGINT, deleted BOOLEAN, product_id BIGINT, count DECIMAL(18,6),"
                    + " product_price DECIMAL(18,6), warehouse_id BIGINT)");
            statement.execute("CREATE TABLE erp_supplier (id BIGINT, tenant_id BIGINT, deleted BOOLEAN, name VARCHAR(100))");
            statement.execute("CREATE TABLE erp_warehouse (id BIGINT, tenant_id BIGINT, deleted BOOLEAN, name VARCHAR(100))");
            statement.execute("CREATE TABLE system_users (id BIGINT, tenant_id BIGINT, deleted BOOLEAN, nickname VARCHAR(100))");
            statement.execute("INSERT INTO erp_supplier VALUES (14,1,FALSE,'供应商甲'),(15,1,FALSE,'供应商乙')");
            statement.execute("INSERT INTO erp_warehouse VALUES (8,1,FALSE,'主仓')");
            statement.execute("INSERT INTO system_users VALUES (9,1,FALSE,'开单人')");

            statement.execute("INSERT INTO erp_purchase_in VALUES "
                    + "(101,1,FALSE,20,'CGRK-1',14,TIMESTAMP '2026-10-01 09:00:00','9',7,'旧入库'),"
                    + "(102,1,FALSE,20,'CGRK-2',15,TIMESTAMP '2026-10-03 09:00:00','9',7,'新入库'),"
                    + "(103,1,FALSE,10,'CGRK-DRAFT',14,TIMESTAMP '2026-10-04 09:00:00','9',7,'未审核'),"
                    + "(104,2,FALSE,20,'CGRK-TENANT',14,TIMESTAMP '2026-10-05 09:00:00','9',7,'其他租户')");
            statement.execute("INSERT INTO erp_purchase_in_items VALUES "
                    + "(1,101,1,FALSE,88,2,10,8),(2,102,1,FALSE,88,4,12,8),"
                    + "(3,103,1,FALSE,88,5,13,8),(4,104,2,FALSE,88,6,14,8),"
                    + "(5,101,1,TRUE,88,7,15,8),(6,101,1,FALSE,99,8,16,8)");
            statement.execute("INSERT INTO erp_purchase_return VALUES "
                    + "(201,1,FALSE,20,'CGTH-1',14,TIMESTAMP '2026-10-02 09:00:00','9',7,'已审核退货'),"
                    + "(202,1,TRUE,20,'CGTH-DELETED',14,TIMESTAMP '2026-10-06 09:00:00','9',7,'已删除')");
            statement.execute("INSERT INTO erp_purchase_return_items VALUES "
                    + "(1,201,1,FALSE,88,1,-11,8),(2,202,1,FALSE,88,9,17,8)");
        }
    }

    private ErpPurchaseHistoryPageReqVO filter() {
        ErpPurchaseHistoryPageReqVO filter = new ErpPurchaseHistoryPageReqVO();
        filter.setPageNo(1);
        filter.setPageSize(10);
        filter.setProductId(88L);
        return filter;
    }
}
