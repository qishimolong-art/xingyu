package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPayableExpenseControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPayableExpenseController controller;

    @Mock
    private ErpPayableExpenseService payableExpenseService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void createDraft_delegatesToDraftService() {
        ErpPayableExpenseDraftSaveReqVO reqVO =
                new ErpPayableExpenseDraftSaveReqVO().setRemark("未完成");
        when(payableExpenseService.createPayableExpenseDraft(reqVO)).thenReturn(9L);

        CommonResult<Long> result = controller.createDraft(reqVO);

        assertEquals(9L, result.getData());
        verify(payableExpenseService).createPayableExpenseDraft(reqVO);
    }

    @Test
    void page_allowsDraftWithoutAccountOrItems() {
        ErpPayableExpenseDO row = new ErpPayableExpenseDO()
                .setId(9L).setStatus(0);
        when(payableExpenseService.getPayableExpensePage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));
        when(payableExpenseService.getPayableExpenseItemListByExpenseIds(any()))
                .thenReturn(Collections.emptyList());
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpPayableExpenseRespVO>> result =
                controller.page(new ErpPayableExpensePageReqVO());

        assertNotNull(result.getData());
        assertEquals(0, result.getData().getList().get(0).getStatus());
    }

}
