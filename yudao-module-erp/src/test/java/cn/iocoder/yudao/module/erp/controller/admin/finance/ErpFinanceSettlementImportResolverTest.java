package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinanceSettlementImportResolverTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceSettlementImportResolver resolver;

    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;

    @Test
    void buildPaymentSaveReqVO_resolvesNamesAndBizNo() {
        mockPaymentContext(singleSupplier(11L, "上海汽配"), singleAccount(21L, "工行基本户"),
                Collections.singletonList(new DeptSimpleRespVO(31L, "采购部", 0L)));
        when(adminUserApi.getUserListByNickname("张会计")).thenReturn(Collections.singletonList(user(41L, "张会计")));
        when(purchaseInMapper.selectByNo("CGRK-001")).thenReturn(new ErpPurchaseInDO().setId(51L));

        ErpFinancePaymentSaveReqVO reqVO = resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31 10:00:00")
                .setFinanceUserName("张会计")
                .setDeptName("采购部")
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setDiscountPrice(new BigDecimal("1.00"))
                .setTotalPrice(new BigDecimal("100.00"))
                .setPaymentPrice(new BigDecimal("99.00"))
                .setBizType("采购入库")
                .setBizNo("CGRK-001")
                .setPaidPrice(new BigDecimal("20.00"))
                .setItemPaymentPrice(new BigDecimal("79.00")),
                resolver.buildPaymentContext());

        assertThat(reqVO.getSupplierId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getDeptId()).isEqualTo(31L);
        assertThat(reqVO.getFinanceUserId()).isEqualTo(41L);
        assertThat(reqVO.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getBizType()).isEqualTo(ErpBizTypeEnum.PURCHASE_IN.getType());
            assertThat(item.getBizId()).isEqualTo(51L);
        });
    }

    @Test
    void buildPaymentSaveReqVO_allowsBlankBizFieldsForLaterWriteOff() {
        mockPaymentContext(singleSupplier(11L, "上海汽配"), singleAccount(21L, "工行基本户"),
                Collections.emptyList());

        ErpFinancePaymentSaveReqVO reqVO = resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31")
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setTotalPrice(BigDecimal.ONE)
                .setPaymentPrice(BigDecimal.ONE),
                resolver.buildPaymentContext());

        assertThat(reqVO.getSupplierId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getItems()).isEmpty();
        verify(purchaseInMapper, never()).selectByNo(any());
        verify(purchaseReturnMapper, never()).selectByNo(any());
        verify(purchasePriceAdjustMapper, never()).selectByNo(any());
    }

    @Test
    void buildPaymentSaveReqVO_keepsNegativeAmounts() {
        mockPaymentContext(singleSupplier(11L, "上海汽配"), singleAccount(21L, "工行基本户"),
                Collections.emptyList());

        ErpFinancePaymentSaveReqVO reqVO = resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31")
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setDiscountPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("-20.00"))
                .setPaymentPrice(new BigDecimal("-20.00")),
                resolver.buildPaymentContext());

        assertThat(reqVO.getTotalPrice()).isEqualByComparingTo("-20.00");
        assertThat(reqVO.getPaymentPrice()).isEqualByComparingTo("-20.00");
        assertThat(reqVO.getDiscountPrice()).isEqualByComparingTo("0");
        assertThat(reqVO.getItems()).isEmpty();
    }

    @Test
    void buildPaymentSaveReqVO_blankPaymentTimeDefaultsToNow() {
        mockPaymentContext(singleSupplier(11L, "上海汽配"), singleAccount(21L, "工行基本户"),
                Collections.emptyList());

        LocalDateTime before = LocalDateTime.now();
        ErpFinancePaymentSaveReqVO reqVO = resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setTotalPrice(BigDecimal.ONE)
                .setPaymentPrice(BigDecimal.ONE),
                resolver.buildPaymentContext());
        LocalDateTime after = LocalDateTime.now();

        assertThat(reqVO.getPaymentTime()).isBetween(before, after);
    }

    @Test
    void buildPaymentSaveReqVO_rejectsPartialBizFields() {
        mockPaymentContext(singleSupplier(11L, "上海汽配"), singleAccount(21L, "工行基本户"),
                Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31")
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setTotalPrice(BigDecimal.ONE)
                .setPaymentPrice(BigDecimal.ONE)
                .setBizType("采购入库")
                .setBizNo("CGRK-001"),
                resolver.buildPaymentContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务类型、业务单号/业务ID、本次付款需同时填写，或全部留空后续核销");
        verify(purchaseInMapper, never()).selectByNo(any());
    }

    @Test
    void buildPaymentSaveReqVO_rejectsDuplicateSupplierName() {
        mockPaymentContext(Arrays.asList(supplier(11L, "上海汽配"), supplier(12L, "上海汽配")),
                singleAccount(21L, "工行基本户"), Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31")
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setTotalPrice(BigDecimal.ONE)
                .setPaymentPrice(BigDecimal.ONE)
                .setBizType("采购入库")
                .setBizNo("CGRK-001")
                .setItemPaymentPrice(BigDecimal.ONE),
                resolver.buildPaymentContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("供应商名称重复：上海汽配");
    }

    @Test
    void buildPaymentSaveReqVO_keepsLegacyIdsWhenNamesAreBlank() {
        mockPaymentContext(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        ErpFinancePaymentSaveReqVO reqVO = resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31")
                .setFinanceUserId(41L)
                .setDeptId(31L)
                .setSupplierId(11L)
                .setAccountId(21L)
                .setTotalPrice(BigDecimal.ONE)
                .setPaymentPrice(BigDecimal.ONE)
                .setBizType("11")
                .setBizId(51L)
                .setItemPaymentPrice(BigDecimal.ONE),
                resolver.buildPaymentContext());

        assertThat(reqVO.getSupplierId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getDeptId()).isEqualTo(31L);
        assertThat(reqVO.getFinanceUserId()).isEqualTo(41L);
        assertThat(reqVO.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getBizType()).isEqualTo(ErpBizTypeEnum.PURCHASE_IN.getType());
            assertThat(item.getBizId()).isEqualTo(51L);
        });
        verify(purchaseInMapper, never()).selectByNo(any());
    }

    @Test
    void buildPaymentSaveReqVO_allowsLegacyIdsWithoutBizForLaterWriteOff() {
        mockPaymentContext(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        ErpFinancePaymentSaveReqVO reqVO = resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31")
                .setSupplierId(11L)
                .setAccountId(21L)
                .setTotalPrice(BigDecimal.ONE)
                .setPaymentPrice(BigDecimal.ONE),
                resolver.buildPaymentContext());

        assertThat(reqVO.getSupplierId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getItems()).isEmpty();
    }

    @Test
    void buildPaymentSaveReqVO_rejectsBizNoWhenTypeMismatch() {
        mockPaymentContext(singleSupplier(11L, "上海汽配"), singleAccount(21L, "工行基本户"),
                Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildPaymentSaveReqVO(new ErpFinancePaymentImportExcelVO()
                .setPaymentTime("2026-08-31")
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setTotalPrice(BigDecimal.ONE)
                .setPaymentPrice(BigDecimal.ONE)
                .setBizType("采购退货")
                .setBizNo("CGRK-001")
                .setItemPaymentPrice(BigDecimal.ONE),
                resolver.buildPaymentContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务单号不存在：CGRK-001");
        verify(purchaseInMapper, never()).selectByNo(any());
    }

    @Test
    void buildReceiptSaveReqVO_resolvesNamesAndBizNo() {
        mockReceiptContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"),
                Collections.singletonList(new DeptSimpleRespVO(31L, "销售部", 0L)));
        when(adminUserApi.getUserListByNickname("李会计")).thenReturn(Collections.singletonList(user(41L, "李会计")));
        when(saleOutMapper.selectByNo("XSCK-001")).thenReturn(new ErpSaleOutDO().setId(51L));

        ErpFinanceReceiptSaveReqVO reqVO = resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setFinanceUserName("李会计")
                .setDeptName("销售部")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(new BigDecimal("100.00"))
                .setReceiptPrice(new BigDecimal("100.00"))
                .setBizType("销售出库")
                .setBizNo("XSCK-001")
                .setItemReceiptPrice(new BigDecimal("100.00")),
                resolver.buildReceiptContext());

        assertThat(reqVO.getCustomerId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getDeptId()).isEqualTo(31L);
        assertThat(reqVO.getFinanceUserId()).isEqualTo(41L);
        assertThat(reqVO.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getBizType()).isEqualTo(ErpBizTypeEnum.SALE_OUT.getType());
            assertThat(item.getBizId()).isEqualTo(51L);
        });
    }

    @Test
    void buildReceiptSaveReqVO_allowsBlankBizFieldsForLaterWriteOff() {
        mockReceiptContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"),
                Collections.emptyList());

        ErpFinanceReceiptSaveReqVO reqVO = resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE),
                resolver.buildReceiptContext());

        assertThat(reqVO.getCustomerId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getItems()).isEmpty();
        verify(saleOutMapper, never()).selectByNo(any());
        verify(saleReturnMapper, never()).selectByNo(any());
        verify(salePriceAdjustMapper, never()).selectByNo(any());
    }

    @Test
    void buildReceiptSaveReqVO_keepsNegativeAmounts() {
        mockReceiptContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"),
                Collections.emptyList());

        ErpFinanceReceiptSaveReqVO reqVO = resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setDiscountPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("-20.00"))
                .setReceiptPrice(new BigDecimal("-20.00")),
                resolver.buildReceiptContext());

        assertThat(reqVO.getTotalPrice()).isEqualByComparingTo("-20.00");
        assertThat(reqVO.getReceiptPrice()).isEqualByComparingTo("-20.00");
        assertThat(reqVO.getDiscountPrice()).isEqualByComparingTo("0");
        assertThat(reqVO.getItems()).isEmpty();
    }

    @Test
    void buildReceiptSaveReqVO_blankReceiptTimeDefaultsToNow() {
        mockReceiptContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"),
                Collections.emptyList());

        LocalDateTime before = LocalDateTime.now();
        ErpFinanceReceiptSaveReqVO reqVO = resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE),
                resolver.buildReceiptContext());
        LocalDateTime after = LocalDateTime.now();

        assertThat(reqVO.getReceiptTime()).isBetween(before, after);
    }

    @Test
    void buildReceiptSaveReqVO_rejectsPartialBizFields() {
        mockReceiptContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"),
                Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE)
                .setBizType("销售出库")
                .setBizNo("XSCK-001"),
                resolver.buildReceiptContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务类型、业务单号/业务ID、本次收款需同时填写，或全部留空后续核销");
        verify(saleOutMapper, never()).selectByNo(any());
    }

    @Test
    void buildReceiptSaveReqVO_allowsLegacyIdsWithoutBizForLaterWriteOff() {
        mockReceiptContext(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        ErpFinanceReceiptSaveReqVO reqVO = resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerId(11L)
                .setAccountId(21L)
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE),
                resolver.buildReceiptContext());

        assertThat(reqVO.getCustomerId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getItems()).isEmpty();
    }

    @Test
    void buildReceiptSaveReqVO_rejectsMissingCustomerName() {
        mockReceiptContext(Collections.emptyList(), singleAccount(21L, "农行收款户"), Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE)
                .setBizType("销售出库")
                .setBizNo("XSCK-001")
                .setItemReceiptPrice(BigDecimal.ONE),
                resolver.buildReceiptContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("客户不存在：杭州客户");
    }

    @Test
    void buildReceiptSaveReqVO_rejectsDuplicateCustomerName() {
        mockReceiptContext(Arrays.asList(customer(11L, "杭州客户"), customer(12L, "杭州客户")),
                singleAccount(21L, "农行收款户"), Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE)
                .setBizType("销售出库")
                .setBizNo("XSCK-001")
                .setItemReceiptPrice(BigDecimal.ONE),
                resolver.buildReceiptContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("客户名称重复：杭州客户");
    }

    @Test
    void buildReceiptSaveReqVO_rejectsUnsupportedBizType() {
        mockReceiptContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"), Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE)
                .setBizType("采购入库")
                .setBizNo("CGRK-001")
                .setItemReceiptPrice(BigDecimal.ONE),
                resolver.buildReceiptContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("收款单业务类型不支持：采购入库");
    }

    @Test
    void buildReceiptSaveReqVO_rejectsNonexistentBizNo() {
        mockReceiptContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"), Collections.emptyList());

        assertThatThrownBy(() -> resolver.buildReceiptSaveReqVO(new ErpFinanceReceiptImportExcelVO()
                .setReceiptTime("2026-08-31")
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setTotalPrice(BigDecimal.ONE)
                .setReceiptPrice(BigDecimal.ONE)
                .setBizType("销售出库")
                .setBizNo("XSCK-404")
                .setItemReceiptPrice(BigDecimal.ONE),
                resolver.buildReceiptContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务单号不存在：XSCK-404");
    }

    @Test
    void buildReceivableMiscSaveReqVO_resolvesNames() {
        mockReceivableMiscContext(singleCustomer(11L, "杭州客户"), singleAccount(21L, "农行收款户"),
                Collections.singletonList(new DeptSimpleRespVO(31L, "销售部", 0L)));

        ErpReceivableMiscSaveReqVO reqVO = resolver.buildReceivableMiscSaveReqVO(new ErpReceivableMiscImportExcelVO()
                .setCustomerName("杭州客户")
                .setAccountName("农行收款户")
                .setDeptName("销售部")
                .setAmount(new BigDecimal("88.00"))
                .setRemark("临时垫付"),
                resolver.buildReceivableMiscContext());

        assertThat(reqVO.getCustomerId()).isEqualTo(11L);
        assertThat(reqVO.getAccountId()).isEqualTo(21L);
        assertThat(reqVO.getDeptId()).isEqualTo(31L);
        assertThat(reqVO.getAmount()).isEqualByComparingTo("88.00");
        assertThat(reqVO.getRemark()).isEqualTo("临时垫付");
    }

    @Test
    void buildPayableMiscSaveReqVO_resolvesNamesAndRejectsDuplicateAccount() {
        mockPayableMiscContext(singleSupplier(11L, "上海汽配"),
                Arrays.asList(new ErpAccountDO().setId(21L).setName("工行基本户"),
                        new ErpAccountDO().setId(22L).setName("工行基本户")),
                Collections.singletonList(new DeptSimpleRespVO(31L, "采购部", 0L)));

        assertThatThrownBy(() -> resolver.buildPayableMiscSaveReqVO(new ErpPayableMiscImportExcelVO()
                .setSupplierName("上海汽配")
                .setAccountName("工行基本户")
                .setDeptName("采购部")
                .setAmount(new BigDecimal("66.00")),
                resolver.buildPayableMiscContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("账户名称重复：工行基本户");
    }

    private void mockPaymentContext(java.util.List<ErpSupplierDO> suppliers, java.util.List<ErpAccountDO> accounts,
                                    java.util.List<DeptSimpleRespVO> depts) {
        when(supplierService.getSupplierPage(any(ErpSupplierPageReqVO.class)))
                .thenReturn(new PageResult<>(suppliers, (long) suppliers.size()));
        when(accountService.getAccountPage(any(ErpAccountPageReqVO.class)))
                .thenReturn(new PageResult<>(accounts, (long) accounts.size()));
        when(dataPermissionDeptService.getDeptSimpleList("erp_finance_payment")).thenReturn(depts);
    }

    private void mockReceiptContext(java.util.List<ErpCustomerDO> customers, java.util.List<ErpAccountDO> accounts,
                                    java.util.List<DeptSimpleRespVO> depts) {
        when(customerService.getCustomerPage(any(ErpCustomerPageReqVO.class)))
                .thenReturn(new PageResult<>(customers, (long) customers.size()));
        when(accountService.getAccountPage(any(ErpAccountPageReqVO.class)))
                .thenReturn(new PageResult<>(accounts, (long) accounts.size()));
        when(dataPermissionDeptService.getDeptSimpleList("erp_finance_receipt")).thenReturn(depts);
    }

    private void mockReceivableMiscContext(java.util.List<ErpCustomerDO> customers,
                                           java.util.List<ErpAccountDO> accounts,
                                           java.util.List<DeptSimpleRespVO> depts) {
        when(customerService.getCustomerPage(any(ErpCustomerPageReqVO.class)))
                .thenReturn(new PageResult<>(customers, (long) customers.size()));
        when(accountService.getAccountPage(any(ErpAccountPageReqVO.class)))
                .thenReturn(new PageResult<>(accounts, (long) accounts.size()));
        when(dataPermissionDeptService.getDeptSimpleList("erp_receivable_misc")).thenReturn(depts);
    }

    private void mockPayableMiscContext(java.util.List<ErpSupplierDO> suppliers,
                                        java.util.List<ErpAccountDO> accounts,
                                        java.util.List<DeptSimpleRespVO> depts) {
        when(supplierService.getSupplierPage(any(ErpSupplierPageReqVO.class)))
                .thenReturn(new PageResult<>(suppliers, (long) suppliers.size()));
        when(accountService.getAccountPage(any(ErpAccountPageReqVO.class)))
                .thenReturn(new PageResult<>(accounts, (long) accounts.size()));
        when(dataPermissionDeptService.getDeptSimpleList("erp_payable_misc")).thenReturn(depts);
    }

    private java.util.List<ErpSupplierDO> singleSupplier(Long id, String name) {
        return Collections.singletonList(supplier(id, name));
    }

    private java.util.List<ErpCustomerDO> singleCustomer(Long id, String name) {
        return Collections.singletonList(customer(id, name));
    }

    private java.util.List<ErpAccountDO> singleAccount(Long id, String name) {
        return Collections.singletonList(new ErpAccountDO().setId(id).setName(name));
    }

    private ErpSupplierDO supplier(Long id, String name) {
        return new ErpSupplierDO().setId(id).setName(name);
    }

    private ErpCustomerDO customer(Long id, String name) {
        return new ErpCustomerDO().setId(id).setName(name);
    }

    private AdminUserRespDTO user(Long id, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }
}
