package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockLockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveOperationPermission;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_CONVERT_QUOTE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_CANCEL_FIRST_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_DELETE_FAIL_NOT_DRAFT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_FINAL_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_FIRST_APPROVE_DEPT_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_FIRST_APPROVE_DISABLED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_FIRST_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_REJECT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_UPDATE_FAIL_NOT_PROCESS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_UNLOCK_STATUS_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_WAREHOUSE_TRANSFER_NOT_APPROVED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE2;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_UNLOCK_APPROVED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_UNLOCK_NOT_CART_SOURCE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_UNLOCK_NOT_TRANSFER_OUT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_UNLOCK_SOURCE_APPROVED_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_UNLOCK_SOURCE_ID_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private ErpSaleConfigMapper saleConfigMapper;
    @Mock
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Mock
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Mock
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpReceivableOtherService receivableOtherService;
    @Mock
    private ErpPayableExpenseService payableExpenseService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpStockLockService stockLockService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Mock
    private ErpSaleItemBatchUpdateSupport batchUpdateSupport;
    @Mock
    private ErpSalePriceLevelPricePicker priceLevelPricePicker;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpProductBatchNoValidator productBatchNoValidator;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleCartService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260509000001";
            }
        });
        lenient().when(warehouseService.validSaleWarehouseList(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            List<ErpWarehouseDO> warehouses = new ArrayList<>();
            for (Long id : ids) {
                if (id != null) {
                    warehouses.add(new ErpWarehouseDO().setId(id).setDeptId(10L).setStockBillEnabled(false));
                }
            }
            return warehouses;
        });
        lenient().when(warehouseService.validSaleWarehouseListForDept(anyCollection(), any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            Long deptId = invocation.getArgument(1);
            List<ErpWarehouseDO> warehouses = new ArrayList<>();
            for (Long id : ids) {
                if (id != null) {
                    warehouses.add(new ErpWarehouseDO().setId(id).setDeptId(deptId).setStockBillEnabled(false));
                }
            }
            return warehouses;
        });
        lenient().when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            Long deptId = invocation.getArgument(1);
            List<ErpWarehouseDO> warehouses = new ArrayList<>();
            for (Long id : ids) {
                if (id != null) {
                    warehouses.add(new ErpWarehouseDO().setId(id).setDeptId(deptId).setStockBillEnabled(false));
                }
            }
            return warehouses;
        });
        lenient().doNothing().when(warehouseService).validateWarehouseSaleAllowedForDept(any(), any());
        lenient().doNothing().when(warehouseService).validateWarehouseSaleSelectableForDept(any(), any());
        lenient().when(warehouseService.isWarehouseSaleAllowedForDept(any(), any())).thenAnswer(invocation -> {
            Long warehouseId = invocation.getArgument(0);
            Long deptId = invocation.getArgument(1);
            return deptId == null || !Long.valueOf(401L).equals(warehouseId);
        });
        lenient().when(warehouseService.resolveDirectWarehouseId(any())).thenReturn(888L);
        lenient().when(saleCartMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);
        lenient().when(stockService.getStock(any(), any()))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        lenient().when(stockService.getOccupiedCountMap(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyMap());
    }

    @AfterEach
    public void clearDataPermissionContext() {
        DataPermissionContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    public void testParseImportData_onlyFactoryCode_success() {
        ErpSaleCartImportExcelVO row = new ErpSaleCartImportExcelVO();
        row.setWarehouseName("默认仓库");
        row.setFactoryCode("F001");
        row.setCount(BigDecimal.ONE);
        row.setProductPrice(new BigDecimal("12.00"));
        ErpProductDO product = new ErpProductDO()
                .setId(201L).setCode("P001").setName("机油滤芯").setFactoryCode("F001")
                .setUnitId(1L).setSalePrice(new BigDecimal("10.00"));
        when(productMapper.selectListByFactoryCodes(anyCollection())).thenReturn(Collections.singletonList(product));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList()).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(301L).setName("默认仓库")));

        ErpSaleCartImportRespVO respVO = saleCartService.parseImportData(Collections.singletonList(row));

        assertEquals(1, respVO.getSuccessCount());
        assertEquals(0, respVO.getFailureCount());
        assertEquals(Long.valueOf(201L), respVO.getItems().get(0).getProductId());
        assertEquals("P001", respVO.getItems().get(0).getProductCode());
    }

    @Test
    public void testParseImportData_missingProductIdentity_returnsReadableMessage() {
        ErpSaleCartImportExcelVO row = new ErpSaleCartImportExcelVO();
        row.setWarehouseName("默认仓库");
        row.setCount(BigDecimal.ONE);
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList()).thenReturn(Collections.emptyList());

        ErpSaleCartImportRespVO respVO = saleCartService.parseImportData(Collections.singletonList(row));

        assertEquals(0, respVO.getSuccessCount());
        assertEquals(1, respVO.getFailureCount());
        assertEquals(2, respVO.getFailureDetails().get(0).getRowNo());
        assertEquals("配件编码、配件名称和厂家编码为三选一字段，请至少填写其中一个",
                respVO.getFailureDetails().get(0).getReason());
    }

    // ==================== ???????????????????????????====================

    @Test
    public void testFinalApproveSaleCart_createGeneratedSaleOutWithCartSourceAndWarehouse() {
        Long cartId = 11L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000001")
                .setCustomerId(21L)
                .setAccountId(31L)
                .setSaleUserId(41L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setFileUrl("https://example.com/legacy-express.jpg")
                .setDeliveryMethod("????????????")
                .setVin("LGBH52E03HY123456")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(101L)
                .setCartId(cartId)
                .setProductId(201L)
                .setWarehouseId(301L)
                .setDeptId(10L)
                .setProductPrice(new BigDecimal("15.00"))
                .setCount(new BigDecimal("3"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        // finalApproveSaleCart ???????????????????????? stockService ???????????????????????????
        when(stockService.getStock(eq(201L), eq(301L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(301L,
                new ErpWarehouseDO().setId(301L).setStockBillEnabled(false)));
        saleCartService.finalApproveSaleCart(cartId);

        verify(saleOutService).createGeneratedSaleOut(argThat(req -> cart.getCustomerId().equals(req.getCustomerId())
                        && cart.getAccountId().equals(req.getAccountId())
                        && cart.getSaleUserId().equals(req.getSaleUserId())
                        && cart.getCartTime().equals(req.getOutTime())
                        && cart.getDeliveryMethod().equals(req.getDeliveryMethod())
                        && cart.getVin().equals(req.getVin())
                        && req.getFileUrl() == null
                        && req.getItems().size() == 1
                        && item.getProductId().equals(req.getItems().get(0).getProductId())
                        && item.getWarehouseId().equals(req.getItems().get(0).getWarehouseId())
                        && item.getCount().equals(req.getItems().get(0).getCount())),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cart.getId()), eq(cart.getNo()), eq(false));
    }

    @Test
    public void testFinalApproveSaleCart_customerAdvanceFreight_createsReceivableDraft() {
        Long cartId = 111L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .setFreightType("代客户付")
                .setFeeAmount(new BigDecimal("88.50"))
                .setLogisticsCompany("Logistics");
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 301L, new BigDecimal("3"));
        when(saleCartMapper.selectById(cartId)).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(cartId)).thenReturn(Collections.singletonList(item));

        saleCartService.finalApproveSaleCart(cartId);

        verify(receivableOtherService).createFromSaleCartFreight(
                argThat((ErpSaleCartFreightDraftCreateReqBO req) ->
                        cartId.equals(req.getCartId())
                                && cart.getNo().equals(req.getCartNo())
                                && cart.getCartTime().toLocalDate().equals(req.getBizTime())
                                && cart.getCustomerId().equals(req.getCustomerId())
                                && cart.getDeptId().equals(req.getDeptId())
                                && cart.getSaleUserId().equals(req.getHandlerId())
                                && cart.getFeeAmount().equals(req.getAmount())));
        verify(payableExpenseService, never()).createFromSaleCartFreight(any());
    }

    @Test
    public void testFinalApproveSaleCart_selfPayFreight_createsExpenseDraft() {
        Long cartId = 112L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .setFreightType("我方自付")
                .setSettleMethod("Cash")
                .setFeeAmount(new BigDecimal("66.00"))
                .setLogisticsCompany("Logistics");
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 301L, new BigDecimal("3"));
        when(saleCartMapper.selectById(cartId)).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(cartId)).thenReturn(Collections.singletonList(item));

        saleCartService.finalApproveSaleCart(cartId);

        verify(payableExpenseService).createFromSaleCartFreight(
                argThat((ErpSaleCartFreightDraftCreateReqBO req) ->
                        cartId.equals(req.getCartId())
                                && cart.getSettleMethod().equals(req.getSettleMethod())
                                && cart.getAccountId().equals(req.getAccountId())
                                && cart.getLogisticsCompany().equals(req.getParty())
                                && cart.getFeeAmount().equals(req.getAmount())));
        verify(receivableOtherService, never()).createFromSaleCartFreight(any());
    }

    @Test
    public void testFinalApproveSaleCart_stockBillCartWarehouse_releasesCartLockAfterBillTakesOver() {
        Long cartId = 12L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .setDeptId(10L);
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 301L, new BigDecimal("3"));
        when(saleCartMapper.selectById(cartId)).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(cartId)).thenReturn(Collections.singletonList(item));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(10L))).thenAnswer(invocation -> {
            Collection<Long> warehouseIds = invocation.getArgument(0);
            List<ErpWarehouseDO> warehouses = new ArrayList<>();
            for (Long warehouseId : warehouseIds) {
                warehouses.add(new ErpWarehouseDO().setId(warehouseId).setDeptId(10L)
                        .setStockBillEnabled(item.getWarehouseId().equals(warehouseId)));
            }
            return warehouses;
        });

        saleCartService.finalApproveSaleCart(cartId);

        verify(saleOutService).createGeneratedSaleOut(any(), eq(ErpSaleBizSourceTypeEnum.CART.getType()),
                eq(cartId), eq(cart.getNo()), eq(true));
        verify(stockLockService).unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
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
                .setDeliveryMethod("????????????")
                .setVin("LSVAA6BR9PN123456")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(103L)
                .setCartId(cartId)
                .setProductId(203L)
                .setProductUnitId(303L)
                .setWarehouseId(403L)
                .setDeptId(10L)
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
                && cart.getDeliveryMethod().equals(quote.getDeliveryMethod())
                && cart.getVin().equals(quote.getVin())
                && ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())));
        verify(saleQuoteItemMapper).insertBatch(argThat((java.util.List<ErpSaleQuoteItemDO> items) -> items.size() == 1
                && item.getProductId().equals(items.get(0).getProductId())
                && new BigDecimal("5").compareTo(items.get(0).getCount()) == 0));
        verify(saleConvertRecordMapper).insertBatch(argThat(records -> {
            java.util.List<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO> list = new ArrayList<>(records);
            return list.size() == 1 && item.getId().equals(list.get(0).getSourceItemId())
                    && new BigDecimal("5").compareTo(list.get(0).getCount()) == 0;
        }));
        // ?????????????????????????????????????????????
        verify(saleCartItemMapper).deleteByCartId(eq(cart.getId()));
        verify(saleCartMapper).deleteById(eq(cart.getId()));
    }

    @Test
    public void testConvertToQuote_submittedCart_success() {
        Long cartId = 14L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000003")
                .setCustomerId(24L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 14, 0))
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(104L)
                .setCartId(cartId)
                .setProductId(204L)
                .setProductUnitId(304L)
                .setWarehouseId(404L)
                .setDeptId(10L)
                .setProductPrice(new BigDecimal("20.00"))
                .setCount(new BigDecimal("2"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(saleQuoteMapper.selectByNo(anyString())).thenReturn(null);

        ErpSaleCartConvertQuoteReqVO reqVO = new ErpSaleCartConvertQuoteReqVO();
        reqVO.setCartId(cartId);
        saleCartService.convertToQuote(reqVO);

        verify(saleQuoteMapper).insert(ArgumentMatchers.<ErpSaleQuoteDO>argThat(quote ->
                ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())
                        && cart.getId().equals(quote.getSourceId())));
        verify(saleCartItemMapper).deleteByCartId(eq(cartId));
        verify(saleCartMapper).deleteById(eq(cartId));
    }

    @Test
    public void testDeleteSaleCart_success_whenBeforeFirstApprove() {
        Long cartId = 51L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260510000001")
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        when(saleCartMapper.selectBatchIds(eq(Collections.singletonList(cartId))))
                .thenReturn(Collections.singletonList(cart));

        saleCartService.deleteSaleCart(Collections.singletonList(cartId));

        verify(stockMoveService).deleteUnapprovedTransferOutBySource(
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId));
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
                SALE_CART_DELETE_FAIL_NOT_DRAFT, cart.getNo());
    }

    // ==================== create ?????????====================

    @Test
    public void testCreateSaleCart_normalCase_createDraftReturnId() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setAccountId(31L);
        reqVO.setSaleUserId(41L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomerForSale(eq(21L), nullable(Long.class))).thenReturn(new ErpCustomerDO().setId(21L));
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
                        && cart.getNo() != null && cart.getNo().startsWith(ErpNoRedisDAO.SALE_CART_NO_PREFIX)
                        && cart.getCreator() == null
                        && cart.getUpdater() == null));
        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) -> items.size() == 1
                && items.get(0).getCartId().equals(999L)));
        verify(customerService).validateCustomerForSale(eq(21L), nullable(Long.class));
        verify(customerService, never()).validateCustomerForGeneratedSale(anyLong(), nullable(Long.class));
    }

    @Test
    public void testCreateSaleCartDraftFromSource_validateCustomerAsGeneratedSale() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setDeptId(10L);
        reqVO.setCustomerId(21L);
        reqVO.setSourceType(ErpSaleBizSourceTypeEnum.MALL_ORDER.getType());
        reqVO.setSourceId(501L);
        reqVO.setSourceNo("MALL20260827000001");
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomerForGeneratedSale(eq(21L), eq(10L)))
                .thenReturn(new ErpCustomerDO().setId(21L));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(999L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        Long resultId = saleCartService.createSaleCartDraftFromSource(reqVO);

        assertEquals(999L, resultId);
        verify(customerService).validateCustomerForGeneratedSale(eq(21L), eq(10L));
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
        verify(saleCartMapper).insert(argThat((ErpSaleCartDO cart) ->
                ErpSaleBizSourceTypeEnum.MALL_ORDER.getType().equals(cart.getSourceType())
                        && Long.valueOf(501L).equals(cart.getSourceId())
                        && "MALL20260827000001".equals(cart.getSourceNo())
                        && Long.valueOf(121L).equals(cart.getSaleUserId())
                        && "121".equals(cart.getCreator())
                        && "121".equals(cart.getUpdater())));
        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) -> items.size() == 1
                && items.get(0).getCartId().equals(999L)
                && "121".equals(items.get(0).getCreator())
                && "121".equals(items.get(0).getUpdater())));
    }

    @Test
    public void testCreateSaleCart_zeroProductPriceDraft_success() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setDeptId(10L);
        reqVO.setItems(Collections.singletonList(buildItemReq(BigDecimal.ONE, BigDecimal.ZERO)));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
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
                        && BigDecimal.ZERO.compareTo(cart.getTotalProductPrice()) == 0
                        && BigDecimal.ONE.compareTo(cart.getTotalCount()) == 0));
        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) -> items.size() == 1
                && Long.valueOf(201L).equals(items.get(0).getProductId())
                && Long.valueOf(401L).equals(items.get(0).getWarehouseId())
                && Long.valueOf(301L).equals(items.get(0).getProductUnitId())
                && BigDecimal.ZERO.compareTo(items.get(0).getProductPrice()) == 0
                && BigDecimal.ZERO.compareTo(items.get(0).getTotalPrice()) == 0));
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
    }

    @Test
    public void testCreateSaleCart_emptyDraft_throwException() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(null);
        reqVO.setItems(Collections.emptyList());

        assertServiceException(() -> saleCartService.createSaleCart(reqVO),
                SALE_CART_DRAFT_ITEMS_REQUIRED);

        verify(saleCartMapper, never()).insert(any(ErpSaleCartDO.class));
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
        verify(saleCartItemMapper, never()).insertBatch(anyCollection());
        verify(stockLockService, never()).lockStock(any(), any(), any(), any(), any(), any(), any());
        verify(stockMoveService, never()).syncTransferOutDraftsBySource(any());
    }

    @Test
    public void testCreateSaleCart_crossDeptProductIgnoreDataPermission_success() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setDeptId(99L);
        ErpSaleCartSaveReqVO.Item itemReq = buildItemReq(new BigDecimal("1"), new BigDecimal("15.00"));
        itemReq.setDeptId(null);
        reqVO.setItems(Collections.singletonList(itemReq));

        when(productService.validProductList(anyCollection())).thenAnswer(invocation -> {
            if (DataPermissionContextHolder.get() == null || DataPermissionContextHolder.get().enable()) {
                throw new ServiceException(PRODUCT_NOT_EXISTS);
            }
            return Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L));
        });
        lenient().when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setStockBillEnabled(false)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("2")));
        when(customerService.validateCustomerForSale(eq(21L), nullable(Long.class))).thenReturn(new ErpCustomerDO().setId(21L));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(999L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        Long resultId = saleCartService.createSaleCart(reqVO);

        assertEquals(999L, resultId);
        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) -> items.size() == 1
                && Long.valueOf(99L).equals(items.get(0).getDeptId())
                && new BigDecimal("2").compareTo(items.get(0).getStockCount()) == 0));
        verify(warehouseService).validateWarehouseSaleSelectableForDept(eq(401L), eq(99L));
    }

    @Test
    public void testCreateAndSubmitSaleCart_success_whenCreatedCartNotVisibleBySelect() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setAccountId(31L);
        reqVO.setSaleUserId(41L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomerForSale(eq(21L), nullable(Long.class))).thenReturn(new ErpCustomerDO().setId(21L));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(401L,
                new ErpWarehouseDO().setId(401L).setName("??????????????????")));
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(999L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        ErpSaleCartSubmitRespVO result = saleCartService.createAndSubmitSaleCart(reqVO);

        assertEquals(999L, result.getId());
        assertEquals(ErpSaleCartStatusEnum.SUBMITTED.getStatus(), result.getStatus());
        verify(saleCartMapper).insert(argThat((ErpSaleCartDO cart) ->
                ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())
                        && reqVO.getCustomerId().equals(cart.getCustomerId())));
        verify(saleCartMapper, never()).selectById(eq(999L));
        verify(saleCartMapper, never()).updateByIdAndStatus(eq(999L), any(), any());
    }

    @Test
    public void testCreateAndSubmitSaleCartFromPurchaseIn_validateAvailableStockExcludingCurrentCart() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setSourceType(ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType());
        reqVO.setSourceId(501L);
        reqVO.setSourceNo("RK20260509000001");
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("6"), new BigDecimal("15.00"))));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        // The occupied query already contains the 6 units of the newly inserted cart.
        when(stockService.getOccupiedCountMap(anyCollection(), anyCollection()))
                .thenReturn(Collections.singletonMap("201_401", new BigDecimal("6")));
        when(customerService.validateCustomerForGeneratedSale(eq(21L), nullable(Long.class)))
                .thenReturn(new ErpCustomerDO().setId(21L));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(999L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        ErpSaleCartSubmitRespVO result = saleCartService.createAndSubmitSaleCartFromPurchaseIn(reqVO);

        assertEquals(999L, result.getId());
        assertEquals(ErpSaleCartStatusEnum.SUBMITTED.getStatus(), result.getStatus());
        verify(customerService).validateCustomerForGeneratedSale(eq(21L), nullable(Long.class));
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
    }

    @Test
    public void testCreateAndSubmitSaleCart_success_whenFirstApproveDisabled() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        mockFirstApproveConfig(false, false, true);
        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomerForSale(eq(21L), nullable(Long.class))).thenReturn(new ErpCustomerDO().setId(21L));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(999L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        ErpSaleCartSubmitRespVO result = saleCartService.createAndSubmitSaleCart(reqVO);

        assertEquals(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(), result.getStatus());
        verify(saleCartMapper).insert(argThat((ErpSaleCartDO cart) ->
                ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())));
    }

    @Test
    public void testCreateAndSubmitSaleCart_crossDeptWhenFirstApproveSkipped_createsTransferOutDraft() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setDeptId(99L);
        ErpSaleCartSaveReqVO.Item itemReq = buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"));
        itemReq.setDeptId(null);
        reqVO.setItems(Collections.singletonList(itemReq));

        mockFirstApproveConfig(false, false, true);
        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")
                        .setStockBillEnabled(false)));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(99L))).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")
                        .setStockBillEnabled(false)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomerForSale(eq(21L), nullable(Long.class))).thenReturn(new ErpCustomerDO().setId(21L));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(998L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        ErpSaleCartSubmitRespVO result = saleCartService.createAndSubmitSaleCart(reqVO);

        assertEquals(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(), result.getStatus());
        verify(stockLockService).lockStock(eq(201L), eq(401L), eq(new BigDecimal("3")),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(998L), any(), anyString());
        verify(stockMoveService).syncTransferOutDraftsBySource(
                ArgumentMatchers.<List<ErpStockMoveSaveReqVO>>argThat(reqs -> reqs.size() == 1
                        && Integer.valueOf(10).equals(reqs.get(0).getTransferDirection())
                        && Long.valueOf(20L).equals(reqs.get(0).getFromDeptId())
                        && Long.valueOf(998L).equals(reqs.get(0).getSourceId())
                        && Long.valueOf(401L).equals(reqs.get(0).getItems().get(0).getFromWarehouseId())
                        && Long.valueOf(888L).equals(reqs.get(0).getItems().get(0).getToWarehouseId())));
    }

    @Test
    public void testCreateAndSubmitSaleCart_crossDeptItemWithoutDept_createStockMoveDraft() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(21L);
        reqVO.setDeptId(99L);
        ErpSaleCartSaveReqVO.Item itemReq = buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"));
        itemReq.setDeptId(null);
        reqVO.setItems(Collections.singletonList(itemReq));

        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")
                        .setStockBillEnabled(false)));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(401L,
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomerForSale(eq(21L), nullable(Long.class))).thenReturn(new ErpCustomerDO().setId(21L));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));
        when(saleCartMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleCartDO cart = invocation.getArgument(0);
            cart.setId(999L);
            return 1;
        }).when(saleCartMapper).insert(any(ErpSaleCartDO.class));

        saleCartService.createAndSubmitSaleCart(reqVO);

        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) -> items.size() == 1
                && Long.valueOf(99L).equals(items.get(0).getDeptId())
                && Long.valueOf(999L).equals(items.get(0).getCartId())));
        verify(warehouseService).validateWarehouseSaleSelectableForDept(eq(401L), eq(99L));
        verify(stockMoveService, never()).createOrUpdateTransferOutDraftBySource(ArgumentMatchers.<ErpStockMoveSaveReqVO>argThat(req ->
                Long.valueOf(99L).equals(req.getDeptId())
                        && ErpSaleBizSourceTypeEnum.CART.getType().equals(req.getSourceType())
                        && Long.valueOf(999L).equals(req.getSourceId())
                        && req.getItems().size() == 1
                        && Long.valueOf(401L).equals(req.getItems().get(0).getFromWarehouseId())
                        && Long.valueOf(201L).equals(req.getItems().get(0).getProductId())));
    }

    @Test
    public void testCreateSaleCart_invalidCustomer_throwException() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setCustomerId(99L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(customerService.validateCustomerForSale(eq(99L), nullable(Long.class)))
                .thenThrow(new ServiceException(CUSTOMER_NOT_EXISTS));

        assertServiceException(() -> saleCartService.createSaleCart(reqVO), CUSTOMER_NOT_EXISTS);
        verify(saleCartMapper, never()).insert(any(ErpSaleCartDO.class));
    }

    // ==================== update ?????????====================

    @Test
    public void testUpdateSaleCart_beforeFirstApprove_success() {
        ErpSaleCartSaveReqVO reqVO = buildBaseSaveReq();
        reqVO.setId(11L);
        reqVO.setCustomerId(21L);
        reqVO.setItems(Collections.singletonList(buildItemReq(new BigDecimal("3"), new BigDecimal("15.00"))));

        when(saleCartMapper.selectById(eq(11L)))
                .thenReturn(new ErpSaleCartDO().setId(11L).setNo("ST20260509000001")
                        .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus()));
        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(201L).setUnitId(301L)));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(customerService.validateCustomerForSale(eq(21L), nullable(Long.class))).thenReturn(new ErpCustomerDO().setId(21L));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));

        saleCartService.updateSaleCart(reqVO);

        verify(saleCartMapper).updateById(argThat((ErpSaleCartDO cart) -> reqVO.getId().equals(cart.getId())
                && reqVO.getCustomerId().equals(cart.getCustomerId())));
        verify(saleCartItemMapper).deleteByCartId(eq(reqVO.getId()));
        verify(saleCartItemMapper).insertBatch(argThat((java.util.List<ErpSaleCartItemDO> items) ->
                items.size() == 1 && items.get(0).getCartId().equals(reqVO.getId())));
        verify(stockMoveService, never()).createOrUpdateTransferOutDraftBySource(any());
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

    @Test
    public void testUpdateSaleCartDraft_emptyContent_success() {
        ErpSaleCartSaveReqVO reqVO = new ErpSaleCartSaveReqVO();
        reqVO.setId(11L);
        reqVO.setRemark("????????????");
        reqVO.setItems(Collections.emptyList());

        when(saleCartMapper.selectById(eq(11L)))
                .thenReturn(new ErpSaleCartDO().setId(11L).setNo("ST20260509000001")
                        .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        when(saleCartItemMapper.selectListByCartId(eq(11L))).thenReturn(Collections.emptyList());

        saleCartService.updateSaleCartDraft(reqVO);

        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
        verify(saleCartMapper).updateById(argThat((ErpSaleCartDO cart) ->
                reqVO.getId().equals(cart.getId()) && reqVO.getRemark().equals(cart.getRemark())));
        verify(saleCartItemMapper).deleteByCartId(eq(reqVO.getId()));
        verify(saleCartItemMapper, never()).insertBatch(any());
    }

    @Test
    public void testUpdateSaleCartDraft_submitted_throwException() {
        ErpSaleCartSaveReqVO reqVO = new ErpSaleCartSaveReqVO();
        reqVO.setId(11L);
        reqVO.setItems(Collections.emptyList());
        ErpSaleCartDO existing = new ErpSaleCartDO().setId(11L).setNo("ST20260509000001")
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        when(saleCartMapper.selectById(eq(11L))).thenReturn(existing);

        assertServiceException(() -> saleCartService.updateSaleCartDraft(reqVO),
                SALE_CART_UPDATE_FAIL_NOT_PROCESS, existing.getNo());

        verify(saleCartMapper, never()).updateById(any(ErpSaleCartDO.class));
        verify(saleCartItemMapper, never()).deleteByCartId(any());
    }

    // ==================== ?????????????????????ubmit / firstApprove / reject ====================

    @Test
    public void testSubmitSaleCart_processToFirstApprove_success() {
        Long cartId = 71L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000003")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(401L,
                new ErpWarehouseDO().setId(401L).setName("Warehouse1")));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        ErpSaleCartSubmitRespVO result = saleCartService.submitSaleCart(cartId);

        assertEquals(ErpSaleCartStatusEnum.SUBMITTED.getStatus(), result.getStatus());
        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testSubmitSaleCart_processToFinalApprove_whenFirstApproveDisabled() {
        Long cartId = 171L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000013")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockFirstApproveConfig(false, false, true);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(401L,
                new ErpWarehouseDO().setId(401L).setName("??????????????????")));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        ErpSaleCartSubmitRespVO result = saleCartService.submitSaleCart(cartId);

        assertEquals(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(), result.getStatus());
        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())
                        && update.getFirstAuditTime() == null && update.getFirstAuditUserId() == null));
    }

    @Test
    public void testSubmitSaleCart_processToFinalApprove_whenDeptNotRequireFirstApprove() {
        Long cartId = 181L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000019")
                .setDeptId(99L).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockFirstApproveConfig(true, true, false);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(Collections.singletonList(new ErpSaleConfigDO().setDeptId(10L)));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        ErpSaleCartSubmitRespVO result = saleCartService.submitSaleCart(cartId);

        assertEquals(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(), result.getStatus());
        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())
                        && update.getFirstAuditTime() == null && update.getFirstAuditUserId() == null));
    }

    @Test
    public void testSubmitSaleCart_processToFirstApprove_whenDeptRequiresFirstApprove() {
        Long cartId = 182L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000020")
                .setDeptId(10L).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockFirstApproveConfig(true, true, false);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(Collections.singletonList(new ErpSaleConfigDO().setDeptId(10L)));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        ErpSaleCartSubmitRespVO result = saleCartService.submitSaleCart(cartId);

        assertEquals(ErpSaleCartStatusEnum.SUBMITTED.getStatus(), result.getStatus());
        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testSubmitSaleCart_crossDept_createStockMoveDraft() {
        Long cartId = 271L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000016")
                .setDeptId(99L).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(99L))).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")
                        .setStockBillEnabled(false)));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(401L,
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()), any(ErpSaleCartDO.class))).thenReturn(1);

        saleCartService.submitSaleCart(cartId);

        verify(stockMoveService, never()).createOrUpdateTransferOutDraftBySource(ArgumentMatchers.<ErpStockMoveSaveReqVO>argThat(req ->
                cart.getDeptId().equals(req.getDeptId())
                        && ErpSaleBizSourceTypeEnum.CART.getType().equals(req.getSourceType())
                        && cart.getId().equals(req.getSourceId())
                        && cart.getNo().equals(req.getSourceNo())
                        && req.getItems().size() == 1
                        && item.getWarehouseId().equals(req.getItems().get(0).getFromWarehouseId())
                        && item.getProductId().equals(req.getItems().get(0).getProductId())
                        && item.getCount().compareTo(req.getItems().get(0).getCount()) == 0));
    }

    @Test
    public void testSubmitSaleCart_crossDeptOldItemDeptNormalized_createStockMoveDraft() {
        Long cartId = 272L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000017")
                .setDeptId(99L).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        item.setDeptId(20L);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(99L))).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")
                        .setStockBillEnabled(false)));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(401L,
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()), any(ErpSaleCartDO.class))).thenReturn(1);

        saleCartService.submitSaleCart(cartId);

        assertEquals(20L, item.getDeptId());
        verify(stockMoveService, never()).createOrUpdateTransferOutDraftBySource(ArgumentMatchers.<ErpStockMoveSaveReqVO>argThat(req ->
                cart.getDeptId().equals(req.getDeptId())
                        && cart.getId().equals(req.getSourceId())
                        && req.getItems().size() == 1
                        && item.getWarehouseId().equals(req.getItems().get(0).getFromWarehouseId())));
    }

    @Test
    public void testCreateCrossDeptTransferOutDraftByCartId_crossDept_createTransferOutDraft() {
        Long cartId = 273L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000018").setDeptId(99L)
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        item.setDeptId(20L);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(99L))).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("CrossDeptWarehouse")
                        .setStockBillEnabled(false)));
        when(warehouseService.resolveDirectWarehouseId(eq(99L))).thenReturn(9999L);

        saleCartService.createCrossDeptTransferOutDraftByCartId(cartId);

        assertEquals(20L, item.getDeptId());
        verify(warehouseService).validateWarehouseSaleSelectableForDept(eq(401L), eq(99L));
        verify(warehouseService).resolveDirectWarehouseId(eq(99L));
        verify(stockMoveService).syncTransferOutDraftsBySource(ArgumentMatchers.<List<ErpStockMoveSaveReqVO>>argThat(reqs -> {
            ErpStockMoveSaveReqVO req = reqs.size() == 1 ? reqs.get(0) : null;
            return req != null && cart.getDeptId().equals(req.getDeptId())
                    && Long.valueOf(20L).equals(req.getFromDeptId())
                    && Integer.valueOf(10).equals(req.getTransferDirection())
                    && ErpSaleBizSourceTypeEnum.CART.getType().equals(req.getSourceType())
                    && cart.getId().equals(req.getSourceId())
                    && cart.getNo().equals(req.getSourceNo())
                    && req.getItems().size() == 1
                    && item.getWarehouseId().equals(req.getItems().get(0).getFromWarehouseId())
                    && Long.valueOf(9999L).equals(req.getItems().get(0).getToWarehouseId())
                    && item.getProductId().equals(req.getItems().get(0).getProductId());
        }));
    }

    @Test
    public void testCreateCrossDeptTransferOutDraftByCartId_mixedWarehouses_onlyCrossDeptItem() {
        Long cartId = 274L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000019").setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO crossDeptItem = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        ErpSaleCartItemDO sameDeptItem = buildCartItem(cartId, 202L, 402L, new BigDecimal("4"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId)))
                .thenReturn(java.util.Arrays.asList(crossDeptItem, sameDeptItem));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(10L))).thenReturn(java.util.Arrays.asList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setStockBillEnabled(false),
                new ErpWarehouseDO().setId(402L).setDeptId(10L).setStockBillEnabled(false)));
        when(warehouseService.isWarehouseSaleAllowedForDept(eq(401L), eq(10L))).thenReturn(false);
        when(warehouseService.isWarehouseSaleAllowedForDept(eq(402L), eq(10L))).thenReturn(true);
        when(warehouseService.resolveDirectWarehouseId(eq(10L))).thenReturn(1010L);

        saleCartService.createCrossDeptTransferOutDraftByCartId(cartId);

        verify(warehouseService).resolveDirectWarehouseId(eq(10L));
        verify(stockMoveService).syncTransferOutDraftsBySource(
                ArgumentMatchers.<List<ErpStockMoveSaveReqVO>>argThat(reqs -> reqs.size() == 1
                        && reqs.get(0).getItems().size() == 1
                        && crossDeptItem.getProductId().equals(reqs.get(0).getItems().get(0).getProductId())
                        && crossDeptItem.getWarehouseId().equals(reqs.get(0).getItems().get(0).getFromWarehouseId())
                        && Long.valueOf(1010L).equals(reqs.get(0).getItems().get(0).getToWarehouseId())));
    }

    @Test
    public void testCreateCrossDeptTransferOutDraftByCartId_multipleSourceDepts_createsOneDraftPerDept() {
        Long cartId = 275L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000020").setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO dept20Item1 = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        ErpSaleCartItemDO dept30Item = buildCartItem(cartId, 202L, 402L, new BigDecimal("4"));
        ErpSaleCartItemDO dept20Item2 = buildCartItem(cartId, 203L, 403L, new BigDecimal("5"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId)))
                .thenReturn(java.util.Arrays.asList(dept20Item1, dept30Item, dept20Item2));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(10L))).thenReturn(java.util.Arrays.asList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setStockBillEnabled(false),
                new ErpWarehouseDO().setId(402L).setDeptId(30L).setStockBillEnabled(false),
                new ErpWarehouseDO().setId(403L).setDeptId(20L).setStockBillEnabled(false)));
        when(warehouseService.isWarehouseSaleAllowedForDept(eq(401L), eq(10L))).thenReturn(false);
        when(warehouseService.isWarehouseSaleAllowedForDept(eq(402L), eq(10L))).thenReturn(false);
        when(warehouseService.isWarehouseSaleAllowedForDept(eq(403L), eq(10L))).thenReturn(false);

        saleCartService.createCrossDeptTransferOutDraftByCartId(cartId);

        verify(stockMoveService).syncTransferOutDraftsBySource(
                ArgumentMatchers.<List<ErpStockMoveSaveReqVO>>argThat(reqs -> reqs.size() == 2
                        && reqs.stream().anyMatch(req -> Long.valueOf(20L).equals(req.getFromDeptId())
                                && req.getItems().size() == 2)
                        && reqs.stream().anyMatch(req -> Long.valueOf(30L).equals(req.getFromDeptId())
                                && req.getItems().size() == 1
                                && Long.valueOf(402L).equals(req.getItems().get(0).getFromWarehouseId()))));
    }

    @Test
    public void testSubmitSaleCart_stockInsufficient_throwExceptionAndKeepProcessStatus() {
        Long cartId = 77L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000009")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        List<ErpSaleCartItemDO> items = new ArrayList<>();
        items.add(buildCartItem(cartId, 201L, 401L, new BigDecimal("5")));
        items.add(buildCartItem(cartId, 202L, 402L, new BigDecimal("6")));
        items.add(buildCartItem(cartId, 203L, 403L, new BigDecimal("7")));
        items.add(buildCartItem(cartId, 204L, 404L, new BigDecimal("8")));
        Map<Long, ErpProductRespVO> productMap = new LinkedHashMap<>();
        productMap.put(201L, new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1"));
        productMap.put(202L, new ErpProductRespVO().setId(202L).setCode("P002").setName("Product2"));
        productMap.put(203L, new ErpProductRespVO().setId(203L).setCode("P003").setName("Product3"));
        productMap.put(204L, new ErpProductRespVO().setId(204L).setCode("P004").setName("Product4"));
        Map<Long, ErpWarehouseDO> warehouseMap = new LinkedHashMap<>();
        warehouseMap.put(401L, new ErpWarehouseDO().setId(401L).setName("Warehouse1"));
        warehouseMap.put(402L, new ErpWarehouseDO().setId(402L).setName("Warehouse2"));
        warehouseMap.put(403L, new ErpWarehouseDO().setId(403L).setName("Warehouse3"));
        warehouseMap.put(404L, new ErpWarehouseDO().setId(404L).setName("Warehouse4"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(items);
        when(productService.getProductVOMap(anyCollection())).thenReturn(productMap);
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(new ArrayList<>(warehouseMap.values()));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(warehouseMap);
        when(stockService.getStock(eq(201L), eq(401L))).thenReturn(new ErpStockDO().setCount(new BigDecimal("2")));
        when(stockService.getStock(eq(202L), eq(402L))).thenReturn(new ErpStockDO().setCount(new BigDecimal("3")));
        when(stockService.getStock(eq(203L), eq(403L))).thenReturn(new ErpStockDO().setCount(new BigDecimal("4")));
        when(stockService.getStock(eq(204L), eq(404L))).thenReturn(new ErpStockDO().setCount(new BigDecimal("5")));

        ServiceException exception = assertThrows(ServiceException.class, () -> saleCartService.submitSaleCart(cartId));

        assertEquals(STOCK_COUNT_NEGATIVE2.getCode(), exception.getCode());
        assertTrue(exception.getMessage().length() > 0);
        assertTrue(exception.getMessage().contains("Product1 / Warehouse1"));
        assertTrue(exception.getMessage().contains("Product3 / Warehouse3"));
        assertTrue(exception.getMessage().contains("Product1 / Warehouse1"));
        verify(saleCartMapper, never()).updateByIdAndStatus(eq(cartId), any(), any());
    }

    @Test
    public void testSubmitSaleCart_crossDeptStockCheckIgnoreDataPermission_success() {
        Long cartId = 277L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000018")
                .setDeptId(99L).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("1"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(201L,
                new ErpProductRespVO().setId(201L).setCode("P001").setName("Product1")));
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setName("Warehouse1").setStockBillEnabled(false)));
        when(stockService.getStock(eq(201L), eq(401L))).thenAnswer(invocation -> {
            if (DataPermissionContextHolder.get() == null || DataPermissionContextHolder.get().enable()) {
                return null;
            }
            return new ErpStockDO().setCount(new BigDecimal("2"));
        });
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.PROCESS.getStatus()), any(ErpSaleCartDO.class))).thenReturn(1);

        ErpSaleCartSubmitRespVO result = saleCartService.submitSaleCart(cartId);

        assertEquals(ErpSaleCartStatusEnum.SUBMITTED.getStatus(), result.getStatus());
        verify(stockMoveService, never()).createOrUpdateTransferOutDraftBySource(ArgumentMatchers.<ErpStockMoveSaveReqVO>argThat(req ->
                cart.getDeptId().equals(req.getDeptId())
                        && cart.getId().equals(req.getSourceId())
                        && req.getItems().size() == 1
                        && item.getWarehouseId().equals(req.getItems().get(0).getFromWarehouseId())));
    }

    @Test
    public void testFirstApproveSaleCart_firstApproveToFinalApprove_success() {
        // firstApproveSaleCart ???????????????UBMITTED -> FIRST_APPROVE
        Long cartId = 72L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000004").setDeptId(99L)
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        item.setDeptId(20L);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setStockBillEnabled(false)));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(99L))).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setStockBillEnabled(false)));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())
                        && update.getFirstAuditTime() != null)))
                .thenReturn(1);

        saleCartService.firstApproveSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())));
        verify(stockMoveService).syncTransferOutDraftsBySource(
                ArgumentMatchers.<List<ErpStockMoveSaveReqVO>>argThat(reqs -> reqs.size() == 1
                        && Integer.valueOf(10).equals(reqs.get(0).getTransferDirection())
                        && cartId.equals(reqs.get(0).getSourceId())
                        && item.getWarehouseId().equals(reqs.get(0).getItems().get(0).getFromWarehouseId())
                        && Long.valueOf(888L).equals(reqs.get(0).getItems().get(0).getToWarehouseId())));
    }

    @Test
    public void testFirstApproveSaleCart_sameDept_staysFirstApproveWithoutTransferOutDraft() {
        Long cartId = 73L;
        ErpSaleCartDO submittedCart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000005").setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        ErpSaleCartDO firstApprovedCart = new ErpSaleCartDO().setId(cartId).setNo(submittedCart.getNo()).setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        item.setDeptId(10L);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(submittedCart, firstApprovedCart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(10L).setStockBillEnabled(false)));

        saleCartService.firstApproveSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())
                        && update.getFirstAuditTime() != null));
        verify(saleCartMapper, never()).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()), any(ErpSaleCartDO.class));
        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any(), any());
        verify(stockMoveService, never()).createOrUpdateTransferOutDraftBySource(any());
        verify(warehouseService, never()).resolveDirectWarehouseId(any());
    }

    @Test
    public void testFirstApproveSaleCart_directWarehouse_staysFirstApproveWithoutSameWarehouseTransfer() {
        Long cartId = 74L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000006").setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 888L, new BigDecimal("3"));
        item.setDeptId(10L);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(888L).setDeptId(10L).setStockBillEnabled(false)));

        saleCartService.firstApproveSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())));
        verify(stockMoveService, never()).createOrUpdateTransferOutDraftBySource(any());
        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any(), any());
    }

    @Test
    public void testCancelFirstApproveSaleCart_deletesTransferOutDraftAndUnlocksStock() {
        Long cartId = 173L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000022")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartMapper.cancelFirstApprove(eq(cartId))).thenReturn(1);

        saleCartService.cancelFirstApproveSaleCart(cartId);

        verify(stockMoveService).deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(stockLockService).unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(saleCartMapper).cancelFirstApprove(cartId);
    }

    @Test
    public void testCancelFirstApproveSaleCart_invalidStatus_rejectsBeforeSideEffects() {
        Long cartId = 174L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000023")
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);

        assertServiceException(() -> saleCartService.cancelFirstApproveSaleCart(cartId),
                SALE_CART_CANCEL_FIRST_APPROVE_FAIL);

        verify(stockMoveService, never()).deleteUnapprovedTransferOutBySource(any(), any());
        verify(stockLockService, never()).unlockStock(any(), any());
        verify(saleCartMapper, never()).cancelFirstApprove(any());
    }

    @Test
    public void testCancelFirstApproveSaleCart_statusChanged_rollsBackWithDedicatedError() {
        Long cartId = 175L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000024")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartMapper.cancelFirstApprove(eq(cartId))).thenReturn(0);

        assertServiceException(() -> saleCartService.cancelFirstApproveSaleCart(cartId), SALE_CART_STATUS_CHANGED);

        verify(stockMoveService).deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(stockLockService).unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(saleCartMapper).cancelFirstApprove(cartId);
    }

    @Test
    public void testCancelFirstApproveSaleCart_unlockStockFails_doesNotUpdateCartStatus() {
        Long cartId = 176L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000025")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        doThrow(new IllegalStateException("????????????????????????")).when(stockLockService)
                .unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);

        assertThrows(IllegalStateException.class, () -> saleCartService.cancelFirstApproveSaleCart(cartId));

        verify(stockMoveService).deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(saleCartMapper, never()).cancelFirstApprove(any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_success() {
        Long transferOutId = 301L;
        Long cartId = 177L;
        ErpStockMoveDO transferOut = buildUnlockTransferOut(transferOutId, cartId);
        ErpStockMoveItemDO item = new ErpStockMoveItemDO().setMoveId(transferOutId);
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260716000177")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(stockMoveService.getStockMoveForUpdate(transferOutId)).thenReturn(transferOut);
        when(stockMoveService.getStockMoveItemListByMoveId(transferOutId))
                .thenReturn(Collections.singletonList(item));
        when(stockMoveService.getUnlockCartPermission(transferOut, Collections.singletonList(item)))
                .thenReturn(ErpStockMoveOperationPermission.allowed());
        when(saleCartMapper.selectById(cartId)).thenReturn(cart);
        when(saleCartMapper.cancelFirstApprove(cartId)).thenReturn(1);

        saleCartService.unlockSaleCartByTransferOutId(transferOutId);

        verify(stockMoveService).deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(stockLockService).unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(saleCartMapper).cancelFirstApprove(cartId);
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_cartStatusCasFails_rollsBackWithDedicatedError() {
        Long transferOutId = 311L;
        Long cartId = 186L;
        ErpStockMoveDO transferOut = buildUnlockTransferOut(transferOutId, cartId);
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260716000186")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(stockMoveService.getStockMoveForUpdate(transferOutId)).thenReturn(transferOut);
        when(stockMoveService.getStockMoveItemListByMoveId(transferOutId)).thenReturn(Collections.emptyList());
        when(stockMoveService.getUnlockCartPermission(transferOut, Collections.emptyList()))
                .thenReturn(ErpStockMoveOperationPermission.allowed());
        when(saleCartMapper.selectById(cartId)).thenReturn(cart);
        when(saleCartMapper.cancelFirstApprove(cartId)).thenReturn(0);

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(transferOutId),
                SALE_CART_STATUS_CHANGED);

        verify(stockMoveService).deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(stockLockService).unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(saleCartMapper).cancelFirstApprove(cartId);
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_notVisibleOrMissing_rejected() {
        when(stockMoveService.getStockMoveForUpdate(302L)).thenReturn(null);

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(302L),
                STOCK_MOVE_NOT_EXISTS);

        verify(stockMoveService, never()).deleteUnapprovedTransferOutBySource(any(), any());
        verify(stockLockService, never()).unlockStock(any(), any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_transferIn_rejected() {
        ErpStockMoveDO transferIn = buildUnlockTransferOut(303L, 178L).setTransferDirection(20);
        when(stockMoveService.getStockMoveForUpdate(303L)).thenReturn(transferIn);

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(303L),
                STOCK_MOVE_UNLOCK_NOT_TRANSFER_OUT);

        verify(stockMoveService, never()).getStockMoveItemListByMoveId(any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_approved_rejected() {
        ErpStockMoveDO approved = buildUnlockTransferOut(304L, 179L)
                .setStatus(20);
        when(stockMoveService.getStockMoveForUpdate(304L)).thenReturn(approved);

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(304L),
                STOCK_MOVE_UNLOCK_APPROVED);

        verify(stockMoveService, never()).getStockMoveItemListByMoveId(any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_nonCartSource_rejected() {
        ErpStockMoveDO manual = buildUnlockTransferOut(305L, 180L).setSourceType(null);
        when(stockMoveService.getStockMoveForUpdate(305L)).thenReturn(manual);

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(305L),
                STOCK_MOVE_UNLOCK_NOT_CART_SOURCE);

        verify(saleCartMapper, never()).selectById(any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_missingSourceId_rejected() {
        ErpStockMoveDO transferOut = buildUnlockTransferOut(306L, 181L).setSourceId(null);
        when(stockMoveService.getStockMoveForUpdate(306L)).thenReturn(transferOut);

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(306L),
                STOCK_MOVE_UNLOCK_SOURCE_ID_MISSING);

        verify(saleCartMapper, never()).selectById(any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_crossDeptDenied_rejected() {
        ErpStockMoveDO transferOut = buildUnlockTransferOut(307L, 182L);
        when(stockMoveService.getStockMoveForUpdate(307L)).thenReturn(transferOut);
        when(stockMoveService.getStockMoveItemListByMoveId(307L)).thenReturn(Collections.emptyList());
        when(stockMoveService.getUnlockCartPermission(transferOut, Collections.emptyList()))
                .thenReturn(ErpStockMoveOperationPermission.denied("denied"));

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(307L),
                STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED);

        verify(saleCartMapper, never()).selectById(any());
        verify(stockMoveService, never()).deleteUnapprovedTransferOutBySource(any(), any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_siblingDeptDenied_rejected() {
        Long cartId = 187L;
        ErpStockMoveDO transferOut = buildUnlockTransferOut(312L, cartId);
        ErpStockMoveDO siblingTransferOut = buildUnlockTransferOut(313L, cartId);
        ErpStockMoveItemDO transferOutItem = new ErpStockMoveItemDO().setMoveId(312L);
        ErpStockMoveItemDO siblingItem = new ErpStockMoveItemDO().setMoveId(313L);
        when(stockMoveService.getStockMoveForUpdate(312L)).thenReturn(transferOut);
        when(stockMoveService.getStockMoveItemListByMoveId(312L))
                .thenReturn(Collections.singletonList(transferOutItem));
        when(stockMoveService.getUnlockCartPermission(transferOut, Collections.singletonList(transferOutItem)))
                .thenReturn(ErpStockMoveOperationPermission.allowed());
        when(saleCartMapper.selectById(cartId)).thenReturn(new ErpSaleCartDO().setId(cartId)
                .setNo("ST20260716000187").setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()));
        when(stockMoveService.getTransferOutListBySource(ErpSaleBizSourceTypeEnum.CART.getType(), cartId))
                .thenReturn(java.util.Arrays.asList(transferOut, siblingTransferOut));
        when(stockMoveService.getStockMoveItemListByMoveId(313L)).thenReturn(Collections.singletonList(siblingItem));
        when(stockMoveService.getUnlockCartPermission(siblingTransferOut, Collections.singletonList(siblingItem)))
                .thenReturn(ErpStockMoveOperationPermission.denied("denied"));

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(312L),
                STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED);

        verify(stockMoveService, never()).deleteUnapprovedTransferOutBySource(any(), any());
        verify(stockLockService, never()).unlockStock(any(), any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_cartStatusChanged_rejectedBeforeDelete() {
        Long cartId = 183L;
        ErpStockMoveDO transferOut = buildUnlockTransferOut(308L, cartId);
        when(stockMoveService.getStockMoveForUpdate(308L)).thenReturn(transferOut);
        when(stockMoveService.getStockMoveItemListByMoveId(308L)).thenReturn(Collections.emptyList());
        when(stockMoveService.getUnlockCartPermission(transferOut, Collections.emptyList()))
                .thenReturn(ErpStockMoveOperationPermission.allowed());
        when(saleCartMapper.selectById(cartId)).thenReturn(new ErpSaleCartDO().setId(cartId)
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus()));

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(308L),
                SALE_CART_UNLOCK_STATUS_INVALID);

        verify(stockMoveService, never()).deleteUnapprovedTransferOutBySource(any(), any());
        verify(stockLockService, never()).unlockStock(any(), any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_sourceCartNotVisibleOrMissing_rejectedBeforeDelete() {
        Long cartId = 185L;
        ErpStockMoveDO transferOut = buildUnlockTransferOut(310L, cartId);
        when(stockMoveService.getStockMoveForUpdate(310L)).thenReturn(transferOut);
        when(stockMoveService.getStockMoveItemListByMoveId(310L)).thenReturn(Collections.emptyList());
        when(stockMoveService.getUnlockCartPermission(transferOut, Collections.emptyList()))
                .thenReturn(ErpStockMoveOperationPermission.allowed());
        when(saleCartMapper.selectById(cartId)).thenReturn(null);

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(310L), SALE_CART_NOT_EXISTS);

        verify(stockMoveService, never()).deleteUnapprovedTransferOutBySource(any(), any());
        verify(stockLockService, never()).unlockStock(any(), any());
    }

    @Test
    public void testUnlockSaleCartByTransferOutId_approvedSourceExists_rejectedBeforeDelete() {
        Long cartId = 184L;
        ErpStockMoveDO transferOut = buildUnlockTransferOut(309L, cartId);
        when(stockMoveService.getStockMoveForUpdate(309L)).thenReturn(transferOut);
        when(stockMoveService.getStockMoveItemListByMoveId(309L)).thenReturn(Collections.emptyList());
        when(stockMoveService.getUnlockCartPermission(transferOut, Collections.emptyList()))
                .thenReturn(ErpStockMoveOperationPermission.allowed());
        when(saleCartMapper.selectById(cartId)).thenReturn(new ErpSaleCartDO().setId(cartId)
                .setNo("ST20260716000184")
                .setStatus(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()));
        ErpStockMoveDO approvedTransferOut = buildUnlockTransferOut(399L, cartId)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(stockMoveService.getTransferOutListBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId))
                .thenReturn(java.util.Arrays.asList(transferOut, approvedTransferOut));

        assertServiceException(() -> saleCartService.unlockSaleCartByTransferOutId(309L),
                STOCK_MOVE_UNLOCK_SOURCE_APPROVED_EXISTS);

        verify(stockMoveService, never()).deleteUnapprovedTransferOutBySource(any(), any());
        verify(stockLockService, never()).unlockStock(any(), any());
    }

    @Test
    public void testFirstApproveSaleCart_disabled_throwException() {
        mockFirstApproveConfig(false, false, true);

        assertServiceException(() -> saleCartService.firstApproveSaleCart(72L), SALE_CART_FIRST_APPROVE_DISABLED);

        verify(saleCartMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    public void testFirstApproveSaleCart_operatorDeptNotRestricted_success() {
        Long cartId = 172L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000014")
                .setDeptId(10L)
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        mockFirstApproveConfig(true, true, true);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(Collections.singletonList(new ErpSaleConfigDO().setDeptId(10L)));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        item.setDeptId(20L);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(401L).setDeptId(20L).setStockBillEnabled(false)));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()), any(ErpSaleCartDO.class))).thenReturn(1);

        saleCartService.firstApproveSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testFirstApproveSaleCart_availableStockInsufficient_throwException() {
        Long cartId = 173L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000023")
                .setDeptId(10L).setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockFirstApproveConfig(true, true, true);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(Collections.singletonList(new ErpSaleConfigDO().setDeptId(10L)));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(stockService.getOccupiedCountMap(anyCollection(), anyCollection()))
                .thenReturn(Collections.singletonMap("201_401", new BigDecimal("11")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> saleCartService.firstApproveSaleCart(cartId));

        assertEquals(STOCK_COUNT_NEGATIVE2.getCode(), exception.getCode());
        verify(stockLockService, never()).lockStock(any(), any(), any(), any(), any(), any(), any());
        verify(saleCartMapper, never()).updateByIdAndStatus(eq(cartId), any(), any());
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
        // ??????????????????????????????????????????GENERATED_SALE_OUT?????????????????????
        Long cartId = 74L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("ST20260509000006")
                .setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);

        assertServiceException(() -> saleCartService.rejectSaleCart(cartId), SALE_CART_REJECT_FAIL);
        verify(saleCartMapper, never()).updateByIdAndStatus(eq(cartId), any(), any());
    }

    // ==================== convertToQuote ?????????====================

    @Test
    public void testConvertToQuote_finalApprovedCart_throwException() {
        // ???????????????????????????????????????????????????????????????????????????????
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

    // ==================== finalApproveSaleCart ?????????====================

    @Test
    public void testFinalApproveSaleCart_optimisticLockFail_throwException() {
        // ???????????????????????????0 ????????????????????????????????????????????????????????????????????????
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
                .setId(101L).setCartId(cartId).setProductId(201L).setWarehouseId(301L).setDeptId(10L)
                .setProductPrice(new BigDecimal("15.00")).setCount(new BigDecimal("3"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        // updateByIdAndStatus ?????????0???????????????????????????
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()), any(ErpSaleCartDO.class)))
                .thenReturn(0);

        assertServiceException(() -> saleCartService.finalApproveSaleCart(cartId), SALE_CART_FINAL_APPROVE_FAIL);
    }

    @Test
    public void testFinalApproveSaleCart_allowSubmittedWhenFirstApproveDisabled() {
        Long cartId = 176L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000015")
                .setCustomerId(21L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(101L).setCartId(cartId).setProductId(201L).setWarehouseId(301L)
                .setProductPrice(new BigDecimal("15.00")).setCount(new BigDecimal("3"));
        mockFirstApproveConfig(false, false, true);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockService.getStock(eq(201L), eq(301L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(301L,
                new ErpWarehouseDO().setId(301L).setStockBillEnabled(false)));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()), any(ErpSaleCartDO.class)))
                .thenReturn(1);
        saleCartService.finalApproveSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testFinalApproveSaleCart_availableStockInsufficient_throwException() {
        Long cartId = 179L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("10")));
        when(stockService.getOccupiedCountMap(anyCollection(), anyCollection()))
                .thenReturn(Collections.singletonMap("201_401", new BigDecimal("8")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> saleCartService.finalApproveSaleCart(cartId));

        assertEquals(STOCK_COUNT_NEGATIVE2.getCode(), exception.getCode());
        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any(), any());
    }

    @Test
    public void testFinalApproveSaleCart_allowSubmittedWhenDeptNotRequireFirstApprove() {
        Long cartId = 177L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000021")
                .setCustomerId(21L)
                .setDeptId(99L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO()
                .setId(101L).setCartId(cartId).setProductId(201L).setWarehouseId(301L)
                .setProductPrice(new BigDecimal("15.00")).setCount(new BigDecimal("3"));
        mockFirstApproveConfig(true, true, false);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(Collections.singletonList(new ErpSaleConfigDO().setDeptId(10L)));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockService.getStock(eq(201L), eq(301L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()), any(ErpSaleCartDO.class)))
                .thenReturn(1);
        saleCartService.finalApproveSaleCart(cartId);

        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.SUBMITTED.getStatus()),
                argThat(update -> ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus().equals(update.getStatus())
                        && update.getFirstAuditTime() == null && update.getFirstAuditUserId() == null));
    }

    @Test
    public void testFinalApproveSaleCart_crossDeptUnapprovedStockMove_throwException() {
        Long cartId = 276L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockCrossDeptFinalApproveContext(cart, item);
        when(stockMoveService.hasUnapprovedTransferOutBySource(eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId)))
                .thenReturn(true);

        assertServiceException(() -> saleCartService.finalApproveSaleCart(cartId), SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);

        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any(), any());
    }

    @Test
    public void testFinalApproveSaleCart_sameDeptUnapprovedLegacyStockMove_ignored() {
        Long cartId = 281L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .setDeptId(10L);
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        item.setDeptId(10L);
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(Collections.singletonList(item));
        when(stockMoveService.hasUnapprovedTransferOutBySource(
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId))).thenReturn(true);
        saleCartService.finalApproveSaleCart(cartId);

        verify(saleOutService).createGeneratedSaleOut(argThat(req ->
                        req.getItems().size() == 1
                                && item.getWarehouseId().equals(req.getItems().get(0).getWarehouseId())),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId), eq(cart.getNo()), eq(false));
        verify(stockMoveService).deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
    }

    @Test
    public void testFinalApproveSaleCart_crossDeptWithoutApprovedStockMove_throwException() {
        Long cartId = 277L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockCrossDeptFinalApproveContext(cart, item);
        when(stockMoveService.hasUnapprovedTransferOutBySource(eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId)))
                .thenReturn(false);

        assertServiceException(() -> saleCartService.finalApproveSaleCart(cartId), SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);

        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any(), any());
    }

    @Test
    public void testFinalApproveSaleCart_crossDeptApprovedStockMove_success() {
        Long cartId = 278L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockCrossDeptFinalApproveContext(cart, item);
        when(stockMoveService.hasUnapprovedTransferOutBySource(eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId)))
                .thenReturn(false);
        mockApprovedTransferOut(cartId, item, 888L, item.getCount());
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()), any(ErpSaleCartDO.class))).thenReturn(1);

        saleCartService.finalApproveSaleCart(cartId);

        verify(saleOutService).createGeneratedSaleOut(argThat(req ->
                        req.getItems().size() == 1
                                && Long.valueOf(888L).equals(req.getItems().get(0).getWarehouseId())
                                && Long.valueOf(401L).equals(req.getItems().get(0).getSourceWarehouseId())
                                && Long.valueOf(20L).equals(req.getItems().get(0).getSourceDeptId())),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId), eq(cart.getNo()), eq(false));
    }

    @Test
    public void testFinalApproveSaleCart_multipleApprovedTransfers_aggregatesAllItems() {
        Long cartId = 284L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item1 = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        ErpSaleCartItemDO item2 = buildCartItem(cartId, 202L, 402L, new BigDecimal("4"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId))).thenReturn(java.util.Arrays.asList(item1, item2));
        Map<Long, ErpProductRespVO> productMap = new LinkedHashMap<>();
        productMap.put(201L, new ErpProductRespVO().setId(201L).setCode("P001").setName("??????1"));
        productMap.put(202L, new ErpProductRespVO().setId(202L).setCode("P002").setName("??????2"));
        lenient().when(productService.getProductVOMap(anyCollection())).thenReturn(productMap);
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(cart.getDeptId())))
                .thenAnswer(invocation -> {
                    Collection<Long> warehouseIds = invocation.getArgument(0);
                    List<ErpWarehouseDO> warehouses = new ArrayList<>();
                    for (Long warehouseId : warehouseIds) {
                        Long deptId = warehouseId >= 888L ? cart.getDeptId()
                                : (Objects.equals(warehouseId, 401L) ? 20L : 30L);
                        warehouses.add(new ErpWarehouseDO().setId(warehouseId).setDeptId(deptId)
                                .setStockBillEnabled(false));
                    }
                    return warehouses;
                });
        lenient().when(stockService.getStock(eq(201L), eq(401L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        lenient().when(stockService.getStock(eq(202L), eq(402L)))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
        ErpStockMoveDO transfer1 = new ErpStockMoveDO().setId(1001L).setStatus(ErpAuditStatus.APPROVE.getStatus());
        ErpStockMoveDO transfer2 = new ErpStockMoveDO().setId(1002L).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(stockMoveService.getTransferOutListBySource(ErpSaleBizSourceTypeEnum.CART.getType(), cartId))
                .thenReturn(java.util.Arrays.asList(transfer1, transfer2));
        when(stockMoveService.getStockMoveItemListByMoveId(1001L)).thenReturn(Collections.singletonList(
                new ErpStockMoveItemDO().setMoveId(1001L).setProductId(201L).setFromWarehouseId(401L)
                        .setToWarehouseId(888L).setCount(new BigDecimal("3"))));
        when(stockMoveService.getStockMoveItemListByMoveId(1002L)).thenReturn(Collections.singletonList(
                new ErpStockMoveItemDO().setMoveId(1002L).setProductId(202L).setFromWarehouseId(402L)
                        .setToWarehouseId(889L).setCount(new BigDecimal("4"))));
        when(saleCartMapper.updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()), any(ErpSaleCartDO.class))).thenReturn(1);

        saleCartService.finalApproveSaleCart(cartId);

        verify(saleOutService).createGeneratedSaleOut(argThat(req -> req.getItems().size() == 2
                        && req.getItems().stream().anyMatch(item -> Long.valueOf(201L).equals(item.getProductId())
                                && Long.valueOf(888L).equals(item.getWarehouseId())
                                && Long.valueOf(401L).equals(item.getSourceWarehouseId())
                                && Long.valueOf(20L).equals(item.getSourceDeptId()))
                        && req.getItems().stream().anyMatch(item -> Long.valueOf(202L).equals(item.getProductId())
                                && Long.valueOf(889L).equals(item.getWarehouseId())
                                && Long.valueOf(402L).equals(item.getSourceWarehouseId())
                                && Long.valueOf(30L).equals(item.getSourceDeptId()))),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId), eq(cart.getNo()), eq(false));
    }

    @Test
    public void testFinalApproveSaleCart_crossDeptApprovedCountMismatch_throwException() {
        Long cartId = 282L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockCrossDeptFinalApproveContext(cart, item);
        mockApprovedTransferOut(cartId, item, 888L, new BigDecimal("2"));

        assertServiceException(() -> saleCartService.finalApproveSaleCart(cartId),
                SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);

        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any(), any());
    }

    @Test
    public void testFinalApproveSaleCart_mixedWarehouses_crossUsesTargetAndSameDeptKeepsSource() {
        Long cartId = 283L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO crossDeptItem = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        ErpSaleCartItemDO sameDeptItem = buildCartItem(cartId, 202L, 402L, new BigDecimal("4"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId)))
                .thenReturn(java.util.Arrays.asList(crossDeptItem, sameDeptItem));
        when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(cart.getDeptId())))
                .thenAnswer(invocation -> {
                    Collection<Long> warehouseIds = invocation.getArgument(0);
                    List<ErpWarehouseDO> warehouses = new ArrayList<>();
                    for (Long warehouseId : warehouseIds) {
                        Long deptId = Objects.equals(warehouseId, crossDeptItem.getWarehouseId()) ? 20L : 10L;
                        warehouses.add(new ErpWarehouseDO().setId(warehouseId).setDeptId(deptId)
                                .setStockBillEnabled(false));
                    }
                    return warehouses;
                });
        mockApprovedTransferOut(cartId, crossDeptItem, 888L, crossDeptItem.getCount());

        saleCartService.finalApproveSaleCart(cartId);

        verify(saleOutService).createGeneratedSaleOut(argThat(req -> req.getItems().size() == 2
                        && req.getItems().stream().anyMatch(item -> Long.valueOf(201L).equals(item.getProductId())
                                && Long.valueOf(888L).equals(item.getWarehouseId())
                                && Long.valueOf(401L).equals(item.getSourceWarehouseId())
                                && Long.valueOf(20L).equals(item.getSourceDeptId()))
                        && req.getItems().stream().anyMatch(item -> Long.valueOf(202L).equals(item.getProductId())
                                && Long.valueOf(402L).equals(item.getWarehouseId())
                                && item.getSourceWarehouseId() == null
                                && item.getSourceDeptId() == null)),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId), eq(cart.getNo()), eq(false));
    }

    @Test
    public void testAutoFinalApproveAfterTransferOut_unapprovedTransferOutExists_noFinalApprove() {
        Long cartId = 279L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        when(saleCartMapper.selectByIdForUpdate(eq(cartId))).thenReturn(cart);
        when(stockMoveService.hasUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId)).thenReturn(true);

        saleCartService.autoFinalApproveAfterTransferOut(cartId);

        verify(stockMoveService).hasUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        verify(stockMoveService, never()).hasApprovedTransferOutBySource(any(), any());
        verify(saleOutService, never()).createGeneratedSaleOut(any(), any(), any(), any(), any());
    }

    @Test
    public void testAutoFinalApproveAfterTransferOut_allTransferOutApproved_finalApprovesOnce() {
        Long cartId = 280L;
        TenantContextHolder.setTenantId(1L);
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockCrossDeptFinalApproveContext(cart, item);
        when(saleCartMapper.selectByIdForUpdate(eq(cartId))).thenAnswer(invocation -> {
            assertNotNull(DataPermissionContextHolder.get());
            assertFalse(DataPermissionContextHolder.get().enable());
            assertEquals(Long.valueOf(1L), TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            return cart;
        });
        when(stockMoveService.hasUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId)).thenReturn(false);
        when(stockMoveService.hasApprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId)).thenReturn(true);
        mockApprovedTransferOut(cartId, item, 888L, item.getCount());
        when(saleOutService.createGeneratedSaleOut(any(), any(), any(), any(), anyBoolean()))
                .thenAnswer(invocation -> {
                    assertNotNull(DataPermissionContextHolder.get());
                    assertFalse(DataPermissionContextHolder.get().enable());
                    return 3001L;
                });

        saleCartService.autoFinalApproveAfterTransferOut(cartId, 900L);

        assertNull(DataPermissionContextHolder.get());
        assertEquals(Long.valueOf(1L), TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
        verify(saleOutService).createGeneratedSaleOut(any(), eq(ErpSaleBizSourceTypeEnum.CART.getType()),
                eq(cartId), eq(cart.getNo()), eq(false));
        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus()),
                argThat(update -> Long.valueOf(900L).equals(update.getFinalAuditUserId())));
    }

    @Test
    public void testAutoFinalApproveAfterTransferOut_multipleTransferOuts_finalApprovesOnLastOne() {
        Long cartId = 284L;
        ErpSaleCartDO cart = buildFinalApproveCart(cartId, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus());
        ErpSaleCartItemDO item = buildCartItem(cartId, 201L, 401L, new BigDecimal("3"));
        mockCrossDeptFinalApproveContext(cart, item);
        when(saleCartMapper.selectByIdForUpdate(eq(cartId))).thenReturn(cart);
        when(stockMoveService.hasUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId)).thenReturn(true, false);
        when(stockMoveService.hasApprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId)).thenReturn(true);
        mockApprovedTransferOut(cartId, item, 888L, item.getCount());
        when(saleOutService.createGeneratedSaleOut(any(), any(), any(), any(), anyBoolean())).thenReturn(3002L);

        saleCartService.autoFinalApproveAfterTransferOut(cartId, 901L);
        saleCartService.autoFinalApproveAfterTransferOut(cartId, 902L);

        verify(saleCartMapper, times(2)).selectByIdForUpdate(cartId);
        verify(saleOutService, times(1)).createGeneratedSaleOut(any(),
                eq(ErpSaleBizSourceTypeEnum.CART.getType()), eq(cartId), eq(cart.getNo()), eq(false));
        verify(saleCartMapper).updateByIdAndStatus(eq(cartId),
                eq(ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus()),
                argThat(update -> Long.valueOf(902L).equals(update.getFinalAuditUserId())));
    }

    @Test
    public void testAutoFinalApproveAfterTransferOut_queryFails_restoresDataPermissionContext() {
        Long cartId = 281L;
        RuntimeException queryException = new RuntimeException("query failed");
        when(saleCartMapper.selectByIdForUpdate(eq(cartId))).thenAnswer(invocation -> {
            assertNotNull(DataPermissionContextHolder.get());
            assertFalse(DataPermissionContextHolder.get().enable());
            throw queryException;
        });

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> saleCartService.autoFinalApproveAfterTransferOut(cartId, 900L));

        assertEquals(queryException, actual);
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    public void testGetFirstApproveConfig_defaultWhenMissing() {
        when(saleConfigMapper.selectByTypeAndCode(eq("SALE_CART_FIRST_APPROVE"), eq("GLOBAL"))).thenReturn(null);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT"))).thenReturn(Collections.emptyList());

        ErpSaleCartFirstApproveConfigRespVO result = saleCartService.getFirstApproveConfig();

        assertEquals(Boolean.TRUE, result.getEnabled());
        assertEquals(Boolean.FALSE, result.getDeptAuthEnabled());
        assertEquals(Boolean.TRUE, result.getIncludeChildDept());
        assertEquals(Boolean.TRUE, result.getCurrentUserAllowed());
        assertTrue(result.getDeptIds().isEmpty());
    }

    @Test
    public void testGetFirstApproveConfig_operatorDeptNotRestricted() {
        mockFirstApproveConfig(true, true, false);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(Collections.singletonList(new ErpSaleConfigDO().setDeptId(10L)));

        ErpSaleCartFirstApproveConfigRespVO result = saleCartService.getFirstApproveConfig();

        assertEquals(Boolean.TRUE, result.getCurrentUserAllowed());
    }

    @Test
    public void testGetFirstApproveRequiredMap_loadsConfigAndDeptTreeOnce() {
        mockFirstApproveConfig(true, true, true);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(Collections.singletonList(new ErpSaleConfigDO().setDeptId(10L)));
        DeptRespDTO childDept = new DeptRespDTO();
        childDept.setId(11L);
        when(deptApi.getChildDeptList(eq(10L))).thenReturn(Collections.singletonList(childDept));

        Map<Long, Boolean> result = saleCartService.getFirstApproveRequiredMap(
                java.util.Arrays.asList(10L, 11L, 12L, 10L, null));

        assertEquals(Boolean.TRUE, result.get(10L));
        assertEquals(Boolean.TRUE, result.get(11L));
        assertEquals(Boolean.FALSE, result.get(12L));
        assertEquals(Boolean.FALSE, result.get(null));
        verify(saleConfigMapper, times(1)).selectByTypeAndCode("SALE_CART_FIRST_APPROVE", "GLOBAL");
        verify(saleConfigMapper, times(1)).selectListByType("SALE_CART_FIRST_APPROVE_DEPT");
        verify(deptApi, times(1)).getChildDeptList(10L);
    }

    @Test
    public void testUpdateFirstApproveConfig_success_upsertDeptConfigs() {
        ErpSaleCartFirstApproveConfigSaveReqVO reqVO = new ErpSaleCartFirstApproveConfigSaveReqVO();
        reqVO.setEnabled(true);
        reqVO.setDeptAuthEnabled(true);
        reqVO.setIncludeChildDept(true);
        reqVO.setDeptIds(new ArrayList<>(java.util.Arrays.asList(10L, 10L, 11L)));
        ErpSaleConfigDO existingGlobal = new ErpSaleConfigDO().setId(1L);
        ErpSaleConfigDO obsoleteDept = new ErpSaleConfigDO().setId(2L).setDeptId(9L);
        ErpSaleConfigDO existingDept = new ErpSaleConfigDO().setId(3L).setDeptId(10L);
        when(saleConfigMapper.selectByTypeAndCode(eq("SALE_CART_FIRST_APPROVE"), eq("GLOBAL")))
                .thenReturn(existingGlobal);
        when(saleConfigMapper.selectListByType(eq("SALE_CART_FIRST_APPROVE_DEPT")))
                .thenReturn(java.util.Arrays.asList(obsoleteDept, existingDept));

        saleCartService.updateFirstApproveConfig(reqVO);

        verify(deptApi).validateDeptList(eq(java.util.Arrays.asList(10L, 11L)));
        verify(saleConfigMapper).updateById(ArgumentMatchers.<ErpSaleConfigDO>argThat(config ->
                existingGlobal.getId().equals(config.getId())
                        && CommonStatusEnum.ENABLE.getStatus().equals(config.getStatus())
                        && config.getConfigValue().contains("\"deptAuthEnabled\":true")));
        verify(saleConfigMapper).deletePhysicalByTypeAndIds(eq("SALE_CART_FIRST_APPROVE_DEPT"), eq(null),
                eq(Collections.singletonList(obsoleteDept.getId())));
        verify(saleConfigMapper).updateById(ArgumentMatchers.<ErpSaleConfigDO>argThat(config ->
                existingDept.getId().equals(config.getId())
                        && config.getDeptId().equals(10L)
                        && config.getCode().equals("DEPT_10")
                        && config.getSort().equals(0)));
        verify(saleConfigMapper).insertBatch(argThat((List<ErpSaleConfigDO> configs) ->
                configs.size() == 1 && configs.get(0).getDeptId().equals(11L)
                        && configs.get(0).getCode().equals("DEPT_11")
                        && configs.get(0).getSort().equals(1)));
    }

    @Test
    public void testUpdateFirstApproveConfig_deptAuthEnabledButDeptEmpty_throwException() {
        ErpSaleCartFirstApproveConfigSaveReqVO reqVO = new ErpSaleCartFirstApproveConfigSaveReqVO();
        reqVO.setEnabled(true);
        reqVO.setDeptAuthEnabled(true);
        reqVO.setIncludeChildDept(true);
        reqVO.setDeptIds(Collections.emptyList());

        assertServiceException(() -> saleCartService.updateFirstApproveConfig(reqVO),
                SALE_CART_FIRST_APPROVE_DEPT_EMPTY);
    }

    // ==================== ?????????????????? ====================

    @Test
    public void testUpdateFirstApproveConfig_disabledAndDeptAuthEnabled_allowEmptyDept() {
        ErpSaleCartFirstApproveConfigSaveReqVO reqVO = new ErpSaleCartFirstApproveConfigSaveReqVO();
        reqVO.setEnabled(false);
        reqVO.setDeptAuthEnabled(true);
        reqVO.setIncludeChildDept(true);
        reqVO.setDeptIds(Collections.emptyList());

        saleCartService.updateFirstApproveConfig(reqVO);

        verify(saleConfigMapper).insert(ArgumentMatchers.<ErpSaleConfigDO>argThat(config ->
                config.getConfigValue().contains("\"enabled\":false")
                        && config.getConfigValue().contains("\"deptAuthEnabled\":true")));
        verify(saleConfigMapper, never()).insertBatch(any());
    }

    @Test
    public void testBatchUpdateSaleCartItems_priceLevel_updatesSelectedPricesAndTotals() {
        Long cartId = 17L;
        ErpSaleCartDO cart = new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000017")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus())
                .setDiscountPercent(BigDecimal.ZERO)
                .setFeeAmount(BigDecimal.ZERO);
        ErpSaleCartItemDO normalItem = new ErpSaleCartItemDO()
                .setId(117L)
                .setCartId(cartId)
                .setProductId(217L)
                .setCount(new BigDecimal("2"))
                .setGiftFlag(false)
                .setProductPrice(new BigDecimal("10.00"))
                .setTotalPrice(new BigDecimal("20.00"));
        ErpSaleCartItemDO giftItem = new ErpSaleCartItemDO()
                .setId(118L)
                .setCartId(cartId)
                .setProductId(218L)
                .setCount(new BigDecimal("3"))
                .setGiftFlag(true)
                .setProductPrice(new BigDecimal("99.00"))
                .setTotalPrice(new BigDecimal("297.00"));
        ErpSaleCartItemDO untouchedItem = new ErpSaleCartItemDO()
                .setId(119L)
                .setCartId(cartId)
                .setProductId(219L)
                .setCount(BigDecimal.ONE)
                .setGiftFlag(false)
                .setProductPrice(new BigDecimal("5.00"))
                .setTotalPrice(new BigDecimal("5.00"));
        ErpSaleCartItemBatchUpdateReqVO reqVO = new ErpSaleCartItemBatchUpdateReqVO();
        reqVO.setCartId(cartId);
        List<Long> itemIds = new ArrayList<>();
        itemIds.add(normalItem.getId());
        itemIds.add(giftItem.getId());
        reqVO.setItemIds(itemIds);
        reqVO.setPriceLevel(10);
        Map<Long, BigDecimal> priceMap = new LinkedHashMap<>();
        priceMap.put(normalItem.getProductId(), new BigDecimal("12.50"));
        priceMap.put(giftItem.getProductId(), new BigDecimal("88.00"));
        when(saleCartMapper.selectById(eq(cartId))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cartId)))
                .thenReturn(List.of(normalItem, giftItem, untouchedItem));
        when(priceLevelPricePicker.pickProductPriceMap(anyCollection(), eq(10))).thenReturn(priceMap);

        saleCartService.batchUpdateSaleCartItems(reqVO);

        verify(saleCartItemMapper).updateBatch(ArgumentMatchers.<Collection<ErpSaleCartItemDO>>argThat(items -> {
            List<ErpSaleCartItemDO> list = new ArrayList<>(items);
            return list.size() == 2
                    && list.get(0).getProductPrice().compareTo(new BigDecimal("12.50")) == 0
                    && list.get(0).getTotalPrice().compareTo(new BigDecimal("25.00")) == 0
                    && list.get(1).getProductPrice().compareTo(BigDecimal.ZERO) == 0
                    && list.get(1).getTotalPrice().compareTo(BigDecimal.ZERO) == 0;
        }));
        verify(saleCartMapper).updateById(ArgumentMatchers.<ErpSaleCartDO>argThat(update ->
                update.getId().equals(cartId)
                        && update.getTotalProductPrice().compareTo(new BigDecimal("30.00")) == 0));
    }

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
        item.setDeptId(10L);
        item.setProductPrice(price);
        item.setCount(count);
        item.setTaxPercent(BigDecimal.ZERO);
        return item;
    }

    private ErpSaleCartItemDO buildCartItem(Long cartId, Long productId, Long warehouseId, BigDecimal count) {
        return new ErpSaleCartItemDO()
                .setCartId(cartId)
                .setProductId(productId)
                .setWarehouseId(warehouseId)
                .setDeptId(10L)
                .setProductPrice(new BigDecimal("15.00"))
                .setCount(count);
    }

    private ErpSaleCartDO buildFinalApproveCart(Long cartId, Integer status) {
        return new ErpSaleCartDO()
                .setId(cartId)
                .setNo("ST20260509000" + cartId)
                .setCustomerId(21L)
                .setAccountId(31L)
                .setSaleUserId(41L)
                .setDeptId(10L)
                .setCartTime(LocalDateTime.of(2026, 5, 9, 11, 0))
                .setStatus(status)
                .setDiscountPercent(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO);
    }

    private ErpStockMoveDO buildUnlockTransferOut(Long transferOutId, Long cartId) {
        return new ErpStockMoveDO().setId(transferOutId)
                .setNo("STO-" + transferOutId)
                .setTransferDirection(10)
                .setStatus(10)
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                .setSourceId(cartId);
    }

    private void mockCrossDeptFinalApproveContext(ErpSaleCartDO cart, ErpSaleCartItemDO item) {
        lenient().when(saleCartMapper.selectById(eq(cart.getId()))).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartId(eq(cart.getId()))).thenReturn(Collections.singletonList(item));
        lenient().when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(item.getProductId(),
                new ErpProductRespVO().setId(item.getProductId()).setCode("P001").setName("????????????")));
        lenient().when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), eq(cart.getDeptId())))
                .thenAnswer(invocation -> {
                    Collection<Long> warehouseIds = invocation.getArgument(0);
                    List<ErpWarehouseDO> warehouses = new ArrayList<>();
                    for (Long warehouseId : warehouseIds) {
                        warehouses.add(new ErpWarehouseDO().setId(warehouseId)
                                .setDeptId(Objects.equals(warehouseId, item.getWarehouseId()) ? 20L : cart.getDeptId())
                                .setName(Objects.equals(warehouseId, item.getWarehouseId())
                                        ? "CrossDeptWarehouse" : "DirectWarehouse")
                                .setStockBillEnabled(false));
                    }
                    return warehouses;
                });
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(item.getWarehouseId(),
                new ErpWarehouseDO().setId(item.getWarehouseId()).setDeptId(20L).setName("Warehouse1").setStockBillEnabled(false)));
        lenient().when(warehouseService.isWarehouseSaleAllowedForDept(eq(item.getWarehouseId()), eq(cart.getDeptId())))
                .thenReturn(false);
        lenient().when(stockService.getStock(eq(item.getProductId()), eq(item.getWarehouseId())))
                .thenReturn(new ErpStockDO().setCount(new BigDecimal("100")));
    }

    private void mockApprovedTransferOut(Long cartId, ErpSaleCartItemDO item, Long targetWarehouseId,
                                         BigDecimal transferCount) {
        Long transferId = 900L + cartId;
        when(stockMoveService.getTransferOutListBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cartId)).thenReturn(Collections.singletonList(
                new ErpStockMoveDO().setId(transferId).setSourceId(cartId)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(stockMoveService.getStockMoveItemListByMoveId(transferId)).thenReturn(Collections.singletonList(
                new ErpStockMoveItemDO().setMoveId(transferId).setProductId(item.getProductId())
                        .setFromWarehouseId(item.getWarehouseId()).setToWarehouseId(targetWarehouseId)
                        .setCount(transferCount)));
    }

    private void mockFirstApproveConfig(boolean enabled, boolean deptAuthEnabled, boolean includeChildDept) {
        Map<String, Boolean> config = new LinkedHashMap<>();
        config.put("enabled", enabled);
        config.put("deptAuthEnabled", deptAuthEnabled);
        config.put("includeChildDept", includeChildDept);
        when(saleConfigMapper.selectByTypeAndCode(eq("SALE_CART_FIRST_APPROVE"), eq("GLOBAL")))
                .thenReturn(new ErpSaleConfigDO().setConfigValue(JsonUtils.toJsonString(config)));
    }

}
