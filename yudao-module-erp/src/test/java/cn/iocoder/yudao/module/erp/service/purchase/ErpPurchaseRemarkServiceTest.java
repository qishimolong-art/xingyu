package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.validation.Validation;
import javax.validation.Validator;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 采购单据独立修改备注路径的回归测试。
 */
public class ErpPurchaseRemarkServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInServiceImpl purchaseInService;
    @InjectMocks
    private ErpPurchaseReturnServiceImpl purchaseReturnService;
    @InjectMocks
    private ErpPurchaseInvoiceServiceImpl purchaseInvoiceService;
    @InjectMocks
    private ErpPurchasePriceAdjustServiceImpl purchasePriceAdjustService;

    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchaseInvoiceMapper purchaseInvoiceMapper;
    @Mock
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    public void testUpdateRemarkRequest_validationBoundaries() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ErpPurchaseUpdateRemarkReqVO reqVO = req(1L, null);
        assertEquals(1, validator.validate(reqVO).size());

        reqVO.setRemark(new String(new char[501]).replace('\0', 'a'));
        assertEquals(1, validator.validate(reqVO).size());

        reqVO.setRemark("");
        assertEquals(0, validator.validate(reqVO).size());
    }

    @Test
    public void testUpdatePurchaseInRemark_approvedSuccess() {
        when(purchaseInMapper.selectById(eq(1L))).thenReturn(new ErpPurchaseInDO()
                .setId(1L).setNo("CGRK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));

        purchaseInService.updatePurchaseInRemark(req(1L, "审批后备注"));

        ErpPurchaseInDO update = capturePurchaseInUpdate();
        assertPartialUpdate(update, 1L, "审批后备注");
    }

    @Test
    public void testUpdatePurchaseInRemark_clearSuccess() {
        when(purchaseInMapper.selectById(eq(1L))).thenReturn(new ErpPurchaseInDO().setId(1L).setNo("CGRK001"));

        purchaseInService.updatePurchaseInRemark(req(1L, ""));

        assertEquals("", capturePurchaseInUpdate().getRemark());
    }

    @Test
    public void testUpdatePurchaseInRemark_notExists() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseInService.updatePurchaseInRemark(req(1L, "备注")));

        assertEquals(PURCHASE_IN_NOT_EXISTS.getCode(), ex.getCode());
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseReturnRemark_approvedSuccess() {
        when(purchaseReturnMapper.selectById(eq(2L))).thenReturn(new ErpPurchaseReturnDO()
                .setId(2L).setNo("CGTH001").setStatus(ErpAuditStatus.APPROVE.getStatus()));

        purchaseReturnService.updatePurchaseReturnRemark(req(2L, "审批后备注"));

        ErpPurchaseReturnDO update = capturePurchaseReturnUpdate();
        assertPartialUpdate(update, 2L, "审批后备注");
    }

    @Test
    public void testUpdatePurchaseReturnRemark_clearSuccess() {
        when(purchaseReturnMapper.selectById(eq(2L))).thenReturn(new ErpPurchaseReturnDO().setId(2L).setNo("CGTH001"));

        purchaseReturnService.updatePurchaseReturnRemark(req(2L, ""));

        assertEquals("", capturePurchaseReturnUpdate().getRemark());
    }

    @Test
    public void testUpdatePurchaseReturnRemark_notExists() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseReturnService.updatePurchaseReturnRemark(req(2L, "备注")));

        assertEquals(PURCHASE_RETURN_NOT_EXISTS.getCode(), ex.getCode());
        verify(purchaseReturnMapper, never()).updateById(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testUpdatePurchaseInvoiceRemark_approvedSuccess() {
        when(purchaseInvoiceMapper.selectById(eq(3L))).thenReturn(new ErpPurchaseInvoiceDO()
                .setId(3L).setNo("CGPJ001").setStatus(ErpAuditStatus.APPROVE.getStatus()));

        purchaseInvoiceService.updatePurchaseInvoiceRemark(req(3L, "审批后备注"));

        ErpPurchaseInvoiceDO update = capturePurchaseInvoiceUpdate();
        assertPartialUpdate(update, 3L, "审批后备注");
    }

    @Test
    public void testUpdatePurchaseInvoiceRemark_clearSuccess() {
        when(purchaseInvoiceMapper.selectById(eq(3L))).thenReturn(new ErpPurchaseInvoiceDO().setId(3L).setNo("CGPJ001"));

        purchaseInvoiceService.updatePurchaseInvoiceRemark(req(3L, ""));

        assertEquals("", capturePurchaseInvoiceUpdate().getRemark());
    }

    @Test
    public void testUpdatePurchaseInvoiceRemark_notExists() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseInvoiceService.updatePurchaseInvoiceRemark(req(3L, "备注")));

        assertEquals(PURCHASE_INVOICE_NOT_EXISTS.getCode(), ex.getCode());
        verify(purchaseInvoiceMapper, never()).updateById(any(ErpPurchaseInvoiceDO.class));
    }

    @Test
    public void testUpdatePurchasePriceAdjustRemark_approvedSuccess() {
        when(purchasePriceAdjustMapper.selectById(eq(4L))).thenReturn(new ErpPurchasePriceAdjustDO()
                .setId(4L).setNo("CGTJ001").setStatus(ErpAuditStatus.APPROVE.getStatus()));

        purchasePriceAdjustService.updatePurchasePriceAdjustRemark(req(4L, "审批后备注"));

        ErpPurchasePriceAdjustDO update = capturePurchasePriceAdjustUpdate();
        assertPartialUpdate(update, 4L, "审批后备注");
    }

    @Test
    public void testUpdatePurchasePriceAdjustRemark_clearSuccess() {
        when(purchasePriceAdjustMapper.selectById(eq(4L))).thenReturn(new ErpPurchasePriceAdjustDO()
                .setId(4L).setNo("CGTJ001"));

        purchasePriceAdjustService.updatePurchasePriceAdjustRemark(req(4L, ""));

        assertEquals("", capturePurchasePriceAdjustUpdate().getRemark());
    }

    @Test
    public void testUpdatePurchasePriceAdjustRemark_notExists() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchasePriceAdjustService.updatePurchasePriceAdjustRemark(req(4L, "备注")));

        assertEquals(PURCHASE_PRICE_ADJUST_NOT_EXISTS.getCode(), ex.getCode());
        verify(purchasePriceAdjustMapper, never()).updateById(any(ErpPurchasePriceAdjustDO.class));
    }

    private ErpPurchaseInDO capturePurchaseInUpdate() {
        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private ErpPurchaseReturnDO capturePurchaseReturnUpdate() {
        ArgumentCaptor<ErpPurchaseReturnDO> captor = ArgumentCaptor.forClass(ErpPurchaseReturnDO.class);
        verify(purchaseReturnMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private ErpPurchaseInvoiceDO capturePurchaseInvoiceUpdate() {
        ArgumentCaptor<ErpPurchaseInvoiceDO> captor = ArgumentCaptor.forClass(ErpPurchaseInvoiceDO.class);
        verify(purchaseInvoiceMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private ErpPurchasePriceAdjustDO capturePurchasePriceAdjustUpdate() {
        ArgumentCaptor<ErpPurchasePriceAdjustDO> captor = ArgumentCaptor.forClass(ErpPurchasePriceAdjustDO.class);
        verify(purchasePriceAdjustMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private static ErpPurchaseUpdateRemarkReqVO req(Long id, String remark) {
        ErpPurchaseUpdateRemarkReqVO reqVO = new ErpPurchaseUpdateRemarkReqVO();
        reqVO.setId(id);
        reqVO.setRemark(remark);
        return reqVO;
    }

    private static void assertPartialUpdate(Object update, Long id, String remark) {
        if (update instanceof ErpPurchaseInDO) {
            ErpPurchaseInDO purchaseIn = (ErpPurchaseInDO) update;
            assertEquals(id, purchaseIn.getId());
            assertEquals(remark, purchaseIn.getRemark());
            assertNull(purchaseIn.getStatus());
            assertNull(purchaseIn.getNo());
        } else if (update instanceof ErpPurchaseReturnDO) {
            ErpPurchaseReturnDO purchaseReturn = (ErpPurchaseReturnDO) update;
            assertEquals(id, purchaseReturn.getId());
            assertEquals(remark, purchaseReturn.getRemark());
            assertNull(purchaseReturn.getStatus());
            assertNull(purchaseReturn.getNo());
        } else if (update instanceof ErpPurchaseInvoiceDO) {
            ErpPurchaseInvoiceDO purchaseInvoice = (ErpPurchaseInvoiceDO) update;
            assertEquals(id, purchaseInvoice.getId());
            assertEquals(remark, purchaseInvoice.getRemark());
            assertNull(purchaseInvoice.getStatus());
            assertNull(purchaseInvoice.getNo());
        } else {
            ErpPurchasePriceAdjustDO priceAdjust = (ErpPurchasePriceAdjustDO) update;
            assertEquals(id, priceAdjust.getId());
            assertEquals(remark, priceAdjust.getRemark());
            assertNull(priceAdjust.getStatus());
            assertNull(priceAdjust.getNo());
        }
    }

}
