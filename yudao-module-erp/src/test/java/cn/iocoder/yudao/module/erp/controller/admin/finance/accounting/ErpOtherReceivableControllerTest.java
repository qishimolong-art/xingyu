package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpOtherReceivableService;
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

    @Test
    public void testGetOtherReceivablePage_includeItemsFalseSkipsItemQuery() {
        ErpOtherReceivablePageReqVO pageReqVO = new ErpOtherReceivablePageReqVO();
        pageReqVO.setIncludeItems(false);
        ErpOtherReceivableDO receivable = new ErpOtherReceivableDO();
        receivable.setId(1L);
        receivable.setNo("QTYS-100");
        receivable.setAccountId(9L);
        receivable.setCreator("7");
        PageResult<ErpOtherReceivableDO> pageResult = new PageResult<>(Collections.singletonList(receivable), 1L);
        when(otherReceivableService.getOtherReceivablePage(any())).thenReturn(pageResult);

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

        CommonResult<PageResult<ErpOtherReceivableRespVO>> result = controller.getOtherReceivablePage(pageReqVO);

        assertEquals(0, result.getCode());
        ErpOtherReceivableRespVO row = result.getData().getList().get(0);
        assertEquals("QTYS-100", row.getNo());
        assertEquals("现金账户", row.getAccountName());
        assertEquals("制单人", row.getCreatorName());
        assertNotNull(row.getItems());
        assertTrue(row.getItems().isEmpty());
        verify(otherReceivableService, never()).getOtherReceivableItemListByReceivableIds(any());
    }

    @Test
    public void testGetOtherReceivablePage_defaultKeepsItemQuery() {
        ErpOtherReceivablePageReqVO pageReqVO = new ErpOtherReceivablePageReqVO();
        ErpOtherReceivableDO receivable = new ErpOtherReceivableDO();
        receivable.setId(1L);
        PageResult<ErpOtherReceivableDO> pageResult = new PageResult<>(Collections.singletonList(receivable), 1L);
        when(otherReceivableService.getOtherReceivablePage(any())).thenReturn(pageResult);
        ErpOtherReceivableItemDO item = new ErpOtherReceivableItemDO();
        item.setId(10L);
        item.setReceivableId(1L);
        when(otherReceivableService.getOtherReceivableItemListByReceivableIds(any()))
                .thenReturn(Collections.singletonList(item));
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpOtherReceivableRespVO>> result = controller.getOtherReceivablePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().getList().get(0).getItems().size());
        verify(otherReceivableService).getOtherReceivableItemListByReceivableIds(any());
    }

}
