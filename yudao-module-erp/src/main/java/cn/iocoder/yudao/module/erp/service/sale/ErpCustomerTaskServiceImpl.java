package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_TASK_NOT_EXISTS;

@Service
@Validated
public class ErpCustomerTaskServiceImpl implements ErpCustomerTaskService {

    @Resource
    private ErpCustomerTaskMapper taskMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public Long createTask(ErpCustomerTaskSaveReqVO createReqVO) {
        customerService.validateCustomer(createReqVO.getCustomerId());
        ErpCustomerTaskDO task = BeanUtils.toBean(createReqVO, ErpCustomerTaskDO.class);
        taskMapper.insert(task);
        return task.getId();
    }

    @Override
    public void updateTask(ErpCustomerTaskSaveReqVO updateReqVO) {
        ErpCustomerTaskDO existing = validateTaskExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getCustomerId(), updateReqVO.getCustomerId())) {
            throw exception(CUSTOMER_TASK_NOT_EXISTS);
        }
        customerService.validateCustomer(updateReqVO.getCustomerId());
        taskMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerTaskDO.class));
    }

    @Override
    public void deleteTask(Long id) {
        validateTaskExists(id);
        taskMapper.deleteById(id);
    }

    private ErpCustomerTaskDO validateTaskExists(Long id) {
        ErpCustomerTaskDO existing = taskMapper.selectById(id);
        if (existing == null) throw exception(CUSTOMER_TASK_NOT_EXISTS);
        validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    private void validateVisibleCustomer(Long customerId) {
        if (customerId == null || customerService.getCustomer(customerId) == null) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerTaskDO getTask(Long id) {
        ErpCustomerTaskDO existing = taskMapper.selectById(id);
        if (existing != null) validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    @Override
    public PageResult<ErpCustomerTaskDO> getTaskPage(ErpCustomerTaskPageReqVO pageReqVO) {
        validateVisibleCustomer(pageReqVO.getCustomerId());
        if (pageReqVO.getPageSize() <= 0 || pageReqVO.getPageSize() > 100) pageReqVO.setPageSize(100);
        return taskMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerTaskDO> getTaskListByCustomerId(Long customerId) {
        validateVisibleCustomer(customerId);
        return taskMapper.selectListByCustomerId(customerId);
    }

}
