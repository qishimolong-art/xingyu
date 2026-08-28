package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ERP_ITEM_BATCH_NO_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_IN_EXCEED_INABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_IN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_COUNT_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_PRICE_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_SUBMIT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_SUPPLIER_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PURCHASE_ORDER_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpPurchaseOrderServiceImpl} 的单元测试类
 */
public class ErpPurchaseOrderServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseOrderServiceImpl purchaseOrderService;
    @InjectMocks
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;

    @Mock
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;

    @BeforeEach
    public void setUp() {
        // 替换 Redis 序号生成器（不连接真实 Redis）
        ReflectionTestUtils.setField(purchaseOrderService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
        ReflectionTestUtils.setField(purchaseOrderService, "supplierDeptPermissionService",
                supplierDeptPermissionService);
        ReflectionTestUtils.setField(purchaseOrderService, "productBatchNoValidator",
                new ErpProductBatchNoValidator());
    }

    private ErpPurchaseOrderSaveReqVO.Item buildItem(Long productId, BigDecimal count, BigDecimal price) {
        ErpPurchaseOrderSaveReqVO.Item item = new ErpPurchaseOrderSaveReqVO.Item();
        item.setProductId(productId);
        item.setCount(count);
        item.setProductPrice(price);
        return item;
    }

    private ErpPurchaseOrderSaveReqVO buildBaseReqVO(Long supplierId, ErpPurchaseOrderSaveReqVO.Item... items) {
        ErpPurchaseOrderSaveReqVO vo = new ErpPurchaseOrderSaveReqVO();
        vo.setSupplierId(supplierId);
        vo.setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        vo.setItems(Arrays.asList(items));
        return vo;
    }

    private DeptRespDTO buildDept(Long id, String name, Long parentId, Integer status) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        dept.setStatus(status);
        return dept;
    }

    private DeptDataPermissionRespDTO buildAllDeptPermission() {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(true);
        return permission;
    }

    private DeptDataPermissionRespDTO buildDeptPermission(Long... deptIds) {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setDeptIds(new HashSet<>(Arrays.asList(deptIds)));
        return permission;
    }

    // ========== getSupplierAvailableDeptSimpleList ==========

    @Test
    public void testGetSupplierAvailableDeptSimpleList_intersection() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpSupplierDO supplier = new ErpSupplierDO().setId(supplierId).setDeptId(10L);
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(supplier);
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Arrays.asList(10L, 20L)));
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setDeptIds(new HashSet<>(Collections.singletonList(20L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept"))).thenReturn(permission);
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(20L)))))
                .thenReturn(Collections.singletonList(buildDept(20L, "采购二部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseOrderService.getSupplierAvailableDeptSimpleList(supplierId);

            assertEquals(1, result.size());
            assertEquals(20L, result.get(0).getId());
            assertEquals("采购二部", result.get(0).getName());
        }
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_allPermissionFiltersDisabledDept() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpSupplierDO supplier = new ErpSupplierDO().setId(supplierId).setDeptId(10L);
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(supplier);
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Arrays.asList(10L, 20L)));
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(true);
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept"))).thenReturn(permission);
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Arrays.asList(10L, 20L)))))
                .thenReturn(Arrays.asList(
                        buildDept(10L, "采购一部", 0L, CommonStatusEnum.ENABLE.getStatus()),
                        buildDept(20L, "采购二部", 0L, CommonStatusEnum.DISABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseOrderService.getSupplierAvailableDeptSimpleList(supplierId);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getId());
        }
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_noIntersection() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpSupplierDO supplier = new ErpSupplierDO().setId(supplierId).setDeptId(10L);
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(supplier);
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Collections.singletonList(20L)));
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setDeptIds(new HashSet<>(Collections.singletonList(30L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept"))).thenReturn(permission);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseOrderService.getSupplierAvailableDeptSimpleList(supplierId);

            assertTrue(result.isEmpty());
            verify(deptApi, never()).getDeptList(any());
        }
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_usesSystemDeptPermissionOnly() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpSupplierDO supplier = new ErpSupplierDO().setId(supplierId).setDeptId(10L);
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(supplier);
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Arrays.asList(10L, 20L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildDeptPermission(20L));
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(20L)))))
                .thenReturn(Collections.singletonList(buildDept(20L, "采购二部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseOrderService.getSupplierAvailableDeptSimpleList(supplierId);

            assertEquals(1, result.size());
            assertEquals(20L, result.get(0).getId());
            verify(permissionApi).getDeptDataPermission(eq(loginUserId), eq("system_dept"));
            verify(permissionApi, never()).getDeptDataPermission(eq(loginUserId), eq("erp_purchase_order"));
        }
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_validateSupplierException() {
        Long supplierId = 100L;
        ServiceException exception = new ServiceException(1, "供应商不存在");
        when(supplierService.validateSupplier(eq(supplierId))).thenThrow(exception);

        ServiceException result = assertThrows(ServiceException.class,
                () -> purchaseOrderService.getSupplierAvailableDeptSimpleList(supplierId));

        assertEquals(exception, result);
        verify(permissionApi, never()).getDeptDataPermission(any(), any());
        verify(deptApi, never()).getDeptList(any());
    }

    // ========== createPurchaseOrder ==========

    @Test
    public void testCreatePurchaseOrder_success() {
        Long supplierId = 100L;
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(supplierId, item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L).setName("螺丝")));
        when(purchaseOrderMapper.selectByNo(any())).thenReturn(null);

        purchaseOrderService.createPurchaseOrder(reqVO);

        verify(supplierService).validateSupplier(eq(supplierId));
        ArgumentCaptor<ErpPurchaseOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).insert(orderCaptor.capture());
        ErpPurchaseOrderDO inserted = orderCaptor.getValue();
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
        assertEquals("CGDD20260520000001", inserted.getNo());
        assertEquals(new BigDecimal("10"), inserted.getTotalCount());
        // totalPrice = 50 - 50*0% = 50
        assertEquals(0, inserted.getTotalPrice().compareTo(new BigDecimal("50.00")));
        verify(purchaseOrderItemMapper).insertBatch(anyList());
    }

    @Test
    public void testCreatePurchaseOrder_giftItemForcesZeroPrice() {
        ErpPurchaseOrderSaveReqVO.Item gift = buildItem(200L, new BigDecimal("3"), new BigDecimal("99"));
        gift.setGift(true);
        ErpPurchaseOrderSaveReqVO.Item normal = buildItem(201L, new BigDecimal("2"), new BigDecimal("100"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, gift, normal);

        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(200L).setUnitId(1L),
                new ErpProductDO().setId(201L).setUnitId(1L)));

        purchaseOrderService.createPurchaseOrder(reqVO);

        ArgumentCaptor<List<ErpPurchaseOrderItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseOrderItemMapper).insertBatch(itemsCaptor.capture());
        List<ErpPurchaseOrderItemDO> items = itemsCaptor.getValue();
        // 赠品行：productPrice/totalPrice/taxPrice 全部强制为 0
        ErpPurchaseOrderItemDO giftItem = items.stream()
                .filter(it -> it.getProductId().equals(200L)).findFirst().orElseThrow(AssertionError::new);
        assertEquals(0, giftItem.getProductPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, giftItem.getTotalPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, giftItem.getTaxPrice().compareTo(BigDecimal.ZERO));

        // 主表合计：totalCount 包含赠品行数量（3+2=5），totalProductPrice 排除赠品（仅 200）
        ArgumentCaptor<ErpPurchaseOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).insert(orderCaptor.capture());
        ErpPurchaseOrderDO order = orderCaptor.getValue();
        assertEquals(0, order.getTotalCount().compareTo(new BigDecimal("5")));
        assertEquals(0, order.getTotalProductPrice().compareTo(new BigDecimal("200.00")));
    }

    @Test
    public void testCreatePurchaseOrder_countZero_throwException() {
        ErpPurchaseOrderSaveReqVO.Item invalid = buildItem(200L, BigDecimal.ZERO, new BigDecimal("5"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, invalid);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.createPurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_ITEM_COUNT_POSITIVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).insert(any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testCreatePurchaseOrder_priceZero_throwException() {
        ErpPurchaseOrderSaveReqVO.Item invalid = buildItem(200L, new BigDecimal("5"), BigDecimal.ZERO);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, invalid);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.createPurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_ITEM_PRICE_POSITIVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).insert(any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testCreatePurchaseOrder_batchNoEnabledWithoutBatchNo_throwException() {
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("5"), new BigDecimal("10"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L).setBatchNoEnabled(true)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.createPurchaseOrder(reqVO));

        assertEquals(ERP_ITEM_BATCH_NO_REQUIRED.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).insert(any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testCreatePurchaseOrder_batchNoEnabledWithBatchNo_success() {
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("5"), new BigDecimal("10"));
        item.setBatchNo("BATCH-001");
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L).setBatchNoEnabled(true)));
        when(purchaseOrderMapper.selectByNo(any())).thenReturn(null);

        purchaseOrderService.createPurchaseOrder(reqVO);

        ArgumentCaptor<List<ErpPurchaseOrderItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseOrderItemMapper).insertBatch(itemsCaptor.capture());
        assertEquals("BATCH-001", itemsCaptor.getValue().get(0).getBatchNo());
    }

    @Test
    public void testCreatePurchaseOrder_withAccountId_validatesAccount() {
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, item);
        reqVO.setAccountId(50L);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));

        purchaseOrderService.createPurchaseOrder(reqVO);

        verify(accountService).validateAccount(eq(50L));
    }

    @Test
    public void testCreatePurchaseOrder_supplierDeptNotAllowed_throwException() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(supplierId, item);
        reqVO.setDeptId(30L);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(
                new ErpSupplierDO().setId(supplierId).setDeptId(10L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.emptyMap());
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildAllDeptPermission());
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(buildDept(10L, "采购一部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> purchaseOrderService.createPurchaseOrder(reqVO));

            assertEquals(PURCHASE_ORDER_SUPPLIER_DEPT_NOT_ALLOWED.getCode(), ex.getCode());
            verify(purchaseOrderMapper, never()).insert(any(ErpPurchaseOrderDO.class));
        }
    }

    @Test
    public void testCreatePurchaseOrderDraft_withoutItems_throwException() {
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.createPurchaseOrderDraft(reqVO));

        assertEquals(PURCHASE_ORDER_SUBMIT_ITEMS_REQUIRED.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).insert(any(ErpPurchaseOrderDO.class));
        verify(purchaseOrderItemMapper, never()).insertBatch(anyList());
    }

    @Test
    public void testCreatePurchaseOrderDraft_priceZero_keepsSelectedStockItem() {
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("2"), BigDecimal.ZERO);
        item.setWarehouseId(7L);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(null, item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(
                7L, new ErpWarehouseDO().setId(7L).setDeptId(3L)));
        when(purchaseOrderMapper.selectByNo(any())).thenReturn(null);

        purchaseOrderService.createPurchaseOrderDraft(reqVO);

        ArgumentCaptor<ErpPurchaseOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).insert(orderCaptor.capture());
        ErpPurchaseOrderDO inserted = orderCaptor.getValue();
        assertEquals(ErpPurchaseOrderStatusEnum.DRAFT.getStatus(), inserted.getStatus());
        assertEquals(0, inserted.getTotalCount().compareTo(new BigDecimal("2")));
        assertEquals(0, inserted.getTotalProductPrice().compareTo(BigDecimal.ZERO));

        ArgumentCaptor<List<ErpPurchaseOrderItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseOrderItemMapper).insertBatch(itemsCaptor.capture());
        ErpPurchaseOrderItemDO insertedItem = itemsCaptor.getValue().get(0);
        assertEquals(200L, insertedItem.getProductId());
        assertEquals(7L, insertedItem.getWarehouseId());
        assertEquals(0, insertedItem.getProductPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, insertedItem.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testUpdatePurchaseOrderDraft_withoutItems_setsZeroTotals() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpPurchaseOrderStatusEnum.DRAFT.getStatus())
                .setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseOrderMapper.updateByIdAndStatus(eq(10L), eq(ErpPurchaseOrderStatusEnum.DRAFT.getStatus()),
                any(ErpPurchaseOrderDO.class))).thenReturn(1);

        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(null);
        reqVO.setId(10L);

        purchaseOrderService.updatePurchaseOrderDraft(reqVO);

        ArgumentCaptor<ErpPurchaseOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateByIdAndStatus(eq(10L), eq(ErpPurchaseOrderStatusEnum.DRAFT.getStatus()),
                orderCaptor.capture());
        ErpPurchaseOrderDO updated = orderCaptor.getValue();
        assertEquals(0, updated.getTotalCount().compareTo(BigDecimal.ZERO));
        assertEquals(0, updated.getTotalProductPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, updated.getTotalTaxPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, updated.getTotalPrice().compareTo(BigDecimal.ZERO));
        verify(purchaseOrderItemMapper).deleteByOrderId(eq(10L));
        verify(purchaseOrderItemMapper, never()).insertBatch(anyList());
    }

    @Test
    public void testUpdatePurchaseOrderDraft_existingSupplierDeptNotAllowed_throwException() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpPurchaseOrderStatusEnum.DRAFT.getStatus())
                .setSupplierId(supplierId).setDeptId(10L)
                .setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(
                new ErpSupplierDO().setId(supplierId).setDeptId(10L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.emptyMap());
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildAllDeptPermission());
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(buildDept(10L, "采购一部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("2"), BigDecimal.ZERO);
        item.setWarehouseId(7L);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(null, item);
        reqVO.setId(10L);
        reqVO.setDeptId(20L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> purchaseOrderService.updatePurchaseOrderDraft(reqVO));

            assertEquals(PURCHASE_ORDER_SUPPLIER_DEPT_NOT_ALLOWED.getCode(), ex.getCode());
            verify(purchaseOrderMapper, never()).updateByIdAndStatus(any(), any(), any());
        }
    }

    @Test
    public void testUpdateAndSubmitPurchaseOrderDraft_movesToProcessNotApprove() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpPurchaseOrderStatusEnum.DRAFT.getStatus())
                .setSupplierId(100L).setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseOrderMapper.updateByIdAndStatus(eq(10L), eq(ErpPurchaseOrderStatusEnum.DRAFT.getStatus()),
                any(ErpPurchaseOrderDO.class))).thenReturn(1);
        ErpPurchaseOrderItemDO persistedItem = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L).setWarehouseId(7L)
                .setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("5"));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(persistedItem));

        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("2"), new BigDecimal("5"));
        item.setWarehouseId(7L);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, item);
        reqVO.setId(10L);

        purchaseOrderService.updateAndSubmitPurchaseOrder(reqVO);

        ArgumentCaptor<ErpPurchaseOrderDO> statusCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPurchaseOrderStatusEnum.DRAFT.getStatus()), statusCaptor.capture());
        assertEquals(ErpPurchaseOrderStatusEnum.PROCESS.getStatus(), statusCaptor.getValue().getStatus());
    }

    // ========== updatePurchaseOrder ==========

    @Test
    public void testUpdatePurchaseOrder_alreadyApproved_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L,
                buildItem(200L, new BigDecimal("1"), new BigDecimal("5")));
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_UPDATE_FAIL_APPROVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).updateById(any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testUpdatePurchaseOrder_notExists_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(null);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L);
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrder_giftModifyWhenHasIn_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        // 旧项已入库 + gift=false；新项试图改为 gift=true
        ErpPurchaseOrderItemDO oldItem = new ErpPurchaseOrderItemDO()
                .setId(99L).setProductId(200L).setCount(new BigDecimal("5"))
                .setInCount(new BigDecimal("2")).setGift(false);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(oldItem));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO().setId(200L).setName("螺丝"));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));

        ErpPurchaseOrderSaveReqVO.Item changedGift = buildItem(200L, new BigDecimal("5"), new BigDecimal("10"));
        changedGift.setId(99L);
        changedGift.setGift(true);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, changedGift);
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrder_supplierDeptPermissionDenied_throwException() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(Collections.emptyList());
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(
                new ErpSupplierDO().setId(supplierId).setDeptId(20L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Collections.singletonList(20L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildDeptPermission(10L));

        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("5"), new BigDecimal("10"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(supplierId, item);
        reqVO.setId(10L);
        reqVO.setDeptId(20L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> purchaseOrderService.updatePurchaseOrder(reqVO));

            assertEquals(PURCHASE_ORDER_SUPPLIER_DEPT_NOT_ALLOWED.getCode(), ex.getCode());
            verify(purchaseOrderMapper, never()).updateById(any(ErpPurchaseOrderDO.class));
        }
    }

    // ========== updatePurchaseOrderRemark ==========

    @Test
    public void testUpdatePurchaseOrderRemark_approvedSuccess() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSupplierId(100L).setTotalPrice(new BigDecimal("50.00"));
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        ErpPurchaseOrderUpdateRemarkReqVO reqVO = new ErpPurchaseOrderUpdateRemarkReqVO();
        reqVO.setId(10L);
        reqVO.setRemark("审批后补充备注");

        purchaseOrderService.updatePurchaseOrderRemark(reqVO);

        ArgumentCaptor<ErpPurchaseOrderDO> captor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateById(captor.capture());
        ErpPurchaseOrderDO updateObj = captor.getValue();
        assertEquals(10L, updateObj.getId());
        assertEquals("审批后补充备注", updateObj.getRemark());
        assertNull(updateObj.getStatus());
        assertNull(updateObj.getSupplierId());
        assertNull(updateObj.getTotalPrice());
        verify(operateLogService).recordUpdate(ERP_PURCHASE_ORDER_TYPE, 10L, "CGDD001");
    }

    @Test
    public void testUpdatePurchaseOrderRemark_clearSuccess() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        ErpPurchaseOrderUpdateRemarkReqVO reqVO = new ErpPurchaseOrderUpdateRemarkReqVO();
        reqVO.setId(10L);
        reqVO.setRemark("");

        purchaseOrderService.updatePurchaseOrderRemark(reqVO);

        ArgumentCaptor<ErpPurchaseOrderDO> captor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateById(captor.capture());
        assertEquals("", captor.getValue().getRemark());
    }

    @Test
    public void testUpdatePurchaseOrderRemark_notExists_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(null);
        ErpPurchaseOrderUpdateRemarkReqVO reqVO = new ErpPurchaseOrderUpdateRemarkReqVO();
        reqVO.setId(10L);
        reqVO.setRemark("备注");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderRemark(reqVO));

        assertEquals(PURCHASE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).updateById(any(ErpPurchaseOrderDO.class));
    }

    // ========== batchUpdatePurchaseOrderItems ==========

    @Test
    public void testGetWarehouseAvailableDeptSimpleList_intersectionAndEnabledOnly() {
        Long warehouseId = 8L;
        Long loginUserId = 104L;
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(warehouseId))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(warehouseId).setDeptId(10L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildDeptPermission(10L, 30L));
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Arrays.asList(
                        buildDept(10L, "采购二部", 0L, CommonStatusEnum.ENABLE.getStatus()),
                        buildDept(30L, "采购三部", 0L, CommonStatusEnum.DISABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseOrderService.getWarehouseAvailableDeptSimpleList(warehouseId);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getId());
            assertEquals("采购二部", result.get(0).getName());
            verify(warehouseService, never()).getWarehouseSaleDeptIds(any());
        }
    }

    @Test
    public void testBatchUpdatePurchaseOrderItems_successWarehouseAndDept() {
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(order);
        List<ErpPurchaseOrderItemDO> items = Arrays.asList(
                new ErpPurchaseOrderItemDO().setId(1L).setOrderId(10L).setProductId(200L)
                        .setWarehouseId(7L).setDeptId(3L).setGift(false)
                        .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO),
                new ErpPurchaseOrderItemDO().setId(2L).setOrderId(10L).setProductId(201L)
                        .setWarehouseId(7L).setDeptId(3L).setGift(false)
                        .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(items);
        when(purchaseInItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(purchaseReturnItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(8L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(8L).setDeptId(10L)));
        ErpPurchaseOrderItemBatchUpdateReqVO reqVO = new ErpPurchaseOrderItemBatchUpdateReqVO();
        reqVO.setOrderId(10L);
        reqVO.setItemIds(Arrays.asList(1L, 2L));
        reqVO.setWarehouseId(8L);
        reqVO.setDeptId(10L);

        purchaseOrderService.batchUpdatePurchaseOrderItems(reqVO);

        verify(stockService).ensureStockExists(eq(200L), eq(8L));
        verify(stockService).ensureStockExists(eq(201L), eq(8L));
        ArgumentCaptor<ErpPurchaseOrderItemDO> itemCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderItemDO.class);
        verify(purchaseOrderItemMapper, times(2)).updateById(itemCaptor.capture());
        itemCaptor.getAllValues().forEach(updateItem -> {
            assertEquals(8L, updateItem.getWarehouseId());
            assertEquals(10L, updateItem.getDeptId());
        });
        verify(operateLogService).recordUpdate(ERP_PURCHASE_ORDER_TYPE, 10L, "CGDD001");
    }

    @Test
    public void testBatchUpdatePurchaseOrderItems_successDeptOnlySameWarehouse() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(
                new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001"));
        List<ErpPurchaseOrderItemDO> items = Arrays.asList(
                new ErpPurchaseOrderItemDO().setId(1L).setOrderId(10L).setProductId(200L)
                        .setWarehouseId(7L).setDeptId(3L).setGift(false)
                        .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO),
                new ErpPurchaseOrderItemDO().setId(2L).setOrderId(10L).setProductId(201L)
                        .setWarehouseId(7L).setDeptId(3L).setGift(false)
                        .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(items);
        when(purchaseInItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(purchaseReturnItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(7L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(7L).setDeptId(3L)));
        ErpPurchaseOrderItemBatchUpdateReqVO reqVO = new ErpPurchaseOrderItemBatchUpdateReqVO();
        reqVO.setOrderId(10L);
        reqVO.setItemIds(Arrays.asList(1L, 2L));
        reqVO.setDeptId(3L);

        purchaseOrderService.batchUpdatePurchaseOrderItems(reqVO);

        ArgumentCaptor<ErpPurchaseOrderItemDO> itemCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderItemDO.class);
        verify(purchaseOrderItemMapper, times(2)).updateById(itemCaptor.capture());
        itemCaptor.getAllValues().forEach(updateItem -> {
            assertEquals(7L, updateItem.getWarehouseId());
            assertEquals(3L, updateItem.getDeptId());
        });
        verify(stockService, never()).ensureStockExists(any(), any());
    }

    @Test
    public void testBatchUpdatePurchaseOrderItems_onlyWarehouseAutoFillsWarehouseDept() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(
                new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001"));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseOrderItemDO().setId(1L).setOrderId(10L).setProductId(200L)
                        .setWarehouseId(7L).setDeptId(3L).setInCount(BigDecimal.ZERO)
                        .setReturnCount(BigDecimal.ZERO)));
        when(purchaseInItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(purchaseReturnItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(8L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(8L).setDeptId(10L)));
        ErpPurchaseOrderItemBatchUpdateReqVO reqVO = new ErpPurchaseOrderItemBatchUpdateReqVO();
        reqVO.setOrderId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setWarehouseId(8L);

        purchaseOrderService.batchUpdatePurchaseOrderItems(reqVO);

        verify(stockService).ensureStockExists(eq(200L), eq(8L));
        ArgumentCaptor<ErpPurchaseOrderItemDO> itemCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderItemDO.class);
        verify(purchaseOrderItemMapper).updateById(itemCaptor.capture());
        assertEquals(8L, itemCaptor.getValue().getWarehouseId());
        assertEquals(10L, itemCaptor.getValue().getDeptId());
    }

    @Test
    public void testBatchUpdatePurchaseOrderItems_hasInCount_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(
                new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001"));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseOrderItemDO().setId(1L).setOrderId(10L).setProductId(200L)
                        .setWarehouseId(7L).setDeptId(3L).setInCount(new BigDecimal("1"))
                        .setReturnCount(BigDecimal.ZERO)));

        ErpPurchaseOrderItemBatchUpdateReqVO reqVO = new ErpPurchaseOrderItemBatchUpdateReqVO();
        reqVO.setOrderId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(20L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.batchUpdatePurchaseOrderItems(reqVO));

        assertEquals(PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_IN.getCode(), ex.getCode());
        verify(purchaseOrderItemMapper, never()).updateById(any(ErpPurchaseOrderItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseOrderItems_duplicateAfterUpdate_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(
                new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001"));
        List<ErpPurchaseOrderItemDO> items = Arrays.asList(
                new ErpPurchaseOrderItemDO().setId(1L).setOrderId(10L).setProductId(200L)
                        .setWarehouseId(7L).setDeptId(3L).setGift(false)
                        .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO),
                new ErpPurchaseOrderItemDO().setId(2L).setOrderId(10L).setProductId(200L)
                        .setWarehouseId(8L).setDeptId(10L).setGift(false)
                        .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(items);
        when(purchaseInItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(purchaseReturnItemMapper.selectCountByOrderItemIds(any())).thenReturn(0L);
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(8L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(8L).setDeptId(10L)));
        ErpPurchaseOrderItemBatchUpdateReqVO reqVO = new ErpPurchaseOrderItemBatchUpdateReqVO();
        reqVO.setOrderId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setWarehouseId(8L);
        reqVO.setDeptId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.batchUpdatePurchaseOrderItems(reqVO));

        assertEquals(PURCHASE_ORDER_ITEM_DUPLICATE.getCode(), ex.getCode());
        verify(purchaseOrderItemMapper, never()).updateById(any(ErpPurchaseOrderItemDO.class));
        verify(stockService, never()).ensureStockExists(any(), any());
    }

    // ========== updatePurchaseOrderStatus ==========

    @Test
    public void testSubmitPurchaseOrderDraft_supplierDeptNotAllowed_throwException() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpPurchaseOrderStatusEnum.DRAFT.getStatus())
                .setSupplierId(supplierId).setDeptId(30L)
                .setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(
                new ErpSupplierDO().setId(supplierId).setDeptId(30L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.emptyMap());
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildAllDeptPermission());
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(30L)))))
                .thenReturn(Collections.singletonList(buildDept(30L, "采购三部", 0L,
                        CommonStatusEnum.DISABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> purchaseOrderService.submitPurchaseOrderDraft(10L));

            assertEquals(PURCHASE_ORDER_SUPPLIER_DEPT_NOT_ALLOWED.getCode(), ex.getCode());
            verify(purchaseOrderMapper, never()).updateByIdAndStatus(any(), any(), any());
        }
    }

    @Test
    public void testUpdatePurchaseOrderStatus_approveSuccess() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSupplierId(100L).setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseOrderItemDO().setId(1L).setProductId(200L).setCount(new BigDecimal("10"))
                        .setProductPrice(new BigDecimal("5"))));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseOrderMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseOrderDO.class))).thenReturn(1);

        purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        verify(purchaseOrderMapper).updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testUpdatePurchaseOrderStatus_approveWhenAlreadyApproved_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.APPROVE.getStatus()));
        assertEquals(PURCHASE_ORDER_APPROVE_FAIL.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processWhenAlreadyProcess_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus()));
        assertEquals(PURCHASE_ORDER_PROCESS_FAIL.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processSuccessWhenNoInbound() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.selectListByOrderIdAndStatus(eq(10L), eq(ErpAuditStatus.APPROVE.getStatus())))
                .thenReturn(Collections.emptyList());
        when(purchaseOrderMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.APPROVE.getStatus()), any(ErpPurchaseOrderDO.class))).thenReturn(1);

        purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus());

        verify(purchaseOrderMapper).updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.APPROVE.getStatus()), any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processWhenHasInCount_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(new BigDecimal("3")).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus()));
        assertEquals(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processWhenHasApprovedIn_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.selectListByOrderIdAndStatus(eq(10L), eq(ErpAuditStatus.APPROVE.getStatus())))
                .thenReturn(Collections.singletonList(new ErpPurchaseInDO().setId(20L)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus()));
        assertEquals(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processWhenHasReturn_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(new BigDecimal("1"));
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.selectListByOrderIdAndStatus(eq(10L), eq(ErpAuditStatus.APPROVE.getStatus())))
                .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus()));
        assertEquals(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_approveLostByOptimisticLock_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSupplierId(100L).setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseOrderItemDO().setId(1L).setProductId(200L).setCount(new BigDecimal("10"))
                        .setProductPrice(new BigDecimal("5"))));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseOrderMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.APPROVE.getStatus()));
        assertEquals(PURCHASE_ORDER_APPROVE_FAIL.getCode(), ex.getCode());
    }

    // ========== updatePurchaseOrderInCount ==========

    @Test
    public void testUpdatePurchaseOrderInCount_success() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> inMap = new HashMap<>();
        inMap.put(1L, new BigDecimal("3"));

        purchaseOrderService.updatePurchaseOrderInCount(10L, inMap);

        verify(purchaseOrderItemMapper).updateById(any(ErpPurchaseOrderItemDO.class));
        ArgumentCaptor<ErpPurchaseOrderDO> captor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getInCount().compareTo(new BigDecimal("3")));
    }

    @Test
    public void testUpdatePurchaseOrderInCount_excess_throwException() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO().setId(200L).setName("螺丝"));
        Map<Long, BigDecimal> inMap = new HashMap<>();
        inMap.put(1L, new BigDecimal("11")); // 超过 count

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderInCount(10L, inMap));
        assertEquals(PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderInCount_unchanged_skipUpdate() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("3"));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> inMap = new HashMap<>();
        inMap.put(1L, new BigDecimal("3")); // 与现有相同

        purchaseOrderService.updatePurchaseOrderInCount(10L, inMap);

        verify(purchaseOrderItemMapper, never()).updateById(any(ErpPurchaseOrderItemDO.class));
        // 父订单更新仍发生
        verify(purchaseOrderMapper).updateById(any(ErpPurchaseOrderDO.class));
    }

    // ========== updatePurchaseOrderReturnCount ==========

    @Test
    public void testUpdatePurchaseOrderReturnCount_excess_throwException() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("5"))
                .setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO().setId(200L).setName("螺丝"));
        Map<Long, BigDecimal> returnMap = new HashMap<>();
        returnMap.put(1L, new BigDecimal("6")); // 超过 inCount

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderReturnCount(10L, returnMap));
        assertEquals(PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderReturnCount_success() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("5"))
                .setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> returnMap = new HashMap<>();
        returnMap.put(1L, new BigDecimal("2"));

        purchaseOrderService.updatePurchaseOrderReturnCount(10L, returnMap);

        verify(purchaseOrderItemMapper).updateById(any(ErpPurchaseOrderItemDO.class));
        ArgumentCaptor<ErpPurchaseOrderDO> captor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getReturnCount().compareTo(new BigDecimal("2")));
    }

    // ========== deletePurchaseOrder ==========

    @Test
    public void testDeletePurchaseOrder_success() {
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseOrderMapper.selectByIds(any())).thenReturn(Collections.singletonList(order));

        purchaseOrderService.deletePurchaseOrder(Collections.singletonList(10L));

        verify(purchaseOrderMapper).deleteById(eq(10L));
        verify(purchaseOrderItemMapper).deleteByOrderId(eq(10L));
    }

    @Test
    public void testDeletePurchaseOrder_alreadyApproved_throwException() {
        ErpPurchaseOrderDO approved = new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseOrderMapper.selectByIds(any())).thenReturn(Collections.singletonList(approved));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.deletePurchaseOrder(Collections.singletonList(10L)));
        assertEquals(PURCHASE_ORDER_DELETE_FAIL_APPROVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).deleteById(any(Long.class));
    }

    @Test
    public void testDeletePurchaseOrder_emptyList_noOp() {
        when(purchaseOrderMapper.selectByIds(any())).thenReturn(Collections.emptyList());

        purchaseOrderService.deletePurchaseOrder(Collections.singletonList(10L));

        verify(purchaseOrderMapper, never()).deleteById(any(Long.class));
        verify(purchaseOrderItemMapper, never()).deleteByOrderId(any(Long.class));
    }

    // ========== validatePurchaseOrder ==========

    @Test
    public void testValidatePurchaseOrder_notApproved_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(
                new ErpPurchaseOrderDO().setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.validatePurchaseOrder(10L));
        assertEquals(PURCHASE_ORDER_NOT_APPROVE.getCode(), ex.getCode());
    }

    @Test
    public void testValidatePurchaseOrder_success() {
        ErpPurchaseOrderDO approved = new ErpPurchaseOrderDO().setId(10L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(approved);

        ErpPurchaseOrderDO result = purchaseOrderService.validatePurchaseOrder(10L);

        assertNotNull(result);
        assertEquals(Long.valueOf(10L), result.getId());
    }

    @Test
    public void testValidatePurchaseOrder_notExists_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.validatePurchaseOrder(10L));
        assertEquals(PURCHASE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ========== getPurchaseOrderItemListByOrderIds ==========

    @Test
    public void testGetPurchaseOrderItemListByOrderIds_emptyInput_returnsEmpty() {
        List<ErpPurchaseOrderItemDO> result = purchaseOrderService
                .getPurchaseOrderItemListByOrderIds(Collections.emptyList());
        assertTrue(result.isEmpty());
        verify(purchaseOrderItemMapper, never()).selectListByOrderIds(any());
    }

    @Test
    public void testGetPurchaseOrderItemListByOrderIds_delegatesToMapper() {
        List<ErpPurchaseOrderItemDO> mocked = Collections.singletonList(new ErpPurchaseOrderItemDO().setId(1L));
        when(purchaseOrderItemMapper.selectListByOrderIds(any())).thenReturn(mocked);

        List<ErpPurchaseOrderItemDO> result = purchaseOrderService
                .getPurchaseOrderItemListByOrderIds(Collections.singletonList(10L));

        assertEquals(1, result.size());
    }

    // ========== getInableItemsByOrderId ==========

    @Test
    public void testGetInableItemsByOrderId_success() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus()));
        ErpPurchaseOrderItemDO item1 = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L).setProductPrice(new BigDecimal("10"))
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("3"))
                .setWarehouseId(7L).setDeptId(66L);
        ErpPurchaseOrderItemDO itemAllIn = new ErpPurchaseOrderItemDO()
                .setId(2L).setProductId(201L).setProductPrice(new BigDecimal("20"))
                .setCount(new BigDecimal("5")).setInCount(new BigDecimal("5"))
                .setWarehouseId(7L);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Arrays.asList(item1, itemAllIn));
        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        ErpProductRespVO p1 = new ErpProductRespVO();
        p1.setId(200L);
        p1.setName("螺丝");
        p1.setCode("P001");
        productMap.put(200L, p1);
        when(productService.getProductVOMap(any())).thenReturn(productMap);

        List<ErpPurchaseOrderInableItemRespVO> result = purchaseOrderService.getInableItemsByOrderId(10L);

        // item1 应有结果（inable = 10-3 = 7），itemAllIn 已全部入库被 convertList 过滤掉
        assertEquals(1, result.size());
        ErpPurchaseOrderInableItemRespVO vo = result.get(0);
        assertNotNull(vo);
        assertEquals(Long.valueOf(1L), vo.getOrderItemId());
        assertEquals(0, vo.getInableCount().compareTo(new BigDecimal("7")));
        assertEquals(Long.valueOf(66L), vo.getDeptId());
        assertEquals("螺丝", vo.getProductName());
    }

    @Test
    public void testGetInableItemsByOrderId_orderNotApproved_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.getInableItemsByOrderId(10L));
        assertEquals(PURCHASE_ORDER_NOT_APPROVE.getCode(), ex.getCode());
    }

    // ========== 涵盖文档要求 PURCHASE_ORDER_IN_EXCEED_INABLE 的常量引用 ==========

    @Test
    public void testGetInableItemsByOrderId_inCountGreaterThanCount_returnsEmpty() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus()));
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("5")).setInCount(new BigDecimal("8"));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());

        List<ErpPurchaseOrderInableItemRespVO> result = purchaseOrderService.getInableItemsByOrderId(10L);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testInExceedInableErrorCodeValue() {
        // 该错误码用于采购入库 Service 的"分批入库"分支引用；本测试用例确保枚举值未被破坏
        assertEquals(1_030_101_012, PURCHASE_ORDER_IN_EXCEED_INABLE.getCode());
    }

}
