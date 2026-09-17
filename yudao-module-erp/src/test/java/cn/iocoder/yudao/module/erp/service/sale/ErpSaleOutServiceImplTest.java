package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutUpdateExpressFileReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_OUT_EXPRESS_FILE_TYPE_INVALID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ERP 销售出库 Service 单元测试
 *
 * @author 汽配ERP
 */
public class ErpSaleOutServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleOutServiceImpl saleOutService;
    @Mock
    private cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService tradeSnapshotService;

    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSaleOrderService saleOrderService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockOutBillService stockOutBillService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
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

    @Test
    public void testGetSaleOutPage_usesEffectiveReceiptPrice() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        PageResult<ErpSaleOutDO> page = new PageResult<>(
                Collections.singletonList(new ErpSaleOutDO().setId(60L)), 1L);
        when(saleOutMapper.selectPage(reqVO)).thenReturn(page);
        when(financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(any(), eq(21)))
                .thenReturn(Collections.singletonMap(60L, new BigDecimal("110")));

        PageResult<ErpSaleOutDO> result = saleOutService.getSaleOutPage(reqVO);

        assertEquals(new BigDecimal("110"), result.getList().get(0).getReceiptPrice());
    }

    @Test
    public void testGetSaleOutPage_receiptEnableUsesOriginalSettlementPrice() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        reqVO.setReceiptEnable(true);
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(61L)
                .setTotalPrice(new BigDecimal("560"))
                .setDiscountPercent(BigDecimal.ZERO)
                .setFeeAmount(BigDecimal.ZERO);
        when(saleOutMapper.selectPage(reqVO)).thenReturn(
                new PageResult<>(Collections.singletonList(saleOut), 1L));
        when(financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(any(), eq(21)))
                .thenReturn(Collections.singletonMap(61L, new BigDecimal("560")));
        when(saleOutItemMapper.selectListByOutIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpSaleOutItemDO().setOutId(61L).setCount(BigDecimal.ONE)
                        .setProductPrice(new BigDecimal("560"))
                        .setOriginalProductPrice(new BigDecimal("700"))));

        PageResult<ErpSaleOutDO> result = saleOutService.getSaleOutPage(reqVO);

        assertEquals(0, new BigDecimal("700").compareTo(result.getList().get(0).getTotalPrice()));
        assertEquals(new BigDecimal("560"), result.getList().get(0).getReceiptPrice());
    }
    @Mock
    private FileApi fileApi;

    @BeforeEach
    public void setUp() {
        // 既有业务夹具沿用原来源数据；真实锁与竞争由独立数据库/SourceLock测试验证。
        lenient().when(saleOutMapper.selectByIdForUpdate(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            ErpSaleOutDO source = saleOutMapper.selectById(id);
            if (source != null) return source;
            return saleOutMapper.selectByIds(Collections.singletonList(id)).stream()
                    .filter(item -> id.equals(item.getId())).findFirst().orElse(null);
        });
        lenient().when(saleOutItemMapper.selectListByOutIdForUpdate(anyLong()))
                .thenAnswer(invocation -> new java.util.ArrayList<>(saleOutItemMapper.selectListByOutId(invocation.getArgument(0))));
        ReflectionTestUtils.setField(saleOutService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
        lenient().when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(400L)));
        lenient().when(warehouseService.validSaleWarehouseListForDept(anyCollection(), any())).thenReturn(
                Collections.singletonList(new ErpWarehouseDO().setId(400L).setDeptId(10L)));
        lenient().when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), any())).thenReturn(
                Collections.singletonList(new ErpWarehouseDO().setId(400L).setDeptId(10L)));
        lenient().when(warehouseService.validSaleSelectableWarehouseListForDept(anyCollection(), any(), any())).thenReturn(
                Collections.singletonList(new ErpWarehouseDO().setId(400L).setDeptId(10L)));
        lenient().doNothing().when(warehouseService).validateWarehouseSaleAllowedForDept(any(), any());
        lenient().doNothing().when(warehouseService).validateWarehouseSaleSelectableForDept(any(), any());
        lenient().doNothing().when(warehouseService).validateWarehouseSaleSelectableForDept(any(), any(), any());
        lenient().when(warehouseService.isWarehouseSaleAllowedForDept(any(), any())).thenReturn(true);
        lenient().when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

    // ==================== createSaleOut ====================

    @Test
    public void testCreateSaleOut_normalCase_returnId() {
        // mock 入参
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setOrderId(5L);
        reqVO.setAccountId(1L);
        reqVO.setSaleUserId(7L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("3"))));

        // mock 依赖
        when(saleOrderService.validateSaleOrder(eq(5L))).thenReturn(
                new ErpSaleOrderDO().setId(5L).setNo("XSDD001").setCustomerId(20L));
        mockProduct();
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        when(saleOutMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleOutDO saleOut = invocation.getArgument(0);
            saleOut.setId(99L);
            return 1;
        }).when(saleOutMapper).insert(any(ErpSaleOutDO.class));
        when(saleOutMapper.selectListByOrderId(eq(5L))).thenReturn(Collections.emptyList());
        when(saleOutItemMapper.selectOrderItemCountSumMapByOutIds(anyCollection()))
                .thenReturn(Collections.emptyMap());

        // 执行
        Long id = saleOutService.createSaleOut(reqVO);

        // 断言
        assertEquals(99L, id);
        verify(adminUserApi).validateUser(eq(7L));
        verify(saleOutMapper).insert(argThat((ErpSaleOutDO saleOut) ->
                ErpAuditStatus.PROCESS.getStatus().equals(saleOut.getStatus())
                        && "XSDD001".equals(saleOut.getOrderNo())
                        && Long.valueOf(20L).equals(saleOut.getCustomerId())));
        verify(saleOutItemMapper).insertBatch(anyCollection());
        verify(saleOrderService).updateSaleOrderOutCount(eq(5L), any());
    }

    @Test
    public void testCreateSaleOut_invalidCustomer_throwException() {
        // 准备 ReqVO（不走 createSaleOut 的客户校验，但走 createGeneratedSaleOut 的客户校验）
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(99L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2"))));

        // mock 客户校验抛出异常
        when(customerService.validateCustomerForGeneratedSale(eq(99L), eq(10L)))
                .thenThrow(new ServiceException(1, "客户不存在"));

        // 验证 createGeneratedSaleOut 因 customer 校验失败抛错
        assertThrows(ServiceException.class,
                () -> saleOutService.createGeneratedSaleOut(reqVO,
                        ErpSaleBizSourceTypeEnum.QUOTE.getType(), 1L, "QUOTE001"));
        verify(saleOutMapper, never()).insert(any(ErpSaleOutDO.class));
    }

    @Test
    public void testCreateSaleOut_invalidWarehouse_throwException() {
        // 销售出库的 createSaleOut 没有直接校验仓库，但 validateSaleOutItems 中的产品校验失败也会抛
        // 这里模拟产品校验失败（间接代表"仓库/产品资料异常"场景）
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setOrderId(5L);
        reqVO.setAccountId(1L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2"))));

        when(saleOrderService.validateSaleOrder(eq(5L))).thenReturn(
                new ErpSaleOrderDO().setId(5L).setNo("XSDD001").setCustomerId(20L));
        when(productService.validProductList(anyCollection()))
                .thenThrow(new ServiceException(1, "产品/仓库相关资料异常"));

        // 验证
        assertThrows(ServiceException.class, () -> saleOutService.createSaleOut(reqVO));
        verify(saleOutMapper, never()).insert(any(ErpSaleOutDO.class));
    }

    // ==================== createGeneratedSaleOut ====================

    @Test
    public void testCreateGeneratedSaleOut_fromQuote_setSourceFields() {
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2"))));

        when(customerService.validateCustomerForGeneratedSale(eq(20L), eq(10L)))
                .thenReturn(new ErpCustomerDO().setId(20L));
        mockProduct();
        when(saleOutMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleOutDO saleOut = invocation.getArgument(0);
            saleOut.setId(101L);
            return 1;
        }).when(saleOutMapper).insert(any(ErpSaleOutDO.class));
        // 自动审核会调 updateSaleOutStatus，对应 mock
        mockApproveStatus(101L);

        Long id = saleOutService.createGeneratedSaleOut(reqVO,
                ErpSaleBizSourceTypeEnum.QUOTE.getType(), 555L, "BJ001");

        assertEquals(101L, id);
        verify(saleOutMapper).insert(argThat((ErpSaleOutDO saleOut) ->
                ErpSaleBizSourceTypeEnum.QUOTE.getType().equals(saleOut.getSourceType())
                        && Long.valueOf(555L).equals(saleOut.getSourceId())
                        && "BJ001".equals(saleOut.getSourceNo())
                        && saleOut.getOrderId() == null
                        && saleOut.getOrderNo() == null));
    }

    @Test
    public void testCreateGeneratedSaleOut_fromCart_setSourceFields() {
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        reqVO.setDeptId(102L);
        ErpSaleOutSaveReqVO.Item item = buildItem(new BigDecimal("2"));
        item.setSourceWarehouseId(401L);
        item.setSourceDeptId(201L);
        reqVO.setItems(Collections.singletonList(item));

        when(customerService.validateCustomerForGeneratedSale(eq(20L), eq(102L)))
                .thenReturn(new ErpCustomerDO().setId(20L));
        mockProduct();
        when(saleOutMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleOutDO saleOut = invocation.getArgument(0);
            saleOut.setId(102L);
            return 1;
        }).when(saleOutMapper).insert(any(ErpSaleOutDO.class));
        mockApproveStatus(102L);

        Long id = saleOutService.createGeneratedSaleOut(reqVO,
                ErpSaleBizSourceTypeEnum.CART.getType(), 666L, "CART001");

        assertEquals(102L, id);
        verify(saleOutMapper).insert(argThat((ErpSaleOutDO saleOut) ->
                ErpSaleBizSourceTypeEnum.CART.getType().equals(saleOut.getSourceType())
                        && Long.valueOf(666L).equals(saleOut.getSourceId())
                        && "CART001".equals(saleOut.getSourceNo())));
        verify(warehouseService, never()).validSaleWarehouseList(anyCollection());
        verify(warehouseService).validSaleSelectableWarehouseListForDept(anyCollection(), eq(102L),
                eq(ErpWarehouseService.SALE_OUT_ALL_PRODUCT_PERMISSION));
        verify(saleOutItemMapper).insertBatch(argThat((List<ErpSaleOutItemDO> items) ->
                items.size() == 1
                        && Long.valueOf(401L).equals(items.get(0).getSourceWarehouseId())
                        && Long.valueOf(201L).equals(items.get(0).getSourceDeptId())
                        && Long.valueOf(400L).equals(items.get(0).getWarehouseId())));
    }

    @Test
    public void testCreateGeneratedSaleOut_fromPriceAdjust_setSourceFields() {
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2"))));

        when(customerService.validateCustomerForGeneratedSale(eq(20L), eq(10L)))
                .thenReturn(new ErpCustomerDO().setId(20L));
        mockProduct();
        when(saleOutMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleOutDO saleOut = invocation.getArgument(0);
            saleOut.setId(103L);
            return 1;
        }).when(saleOutMapper).insert(any(ErpSaleOutDO.class));
        mockApproveStatus(103L);

        Long id = saleOutService.createGeneratedSaleOut(reqVO,
                ErpSaleBizSourceTypeEnum.PRICE_ADJUST.getType(), 777L, "TJ001");

        assertEquals(103L, id);
        verify(saleOutMapper).insert(argThat((ErpSaleOutDO saleOut) ->
                ErpSaleBizSourceTypeEnum.PRICE_ADJUST.getType().equals(saleOut.getSourceType())
                        && Long.valueOf(777L).equals(saleOut.getSourceId())
                        && "TJ001".equals(saleOut.getSourceNo())));
    }

    // ==================== updateSaleOut ====================

    @Test
    public void testUpdateSaleOut_processStatus_success() {
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setId(10L);
        reqVO.setOrderId(5L);
        reqVO.setAccountId(1L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("3"))));

        // 存在且未审核
        when(saleOutMapper.selectById(eq(10L))).thenReturn(
                new ErpSaleOutDO().setId(10L).setNo("XSCK001")
                        .setStatus(ErpAuditStatus.PROCESS.getStatus())
                        .setOrderId(5L));
        when(saleOrderService.validateSaleOrder(eq(5L))).thenReturn(
                new ErpSaleOrderDO().setId(5L).setNo("XSDD001").setCustomerId(20L));
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        mockProduct();
        when(saleOutItemMapper.selectListByOutId(eq(10L))).thenReturn(Collections.emptyList());
        when(saleOutMapper.selectListByOrderId(eq(5L))).thenReturn(Collections.emptyList());
        when(saleOutItemMapper.selectOrderItemCountSumMapByOutIds(anyCollection()))
                .thenReturn(Collections.emptyMap());

        // 执行
        saleOutService.updateSaleOut(reqVO);

        // 断言
        verify(saleOutMapper).updateById(any(ErpSaleOutDO.class));
        verify(saleOrderService).updateSaleOrderOutCount(eq(5L), any());
    }

    @Test
    public void testUpdateSaleOut_approvedStatus_throwException() {
        ErpSaleOutSaveReqVO reqVO = buildBaseReq();
        reqVO.setId(10L);

        when(saleOutMapper.selectById(eq(10L))).thenReturn(
                new ErpSaleOutDO().setId(10L).setNo("XSCK001")
                        .setStatus(ErpAuditStatus.APPROVE.getStatus()));

        assertThrows(ServiceException.class, () -> saleOutService.updateSaleOut(reqVO));
        verify(saleOutMapper, never()).updateById(any(ErpSaleOutDO.class));
    }

    @Test
    public void testUpdateSaleOutExpressFile_onlyUpdatesExpressField() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(10L)
                .setNo("XSCK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setFileUrl("https://example.com/general.pdf");
        when(saleOutMapper.selectById(eq(10L))).thenReturn(saleOut);
        ErpSaleOutUpdateExpressFileReqVO reqVO = new ErpSaleOutUpdateExpressFileReqVO();
        reqVO.setId(10L);
        reqVO.setExpressFileUrl("https://example.com/express.jpg");

        saleOutService.updateSaleOutExpressFile(reqVO);

        verify(saleOutMapper).updateById(argThat((ErpSaleOutDO updateObj) ->
                Long.valueOf(10L).equals(updateObj.getId())
                        && "https://example.com/express.jpg".equals(updateObj.getExpressFileUrl())
                        && updateObj.getFileUrl() == null
                        && updateObj.getStatus() == null
                        && updateObj.getCustomerId() == null
                        && updateObj.getTotalPrice() == null));
    }

    @Test
    public void testUpdateSaleOutExpressFile_notExists_throwException() {
        ErpSaleOutUpdateExpressFileReqVO reqVO = new ErpSaleOutUpdateExpressFileReqVO();
        reqVO.setId(999L);
        reqVO.setExpressFileUrl("https://example.com/express.jpg");
        when(saleOutMapper.selectById(eq(999L))).thenReturn(null);

        assertThrows(ServiceException.class, () -> saleOutService.updateSaleOutExpressFile(reqVO));

        verify(saleOutMapper, never()).updateById(any(ErpSaleOutDO.class));
    }

    @Test
    public void testUploadSaleOutExpressFile_uploadsAndOnlyUpdatesExpressField() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(10L).setNo("XSCK001");
        when(saleOutMapper.selectById(eq(10L))).thenReturn(saleOut);
        byte[] content = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        when(fileApi.createFile(any(byte[].class), eq("express.png"),
                eq("erp/sale-out/express/10"), eq("image/png")))
                .thenReturn("https://example.com/express.png");

        String result = saleOutService.uploadSaleOutExpressFile(10L, content, "express.png");

        assertEquals("https://example.com/express.png", result);
        verify(fileApi).createFile(argThat(bytes -> Arrays.equals(content, bytes)), eq("express.png"),
                eq("erp/sale-out/express/10"), eq("image/png"));
        verify(saleOutMapper).updateById(argThat((ErpSaleOutDO updateObj) ->
                Long.valueOf(10L).equals(updateObj.getId())
                        && "https://example.com/express.png".equals(updateObj.getExpressFileUrl())
                        && updateObj.getFileUrl() == null
                        && updateObj.getStatus() == null
                        && updateObj.getCustomerId() == null
                        && updateObj.getTotalPrice() == null));
    }

    @Test
    public void testUploadSaleOutExpressFile_rejectsSpoofedImage() {
        when(saleOutMapper.selectById(eq(10L))).thenReturn(new ErpSaleOutDO().setId(10L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> saleOutService.uploadSaleOutExpressFile(10L, new byte[]{1, 2, 3}, "express.png"));

        assertEquals(SALE_OUT_EXPRESS_FILE_TYPE_INVALID.getCode(), exception.getCode());
        verify(fileApi, never()).createFile(any(byte[].class), anyString(), anyString(), anyString());
        verify(saleOutMapper, never()).updateById(any(ErpSaleOutDO.class));
    }

    @Test
    public void testUploadSaleOutExpressFile_rejectsOversizedImage() {
        when(saleOutMapper.selectById(eq(10L))).thenReturn(new ErpSaleOutDO().setId(10L));
        byte[] oversizedContent = new byte[5 * 1024 * 1024 + 1];

        ServiceException exception = assertThrows(ServiceException.class,
                () -> saleOutService.uploadSaleOutExpressFile(10L, oversizedContent, "express.png"));

        assertEquals(SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED.getCode(), exception.getCode());
        verify(fileApi, never()).createFile(any(byte[].class), anyString(), anyString(), anyString());
        verify(saleOutMapper, never()).updateById(any(ErpSaleOutDO.class));
    }

    // ==================== updateSaleOutStatus ====================

    @Test
    public void testUpdateSaleOutStatus_approve_createStockRecordOut() {
        // 准备销售出库（PROCESS 状态）
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(20L).setNo("XSCK001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setReceiptPrice(BigDecimal.ZERO)
                .setOutTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(saleOutMapper.selectById(eq(20L))).thenReturn(saleOut);
        // 乐观锁更新成功
        when(saleOutMapper.updateByIdAndStatus(eq(20L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpSaleOutDO.class)))
                .thenReturn(1);
        // 出库项
        ErpSaleOutItemDO item = new ErpSaleOutItemDO()
                .setId(200L).setOutId(20L).setProductId(300L).setWarehouseId(400L)
                .setCount(new BigDecimal("5")).setProductPrice(new BigDecimal("10"));
        when(saleOutItemMapper.selectListByOutId(eq(20L))).thenReturn(Collections.singletonList(item));
        // 未开账：跳过凭证生成


        // 执行：审核通过
        saleOutService.updateSaleOutStatus(20L, ErpAuditStatus.APPROVE.getStatus());

        // 断言：扣库存
        verify(stockRecordService).createStockRecord(argThat((ErpStockRecordCreateReqBO bo) ->
                bo.getProductId().equals(300L) && bo.getWarehouseId().equals(400L)
                        // 销售出库的扣减是 count.negate()
                        && bo.getCount().compareTo(new BigDecimal("-5")) == 0
                        && bo.getUnitPrice().compareTo(new BigDecimal("10")) == 0));
        // 未开账不应建凭证
        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    public void testUpdateSaleOutStatus_processFromApprove_throwException() {
        // APPROVE 状态反审核
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(21L).setNo("XSCK002")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setReceiptPrice(BigDecimal.ZERO)
                .setOutTime(LocalDateTime.of(2026, 5, 20, 10, 0));

        // 执行 + 断言：当前销售出库状态接口只允许审批，不支持反审核
        assertThrows(ServiceException.class,
                () -> saleOutService.updateSaleOutStatus(21L, ErpAuditStatus.PROCESS.getStatus()));

        verify(saleOutMapper, never()).selectById(eq(21L));
        verify(saleOutMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
        verify(voucherMapper, never()).deleteById(anyLong());
        verify(voucherItemMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    public void testUpdateSaleOutStatus_optimisticLockFail_throwException() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(22L).setNo("XSCK003")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setReceiptPrice(BigDecimal.ZERO)
                .setOutTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(saleOutMapper.selectById(eq(22L))).thenReturn(saleOut);
        // 乐观锁失败：updateByIdAndStatus 返回 0
        when(saleOutMapper.updateByIdAndStatus(eq(22L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpSaleOutDO.class)))
                .thenReturn(0);

        // 执行 + 断言
        assertThrows(ServiceException.class,
                () -> saleOutService.updateSaleOutStatus(22L, ErpAuditStatus.APPROVE.getStatus()));
        verify(stockRecordService, never()).createStockRecord(any());
    }

    // ==================== deleteSaleOut ====================

    @Test
    public void testDeleteSaleOut_processStatus_success() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(30L).setNo("XSCK004")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setOrderId(5L);
        when(saleOutMapper.selectByIds(any())).thenReturn(Collections.singletonList(saleOut));
        when(saleOutMapper.selectListByOrderId(eq(5L))).thenReturn(Collections.emptyList());
        when(saleOutItemMapper.selectOrderItemCountSumMapByOutIds(anyCollection()))
                .thenReturn(Collections.emptyMap());

        saleOutService.deleteSaleOut(Collections.singletonList(30L));

        verify(saleOutMapper).deleteById(eq(30L));
        verify(saleOutItemMapper).deleteByOutId(eq(30L));
        verify(saleOrderService).updateSaleOrderOutCount(eq(5L), any());
    }

    @Test
    public void testDeleteSaleOut_hasReturn_throwException() {
        // 复用 task 描述：已审核的销售出库单不允许删除（业务上"有退货"的语义在此体现为不可删的硬约束）
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(31L).setNo("XSCK005")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(saleOutMapper.selectByIds(any())).thenReturn(Collections.singletonList(saleOut));

        assertThrows(ServiceException.class,
                () -> saleOutService.deleteSaleOut(Collections.singletonList(31L)));
        verify(saleOutMapper, never()).deleteById(anyLong());
    }

    // ==================== getReturnableItemsByOutId ====================

    @Test
    public void testGetReturnableItemsByOutId_partialReturned_returnRemaining() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(40L).setNo("XSCK006")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(saleOutMapper.selectById(eq(40L))).thenReturn(saleOut);
        ErpSaleOutItemDO item1 = new ErpSaleOutItemDO()
                .setId(500L).setOutId(40L).setProductId(600L).setWarehouseId(700L).setDeptId(800L)
                .setCount(new BigDecimal("10")).setProductPrice(new BigDecimal("12.34"));
        when(saleOutItemMapper.selectListByOutId(eq(40L))).thenReturn(Collections.singletonList(item1));
        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(600L);
        product.setCode("P600");
        product.setName("产品600");
        product.setBarCode("BAR600");
        product.setUnitName("件");
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(600L, product));
        when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Collections.singletonMap(700L,
                new ErpWarehouseDO().setId(700L).setName("仓库700")));
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(800L);
        dept.setName("部门800");
        when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.singletonMap(800L, dept));
        // 已退 3，剩余可退 7
        Map<Long, BigDecimal> returnedMap = new HashMap<>();
        returnedMap.put(500L, new BigDecimal("3"));
        when(saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(anyCollection()))
                .thenReturn(returnedMap);

        List<ErpSaleReturnableItemRespVO> resp = saleOutService.getReturnableItemsByOutId(40L);

        assertEquals(1, resp.size());
        ErpSaleReturnableItemRespVO vo = resp.get(0);
        assertEquals(40L, vo.getSourceOutId());
        assertEquals(500L, vo.getSourceOutItemId());
        assertEquals("P600", vo.getProductCode());
        assertEquals("产品600", vo.getProductName());
        assertEquals("BAR600", vo.getProductBarCode());
        assertEquals("件", vo.getProductUnitName());
        assertEquals("仓库700", vo.getWarehouseName());
        assertEquals("部门800", vo.getDeptName());
        assertEquals(0, new BigDecimal("10").compareTo(vo.getOutCount()));
        assertEquals(0, new BigDecimal("3").compareTo(vo.getReturnedCount()));
        assertEquals(0, new BigDecimal("7").compareTo(vo.getReturnableCount()));
    }

    @Test
    public void testGetReturnableItemsByOutId_allReturned_returnEmpty() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(41L).setNo("XSCK007")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(saleOutMapper.selectById(eq(41L))).thenReturn(saleOut);
        ErpSaleOutItemDO item = new ErpSaleOutItemDO()
                .setId(501L).setOutId(41L).setProductId(600L).setWarehouseId(700L)
                .setCount(new BigDecimal("5")).setProductPrice(new BigDecimal("12.34"));
        when(saleOutItemMapper.selectListByOutId(eq(41L))).thenReturn(Collections.singletonList(item));
        // 已退 5，剩余 0
        Map<Long, BigDecimal> returnedMap = new HashMap<>();
        returnedMap.put(501L, new BigDecimal("5"));
        when(saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(anyCollection()))
                .thenReturn(returnedMap);

        List<ErpSaleReturnableItemRespVO> resp = saleOutService.getReturnableItemsByOutId(41L);

        // 业务实现：仍返回 1 条，但 returnableCount = 0
        assertEquals(1, resp.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(resp.get(0).getReturnableCount()));
    }

    // ==================== updateSaleInReceiptPrice ====================

    @Test
    public void testUpdateSaleInReceiptPrice_setReceipt_success() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(50L).setNo("XSCK008")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setReceiptPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("100"));
        when(saleOutMapper.selectById(eq(50L))).thenReturn(saleOut);

        saleOutService.updateSaleInReceiptPrice(50L, new BigDecimal("60"));

        verify(saleOutMapper).updateById(argThat((ErpSaleOutDO updateObj) ->
                Long.valueOf(50L).equals(updateObj.getId())
                        && new BigDecimal("60").equals(updateObj.getReceiptPrice())));
    }

    // ==================== 工具方法 ====================

    private ErpSaleOutSaveReqVO buildBaseReq() {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setOutTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        reqVO.setOtherPrice(BigDecimal.ZERO);
        return reqVO;
    }

    private ErpSaleOutSaveReqVO.Item buildItem(BigDecimal count) {
        ErpSaleOutSaveReqVO.Item item = new ErpSaleOutSaveReqVO.Item();
        item.setWarehouseId(400L);
        item.setDeptId(10L);
        item.setProductId(300L);
        item.setProductUnitId(500L);
        item.setProductPrice(new BigDecimal("10"));
        item.setCount(count);
        item.setTaxPercent(BigDecimal.ZERO);
        return item;
    }

    private void mockProduct() {
        when(productService.validProductList(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpProductDO()
                        .setId(300L).setUnitId(500L)));
    }

    /**
     * mock updateSaleOutStatus(APPROVE) 链路：审核通过 + 未开账。
     */
    private void mockApproveStatus(Long id) {
        // 注意：createGeneratedSaleOut 插入后立即调 updateSaleOutStatus(APPROVE)，
        // 触发再次 selectById 以及更新状态、扣库存
        // 这里通过 lenient 避免严格 stubbing 检查
        lenient().when(saleOutMapper.selectById(eq(id))).thenAnswer(invocation ->
                new ErpSaleOutDO().setId(id).setNo("XSCK_GEN")
                        .setStatus(ErpAuditStatus.PROCESS.getStatus())
                        .setReceiptPrice(BigDecimal.ZERO)
                        .setOutTime(LocalDateTime.of(2026, 5, 20, 10, 0)));
        lenient().when(saleOutMapper.updateByIdAndStatus(eq(id), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpSaleOutDO.class)))
                .thenReturn(1);
        lenient().when(saleOutItemMapper.selectListByOutId(eq(id))).thenReturn(Collections.emptyList());
        lenient().when(bookOpenService.isVoucherTypeEnabled(any(), anyInt())).thenReturn(false);
    }

    // 仅声明 anyInt 静态导入：通过 Mockito 内部已有 anyInt
    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
