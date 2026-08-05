package cn.iocoder.yudao.module.system.framework.datapermission;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRule;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionConfigRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class FormDataPermissionRule implements DataPermissionRule {

    private static final String CONTEXT_KEY = FormDataPermissionRule.class.getSimpleName();

    private final ObjectProvider<FormPermissionConfigRegistry> registryProvider;
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
        String alias = tableAlias != null ? tableAlias.getName() : tableName;
        Long tenantId = TenantContextHolder.getTenantId();
        String tenantSql = tenantId == null ? "" : " AND p.tenant_id = " + tenantId;
        String sql = "EXISTS (SELECT 1 FROM form_data_permission p "
                + "WHERE p.form_type = '" + tableName + "' AND p.form_id = " + alias + ".id "
                + "AND p.user_id = " + loginUser.getId() + " AND p.deleted = 0" + tenantSql + ")";
        try {
            return CCJSqlParserUtil.parseCondExpression(sql);
        } catch (JSQLParserException ex) {
            log.error("[getExpression][表单数据权限 SQL 解析失败，sql({})]", sql, ex);
            throw new IllegalStateException("表单数据权限 SQL 构建失败", ex);
        }
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
