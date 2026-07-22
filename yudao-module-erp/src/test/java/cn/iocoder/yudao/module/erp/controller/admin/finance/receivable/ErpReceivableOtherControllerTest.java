package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ErpReceivableOtherControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpReceivableOtherController controller;

    @Mock
    private ErpReceivableOtherService receivableOtherService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void page_fillsCustomerSalespersonAndAuditFields() {
        LocalDateTime auditTime = LocalDateTime.of(2026, 7, 20, 10, 30);
        ErpReceivableOtherDO row = new ErpReceivableOtherDO();
        row.setId(1L);
        row.setStatus(20);
        row.setCustomerId(10L);
        row.setHandlerId(5L);
        row.setDeptId(6L);
        row.setCreator("2");
        row.setUpdater("3");
        row.setUpdateTime(auditTime);
        when(receivableOtherService.getReceivableOtherPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        ErpCustomerDO customer = ErpCustomerDO.builder()
                .id(10L).name("测试客户").saleUserId(4L).build();
        when(customerService.getCustomerMap(any()))
                .thenReturn(Collections.singletonMap(10L, customer));

        Map<Long, AdminUserRespDTO> userMap = new HashMap<>();
        userMap.put(2L, user("创建人"));
        userMap.put(3L, user("审核人"));
        userMap.put(4L, user("业务员"));
        userMap.put(5L, user("经手人"));
        when(adminUserApi.getUserMap(any())).thenReturn(userMap);

        DeptRespDTO dept = new DeptRespDTO();
        dept.setName("财务部");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(6L, dept));

        CommonResult<PageResult<ErpReceivableOtherRespVO>> result =
                controller.page(new ErpReceivableOtherPageReqVO());

        assertNotNull(result.getData());
        ErpReceivableOtherRespVO vo = result.getData().getList().get(0);
        assertEquals("测试客户", vo.getCustomerName());
        assertEquals(4L, vo.getSaleUserId());
        assertEquals("业务员", vo.getSaleUserName());
        assertEquals("创建人", vo.getCreatorName());
        assertEquals("审核人", vo.getAuditorName());
        assertEquals(auditTime, vo.getAuditTime());
        assertEquals("经手人", vo.getHandlerName());
        assertEquals("财务部", vo.getDeptName());
    }

    private static AdminUserRespDTO user(String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setNickname(nickname);
        return user;
    }
}
