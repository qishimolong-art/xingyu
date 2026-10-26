package cn.iocoder.yudao.module.erp.framework.mybatis;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.statement.StatementHandler;

import java.sql.Connection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSqlParser 4.9 会把部分 ERP 明细锁定查询的 FOR UPDATE 输出到 ORDER BY 前面。
 * 仅在 JDBC prepare 前修复已知查询，避免后续租户或数据权限解析再次改变顺序。
 */
public class ErpFinanceLockOrderInterceptor implements InnerInterceptor {

    private static final String FINANCE_MAPPER_PREFIX = "cn.iocoder.yudao.module.erp.dal.mysql.finance.";
    private static final String PURCHASE_MAPPER_PREFIX = "cn.iocoder.yudao.module.erp.dal.mysql.purchase.";
    private static final Set<String> LOCK_SELECTS = Set.of(
            FINANCE_MAPPER_PREFIX + "ErpFinancePaymentItemMapper.selectList",
            FINANCE_MAPPER_PREFIX + "ErpFinanceReceiptItemMapper.selectList",
            FINANCE_MAPPER_PREFIX + "receivable.ErpReceivableOtherIncomeItemMapper.selectList",
            FINANCE_MAPPER_PREFIX + "payable.ErpPayableExpenseItemMapper.selectList",
            PURCHASE_MAPPER_PREFIX + "ErpPurchaseInItemMapper.selectList",
            PURCHASE_MAPPER_PREFIX + "ErpPurchasePriceAdjustItemMapper.selectList",
            PURCHASE_MAPPER_PREFIX + "ErpPurchaseReturnItemMapper.selectList");
    private static final Pattern INVALID_TAIL = Pattern.compile(
            "\\s+FOR\\s+UPDATE\\s+ORDER\\s+BY\\s+id\\s+ASC\\s*\\z", Pattern.CASE_INSENSITIVE);

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler handler = PluginUtils.mpStatementHandler(statementHandler);
        String statementId = handler.mappedStatement().getId();
        if (!LOCK_SELECTS.contains(statementId)) {
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
