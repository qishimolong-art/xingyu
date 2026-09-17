package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptBizWriteOffItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidateRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceReceiptService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinanceReceiptControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceReceiptController controller;

    @Mock
    private ErpFinanceReceiptService financeReceiptService;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpVoucherMapper voucherMapper;

    @BeforeEach
    void setUp() {
        lenient().when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());
        lenient().when(accountService.getAccountMap(any())).thenReturn(Collections.emptyMap());
        lenient().when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        lenient().when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());
        lenient().when(voucherMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void getFormCandidatesDelegatesToFinanceReceiptService() {
        ErpFinanceReceiptFormCandidateReqVO reqVO = new ErpFinanceReceiptFormCandidateReqVO()
                .setCustomerId(4L).setDeptId(7L).setNo("XSCK");
        List<ErpFinanceReceiptFormCandidateRespVO> candidates = Collections.singletonList(
                new ErpFinanceReceiptFormCandidateRespVO()
                        .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(100L)
                        .setBizNo("XSCK20260902000001").setCustomerId(4L).setDeptId(7L)
                        .setTotalPrice(new BigDecimal("300"))
                        .setAllocatedPrice(new BigDecimal("100"))
                        .setUnallocatedPrice(new BigDecimal("200")));
        when(financeReceiptService.getFormCandidates(reqVO)).thenReturn(candidates);

        CommonResult<List<ErpFinanceReceiptFormCandidateRespVO>> result = controller.getFormCandidates(reqVO);

        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("XSCK20260902000001", result.getData().get(0).getBizNo());
        assertEquals(0, new BigDecimal("200").compareTo(result.getData().get(0).getUnallocatedPrice()));
        verify(financeReceiptService).getFormCandidates(reqVO);
    }

    @Test
    void getWriteOffItemsByBizMapsReceiptContext() {
        LocalDateTime receiptTime = LocalDateTime.of(2026, 9, 6, 10, 20);
        LocalDateTime writeOffTime = LocalDateTime.of(2026, 9, 6, 11, 30);
        List<ErpFinanceReceiptItemDO> items = Arrays.asList(
                new ErpFinanceReceiptItemDO().setId(101L).setReceiptId(201L)
                        .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(10L)
                        .setReceiptPrice(new BigDecimal("40"))
                        .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                        .setWriteOffTime(writeOffTime).setRemark("首款"),
                new ErpFinanceReceiptItemDO().setId(102L).setReceiptId(202L)
                        .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(10L)
                        .setReceiptPrice(new BigDecimal("10"))
                        .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.REVERSED.getStatus()));
        when(financeReceiptService.getFinanceReceiptItemListByBiz(
                eq(ErpBizTypeEnum.SALE_OUT.getType()), eq(10L))).thenReturn(items);
        when(financeReceiptService.getFinanceReceiptList(any())).thenReturn(Arrays.asList(
                new ErpFinanceReceiptDO().setId(201L).setNo("SK-001").setReceiptTime(receiptTime)
                        .setAccountId(301L).setFinanceUserId(401L),
                new ErpFinanceReceiptDO().setId(202L).setNo("SK-002").setAccountId(302L)));
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(401L);
        user.setNickname("李会计");
        when(accountService.getAccountMap(any())).thenReturn(Collections.singletonMap(301L,
                new ErpAccountDO().setId(301L).setName("基本户")));
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.singletonMap(401L, user));

        CommonResult<List<ErpFinanceReceiptBizWriteOffItemRespVO>> result =
                controller.getWriteOffItemsByBiz(ErpBizTypeEnum.SALE_OUT.getType(), 10L);

        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        ErpFinanceReceiptBizWriteOffItemRespVO first = result.getData().get(0);
        assertEquals(101L, first.getReceiptItemId());
        assertEquals(201L, first.getReceiptId());
        assertEquals("SK-001", first.getReceiptNo());
        assertEquals(receiptTime, first.getReceiptTime());
        assertEquals("基本户", first.getAccountName());
        assertEquals("李会计", first.getFinanceUserName());
        assertEquals(0, new BigDecimal("40").compareTo(first.getReceiptPrice()));
        assertEquals(writeOffTime, first.getWriteOffTime());
        assertEquals(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus(), first.getWriteOffStatus());
        assertEquals("首款", first.getRemark());
        verify(financeReceiptService).getFinanceReceiptItemListByBiz(
                eq(ErpBizTypeEnum.SALE_OUT.getType()), eq(10L));
    }

}
