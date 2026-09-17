package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateSaleCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BIZ_PROCESS_FAIL_VOUCHER_APPROVED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_DATA_PERMISSION_DENIED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_ADJUST;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_INVOICE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_RETURN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_SALE_CART;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_STOCK_IN_BILL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_TRANSFER_OUT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_COUNT_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_OPERATION_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_PRICE_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_UPDATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_SUBMIT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_SUPPLIER_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_TRANSFER_OUT_EXCEED_AVAILABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_TRANSFER_OUT_SOURCE_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_IN_EXCEED_INABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpPurchaseInServiceImpl} 的单元测试类
 */
public class ErpPurchaseInServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInServiceImpl purchaseInService;
    @InjectMocks
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;

    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private cn.iocoder.yudao.module.erp.service.purchase.cost.ErpPurchaseCostConfirmationService costConfirmationService;
    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpPurchaseInvoiceItemMapper purchaseInvoiceItemMapper;
    @Mock
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Mock
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpPurchaseOrderService purchaseOrderService;
    @Mock
    private ErpSaleCartService saleCartService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpStockInBillService stockInBillService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Mock
    private ErpVoucherService voucherService;
    @Mock
    private ErpBookOpenService bookOpenService;
    @Mock
    private ErpVoucherMapper voucherMapper;
    @Mock
    private ErpVoucherItemMapper voucherItemMapper;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Mock
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @BeforeEach
    public void setUp() {
        // 旧纯Mockito夹具的数据读取沿用；真实FOR UPDATE行为由MySQL集成测试验证。
        org.mockito.Mockito.lenient().when(purchaseInMapper.selectByIdForUpdate(anyLong()))
                .thenAnswer(invocation -> purchaseInMapper.selectById((Long)invocation.getArgument(0)));
        org.mockito.Mockito.lenient().when(purchaseInItemMapper.selectListByInIdForUpdate(anyLong()))
                .thenAnswer(invocation -> purchaseInItemMapper.selectListByInId(invocation.getArgument(0)));
        // 替换 Redis 序号生成器（不连接真实 Redis）
        ReflectionTestUtils.setField(purchaseInService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
        ReflectionTestUtils.setField(purchaseInService, "supplierDeptPermissionService",
                supplierDeptPermissionService);
    }

    private ErpPurchaseInSaveReqVO.Item buildItem(Long productId, BigDecimal count, BigDecimal price) {
        ErpPurchaseInSaveReqVO.Item item = new ErpPurchaseInSaveReqVO.Item();
        item.setProductId(productId);
        item.setProductUnitId(1L);
        item.setWarehouseId(10L);
        item.setCount(count);
        item.setProductPrice(price);
        return item;
    }

    private ErpPurchaseInSaveReqVO buildBaseReqVO(ErpPurchaseInSaveReqVO.Item... items) {
        ErpPurchaseInSaveReqVO vo = new ErpPurchaseInSaveReqVO();
        vo.setSupplierId(99L);
        vo.setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
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
        when(supplierService.validateSupplier(eq(supplierId)))
                .thenReturn(new ErpSupplierDO().setId(supplierId).setDeptId(10L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Arrays.asList(10L, 20L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildDeptPermission(20L));
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(20L)))))
                .thenReturn(Collections.singletonList(buildDept(20L, "采购二部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseInService.getSupplierAvailableDeptSimpleList(supplierId);

            assertEquals(1, result.size());
            assertEquals(20L, result.get(0).getId());
            assertEquals("采购二部", result.get(0).getName());
        }
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_allPermissionFiltersDisabledDept() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        when(supplierService.validateSupplier(eq(supplierId)))
                .thenReturn(new ErpSupplierDO().setId(supplierId).setDeptId(10L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Arrays.asList(10L, 20L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildAllDeptPermission());
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Arrays.asList(10L, 20L)))))
                .thenReturn(Arrays.asList(
                        buildDept(10L, "采购一部", 0L, CommonStatusEnum.ENABLE.getStatus()),
                        buildDept(20L, "采购二部", 0L, CommonStatusEnum.DISABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseInService.getSupplierAvailableDeptSimpleList(supplierId);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getId());
        }
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_noIntersection() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        when(supplierService.validateSupplier(eq(supplierId)))
                .thenReturn(new ErpSupplierDO().setId(supplierId).setDeptId(10L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Collections.singletonList(20L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildDeptPermission(30L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseInService.getSupplierAvailableDeptSimpleList(supplierId);

            assertTrue(result.isEmpty());
            verify(deptApi, never()).getDeptList(any());
        }
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_validateSupplierException() {
        Long supplierId = 100L;
        ServiceException exception = new ServiceException(1, "供应商不存在");
        when(supplierService.validateSupplier(eq(supplierId))).thenThrow(exception);

        ServiceException result = assertThrows(ServiceException.class,
                () -> purchaseInService.getSupplierAvailableDeptSimpleList(supplierId));

        assertEquals(exception, result);
        verify(permissionApi, never()).getDeptDataPermission(any(), any());
        verify(deptApi, never()).getDeptList(any());
    }

    @Test
    public void testGetSupplierAvailableDeptSimpleList_usesSystemDeptPermissionOnly() {
        Long supplierId = 100L;
        Long loginUserId = 104L;
        when(supplierService.validateSupplier(eq(supplierId)))
                .thenReturn(new ErpSupplierDO().setId(supplierId).setDeptId(10L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildDeptPermission(10L));
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(buildDept(10L, "采购一部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseInService.getSupplierAvailableDeptSimpleList(supplierId);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getId());
            verify(permissionApi).getDeptDataPermission(eq(loginUserId), eq("system_dept"));
            verify(permissionApi, never()).getDeptDataPermission(eq(loginUserId), eq("erp_purchase_in"));
        }
    }

    @Test
    public void testGetWarehouseAvailableDeptSimpleList_usesSystemDeptPermissionOnly() {
        Long warehouseId = 8L;
        Long loginUserId = 104L;
        when(warehouseService.validPurchaseWarehouseList(Collections.singleton(warehouseId)))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(warehouseId).setDeptId(20L)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildDeptPermission(20L));
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(20L)))))
                .thenReturn(Collections.singletonList(buildDept(20L, "采购二部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = purchaseInService.getWarehouseAvailableDeptSimpleList(warehouseId);

            assertEquals(1, result.size());
            assertEquals(20L, result.get(0).getId());
            verify(permissionApi).getDeptDataPermission(eq(loginUserId), eq("system_dept"));
            verify(permissionApi, never()).getDeptDataPermission(eq(loginUserId), eq("erp_purchase_in"));
        }
    }

    // ========== createPurchaseIn ==========

    @Test
    public void testCreatePurchaseIn_success() {
        // 仅含 supplierId（虽然 SaveReqVO 没 supplierId 字段，但若无 orderId，supplierId 由 BeanUtils 拷贝过去）
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L).setName("螺丝")));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);

        purchaseInService.createPurchaseIn(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).insert(captor.capture());
        ErpPurchaseInDO inserted = captor.getValue();
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
        assertEquals("CGRK20260520000001", inserted.getNo());
        assertEquals(Long.valueOf(99L), inserted.getSupplierId());
        assertEquals("", inserted.getOrderNo());
        assertEquals(0, inserted.getTotalCount().compareTo(new BigDecimal("10")));
        // totalProductPrice = 50；taxPrice = 0；totalPrice = 50 - 0 + 0 = 50
        assertEquals(0, inserted.getTotalPrice().compareTo(new BigDecimal("50.00")));
        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(itemsCaptor.capture());
        assertNull(itemsCaptor.getValue().get(0).getOrderItemId());
        // orderId 为空，不应调用订单更新
        assertEquals(null, inserted.getOrderId());
        verify(supplierService).validateSupplier(eq(99L));
        verify(purchaseOrderService, never()).updatePurchaseOrderInCount(anyLong(), any());
    }

    @Test
    public void testCreatePurchaseIn_emptyItems_throwException() {
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO();
        reqVO.setItems(Collections.emptyList());

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_SUBMIT_ITEMS_REQUIRED);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
        verify(purchaseInItemMapper, never()).insertBatch(anyList());
    }

    @Test
    public void testCreatePurchaseIn_supplierDeptNotAllowed_throwException() {
        Long loginUserId = 104L;
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);
        reqVO.setSupplierId(100L);
        reqVO.setDeptId(30L);
        when(supplierService.validateSupplier(eq(100L)))
                .thenReturn(new ErpSupplierDO().setId(100L).setDeptId(10L));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(100L))))
                .thenReturn(Collections.emptyMap());
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildAllDeptPermission());
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(buildDept(10L, "采购一部", 0L,
                        CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                    PURCHASE_IN_SUPPLIER_DEPT_NOT_ALLOWED);
        }
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_withOrderId_carriesSupplierAndOrderNo() {
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);
        reqVO.setOrderId(50L);

        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L).setNo("CGDD001").setSupplierId(999L).setDeptId(88L));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(purchaseInMapper.selectListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(new ErpPurchaseInDO().setId(10L).setOrderId(50L)
                        .setStatus(ErpAuditStatus.PROCESS.getStatus())));
        Map<Long, BigDecimal> generatedInCountMap = new HashMap<>();
        generatedInCountMap.put(101L, new BigDecimal("3"));
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(generatedInCountMap);

        purchaseInService.createPurchaseIn(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).insert(captor.capture());
        ErpPurchaseInDO inserted = captor.getValue();
        assertEquals("CGDD001", inserted.getOrderNo());
        assertEquals(Long.valueOf(999L), inserted.getSupplierId());
        assertEquals(Long.valueOf(88L), inserted.getDeptId());
        // 有 orderId，触发更新订单入库数量
        verify(purchaseOrderService).updatePurchaseOrderInCount(eq(50L), any());
    }

    @Test
    public void testCreatePurchaseIn_withOrderIdCountsAllGeneratedIn() {
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);
        reqVO.setOrderId(50L);

        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L).setNo("CGDD001").setSupplierId(999L).setDeptId(88L));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(purchaseInMapper.selectListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(new ErpPurchaseInDO().setId(10L).setOrderId(50L)
                        .setStatus(ErpAuditStatus.PROCESS.getStatus())));
        Map<Long, BigDecimal> inCountMap = new HashMap<>();
        inCountMap.put(101L, new BigDecimal("10"));
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(inCountMap);

        purchaseInService.createPurchaseIn(reqVO);

        verify(purchaseInMapper).selectListByOrderId(eq(50L));
        verify(purchaseInMapper, never()).selectListByOrderIdAndStatus(eq(50L), eq(ErpAuditStatus.APPROVE.getStatus()));
        verify(purchaseOrderService).updatePurchaseOrderInCount(eq(50L), eq(inCountMap));
    }

    @Test
    public void testCreatePurchaseIn_countZero_throwException() {
        ErpPurchaseInSaveReqVO.Item invalid = buildItem(200L, BigDecimal.ZERO, new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(invalid);

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_ITEM_COUNT_POSITIVE);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_priceNegative_throwException() {
        ErpPurchaseInSaveReqVO.Item invalid = buildItem(200L, new BigDecimal("5"), new BigDecimal("-1"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(invalid);

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_ITEM_PRICE_POSITIVE);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_nonGiftZeroPrice_throwException() {
        ErpPurchaseInSaveReqVO.Item invalid = buildItem(200L, new BigDecimal("5"), BigDecimal.ZERO);
        invalid.setGift(false);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(invalid);

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_ITEM_PRICE_POSITIVE);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_giftForcesZeroPrice() {
        ErpPurchaseInSaveReqVO.Item gift = buildItem(200L, new BigDecimal("5"), new BigDecimal("99"));
        gift.setGift(true);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(gift);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);

        purchaseInService.createPurchaseIn(reqVO);

        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(itemsCaptor.capture());
        ErpPurchaseInItemDO inserted = itemsCaptor.getValue().get(0);
        assertTrue(Boolean.TRUE.equals(inserted.getGift()));
        assertEquals(0, inserted.getProductPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, inserted.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testCreatePurchaseIn_noExists_throwException() {
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        // 已存在同号入库单
        when(purchaseInMapper.selectByNo(any())).thenReturn(new ErpPurchaseInDO().setId(99L));

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_NO_EXISTS);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_wholeQtyOverridesCount() {
        // wholeQty=10, packageQty=2 → count 应被覆盖为 20
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), new BigDecimal("5"));
        item.setWholeQty(10);
        item.setPackageQty(2);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);

        purchaseInService.createPurchaseIn(reqVO);

        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(itemsCaptor.capture());
        List<ErpPurchaseInItemDO> items = itemsCaptor.getValue();
        assertEquals(1, items.size());
        // wholeQty(10) × packageQty(2) = 20，而非 VO 传的 3
        assertEquals(0, items.get(0).getCount().compareTo(new BigDecimal("20")));
        assertEquals(Integer.valueOf(2), items.get(0).getPackageQty());
        assertEquals(Integer.valueOf(10), items.get(0).getWholeQty());
    }

    // ========== updatePurchaseIn ==========

    @Test
    public void testUpdatePurchaseIn_success() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpPurchaseInItemDO oldItem = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(100L).setWarehouseId(10L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN)
                .setTotalPrice(BigDecimal.TEN);
        ErpPurchaseInItemDO deletedItem = new ErpPurchaseInItemDO()
                .setId(12L).setInId(10L).setProductId(300L).setWarehouseId(10L)
                .setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("20"))
                .setTotalPrice(new BigDecimal("40"));
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(100L).setUnitId(1L),
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(warehouseService.validPurchaseWarehouseList(any())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(10L)));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(10L,
                new ErpWarehouseDO().setId(10L)));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Arrays.asList(oldItem, deletedItem));

        ErpPurchaseInSaveReqVO.Item updateItem = buildItem(100L, new BigDecimal("3"), new BigDecimal("10"));
        updateItem.setId(11L);
        updateItem.setOperation("update");
        ErpPurchaseInSaveReqVO.Item insertItem = buildItem(200L, new BigDecimal("5"), new BigDecimal("6"));
        insertItem.setOperation("insert");
        ErpPurchaseInSaveReqVO.Item deleteItem = new ErpPurchaseInSaveReqVO.Item();
        deleteItem.setId(12L);
        deleteItem.setOperation("delete");
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(updateItem, insertItem, deleteItem);
        reqVO.setId(10L);

        purchaseInService.updatePurchaseIn(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> updateCaptor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(updateCaptor.capture());
        assertEquals(0, new BigDecimal("8").compareTo(updateCaptor.getValue().getTotalCount()));
        assertEquals(0, new BigDecimal("60").compareTo(updateCaptor.getValue().getTotalProductPrice()));
        ArgumentCaptor<List<ErpPurchaseInItemDO>> insertCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(insertCaptor.capture());
        assertEquals(1, insertCaptor.getValue().size());
        assertEquals(200L, insertCaptor.getValue().get(0).getProductId());
        ArgumentCaptor<List<ErpPurchaseInItemDO>> updateItemCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).updateBatch(updateItemCaptor.capture());
        assertEquals(1, updateItemCaptor.getValue().size());
        assertEquals(11L, updateItemCaptor.getValue().get(0).getId());
        verify(purchaseInItemMapper).deleteByIds(eq(Collections.singletonList(12L)));
        verify(purchaseInItemMapper, never()).deleteByInId(anyLong());
    }

    @Test
    public void testUpdatePurchaseIn_emptyItems_keepsExistingItems() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpPurchaseInItemDO oldItem = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(100L).setWarehouseId(10L)
                .setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("10"))
                .setTotalPrice(new BigDecimal("20"));
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(oldItem));

        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO();
        reqVO.setId(10L);
        reqVO.setItems(Collections.emptyList());

        purchaseInService.updatePurchaseIn(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> updateCaptor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(updateCaptor.capture());
        assertEquals(0, new BigDecimal("2").compareTo(updateCaptor.getValue().getTotalCount()));
        assertEquals(0, new BigDecimal("20").compareTo(updateCaptor.getValue().getTotalProductPrice()));
        verify(purchaseInItemMapper, never()).deleteByInId(anyLong());
        verify(purchaseInItemMapper, never()).deleteByIds(anyList());
        verify(purchaseInItemMapper, never()).insertBatch(anyList());
        verify(purchaseInItemMapper, never()).updateBatch(anyList());
    }

    @Test
    public void testUpdatePurchaseIn_deleteAllItems_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpPurchaseInItemDO oldItem = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(100L).setWarehouseId(10L)
                .setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("10"))
                .setTotalPrice(new BigDecimal("20"));
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(oldItem));

        ErpPurchaseInSaveReqVO.Item deleteItem = new ErpPurchaseInSaveReqVO.Item();
        deleteItem.setId(11L);
        deleteItem.setOperation("delete");
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(deleteItem);
        reqVO.setId(10L);

        assertServiceException(() -> purchaseInService.updatePurchaseIn(reqVO),
                PURCHASE_IN_SUBMIT_ITEMS_REQUIRED);
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
        verify(purchaseInItemMapper, never()).deleteByIds(anyList());
    }

    @Test
    public void testUpdatePurchaseIn_invalidOperation_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, BigDecimal.ONE, BigDecimal.TEN);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);
        reqVO.setId(10L);

        assertServiceException(() -> purchaseInService.updatePurchaseIn(reqVO),
                PURCHASE_IN_ITEM_OPERATION_INVALID);
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseIn_deleteMissingItem_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());
        ErpPurchaseInSaveReqVO.Item deleteItem = new ErpPurchaseInSaveReqVO.Item();
        deleteItem.setId(99L);
        deleteItem.setOperation("delete");
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(deleteItem);
        reqVO.setId(10L);

        assertServiceException(() -> purchaseInService.updatePurchaseIn(reqVO),
                PURCHASE_IN_ITEM_UPDATE_NOT_EXISTS);
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseIn_notExists_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(null);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO();
        reqVO.setId(10L);

        assertServiceException(() -> purchaseInService.updatePurchaseIn(reqVO),
                PURCHASE_IN_NOT_EXISTS);
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseIn_alreadyApproved_throwException() {
        ErpPurchaseInDO approved = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(approved);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO();
        reqVO.setId(10L);

        assertServiceException(() -> purchaseInService.updatePurchaseIn(reqVO),
                PURCHASE_IN_UPDATE_FAIL_APPROVE, "CGRK001");
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    // ========== getPurchaseIn ==========

    @Test
    public void testGetPurchaseIn_dataPermissionDenied_throwException() {
        ErpPurchaseInDO hidden = new ErpPurchaseInDO().setId(10L).setNo("CGRK001");
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(null, hidden);

        assertServiceException(() -> purchaseInService.getPurchaseIn(10L),
                PURCHASE_IN_DATA_PERMISSION_DENIED);
    }

    // ========== batchUpdatePurchaseInItems ==========

    @Test
    public void testBatchUpdatePurchaseInItems_success() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(100L).setWarehouseId(11L).setDeptId(101L)
                .setGift(false);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(purchaseInvoiceItemMapper.selectListBySourceInId(eq(10L))).thenReturn(Collections.emptyList());
        when(stockInBillService.getPurchaseInSourceItemList(eq(10L))).thenReturn(Collections.emptyList());
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(30L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(30L).setDeptId(300L)));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setWarehouseId(30L);
        reqVO.setDeptId(300L);

        purchaseInService.batchUpdatePurchaseInItems(reqVO);

        verify(stockService).ensureStockExists(eq(100L), eq(30L));
        ArgumentCaptor<ErpPurchaseInItemDO> itemCaptor = ArgumentCaptor.forClass(ErpPurchaseInItemDO.class);
        verify(purchaseInItemMapper).updateById(itemCaptor.capture());
        assertEquals(Long.valueOf(1L), itemCaptor.getValue().getId());
        assertEquals(Long.valueOf(30L), itemCaptor.getValue().getWarehouseId());
        assertEquals(Long.valueOf(300L), itemCaptor.getValue().getDeptId());
        verify(operateLogService).recordUpdate(eq("采购入库"), eq(10L), eq("CGRK001"));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_ensureStockOnceForSameProductWarehouse() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpPurchaseInItemDO normalItem = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(100L).setWarehouseId(11L).setDeptId(101L)
                .setGift(false);
        ErpPurchaseInItemDO giftItem = new ErpPurchaseInItemDO()
                .setId(2L).setInId(10L).setProductId(100L).setWarehouseId(12L).setDeptId(102L)
                .setGift(true);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Arrays.asList(normalItem, giftItem));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(purchaseInvoiceItemMapper.selectListBySourceInId(eq(10L))).thenReturn(Collections.emptyList());
        when(stockInBillService.getPurchaseInSourceItemList(eq(10L))).thenReturn(Collections.emptyList());
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(30L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(30L).setDeptId(300L)));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Arrays.asList(1L, 2L));
        reqVO.setWarehouseId(30L);
        reqVO.setDeptId(300L);

        purchaseInService.batchUpdatePurchaseInItems(reqVO);

        verify(stockService, times(1)).ensureStockExists(eq(100L), eq(30L));
        verify(purchaseInItemMapper, times(2)).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_fieldRequired_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FIELD_REQUIRED);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_approved_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(300L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_APPROVE, "CGRK001");

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_adjusted_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false).setAdjusted(true)));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(101L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_ADJUST);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_hasReturn_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false)));
        Map<Long, BigDecimal> returnCountMap = new HashMap<>();
        returnCountMap.put(1L, BigDecimal.ONE);
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(returnCountMap);
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(300L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_RETURN);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_hasTransferOut_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false)));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        Map<Long, BigDecimal> transferOutCountMap = new HashMap<>();
        transferOutCountMap.put(1L, BigDecimal.ONE);
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(transferOutCountMap);
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(101L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_TRANSFER_OUT);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_hasSaleCart_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false)));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        Map<Long, BigDecimal> saleCartCountMap = new HashMap<>();
        saleCartCountMap.put(1L, BigDecimal.ONE);
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(saleCartCountMap);
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(101L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_SALE_CART);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_hasInvoice_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false)));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(purchaseInvoiceItemMapper.selectListBySourceInId(eq(10L)))
                .thenReturn(Collections.singletonList(new ErpPurchaseInvoiceItemDO()));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(101L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_INVOICE);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_hasStockInBill_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false)));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(purchaseInvoiceItemMapper.selectListBySourceInId(eq(10L))).thenReturn(Collections.emptyList());
        when(stockInBillService.getPurchaseInSourceItemList(eq(10L))).thenReturn(Collections.singletonList(
                new ErpStockInBillItemDO().setSourceItemId(1L)));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(101L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_STOCK_IN_BILL);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_deptNotAllowed_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false)));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(purchaseInvoiceItemMapper.selectListBySourceInId(eq(10L))).thenReturn(Collections.emptyList());
        when(stockInBillService.getPurchaseInSourceItemList(eq(10L))).thenReturn(Collections.emptyList());
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(11L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(11L).setDeptId(101L)));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setDeptId(999L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
    }

    @Test
    public void testBatchUpdatePurchaseInItems_duplicateAfterWarehouseChange_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Arrays.asList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L).setProductId(100L)
                        .setWarehouseId(11L).setDeptId(101L).setGift(false),
                new ErpPurchaseInItemDO().setId(2L).setInId(10L).setProductId(100L)
                        .setWarehouseId(30L).setDeptId(300L).setGift(false)));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(purchaseInvoiceItemMapper.selectListBySourceInId(eq(10L))).thenReturn(Collections.emptyList());
        when(stockInBillService.getPurchaseInSourceItemList(eq(10L))).thenReturn(Collections.emptyList());
        when(warehouseService.validPurchaseWarehouseList(eq(Collections.singleton(30L))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(30L).setDeptId(300L)));
        ErpPurchaseInItemBatchUpdateReqVO reqVO = new ErpPurchaseInItemBatchUpdateReqVO();
        reqVO.setInId(10L);
        reqVO.setItemIds(Collections.singletonList(1L));
        reqVO.setWarehouseId(30L);
        reqVO.setDeptId(300L);

        assertServiceException(() -> purchaseInService.batchUpdatePurchaseInItems(reqVO),
                PURCHASE_IN_ITEM_DUPLICATE, "100");

        verify(purchaseInItemMapper, never()).updateById(any(ErpPurchaseInItemDO.class));
        verify(stockService, never()).ensureStockExists(anyLong(), anyLong());
    }

    // ========== updatePurchaseInStatus ==========

    @Test
    public void testUpdatePurchaseInStatus_approveSuccess_triggersStockAndLastPriceAndVoucher() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setOrderId(50L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(10L)
                .setCount(new BigDecimal("5")).setProductPrice(new BigDecimal("10"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        Map<Long, ErpWarehouseDO> warehouseMap = Collections.singletonMap(10L,
                new ErpWarehouseDO().setId(10L).setStockBillEnabled(false));
        when(warehouseService.getWarehouseMap(any())).thenReturn(warehouseMap);

        // 开账启用，触发凭证生成
        when(bookOpenService.isVoucherTypeEnabled(any(), eq(ErpVoucherTypeEnum.PURCHASE.getType())))
                .thenReturn(true);
        when(supplierService.validateSupplier(eq(99L)))
                .thenReturn(new ErpSupplierDO().setId(99L).setName("芋道供应商"));
        when(autoVoucherBuilder.buildPurchaseInItems(any(), eq("芋道供应商")))
                .thenReturn(Collections.singletonList(new ErpVoucherItemDO()));
        when(purchaseInMapper.selectListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(new ErpPurchaseInDO().setId(10L).setOrderId(50L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        Map<Long, BigDecimal> inCountMap = Collections.singletonMap(1000L, new BigDecimal("5"));
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(eq(Collections.singletonList(10L))))
                .thenReturn(inCountMap);

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        // 库存流水：bizType=PURCHASE_IN（正向）
        ArgumentCaptor<ErpStockRecordCreateReqBO> recordCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService).createStockRecord(recordCaptor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.PURCHASE_IN.getType(), recordCaptor.getValue().getBizType());
        assertEquals(0, recordCaptor.getValue().getCount().compareTo(new BigDecimal("5")));
        assertEquals(0, recordCaptor.getValue().getUnitPrice().compareTo(new BigDecimal("10")));
        assertEquals(LocalDateTime.of(2026, 5, 20, 10, 0, 0), recordCaptor.getValue().getBizDate());
        // 回写 lastPurchasePrice
        verify(productService).updateProductLastPurchasePrice(eq(200L), eq(new BigDecimal("10")));
        // 凭证生成
        verify(voucherService).createVoucherFromBiz(eq(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType()),
                eq(10L), eq("CGRK001"), any(), any(), any(String.class), anyList());
        verify(purchaseOrderService).updatePurchaseOrderInCount(eq(50L), eq(inCountMap));
        verify(stockInBillService).createFromPurchaseIn(eq(existing), eq(Collections.emptyList()));
    }

    @Test
    public void testCreatePurchaseInDraft_rejectsEmptyItems() {
        ErpPurchaseInDraftCreateReqVO reqVO = new ErpPurchaseInDraftCreateReqVO();
        reqVO.setItems(Collections.emptyList());

        assertServiceException(() -> purchaseInService.createPurchaseInDraft(reqVO),
                PURCHASE_IN_SUBMIT_ITEMS_REQUIRED);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
        verify(purchaseInItemMapper, never()).insertBatch(anyList());
    }

    @Test
    public void testUpdatePurchaseInDraft_emptyItems_keepsExistingItems() {
        ErpPurchaseInDO draft = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(0)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        ErpPurchaseInItemDO oldItem = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(200L).setWarehouseId(10L)
                .setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("10"))
                .setTotalPrice(new BigDecimal("20"));
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(draft);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(oldItem));
        ErpPurchaseInDraftUpdateReqVO reqVO = new ErpPurchaseInDraftUpdateReqVO();
        reqVO.setId(10L);
        reqVO.setItems(Collections.emptyList());

        purchaseInService.updatePurchaseInDraft(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(captor.capture());
        ErpPurchaseInDO updateObj = captor.getValue();
        assertEquals(0, updateObj.getTotalCount().compareTo(new BigDecimal("2")));
        assertEquals(0, updateObj.getTotalProductPrice().compareTo(new BigDecimal("20")));
        assertEquals(0, updateObj.getTotalTaxPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, updateObj.getTotalPrice().compareTo(new BigDecimal("20")));
        verify(purchaseInItemMapper, never()).deleteByInId(anyLong());
        verify(purchaseInItemMapper, never()).deleteByIds(anyList());
        verify(purchaseInItemMapper, never()).insertBatch(anyList());
        verify(purchaseInItemMapper, never()).updateBatch(anyList());
    }

    @Test
    public void testUpdatePurchaseInDraft_deleteAllItemsByOperation() {
        ErpPurchaseInDO draft = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(0)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        ErpPurchaseInItemDO oldItem = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(200L).setWarehouseId(10L)
                .setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("10"))
                .setTotalPrice(new BigDecimal("20"));
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(draft);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(oldItem));
        ErpPurchaseInSaveReqVO.Item deleteItem = new ErpPurchaseInSaveReqVO.Item();
        deleteItem.setId(11L);
        deleteItem.setOperation("delete");
        ErpPurchaseInDraftUpdateReqVO reqVO = new ErpPurchaseInDraftUpdateReqVO();
        reqVO.setId(10L);
        reqVO.setItems(Collections.singletonList(deleteItem));

        purchaseInService.updatePurchaseInDraft(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getTotalCount().compareTo(BigDecimal.ZERO));
        assertEquals(0, captor.getValue().getTotalProductPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, captor.getValue().getTotalPrice().compareTo(BigDecimal.ZERO));
        verify(purchaseInItemMapper).deleteByIds(eq(Collections.singletonList(11L)));
        verify(purchaseInItemMapper, never()).deleteByInId(anyLong());
    }

    @Test
    public void testCreatePurchaseInDraft_allowsNonGiftZeroPrice() {
        ErpPurchaseInDraftCreateReqVO reqVO = new ErpPurchaseInDraftCreateReqVO();
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, BigDecimal.ONE, BigDecimal.ZERO);
        item.setGift(false);
        reqVO.setItems(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);

        purchaseInService.createPurchaseInDraft(reqVO);

        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(itemsCaptor.capture());
        assertEquals(0, itemsCaptor.getValue().get(0).getProductPrice().compareTo(BigDecimal.ZERO));
        assertEquals(Boolean.FALSE, itemsCaptor.getValue().get(0).getGift());
    }

    @Test
    public void testSubmitPurchaseIn_movesDraftToProcess() {
        ErpPurchaseInDO draft = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .setStatus(0);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(200L).setProductUnitId(1L)
                .setWarehouseId(10L).setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(draft);
        when(purchaseInItemMapper.selectListByInId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.updateByIdAndStatus(eq(10L), eq(0), any()))
                .thenReturn(1);

        purchaseInService.submitPurchaseIn(10L);

        verify(supplierService).validateSupplier(99L);
        verify(purchaseInMapper).updateByIdAndStatus(eq(10L), eq(0),
                org.mockito.ArgumentMatchers.argThat(update ->
                        ErpAuditStatus.PROCESS.getStatus().equals(update.getStatus())));
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    public void testSubmitPurchaseIn_disabledSupplierDept_throwException() {
        Long loginUserId = 104L;
        ErpPurchaseInDO draft = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L).setDeptId(30L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .setStatus(0);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(200L).setProductUnitId(1L)
                .setWarehouseId(10L).setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(draft);
        when(purchaseInItemMapper.selectListByInId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(supplierService.validateSupplier(eq(99L)))
                .thenReturn(new ErpSupplierDO().setId(99L).setDeptId(30L));
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(99L))))
                .thenReturn(Collections.emptyMap());
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("system_dept")))
                .thenReturn(buildAllDeptPermission());
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(30L)))))
                .thenReturn(Collections.singletonList(buildDept(30L, "采购三部", 0L,
                        CommonStatusEnum.DISABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            assertServiceException(() -> purchaseInService.submitPurchaseIn(10L),
                    PURCHASE_IN_SUPPLIER_DEPT_NOT_ALLOWED);
        }
        verify(purchaseInMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testUpdateAndSubmitPurchaseInDraft_persistsCurrentItemsBeforeSubmit() {
        ErpPurchaseInDO draft = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .setStatus(0);
        ErpPurchaseInDraftUpdateReqVO reqVO = new ErpPurchaseInDraftUpdateReqVO();
        reqVO.setId(10L);
        reqVO.setSupplierId(99L);
        reqVO.setInTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        ErpPurchaseInSaveReqVO.Item insertItem = buildItem(200L, BigDecimal.ONE, BigDecimal.TEN);
        insertItem.setOperation("insert");
        reqVO.setItems(Collections.singletonList(insertItem));
        ErpPurchaseInItemDO persistedItem = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(200L).setProductUnitId(1L)
                .setWarehouseId(10L).setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(draft);
        when(purchaseInItemMapper.selectListByInId(eq(10L)))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(persistedItem));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.emptyMap());
        when(purchaseInMapper.updateByIdAndStatus(eq(10L), eq(0), any()))
                .thenReturn(1);

        purchaseInService.updateAndSubmitPurchaseInDraft(reqVO);

        InOrder inOrder = inOrder(purchaseInItemMapper, purchaseInMapper);
        inOrder.verify(purchaseInItemMapper).insertBatch(anyList());
        inOrder.verify(purchaseInMapper).updateByIdAndStatus(eq(10L), eq(0),
                org.mockito.ArgumentMatchers.argThat(update ->
                        ErpAuditStatus.PROCESS.getStatus().equals(update.getStatus())));
        verify(purchaseInItemMapper, never()).deleteByInId(anyLong());
    }

    @Test
    public void testSubmitPurchaseIn_rejectsEmptyItems() {
        ErpPurchaseInDO draft = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .setStatus(0);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(draft);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());

        assertServiceException(() -> purchaseInService.submitPurchaseIn(10L),
                PURCHASE_IN_SUBMIT_ITEMS_REQUIRED);
        verify(purchaseInMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testSubmitPurchaseIn_rejectsNonGiftZeroPrice() {
        ErpPurchaseInDO draft = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .setStatus(0);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(11L).setInId(10L).setProductId(200L).setProductUnitId(1L)
                .setWarehouseId(10L).setCount(BigDecimal.ONE).setProductPrice(BigDecimal.ZERO)
                .setGift(false);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(draft);
        when(purchaseInItemMapper.selectListByInId(eq(10L)))
                .thenReturn(Collections.singletonList(item));

        assertServiceException(() -> purchaseInService.submitPurchaseIn(10L),
                PURCHASE_IN_ITEM_PRICE_POSITIVE);
        verify(purchaseInMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testSubmitPurchaseIn_rejectsNonDraft() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus()));

        assertServiceException(() -> purchaseInService.submitPurchaseIn(10L),
                PURCHASE_IN_SUBMIT_FAIL);
        verify(purchaseInMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInStatus_legacyStockBillWarehouse_postsImmediately() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(10L)
                .setCount(new BigDecimal("5")).setProductPrice(new BigDecimal("10"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        org.mockito.Mockito.lenient().when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(10L,
                new ErpWarehouseDO().setId(10L).setStockBillEnabled(true)));

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        verify(stockRecordService).createStockRecord(any());
        verify(stockInBillService,never()).createFromPurchaseIn(any(),any());
        verify(productService).updateProductLastPurchasePrice(eq(200L), eq(new BigDecimal("10")));
    }

    @Test
    public void testUpdatePurchaseInStatus_approveWhenAlreadyApproved_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                ErpAuditStatus.APPROVE.getStatus()), PURCHASE_IN_APPROVE_FAIL);
        verify(purchaseInMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInStatus_processFailWhenPaymentExists_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(new BigDecimal("100"));
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                ErpAuditStatus.PROCESS.getStatus()), PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT);
        verify(purchaseInMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInStatus_processFailWhenVoucherApproved_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        // 关联凭证已审核
        ErpVoucherDO approvedVoucher = new ErpVoucherDO()
                .setId(500L).setVoucherNo("记-202605-000001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(eq(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType()), eq(10L)))
                .thenReturn(Collections.singletonList(approvedVoucher));

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                        ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED, "记-202605-000001");
        verify(voucherMapper, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdatePurchaseInStatus_processSuccess_deletesUnauditedVoucher() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        // 关联凭证未审核 → 应被自动删除
        ErpVoucherDO unauditedVoucher = new ErpVoucherDO()
                .setId(500L).setVoucherNo("记-202605-000001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(eq(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType()), eq(10L)))
                .thenReturn(Collections.singletonList(unauditedVoucher));
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.APPROVE.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper).deleteById(eq(500L));
        verify(voucherItemMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    public void testUpdatePurchaseInStatus_approveSkipsVoucherWhenBookDisabled() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());
        // 未开账 → 不生成凭证
        when(bookOpenService.isVoucherTypeEnabled(any(), eq(ErpVoucherTypeEnum.PURCHASE.getType())))
                .thenReturn(false);

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(any(), any(), any(), any(), any(), any(), any());
        verify(autoVoucherBuilder, never()).buildPurchaseInItems(any(), any());
        verify(supplierService).validateSupplier(eq(99L));
    }

    @Test
    public void testUpdatePurchaseInStatus_rejectsNonGiftZeroPrice() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(10L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.ZERO).setGift(false);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInItemMapper.selectListByInId(eq(10L)))
                .thenReturn(Collections.singletonList(item));

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(
                10L, ErpAuditStatus.APPROVE.getStatus()), PURCHASE_IN_ITEM_PRICE_POSITIVE);
        verify(purchaseInMapper, never()).updateByIdAndStatus(any(), any(), any());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    public void testUpdatePurchaseInStatus_giftZeroPriceDoesNotOverwriteLastPurchasePrice() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001")
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(10L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.ZERO).setGift(true);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInItemMapper.selectListByInId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        verify(productService, never()).updateProductLastPurchasePrice(anyLong(), any());
        verify(stockRecordService).createStockRecord(any());
    }

    @Test
    public void testUpdatePurchaseInStatus_approveOptimisticLockFail_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                ErpAuditStatus.APPROVE.getStatus()), PURCHASE_IN_APPROVE_FAIL);
        verify(stockRecordService, never()).createStockRecord(any());
    }

    // ========== updatePurchaseInPaymentPrice ==========

    @Test
    public void testUpdatePurchaseInPaymentPrice_success() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setPaymentPrice(BigDecimal.ZERO).setTotalPrice(new BigDecimal("100")));

        purchaseInService.updatePurchaseInPaymentPrice(10L, new BigDecimal("50"));

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getPaymentPrice().compareTo(new BigDecimal("50")));
    }

    @Test
    public void testUpdatePurchaseInPaymentPrice_unchanged_skipUpdate() {
        BigDecimal payment = new BigDecimal("50");
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setPaymentPrice(payment).setTotalPrice(new BigDecimal("100")));

        purchaseInService.updatePurchaseInPaymentPrice(10L, payment);

        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseInPaymentPrice_exceed_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setPaymentPrice(BigDecimal.ZERO).setTotalPrice(new BigDecimal("100")));

        assertServiceException(() -> purchaseInService.updatePurchaseInPaymentPrice(10L,
                        new BigDecimal("200")),
                PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED, new BigDecimal("200"), new BigDecimal("100"));
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    // ========== deletePurchaseIn ==========

    @Test
    public void testDeletePurchaseIn_success() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseInMapper.selectById(anyLong())).thenReturn(in);

        purchaseInService.deletePurchaseIn(Collections.singletonList(10L));

        verify(purchaseInMapper).deleteById(eq(10L));
        verify(purchaseInItemMapper).deleteByInId(eq(10L));
    }

    @Test
    public void testDeletePurchaseIn_alreadyApproved_throwException() {
        ErpPurchaseInDO approved = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(anyLong())).thenReturn(approved);

        assertServiceException(() -> purchaseInService.deletePurchaseIn(Collections.singletonList(10L)),
                PURCHASE_IN_DELETE_FAIL_APPROVE, "CGRK001");
        verify(purchaseInMapper, never()).deleteById(anyLong());
    }

    @Test
    public void testDeletePurchaseIn_emptyResult_noOp() {
        when(purchaseInMapper.selectById(anyLong())).thenReturn(null);

        purchaseInService.deletePurchaseIn(Collections.singletonList(10L));

        verify(purchaseInMapper, never()).deleteById(anyLong());
        verify(purchaseInItemMapper, never()).deleteByInId(anyLong());
    }

    // ========== getPurchaseIn / validatePurchaseIn ==========

    @Test
    public void testGetPurchaseIn_returnsExisting() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(in);

        assertSame(in, purchaseInService.getPurchaseIn(10L));
    }

    @Test
    public void testValidatePurchaseIn_notExists_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(null);

        assertServiceException(() -> purchaseInService.validatePurchaseIn(10L),
                PURCHASE_IN_NOT_EXISTS);
    }

    @Test
    public void testValidatePurchaseIn_notApproved_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        assertServiceException(() -> purchaseInService.validatePurchaseIn(10L),
                PURCHASE_IN_NOT_APPROVE);
    }

    @Test
    public void testValidatePurchaseIn_success() {
        ErpPurchaseInDO approved = new ErpPurchaseInDO().setId(10L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(approved);

        ErpPurchaseInDO result = purchaseInService.validatePurchaseIn(10L);

        assertNotNull(result);
        assertEquals(Long.valueOf(10L), result.getId());
    }

    // ========== getPurchaseInPage ==========

    @Test
    public void testGetPurchaseInPage_delegatesToMapper() {
        ErpPurchaseInPageReqVO reqVO = new ErpPurchaseInPageReqVO();
        PageResult<ErpPurchaseInDO> page = new PageResult<>(
                Collections.singletonList(new ErpPurchaseInDO().setId(1L)), 1L);
        when(purchaseInMapper.selectPage(eq(reqVO))).thenReturn(page);
        when(financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(any(), eq(11)))
                .thenReturn(Collections.singletonMap(1L, new BigDecimal("560")));

        assertSame(page, purchaseInService.getPurchaseInPage(reqVO));
        assertEquals(new BigDecimal("560"), page.getList().get(0).getPaymentPrice());
    }

    // ========== 子表查询方法 ==========

    @Test
    public void testGetPurchaseInItemListByInId_delegatesToMapper() {
        List<ErpPurchaseInItemDO> mocked = Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(mocked);

        assertSame(mocked, purchaseInService.getPurchaseInItemListByInId(10L));
    }

    @Test
    public void testGetPurchaseInItemPage_delegatesToMapper() {
        ErpPurchaseInItemPageReqVO reqVO = new ErpPurchaseInItemPageReqVO();
        reqVO.setInId(10L);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO().setId(10L));
        PageResult<ErpPurchaseInItemDO> page = new PageResult<>(
                Collections.singletonList(new ErpPurchaseInItemDO().setId(1L).setInId(10L)), 1L);
        when(purchaseInItemMapper.selectPageByInId(eq(reqVO))).thenReturn(page);

        assertSame(page, purchaseInService.getPurchaseInItemPage(reqVO));
        verify(purchaseInItemMapper).selectPageByInId(eq(reqVO));
    }

    @Test
    public void testGetPurchaseInItemPage_missingIn_throwException() {
        ErpPurchaseInItemPageReqVO reqVO = new ErpPurchaseInItemPageReqVO();
        reqVO.setInId(10L);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(null);

        assertServiceException(() -> purchaseInService.getPurchaseInItemPage(reqVO), PURCHASE_IN_NOT_EXISTS);
        verify(purchaseInItemMapper, never()).selectPageByInId(any());
    }

    @Test
    public void testGetPurchaseInItemListByInIds_emptyInput_returnsEmpty() {
        List<ErpPurchaseInItemDO> result = purchaseInService.getPurchaseInItemListByInIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(purchaseInItemMapper, never()).selectListByInIds(any());
    }

    @Test
    public void testGetTransferOutCountMapByInItemIds_delegatesToMapper() {
        List<Long> itemIds = Arrays.asList(1L, 2L);
        Map<Long, BigDecimal> movedCountMap = new HashMap<>();
        movedCountMap.put(1L, new BigDecimal("2.5"));
        movedCountMap.put(2L, new BigDecimal("3"));
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(eq(itemIds)))
                .thenReturn(movedCountMap);

        Map<Long, BigDecimal> result = purchaseInService.getTransferOutCountMapByInItemIds(itemIds);

        assertSame(movedCountMap, result);
        verify(stockMoveItemMapper).selectMovedCountMapBySourceInItemIds(eq(itemIds));
    }

    @Test
    public void testGetTransferOutCountMapByInItemIds_emptyInput_returnsEmpty() {
        Map<Long, BigDecimal> result = purchaseInService.getTransferOutCountMapByInItemIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(stockMoveItemMapper, never()).selectMovedCountMapBySourceInItemIds(any());
    }

    // ========== createTransferOutFromPurchaseIn ==========

    @Test
    public void testCreateTransferOutFromPurchaseIn_groupsBySourceWarehouse() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setDeptId(9L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        List<ErpPurchaseInItemDO> sourceItems = Arrays.asList(
                new ErpPurchaseInItemDO().setId(101L).setInId(10L).setWarehouseId(1L).setProductId(1001L)
                        .setCount(new BigDecimal("5")).setProductPrice(new BigDecimal("10")),
                new ErpPurchaseInItemDO().setId(102L).setInId(10L).setWarehouseId(2L).setProductId(1002L)
                        .setCount(new BigDecimal("6")).setProductPrice(new BigDecimal("20")),
                new ErpPurchaseInItemDO().setId(103L).setInId(10L).setWarehouseId(1L).setProductId(1003L)
                        .setCount(new BigDecimal("7")).setProductPrice(new BigDecimal("30")));
        when(purchaseInMapper.selectById(10L)).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectListByInId(10L)).thenReturn(sourceItems);
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any())).thenReturn(Collections.emptyMap());
        when(stockMoveService.createStockMove(any())).thenReturn(10001L, 10002L);

        ErpPurchaseInCreateTransferOutReqVO reqVO = buildTransferOutReqVO(10L,
                buildTransferOutItem(101L, 11L, "2"),
                buildTransferOutItem(102L, 12L, "3"),
                buildTransferOutItem(103L, 13L, "4"));
        ErpPurchaseInCreateTransferOutRespVO result = purchaseInService.createTransferOutFromPurchaseIn(reqVO);

        ArgumentCaptor<ErpStockMoveSaveReqVO> captor = ArgumentCaptor.forClass(ErpStockMoveSaveReqVO.class);
        verify(stockMoveService, times(2)).createStockMove(captor.capture());
        List<ErpStockMoveSaveReqVO> moveRequests = captor.getAllValues();
        assertEquals(Arrays.asList(10001L, 10002L), result.getIds());
        assertEquals(Long.valueOf(10001L), result.getId());
        assertEquals(2, moveRequests.get(0).getItems().size());
        assertTrue(moveRequests.get(0).getItems().stream()
                .allMatch(item -> Long.valueOf(1L).equals(item.getFromWarehouseId())));
        assertEquals(1, moveRequests.get(1).getItems().size());
        assertEquals(Long.valueOf(2L), moveRequests.get(1).getItems().get(0).getFromWarehouseId());
        assertTrue(moveRequests.stream().allMatch(move -> Long.valueOf(10L).equals(move.getSourceId())));
    }

    @Test
    public void testCreateTransferOutFromPurchaseIn_rejectsCumulativeExceededCount() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        ErpPurchaseInItemDO sourceItem = new ErpPurchaseInItemDO().setId(101L).setInId(10L)
                .setWarehouseId(1L).setProductId(1001L).setCount(new BigDecimal("5"));
        when(purchaseInMapper.selectById(10L)).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectListByInId(10L)).thenReturn(Collections.singletonList(sourceItem));
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any()))
                .thenReturn(Collections.singletonMap(101L, new BigDecimal("3")));
        ErpPurchaseInCreateTransferOutReqVO reqVO = buildTransferOutReqVO(10L,
                buildTransferOutItem(101L, 11L, "2.01"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> purchaseInService.createTransferOutFromPurchaseIn(reqVO));
        assertEquals(PURCHASE_IN_TRANSFER_OUT_EXCEED_AVAILABLE.getCode(), exception.getCode());
        verify(stockMoveService, never()).createStockMove(any());
    }

    @Test
    public void testCreateTransferOutFromPurchaseIn_propagatesGroupCreationFailure() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        List<ErpPurchaseInItemDO> sourceItems = Arrays.asList(
                new ErpPurchaseInItemDO().setId(101L).setInId(10L).setWarehouseId(1L).setProductId(1001L)
                        .setCount(new BigDecimal("5")),
                new ErpPurchaseInItemDO().setId(102L).setInId(10L).setWarehouseId(2L).setProductId(1002L)
                        .setCount(new BigDecimal("6")));
        when(purchaseInMapper.selectById(10L)).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectListByInId(10L)).thenReturn(sourceItems);
        when(stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(any())).thenReturn(Collections.emptyMap());
        when(stockMoveService.createStockMove(any())).thenReturn(10001L)
                .thenThrow(new IllegalStateException("second group failed"));
        ErpPurchaseInCreateTransferOutReqVO reqVO = buildTransferOutReqVO(10L,
                buildTransferOutItem(101L, 11L, "2"),
                buildTransferOutItem(102L, 12L, "3"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> purchaseInService.createTransferOutFromPurchaseIn(reqVO));

        assertEquals("second group failed", exception.getMessage());
        verify(stockMoveService, times(2)).createStockMove(any());
    }

    private ErpPurchaseInCreateTransferOutReqVO buildTransferOutReqVO(
            Long sourceInId, ErpPurchaseInCreateTransferOutReqVO.Item... items) {
        ErpPurchaseInCreateTransferOutReqVO reqVO = new ErpPurchaseInCreateTransferOutReqVO();
        reqVO.setSourceInId(sourceInId);
        reqVO.setMoveTime(LocalDateTime.of(2026, 7, 22, 10, 0));
        reqVO.setItems(Arrays.asList(items));
        return reqVO;
    }

    private ErpPurchaseInCreateTransferOutReqVO.Item buildTransferOutItem(
            Long sourceInItemId, Long toWarehouseId, String count) {
        ErpPurchaseInCreateTransferOutReqVO.Item item = new ErpPurchaseInCreateTransferOutReqVO.Item();
        item.setSourceInItemId(sourceInItemId);
        item.setToWarehouseId(toWarehouseId);
        item.setCount(new BigDecimal(count));
        return item;
    }

    // ========== getReturnableItemsByInId ==========

    @Test
    public void testGetReturnableItemsByInId_success() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        // 入库项：count=10
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setProductId(200L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("12.34")).setCount(new BigDecimal("10"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        // 已退 3
        Map<Long, BigDecimal> returnedMap = new HashMap<>();
        returnedMap.put(100L, new BigDecimal("3"));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any())).thenReturn(returnedMap);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setName("螺丝").setCode("P001")));

        List<ErpPurchaseReturnableItemRespVO> result = purchaseInService.getReturnableItemsByInId(10L);

        assertEquals(1, result.size());
        ErpPurchaseReturnableItemRespVO vo = result.get(0);
        assertEquals(0, vo.getInCount().compareTo(new BigDecimal("10")));
        assertEquals(0, vo.getReturnedCount().compareTo(new BigDecimal("3")));
        // 可退 = 10 - 3 = 7
        assertEquals(0, vo.getReturnableCount().compareTo(new BigDecimal("7")));
        assertEquals("螺丝", vo.getProductName());
        assertEquals("P001", vo.getProductCode());
        assertEquals("CGRK001", vo.getSourceInNo());
    }

    @Test
    public void testGetReturnableItemsByInId_returnableNegativeClampedToZero() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        // 入库 5，已退 8（异常超过）→ returnable 不能为负，应钳制为 0
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setProductId(200L)
                .setCount(new BigDecimal("5"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> returnedMap = new HashMap<>();
        returnedMap.put(100L, new BigDecimal("8"));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any())).thenReturn(returnedMap);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L)));

        List<ErpPurchaseReturnableItemRespVO> result = purchaseInService.getReturnableItemsByInId(10L);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getReturnableCount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testGetReturnableItemsByInId_inNotApproved_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        assertServiceException(() -> purchaseInService.getReturnableItemsByInId(10L),
                PURCHASE_IN_NOT_APPROVE);
    }

    @Test
    public void testGetReturnableItemsByInId_emptyItems_returnsEmpty() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());

        List<ErpPurchaseReturnableItemRespVO> result = purchaseInService.getReturnableItemsByInId(10L);

        assertTrue(result.isEmpty());
        verify(purchaseReturnItemMapper, never()).selectReturnedCountMapBySourceInItemIds(any());
    }

    // ========== getSaleCartableItemsByInId ==========

    @Test
    public void testGetSaleCartableItemsByInId_includesPriceLevelFields() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setProductId(200L).setProductUnitId(1L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("12.34")).setCount(new BigDecimal("10"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> convertedMap = new HashMap<>();
        convertedMap.put(100L, new BigDecimal("3"));
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any())).thenReturn(convertedMap);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setName("螺丝").setCode("P001")
                        .setPurchasePrice(new BigDecimal("9.50"))
                        .setSalePrice(new BigDecimal("17.50"))
                        .setMinPrice(new BigDecimal("11.00"))
                        .setReferencePrice(new BigDecimal("15.00"))
                        .setRetailPrice(new BigDecimal("18.00"))
                        .setLastPurchasePrice(new BigDecimal("8.80"))
                        .setBackupPrice1(new BigDecimal("16.00"))
                        .setWholesalePrice(new BigDecimal("13.00"))
                        .setSharePrice(new BigDecimal("14.00"))));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(200L,
                new ErpProductRespVO().setId(200L).setUnitName("个")
                        .setCustomFields(Collections.singletonMap("orderPriceCustom", new BigDecimal("10.20")))));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(5L,
                new ErpWarehouseDO().setId(5L).setName("销售仓")));
        when(stockService.getStockMap(any(), any())).thenReturn(Collections.singletonMap("200_5",
                new ErpStockDO().setProductId(200L).setWarehouseId(5L).setCostPrice(new BigDecimal("7.25"))));

        List<ErpPurchaseInSaleCartableItemRespVO> result = purchaseInService.getSaleCartableItemsByInId(10L);

        assertEquals(1, result.size());
        ErpPurchaseInSaleCartableItemRespVO vo = result.get(0);
        assertEquals(0, vo.getSaleCartableCount().compareTo(new BigDecimal("7")));
        assertEquals(0, vo.getSalePrice().compareTo(new BigDecimal("17.50")));
        assertEquals(0, vo.getMinPrice().compareTo(new BigDecimal("11.00")));
        assertEquals(0, vo.getProductPurchasePrice().compareTo(new BigDecimal("9.50")));
        assertEquals(0, vo.getCostPrice().compareTo(new BigDecimal("7.25")));
        assertEquals(0, vo.getReferencePrice().compareTo(new BigDecimal("15.00")));
        assertEquals(0, vo.getRetailPrice().compareTo(new BigDecimal("18.00")));
        assertEquals(0, vo.getLastPurchasePrice().compareTo(new BigDecimal("8.80")));
        assertEquals(0, vo.getBackupPrice1().compareTo(new BigDecimal("16.00")));
        assertEquals(0, vo.getWholesalePrice().compareTo(new BigDecimal("13.00")));
        assertEquals(0, vo.getSharePrice().compareTo(new BigDecimal("14.00")));
        assertEquals(new BigDecimal("10.20"), vo.getCustomFields().get("orderPriceCustom"));
        assertEquals("螺丝", vo.getProductName());
        assertEquals("P001", vo.getProductCode());
    }

    @Test
    public void testGetSaleCartableItemPage_includesPriceLevelFields() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setProductId(200L).setProductUnitId(1L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("12.34")).setCount(new BigDecimal("10"));
        when(purchaseInItemMapper.selectSaleCartablePageByInId(any())).thenReturn(
                new PageResult<>(Collections.singletonList(item), 1L));
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.singletonMap(100L, new BigDecimal("3")));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setName("螺丝").setCode("P001")
                        .setRetailPrice(new BigDecimal("18.00"))
                        .setMinPrice(new BigDecimal("11.00"))
                        .setSharePrice(new BigDecimal("14.00"))));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(200L,
                new ErpProductRespVO().setId(200L).setUnitName("个")
                        .setCustomFields(Collections.singletonMap("orderPriceCustom", new BigDecimal("10.20")))));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(5L,
                new ErpWarehouseDO().setId(5L).setName("销售仓")));
        when(stockService.getStockMap(any(), any())).thenReturn(Collections.singletonMap("200_5",
                new ErpStockDO().setProductId(200L).setWarehouseId(5L).setCostPrice(new BigDecimal("7.25"))));

        ErpPurchaseInSaleCartableItemPageReqVO reqVO = new ErpPurchaseInSaleCartableItemPageReqVO();
        reqVO.setInId(10L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        PageResult<ErpPurchaseInSaleCartableItemRespVO> result =
                purchaseInService.getSaleCartableItemPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(0, result.getList().get(0).getSaleCartableCount().compareTo(new BigDecimal("7")));
        assertEquals(0, result.getList().get(0).getCostPrice().compareTo(new BigDecimal("7.25")));
        assertEquals(0, result.getList().get(0).getMinPrice().compareTo(new BigDecimal("11.00")));
        assertEquals(0, result.getList().get(0).getSharePrice().compareTo(new BigDecimal("14.00")));
        assertEquals(new BigDecimal("10.20"), result.getList().get(0).getCustomFields().get("orderPriceCustom"));
        verify(purchaseInItemMapper).selectSaleCartablePageByInId(eq(reqVO));
        verify(stockService).getStockMap(any(), any());
    }

    @Test
    public void testCreateSaleCartFromPurchaseIn_queriesRequestedSourceItemsOnly() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        ErpPurchaseInItemDO sourceItem = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setProductId(200L).setWarehouseId(6L)
                .setProductPrice(new BigDecimal("12.34")).setCount(new BigDecimal("10"))
                .setBatchNo("B1");
        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(sourceItem));
        when(saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(any()))
                .thenReturn(Collections.emptyMap());
        ErpSaleCartSubmitRespVO submitRespVO = new ErpSaleCartSubmitRespVO();
        submitRespVO.setId(500L);
        submitRespVO.setNo("XSTC001");
        submitRespVO.setStatus(10);
        when(saleCartService.createAndSubmitSaleCartFromPurchaseIn(any())).thenReturn(submitRespVO);
        when(saleCartService.getSaleCartItemListByCartId(eq(500L))).thenReturn(Collections.singletonList(
                new ErpSaleCartItemDO().setId(700L).setProductId(200L).setWarehouseId(6L)
                        .setCount(new BigDecimal("2")).setGiftFlag(false).setBatchNo("B1")));

        ErpPurchaseInCreateSaleCartReqVO reqVO = new ErpPurchaseInCreateSaleCartReqVO();
        reqVO.setSourceInId(10L);
        reqVO.setCustomerId(300L);
        ErpPurchaseInCreateSaleCartReqVO.Item item = new ErpPurchaseInCreateSaleCartReqVO.Item();
        item.setSourceInItemId(100L);
        item.setWarehouseId(6L);
        item.setCount(new BigDecimal("2"));
        item.setProductPrice(new BigDecimal("20"));
        item.setBatchNo("B1");
        reqVO.setItems(Collections.singletonList(item));

        purchaseInService.createSaleCartFromPurchaseIn(reqVO);

        verify(purchaseInItemMapper).selectBatchIds(any());
        verify(purchaseInItemMapper, never()).selectListByInId(eq(10L));
        verify(saleConvertRecordMapper).selectPurchaseInToCartCountMapBySourceItemIds(any());
    }

    @Test
    public void testCreateSaleCartFromPurchaseIn_sourceItemNotBelongToIn_throwException() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(100L).setInId(11L).setProductId(200L)
                        .setWarehouseId(6L).setCount(new BigDecimal("10"))));

        ErpPurchaseInCreateSaleCartReqVO reqVO = new ErpPurchaseInCreateSaleCartReqVO();
        reqVO.setSourceInId(10L);
        reqVO.setCustomerId(300L);
        ErpPurchaseInCreateSaleCartReqVO.Item item = new ErpPurchaseInCreateSaleCartReqVO.Item();
        item.setSourceInItemId(100L);
        item.setWarehouseId(6L);
        item.setCount(new BigDecimal("2"));
        item.setProductPrice(new BigDecimal("20"));
        reqVO.setItems(Collections.singletonList(item));

        assertServiceException(() -> purchaseInService.createSaleCartFromPurchaseIn(reqVO),
                PURCHASE_IN_TRANSFER_OUT_SOURCE_ITEM_NOT_EXISTS);
        verify(purchaseInItemMapper, never()).selectListByInId(eq(10L));
        verify(saleCartService, never()).createAndSubmitSaleCartFromPurchaseIn(any());
    }

    // ========== createPurchaseInFromOrder ==========

    @Test
    public void testCreatePurchaseInFromOrder_success() {
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(50L).setNo("CGDD001").setSupplierId(999L)
                .setAccountId(300L).setFeeAmount(new BigDecimal("12.50")).setFileUrl("https://example.com/order.pdf")
                .setRemark("订单备注").setFactoryOrderNo("F20260520").setInvoiceType("专票")
                .setSettleMethod("月结").setDeptId(88L).setTaxPercent(new BigDecimal("13"))
                .setPurchaser(700L);
        when(purchaseOrderService.validatePurchaseOrder(eq(50L))).thenReturn(order);
        ErpPurchaseOrderItemDO orderItem = new ErpPurchaseOrderItemDO()
                .setId(101L).setProductId(200L).setProductUnitId(1L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("10"))
                .setInCount(new BigDecimal("3")).setTaxPercent(new BigDecimal("13"))
                .setWarehouseId(10L).setDeptId(66L).setBatchNo("B20260520")
                .setWarehousePosition("A-01-02").setDrawingNo("DWG-001").setBrand("博世")
                .setVehicleModel("大众-朗逸").setOriginPlace("德国").setRemark("明细备注")
                .setGift(false);
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(orderItem));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(Collections.emptyMap());

        // 自动审批后会再次 selectById（updatePurchaseInStatus 内部调用）
        // 让插入后的 selectById 也能拿到记录
        when(purchaseInMapper.selectListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(new ErpPurchaseInDO().setId(10L).setOrderId(50L)
                        .setStatus(ErpAuditStatus.PROCESS.getStatus())));
        Map<Long, BigDecimal> generatedInCountMap = new HashMap<>();
        generatedInCountMap.put(101L, new BigDecimal("3"));
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(generatedInCountMap);

        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        reqVO.setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(101L);
        reqItem.setCount(new BigDecimal("5")); // 可入库 = 10-3 = 7，5 ≤ 7 OK
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        purchaseInService.createPurchaseInFromOrder(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> inCaptor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).insert(inCaptor.capture());
        ErpPurchaseInDO inserted = inCaptor.getValue();
        assertEquals(Long.valueOf(50L), inserted.getOrderId());
        assertEquals("CGDD001", inserted.getOrderNo());
        assertEquals(Long.valueOf(999L), inserted.getSupplierId());
        assertEquals(Long.valueOf(88L), inserted.getDeptId());
        assertEquals(Long.valueOf(300L), inserted.getAccountId());
        assertEquals(0, inserted.getFeeAmount().compareTo(new BigDecimal("12.50")));
        assertEquals("https://example.com/order.pdf", inserted.getFileUrl());
        assertEquals("订单备注", inserted.getRemark());
        assertEquals("F20260520", inserted.getFactoryOrderNo());
        assertEquals("专票", inserted.getInvoiceType());
        assertEquals("月结", inserted.getSettleMethod());
        assertEquals("700", inserted.getPurchaser());
        assertEquals(0, inserted.getTaxRate().compareTo(new BigDecimal("13")));

        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(itemsCaptor.capture());
        List<ErpPurchaseInItemDO> insertedItems = itemsCaptor.getValue();
        assertEquals(1, insertedItems.size());
        ErpPurchaseInItemDO insertedItem = insertedItems.get(0);
        assertEquals(Long.valueOf(101L), insertedItem.getOrderItemId());
        assertEquals(Long.valueOf(200L), insertedItem.getProductId());
        assertEquals(Long.valueOf(10L), insertedItem.getWarehouseId());
        assertEquals(Long.valueOf(66L), insertedItem.getDeptId());
        assertEquals(0, insertedItem.getCount().compareTo(new BigDecimal("5")));
        assertEquals(0, insertedItem.getProductPrice().compareTo(new BigDecimal("10")));
        assertEquals("B20260520", insertedItem.getBatchNo());
        assertEquals("A-01-02", insertedItem.getWarehousePosition());
        assertEquals("DWG-001", insertedItem.getDrawingNo());
        assertEquals("博世", insertedItem.getBrand());
        assertEquals("大众-朗逸", insertedItem.getVehicleModel());
        assertEquals("德国", insertedItem.getOriginPlace());
        assertEquals("明细备注", insertedItem.getRemark());

    }

    @Test
    public void testCreatePurchaseInFromOrder_orderItemNotFound_throwException() {
        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L));
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.emptyList());

        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(999L); // 不存在
        reqItem.setCount(new BigDecimal("1"));
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        assertServiceException(() -> purchaseInService.createPurchaseInFromOrder(reqVO),
                PURCHASE_ORDER_NOT_EXISTS);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseInFromOrder_countExceedsInable_throwException() {
        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L));
        when(purchaseInMapper.selectListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(new ErpPurchaseInDO().setId(10L).setOrderId(50L)
                        .setStatus(ErpAuditStatus.PROCESS.getStatus())));
        Map<Long, BigDecimal> generatedInCountMap = new HashMap<>();
        generatedInCountMap.put(101L, new BigDecimal("8"));
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(generatedInCountMap);
        ErpPurchaseOrderItemDO orderItem = new ErpPurchaseOrderItemDO()
                .setId(101L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("8")); // 可入库=2
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(orderItem));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO()
                .setId(200L).setName("螺丝"));

        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(101L);
        reqItem.setCount(new BigDecimal("5")); // > 可入库 2
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        assertServiceException(() -> purchaseInService.createPurchaseInFromOrder(reqVO),
                PURCHASE_ORDER_IN_EXCEED_INABLE, "螺丝", new BigDecimal("2"), new BigDecimal("5"));
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseInFromOrder_pendingInOccupiesAll_throwException() {
        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L));
        when(purchaseInMapper.selectListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(new ErpPurchaseInDO().setId(10L).setOrderId(50L)
                        .setStatus(ErpAuditStatus.PROCESS.getStatus())));
        Map<Long, BigDecimal> generatedInCountMap = new HashMap<>();
        generatedInCountMap.put(101L, new BigDecimal("10"));
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(generatedInCountMap);
        ErpPurchaseOrderItemDO orderItem = new ErpPurchaseOrderItemDO()
                .setId(101L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(BigDecimal.ZERO);
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(orderItem));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO()
                .setId(200L).setName("P200"));

        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(101L);
        reqItem.setCount(BigDecimal.ONE);
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        assertServiceException(() -> purchaseInService.createPurchaseInFromOrder(reqVO),
                PURCHASE_ORDER_IN_EXCEED_INABLE, "P200", BigDecimal.ZERO, BigDecimal.ONE);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseInFromOrder_giftForcesZeroPrice() {
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(50L).setNo("CGDD001").setSupplierId(999L);
        when(purchaseOrderService.validatePurchaseOrder(eq(50L))).thenReturn(order);
        // 赠品行
        ErpPurchaseOrderItemDO giftOrderItem = new ErpPurchaseOrderItemDO()
                .setId(101L).setProductId(200L).setProductUnitId(1L)
                .setProductPrice(new BigDecimal("99")).setCount(new BigDecimal("10"))
                .setInCount(BigDecimal.ZERO).setGift(true);
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(giftOrderItem));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(purchaseInMapper.selectListByOrderId(eq(50L)))
                .thenReturn(Collections.emptyList());
        // 后续 updatePurchaseInStatus 链路
        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        reqVO.setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(101L);
        reqItem.setCount(new BigDecimal("5"));
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        purchaseInService.createPurchaseInFromOrder(reqVO);

        // 验证插入的子表项：赠品行 productPrice 应被强制设为 0
        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper, times(1)).insertBatch(itemsCaptor.capture());
        List<ErpPurchaseInItemDO> insertedItems = itemsCaptor.getValue();
        assertEquals(1, insertedItems.size());
        assertTrue(Boolean.TRUE.equals(insertedItems.get(0).getGift()));
        assertEquals(0, insertedItems.get(0).getProductPrice().compareTo(BigDecimal.ZERO));
    }

    // ========== getApprovedPurchaseInsBySupplier ==========

    @Test
    public void testGetApprovedPurchaseInsBySupplier_nullSupplier_returnsEmpty() {
        List<ErpPurchaseInForAdjustRespVO> result = purchaseInService.getApprovedPurchaseInsBySupplier(null);

        assertTrue(result.isEmpty());
        verify(purchaseInMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    public void testGetApprovedPurchaseInsBySupplier_noInList_returnsEmpty() {
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ErpPurchaseInForAdjustRespVO> result = purchaseInService.getApprovedPurchaseInsBySupplier(99L);

        assertTrue(result.isEmpty());
        verify(supplierService, never()).getSupplierMap(any());
    }

    @Test
    public void testGetApprovedPurchaseInsBySupplier_success() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        in.setUpdater("123");
        in.setUpdateTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(in));
        // 入库项 2 行（用于统计 itemCount）
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Arrays.asList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L),
                new ErpPurchaseInItemDO().setId(2L).setInId(10L)));
        Map<Long, ErpSupplierDO> supplierMap = new HashMap<>();
        supplierMap.put(99L, new ErpSupplierDO().setId(99L).setName("芋道供应商"));
        when(supplierService.getSupplierMap(any())).thenReturn(supplierMap);
        Map<Long, cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO> userMap = new HashMap<>();
        cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO user =
                new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO();
        user.setId(123L);
        user.setNickname("张三");
        userMap.put(123L, user);
        when(adminUserApi.getUserMap(any())).thenReturn(userMap);

        List<ErpPurchaseInForAdjustRespVO> result = purchaseInService.getApprovedPurchaseInsBySupplier(99L);

        assertEquals(1, result.size());
        ErpPurchaseInForAdjustRespVO vo = result.get(0);
        assertEquals(Integer.valueOf(2), vo.getItemCount());
        assertEquals("芋道供应商", vo.getSupplierName());
        assertEquals(Long.valueOf(123L), vo.getAuditorId());
        assertEquals("张三", vo.getAuditorName());
        assertNotNull(vo.getApproveTime());
    }

    @Test
    public void testGetApprovedPurchaseInsBySupplier_filtersApprovedInvoice() {
        ErpPurchaseInDO invoiced = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        ErpPurchaseInDO available = new ErpPurchaseInDO().setId(11L).setNo("CGRK002").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(invoiced, available));
        when(purchaseInvoiceItemMapper.selectApprovedListBySourceInIds(any()))
                .thenReturn(Collections.singletonList(new ErpPurchaseInvoiceItemDO().setSourceInId(10L)));
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(2L).setInId(11L)));
        when(supplierService.getSupplierMap(any())).thenReturn(Collections.emptyMap());

        List<ErpPurchaseInForAdjustRespVO> result = purchaseInService.getApprovedPurchaseInsBySupplier(99L);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(11L), result.get(0).getId());
        assertEquals(Boolean.FALSE, result.get(0).getHasInvoice());
    }

    // ========== getApprovedPurchaseInItemsBySupplier ==========

    @Test
    public void testGetApprovedPurchaseInItemsBySupplier_nullSupplier_returnsEmpty() {
        List<ErpPurchaseInItemForAdjustRespVO> result =
                purchaseInService.getApprovedPurchaseInItemsBySupplier(null, false);

        assertTrue(result.isEmpty());
        verify(purchaseInMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    public void testGetApprovedPurchaseInItemsBySupplier_excludeAdjustedFiltersAdjustedItems() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(in));
        // 两项：一项已调价、一项未调价
        ErpPurchaseInItemDO adjustedItem = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))
                .setAdjusted(true);
        ErpPurchaseInItemDO unadjustedItem = new ErpPurchaseInItemDO()
                .setId(2L).setInId(10L).setProductId(201L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("20")).setCount(new BigDecimal("3"))
                .setAdjusted(false);
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Arrays.asList(adjustedItem, unadjustedItem));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.emptyMap());

        List<ErpPurchaseInItemForAdjustRespVO> result =
                purchaseInService.getApprovedPurchaseInItemsBySupplier(99L, true);

        // 已调价行被过滤掉，仅返回未调价行
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(2L), result.get(0).getId());
    }

    @Test
    public void testGetApprovedPurchaseInItemsBySupplier_filtersApprovedInvoice() {
        ErpPurchaseInDO invoiced = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        ErpPurchaseInDO available = new ErpPurchaseInDO().setId(11L).setNo("CGRK002").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(invoiced, available));
        when(purchaseInvoiceItemMapper.selectApprovedListBySourceInIds(any()))
                .thenReturn(Collections.singletonList(new ErpPurchaseInvoiceItemDO().setSourceInId(10L)));
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(2L).setInId(11L).setProductId(201L).setWarehouseId(5L)
                        .setProductPrice(new BigDecimal("20")).setCount(new BigDecimal("3"))));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.emptyMap());

        List<ErpPurchaseInItemForAdjustRespVO> result =
                purchaseInService.getApprovedPurchaseInItemsBySupplier(99L, false);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(11L), result.get(0).getInId());
        assertEquals(Boolean.FALSE, result.get(0).getHasInvoice());
    }

    @Test
    public void testGetApprovedPurchaseInItemsBySupplier_successWithFullAggregation() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(in));
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(5L)
                .setDeptId(15L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))
                .setVehicleModel("大众-朗逸").setBrand("博世").setOriginPlace("德国");
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Collections.singletonList(item));
        // 产品资料
        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(200L);
        product.setCode("P001");
        product.setName("螺丝");
        product.setUnitName("件");
        productMap.put(200L, product);
        when(productService.getProductVOMap(any())).thenReturn(productMap);
        // 仓库
        Map<Long, ErpWarehouseDO> warehouseMap = new HashMap<>();
        warehouseMap.put(5L, new ErpWarehouseDO().setId(5L).setName("主仓库"));
        when(warehouseService.getWarehouseMap(any())).thenReturn(warehouseMap);

        List<ErpPurchaseInItemForAdjustRespVO> result =
                purchaseInService.getApprovedPurchaseInItemsBySupplier(99L, false);

        assertEquals(1, result.size());
        ErpPurchaseInItemForAdjustRespVO vo = result.get(0);
        assertEquals("CGRK001", vo.getInNo());
        assertEquals("P001", vo.getProductCode());
        assertEquals("螺丝", vo.getProductName());
        assertEquals("件", vo.getProductUnitName());
        assertEquals("主仓库", vo.getWarehouseName());
        assertEquals(Long.valueOf(15L), vo.getDeptId());
        // item 自有的车型/品牌/产地优先于产品资料
        assertEquals("大众-朗逸", vo.getVehicleModel());
        assertEquals("博世", vo.getBrand());
        assertEquals("德国", vo.getOriginPlace());
    }

    @Test
    public void testParsePurchaseInImportTime_acceptsCommonYearFirstFormats() {
        assertPurchaseInImportTime("2026/8/1", LocalDateTime.of(2026, 8, 1, 0, 0));
        assertPurchaseInImportTime("2026/08/01", LocalDateTime.of(2026, 8, 1, 0, 0));
        assertPurchaseInImportTime("2026-8-1", LocalDateTime.of(2026, 8, 1, 0, 0));
        assertPurchaseInImportTime("2026-08-11", LocalDateTime.of(2026, 8, 11, 0, 0));
        assertPurchaseInImportTime("2026/8/1 09:05", LocalDateTime.of(2026, 8, 1, 9, 5));
        assertPurchaseInImportTime("2026/8/1 09:05:30", LocalDateTime.of(2026, 8, 1, 9, 5, 30));
        assertPurchaseInImportTime("2026-08-11 09:05:30", LocalDateTime.of(2026, 8, 11, 9, 5, 30));
        assertPurchaseInImportTime(" 2026/8/1 ", LocalDateTime.of(2026, 8, 1, 0, 0));
    }

    @Test
    public void testParsePurchaseInImportTime_blankReturnsDefaultValue() {
        LocalDateTime defaultValue = LocalDateTime.of(2026, 8, 28, 10, 30);

        LocalDateTime actual = ReflectionTestUtils.invokeMethod(purchaseInService,
                "parsePurchaseInImportTime", " ", defaultValue);

        assertEquals(defaultValue, actual);
    }

    @Test
    public void testImportPurchaseInOrderList_nullInTime_defaultsCurrentTime() {
        assertImportPurchaseInOrderDefaultsCurrentTime(null);
    }

    @Test
    public void testImportPurchaseInOrderList_blankInTime_defaultsCurrentTime() {
        assertImportPurchaseInOrderDefaultsCurrentTime("   ");
    }

    @Test
    public void testParsePurchaseInImportTime_rejectsAmbiguousAndCompactFormats() {
        assertInvalidPurchaseInImportTime("08/11/2026");
        assertInvalidPurchaseInImportTime("20260811");
    }

    @Test
    public void testImportPurchaseInOrderList_invalidInTime_returnsReadableMessage() {
        ErpPurchaseInOrderImportExcelVO row = new ErpPurchaseInOrderImportExcelVO();
        row.setNo("CGRK20260811000002");
        row.setSupplierName("芋道供应商");
        row.setInTime("08/11/2026");
        row.setProductName("产品1");
        row.setWarehouseName("主仓库");
        row.setItemCount(BigDecimal.ONE);
        row.setProductPrice(BigDecimal.ONE);

        when(supplierService.getSupplierPage(any())).thenReturn(new PageResult<>(
                Collections.singletonList(new ErpSupplierDO().setId(100L).setName("芋道供应商")
                        .setStatus(CommonStatusEnum.ENABLE.getStatus())), 1L));
        when(productMapper.selectListByNames(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setCode("P000001").setName("产品1").setUnitId(1L)
                        .setPurchasePrice(BigDecimal.ONE)));
        when(warehouseService.getPurchaseWarehouseListByStatus(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(10L).setName("主仓库")));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());

        ErpPurchaseImportResultRespVO result = purchaseInService.importPurchaseInOrderList(
                Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(Integer.valueOf(2), result.getFailureDetails().get(0).getRowNo());
        assertEquals("CGRK20260811000002", result.getFailureDetails().get(0).getOrderNo());
        assertEquals("入库时间格式不正确，请使用 yyyy-MM-dd、yyyy/M/d 或常见日期时间格式",
                result.getFailureDetails().get(0).getReason());
    }

    private void assertPurchaseInImportTime(String value, LocalDateTime expected) {
        LocalDateTime actual = ReflectionTestUtils.invokeMethod(purchaseInService,
                "parsePurchaseInImportTime", value, null);
        assertEquals(expected, actual);
    }

    private void assertInvalidPurchaseInImportTime(String value) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(purchaseInService, "parsePurchaseInImportTime", value, null));
        assertEquals("入库时间格式不正确，请使用 yyyy-MM-dd、yyyy/M/d 或常见日期时间格式",
                exception.getMessage());
    }

    private void assertImportPurchaseInOrderDefaultsCurrentTime(String inTime) {
        mockSuccessfulPurchaseInOrderImport();
        ErpPurchaseInOrderImportExcelVO row = buildPurchaseInOrderImportRow(inTime);

        LocalDateTime before = LocalDateTime.now();
        ErpPurchaseImportResultRespVO result = purchaseInService.importPurchaseInOrderList(
                Collections.singletonList(row));
        LocalDateTime after = LocalDateTime.now();

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).insert(captor.capture());
        LocalDateTime insertedInTime = captor.getValue().getInTime();
        assertNotNull(insertedInTime);
        assertTrue(!insertedInTime.isBefore(before));
        assertTrue(!insertedInTime.isAfter(after));
    }

    private void mockSuccessfulPurchaseInOrderImport() {
        ErpSupplierDO supplier = new ErpSupplierDO().setId(100L).setName("芋道供应商")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        ErpProductDO product = new ErpProductDO().setId(200L).setCode("P000001").setName("产品1").setUnitId(1L)
                .setPurchasePrice(BigDecimal.ONE);
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(10L).setName("主仓库");
        when(supplierService.getSupplierPage(any())).thenReturn(new PageResult<>(
                Collections.singletonList(supplier), 1L));
        when(productMapper.selectListByCodes(any())).thenReturn(Collections.singletonList(product));
        when(warehouseService.getPurchaseWarehouseListByStatus(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.singletonList(warehouse));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(supplierService.validateSupplier(eq(100L))).thenReturn(supplier);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(product));
        when(warehouseService.validPurchaseWarehouseList(any())).thenReturn(Collections.singletonList(warehouse));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(10L, warehouse));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
    }

    private ErpPurchaseInOrderImportExcelVO buildPurchaseInOrderImportRow(String inTime) {
        ErpPurchaseInOrderImportExcelVO row = new ErpPurchaseInOrderImportExcelVO();
        row.setNo("CGRK20260828000001");
        row.setSupplierName("芋道供应商");
        row.setInTime(inTime);
        row.setProductCode("P000001");
        row.setWarehouseName("主仓库");
        row.setItemCount(BigDecimal.ONE);
        row.setProductPrice(BigDecimal.ONE);
        return row;
    }

}
