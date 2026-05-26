package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_TASK_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ErpCustomerTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerTaskServiceImpl customerTaskService;

    @Mock
    private ErpCustomerTaskMapper taskMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== createTask ====================

    @Test
    public void testCreateTask_success() {
        // 准备参数
        ErpCustomerTaskSaveReqVO reqVO = new ErpCustomerTaskSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setYear(2026);
        reqVO.setMonth(5);
        reqVO.setTaskLevel("A");
        reqVO.setTaskAmount(new BigDecimal("100000"));
        reqVO.setRemark("年度任务");

        // mock：insert 回填 id
        when(taskMapper.insert(ArgumentMatchers.<ErpCustomerTaskDO>any())).thenAnswer(invocation -> {
            ErpCustomerTaskDO task = invocation.getArgument(0);
            task.setId(200L);
            return 1;
        });

        // 执行
        Long resultId = customerTaskService.createTask(reqVO);

        // 断言
        assertEquals(200L, resultId);
        verify(customerService).validateCustomer(eq(1L));
        verify(taskMapper).insert(ArgumentMatchers.<ErpCustomerTaskDO>argThat(task ->
                task.getCustomerId().equals(1L)
                        && task.getYear().equals(2026)
                        && task.getMonth().equals(5)
                        && "A".equals(task.getTaskLevel())));
    }

    @Test
    public void testCreateTask_customerNotExists() {
        // 准备参数
        ErpCustomerTaskSaveReqVO reqVO = new ErpCustomerTaskSaveReqVO();
        reqVO.setCustomerId(999L);
        reqVO.setYear(2026);

        // mock：客户校验抛异常
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(999L));

        // 执行 & 断言
        assertServiceException(() -> customerTaskService.createTask(reqVO), CUSTOMER_NOT_EXISTS);
        verify(taskMapper, never()).insert(ArgumentMatchers.<ErpCustomerTaskDO>any());
    }

    // ==================== updateTask ====================

    @Test
    public void testUpdateTask_success() {
        // 准备参数
        ErpCustomerTaskSaveReqVO reqVO = new ErpCustomerTaskSaveReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(1L);
        reqVO.setYear(2026);
        reqVO.setMonth(6);
        reqVO.setTaskAmount(new BigDecimal("200000"));

        // mock：存在校验通过
        when(taskMapper.selectById(eq(10L))).thenReturn(new ErpCustomerTaskDO().setId(10L));

        // 执行
        customerTaskService.updateTask(reqVO);

        // 断言
        verify(customerService).validateCustomer(eq(1L));
        verify(taskMapper).updateById(ArgumentMatchers.<ErpCustomerTaskDO>argThat(task ->
                task.getId().equals(10L)
                        && task.getYear().equals(2026)
                        && task.getMonth().equals(6)));
    }

    @Test
    public void testUpdateTask_notExists() {
        // 准备参数
        ErpCustomerTaskSaveReqVO reqVO = new ErpCustomerTaskSaveReqVO();
        reqVO.setId(999L);
        reqVO.setCustomerId(1L);
        reqVO.setYear(2026);

        // mock：不存在
        when(taskMapper.selectById(eq(999L))).thenReturn(null);

        // 执行 & 断言
        assertServiceException(() -> customerTaskService.updateTask(reqVO), CUSTOMER_TASK_NOT_EXISTS);
        verify(taskMapper, never()).updateById(ArgumentMatchers.<ErpCustomerTaskDO>any());
    }

    // ==================== deleteTask ====================

    @Test
    public void testDeleteTask_success() {
        // mock：存在
        when(taskMapper.selectById(eq(20L))).thenReturn(new ErpCustomerTaskDO().setId(20L));

        // 执行
        customerTaskService.deleteTask(20L);

        // 断言
        verify(taskMapper).deleteById(eq(20L));
    }

    @Test
    public void testDeleteTask_notExists() {
        // mock：不存在
        when(taskMapper.selectById(eq(999L))).thenReturn(null);

        // 执行 & 断言
        assertServiceException(() -> customerTaskService.deleteTask(999L), CUSTOMER_TASK_NOT_EXISTS);
        verify(taskMapper, never()).deleteById(any());
    }

    // ==================== getTaskListByCustomerId ====================

    @Test
    public void testGetTaskListByCustomerId_success() {
        // 准备数据
        Long customerId = 5L;
        ErpCustomerTaskDO task1 = new ErpCustomerTaskDO().setId(1L).setCustomerId(customerId).setYear(2026).setMonth(1);
        ErpCustomerTaskDO task2 = new ErpCustomerTaskDO().setId(2L).setCustomerId(customerId).setYear(2026).setMonth(2);
        when(taskMapper.selectListByCustomerId(eq(customerId))).thenReturn(Arrays.asList(task1, task2));

        // 执行
        List<ErpCustomerTaskDO> result = customerTaskService.getTaskListByCustomerId(customerId);

        // 断言
        assertEquals(2, result.size());
        verify(customerService).validateCustomer(eq(customerId));
        verify(taskMapper).selectListByCustomerId(eq(customerId));
    }

}
