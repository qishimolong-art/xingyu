package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class UserErpBizDataReferenceService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z0-9_]+");

    private static final List<TableUserColumn> ERP_BIZ_USER_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            new TableUserColumn("erp_purchase_order", "purchaser"),
            new TableUserColumn("erp_purchase_in", "purchaser"),
            new TableUserColumn("erp_purchase_in", "accountant"),
            new TableUserColumn("erp_purchase_in", "handler"),
            new TableUserColumn("erp_purchase_return", "purchaser"),
            new TableUserColumn("erp_purchase_return", "handler"),
            new TableUserColumn("erp_purchase_invoice", "handler_id"),
            new TableUserColumn("erp_supplier", "purchaser"),
            new TableUserColumn("erp_customer", "sale_user_id"),
            new TableUserColumn("erp_customer", "developer_user_id"),
            new TableUserColumn("erp_customer_extend_info", "reconcile_service_user_id"),
            new TableUserColumn("erp_sale_cart", "sale_user_id"),
            new TableUserColumn("erp_sale_cart", "developer_user_id"),
            new TableUserColumn("erp_sale_order", "sale_user_id"),
            new TableUserColumn("erp_sale_out", "sale_user_id"),
            new TableUserColumn("erp_sale_quote", "sale_user_id"),
            new TableUserColumn("erp_sale_quote", "developer_user_id"),
            new TableUserColumn("erp_sale_return", "sale_user_id"),
            new TableUserColumn("erp_sale_return", "developer_user_id"),
            new TableUserColumn("erp_book_open", "operator_user_id"),
            new TableUserColumn("erp_finance_payment", "finance_user_id"),
            new TableUserColumn("erp_finance_receipt", "finance_user_id"),
            new TableUserColumn("erp_finance_transfer", "finance_user_id"),
            new TableUserColumn("erp_payable_writeoff", "operator_user_id"),
            new TableUserColumn("erp_receivable_writeoff", "operator_user_id"),
            new TableUserColumn("erp_payable_expense", "handler_id"),
            new TableUserColumn("erp_payable_expense_item", "handler_id"),
            new TableUserColumn("erp_payable_other", "handler_id"),
            new TableUserColumn("erp_receivable_other", "handler_id"),
            new TableUserColumn("erp_receivable_other_item", "handler_id"),
            new TableUserColumn("erp_receivable_other_income", "handler_id"),
            new TableUserColumn("erp_receivable_other_income_item", "handler_id"),
            new TableUserColumn("erp_voucher_attribution", "handler_user_id")
    ));

    @Resource
    private DataSource dataSource;

    public boolean existsByUserIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return false;
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Long> cleanUserIds = userIds.stream().distinct().collect(Collectors.toList());
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 0L;
        }
        for (TableUserColumn tableUserColumn : ERP_BIZ_USER_COLUMNS) {
            if (existsInTable(jdbcTemplate, tableUserColumn, cleanUserIds, tenantId)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getErpBizUserColumns() {
        return ERP_BIZ_USER_COLUMNS.stream()
                .map(tableUserColumn -> tableUserColumn.tableName + "." + tableUserColumn.columnName)
                .collect(Collectors.toList());
    }

    private boolean existsInTable(JdbcTemplate jdbcTemplate, TableUserColumn tableUserColumn,
                                  List<Long> userIds, Long tenantId) {
        if (!isSafeIdentifier(tableUserColumn.tableName) || !isSafeIdentifier(tableUserColumn.columnName)
                || !hasRequiredColumns(jdbcTemplate, tableUserColumn)) {
            return false;
        }
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT 1 FROM " + tableUserColumn.tableName
                + " WHERE " + tableUserColumn.columnName + " IN (" + placeholders + ")"
                + " AND tenant_id = ?"
                + " AND deleted = FALSE"
                + " LIMIT 1";
        Object[] params = new Object[userIds.size() + 1];
        for (int i = 0; i < userIds.size(); i++) {
            params[i] = userIds.get(i);
        }
        params[userIds.size()] = tenantId;
        return Boolean.TRUE.equals(jdbcTemplate.query(sql, params,
                (org.springframework.jdbc.core.ResultSetExtractor<Boolean>) rs -> rs.next()));
    }

    private boolean hasRequiredColumns(JdbcTemplate jdbcTemplate, TableUserColumn tableUserColumn) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            String actualTableName = findTableName(metaData, tableUserColumn.tableName);
            return actualTableName != null
                    && hasColumn(metaData, actualTableName, tableUserColumn.columnName)
                    && hasColumn(metaData, actualTableName, "tenant_id")
                    && hasColumn(metaData, actualTableName, "deleted");
        }));
    }

    private String findTableName(DatabaseMetaData metaData, String tableName) throws SQLException {
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getTables(null, null, candidate, null)) {
                if (resultSet.next()) {
                    return resultSet.getString("TABLE_NAME");
                }
            }
        }
        return null;
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        for (String candidate : identifierCandidates(columnName)) {
            try (ResultSet resultSet = metaData.getColumns(null, null, tableName, candidate)) {
                if (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSafeIdentifier(String identifier) {
        return SAFE_IDENTIFIER.matcher(identifier).matches();
    }

    private List<String> identifierCandidates(String name) {
        return Arrays.asList(name, name.toUpperCase(Locale.ROOT), name.toLowerCase(Locale.ROOT));
    }

    private static final class TableUserColumn {

        private final String tableName;
        private final String columnName;

        private TableUserColumn(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }

    }

}
