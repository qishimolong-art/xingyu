package cn.iocoder.yudao.module.system.framework.datapermission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.enums.permission.FormPermissionFieldValueTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionConfigRegistry;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionTableMeta;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FormDataPermissionRule} 的单元测试
 *
 * 重点锁定“基础角色/部门数据范围 OR 表单字段规则 OR 显式授权”的并集语义。
 */
class FormDataPermissionRuleTest extends BaseMockitoUnitTest {

    private static final String SALE_CART_FORM = "erp_sale_cart";

    private FormDataPermissionRule rule;

    @Mock
    private ObjectProvider<FormPermissionConfigRegistry> registryProvider;
    @Mock
    private ObjectProvider<DeptDataPermissionRule> deptRuleProvider;
    @Mock
    private PermissionCommonApi permissionApi;
    @Mock
    private FormPermissionConfigRegistry registry;

    private DeptDataPermissionRule deptRule;

    @BeforeEach
    void setUp() {
        deptRule = new DeptDataPermissionRule(permissionApi);
        deptRule.addDeptColumn(SALE_CART_FORM, "dept_id");
        deptRule.addUserColumn(SALE_CART_FORM, "sale_user_id");
        rule = new FormDataPermissionRule(registryProvider, deptRuleProvider, permissionApi);
        lenient().when(deptRuleProvider.getIfAvailable()).thenReturn(deptRule);
        lenient().when(registryProvider.getIfAvailable()).thenReturn(registry);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testGetExpression_saleCartUnionBaseFieldAndExplicitPermission() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = buildAdminLoginUser(17L);
            securityMock.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            TenantContextHolder.setTenantId(1L);
            when(permissionApi.getDeptDataPermission(eq(17L), eq(SALE_CART_FORM)))
                    .thenReturn(new DeptDataPermissionRespDTO()
                            .setAll(false)
                            .setDeptIds(CollUtil.newLinkedHashSet(2L))
                            .setSelf(false));
            when(registry.getTableMeta(eq(SALE_CART_FORM))).thenReturn(new FormPermissionTableMeta(SALE_CART_FORM,
                    Arrays.asList(
                            new FormPermissionTableMeta.FieldMeta("sale_user_id",
                                    FormPermissionFieldValueTypeEnum.SINGLE_ID.getCode()),
                            new FormPermissionTableMeta.FieldMeta("creator",
                                    FormPermissionFieldValueTypeEnum.SINGLE_ID.getCode()),
                            new FormPermissionTableMeta.FieldMeta("shared_user_ids",
                                    FormPermissionFieldValueTypeEnum.CSV_IDS.getCode()),
                            new FormPermissionTableMeta.FieldMeta("json_user_ids",
                                    FormPermissionFieldValueTypeEnum.JSON_IDS.getCode()),
                            new FormPermissionTableMeta.FieldMeta("sale_user_id) OR 1 = 1 --",
                                    FormPermissionFieldValueTypeEnum.SINGLE_ID.getCode()))));

            Expression expression = rule.getExpression(SALE_CART_FORM, new Alias("c"));

            String sql = expression.toString();
            assertTrue(sql.contains("c.dept_id IN (2)"), sql);
            assertTrue(sql.contains("c.sale_user_id = '17'"), sql);
            assertTrue(sql.contains("c.creator = '17'"), sql);
            assertTrue(sql.contains("FIND_IN_SET('17', c.shared_user_ids) > 0"), sql);
            assertTrue(sql.contains("EXISTS (SELECT 1 FROM form_data_permission p"), sql);
            assertTrue(sql.contains("p.form_type = 'erp_sale_cart'"), sql);
            assertTrue(sql.contains("p.form_id = c.id"), sql);
            assertTrue(sql.contains("p.user_id = 17"), sql);
            assertTrue(sql.contains("p.deleted = 0"), sql);
            assertTrue(sql.contains("p.tenant_id = 1"), sql);
            assertTrue(sql.contains(" OR "), sql);
            assertTrue(!sql.contains("json_user_ids"), sql);
            assertTrue(!sql.contains("1 = 1"), sql);
        }
    }

    @Test
    void testGetExpression_allDataScopeDoNotAppendExtraRules() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = buildAdminLoginUser(17L);
            securityMock.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getDeptDataPermission(eq(17L), eq(SALE_CART_FORM)))
                    .thenReturn(new DeptDataPermissionRespDTO().setAll(true));

            Expression expression = rule.getExpression(SALE_CART_FORM, new Alias("c"));

            assertNull(expression);
            verify(registry, never()).getTableMeta(eq(SALE_CART_FORM));
        }
    }

    @Test
    void testGetExpression_noBaseAndNoFieldStillKeepsExplicitPermission() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = buildAdminLoginUser(17L);
            securityMock.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getDeptDataPermission(eq(17L), eq(SALE_CART_FORM)))
                    .thenReturn(new DeptDataPermissionRespDTO().setAll(false).setSelf(false));
            when(registry.getTableMeta(eq(SALE_CART_FORM)))
                    .thenReturn(new FormPermissionTableMeta(SALE_CART_FORM, Collections.emptyList()));

            Expression expression = rule.getExpression(SALE_CART_FORM, new Alias("c"));

            String sql = expression.toString();
            assertTrue(sql.contains("null = null"), sql);
            assertTrue(sql.contains(" OR "), sql);
            assertTrue(sql.contains("EXISTS (SELECT 1 FROM form_data_permission p"), sql);
            assertTrue(sql.contains("p.form_type = 'erp_sale_cart'"), sql);
            assertTrue(sql.contains("p.form_id = c.id"), sql);
            assertTrue(sql.contains("p.user_id = 17"), sql);
            assertTrue(sql.contains("p.deleted = 0"), sql);
        }
    }

    private LoginUser buildAdminLoginUser(Long userId) {
        return new LoginUser()
                .setId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue());
    }

}
