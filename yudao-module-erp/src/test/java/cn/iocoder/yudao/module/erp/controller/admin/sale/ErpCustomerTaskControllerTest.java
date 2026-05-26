package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerTaskDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerTaskController} 的单元测试
 */
public class ErpCustomerTaskControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerTaskController controller;

    @Mock
    private ErpCustomerTaskService taskService;

    @Test
    public void testCreateTask_paramPassThrough() {
        ErpCustomerTaskSaveReqVO reqVO = new ErpCustomerTaskSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setYear(2026);
        when(taskService.createTask(any())).thenReturn(88L);

        CommonResult<Long> result = controller.createTask(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(88L, result.getData());
        verify(taskService).createTask(eq(reqVO));
    }

    @Test
    public void testCreateTask_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerTaskController.class.getMethod("createTask", ErpCustomerTaskSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testUpdateTask_paramPassThrough() {
        ErpCustomerTaskSaveReqVO reqVO = new ErpCustomerTaskSaveReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(1L);
        reqVO.setYear(2026);

        CommonResult<Boolean> result = controller.updateTask(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(taskService).updateTask(eq(reqVO));
    }

    @Test
    public void testUpdateTask_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerTaskController.class.getMethod("updateTask", ErpCustomerTaskSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testDeleteTask_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteTask(77L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(taskService).deleteTask(eq(77L));
    }

    @Test
    public void testDeleteTask_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerTaskController.class.getMethod("deleteTask", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testGetTask_paramPassThrough() {
        ErpCustomerTaskDO task = new ErpCustomerTaskDO();
        task.setId(55L);
        task.setCustomerId(1L);
        task.setYear(2026);
        when(taskService.getTask(eq(55L))).thenReturn(task);

        CommonResult<ErpCustomerTaskRespVO> result = controller.getTask(55L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(55L, result.getData().getId());
        verify(taskService).getTask(eq(55L));
    }

    @Test
    public void testGetTask_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerTaskController.class.getMethod("getTask", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetTaskPage_paramPassThrough() {
        ErpCustomerTaskPageReqVO pageReqVO = new ErpCustomerTaskPageReqVO();
        pageReqVO.setCustomerId(1L);
        PageResult<ErpCustomerTaskDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(taskService.getTaskPage(eq(pageReqVO))).thenReturn(pageResult);

        CommonResult<PageResult<ErpCustomerTaskRespVO>> result = controller.getTaskPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        verify(taskService).getTaskPage(eq(pageReqVO));
    }

    @Test
    public void testGetTaskPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerTaskController.class.getMethod("getTaskPage", ErpCustomerTaskPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetTaskListByCustomer_paramPassThrough() {
        List<ErpCustomerTaskDO> list = Collections.singletonList(new ErpCustomerTaskDO());
        when(taskService.getTaskListByCustomerId(eq(1L))).thenReturn(list);

        CommonResult<List<ErpCustomerTaskRespVO>> result = controller.getTaskListByCustomer(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(taskService).getTaskListByCustomerId(eq(1L));
    }

    @Test
    public void testGetTaskListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerTaskController.class.getMethod("getTaskListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

}
