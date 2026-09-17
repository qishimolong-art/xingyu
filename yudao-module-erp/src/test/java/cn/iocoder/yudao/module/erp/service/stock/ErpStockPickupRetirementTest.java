package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Old cached clients can still call the service: every historical source must be unable to add stock. */
class ErpStockPickupRetirementTest {
    @Test void allHistoricalSourcesRejectPickupWithoutInventoryOrPickupWrites() {
        for(Integer source:Arrays.asList(10,70,999,null)) {
            ErpStockInBillServiceImpl service=new ErpStockInBillServiceImpl();
            ErpStockInBillMapper bills=mock(ErpStockInBillMapper.class);
            ErpStockInBillItemMapper items=mock(ErpStockInBillItemMapper.class);
            ErpStockInBillPickupRecordMapper records=mock(ErpStockInBillPickupRecordMapper.class);
            ErpStockRecordService stock=mock(ErpStockRecordService.class);
            ReflectionTestUtils.setField(service,"stockInBillMapper",bills);
            ReflectionTestUtils.setField(service,"stockInBillItemMapper",items);
            ReflectionTestUtils.setField(service,"pickupRecordMapper",records);
            ReflectionTestUtils.setField(service,"stockRecordService",stock);
            when(bills.selectById(1L)).thenReturn(new ErpStockInBillDO().setId(1L).setSourceBizType(source).setStatus(10));
            ErpStockInBillPickupReqVO request=new ErpStockInBillPickupReqVO();request.setId(1L);
            ErpStockInBillPickupReqVO.Item line=new ErpStockInBillPickupReqVO.Item();line.setItemId(2L);line.setPickupCount(BigDecimal.ONE);
            request.setItems(Collections.singletonList(line));
            ServiceException exception=assertThrows(ServiceException.class,()->service.pickup(request),"source="+source);
            assertTrue(exception.getMessage().contains("领货")||exception.getMessage().contains("提货"));
            verifyNoInteractions(stock,items,records);
            verify(bills,never()).updateById(any(ErpStockInBillDO.class));
        }
    }
}
