package cn.iocoder.yudao.module.erp.service.purchase.history;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryRespVO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseHistoryServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseHistoryService service;
    @Mock
    private ErpPurchaseHistoryRepository repository;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        when(fieldPermissionMasker.getHiddenFieldSet(any())).thenReturn(Collections.emptySet());
        when(repository.count(any(), any())).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void bothPermissionsIncludeBothSources() {
        stubPermission(true, true);
        ErpPurchaseHistoryRespVO row = row(ErpPurchaseHistoryRespVO.TYPE_RETURN);
        when(repository.page(any(), any(), anyLong(), anyInt())).thenReturn(List.of(row));

        PageResult<ErpPurchaseHistoryRespVO> result = invoke();

        assertEquals(1L, result.getTotal());
        ErpPurchaseHistoryRepository.Scope scope = capturedScope();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.getField(scope, "includePurchaseIn"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.getField(scope, "includePurchaseReturn"));
    }

    @Test
    void purchaseInPermissionOnlyKeepsHistoryAvailable() {
        stubPermission(true, false);
        when(repository.page(any(), any(), anyLong(), anyInt()))
                .thenReturn(List.of(row(ErpPurchaseHistoryRespVO.TYPE_IN)));

        invoke();

        ErpPurchaseHistoryRepository.Scope scope = capturedScope();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.getField(scope, "includePurchaseIn"));
        assertEquals(false, org.springframework.test.util.ReflectionTestUtils.getField(scope, "includePurchaseReturn"));
        verify(permissionApi, never()).getDeptDataPermission(99L, "erp_purchase_return");
    }

    @Test
    void purchaseReturnPermissionOnlyKeepsHistoryAvailable() {
        stubPermission(false, true);
        when(repository.page(any(), any(), anyLong(), anyInt()))
                .thenReturn(List.of(row(ErpPurchaseHistoryRespVO.TYPE_RETURN)));

        invoke();

        ErpPurchaseHistoryRepository.Scope scope = capturedScope();
        assertEquals(false, org.springframework.test.util.ReflectionTestUtils.getField(scope, "includePurchaseIn"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.getField(scope, "includePurchaseReturn"));
        verify(permissionApi, never()).getDeptDataPermission(99L, "erp_purchase_in");
    }

    @Test
    void hiddenReturnPriceIsMaskedWithoutChangingCount() {
        stubPermission(true, true);
        ErpPurchaseHistoryRespVO returnRow = row(ErpPurchaseHistoryRespVO.TYPE_RETURN);
        when(repository.page(any(), any(), anyLong(), anyInt())).thenReturn(List.of(returnRow));
        when(fieldPermissionMasker.getHiddenFieldSet("erp_purchase_return"))
                .thenReturn(Set.of("item_productPrice"));

        PageResult<ErpPurchaseHistoryRespVO> result = invoke();

        assertNull(result.getList().get(0).getProductPrice());
        assertEquals(new BigDecimal("-2"), result.getList().get(0).getCount());
    }

    private PageResult<ErpPurchaseHistoryRespVO> invoke() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            return service.getPage(filter());
        }
    }

    private void stubPermission(boolean purchaseIn, boolean purchaseReturn) {
        when(permissionApi.hasAnyPermissions(99L, "erp:purchase-in:query")).thenReturn(purchaseIn);
        when(permissionApi.hasAnyPermissions(99L, "erp:purchase-return:query")).thenReturn(purchaseReturn);
        DeptDataPermissionRespDTO all = new DeptDataPermissionRespDTO();
        all.setAll(true);
        if (purchaseIn) {
            when(permissionApi.getDeptDataPermission(99L, "erp_purchase_in")).thenReturn(all);
        }
        if (purchaseReturn) {
            when(permissionApi.getDeptDataPermission(99L, "erp_purchase_return")).thenReturn(all);
        }
    }

    private ErpPurchaseHistoryRepository.Scope capturedScope() {
        ArgumentCaptor<ErpPurchaseHistoryRepository.Scope> captor =
                ArgumentCaptor.forClass(ErpPurchaseHistoryRepository.Scope.class);
        verify(repository).count(eq(filter()), captor.capture());
        return captor.getValue();
    }

    private ErpPurchaseHistoryPageReqVO filter() {
        ErpPurchaseHistoryPageReqVO filter = new ErpPurchaseHistoryPageReqVO();
        filter.setPageNo(1);
        filter.setPageSize(10);
        filter.setProductId(88L);
        return filter;
    }

    private ErpPurchaseHistoryRespVO row(String type) {
        return new ErpPurchaseHistoryRespVO()
                .setDocumentTypeCode(type)
                .setCount(new BigDecimal("-2"))
                .setProductPrice(new BigDecimal("18.50"));
    }
}
