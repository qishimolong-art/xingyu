package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceMixedSourceItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceDocumentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceDocumentRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceSourceAllocationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceSourceAllocationMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceSourceTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_OVER_ALLOCATED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_SUPPLIER_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

class ErpPurchaseInvoiceSourceServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInvoiceSourceService sourceService;

    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpPurchasePriceAdjustMapper priceAdjustMapper;
    @Mock
    private ErpPurchasePriceAdjustItemMapper priceAdjustItemMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpPurchaseInvoiceSourceAllocationMapper allocationMapper;

    @Test
    void getSourceDocumentPage_usesRemainingAwarePurchaseInPage() {
        ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO = new ErpPurchaseInvoiceSourceDocumentPageReqVO();
        reqVO.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
        reqVO.setSupplierId(100L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        ErpPurchaseInDO source = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(100L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectInvoiceSourcePage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(source), 1L));
        when(purchaseInItemMapper.selectListByInIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(101L).setInId(10L).setProductId(301L)
                        .setCount(BigDecimal.TEN).setProductPrice(new BigDecimal("8"))));
        when(allocationMapper.selectAllocatedCountMap(eq(10), anyCollection()))
                .thenReturn(Map.of(101L, new BigDecimal("3")));

        PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> result = sourceService.getSourceDocumentPage(reqVO);

        verify(purchaseInMapper).selectInvoiceSourcePage(reqVO);
        assertEquals(1L, result.getTotal());
        assertEquals(0, new BigDecimal("7").compareTo(result.getList().get(0).getRemainingCount()));
        assertEquals("部分开票", result.getList().get(0).getInvoiceStatus());
    }

    @Test
    void getSourceDocumentPage_withoutTypeMergesSortsAndSlicesAllSources() {
        ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO = new ErpPurchaseInvoiceSourceDocumentPageReqVO();
        reqVO.setSupplierId(100L);
        reqVO.setKeyword("CG");
        reqVO.setPageNo(2);
        reqVO.setPageSize(2);
        ErpPurchaseInDO newest = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(100L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 10, 12, 10, 0));
        ErpPurchaseInDO third = new ErpPurchaseInDO().setId(11L).setNo("CGRK002").setSupplierId(100L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 10, 10, 10, 0));
        ErpPurchasePriceAdjustDO second = new ErpPurchasePriceAdjustDO().setId(20L).setNo("CGTJ001")
                .setSupplierId(100L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setAdjustTime(LocalDateTime.of(2026, 10, 11, 10, 0));
        ErpPurchaseReturnDO fourth = new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001")
                .setSupplierId(100L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setReturnTime(LocalDateTime.of(2026, 10, 9, 10, 0));
        when(purchaseInMapper.selectInvoiceSourcePage(any()))
                .thenReturn(new PageResult<>(Arrays.asList(newest, third), 2L));
        when(priceAdjustMapper.selectInvoiceSourcePage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(second), 1L));
        when(purchaseReturnMapper.selectInvoiceSourcePage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(fourth), 1L));
        when(purchaseInItemMapper.selectListByInIds(anyCollection())).thenReturn(Collections.emptyList());
        when(purchaseReturnItemMapper.selectListByReturnIds(anyCollection())).thenReturn(Collections.emptyList());

        PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> result = sourceService.getSourceDocumentPage(reqVO);

        assertEquals(4L, result.getTotal());
        assertEquals(Arrays.asList("CGRK002", "CGTH001"), result.getList().stream()
                .map(ErpPurchaseInvoiceSourceDocumentRespVO::getNo).collect(Collectors.toList()));
        ArgumentCaptor<ErpPurchaseInvoiceSourceDocumentPageReqVO> captor =
                ArgumentCaptor.forClass(ErpPurchaseInvoiceSourceDocumentPageReqVO.class);
        verify(purchaseInMapper).selectInvoiceSourcePage(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getPageNo());
        assertEquals(Integer.valueOf(4), captor.getValue().getPageSize());
        assertEquals("CG", captor.getValue().getKeyword());
    }

    @Test
    void getSourceDocumentPage_withoutTypeUsesStableTieBreakers() {
        ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO = new ErpPurchaseInvoiceSourceDocumentPageReqVO();
        reqVO.setSupplierId(100L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        LocalDateTime sameBizTime = LocalDateTime.of(2026, 10, 12, 10, 0);
        ErpPurchaseInDO inbound10 = new ErpPurchaseInDO().setId(10L).setNo("CGRK010").setSupplierId(100L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus()).setInTime(sameBizTime);
        ErpPurchaseInDO inbound11 = new ErpPurchaseInDO().setId(11L).setNo("CGRK011").setSupplierId(100L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus()).setInTime(sameBizTime);
        ErpPurchasePriceAdjustDO adjust = new ErpPurchasePriceAdjustDO().setId(30L).setNo("CGTJ030")
                .setSupplierId(100L).setStatus(ErpAuditStatus.APPROVE.getStatus()).setAdjustTime(sameBizTime);
        ErpPurchaseReturnDO purchaseReturn = new ErpPurchaseReturnDO().setId(40L).setNo("CGTH040")
                .setSupplierId(100L).setStatus(ErpAuditStatus.APPROVE.getStatus()).setReturnTime(sameBizTime);
        when(purchaseInMapper.selectInvoiceSourcePage(any()))
                .thenReturn(new PageResult<>(Arrays.asList(inbound10, inbound11), 2L));
        when(priceAdjustMapper.selectInvoiceSourcePage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(adjust), 1L));
        when(purchaseReturnMapper.selectInvoiceSourcePage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(purchaseReturn), 1L));

        PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> result = sourceService.getSourceDocumentPage(reqVO);

        assertEquals(Arrays.asList("CGRK011", "CGRK010", "CGTJ030", "CGTH040"), result.getList().stream()
                .map(ErpPurchaseInvoiceSourceDocumentRespVO::getNo).collect(Collectors.toList()));
    }

    @Test
    void getMixedSourceItemPage_returnsAllTypesWithCanonicalDirections() {
        ErpPurchaseInvoiceMixedSourceItemPageReqVO reqVO = new ErpPurchaseInvoiceMixedSourceItemPageReqVO();
        reqVO.setSupplierId(100L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSourceRefs(Arrays.asList(mixedRef(10, 10L), mixedRef(20, 20L), mixedRef(30, 30L)));
        when(purchaseInMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())
                        .setInTime(LocalDateTime.of(2026, 10, 12, 10, 0))));
        when(priceAdjustMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchasePriceAdjustDO().setId(20L).setNo("CGTJ001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())
                        .setAdjustTime(LocalDateTime.of(2026, 10, 11, 10, 0))));
        when(purchaseReturnMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())
                        .setReturnTime(LocalDateTime.of(2026, 10, 10, 10, 0))));
        when(purchaseInItemMapper.selectInvoiceMixedSourcePage(any())).thenReturn(new PageResult<>(
                Collections.singletonList(new ErpPurchaseInItemDO().setId(101L).setInId(10L).setProductId(301L)
                        .setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("10"))), 1L));
        when(priceAdjustItemMapper.selectInvoiceMixedSourcePage(any())).thenReturn(new PageResult<>(Arrays.asList(
                adjustItem(201L, 20L, 302L, "10", "12", "2", "4"),
                adjustItem(202L, 20L, 303L, "10", "8", "2", "-4")), 2L));
        when(purchaseReturnItemMapper.selectInvoiceMixedSourcePage(any())).thenReturn(new PageResult<>(
                Collections.singletonList(new ErpPurchaseReturnItemDO().setId(301L).setReturnId(30L)
                        .setProductId(304L).setCount(new BigDecimal("2")).setProductPrice(new BigDecimal("8"))), 1L));
        when(allocationMapper.selectAllocatedCountMap(any(), anyCollection())).thenReturn(Collections.emptyMap());

        PageResult<ErpPurchaseInvoiceItemDO> result = sourceService.getMixedSourceItemPage(reqVO);

        assertEquals(4L, result.getTotal());
        assertEquals(Arrays.asList(10, 20, 20, 30), result.getList().stream()
                .map(ErpPurchaseInvoiceItemDO::getSourceType).collect(Collectors.toList()));
        assertEquals(0, new BigDecimal("20").compareTo(result.getList().get(0).getTotalPrice()));
        assertEquals(0, new BigDecimal("4").compareTo(result.getList().get(1).getTotalPrice()));
        assertEquals(0, new BigDecimal("-4").compareTo(result.getList().get(2).getTotalPrice()));
        assertEquals(0, new BigDecimal("-16").compareTo(result.getList().get(3).getTotalPrice()));
    }

    @Test
    void getSourceItemPage_keepsPositiveAndNegativeAdjustments() {
        ErpPurchaseInvoiceSourceItemPageReqVO reqVO = new ErpPurchaseInvoiceSourceItemPageReqVO();
        reqVO.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST.getType());
        reqVO.setSourceDocIds(Collections.singletonList(20L));
        reqVO.setSupplierId(100L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        List<ErpPurchasePriceAdjustItemDO> sourceItems = Arrays.asList(
                adjustItem(201L, 20L, 301L, "10", "12", "5", "10"),
                adjustItem(202L, 20L, 302L, "10", "8", "5", "-10"));
        when(priceAdjustItemMapper.selectInvoiceSourcePage(reqVO)).thenReturn(new PageResult<>(sourceItems, 2L));
        when(priceAdjustMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchasePriceAdjustDO().setId(20L).setNo("CGTJ001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(allocationMapper.selectAllocatedCountMap(eq(20), anyCollection())).thenReturn(Collections.emptyMap());

        PageResult<ErpPurchaseInvoiceItemDO> result = sourceService.getSourceItemPage(reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getList().size());
        assertEquals(0, new BigDecimal("2").compareTo(result.getList().get(0).getProductPrice()));
        assertEquals(0, new BigDecimal("10").compareTo(result.getList().get(0).getTotalPrice()));
        assertEquals(0, new BigDecimal("-2").compareTo(result.getList().get(1).getProductPrice()));
        assertEquals(0, new BigDecimal("-10").compareTo(result.getList().get(1).getTotalPrice()));
    }

    @Test
    void getSourceItemPage_returnsNegativePurchaseReturnAmount() {
        ErpPurchaseInvoiceSourceItemPageReqVO reqVO = new ErpPurchaseInvoiceSourceItemPageReqVO();
        reqVO.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType());
        reqVO.setSourceDocIds(Collections.singletonList(30L));
        reqVO.setSupplierId(100L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        ErpPurchaseReturnItemDO sourceItem = new ErpPurchaseReturnItemDO().setId(301L).setReturnId(30L)
                .setProductId(401L).setCount(new BigDecimal("3")).setProductPrice(new BigDecimal("8"));
        when(purchaseReturnItemMapper.selectInvoiceSourcePage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(sourceItem), 1L));
        when(purchaseReturnMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(allocationMapper.selectAllocatedCountMap(eq(30), anyCollection())).thenReturn(Collections.emptyMap());

        ErpPurchaseInvoiceItemDO result = sourceService.getSourceItemPage(reqVO).getList().get(0);

        assertEquals(0, new BigDecimal("-24").compareTo(result.getTotalPrice()));
        assertEquals(Integer.valueOf(-1), result.getAmountDirection());
    }

    @Test
    void getSourceItemPage_rejectsOtherSupplier() {
        ErpPurchaseInvoiceSourceItemPageReqVO reqVO = new ErpPurchaseInvoiceSourceItemPageReqVO();
        reqVO.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType());
        reqVO.setSourceDocIds(Collections.singletonList(30L));
        reqVO.setSupplierId(100L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        ErpPurchaseReturnItemDO sourceItem = new ErpPurchaseReturnItemDO().setId(301L).setReturnId(30L)
                .setProductId(401L).setCount(BigDecimal.ONE).setProductPrice(new BigDecimal("8"));
        when(purchaseReturnItemMapper.selectInvoiceSourcePage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(sourceItem), 1L));
        when(purchaseReturnMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001").setSupplierId(101L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(allocationMapper.selectAllocatedCountMap(eq(30), anyCollection())).thenReturn(Collections.emptyMap());

        assertServiceException(() -> sourceService.getSourceItemPage(reqVO),
                PURCHASE_INVOICE_SOURCE_SUPPLIER_MISMATCH, "CGTH001");
    }

    @Test
    void normalizeAndValidateSourceItems_rejectsCountBeyondRemaining() {
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
        item.setSourceItemId(101L);
        item.setCount(new BigDecimal("3"));
        when(purchaseInItemMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(101L).setInId(10L).setProductId(301L)
                        .setCount(BigDecimal.TEN).setProductPrice(BigDecimal.ONE)));
        when(purchaseInMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(allocationMapper.selectAllocatedCountMap(eq(10), anyCollection()))
                .thenReturn(Map.of(101L, new BigDecimal("8")));

        assertServiceException(() -> sourceService.normalizeAndValidateSourceItems(
                Collections.singletonList(item), 100L), PURCHASE_INVOICE_SOURCE_OVER_ALLOCATED,
                "CGRK001", "2", "3");
    }

    @Test
    void resolveImportSourceItem_keepsNegativeAdjustmentDifference() {
        when(priceAdjustMapper.selectByNo("CGTJ001")).thenReturn(
                new ErpPurchasePriceAdjustDO().setId(20L).setNo("CGTJ001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus()));
        when(priceAdjustItemMapper.selectListByAdjustId(20L)).thenReturn(Collections.singletonList(
                adjustItem(201L, 20L, 301L, "10", "8", "5", "-10")));
        when(allocationMapper.selectAllocatedCountMap(eq(20), anyCollection())).thenReturn(Collections.emptyMap());

        ErpPurchaseInvoiceSaveReqVO.Item result = sourceService.resolveImportSourceItem(
                ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST.getType(), "CGTJ001", 301L,
                new BigDecimal("2"), 100L);

        assertEquals(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST.getType(), result.getSourceType());
        assertEquals(0, new BigDecimal("-2").compareTo(result.getProductPrice()));
        assertEquals(0, new BigDecimal("2").compareTo(result.getCount()));
        assertEquals(Integer.valueOf(-1), result.getAmountDirection());
    }

    @Test
    void resolveImportSourceItem_keepsPositiveReturnPriceAndNegativeDirection() {
        when(purchaseReturnMapper.selectByNo("CGTH001")).thenReturn(
                new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus()));
        when(purchaseReturnItemMapper.selectListByReturnId(30L)).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnItemDO().setId(301L).setReturnId(30L).setProductId(401L)
                        .setCount(new BigDecimal("3")).setProductPrice(new BigDecimal("8"))));
        when(allocationMapper.selectAllocatedCountMap(eq(30), anyCollection())).thenReturn(Collections.emptyMap());

        ErpPurchaseInvoiceSaveReqVO.Item result = sourceService.resolveImportSourceItem(
                ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType(), "CGTH001", 401L,
                new BigDecimal("2"), 100L);

        assertEquals(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType(), result.getSourceType());
        assertEquals(0, new BigDecimal("8").compareTo(result.getProductPrice()));
        assertEquals(0, new BigDecimal("2").compareTo(result.getCount()));
        assertEquals(Integer.valueOf(-1), result.getAmountDirection());
    }

    @Test
    void mergeSelectedSourceItems_existingInvoiceAppendsAllPagesAndPreservesIncrementalOperations() {
        ErpPurchaseInvoiceSaveReqVO reqVO = new ErpPurchaseInvoiceSaveReqVO();
        reqVO.setSupplierId(100L);
        ErpPurchaseInvoiceSaveReqVO.SourceRef sourceRef = new ErpPurchaseInvoiceSaveReqVO.SourceRef();
        sourceRef.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType());
        sourceRef.setSourceDocId(30L);
        reqVO.setSourceRefs(Collections.singletonList(sourceRef));

        ErpPurchaseInvoiceSaveReqVO.Item existingUpdate = new ErpPurchaseInvoiceSaveReqVO.Item();
        existingUpdate.setId(501L);
        existingUpdate.setOperation("update");
        existingUpdate.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
        existingUpdate.setSourceDocId(10L);
        existingUpdate.setSourceItemId(101L);
        existingUpdate.setProductId(401L);
        existingUpdate.setCount(BigDecimal.ONE);
        existingUpdate.setProductPrice(BigDecimal.TEN);
        ErpPurchaseInvoiceSaveReqVO.Item manualInsert = new ErpPurchaseInvoiceSaveReqVO.Item();
        manualInsert.setProductId(499L);
        manualInsert.setCount(BigDecimal.ONE);
        manualInsert.setProductPrice(new BigDecimal("5"));
        ErpPurchaseInvoiceSaveReqVO.Item visibleSourceItem = new ErpPurchaseInvoiceSaveReqVO.Item();
        visibleSourceItem.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType());
        visibleSourceItem.setSourceDocId(30L);
        visibleSourceItem.setSourceItemId(301L);
        visibleSourceItem.setProductId(401L);
        visibleSourceItem.setCount(BigDecimal.ONE);
        visibleSourceItem.setProductPrice(new BigDecimal("999"));
        reqVO.setItems(Arrays.asList(existingUpdate, manualInsert, visibleSourceItem));

        ErpPurchaseInvoiceItemDO oldItem = new ErpPurchaseInvoiceItemDO().setId(501L)
                .setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType())
                .setSourceDocId(10L).setSourceItemId(101L).setProductId(401L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN);
        when(purchaseReturnItemMapper.selectListByReturnIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpPurchaseReturnItemDO().setId(301L).setReturnId(30L).setProductId(401L)
                        .setCount(new BigDecimal("3")).setProductPrice(new BigDecimal("8")),
                new ErpPurchaseReturnItemDO().setId(302L).setReturnId(30L).setProductId(402L)
                        .setCount(new BigDecimal("4")).setProductPrice(new BigDecimal("6"))));
        when(purchaseReturnMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(allocationMapper.selectAllocatedCountMap(eq(30), anyCollection())).thenReturn(Collections.emptyMap());

        sourceService.mergeSelectedSourceItems(reqVO, Collections.singletonList(oldItem));

        assertEquals(4, reqVO.getItems().size());
        assertEquals("update", reqVO.getItems().get(0).getOperation());
        assertEquals("insert", reqVO.getItems().get(1).getOperation());
        Map<Long, ErpPurchaseInvoiceSaveReqVO.Item> sourceItems = reqVO.getItems().stream()
                .filter(item -> item.getSourceItemId() != null
                        && ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType().equals(item.getSourceType()))
                .collect(Collectors.toMap(ErpPurchaseInvoiceSaveReqVO.Item::getSourceItemId, Function.identity()));
        assertEquals(2, sourceItems.size());
        assertNotNull(sourceItems.get(301L));
        assertEquals("insert", sourceItems.get(301L).getOperation());
        assertEquals(0, BigDecimal.ONE.compareTo(sourceItems.get(301L).getCount()));
        assertEquals(0, new BigDecimal("8").compareTo(sourceItems.get(301L).getProductPrice()));
        assertNotNull(sourceItems.get(302L));
        assertEquals("insert", sourceItems.get(302L).getOperation());
        assertEquals(0, new BigDecimal("4").compareTo(sourceItems.get(302L).getCount()));
    }

    @Test
    void createApprovedAllocations_locksAndAggregatesDuplicateSourceRows() {
        ErpPurchaseInvoiceItemDO first = invoiceReturnItem(501L, 301L, "3", "999");
        ErpPurchaseInvoiceItemDO second = invoiceReturnItem(502L, 301L, "4", "999");
        when(purchaseReturnItemMapper.selectListByIdsForUpdate(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnItemDO().setId(301L).setReturnId(30L).setProductId(401L)
                        .setCount(BigDecimal.TEN).setProductPrice(new BigDecimal("8"))));
        when(purchaseReturnMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(allocationMapper.selectAllocatedCountMap(eq(30), anyCollection()))
                .thenReturn(Map.of(301L, new BigDecimal("2")));

        sourceService.createApprovedAllocations(900L, 100L, Arrays.asList(first, second));

        verify(purchaseReturnItemMapper).selectListByIdsForUpdate(anyCollection());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpPurchaseInvoiceSourceAllocationDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(allocationMapper).insertBatch(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(0, new BigDecimal("7").compareTo(captor.getValue().get(0).getAllocatedCount()));
        assertEquals(0, new BigDecimal("-56").compareTo(captor.getValue().get(0).getAllocatedAmount()));
        assertEquals(0, new BigDecimal("8").compareTo(first.getProductPrice()));
        assertEquals(0, new BigDecimal("-24").compareTo(first.getTotalPrice()));
        assertEquals(0, new BigDecimal("-32").compareTo(second.getTotalPrice()));
    }

    @Test
    void createApprovedAllocations_locksMixedSourcesInStableTypeOrder() {
        ErpPurchaseInvoiceItemDO returnItem = invoiceReturnItem(502L, 301L, "1", "-8");
        ErpPurchaseInvoiceItemDO inboundItem = new ErpPurchaseInvoiceItemDO().setId(501L)
                .setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType())
                .setSourceItemId(101L).setCount(BigDecimal.ONE).setTotalPrice(new BigDecimal("10"));
        when(purchaseInItemMapper.selectListByIdsForUpdate(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(101L).setInId(10L).setProductId(401L)
                        .setCount(BigDecimal.TEN).setProductPrice(BigDecimal.TEN)));
        when(purchaseInMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(purchaseReturnItemMapper.selectListByIdsForUpdate(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnItemDO().setId(301L).setReturnId(30L).setProductId(401L)
                        .setCount(BigDecimal.TEN).setProductPrice(new BigDecimal("8"))));
        when(purchaseReturnMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpPurchaseReturnDO().setId(30L).setNo("CGTH001").setSupplierId(100L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())));
        when(allocationMapper.selectAllocatedCountMap(any(), anyCollection())).thenReturn(Collections.emptyMap());

        sourceService.createApprovedAllocations(900L, 100L, Arrays.asList(returnItem, inboundItem));

        InOrder lockOrder = inOrder(purchaseInItemMapper, purchaseReturnItemMapper);
        lockOrder.verify(purchaseInItemMapper).selectListByIdsForUpdate(anyCollection());
        lockOrder.verify(purchaseReturnItemMapper).selectListByIdsForUpdate(anyCollection());
    }

    private static ErpPurchasePriceAdjustItemDO adjustItem(Long id, Long adjustId, Long productId,
                                                            String oldPrice, String newPrice,
                                                            String count, String adjustPrice) {
        return new ErpPurchasePriceAdjustItemDO().setId(id).setAdjustId(adjustId).setProductId(productId)
                .setOldPrice(new BigDecimal(oldPrice)).setNewPrice(new BigDecimal(newPrice))
                .setCount(new BigDecimal(count)).setAdjustPrice(new BigDecimal(adjustPrice));
    }

    private static ErpPurchaseInvoiceMixedSourceItemPageReqVO.SourceRef mixedRef(Integer sourceType,
                                                                                 Long sourceDocId) {
        ErpPurchaseInvoiceMixedSourceItemPageReqVO.SourceRef ref =
                new ErpPurchaseInvoiceMixedSourceItemPageReqVO.SourceRef();
        ref.setSourceType(sourceType);
        ref.setSourceDocId(sourceDocId);
        return ref;
    }

    private static ErpPurchaseInvoiceItemDO invoiceReturnItem(Long id, Long sourceItemId,
                                                               String count, String amount) {
        return new ErpPurchaseInvoiceItemDO().setId(id)
                .setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType())
                .setSourceItemId(sourceItemId).setCount(new BigDecimal(count))
                .setTotalPrice(new BigDecimal(amount));
    }
}
