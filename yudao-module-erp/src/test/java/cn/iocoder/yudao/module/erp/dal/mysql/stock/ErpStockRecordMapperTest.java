package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockRecordMapperTest {

    @Test
    void testGetOrderExpression_supportsReportColumns() {
        assertEquals("erp_stock_record.create_time", ErpStockRecordMapper.getOrderExpression("approveTime"));
        assertEquals("erp_stock_record.biz_date", ErpStockRecordMapper.getOrderExpression("bizDate"));
        assertTrue(ErpStockRecordMapper.getOrderExpression("productName").contains("erp_product"));
        assertTrue(ErpStockRecordMapper.getOrderExpression("warehouseName").contains("erp_warehouse"));
        assertTrue(ErpStockRecordMapper.getOrderExpression("deptName").contains("system_dept"));
        assertTrue(ErpStockRecordMapper.getOrderExpression("inCount").contains("count > 0"));
        assertTrue(ErpStockRecordMapper.getOrderExpression("outAmount").contains("ABS"));
        assertEquals("erp_stock_record.cost_amount", ErpStockRecordMapper.getOrderExpression("costAmount"));
    }

    @Test
    void testAppendDateFilters_supportsBusinessAndApprovalDates() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setBizDate(new LocalDateTime[]{
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59)
        });
        reqVO.setApproveTime(new LocalDateTime[]{
                LocalDateTime.of(2026, 10, 10, 0, 0),
                LocalDateTime.of(2026, 10, 20, 23, 59, 59)
        });
        QueryWrapperX<ErpStockRecordDO> wrapper = new QueryWrapperX<>();

        ErpStockRecordMapper.appendDateFilters(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("biz_date BETWEEN"));
        assertTrue(sql.contains("create_time BETWEEN"));
    }

    @Test
    void testGetOrderExpression_rejectsUnknownField() {
        assertNull(ErpStockRecordMapper.getOrderExpression("id desc; delete from erp_stock_record"));
        assertNull(ErpStockRecordMapper.getOrderExpression(null));
    }

    @Test
    void testOrderByIfPresent_appliesRequestedDirectionAndStableOrder() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setOrderField("productCode");
        reqVO.setOrderDirection("asc");
        QueryWrapperX<ErpStockRecordDO> wrapper = new QueryWrapperX<>();

        ErpStockRecordMapper.orderByIfPresent(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("erp_product"));
        assertTrue(sql.contains("ASC"));
        assertTrue(sql.contains("id DESC"));
    }

    @Test
    void testOrderByIfPresent_supportsApprovalTimeAndStableOrder() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setOrderField("approveTime");
        reqVO.setOrderDirection("desc");
        QueryWrapperX<ErpStockRecordDO> descWrapper = new QueryWrapperX<>();

        ErpStockRecordMapper.orderByIfPresent(descWrapper, reqVO);

        String descSql = descWrapper.getSqlSegment();
        assertTrue(descSql.contains("create_time DESC"));
        assertTrue(descSql.contains("id DESC"));

        reqVO.setOrderDirection("asc");
        QueryWrapperX<ErpStockRecordDO> ascWrapper = new QueryWrapperX<>();

        ErpStockRecordMapper.orderByIfPresent(ascWrapper, reqVO);

        String ascSql = ascWrapper.getSqlSegment();
        assertTrue(ascSql.contains("create_time ASC"));
        assertTrue(ascSql.contains("id DESC"));
    }

    @Test
    void testOrderByIfPresent_invalidInputFallsBackToIdDesc() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setOrderField("unknown");
        reqVO.setOrderDirection("asc");
        QueryWrapperX<ErpStockRecordDO> wrapper = new QueryWrapperX<>();

        ErpStockRecordMapper.orderByIfPresent(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("id DESC"));
        assertFalse(sql.contains("unknown"));
    }

    @Test
    void testAppendBatchNoFilter_supportsExactAndUnassignedBatch() {
        ErpStockRecordPageReqVO exactReqVO = new ErpStockRecordPageReqVO();
        exactReqVO.setBatchNo("B-001");
        QueryWrapperX<ErpStockRecordDO> exactWrapper = new QueryWrapperX<>();
        ErpStockRecordMapper.appendBatchNoFilter(exactWrapper, exactReqVO);
        assertTrue(exactWrapper.getSqlSegment().contains("batch_no"));

        ErpStockRecordPageReqVO unassignedReqVO = new ErpStockRecordPageReqVO();
        unassignedReqVO.setUnassignedBatch(true);
        QueryWrapperX<ErpStockRecordDO> unassignedWrapper = new QueryWrapperX<>();
        ErpStockRecordMapper.appendBatchNoFilter(unassignedWrapper, unassignedReqVO);
        assertTrue(unassignedWrapper.getSqlSegment().contains("batch_no IS NULL"));
        assertTrue(unassignedWrapper.getSqlSegment().contains("TRIM(batch_no) = ''"));
    }
}
