package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.service.stock.ErpSaleCartTransferOutApprovedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class ErpSaleCartTransferOutApprovedListenerTest extends BaseMockitoUnitTest {

    private ErpSaleCartTransferOutApprovedListener listener;

    @Mock
    private ErpSaleCartService saleCartService;

    @BeforeEach
    void setUp() {
        listener = new ErpSaleCartTransferOutApprovedListener(saleCartService);
    }

    @Test
    void onTransferOutApproved_triggersSaleCartAutoFinalApprove() {
        listener.onTransferOutApproved(new ErpSaleCartTransferOutApprovedEvent(72L, 10L, 99L));

        verify(saleCartService).autoFinalApproveAfterTransferOut(72L, 99L);
    }

    @Test
    void onTransferOutApproved_propagatesFailureToRollbackApproval() {
        doThrow(new IllegalStateException("persistent database error"))
                .when(saleCartService).autoFinalApproveAfterTransferOut(72L, 99L);

        assertThrows(IllegalStateException.class, () -> listener.onTransferOutApproved(
                new ErpSaleCartTransferOutApprovedEvent(72L, 10L, 99L)));

        verify(saleCartService).autoFinalApproveAfterTransferOut(72L, 99L);
    }

    @Test
    void onTransferOutApproved_isSynchronousEventListener() throws NoSuchMethodException {
        Method method = ErpSaleCartTransferOutApprovedListener.class.getMethod(
                "onTransferOutApproved", ErpSaleCartTransferOutApprovedEvent.class);

        assertNotNull(method.getAnnotation(EventListener.class));
    }

}
