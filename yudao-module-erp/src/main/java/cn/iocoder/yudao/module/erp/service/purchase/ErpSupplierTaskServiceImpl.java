package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliertask.ErpSupplierTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierTaskServiceImpl implements ErpSupplierTaskService {

    @Resource
    private ErpSupplierTaskMapper supplierTaskMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierTask(ErpSupplierTaskSaveReqVO createReqVO) {
        validateMonth(createReqVO.getMonth());
        validateSupplierExists(createReqVO.getSupplierId());
        validateDuplicate(createReqVO, null);
        ErpSupplierTaskDO task = BeanUtils.toBean(createReqVO, ErpSupplierTaskDO.class);
        supplierTaskMapper.insert(task);
        return task.getId();
    }

    @Override
    public void updateSupplierTask(ErpSupplierTaskSaveReqVO updateReqVO) {
        validateMonth(updateReqVO.getMonth());
        validateSupplierExists(updateReqVO.getSupplierId());
        validateSupplierTaskExists(updateReqVO.getId());
        validateDuplicate(updateReqVO, updateReqVO.getId());
        supplierTaskMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierTaskDO.class));
    }

    @Override
    public void deleteSupplierTask(Long id) {
        validateSupplierTaskExists(id);
        supplierTaskMapper.deleteById(id);
    }

    private void validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw exception(SUPPLIER_TASK_MONTH_INVALID);
        }
    }

    private void validateDuplicate(ErpSupplierTaskSaveReqVO reqVO, Long selfId) {
        ErpSupplierTaskDO duplicate = supplierTaskMapper.selectByUniqueKey(
                reqVO.getSupplierId(), reqVO.getYear(), reqVO.getMonth(), reqVO.getTaskLevel());
        if (duplicate != null && (selfId == null || !selfId.equals(duplicate.getId()))) {
            throw exception(SUPPLIER_TASK_DUPLICATE);
        }
    }

    private void validateSupplierTaskExists(Long id) {
        if (supplierTaskMapper.selectById(id) == null) {
            throw exception(SUPPLIER_TASK_NOT_EXISTS);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierTaskDO getSupplierTask(Long id) {
        return supplierTaskMapper.selectById(id);
    }

    @Override
    public List<ErpSupplierTaskDO> getSupplierTaskList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierTaskMapper.selectListBySupplierId(supplierId);
    }

}
