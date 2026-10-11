package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.ProductWarehousePair;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpPurchaseOrderDetailReportServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseOrderDetailReportService service;

    @Mock
    private ErpPurchaseOrderDetailReportRepository repository;
    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @AfterEach
    void tearDownTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void testGetSummary_deduplicatesProductWarehousePairsAndMasksPrice() {
        TenantContextHolder.setTenantId(1L);
        DeptDataPermissionRespDTO all = new DeptDataPermissionRespDTO();
        all.setAll(true);
        when(permissionApi.getDeptDataPermission(anyLong(), anyString())).thenReturn(all);
        when(fieldPermissionMasker.isFieldHidden(anyString(), anyString())).thenReturn(true);

        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(20);
        Summary databaseSummary = new Summary().setRowCount(3L)
                .setTotalCount(new BigDecimal("15"))
                .setTotalPrice(new BigDecimal("300"))
                .setOrderInTransitCount(new BigDecimal("8"));
        when(repository.summary(any(), any())).thenReturn(databaseSummary);
        when(repository.distinctPairs(any(), any())).thenReturn(Arrays.asList(
                pair(101L, 201L), pair(101L, 201L), pair(101L, 202L)));

        Map<String, BigDecimal> transit = new HashMap<>();
        transit.put("101_201", new BigDecimal("5"));
        transit.put("101_202", new BigDecimal("7"));
        when(purchaseOrderItemMapper.selectInTransitCountMap(any(), any(), any())).thenReturn(transit);
        Map<String, BigDecimal> pending = new HashMap<>();
        pending.put("101_201", new BigDecimal("2"));
        pending.put("101_202", new BigDecimal("3"));
        when(stockMapper.selectPendingInCountMap(any(), any())).thenReturn(pending);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9L);

            Summary result = service.getSummary(filter);

            assertEquals(new BigDecimal("12"), result.getInTransitCount());
            assertEquals(new BigDecimal("5"), result.getPendingInCount());
            assertEquals(new BigDecimal("8"), result.getOrderInTransitCount());
            assertEquals(Boolean.FALSE, result.getPriceVisible());
            assertNull(result.getTotalPrice());
        }
    }

    private ProductWarehousePair pair(Long productId, Long warehouseId) {
        return new ProductWarehousePair().setProductId(productId).setWarehouseId(warehouseId);
    }
}
