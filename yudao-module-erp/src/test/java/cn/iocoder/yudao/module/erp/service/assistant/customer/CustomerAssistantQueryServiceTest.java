package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantProperties;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerAssistantQueryServiceTest {
    private final CustomerAssistantQueryService service=new CustomerAssistantQueryService();

    @Test
    void hiddenPriceMustFailBeforeAnyProductQuery() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.price(
                principal(4,false),Collections.singletonMap("productId",1L)));
        assertEquals("PRICE_HIDDEN",failure.getCode());
    }

    @Test
    void legacyCostPriceLevelMustNeverReachCustomerQuery() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.price(
                principal(6,true),Collections.singletonMap("productId",1L)));
        assertEquals("PRICE_LEVEL_FORBIDDEN",failure.getCode());
    }

    @Test
    void legacyOrderMustNotReuseSameNumericSaleCartDeliveryProgress() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        ErpSalePickDeliveryService deliveryService = mock(ErpSalePickDeliveryService.class);
        AssistantProperties properties = new AssistantProperties();
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        ReflectionTestUtils.setField(service, "pickDeliveryService", deliveryService);
        ReflectionTestUtils.setField(service, "properties", properties);

        Map<String, Object> saleCart = orderRow("SALE_CART", 7L, "SC-7");
        Map<String, Object> legacyOrder = orderRow("LEGACY_ORDER", 7L, "SO-7");
        when(jdbc.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(Arrays.asList(saleCart, legacyOrder));
        ErpSalePickDeliverySummaryRespVO progress = new ErpSalePickDeliverySummaryRespVO();
        progress.setPickStatus(1);
        when(deliveryService.getSummaryMapBySaleCartIds(Collections.singletonList(7L)))
                .thenReturn(Collections.singletonMap(7L, progress));

        Map<String, Object> result = service.orders(principal(4, true), Collections.emptyMap());
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals("拣货中", rows.get(0).get("fulfillmentStage"));
        assertEquals("待履约", rows.get(1).get("fulfillmentStage"));
    }

    private Map<String, Object> orderRow(String kind, Long id, String no) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("source_kind", kind);
        row.put("id", id);
        row.put("no", no);
        row.put("status", 20);
        row.put("order_date", LocalDateTime.now());
        row.put("amount", BigDecimal.TEN);
        row.put("ordered_count", BigDecimal.TEN);
        row.put("out_count", BigDecimal.ZERO);
        row.put("return_count", BigDecimal.ZERO);
        return row;
    }

    private CustomerAssistantPrincipal principal(int priceLevel,boolean visible) {
        return new CustomerAssistantPrincipal(1L,2L,3L,"客户",priceLevel,visible,true,
                Collections.singleton(10L));
    }
}
