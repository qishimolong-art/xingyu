package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayableRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableItemDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpOtherPayableService;
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

public class ErpOtherPayableControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpOtherPayableController controller;

    @Mock
    private ErpOtherPayableService otherPayableService;

    // ==================== createOtherPayable ====================

    @Test
    public void testCreateOtherPayable_paramPassThrough() {
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        when(otherPayableService.createOtherPayable(any())).thenReturn(88L);

        CommonResult<Long> result = controller.createOtherPayable(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(88L, result.getData());
        verify(otherPayableService).createOtherPayable(eq(reqVO));
    }

    // ==================== updateOtherPayable ====================

    @Test
    public void testUpdateOtherPayable_paramPassThrough() {
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();

        CommonResult<Boolean> result = controller.updateOtherPayable(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(otherPayableService).updateOtherPayable(eq(reqVO));
    }

    // ==================== updateOtherPayableStatus ====================

    @Test
    public void testUpdateOtherPayableStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateOtherPayableStatus(7L, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(otherPayableService).updateOtherPayableStatus(eq(7L), eq(20));
    }

    // ==================== deleteOtherPayable ====================

    @Test
    public void testDeleteOtherPayable_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteOtherPayable(5L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(otherPayableService).deleteOtherPayable(eq(5L));
    }

    // ==================== getOtherPayable ====================

    @Test
    public void testGetOtherPayable_returnsNullWhenNotFound() {
        when(otherPayableService.getOtherPayable(eq(1L))).thenReturn(null);

        CommonResult<ErpOtherPayableRespVO> result = controller.getOtherPayable(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetOtherPayable_buildsRespVOWithItems() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(1L);
        payable.setNo("QTYF-001");
        when(otherPayableService.getOtherPayable(eq(1L))).thenReturn(payable);

        ErpOtherPayableItemDO item = new ErpOtherPayableItemDO();
        item.setId(11L);
        when(otherPayableService.getOtherPayableItemListByPayableId(eq(1L)))
                .thenReturn(Arrays.asList(item));

        CommonResult<ErpOtherPayableRespVO> result = controller.getOtherPayable(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("QTYF-001", result.getData().getNo());
        assertNotNull(result.getData().getItems());
        assertEquals(1, result.getData().getItems().size());
    }

    // ==================== getOtherPayablePage ====================

    @Test
    public void testGetOtherPayablePage_emptyList() {
        ErpOtherPayablePageReqVO pageReqVO = new ErpOtherPayablePageReqVO();
        PageResult<ErpOtherPayableDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(otherPayableService.getOtherPayablePage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpOtherPayableRespVO>> result = controller.getOtherPayablePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(otherPayableService).getOtherPayablePage(eq(pageReqVO));
    }

    @Test
    public void testGetOtherPayablePage_withResults() {
        ErpOtherPayablePageReqVO pageReqVO = new ErpOtherPayablePageReqVO();
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(1L);
        payable.setNo("QTYF-100");
        PageResult<ErpOtherPayableDO> pageResult = new PageResult<>(Arrays.asList(payable), 1L);
        when(otherPayableService.getOtherPayablePage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpOtherPayableRespVO>> result = controller.getOtherPayablePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals("QTYF-100", result.getData().getList().get(0).getNo());
    }

}
