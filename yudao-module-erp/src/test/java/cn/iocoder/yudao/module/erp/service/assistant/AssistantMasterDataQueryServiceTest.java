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

    @BeforeEach
    void setUp() {
        service = new AssistantMasterDataQueryService();
        permissions = mock(PermissionApi.class);
        customers = mock(ErpCustomerService.class);
        suppliers = mock(ErpSupplierService.class);
        products = mock(ErpProductService.class);
        ReflectionTestUtils.setField(service, "permissions", permissions);
        ReflectionTestUtils.setField(service, "customerService", customers);
        ReflectionTestUtils.setField(service, "supplierService", suppliers);
        ReflectionTestUtils.setField(service, "productService", products);
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
        assertQuery("包含停用的客户总数", AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                AssistantMasterDataQueryService.StatusMode.ALL);
        assertNull(service.parse("库存中有多少个配件"));
        assertNull(service.parse("客户本月销售额"));
        assertNull(service.parse("供应商采购额"));
        assertNull(service.parse("客户欠款有多少"));
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
