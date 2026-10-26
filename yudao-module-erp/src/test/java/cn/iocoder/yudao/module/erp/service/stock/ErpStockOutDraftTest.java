package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

class ErpStockOutDraftTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpStockOutServiceImpl service;
    @Mock private ErpStockOutMapper mapper;
    @Mock private ErpStockOutItemMapper itemMapper;
    @Mock private ErpProductService productService;
    @Mock private ErpWarehouseService warehouseService;
    @Mock private ErpCustomerService partnerService;
    @Mock private ErpNoRedisDAO noRedisDAO;
    @Mock private ErpOperateLogService operateLogService;
    @Mock private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock private ErpSalePickDeliveryService pickDeliveryService;
    @Mock private ErpStockRecordService recordService;
    @Spy private ErpStockItemSnapshotSupport snapshotSupport = new ErpStockItemSnapshotSupport();

    private ErpStockOutSaveReqVO request() {
        ErpStockOutSaveReqVO.Item item = new ErpStockOutSaveReqVO.Item();
        item.setProductId(1L); item.setWarehouseId(2L);
        item.setCount(new BigDecimal("1.234567")); item.setProductPrice(new BigDecimal("2.34"));
        ErpStockOutSaveReqVO req = new ErpStockOutSaveReqVO();
        req.setCustomerId(3L);
        req.setItems(Collections.singletonList(item));
        return req;
    }
    private void prepare() {
        when(productService.validProductList(anySet())).thenReturn(Collections.singletonList(new ErpProductDO().setId(1L)));
    }
    @Test void draftPersistsStatusZeroAndDoesNotConfirmOrPostInventory() {
        prepare();
        when(noRedisDAO.generate(anyString())).thenReturn("DRAFT-1");
        doAnswer(inv -> { ((ErpStockOutDO)inv.getArgument(0)).setId(5L); return 1; }).when(mapper).insert(any(ErpStockOutDO.class));
        ErpStockOutSaveReqVO req = request(); req.setId(999L); req.getItems().get(0).setProductPrice(null);
        req.setCustomerId(null);
        assertEquals(5L, service.createStockOutDraft(req));
        ArgumentCaptor<ErpStockOutDO> doc = ArgumentCaptor.forClass(ErpStockOutDO.class);
        verify(mapper).insert(doc.capture());
        assertEquals(0, doc.getValue().getStatus());
        assertEquals(new BigDecimal("1.234567"), doc.getValue().getTotalCount());
        assertEquals(0, doc.getValue().getTotalPrice().signum());
        verify(warehouseService).validateCurrentUserWarehousePermission(Collections.singleton(2L));
        verify(itemMapper).insertBatch(anyCollection());
        verifyNoInteractions(partnerService);
        verifyNoInteractions(pickDeliveryService, recordService);
    }
    @Test void ordinaryCreateStillUsesPendingConfirmation() {
        prepare(); when(noRedisDAO.generate(anyString())).thenReturn("NORMAL-1");
        service.createStockOut(request());
        ArgumentCaptor<ErpStockOutDO> doc = ArgumentCaptor.forClass(ErpStockOutDO.class);
        verify(mapper).insert(doc.capture()); assertEquals(10, doc.getValue().getStatus());
        verifyNoInteractions(pickDeliveryService, recordService);
    }
    @Test void invalidDraftDoesNotWriteAndRetainsWarehouseGuard() {
        ErpStockOutSaveReqVO req=request(); req.getItems().get(0).setCount(BigDecimal.ZERO);
        assertServiceException(() -> service.createStockOutDraft(req), STOCK_OUT_ITEM_EMPTY);
        verifyNoInteractions(mapper, itemMapper);
    }
    @Test void draftStillRejectsUnavailableWarehouseBeforeWriting() {
        prepare();
        doThrow(new IllegalArgumentException("warehouse denied")).when(warehouseService).validateCurrentUserWarehousePermission(anySet());
        assertThrows(IllegalArgumentException.class, () -> service.createStockOutDraft(request()));
        verifyNoInteractions(mapper, itemMapper);
    }
    @Test void emptyDraftDoesNotWrite() {
        ErpStockOutSaveReqVO req=request(); req.setItems(Collections.emptyList());
        assertServiceException(() -> service.createStockOutDraft(req), STOCK_OUT_ITEM_EMPTY);
        verifyNoInteractions(mapper, itemMapper);
    }
    @Test void editingAndSavingDraftUsesOriginalIdAndPromotesToPendingConfirmation() {
        prepare();
        when(mapper.selectById(5L)).thenReturn(new ErpStockOutDO().setId(5L).setStatus(0));
        ErpStockOutSaveReqVO req=request(); req.setId(5L);
        service.updateStockOut(req);
        ArgumentCaptor<ErpStockOutDO> doc=ArgumentCaptor.forClass(ErpStockOutDO.class);
        verify(mapper).updateById(doc.capture());
        assertEquals(5L, doc.getValue().getId()); assertEquals(10, doc.getValue().getStatus());
        verifyNoInteractions(pickDeliveryService, recordService);
    }
    @Test void draftCanBeDeletedButCannotBeConfirmedDirectly() {
        ErpStockOutDO doc=new ErpStockOutDO().setId(5L).setStatus(0);
        when(mapper.selectByIds(Collections.singletonList(5L))).thenReturn(Collections.singletonList(doc));
        service.deleteStockOut(Collections.singletonList(5L));
        verify(mapper).deleteById(5L); verify(itemMapper).deleteByOutId(5L);
        when(mapper.selectByIdForUpdate(5L)).thenReturn(doc);
        assertServiceException(() -> service.confirmStockOut(5L), STOCK_OUT_CONFIRM_FAIL);
        verifyNoInteractions(pickDeliveryService, recordService);
    }
}
