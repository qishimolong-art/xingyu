package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpPreReceiptService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpPreReceiptControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPreReceiptController controller;

    @Mock
    private ErpPreReceiptService preReceiptService;

    @Mock
    private ErpAccountService accountService;

    @Mock
    private AdminUserApi adminUserApi;

    // ==================== createPreReceipt ====================

    @Test
    public void testCreatePreReceipt_paramPassThrough() {
        ErpPreReceiptSaveReqVO reqVO = new ErpPreReceiptSaveReqVO();
        when(preReceiptService.createPreReceipt(any())).thenReturn(88L);

        CommonResult<Long> result = controller.createPreReceipt(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(88L, result.getData());
        verify(preReceiptService).createPreReceipt(eq(reqVO));
    }

    // ==================== updatePreReceipt ====================

    @Test
    public void testUpdatePreReceipt_paramPassThrough() {
        ErpPreReceiptSaveReqVO reqVO = new ErpPreReceiptSaveReqVO();

        CommonResult<Boolean> result = controller.updatePreReceipt(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(preReceiptService).updatePreReceipt(eq(reqVO));
    }

    // ==================== updatePreReceiptStatus ====================

    @Test
    public void testUpdatePreReceiptStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updatePreReceiptStatus(1L, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(preReceiptService).updatePreReceiptStatus(eq(1L), eq(20));
    }

    // ==================== deletePreReceipt ====================

    @Test
    public void testDeletePreReceipt_paramPassThrough() {
        List<Long> ids = Arrays.asList(1L, 2L);

        CommonResult<Boolean> result = controller.deletePreReceipt(ids);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(preReceiptService).deletePreReceipt(eq(ids));
    }

    // ==================== getPreReceipt ====================

    @Test
    public void testGetPreReceipt_returnsNullWhenNotFound() {
        when(preReceiptService.getPreReceipt(eq(1L))).thenReturn(null);

        CommonResult<ErpPreReceiptRespVO> result = controller.getPreReceipt(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetPreReceipt_paramPassThrough() {
        ErpPreReceiptDO preReceipt = new ErpPreReceiptDO();
        preReceipt.setId(1L);
        preReceipt.setNo("YSKD-001");
        when(preReceiptService.getPreReceipt(eq(1L))).thenReturn(preReceipt);
        ErpPreReceiptItemDO item = new ErpPreReceiptItemDO();
        item.setId(10L);
        when(preReceiptService.getPreReceiptItemListByPreReceiptId(eq(1L)))
                .thenReturn(Arrays.asList(item));

        CommonResult<ErpPreReceiptRespVO> result = controller.getPreReceipt(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("YSKD-001", result.getData().getNo());
        assertNotNull(result.getData().getItems());
        assertEquals(1, result.getData().getItems().size());
    }

    // ==================== getPreReceiptPage ====================

    @Test
    public void testGetPreReceiptPage_emptyList() {
        ErpPreReceiptPageReqVO pageReqVO = new ErpPreReceiptPageReqVO();
        PageResult<ErpPreReceiptDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(preReceiptService.getPreReceiptPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpPreReceiptRespVO>> result = controller.getPreReceiptPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(preReceiptService).getPreReceiptPage(eq(pageReqVO));
    }

    @Test
    public void testGetPreReceiptPage_includeItemsFalseSkipsItemQuery() {
        ErpPreReceiptPageReqVO pageReqVO = new ErpPreReceiptPageReqVO();
        pageReqVO.setIncludeItems(false);
        ErpPreReceiptDO preReceipt = new ErpPreReceiptDO();
        preReceipt.setId(1L);
        preReceipt.setNo("YSKD-100");
        preReceipt.setAccountId(9L);
        preReceipt.setCreator("7");
        PageResult<ErpPreReceiptDO> pageResult = new PageResult<>(Collections.singletonList(preReceipt), 1L);
        when(preReceiptService.getPreReceiptPage(any())).thenReturn(pageResult);

        ErpAccountDO account = new ErpAccountDO();
        account.setId(9L);
        account.setName("现金账户");
        Map<Long, ErpAccountDO> accountMap = new HashMap<>();
        accountMap.put(9L, account);
        when(accountService.getAccountMap(any())).thenReturn(accountMap);

        AdminUserRespDTO creator = new AdminUserRespDTO();
        creator.setId(7L);
        creator.setNickname("制单人");
        Map<Long, AdminUserRespDTO> userMap = new HashMap<>();
        userMap.put(7L, creator);
        when(adminUserApi.getUserMap(any())).thenReturn(userMap);

        CommonResult<PageResult<ErpPreReceiptRespVO>> result = controller.getPreReceiptPage(pageReqVO);

        assertEquals(0, result.getCode());
        ErpPreReceiptRespVO row = result.getData().getList().get(0);
        assertEquals("YSKD-100", row.getNo());
        assertEquals("现金账户", row.getAccountName());
        assertEquals("制单人", row.getCreatorName());
        assertNotNull(row.getItems());
        assertTrue(row.getItems().isEmpty());
        verify(preReceiptService, never()).getPreReceiptItemListByPreReceiptIds(any());
    }

    @Test
    public void testGetPreReceiptPage_defaultKeepsItemQuery() {
        ErpPreReceiptPageReqVO pageReqVO = new ErpPreReceiptPageReqVO();
        ErpPreReceiptDO preReceipt = new ErpPreReceiptDO();
        preReceipt.setId(1L);
        PageResult<ErpPreReceiptDO> pageResult = new PageResult<>(Collections.singletonList(preReceipt), 1L);
        when(preReceiptService.getPreReceiptPage(any())).thenReturn(pageResult);
        ErpPreReceiptItemDO item = new ErpPreReceiptItemDO();
        item.setId(10L);
        item.setPreReceiptId(1L);
        when(preReceiptService.getPreReceiptItemListByPreReceiptIds(any()))
                .thenReturn(Collections.singletonList(item));
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpPreReceiptRespVO>> result = controller.getPreReceiptPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().getList().get(0).getItems().size());
        verify(preReceiptService).getPreReceiptItemListByPreReceiptIds(any());
    }

}
