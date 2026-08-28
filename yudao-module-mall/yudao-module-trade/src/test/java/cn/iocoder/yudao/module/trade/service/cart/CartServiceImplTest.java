package cn.iocoder.yudao.module.trade.service.cart;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.service.stock.ErpMallStockService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.product.enums.spu.ProductSpuStatusEnum;
import cn.iocoder.yudao.module.trade.controller.app.cart.vo.AppCartAddReqVO;
import cn.iocoder.yudao.module.trade.controller.app.cart.vo.AppCartListRespVO;
import cn.iocoder.yudao.module.trade.controller.app.cart.vo.AppCartUpdateCountReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.cart.CartDO;
import cn.iocoder.yudao.module.trade.dal.mysql.cart.CartMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.CARD_ITEM_NOT_FOUND;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.CART_DEPT_REQUIRED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CartServiceImpl} unit tests.
 */
public class CartServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CartServiceImpl cartService;

    @Mock
    private CartMapper cartMapper;
    @Mock
    private ProductSpuApi productSpuApi;
    @Mock
    private ProductSkuApi productSkuApi;
    @Mock
    private ErpMallStockService mallStockService;
    @Mock
    private ErpCustomerMemberApi customerMemberApi;

    @Test
    void addCart_sameSkuAndStockDifferentDept_insertNewCart() {
        when(customerMemberApi.validateCustomerMemberAuth(eq(20L), eq(2L))).thenReturn(auth());
        when(cartMapper.selectByScopeAndSkuIdAndStockId(eq(20L), eq(10L), eq(2L), eq(100L), eq(200L)))
                .thenReturn(null);
        when(productSkuApi.getSku(eq(100L))).thenReturn(new ProductSkuRespDTO()
                .setId(100L).setSpuId(300L).setStock(10));
        when(mallStockService.validateMallStock(eq(300L), eq(100L), eq(200L), eq(1)))
                .thenReturn(new ErpMallStockOptionBO()
                        .setStockId(200L).setErpProductId(400L).setWarehouseId(500L));

        cartService.addCart(20L, new AppCartAddReqVO()
                .setSkuId(100L).setStockId(200L).setCount(1).setDeptId(2L));

        ArgumentCaptor<CartDO> cartCaptor = ArgumentCaptor.forClass(CartDO.class);
        verify(cartMapper).insert(cartCaptor.capture());
        assertEquals(20L, cartCaptor.getValue().getUserId());
        assertEquals(10L, cartCaptor.getValue().getCustomerId());
        assertEquals(2L, cartCaptor.getValue().getDeptId());
        assertEquals(100L, cartCaptor.getValue().getSkuId());
        assertEquals(200L, cartCaptor.getValue().getStockId());
    }

    @Test
    void getCartList_filterByDeptScope() {
        when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(auth());
        when(customerMemberApi.validateCustomerMemberAuth(eq(20L), eq(1L))).thenReturn(auth());
        when(cartMapper.selectListByScope(eq(20L), eq(10L), eq(1L))).thenReturn(emptyList());

        AppCartListRespVO result = cartService.getCartList(20L, 1L);

        assertTrue(result.getAuthorized());
        assertTrue(result.getPriceVisible());
        assertTrue(result.getOrderEnabled());
        verify(cartMapper).selectListByScope(eq(20L), eq(10L), eq(1L));
    }

    @Test
    void getCartList_erpStockAvailableAndMallSpuStockZero_returnValidCart() {
        when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(auth());
        when(customerMemberApi.validateCustomerMemberAuth(eq(20L), eq(1L))).thenReturn(auth());
        when(cartMapper.selectListByScope(eq(20L), eq(10L), eq(1L))).thenReturn(new ArrayList<>(singletonList(
                new CartDO().setId(1L).setUserId(20L).setCustomerId(10L).setDeptId(1L).setSpuId(300L)
                        .setSkuId(100L).setStockId(200L).setCount(1).setSelected(true))));
        when(productSpuApi.getSpuList(eq(Collections.singleton(300L)))).thenReturn(singletonList(
                new ProductSpuRespDTO().setId(300L).setStatus(ProductSpuStatusEnum.ENABLE.getStatus())
                        .setStock(0)));
        when(productSkuApi.getSkuList(eq(Collections.singleton(100L)))).thenReturn(singletonList(
                new ProductSkuRespDTO().setId(100L).setSpuId(300L).setStock(0)));
        when(mallStockService.getMallStockOptionMap(eq(Collections.singleton(200L))))
                .thenReturn(Collections.singletonMap(200L, new ErpMallStockOptionBO()
                        .setStockId(200L).setErpProductId(400L).setWarehouseId(500L).setWarehouseName("甘孜仓")
                        .setAvailable(true).setAvailableCount(BigDecimal.valueOf(2)).setAvailableStatusText("现货")));

        AppCartListRespVO result = cartService.getCartList(20L, 1L);

        assertEquals(1, result.getValidList().size());
        assertTrue(result.getInvalidList().isEmpty());
        assertEquals("甘孜仓", result.getValidList().get(0).getWarehouseName());
        assertEquals(BigDecimal.valueOf(2), result.getValidList().get(0).getStockAvailableCount());
    }

    @Test
    void getCartList_unauthorized_returnEmpty() {
        when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(unauthorizedAuth());

        AppCartListRespVO result = cartService.getCartList(20L, 1L);

        assertEquals(false, result.getAuthorized());
        assertEquals(false, result.getPriceVisible());
        assertEquals(false, result.getOrderEnabled());
        assertTrue(result.getValidList().isEmpty());
        assertTrue(result.getInvalidList().isEmpty());
        verify(customerMemberApi, never()).validateCustomerMemberAuth(eq(20L), eq(1L));
        verify(cartMapper, never()).selectListByScope(eq(20L), eq(10L), eq(1L));
    }

    @Test
    void getCartCount_filterByDeptScope() {
        when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(auth());
        when(customerMemberApi.validateCustomerMemberAuth(eq(20L), eq(1L))).thenReturn(auth());
        when(cartMapper.selectSumByScope(eq(20L), eq(10L), eq(1L))).thenReturn(3);

        assertEquals(3, cartService.getCartCount(20L, 1L));
    }

    @Test
    void getCartCount_unauthorized_returnZero() {
        when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(unauthorizedAuth());

        assertEquals(0, cartService.getCartCount(20L, 1L));
        verify(customerMemberApi, never()).validateCustomerMemberAuth(eq(20L), eq(1L));
        verify(cartMapper, never()).selectSumByScope(eq(20L), eq(10L), eq(1L));
    }

    @Test
    void updateCartCount_crossDeptCartNotFound() {
        when(customerMemberApi.validateCustomerMemberAuth(eq(20L), eq(1L))).thenReturn(auth());
        when(cartMapper.selectById(eq(99L), eq(20L), eq(10L), eq(1L))).thenReturn(null);

        assertServiceException(() -> cartService.updateCartCount(20L,
                new AppCartUpdateCountReqVO().setId(99L).setCount(2).setDeptId(1L)), CARD_ITEM_NOT_FOUND);
    }

    @Test
    void addCart_deptRequired() {
        assertServiceException(() -> cartService.addCart(20L,
                new AppCartAddReqVO().setSkuId(100L).setStockId(200L).setCount(1)), CART_DEPT_REQUIRED);

        verify(customerMemberApi, never()).validateCustomerMemberAuth(eq(20L), eq(null));
    }

    private ErpCustomerMemberAuthRespDTO auth() {
        return new ErpCustomerMemberAuthRespDTO()
                .setAuthorized(true)
                .setCustomerId(10L)
                .setMemberUserId(20L)
                .setPriceVisible(true)
                .setOrderEnabled(true);
    }

    private ErpCustomerMemberAuthRespDTO unauthorizedAuth() {
        return ErpCustomerMemberAuthRespDTO.unauthorized().setMemberUserId(20L);
    }

}
