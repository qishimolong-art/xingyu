package cn.iocoder.yudao.module.erp.framework.mybatis;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.statement.StatementHandler;

import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSqlParser 4.9 会把收付款明细的 FOR UPDATE 输出到 ORDER BY 前面。
 * 仅在 JDBC prepare 前修复这两处已知查询，避免后续权限解析再次改变顺序。
 */
public class ErpFinanceLockOrderInterceptor implements InnerInterceptor {

    private static final String MAPPER_PREFIX = "cn.iocoder.yudao.module.erp.dal.mysql.finance.";
    private static final String PAYMENT_SELECT = MAPPER_PREFIX + "ErpFinancePaymentItemMapper.selectList";
    private static final String RECEIPT_SELECT = MAPPER_PREFIX + "ErpFinanceReceiptItemMapper.selectList";
    private static final Pattern INVALID_TAIL = Pattern.compile(
            "\\s+FOR\\s+UPDATE\\s+ORDER\\s+BY\\s+id\\s+ASC\\s*\\z", Pattern.CASE_INSENSITIVE);

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler handler = PluginUtils.mpStatementHandler(statementHandler);
        String statementId = handler.mappedStatement().getId();
        if (!PAYMENT_SELECT.equals(statementId) && !RECEIPT_SELECT.equals(statementId)) {
            return;
        }
        PluginUtils.MPBoundSql boundSql = handler.mPBoundSql();
        String sql = boundSql.sql();
        Matcher matcher = INVALID_TAIL.matcher(sql);
        if (matcher.find()) {
            // 不重新解析 SQL，不修改 WHERE、参数映射、权限条件或其他查询形式。
            boundSql.sql(sql.substring(0, matcher.start()) + " ORDER BY id ASC FOR UPDATE");
        }
    }
}
