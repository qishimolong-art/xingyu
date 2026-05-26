package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionApplyReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateFromBizReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSearchSourceBizReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherAttributionDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherAttributionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpVoucherAttributionControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpVoucherAttributionController controller;

    @Mock
    private ErpVoucherAttributionService voucherAttributionService;

    // ==================== createVoucherAttribution ====================

    @Test
    public void testCreateVoucherAttribution_paramPassThrough() {
        ErpVoucherAttributionSaveReqVO reqVO = new ErpVoucherAttributionSaveReqVO();
        reqVO.setBizType(1);
        reqVO.setBizDate(LocalDateTime.of(2026, 5, 14, 10, 0));
        when(voucherAttributionService.createVoucherAttribution(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createVoucherAttribution(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(voucherAttributionService).createVoucherAttribution(eq(reqVO));
    }

    // ==================== updateVoucherAttribution ====================

    @Test
    public void testUpdateVoucherAttribution_paramPassThrough() {
        ErpVoucherAttributionSaveReqVO reqVO = new ErpVoucherAttributionSaveReqVO();
        reqVO.setId(1L);
        reqVO.setBizType(1);
        reqVO.setBizDate(LocalDateTime.of(2026, 5, 14, 10, 0));

        CommonResult<Boolean> result = controller.updateVoucherAttribution(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherAttributionService).updateVoucherAttribution(eq(reqVO));
    }

    // ==================== deleteVoucherAttribution ====================

    @Test
    public void testDeleteVoucherAttribution_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteVoucherAttribution(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherAttributionService).deleteVoucherAttribution(eq(1L));
    }

    // ==================== getVoucherAttribution ====================

    @Test
    public void testGetVoucherAttribution_paramPassThrough() {
        ErpVoucherAttributionDO attribution = new ErpVoucherAttributionDO();
        attribution.setId(1L);
        attribution.setBizType(1);
        attribution.setBizNo("CGRK202605000001");
        attribution.setBizDate(LocalDateTime.of(2026, 5, 14, 10, 0));
        attribution.setAttributionStatus(10);
        when(voucherAttributionService.getVoucherAttribution(eq(1L))).thenReturn(attribution);

        CommonResult<ErpVoucherAttributionRespVO> result = controller.getVoucherAttribution(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals(1, result.getData().getBizType());
        assertEquals("CGRK202605000001", result.getData().getBizNo());
        verify(voucherAttributionService).getVoucherAttribution(eq(1L));
    }

    // ==================== getVoucherAttributionPage ====================

    @Test
    public void testGetVoucherAttributionPage_emptyList() {
        ErpVoucherAttributionPageReqVO pageReqVO = new ErpVoucherAttributionPageReqVO();
        PageResult<ErpVoucherAttributionDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(voucherAttributionService.getVoucherAttributionPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpVoucherAttributionRespVO>> result =
                controller.getVoucherAttributionPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(voucherAttributionService).getVoucherAttributionPage(eq(pageReqVO));
    }

    @Test
    public void testGetVoucherAttributionPage_nonEmptyList() {
        ErpVoucherAttributionPageReqVO pageReqVO = new ErpVoucherAttributionPageReqVO();
        ErpVoucherAttributionDO attribution = new ErpVoucherAttributionDO();
        attribution.setId(1L);
        attribution.setBizType(1);
        attribution.setBizNo("CGRK202605000001");
        PageResult<ErpVoucherAttributionDO> pageResult =
                new PageResult<>(Arrays.asList(attribution), 1L);
        when(voucherAttributionService.getVoucherAttributionPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpVoucherAttributionRespVO>> result =
                controller.getVoucherAttributionPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(1L, result.getData().getList().get(0).getId());
    }

    // ==================== applyAttribution ====================

    @Test
    public void testApplyAttribution_paramPassThrough() {
        ErpVoucherAttributionApplyReqVO reqVO = new ErpVoucherAttributionApplyReqVO();
        reqVO.setIds(Arrays.asList(1L, 2L));
        reqVO.setAttributionYear(2026);
        reqVO.setAttributionMonth(5);

        CommonResult<Boolean> result = controller.applyAttribution(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherAttributionService).applyAttribution(eq(reqVO));
    }

    // ==================== generateVouchers ====================

    @Test
    public void testGenerateVouchers_paramPassThrough() {
        ErpVoucherAttributionGenerateReqVO reqVO = new ErpVoucherAttributionGenerateReqVO();
        reqVO.setIds(Arrays.asList(1L, 2L));
        when(voucherAttributionService.generateVouchers(any()))
                .thenReturn(Arrays.asList(100L, 101L));

        CommonResult<List<Long>> result = controller.generateVouchers(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(100L, result.getData().get(0));
        assertEquals(101L, result.getData().get(1));
        verify(voucherAttributionService).generateVouchers(eq(reqVO));
    }

    // ==================== generateVouchersFromBiz ====================

    @Test
    public void testGenerateVouchersFromBiz_paramPassThrough() {
        ErpVoucherAttributionGenerateFromBizReqVO reqVO = new ErpVoucherAttributionGenerateFromBizReqVO();
        ErpVoucherAttributionGenerateFromBizReqVO.BizItem item =
                new ErpVoucherAttributionGenerateFromBizReqVO.BizItem();
        item.setBizType(8);
        item.setBizId(1024L);
        reqVO.setItems(Collections.singletonList(item));
        reqVO.setVoucherMakeDate(LocalDate.of(2026, 5, 19));
        reqVO.setAttributionYear(2026);
        reqVO.setAttributionMonth(5);
        when(voucherAttributionService.generateVouchersFromBiz(any()))
                .thenReturn(Collections.singletonList(200L));

        CommonResult<List<Long>> result = controller.generateVouchersFromBiz(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(200L, result.getData().get(0));
        verify(voucherAttributionService).generateVouchersFromBiz(eq(reqVO));
    }

    // ==================== searchSourceBizPage ====================

    @Test
    public void testSearchSourceBizPage_paramPassThrough() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(8);
        reqVO.setBizDateStart(LocalDate.of(2026, 5, 1));
        reqVO.setBizDateEnd(LocalDate.of(2026, 5, 31));
        ErpVoucherAttributionRespVO vo = new ErpVoucherAttributionRespVO();
        vo.setId(1L);
        vo.setBizType(8);
        PageResult<ErpVoucherAttributionRespVO> pageResult =
                new PageResult<>(Collections.singletonList(vo), 1L);
        when(voucherAttributionService.searchSourceBizPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpVoucherAttributionRespVO>> result =
                controller.searchSourceBizPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(1L, result.getData().getList().get(0).getId());
        verify(voucherAttributionService).searchSourceBizPage(eq(reqVO));
    }

}
