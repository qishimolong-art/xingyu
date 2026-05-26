package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpOpeningBalanceUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpSubjectAuxiliaryDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAccountingSubjectService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpSubjectAuxiliaryService;
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

public class ErpAccountingSubjectControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAccountingSubjectController controller;

    @Mock
    private ErpAccountingSubjectService subjectService;

    @Mock
    private ErpSubjectAuxiliaryService subjectAuxiliaryService;

    // ==================== createSubject ====================

    @Test
    public void testCreateSubject_paramPassThrough() {
        ErpAccountingSubjectSaveReqVO reqVO = new ErpAccountingSubjectSaveReqVO();
        reqVO.setSubjectCode("1001");
        reqVO.setSubjectName("库存现金");
        reqVO.setSubjectCategory(1);
        when(subjectService.createSubject(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createSubject(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(subjectService).createSubject(eq(reqVO));
    }

    // ==================== updateSubject ====================

    @Test
    public void testUpdateSubject_paramPassThrough() {
        ErpAccountingSubjectSaveReqVO reqVO = new ErpAccountingSubjectSaveReqVO();
        reqVO.setId(1L);
        reqVO.setSubjectCode("1001");
        reqVO.setSubjectName("库存现金");
        reqVO.setSubjectCategory(1);

        CommonResult<Boolean> result = controller.updateSubject(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(subjectService).updateSubject(eq(reqVO));
    }

    // ==================== deleteSubject ====================

    @Test
    public void testDeleteSubject_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteSubject(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(subjectService).deleteSubject(eq(1L));
    }

    // ==================== getSubject ====================

    @Test
    public void testGetSubject_returnsNullWhenNotFound() {
        when(subjectService.getSubject(eq(1L))).thenReturn(null);

        CommonResult<ErpAccountingSubjectRespVO> result = controller.getSubject(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetSubject_fillsAuxiliaryTypes() {
        ErpAccountingSubjectDO subject = ErpAccountingSubjectDO.builder()
                .id(1L)
                .subjectCode("1001")
                .subjectName("库存现金")
                .subjectCategory(1)
                .isLeaf(true)
                .balanceDirection(1)
                .build();
        when(subjectService.getSubject(eq(1L))).thenReturn(subject);
        ErpSubjectAuxiliaryDO aux1 = ErpSubjectAuxiliaryDO.builder()
                .id(10L).subjectId(1L).auxiliaryType("supplier").build();
        ErpSubjectAuxiliaryDO aux2 = ErpSubjectAuxiliaryDO.builder()
                .id(11L).subjectId(1L).auxiliaryType("dept").build();
        when(subjectAuxiliaryService.getListBySubjectId(eq(1L))).thenReturn(Arrays.asList(aux1, aux2));

        CommonResult<ErpAccountingSubjectRespVO> result = controller.getSubject(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("1001", result.getData().getSubjectCode());
        assertEquals("库存现金", result.getData().getSubjectName());
        assertNotNull(result.getData().getAuxiliaryTypes());
        assertEquals(2, result.getData().getAuxiliaryTypes().size());
        assertEquals("supplier", result.getData().getAuxiliaryTypes().get(0));
        assertEquals("dept", result.getData().getAuxiliaryTypes().get(1));
        verify(subjectAuxiliaryService).getListBySubjectId(eq(1L));
    }

    // ==================== getSubjectList (list) ====================

    @Test
    public void testGetSubjectList_paramPassThrough() {
        ErpAccountingSubjectRespVO vo1 = new ErpAccountingSubjectRespVO();
        vo1.setId(1L);
        vo1.setSubjectCode("1001");
        ErpAccountingSubjectRespVO vo2 = new ErpAccountingSubjectRespVO();
        vo2.setId(2L);
        vo2.setSubjectCode("1002");
        when(subjectService.getSubjectTreeList(eq(1))).thenReturn(Arrays.asList(vo1, vo2));

        CommonResult<List<ErpAccountingSubjectRespVO>> result = controller.getSubjectList(1);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(1L, result.getData().get(0).getId());
        verify(subjectService).getSubjectTreeList(eq(1));
    }

    // ==================== getSubjectTree (tree) ====================

    @Test
    public void testGetSubjectTree_paramPassThrough() {
        ErpAccountingSubjectRespVO vo = new ErpAccountingSubjectRespVO();
        vo.setId(1L);
        vo.setSubjectCode("1001");
        when(subjectService.getSubjectTree()).thenReturn(Collections.singletonList(vo));

        CommonResult<List<ErpAccountingSubjectRespVO>> result = controller.getSubjectTree();

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(1L, result.getData().get(0).getId());
        verify(subjectService).getSubjectTree();
    }

    // ==================== getSimpleList (simpleList) ====================

    @Test
    public void testGetSimpleList_mapsFieldsManually() {
        ErpAccountingSubjectDO d1 = ErpAccountingSubjectDO.builder()
                .id(1L)
                .subjectCode("1001")
                .subjectName("库存现金")
                .shortName("现金")
                .subjectCategory(1)
                .isLeaf(true)
                .balanceDirection(1)
                .build();
        ErpAccountingSubjectDO d2 = ErpAccountingSubjectDO.builder()
                .id(2L)
                .subjectCode("1002")
                .subjectName("银行存款")
                .shortName("银行")
                .subjectCategory(1)
                .isLeaf(true)
                .balanceDirection(1)
                .build();
        when(subjectService.getSubjectSimpleList(eq(true), eq(1))).thenReturn(Arrays.asList(d1, d2));

        CommonResult<List<ErpAccountingSubjectRespVO>> result = controller.getSimpleList(true, 1);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        // 验证字段手动映射
        ErpAccountingSubjectRespVO vo1 = result.getData().get(0);
        assertEquals(1L, vo1.getId());
        assertEquals("1001", vo1.getSubjectCode());
        assertEquals("库存现金", vo1.getSubjectName());
        assertEquals("现金", vo1.getShortName());
        assertEquals(1, vo1.getSubjectCategory());
        assertEquals(Boolean.TRUE, vo1.getIsLeaf());
        assertEquals(1, vo1.getBalanceDirection());
        verify(subjectService).getSubjectSimpleList(eq(true), eq(1));
    }

    // ==================== getSubjectByCode ====================

    @Test
    public void testGetSubjectByCode_returnsNullWhenNotFound() {
        when(subjectService.getSubjectByCode(eq("9999"))).thenReturn(null);

        CommonResult<ErpAccountingSubjectRespVO> result = controller.getSubjectByCode("9999");

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetSubjectByCode_fillsAuxiliaryTypes() {
        ErpAccountingSubjectDO subject = ErpAccountingSubjectDO.builder()
                .id(1L)
                .subjectCode("1001")
                .subjectName("库存现金")
                .build();
        when(subjectService.getSubjectByCode(eq("1001"))).thenReturn(subject);
        ErpSubjectAuxiliaryDO aux = ErpSubjectAuxiliaryDO.builder()
                .id(10L).subjectId(1L).auxiliaryType("project").build();
        when(subjectAuxiliaryService.getListBySubjectId(eq(1L))).thenReturn(Collections.singletonList(aux));

        CommonResult<ErpAccountingSubjectRespVO> result = controller.getSubjectByCode("1001");

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("1001", result.getData().getSubjectCode());
        assertEquals(1, result.getData().getAuxiliaryTypes().size());
        assertEquals("project", result.getData().getAuxiliaryTypes().get(0));
    }

    // ==================== getAuxiliaryListBySubject ====================

    @Test
    public void testGetAuxiliaryListBySubject_returnsTypeStrings() {
        ErpSubjectAuxiliaryDO aux1 = ErpSubjectAuxiliaryDO.builder()
                .id(10L).subjectId(1L).auxiliaryType("supplier").build();
        ErpSubjectAuxiliaryDO aux2 = ErpSubjectAuxiliaryDO.builder()
                .id(11L).subjectId(1L).auxiliaryType("customer").build();
        when(subjectAuxiliaryService.getListBySubjectId(eq(1L))).thenReturn(Arrays.asList(aux1, aux2));

        CommonResult<List<String>> result = controller.getAuxiliaryListBySubject(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals("supplier", result.getData().get(0));
        assertEquals("customer", result.getData().get(1));
        verify(subjectAuxiliaryService).getListBySubjectId(eq(1L));
    }

    // ==================== batchUpdateOpeningBalance ====================

    @Test
    public void testBatchUpdateOpeningBalance_paramPassThrough() {
        ErpOpeningBalanceUpdateReqVO reqVO = new ErpOpeningBalanceUpdateReqVO();
        ErpOpeningBalanceUpdateReqVO.Item item = new ErpOpeningBalanceUpdateReqVO.Item();
        item.setId(1L);
        reqVO.setItems(Collections.singletonList(item));

        CommonResult<Boolean> result = controller.batchUpdateOpeningBalance(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(subjectService).batchUpdateOpeningBalance(eq(reqVO));
    }

}
