package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpVoucherControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpVoucherController controller;

    @Mock
    private ErpVoucherService voucherService;

    // ==================== createVoucher ====================

    @Test
    public void testCreateVoucher_paramPassThrough() {
        ErpVoucherSaveReqVO reqVO = new ErpVoucherSaveReqVO();
        when(voucherService.createVoucher(any())).thenReturn(66L);

        CommonResult<Long> result = controller.createVoucher(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(66L, result.getData());
        verify(voucherService).createVoucher(eq(reqVO));
    }

    // ==================== updateVoucher ====================

    @Test
    public void testUpdateVoucher_paramPassThrough() {
        ErpVoucherSaveReqVO reqVO = new ErpVoucherSaveReqVO();

        CommonResult<Boolean> result = controller.updateVoucher(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherService).updateVoucher(eq(reqVO));
    }

    // ==================== deleteVoucher ====================

    @Test
    public void testDeleteVoucher_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteVoucher(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherService).deleteVoucher(eq(1L));
    }

    // ==================== getVoucher ====================

    @Test
    public void testGetVoucher_returnsNullWhenNotFound() {
        when(voucherService.getVoucher(eq(1L))).thenReturn(null);

        CommonResult<ErpVoucherRespVO> result = controller.getVoucher(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetVoucher_paramPassThrough() {
        ErpVoucherDO voucher = new ErpVoucherDO();
        voucher.setId(1L);
        voucher.setVoucherNo("记-202605-000001");
        when(voucherService.getVoucher(eq(1L))).thenReturn(voucher);
        ErpVoucherItemDO item = new ErpVoucherItemDO();
        item.setId(10L);
        when(voucherService.getVoucherItemListByVoucherId(eq(1L)))
                .thenReturn(Arrays.asList(item));

        CommonResult<ErpVoucherRespVO> result = controller.getVoucher(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("记-202605-000001", result.getData().getVoucherNo());
        assertNotNull(result.getData().getItems());
        assertEquals(1, result.getData().getItems().size());
    }

    // ==================== getVoucherPage ====================

    @Test
    public void testGetVoucherPage_emptyList() {
        ErpVoucherPageReqVO pageReqVO = new ErpVoucherPageReqVO();
        PageResult<ErpVoucherDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(voucherService.getVoucherPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpVoucherRespVO>> result = controller.getVoucherPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(voucherService).getVoucherPage(eq(pageReqVO));
    }

    @Test
    public void testGetVoucherPage_nonEmptyList() {
        ErpVoucherPageReqVO pageReqVO = new ErpVoucherPageReqVO();
        ErpVoucherDO voucher = new ErpVoucherDO();
        voucher.setId(1L);
        voucher.setVoucherNo("记-202605-000001");
        PageResult<ErpVoucherDO> pageResult = new PageResult<>(Arrays.asList(voucher), 1L);
        when(voucherService.getVoucherPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpVoucherRespVO>> result = controller.getVoucherPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(1L, result.getData().getList().get(0).getId());
    }

    // ==================== auditVoucher ====================

    @Test
    public void testAuditVoucher_paramPassThrough() {
        CommonResult<Boolean> result = controller.auditVoucher(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherService).auditVoucher(eq(1L));
    }

    // ==================== processVoucher ====================

    @Test
    public void testProcessVoucher_paramPassThrough() {
        CommonResult<Boolean> result = controller.processVoucher(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherService).processVoucher(eq(1L));
    }

}
