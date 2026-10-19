package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Filter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.*;
import static org.junit.jupiter.api.Assertions.*;

class ErpStockAgeReportRepositoryTest {

    private final ErpStockAgeReportRepository repository = new ErpStockAgeReportRepository();

    @Test
    void shouldKeepSevenDayRangesContinuousAndNonOverlapping() {
        assertEquals(" AND report.age_days BETWEEN 0 AND 30",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_WITHIN_1_MONTH));
        assertEquals(" AND report.age_days BETWEEN 31 AND 90",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_MONTHS_2_TO_3));
        assertEquals(" AND report.age_days BETWEEN 91 AND 180",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_MONTHS_4_TO_6));
        assertEquals(" AND report.age_days BETWEEN 181 AND 365",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_MONTHS_7_TO_12));
        assertEquals(" AND report.age_days BETWEEN 366 AND 730",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_YEARS_1_TO_2));
        assertEquals(" AND report.age_days BETWEEN 731 AND 1095",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_YEARS_2_TO_3));
        assertEquals(" AND report.age_days>=1096",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_OVER_3_YEARS));
        assertEquals(" AND report.age_days IS NULL",
                ErpStockAgeReportRepository.ageBucketCondition(BUCKET_UNCLASSIFIED));
    }

    @Test
    void shouldAggregateAllNegativeMovementsAndPreferLastOutbound() {
        Filter filter = filter();
        ErpStockAgeReportRepository.Sql sql = repository.reportSql(filter,
                new ErpStockAgeReportRepository.Scope(1L, true,
                        Collections.emptyList(), Collections.emptyList(), null),
                LocalDate.of(2026, 10, 14));

        assertTrue(sql.cte.contains("MAX(CASE WHEN count<0"));
        assertTrue(sql.cte.contains("WHEN m.last_outbound_time IS NOT NULL THEN '最后有效出库'"));
        assertTrue(sql.cte.contains("COALESCE(m.last_outbound_time,m.last_inbound_time)"));
        assertTrue(sql.cte.contains("count>0 AND biz_type=34"));
        assertTrue(sql.cte.contains("s.count>0"));
        assertTrue(sql.cte.contains("s.cost_price cost_unit_price"));
        assertTrue(sql.cte.contains("COALESCE(s.cost_amount,s.count*s.cost_price) cost_amount"));
        assertTrue(sql.cte.contains("WHEN age_days BETWEEN 0 AND 30 THEN stock_quantity ELSE 0"));
        assertTrue(sql.cte.contains("WHEN age_days>=1096 THEN stock_quantity ELSE 0"));
        assertFalse(sql.cte.contains("supplier"));
        assertFalse(sql.cte.contains("purchaser"));
    }

    @Test
    void shouldApplyTenantDeletionAndWarehousePermissionScope() {
        Filter filter = filter();
        filter.setDeptId(10L);
        filter.setWarehouseId(20L);
        filter.setCategoryId(30L);
        filter.setKeyword("制动 片");
        ErpStockAgeReportRepository.Sql sql = repository.reportSql(filter,
                new ErpStockAgeReportRepository.Scope(7L, false,
                        Arrays.asList(20L, 21L), Collections.singletonList(22L), "99"),
                LocalDate.of(2026, 10, 14));

        assertTrue(sql.cte.contains("erp_stock_record WHERE tenant_id=:tenant AND deleted=b'0'"));
        assertTrue(sql.cte.contains("s.tenant_id=:tenant AND s.deleted=b'0'"));
        assertTrue(sql.cte.contains("s.warehouse_id IN (:departmentWarehouseIds)"));
        assertTrue(sql.cte.contains("s.warehouse_id IN (:selfWarehouseIds) AND s.creator=:selfCreator"));
        assertEquals(7L, sql.params.get("tenant"));
        assertEquals("%制动%片%", sql.params.get("keyword"));
        assertEquals(10L, sql.params.get("deptId"));
        assertEquals(20L, sql.params.get("warehouseId"));
        assertEquals(30L, sql.params.get("categoryId"));
    }

    @Test
    void shouldSeparateNeverOutboundHasOutboundAndUnclassifiedFilters() {
        assertEquals(" AND report.last_outbound_time IS NULL",
                ErpStockAgeReportRepository.outboundCondition(OUTBOUND_NEVER));
        assertEquals(" AND report.last_outbound_time IS NOT NULL",
                ErpStockAgeReportRepository.outboundCondition(OUTBOUND_HAS));
        assertEquals("", ErpStockAgeReportRepository.outboundCondition(OUTBOUND_ALL));
        assertEquals("", ErpStockAgeReportRepository.ageBucketCondition(BUCKET_ALL));
    }

    private Filter filter() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(10);
        return filter;
    }
}
