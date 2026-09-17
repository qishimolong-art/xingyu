package cn.iocoder.yudao.module.erp.service.stock.cost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpStockDimensionServiceTest {
    @Test
    void disabledNeverRequiresNewTablesOrTransaction() {
        ErpStockDimensionService service = new ErpStockDimensionService();
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbc);
        assertFalse(service.initializeDimensions(Collections.singletonList(dimension(1, 2))));
        service.reserveDimensions(Collections.singletonList(dimension(1, 2)));
        service.assertProductIdentityChange(1L);
        service.assertWarehouseDepartmentChange(2L, 3L);
        service.assertStockRemoval(4L);
        service.assertLegacyInsertAllowed();
        verifyNoInteractions(jdbc);
    }

    @Test
    void enabledGuardCannotTakeEphemeralAutocommitLock() {
        ErpStockDimensionService service = new ErpStockDimensionService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ServiceException error = assertThrows(ServiceException.class, service::lockIdentityChanges);
        assertTrue(error.getMessage().contains("业务事务"));
    }

    @Test
    void dimensionOrderingIsDeterministicAcrossReverseBatches() {
        List<ErpStockDO> ordered = ErpStockDimensionService.ordered(Arrays.asList(
                dimension(2, 1), dimension(1, 3), dimension(1, 2), dimension(1, 3)));
        assertEquals(3, ordered.size());
        assertEquals(1L, ordered.get(0).getProductId());
        assertEquals(2L, ordered.get(0).getWarehouseId());
        assertEquals(3L, ordered.get(1).getWarehouseId());
        assertEquals(2L, ordered.get(2).getProductId());
        assertThrows(ServiceException.class,
                () -> ErpStockDimensionService.ordered(Collections.singletonList(dimension(0, 2))));
    }

    @Test
    void enabledLegacyWriterCannotInventMissingStock() {
        ErpStockDimensionService service = new ErpStockDimensionService();
        ReflectionTestUtils.setField(service, "enabled", true);
        assertThrows(ServiceException.class, service::assertLegacyInsertAllowed);
    }

    private static ErpStockDO dimension(long product, long warehouse) {
        return new ErpStockDO().setProductId(product).setWarehouseId(warehouse);
    }
}
