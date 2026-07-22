package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ErpStockLockServiceImplTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 7L;

    @InjectMocks
    private ErpStockLockServiceImpl stockLockService;

    @Mock
    private ErpStockLockMapper stockLockMapper;
    @Mock
    private ErpStockMapper stockMapper;

    @BeforeEach
    void setUpContext() {
        DataPermissionContextHolder.clear();
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDownContext() {
        DataPermissionContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void testLockStock_crossDeptWarehouse_ignoresDeptPermissionAndKeepsTenant() {
        Long productId = 955L;
        Long warehouseId = 1L;
        BigDecimal count = new BigDecimal("1.000000");
        ErpStockDO stock = new ErpStockDO().setId(974L).setProductId(productId).setWarehouseId(warehouseId)
                .setDeptId(148L).setCount(new BigDecimal("18.000000")).setLockCount(BigDecimal.ZERO);
        when(stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId)).thenAnswer(invocation -> {
            assertInventoryAccessContext();
            return stock;
        });
        when(stockMapper.tryIncreaseLockCount(stock.getId(), count)).thenAnswer(invocation -> {
            assertInventoryAccessContext();
            return 1;
        });

        stockLockService.lockStock(productId, warehouseId, count, 10, 114L, 187L,
                "XSST20260716000004");

        ArgumentCaptor<ErpStockLockDO> lockCaptor = ArgumentCaptor.forClass(ErpStockLockDO.class);
        verify(stockLockMapper).insert(lockCaptor.capture());
        ErpStockLockDO lock = lockCaptor.getValue();
        assertEquals(productId, lock.getProductId());
        assertEquals(warehouseId, lock.getWarehouseId());
        assertEquals(count, lock.getLockCount());
        assertEquals(114L, lock.getBizId());
        assertEquals(187L, lock.getBizItemId());
        assertEquals(1, lock.getStatus());
        assertContextRestored();
    }

    @Test
    void testLockStock_atomicUpdateFails_reportsActualAvailableStock() {
        Long productId = 955L;
        Long warehouseId = 1L;
        BigDecimal requiredCount = new BigDecimal("17");
        ErpStockDO stock = new ErpStockDO().setId(974L).setProductId(productId).setWarehouseId(warehouseId)
                .setCount(new BigDecimal("18")).setLockCount(new BigDecimal("2"));
        when(stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId)).thenAnswer(invocation -> {
            assertInventoryAccessContext();
            return stock;
        });
        when(stockMapper.tryIncreaseLockCount(stock.getId(), requiredCount)).thenAnswer(invocation -> {
            assertInventoryAccessContext();
            return 0;
        });

        assertServiceException(
                () -> stockLockService.lockStock(productId, warehouseId, requiredCount, 10, 114L, 187L, "CART-1"),
                STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH, productId, warehouseId, new BigDecimal("16"), requiredCount);

        verify(stockLockMapper, never()).insert(any(ErpStockLockDO.class));
        assertContextRestored();
    }

    @Test
    void testLockStock_repeatedBizItem_doesNotLockAgain() {
        when(stockLockMapper.selectActiveByBizItem(10, 114L, 187L))
                .thenReturn(ErpStockLockDO.builder().id(1001L).status(1).build());

        stockLockService.lockStock(955L, 1L, BigDecimal.ONE, 10, 114L, 187L, "CART-1");

        verifyNoInteractions(stockMapper);
        verify(stockLockMapper, never()).insert(any(ErpStockLockDO.class));
        assertContextRestored();
    }

    @Test
    void testUnlockStock_crossDeptWarehouse_ignoresDeptPermission() {
        ErpStockLockDO lock = activeLock();
        ErpStockDO stock = stockForLock(lock);
        when(stockLockMapper.selectListByBiz(10, 114L)).thenReturn(Collections.singletonList(lock));
        when(stockLockMapper.updateStatusIfActive(lock.getId(), 2)).thenReturn(1);
        stubStockDecrease(lock, stock);

        stockLockService.unlockStock(10, 114L);

        verify(stockMapper).tryDecreaseLockCount(stock.getId(), lock.getLockCount());
        assertContextRestored();
    }

    @Test
    void testDeductStock_crossDeptWarehouse_ignoresDeptPermission() {
        ErpStockLockDO lock = activeLock();
        ErpStockDO stock = stockForLock(lock);
        when(stockLockMapper.selectListByBiz(10, 114L)).thenReturn(Collections.singletonList(lock));
        when(stockLockMapper.updateStatusIfActive(lock.getId(), 3)).thenReturn(1);
        stubStockDecrease(lock, stock);

        stockLockService.deductStock(10, 114L);

        verify(stockMapper).tryDecreaseLockCount(stock.getId(), lock.getLockCount());
        assertContextRestored();
    }

    @Test
    void testTransferStockLocks_movesLockCountAndWarehouse() {
        ErpStockLockDO lock = activeLock();
        Long toWarehouseId = 2L;
        ErpStockDO fromStock = stockForLock(lock);
        ErpStockDO toStock = new ErpStockDO().setId(975L).setProductId(lock.getProductId())
                .setWarehouseId(toWarehouseId).setDeptId(149L)
                .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO);
        when(stockLockMapper.selectListByBiz(10, 114L)).thenReturn(Collections.singletonList(lock));
        when(stockLockMapper.updateWarehouseIfActive(lock.getId(), lock.getWarehouseId(), toWarehouseId))
                .thenReturn(1);
        when(stockMapper.selectByProductIdAndWarehouseId(lock.getProductId(), lock.getWarehouseId()))
                .thenReturn(fromStock);
        when(stockMapper.tryDecreaseLockCount(fromStock.getId(), lock.getLockCount())).thenReturn(1);
        when(stockMapper.selectByProductIdAndWarehouseId(lock.getProductId(), toWarehouseId))
                .thenReturn(toStock);
        when(stockMapper.tryIncreaseLockCount(toStock.getId(), lock.getLockCount())).thenReturn(1);

        stockLockService.transferStockLocks(10, 114L, lock.getProductId(),
                lock.getWarehouseId(), toWarehouseId);

        verify(stockLockMapper).updateWarehouseIfActive(lock.getId(), lock.getWarehouseId(), toWarehouseId);
        verify(stockMapper).tryDecreaseLockCount(fromStock.getId(), lock.getLockCount());
        verify(stockMapper).tryIncreaseLockCount(toStock.getId(), lock.getLockCount());
        assertContextRestored();
    }

    @Test
    void testTransferStockLocks_unrelatedLock_doesNotChangeStock() {
        ErpStockLockDO lock = activeLock();
        when(stockLockMapper.selectListByBiz(10, 114L)).thenReturn(Collections.singletonList(lock));

        stockLockService.transferStockLocks(10, 114L, 956L, 1L, 2L);

        verify(stockLockMapper, never()).updateWarehouseIfActive(any(), any(), any());
        verifyNoInteractions(stockMapper);
        assertContextRestored();
    }

    @Test
    void testUnlockStock_alreadyHandled_doesNotDecreaseAgain() {
        ErpStockLockDO lock = activeLock();
        when(stockLockMapper.selectListByBiz(10, 114L)).thenReturn(Collections.singletonList(lock));
        when(stockLockMapper.updateStatusIfActive(lock.getId(), 2)).thenReturn(0);

        stockLockService.unlockStock(10, 114L);

        verifyNoInteractions(stockMapper);
        assertContextRestored();
    }

    private void stubStockDecrease(ErpStockLockDO lock, ErpStockDO stock) {
        when(stockMapper.selectByProductIdAndWarehouseId(lock.getProductId(), lock.getWarehouseId()))
                .thenAnswer(invocation -> {
                    assertInventoryAccessContext();
                    return stock;
                });
        when(stockMapper.tryDecreaseLockCount(stock.getId(), lock.getLockCount())).thenAnswer(invocation -> {
            assertInventoryAccessContext();
            return 1;
        });
    }

    private static ErpStockLockDO activeLock() {
        return ErpStockLockDO.builder().id(1001L).productId(955L).warehouseId(1L)
                .lockCount(BigDecimal.ONE).bizType(10).bizId(114L).bizItemId(187L).status(1).build();
    }

    private static ErpStockDO stockForLock(ErpStockLockDO lock) {
        return new ErpStockDO().setId(974L).setProductId(lock.getProductId())
                .setWarehouseId(lock.getWarehouseId()).setDeptId(148L)
                .setCount(new BigDecimal("18")).setLockCount(lock.getLockCount());
    }

    private static void assertInventoryAccessContext() {
        assertNotNull(DataPermissionContextHolder.get());
        assertFalse(DataPermissionContextHolder.get().enable());
        assertEquals(TENANT_ID, TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
    }

    private static void assertContextRestored() {
        assertNull(DataPermissionContextHolder.get());
        assertEquals(TENANT_ID, TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
    }

}
