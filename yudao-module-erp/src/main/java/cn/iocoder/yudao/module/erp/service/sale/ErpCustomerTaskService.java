package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerTaskDO;

import java.util.List;

public interface ErpCustomerTaskService {

    Long createTask(ErpCustomerTaskSaveReqVO createReqVO);
    void updateTask(ErpCustomerTaskSaveReqVO updateReqVO);
    void deleteTask(Long id);
    ErpCustomerTaskDO getTask(Long id);
    PageResult<ErpCustomerTaskDO> getTaskPage(ErpCustomerTaskPageReqVO pageReqVO);
    List<ErpCustomerTaskDO> getTaskListByCustomerId(Long customerId);

}
