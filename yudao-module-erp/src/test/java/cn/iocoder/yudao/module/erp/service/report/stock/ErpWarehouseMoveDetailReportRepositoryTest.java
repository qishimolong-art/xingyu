package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpWarehouseMoveDetailReportModels.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpWarehouseMoveDetailReportRepositoryTest {

    private final ErpWarehouseMoveDetailReportRepository repository =
            new ErpWarehouseMoveDetailReportRepository();

    @Test
    void shouldBuildApprovedDetailQueryWithCurrentMasterDataAndDeptSelfPermission() {
        Filter filter = filter();
        filter.setApproveTime(new LocalDateTime[]{LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59)});
        filter.setKeyword("制动 片");
        filter.setFromWarehouseId(11L);
        filter.setToWarehouseId(12L);

        Object sql = repository.sql(filter, new ErpWarehouseMoveDetailReportRepository.Scope(
                7L, false, Arrays.asList(21L, 22L), true, 88L,
                Arrays.asList("document_no", "product_code", "product_name", "from_shelf")));
        String body = (String) ReflectionTestUtils.getField(sql, "body");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) ReflectionTestUtils.getField(sql, "params");

        assertTrue(body.contains("FROM erp_warehouse_move_item wmi"));
        assertTrue(body.contains("INNER JOIN erp_warehouse_move wm"));
        assertTrue(body.contains("wmi.deleted=b'0' AND wm.status=:approved"));
        assertTrue(body.contains("LEFT JOIN erp_product p"));
        assertTrue(body.contains("LEFT JOIN erp_product_unit pu"));
        assertTrue(body.contains("LEFT JOIN erp_warehouse fw"));
        assertTrue(body.contains("LEFT JOIN erp_warehouse tw"));
        assertTrue(body.contains("LEFT JOIN system_users creator"));
        assertTrue(body.contains("LEFT JOIN system_users approver"));
        assertTrue(body.contains("wmi.cost_price"));
        assertTrue(body.contains("wmi.cost_amount"));
        assertTrue(body.contains("wmi.product_price move_price"));
        assertTrue(body.contains("wmi.total_price move_amount"));
        assertTrue(body.contains("wm.dept_id IN (:deptIds) OR wm.creator=:selfCreator"));
        assertTrue(body.contains("report.approve_time>=:approveTimeFrom"));
        assertTrue(body.contains("report.from_warehouse_id=:fromWarehouseId"));
        assertTrue(body.contains("report.to_warehouse_id=:toWarehouseId"));
        assertTrue(body.contains("report.product_code LIKE :keyword"));
        assertFalse(body.contains("supplier"));
        assertEquals(7L, params.get("tenantId"));
        assertEquals(20, params.get("approved"));
        assertEquals("88", params.get("selfCreator"));
        assertEquals("%制动%片%", params.get("keyword"));
    }

    @Test
    void shouldWhitelistSortAndDenyEmptyPermissionScope() {
        Filter filter = filter();
        filter.setOrderField("item_id DESC; DROP TABLE erp_warehouse_move_item");
        filter.setOrderDirection("asc");
        assertEquals(" ORDER BY report.approve_time DESC,report.document_no DESC,report.item_id ASC",
                repository.orderBy(filter));

        filter.setOrderField("moveAmount");
        assertEquals(" ORDER BY report.move_amount ASC,report.approve_time DESC,"
                        + "report.document_no DESC,report.item_id ASC",
                repository.orderBy(filter));

        Object denied = repository.sql(filter, new ErpWarehouseMoveDetailReportRepository.Scope(
                1L, false, Collections.emptySet(), false, 9L, Collections.emptyList()));
        assertTrue(((String) ReflectionTestUtils.getField(denied, "body")).contains("AND 1=0"));

        Object all = repository.sql(filter, new ErpWarehouseMoveDetailReportRepository.Scope(
                1L, true, Collections.emptySet(), false, 9L, Collections.emptyList()));
        String allBody = (String) ReflectionTestUtils.getField(all, "body");
        assertFalse(allBody.contains("deptIds"));
        assertFalse(allBody.contains("selfCreator"));
    }

    private Filter filter() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(10);
        return filter;
    }
}
