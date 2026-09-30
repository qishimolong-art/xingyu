package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ErpMiscSettlementSqlTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void competingApprovalsSeeCommittedBalanceAfterOriginalLock(boolean receivable) throws Exception {
        try (Connection first = database(receivable)) {
            String m = receivable ? "receivable" : "payable", d = receivable ? "receipt" : "payment";
            execute(first, "INSERT INTO erp_" + m + "_misc(id,tenant_id,amount) VALUES (1,1,1000)");
            execute(first, "INSERT INTO erp_finance_" + d + "(id,tenant_id,source_" + m + "_misc_id," + d + "_price,status) VALUES (10,1,1,700,10),(11,1,1,400,10)");
            first.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            first.setAutoCommit(false);
            assertEquals(1000, number(first, "SELECT amount FROM erp_" + m + "_misc WHERE id=1 FOR UPDATE"));
            String url = first.getMetaData().getURL();
            java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CompletableFuture<Integer> remaining = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try (Connection second = DriverManager.getConnection(url, "sa", "")) {
                    second.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    second.setAutoCommit(false);
                    entered.countDown();
                    int original = number(second, "SELECT amount FROM erp_" + m + "_misc WHERE id=1 FOR UPDATE");
                    String rows = receivable ? ErpMiscSettlementSql.RECEIVABLE_ROWS : ErpMiscSettlementSql.PAYABLE_ROWS;
                    int settled = number(second, "SELECT COALESCE(SUM(amount),0) FROM (" + rows + ") s WHERE tenant_id=1 AND misc_id=1");
                    second.rollback();
                    return original - settled;
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            execute(first, "UPDATE erp_finance_" + d + " SET status=20 WHERE id=10");
            first.commit();
            assertEquals(300, remaining.get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(10, number(first, "SELECT status FROM erp_finance_" + d + " WHERE id=11"));
            Class<?> service = Class.forName("cn.iocoder.yudao.module.erp.service.finance.ErpFinance" + (receivable ? "Receipt" : "Payment") + "ServiceImpl");
            org.springframework.transaction.annotation.Transactional tx = service.getMethod("approveFinance" + (receivable ? "Receipt" : "Payment"), Long.class)
                    .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
            assertEquals(org.springframework.transaction.annotation.Isolation.READ_COMMITTED, tx.isolation());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reconcilesDirectItemsLegacyReversalAndTenant(boolean receivable) throws Exception {
        try (Connection c = database(receivable)) {
            String d = receivable ? "receipt" : "payment";
            String m = receivable ? "receivable" : "payable";
            int biz = receivable ? 24 : 14;
            String label = receivable ? "收款单转其他应收冲减" : "付款单转其他应付冲减";
            execute(c, "INSERT INTO erp_" + m + "_misc(id,tenant_id,amount) VALUES (1,1,1000),(2,1,-100),(1,2,9999)");
            // Approved direct 300, pending 900 and deleted 700 must not all be summed.
            execute(c, "INSERT INTO erp_finance_" + d + "(id,tenant_id,source_" + m + "_misc_id," + d + "_price,status,deleted) VALUES (10,1,1,300,20,0),(11,1,1,900,10,0),(12,1,1,700,20,1),(10,2,1,9999,20,0),(13,1,2,-30,20,0)");
            execute(c, "INSERT INTO erp_finance_" + d + "(id,tenant_id) VALUES (20,1),(21,1),(22,1)");
            execute(c, "INSERT INTO erp_finance_" + d + "_item(id,tenant_id," + d + "_id,biz_type,biz_id," + d + "_price,write_off_status) VALUES (1,1,10," + biz + ",1,300,1),(2,1,20," + biz + ",1,40,1),(3,1,20," + biz + ",1,60,1),(4,1,21," + biz + ",1,200,2)");
            // Mirror offsets for direct, items and reversed items; only source 22 is fallback.
            execute(c, "INSERT INTO erp_" + m + "_misc(id,tenant_id,amount,source_type,source_misc_id,source_id) VALUES (101,1,-300,'" + label + "',1,10),(102,1,-100,'" + label + "',1,20),(103,1,-200,'" + label + "',1,21),(104,1,-50,'" + label + "',1,22)");
            String rows = receivable ? ErpMiscSettlementSql.RECEIVABLE_ROWS : ErpMiscSettlementSql.PAYABLE_ROWS;
            String query = "SELECT SUM(amount) FROM (" + rows + ") s WHERE tenant_id=1 AND misc_id=1";
            assertEquals(450, number(c, query));
            assertEquals(3, number(c, "SELECT COUNT(*) FROM (" + rows + ") s WHERE tenant_id=1 AND misc_id=1"));
            assertEquals(-30, number(c, "SELECT SUM(amount) FROM (" + rows + ") s WHERE tenant_id=1 AND misc_id=2"));
            // Real tenant SQL rewriting must preserve the unions and correlated NOT EXISTS.
            TenantLineInnerInterceptor tenant = new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override public Expression getTenantId() { return new LongValue(1); }
            });
            assertEquals(450, number(c, tenant.parserSingle(query, null)));
            String ledger = receivable ? ErpMiscSettlementSql.RECEIVABLE_LEDGER : ErpMiscSettlementSql.PAYABLE_LEDGER;
            assertEquals(480, number(c, "SELECT SUM(amount) FROM " + ledger + " x WHERE tenant_id=1"));
            assertEquals(1000 - 100, number(c, "SELECT SUM(amount) FROM " + ledger + " x WHERE tenant_id=1 AND biz_time < '2026-09-02'"));
            assertEquals(-420, number(c, "SELECT SUM(amount) FROM " + ledger + " x WHERE tenant_id=1 AND biz_time >= '2026-09-02'"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rendersAndExecutesPagedDetailAndTotals(boolean receivable) throws Exception {
        Class<?> mapper = Class.forName("cn.iocoder.yudao.module.erp.dal.mysql.finance." + (receivable ? "receivable.ErpReceivableMiscMapper" : "payable.ErpPayableMiscMapper"));
        try (Connection c = database(receivable)) {
            for (java.lang.reflect.Method method : mapper.getMethods()) {
                if (!method.getName().equals("selectSettlementPage") && !method.getName().equals("selectSettlementTotals")) continue;
                String script = String.join(" ", method.getAnnotation(org.apache.ibatis.annotations.Select.class).value());
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("id", 1L);
                params.put("ids", java.util.Collections.singleton(1L));
                org.apache.ibatis.mapping.BoundSql sql = new org.apache.ibatis.scripting.xmltags.XMLLanguageDriver()
                        .createSqlSource(new org.apache.ibatis.session.Configuration(), script, java.util.Map.class).getBoundSql(params);
                try (PreparedStatement statement = c.prepareStatement(sql.getSql())) {
                    for (int i = 1; i <= sql.getParameterMappings().size(); i++) statement.setLong(i, 1L);
                    try (ResultSet result = statement.executeQuery()) { assertFalse(result.next()); }
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pendingReservationsRespectStatusDeletionTenantAndSelfExclusion(boolean receivable) throws Exception {
        try (Connection c = database(receivable)) {
            String m = receivable ? "receivable" : "payable", d = receivable ? "receipt" : "payment";
            execute(c, "INSERT INTO erp_" + m + "_misc(id,tenant_id,amount) VALUES (1,1,1000),(1,2,1000)");
            execute(c, "INSERT INTO erp_finance_" + d + "(id,tenant_id,source_" + m + "_misc_id," + d + "_price,status,deleted) VALUES "
                    + "(10,1,1,300,10,0),(11,1,1,200,10,0),(12,1,1,900,0,0),(13,1,1,800,30,0),(14,1,1,700,10,1),(15,1,1,100,20,0),(16,2,1,9999,10,0)");
            assertEquals(500, pending(c, receivable, null));
            assertEquals(0, pendingValue(c, receivable, null, true));
            execute(c, "UPDATE erp_finance_" + d + " SET " + d + "_price=-200 WHERE id=11 AND tenant_id=1");
            assertEquals(1, pendingValue(c, receivable, null, true));
            execute(c, "UPDATE erp_finance_" + d + " SET " + d + "_price=200 WHERE id=11 AND tenant_id=1");
            assertEquals(200, pending(c, receivable, 10L));
            execute(c, "UPDATE erp_finance_" + d + " SET status=20 WHERE id=10 AND tenant_id=1");
            assertEquals(200, pending(c, receivable, null));
            execute(c, "UPDATE erp_finance_" + d + " SET deleted=1 WHERE id=11 AND tenant_id=1");
            assertEquals(0, pending(c, receivable, null));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void competingSubmissionsSeeReservationAfterOriginalLock(boolean receivable) throws Exception {
        try (Connection first = database(receivable)) {
            String m = receivable ? "receivable" : "payable", d = receivable ? "receipt" : "payment";
            execute(first, "INSERT INTO erp_" + m + "_misc(id,tenant_id,amount) VALUES (1,1,1000)");
            first.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            first.setAutoCommit(false);
            number(first, "SELECT amount FROM erp_" + m + "_misc WHERE id=1 FOR UPDATE");
            String url = first.getMetaData().getURL();
            java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CompletableFuture<Integer> remaining = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try (Connection second = DriverManager.getConnection(url, "sa", "")) {
                    second.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    second.setAutoCommit(false);
                    entered.countDown();
                    int original = number(second, "SELECT amount FROM erp_" + m + "_misc WHERE id=1 FOR UPDATE");
                    int available = original - pending(second, receivable, null);
                    second.rollback();
                    return available;
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            execute(first, "INSERT INTO erp_finance_" + d + "(id,tenant_id,source_" + m + "_misc_id," + d + "_price,status) VALUES (10,1,1,700,10)");
            first.commit();
            assertEquals(300, remaining.get(5, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    private static int pending(Connection c, boolean receivable, Long excludeId) throws Exception {
        return pendingValue(c, receivable, excludeId, false);
    }

    private static int pendingValue(Connection c, boolean receivable, Long excludeId, boolean invalid) throws Exception {
        Class<?> mapper = Class.forName("cn.iocoder.yudao.module.erp.dal.mysql.finance." + (receivable ? "receivable.ErpReceivableMiscMapper" : "payable.ErpPayableMiscMapper"));
        String script = String.join(" ", mapper.getMethod("selectPendingTransferTotals", java.util.Collection.class, Long.class)
                .getAnnotation(org.apache.ibatis.annotations.Select.class).value());
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("ids", java.util.Collections.singleton(1L));
        params.put("excludeId", excludeId);
        org.apache.ibatis.mapping.BoundSql sql = new org.apache.ibatis.scripting.xmltags.XMLLanguageDriver()
                .createSqlSource(new org.apache.ibatis.session.Configuration(), script, java.util.Map.class).getBoundSql(params);
        TenantLineInnerInterceptor tenant = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() { return new LongValue(1); }
        });
        try (PreparedStatement statement = c.prepareStatement(tenant.parserSingle(sql.getSql(), null))) {
            int parameter = 1;
            if (excludeId != null) statement.setLong(parameter++, excludeId);
            statement.setLong(parameter, 1L);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt(invalid ? "invalid_count" : "pending_amount");
            }
        }
    }

    static Connection database(boolean receivable) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        String m = receivable ? "receivable" : "payable", d = receivable ? "receipt" : "payment", party = receivable ? "customer" : "supplier";
        execute(c, "CREATE TABLE erp_" + m + "_misc (id BIGINT, tenant_id BIGINT, deleted INT DEFAULT 0, status INT DEFAULT 20, amount DECIMAL(18,2), source_type VARCHAR(100), source_id BIGINT, source_misc_id BIGINT, no VARCHAR(64), remark VARCHAR(255), file_url VARCHAR(255), " + party + "_id BIGINT DEFAULT 1, dept_id BIGINT DEFAULT 1, handler_id BIGINT DEFAULT 1, biz_time TIMESTAMP DEFAULT '2026-09-01 00:00:00')");
        execute(c, "CREATE TABLE erp_finance_" + d + " (id BIGINT, tenant_id BIGINT, deleted INT DEFAULT 0, status INT DEFAULT 20, source_" + m + "_misc_id BIGINT, " + d + "_price DECIMAL(18,2), " + d + "_time TIMESTAMP DEFAULT '2026-09-03 00:00:00', dept_id BIGINT DEFAULT 1, finance_user_id BIGINT DEFAULT 1, no VARCHAR(64), account_id BIGINT, remark VARCHAR(255))");
        execute(c, "CREATE TABLE erp_finance_" + d + "_item (id BIGINT, tenant_id BIGINT, deleted INT DEFAULT 0, " + d + "_id BIGINT, biz_type INT, biz_id BIGINT, " + d + "_price DECIMAL(18,2), write_off_status INT)");
        return c;
    }

    static void execute(Connection c, String sql) throws Exception { try (Statement s = c.createStatement()) { s.execute(sql); } }
    static int number(Connection c, String sql) throws Exception { try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) { assertTrue(rs.next()); return rs.getBigDecimal(1).intValueExact(); } }
}
