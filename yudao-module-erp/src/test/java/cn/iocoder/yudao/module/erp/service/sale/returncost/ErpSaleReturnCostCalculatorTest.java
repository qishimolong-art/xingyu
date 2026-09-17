package cn.iocoder.yudao.module.erp.service.sale.returncost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpSaleReturnCostCalculatorTest {
    private static BigDecimal n(String value) { return new BigDecimal(value); }

    @Test void cumulativeAllocationReturnsExactOriginal650() {
        BigDecimal first = ErpSaleReturnCostCalculator.allocate(n("3"), n("650"), n("0"), n("0"), n("1"));
        BigDecimal second = ErpSaleReturnCostCalculator.allocate(n("3"), n("650"), n("1"), first, n("1"));
        BigDecimal third = ErpSaleReturnCostCalculator.allocate(n("3"), n("650"), n("2"), first.add(second), n("1"));
        assertEquals(n("216.666667"), first);
        assertEquals(n("216.666666"), second);
        assertEquals(n("650.000000"), first.add(second).add(third));
    }

    @Test void zeroConfirmedCostAllowedMissingAndOverReturnRejected() {
        assertEquals(n("0.000000"), ErpSaleReturnCostCalculator.allocate(n("3"), n("0"), n("0"), n("0"), n("1")));
        assertThrows(ServiceException.class, () -> ErpSaleReturnCostCalculator.allocate(n("3"), null, n("0"), n("0"), n("1")));
        assertThrows(ServiceException.class, () -> ErpSaleReturnCostCalculator.allocate(n("3"), n("650"), n("2"), n("433.333333"), n("2")));
    }

    @Test void fractionAndFullReturnDoNotMultiplyRoundedUnitPrice() {
        BigDecimal first = ErpSaleReturnCostCalculator.allocate(n("3"), n("650"), n("0"), n("0"), n("0.5"));
        assertEquals(n("108.333333"), first);
        BigDecimal remainder = ErpSaleReturnCostCalculator.allocate(n("3"), n("650"), n("0.5"), first, n("2.5"));
        assertEquals(n("650.000000"), first.add(remainder));
    }

    @Test void sourceSignatureIgnoresLifecycleButTracksQuantityAndSource() {
        ErpSaleReturnDO header = new ErpSaleReturnDO().setId(1L).setStatus(10).setSourceOutId(3L);
        ErpSaleReturnItemDO item = new ErpSaleReturnItemDO().setId(2L).setCount(n("1"));
        String before = ErpSaleReturnCostService.sourceSignature(header, Collections.singletonList(item));
        header.setStatus(20);
        assertEquals(before, ErpSaleReturnCostService.sourceSignature(header, Collections.singletonList(item)));
        item.setCount(n("2"));
        assertNotEquals(before, ErpSaleReturnCostService.sourceSignature(header, Collections.singletonList(item)));
    }

    @Test void disabledNeverReadsNewTablesOrSource() {
        ErpSaleReturnCostService service = new ErpSaleReturnCostService();
        ErpSaleReturnCostRepository repository = mock(ErpSaleReturnCostRepository.class);
        ReflectionTestUtils.setField(service, "repository", repository);
        assertTrue(service.prepareApproval(null, null).isEmpty());
        assertNull(service.getPreparedReturnStatus(1L));
        verifyNoInteractions(repository);
    }
}
