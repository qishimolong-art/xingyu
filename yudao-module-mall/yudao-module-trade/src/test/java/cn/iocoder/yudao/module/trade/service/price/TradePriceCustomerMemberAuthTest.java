package cn.iocoder.yudao.module.trade.service.price;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.promotion.api.discount.DiscountActivityApi;
import cn.iocoder.yudao.module.promotion.api.reward.RewardActivityApi;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeProductSettlementRespVO;
import cn.iocoder.yudao.module.trade.service.price.calculator.TradeDiscountActivityPriceCalculator;
import cn.iocoder.yudao.module.trade.service.price.calculator.TradePriceCalculator;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Customer-member authorization tests for {@link TradePriceServiceImpl}.
 */
public class TradePriceCustomerMemberAuthTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradePriceServiceImpl tradePriceService;

    @Mock
    private ProductSkuApi productSkuApi;
    @Mock
    private ProductSpuApi productSpuApi;
    @Mock
    private DiscountActivityApi discountActivityApi;
    @Mock
    private RewardActivityApi rewardActivityApi;
    @Mock
    private ErpCustomerMemberApi customerMemberApi;
    @Mock
    private List<TradePriceCalculator> priceCalculators;
    @Mock
    private TradeDiscountActivityPriceCalculator discountActivityPriceCalculator;

    @Test
    void calculateProductPrice_notAuthorized() {
        when(customerMemberApi.isCustomerMemberAuthorized(eq(20L))).thenReturn(false);
        when(productSkuApi.getSkuListBySpuId(eq(singletonList(10L)))).thenReturn(asList(
                new ProductSkuRespDTO().setId(100L).setSpuId(10L).setPrice(1000),
                new ProductSkuRespDTO().setId(101L).setSpuId(10L).setPrice(2000)));

        List<AppTradeProductSettlementRespVO> result = tradePriceService.calculateProductPrice(20L, singletonList(10L));

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getSpuId());
        assertFalse(result.get(0).getPriceVisible());
        assertEquals(2, result.get(0).getSkus().size());
        assertEquals(100L, result.get(0).getSkus().get(0).getId());
        assertNull(result.get(0).getSkus().get(0).getPromotionPrice());
        assertNull(result.get(0).getRewardActivity());
        verify(discountActivityApi, never()).getMatchDiscountProductListBySkuIds(anyCollection());
        verify(rewardActivityApi, never()).getMatchRewardActivityListBySpuIds(eq(singletonList(10L)));
    }

    @Test
    void calculateProductPrice_authorized() {
        when(customerMemberApi.isCustomerMemberAuthorized(eq(20L))).thenReturn(true);
        when(productSkuApi.getSkuListBySpuId(eq(singletonList(10L)))).thenReturn(
                singletonList(new ProductSkuRespDTO().setId(100L).setSpuId(10L).setPrice(1000)));
        when(discountActivityPriceCalculator.getMemberLevel(eq(20L))).thenReturn(null);
        when(discountActivityApi.getMatchDiscountProductListBySkuIds(anyCollection())).thenReturn(emptyList());
        when(rewardActivityApi.getMatchRewardActivityListBySpuIds(eq(singletonList(10L)))).thenReturn(emptyList());

        List<AppTradeProductSettlementRespVO> result = tradePriceService.calculateProductPrice(20L, singletonList(10L));

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getSpuId());
        assertTrue(result.get(0).getPriceVisible());
        assertEquals(100L, result.get(0).getSkus().get(0).getId());
    }

}
