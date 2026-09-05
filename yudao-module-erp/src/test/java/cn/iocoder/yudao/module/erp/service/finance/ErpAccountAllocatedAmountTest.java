package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ErpAccountAllocatedAmountTest {

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
