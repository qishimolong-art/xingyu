package cn.iocoder.yudao.module.erp.framework.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpFinanceLockOrderInterceptorTest {
    static final String PREFIX = "cn.iocoder.yudao.module.erp.dal.mysql.finance.";

    static TenantLineInnerInterceptor tenant() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() { return new LongValue(1); }
        });
    }

    static DataPermissionInterceptor permission() {
        return new DataPermissionInterceptor(new MultiDataPermissionHandler() {
            @Override public Expression getSqlSegment(Table table, Expression where, String statementId) {
                try {
                    return CCJSqlParserUtil.parseCondExpression("creator = '101'");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"Payment", "Receipt"})
    void repairsAfterTenantAndPermissionParsingAtActualPrepare(String kind) throws Exception {
        String table = "erp_finance_" + kind.toLowerCase() + "_item";
        Configuration config = new Configuration();
        ParameterMapping parameter = new ParameterMapping.Builder(config, "documentId", Long.class).build();
        MappedStatement ms = new MappedStatement.Builder(config, PREFIX + "ErpFinance" + kind + "ItemMapper.selectList",
                new StaticSqlSource(config, "SELECT id FROM " + table + " WHERE deleted = 0 AND "
                        + kind.toLowerCase() + "_id = ? ORDER BY id ASC FOR UPDATE",
                        Collections.singletonList(parameter)), SqlCommandType.SELECT).build();
        BoundSql sql = ms.getBoundSql(Collections.singletonMap("documentId", 52L));
        sql.setAdditionalParameter("preserved", 123L);
        tenant().beforeQuery(null, ms, sql.getParameterObject(), RowBounds.DEFAULT, null, sql);
        permission().beforeQuery(null, ms, sql.getParameterObject(), RowBounds.DEFAULT, null, sql);
        assertTrue(sql.getSql().endsWith("FOR UPDATE ORDER BY id ASC"), sql.getSql());
        String expected = sql.getSql().replace("FOR UPDATE ORDER BY id ASC", "ORDER BY id ASC FOR UPDATE");

        MybatisPlusInterceptor chain = new MybatisPlusInterceptor();
        chain.addInnerInterceptor(tenant());
        chain.addInnerInterceptor(permission());
        new ErpFinanceMybatisConfiguration().erpFinanceLockOrderRegistration(chain).afterSingletonsInstantiated();
        StatementHandler handler = config.newStatementHandler(null, ms, sql.getParameterObject(), RowBounds.DEFAULT, null, sql);
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(expected)).thenReturn(mock(PreparedStatement.class));
        ((StatementHandler) chain.plugin(handler)).prepare(connection, null);

        verify(connection).prepareStatement(expected);
        assertEquals(expected, sql.getSql());
        assertTrue(expected.contains("tenant_id = 1"));
        assertTrue(expected.contains("creator = '101'"));
        assertTrue(expected.contains("deleted = 0"));
        assertSame(parameter, sql.getParameterMappings().get(0));
        assertEquals(123L, sql.getAdditionalParameter("preserved"));
        assertEquals(52L, ((java.util.Map<?, ?>) sql.getParameterObject()).get("documentId"));
        new ErpFinanceLockOrderInterceptor().beforePrepare(handler, connection, null);
        assertEquals(expected, sql.getSql(), "修复必须幂等");
    }

    @Test
    void acceptsCaseAndWhitespaceOnlyForExactKnownTail() {
        assertEquals("SELECT id FROM t ORDER BY id ASC FOR UPDATE", process("ErpFinancePaymentItemMapper.selectList",
                "SELECT id FROM t\nfor\tupdate\norder BY\tid\tasc  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT id FROM t ORDER BY id ASC", "SELECT id FROM t ORDER BY id ASC FOR UPDATE",
            "SELECT id FROM t FOR UPDATE", "SELECT id FROM t FOR UPDATE ORDER BY id DESC",
            "SELECT id FROM t FOR UPDATE ORDER BY biz_id ASC", "SELECT id FROM t FOR UPDATE ORDER BY id ASC LIMIT 1"})
    void leavesOtherSqlFormsUntouched(String sql) {
        assertEquals(sql, process("ErpFinancePaymentItemMapper.selectList", sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ErpFinancePaymentMapper.selectList", "ErpFinancePaymentItemMapper.selectById",
            "ErpFinanceReceiptItemMapper.selectPage", "AnotherMapper.selectList"})
    void leavesOtherMappedStatementsUntouched(String id) {
        String sql = "SELECT id FROM t FOR UPDATE ORDER BY id ASC";
        assertEquals(sql, process(id, sql));
    }

    @Test
    void registersLastAfterSingletonInitialization() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            MybatisPlusInterceptor chain = new MybatisPlusInterceptor();
            context.registerBean(MybatisPlusInterceptor.class, () -> chain);
            context.registerBean("tenantRegistration", TenantLineInnerInterceptor.class, () -> {
                TenantLineInnerInterceptor tenant = tenant();
                chain.addInnerInterceptor(tenant);
                return tenant;
            });
            context.register(ErpFinanceMybatisConfiguration.class);
            context.refresh();
            assertEquals(2, chain.getInterceptors().size());
            assertTrue(chain.getInterceptors().get(0) instanceof TenantLineInnerInterceptor);
            assertTrue(chain.getInterceptors().get(1) instanceof ErpFinanceLockOrderInterceptor);
        }
    }

    private String process(String id, String sql) {
        Configuration config = new Configuration();
        MappedStatement ms = new MappedStatement.Builder(config, PREFIX + id,
                new StaticSqlSource(config, sql), SqlCommandType.SELECT).build();
        BoundSql boundSql = ms.getBoundSql(null);
        StatementHandler handler = config.newStatementHandler(null, ms, null, RowBounds.DEFAULT, null, boundSql);
        new ErpFinanceLockOrderInterceptor().beforePrepare(handler, null, null);
        return boundSql.getSql();
    }
}
