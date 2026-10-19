package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantEntityResolverTest {

    @Test void normalizesOnlyBusinessWrappersAndKeepsCodePunctuation() {
        assertEquals("145988",AssistantEntityResolver.normalizeKeyword(" 产品编码为：145988 "));
        assertEquals("WSC56208/WSC20856",AssistantEntityResolver.normalizeKeyword("“厂家编码 WSC56208/WSC20856”"));
        assertEquals("卫斯卡空调滤清器",AssistantEntityResolver.normalizeKeyword("卫斯卡空调滤清器"));
        assertNull(AssistantEntityResolver.normalizeKeyword("   "));
    }

    @Test void extractsExplicitEntityTypeAndIdentifierWithoutModelGuessing() {
        Map<String,Object> product=AssistantEntityResolver.explicitReference("产品编码为145988在各仓库的库存");
        assertEquals("PRODUCT",product.get("entityType"));assertEquals("145988",product.get("keyword"));
        Map<String,Object> customer=AssistantEntityResolver.explicitReference("查询编码KH001的客户本月收款");
        assertEquals("CUSTOMER",customer.get("entityType"));assertEquals("KH001",customer.get("keyword"));
        Map<String,Object> supplier=AssistantEntityResolver.explicitReference("供应商旧编码 GYS-01 的付款情况");
        assertEquals("SUPPLIER",supplier.get("entityType"));assertEquals("GYS-01",supplier.get("keyword"));
        assertNull(AssistantEntityResolver.explicitReference("蛟龙港仓当前库存"));
    }

    @Test void explicitProductBrandScopeSearchesNameEvenWhenBrandFieldIsHidden() {
        LoginUser login=new LoginUser();login.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        try {
            PermissionApi permissions=mock(PermissionApi.class);
            when(permissions.hasAnyPermissions(101L,"erp:assistant:query")).thenReturn(true);
            when(permissions.hasAnyPermissions(101L,"erp:stock:query","erp:sale-report:query",
                    "erp:purchase-report:query","erp:sale-order:query")).thenReturn(true);
            DeptDataPermissionRespDTO deptScope=new DeptDataPermissionRespDTO();deptScope.setAll(true);
            when(permissions.getDeptDataPermission(101L,"erp_product")).thenReturn(deptScope);
            when(permissions.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
            ErpWarehouseService warehouses=mock(ErpWarehouseService.class);
            when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(
                    new ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
            AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
            when(reader.query(argThat(sql->sql.contains("COALESCE(e.name,'')) LIKE")
                            && !sql.contains("COALESCE(e.brand,'')) LIKE")),
                    argThat(parameters->parameters.contains("美孚")),eq(21))).thenAnswer(call->{
                Map<String,Object> row=new HashMap<>();row.put("id",9L);row.put("name","美孚经典表现");
                row.put("code","P009");row.put("description",null);row.put("matchedBy","BRAND");
                return Collections.singletonList(row);
            });
            AssistantEntityResolver resolver=resolver(mock(DeptApi.class));
            ReflectionTestUtils.setField(resolver,"permissions",permissions);
            ReflectionTestUtils.setField(resolver,"warehouses",warehouses);
            ReflectionTestUtils.setField(resolver,"reader",reader);

            List<Map<String,Object>> rows=resolver.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚",
                    AssistantEntityResolver.ProductMatchScope.BRAND);

            assertEquals(1,rows.size());assertEquals("BRAND",rows.get(0).get("matchType"));
            verify(reader).query(argThat(sql->sql.contains("COALESCE(e.name,'')) LIKE")
                            && !sql.contains("COALESCE(e.brand,'')) LIKE")),anyList(),eq(21));

            when(permissions.getCurrentUserHiddenFields("erp_product"))
                    .thenReturn(Collections.singletonList("brand"));
            assertEquals(1,resolver.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚",
                    AssistantEntityResolver.ProductMatchScope.BRAND).size());
        } finally {
            TenantContextHolder.clear();SecurityContextHolder.clearContext();
        }
    }

    @Test void productKeywordResolverNeverSearchesAuxiliaryProductFields() {
        LoginUser login=new LoginUser();login.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        try {
            PermissionApi permissions=mock(PermissionApi.class);
            when(permissions.hasAnyPermissions(101L,"erp:assistant:query")).thenReturn(true);
            when(permissions.hasAnyPermissions(101L,"erp:stock:query","erp:sale-report:query",
                    "erp:purchase-report:query","erp:sale-order:query")).thenReturn(true);
            DeptDataPermissionRespDTO deptScope=new DeptDataPermissionRespDTO();deptScope.setAll(true);
            when(permissions.getDeptDataPermission(101L,"erp_product")).thenReturn(deptScope);
            when(permissions.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
            ErpWarehouseService warehouses=mock(ErpWarehouseService.class);
            when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(
                    new ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
            AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
            when(reader.query(anyString(),anyList(),eq(21))).thenReturn(Collections.emptyList());
            AssistantEntityResolver resolver=resolver(mock(DeptApi.class));
            ReflectionTestUtils.setField(resolver,"permissions",permissions);
            ReflectionTestUtils.setField(resolver,"warehouses",warehouses);
            ReflectionTestUtils.setField(resolver,"reader",reader);

            assertTrue(resolver.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"k40 18").isEmpty());

            verify(reader,atLeastOnce()).query(argThat(sql->sql.contains("erp_product e")
                    && (sql.contains("e.name=?") || sql.contains("COALESCE(e.name,'')) LIKE"))),anyList(),eq(21));
            verify(reader,never()).query(argThat(sql->sql.contains("COALESCE(e.code,''))")
                    || sql.contains("COALESCE(e.factory_code,''))")
                    || sql.contains("COALESCE(e.bar_code,''))")
                    || sql.contains("COALESCE(e.pinyin_code,''))")
                    || sql.contains("COALESCE(e.wubi_code,''))")
                    || sql.contains("COALESCE(e.vehicle_model,''))")
                    || sql.contains("COALESCE(e.drawing_no,''))")
                    || sql.contains("COALESCE(e.brand,''))")),anyList(),anyInt());
        } finally {
            TenantContextHolder.clear();SecurityContextHolder.clearContext();
        }
    }

    @Test void resolvesDepartmentFromAuthoritativeDepartmentApi() {
        DeptApi departments=mock(DeptApi.class);
        AssistantEntityResolver resolver=resolver(departments);
        DeptRespDTO ganzi=department(10L,"甘孜分公司");
        when(departments.getDeptListByName("甘孜分公司")).thenReturn(Collections.singletonList(ganzi));

        List<Map<String,Object>> rows=resolver.resolveDepartment(
                new HashMap<>(Collections.singletonMap("entityAll",true)),"甘孜分公司",null);

        assertEquals(1,rows.size());
        assertEquals(10L,rows.get(0).get("id"));
        assertEquals("DEPARTMENT",rows.get(0).get("entityType"));
        assertEquals("NAME",rows.get(0).get("matchType"));
        verify(departments,never()).getDeptSimplePage(any(),any(),any(),any());
    }

    @Test void departmentResolutionKeepsPermissionScopeForFuzzySearch() {
        DeptApi departments=mock(DeptApi.class);
        AssistantEntityResolver resolver=resolver(departments);
        when(departments.getDeptListByName("甘孜")).thenReturn(Collections.emptyList());
        when(departments.getDeptSimplePage(eq(CommonStatusEnum.ENABLE.getStatus()),eq("甘孜"),
                eq(Set.of(10L)),any())).thenReturn(new PageResult<>(Collections.singletonList(department(10L,"甘孜分公司")),1L));
        Map<String,Object> args=new HashMap<>();
        args.put("entityAll",false);args.put("entityDeptIds",Set.of(10L));

        List<Map<String,Object>> rows=resolver.resolveDepartment(args,"甘孜",null);

        assertEquals(1,rows.size());
        assertEquals("FUZZY_NAME",rows.get(0).get("matchType"));
        verify(departments).getDeptSimplePage(eq(CommonStatusEnum.ENABLE.getStatus()),eq("甘孜"),
                eq(Set.of(10L)),argThat(page -> page.getPageNo()==1 && page.getPageSize()==21));
    }

    @Test void userResolutionUsesSystemUserPermissionAndDepartmentScope() {
        LoginUser login=new LoginUser();login.setId(101L);
        login.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID,"10"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        try {
            PermissionApi permissions=mock(PermissionApi.class);
            when(permissions.hasAnyPermissions(101L,"erp:assistant:query")).thenReturn(true);
            when(permissions.hasAnyPermissions(101L,"system:user:query")).thenReturn(true);
            DeptDataPermissionRespDTO scope=new DeptDataPermissionRespDTO();scope.setSelf(true);scope.setDeptIds(Set.of(10L));
            when(permissions.getDeptDataPermission(101L,"system_users")).thenReturn(scope);
            when(permissions.getCurrentUserHiddenFields("system_users")).thenReturn(Collections.emptyList());
            when(permissions.getCurrentUserHiddenFields("system_users",10L)).thenReturn(Collections.emptyList());
            AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
            Map<String,Object> row=new HashMap<>();row.put("id",1L);row.put("name","张三");
            row.put("code",null);row.put("description",null);row.put("matchedBy","NAME");
            when(reader.query(anyString(),anyList(),eq(21))).thenReturn(Collections.singletonList(row));
            AssistantEntityResolver resolver=resolver(mock(DeptApi.class));
            ReflectionTestUtils.setField(resolver,"permissions",permissions);
            ReflectionTestUtils.setField(resolver,"reader",reader);

            List<Map<String,Object>> rows=resolver.resolveType(AssistantEntityResolver.EntityType.USER,"张三");

            assertEquals(1,rows.size());assertEquals("USER",rows.get(0).get("entityType"));
            assertFalse(rows.get(0).containsKey("mobile") || rows.get(0).containsKey("avatar")
                    || rows.get(0).containsKey("username") || rows.get(0).containsKey("deptId"));
            verify(permissions).getDeptDataPermission(101L,"system_users");
            verify(reader).query(argThat(sql->sql.contains("system_user_dept sud") && sql.contains("e.dept_id IN")),
                    argThat(parameters->parameters.contains(10L) && parameters.contains("101")),eq(21));
        } finally {
            TenantContextHolder.clear();SecurityContextHolder.clearContext();
        }
    }

    @Test void salespersonResolutionAcceptsSupportedSaleDocumentPermissionWithoutSystemUserPermission() {
        LoginUser login=new LoginUser();login.setId(101L);
        login.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID,"10"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        try {
            PermissionApi permissions=mock(PermissionApi.class);
            when(permissions.hasAnyPermissions(101L,"erp:assistant:query")).thenReturn(true);
            when(permissions.hasAnyPermissions(101L,"erp:sale-report:query","erp:receivable-account:query",
                    "erp:sale-order:query","erp:sale-quote:query","erp:sale-cart:query","erp:sale-out:query",
                    "erp:sale-return:query","erp:sale-pick:query","erp:sale-delivery:query")).thenReturn(true);
            DeptDataPermissionRespDTO scope=new DeptDataPermissionRespDTO();scope.setDeptIds(Set.of(10L));
            when(permissions.getDeptDataPermission(101L,"erp_sale_out")).thenReturn(scope);
            when(permissions.getCurrentUserHiddenFields("system_users")).thenReturn(Collections.emptyList());
            when(permissions.getCurrentUserHiddenFields("system_users",10L)).thenReturn(Collections.emptyList());
            AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
            Map<String,Object> row=new HashMap<>();row.put("id",2L);row.put("name","张三");
            row.put("code",null);row.put("description","业务员");row.put("matchedBy","NAME");
            when(reader.query(anyString(),anyList(),eq(21))).thenReturn(Collections.singletonList(row));
            AssistantEntityResolver resolver=resolver(mock(DeptApi.class));
            ReflectionTestUtils.setField(resolver,"permissions",permissions);
            ReflectionTestUtils.setField(resolver,"reader",reader);

            List<Map<String,Object>> rows=resolver.resolveType(AssistantEntityResolver.EntityType.SALESPERSON,"张三");

            assertEquals(1,rows.size());assertEquals("SALESPERSON",rows.get(0).get("entityType"));
            verify(permissions,never()).hasAnyPermissions(101L,"system:user:query");
            verify(reader).query(argThat(sql->sql.contains("system_users e") && sql.contains("e.dept_id IN")),
                    argThat(parameters->parameters.contains(10L)),eq(21));
        } finally {
            TenantContextHolder.clear();SecurityContextHolder.clearContext();
        }
    }

    @Test void resolvesCustomerHomophoneOnlyInsideCurrentPermissionAndVisiblePinyinField() {
        LoginUser login=new LoginUser();login.setId(101L);
        login.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID,"10"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        try {
            PermissionApi permissions=mock(PermissionApi.class);
            when(permissions.hasAnyPermissions(101L,"erp:assistant:query")).thenReturn(true);
            when(permissions.hasAnyPermissions(101L,"erp:sale-report:query","erp:finance-receipt:query",
                    "erp:receivable-account:query","erp:sale-order:query")).thenReturn(true);
            DeptDataPermissionRespDTO scope=new DeptDataPermissionRespDTO();scope.setAll(true);
            when(permissions.getDeptDataPermission(101L,"erp_customer")).thenReturn(scope);
            when(permissions.getCurrentUserHiddenFields("erp_customer")).thenReturn(Collections.emptyList());
            AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
            when(reader.query(anyString(),anyList(),eq(21))).thenAnswer(call->{
                String sql=call.getArgument(0);
                if(!sql.contains("LIKE CONCAT('%',UPPER(?),'%')")) return Collections.emptyList();
                Map<String,Object> row=new HashMap<>();row.put("id",8L);row.put("name","成都路通源");
                row.put("code","KH008");row.put("description",null);row.put("matchedBy","PINYIN");
                return Collections.singletonList(row);
            });
            AssistantEntityResolver resolver=resolver(mock(DeptApi.class));
            ReflectionTestUtils.setField(resolver,"permissions",permissions);
            ReflectionTestUtils.setField(resolver,"reader",reader);
            AssistantPhoneticNormalizer phonetic=new AssistantPhoneticNormalizer();
            AssistantProperties properties=(AssistantProperties)ReflectionTestUtils.getField(resolver,"properties");
            ReflectionTestUtils.setField(phonetic,"properties",properties);
            ReflectionTestUtils.setField(phonetic,"companyKnowledge",null);
            ReflectionTestUtils.setField(resolver,"phonetic",phonetic);

            List<Map<String,Object>> rows=resolver.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都路通原");

            assertEquals(1,rows.size());
            assertEquals("成都路通源",rows.get(0).get("name"));
            assertEquals("PHONETIC",rows.get(0).get("matchType"));
            assertEquals("成都路通原",rows.get(0).get("correctedFrom"));
            verify(reader,atLeastOnce()).query(argThat(sql->sql.contains("erp_customer e") && sql.contains("pinyin_code")),
                    argThat(parameters->parameters.contains("CDLTY")),eq(21));
        } finally {
            TenantContextHolder.clear();SecurityContextHolder.clearContext();
        }
    }

    @Test void hiddenPinyinFieldDisablesPhoneticEntityFallback() {
        LoginUser login=new LoginUser();login.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        try {
            PermissionApi permissions=mock(PermissionApi.class);
            when(permissions.hasAnyPermissions(101L,"erp:assistant:query")).thenReturn(true);
            when(permissions.hasAnyPermissions(101L,"erp:sale-report:query","erp:finance-receipt:query",
                    "erp:receivable-account:query","erp:sale-order:query")).thenReturn(true);
            DeptDataPermissionRespDTO scope=new DeptDataPermissionRespDTO();scope.setAll(true);
            when(permissions.getDeptDataPermission(101L,"erp_customer")).thenReturn(scope);
            when(permissions.getCurrentUserHiddenFields("erp_customer")).thenReturn(Collections.singletonList("pinyinCode"));
            AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
            when(reader.query(anyString(),anyList(),eq(21))).thenReturn(Collections.emptyList());
            AssistantEntityResolver resolver=resolver(mock(DeptApi.class));
            ReflectionTestUtils.setField(resolver,"permissions",permissions);
            ReflectionTestUtils.setField(resolver,"reader",reader);
            AssistantPhoneticNormalizer phonetic=new AssistantPhoneticNormalizer();
            ReflectionTestUtils.setField(phonetic,"properties",ReflectionTestUtils.getField(resolver,"properties"));
            ReflectionTestUtils.setField(phonetic,"companyKnowledge",null);
            ReflectionTestUtils.setField(resolver,"phonetic",phonetic);

            assertTrue(resolver.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都路通原").isEmpty());
            verify(reader,never()).query(argThat(sql->sql.contains("LIKE CONCAT('%',UPPER(?),'%')")),anyList(),anyInt());
        } finally {
            TenantContextHolder.clear();SecurityContextHolder.clearContext();
        }
    }

    private static AssistantEntityResolver resolver(DeptApi departments) {
        AssistantEntityResolver resolver=new AssistantEntityResolver();
        ReflectionTestUtils.setField(resolver,"departments",departments);
        ReflectionTestUtils.setField(resolver,"properties",new AssistantProperties());
        return resolver;
    }

    private static DeptRespDTO department(Long id,String name) {
        DeptRespDTO department=new DeptRespDTO();department.setId(id);department.setName(name);
        department.setStatus(CommonStatusEnum.ENABLE.getStatus());return department;
    }

}
