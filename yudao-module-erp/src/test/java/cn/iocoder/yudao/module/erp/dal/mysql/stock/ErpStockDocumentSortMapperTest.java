package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockDocumentSortMapperTest {

    @Test
    void testWhitelists_supportConfiguredColumns() {
        assertNotNull(ErpStockInMapper.getOrderColumn("inTime"));
        assertNotNull(ErpStockOutMapper.getOrderColumn("outTime"));
        assertNotNull(ErpStockCheckMapper.getOrderColumn("checkType"));
        assertNotNull(ErpStockCheckMapper.getOrderColumn("totalCount"));
        assertNotNull(ErpStockCheckMapper.getOrderExpression("productNames"));
        assertNotNull(ErpStockCheckMapper.getOrderExpression("deptName"));
        assertNotNull(ErpStockCheckMapper.getOrderExpression("creatorName"));
        assertNotNull(ErpStockMoveMapper.getOrderColumn("relatedMoveNo"));
        assertNotNull(ErpStockMoveMapper.getOrderColumn("totalCount"));
        assertNotNull(ErpStockMoveMapper.getOrderColumn("totalPrice"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("fromDeptName"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("toDeptName"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("fromWarehouseNames"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("toWarehouseNames"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("productNames"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("productCodes"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("creatorName"));
        assertNotNull(ErpStockMoveMapper.getOrderExpression("updaterName"));
        assertNotNull(ErpStockInBillMapper.getOrderColumn("printCount"));
        assertNotNull(ErpStockOutBillMapper.getOrderColumn("shippingArea"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderColumn("totalCostAmount"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("fromWarehouseName"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("toWarehouseName"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("productNames"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("productCodes"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("deptName"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("handlerName"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("itemCount"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("approveUserName"));
        assertNotNull(ErpWarehouseMoveMapper.getOrderExpression("creatorName"));
    }

    @Test
    void testWhitelists_rejectUnknownAndInjectionLikeFields() {
        String unsafe = "id desc; delete from erp_stock_move";
        assertNull(ErpStockInMapper.getOrderColumn(unsafe));
        assertNull(ErpStockOutMapper.getOrderColumn(unsafe));
        assertNull(ErpStockCheckMapper.getOrderColumn(unsafe));
        assertNull(ErpStockCheckMapper.getOrderExpression(unsafe));
        assertNull(ErpStockMoveMapper.getOrderColumn(unsafe));
        assertNull(ErpStockMoveMapper.getOrderExpression(unsafe));
        assertNull(ErpStockInBillMapper.getOrderColumn(unsafe));
        assertNull(ErpStockOutBillMapper.getOrderColumn(unsafe));
        assertNull(ErpWarehouseMoveMapper.getOrderColumn(unsafe));
        assertNull(ErpWarehouseMoveMapper.getOrderExpression(unsafe));
    }

    @Test
    void testMoveOrder_appliesDirectionAndStableTieBreaker() {
        ErpStockMovePageReqVO reqVO = new ErpStockMovePageReqVO();
        reqVO.setOrderField("moveTime");
        reqVO.setOrderDirection("asc");
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("moveTime"), sql);
        assertTrue(sql.contains("ASC"), sql);
        assertTrue(sql.contains("id DESC"), sql);
    }

    @Test
    void testMoveOrder_invalidDirectionFallsBackSafely() {
        ErpStockMovePageReqVO reqVO = new ErpStockMovePageReqVO();
        reqVO.setOrderField("moveTime");
        reqVO.setOrderDirection("asc; drop table erp_stock_move");
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("id DESC"));
        assertTrue(!sql.contains("drop table"));
    }

    @Test
    void testMoveRelatedOrder_appliesExpressionAndStableTieBreaker() {
        ErpStockMovePageReqVO reqVO = new ErpStockMovePageReqVO();
        reqVO.setOrderField("productNames");
        reqVO.setOrderDirection("desc");
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("erp_product"), sql);
        assertTrue(sql.contains("DESC"), sql);
        assertTrue(sql.contains("t.id DESC"), sql);
    }

    @Test
    void testWarehouseMoveRelatedOrder_appliesExpressionAndStableTieBreaker() {
        ErpWarehouseMovePageReqVO reqVO = new ErpWarehouseMovePageReqVO();
        reqVO.setOrderField("fromWarehouseName");
        reqVO.setOrderDirection("asc");
        MPJLambdaWrapperX<ErpWarehouseMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpWarehouseMoveMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("erp_warehouse"), sql);
        assertTrue(sql.contains("ASC"), sql);
        assertTrue(sql.contains("t.id DESC"), sql);
    }

    @Test
    void testWarehouseMoveOrder_invalidDirectionFallsBackSafely() {
        ErpWarehouseMovePageReqVO reqVO = new ErpWarehouseMovePageReqVO();
        reqVO.setOrderField("fromWarehouseName");
        reqVO.setOrderDirection("asc; drop table erp_warehouse_move");
        MPJLambdaWrapperX<ErpWarehouseMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpWarehouseMoveMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("id DESC"), sql);
        assertTrue(!sql.contains("drop table"));
    }
}
