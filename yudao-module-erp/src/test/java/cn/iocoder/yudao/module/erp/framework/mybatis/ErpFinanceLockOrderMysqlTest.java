package cn.iocoder.yudao.module.erp.framework.mybatis;

import cn.iocoder.yudao.module.erp.dal.mysql.finance.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.*;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.*;
import cn.iocoder.yudao.module.erp.service.finance.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** 仅允许本机已存在的隔离测试库；普通用例全部回滚，锁用例的唯一已提交行精确清理。 */
@EnabledIfSystemProperty(named = "finance.lock.mysql", matches = "true")
class ErpFinanceLockOrderMysqlTest {
    private static final long ID = 962600001L;
    private DriverManagerDataSource dataSource;
    private SqlSessionFactory factory;
    private SqlSession session;

    @BeforeEach
    void setup() throws Exception {
        Path root = Paths.get("../..").toRealPath();
        Map<?, ?> config = new ObjectMapper().readValue(
                root.resolve(".local/erp-assistant/test-connection.json").toFile(), Map.class);
        assertEquals("127.0.0.1", config.get("host"));
        assertEquals(13316, config.get("port"));
        assertEquals("erp_assistant_test", config.get("database"));
        dataSource = new DriverManagerDataSource(
                "jdbc:mysql://127.0.0.1:13316/erp_assistant_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                (String) config.get("user"), (String) config.get("password"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals(13316, jdbc.queryForObject("SELECT @@port", Integer.class));
        assertEquals("erp_assistant_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals(root.resolve(".local/erp-assistant/mysql").toRealPath(),
                Paths.get(jdbc.queryForObject("SELECT @@datadir", String.class)).toRealPath());

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        MybatisPlusInterceptor chain = new MybatisPlusInterceptor();
        chain.addInnerInterceptor(ErpFinanceLockOrderInterceptorTest.tenant());
        chain.addInnerInterceptor(ErpFinanceLockOrderInterceptorTest.permission());
        new ErpFinanceMybatisConfiguration().erpFinanceLockOrderRegistration(chain).afterSingletonsInstantiated();
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setConfiguration(configuration);
        // 独立 SqlSession 没有 Spring 事务上下文，必须显式使用 JDBC 事务，确保测试回滚有效。
        bean.setTransactionFactory(new JdbcTransactionFactory());
        bean.setGlobalConfig(new GlobalConfig().setMetaObjectHandler(new DefaultDBFieldHandler(
                new DefaultListableBeanFactory().getBeanProvider(PermissionCommonApi.class))));
        bean.setPlugins(chain);
        factory = bean.getObject();
        for (Class<?> mapper : Arrays.asList(ErpFinancePaymentItemMapper.class, ErpFinanceReceiptItemMapper.class,
                ErpFinancePaymentMapper.class, ErpFinanceReceiptMapper.class,
                ErpPayableMiscMapper.class, ErpReceivableMiscMapper.class)) {
            factory.getConfiguration().addMapper(mapper);
        }
        session = factory.openSession(false);
        assertFalse(session.getConnection().getAutoCommit());
    }

    @AfterEach
    void cleanup() {
        if (session != null) {
            session.rollback(true);
            session.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"payment", "receipt"})
    void realMapperFiltersTenantDeletedAndPermissionAndSorts(String kind) throws Exception {
        String table = "erp_finance_" + kind + "_item";
        Connection connection = session.getConnection();
        // 倒序插入，另含其他租户、已删除、权限外以及其他单据的明细。
        for (int i : new int[]{1, 0, 2, 3, 4, 5}) {
            insert(connection, table, "id", ID + i, kind + "_id", i == 5 ? ID + 100 : ID,
                    "tenant_id", i == 2 ? 2 : 1, "deleted", i == 3 ? 1 : 0,
                    "creator", i == 4 ? "102" : "101");
        }
        assertEquals(Arrays.asList(ID, ID + 1), lockedIds(kind, ID));
        assertTrue(lockedIds(kind, ID + 200).isEmpty());
        // 同一 Mapper 的普通 selectList 查询保持正常。
        assertEquals(2, "payment".equals(kind)
                ? session.getMapper(ErpFinancePaymentItemMapper.class).selectListByPaymentId(ID).size()
                : session.getMapper(ErpFinanceReceiptItemMapper.class).selectListByReceiptId(ID).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"payment", "receipt"})
    void selectedRowsRemainLockedUntilTransactionEnds(String kind) throws Exception {
        String table = "erp_finance_" + kind + "_item";
        boolean inserted = false;
        try {
            // 先提交唯一的测试行，避免把 INSERT 锁误判为 SELECT FOR UPDATE 锁。
            try (Connection seed = dataSource.getConnection()) {
                seed.setAutoCommit(true);
                insert(seed, table, "id", ID, kind + "_id", ID, "creator", "101", "tenant_id", 1);
                inserted = true;
            }
            assertEquals(Collections.singletonList(ID), lockedIds(kind, ID));
            try (Connection contender = dataSource.getConnection()) {
                contender.setAutoCommit(false);
                try (Statement statement = contender.createStatement()) {
                    statement.execute("SET SESSION innodb_lock_wait_timeout = 1");
                }
                try (PreparedStatement update = contender.prepareStatement(
                        "UPDATE " + table + " SET remark = ? WHERE id = ? AND tenant_id = 1 AND deleted = 0")) {
                    update.setString(1, "lock-test");
                    update.setLong(2, ID);
                    SQLException blocked = assertThrows(SQLException.class, update::executeUpdate);
                    assertEquals(1205, blocked.getErrorCode());
                    contender.rollback();
                    session.rollback(true);
                    assertEquals(1, update.executeUpdate(), "释放查询事务后另一事务应可修改");
                    contender.rollback();
                }
            }
        } finally {
            session.rollback(true);
            if (inserted) {
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM " + table
                        + " WHERE id = ? AND tenant_id = 1 AND deleted = 0 AND creator = '101'", Integer.class, ID));
                assertEquals(1, jdbc.update("DELETE FROM " + table
                        + " WHERE id = ? AND tenant_id = 1 AND deleted = 0 AND creator = '101'", ID));
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, ID));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"payment", "receipt"})
    void transferredAndOrdinaryDocumentsSaveAndReloadThroughRealMappers(String kind) throws Exception {
        boolean payment = "payment".equals(kind);
        String source = payment ? "payable" : "receivable";
        String party = payment ? "supplier_id" : "customer_id";
        Connection connection = session.getConnection();
        insert(connection, "erp_" + source + "_misc", "id", ID, "no", "LOCK-SOURCE", "status", 20,
                party, ID, "amount", 2000);
        Object service = payment ? new ErpFinancePaymentServiceImpl() : new ErpFinanceReceiptServiceImpl();
        // 真实 Service、主表/明细/来源 Mapper、SQL 拦截链和 MySQL；外部档案/权限/日志服务用 mock 隔离。
        for (Field field : service.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Resource.class)) {
                Object dependency = factory.getConfiguration().hasMapper(field.getType())
                        ? session.getMapper(field.getType()) : mock(field.getType());
                ReflectionTestUtils.setField(service, field.getName(), dependency);
            }
        }
        for (boolean transferred : new boolean[]{true, false}) {
            for (boolean draft : new boolean[]{false, true}) {
                long documentId = ID + (transferred ? 10 : 20) + (draft ? 1 : 0);
                insert(connection, "erp_finance_" + kind, "id", documentId, "no", "LOCK-" + documentId,
                        "status", draft ? 0 : 10, party, ID, "total_price", 2000, kind + "_price", 2000,
                        "source_" + source + "_misc_id", transferred ? ID : null,
                        "source_" + source + "_misc_no", transferred ? "LOCK-SOURCE" : null);
                if (payment) {
                    ErpFinancePaymentServiceImpl target = (ErpFinancePaymentServiceImpl) service;
                    if (draft) {
                        target.updateFinancePaymentDraft(new ErpFinancePaymentDraftSaveReqVO().setId(documentId)
                                .setSupplierId(ID).setTotalPrice(new BigDecimal("800")).setDiscountPrice(BigDecimal.ZERO)
                                .setItems(Collections.emptyList()));
                    } else {
                        target.updateFinancePayment(new ErpFinancePaymentSaveReqVO().setId(documentId)
                                .setSupplierId(ID).setPaymentTime(LocalDateTime.now()).setTotalPrice(new BigDecimal("800"))
                                .setDiscountPrice(BigDecimal.ZERO).setItems(Collections.emptyList()));
                    }
                    cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO saved =
                            session.getMapper(ErpFinancePaymentMapper.class).selectById(documentId);
                    assertEquals(0, saved.getPaymentPrice().compareTo(new BigDecimal("800")));
                    assertEquals(transferred ? Long.valueOf(ID) : null, saved.getSourcePayableMiscId());
                    assertEquals(transferred ? "LOCK-SOURCE" : null, saved.getSourcePayableMiscNo());
                } else {
                    ErpFinanceReceiptServiceImpl target = (ErpFinanceReceiptServiceImpl) service;
                    if (draft) {
                        target.updateFinanceReceiptDraft(new ErpFinanceReceiptDraftSaveReqVO().setId(documentId)
                                .setCustomerId(ID).setTotalPrice(new BigDecimal("800")).setDiscountPrice(BigDecimal.ZERO)
                                .setItems(Collections.emptyList()));
                    } else {
                        target.updateFinanceReceipt(new ErpFinanceReceiptSaveReqVO().setId(documentId)
                                .setCustomerId(ID).setReceiptTime(LocalDateTime.now()).setTotalPrice(new BigDecimal("800"))
                                .setDiscountPrice(BigDecimal.ZERO).setItems(Collections.emptyList()));
                    }
                    cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO saved =
                            session.getMapper(ErpFinanceReceiptMapper.class).selectById(documentId);
                    assertEquals(0, saved.getReceiptPrice().compareTo(new BigDecimal("800")));
                    assertEquals(transferred ? Long.valueOf(ID) : null, saved.getSourceReceivableMiscId());
                    assertEquals(transferred ? "LOCK-SOURCE" : null, saved.getSourceReceivableMiscNo());
                }
                assertTrue(lockedIds(kind, documentId).isEmpty());
            }
        }
    }

    private List<Long> lockedIds(String kind, long documentId) {
        List<Long> ids = new ArrayList<>();
        if ("payment".equals(kind)) {
            session.getMapper(ErpFinancePaymentItemMapper.class).selectListByPaymentIdForUpdate(documentId)
                    .forEach(item -> ids.add(item.getId()));
        } else {
            session.getMapper(ErpFinanceReceiptItemMapper.class).selectListByReceiptIdForUpdate(documentId)
                    .forEach(item -> ids.add(item.getId()));
        }
        return ids;
    }

    private void insert(Connection connection, String table, Object... pairs) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put((String) pairs[i], pairs[i + 1]);
        }
        try (PreparedStatement check = connection.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE id = ?")) {
            check.setObject(1, values.get("id"));
            try (ResultSet rows = check.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1), "测试 ID 必须空闲，禁止覆盖已有行");
            }
        }
        try (PreparedStatement metadata = connection.prepareStatement("SELECT column_name,data_type,is_nullable,column_default,extra "
                + "FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?")) {
            metadata.setString(1, table);
            try (ResultSet columns = metadata.executeQuery()) {
                while (columns.next()) {
                    String name = columns.getString(1);
                    if (values.containsKey(name)) continue;
                    if ("tenant_id".equals(name)) { values.put(name, 1); continue; }
                    if ("creator".equals(name)) { values.put(name, "101"); continue; }
                    if (!"NO".equals(columns.getString(3)) || columns.getObject(4) != null
                            || columns.getString(5).contains("auto_increment")) continue;
                    String type = columns.getString(2);
                    values.put(name, Arrays.asList("varchar", "char", "text", "longtext").contains(type) ? "lock-test"
                            : Arrays.asList("datetime", "timestamp", "date").contains(type) ? "2026-09-26 12:00:00" : 0);
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table + " (`"
                + String.join("`,`", values.keySet()) + "`) VALUES ("
                + String.join(",", Collections.nCopies(values.size(), "?")) + ")")) {
            int i = 1;
            for (Object value : values.values()) insert.setObject(i++, value);
            assertEquals(1, insert.executeUpdate());
        }
    }
}
