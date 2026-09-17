package cn.iocoder.yudao.module.erp.service.sale.returncost;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpSaleReturnCurrentCostServiceTest {
    @Test void incomingQuantityCanExceedBasisAndCumulativeRoundingDoesNotMultiplyRoundedPrice() {
        BigDecimal one = ErpSaleReturnCurrentCostService.proportional(new BigDecimal("650"), BigDecimal.ONE, new BigDecimal("3"));
        BigDecimal three = ErpSaleReturnCurrentCostService.proportional(new BigDecimal("650"), new BigDecimal("3"), new BigDecimal("3"));
        BigDecimal four = ErpSaleReturnCurrentCostService.proportional(new BigDecimal("650"), new BigDecimal("4"), new BigDecimal("3"));
        assertEquals(new BigDecimal("216.666667"), one);
        assertEquals(new BigDecimal("650.000000"), three);
        assertEquals(new BigDecimal("866.666667"), four);
        assertNotEquals(one.multiply(new BigDecimal("3")), three);
    }

    @Test void confirmationRequiresTwoExplicitAmountsButAcceptsConfirmedZero() {
        ConfirmRequest request = new ConfirmRequest();
        request.setId(1L); request.setExpectedRevision(0); request.setExpectedSourceSignature("source");
        request.setExpectedBasisSignature("basis"); request.setRequestKey("request-1"); request.setEvidence("核对依据");
        ConfirmItem item = new ConfirmItem(); item.setSourceItemId(10L); item.setEvidence("零成本赠品确认");
        item.setFinancialAmount(BigDecimal.ZERO); request.setItems(Collections.singletonList(item));
        assertThrows(RuntimeException.class, () -> ErpSaleReturnCurrentCostService.validateRequest(request));
        item.setSettlementAmount(BigDecimal.ZERO);
        assertDoesNotThrow(() -> ErpSaleReturnCurrentCostService.validateRequest(request));
        item.setFinancialAmount(new BigDecimal("0.0000001"));
        assertThrows(RuntimeException.class, () -> ErpSaleReturnCurrentCostService.validateRequest(request));
    }

    @Test void noOriginalModeRejectsAnyOriginalReferenceIncludingLegacyOrder() {
        ErpSaleReturnCurrentCostService service = new ErpSaleReturnCurrentCostService();
        ErpSaleReturnDO header = new ErpSaleReturnDO().setReturnMode(20);
        ErpSaleReturnItemDO item = new ErpSaleReturnItemDO();
        assertDoesNotThrow(() -> service.validateNoOriginalSource(header, Collections.singletonList(item)));
        item.setOrderItemId(5L);
        assertThrows(RuntimeException.class, () -> service.validateNoOriginalSource(header, Collections.singletonList(item)));
        item.setOrderItemId(null); header.setSourceOutNo("");
        assertThrows(RuntimeException.class, () -> service.validateNoOriginalSource(header, Collections.singletonList(item)));
    }

    @Test void disabledApprovalDoesNotAccessNewTables() {
        ErpSaleReturnCurrentCostService service = new ErpSaleReturnCurrentCostService();
        ErpSaleReturnCurrentCostRepository repository = mock(ErpSaleReturnCurrentCostRepository.class);
        ReflectionTestUtils.setField(service, "repository", repository);
        service.authorizeApproval(1L, null);
        assertFalse(service.isApprovedRetry(1L));
        assertTrue(service.prepareApproval(null, null).isEmpty());
        verifyNoInteractions(repository);
    }
}
