package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ErpSaleQuoteServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleQuoteServiceImpl saleQuoteService;

    @Mock
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Mock
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Mock
    private ErpSaleCartMapper saleCartMapper;
    @Mock
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Mock
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpStockService stockService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleQuoteService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260509000001";
            }
        });
    }

    @Test
    public void testApproveSaleQuote_createGeneratedSaleOutWithQuoteSource() {
        Long quoteId = 10L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000001")
                .setCustomerId(20L)
                .setAccountId(30L)
                .setSaleUserId(40L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 10, 0))
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(100L)
                .setQuoteId(quoteId)
                .setProductId(200L)
                .setWarehouseId(300L)
                .setProductPrice(new BigDecimal("12.50"))
                .setCount(new BigDecimal("2"));
        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteId(eq(quoteId))).thenReturn(Collections.singletonList(item));
        when(saleQuoteMapper.updateByIdAndStatus(eq(quoteId), eq(ErpSaleQuoteStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        saleQuoteService.approveSaleQuote(quoteId);

        verify(saleOutService).createGeneratedSaleOut(argThat(req -> quote.getCustomerId().equals(req.getCustomerId())
                        && quote.getAccountId().equals(req.getAccountId())
                        && quote.getSaleUserId().equals(req.getSaleUserId())
                        && quote.getQuoteTime().equals(req.getOutTime())
                        && req.getItems().size() == 1
                        && item.getProductId().equals(req.getItems().get(0).getProductId())
                        && item.getWarehouseId().equals(req.getItems().get(0).getWarehouseId())
                        && item.getCount().equals(req.getItems().get(0).getCount())),
                eq(ErpSaleBizSourceTypeEnum.QUOTE.getType()), eq(quote.getId()), eq(quote.getNo()));
    }

    @Test
    public void testConvertToCart_partItems_createCartAndUpdateConvertedCount() {
        Long quoteId = 12L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000002")
                .setCustomerId(22L)
                .setAccountId(32L)
                .setSaleUserId(42L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 12, 0))
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(102L)
                .setQuoteId(quoteId)
                .setProductId(202L)
                .setProductUnitId(302L)
                .setWarehouseId(402L)
                .setProductPrice(new BigDecimal("20.00"))
                .setCount(new BigDecimal("10"))
                .setConvertedCount(new BigDecimal("3"));
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteConvertCartReqVO.Item()
                .setQuoteItemId(item.getId()).setCount(new BigDecimal("4"))));
        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteId(eq(quoteId))).thenReturn(Collections.singletonList(item));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        when(saleQuoteMapper.updateByIdAndStatus(eq(quoteId), eq(ErpSaleQuoteStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        saleQuoteService.convertToCart(reqVO);

        verify(saleCartMapper).insert(ArgumentMatchers.<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO>argThat(cart -> quote.getCustomerId().equals(cart.getCustomerId())
                && ErpSaleBizSourceTypeEnum.QUOTE.getType().equals(cart.getSourceType())
                && quote.getId().equals(cart.getSourceId())
                && quote.getNo().equals(cart.getSourceNo())));
        verify(saleCartItemMapper).insertBatch(argThat(items -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> list = new ArrayList<>(items);
            return list.size() == 1 && item.getProductId().equals(list.get(0).getProductId())
                    && new BigDecimal("4").compareTo(list.get(0).getCount()) == 0;
        }));
        verify(saleQuoteItemMapper).updateById(ArgumentMatchers.<ErpSaleQuoteItemDO>argThat(update -> item.getId().equals(update.getId())
                && new BigDecimal("7").compareTo(update.getConvertedCount()) == 0));
        verify(saleConvertRecordMapper).insertBatch(argThat(records -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO> list = new ArrayList<>(records);
            return list.size() == 1 && item.getId().equals(list.get(0).getSourceItemId())
                    && new BigDecimal("4").compareTo(list.get(0).getCount()) == 0;
        }));
    }

}
