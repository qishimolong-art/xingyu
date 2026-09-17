package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPickReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutBillMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

/** 已取消领货：旧客户端不能重新创建或扣库存，含缺失/伪造来源输入。 */
class ErpStockOutBillRetiredServiceTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpStockOutBillServiceImpl service;
    @Mock private ErpStockOutBillMapper stockOutBillMapper;
    @Mock private ErpStockRecordService stockRecordService;

    @Test
    void oldCreateAlwaysRejectsBeforeAnyWrite() {
        ServiceException failure = assertThrows(ServiceException.class,
                () -> service.createFromSaleOut(new ErpSaleOutDO().setId(11L), Collections.emptyList()));
        assertEquals(409, failure.getCode());
        assertTrue(failure.getMessage().contains("销售审核直接扣库存"));
        assertThrows(ServiceException.class, () -> service.createFromSaleOut(null, null));
        verifyNoInteractions(stockOutBillMapper, stockRecordService);
    }

    @Test
    void oldPickAlwaysRejectsWithoutLoadingOrWritingStock() {
        ServiceException failure = assertThrows(ServiceException.class,
                () -> service.pick(new ErpStockOutBillPickReqVO().setId(12L)));
        assertEquals(409, failure.getCode());
        assertThrows(ServiceException.class, () -> service.pick(null));
        verifyNoInteractions(stockOutBillMapper, stockRecordService);
    }
}
