package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentItemDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpPrePaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpPrePaymentControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPrePaymentController controller;

    @Mock
    private ErpPrePaymentService prePaymentService;

    // ==================== createPrePayment ====================

    @Test
    public void testCreatePrePayment_paramPassThrough() {
        ErpPrePaymentSaveReqVO reqVO = new ErpPrePaymentSaveReqVO();
        when(prePaymentService.createPrePayment(any())).thenReturn(77L);

        CommonResult<Long> result = controller.createPrePayment(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(77L, result.getData());
        verify(prePaymentService).createPrePayment(eq(reqVO));
    }

    // ==================== updatePrePayment ====================

    @Test
    public void testUpdatePrePayment_paramPassThrough() {
        ErpPrePaymentSaveReqVO reqVO = new ErpPrePaymentSaveReqVO();

        CommonResult<Boolean> result = controller.updatePrePayment(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(prePaymentService).updatePrePayment(eq(reqVO));
    }

    // ==================== updatePrePaymentStatus ====================

    @Test
    public void testUpdatePrePaymentStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updatePrePaymentStatus(8L, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(prePaymentService).updatePrePaymentStatus(eq(8L), eq(20));
    }

    @Test
    public void testUpdatePrePaymentStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpPrePaymentController.class.getMethod("updatePrePaymentStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:pre-payment:update-status"));
    }

    @Test
    public void testUpdatePrePaymentStatus_rejectsProcessStatus() {
        assertThrows(RuntimeException.class, () -> controller.updatePrePaymentStatus(8L, 10));
        verify(prePaymentService, never()).updatePrePaymentStatus(any(), any());
    }

    // ==================== deletePrePayment ====================

    @Test
    public void testDeletePrePayment_paramPassThrough() {
        CommonResult<Boolean> result = controller.deletePrePayment(6L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(prePaymentService).deletePrePayment(eq(6L));
    }

    // ==================== getPrePayment ====================

    @Test
    public void testGetPrePayment_returnsNullWhenNotFound() {
        when(prePaymentService.getPrePayment(eq(1L))).thenReturn(null);

        CommonResult<ErpPrePaymentRespVO> result = controller.getPrePayment(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetPrePayment_buildsRespVOWithItems() {
        ErpPrePaymentDO prePayment = new ErpPrePaymentDO();
        prePayment.setId(1L);
        prePayment.setNo("YFKD-001");
        when(prePaymentService.getPrePayment(eq(1L))).thenReturn(prePayment);

        ErpPrePaymentItemDO item = new ErpPrePaymentItemDO();
        item.setId(12L);
        when(prePaymentService.getPrePaymentItemListByPrePaymentId(eq(1L)))
                .thenReturn(Arrays.asList(item));

        CommonResult<ErpPrePaymentRespVO> result = controller.getPrePayment(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("YFKD-001", result.getData().getNo());
        assertNotNull(result.getData().getItems());
        assertEquals(1, result.getData().getItems().size());
    }

    // ==================== getPrePaymentPage ====================

    @Test
    public void testGetPrePaymentPage_emptyList() {
        ErpPrePaymentPageReqVO pageReqVO = new ErpPrePaymentPageReqVO();
        PageResult<ErpPrePaymentDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(prePaymentService.getPrePaymentPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpPrePaymentRespVO>> result = controller.getPrePaymentPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(prePaymentService).getPrePaymentPage(eq(pageReqVO));
    }

    @Test
    public void testGetPrePaymentPage_withResults() {
        ErpPrePaymentPageReqVO pageReqVO = new ErpPrePaymentPageReqVO();
        pageReqVO.setIncludeItems(false);
        ErpPrePaymentDO prePayment = new ErpPrePaymentDO();
        prePayment.setId(1L);
        prePayment.setNo("YFKD-100");
        PageResult<ErpPrePaymentDO> pageResult = new PageResult<>(Arrays.asList(prePayment), 1L);
        when(prePaymentService.getPrePaymentPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpPrePaymentRespVO>> result = controller.getPrePaymentPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals("YFKD-100", result.getData().getList().get(0).getNo());
        verify(prePaymentService).getPrePaymentPage(eq(pageReqVO));
    }

}
