package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerExtendMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_EXTEND_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ErpCustomerExtendServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerExtendServiceImpl customerExtendService;

    @Mock
    private ErpCustomerExtendMapper extendMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== createExtend ====================

    @Test
    public void testCreateExtend_success() {
        // 准备参数
        ErpCustomerExtendSaveReqVO reqVO = new ErpCustomerExtendSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setExtendKey("birthday");
        reqVO.setExtendName("生日");
        reqVO.setExtendValue("1990-01-01");
        reqVO.setExtendType("date");
        reqVO.setSort(0);

        // mock：insert 回填 id
        when(extendMapper.insert(ArgumentMatchers.<ErpCustomerExtendDO>any())).thenAnswer(invocation -> {
            ErpCustomerExtendDO extend = invocation.getArgument(0);
            extend.setId(300L);
            return 1;
        });

        // 执行
        Long resultId = customerExtendService.createExtend(reqVO);

        // 断言
        assertEquals(300L, resultId);
        verify(customerService).validateCustomer(eq(1L));
        verify(extendMapper).insert(ArgumentMatchers.<ErpCustomerExtendDO>argThat(extend ->
                extend.getCustomerId().equals(1L)
                        && "birthday".equals(extend.getExtendKey())
                        && "生日".equals(extend.getExtendName())
                        && "1990-01-01".equals(extend.getExtendValue())));
    }

    @Test
    public void testCreateExtend_customerNotExists() {
        // 准备参数
        ErpCustomerExtendSaveReqVO reqVO = new ErpCustomerExtendSaveReqVO();
        reqVO.setCustomerId(999L);
        reqVO.setExtendKey("birthday");

        // mock：客户校验抛异常
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(999L));

        // 执行 & 断言
        assertServiceException(() -> customerExtendService.createExtend(reqVO), CUSTOMER_NOT_EXISTS);
        verify(extendMapper, never()).insert(ArgumentMatchers.<ErpCustomerExtendDO>any());
    }

    // ==================== updateExtend ====================

    @Test
    public void testUpdateExtend_success() {
        // 准备参数
        ErpCustomerExtendSaveReqVO reqVO = new ErpCustomerExtendSaveReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(1L);
        reqVO.setExtendKey("hobby");
        reqVO.setExtendValue("足球");

        // mock：存在校验通过
        when(extendMapper.selectById(eq(10L))).thenReturn(new ErpCustomerExtendDO().setId(10L));

        // 执行
        customerExtendService.updateExtend(reqVO);

        // 断言
        verify(customerService).validateCustomer(eq(1L));
        verify(extendMapper).updateById(ArgumentMatchers.<ErpCustomerExtendDO>argThat(extend ->
                extend.getId().equals(10L)
                        && "hobby".equals(extend.getExtendKey())
                        && "足球".equals(extend.getExtendValue())));
    }

    @Test
    public void testUpdateExtend_notExists() {
        // 准备参数
        ErpCustomerExtendSaveReqVO reqVO = new ErpCustomerExtendSaveReqVO();
        reqVO.setId(999L);
        reqVO.setCustomerId(1L);
        reqVO.setExtendKey("hobby");

        // mock：不存在
        when(extendMapper.selectById(eq(999L))).thenReturn(null);

        // 执行 & 断言
        assertServiceException(() -> customerExtendService.updateExtend(reqVO), CUSTOMER_EXTEND_NOT_EXISTS);
        verify(extendMapper, never()).updateById(ArgumentMatchers.<ErpCustomerExtendDO>any());
    }

    // ==================== deleteExtend ====================

    @Test
    public void testDeleteExtend_success() {
        // mock：存在
        when(extendMapper.selectById(eq(20L))).thenReturn(new ErpCustomerExtendDO().setId(20L));

        // 执行
        customerExtendService.deleteExtend(20L);

        // 断言
        verify(extendMapper).deleteById(eq(20L));
    }

    @Test
    public void testDeleteExtend_notExists() {
        // mock：不存在
        when(extendMapper.selectById(eq(999L))).thenReturn(null);

        // 执行 & 断言
        assertServiceException(() -> customerExtendService.deleteExtend(999L), CUSTOMER_EXTEND_NOT_EXISTS);
        verify(extendMapper, never()).deleteById(any());
    }

    // ==================== getExtendListByCustomerId ====================

    @Test
    public void testGetExtendListByCustomerId_success() {
        // 准备数据
        Long customerId = 5L;
        ErpCustomerExtendDO extend1 = new ErpCustomerExtendDO().setId(1L).setCustomerId(customerId).setExtendKey("k1");
        ErpCustomerExtendDO extend2 = new ErpCustomerExtendDO().setId(2L).setCustomerId(customerId).setExtendKey("k2");
        when(extendMapper.selectListByCustomerId(eq(customerId))).thenReturn(Arrays.asList(extend1, extend2));

        // 执行
        List<ErpCustomerExtendDO> result = customerExtendService.getExtendListByCustomerId(customerId);

        // 断言
        assertEquals(2, result.size());
        verify(customerService).validateCustomer(eq(customerId));
        verify(extendMapper).selectListByCustomerId(eq(customerId));
    }

}
