package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpSaleQuoteProgressServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleQuoteProgressService progressService;
    @Mock
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Mock
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Mock
    private ErpSaleCartMapper saleCartMapper;
    @Mock
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;

    @Test
    void getProgressMap_distinguishesOrderedPartialConvertedAndActualOut() {
        List<Long> quoteIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        List<ErpSaleQuoteDO> quotes = Arrays.asList(
                quote(1L, ErpSaleQuoteStatusEnum.APPROVE.getStatus()),
                quote(2L, ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus()),
                quote(3L, ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus()),
                quote(4L, ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()),
                quote(5L, ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()));
        List<ErpSaleQuoteItemDO> items = Arrays.asList(
                item(11L, 1L, "10"), item(12L, 2L, "10"), item(13L, 3L, "10"),
                item(14L, 4L, "10"), item(15L, 5L, "10"));
        List<ErpSaleConvertRecordDO> records = Arrays.asList(
                record(2L, 12L, 102L, "4"),
                record(3L, 13L, 103L, "10"),
                record(4L, 14L, 104L, "10"));
        List<ErpSaleCartDO> carts = Arrays.asList(
                cart(102L, ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                cart(103L, ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                cart(104L, ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus()));
        when(saleQuoteMapper.selectByIds(quoteIds)).thenReturn(quotes);
        when(saleQuoteItemMapper.selectListByQuoteIds(quoteIds)).thenReturn(items);
        when(saleConvertRecordMapper.selectQuoteToCartListByQuoteIds(quoteIds)).thenReturn(records);
        when(saleCartMapper.selectByIds(ArgumentMatchers.<Long>anyCollection())).thenReturn(carts);

        Map<Long, ErpSaleQuoteProgressService.Progress> result = progressService.getProgressMap(quoteIds);

        assertProgress(result.get(1L), 20, 0, "0", "0", "10");
        assertProgress(result.get(2L), 30, 30, "4", "0", "6");
        assertProgress(result.get(3L), 40, 40, "10", "0", "0");
        assertProgress(result.get(4L), 50, 50, "10", "10", "0");
        // 兼容旧版直接审核出库的历史状态 50：没有转换记录也保持为已出库，避免再次出库。
        assertProgress(result.get(5L), 50, 50, "10", "10", "0");
    }

    @Test
    void refreshQuoteProgress_deletedCartReleasesConvertedCountAndReturnsToOrdered() {
        Long quoteId = 6L;
        ErpSaleQuoteDO quote = quote(quoteId, ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus());
        ErpSaleQuoteItemDO item = item(16L, quoteId, "10").setConvertedCount(new BigDecimal("10"));
        ErpSaleConvertRecordDO staleRecord = record(quoteId, item.getId(), 106L, "10");
        when(saleQuoteMapper.selectByIdForUpdate(quoteId)).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(quoteId)).thenReturn(Collections.singletonList(item));
        when(saleConvertRecordMapper.selectQuoteToCartListByQuoteIds(Collections.singleton(quoteId)))
                .thenReturn(Collections.singletonList(staleRecord));
        when(saleCartMapper.selectByIds(Collections.singleton(106L))).thenReturn(Collections.emptyList());

        ErpSaleQuoteProgressService.Progress result = progressService.refreshQuoteProgress(quoteId);

        assertProgress(result, 20, 0, "0", "0", "10");
        verify(saleQuoteItemMapper).updateById(ArgumentMatchers.<ErpSaleQuoteItemDO>argThat(
                update -> item.getId().equals(update.getId())
                && BigDecimal.ZERO.compareTo(update.getConvertedCount()) == 0));
        verify(saleQuoteMapper).updateById(ArgumentMatchers.<ErpSaleQuoteDO>argThat(
                update -> quoteId.equals(update.getId())
                && ErpSaleQuoteStatusEnum.APPROVE.getStatus().equals(update.getStatus())));
    }

    private void assertProgress(ErpSaleQuoteProgressService.Progress progress, int rawStatus, int outStatus,
                                String converted, String actualOut, String remaining) {
        assertEquals(rawStatus, progress.getRawStatus());
        assertEquals(ErpSaleQuoteStatusEnum.APPROVE.getStatus(), progress.getOrderStatus());
        assertEquals(outStatus, progress.getOutStatus());
        assertEquals(0, new BigDecimal(converted).compareTo(progress.getConvertedCount()));
        assertEquals(0, new BigDecimal(actualOut).compareTo(progress.getActualOutCount()));
        assertEquals(0, new BigDecimal(remaining).compareTo(progress.getRemainingCount()));
    }

    private ErpSaleQuoteDO quote(Long id, Integer status) {
        return new ErpSaleQuoteDO().setId(id).setStatus(status);
    }

    private ErpSaleQuoteItemDO item(Long id, Long quoteId, String count) {
        return new ErpSaleQuoteItemDO().setId(id).setQuoteId(quoteId).setCount(new BigDecimal(count));
    }

    private ErpSaleConvertRecordDO record(Long quoteId, Long quoteItemId, Long cartId, String count) {
        return new ErpSaleConvertRecordDO().setSourceId(quoteId).setSourceItemId(quoteItemId)
                .setTargetId(cartId).setCount(new BigDecimal(count));
    }

    private ErpSaleCartDO cart(Long id, Integer status) {
        return new ErpSaleCartDO().setId(id).setStatus(status);
    }
}
