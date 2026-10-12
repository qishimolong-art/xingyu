package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_SALE_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_CONVERT_CART_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_CONVERT_COUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_DELETE_FAIL_GENERATED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_ITEM_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_ITEM_PRODUCT_PRICE_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_REVERSE_ORDER_FAIL_CONVERTED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_SUBMIT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_UPDATE_FAIL_GENERATED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_LEVEL_PERMISSION_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpSaleCartService saleCartService;
    @Mock
    private ErpSaleCartTransferLinkService saleCartTransferLinkService;
    @Mock
    private ErpSaleQuoteProgressService saleQuoteProgressService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Mock
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpSaleItemBatchUpdateSupport batchUpdateSupport;
    @Mock
    private ErpSalePriceLevelPricePicker priceLevelPricePicker;
    @Mock
    private ErpSalePriceLevelPermissionValidator priceLevelPermissionValidator;
    @Mock
    private ErpSaleDirectDeptPermissionService saleDirectDeptPermissionService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleQuoteService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260509000001";
            }
        });
        lenient().when(warehouseService.validSaleWarehouseList(ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> buildWarehouseList(invocation.getArgument(0)));
        lenient().when(warehouseService.validSaleSelectableWarehouseListForDept(
                        ArgumentMatchers.anyCollection(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenAnswer(invocation -> buildWarehouseList(invocation.getArgument(0)));
        lenient().when(warehouseService.getWarehouseMap(ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> {
                    Map<Long, ErpWarehouseDO> map = new HashMap<>();
                    buildWarehouseList(invocation.getArgument(0)).forEach(warehouse -> map.put(warehouse.getId(), warehouse));
                    return map;
                });
    }

    private List<ErpWarehouseDO> buildWarehouseList(Iterable<Long> warehouseIds) {
        List<ErpWarehouseDO> warehouses = new ArrayList<>();
        if (warehouseIds == null) {
            return warehouses;
        }
        for (Long warehouseId : warehouseIds) {
            if (warehouseId != null) {
                warehouses.add(new ErpWarehouseDO().setId(warehouseId).setDeptId(warehouseId + 1000));
            }
        }
        return warehouses;
    }

    @Test
    public void testParseImportData_missingDefaultWarehouse_returnsReadableMessage() {
        ErpSaleQuoteImportExcelVO row = new ErpSaleQuoteImportExcelVO();
        row.setProductCode("P0001");
        row.setCount(BigDecimal.ONE);
        ErpProductDO product = new ErpProductDO()
                .setId(10L)
                .setCode("P0001")
                .setUnitId(20L)
                .setSalePrice(new BigDecimal("100.00"));
        when(productMapper.selectListByCodes(anyCollection())).thenReturn(Collections.singletonList(product));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.emptyMap());

        ErpSaleQuoteImportRespVO respVO = saleQuoteService.parseImportData(Collections.singletonList(row));

        assertEquals(0, respVO.getSuccessCount());
        assertEquals(1, respVO.getFailureCount());
        assertEquals(2, respVO.getFailureDetails().get(0).getRowNo());
        assertEquals("P0001", respVO.getFailureDetails().get(0).getProductCode());
        assertEquals("产品默认仓库不能为空", respVO.getFailureDetails().get(0).getReason());
    }

    @Test
    public void testApproveSaleQuote_onlyChangesToOrderedWithoutCreatingOutboundDocument() {
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
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);
        when(saleQuoteMapper.updateByIdAndStatus(eq(quoteId), eq(ErpSaleQuoteStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.APPROVE.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        assertEquals(quoteId, saleQuoteService.approveSaleQuote(quoteId));

        verify(saleQuoteMapper).updateByIdAndStatus(eq(quoteId),
                eq(ErpSaleQuoteStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.APPROVE.getStatus().equals(update.getStatus())));
        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any());
        verify(stockService, never()).getStockCount(anyLong(), anyLong());
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
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        quote.setCreator("source-creator");
        quote.setCreateTime(LocalDateTime.of(2026, 5, 9, 11, 50));
        quote.setUpdater("source-updater");
        quote.setUpdateTime(LocalDateTime.of(2026, 5, 9, 11, 55));
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(102L)
                .setQuoteId(quoteId)
                .setProductId(202L)
                .setProductUnitId(302L)
                .setWarehouseId(402L)
                .setDeptId(502L)
                .setBatchNo("B-NEW")
                .setProductPrice(new BigDecimal("20.00"))
                .setCount(new BigDecimal("10"))
                .setConvertedCount(new BigDecimal("3"));
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteConvertCartReqVO.Item()
                .setQuoteItemId(item.getId()).setCount(new BigDecimal("4"))));
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(eq(quoteId))).thenReturn(Collections.singletonList(item));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(901L);
            return null;
        }).when(saleCartTransferLinkService).insertCart(any());
        when(saleCartItemMapper.insertBatch(any())).thenAnswer(invocation -> {
            Collection<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> items = invocation.getArgument(0);
            items.iterator().next().setId(902L);
            return true;
        });

        LocalDateTime conversionStartedAt = LocalDateTime.now();
        assertEquals(901L, saleQuoteService.convertToCart(reqVO));
        LocalDateTime conversionFinishedAt = LocalDateTime.now();

        verify(saleDocumentDefaultService).fillCreateDefaults(ArgumentMatchers.<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO>argThat(cart ->
                cart.getCreator() == null && cart.getCreateTime() == null
                        && cart.getUpdater() == null && cart.getUpdateTime() == null));
        verify(saleCartTransferLinkService).insertCart(ArgumentMatchers.<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO>argThat(cart -> quote.getCustomerId().equals(cart.getCustomerId())
                && ErpSaleCartStatusEnum.PROCESS.getStatus().equals(cart.getStatus())
                && ErpSaleBizSourceTypeEnum.QUOTE.getType().equals(cart.getSourceType())
                && quote.getId().equals(cart.getSourceId())
                && quote.getNo().equals(cart.getSourceNo())
                && cart.getCartTime() != null
                && !cart.getCartTime().isBefore(conversionStartedAt)
                && !cart.getCartTime().isAfter(conversionFinishedAt)
                && !quote.getQuoteTime().equals(cart.getCartTime())));
        verify(saleCartItemMapper).insertBatch(argThat(items -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> list = new ArrayList<>(items);
            return list.size() == 1 && item.getProductId().equals(list.get(0).getProductId())
                    && item.getWarehouseId().equals(list.get(0).getWarehouseId())
                    && item.getDeptId().equals(list.get(0).getDeptId())
                    && item.getBatchNo().equals(list.get(0).getBatchNo())
                    && new BigDecimal("4").compareTo(list.get(0).getCount()) == 0;
        }));
        verify(saleConvertRecordMapper).insertBatch(argThat(records -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO> list = new ArrayList<>(records);
            return list.size() == 1 && item.getId().equals(list.get(0).getSourceItemId())
                    && Long.valueOf(902L).equals(list.get(0).getTargetItemId())
                    && new BigDecimal("4").compareTo(list.get(0).getCount()) == 0;
        }));
        verify(saleCartService).submitSaleCart(901L);
        verify(saleQuoteProgressService).refreshQuoteProgress(quoteId);
    }

    @Test
    public void testConvertToCart_submitFailure_doesNotRefreshQuoteProgress() {
        Long quoteId = 121L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId).setNo("BJ20260509000121").setCustomerId(221L).setDeptId(321L)
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(122L).setQuoteId(quoteId).setProductId(222L).setProductUnitId(322L)
                .setWarehouseId(422L).setDeptId(321L).setProductPrice(new BigDecimal("20.00"))
                .setTaxPercent(BigDecimal.ZERO).setCount(new BigDecimal("2")).setConvertedCount(BigDecimal.ZERO);
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteConvertCartReqVO.Item()
                .setQuoteItemId(item.getId()).setCount(BigDecimal.ONE)));
        when(saleQuoteMapper.selectByIdForUpdate(quoteId)).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(quoteId)).thenReturn(Collections.singletonList(item));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(921L);
            return null;
        }).when(saleCartTransferLinkService).insertCart(any());
        when(saleCartItemMapper.insertBatch(any())).thenAnswer(invocation -> {
            Collection<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> items = invocation.getArgument(0);
            items.iterator().next().setId(922L);
            return true;
        });
        doThrow(new ServiceException(SALE_QUOTE_CONVERT_CART_FAIL)).when(saleCartService).submitSaleCart(921L);

        assertServiceException(() -> saleQuoteService.convertToCart(reqVO), SALE_QUOTE_CONVERT_CART_FAIL);

        verify(saleConvertRecordMapper).insertBatch(any());
        verify(saleQuoteProgressService, never()).refreshQuoteProgress(anyLong());
    }

    @Test
    public void testBatchUpdateSaleQuoteItems_warehouseDept_persistChangedFieldsAndClearBatchNo() {
        Long quoteId = 15L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000005")
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus());
        ErpSaleQuoteItemDO firstItem = new ErpSaleQuoteItemDO()
                .setId(105L)
                .setQuoteId(quoteId)
                .setProductId(205L)
                .setWarehouseId(405L)
                .setDeptId(505L)
                .setBatchNo("OLD-A");
        ErpSaleQuoteItemDO secondItem = new ErpSaleQuoteItemDO()
                .setId(106L)
                .setQuoteId(quoteId)
                .setProductId(206L)
                .setWarehouseId(406L)
                .setDeptId(506L)
                .setBatchNo("OLD-B");
        ErpWarehouseDO targetWarehouse = new ErpWarehouseDO().setId(407L).setDeptId(507L);
        ErpSaleQuoteItemBatchUpdateReqVO reqVO = new ErpSaleQuoteItemBatchUpdateReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItemIds(Arrays.asList(firstItem.getId(), secondItem.getId()));
        reqVO.setWarehouseId(targetWarehouse.getId());
        reqVO.setDeptId(607L);
        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteId(eq(quoteId)))
                .thenReturn(Arrays.asList(firstItem, secondItem));
        when(batchUpdateSupport.validateTargetWarehouse(eq(targetWarehouse.getId()))).thenReturn(targetWarehouse);
        when(batchUpdateSupport.resolveTargetDeptId(eq(targetWarehouse), eq(607L), anyString(), any()))
                .thenReturn(607L);

        saleQuoteService.batchUpdateSaleQuoteItems(reqVO);

        verify(saleQuoteItemMapper).updateWarehouseDeptByIds(argThat((Collection<Long> ids) ->
                        ids.size() == 2 && ids.contains(firstItem.getId()) && ids.contains(secondItem.getId())),
                eq(targetWarehouse.getId()), eq(607L), eq(true));
    }

    @Test
    public void testBatchUpdateSaleQuoteItems_priceLevel_updatesSelectedPricesAndTotals() {
        Long quoteId = 16L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000006")
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setFeeAmount(BigDecimal.ZERO);
        ErpSaleQuoteItemDO normalItem = new ErpSaleQuoteItemDO()
                .setId(107L)
                .setQuoteId(quoteId)
                .setProductId(207L)
                .setCount(new BigDecimal("2"))
                .setGiftFlag(false)
                .setProductPrice(new BigDecimal("10.00"))
                .setTotalPrice(new BigDecimal("20.00"))
                .setTaxPercent(new BigDecimal("10"));
        ErpSaleQuoteItemDO giftItem = new ErpSaleQuoteItemDO()
                .setId(108L)
                .setQuoteId(quoteId)
                .setProductId(208L)
                .setCount(new BigDecimal("3"))
                .setGiftFlag(true)
                .setProductPrice(new BigDecimal("99.00"))
                .setTotalPrice(new BigDecimal("297.00"))
                .setTaxPercent(new BigDecimal("10"));
        ErpSaleQuoteItemDO untouchedItem = new ErpSaleQuoteItemDO()
                .setId(109L)
                .setQuoteId(quoteId)
                .setProductId(209L)
                .setCount(BigDecimal.ONE)
                .setGiftFlag(false)
                .setProductPrice(new BigDecimal("5.00"))
                .setTotalPrice(new BigDecimal("5.00"));
        ErpSaleQuoteItemBatchUpdateReqVO reqVO = new ErpSaleQuoteItemBatchUpdateReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItemIds(Arrays.asList(normalItem.getId(), giftItem.getId()));
        reqVO.setPriceLevel(10);
        Map<Long, BigDecimal> priceMap = new HashMap<>();
        priceMap.put(normalItem.getProductId(), new BigDecimal("12.50"));
        priceMap.put(giftItem.getProductId(), new BigDecimal("88.00"));
        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteId(eq(quoteId)))
                .thenReturn(Arrays.asList(normalItem, giftItem, untouchedItem));
        when(priceLevelPricePicker.pickProductPriceMap(anyCollection(), eq(10))).thenReturn(priceMap);

        saleQuoteService.batchUpdateSaleQuoteItems(reqVO);

        verify(priceLevelPermissionValidator).validateSelectablePriceLevel(eq(10), nullable(Long.class));
        verify(saleQuoteItemMapper).updateBatch(ArgumentMatchers.<Collection<ErpSaleQuoteItemDO>>argThat(items -> {
            List<ErpSaleQuoteItemDO> list = new ArrayList<>(items);
            return list.size() == 2
                    && list.get(0).getProductPrice().compareTo(new BigDecimal("12.50")) == 0
                    && list.get(0).getTotalPrice().compareTo(new BigDecimal("25.00")) == 0
                    && list.get(1).getProductPrice().compareTo(BigDecimal.ZERO) == 0
                    && list.get(1).getTotalPrice().compareTo(BigDecimal.ZERO) == 0;
        }));
        verify(saleQuoteMapper).updateById(ArgumentMatchers.<ErpSaleQuoteDO>argThat(update ->
                update.getId().equals(quoteId)
                        && update.getTotalProductPrice().compareTo(new BigDecimal("30.00")) == 0));
    }

    @Test
    public void testBatchUpdateSaleQuoteItems_priceLevelPermissionDenied() {
        Long quoteId = 17L;
        Long deptId = 20L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000007")
                .setDeptId(deptId)
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setFeeAmount(BigDecimal.ZERO);
        ErpSaleQuoteItemDO normalItem = new ErpSaleQuoteItemDO()
                .setId(117L)
                .setQuoteId(quoteId)
                .setProductId(217L)
                .setCount(new BigDecimal("2"))
                .setGiftFlag(false)
                .setProductPrice(new BigDecimal("10.00"))
                .setTotalPrice(new BigDecimal("20.00"))
                .setTaxPercent(new BigDecimal("10"));
        ErpSaleQuoteItemBatchUpdateReqVO reqVO = new ErpSaleQuoteItemBatchUpdateReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItemIds(Collections.singletonList(normalItem.getId()));
        reqVO.setPriceLevel(10);
        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteId(eq(quoteId)))
                .thenReturn(Collections.singletonList(normalItem));
        doThrow(new ServiceException(SALE_PRICE_LEVEL_PERMISSION_DENIED)).when(priceLevelPermissionValidator)
                .validateSelectablePriceLevel(eq(10), eq(deptId));

        assertServiceException(() -> saleQuoteService.batchUpdateSaleQuoteItems(reqVO),
                SALE_PRICE_LEVEL_PERMISSION_DENIED);

        verify(priceLevelPricePicker, never()).pickProductPriceMap(anyCollection(), any());
        verify(saleQuoteItemMapper, never()).updateBatch(anyCollection());
    }

    @Test
    public void testConvertToCart_duplicateQuoteItem_aggregateCountOnce() {
        Long quoteId = 13L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000003")
                .setCustomerId(23L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 13, 0))
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(103L)
                .setQuoteId(quoteId)
                .setProductId(203L)
                .setProductUnitId(303L)
                .setWarehouseId(403L)
                .setProductPrice(new BigDecimal("10.00"))
                .setTaxPercent(BigDecimal.ZERO)
                .setCount(new BigDecimal("5"))
                .setConvertedCount(BigDecimal.ZERO);
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Arrays.asList(
                new ErpSaleQuoteConvertCartReqVO.Item().setQuoteItemId(item.getId()).setCount(new BigDecimal("2")),
                new ErpSaleQuoteConvertCartReqVO.Item().setQuoteItemId(item.getId()).setCount(new BigDecimal("3"))));
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(eq(quoteId))).thenReturn(Collections.singletonList(item));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(902L);
            return null;
        }).when(saleCartTransferLinkService).insertCart(any());

        saleQuoteService.convertToCart(reqVO);

        verify(saleCartItemMapper).insertBatch(argThat(items -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> list = new ArrayList<>(items);
            return list.size() == 1
                    && item.getProductId().equals(list.get(0).getProductId())
                    && new BigDecimal("5").compareTo(list.get(0).getCount()) == 0
                    && new BigDecimal("50.00").compareTo(list.get(0).getTotalPrice()) == 0;
        }));
        verify(saleConvertRecordMapper).insertBatch(argThat(records -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO> list = new ArrayList<>(records);
            return list.size() == 1 && item.getId().equals(list.get(0).getSourceItemId())
                    && new BigDecimal("5").compareTo(list.get(0).getCount()) == 0;
        }));
        verify(saleCartService).submitSaleCart(902L);
        verify(saleQuoteProgressService).refreshQuoteProgress(quoteId);
    }

    @Test
    public void testConvertToCart_sameStockDifferentSourceItems_shareMergedTargetItem() {
        Long quoteId = 113L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId).setNo("BJ20261012000113").setCustomerId(23L)
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteItemDO first = new ErpSaleQuoteItemDO()
                .setId(1003L).setQuoteId(quoteId).setProductId(203L).setProductUnitId(303L)
                .setWarehouseId(403L).setBatchNo(" B1 ").setGiftFlag(false)
                .setProductPrice(new BigDecimal("10")).setTaxPercent(BigDecimal.ZERO)
                .setCount(new BigDecimal("2")).setConvertedCount(BigDecimal.ZERO);
        ErpSaleQuoteItemDO second = new ErpSaleQuoteItemDO()
                .setId(1004L).setQuoteId(quoteId).setProductId(203L).setProductUnitId(303L)
                .setWarehouseId(403L).setBatchNo("B1").setGiftFlag(false)
                .setProductPrice(new BigDecimal("12")).setTaxPercent(BigDecimal.ZERO)
                .setCount(new BigDecimal("3")).setConvertedCount(BigDecimal.ZERO);
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO().setQuoteId(quoteId)
                .setItems(Arrays.asList(
                        new ErpSaleQuoteConvertCartReqVO.Item().setQuoteItemId(first.getId()).setCount(first.getCount()),
                        new ErpSaleQuoteConvertCartReqVO.Item().setQuoteItemId(second.getId()).setCount(second.getCount())));
        when(saleQuoteMapper.selectByIdForUpdate(quoteId)).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(quoteId)).thenReturn(Arrays.asList(first, second));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(903L);
            return null;
        }).when(saleCartTransferLinkService).insertCart(any());
        when(saleCartItemMapper.insertBatch(any())).thenAnswer(invocation -> {
            Collection<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> items = invocation.getArgument(0);
            items.iterator().next().setId(904L);
            return true;
        });

        saleQuoteService.convertToCart(reqVO);

        verify(saleCartItemMapper).insertBatch(argThat(items -> {
            List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> list = new ArrayList<>(items);
            return list.size() == 1 && new BigDecimal("5").compareTo(list.get(0).getCount()) == 0
                    && new BigDecimal("12").compareTo(list.get(0).getProductPrice()) == 0;
        }));
        verify(saleConvertRecordMapper).insertBatch(argThat(records -> {
            List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO> list = new ArrayList<>(records);
            return list.size() == 2
                    && list.stream().allMatch(record -> Long.valueOf(904L).equals(record.getTargetItemId()))
                    && list.stream().anyMatch(record -> first.getId().equals(record.getSourceItemId())
                            && first.getCount().compareTo(record.getCount()) == 0)
                    && list.stream().anyMatch(record -> second.getId().equals(record.getSourceItemId())
                            && second.getCount().compareTo(record.getCount()) == 0);
        }));
    }

    @Test
    public void testConvertToCart_crossDeptStockCountIgnoreDataPermission_success() {
        Long quoteId = 14L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000004")
                .setCustomerId(24L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 14, 0))
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(104L)
                .setQuoteId(quoteId)
                .setProductId(204L)
                .setProductUnitId(304L)
                .setWarehouseId(404L)
                .setProductPrice(new BigDecimal("10.00"))
                .setTaxPercent(BigDecimal.ZERO)
                .setCount(new BigDecimal("1"))
                .setConvertedCount(BigDecimal.ZERO);
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteConvertCartReqVO.Item()
                .setQuoteItemId(item.getId()).setCount(new BigDecimal("1"))));
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(eq(quoteId))).thenReturn(Collections.singletonList(item));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(903L);
            return null;
        }).when(saleCartTransferLinkService).insertCart(any());
        when(stockService.getStockCount(eq(204L), eq(404L))).thenAnswer(invocation -> {
            if (DataPermissionContextHolder.get() == null || DataPermissionContextHolder.get().enable()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal("2");
        });
        saleQuoteService.convertToCart(reqVO);

        verify(saleCartItemMapper).insertBatch(argThat(items -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> list = new ArrayList<>(items);
            return list.size() == 1 && new BigDecimal("2").compareTo(list.get(0).getStockCount()) == 0;
        }));
        verify(saleCartService).submitSaleCart(903L);
        verify(saleQuoteProgressService).refreshQuoteProgress(quoteId);
    }

    // ==================== create ====================

    @Test
    public void testCreateSaleQuote_normalCase_returnId() {
        // 准备：创建参数
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(50L);
        reqVO.setAccountId(60L);
        reqVO.setSaleUserId(70L);
        reqVO.setDeptId(80L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 9, 0));
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        reqVO.setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(500L);
        itemVO.setWarehouseId(600L);
        itemVO.setDeptId(80L);
        itemVO.setProductPrice(new BigDecimal("10.00"));
        itemVO.setCount(new BigDecimal("5"));
        itemVO.setTaxPercent(BigDecimal.ZERO);
        reqVO.setItems(Collections.singletonList(itemVO));

        // mock 依赖
        ErpProductDO product = new ErpProductDO().setId(500L).setUnitId(700L);
        when(productService.validProductList(eq(Collections.singleton(500L))))
                .thenReturn(Collections.singletonList(product));
        when(stockService.getStock(eq(500L), eq(600L)))
                .thenReturn(new ErpStockDO().setProductId(500L).setWarehouseId(600L).setDeptId(1600L));
        when(customerService.getCustomerSaleDeptIds(eq(50L))).thenReturn(Collections.singletonList(80L));
        when(saleQuoteMapper.selectByNo(anyString())).thenReturn(null);

        // 执行：使用 Answer 模拟数据库回填生成的 id
        when(saleQuoteMapper.insert(ArgumentMatchers.<ErpSaleQuoteDO>any())).thenAnswer(invocation -> {
            ErpSaleQuoteDO quote = invocation.getArgument(0);
            quote.setId(999L);
            return 1;
        });
        Long resultId = saleQuoteService.createSaleQuote(reqVO);

        // 断言：返回 ID
        assertNotNull(resultId);
        assertEquals(999L, resultId);
        // 校验依赖调用
        verify(customerService).validateCustomerForSale(eq(50L), eq(80L));
        verify(customerService).validateCustomerSaleDept(eq(50L), eq(80L));
        verify(accountService).validateAccount(eq(60L));
        verify(adminUserApi).validateUser(eq(70L));
        verify(warehouseService).validateWarehouseSaleSelectableForDept(eq(600L), eq(80L),
                eq(ErpWarehouseService.SALE_QUOTE_ALL_PRODUCT_PERMISSION));
        verify(saleQuoteMapper).insert(ArgumentMatchers.<ErpSaleQuoteDO>argThat(quote -> ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())
                && reqVO.getCustomerId().equals(quote.getCustomerId())));
        verify(saleQuoteItemMapper).insertBatch(argThat((List<ErpSaleQuoteItemDO> items) -> items.size() == 1
                && Long.valueOf(999L).equals(items.get(0).getQuoteId())
                && Long.valueOf(500L).equals(items.get(0).getProductId())
                && Long.valueOf(600L).equals(items.get(0).getWarehouseId())
                && Long.valueOf(1600L).equals(items.get(0).getDeptId())));
    }

    @Test
    public void testCreateSaleQuote_crossDeptWarehouse_itemDeptFollowsStockDept() {
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(56L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 9, 0));
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        reqVO.setOtherPrice(BigDecimal.ZERO);
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(506L);
        itemVO.setWarehouseId(606L);
        itemVO.setDeptId(1606L);
        itemVO.setProductPrice(new BigDecimal("10.00"));
        itemVO.setCount(new BigDecimal("5"));
        reqVO.setItems(Collections.singletonList(itemVO));

        when(productService.validProductList(eq(Collections.singleton(506L))))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(506L).setUnitId(706L)));
        when(stockService.getStock(eq(506L), eq(606L)))
                .thenReturn(new ErpStockDO().setProductId(506L).setWarehouseId(606L).setDeptId(2606L));
        when(customerService.getCustomerSaleDeptIds(eq(56L))).thenReturn(Collections.singletonList(80L));
        when(saleQuoteMapper.selectByNo(anyString())).thenReturn(null);
        when(saleQuoteMapper.insert(ArgumentMatchers.<ErpSaleQuoteDO>any())).thenAnswer(invocation -> {
            ErpSaleQuoteDO quote = invocation.getArgument(0);
            quote.setId(1000L);
            return 1;
        });

        Long resultId = saleQuoteService.createSaleQuote(reqVO);

        assertEquals(1000L, resultId);
        verify(warehouseService).validateWarehouseSaleSelectableForDept(eq(606L), eq(80L),
                eq(ErpWarehouseService.SALE_QUOTE_ALL_PRODUCT_PERMISSION));
        verify(saleQuoteItemMapper).insertBatch(argThat((List<ErpSaleQuoteItemDO> items) -> items.size() == 1
                && Long.valueOf(606L).equals(items.get(0).getWarehouseId())
                && Long.valueOf(2606L).equals(items.get(0).getDeptId())));
    }

    @Test
    public void testCreateSaleQuote_customerDeptNotAllowed_throwException() {
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(55L);
        reqVO.setDeptId(81L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 9, 0));
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(505L);
        itemVO.setWarehouseId(605L);
        itemVO.setProductPrice(new BigDecimal("10.00"));
        itemVO.setCount(new BigDecimal("5"));
        reqVO.setItems(Collections.singletonList(itemVO));

        when(productService.validProductList(eq(Collections.singleton(505L))))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(505L).setUnitId(705L)));
        doThrow(new ServiceException(CUSTOMER_SALE_DEPT_NOT_ALLOWED))
                .when(customerService).validateCustomerSaleDept(eq(55L), eq(81L));

        assertServiceException(() -> saleQuoteService.createSaleQuote(reqVO), CUSTOMER_SALE_DEPT_NOT_ALLOWED);
        verify(productService).validProductList(eq(Collections.singleton(505L)));
        verify(saleQuoteMapper, never()).insert(ArgumentMatchers.<ErpSaleQuoteDO>any());
    }

    @Test
    public void testCreateSaleQuote_zeroProductPrice_throwException() {
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(54L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 9, 0));
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(504L);
        itemVO.setWarehouseId(604L);
        itemVO.setProductPrice(BigDecimal.ZERO);
        itemVO.setCount(new BigDecimal("5"));
        itemVO.setGiftFlag(Boolean.FALSE);
        reqVO.setItems(Collections.singletonList(itemVO));

        assertServiceException(() -> saleQuoteService.createSaleQuote(reqVO),
                SALE_QUOTE_ITEM_PRODUCT_PRICE_POSITIVE, 1);
        verify(productService, never()).validProductList(any());
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
        verify(saleQuoteMapper, never()).insert(ArgumentMatchers.<ErpSaleQuoteDO>any());
    }

    @Test
    public void testCreateSaleQuote_invalidCustomer_throwException() {
        // 准备：创建参数
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(51L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 9, 0));
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(501L);
        itemVO.setWarehouseId(601L);
        itemVO.setProductPrice(new BigDecimal("10.00"));
        itemVO.setCount(new BigDecimal("5"));
        reqVO.setItems(Collections.singletonList(itemVO));
        // mock：产品校验通过；客户校验抛异常
        ErpProductDO product = new ErpProductDO().setId(501L).setUnitId(701L);
        when(productService.validProductList(any()))
                .thenReturn(Collections.singletonList(product));
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomerForSale(eq(51L), nullable(Long.class));

        // 执行 & 断言
        assertServiceException(() -> saleQuoteService.createSaleQuote(reqVO), CUSTOMER_NOT_EXISTS);
        // 校验：未走到 insert
        verify(saleQuoteMapper, never()).insert(ArgumentMatchers.<ErpSaleQuoteDO>any());
    }

    @Test
    public void testCreateSaleQuote_invalidProduct_throwException() {
        // 准备：创建参数
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(52L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 9, 0));
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(502L);
        itemVO.setWarehouseId(602L);
        itemVO.setProductPrice(new BigDecimal("10.00"));
        itemVO.setCount(new BigDecimal("5"));
        reqVO.setItems(Collections.singletonList(itemVO));
        // mock：产品校验失败
        when(productService.validProductList(any()))
                .thenThrow(new ServiceException(PRODUCT_NOT_EXISTS));

        // 执行 & 断言
        assertServiceException(() -> saleQuoteService.createSaleQuote(reqVO), PRODUCT_NOT_EXISTS);
        // 校验：未走到客户校验和 insert
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
        verify(saleQuoteMapper, never()).insert(ArgumentMatchers.<ErpSaleQuoteDO>any());
    }

    @Test
    public void testCreateSaleQuote_duplicateItem_throwException() {
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(53L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 9, 0));
        ErpSaleQuoteSaveReqVO.Item itemVO1 = new ErpSaleQuoteSaveReqVO.Item();
        itemVO1.setProductId(503L);
        itemVO1.setWarehouseId(603L);
        itemVO1.setProductPrice(new BigDecimal("10.00"));
        itemVO1.setCount(new BigDecimal("5"));
        itemVO1.setGiftFlag(Boolean.FALSE);
        ErpSaleQuoteSaveReqVO.Item itemVO2 = new ErpSaleQuoteSaveReqVO.Item();
        itemVO2.setProductId(503L);
        itemVO2.setWarehouseId(603L);
        itemVO2.setProductPrice(new BigDecimal("11.00"));
        itemVO2.setCount(new BigDecimal("2"));
        itemVO2.setGiftFlag(Boolean.FALSE);
        reqVO.setItems(Arrays.asList(itemVO1, itemVO2));

        assertServiceException(() -> saleQuoteService.createSaleQuote(reqVO),
                SALE_QUOTE_ITEM_DUPLICATE, "productId=503, warehouseId=603, giftFlag=false");
        verify(productService, never()).validProductList(any());
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
        verify(saleQuoteMapper, never()).insert(ArgumentMatchers.<ErpSaleQuoteDO>any());
    }

    // ==================== update ====================

    @Test
    public void testUpdateSaleQuote_processStatus_success() {
        Long quoteId = 60L;
        // 准备：existing quote 为 PROCESS（草稿）
        ErpSaleQuoteDO existQuote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000060")
                .setCustomerId(80L)
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus());
        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(existQuote);
        // 准备：更新参数
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setId(quoteId);
        reqVO.setCustomerId(80L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 11, 0));
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(800L);
        itemVO.setWarehouseId(900L);
        itemVO.setProductPrice(new BigDecimal("15.00"));
        itemVO.setCount(new BigDecimal("2"));
        reqVO.setItems(Collections.singletonList(itemVO));
        // mock
        ErpProductDO product = new ErpProductDO().setId(800L).setUnitId(1000L);
        when(productService.validProductList(any()))
                .thenReturn(Collections.singletonList(product));

        // 执行
        saleQuoteService.updateSaleQuote(reqVO);

        // 断言
        verify(saleQuoteMapper).updateById(ArgumentMatchers.<ErpSaleQuoteDO>argThat(update -> quoteId.equals(update.getId())));
        verify(saleQuoteItemMapper).deleteByQuoteId(eq(quoteId));
        verify(saleQuoteItemMapper).insertBatch(argThat((List<ErpSaleQuoteItemDO> items) -> items.size() == 1
                && quoteId.equals(items.get(0).getQuoteId())
                && Long.valueOf(1900L).equals(items.get(0).getDeptId())));
    }

    @Test
    public void testUpdateSaleQuote_emptyIncrement_keepsExistingItems() {
        Long quoteId = 62L;
        ErpSaleQuoteDO existQuote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000062")
                .setCustomerId(80L)
                .setDeptId(81L)
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus());
        ErpSaleQuoteItemDO oldItem = new ErpSaleQuoteItemDO()
                .setId(162L)
                .setQuoteId(quoteId)
                .setProductId(800L)
                .setWarehouseId(900L)
                .setProductPrice(new BigDecimal("15.00"))
                .setCount(new BigDecimal("2"))
                .setGiftFlag(Boolean.FALSE)
                .setConvertedCount(BigDecimal.ONE);
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setId(quoteId);
        reqVO.setCustomerId(80L);
        reqVO.setDeptId(81L);
        reqVO.setItems(Collections.emptyList());

        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(existQuote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(eq(quoteId)))
                .thenReturn(Collections.singletonList(oldItem));
        when(customerService.getCustomerSaleDeptIds(eq(80L))).thenReturn(Collections.singletonList(81L));
        when(productService.validProductList(eq(Collections.singleton(800L))))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(800L).setUnitId(1000L)));

        saleQuoteService.updateSaleQuote(reqVO);

        verify(saleQuoteMapper).updateById(ArgumentMatchers.<ErpSaleQuoteDO>argThat(update -> quoteId.equals(update.getId())
                && BigDecimal.valueOf(2).compareTo(update.getTotalCount()) == 0));
        verify(saleQuoteItemMapper, never()).deleteByQuoteId(anyLong());
        verify(saleQuoteItemMapper, never()).deleteByIds(anyCollection());
        verify(saleQuoteItemMapper, never()).insertBatch(any());
        verify(saleQuoteItemMapper, never()).updateBatch(ArgumentMatchers.<Collection<ErpSaleQuoteItemDO>>any());
    }

    @Test
    public void testUpdateSaleQuote_deleteAllItems_throwException() {
        Long quoteId = 63L;
        ErpSaleQuoteDO existQuote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000063")
                .setCustomerId(80L)
                .setDeptId(81L)
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus());
        ErpSaleQuoteItemDO oldItem = new ErpSaleQuoteItemDO()
                .setId(163L)
                .setQuoteId(quoteId)
                .setProductId(800L)
                .setWarehouseId(900L)
                .setProductPrice(new BigDecimal("15.00"))
                .setCount(new BigDecimal("2"))
                .setGiftFlag(Boolean.FALSE);
        ErpSaleQuoteSaveReqVO.Item deleteItem = new ErpSaleQuoteSaveReqVO.Item();
        deleteItem.setId(163L);
        deleteItem.setOperation("delete");
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setId(quoteId);
        reqVO.setCustomerId(80L);
        reqVO.setDeptId(81L);
        reqVO.setItems(Collections.singletonList(deleteItem));

        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(existQuote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(eq(quoteId)))
                .thenReturn(Collections.singletonList(oldItem));

        assertServiceException(() -> saleQuoteService.updateSaleQuote(reqVO),
                SALE_QUOTE_SUBMIT_ITEMS_REQUIRED);

        verify(saleQuoteMapper, never()).updateById(ArgumentMatchers.<ErpSaleQuoteDO>any());
        verify(saleQuoteItemMapper, never()).deleteByQuoteId(anyLong());
        verify(saleQuoteItemMapper, never()).deleteByIds(anyCollection());
    }

    @Test
    public void testUpdateSaleQuote_alreadyApproved_throwException() {
        Long quoteId = 61L;
        // 准备：existing quote 为 GENERATED_SALE_OUT（已生成销售单，不可改）
        ErpSaleQuoteDO existQuote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000061")
                .setStatus(ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus());
        when(saleQuoteMapper.selectById(eq(quoteId))).thenReturn(existQuote);
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setId(quoteId);
        reqVO.setCustomerId(81L);
        reqVO.setQuoteTime(LocalDateTime.of(2026, 5, 9, 11, 0));
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteSaveReqVO.Item()));

        // 执行 & 断言：抛 SALE_QUOTE_UPDATE_FAIL_GENERATED
        assertServiceException(() -> saleQuoteService.updateSaleQuote(reqVO),
                SALE_QUOTE_UPDATE_FAIL_GENERATED, existQuote.getNo());
        // 未发生数据库写
        verify(saleQuoteMapper, never()).updateById(ArgumentMatchers.<ErpSaleQuoteDO>any());
        verify(saleQuoteItemMapper, never()).deleteByQuoteId(any());
    }

    // ==================== approveSaleQuote ====================

    @Test
    public void testApproveSaleQuote_alreadyApproved_throwException() {
        Long quoteId = 70L;
        // 准备：quote 已经是 GENERATED_SALE_OUT，updateByIdAndStatus(... PROCESS ...) 会返回 0
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000070")
                .setCustomerId(82L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setStatus(ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);

        // 执行 & 断言：抛 SALE_QUOTE_APPROVE_FAIL
        assertServiceException(() -> saleQuoteService.approveSaleQuote(quoteId), SALE_QUOTE_APPROVE_FAIL);
        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any());
    }

    @Test
    public void testApproveSaleQuote_optimisticLockFail_throwException() {
        Long quoteId = 71L;
        // 准备：quote 初始为 PROCESS，但并发场景下 updateByIdAndStatus 返回 0
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000071")
                .setCustomerId(83L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);
        // 模拟乐观锁失败（被其它事务修改）
        when(saleQuoteMapper.updateByIdAndStatus(eq(quoteId), eq(ErpSaleQuoteStatusEnum.PROCESS.getStatus()),
                ArgumentMatchers.<ErpSaleQuoteDO>any())).thenReturn(0);

        // 执行 & 断言：抛 SALE_QUOTE_APPROVE_FAIL
        assertServiceException(() -> saleQuoteService.approveSaleQuote(quoteId), SALE_QUOTE_APPROVE_FAIL);
        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any());
    }

    @Test
    public void testReverseOrderSaleQuote_withoutConversion_success() {
        Long quoteId = 72L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO().setId(quoteId).setNo("BJ20260509000072")
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus());
        when(saleQuoteMapper.selectByIdForUpdate(quoteId)).thenReturn(quote);
        when(saleConvertRecordMapper.selectQuoteToCartListByQuoteIds(Collections.singleton(quoteId)))
                .thenReturn(Collections.emptyList());
        when(saleQuoteMapper.updateByIdAndStatus(eq(quoteId), eq(ErpSaleQuoteStatusEnum.APPROVE.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        saleQuoteService.reverseOrderSaleQuote(quoteId);

        verify(saleQuoteMapper).updateByIdAndStatus(eq(quoteId),
                eq(ErpSaleQuoteStatusEnum.APPROVE.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testReverseOrderSaleQuote_withConversion_rejected() {
        Long quoteId = 73L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO().setId(quoteId).setNo("BJ20260509000073")
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus());
        when(saleQuoteMapper.selectByIdForUpdate(quoteId)).thenReturn(quote);
        when(saleConvertRecordMapper.selectQuoteToCartListByQuoteIds(Collections.singleton(quoteId)))
                .thenReturn(Collections.singletonList(new cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO()
                        .setTargetId(173L)));
        when(saleCartMapper.selectByIds(Collections.singleton(173L))).thenReturn(Collections.singletonList(
                new cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO().setId(173L)));

        assertServiceException(() -> saleQuoteService.reverseOrderSaleQuote(quoteId),
                SALE_QUOTE_REVERSE_ORDER_FAIL_CONVERTED);
        verify(saleQuoteMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    // ==================== convertToCart ====================

    @Test
    public void testConvertToCart_exceedRemainingCount_throwException() {
        Long quoteId = 80L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000080")
                .setCustomerId(90L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 14, 0))
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus());
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(180L)
                .setQuoteId(quoteId)
                .setProductId(280L)
                .setWarehouseId(380L)
                .setProductPrice(new BigDecimal("20.00"))
                .setCount(new BigDecimal("10"))
                .setConvertedCount(new BigDecimal("3")); // 已转 3，剩余 7
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(eq(quoteId))).thenReturn(Collections.singletonList(item));

        // 本次转换 8 > 剩余 7
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteConvertCartReqVO.Item()
                .setQuoteItemId(item.getId()).setCount(new BigDecimal("8"))));

        // 执行 & 断言：抛 SALE_QUOTE_CONVERT_COUNT_EXCEED
        assertServiceException(() -> saleQuoteService.convertToCart(reqVO),
                SALE_QUOTE_CONVERT_COUNT_EXCEED, item.getId(), new BigDecimal("8"), new BigDecimal("7"));
        // 未发生任何写入
        verify(saleCartMapper, never()).insert(ArgumentMatchers.<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO>any());
        verify(saleQuoteItemMapper, never()).updateById(ArgumentMatchers.<ErpSaleQuoteItemDO>any());
    }

    @Test
    public void testConvertToCart_allItems_setStatusFullConverted() {
        Long quoteId = 81L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000081")
                .setCustomerId(91L)
                .setAccountId(101L)
                .setSaleUserId(111L)
                .setQuoteTime(LocalDateTime.of(2026, 5, 9, 15, 0))
                .setStatus(ErpSaleQuoteStatusEnum.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        // item count=10 convertedCount=0，本次转 10，应进入 CONVERTED_CART（完全转换）
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(181L)
                .setQuoteId(quoteId)
                .setProductId(281L)
                .setProductUnitId(381L)
                .setWarehouseId(481L)
                .setProductPrice(new BigDecimal("20.00"))
                .setCount(new BigDecimal("10"))
                .setConvertedCount(BigDecimal.ZERO);
        when(saleQuoteMapper.selectByIdForUpdate(eq(quoteId))).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(eq(quoteId))).thenReturn(Collections.singletonList(item));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(903L);
            return null;
        }).when(saleCartTransferLinkService).insertCart(any());
        when(saleCartItemMapper.insertBatch(any())).thenAnswer(invocation -> {
            Collection<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO> items = invocation.getArgument(0);
            items.iterator().next().setId(904L);
            return true;
        });
        // 库存查询桩
        Map<Long, BigDecimal> stockMap = new HashMap<>();
        stockMap.put(281L, new BigDecimal("100"));
        lenient().when(stockService.getStockCountMap(any())).thenReturn(stockMap);

        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteConvertCartReqVO.Item()
                .setQuoteItemId(item.getId()).setCount(new BigDecimal("10"))));

        // 执行
        saleQuoteService.convertToCart(reqVO);

        // 状态及累计数量由统一进度服务按转换记录刷新，避免多入口各自维护状态。
        verify(saleCartService).submitSaleCart(903L);
        verify(saleQuoteProgressService).refreshQuoteProgress(quoteId);
    }

    // ==================== draft ====================

    @Test
    public void testCreateSaleQuoteDraft_withoutValidItems_throwException() {
        ErpSaleQuoteDraftCreateReqVO reqVO = new ErpSaleQuoteDraftCreateReqVO();
        reqVO.setSaleUserId(70L);
        reqVO.setItems(Collections.singletonList(new ErpSaleQuoteSaveReqVO.Item()));

        assertServiceException(() -> saleQuoteService.createSaleQuoteDraft(reqVO),
                SALE_QUOTE_DRAFT_ITEMS_REQUIRED);

        verify(saleQuoteMapper, never()).insert(any(ErpSaleQuoteDO.class));
        verify(saleQuoteItemMapper, never()).insertBatch(any());
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
    }

    @Test
    public void testCreateSaleQuoteDraft_zeroProductPrice_success() {
        ErpSaleQuoteDraftCreateReqVO reqVO = new ErpSaleQuoteDraftCreateReqVO();
        reqVO.setDeptId(80L);
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setProductId(501L);
        itemVO.setWarehouseId(601L);
        itemVO.setProductPrice(BigDecimal.ZERO);
        itemVO.setCount(BigDecimal.ONE);
        itemVO.setGiftFlag(Boolean.FALSE);
        reqVO.setItems(Collections.singletonList(itemVO));

        when(productService.validProductList(eq(Collections.singleton(501L))))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(501L).setUnitId(701L)));
        when(saleQuoteMapper.selectByNo(anyString())).thenReturn(null);
        when(saleQuoteMapper.insert(ArgumentMatchers.<ErpSaleQuoteDO>any()))
                .thenAnswer(invocation -> {
                    ErpSaleQuoteDO quote = invocation.getArgument(0);
                    quote.setId(901L);
                    return 1;
                });

        Long quoteId = saleQuoteService.createSaleQuoteDraft(reqVO);

        assertEquals(901L, quoteId);
        verify(saleQuoteMapper).insert(ArgumentMatchers.<ErpSaleQuoteDO>argThat(quote ->
                ErpSaleQuoteStatusEnum.DRAFT.getStatus().equals(quote.getStatus())
                        && BigDecimal.ZERO.compareTo(quote.getTotalProductPrice()) == 0
                        && BigDecimal.ONE.compareTo(quote.getTotalCount()) == 0));
        verify(saleQuoteItemMapper).insertBatch(argThat(items -> {
            List<ErpSaleQuoteItemDO> list = new ArrayList<>(items);
            return list.size() == 1
                    && Long.valueOf(501L).equals(list.get(0).getProductId())
                    && Long.valueOf(601L).equals(list.get(0).getWarehouseId())
                    && Long.valueOf(701L).equals(list.get(0).getProductUnitId())
                    && BigDecimal.ZERO.compareTo(list.get(0).getProductPrice()) == 0
                    && BigDecimal.ZERO.compareTo(list.get(0).getTotalPrice()) == 0;
        }));
    }

    @Test
    public void testUpdateSaleQuoteDraft_emptyIncrementPreservesStatusAndItems() {
        Long quoteId = 102L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ-DRAFT-102")
                .setDeptId(80L)
                .setStatus(ErpSaleQuoteStatusEnum.DRAFT.getStatus());
        when(saleQuoteMapper.selectById(quoteId)).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(quoteId)).thenReturn(Collections.emptyList());
        ErpSaleQuoteDraftUpdateReqVO reqVO = new ErpSaleQuoteDraftUpdateReqVO();
        reqVO.setId(quoteId);
        reqVO.setItems(Collections.emptyList());

        saleQuoteService.updateSaleQuoteDraft(reqVO);

        verify(saleQuoteMapper).updateById(ArgumentMatchers.<ErpSaleQuoteDO>argThat(update ->
                quoteId.equals(update.getId())
                        && quote.getNo().equals(update.getNo())
                        && ErpSaleQuoteStatusEnum.DRAFT.getStatus().equals(update.getStatus())
                        && quote.getDeptId().equals(update.getDeptId())));
        verify(saleQuoteItemMapper, never()).deleteByQuoteId(quoteId);
        verify(saleQuoteItemMapper, never()).insertBatch(any());
    }

    @Test
    public void testUpdateSaleQuoteDraft_usesSaleDetailVisiblePriceMasking() {
        Long quoteId = 106L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ-DRAFT-106")
                .setCustomerId(90L)
                .setDeptId(80L)
                .setStatus(ErpSaleQuoteStatusEnum.DRAFT.getStatus());
        ErpSaleQuoteItemDO oldItem = new ErpSaleQuoteItemDO()
                .setId(206L)
                .setQuoteId(quoteId)
                .setProductId(306L)
                .setWarehouseId(406L)
                .setProductPrice(BigDecimal.ZERO)
                .setCount(BigDecimal.ONE)
                .setGiftFlag(Boolean.FALSE);
        ErpSaleQuoteDraftUpdateReqVO reqVO = new ErpSaleQuoteDraftUpdateReqVO();
        reqVO.setId(quoteId);
        reqVO.setCustomerId(90L);
        reqVO.setDeptId(80L);
        ErpSaleQuoteSaveReqVO.Item itemVO = new ErpSaleQuoteSaveReqVO.Item();
        itemVO.setId(206L);
        itemVO.setProductId(306L);
        itemVO.setWarehouseId(406L);
        itemVO.setProductPrice(new BigDecimal("20.00"));
        itemVO.setCount(BigDecimal.ONE);
        itemVO.setGiftFlag(Boolean.FALSE);
        reqVO.setItems(Collections.singletonList(itemVO));

        when(saleQuoteMapper.selectById(quoteId)).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteIdForUpdate(quoteId)).thenReturn(Collections.singletonList(oldItem));
        when(customerService.getCustomerSaleDeptIds(90L)).thenReturn(Collections.singletonList(80L));
        when(productService.validProductList(eq(Collections.singleton(306L))))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(306L).setUnitId(706L)));

        saleQuoteService.updateSaleQuoteDraft(reqVO);

        verify(fieldPermissionMasker).preserveSaleDetailHiddenFields("erp_sale_quote", reqVO, quote);
        verify(fieldPermissionMasker).preserveSaleDetailHiddenItemFields("erp_sale_quote", reqVO,
                reqVO.getItems(), Collections.singletonList(oldItem));
        verify(saleQuoteItemMapper).insertBatch(argThat(items -> {
            List<ErpSaleQuoteItemDO> list = new ArrayList<>(items);
            return list.size() == 1
                    && new BigDecimal("20.00").compareTo(list.get(0).getProductPrice()) == 0
                    && new BigDecimal("20.00").compareTo(list.get(0).getTotalPrice()) == 0;
        }));
    }

    @Test
    public void testSubmitSaleQuote_draftBecomesProcess() {
        Long quoteId = 103L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ-DRAFT-103")
                .setCustomerId(90L)
                .setDeptId(80L)
                .setStatus(ErpSaleQuoteStatusEnum.DRAFT.getStatus());
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setId(203L)
                .setQuoteId(quoteId)
                .setProductId(303L)
                .setWarehouseId(403L)
                .setProductPrice(new BigDecimal("12.00"))
                .setCount(BigDecimal.ONE)
                .setGiftFlag(Boolean.FALSE);
        when(saleQuoteMapper.selectById(quoteId)).thenReturn(quote);
        when(saleQuoteItemMapper.selectListByQuoteId(quoteId))
                .thenReturn(Collections.singletonList(item));
        when(customerService.getCustomerSaleDeptIds(90L)).thenReturn(Collections.singletonList(80L));
        when(productService.validProductList(eq(Collections.singleton(303L))))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(303L).setUnitId(503L)));
        when(saleQuoteMapper.updateByIdAndStatus(eq(quoteId), eq(ErpSaleQuoteStatusEnum.DRAFT.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        saleQuoteService.submitSaleQuote(quoteId);

        verify(customerService).validateCustomerForSale(90L, 80L);
        verify(customerService).validateCustomerSaleDept(90L, 80L);
        verify(saleQuoteMapper).updateByIdAndStatus(eq(quoteId),
                eq(ErpSaleQuoteStatusEnum.DRAFT.getStatus()),
                argThat(update -> ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(update.getStatus())
                        && Long.valueOf(80L).equals(update.getDeptId())));
    }

    @Test
    public void testSubmitSaleQuote_nonDraftRejected() {
        Long quoteId = 104L;
        when(saleQuoteMapper.selectById(quoteId)).thenReturn(new ErpSaleQuoteDO()
                .setId(quoteId)
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> saleQuoteService.submitSaleQuote(quoteId),
                SALE_QUOTE_SUBMIT_FAIL);
        verify(saleQuoteMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    public void testConvertToCart_draftRejected() {
        Long quoteId = 105L;
        when(saleQuoteMapper.selectByIdForUpdate(quoteId)).thenReturn(new ErpSaleQuoteDO()
                .setId(quoteId)
                .setStatus(ErpSaleQuoteStatusEnum.DRAFT.getStatus()));
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(quoteId);
        reqVO.setItems(Collections.emptyList());

        assertServiceException(() -> saleQuoteService.convertToCart(reqVO),
                SALE_QUOTE_CONVERT_CART_FAIL);
        verify(saleQuoteItemMapper, never()).selectListByQuoteIdForUpdate(any());
        verify(saleCartMapper, never()).insert(ArgumentMatchers.<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO>any());
    }

    // ==================== delete ====================

    @Test
    public void testDeleteSaleQuote_processStatus_success() {
        Long quoteId = 90L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000090")
                .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus());
        when(saleQuoteMapper.selectByIds(eq(Collections.singletonList(quoteId))))
                .thenReturn(Collections.singletonList(quote));

        // 执行
        saleQuoteService.deleteSaleQuote(Collections.singletonList(quoteId));

        // 断言
        verify(saleQuoteMapper).deleteById(eq(quoteId));
        verify(saleQuoteItemMapper).deleteByQuoteId(eq(quoteId));
    }

    @Test
    public void testDeleteSaleQuote_alreadyApproved_throwException() {
        Long quoteId = 91L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO()
                .setId(quoteId)
                .setNo("BJ20260509000091")
                .setStatus(ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus());
        when(saleQuoteMapper.selectByIds(eq(Collections.singletonList(quoteId))))
                .thenReturn(Collections.singletonList(quote));

        // 执行 & 断言：抛 SALE_QUOTE_DELETE_FAIL_GENERATED
        assertServiceException(() -> saleQuoteService.deleteSaleQuote(Collections.singletonList(quoteId)),
                SALE_QUOTE_DELETE_FAIL_GENERATED, quote.getNo());
        // 未发生删除
        verify(saleQuoteMapper, never()).deleteById(any());
        verify(saleQuoteItemMapper, never()).deleteByQuoteId(any());
    }

}
