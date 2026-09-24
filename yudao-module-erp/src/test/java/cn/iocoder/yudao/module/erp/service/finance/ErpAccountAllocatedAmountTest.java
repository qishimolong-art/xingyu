package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountServiceImpl;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpAccountAllocatedAmountTest {

    @Test
    void payableOtherMapperMustBeInjectedForDetailDiscountAdjustmentLookup() throws NoSuchFieldException {
        Field field = ErpPayableAccountServiceImpl.class.getDeclaredField("payableOtherMapper");

        assertThat(field.getAnnotation(Resource.class)).isNotNull();
    }

    @Test
    void payableAllocationIsShownWithoutReducingBalanceTwice() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpPayableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildAllocatedRow",
                "采购入库", 11, 1L, LocalDateTime.now(), "PI-1",
                new BigDecimal("100"), new BigDecimal("40"));

        assertThat(row).isNotNull();
        assertThat(row.getAllocatedAmount()).isEqualByComparingTo("40");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("100");
    }

    @Test
    void payableWriteOffRowDoesNotChangeRunningBalance() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpPayableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildRow",
                "writeoff", 11, 1L, LocalDateTime.now(), "PI-1",
                new BigDecimal("-40"), true);

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("0");
        assertThat(row.getPaymentAmount()).isEqualByComparingTo("0");
        assertThat(row.getWriteOffAmount()).isEqualByComparingTo("40");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("0");
    }

    @Test
    void payableDecreaseBusinessIsShownAsNegativePayable() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpPayableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildAllocatedRow",
                "采购退货", 12, 1L, LocalDateTime.now(), "PR-1",
                new BigDecimal("-30"), BigDecimal.ZERO);

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("-30");
        assertThat(row.getPaymentAmount()).isEqualByComparingTo("0");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("-30");
    }

    @Test
    void payablePurchaseInDetailUsesOriginalAmountAndMarksAdjusted() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpPurchaseInDO purchaseIn = ErpPurchaseInDO.builder()
                .id(1L)
                .no("PI-ADJ")
                .inTime(LocalDateTime.now())
                .totalPrice(new BigDecimal("120"))
                .discountPercent(BigDecimal.ZERO)
                .adjusted(true)
                .build();
        ErpPurchaseInItemDO item = ErpPurchaseInItemDO.builder()
                .inId(1L)
                .count(new BigDecimal("3"))
                .productPrice(new BigDecimal("40"))
                .originalProductPrice(new BigDecimal("30"))
                .build();

        ErpPayableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildPurchaseInDetailRow",
                purchaseIn, BigDecimal.ZERO, Collections.singletonList(item));

        assertThat(row).isNotNull();
        assertThat(row.getDocNo()).isEqualTo("PI-ADJ");
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("90");
        assertThat(row.getWriteOffBaseAmount()).isEqualByComparingTo("90");
        assertThat(row.getPriceAdjusted()).isTrue();
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("90");
    }

    @Test
    void payablePriceAdjustDetailKeepsSignedAdjustmentAsSeparateRow() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpPayableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildAllocatedRow",
                "采购调价", 13, 2L, LocalDateTime.now(), "DS-1",
                new BigDecimal("-20"), BigDecimal.ZERO);

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("-20");
        assertThat(row.getPaymentAmount()).isEqualByComparingTo("0");
        assertThat(row.getPriceAdjusted()).isNull();
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("-20");
    }

    @Test
    void payablePaymentAmountUsesPaymentColumnOnly() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpPayableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildPaymentAllocatedRow",
                "付款单", null, 1L, LocalDateTime.now(), "FK-1",
                new BigDecimal("80"), new BigDecimal("50"));

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("0");
        assertThat(row.getPaymentAmount()).isEqualByComparingTo("80");
        assertThat(row.getAllocatedAmount()).isEqualByComparingTo("50");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("-80");
    }

    @Test
    void payablePaymentWithDiscountAdjustmentUsesActualPaymentAmount() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpFinancePaymentDO payment = new ErpFinancePaymentDO()
                .setId(1L)
                .setTotalPrice(new BigDecimal("100"))
                .setPaymentPrice(new BigDecimal("90"));

        BigDecimal amountWithDiscountAdjustment = ReflectionTestUtils.invokeMethod(service,
                "resolvePaymentDetailPaymentAmount", payment, Collections.singleton(1L));
        BigDecimal amountWithoutDiscountAdjustment = ReflectionTestUtils.invokeMethod(service,
                "resolvePaymentDetailPaymentAmount", payment, Collections.emptySet());

        assertThat(amountWithDiscountAdjustment).isEqualByComparingTo("90");
        assertThat(amountWithoutDiscountAdjustment).isEqualByComparingTo("100");
    }

    @Test
    void payableNegativePaymentKeepsSignAndIncreasesBalance() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        ErpPayableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildPaymentAllocatedRow",
                "付款单", null, 1L, LocalDateTime.now(), "FK-NEG",
                new BigDecimal("-20"), new BigDecimal("-10"));

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("0");
        assertThat(row.getPaymentAmount()).isEqualByComparingTo("-20");
        assertThat(row.getAllocatedAmount()).isEqualByComparingTo("10");
        assertThat(row.getWriteOffBaseAmount()).isEqualByComparingTo("20");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("20");
    }

    @Test
    void payableDetailRowsKeepSourceDeptAndFillDeptName() {
        ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
        DeptApi deptApi = mock(DeptApi.class);
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(7L);
        dept.setName("采购一部");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(7L, dept));
        ReflectionTestUtils.setField(service, "deptApi", deptApi);
        ErpPayableDetailRespVO purchaseIn = ReflectionTestUtils.invokeMethod(service, "buildAllocatedRow",
                "采购入库", 11, 1L, LocalDateTime.now(), "PI-DEPT",
                new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("100"), 7L);
        ErpPayableDetailRespVO payment = ReflectionTestUtils.invokeMethod(service, "buildPaymentAllocatedRow",
                "付款单", null, 2L, LocalDateTime.now(), "FK-DEPT",
                new BigDecimal("80"), BigDecimal.ZERO, 7L);

        ReflectionTestUtils.invokeMethod(service, "fillDeptNames", Arrays.asList(purchaseIn, payment));

        assertThat(purchaseIn).isNotNull();
        assertThat(payment).isNotNull();
        assertThat(purchaseIn.getDeptId()).isEqualTo(7L);
        assertThat(payment.getDeptId()).isEqualTo(7L);
        assertThat(purchaseIn.getDeptName()).isEqualTo("采购一部");
        assertThat(payment.getDeptName()).isEqualTo("采购一部");
    }

    @Test
    void receivableAllocationIsShownWithoutReducingBalanceTwice() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        ErpReceivableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildAllocatedRow",
                "销售出库", 21, 2L, LocalDateTime.now(), "SO-1",
                new BigDecimal("100"), new BigDecimal("-100"));

        assertThat(row).isNotNull();
        assertThat(row.getAllocatedAmount()).isEqualByComparingTo("100");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("100");
    }

    @Test
    void receivableSaleOutDetailUsesOriginalAmountAndMarksReturnAndAdjusted() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        ErpSaleOutDO saleOut = ErpSaleOutDO.builder()
                .id(2L)
                .no("XSCK-ADJ")
                .outTime(LocalDateTime.now())
                .totalPrice(new BigDecimal("120"))
                .discountPercent(BigDecimal.ZERO)
                .adjusted(true)
                .returnStatus(2)
                .deptId(8L)
                .build();
        ErpSaleOutItemDO item = ErpSaleOutItemDO.builder()
                .outId(2L)
                .count(new BigDecimal("3"))
                .productPrice(new BigDecimal("40"))
                .originalProductPrice(new BigDecimal("30"))
                .build();

        ErpReceivableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildSaleOutDetailRow",
                saleOut, BigDecimal.ZERO, Collections.singletonList(item));

        assertThat(row).isNotNull();
        assertThat(row.getDocNo()).isEqualTo("XSCK-ADJ");
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("90");
        assertThat(row.getWriteOffBaseAmount()).isEqualByComparingTo("90");
        assertThat(row.getReturnStatus()).isEqualTo(2);
        assertThat(row.getPriceAdjusted()).isTrue();
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("90");
    }

    @Test
    void receivableDecreaseBusinessIsShownAsNegativeIncrease() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        ErpReceivableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildAllocatedRow",
                "销售退货", 22, 2L, LocalDateTime.now(), "SR-1",
                new BigDecimal("-30"), BigDecimal.ZERO);

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("-30");
        assertThat(row.getOtherReceivableAmount()).isEqualByComparingTo("0");
        assertThat(row.getReceiptAmount()).isEqualByComparingTo("0");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("-30");
    }

    @Test
    void receivableOtherAmountUsesDedicatedColumn() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        ErpReceivableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildOtherReceivableRow",
                "其他应收", null, 2L, LocalDateTime.now(), "OR-1",
                new BigDecimal("25"));

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("0");
        assertThat(row.getOtherReceivableAmount()).isEqualByComparingTo("25");
        assertThat(row.getReceiptAmount()).isEqualByComparingTo("0");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("25");
    }

    @Test
    void receivableReceiptAmountUsesReceiptColumnOnly() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        ErpReceivableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildReceiptAllocatedRow",
                "收款单", null, 2L, LocalDateTime.now(), "RC-1",
                new BigDecimal("80"), new BigDecimal("50"));

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("0");
        assertThat(row.getOtherReceivableAmount()).isEqualByComparingTo("0");
        assertThat(row.getReceiptAmount()).isEqualByComparingTo("80");
        assertThat(row.getAllocatedAmount()).isEqualByComparingTo("50");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("-80");
    }

    @Test
    void receivableNegativeReceiptKeepsSignAndIncreasesBalance() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        ErpReceivableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildReceiptAllocatedRow",
                "收款单", null, 2L, LocalDateTime.now(), "RC-NEG",
                new BigDecimal("-44802"), new BigDecimal("-100"));

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("0");
        assertThat(row.getOtherReceivableAmount()).isEqualByComparingTo("0");
        assertThat(row.getReceiptAmount()).isEqualByComparingTo("-44802");
        assertThat(row.getAllocatedAmount()).isEqualByComparingTo("100");
        assertThat(row.getWriteOffBaseAmount()).isEqualByComparingTo("44802");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("44802");
    }

    @Test
    void receivableDetailRowsKeepSourceDeptAndFillDeptName() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        DeptApi deptApi = mock(DeptApi.class);
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(8L);
        dept.setName("销售二部");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(8L, dept));
        ReflectionTestUtils.setField(service, "deptApi", deptApi);
        ErpReceivableDetailRespVO saleOut = ReflectionTestUtils.invokeMethod(service, "buildAllocatedRow",
                "销售出库", 21, 1L, LocalDateTime.now(), "XS-DEPT",
                new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("100"), 8L);
        ErpReceivableDetailRespVO receipt = ReflectionTestUtils.invokeMethod(service, "buildReceiptAllocatedRow",
                "收款单", null, 2L, LocalDateTime.now(), "SK-DEPT",
                new BigDecimal("80"), BigDecimal.ZERO, 8L);

        ReflectionTestUtils.invokeMethod(service, "fillDeptNames", Arrays.asList(saleOut, receipt));

        assertThat(saleOut).isNotNull();
        assertThat(receipt).isNotNull();
        assertThat(saleOut.getDeptId()).isEqualTo(8L);
        assertThat(receipt.getDeptId()).isEqualTo(8L);
        assertThat(saleOut.getDeptName()).isEqualTo("销售二部");
        assertThat(receipt.getDeptName()).isEqualTo("销售二部");
    }

    @Test
    void receivableWriteOffRowDoesNotChangeRunningBalance() {
        ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
        ErpReceivableDetailRespVO row = ReflectionTestUtils.invokeMethod(service, "buildRow",
                "writeoff", 21, 2L, LocalDateTime.now(), "SO-1",
                new BigDecimal("-40"), true);

        assertThat(row).isNotNull();
        assertThat(row.getIncreaseAmount()).isEqualByComparingTo("0");
        assertThat(row.getOtherReceivableAmount()).isEqualByComparingTo("0");
        assertThat(row.getReceiptAmount()).isEqualByComparingTo("0");
        assertThat(row.getWriteOffAmount()).isEqualByComparingTo("40");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(service, "getChangeAmount", row))
                .isEqualByComparingTo("0");
    }
}
