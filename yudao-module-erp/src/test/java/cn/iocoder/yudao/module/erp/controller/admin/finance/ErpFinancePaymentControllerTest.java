package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentBizWriteOffItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinancePaymentService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinancePaymentControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinancePaymentController controller;

    @Mock
    private ErpFinancePaymentService financePaymentService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpVoucherMapper voucherMapper;

    @Test
    void getFormCandidatesDelegatesToFinancePaymentService() {
        ErpFinancePaymentFormCandidateReqVO reqVO = new ErpFinancePaymentFormCandidateReqVO()
                .setSupplierId(4L).setDeptId(7L).setNo("CGRK");
        List<ErpFinancePaymentFormCandidateRespVO> candidates = Collections.singletonList(
                new ErpFinancePaymentFormCandidateRespVO()
                        .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(100L)
                        .setBizNo("CGRK20260902000001").setSupplierId(4L).setDeptId(7L)
                        .setTotalPrice(new BigDecimal("300"))
                        .setAllocatedPrice(new BigDecimal("100"))
                        .setUnallocatedPrice(new BigDecimal("200")));
        when(financePaymentService.getFormCandidates(reqVO)).thenReturn(candidates);

        CommonResult<List<ErpFinancePaymentFormCandidateRespVO>> result = controller.getFormCandidates(reqVO);

        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("CGRK20260902000001", result.getData().get(0).getBizNo());
        assertEquals(0, new BigDecimal("200").compareTo(result.getData().get(0).getUnallocatedPrice()));
        verify(financePaymentService).getFormCandidates(reqVO);
    }

    @Test
    void getWriteOffItemsByBizMapsPaymentContext() {
        LocalDateTime paymentTime = LocalDateTime.of(2026, 9, 5, 10, 20);
        LocalDateTime writeOffTime = LocalDateTime.of(2026, 9, 5, 11, 30);
        List<ErpFinancePaymentItemDO> items = Arrays.asList(
                new ErpFinancePaymentItemDO().setId(101L).setPaymentId(201L)
                        .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(10L)
                        .setPaymentPrice(new BigDecimal("40"))
                        .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                        .setWriteOffTime(writeOffTime).setRemark("首付款"),
                new ErpFinancePaymentItemDO().setId(102L).setPaymentId(202L)
                        .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(10L)
                        .setPaymentPrice(new BigDecimal("10"))
                        .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.REVERSED.getStatus()));
        when(financePaymentService.getFinancePaymentItemListByBiz(
                eq(ErpBizTypeEnum.PURCHASE_IN.getType()), eq(10L))).thenReturn(items);
        when(financePaymentService.getFinancePaymentList(any())).thenReturn(Arrays.asList(
                new ErpFinancePaymentDO().setId(201L).setNo("FK-001").setPaymentTime(paymentTime)
                        .setAccountId(301L).setFinanceUserId(401L),
                new ErpFinancePaymentDO().setId(202L).setNo("FK-002").setAccountId(302L)));
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(401L);
        user.setNickname("李会计");
        when(accountService.getAccountMap(any())).thenReturn(Collections.singletonMap(301L,
                new ErpAccountDO().setId(301L).setName("基本户")));
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.singletonMap(401L, user));

        CommonResult<List<ErpFinancePaymentBizWriteOffItemRespVO>> result =
                controller.getWriteOffItemsByBiz(ErpBizTypeEnum.PURCHASE_IN.getType(), 10L);

        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        ErpFinancePaymentBizWriteOffItemRespVO first = result.getData().get(0);
        assertEquals(101L, first.getPaymentItemId());
        assertEquals(201L, first.getPaymentId());
        assertEquals("FK-001", first.getPaymentNo());
        assertEquals(paymentTime, first.getPaymentTime());
        assertEquals("基本户", first.getAccountName());
        assertEquals("李会计", first.getFinanceUserName());
        assertEquals(0, new BigDecimal("40").compareTo(first.getPaymentPrice()));
        assertEquals(writeOffTime, first.getWriteOffTime());
        assertEquals(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus(), first.getWriteOffStatus());
        assertEquals("首付款", first.getRemark());
        verify(financePaymentService).getFinancePaymentItemListByBiz(
                eq(ErpBizTypeEnum.PURCHASE_IN.getType()), eq(10L));
    }

}
