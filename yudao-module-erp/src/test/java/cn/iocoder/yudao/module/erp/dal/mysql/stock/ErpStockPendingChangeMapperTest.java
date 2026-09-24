package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockPendingChangeMapperTest {

    @Test
    void pendingInExpression_containsAllConfirmedSourcesAndRemainingBillCount() {
        String expression = ErpStockMapper.pendingInCountExpression();

        assertTrue(expression.contains("erp_purchase_in_items"));
        assertTrue(expression.contains("erp_sale_return_items"));
        assertTrue(expression.contains("erp_stock_in_item"));
        assertTrue(expression.contains("erp_stock_move_item"));
        assertTrue(expression.contains("sm.transfer_direction = 20"));
        assertTrue(expression.contains("to_warehouse_id"));
        assertTrue(expression.contains("erp_warehouse_move_item"));
        assertTrue(expression.contains("sci.count > 0"));
        assertTrue(expression.contains("erp_stock_in_bill_item"));
        assertTrue(expression.contains("sibi.count, 0) - COALESCE(sibi.picked_count"));
    }

    @Test
    void occupiedExpression_containsAllConfirmedSourcesAndDoesNotUsePersistedCount() {
        String expression = ErpStockMapper.occupiedCountExpression();

        assertTrue(expression.contains("erp_sale_cart_items"));
        assertTrue(expression.contains("sc.status IN (20,30)"));
        assertFalse(expression.contains("sc.status IN (10,20,30)"));
        assertTrue(expression.contains("NOT EXISTS"));
        assertTrue(expression.contains("erp_sale_out_items"));
        assertTrue(expression.contains("erp_purchase_return_items"));
        assertTrue(expression.contains("erp_stock_out_item"));
        assertTrue(expression.contains("sm.transfer_direction = 10"));
        assertTrue(expression.contains("from_warehouse_id"));
        assertTrue(expression.contains("erp_warehouse_move_item"));
        assertTrue(expression.contains("sci2.count < 0"));
        assertTrue(expression.contains("erp_stock_out_bill_item"));
        assertTrue(expression.contains("sobi.count, 0) - COALESCE(sobi.picked_count"));
        assertFalse(ErpStockMapper.getOrderExpression("occupiedCount", null).contains("erp_stock.occupied_count"));
    }

    @Test
    void inTransitExpression_remainsPurchaseOrderOnly() {
        String expression = ErpStockMapper.inTransitCountExpression();

        assertTrue(expression.contains("erp_purchase_order_items"));
        assertFalse(expression.contains("erp_stock_in_bill"));
        assertFalse(expression.contains("erp_purchase_in_items"));
    }
}
