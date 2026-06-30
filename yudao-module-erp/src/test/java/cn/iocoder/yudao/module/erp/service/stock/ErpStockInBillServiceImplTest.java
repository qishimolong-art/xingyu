package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillPickupRecordMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_BILL_PICKUP_COUNT_EXCEED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpStockInBillServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockInBillServiceImpl stockInBillService;

    @Mock
    private ErpStockInBillMapper stockInBillMapper;
    @Mock
    private ErpStockInBillItemMapper stockInBillItemMapper;
    @Mock
    private ErpStockInBillPickupRecordMapper pickupRecordMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private AdminUserApi adminUserApi;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(stockInBillService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260629000001";
            }
        });
    }

    @Test
    public void testCreateFromPurchaseIn_success() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001")
                .setInTime(LocalDateTime.of(2026, 6, 29, 10, 0, 0));
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setWarehouseId(20L).setProductId(30L).setProductUnitId(40L)
                .setCount(new BigDecimal("5")).setProductPrice(new BigDecimal("12.34"));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(20L,
                new ErpWarehouseDO().setId(20L).setName("主仓")));
        when(stockInBillMapper.selectByNo(any())).thenReturn(null);

        stockInBillService.createFromPurchaseIn(purchaseIn, Collections.singletonList(item));

        ArgumentCaptor<ErpStockInBillDO> billCaptor = ArgumentCaptor.forClass(ErpStockInBillDO.class);
        verify(stockInBillMapper).insert(billCaptor.capture());
        assertEquals("RCD20260629000001", billCaptor.getValue().getNo());
        assertEquals(0, billCaptor.getValue().getTotalCount().compareTo(new BigDecimal("5")));

        ArgumentCaptor<java.util.List<ErpStockInBillItemDO>> itemsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(stockInBillItemMapper).insertBatch(itemsCaptor.capture());
        assertEquals(Long.valueOf(100L), itemsCaptor.getValue().get(0).getSourceItemId());
        assertEquals(0, itemsCaptor.getValue().get(0).getPickedCount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testPickup_success_createStockRecord() {
        ErpStockInBillDO bill = new ErpStockInBillDO()
                .setId(1L).setNo("RCD001").setSourceId(10L).setSourceNo("CGRK001")
                .setStatus(ErpStockInBillServiceImpl.STATUS_WAIT_PICKUP);
        when(stockInBillMapper.selectById(eq(1L))).thenReturn(bill);
        ErpStockInBillItemDO item = new ErpStockInBillItemDO()
                .setId(2L).setBillId(1L).setSourceId(10L).setSourceItemId(100L)
                .setProductId(200L).setWarehouseId(300L)
                .setCount(new BigDecimal("5")).setPickedCount(BigDecimal.ZERO)
                .setProductPrice(new BigDecimal("12.34"));
        when(stockInBillItemMapper.selectListByBillId(eq(1L))).thenReturn(Collections.singletonList(item));
        ErpStockInBillPickupReqVO reqVO = new ErpStockInBillPickupReqVO();
        reqVO.setId(1L);
        ErpStockInBillPickupReqVO.Item reqItem = new ErpStockInBillPickupReqVO.Item();
        reqItem.setItemId(2L);
        reqItem.setPickupCount(new BigDecimal("3"));
        reqVO.setItems(Collections.singletonList(reqItem));

        stockInBillService.pickup(reqVO);

        ArgumentCaptor<ErpStockRecordCreateReqBO> recordCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService).createStockRecord(recordCaptor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.PURCHASE_IN.getType(), recordCaptor.getValue().getBizType());
        assertEquals(0, recordCaptor.getValue().getCount().compareTo(new BigDecimal("3")));
        assertEquals(Long.valueOf(10L), recordCaptor.getValue().getBizId());
        assertEquals(Long.valueOf(100L), recordCaptor.getValue().getBizItemId());
        verify(stockInBillItemMapper).updateById(any(ErpStockInBillItemDO.class));
        verify(stockInBillMapper).updateById(any(ErpStockInBillDO.class));
        verify(pickupRecordMapper).insertBatch(any());
    }

    @Test
    public void testPickup_countExceed_throwException() {
        when(stockInBillMapper.selectById(eq(1L))).thenReturn(new ErpStockInBillDO()
                .setId(1L).setNo("RCD001").setStatus(ErpStockInBillServiceImpl.STATUS_WAIT_PICKUP));
        when(stockInBillItemMapper.selectListByBillId(eq(1L))).thenReturn(Collections.singletonList(
                new ErpStockInBillItemDO().setId(2L).setBillId(1L)
                        .setCount(new BigDecimal("5")).setPickedCount(new BigDecimal("4"))));
        ErpStockInBillPickupReqVO reqVO = new ErpStockInBillPickupReqVO();
        reqVO.setId(1L);
        ErpStockInBillPickupReqVO.Item reqItem = new ErpStockInBillPickupReqVO.Item();
        reqItem.setItemId(2L);
        reqItem.setPickupCount(new BigDecimal("2"));
        reqVO.setItems(Collections.singletonList(reqItem));

        assertServiceException(() -> stockInBillService.pickup(reqVO),
                STOCK_IN_BILL_PICKUP_COUNT_EXCEED, 2L, new BigDecimal("2"), new BigDecimal("1"));
        verify(stockRecordService, never()).createStockRecord(any());
    }

}
