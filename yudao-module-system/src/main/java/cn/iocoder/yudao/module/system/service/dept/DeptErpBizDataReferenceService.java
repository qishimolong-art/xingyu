package cn.iocoder.yudao.module.system.service.dept;

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
public class DeptErpBizDataReferenceService {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("[a-z0-9_]+");

    private static final List<String> ERP_BIZ_TABLES = Collections.unmodifiableList(Arrays.asList(
            "erp_product",
            "erp_product_category",
            "erp_product_price_system",
            "erp_product_unit",
            "erp_product_universal",
            "erp_price_system",
            "erp_purchase_in",
            "erp_purchase_invoice",
            "erp_purchase_order",
            "erp_purchase_price_adjust",
            "erp_purchase_return",
            "erp_purchase_suggestion",
            "erp_supplier",
            "erp_supplier_contact",
            "erp_customer",
            "erp_customer_contact",
            "erp_sale_cart",
            "erp_sale_order",
            "erp_sale_out",
            "erp_sale_price_adjust",
            "erp_sale_quote",
            "erp_sale_return",
            "erp_sale_config",
            "erp_stock",
            "erp_stock_check",
            "erp_stock_in",
            "erp_stock_move",
            "erp_stock_out",
            "erp_stock_record",
            "erp_warehouse",
            "erp_chain_order",
            "erp_account",
            "erp_finance_payment",
            "erp_finance_receipt",
            "erp_finance_transfer",
            "erp_other_payable",
            "erp_payable_expense",
            "erp_payable_expense_item",
            "erp_payable_other",
            "erp_pre_payment",
            "erp_other_receivable",
            "erp_pre_receipt",
            "erp_pre_receivable",
            "erp_receivable_other",
            "erp_receivable_other_income",
            "erp_receivable_other_income_item",
            "erp_receivable_other_item",
            "erp_voucher",
            "erp_voucher_attribution"
    ));

    @Resource
    private DataSource dataSource;

    public boolean existsByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return false;
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Long> cleanDeptIds = deptIds.stream().distinct().collect(Collectors.toList());
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 0L;
        }
        for (String tableName : ERP_BIZ_TABLES) {
            if (existsInTable(jdbcTemplate, tableName, cleanDeptIds, tenantId)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getErpBizTables() {
        return ERP_BIZ_TABLES;
    }

    private boolean existsInTable(JdbcTemplate jdbcTemplate, String tableName, List<Long> deptIds, Long tenantId) {
        if (!SAFE_TABLE_NAME.matcher(tableName).matches() || !hasRequiredColumns(jdbcTemplate, tableName)) {
            return false;
        }
        String placeholders = deptIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT 1 FROM " + tableName
                + " WHERE dept_id IN (" + placeholders + ")"
                + " AND tenant_id = ?"
                + " AND deleted = FALSE"
                + " LIMIT 1";
        Object[] params = new Object[deptIds.size() + 1];
        for (int i = 0; i < deptIds.size(); i++) {
            params[i] = deptIds.get(i);
        }
        params[deptIds.size()] = tenantId;
        return Boolean.TRUE.equals(jdbcTemplate.query(sql, params,
                (org.springframework.jdbc.core.ResultSetExtractor<Boolean>) rs -> rs.next()));
    }

    private boolean hasRequiredColumns(JdbcTemplate jdbcTemplate, String tableName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            String actualTableName = findTableName(metaData, tableName);
            return actualTableName != null
                    && hasColumn(metaData, actualTableName, "dept_id")
                    && hasColumn(metaData, actualTableName, "tenant_id")
                    && hasColumn(metaData, actualTableName, "deleted");
        }));
    }

    private String findTableName(DatabaseMetaData metaData, String tableName) throws SQLException {
        for (String candidate : tableNameCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getTables(null, null, candidate, null)) {
                if (resultSet.next()) {
                    return resultSet.getString("TABLE_NAME");
                }
            }
        }
        return null;
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        for (String candidate : tableNameCandidates(columnName)) {
            try (ResultSet resultSet = metaData.getColumns(null, null, tableName, candidate)) {
                if (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> tableNameCandidates(String name) {
        return Arrays.asList(name, name.toUpperCase(Locale.ROOT), name.toLowerCase(Locale.ROOT));
    }

}
