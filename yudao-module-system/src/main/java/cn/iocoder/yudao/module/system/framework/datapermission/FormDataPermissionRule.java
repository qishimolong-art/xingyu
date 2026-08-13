package cn.iocoder.yudao.module.system.framework.datapermission;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRule;
import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.enums.permission.FormPermissionFieldValueTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionConfigRegistry;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionTableMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class FormDataPermissionRule implements DataPermissionRule {

    private static final String CONTEXT_KEY = FormDataPermissionRule.class.getSimpleName();
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final ObjectProvider<FormPermissionConfigRegistry> registryProvider;
    private final ObjectProvider<DeptDataPermissionRule> deptRuleProvider;
    private final PermissionCommonApi permissionApi;

    @Override
    public Set<String> getTableNames() {
        FormPermissionConfigRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return Collections.emptySet();
        }
        return registry.getEnabledFormTypes();
    }

    @Override
    public Expression getExpression(String tableName, Alias tableAlias) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return null;
        }
        if (ObjectUtil.notEqual(loginUser.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            return null;
        }
        if (hasAllDataScope(loginUser, tableName)) {
            return null;
        }

        Expression expression = null;
        expression = or(expression, buildBaseExpression(tableName, tableAlias, loginUser));
        expression = or(expression, buildFieldExpression(tableName, tableAlias, loginUser));
        expression = or(expression, buildExplicitPermissionExpression(tableName, tableAlias, loginUser));
        return expression != null ? expression : noPermissionExpression();
    }

    private Expression buildBaseExpression(String tableName, Alias tableAlias, LoginUser loginUser) {
        DeptDataPermissionRule deptDataPermissionRule = deptRuleProvider.getIfAvailable();
        if (deptDataPermissionRule == null) {
            return null;
        }
        return deptDataPermissionRule.buildExpression(tableName, tableAlias, loginUser);
    }

    private Expression buildFieldExpression(String tableName, Alias tableAlias, LoginUser loginUser) {
        FormPermissionConfigRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return null;
        }
        FormPermissionTableMeta tableMeta = registry.getTableMeta(tableName);
        if (tableMeta == null) {
            return null;
        }

        Expression expression = null;
        for (FormPermissionTableMeta.FieldMeta field : tableMeta.getFields()) {
            expression = or(expression, buildSingleFieldExpression(tableName, tableAlias, field, loginUser));
        }
        return expression;
    }

    private Expression buildSingleFieldExpression(String tableName, Alias tableAlias,
                                                  FormPermissionTableMeta.FieldMeta field, LoginUser loginUser) {
        String columnName = field.getColumnName();
        if (!isSafeIdentifier(columnName)) {
            log.warn("[buildSingleFieldExpression][表单({}) 字段({}) 非法，已跳过]", tableName, columnName);
            return null;
        }

        String columnSql = MyBatisUtils.buildColumn(tableName, tableAlias, columnName).toString();
        String userId = escapeSqlString(String.valueOf(loginUser.getId()));
        String valueType = field.getValueType();
        if (StrUtil.isBlank(valueType) || FormPermissionFieldValueTypeEnum.SINGLE_ID.getCode().equals(valueType)) {
            return parseExpression(columnSql + " = '" + userId + "'");
        }
        if (FormPermissionFieldValueTypeEnum.CSV_IDS.getCode().equals(valueType)) {
            return parseExpression("FIND_IN_SET('" + userId + "', " + columnSql + ") > 0");
        }
        if (FormPermissionFieldValueTypeEnum.JSON_IDS.getCode().equals(valueType)) {
            log.debug("[buildSingleFieldExpression][表单({}) 字段({}) 为 JSON_IDS，阶段 2 暂不直接生成 JSON SQL]",
                    tableName, columnName);
            return null;
        }
        log.warn("[buildSingleFieldExpression][表单({}) 字段({}) valueType({}) 不支持，已跳过]",
                tableName, columnName, valueType);
        return null;
    }

    private Expression buildExplicitPermissionExpression(String tableName, Alias tableAlias, LoginUser loginUser) {
        String alias = tableAlias != null ? tableAlias.getName() : tableName;
        Long tenantId = TenantContextHolder.getTenantId();
        String tenantSql = tenantId == null ? "" : " AND p.tenant_id = " + tenantId;
        String sql = "EXISTS (SELECT 1 FROM form_data_permission p "
                + "WHERE p.form_type = '" + escapeSqlString(tableName) + "' AND p.form_id = " + alias + ".id "
                + "AND p.user_id = " + loginUser.getId() + " AND p.deleted = 0" + tenantSql + ")";
        return parseExpression(sql);
    }

    private Expression parseExpression(String sql) {
        try {
            return CCJSqlParserUtil.parseCondExpression(sql);
        } catch (JSQLParserException ex) {
            log.error("[parseExpression][表单数据权限 SQL 解析失败，sql({})]", sql, ex);
            throw new IllegalStateException("表单数据权限 SQL 构建失败", ex);
        }
    }

    private Expression or(Expression left, Expression right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return new ParenthesedExpressionList(new OrExpression(left, right));
    }

    private Expression noPermissionExpression() {
        return new EqualsTo(null, null);
    }

    private boolean isSafeIdentifier(String identifier) {
        return StrUtil.isNotBlank(identifier) && SAFE_IDENTIFIER.matcher(identifier).matches();
    }

    private String escapeSqlString(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private boolean hasAllDataScope(LoginUser loginUser, String tableName) {
        String cacheKey = CONTEXT_KEY + ":" + tableName;
        DeptDataPermissionRespDTO permission = loginUser.getContext(cacheKey, DeptDataPermissionRespDTO.class);
        if (permission == null) {
            permission = permissionApi.getDeptDataPermission(loginUser.getId(), tableName);
            if (permission != null) {
                loginUser.setContext(cacheKey, permission);
            }
        }
        return permission != null && Boolean.TRUE.equals(permission.getAll());
    }

}
