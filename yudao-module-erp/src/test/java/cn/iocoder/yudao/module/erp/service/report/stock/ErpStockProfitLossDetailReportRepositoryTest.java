package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockProfitLossDetailReportRepositoryTest {

    private final ErpStockProfitLossDetailReportRepository repository =
            new ErpStockProfitLossDetailReportRepository();

    @Test
    void shouldBuildTenantApprovedDetailQueryWithBoundedWarehouseScope() {
        Filter filter = new Filter();
        filter.setKeyword("刹车 片");
        filter.setCheckType(1);
        filter.setDocumentNo("PD2026");
        filter.setWarehouseId(10L);
        filter.setCreatorKeyword("张");
        filter.setCheckDate(new LocalDateTime[]{LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59)});

        Object sql = repository.base(filter, new ErpStockProfitLossDetailReportRepository.Scope(
                1L, false, Arrays.asList(10L, 11L), Collections.singletonList(12L), "99"));
        String text = (String) ReflectionTestUtils.getField(sql, "text");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) ReflectionTestUtils.getField(sql, "params");

        assertTrue(text.contains("i.tenant_id=:tenant AND i.deleted=b'0' AND c.status=:approvedStatus"));
        assertTrue(text.contains("i.warehouse_id IN (:departmentWarehouseIds)"));
        assertTrue(text.contains("i.warehouse_id IN (:selfWarehouseIds) AND c.creator=:selfCreator"));
        assertTrue(text.contains("p.purchase_price AS cost_unit_price"));
        assertTrue(text.contains("i.product_price AS adjusted_unit_price"));
        assertTrue(text.contains("i.total_price AS profit_loss_amount"));
        assertTrue(text.contains("NULL AS related_no"));
        assertEquals(1L, params.get("tenant"));
        assertEquals(20, params.get("approvedStatus"));
        assertEquals("%刹车%片%", params.get("keyword"));
        assertEquals(10L, params.get("warehouseId"));
    }

    @Test
    void shouldWhitelistRemoteSortAndAvoidScopePredicateForAllAccess() {
        Filter filter = new Filter();
        filter.setOrderField("r.item_id DESC; DROP TABLE erp_stock_check");
        filter.setOrderDirection("asc");
        String fallbackOrder = ReflectionTestUtils.invokeMethod(repository, "orderBy", filter);
        assertEquals(" ORDER BY r.check_date DESC,r.check_id DESC,r.item_id ASC", fallbackOrder);

        filter.setOrderField("profitLossAmount");
        String allowedOrder = ReflectionTestUtils.invokeMethod(repository, "orderBy", filter);
        assertEquals(" ORDER BY r.profit_loss_amount ASC,r.check_id DESC,r.item_id ASC", allowedOrder);

        Object sql = repository.base(new Filter(), new ErpStockProfitLossDetailReportRepository.Scope(
                1L, true, Collections.emptyList(), Collections.emptyList(), null));
        String text = (String) ReflectionTestUtils.getField(sql, "text");
        assertFalse(text.contains("departmentWarehouseIds"));
        assertFalse(text.contains("selfWarehouseIds"));
    }
}
