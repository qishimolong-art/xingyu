package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordSummaryVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

public class ErpStockRecordServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockRecordServiceImpl stockRecordService;

    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpStockInItemMapper stockInItemMapper;
    @Mock
    private ErpStockOutItemMapper stockOutItemMapper;
    @Mock
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Mock
    private ErpStockCheckItemMapper stockCheckItemMapper;

    @Test
    public void testGetStockRecordSummary_totalPriceFallback() {
        ErpStockRecordDO inRecord = new ErpStockRecordDO()
                .setBizType(10)
                .setBizItemId(1001L)
                .setCount(new BigDecimal("4"))
                .setUnitPrice(new BigDecimal("2.50"))
                .setTotalPrice(null);
        ErpStockRecordDO outRecord = new ErpStockRecordDO()
                .setBizType(20)
                .setBizItemId(1002L)
                .setCount(new BigDecimal("-3"))
                .setUnitPrice(new BigDecimal("5.00"))
                .setTotalPrice(BigDecimal.ZERO);
        when(stockRecordMapper.selectPageWithProductFilter(any(), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Arrays.asList(inRecord, outRecord), 2L));

        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(new ErpStockRecordPageReqVO());

        assertEquals(new BigDecimal("4"), summary.getTotalInCount());
        assertEquals(new BigDecimal("10.00"), summary.getTotalInAmount());
        assertEquals(new BigDecimal("3"), summary.getTotalOutCount());
        assertEquals(new BigDecimal("15.00"), summary.getTotalOutAmount());
        assertEquals(2L, summary.getRecordCount());
    }

    @Test
    public void testGetStockRecordSummary_purchaseInFallbackFromSourceItem() {
        ErpStockRecordDO inRecord = new ErpStockRecordDO()
                .setBizType(ErpStockRecordBizTypeEnum.PURCHASE_IN.getType())
                .setBizItemId(1001L)
                .setCount(new BigDecimal("4"))
                .setUnitPrice(null)
                .setTotalPrice(null);
        when(stockRecordMapper.selectPageWithProductFilter(any(), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(inRecord), 1L));
        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1001L)
                        .setProductPrice(new BigDecimal("2.50"))
                        .setTotalPrice(new BigDecimal("10.00"))));

        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(new ErpStockRecordPageReqVO());

        assertEquals(new BigDecimal("10.00"), summary.getTotalInAmount());
        assertEquals(1L, summary.getRecordCount());
    }

    @Test
    public void testGetStockRecordPage_saleOutFallbackFromSourceItem() {
        ErpStockRecordDO outRecord = new ErpStockRecordDO()
                .setBizType(ErpStockRecordBizTypeEnum.SALE_OUT.getType())
                .setBizItemId(2001L)
                .setCount(new BigDecimal("-3"))
                .setUnitPrice(null)
                .setTotalPrice(null);
        when(stockRecordMapper.selectPageWithProductFilter(any(), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(outRecord), 1L));
        when(saleOutItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                new ErpSaleOutItemDO().setId(2001L)
                        .setProductPrice(new BigDecimal("12.00"))
                        .setTotalPrice(new BigDecimal("36.00"))));

        PageResult<ErpStockRecordDO> page = stockRecordService.getStockRecordPage(new ErpStockRecordPageReqVO());
        ErpStockRecordDO row = page.getList().get(0);

        assertEquals(new BigDecimal("12.00"), row.getUnitPrice());
        assertEquals(new BigDecimal("-36.00"), row.getTotalPrice());
    }

    @Test
    public void testGetStockRecordSummary_emptyProductFilter_returnsEmpty() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setProductCode("P-1");
        when(productMapper.selectMaps(any())).thenReturn(Collections.emptyList());

        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(reqVO);

        assertEquals(BigDecimal.ZERO, summary.getTotalInAmount());
        assertEquals(BigDecimal.ZERO, summary.getTotalOutAmount());
        assertEquals(0L, summary.getRecordCount());
    }

}
