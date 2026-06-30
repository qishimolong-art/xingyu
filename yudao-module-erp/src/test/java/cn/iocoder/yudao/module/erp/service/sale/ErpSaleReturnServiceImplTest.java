package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnModeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ErpSaleReturnServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleReturnServiceImpl saleReturnService;

    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSaleOrderService saleOrderService;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
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
    private ErpStockService stockService;
    @Mock
    private ErpStockOutBillService stockOutBillService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleReturnService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260510000001";
            }
        });
    }

    // ==================== createSaleReturn ====================

    @Test
    public void testCreateBySaleOut_exceedReturnableCount_throwException() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_SALE_OUT.getMode());
        reqVO.setSourceOutId(10L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("6")).setSourceOutItemId(100L)));

        when(saleOutService.validateSaleOut(eq(10L))).thenReturn(new ErpSaleOutDO()
                .setId(10L).setNo("XSCK001").setCustomerId(20L));
        when(saleOutService.getSaleOutItemListByOutId(eq(10L))).thenReturn(Collections.singletonList(new ErpSaleOutItemDO()
                .setId(100L).setOutId(10L).setProductId(200L).setWarehouseId(300L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))));
        when(stockOutBillService.getSaleOutSourceItemList(eq(10L))).thenReturn(Collections.emptyList());
        assertThrows(ServiceException.class, () -> saleReturnService.createSaleReturn(reqVO));
        verify(saleReturnMapper, never()).insert(any(ErpSaleReturnDO.class));
    }

    @Test
    public void testCreateByStock_withoutSaleOrder_createReturn() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_STOCK.getMode());
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2"))));
        mockProduct();
        when(customerService.validateCustomer(eq(20L))).thenReturn(new ErpCustomerDO().setId(20L));
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        when(saleReturnMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleReturnDO saleReturn = invocation.getArgument(0);
            saleReturn.setId(99L);
            return 1;
        }).when(saleReturnMapper).insert(any(ErpSaleReturnDO.class));

        saleReturnService.createSaleReturn(reqVO);

        verify(saleOrderService, never()).validateSaleOrder(anyLong());
        verify(saleReturnMapper).insert(argThat((ErpSaleReturnDO saleReturn) -> reqVO.getReturnMode().equals(saleReturn.getReturnMode())
                && reqVO.getCustomerId().equals(saleReturn.getCustomerId())
                && saleReturn.getOrderId() == null));
        verify(saleReturnItemMapper).insertBatch(argThat(items -> items.iterator().next().getOrderItemId() == null));
    }

    @Test
    public void testCreateBySaleOut_normalCase_createReturn() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_SALE_OUT.getMode());
        reqVO.setSourceOutId(10L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("3")).setSourceOutItemId(100L)));

        when(saleOutService.validateSaleOut(eq(10L))).thenReturn(new ErpSaleOutDO()
                .setId(10L).setNo("XSCK001").setCustomerId(20L));
        when(saleOutService.getSaleOutItemListByOutId(eq(10L))).thenReturn(Collections.singletonList(new ErpSaleOutItemDO()
                .setId(100L).setOutId(10L).setProductId(200L).setWarehouseId(300L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))));
        when(stockOutBillService.getSaleOutSourceItemList(eq(10L))).thenReturn(Collections.emptyList());
        when(saleReturnMapper.selectListBySourceOutId(eq(10L))).thenReturn(Collections.emptyList());
        mockProduct();
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        when(saleReturnMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleReturnDO saleReturn = invocation.getArgument(0);
            saleReturn.setId(101L);
            return 1;
        }).when(saleReturnMapper).insert(any(ErpSaleReturnDO.class));

        saleReturnService.createSaleReturn(reqVO);

        // 验证写入主表时来源销售单字段被正确回填，且 customerId 来自原销售单
        verify(saleReturnMapper).insert(argThat((ErpSaleReturnDO saleReturn) -> Long.valueOf(10L).equals(saleReturn.getSourceOutId())
                && "XSCK001".equals(saleReturn.getSourceOutNo())
                && Long.valueOf(20L).equals(saleReturn.getCustomerId())
                && saleReturn.getOrderId() == null
                && saleReturn.getOrderNo() == null
                && ErpSaleReturnModeEnum.BY_SALE_OUT.getMode().equals(saleReturn.getReturnMode())
                && ErpAuditStatus.PROCESS.getStatus().equals(saleReturn.getStatus())));
        verify(saleReturnItemMapper).insertBatch(argThat(items -> items.iterator().next().getSourceOutItemId().equals(100L)));
    }

    @Test
    public void testCreateBySaleOut_withStockOutBill_exceedPickedCount_throwException() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_SALE_OUT.getMode());
        reqVO.setSourceOutId(10L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("3")).setSourceOutItemId(100L)));

        when(saleOutService.validateSaleOut(eq(10L))).thenReturn(new ErpSaleOutDO()
                .setId(10L).setNo("XSCK001").setCustomerId(20L));
        when(saleOutService.getSaleOutItemListByOutId(eq(10L))).thenReturn(Collections.singletonList(new ErpSaleOutItemDO()
                .setId(100L).setOutId(10L).setProductId(200L).setWarehouseId(300L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))));
        when(stockOutBillService.getSaleOutSourceItemList(eq(10L))).thenReturn(Collections.singletonList(
                new ErpStockOutBillItemDO().setSourceItemId(100L).setPickedCount(new BigDecimal("2"))));
        when(saleReturnMapper.selectListBySourceOutId(eq(10L))).thenReturn(Collections.emptyList());

        assertException(() -> saleReturnService.createSaleReturn(reqVO),
                ErrorCodeConstants.SALE_RETURN_EXCEED_RETURNABLE);
        verify(saleReturnMapper, never()).insert(any(ErpSaleReturnDO.class));
    }

    @Test
    public void testCreateBySaleOut_withStockOutBill_returnPickedCount_success() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_SALE_OUT.getMode());
        reqVO.setSourceOutId(10L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2")).setSourceOutItemId(100L)));

        when(saleOutService.validateSaleOut(eq(10L))).thenReturn(new ErpSaleOutDO()
                .setId(10L).setNo("XSCK001").setCustomerId(20L));
        when(saleOutService.getSaleOutItemListByOutId(eq(10L))).thenReturn(Collections.singletonList(new ErpSaleOutItemDO()
                .setId(100L).setOutId(10L).setProductId(200L).setWarehouseId(300L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))));
        when(stockOutBillService.getSaleOutSourceItemList(eq(10L))).thenReturn(Collections.singletonList(
                new ErpStockOutBillItemDO().setSourceItemId(100L).setPickedCount(new BigDecimal("2"))));
        when(saleReturnMapper.selectListBySourceOutId(eq(10L))).thenReturn(Collections.emptyList());
        mockProduct();
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        when(saleReturnMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleReturnDO saleReturn = invocation.getArgument(0);
            saleReturn.setId(102L);
            return 1;
        }).when(saleReturnMapper).insert(any(ErpSaleReturnDO.class));

        saleReturnService.createSaleReturn(reqVO);

        verify(saleReturnMapper).insert(any(ErpSaleReturnDO.class));
        verify(saleReturnItemMapper).insertBatch(argThat(items ->
                new BigDecimal("2").compareTo(items.iterator().next().getCount()) == 0));
    }

    @Test
    public void testCreateBySaleOut_sourceOutNotApproved_throwException() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_SALE_OUT.getMode());
        reqVO.setSourceOutId(10L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("3")).setSourceOutItemId(100L)));

        // 源销售单未审核
        when(saleOutService.validateSaleOut(eq(10L)))
                .thenThrow(new ServiceException(ErrorCodeConstants.SALE_OUT_NOT_APPROVE.getCode(),
                        ErrorCodeConstants.SALE_OUT_NOT_APPROVE.getMsg()));

        assertException(() -> saleReturnService.createSaleReturn(reqVO), ErrorCodeConstants.SALE_OUT_NOT_APPROVE);
        verify(saleReturnMapper, never()).insert(any(ErpSaleReturnDO.class));
    }

    @Test
    public void testCreateByStock_invalidCustomer_throwException() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_STOCK.getMode());
        reqVO.setCustomerId(null); // 按库存退货必须填 customerId
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2"))));

        assertException(() -> saleReturnService.createSaleReturn(reqVO),
                ErrorCodeConstants.SALE_RETURN_BY_STOCK_CUSTOMER_REQUIRED);
        verify(saleReturnMapper, never()).insert(any(ErpSaleReturnDO.class));
        verify(customerService, never()).validateCustomer(anyLong());
    }

    @Test
    public void testCreateByOrder_normalCase_createReturn() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.LEGACY_ORDER.getMode());
        reqVO.setOrderId(50L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2")).setOrderItemId(500L)));

        when(saleOrderService.validateSaleOrder(eq(50L))).thenReturn(new ErpSaleOrderDO()
                .setId(50L).setNo("XSDD001").setCustomerId(80L));
        mockProduct();
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        when(saleReturnMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleReturnDO saleReturn = invocation.getArgument(0);
            saleReturn.setId(123L);
            return 1;
        }).when(saleReturnMapper).insert(any(ErpSaleReturnDO.class));

        saleReturnService.createSaleReturn(reqVO);

        // 验证 LEGACY_ORDER 模式下：orderNo、customerId 来自销售订单；sourceOutId 不被填充
        verify(saleReturnMapper).insert(argThat((ErpSaleReturnDO saleReturn) -> "XSDD001".equals(saleReturn.getOrderNo())
                && Long.valueOf(80L).equals(saleReturn.getCustomerId())
                && saleReturn.getSourceOutId() == null
                && saleReturn.getSourceOutNo() == null
                && ErpSaleReturnModeEnum.LEGACY_ORDER.getMode().equals(saleReturn.getReturnMode())));
        // 验证退货数量回写销售订单
        verify(saleOrderService).updateSaleOrderReturnCount(eq(50L), any());
    }

    // ==================== updateSaleReturn ====================

    @Test
    public void testUpdateSaleReturn_processStatus_success() {
        Long id = 200L;
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_STOCK.getMode());
        reqVO.setId(id);
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("3"))));

        when(saleReturnMapper.selectById(eq(id))).thenReturn(new ErpSaleReturnDO()
                .setId(id).setNo("XTH001").setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setReturnMode(ErpSaleReturnModeEnum.BY_STOCK.getMode()));
        when(customerService.validateCustomer(eq(20L))).thenReturn(new ErpCustomerDO().setId(20L));
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        mockProduct();
        when(saleReturnItemMapper.selectListByReturnId(eq(id))).thenReturn(Collections.emptyList());

        saleReturnService.updateSaleReturn(reqVO);

        verify(saleReturnMapper).updateById(argThat((ErpSaleReturnDO update) -> id.equals(update.getId())
                && Long.valueOf(20L).equals(update.getCustomerId())
                && ErpSaleReturnModeEnum.BY_STOCK.getMode().equals(update.getReturnMode())));
        verify(saleReturnItemMapper).insertBatch(any());
    }

    @Test
    public void testUpdateSaleReturn_approvedStatus_throwException() {
        Long id = 201L;
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_STOCK.getMode());
        reqVO.setId(id);
        reqVO.setCustomerId(20L);

        when(saleReturnMapper.selectById(eq(id))).thenReturn(new ErpSaleReturnDO()
                .setId(id).setNo("XTH001").setStatus(ErpAuditStatus.APPROVE.getStatus()));

        assertException(() -> saleReturnService.updateSaleReturn(reqVO),
                ErrorCodeConstants.SALE_RETURN_UPDATE_FAIL_APPROVE, "XTH001");
        verify(saleReturnMapper, never()).updateById(any(ErpSaleReturnDO.class));
    }

    // ==================== updateSaleReturnStatus ====================

    @Test
    public void testUpdateSaleReturnStatus_approve_createStockRecordIn() {
        Long id = 300L;
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO()
                .setId(id).setNo("XTH300")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setRefundPrice(BigDecimal.ZERO)
                .setReturnTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .setSourceOutId(null); // 简化：避免触发 updateSaleOutReturnStatus
        when(saleReturnMapper.selectById(eq(id))).thenReturn(saleReturn);
        when(saleReturnMapper.updateByIdAndStatus(eq(id), eq(ErpAuditStatus.PROCESS.getStatus()),
                argThat((ErpSaleReturnDO upd) -> ErpAuditStatus.APPROVE.getStatus().equals(upd.getStatus()))))
                .thenReturn(1);
        ErpSaleReturnItemDO item = new ErpSaleReturnItemDO()
                .setId(1000L).setReturnId(id)
                .setProductId(200L).setWarehouseId(300L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("2"));
        when(saleReturnItemMapper.selectListByReturnId(eq(id))).thenReturn(Collections.singletonList(item));
        // 未开账时不生成凭证
        when(bookOpenService.isVoucherTypeEnabled(any(LocalDate.class), eq(ErpVoucherTypeEnum.SALE.getType())))
                .thenReturn(false);

        saleReturnService.updateSaleReturnStatus(id, ErpAuditStatus.APPROVE.getStatus());

        // 入库流水：count 为正数，bizType=SALE_RETURN
        verify(stockRecordService).createStockRecord(argThat((ErpStockRecordCreateReqBO bo) -> ErpStockRecordBizTypeEnum.SALE_RETURN.getType().equals(bo.getBizType())
                && new BigDecimal("2").compareTo(bo.getCount()) == 0
                && Long.valueOf(200L).equals(bo.getProductId())
                && Long.valueOf(300L).equals(bo.getWarehouseId())));
        // 未开账不生成凭证
        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    public void testUpdateSaleReturnStatus_processFromApprove_throwException() {
        Long id = 301L;

        assertException(() -> saleReturnService.updateSaleReturnStatus(id, ErpAuditStatus.PROCESS.getStatus()),
                ErrorCodeConstants.SALE_RETURN_PROCESS_FAIL);
        verify(saleReturnMapper, never()).selectById(eq(id));
        verify(saleReturnMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
        verify(voucherMapper, never()).deleteById(anyLong());
        verify(voucherItemMapper, never()).delete(any());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    public void testUpdateSaleReturnStatus_processFromApprove_rejectBeforeVoucherCheck() {
        Long id = 302L;

        assertException(() -> saleReturnService.updateSaleReturnStatus(id, ErpAuditStatus.PROCESS.getStatus()),
                ErrorCodeConstants.SALE_RETURN_PROCESS_FAIL);
        verify(saleReturnMapper, never()).selectById(eq(id));
        verify(saleReturnMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
        verify(voucherMapper, never()).selectListByBiz(anyInt(), anyLong());
    }

    @Test
    public void testUpdateSaleReturnStatus_optimisticLockFail_throwException() {
        Long id = 303L;
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO()
                .setId(id).setNo("XTH303")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setRefundPrice(BigDecimal.ZERO);
        when(saleReturnMapper.selectById(eq(id))).thenReturn(saleReturn);
        // 乐观锁失败：updateByIdAndStatus 返回 0
        when(saleReturnMapper.updateByIdAndStatus(eq(id), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(0);

        assertException(() -> saleReturnService.updateSaleReturnStatus(id, ErpAuditStatus.APPROVE.getStatus()),
                ErrorCodeConstants.SALE_RETURN_APPROVE_FAIL);
        verify(stockRecordService, never()).createStockRecord(any());
    }

    // ==================== deleteSaleReturn ====================

    @Test
    public void testDeleteSaleReturn_processStatus_success() {
        Long id = 400L;
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO()
                .setId(id).setNo("XTH400")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setOrderId(null);
        when(saleReturnMapper.selectByIds(eq(Collections.singletonList(id))))
                .thenReturn(Collections.singletonList(saleReturn));

        saleReturnService.deleteSaleReturn(Collections.singletonList(id));

        verify(saleReturnMapper).deleteById(eq(id));
        verify(saleReturnItemMapper).deleteByReturnId(eq(id));
    }

    @Test
    public void testDeleteSaleReturn_approvedStatus_throwException() {
        Long id = 401L;
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO()
                .setId(id).setNo("XTH401")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(saleReturnMapper.selectByIds(eq(Collections.singletonList(id))))
                .thenReturn(Collections.singletonList(saleReturn));

        assertException(() -> saleReturnService.deleteSaleReturn(Collections.singletonList(id)),
                ErrorCodeConstants.SALE_RETURN_DELETE_FAIL_APPROVE, "XTH401");
        verify(saleReturnMapper, never()).deleteById(anyLong());
        verify(saleReturnItemMapper, never()).deleteByReturnId(anyLong());
    }

    // ==================== updateSaleReturnRefundPrice ====================

    @Test
    public void testUpdateSaleReturnRefundPrice_setRefund_success() {
        Long id = 500L;
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO()
                .setId(id).setNo("XTH500")
                .setRefundPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("100"));
        when(saleReturnMapper.selectById(eq(id))).thenReturn(saleReturn);

        saleReturnService.updateSaleReturnRefundPrice(id, new BigDecimal("50"));

        verify(saleReturnMapper).updateById(argThat((ErpSaleReturnDO upd) -> id.equals(upd.getId())
                && new BigDecimal("50").compareTo(upd.getRefundPrice()) == 0));
    }

    // ==================== Helper methods ====================

    private ErpSaleReturnSaveReqVO buildBaseReq(Integer returnMode) {
        ErpSaleReturnSaveReqVO reqVO = new ErpSaleReturnSaveReqVO();
        reqVO.setReturnMode(returnMode);
        reqVO.setReturnTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        reqVO.setAccountId(1L);
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        reqVO.setOtherPrice(BigDecimal.ZERO);
        return reqVO;
    }

    private ErpSaleReturnSaveReqVO.Item buildItem(BigDecimal count) {
        ErpSaleReturnSaveReqVO.Item item = new ErpSaleReturnSaveReqVO.Item();
        item.setWarehouseId(300L);
        item.setProductId(200L);
        item.setProductUnitId(400L);
        item.setProductPrice(new BigDecimal("10"));
        item.setCount(count);
        item.setTaxPercent(BigDecimal.ZERO);
        return item;
    }

    private void mockProduct() {
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(new ErpProductDO()
                .setId(200L).setUnitId(400L)));
    }

    /**
     * 校验抛出的 ServiceException 是否匹配指定错误码（按 code 比较，避免 message 参数差异）。
     */
    private void assertException(Runnable runnable, ErrorCode expected, Object... params) {
        ServiceException ex = assertThrows(ServiceException.class, runnable::run);
        assertEquals(expected.getCode(), ex.getCode(), "错误码不匹配");
    }

}
