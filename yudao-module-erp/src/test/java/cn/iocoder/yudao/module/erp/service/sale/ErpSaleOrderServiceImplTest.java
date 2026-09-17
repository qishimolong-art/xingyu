package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ErpSaleOrderServiceImpl} Mockito 单元测试
 */
public class ErpSaleOrderServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleOrderServiceImpl saleOrderService;

    @Mock
    private ErpSaleOrderMapper saleOrderMapper;
    @Mock
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Mock
    private ErpOperateLogService operateLogService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleOrderService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
        ReflectionTestUtils.setField(saleOrderService, "productBatchNoValidator",
                new ErpProductBatchNoValidator());
        lenient().when(warehouseService.validSaleWarehouseList(anyCollection()))
                .thenAnswer(invocation -> buildWarehouseList(invocation.getArgument(0)));
        lenient().when(warehouseService.validSaleSelectableWarehouseListForDept(
                        anyCollection(), any(), any()))
                .thenAnswer(invocation -> buildWarehouseList(invocation.getArgument(0)));
        lenient().when(warehouseService.getWarehouseMap(anyCollection()))
                .thenAnswer(invocation -> {
                    Map<Long, ErpWarehouseDO> map = new HashMap<>();
                    buildWarehouseList(invocation.getArgument(0)).forEach(warehouse -> map.put(warehouse.getId(), warehouse));
                    return map;
                });
        lenient().when(stockService.getStock(anyLong(), anyLong())).thenReturn(null);
    }

    // ==================== create 类 ====================

    @Test
    public void testCreateSaleOrder_normalCase_returnId() {
        // mock 数据
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        reqVO.setAccountId(30L);
        reqVO.setSaleUserId(40L);
        reqVO.setItems(Collections.singletonList(buildItem(200L, new BigDecimal("10.00"), new BigDecimal("2"))));

        // mock 行为
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(400L).setDefaultWarehouseId(300L).setName("螺丝刀")));
        when(customerService.validateCustomerForSale(eq(20L), nullable(Long.class)))
                .thenReturn(new ErpCustomerDO().setId(20L));
        when(accountService.validateAccount(eq(30L))).thenReturn(new ErpAccountDO());
        when(saleOrderMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleOrderDO order = invocation.getArgument(0);
            order.setId(99L);
            return 1;
        }).when(saleOrderMapper).insert(any(ErpSaleOrderDO.class));

        // 调用
        Long id = saleOrderService.createSaleOrder(reqVO);

        // 断言
        assertNotNull(id);
        assertEquals(99L, id);
        // 验证依赖调用
        verify(customerService).validateCustomerForSale(eq(20L), nullable(Long.class));
        verify(accountService).validateAccount(eq(30L));
        verify(adminUserApi).validateUser(eq(40L));
        verify(saleOrderMapper).insert(argThat((ErpSaleOrderDO order) ->
                ErpAuditStatus.PROCESS.getStatus().equals(order.getStatus())
                        && reqVO.getCustomerId().equals(order.getCustomerId())
                        && order.getNo().startsWith(ErpNoRedisDAO.SALE_ORDER_NO_PREFIX)));
        verify(saleOrderItemMapper).insertBatch(argThat(items -> items.iterator().next().getOrderId().equals(99L)));
    }

    @Test
    public void testCreateSaleOrder_batchNoEnabledBlankBatchNo_success() {
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(200L, new BigDecimal("10.00"), new BigDecimal("2"))));
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(400L).setDefaultWarehouseId(300L)
                        .setBatchNoEnabled(true)));
        when(customerService.validateCustomerForSale(eq(20L), nullable(Long.class)))
                .thenReturn(new ErpCustomerDO().setId(20L));
        when(saleOrderMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleOrderDO order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        }).when(saleOrderMapper).insert(any(ErpSaleOrderDO.class));

        Long id = saleOrderService.createSaleOrder(reqVO);

        assertEquals(100L, id);
        verify(saleOrderItemMapper).insertBatch(argThat(items -> items.iterator().next().getBatchNo() == null));
    }

    @Test
    public void testCreateSaleOrder_batchNoEnabledWithBatchNo_success() {
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        ErpSaleOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("10.00"), new BigDecimal("2"));
        item.setBatchNo("BN001");
        reqVO.setItems(Collections.singletonList(item));
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(400L).setDefaultWarehouseId(300L)
                        .setBatchNoEnabled(true)));
        when(customerService.validateCustomerForSale(eq(20L), nullable(Long.class)))
                .thenReturn(new ErpCustomerDO().setId(20L));
        when(saleOrderMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleOrderDO order = invocation.getArgument(0);
            order.setId(101L);
            return 1;
        }).when(saleOrderMapper).insert(any(ErpSaleOrderDO.class));

        Long id = saleOrderService.createSaleOrder(reqVO);

        assertEquals(101L, id);
        verify(saleOrderItemMapper).insertBatch(argThat(items -> "BN001".equals(items.iterator().next().getBatchNo())));
    }

    @Test
    public void testCreateSaleOrder_batchNoDisabledWithBatchNo_throwException() {
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        ErpSaleOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("10.00"), new BigDecimal("2"));
        item.setBatchNo("BN001");
        reqVO.setItems(Collections.singletonList(item));
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(400L).setDefaultWarehouseId(300L)
                        .setBatchNoEnabled(false)));

        assertServiceException(() -> saleOrderService.createSaleOrder(reqVO), ERP_ITEM_BATCH_NO_DISABLED, 1);
        verify(saleOrderMapper, never()).insert(any(ErpSaleOrderDO.class));
        verify(saleOrderItemMapper, never()).insertBatch(anyCollection());
    }

    @Test
    public void testCreateSaleOrder_invalidCustomer_throwException() {
        // mock 数据
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(200L, new BigDecimal("10.00"), new BigDecimal("2"))));

        // mock 行为：产品 OK，但客户校验抛 CUSTOMER_NOT_EXISTS
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(400L).setDefaultWarehouseId(300L)));
        doThrow(exceptionOf(CUSTOMER_NOT_EXISTS)).when(customerService)
                .validateCustomerForSale(eq(20L), nullable(Long.class));

        // 调用 + 断言
        assertServiceException(() -> saleOrderService.createSaleOrder(reqVO), CUSTOMER_NOT_EXISTS);
        // 验证未插入
        verify(saleOrderMapper, never()).insert(any(ErpSaleOrderDO.class));
        verify(saleOrderItemMapper, never()).insertBatch(anyCollection());
    }

    @Test
    public void testCreateSaleOrder_invalidProduct_throwException() {
        // mock 数据
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(200L, new BigDecimal("10.00"), new BigDecimal("2"))));

        // mock 行为：产品校验直接抛
        doThrow(exceptionOf(PRODUCT_NOT_EXISTS)).when(productService).validProductList(anyCollection());

        // 调用 + 断言
        assertServiceException(() -> saleOrderService.createSaleOrder(reqVO), PRODUCT_NOT_EXISTS);
        // 验证未走到客户校验
        verify(customerService, never()).validateCustomerForSale(anyLong(), nullable(Long.class));
        verify(saleOrderMapper, never()).insert(any(ErpSaleOrderDO.class));
    }

    // ==================== update 类 ====================

    @Test
    public void testUpdateSaleOrder_normalCase_success() {
        // mock 数据
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setId(99L);
        reqVO.setCustomerId(20L);
        reqVO.setAccountId(30L);
        reqVO.setSaleUserId(40L);
        reqVO.setItems(Collections.singletonList(buildItem(200L, new BigDecimal("10.00"), new BigDecimal("3"))));

        // mock 行为：原订单未审核
        when(saleOrderMapper.selectById(eq(99L))).thenReturn(new ErpSaleOrderDO()
                .setId(99L).setNo("XSDD001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(400L).setDefaultWarehouseId(300L)));
        when(customerService.validateCustomerForSale(eq(20L), nullable(Long.class)))
                .thenReturn(new ErpCustomerDO().setId(20L));
        when(accountService.validateAccount(eq(30L))).thenReturn(new ErpAccountDO());
        when(saleOrderItemMapper.selectListByOrderId(eq(99L))).thenReturn(Collections.emptyList());

        // 调用
        saleOrderService.updateSaleOrder(reqVO);

        // 验证
        verify(saleOrderMapper).updateById(argThat((ErpSaleOrderDO order) -> 99L == order.getId()
                && reqVO.getCustomerId().equals(order.getCustomerId())));
        verify(saleOrderItemMapper).insertBatch(anyCollection());
    }

    @Test
    public void testUpdateSaleOrder_notExists_throwException() {
        // mock 数据
        ErpSaleOrderSaveReqVO reqVO = buildBaseReq();
        reqVO.setId(99L);
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(200L, new BigDecimal("10.00"), new BigDecimal("2"))));

        // mock 行为：订单不存在
        when(saleOrderMapper.selectById(eq(99L))).thenReturn(null);

        // 调用 + 断言
        assertServiceException(() -> saleOrderService.updateSaleOrder(reqVO), SALE_ORDER_NOT_EXISTS);
        // 验证未更新
        verify(saleOrderMapper, never()).updateById(any(ErpSaleOrderDO.class));
    }

    // ==================== status 状态机 ====================

    @Test
    public void testUpdateSaleOrderStatus_processToApprove_success() {
        // mock 数据
        Long id = 99L;
        ErpSaleOrderDO order = new ErpSaleOrderDO()
                .setId(id).setNo("XSDD001").setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setOutCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);

        // mock 行为
        when(saleOrderMapper.selectById(eq(id))).thenReturn(order);
        when(saleOrderMapper.updateByIdAndStatus(eq(id), eq(ErpAuditStatus.PROCESS.getStatus()),
                argThat(update -> ErpAuditStatus.APPROVE.getStatus().equals(update.getStatus()))))
                .thenReturn(1);

        // 调用
        saleOrderService.updateSaleOrderStatus(id, ErpAuditStatus.APPROVE.getStatus());

        // 验证
        verify(saleOrderMapper).updateByIdAndStatus(eq(id), eq(ErpAuditStatus.PROCESS.getStatus()), any());
    }

    @Test
    public void testUpdateSaleOrderStatus_alreadyApproved_throwException() {
        // mock 数据：已审核状态再次审核
        Long id = 99L;
        ErpSaleOrderDO order = new ErpSaleOrderDO()
                .setId(id).setNo("XSDD001").setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setOutCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);

        // mock 行为
        when(saleOrderMapper.selectById(eq(id))).thenReturn(order);

        // 调用 + 断言
        assertServiceException(
                () -> saleOrderService.updateSaleOrderStatus(id, ErpAuditStatus.APPROVE.getStatus()),
                SALE_ORDER_APPROVE_FAIL);
        // 验证未更新
        verify(saleOrderMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any());
    }

    @Test
    public void testUpdateSaleOrderStatus_optimisticLockFail_throwException() {
        // mock 数据
        Long id = 99L;
        ErpSaleOrderDO order = new ErpSaleOrderDO()
                .setId(id).setNo("XSDD001").setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setOutCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);

        // mock 行为：updateByIdAndStatus 返回 0 表示乐观锁失败
        when(saleOrderMapper.selectById(eq(id))).thenReturn(order);
        when(saleOrderMapper.updateByIdAndStatus(eq(id), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(0);

        // 调用 + 断言
        assertServiceException(
                () -> saleOrderService.updateSaleOrderStatus(id, ErpAuditStatus.APPROVE.getStatus()),
                SALE_ORDER_APPROVE_FAIL);
    }

    // ==================== outCount / returnCount 联动 ====================

    @Test
    public void testUpdateSaleOrderOutCount_addToExisting_accumulate() {
        // mock 数据
        Long orderId = 99L;
        ErpSaleOrderItemDO item1 = new ErpSaleOrderItemDO()
                .setId(101L).setOrderId(orderId).setProductId(200L)
                .setCount(new BigDecimal("10")).setOutCount(new BigDecimal("2"));
        ErpSaleOrderItemDO item2 = new ErpSaleOrderItemDO()
                .setId(102L).setOrderId(orderId).setProductId(201L)
                .setCount(new BigDecimal("5")).setOutCount(new BigDecimal("1"));

        // mock 行为
        when(saleOrderItemMapper.selectListByOrderId(eq(orderId))).thenReturn(Arrays.asList(item1, item2));

        // outCountMap：item1 更新到 5（< 10 合法），item2 更新到 3（< 5 合法）
        Map<Long, BigDecimal> outCountMap = new HashMap<>();
        outCountMap.put(101L, new BigDecimal("5"));
        outCountMap.put(102L, new BigDecimal("3"));

        // 调用
        saleOrderService.updateSaleOrderOutCount(orderId, outCountMap);

        // 验证：两个 item 都被更新
        verify(saleOrderItemMapper).updateById(argThat((ErpSaleOrderItemDO update) ->
                101L == update.getId() && new BigDecimal("5").compareTo(update.getOutCount()) == 0));
        verify(saleOrderItemMapper).updateById(argThat((ErpSaleOrderItemDO update) ->
                102L == update.getId() && new BigDecimal("3").compareTo(update.getOutCount()) == 0));
        // 验证主表更新 totalOutCount = 5 + 3 = 8
        verify(saleOrderMapper).updateById(argThat((ErpSaleOrderDO update) -> orderId.equals(update.getId())
                && new BigDecimal("8").compareTo(update.getOutCount()) == 0));
    }

    @Test
    public void testUpdateSaleOrderReturnCount_addToExisting_accumulate() {
        // mock 数据
        Long orderId = 99L;
        ErpSaleOrderItemDO item1 = new ErpSaleOrderItemDO()
                .setId(101L).setOrderId(orderId).setProductId(200L)
                .setCount(new BigDecimal("10")).setOutCount(new BigDecimal("8")).setReturnCount(new BigDecimal("1"));
        ErpSaleOrderItemDO item2 = new ErpSaleOrderItemDO()
                .setId(102L).setOrderId(orderId).setProductId(201L)
                .setCount(new BigDecimal("5")).setOutCount(new BigDecimal("4")).setReturnCount(new BigDecimal("0"));

        // mock 行为
        when(saleOrderItemMapper.selectListByOrderId(eq(orderId))).thenReturn(Arrays.asList(item1, item2));

        // returnCountMap：item1 更新到 3（< outCount=8 合法），item2 更新到 2（< outCount=4 合法）
        Map<Long, BigDecimal> returnCountMap = new HashMap<>();
        returnCountMap.put(101L, new BigDecimal("3"));
        returnCountMap.put(102L, new BigDecimal("2"));

        // 调用
        saleOrderService.updateSaleOrderReturnCount(orderId, returnCountMap);

        // 验证：两个 item 都被更新
        verify(saleOrderItemMapper).updateById(argThat((ErpSaleOrderItemDO update) ->
                101L == update.getId() && new BigDecimal("3").compareTo(update.getReturnCount()) == 0));
        verify(saleOrderItemMapper).updateById(argThat((ErpSaleOrderItemDO update) ->
                102L == update.getId() && new BigDecimal("2").compareTo(update.getReturnCount()) == 0));
        // 验证主表更新 totalReturnCount = 3 + 2 = 5
        verify(saleOrderMapper).updateById(argThat((ErpSaleOrderDO update) -> orderId.equals(update.getId())
                && new BigDecimal("5").compareTo(update.getReturnCount()) == 0));
    }

    // ==================== delete 类 ====================

    @Test
    public void testDeleteSaleOrder_processStatus_success() {
        // mock 数据：流程中订单可删除
        Long id = 99L;
        ErpSaleOrderDO order = new ErpSaleOrderDO()
                .setId(id).setNo("XSDD001").setStatus(ErpAuditStatus.PROCESS.getStatus());

        // mock 行为
        when(saleOrderMapper.selectByIds(eq(Collections.singletonList(id)))).thenReturn(Collections.singletonList(order));

        // 调用
        saleOrderService.deleteSaleOrder(Collections.singletonList(id));

        // 验证
        verify(saleOrderMapper).deleteById(eq(id));
        verify(saleOrderItemMapper).deleteByOrderId(eq(id));
    }

    @Test
    public void testDeleteSaleOrder_approvedStatus_throwException() {
        // mock 数据：已审核订单不可删除
        Long id = 99L;
        ErpSaleOrderDO order = new ErpSaleOrderDO()
                .setId(id).setNo("XSDD001").setStatus(ErpAuditStatus.APPROVE.getStatus());

        // mock 行为
        when(saleOrderMapper.selectByIds(eq(Collections.singletonList(id)))).thenReturn(Collections.singletonList(order));

        // 调用 + 断言
        assertServiceException(() -> saleOrderService.deleteSaleOrder(Collections.singletonList(id)),
                SALE_ORDER_DELETE_FAIL_APPROVE, "XSDD001");
        // 验证未删除
        verify(saleOrderMapper, never()).deleteById(anyLong());
        verify(saleOrderItemMapper, never()).deleteByOrderId(anyLong());
    }

    // ==================== 查询类 ====================

    @Test
    public void testValidateSaleOrder_notExists_throwException() {
        // mock 行为
        when(saleOrderMapper.selectById(eq(99L))).thenReturn(null);

        // 调用 + 断言
        assertServiceException(() -> saleOrderService.validateSaleOrder(99L), SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void testValidateSaleOrder_exists_returnDO() {
        // mock 数据：已审核订单
        Long id = 99L;
        ErpSaleOrderDO order = new ErpSaleOrderDO()
                .setId(id).setNo("XSDD001").setStatus(ErpAuditStatus.APPROVE.getStatus());

        // mock 行为
        when(saleOrderMapper.selectById(eq(id))).thenReturn(order);

        // 调用
        ErpSaleOrderDO result = saleOrderService.validateSaleOrder(id);

        // 断言
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    // ==================== 工具方法 ====================

    private ErpSaleOrderSaveReqVO buildBaseReq() {
        ErpSaleOrderSaveReqVO reqVO = new ErpSaleOrderSaveReqVO();
        reqVO.setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        return reqVO;
    }

    private ErpSaleOrderSaveReqVO.Item buildItem(Long productId, BigDecimal productPrice, BigDecimal count) {
        ErpSaleOrderSaveReqVO.Item item = new ErpSaleOrderSaveReqVO.Item();
        item.setProductId(productId);
        item.setProductUnitId(400L);
        item.setProductPrice(productPrice);
        item.setCount(count);
        item.setTaxPercent(BigDecimal.ZERO);
        return item;
    }

    private List<ErpWarehouseDO> buildWarehouseList(Iterable<Long> warehouseIds) {
        java.util.ArrayList<ErpWarehouseDO> warehouses = new java.util.ArrayList<>();
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

    private static cn.iocoder.yudao.framework.common.exception.ServiceException exceptionOf(
            cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        return new cn.iocoder.yudao.framework.common.exception.ServiceException(errorCode);
    }

}
