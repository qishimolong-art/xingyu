package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbill.ErpSupplierBillSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBillDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierBillMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierBillServiceImpl implements ErpSupplierBillService {

    @Resource
    private ErpSupplierBillMapper supplierBillMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierBill(ErpSupplierBillSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
        ErpSupplierBillDO bill = BeanUtils.toBean(createReqVO, ErpSupplierBillDO.class);
        supplierBillMapper.insert(bill);
        return bill.getId();
    }

    @Override
    public void updateSupplierBill(ErpSupplierBillSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getSupplierId());
        validateSupplierBillExists(updateReqVO.getId());
        supplierBillMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierBillDO.class));
    }

    @Override
    public void deleteSupplierBill(Long id) {
        validateSupplierBillExists(id);
        supplierBillMapper.deleteById(id);
    }

    private void validateSupplierBillExists(Long id) {
        if (supplierBillMapper.selectById(id) == null) {
            throw exception(SUPPLIER_BILL_NOT_EXISTS);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierBillDO getSupplierBill(Long id) {
        return supplierBillMapper.selectById(id);
    }

    @Override
    public List<ErpSupplierBillDO> getSupplierBillList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierBillMapper.selectListBySupplierId(supplierId);
    }

}
