package cn.iocoder.yudao.module.trade.service.order.handler;

import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TradeProductSkuOrderHandlerTest {

    private ProductSkuApi productSkuApi;
    private TradeProductSkuOrderHandler handler;

    @BeforeEach
    void setUp() {
        productSkuApi = mock(ProductSkuApi.class);
        handler = new TradeProductSkuOrderHandler();
        ReflectionTestUtils.setField(handler, "productSkuApi", productSkuApi);
    }

    @Test
    void beforeOrderCreate_whenErpStockSelected_shouldSkipMallSkuStock() {
        handler.beforeOrderCreate(new TradeOrderDO(),
                Collections.singletonList(buildOrderItem(14626L, 1, 1001L)));

        verifyNoInteractions(productSkuApi);
    }

    @Test
    void beforeOrderCreate_whenMixedStockItems_shouldOnlyUpdateMallSkuStockItems() {
        handler.beforeOrderCreate(new TradeOrderDO(), Arrays.asList(
                buildOrderItem(14626L, 1, 1001L),
                buildOrderItem(14627L, 2, null)));

        verify(productSkuApi).updateSkuStock(argThat(reqDTO ->
                reqDTO.getItems().size() == 1
                        && reqDTO.getItems().get(0).getId().equals(14627L)
                        && reqDTO.getItems().get(0).getIncrCount().equals(-2)));
    }

    @Test
    void afterCancelOrderItem_whenErpStockSelected_shouldSkipMallSkuStock() {
        handler.afterCancelOrderItem(new TradeOrderDO(), buildOrderItem(14626L, 1, 1001L));

        verifyNoInteractions(productSkuApi);
    }

    private TradeOrderItemDO buildOrderItem(Long skuId, Integer count, Long stockId) {
        return new TradeOrderItemDO()
                .setSkuId(skuId)
                .setCount(count)
                .setStockId(stockId);
    }

}
