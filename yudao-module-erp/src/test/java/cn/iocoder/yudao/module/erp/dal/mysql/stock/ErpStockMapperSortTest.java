package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockMapperSortTest {

    @Test
    void shouldWhitelistSharedStockSelectorFields() {
        assertTrue(ErpStockMapper.getOrderExpression("warehouseName", null).contains("erp_warehouse"));
        assertTrue(ErpStockMapper.getOrderExpression("deptName", null).contains("system_dept"));
        assertEquals("erp_stock.count", ErpStockMapper.getOrderExpression("count", null));
        assertNotNull(ErpStockMapper.getOrderExpression("pendingInCount", null));
        assertTrue(ErpStockMapper.getOrderExpression("availableCount", null).contains("erp_sale_cart_items"));
        assertTrue(ErpStockMapper.getOrderExpression("availableCount", null).contains("erp_stock.count"));
        assertNotNull(ErpStockMapper.getOrderExpression("lastPurchasePrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("retailPrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("productPurchasePrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("minPrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("grossProfitRate", null));
        assertNotNull(ErpStockMapper.getOrderExpression("backupPrice1", null));
        assertNotNull(ErpStockMapper.getOrderExpression("wholesalePrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("sharePrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("featureCode", null));
        assertNotNull(ErpStockMapper.getOrderExpression("weight", null));
        assertNull(ErpStockMapper.getOrderExpression("lastSalePrice", null));
        assertNull(ErpStockMapper.getOrderExpression("id desc; delete from erp_stock", null));
    }

    @Test
    void optimizedAvailableCountSortSqlAggregatesOccupiedSourcesByCandidateStock() {
        String sql = ErpStockMapper.availableCountSortedSql();

        assertTrue(sql.contains("SELECT * FROM erp_stock"));
        assertTrue(sql.contains("INNER JOIN (SELECT * FROM erp_stock"));
        assertTrue(sql.contains("GROUP BY occupied_source.product_id, occupied_source.warehouse_id"));
        assertTrue(sql.contains("SUM(occupied_source.occupied_count_delta) AS occupied_count"));
        assertTrue(sql.contains("ORDER BY (COALESCE(erp_stock.count, 0) - COALESCE(occupied.occupied_count, 0))"));
        assertTrue(sql.contains("erp_sale_cart_items"));
        assertTrue(sql.contains("erp_sale_out_items"));
        assertTrue(sql.contains("erp_purchase_return_items"));
        assertTrue(sql.contains("erp_stock_out_item"));
        assertTrue(sql.contains("erp_stock_move_item"));
        assertTrue(sql.contains("erp_warehouse_move_item"));
        assertTrue(sql.contains("erp_stock_check_item"));
        assertTrue(sql.contains("erp_stock_out_bill_item"));
    }

    @Test
    void shouldUseOptimizedAvailableCountSortOnlyForExplicitDirection() {
        cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO reqVO =
                new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO();
        reqVO.setOrderField("availableCount");
        reqVO.setOrderDirection("desc");

        assertTrue(ErpStockMapper.isAvailableCountSort(reqVO));
        assertEquals("DESC", ErpStockMapper.normalizeOrderDirection(reqVO.getOrderDirection()));

        reqVO.setOrderDirection("asc");
        assertTrue(ErpStockMapper.isAvailableCountSort(reqVO));
        assertEquals("ASC", ErpStockMapper.normalizeOrderDirection(reqVO.getOrderDirection()));

        reqVO.setOrderDirection("id desc; delete from erp_stock");
        assertTrue(!ErpStockMapper.isAvailableCountSort(reqVO));

        reqVO.setOrderField("count");
        reqVO.setOrderDirection("desc");
        assertTrue(!ErpStockMapper.isAvailableCountSort(reqVO));
    }

    @Test
    void summaryValueReadersReturnZeroForNullRow() {
        assertEquals(0L, ErpStockMapper.getLong(null, "total_rows"));
        assertEquals(BigDecimal.ZERO, ErpStockMapper.getBigDecimal(null, "total_stock_count"));
    }

    @Test
    void productStockListDefaultSortPrioritizesExactProductIds() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setKeywordSearchMode(ErpStockPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_STOCK_LIST);
        reqVO.setExactKeywordProductIds(Arrays.asList(138L, 145L, 138L));
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();

        ErpStockMapper.orderByIfPresent(wrapper, reqVO);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("CASE WHEN erp_stock.product_id IN (138,145) THEN 0 ELSE 1 END ASC"), sql);
        assertTrue(sql.contains("warehouse_id ASC"), sql);
        assertTrue(sql.contains("id DESC"), sql);
    }

    @Test
    void productStockListManualSortOverridesExactPriority() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setKeywordSearchMode(ErpStockPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_STOCK_LIST);
        reqVO.setExactKeywordProductIds(Arrays.asList(138L, 145L));
        reqVO.setOrderField("count");
        reqVO.setOrderDirection("asc");
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();

        ErpStockMapper.orderByIfPresent(wrapper, reqVO);

        String sql = wrapper.getCustomSqlSegment();
        assertFalse(sql.contains("CASE WHEN"), sql);
        assertTrue(sql.contains("erp_stock.count ASC"), sql);
        assertTrue(sql.contains("id DESC"), sql);
    }

    @Test
    void legacyStockCallsKeepWarehouseDefaultSort() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setExactKeywordProductIds(Arrays.asList(138L, 145L));
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();

        ErpStockMapper.orderByIfPresent(wrapper, reqVO);

        String sql = wrapper.getCustomSqlSegment();
        assertFalse(sql.contains("CASE WHEN"), sql);
        assertTrue(sql.contains("warehouse_id ASC"), sql);
        assertTrue(sql.contains("id DESC"), sql);
    }
}
