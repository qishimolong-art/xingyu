package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.validation.Validation;
import javax.validation.Validator;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_OUT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_ADJUST_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_QUOTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_RETURN_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售单据独立修改备注路径的回归测试。
 */
public class ErpSaleRemarkServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleCartServiceImpl saleCartService;
    @InjectMocks
    private ErpSaleQuoteServiceImpl saleQuoteService;
    @InjectMocks
    private ErpSaleOrderServiceImpl saleOrderService;
    @InjectMocks
    private ErpSaleOutServiceImpl saleOutService;
    @InjectMocks
    private ErpSaleReturnServiceImpl saleReturnService;
    @InjectMocks
    private ErpSalePriceAdjustServiceImpl salePriceAdjustService;

    @Mock
    private ErpSaleCartMapper saleCartMapper;
    @Mock
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Mock
    private ErpSaleOrderMapper saleOrderMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    public void testUpdateRemarkRequest_validationBoundaries() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ErpSaleUpdateRemarkReqVO reqVO = req(1L, null);
        assertEquals(1, validator.validate(reqVO).size());

        reqVO.setRemark(new String(new char[501]).replace('\0', 'a'));
        assertEquals(1, validator.validate(reqVO).size());

        reqVO.setRemark("");
        assertEquals(0, validator.validate(reqVO).size());
    }

    @Test
    public void testUpdateSaleCartRemark_approvedSuccess() {
        when(saleCartMapper.selectById(eq(1L))).thenReturn(new ErpSaleCartDO()
                .setId(1L).setNo("XSTC001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        saleCartService.updateSaleCartRemark(req(1L, "审批后备注"));
        assertPartialUpdate(capture(saleCartMapper, ErpSaleCartDO.class), 1L, "审批后备注");
    }

    @Test
    public void testUpdateSaleQuoteRemark_approvedSuccess() {
        when(saleQuoteMapper.selectById(eq(2L))).thenReturn(new ErpSaleQuoteDO()
                .setId(2L).setNo("XSBJ001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        saleQuoteService.updateSaleQuoteRemark(req(2L, "审批后备注"));
        assertPartialUpdate(capture(saleQuoteMapper, ErpSaleQuoteDO.class), 2L, "审批后备注");
    }

    @Test
    public void testUpdateSaleOrderRemark_approvedSuccess() {
        when(saleOrderMapper.selectById(eq(3L))).thenReturn(new ErpSaleOrderDO()
                .setId(3L).setNo("XSDD001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        saleOrderService.updateSaleOrderRemark(req(3L, "审批后备注"));
        assertPartialUpdate(capture(saleOrderMapper, ErpSaleOrderDO.class), 3L, "审批后备注");
    }

    @Test
    public void testUpdateSaleOutRemark_approvedSuccess() {
        when(saleOutMapper.selectById(eq(4L))).thenReturn(new ErpSaleOutDO()
                .setId(4L).setNo("XSCK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        saleOutService.updateSaleOutRemark(req(4L, "审批后备注"));
        assertPartialUpdate(capture(saleOutMapper, ErpSaleOutDO.class), 4L, "审批后备注");
    }

    @Test
    public void testUpdateSaleReturnRemark_approvedSuccess() {
        when(saleReturnMapper.selectById(eq(5L))).thenReturn(new ErpSaleReturnDO()
                .setId(5L).setNo("XSTH001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        saleReturnService.updateSaleReturnRemark(req(5L, "审批后备注"));
        assertPartialUpdate(capture(saleReturnMapper, ErpSaleReturnDO.class), 5L, "审批后备注");
    }

    @Test
    public void testUpdateSalePriceAdjustRemark_approvedSuccess() {
        when(salePriceAdjustMapper.selectById(eq(6L))).thenReturn(new ErpSalePriceAdjustDO()
                .setId(6L).setNo("XSTJ001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        salePriceAdjustService.updateSalePriceAdjustRemark(req(6L, "审批后备注"));
        assertPartialUpdate(capture(salePriceAdjustMapper, ErpSalePriceAdjustDO.class), 6L, "审批后备注");
    }

    @Test
    public void testUpdateRemark_unapprovedAndClearSuccess() {
        when(saleOrderMapper.selectById(eq(7L))).thenReturn(new ErpSaleOrderDO()
                .setId(7L).setNo("XSDD002").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        saleOrderService.updateSaleOrderRemark(req(7L, ""));
        assertPartialUpdate(capture(saleOrderMapper, ErpSaleOrderDO.class), 7L, "");
    }

    @Test
    public void testUpdateSaleCartRemark_notExists() {
        assertMissing(SALE_CART_NOT_EXISTS.getCode(),
                () -> saleCartService.updateSaleCartRemark(req(1L, "备注")));
        verify(saleCartMapper, never()).updateById(any(ErpSaleCartDO.class));
    }

    @Test
    public void testUpdateSaleQuoteRemark_notExists() {
        assertMissing(SALE_QUOTE_NOT_EXISTS.getCode(),
                () -> saleQuoteService.updateSaleQuoteRemark(req(2L, "备注")));
        verify(saleQuoteMapper, never()).updateById(any(ErpSaleQuoteDO.class));
    }

    @Test
    public void testUpdateSaleOrderRemark_notExists() {
        assertMissing(SALE_ORDER_NOT_EXISTS.getCode(),
                () -> saleOrderService.updateSaleOrderRemark(req(3L, "备注")));
        verify(saleOrderMapper, never()).updateById(any(ErpSaleOrderDO.class));
    }

    @Test
    public void testUpdateSaleOutRemark_notExists() {
        assertMissing(SALE_OUT_NOT_EXISTS.getCode(),
                () -> saleOutService.updateSaleOutRemark(req(4L, "备注")));
        verify(saleOutMapper, never()).updateById(any(ErpSaleOutDO.class));
    }

    @Test
    public void testUpdateSaleReturnRemark_notExists() {
        assertMissing(SALE_RETURN_NOT_EXISTS.getCode(),
                () -> saleReturnService.updateSaleReturnRemark(req(5L, "备注")));
        verify(saleReturnMapper, never()).updateById(any(ErpSaleReturnDO.class));
    }

    @Test
    public void testUpdateSalePriceAdjustRemark_notExists() {
        assertMissing(SALE_PRICE_ADJUST_NOT_EXISTS.getCode(),
                () -> salePriceAdjustService.updateSalePriceAdjustRemark(req(6L, "备注")));
        verify(salePriceAdjustMapper, never()).updateById(any(ErpSalePriceAdjustDO.class));
    }

    private static ErpSaleUpdateRemarkReqVO req(Long id, String remark) {
        ErpSaleUpdateRemarkReqVO reqVO = new ErpSaleUpdateRemarkReqVO();
        reqVO.setId(id);
        reqVO.setRemark(remark);
        return reqVO;
    }

    private static void assertMissing(int expectedCode, Runnable action) {
        ServiceException ex = assertThrows(ServiceException.class, action::run);
        assertEquals(expectedCode, ex.getCode());
    }

    private static <T> T capture(Object mapper, Class<T> type) {
        ArgumentCaptor<T> captor = ArgumentCaptor.forClass(type);
        if (mapper instanceof ErpSaleCartMapper) {
            verify((ErpSaleCartMapper) mapper).updateById((ErpSaleCartDO) captor.capture());
        } else if (mapper instanceof ErpSaleQuoteMapper) {
            verify((ErpSaleQuoteMapper) mapper).updateById((ErpSaleQuoteDO) captor.capture());
        } else if (mapper instanceof ErpSaleOrderMapper) {
            verify((ErpSaleOrderMapper) mapper).updateById((ErpSaleOrderDO) captor.capture());
        } else if (mapper instanceof ErpSaleOutMapper) {
            verify((ErpSaleOutMapper) mapper).updateById((ErpSaleOutDO) captor.capture());
        } else if (mapper instanceof ErpSaleReturnMapper) {
            verify((ErpSaleReturnMapper) mapper).updateById((ErpSaleReturnDO) captor.capture());
        } else {
            verify((ErpSalePriceAdjustMapper) mapper).updateById((ErpSalePriceAdjustDO) captor.capture());
        }
        return captor.getValue();
    }

    private static void assertPartialUpdate(Object update, Long id, String remark) {
        if (update instanceof ErpSaleCartDO) {
            ErpSaleCartDO entity = (ErpSaleCartDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else if (update instanceof ErpSaleQuoteDO) {
            ErpSaleQuoteDO entity = (ErpSaleQuoteDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else if (update instanceof ErpSaleOrderDO) {
            ErpSaleOrderDO entity = (ErpSaleOrderDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else if (update instanceof ErpSaleOutDO) {
            ErpSaleOutDO entity = (ErpSaleOutDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else if (update instanceof ErpSaleReturnDO) {
            ErpSaleReturnDO entity = (ErpSaleReturnDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else {
            ErpSalePriceAdjustDO entity = (ErpSalePriceAdjustDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        }
    }

}
