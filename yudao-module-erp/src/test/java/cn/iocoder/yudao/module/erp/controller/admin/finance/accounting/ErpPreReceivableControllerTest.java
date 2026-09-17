package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivableRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpPreReceivableService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpPreReceivableControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPreReceivableController controller;

    @Mock
    private ErpPreReceivableService preReceivableService;

    @Mock
    private ErpAccountService accountService;

    // ==================== createPreReceivable ====================

    @Test
    public void testCreatePreReceivable_paramPassThrough() {
        ErpPreReceivableSaveReqVO reqVO = new ErpPreReceivableSaveReqVO();
        when(preReceivableService.createPreReceivable(any())).thenReturn(77L);

        CommonResult<Long> result = controller.createPreReceivable(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(77L, result.getData());
        verify(preReceivableService).createPreReceivable(eq(reqVO));
    }

    // ==================== updatePreReceivable ====================

    @Test
    public void testUpdatePreReceivable_paramPassThrough() {
        ErpPreReceivableSaveReqVO reqVO = new ErpPreReceivableSaveReqVO();

        CommonResult<Boolean> result = controller.updatePreReceivable(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(preReceivableService).updatePreReceivable(eq(reqVO));
    }

    // ==================== updatePreReceivableStatus ====================

    @Test
    public void testUpdatePreReceivableStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updatePreReceivableStatus(1L, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(preReceivableService).updatePreReceivableStatus(eq(1L), eq(20));
    }

    @Test
    public void testUpdatePreReceivableStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpPreReceivableController.class.getMethod("updatePreReceivableStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:pre-receivable:update-status"));
    }

    @Test
    public void testUpdatePreReceivableStatus_rejectsProcessStatus() {
        assertThrows(RuntimeException.class, () -> controller.updatePreReceivableStatus(1L, 10));
        verify(preReceivableService, never()).updatePreReceivableStatus(any(), any());
    }

    // ==================== deletePreReceivable ====================

    @Test
    public void testDeletePreReceivable_paramPassThrough() {
        List<Long> ids = Arrays.asList(1L, 2L);

        CommonResult<Boolean> result = controller.deletePreReceivable(ids);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(preReceivableService).deletePreReceivable(eq(ids));
    }

    // ==================== getPreReceivable ====================

    @Test
    public void testGetPreReceivable_returnsNullWhenNotFound() {
        when(preReceivableService.getPreReceivable(eq(1L))).thenReturn(null);

        CommonResult<ErpPreReceivableRespVO> result = controller.getPreReceivable(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetPreReceivable_paramPassThroughWithAccount() {
        ErpPreReceivableDO preReceivable = new ErpPreReceivableDO();
        preReceivable.setId(1L);
        preReceivable.setNo("YSZK-001");
        preReceivable.setAccountId(100L);
        when(preReceivableService.getPreReceivable(eq(1L))).thenReturn(preReceivable);

        ErpAccountDO account = new ErpAccountDO();
        account.setId(100L);
        account.setName("中国银行");
        when(accountService.getAccount(eq(100L))).thenReturn(account);

        ErpPreReceivableItemDO item = new ErpPreReceivableItemDO();
        item.setId(10L);
        when(preReceivableService.getPreReceivableItemListByPreReceivableId(eq(1L)))
                .thenReturn(Arrays.asList(item));

        CommonResult<ErpPreReceivableRespVO> result = controller.getPreReceivable(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("YSZK-001", result.getData().getNo());
        assertEquals("中国银行", result.getData().getAccountName());
        assertNotNull(result.getData().getItems());
        assertEquals(1, result.getData().getItems().size());
    }

    @Test
    public void testGetPreReceivable_nullAccountIdSkipsAccountLookup() {
        ErpPreReceivableDO preReceivable = new ErpPreReceivableDO();
        preReceivable.setId(1L);
        preReceivable.setAccountId(null);
        when(preReceivableService.getPreReceivable(eq(1L))).thenReturn(preReceivable);
        when(preReceivableService.getPreReceivableItemListByPreReceivableId(eq(1L)))
                .thenReturn(Collections.emptyList());

        CommonResult<ErpPreReceivableRespVO> result = controller.getPreReceivable(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertNull(result.getData().getAccountName());
    }

    // ==================== getPreReceivablePage ====================

    @Test
    public void testGetPreReceivablePage_emptyList() {
        ErpPreReceivablePageReqVO pageReqVO = new ErpPreReceivablePageReqVO();
        PageResult<ErpPreReceivableDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(preReceivableService.getPreReceivablePage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpPreReceivableRespVO>> result = controller.getPreReceivablePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(preReceivableService).getPreReceivablePage(eq(pageReqVO));
    }

    @Test
    public void testGetPreReceivablePage_includeItemsFalseWithResults() {
        ErpPreReceivablePageReqVO pageReqVO = new ErpPreReceivablePageReqVO();
        pageReqVO.setIncludeItems(false);
        ErpPreReceivableDO preReceivable = new ErpPreReceivableDO();
        preReceivable.setId(1L);
        preReceivable.setNo("YSZK-100");
        preReceivable.setAccountId(100L);
        PageResult<ErpPreReceivableDO> pageResult = new PageResult<>(Collections.singletonList(preReceivable), 1L);
        when(preReceivableService.getPreReceivablePage(any())).thenReturn(pageResult);

        ErpAccountDO account = new ErpAccountDO();
        account.setId(100L);
        account.setName("中国银行");
        when(accountService.getAccountMap(any())).thenReturn(Collections.singletonMap(100L, account));

        CommonResult<PageResult<ErpPreReceivableRespVO>> result = controller.getPreReceivablePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals("YSZK-100", result.getData().getList().get(0).getNo());
        assertEquals("中国银行", result.getData().getList().get(0).getAccountName());
        verify(preReceivableService).getPreReceivablePage(eq(pageReqVO));
    }

}
