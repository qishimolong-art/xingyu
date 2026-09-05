package cn.iocoder.yudao.module.erp.service.finance.settlement;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.settlement.ErpSettlementOffsetDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.settlement.ErpSettlementOffsetMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ErpSettlementOffsetServiceImplTest extends BaseMockitoUnitTest {

    private static final Long LOGIN_USER_ID = 100L;

    @InjectMocks
    private ErpSettlementOffsetServiceImpl settlementOffsetService;

    @Mock
    private ErpSettlementOffsetMapper settlementOffsetMapper;
    @Mock
    private ErpReceivableAccountService receivableAccountService;
    @Mock
    private ErpPayableAccountService payableAccountService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private PermissionApi permissionApi;

    @Test
    void getSettlementOffsetPage_filtersInvisibleSupplierBeforeDetailQuery() {
        ErpSettlementOffsetPageReqVO reqVO = createPageReqVO();
        ErpSettlementOffsetDO candidate = createCandidate(1L, 11L);
        when(settlementOffsetMapper.selectList(reqVO)).thenReturn(Collections.singletonList(candidate));
        when(customerService.getCustomerList(eq(Collections.singleton(1L))))
                .thenReturn(Collections.singletonList(ErpCustomerDO.builder().id(1L).build()));
        when(supplierService.getSupplierList(eq(Collections.singleton(11L))))
                .thenReturn(Collections.emptyList());

        PageResult<ErpSettlementOffsetDO> result = invokeAsBranchUser(reqVO);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verifyNoInteractions(receivableAccountService, payableAccountService);
    }

    @Test
    void getSettlementOffsetPage_filtersInvisibleCustomerBeforeDetailQuery() {
        ErpSettlementOffsetPageReqVO reqVO = createPageReqVO();
        ErpSettlementOffsetDO candidate = createCandidate(2L, 12L);
        when(settlementOffsetMapper.selectList(reqVO)).thenReturn(Collections.singletonList(candidate));
        when(customerService.getCustomerList(eq(Collections.singleton(2L))))
                .thenReturn(Collections.emptyList());
        when(supplierService.getSupplierList(eq(Collections.singleton(12L))))
                .thenReturn(Collections.singletonList(ErpSupplierDO.builder().id(12L).build()));

        PageResult<ErpSettlementOffsetDO> result = invokeAsBranchUser(reqVO);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verifyNoInteractions(receivableAccountService, payableAccountService);
    }

    @Test
    void getSettlementOffsetPage_keepsArchiveVisibleCandidateAndCalculatesBalance() {
        ErpSettlementOffsetPageReqVO reqVO = createPageReqVO();
        ErpSettlementOffsetDO candidate = createCandidate(3L, 13L);
        when(settlementOffsetMapper.selectList(reqVO)).thenReturn(Collections.singletonList(candidate));
        when(customerService.getCustomerList(eq(Collections.singleton(3L))))
                .thenReturn(Collections.singletonList(ErpCustomerDO.builder().id(3L).build()));
        when(supplierService.getSupplierList(eq(Collections.singleton(13L))))
                .thenReturn(Collections.singletonList(ErpSupplierDO.builder().id(13L).build()));
        ErpReceivableDetailRespVO receivable = new ErpReceivableDetailRespVO();
        receivable.setIncreaseAmount(new BigDecimal("100"));
        receivable.setOtherReceivableAmount(new BigDecimal("15"));
        receivable.setReceiptAmount(new BigDecimal("20"));
        receivable.setWriteOffAmount(new BigDecimal("10"));
        when(receivableAccountService.getReceivableDetailList(any(ErpReceivableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Collections.singletonList(receivable));
        ErpPayableDetailRespVO payable = new ErpPayableDetailRespVO();
        payable.setIncreaseAmount(new BigDecimal("50"));
        payable.setPaymentAmount(new BigDecimal("5"));
        payable.setWriteOffAmount(new BigDecimal("5"));
        when(payableAccountService.getPayableDetailList(any(ErpPayableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Collections.singletonList(payable));

        PageResult<ErpSettlementOffsetDO> result = invokeAsBranchUser(reqVO);

        assertThat(result.getList()).containsExactly(candidate);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(candidate.getReceivableBalance()).isEqualByComparingTo("95");
        assertThat(candidate.getPayableBalance()).isEqualByComparingTo("45");
        assertThat(candidate.getOffsetBalance()).isEqualByComparingTo("50");
        assertThat(reqVO.getShowZeroBalance()).isFalse();
        verify(receivableAccountService).getReceivableDetailList(any(ErpReceivableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class));
        verify(payableAccountService).getPayableDetailList(any(ErpPayableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class));
    }

    @Test
    void getSettlementOffsetPage_sortsByCustomerCode() {
        ErpSettlementOffsetPageReqVO reqVO = createPageReqVO();
        reqVO.setOrderField("customerCode");
        reqVO.setOrderDirection("asc");
        ErpSettlementOffsetDO customerB = createCandidate(4L, 14L);
        customerB.setCustomerCode("KH-B");
        ErpSettlementOffsetDO customerA = createCandidate(5L, 15L);
        customerA.setCustomerCode("KH-A");
        when(settlementOffsetMapper.selectList(reqVO)).thenReturn(Arrays.asList(customerB, customerA));
        when(customerService.getCustomerList(any())).thenReturn(Arrays.asList(
                ErpCustomerDO.builder().id(4L).build(), ErpCustomerDO.builder().id(5L).build()));
        when(supplierService.getSupplierList(any())).thenReturn(Arrays.asList(
                ErpSupplierDO.builder().id(14L).build(), ErpSupplierDO.builder().id(15L).build()));
        ErpReceivableDetailRespVO receivable = createReceivableDetail(1L, "YS-1", "100");
        when(receivableAccountService.getReceivableDetailList(any(ErpReceivableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Collections.singletonList(receivable));
        ErpPayableDetailRespVO payable = createPayableDetail(11L, "YF-1");
        payable.setIncreaseAmount(BigDecimal.TEN);
        when(payableAccountService.getPayableDetailList(any(ErpPayableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Collections.singletonList(payable));

        PageResult<ErpSettlementOffsetDO> result = invokeAsBranchUser(reqVO);

        assertThat(result.getList()).extracting(ErpSettlementOffsetDO::getCustomerCode)
                .containsExactly("KH-A", "KH-B");
    }

    @Test
    void getSettlementOffsetPage_returnsEmptyWhenDocumentScopeHasNoAccess() {
        ErpSettlementOffsetPageReqVO reqVO = createPageReqVO();
        when(permissionApi.getDeptDataPermission(LOGIN_USER_ID, "erp_finance_settlement_offset"))
                .thenReturn(new DeptDataPermissionRespDTO());

        PageResult<ErpSettlementOffsetDO> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            result = settlementOffsetService.getSettlementOffsetPage(reqVO);
        }

        assertThat(result.getList()).isEmpty();
        verify(settlementOffsetMapper, never()).selectList(any(ErpSettlementOffsetPageReqVO.class));
        verifyNoInteractions(customerService, supplierService, receivableAccountService, payableAccountService);
    }

    @Test
    void getSettlementOffsetDetail_sortsWhitelistedFieldsAndKeepsNullLast() {
        ErpSettlementOffsetDetailReqVO reqVO = new ErpSettlementOffsetDetailReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setSupplierId(11L);
        reqVO.setReceivableOrderField("otherReceivableAmount");
        reqVO.setReceivableOrderDirection("asc");
        reqVO.setPayableOrderField("docNo");
        reqVO.setPayableOrderDirection("desc");
        ErpReceivableDetailRespVO receivableA = createReceivableDetail(1L, "YS-1", "10");
        receivableA.setOtherReceivableAmount(new BigDecimal("10"));
        ErpReceivableDetailRespVO receivableB = createReceivableDetail(2L, "YS-2", "2");
        receivableB.setOtherReceivableAmount(new BigDecimal("2"));
        ErpReceivableDetailRespVO receivableBlank = createReceivableDetail(3L, "YS-3", null);
        when(receivableAccountService.getReceivableDetailList(any(ErpReceivableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Arrays.asList(receivableA, receivableBlank, receivableB));
        ErpPayableDetailRespVO payableA = createPayableDetail(11L, "YF-2");
        ErpPayableDetailRespVO payableB = createPayableDetail(12L, "YF-10");
        when(payableAccountService.getPayableDetailList(any(ErpPayableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Arrays.asList(payableA, payableB));

        ErpSettlementOffsetDetailRespVO result = invokeDetailAsBranchUser(reqVO);

        assertThat(result.getReceivableDetails()).extracting(ErpReceivableDetailRespVO::getBizId)
                .containsExactly(2L, 1L, 3L);
        assertThat(result.getPayableDetails()).extracting(ErpPayableDetailRespVO::getBizId)
                .containsExactly(11L, 12L);
    }

    @Test
    void getSettlementOffsetDetail_rejectsUnknownFieldAndInvalidDirection() {
        ErpSettlementOffsetDetailReqVO reqVO = new ErpSettlementOffsetDetailReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setSupplierId(11L);
        reqVO.setReceivableOrderField("docNo desc; drop table erp_customer");
        reqVO.setReceivableOrderDirection("asc");
        reqVO.setPayableOrderField("docNo");
        reqVO.setPayableOrderDirection("sideways");
        ErpReceivableDetailRespVO receivableA = createReceivableDetail(1L, "YS-2", "1");
        ErpReceivableDetailRespVO receivableB = createReceivableDetail(2L, "YS-1", "2");
        ErpPayableDetailRespVO payableA = createPayableDetail(11L, "YF-1");
        ErpPayableDetailRespVO payableB = createPayableDetail(12L, "YF-2");
        when(receivableAccountService.getReceivableDetailList(any(ErpReceivableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Arrays.asList(receivableA, receivableB));
        when(payableAccountService.getPayableDetailList(any(ErpPayableDetailReqVO.class),
                any(ErpFinanceVisibleScope.class))).thenReturn(Arrays.asList(payableA, payableB));

        ErpSettlementOffsetDetailRespVO result = invokeDetailAsBranchUser(reqVO);

        assertThat(result.getReceivableDetails()).extracting(ErpReceivableDetailRespVO::getBizId)
                .containsExactly(1L, 2L);
        assertThat(result.getPayableDetails()).extracting(ErpPayableDetailRespVO::getBizId)
                .containsExactly(11L, 12L);
    }

    private PageResult<ErpSettlementOffsetDO> invokeAsBranchUser(ErpSettlementOffsetPageReqVO reqVO) {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO()
                .setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(LOGIN_USER_ID, "erp_finance_settlement_offset"))
                .thenReturn(permission);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            return settlementOffsetService.getSettlementOffsetPage(reqVO);
        }
    }

    private ErpSettlementOffsetDetailRespVO invokeDetailAsBranchUser(ErpSettlementOffsetDetailReqVO reqVO) {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO()
                .setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(LOGIN_USER_ID, "erp_finance_settlement_offset"))
                .thenReturn(permission);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            return settlementOffsetService.getSettlementOffsetDetail(reqVO);
        }
    }

    private ErpSettlementOffsetPageReqVO createPageReqVO() {
        ErpSettlementOffsetPageReqVO reqVO = new ErpSettlementOffsetPageReqVO();
        reqVO.setShowZeroBalance(false);
        reqVO.setOrderField("offsetBalance");
        reqVO.setOrderDirection("desc");
        return reqVO;
    }

    private ErpSettlementOffsetDO createCandidate(Long customerId, Long supplierId) {
        ErpSettlementOffsetDO candidate = new ErpSettlementOffsetDO();
        candidate.setCustomerCode("KH-" + customerId);
        candidate.setSubjectName("往来单位");
        candidate.setCustomerId(customerId);
        candidate.setSupplierId(supplierId);
        return candidate;
    }

    private ErpReceivableDetailRespVO createReceivableDetail(Long bizId, String docNo, String increaseAmount) {
        ErpReceivableDetailRespVO detail = new ErpReceivableDetailRespVO();
        detail.setBizId(bizId);
        detail.setDocNo(docNo);
        detail.setIncreaseAmount(increaseAmount == null ? null : new BigDecimal(increaseAmount));
        return detail;
    }

    private ErpPayableDetailRespVO createPayableDetail(Long bizId, String docNo) {
        ErpPayableDetailRespVO detail = new ErpPayableDetailRespVO();
        detail.setBizId(bizId);
        detail.setDocNo(docNo);
        return detail;
    }
}
