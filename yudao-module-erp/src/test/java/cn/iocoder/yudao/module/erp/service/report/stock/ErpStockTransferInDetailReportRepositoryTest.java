package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockTransferInDetailReportRepositoryTest {

    private final ErpStockTransferInDetailReportRepository repository =
            new ErpStockTransferInDetailReportRepository();

    @Test
    void shouldBuildApprovedTransferInDetailQueryWithCurrentMasterDataAndFullDeptScope() {
        Filter filter = filter();
        filter.setMoveDate(new LocalDateTime[]{LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59)});
        filter.setDocumentNo("DBRK");
        filter.setProductKeyword("制动 片");
        filter.setFromWarehouseId(11L);
        filter.setToWarehouseId(12L);
        filter.setCountMin(new BigDecimal("1.00"));
        filter.setPriceMax(new BigDecimal("99.99"));
        filter.setAmountMin(new BigDecimal("20.00"));
        filter.setCreatorId(88L);

        Object sql = repository.sql(filter, new ErpStockTransferInDetailReportRepository.Scope(
                7L, new ErpStockTransferOutPermissionScope(false, Arrays.asList(21L, 22L)),
                Arrays.asList("product_code", "product_name", "standard")));
        String body = (String) ReflectionTestUtils.getField(sql, "body");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) ReflectionTestUtils.getField(sql, "params");

        assertTrue(body.contains("sm.transfer_direction=:transferIn AND sm.status=:approved"));
        assertTrue(body.contains("sm.tenant_id=smi.tenant_id AND sm.deleted=b'0'"));
        assertTrue(body.contains("FROM erp_stock_move_item smi"));
        assertTrue(body.contains("LEFT JOIN erp_product p"));
        assertTrue(body.contains("p.deleted=b'0'"));
        assertTrue(body.contains("LEFT JOIN erp_warehouse fw"));
        assertTrue(body.contains("LEFT JOIN erp_warehouse tw"));
        assertTrue(body.contains("COALESCE(smi.total_price,ROUND(smi.count*smi.product_price,6)) total_amount"));
        assertTrue(body.contains("smi.product_price cost_unit_price"));
        assertTrue(body.contains("sm.dept_id IN (:deptIds)"));
        assertTrue(body.contains("sm.from_dept_id IN (:deptIds)"));
        assertTrue(body.contains("sm.to_dept_id IN (:deptIds)"));
        assertTrue(body.contains("smi.from_dept_id IN (:deptIds)"));
        assertTrue(body.contains("smi.to_dept_id IN (:deptIds)"));
        assertTrue(body.contains("report.product_code LIKE :productKeyword"));
        assertTrue(body.contains("report.product_name LIKE :productKeyword"));
        assertFalse(body.contains("supplier"));
        assertEquals(7L, params.get("tenantId"));
        assertEquals(20, params.get("transferIn"));
        assertEquals(20, params.get("approved"));
        assertEquals("%制动%片%", params.get("productKeyword"));
        assertEquals("88", params.get("creatorId"));
    }

    @Test
    void shouldWhitelistSortAndDenyEmptyDepartmentScope() {
        Filter filter = filter();
        filter.setOrderField("item_id DESC; DROP TABLE erp_stock_move");
        filter.setOrderDirection("asc");
        String fallback = ReflectionTestUtils.invokeMethod(repository, "orderBy", filter);
        assertEquals(" ORDER BY report.move_date DESC,report.document_no DESC,report.item_id ASC", fallback);

        filter.setOrderField("totalAmount");
        String allowed = ReflectionTestUtils.invokeMethod(repository, "orderBy", filter);
        assertEquals(" ORDER BY report.total_amount ASC,report.move_date DESC,report.document_no DESC,report.item_id ASC",
                allowed);

        Object deniedSql = repository.sql(filter, new ErpStockTransferInDetailReportRepository.Scope(
                1L, new ErpStockTransferOutPermissionScope(false, Collections.emptySet()),
                Collections.emptyList()));
        String deniedBody = (String) ReflectionTestUtils.getField(deniedSql, "body");
        assertTrue(deniedBody.contains("AND 1=0"));

        Object allSql = repository.sql(filter, new ErpStockTransferInDetailReportRepository.Scope(
                1L, new ErpStockTransferOutPermissionScope(true, Collections.emptySet()),
                Collections.emptyList()));
        String allBody = (String) ReflectionTestUtils.getField(allSql, "body");
        assertFalse(allBody.contains("deptIds"));
    }

    @Test
    void shouldKeepExactExcelColumnOrderAndExcludeTechnicalFields() throws NoSuchFieldException {
        String[] fields = {"moveDate", "documentNo", "productCode", "productName", "vehicleModel",
                "originPlace", "toWarehouseName", "count", "productPrice", "totalAmount",
                "costUnitPrice", "costAmount", "creatorName", "fromWarehouseName", "standard", "drawingNo"};
        for (int index = 0; index < fields.length; index++) {
            ExcelProperty property = Row.class.getDeclaredField(fields[index]).getAnnotation(ExcelProperty.class);
            assertNotNull(property, fields[index] + " must be exported");
            assertEquals(index, property.index(), fields[index] + " export order");
        }
        for (String field : new String[]{"moveId", "itemId", "productId", "fromWarehouseId",
                "toWarehouseId", "priceVisible", "amountVisible", "hiddenFields"}) {
            assertNull(Row.class.getDeclaredField(field).getAnnotation(ExcelProperty.class),
                    field + " must stay out of Excel");
        }
    }

    private Filter filter() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(10);
        return filter;
    }
}
