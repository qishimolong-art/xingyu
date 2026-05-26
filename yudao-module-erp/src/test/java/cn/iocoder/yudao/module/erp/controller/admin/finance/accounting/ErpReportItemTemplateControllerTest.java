package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpReportItemTemplateDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpReportItemTemplateService;
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

public class ErpReportItemTemplateControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpReportItemTemplateController controller;

    @Mock
    private ErpReportItemTemplateService reportItemTemplateService;

    // ==================== createReportItemTemplate ====================

    @Test
    public void testCreateReportItemTemplate_paramPassThrough() {
        ErpReportItemTemplateSaveReqVO reqVO = new ErpReportItemTemplateSaveReqVO();
        when(reportItemTemplateService.createReportItemTemplate(any())).thenReturn(66L);

        CommonResult<Long> result = controller.createReportItemTemplate(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(66L, result.getData());
        verify(reportItemTemplateService).createReportItemTemplate(eq(reqVO));
    }

    // ==================== updateReportItemTemplate ====================

    @Test
    public void testUpdateReportItemTemplate_paramPassThrough() {
        ErpReportItemTemplateSaveReqVO reqVO = new ErpReportItemTemplateSaveReqVO();

        CommonResult<Boolean> result = controller.updateReportItemTemplate(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(reportItemTemplateService).updateReportItemTemplate(eq(reqVO));
    }

    // ==================== deleteReportItemTemplate ====================

    @Test
    public void testDeleteReportItemTemplate_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteReportItemTemplate(4L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(reportItemTemplateService).deleteReportItemTemplate(eq(4L));
    }

    // ==================== getReportItemTemplate ====================

    @Test
    public void testGetReportItemTemplate_returnsRespVO() {
        ErpReportItemTemplateDO template = new ErpReportItemTemplateDO();
        template.setId(1L);
        template.setReportType(1);
        template.setItemName("货币资金");
        template.setRowNo(1);
        when(reportItemTemplateService.getReportItemTemplate(eq(1L))).thenReturn(template);

        CommonResult<ErpReportItemTemplateRespVO> result = controller.getReportItemTemplate(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals(1, result.getData().getReportType());
        assertEquals("货币资金", result.getData().getItemName());
    }

    @Test
    public void testGetReportItemTemplate_returnsNullWhenNotFound() {
        when(reportItemTemplateService.getReportItemTemplate(eq(99L))).thenReturn(null);

        CommonResult<ErpReportItemTemplateRespVO> result = controller.getReportItemTemplate(99L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    // ==================== getReportItemTemplateList ====================

    @Test
    public void testGetReportItemTemplateList_paramPassThrough() {
        ErpReportItemTemplateDO template = new ErpReportItemTemplateDO();
        template.setId(1L);
        template.setReportType(1);
        template.setSide(1);
        template.setItemName("流动资产");
        when(reportItemTemplateService.getReportItemTemplateList(eq(1), eq(1)))
                .thenReturn(Arrays.asList(template));

        CommonResult<List<ErpReportItemTemplateRespVO>> result =
                controller.getReportItemTemplateList(1, 1);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("流动资产", result.getData().get(0).getItemName());
        verify(reportItemTemplateService).getReportItemTemplateList(eq(1), eq(1));
    }

    @Test
    public void testGetReportItemTemplateList_sideNull() {
        when(reportItemTemplateService.getReportItemTemplateList(eq(2), eq(null)))
                .thenReturn(Collections.emptyList());

        CommonResult<List<ErpReportItemTemplateRespVO>> result =
                controller.getReportItemTemplateList(2, null);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
        verify(reportItemTemplateService).getReportItemTemplateList(eq(2), eq(null));
    }

    // ==================== getReportItemTemplatePage ====================

    @Test
    public void testGetReportItemTemplatePage_emptyList() {
        ErpReportItemTemplatePageReqVO pageReqVO = new ErpReportItemTemplatePageReqVO();
        PageResult<ErpReportItemTemplateDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(reportItemTemplateService.getReportItemTemplatePage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpReportItemTemplateRespVO>> result =
                controller.getReportItemTemplatePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(reportItemTemplateService).getReportItemTemplatePage(eq(pageReqVO));
    }

    @Test
    public void testGetReportItemTemplatePage_withResults() {
        ErpReportItemTemplatePageReqVO pageReqVO = new ErpReportItemTemplatePageReqVO();
        ErpReportItemTemplateDO template = new ErpReportItemTemplateDO();
        template.setId(1L);
        template.setItemName("应收账款");
        PageResult<ErpReportItemTemplateDO> pageResult = new PageResult<>(Arrays.asList(template), 1L);
        when(reportItemTemplateService.getReportItemTemplatePage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpReportItemTemplateRespVO>> result =
                controller.getReportItemTemplatePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals("应收账款", result.getData().getList().get(0).getItemName());
    }

}
