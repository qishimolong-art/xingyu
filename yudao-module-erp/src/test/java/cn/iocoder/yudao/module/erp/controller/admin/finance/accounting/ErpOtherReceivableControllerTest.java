package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpOtherReceivableService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpOtherReceivableControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpOtherReceivableController controller;

    @Mock
    private ErpOtherReceivableService otherReceivableService;

    @Mock
    private ErpAccountService accountService;

    @Mock
    private AdminUserApi adminUserApi;

    // ==================== createOtherReceivable ====================

    @Test
    public void testCreateOtherReceivable_paramPassThrough() {
        ErpOtherReceivableSaveReqVO reqVO = new ErpOtherReceivableSaveReqVO();
        when(otherReceivableService.createOtherReceivable(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createOtherReceivable(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(otherReceivableService).createOtherReceivable(eq(reqVO));
    }

    // ==================== updateOtherReceivable ====================

    @Test
    public void testUpdateOtherReceivable_paramPassThrough() {
        ErpOtherReceivableSaveReqVO reqVO = new ErpOtherReceivableSaveReqVO();

        CommonResult<Boolean> result = controller.updateOtherReceivable(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(otherReceivableService).updateOtherReceivable(eq(reqVO));
    }

    // ==================== updateOtherReceivableStatus ====================

    @Test
    public void testUpdateOtherReceivableStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateOtherReceivableStatus(1L, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(otherReceivableService).updateOtherReceivableStatus(eq(1L), eq(20));
    }

    // ==================== deleteOtherReceivable ====================

    @Test
    public void testDeleteOtherReceivable_paramPassThrough() {
        List<Long> ids = Arrays.asList(1L, 2L);

        CommonResult<Boolean> result = controller.deleteOtherReceivable(ids);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(otherReceivableService).deleteOtherReceivable(eq(ids));
    }

    // ==================== getOtherReceivable ====================

    @Test
    public void testGetOtherReceivable_returnsNullWhenNotFound() {
        when(otherReceivableService.getOtherReceivable(eq(1L))).thenReturn(null);

        CommonResult<ErpOtherReceivableRespVO> result = controller.getOtherReceivable(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetOtherReceivable_paramPassThrough() {
        ErpOtherReceivableDO receivable = new ErpOtherReceivableDO();
        receivable.setId(1L);
        receivable.setNo("QTYS-001");
        when(otherReceivableService.getOtherReceivable(eq(1L))).thenReturn(receivable);
        ErpOtherReceivableItemDO item = new ErpOtherReceivableItemDO();
        item.setId(10L);
        when(otherReceivableService.getOtherReceivableItemListByReceivableId(eq(1L)))
                .thenReturn(Arrays.asList(item));

        CommonResult<ErpOtherReceivableRespVO> result = controller.getOtherReceivable(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("QTYS-001", result.getData().getNo());
        assertNotNull(result.getData().getItems());
        assertEquals(1, result.getData().getItems().size());
    }

    // ==================== getOtherReceivablePage ====================

    @Test
    public void testGetOtherReceivablePage_emptyList() {
        ErpOtherReceivablePageReqVO pageReqVO = new ErpOtherReceivablePageReqVO();
        PageResult<ErpOtherReceivableDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(otherReceivableService.getOtherReceivablePage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpOtherReceivableRespVO>> result = controller.getOtherReceivablePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(otherReceivableService).getOtherReceivablePage(eq(pageReqVO));
    }

}
