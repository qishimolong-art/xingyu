package cn.iocoder.yudao.module.erp.service.stock.cost;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DuplicateKeyException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpDualCostPostingServiceTest {
    private ErpDualCostPostingService service;
    private ErpDualCostLedgerRepository repository;
    private RuntimeException insertFailure;
    private long insertedId;
    private final LocalDateTime cutover = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setup() {
        repository = mock(ErpDualCostLedgerRepository.class, invocation -> {
            if ("insertPosting".equals(invocation.getMethod().getName())) {
                if (insertFailure != null) { throw insertFailure; }
                return insertedId;
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
        service = new ErpDualCostPostingService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "tradeSnapshotService",
                mock(cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService.class));
        ReflectionTestUtils.setField(service, "cutover", cutover.toString());
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        TransactionSynchronizationManager.clear();
    }

    private ErpStockRecordCreateReqBO sale() {
        return new ErpStockRecordCreateReqBO(10L, 20L, new BigDecimal("-2"), 50, 100L, 1001L,
                "SALE-100", new BigDecimal("150"), cutover.plusDays(1)).setAccountingDeptId(30L);
    }

    private void enabledWithOpening() {
        ReflectionTestUtils.setField(service, "enabled", true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        Map<String, Object> stock = new HashMap<>();
        stock.put("id", 9L);
        stock.put("dept_id", 40L);
        stock.put("count", new BigDecimal("10"));
        when(repository.lockStock(1, 10, 20)).thenReturn(stock);
        Map<String, Object> balance = new HashMap<>();
        balance.put("cutover_at", Timestamp.valueOf(cutover));
        balance.put("quantity", new BigDecimal("10"));
        balance.put("financial_amount", new BigDecimal("1000"));
        balance.put("settlement_amount", new BigDecimal("1200"));
        balance.put("legacy_record_id", 0L);
        balance.put("stock_dept_id", 40L);
        when(repository.lockBalance(1, 9)).thenReturn(balance);
    }

    @Test
    void disabledDoesNotAccessUnmigratedTables() {
        Runnable legacy = mock(Runnable.class);
        assertFalse(service.post(sale(), legacy));
        verifyNoInteractions(repository, legacy);
        service.assertLegacyMutationAllowed(false);
    }

    @Test
    void enabledRequiresActualApprovalTransaction() {
        ReflectionTestUtils.setField(service, "enabled", true);
        assertThrows(IllegalStateException.class, () -> service.post(sale(), () -> {}));
        verifyNoInteractions(repository);
    }

    @Test
    void sameActionSamePayloadSkipsEveryWrite() {
        enabledWithOpening();
        ErpStockRecordCreateReqBO request = sale();
        Map<String, Object> existing = new HashMap<>();
        existing.put("request_hash", ErpDualCostPostingService.fingerprint(request));
        insertFailure = actionConflict();
        when(repository.findPosting(1, ErpDualCostPostingService.actionKey(request))).thenReturn(existing);
        Runnable legacy = mock(Runnable.class);
        assertTrue(service.post(request, legacy));
        verifyNoInteractions(legacy);
        assertTrue(mockingDetails(repository).getInvocations().stream()
                .noneMatch(invocation -> "execute".equals(invocation.getMethod().getName())));
    }

    @Test
    void sameActionChangedPayloadFails() {
        enabledWithOpening();
        Map<String, Object> existing = new HashMap<>();
        existing.put("request_hash", ErpDualCostPostingService.fingerprint(sale()));
        insertFailure = actionConflict();
        when(repository.findPosting(anyLong(), anyString())).thenReturn(existing);
        assertThrows(IllegalStateException.class,
                () -> service.post(sale().setCount(new BigDecimal("-3")), () -> fail("不得改变实物库存")));
    }

    @Test
    void missingOpeningAndUnadaptedTypesFailBeforeLegacy() {
        enabledWithOpening();
        when(repository.lockBalance(1, 9)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.post(sale(), () -> fail("缺失期初不能写旧账")));
        assertThrows(IllegalStateException.class, () -> service.post(sale().setBizType(32), () -> fail("调拨未适配")));
    }

    @Test
    void purchaseInclusivePriceIsNotAutomaticallyConfirmedCost() {
        ErpStockRecordCreateReqBO request = sale().setBizType(70).setCount(BigDecimal.ONE)
                .setSourcePriceBasis("INCLUSIVE_UNCONFIRMED");
        assertThrows(IllegalStateException.class, () -> ErpDualCostPostingService.validateRequest(request));
        request.setCostBasisConfirmed(true).setFinancialUnitCost(new BigDecimal("100"))
                .setSettlementUnitCost(new BigDecimal("120"));
        assertThrows(IllegalStateException.class, () -> ErpDualCostPostingService.validateRequest(request));
        request.setFinancialMovementAmount(new BigDecimal("100")).setSettlementMovementAmount(new BigDecimal("100"))
                .setCostConfirmationId(1L).setCostConfirmationRevision(1);
        assertDoesNotThrow(() -> ErpDualCostPostingService.validateRequest(request));
    }

    @Test
    void replayIdentityAllowsExplicitNewActionAndNormalizesDecimalScale() {
        ErpStockRecordCreateReqBO first = sale();
        ErpStockRecordCreateReqBO equivalent = sale().setCount(new BigDecimal("-2.000000"));
        assertEquals(ErpDualCostPostingService.fingerprint(first), ErpDualCostPostingService.fingerprint(equivalent));
        assertNotEquals(ErpDualCostPostingService.actionKey(first),
                ErpDualCostPostingService.actionKey(sale().setPostingActionKey("APPROVE_2")));
    }

    @Test
    void openingMustMatchPhysicalQuantityAndCannotBeOverwritten() {
        enabledWithOpening();
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThrows(IllegalStateException.class, () -> service.confirmOpening(10, 20, new BigDecimal("9"),
                new BigDecimal("900"), new BigDecimal("1080"), cutover, "核对单1", 1));
        assertThrows(IllegalStateException.class, () -> service.confirmOpening(10, 20, new BigDecimal("10"),
                new BigDecimal("1000"), new BigDecimal("1200"), cutover, "核对单1", 1));
    }
    @Test
    void openingPreparationRequiresLegacyCursorEvenBeforeSwitchEnabled() {
        enabledWithOpening();
        ReflectionTestUtils.setField(service, "enabled", false);
        when(repository.lockBalance(1, 9)).thenReturn(null);
        when(repository.verifyLegacyCursorForOpening(1, 10, 20)).thenReturn(88L);
        service.confirmOpening(10, 20, new BigDecimal("10"), new BigDecimal("1000"),
                new BigDecimal("1200"), cutover, "核对单1", 1);
        verify(repository).verifyLegacyCursorForOpening(1, 10, 20);
        verify(repository).execute(startsWith("INSERT INTO erp_stock_dual_cost_balance"), any());
    }
    @Test
    void purchaseReturnAcceptsRefundPriceButNeverCallerCostOverride() {
        ErpStockRecordCreateReqBO request = sale().setBizType(80).setUnitPrice(new BigDecimal("1000"));
        assertDoesNotThrow(() -> ErpDualCostPostingService.validateRequest(request));
        request.setFinancialMovementAmount(new BigDecimal("-20"));
        assertThrows(IllegalStateException.class, () -> ErpDualCostPostingService.validateRequest(request));
        request.setFinancialMovementAmount(null).setFinancialUnitCost(new BigDecimal("10"));
        assertThrows(IllegalStateException.class, () -> ErpDualCostPostingService.validateRequest(request));
    }

    @Test
    void unrelatedUniqueConstraintIsNotTreatedAsIdempotency() {
        enabledWithOpening();
        DuplicateKeyException failure = new DuplicateKeyException("other constraint",
                new SQLException("Duplicate entry '1' for key 'other_unique'", "23000", 1062));
        insertFailure = failure;
        assertSame(failure, assertThrows(DuplicateKeyException.class, () -> service.post(sale(), () -> fail("不能写旧库存"))));
        verify(repository, never()).findPosting(anyLong(), anyString());
    }

    @Test
    void unfinishedClaimCannotPassBeforeCommitEvenIfCallerCatchesFailure() {
        enabledWithOpening();
        insertedId = 99L;
        assertThrows(IllegalArgumentException.class,
                () -> service.post(sale().setCount(new BigDecimal("-11")), () -> fail("数量不足不能写库存")));
        assertFalse(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        assertThrows(IllegalStateException.class,
                () -> TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.beforeCommit(false)));
        verify(repository, never()).findPosting(anyLong(), anyString());
    }

    private DuplicateKeyException actionConflict() {
        return new DuplicateKeyException("actual action conflict", new SQLException(
                "Duplicate entry '1-50:1001:APPROVE' for key 'erp_stock_dual_cost_posting.uk_tenant_action'", "23000", 1062));
    }

}
