package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordServiceImpl;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ErpFormOperateLogAspectTest extends BaseMockitoUnitTest {

    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void recordCreate_voidResultAndNoId_skipsFallbackLogWithoutUnboxingNull() {
        ErpFormOperateLogAspect aspect = new ErpFormOperateLogAspect();
        ReflectionTestUtils.setField(aspect, "operateLogService", operateLogService);
        ErpStockRecordServiceImpl target = mock(ErpStockRecordServiceImpl.class);
        ErpStockRecordCreateReqBO request = mock(ErpStockRecordCreateReqBO.class);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(aspect, "recordLog",
                target, "createStockRecord", new Object[]{request}, null,
                Collections.emptyList(), Collections.emptyMap()));

        verifyNoInteractions(operateLogService);
    }

}
