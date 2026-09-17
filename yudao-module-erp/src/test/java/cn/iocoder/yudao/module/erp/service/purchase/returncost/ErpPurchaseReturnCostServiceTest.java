package cn.iocoder.yudao.module.erp.service.purchase.returncost;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpPurchaseReturnCostServiceTest {
    @Test void disabledPathDoesNotReadUnmigratedTablesOrSources() {
        ErpPurchaseReturnCostService service = new ErpPurchaseReturnCostService();
        ErpPurchaseReturnCostRepository repository = mock(ErpPurchaseReturnCostRepository.class);
        ReflectionTestUtils.setField(service,"repository",repository);
        service.lockBeforeMutation(1L,null);
        service.lockBeforeMutations(Collections.singletonList(1L),null);
        service.validateLockedMutationSources(null);
        assertTrue(service.normalizeAndValidateSources(null,null,false).isEmpty());
        assertTrue(service.prepareApproval(null,null).isEmpty());
        assertTrue(service.preparedOrigins(1L).isEmpty());
        verifyNoInteractions(repository);
    }
}
