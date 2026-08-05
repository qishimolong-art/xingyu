package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

class ErpStockTransferLedgerServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockTransferLedgerServiceImpl service;

    @Mock
    private ErpStockMoveMapper stockMoveMapper;
    @Mock
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private DeptApi deptApi;

    private final LocalDateTime moveTime = LocalDateTime.of(2026, 7, 20, 10, 0);

    @BeforeEach
    void setUpDefaults() {
        when(stockMoveService.getTransferOutPermissionScope())
                .thenReturn(new ErpStockTransferOutPermissionScope(true, Collections.emptySet()));
        when(stockMoveService.getTransferInPermissionScope())
                .thenReturn(new ErpStockTransferOutPermissionScope(true, Collections.emptySet()));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(
                100L, new ErpProductRespVO().setId(100L).setCode("P100").setName("测试产品")));
        when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(
                java.util.stream.Stream.of(
                                new ErpWarehouseDO().setId(10L).setName("调出仓"),
                                new ErpWarehouseDO().setId(20L).setName("调入仓"))
                        .collect(java.util.stream.Collectors.toMap(ErpWarehouseDO::getId, value -> value)));
        when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

    @Test
    void summary_ignoresStaleInboundWhenOutboundIsUnapproved() {
        ErpStockMoveDO out = move(1L, "OUT-1", 10, 2L, 10);
        ErpStockMoveDO in = move(2L, "IN-1", 20, 1L, 10);
        mockMoves(out, in, Arrays.asList(
                item(11L, 1L, "1.20"),
                item(12L, 1L, "0.80"),
                item(21L, 2L, "2.00")));

        PageResult<ErpStockTransferLedgerSummaryRespVO> result = service.getSummaryPage(request());

        assertEquals(1L, result.getTotal());
        ErpStockTransferLedgerSummaryRespVO summary = result.getList().get(0);
        assertEquals("ABNORMAL", summary.getLedgerStatus());
        assertEquals(0, summary.getTransferOutCount().compareTo(new BigDecimal("2.00")));
        assertEquals(0, summary.getTransferInCount().compareTo(BigDecimal.ZERO));
        assertEquals(0L, summary.getPendingGroupCount());
        assertEquals(1L, summary.getAbnormalGroupCount());

        ErpStockTransferLedgerDetailPageReqVO detailRequest = new ErpStockTransferLedgerDetailPageReqVO();
        detailRequest.setMoveTime(dateRange());
        detailRequest.setBusinessDate(LocalDate.of(2026, 7, 20));
        detailRequest.setPageNo(1);
        detailRequest.setPageSize(20);
        ErpStockTransferLedgerDetailRespVO detail = service.getDetailPage(detailRequest).getList().get(0);
        assertNull(detail.getTransferInId());
        assertEquals("", detail.getTransferInNo());
        assertEquals(0, detail.getTransferInCount().compareTo(BigDecimal.ZERO));
        assertEquals("ABNORMAL", detail.getMatchStatus());
        assertTrue(detail.getExceptionCodes().contains("MISSING_TRANSFER_IN"));
    }

    @Test
    void summary_marksApprovedMatchedTransferAsNormal() {
        ErpStockMoveDO out = move(1L, "OUT-APPROVED", 10, 2L, 20);
        ErpStockMoveDO in = move(2L, "IN-APPROVED", 20, 1L, 20);
        mockMoves(out, in, Arrays.asList(
                item(11L, 1L, "2"),
                item(21L, 2L, "2")));

        ErpStockTransferLedgerSummaryRespVO summary = service.getSummaryPage(request()).getList().get(0);

        assertEquals("NORMAL", summary.getLedgerStatus());
        assertEquals(0, summary.getTransferOutCount().compareTo(new BigDecimal("2")));
        assertEquals(0, summary.getTransferInCount().compareTo(new BigDecimal("2")));
        assertEquals(0L, summary.getAbnormalGroupCount());
        assertEquals(1L, summary.getCompletedGroupCount());
    }

    @Test
    void detail_marksMissingInboundItemAndCountMismatchPrecisely() {
        ErpStockMoveDO out = move(1L, "OUT-1", 10, 2L, 20);
        ErpStockMoveDO in = move(2L, "IN-1", 20, 1L, 20);
        ErpStockMoveItemDO secondOut = item(12L, 1L, "3");
        secondOut.setProductId(101L);
        mockMoves(out, in, Arrays.asList(
                item(11L, 1L, "2"),
                secondOut,
                item(21L, 2L, "1")));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.emptyMap());

        ErpStockTransferLedgerDetailPageReqVO request = new ErpStockTransferLedgerDetailPageReqVO();
        request.setMoveTime(dateRange());
        request.setBusinessDate(LocalDate.of(2026, 7, 20));
        request.setPageNo(1);
        request.setPageSize(20);
        PageResult<ErpStockTransferLedgerDetailRespVO> result = service.getDetailPage(request);

        assertEquals(2L, result.getTotal());
        assertTrue(result.getList().stream().allMatch(row -> "ABNORMAL".equals(row.getMatchStatus())));
        assertTrue(result.getList().stream().anyMatch(row -> row.getExceptionCodes().contains("COUNT_MISMATCH")));
        assertTrue(result.getList().stream().anyMatch(row -> row.getExceptionCodes().contains("ITEM_MISSING_IN")));
    }

    @Test
    void summary_marksUnapprovedOutboundWithoutInboundAsAbnormal() {
        ErpStockMoveDO out = move(1L, "OUT-UNAPPROVED", 10, null, 10);
        when(stockMoveMapper.selectTransferOutList(any(ErpStockMovePageReqVO.class),
                anyCollection(), anyBoolean())).thenReturn(Collections.singletonList(out));
        when(stockMoveMapper.selectTransferInList(any(ErpStockMovePageReqVO.class),
                anyCollection(), anyBoolean())).thenReturn(Collections.emptyList());
        when(stockMoveMapper.selectLedgerRelatedList(anyCollection())).thenReturn(Collections.singletonList(out));
        when(stockMoveItemMapper.selectListByMoveIds(anyCollection()))
                .thenReturn(Collections.singletonList(item(11L, 1L, "2")));

        PageResult<ErpStockTransferLedgerSummaryRespVO> result = service.getSummaryPage(request());

        ErpStockTransferLedgerSummaryRespVO summary = result.getList().get(0);
        assertEquals("ABNORMAL", summary.getLedgerStatus());
        assertEquals(1L, summary.getAbnormalGroupCount());
        assertEquals(0L, summary.getPendingGroupCount());
        assertEquals(0, summary.getDifferenceCount().compareTo(new BigDecimal("2")));

        ErpStockTransferLedgerDetailPageReqVO detailRequest = new ErpStockTransferLedgerDetailPageReqVO();
        detailRequest.setMoveTime(dateRange());
        detailRequest.setBusinessDate(LocalDate.of(2026, 7, 20));
        detailRequest.setPageNo(1);
        detailRequest.setPageSize(20);
        PageResult<ErpStockTransferLedgerDetailRespVO> detail = service.getDetailPage(detailRequest);
        assertEquals("ABNORMAL", detail.getList().get(0).getMatchStatus());
        assertTrue(detail.getList().get(0).getExceptionCodes().contains("MISSING_TRANSFER_IN"));
        assertEquals("缺少调拨入库", detail.getList().get(0).getExceptionReason());
    }

    private void mockMoves(ErpStockMoveDO out, ErpStockMoveDO in, java.util.List<ErpStockMoveItemDO> items) {
        when(stockMoveMapper.selectTransferOutList(any(ErpStockMovePageReqVO.class),
                anyCollection(), anyBoolean())).thenReturn(Collections.singletonList(out));
        when(stockMoveMapper.selectTransferInList(any(ErpStockMovePageReqVO.class),
                anyCollection(), anyBoolean())).thenReturn(Collections.singletonList(in));
        when(stockMoveMapper.selectLedgerRelatedList(anyCollection())).thenReturn(Arrays.asList(out, in));
        when(stockMoveItemMapper.selectListByMoveIds(anyCollection())).thenReturn(items);
    }

    private ErpStockTransferLedgerPageReqVO request() {
        ErpStockTransferLedgerPageReqVO request = new ErpStockTransferLedgerPageReqVO();
        request.setMoveTime(dateRange());
        request.setPageNo(1);
        request.setPageSize(20);
        return request;
    }

    private LocalDateTime[] dateRange() {
        return new LocalDateTime[]{
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59, 59)
        };
    }

    private ErpStockMoveDO move(Long id, String no, Integer direction, Long relatedId, Integer status) {
        return new ErpStockMoveDO().setId(id).setNo(no).setTransferDirection(direction)
                .setRelatedMoveId(relatedId).setMoveTime(moveTime).setStatus(status)
                .setFromDeptId(1L).setToDeptId(2L);
    }

    private ErpStockMoveItemDO item(Long id, Long moveId, String count) {
        return new ErpStockMoveItemDO().setId(id).setMoveId(moveId).setProductId(100L)
                .setFromWarehouseId(10L).setToWarehouseId(20L)
                .setFromDeptId(1L).setToDeptId(2L)
                .setBatchNo("B1").setCount(new BigDecimal(count));
    }

}
