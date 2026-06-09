package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
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
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
