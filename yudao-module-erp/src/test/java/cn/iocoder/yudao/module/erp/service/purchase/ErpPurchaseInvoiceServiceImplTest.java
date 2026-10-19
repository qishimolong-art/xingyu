package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_TOTAL_ZERO;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUPPLIER_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
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
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpPurchaseInvoiceSourceService purchaseInvoiceSourceService;
    @Mock
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;

    @BeforeEach
    public void setUp() {
        lenient().doNothing().when(purchaseInvoiceSourceService).mergeSelectedSourceItems(any(), anyList());
        lenient().when(supplierDeptPermissionService.hasAvailableDept(any(), anyLong(), eq("system_dept")))
                .thenReturn(true);
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
    public void testCreatePurchaseInvoice_sourceInIdsMergesSourceItems() {
        ErpPurchaseInvoiceSaveReqVO reqVO = new ErpPurchaseInvoiceSaveReqVO();
        reqVO.setSupplierId(100L);
        reqVO.setInvoiceDate(LocalDate.of(2026, 7, 27));
        reqVO.setInvoiceType("增值税专用发票");
        reqVO.setInvoiceNo("INV-20260727-001");
        reqVO.setSourceInIds(Collections.singletonList(200L));
        reqVO.setExcludedSourceInItemIds(Collections.singletonList(1002L));
        ErpPurchaseInvoiceSaveReqVO.Item editedItem = new ErpPurchaseInvoiceSaveReqVO.Item();
        editedItem.setSourceInId(200L);
        editedItem.setSourceInNo("CGRK001");
        editedItem.setSourceInItemId(1001L);
        editedItem.setProductId(300L);
        editedItem.setCount(new BigDecimal("2"));
        editedItem.setProductPrice(new BigDecimal("9"));
        editedItem.setRemark("分页改价");
        reqVO.setItems(Collections.singletonList(editedItem));

        when(purchaseInMapper.selectBatchIds(Collections.singleton(200L))).thenReturn(Collections.singletonList(
                new ErpPurchaseInDO().setId(200L).setNo("CGRK001")
                        .setStatus(ErpAuditStatus.APPROVE.getStatus()).setDeptId(88L)));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());

        purchaseInvoiceService.createPurchaseInvoice(reqVO);

        ArgumentCaptor<List> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInvoiceItemMapper).insertBatch(itemCaptor.capture());
        @SuppressWarnings("unchecked")
        List<ErpPurchaseInvoiceItemDO> insertedItems = itemCaptor.getValue();
        assertEquals(1, insertedItems.size());
        assertEquals(Long.valueOf(200L), insertedItems.get(0).getSourceInId());
        assertEquals("CGRK001", insertedItems.get(0).getSourceInNo());
        assertEquals(Long.valueOf(1001L), insertedItems.get(0).getSourceInItemId());
        assertEquals(new BigDecimal("2"), insertedItems.get(0).getCount());
        assertEquals(new BigDecimal("9"), insertedItems.get(0).getProductPrice());
        assertEquals(0, new BigDecimal("18").compareTo(insertedItems.get(0).getTotalPrice()));
        assertEquals("分页改价", insertedItems.get(0).getRemark());
    }

    @Test
    public void testCreatePurchaseInvoice_zeroTotal_throwException() {
        ErpPurchaseInvoiceSaveReqVO reqVO = new ErpPurchaseInvoiceSaveReqVO();
        reqVO.setSupplierId(100L);
        reqVO.setInvoiceDate(LocalDate.of(2026, 7, 27));
        reqVO.setInvoiceType("增值税专用发票");
        reqVO.setInvoiceNo("INV-20260727-ZERO");
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setProductId(300L);
        item.setCount(BigDecimal.ONE);
        item.setProductPrice(BigDecimal.ZERO);
        reqVO.setItems(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));

        assertServiceException(() -> purchaseInvoiceService.createPurchaseInvoice(reqVO),
                PURCHASE_INVOICE_TOTAL_ZERO);

        verify(purchaseInvoiceMapper, never()).insert(any(ErpPurchaseInvoiceDO.class));
        verify(purchaseInvoiceItemMapper, never()).insertBatch(any());
    }

    @Test
    public void testCreatePurchaseInvoiceDraft_zeroTotal_success() {
        ErpPurchaseInvoiceDraftCreateReqVO reqVO = new ErpPurchaseInvoiceDraftCreateReqVO();
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setProductId(300L);
        item.setCount(BigDecimal.ONE);
        item.setProductPrice(BigDecimal.ZERO);
        reqVO.setItems(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));

        purchaseInvoiceService.createPurchaseInvoiceDraft(reqVO);

        ArgumentCaptor<ErpPurchaseInvoiceDO> captor = ArgumentCaptor.forClass(ErpPurchaseInvoiceDO.class);
        verify(purchaseInvoiceMapper).insert(captor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getTotalAmount()));
        assertEquals(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus(), captor.getValue().getStatus());
        verify(purchaseInvoiceItemMapper).insertBatch(any());
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
    public void testCreatePurchaseInvoiceDraft_rejectsUnavailableSupplierDept() {
        ErpPurchaseInvoiceDraftCreateReqVO reqVO = new ErpPurchaseInvoiceDraftCreateReqVO();
        reqVO.setSupplierId(100L);
        reqVO.setDeptId(7L);
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setProductId(300L);
        item.setCount(BigDecimal.ONE);
        item.setProductPrice(BigDecimal.TEN);
        reqVO.setItems(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));
        ErpSupplierDO supplier = new ErpSupplierDO().setId(100L);
        when(supplierService.validateSupplier(100L)).thenReturn(supplier);
        when(supplierDeptPermissionService.hasAvailableDept(supplier, 7L, "system_dept"))
                .thenReturn(false);

        assertServiceException(() -> purchaseInvoiceService.createPurchaseInvoiceDraft(reqVO),
                PURCHASE_INVOICE_SUPPLIER_DEPT_NOT_ALLOWED);

        verify(purchaseInvoiceMapper, never()).insert(any(ErpPurchaseInvoiceDO.class));
    }

    @Test
    public void testGetSupplierAvailableDept_usesSystemDeptPermission() {
        DeptSimpleRespVO dept = new DeptSimpleRespVO().setId(7L).setName("采购部");
        PageParam pageParam = new PageParam().setPageNo(1).setPageSize(20);
        PageResult<DeptSimpleRespVO> page = new PageResult<>(Collections.singletonList(dept), 1L);
        when(supplierDeptPermissionService.getAvailableDeptSimpleList(100L, "system_dept"))
                .thenReturn(Collections.singletonList(dept));
        when(supplierDeptPermissionService.getAvailableDeptSimplePage(100L, "system_dept", pageParam))
                .thenReturn(page);

        assertEquals(1, purchaseInvoiceService.getSupplierAvailableDeptSimpleList(100L).size());
        assertTrue(page == purchaseInvoiceService.getSupplierAvailableDeptSimplePage(100L, pageParam));
        verify(supplierDeptPermissionService).getAvailableDeptSimpleList(100L, "system_dept");
        verify(supplierDeptPermissionService).getAvailableDeptSimplePage(100L, "system_dept", pageParam);
    }

    @Test
    public void testUpdatePurchaseInvoiceStatus_rejectsUnavailableSupplierDept() {
        ErpPurchaseInvoiceDO invoice = new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ10").setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSupplierId(100L).setDeptId(7L);
        ErpSupplierDO supplier = new ErpSupplierDO().setId(100L);
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(invoice);
        when(supplierService.validateSupplier(100L)).thenReturn(supplier);
        when(supplierDeptPermissionService.hasAvailableDept(supplier, 7L, "system_dept"))
                .thenReturn(false);

        assertServiceException(() -> purchaseInvoiceService.updatePurchaseInvoiceStatus(
                10L, ErpAuditStatus.APPROVE.getStatus()), PURCHASE_INVOICE_SUPPLIER_DEPT_NOT_ALLOWED);

        verify(purchaseInvoiceItemMapper, never()).selectListByInvoiceId(10L);
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
    public void testUpdatePurchaseInvoiceDraft_emptyIncrementalItemsKeepsDraftStatus() {
        ErpPurchaseInvoiceDraftUpdateReqVO reqVO = new ErpPurchaseInvoiceDraftUpdateReqVO();
        reqVO.setId(10L);
        reqVO.setItems(Collections.emptyList());
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001").setStatus(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()));
        when(purchaseInvoiceItemMapper.selectListByInvoiceIdForUpdate(10L)).thenReturn(Collections.emptyList());
        when(purchaseInvoiceMapper.updateDraftByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        purchaseInvoiceService.updatePurchaseInvoiceDraft(reqVO);

        ArgumentCaptor<ErpPurchaseInvoiceDO> captor = ArgumentCaptor.forClass(ErpPurchaseInvoiceDO.class);
        verify(purchaseInvoiceMapper).updateDraftByIdAndStatus(eq(10L),
                eq(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus()), captor.capture());
        assertNull(captor.getValue().getStatus());
        assertEquals(BigDecimal.ZERO, captor.getValue().getTotalAmount());
        verify(purchaseInvoiceItemMapper, never()).deleteByInvoiceId(10L);
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
        when(purchaseInvoiceItemMapper.selectListByInvoiceIdForUpdate(10L)).thenReturn(Collections.emptyList());
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L))
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
    public void testSubmitPurchaseInvoice_zeroTotal_throwException() {
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setStatus(ErpPurchaseInvoiceStatusEnum.DRAFT.getStatus())
                .setSupplierId(100L)
                .setInvoiceDate(LocalDate.of(2026, 7, 27))
                .setInvoiceType("增值税专用发票")
                .setInvoiceNo("INV-20260727-ZERO"));
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L)).thenReturn(Collections.singletonList(
                new ErpPurchaseInvoiceItemDO().setId(20L).setInvoiceId(10L).setProductId(300L)
                        .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.ZERO)));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L)));

        assertServiceException(() -> purchaseInvoiceService.submitPurchaseInvoice(10L),
                PURCHASE_INVOICE_TOTAL_ZERO);

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
        invoice.setTotalAmount(BigDecimal.TEN);
        when(purchaseInvoiceMapper.selectById(eq(10L))).thenReturn(invoice);
        when(purchaseInvoiceMapper.updateByIdAndStatus(eq(10L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(eq(10L))).thenReturn(Arrays.asList(
                new ErpPurchaseInvoiceItemDO().setSourceInId(100L).setTotalPrice(new BigDecimal("5")),
                new ErpPurchaseInvoiceItemDO().setSourceInId(101L).setTotalPrice(new BigDecimal("5"))
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

    @Test
    public void testUpdatePurchaseInvoiceStatus_recalculatesCanonicalSourceAmounts() {
        ErpPurchaseInvoiceDO invoice = new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setSupplierId(100L)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalAmount(new BigDecimal("999"));
        ErpPurchaseInvoiceItemDO returnItem = new ErpPurchaseInvoiceItemDO()
                .setId(20L).setInvoiceId(10L)
                .setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType())
                .setSourceItemId(301L).setProductId(400L)
                .setCount(new BigDecimal("2"))
                .setProductPrice(new BigDecimal("999"))
                .setTotalPrice(new BigDecimal("999"));
        ErpPurchaseInvoiceItemDO manualItem = new ErpPurchaseInvoiceItemDO()
                .setId(21L).setInvoiceId(10L).setProductId(401L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN)
                .setTotalPrice(BigDecimal.TEN);
        List<ErpPurchaseInvoiceItemDO> items = Arrays.asList(returnItem, manualItem);
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(invoice);
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L)).thenReturn(items);
        when(purchaseInvoiceMapper.updateByIdAndStatus(eq(10L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ErpPurchaseInvoiceItemDO> sourceItems = invocation.getArgument(2);
            sourceItems.get(0).setProductPrice(new BigDecimal("8"));
            sourceItems.get(0).setTaxExclusivePrice(new BigDecimal("-16"));
            sourceItems.get(0).setTaxPrice(BigDecimal.ZERO);
            sourceItems.get(0).setTotalPrice(new BigDecimal("-16"));
            return null;
        }).when(purchaseInvoiceSourceService).createApprovedAllocations(eq(10L), eq(100L), anyList());

        purchaseInvoiceService.updatePurchaseInvoiceStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpPurchaseInvoiceItemDO>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInvoiceItemMapper).updateBatch(itemCaptor.capture());
        assertEquals(1, itemCaptor.getValue().size());
        assertEquals(0, new BigDecimal("-16").compareTo(itemCaptor.getValue().get(0).getTotalPrice()));
        ArgumentCaptor<ErpPurchaseInvoiceDO> mainCaptor = ArgumentCaptor.forClass(ErpPurchaseInvoiceDO.class);
        verify(purchaseInvoiceMapper).updateByIdAndStatus(eq(10L), eq(ErpAuditStatus.PROCESS.getStatus()),
                mainCaptor.capture());
        assertEquals(0, new BigDecimal("-6").compareTo(mainCaptor.getValue().getTotalAmount()));
        assertEquals(ErpAuditStatus.APPROVE.getStatus(), mainCaptor.getValue().getStatus());
        assertNotNull(mainCaptor.getValue().getApproveTime());
    }

    @Test
    public void testUpdatePurchaseInvoiceStatus_recalculatedZeroTotal_throwException() {
        ErpPurchaseInvoiceDO invoice = new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setSupplierId(100L)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalAmount(new BigDecimal("999"));
        ErpPurchaseInvoiceItemDO sourceItem = new ErpPurchaseInvoiceItemDO()
                .setId(20L).setInvoiceId(10L)
                .setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType())
                .setSourceItemId(301L).setProductId(400L)
                .setCount(BigDecimal.ONE)
                .setProductPrice(new BigDecimal("999"))
                .setTotalPrice(new BigDecimal("999"));
        ErpPurchaseInvoiceItemDO manualItem = new ErpPurchaseInvoiceItemDO()
                .setId(21L).setInvoiceId(10L).setProductId(401L)
                .setCount(BigDecimal.ONE).setProductPrice(BigDecimal.TEN)
                .setTotalPrice(BigDecimal.TEN);
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(invoice);
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L))
                .thenReturn(Arrays.asList(sourceItem, manualItem));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ErpPurchaseInvoiceItemDO> sourceItems = invocation.getArgument(2);
            sourceItems.get(0).setProductPrice(BigDecimal.TEN);
            sourceItems.get(0).setTaxExclusivePrice(new BigDecimal("-10"));
            sourceItems.get(0).setTaxPrice(BigDecimal.ZERO);
            sourceItems.get(0).setTotalPrice(new BigDecimal("-10"));
            return null;
        }).when(purchaseInvoiceSourceService).createApprovedAllocations(eq(10L), eq(100L), anyList());

        assertServiceException(() -> purchaseInvoiceService.updatePurchaseInvoiceStatus(
                10L, ErpAuditStatus.APPROVE.getStatus()), PURCHASE_INVOICE_TOTAL_ZERO);

        verify(purchaseInvoiceItemMapper, never()).updateBatch(anyList());
        verify(purchaseInvoiceMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInvoiceStatus_zeroTotal_throwException() {
        ErpPurchaseInvoiceDO invoice = new ErpPurchaseInvoiceDO()
                .setId(10L).setNo("CGPJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalAmount(BigDecimal.ZERO);
        when(purchaseInvoiceMapper.selectById(10L)).thenReturn(invoice);
        when(purchaseInvoiceItemMapper.selectListByInvoiceId(10L)).thenReturn(Collections.emptyList());

        assertServiceException(() -> purchaseInvoiceService.updatePurchaseInvoiceStatus(
                10L, ErpAuditStatus.APPROVE.getStatus()), PURCHASE_INVOICE_TOTAL_ZERO);

        verify(purchaseInvoiceSourceService).createApprovedAllocations(eq(10L), any(), anyList());
        verify(purchaseInvoiceMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
    }

    @Test
    public void testImportPurchaseInvoice_defaultsBlankInvoiceDateToToday() {
        mockSuccessfulPurchaseInvoiceImport();
        ErpPurchaseInvoiceImportExcelVO row = buildPurchaseInvoiceImportRow();
        row.setInvoiceDate("   ");

        LocalDate before = LocalDate.now();
        ErpPurchaseImportResultRespVO result = purchaseInvoiceService.importPurchaseInvoiceList(
                Collections.singletonList(row));
        LocalDate after = LocalDate.now();

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpPurchaseInvoiceDO> captor = ArgumentCaptor.forClass(ErpPurchaseInvoiceDO.class);
        verify(purchaseInvoiceMapper).insert(captor.capture());
        LocalDate invoiceDate = captor.getValue().getInvoiceDate();
        assertNotNull(invoiceDate);
        assertTrue(!invoiceDate.isBefore(before));
        assertTrue(!invoiceDate.isAfter(after));
    }

    @Test
    public void testImportPurchaseInvoice_onlyProductName_success() {
        mockSuccessfulPurchaseInvoiceImport();
        ErpPurchaseInvoiceImportExcelVO row = buildPurchaseInvoiceImportRow();
        row.setProductCode(null);
        row.setProductName("产品1");

        ErpPurchaseImportResultRespVO result = purchaseInvoiceService.importPurchaseInvoiceList(
                Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInvoiceItemMapper).insertBatch(captor.capture());
        ErpPurchaseInvoiceItemDO item = (ErpPurchaseInvoiceItemDO) captor.getValue().get(0);
        assertEquals(Long.valueOf(300L), item.getProductId());
    }

    @Test
    public void testImportPurchaseInvoice_priceAdjustSourceUsesCanonicalNegativeDifference() {
        mockSuccessfulPurchaseInvoiceImport();
        ErpPurchaseInvoiceImportExcelVO row = buildPurchaseInvoiceImportRow();
        row.setSourceType("采购调价");
        row.setSourceDocNo("CGTJ001");
        row.setProductPrice(null);
        ErpPurchaseInvoiceSaveReqVO.Item sourceItem = new ErpPurchaseInvoiceSaveReqVO.Item();
        sourceItem.setSourceType(20);
        sourceItem.setSourceDocId(200L);
        sourceItem.setSourceDocNo("CGTJ001");
        sourceItem.setSourceItemId(201L);
        sourceItem.setProductId(300L);
        sourceItem.setCount(BigDecimal.ONE);
        sourceItem.setProductPrice(new BigDecimal("-2"));
        when(purchaseInvoiceSourceService.resolveImportSourceItem(
                eq(20), eq("CGTJ001"), eq(300L), eq(BigDecimal.ONE), eq(100L))).thenReturn(sourceItem);

        ErpPurchaseImportResultRespVO result = purchaseInvoiceService.importPurchaseInvoiceList(
                Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInvoiceItemMapper).insertBatch(captor.capture());
        ErpPurchaseInvoiceItemDO item = (ErpPurchaseInvoiceItemDO) captor.getValue().get(0);
        assertEquals(Integer.valueOf(20), item.getSourceType());
        assertEquals("CGTJ001", item.getSourceDocNo());
        assertEquals(0, new BigDecimal("-2").compareTo(item.getProductPrice()));
        assertEquals(0, new BigDecimal("-2").compareTo(item.getTotalPrice()));
    }

    @Test
    public void testImportPurchaseInvoice_invalidInvoiceDateReturnsFailure() {
        mockPurchaseInvoiceImportValidationLookups();
        ErpPurchaseInvoiceImportExcelVO row = buildPurchaseInvoiceImportRow();
        row.setInvoiceDate("2026/99/99");

        ErpPurchaseImportResultRespVO result = purchaseInvoiceService.importPurchaseInvoiceList(
                Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(Integer.valueOf(2), result.getFailureDetails().get(0).getRowNo());
        assertEquals("INV-20260702-001", result.getFailureDetails().get(0).getOrderNo());
        assertEquals("开票日期格式不正确，请使用 yyyy-MM-dd", result.getFailureDetails().get(0).getReason());
        verify(purchaseInvoiceMapper, never()).insert(any(ErpPurchaseInvoiceDO.class));
    }

    private ErpPurchaseInvoiceImportExcelVO buildPurchaseInvoiceImportRow() {
        ErpPurchaseInvoiceImportExcelVO row = new ErpPurchaseInvoiceImportExcelVO();
        row.setSupplierName("芋道供应商");
        row.setInvoiceDate("2026-07-02");
        row.setInvoiceType("增值税专用发票");
        row.setInvoiceNo("INV-20260702-001");
        row.setProductCode("P000001");
        row.setCount(BigDecimal.ONE);
        row.setProductPrice(BigDecimal.TEN);
        return row;
    }

    private void mockPurchaseInvoiceImportValidationLookups() {
        ErpSupplierDO supplier = new ErpSupplierDO().setId(100L).setName("芋道供应商")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        ErpProductDO product = new ErpProductDO().setId(300L).setCode("P000001").setName("产品1")
                .setUnitId(1L).setPurchasePrice(BigDecimal.TEN);
        lenient().when(supplierService.getSupplierPage(any())).thenReturn(new PageResult<>(
                Collections.singletonList(supplier), 1L));
        lenient().when(productMapper.selectListByCodes(any())).thenReturn(Collections.singletonList(product));
        lenient().when(productMapper.selectListByNames(any())).thenReturn(Collections.singletonList(product));
    }

    private void mockSuccessfulPurchaseInvoiceImport() {
        ErpProductDO product = new ErpProductDO().setId(300L).setCode("P000001").setName("产品1")
                .setUnitId(1L).setPurchasePrice(BigDecimal.TEN);
        mockPurchaseInvoiceImportValidationLookups();
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(product));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
    }
}
