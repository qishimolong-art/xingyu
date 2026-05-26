package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliertask.ErpSupplierTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierTaskDO;

import java.util.List;

public interface ErpSupplierTaskService {

    Long createSupplierTask(ErpSupplierTaskSaveReqVO createReqVO);
    void updateSupplierTask(ErpSupplierTaskSaveReqVO updateReqVO);
    void deleteSupplierTask(Long id);
    ErpSupplierTaskDO getSupplierTask(Long id);
    List<ErpSupplierTaskDO> getSupplierTaskList(Long supplierId);

}
