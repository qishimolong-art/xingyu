package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpSaleCartServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleCartServiceImpl saleCartService;

    @Mock
    private ErpSaleCartMapper saleCartMapper;
    @Mock
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Mock
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Mock
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Mock
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;
    @Mock
    private ErpSaleOutService saleOutService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleCartService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260509000001";
            }
        });
    }

    @Test
    public void testFinalApproveSaleCart_createGeneratedSaleOutWithCartSource() {
        Long cartId = 11L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000001")
                .setCustomerId(21L)
                .setAccountId(31L)
                .setSaleUserId(41L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(101L)
                .setCartId(cartId)
                .setProductId(201L)
                .setWarehouseId(301L)
                .setProductPrice(new BigDecimal("15.00"))
                .setCount(new BigDecimal("3"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId), eq(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        saleCartService.finalApproveSaleCart(cartId);

        verify(saleOutService).createGeneratedSaleOut(argThat(req -> cart.getCustomerId().equals(req.getCustomerId())
                        && cart.getAccountId().equals(req.getAccountId())
                        && cart.getSaleUserId().equals(req.getSaleUserId())
                        && cart.getCartTime().equals(req.getOutTime())
                        && req.getItems().size() == 1
                        && item.getProductId().equals(req.getItems().get(0).getProductId())
                        && item.getWarehouseId().equals(req.getItems().get(0).getWarehouseId())
                        && item.getCount().equals(req.getItems().get(0).getCount())),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cart.getId()), eq(cart.getNo()));
    }

    @Test
    public void testConvertToQuote_createQuoteAndMarkCartConverted() {
        Long cartId = 13L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000002")
                .setCustomerId(23L)
                .setAccountId(33L)
                .setSaleUserId(43L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 13, 0))
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(103L)
                .setCartId(cartId)
                .setProductId(203L)
                .setProductUnitId(303L)
                .setWarehouseId(403L)
                .setProductPrice(new BigDecimal("18.00"))
                .setCount(new BigDecimal("5"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(saleQuoteMapper.selectByNo(anyString())).thenReturn(null);
        when(saleCartMapper.updateByIdAndStatus(eq(cartId), eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.CONVERTED_QUOTE.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        saleCartService.convertToQuote(cartId);

        verify(saleQuoteMapper).insert(ArgumentMatchers.<ErpSaleQuoteDO>argThat(quote -> cart.getCustomerId().equals(quote.getCustomerId())
                && ErpSaleBizSourceTypeEnum.CART.getType().equals(quote.getSourceType())
                && cart.getId().equals(quote.getSourceId())
                && cart.getNo().equals(quote.getSourceNo())
                && ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())));
        verify(saleQuoteItemMapper).insertBatch(argThat((java.util.List<ErpSaleQuoteItemDO> items) -> items.size() == 1
                && item.getProductId().equals(items.get(0).getProductId())
                && new BigDecimal("5").compareTo(items.get(0).getCount()) == 0));
        verify(saleConvertRecordMapper).insertBatch(argThat(records -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO> list = new ArrayList<>(records);
            return list.size() == 1 && item.getId().equals(list.get(0).getSourceItemId())
                    && new BigDecimal("5").compareTo(list.get(0).getCount()) == 0;
        }));
    }

}
