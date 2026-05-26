package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
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

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_CONVERT_QUOTE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_DELETE_FAIL_FINAL_APPROVED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_FINAL_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_FIRST_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_REJECT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_UPDATE_FAIL_NOT_PROCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private AdminUserApi adminUserApi;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleCartService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260509000001";
            }
        });
    }

    // ==================== 现有保留测试 ====================

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
        // finalApproveSaleCart 内部会通过 stockService 实时校验库存
        when(stockService.getStock(eq(201L), eq(301L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
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

        ErpSaleCartConvertQuoteReqVO reqVO = new ErpSaleCartConvertQuoteReqVO();
        reqVO.setCartId(cartId);
        reqVO.setCartItemIds(Collections.singletonList(item.getId()));
        saleCartService.convertToQuote(reqVO);

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
        // 转换后会删除原手推车
        verify(saleCartItemMapper).deleteByCartId(eq(cart.getId()));
        verify(saleCartMapper).deleteById(eq(cart.getId()));
    }

    @Test
    public void testDeleteSaleCart_success_whenProcess() {
        Long cartId = 51L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260510000001")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        when(saleCartMapper.selectBatchIds(eq(Collections.singletonList(cartId))))
                .thenReturn(Collections.singletonList(cart));

        saleCartService.deleteSaleCart(Collections.singletonList(cartId));

        verify(saleCartMapper).deleteBatchIds(eq(Collections.singletonList(cartId)));
        verify(saleCartItemMapper).deleteByCartId(eq(cartId));
    }

    @Test
    public void testDeleteSaleCart_fail_whenGeneratedSaleOut() {
        Long cartId = 52L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260510000002")
                .setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        when(saleCartMapper.selectBatchIds(eq(Collections.singletonList(cartId))))
                .thenReturn(Collections.singletonList(cart));

        assertServiceException(() -> saleCartService.deleteSaleCart(Collections.singletonList(cartId)),
                SALE_CART_DELETE_FAIL_FINAL_APPROVED, cart.getNo());
    }

    // ==================== create 场景 ====================

    @Test
    public void testCreateSaleCart_normalCase_returnId() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setAccountId(31L);
        reqVO.setSaleUserId(41L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomer(eq(21L))).thenReturn(new ErpCustomerDO().setId(21L));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(999L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        Long resultId = saleCartService.createSaleCart(reqVO);

        assertEquals(999L, resultId);
        verify(saleCartMapper).insert(argThat((ErpSaleCartDO cart) ->
                ErpSaleCartStatusEnum.PROCESS.getStatus().equals(cart.getStatus())
                        && reqVO.getCustomerId().equals(cart.getCustomerId())
                        && cart.getNo() != null && cart.getNo().startsWith(ErpNoRedisDAO.SALE_CART_NO_PREFIX)));
        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) -> items.size() == 1
                && items.get(0).getCartId().equals(999L)));
    }

    @Test
    public void testCreateSaleCart_invalidCustomer_throwException() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(99L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomer(eq(99L)))
                .thenThrow(new ServiceException(CUSTOMER_NOT_EXISTS));

        assertServiceException(() -> saleCartService.createSaleCart(reqVO), CUSTOMER_NOT_EXISTS);
        verify(saleCartMapper, never()).insert(any(ErpSaleCartDO.class));
    }

    // ==================== update 场景 ====================

    @Test
    public void testUpdateSaleCart_processStatus_success() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setId(11L);
        reqVO.setCustomerId(21L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(saleCartMapper.selectById(eq(11L)))
                .thenReturn(new ErpSaleCartDO().setId(11L).setNo("ST20260509000001")
                        .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomer(eq(21L))).thenReturn(new ErpCustomerDO().setId(21L));

        saleCartService.updateSaleCart(reqVO);

        verify(saleCartMapper).updateById(argThat((ErpSaleCartDO cart) -> reqVO.getId().equals(cart.getId())
                && reqVO.getCustomerId().equals(cart.getCustomerId())));
        verify(saleCartItemMapper).deleteByCartId(eq(reqVO.getId()));
        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) ->
                items.size() == 1 && items.get(0).getCartId().equals(reqVO.getId())));
    }

    @Test
    public void testUpdateSaleCart_finalApproved_throwException() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setId(11L);
        reqVO.setCustomerId(21L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        ErpSaleCartDO existing = new ErpSaleCartDO().setId(11L).setNo("ST20260509000001")
                .setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        when(saleCartMapper.selectById(eq(11L))).thenReturn(existing);

        assertServiceException(() -> saleCartService.updateSaleCart(reqVO),
                SALE_CART_UPDATE_FAIL_NOT_PROCESS, existing.getNo());
        verify(saleCartMapper, never()).updateById(any(ErpSaleCartDO.class));
    }

    // ==================== 状态机：submit / firstApprove / reject ====================

    @Test
    public void testSubmitSaleCart_processToFirstApprove_success() {
        // 注：方法名是 submitSaleCart，业务流转 PROCESS -> SUBMITTED（待初审）
        Long cartId = 71L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000003")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        saleCartService.submitSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testFirstApproveSaleCart_firstApproveToFinalApprove_success() {
        // firstApproveSaleCart 流转：SUBMITTED -> FIRST_APPROVE
        Long cartId = 72L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000004")
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())
                        && update.getFirstAuditTime() != null)))
                .thenReturn(1);

        saleCartService.firstApproveSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testRejectSaleCart_resetToProcess_success() {
        Long cartId = 73L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000005")
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);

        saleCartService.rejectSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.PROCESS.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testRejectSaleCart_alreadyFinalApproved_throwException() {
        // 已生成销售单状态（GENERATED_SALE_OUT）不可拒绝
        Long cartId = 74L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000006")
                .setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);

        assertServiceException(() -> saleCartService.rejectSaleCart(cartId), SALE_CART_REJECT_FAIL);
        verify(saleCartMapper, never()).updateByIdAndStatus(eq(cartId), any(), any());
    }

    // ==================== convertToQuote 异常 ====================

    @Test
    public void testConvertToQuote_finalApprovedCart_throwException() {
        // 已终审/已生成销售单的手推车不可转报价
        Long cartId = 75L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000007")
                .setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);

        ErpSaleCartConvertQuoteReqVO reqVO = new ErpSaleCartConvertQuoteReqVO();
        reqVO.setCartId(cartId);
        reqVO.setCartItemIds(Collections.singletonList(101L));

        assertServiceException(() -> saleCartService.convertToQuote(reqVO), SALE_CART_CONVERT_QUOTE_FAIL);
        verify(saleQuoteMapper, never()).insert(any(ErpSaleQuoteDO.class));
    }

    // ==================== finalApproveSaleCart 异常 ====================

    @Test
    public void testFinalApproveSaleCart_optimisticLockFail_throwException() {
        // 模拟更新返回 0 行（乐观锁失败：并发或状态不符）
        Long cartId = 76L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000008")
                .setCustomerId(21L)
                .setAccountId(31L)
                .setSaleUserId(41L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(101L).setCartId(cartId).setProductId(201L).setWarehouseId(301L)
                .setProductPrice(new BigDecimal("15.00")).setCount(new BigDecimal("3"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockService.getStock(eq(201L), eq(301L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        // updateByIdAndStatus 返回 0：乐观锁失败
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()), any(ErpSaleCartDO.class)))
                .thenReturn(0);

        assertServiceException(() -> saleCartService.finalApproveSaleCart(cartId), SALE_CART_FINAL_APPROVE_FAIL);
    }

    // ==================== 辅助方法 ====================

    private ErpSaleCartSaveReqVO buildBaseSaveReq() {
        ErpSaleCartSaveReqVO reqVO = new ErpSaleCartSaveReqVO();
        reqVO.setCartTime(LocalDateTime.of(2026, 5, 9, 10, 0));
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        reqVO.setOtherPrice(BigDecimal.ZERO);
        return reqVO;
    }

    private ErpSaleCartSaveReqVO.Item buildItemReq(BigDecimal count, BigDecimal price) {
        ErpSaleCartSaveReqVO.Item item = new ErpSaleCartSaveReqVO.Item();
        item.setProductId(201L);
        item.setWarehouseId(401L);
        item.setProductPrice(price);
        item.setCount(count);
        item.setTaxPercent(BigDecimal.ZERO);
        return item;
    }

}
