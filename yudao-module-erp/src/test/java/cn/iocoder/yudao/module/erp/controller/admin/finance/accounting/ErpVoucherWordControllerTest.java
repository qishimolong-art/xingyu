package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherWordDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherWordService;
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

public class ErpVoucherWordControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpVoucherWordController controller;

    @Mock
    private ErpVoucherWordService voucherWordService;

    // ==================== createVoucherWord ====================

    @Test
    public void testCreateVoucherWord_paramPassThrough() {
        ErpVoucherWordSaveReqVO reqVO = new ErpVoucherWordSaveReqVO();
        when(voucherWordService.createVoucherWord(any())).thenReturn(55L);

        CommonResult<Long> result = controller.createVoucherWord(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(55L, result.getData());
        verify(voucherWordService).createVoucherWord(eq(reqVO));
    }

    // ==================== updateVoucherWord ====================

    @Test
    public void testUpdateVoucherWord_paramPassThrough() {
        ErpVoucherWordSaveReqVO reqVO = new ErpVoucherWordSaveReqVO();

        CommonResult<Boolean> result = controller.updateVoucherWord(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherWordService).updateVoucherWord(eq(reqVO));
    }

    // ==================== deleteVoucherWord ====================

    @Test
    public void testDeleteVoucherWord_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteVoucherWord(3L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(voucherWordService).deleteVoucherWord(eq(3L));
    }

    // ==================== getVoucherWord ====================

    @Test
    public void testGetVoucherWord_returnsRespVO() {
        ErpVoucherWordDO word = new ErpVoucherWordDO();
        word.setId(1L);
        word.setCode("记");
        word.setName("记账凭证");
        word.setEnable(true);
        word.setSort(1);
        when(voucherWordService.getVoucherWord(eq(1L))).thenReturn(word);

        CommonResult<ErpVoucherWordRespVO> result = controller.getVoucherWord(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("记", result.getData().getCode());
        assertEquals("记账凭证", result.getData().getName());
    }

    @Test
    public void testGetVoucherWord_returnsEmptyWhenNotFound() {
        when(voucherWordService.getVoucherWord(eq(99L))).thenReturn(null);

        CommonResult<ErpVoucherWordRespVO> result = controller.getVoucherWord(99L);

        assertEquals(0, result.getCode());
        // BeanUtils.toBean(null, ...) yields null
        assertNull(result.getData());
    }

    // ==================== getVoucherWordPage ====================

    @Test
    public void testGetVoucherWordPage_emptyList() {
        ErpVoucherWordPageReqVO pageReqVO = new ErpVoucherWordPageReqVO();
        PageResult<ErpVoucherWordDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(voucherWordService.getVoucherWordPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpVoucherWordRespVO>> result = controller.getVoucherWordPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(voucherWordService).getVoucherWordPage(eq(pageReqVO));
    }

    @Test
    public void testGetVoucherWordPage_withResults() {
        ErpVoucherWordPageReqVO pageReqVO = new ErpVoucherWordPageReqVO();
        ErpVoucherWordDO word = new ErpVoucherWordDO();
        word.setId(1L);
        word.setCode("记");
        word.setName("记账凭证");
        PageResult<ErpVoucherWordDO> pageResult = new PageResult<>(Arrays.asList(word), 1L);
        when(voucherWordService.getVoucherWordPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpVoucherWordRespVO>> result = controller.getVoucherWordPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals("记", result.getData().getList().get(0).getCode());
    }

    // ==================== getVoucherWordSimpleList ====================

    @Test
    public void testGetVoucherWordSimpleList_mapsManually() {
        ErpVoucherWordDO word1 = new ErpVoucherWordDO();
        word1.setId(1L);
        word1.setCode("记");
        word1.setName("记账凭证");
        word1.setSort(1);
        ErpVoucherWordDO word2 = new ErpVoucherWordDO();
        word2.setId(2L);
        word2.setCode("收");
        word2.setName("收款凭证");
        word2.setSort(2);
        when(voucherWordService.getEnabledVoucherWordList())
                .thenReturn(Arrays.asList(word1, word2));

        CommonResult<List<ErpVoucherWordRespVO>> result = controller.getVoucherWordSimpleList();

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(1L, result.getData().get(0).getId());
        assertEquals("记", result.getData().get(0).getCode());
        assertEquals("记账凭证", result.getData().get(0).getName());
        assertEquals(1, result.getData().get(0).getSort());
        assertEquals("收", result.getData().get(1).getCode());
        verify(voucherWordService).getEnabledVoucherWordList();
    }

    @Test
    public void testGetVoucherWordSimpleList_emptyList() {
        when(voucherWordService.getEnabledVoucherWordList()).thenReturn(Collections.emptyList());

        CommonResult<List<ErpVoucherWordRespVO>> result = controller.getVoucherWordSimpleList();

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

}
