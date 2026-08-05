package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_ITEM_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUBMIT_NO_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class ErpPurchaseInvoiceServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInvoiceServiceImpl purchaseInvoiceService;

    @Mock
    private ErpPurchaseInvoiceMapper purchaseInvoiceMapper;
    @Mock
    private ErpPurchaseInvoiceItemMapper purchaseInvoiceItemMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(purchaseInvoiceService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
    }

    @Test
    public void testCreatePurchaseInvoice_fillsDeptIdFromSourceIn() {
        ErpPurchaseInvoiceSaveReqVO reqVO = new ErpPurchaseInvoiceSaveReqVO();
        reqVO.setSupplierId(100L);
        reqVO.setInvoiceDate(LocalDate.of(2026, 7, 27));
        reqVO.setInvoiceType("增值税专用发票");
        reqVO.setInvoiceNo("INV-20260727-001");
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setSourceInId(200L);
        item.setProductId(300L);
        item.setCount(BigDecimal.ONE);
        item.setProductPrice(new BigDecimal("10"));
        reqVO.setItems(Collections.singletonList(item));

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));
        when(purchaseInMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                new ErpPurchaseInDO().setId(200L).setDeptId(88L)));

        purchaseInvoiceService.createPurchaseInvoice(reqVO);

        ArgumentCaptor<ErpPurchaseInvoiceDO> captor = ArgumentCaptor.forClass(ErpPurchaseInvoiceDO.class);
        verify(purchaseInvoiceMapper).insert(captor.capture());
        assertEquals(Long.valueOf(88L), captor.getValue().getDeptId());
        assertEquals(ErpPurchaseInvoiceStatusEnum.PROCESS.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testCreatePurchaseInvoiceDraft_rejectsEmptyItems() {
        ErpPurchaseInvoiceDraftCreateReqVO reqVO = new ErpPurchaseInvoiceDraftCreateReqVO();
        reqVO.setItems(Collections.emptyList());

        assertServiceException(() -> purchaseInvoiceService.createPurchaseInvoiceDraft(reqVO),
                PURCHASE_INVOICE_ITEM_EMPTY);
        verify(purchaseInvoiceMapper, never()).insert(any(ErpPurchaseInvoiceDO.class));
        verify(purchaseInvoiceItemMapper, never()).insertBatch(any());
    }

    @Test
    public void testUpdatePurchaseInvoiceDraft_nonDraft_throwException() {
        ErpPurchaseInvoiceDraftUpdateReqVO reqVO = new ErpPurchaseInvoiceDraftUpdateReqVO();
        reqVO.setId(10L);
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001").setStatus(ErpPurchaseInvoiceStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> purchaseInvoiceService.updatePurchaseInvoiceDraft(reqVO),
                PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT, "CGPJ001");
        verify(purchaseInvoiceMapper, never()).updateDraftByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInvoiceDraft_replacesItemsAndKeepsDraftStatus() {
        ErpPurchaseInvoiceDraftUpdateReqVO reqVO = new ErpPurchaseInvoiceDraftUpdateReqVO();
        reqVO.setId(10L);
        reqVO.setItems(Collections.emptyList());
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001").setStatus(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()));
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L)).thenReturn(Collections.emptyList());
        when(purchaseInvoiceMapper.updateDraftByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        purchaseInvoiceService.updatePurchaseInvoiceDraft(reqVO);

        ArgumentCaptor<ErpPurchaseInvoiceDO> captor = ArgumentCaptor.forClass(ErpPurchaseInvoiceDO.class);
        verify(purchaseInvoiceMapper).updateDraftByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()), captor.capture());
        assertNull(captor.getValue().getStatus());
        assertEquals(BigDecimal.ZERO, captor.getValue().getTotalAmount());
        verify(purchaseInvoiceItemMapper).deleteByInvoiceId(10L);
        verify(purchaseInvoiceItemMapper, never()).insertBatch(any());
    }

    @Test
    public void testSubmitPurchaseInvoice_movesDraftToProcess() {
        ErpPurchaseInvoiceDO draft = new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setStatus(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus())
                .setSupplierId(100L)
                .setInvoiceDate(LocalDate.of(2026, 7, 27))
                .setInvoiceType("增值税专用发票")
                .setInvoiceNo("INV-20260727-001");
        ErpPurchaseInvoiceItemDO item = new ErpPurchaseInvoiceItemDO()
                .setId(20L).setInvoiceId(10L).setProductId(300L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN);
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(draft);
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L))
                .thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));
        when(purchaseInvoiceMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        purchaseInvoiceService.submitPurchaseInvoice(10L);

        verify(supplierService).validateSupplier(100L);
        verify(purchaseInvoiceMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()),
                org.mockito.ArgumentMatchers.argThat(update ->
                        ErpPurchaseInvoiceStatusEnum.PROCESS.getStatus().equals(update.getStatus())));
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdateAndSubmitPurchaseInvoiceDraft_persistsCurrentItemsBeforeSubmit() {
        ErpPurchaseInvoiceDO draft = new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setStatus(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus())
                .setSupplierId(100L)
                .setInvoiceDate(LocalDate.of(2026, 7, 27))
                .setInvoiceType("澧炲€肩◣涓撶敤鍙戠エ")
                .setInvoiceNo("INV-20260727-001");
        ErpPurchaseInvoiceDraftUpdateReqVO reqVO = new ErpPurchaseInvoiceDraftUpdateReqVO();
        reqVO.setId(10L);
        reqVO.setSupplierId(100L);
        reqVO.setInvoiceDate(LocalDate.of(2026, 7, 27));
        reqVO.setInvoiceType("澧炲€肩◣涓撶敤鍙戠エ");
        reqVO.setInvoiceNo("INV-20260727-001");
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setProductId(300L);
        item.setCount(BigDecimal.ONE);
        item.setProductPrice(BigDecimal.TEN);
        reqVO.setItems(Collections.singletonList(item));
        ErpPurchaseInvoiceItemDO persistedItem = new ErpPurchaseInvoiceItemDO()
                .setId(20L).setInvoiceId(10L).setProductId(300L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN);
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(draft);
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(persistedItem));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(purchaseInvoiceMapper.updateDraftByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);
        when(purchaseInvoiceMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        purchaseInvoiceService.updateAndSubmitPurchaseInvoiceDraft(reqVO);

        InOrder inOrder = inOrder(purchaseInvoiceItemMapper, purchaseInvoiceMapper);
        inOrder.verify(purchaseInvoiceItemMapper).deleteByInvoiceId(10L);
        inOrder.verify(purchaseInvoiceItemMapper).insertBatch(anyList());
        inOrder.verify(purchaseInvoiceMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()),
                org.mockito.ArgumentMatchers.argThat(update ->
                        ErpPurchaseInvoiceStatusEnum.PROCESS.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testSubmitPurchaseInvoice_rejectsEmptyItems() {
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setStatus(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus())
                .setSupplierId(100L)
                .setInvoiceDate(LocalDate.of(2026, 7, 27))
                .setInvoiceType("增值税专用发票")
                .setInvoiceNo("INV-20260727-001"));
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L)).thenReturn(Collections.emptyList());

        assertServiceException(() -> purchaseInvoiceService.submitPurchaseInvoice(10L),
                PURCHASE_INVOICE_ITEM_EMPTY);
        verify(purchaseInvoiceMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testSubmitPurchaseInvoice_rejectsBlankInvoiceNo() {
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setStatus(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus())
                .setSupplierId(100L)
                .setInvoiceDate(LocalDate.of(2026, 7, 27))
                .setInvoiceType("增值税专用发票")
                .setInvoiceNo(" "));

        assertServiceException(() -> purchaseInvoiceService.submitPurchaseInvoice(10L),
                PURCHASE_INVOICE_SUBMIT_NO_REQUIRED);
        verify(purchaseInvoiceItemMapper, never()).selectListByInvoiceId(anyLong());
        verify(purchaseInvoiceMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testSubmitPurchaseInvoice_rejectsNonDraft() {
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setStatus(ErpPurchaseInvoiceStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> purchaseInvoiceService.submitPurchaseInvoice(10L),
                PURCHASE_INVOICE_SUBMIT_FAIL);
        verify(purchaseInvoiceMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInvoiceStatus_approve_marksSourceInHasInvoice() {
        ErpPurchaseInvoiceDO invoice = new ErpPurchaseInvoiceDO();
        invoice.setId(10L);
        invoice.setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseInvoiceMapper.selectById(eq(10L))).thenReturn(invoice);
        when(purchaseInvoiceMapper.updateByIdAndStatus(eq(10L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(eq(10L))).thenReturn(Arrays.asList(
                new ErpPurchaseInvoiceItemDO().setSourceInId(100L),
                new ErpPurchaseInvoiceItemDO().setSourceInId(101L)
        ));
        when(purchaseInMapper.selectBatchIds(any())).thenReturn(Arrays.asList(
                new ErpPurchaseInDO().setId(100L),
                new ErpPurchaseInDO().setId(101L)
        ));

        purchaseInvoiceService.updatePurchaseInvoiceStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper, times(2)).updateById(captor.capture());
        assertEquals(100L, captor.getAllValues().get(0).getId().longValue());
        assertEquals(Boolean.TRUE, captor.getAllValues().get(0).getHasInvoice());
        assertEquals(101L, captor.getAllValues().get(1).getId().longValue());
        assertEquals(Boolean.TRUE, captor.getAllValues().get(1).getHasInvoice());
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), invoice.getStatus());
    }
}
