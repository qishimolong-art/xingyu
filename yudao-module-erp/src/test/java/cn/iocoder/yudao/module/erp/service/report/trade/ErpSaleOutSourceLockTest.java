package cn.iocoder.yudao.module.erp.service.report.trade;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 实际Service在父锁后读取状态，不允许编辑或删除越过已经成功的审核。 */
class ErpSaleOutSourceLockTest {
    private ErpSaleOutServiceImpl service;
    private ErpSaleOutMapper mapper;
    @BeforeEach void setup(){service=new ErpSaleOutServiceImpl();mapper=mock(ErpSaleOutMapper.class);ReflectionTestUtils.setField(service,"saleOutMapper",mapper);}
    @Test void updateChecksLockedStateRatherThanEarlierRead(){
        when(mapper.selectByIdForUpdate(7L)).thenReturn(new ErpSaleOutDO().setId(7L).setNo("XS7").setStatus(20));
        ErpSaleOutSaveReqVO request=new ErpSaleOutSaveReqVO();request.setId(7L);
        assertThrows(ServiceException.class,()->service.updateSaleOut(request));
        verify(mapper).selectByIdForUpdate(7L);verify(mapper,never()).selectById(7L);verify(mapper,never()).updateById(any(ErpSaleOutDO.class));
    }
    @Test void deletionLocksDistinctParentsInSortedOrderBeforeRejectingApprovedSource(){
        when(mapper.selectByIdForUpdate(2L)).thenReturn(new ErpSaleOutDO().setId(2L).setNo("XS2").setStatus(10));
        when(mapper.selectByIdForUpdate(9L)).thenReturn(new ErpSaleOutDO().setId(9L).setNo("XS9").setStatus(20));
        assertThrows(ServiceException.class,()->service.deleteSaleOut(Arrays.asList(9L,2L,9L)));
        InOrder order=inOrder(mapper);order.verify(mapper).selectByIdForUpdate(2L);order.verify(mapper).selectByIdForUpdate(9L);
        verify(mapper,never()).deleteById(2L);verify(mapper,never()).deleteById(9L);
    }
    @Test void repeatApprovalRejectsLockedApprovedStateBeforeAnyStockChange(){
        when(mapper.selectByIdForUpdate(7L)).thenReturn(new ErpSaleOutDO().setId(7L).setNo("XS7").setStatus(20));
        assertThrows(ServiceException.class,()->service.updateSaleOutStatus(7L,20));
        verify(mapper).selectByIdForUpdate(7L);verify(mapper,never()).updateById(any(ErpSaleOutDO.class));
    }
}
