package cn.iocoder.yudao.module.erp.service.stock.cost;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in integration checks against a newly created, isolated local test schema only. */
@EnabledIfEnvironmentVariable(named = "ERP_REPORT_MYSQL_TEST_URL", matches = ".+")
class ErpDualCostMysqlIntegrationTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private ErpDualCostPostingService service;
    private DataSourceTransactionManager transactionManager;
    private final LocalDateTime cutover = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void prepareIsolatedDatabase() throws Exception {
        String url = System.getenv("ERP_REPORT_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_foundation_test(?:\\?.*)?"),
                "Only the explicitly isolated fixture instance may be used");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "root", "");
        jdbc = new JdbcTemplate(dataSource);
        String dataDirectory = jdbc.queryForObject("SELECT @@datadir", String.class).replace('\\', '/');
        assertTrue(dataDirectory.contains("/Temp/xingyu-report-mysql-"),
                "Refuse to change tables unless the server uses our dedicated temporary data directory");
        transactionManager = new DataSourceTransactionManager(dataSource);
        transaction = new TransactionTemplate(transactionManager);
        String ddl = new String(Files.readAllBytes(Paths.get("../sql/mysql/erp_report_dual_cost_foundation_20260909.sql")), StandardCharsets.UTF_8);
        for (int repeat = 0; repeat < 2; repeat++) {
            for (String statement : ddl.split(";")) {
                if (!statement.trim().isEmpty()) jdbc.execute(statement);
            }
        }
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_stock (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, "
                + "product_id BIGINT NOT NULL, warehouse_id BIGINT NOT NULL, dept_id BIGINT, count DECIMAL(24,6), "
                + "cost_price DECIMAL(24,6), cost_amount DECIMAL(24,6), "
                + "deleted BIT NOT NULL DEFAULT 0, UNIQUE KEY stock_dimension(tenant_id,product_id,warehouse_id)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_stock_record (id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, "
                + "product_id BIGINT NOT NULL, warehouse_id BIGINT NOT NULL, deleted BIT NOT NULL DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("DELETE FROM erp_stock_dual_cost_posting");
        jdbc.execute("DELETE FROM erp_stock_dual_cost_balance");
        jdbc.execute("DELETE FROM erp_stock");
        jdbc.execute("DELETE FROM erp_stock_record");
        jdbc.update("INSERT INTO erp_stock VALUES (11,1,101,201,301,10,100,1000,0),(22,2,101,201,302,10,100,1000,0)");
        ErpDualCostLedgerRepository repository = new ErpDualCostLedgerRepository();
        ReflectionTestUtils.setField(repository, "jdbcTemplate", jdbc);
        service = new ErpDualCostPostingService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "cutover", cutover.toString());
        TenantContextHolder.setTenantId(1L);
        opening(1L);
        opening(2L);
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    private void opening(long tenant) {
        TenantContextHolder.setTenantId(tenant);
        transaction.execute(status -> {
            service.confirmOpening(101, 201, new BigDecimal("10"), new BigDecimal("1000"),
                    new BigDecimal("1200"), cutover, "Independently reconciled fixture", 99);
            return null;
        });
        TenantContextHolder.setTenantId(1L);
    }

    private ErpStockRecordCreateReqBO issue(long item, String count) {
        ErpStockRecordCreateReqBO request = new ErpStockRecordCreateReqBO(101L, 201L,
                new BigDecimal(count), 50, 401L, item, "SALE-401", null, cutover.plusDays(1));
        request.setAccountingDeptId(501L);
        return request;
    }

    private void post(long tenant, ErpStockRecordCreateReqBO request, AtomicInteger calls) {
        TenantContextHolder.setTenantId(tenant);
        try {
            transaction.execute(status -> service.post(request, () -> {
                calls.incrementAndGet();
                jdbc.update("UPDATE erp_stock SET count=count+? WHERE tenant_id=? AND product_id=101 AND warehouse_id=201",
                        request.getCount(), tenant);
                jdbc.update("INSERT INTO erp_stock_record(tenant_id,product_id,warehouse_id) VALUES (?,101,201)", tenant);
            }));
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void amount(String column, String expected, long tenant) {
        BigDecimal value = jdbc.queryForObject("SELECT " + column + " FROM erp_stock_dual_cost_balance WHERE tenant_id=?", BigDecimal.class, tenant);
        assertEquals(0, new BigDecimal(expected).compareTo(value), column);
    }

    @Test
    void independentCostsAndExactRetryPreserveSnapshot() {
        AtomicInteger calls = new AtomicInteger();
        post(1, issue(1, "-3"), calls);
        post(1, issue(1, "-3.000000"), calls);
        assertEquals(1, calls.get());
        amount("quantity", "7", 1);
        amount("financial_amount", "700", 1);
        amount("settlement_amount", "840", 1);
        Map<String, Object> event = jdbc.queryForMap("SELECT * FROM erp_stock_dual_cost_posting");
        assertEquals(0, new BigDecimal("-300").compareTo((BigDecimal) event.get("financial_movement")));
        assertEquals(0, new BigDecimal("-360").compareTo((BigDecimal) event.get("settlement_movement")));
        assertEquals(301L, ((Number) event.get("stock_dept_id")).longValue());
        assertEquals(501L, ((Number) event.get("accounting_dept_id")).longValue());
        assertNotNull(event.get("posted_at"));
        assertThrows(IllegalStateException.class, () -> post(1, issue(1, "-2"), calls));
        amount("quantity", "7", 1);
    }

    @Test
    void failureAfterLegacyInventoryWriteRollsBackEveryLedger() {
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> service.post(issue(1, "-3"), () -> {
            jdbc.update("UPDATE erp_stock SET count=7 WHERE tenant_id=1");
            throw new IllegalStateException("injected downstream accounting failure");
        })));
        amount("quantity", "10", 1);
        amount("financial_amount", "1000", 1);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting", Integer.class));
        assertEquals(0, new BigDecimal("10").compareTo(jdbc.queryForObject("SELECT count FROM erp_stock WHERE tenant_id=1", BigDecimal.class)));
        post(1, issue(1, "-3"), new AtomicInteger());
        amount("quantity", "7", 1);
    }

    @Test
    void concurrentSameActionWritesExactlyOnceUnderMysqlRepeatableRead() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Void> writer = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            post(1, issue(1, "-3"), calls);
            return null;
        };
        try {
            Future<Void> first = workers.submit(writer);
            Future<Void> second = workers.submit(writer);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
            assertEquals(1, calls.get());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting", Integer.class));
            amount("quantity", "7", 1);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void identicalBusinessIdentityInDifferentTenantsDoesNotCollide() {
        AtomicInteger calls = new AtomicInteger();
        post(1, issue(1, "-3"), calls);
        post(2, issue(1, "-3"), calls);
        assertEquals(2, calls.get());
        amount("quantity", "7", 1);
        amount("quantity", "7", 2);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting", Integer.class));
    }

    @Test
    void unconfirmedInclusivePurchasePriceDoesNotCreateAnyEntry() {
        ErpStockRecordCreateReqBO request = issue(1, "2");
        request.setBizType(70);
        request.setUnitPrice(new BigDecimal("113"));
        request.setSourcePriceBasis("INCLUSIVE_UNCONFIRMED");
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> post(1, request, calls));
        assertEquals(0, calls.get());
        amount("quantity", "10", 1);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting", Integer.class));
    }

    @Test
    void unchangedQuantityDoesNotHideStaleOpeningCostOrInterveningLegacyFlow() {
        jdbc.update("UPDATE erp_stock SET cost_price=101,cost_amount=1010 WHERE tenant_id=1");
        assertThrows(IllegalStateException.class, () -> post(1, issue(1, "-1"), new AtomicInteger()));
        jdbc.update("UPDATE erp_stock SET cost_price=100,cost_amount=1000 WHERE tenant_id=1");
        jdbc.update("INSERT INTO erp_stock_record(tenant_id,product_id,warehouse_id) VALUES (1,101,201)");
        assertThrows(IllegalStateException.class, () -> post(1, issue(1, "-1"), new AtomicInteger()));
        amount("quantity", "10", 1);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting", Integer.class));
    }

    @Test
    void callerCatchingPostingFailureStillCannotCommitPartialTransaction() {
        ProxyFactory factory = new ProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        ErpDualCostPostingService proxied = (ErpDualCostPostingService) factory.getProxy();
        assertThrows(UnexpectedRollbackException.class, () -> transaction.execute(status -> {
            try {
                proxied.post(issue(1, "-3"), () -> {
                    jdbc.update("UPDATE erp_stock SET count=7 WHERE tenant_id=1");
                    throw new IllegalStateException("downstream failure caught by caller");
                });
            } catch (IllegalStateException ignored) {
                // Deliberately reproduce an unsafe caller. The inner annotation must mark rollback-only.
            }
            return null;
        }));
        amount("quantity", "10", 1);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting", Integer.class));
    }

    @Test
    void waitingPosterReceivesTimestampAfterEarlierLockedPosting() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<?> first = workers.submit(() -> {
                TenantContextHolder.setTenantId(1L);
                try {
                    transaction.execute(status -> {
                        jdbc.queryForMap("SELECT id FROM erp_stock WHERE tenant_id=1 FOR UPDATE");
                        locked.countDown();
                        try {
                            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("lock wait timed out");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        post(1, issue(1, "-1"), new AtomicInteger());
                        return null;
                    });
                } finally {
                    TenantContextHolder.clear();
                }
            });
            assertTrue(locked.await(10, TimeUnit.SECONDS));
            Future<?> second = workers.submit(() -> post(1, issue(2, "-1"), new AtomicInteger()));
            boolean waiting = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                if (jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class) > 0) {
                    waiting = true;
                    break;
                }
                Thread.sleep(20);
            }
            assertTrue(waiting, "Second poster must actually be blocked by the first stock lock");
            release.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_stock_dual_cost_posting first_event "
                    + "JOIN erp_stock_dual_cost_posting later_event ON first_event.biz_item_id=1 AND later_event.biz_item_id=2 "
                    + "WHERE first_event.posted_at > later_event.posted_at OR first_event.id > later_event.id", Integer.class));
            amount("quantity", "8", 1);
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }
}
