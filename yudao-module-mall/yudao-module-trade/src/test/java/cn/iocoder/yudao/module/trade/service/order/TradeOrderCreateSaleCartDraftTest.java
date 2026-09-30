package cn.iocoder.yudao.module.trade.service.order;

import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.sale.ErpSaleCartApi;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateReqDTO;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateRespDTO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.service.stock.ErpMallStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpMallAutoWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallAutoWarehouseResultBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.member.api.address.MemberAddressApi;
import cn.iocoder.yudao.module.member.api.address.dto.MemberAddressRespDTO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderCreateRespVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderSettlementReqVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderSettlementRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.cart.CartDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.delivery.DeliveryPickUpStoreDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderItemMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.dal.redis.no.TradeNoRedisDAO;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.framework.order.config.TradeOrderProperties;
import cn.iocoder.yudao.module.trade.service.cart.CartService;
import cn.iocoder.yudao.module.trade.service.delivery.DeliveryPickUpStoreService;
import cn.iocoder.yudao.module.trade.service.order.handler.TradeOrderHandler;
import cn.iocoder.yudao.module.trade.service.price.TradePriceService;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateRespBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeOrderCreateSaleCartDraftTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeOrderUpdateServiceImpl tradeOrderUpdateService;

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private TradeNoRedisDAO tradeNoRedisDAO;
    @Mock
    private CartService cartService;
    @Mock
    private TradePriceService tradePriceService;
    @Mock
    private ErpMallStockService mallStockService;
    @Mock
    private ErpMallAutoWarehouseService autoWarehouseService;
    @Mock
    private ErpCustomerMemberApi customerMemberApi;
    @Mock
    private ErpSaleCartApi saleCartApi;
    @Mock
    private PayOrderApi payOrderApi;
    @Mock
    private TradeOrderProperties tradeOrderProperties;
    @Mock
    private DeliveryPickUpStoreService pickUpStoreService;
    @Mock
    private MemberAddressApi addressApi;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tradeOrderUpdateService, "tradeOrderHandlers",
                Collections.<TradeOrderHandler>emptyList());
        lenient().when(tradeNoRedisDAO.generate(anyString())).thenReturn("MO-001");
        lenient().when(tradeOrderProperties.getPayAppKey()).thenReturn("mall");
        lenient().when(tradeOrderProperties.getPayExpireTime()).thenReturn(Duration.ofDays(1));
        lenient().doAnswer(invocation -> {
            TradeOrderDO order = invocation.getArgument(0);
            order.setId(900L);
            return 1;
        }).when(tradeOrderMapper).insert(any(TradeOrderDO.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createOrder_shouldCreateSaleCartDraftBeforePayAndDeleteCurrentDeptCarts() {
        Long userId = 20L;
        Long deptId = 2L;
        AppTradeOrderCreateReqVO reqVO = new AppTradeOrderCreateReqVO();
        reqVO.setDeptId(deptId);
        reqVO.setDeliveryType(DeliveryTypeEnum.PICK_UP.getType());
        reqVO.setReceiverName("张三");
        reqVO.setReceiverMobile("13800000000");
        reqVO.setItems(Collections.singletonList(new AppTradeOrderSettlementReqVO.Item().setCartId(501L)));
        when(customerMemberApi.validateCustomerMemberAuth(userId, deptId)).thenReturn(
                new ErpCustomerMemberAuthRespDTO().setAuthorized(true).setCustomerId(88L));
        when(cartService.getCartList(eq(userId), eq(deptId), eq(Collections.singleton(501L)))).thenReturn(
                Collections.singletonList(new CartDO().setId(501L).setSkuId(11L).setStockId(301L).setCount(2)));
        when(tradePriceService.calculateOrderPrice(any(TradePriceCalculateReqBO.class))).thenReturn(calculateResp());
        when(mallStockService.validateMallStock(101L, 11L, 301L, 2)).thenReturn(new ErpMallStockOptionBO()
                .setErpProductId(1001L)
                .setWarehouseId(2001L)
                .setWarehouseName("一号仓"));
        when(saleCartApi.createSaleCartDraft(any(ErpSaleCartDraftCreateReqDTO.class))).thenReturn(
                new ErpSaleCartDraftCreateRespDTO()
                        .setId(3001L)
                        .setNo("XSTC-001")
                        .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        when(payOrderApi.createOrder(any())).thenReturn(7001L);

        AppTradeOrderCreateRespVO respVO = tradeOrderUpdateService.createOrder(userId, reqVO);

        assertEquals(900L, respVO.getId());
        assertEquals("MO-001", respVO.getNo());
        assertEquals(7001L, respVO.getPayOrderId());
        assertEquals(3001L, respVO.getSaleCartId());
        assertEquals("XSTC-001", respVO.getSaleCartNo());
        assertEquals(ErpSaleCartStatusEnum.PROCESS.getStatus(), respVO.getSaleCartStatus());

        ArgumentCaptor<ErpSaleCartDraftCreateReqDTO> captor = ArgumentCaptor.forClass(
                ErpSaleCartDraftCreateReqDTO.class);
        verify(saleCartApi).createSaleCartDraft(captor.capture());
        ErpSaleCartDraftCreateReqDTO saleCartReq = captor.getValue();
        assertEquals(88L, saleCartReq.getCustomerId());
        assertEquals(deptId, saleCartReq.getDeptId());
        assertEquals(ErpSaleBizSourceTypeEnum.MALL_ORDER.getType(), saleCartReq.getSourceType());
        assertEquals(900L, saleCartReq.getSourceId());
        assertEquals("MO-001", saleCartReq.getSourceNo());
        assertEquals("小程序生成", saleCartReq.getRemark());
        assertEquals(1, saleCartReq.getItems().size());
        ErpSaleCartDraftCreateReqDTO.Item item = saleCartReq.getItems().get(0);
        assertEquals(1001L, item.getProductId());
        assertEquals(2001L, item.getWarehouseId());
        assertEquals(deptId, item.getDeptId());
        assertEquals(MoneyUtils.fenToYuan(1234), item.getProductPrice());
        assertEquals(0, item.getCount().compareTo(new java.math.BigDecimal("2")));

        InOrder inOrder = inOrder(saleCartApi, payOrderApi, cartService);
        inOrder.verify(saleCartApi).createSaleCartDraft(any(ErpSaleCartDraftCreateReqDTO.class));
        inOrder.verify(payOrderApi).createOrder(any());
        inOrder.verify(cartService).deleteCart(eq(userId), eq(deptId), eq((Set<Long>) Collections.singleton(501L)));
        assertNotNull(respVO);
    }

    @Test
    void createOrder_immediateBuyShouldNotDeleteCart() {
        Long userId = 20L;
        Long deptId = 2L;
        AppTradeOrderCreateReqVO reqVO = new AppTradeOrderCreateReqVO();
        reqVO.setDeptId(deptId);
        reqVO.setDeliveryType(DeliveryTypeEnum.PICK_UP.getType());
        reqVO.setReceiverName("张三");
        reqVO.setReceiverMobile("13800000000");
        reqVO.setItems(Collections.singletonList(new AppTradeOrderSettlementReqVO.Item()
                .setSkuId(11L)
                .setStockId(301L)
                .setCount(2)));
        when(customerMemberApi.validateCustomerMemberAuth(userId, deptId)).thenReturn(
                new ErpCustomerMemberAuthRespDTO().setAuthorized(true).setCustomerId(88L));
        when(cartService.getCartList(eq(userId), eq(deptId), any())).thenReturn(Collections.emptyList());
        when(tradePriceService.calculateOrderPrice(any(TradePriceCalculateReqBO.class))).thenReturn(calculateResp()
                .setItems(Collections.singletonList(new TradePriceCalculateRespBO.OrderItem()
                        .setSpuId(101L)
                        .setSkuId(11L)
                        .setCount(2)
                        .setStockId(301L)
                        .setSelected(true)
                        .setPrice(1234)
                        .setDiscountPrice(0)
                        .setDeliveryPrice(0)
                        .setCouponPrice(0)
                        .setPointPrice(0)
                        .setUsePoint(0)
                        .setVipPrice(0)
                        .setPayPrice(2468)
                        .setSpuName("测试商品")
                        .setPicUrl("pic"))));
        when(mallStockService.validateMallStock(101L, 11L, 301L, 2)).thenReturn(new ErpMallStockOptionBO()
                .setErpProductId(1001L)
                .setWarehouseId(2001L)
                .setWarehouseName("一号仓"));
        when(saleCartApi.createSaleCartDraft(any(ErpSaleCartDraftCreateReqDTO.class))).thenReturn(
                new ErpSaleCartDraftCreateRespDTO()
                        .setId(3001L)
                        .setNo("XSTC-001")
                        .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        when(payOrderApi.createOrder(any())).thenReturn(7001L);

        tradeOrderUpdateService.createOrder(userId, reqVO);

        verify(cartService, never()).deleteCart(any(), any(), any());
    }

    @Test
    void createOrder_autoWarehouse_reallocateAndSnapshotPickupLocation() {
        Long userId = 20L;
        Long deptId = 2L;
        when(tradeOrderProperties.getAddressAutoWarehouseEnabled()).thenReturn(true);
        AppTradeOrderCreateReqVO reqVO = new AppTradeOrderCreateReqVO();
        reqVO.setDeptId(deptId);
        reqVO.setDeliveryType(DeliveryTypeEnum.PICK_UP.getType());
        reqVO.setPickUpStoreId(66L);
        reqVO.setReceiverName("张三");
        reqVO.setReceiverMobile("13800000000");
        reqVO.setRemark("  请尽快送货  ");
        reqVO.setItems(Collections.singletonList(new AppTradeOrderSettlementReqVO.Item()
                .setSkuId(11L).setCount(2).setStockId(999L)));
        when(pickUpStoreService.getDeliveryPickUpStore(66L)).thenReturn(new DeliveryPickUpStoreDO()
                .setId(66L).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setLongitude(104.123456D).setLatitude(30.654321D));
        when(customerMemberApi.validateCustomerMemberAuth(userId, deptId)).thenReturn(
                new ErpCustomerMemberAuthRespDTO().setAuthorized(true).setCustomerId(88L));
        when(cartService.getCartList(eq(userId), eq(deptId), any())).thenReturn(Collections.emptyList());
        when(tradePriceService.calculateOrderPrice(any(TradePriceCalculateReqBO.class))).thenReturn(calculateResp());
        when(autoWarehouseService.allocate(eq(deptId), any(BigDecimal.class), any(BigDecimal.class), any()))
                .thenReturn(new ErpMallAutoWarehouseResultBO()
                        .setStatus(ErpMallAutoWarehouseResultBO.STATUS_READY)
                        .setAssignments(Collections.singletonList(new ErpMallAutoWarehouseResultBO.Assignment()
                                .setIndex(0).setStockId(701L).setErpProductId(1001L)
                                .setWarehouseId(2001L).setWarehouseName("自动仓"))));
        when(saleCartApi.createSaleCartDraft(any(ErpSaleCartDraftCreateReqDTO.class))).thenReturn(
                new ErpSaleCartDraftCreateRespDTO().setId(3001L).setNo("XSTC-001")
                        .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        when(payOrderApi.createOrder(any())).thenReturn(7001L);

        tradeOrderUpdateService.createOrder(userId, reqVO);

        ArgumentCaptor<TradePriceCalculateReqBO> priceCaptor = ArgumentCaptor.forClass(TradePriceCalculateReqBO.class);
        verify(tradePriceService).calculateOrderPrice(priceCaptor.capture());
        assertEquals(null, priceCaptor.getValue().getItems().get(0).getStockId());
        ArgumentCaptor<TradeOrderDO> orderCaptor = ArgumentCaptor.forClass(TradeOrderDO.class);
        verify(tradeOrderMapper).insert(orderCaptor.capture());
        assertEquals(new BigDecimal("104.123456"), orderCaptor.getValue().getReceiverLongitude());
        assertEquals(new BigDecimal("30.654321"), orderCaptor.getValue().getReceiverLatitude());
        ArgumentCaptor<ErpSaleCartDraftCreateReqDTO> saleCartCaptor = ArgumentCaptor.forClass(
                ErpSaleCartDraftCreateReqDTO.class);
        verify(saleCartApi).createSaleCartDraft(saleCartCaptor.capture());
        assertEquals("小程序生成；客户备注：请尽快送货", saleCartCaptor.getValue().getRemark());
        verify(mallStockService, never()).validateMallStock(any(), any(), any(), any());
    }

    @Test
    void buildSaleCartRemark_shouldHandleBlankAndLimitLength() {
        assertEquals("小程序生成", TradeOrderUpdateServiceImpl.buildSaleCartRemark(null));
        assertEquals("小程序生成", TradeOrderUpdateServiceImpl.buildSaleCartRemark(" \t\r\n "));
        assertEquals("小程序生成；客户备注：客户原备注",
                TradeOrderUpdateServiceImpl.buildSaleCartRemark("  客户原备注  "));

        String result = TradeOrderUpdateServiceImpl.buildSaleCartRemark("😀".repeat(600));
        assertEquals(TradeOrderUpdateServiceImpl.ERP_SALE_CART_REMARK_MAX_LENGTH,
                result.codePointCount(0, result.length()));
        assertTrue(result.startsWith("小程序生成；客户备注："));
    }

    @Test
    void settlementOrder_autoWarehouse_readyWithoutExposingWarehouse() {
        Long userId = 20L;
        Long deptId = 2L;
        when(tradeOrderProperties.getAddressAutoWarehouseEnabled()).thenReturn(true);
        AppTradeOrderSettlementReqVO reqVO = new AppTradeOrderSettlementReqVO()
                .setDeptId(deptId).setDeliveryType(DeliveryTypeEnum.EXPRESS.getType()).setAddressId(77L)
                .setPointStatus(false).setItems(Collections.singletonList(
                        new AppTradeOrderSettlementReqVO.Item().setSkuId(11L).setCount(2)));
        when(addressApi.getAddress(77L, userId)).thenReturn(new MemberAddressRespDTO().setId(77L)
                .setLongitude(new BigDecimal("104.123456")).setLatitude(new BigDecimal("30.654321")));
        when(customerMemberApi.validateCustomerMemberAuth(userId, deptId)).thenReturn(
                new ErpCustomerMemberAuthRespDTO().setAuthorized(true).setCustomerId(88L));
        when(cartService.getCartList(eq(userId), eq(deptId), any())).thenReturn(Collections.emptyList());
        when(tradePriceService.calculateOrderPrice(any(TradePriceCalculateReqBO.class))).thenReturn(calculateResp());
        when(autoWarehouseService.allocate(eq(deptId), any(BigDecimal.class), any(BigDecimal.class), any()))
                .thenReturn(new ErpMallAutoWarehouseResultBO()
                        .setStatus(ErpMallAutoWarehouseResultBO.STATUS_READY)
                        .setAssignments(Collections.singletonList(new ErpMallAutoWarehouseResultBO.Assignment()
                                .setIndex(0).setStockId(701L).setErpProductId(1001L)
                                .setWarehouseId(2001L).setWarehouseName("内部自动仓"))));

        AppTradeOrderSettlementRespVO result = tradeOrderUpdateService.settlementOrder(userId, reqVO);

        assertEquals("READY", result.getFulfillmentStatus());
        assertEquals("库存确认完成", result.getFulfillmentMessage());
        assertNull(result.getItems().get(0).getWarehouseName());
        assertNull(result.getItems().get(0).getStockId());
    }

    private TradePriceCalculateRespBO calculateResp() {
        return new TradePriceCalculateRespBO()
                .setType(0)
                .setPrice(new TradePriceCalculateRespBO.Price()
                        .setTotalPrice(2468)
                        .setDiscountPrice(0)
                        .setDeliveryPrice(0)
                        .setCouponPrice(0)
                        .setPointPrice(0)
                        .setVipPrice(0)
                        .setPayPrice(2468))
                .setItems(Collections.singletonList(new TradePriceCalculateRespBO.OrderItem()
                        .setSpuId(101L)
                        .setSkuId(11L)
                        .setCount(2)
                        .setCartId(501L)
                        .setStockId(301L)
                        .setSelected(true)
                        .setPrice(1234)
                        .setDiscountPrice(0)
                        .setDeliveryPrice(0)
                        .setCouponPrice(0)
                        .setPointPrice(0)
                        .setUsePoint(0)
                        .setVipPrice(0)
                        .setPayPrice(2468)
                        .setSpuName("测试商品")
                        .setPicUrl("pic")));
    }

}
