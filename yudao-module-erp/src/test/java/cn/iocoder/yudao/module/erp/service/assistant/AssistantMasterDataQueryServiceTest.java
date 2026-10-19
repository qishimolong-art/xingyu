package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantMasterDataQueryServiceTest {

    private AssistantMasterDataQueryService service;
    private PermissionApi permissions;
    private ErpCustomerService customers;
    private ErpSupplierService suppliers;
    private ErpProductService products;
    private AdminUserApi users;

    @BeforeEach
    void setUp() {
        service = new AssistantMasterDataQueryService();
        permissions = mock(PermissionApi.class);
        customers = mock(ErpCustomerService.class);
        suppliers = mock(ErpSupplierService.class);
        products = mock(ErpProductService.class);
        users = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(service, "permissions", permissions);
        ReflectionTestUtils.setField(service, "customerService", customers);
        ReflectionTestUtils.setField(service, "supplierService", suppliers);
        ReflectionTestUtils.setField(service, "productService", products);
        ReflectionTestUtils.setField(service, "userApi", users);
        LoginUser user = new LoginUser();
        user.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "unused", Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        when(permissions.hasAnyPermissions(anyLong(), anyString())).thenReturn(true);
        DeptDataPermissionRespDTO scope = new DeptDataPermissionRespDTO();
        scope.setAll(true);
        when(permissions.getDeptDataPermission(eq(101L), anyString())).thenReturn(scope);
        when(products.hasCurrentUserProductArchiveVisibleScope()).thenReturn(true);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void parsesArchiveTypeStatusAndRejectsBusinessMetrics() {
        assertQuery("总共有多少个客户", AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                AssistantMasterDataQueryService.StatusMode.ENABLED);
        assertQuery("停用供应商有多少", AssistantMasterDataQueryService.ArchiveType.SUPPLIER,
                AssistantMasterDataQueryService.StatusMode.DISABLED);
        assertQuery("启用和停用配件分别有多少", AssistantMasterDataQueryService.ArchiveType.PRODUCT,
                AssistantMasterDataQueryService.StatusMode.SPLIT);
        assertQuery("总共有多少个商品", AssistantMasterDataQueryService.ArchiveType.PRODUCT,
                AssistantMasterDataQueryService.StatusMode.ENABLED);
        assertQuery("包含停用的客户总数", AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                AssistantMasterDataQueryService.StatusMode.ALL);
        assertQuery("有多少个员工", AssistantMasterDataQueryService.ArchiveType.EMPLOYEE,
                AssistantMasterDataQueryService.StatusMode.ENABLED);
        assertQuery("启用和停用职工分别有多少", AssistantMasterDataQueryService.ArchiveType.EMPLOYEE,
                AssistantMasterDataQueryService.StatusMode.SPLIT);
        assertNull(service.parse("库存中有多少个配件"));
        assertNull(service.parse("客户本月销售额"));
        assertNull(service.parse("供应商采购额"));
        assertNull(service.parse("客户欠款有多少"));
    }

    @Test
    void parsesProductCountKeywordWithoutDroppingBrandOrSeriesConstraint() {
        for (String question : new String[]{"ERP中有多少个美孚产品", "美孚产品有多少个"}) {
            AssistantMasterDataQueryService.Query query = service.parse(question);
            assertNotNull(query, question);
            assertEquals(AssistantMasterDataQueryService.ArchiveType.PRODUCT, query.getArchiveType(), question);
            assertEquals(AssistantMasterDataQueryService.StatusMode.ENABLED, query.getStatusMode(), question);
            assertEquals("美孚", query.getProductKeyword(), question);
        }
        assertNull(service.parse("ERP中有多少个产品").getProductKeyword());
        assertEquals("黑霸王", service.parse("黑霸王系列商品有几个").getProductKeyword());
    }

    @Test
    void parsesExplicitProductBrandSeparatelyFromArchiveKeyword() {
        for(String question:new String[]{"品牌为美孚的产品有多少个","美孚品牌配件有几个","美孚牌产品总数"}) {
            AssistantMasterDataQueryService.Query query=service.parse(question);
            assertNotNull(query,question);
            assertEquals(AssistantMasterDataQueryService.ArchiveType.PRODUCT,query.getArchiveType(),question);
            assertEquals("美孚",query.getProductBrand(),question);
            assertNull(query.getProductKeyword(),question);
        }
    }

    @Test
    void customerCountUsesEnabledStatusAndDeterministicAnswer() {
        when(customers.getCustomerPageByStatus(any(ErpCustomerPageReqVO.class),
                eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(new PageResult<>(Collections.emptyList(), 204L));
        Map<String, Object> result = service.execute(new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                AssistantMasterDataQueryService.StatusMode.ENABLED));
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("当前账号可见范围内共有 204 个启用客户档案。", result.get("answerText"));
        assertEquals("启用客户数量", result.get("summaryLabel"));
        verify(customers).getCustomerPageByStatus(argThat(request -> request.getPageNo() == 1
                        && request.getPageSize() == 1),
                eq(CommonStatusEnum.ENABLE.getStatus()));
    }

    @Test
    void supplierSplitReturnsBothStatusCounts() {
        when(suppliers.getSupplierPageByStatus(any(ErpSupplierPageReqVO.class),
                eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(new PageResult<>(Collections.emptyList(), 8L));
        when(suppliers.getSupplierPageByStatus(any(ErpSupplierPageReqVO.class),
                eq(CommonStatusEnum.DISABLE.getStatus())))
                .thenReturn(new PageResult<>(Collections.emptyList(), 2L));
        Map<String, Object> result = service.execute(new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.SUPPLIER,
                AssistantMasterDataQueryService.StatusMode.SPLIT));
        assertEquals("当前账号可见范围内启用供应商 8 个，停用供应商 2 个，共 10 个。", result.get("answerText"));
        assertEquals(2, ((java.util.List<?>) result.get("summary")).size());
    }

    @Test
    void productCountUsesArchivePageScopeAndStatus() {
        when(products.getProductVOPage(any(ErpProductPageReqVO.class), eq(false)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 36L));
        Map<String, Object> result = service.execute(new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.PRODUCT,
                AssistantMasterDataQueryService.StatusMode.DISABLED));
        assertEquals("当前账号可见范围内共有 36 个停用配件档案。", result.get("answerText"));
        ArgumentCaptor<ErpProductPageReqVO> captor = ArgumentCaptor.forClass(ErpProductPageReqVO.class);
        verify(products).getProductVOPage(captor.capture(), eq(false));
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(Boolean.TRUE, captor.getValue().getIncludeSaleDistributedArchive());
        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    void filteredProductCountUsesExistingVisibleKeywordPageAndReportsFilter() {
        when(products.getProductVOPage(any(ErpProductPageReqVO.class), eq(false)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 27L));
        AssistantMasterDataQueryService.Query query = service.parse("ERP中有多少个美孚产品");

        Map<String, Object> result = service.execute(query);

        assertEquals("当前账号可见范围内关键词“美孚”匹配的共有 27 个启用配件档案。", result.get("answerText"));
        assertEquals("启用配件数量（关键词：美孚）", result.get("summaryLabel"));
        assertEquals("当前登录用户有权限查看的配件档案；产品关键词：美孚", result.get("scope"));
        assertEquals("美孚", ((Map<?, ?>) result.get("queryTrace")).get("productKeyword"));
        ArgumentCaptor<ErpProductPageReqVO> captor = ArgumentCaptor.forClass(ErpProductPageReqVO.class);
        verify(products).getProductVOPage(captor.capture(), eq(false));
        assertEquals("美孚", captor.getValue().getKeyword());
        assertEquals(ErpProductPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_ARCHIVE,
                captor.getValue().getKeywordSearchMode());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(Boolean.TRUE, captor.getValue().getIncludeSaleDistributedArchive());
    }

    @Test
    void explicitProductBrandUsesDedicatedFilterAndPermission() {
        when(products.getProductVOPage(any(ErpProductPageReqVO.class),eq(false)))
                .thenReturn(new PageResult<>(Collections.emptyList(),12L));

        Map<String,Object> result=service.execute(service.parse("品牌为美孚的产品有多少个"));

        assertEquals("当前账号可见范围内品牌“美孚”的共有 12 个启用配件档案。",result.get("answerText"));
        ArgumentCaptor<ErpProductPageReqVO> captor=ArgumentCaptor.forClass(ErpProductPageReqVO.class);
        verify(products).getProductVOPage(captor.capture(),eq(false));
        assertEquals("美孚",captor.getValue().getBrand());
        assertNull(captor.getValue().getKeyword());
        assertEquals(ErpProductPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_ARCHIVE,
                captor.getValue().getKeywordSearchMode());

        reset(products);
        when(products.hasCurrentUserProductArchiveVisibleScope()).thenReturn(true);
        when(permissions.getCurrentUserHiddenFields("erp_product",null,false))
                .thenReturn(Collections.singletonList("brand"));
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->service.execute(service.parse("美孚品牌产品有几个")));
        assertEquals("FIELD_FORBIDDEN",failure.getCode());
        verify(products,never()).getProductVOPage(any(ErpProductPageReqVO.class),anyBoolean());
    }

    @Test
    void filteredProductCountReturnsExplicitFilteredEmptyResult() {
        when(products.getProductVOPage(any(ErpProductPageReqVO.class), eq(false)))
                .thenReturn(PageResult.empty());

        Map<String, Object> result = service.execute(service.parse("未知系列产品有多少个"));

        assertEquals("EMPTY", result.get("status"));
        assertEquals("当前账号可见范围内没有关键词“未知”匹配的启用配件档案。", result.get("emptyText"));
        assertTrue(((java.util.List<?>) result.get("summary")).isEmpty());
    }

    @Test
    void zeroCountReturnsExplicitEmptyResult() {
        when(customers.getCustomerPageByStatus(any(ErpCustomerPageReqVO.class), anyInt()))
                .thenReturn(PageResult.empty());
        Map<String, Object> result = service.execute(new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                AssistantMasterDataQueryService.StatusMode.ENABLED));
        assertEquals("EMPTY", result.get("status"));
        assertEquals("当前账号可见范围内没有启用客户档案。", result.get("emptyText"));
        assertTrue(((java.util.List<?>) result.get("summary")).isEmpty());
    }

    @Test
    void employeeCountUsesTenantUserPageAndSystemUserPermission() {
        when(users.getUserSimplePage(eq(CommonStatusEnum.ENABLE.getStatus()), isNull(), any()))
                .thenReturn(new PageResult<>(Collections.emptyList(), 42L));
        Map<String, Object> result = service.execute(new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.EMPLOYEE,
                AssistantMasterDataQueryService.StatusMode.ENABLED));
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("按 ERP 员工账号口径，当前租户内共有 42 个启用员工档案。", result.get("answerText"));
        assertEquals("当前租户内员工账号", result.get("scope"));
        assertEquals(Collections.singletonList("ERP 系统用户档案；当前租户、启停用状态口径"), result.get("basis"));
        verify(permissions).hasAnyPermissions(101L, "system:user:query");
        verify(users).getUserSimplePage(eq(CommonStatusEnum.ENABLE.getStatus()), isNull(),
                argThat(request -> request.getPageNo() == 1 && request.getPageSize() == 1));
    }

    @Test
    void employeeCountRequiresSystemUserQueryPermission() {
        when(permissions.hasAnyPermissions(101L, "system:user:query")).thenReturn(false);
        AssistantFailure forbidden = assertThrows(AssistantFailure.class, () -> service.validateAccess(
                new AssistantMasterDataQueryService.Query(AssistantMasterDataQueryService.ArchiveType.EMPLOYEE,
                        AssistantMasterDataQueryService.StatusMode.ENABLED)));
        assertEquals("FORBIDDEN", forbidden.getCode());
        assertEquals("当前账号没有员工档案查询权限", forbidden.getMessage());
        verifyNoInteractions(users);
    }

    @Test
    void missingBusinessPermissionOrScopeIsNotReportedAsZero() {
        when(permissions.hasAnyPermissions(101L, "erp:customer:query")).thenReturn(false);
        AssistantFailure forbidden = assertThrows(AssistantFailure.class, () -> service.validateAccess(
                new AssistantMasterDataQueryService.Query(AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                        AssistantMasterDataQueryService.StatusMode.ENABLED)));
        assertEquals("FORBIDDEN", forbidden.getCode());
        assertEquals("当前账号没有客户档案查询权限", forbidden.getMessage());

        when(permissions.hasAnyPermissions(101L, "erp:customer:query")).thenReturn(true);
        when(permissions.getDeptDataPermission(101L, "erp_customer"))
                .thenReturn(new DeptDataPermissionRespDTO());
        AssistantFailure noScope = assertThrows(AssistantFailure.class, () -> service.validateAccess(
                new AssistantMasterDataQueryService.Query(AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                        AssistantMasterDataQueryService.StatusMode.ENABLED)));
        assertEquals("FORBIDDEN", noScope.getCode());
        assertEquals("当前账号没有可见的客户档案范围", noScope.getMessage());
    }

    private void assertQuery(String question, AssistantMasterDataQueryService.ArchiveType type,
                             AssistantMasterDataQueryService.StatusMode mode) {
        AssistantMasterDataQueryService.Query query = service.parse(question);
        assertNotNull(query);
        assertEquals(type, query.getArchiveType());
        assertEquals(mode, query.getStatusMode());
    }
}
