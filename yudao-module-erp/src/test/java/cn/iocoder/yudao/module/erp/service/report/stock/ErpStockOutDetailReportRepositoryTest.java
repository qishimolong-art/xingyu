package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockOutDetailReportModels.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockOutDetailReportRepositoryTest {

    private final ErpStockOutDetailReportRepository repository = new ErpStockOutDetailReportRepository();

    @Test
    void shouldBuildApprovedDetailQueryWithCostRecordCurrentShelfAndPermission() {
        Filter filter = filter();
        filter.setApproveTime(new LocalDateTime[]{LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59)});
        filter.setDocumentTime(new LocalDateTime[]{LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59)});
        filter.setKeyword("制动 片");
        filter.setWarehouseId(11L);
        filter.setRelatedPartyId(12L);
        filter.setCategoryId(13L);
        filter.setDeptId(14L);
        filter.setBusinessUserId(88L);

        Object sql = repository.sql(filter, new ErpStockOutDetailReportRepository.Scope(
                7L, false, Arrays.asList(21L, 22L), true, 88L,
                Arrays.asList("document_no", "product_code", "product_name", "shelf")));
        String body = (String) ReflectionTestUtils.getField(sql, "body");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) ReflectionTestUtils.getField(sql, "params");

        assertTrue(body.contains("FROM erp_stock_out_item soi"));
        assertTrue(body.contains("INNER JOIN erp_stock_out so"));
        assertTrue(body.contains("soi.deleted=b'0' AND so.status=:approved"));
        assertTrue(body.contains("LEFT JOIN erp_customer customer"));
        assertTrue(body.contains("SELECT MAX(stock_match.id) FROM erp_stock stock_match"));
        assertTrue(body.contains("SELECT MAX(sr_match.id) FROM erp_stock_record sr_match"));
        assertTrue(body.contains("sr_match.biz_type=:otherOut"));
        assertTrue(body.contains("sr_match.biz_item_id=soi.id"));
        assertTrue(body.contains("so.dept_id IN (:scopeDeptIds) OR so.creator=:scopeCreator"));
        assertTrue(body.contains("report.approve_time>=:approveTimeFrom"));
        assertTrue(body.contains("report.document_time>=:documentTimeFrom"));
        assertTrue(body.contains("report.warehouse_id=:warehouseId"));
        assertTrue(body.contains("report.related_party_id=:relatedPartyId"));
        assertTrue(body.contains("report.category_id=:categoryId"));
        assertTrue(body.contains("report.creator=:businessUserId"));
        assertTrue(body.contains("report.product_code LIKE :keyword"));
        assertEquals(7L, params.get("tenantId"));
        assertEquals(20, params.get("approved"));
        assertEquals("88", params.get("scopeCreator"));
        assertEquals("88", params.get("businessUserId"));
        assertEquals("%制动%片%", params.get("keyword"));
    }

    @Test
    void shouldWhitelistSortAndDenyEmptyPermissionScope() {
        Filter filter = filter();
        filter.setOrderField("item_id DESC; DROP TABLE erp_stock_out_item");
        filter.setOrderDirection("asc");
        assertEquals(" ORDER BY report.approve_time DESC,report.document_no DESC,report.item_id ASC",
                repository.orderBy(filter));

        filter.setOrderField("costAmount");
        assertEquals(" ORDER BY report.cost_amount ASC,report.approve_time DESC,"
                        + "report.document_no DESC,report.item_id ASC",
                repository.orderBy(filter));

        Object denied = repository.sql(filter, new ErpStockOutDetailReportRepository.Scope(
                1L, false, Collections.emptySet(), false, 9L, Collections.emptyList()));
        assertTrue(((String) ReflectionTestUtils.getField(denied, "body")).contains("AND 1=0"));

        Object all = repository.sql(filter, new ErpStockOutDetailReportRepository.Scope(
                1L, true, Collections.emptySet(), false, 9L, Collections.emptyList()));
        String allBody = (String) ReflectionTestUtils.getField(all, "body");
        assertFalse(allBody.contains("scopeDeptIds"));
        assertFalse(allBody.contains("scopeCreator"));
    }

    private Filter filter() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(10);
        return filter;
    }
}
