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
}
