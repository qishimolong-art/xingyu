package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontract.ErpSupplierContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContractDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierContractMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierContractServiceImpl implements ErpSupplierContractService {

    @Resource
    private ErpSupplierContractMapper supplierContractMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierContract(ErpSupplierContractSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
        ErpSupplierContractDO contract = BeanUtils.toBean(createReqVO, ErpSupplierContractDO.class);
        supplierContractMapper.insert(contract);
        return contract.getId();
    }

    @Override
    public void updateSupplierContract(ErpSupplierContractSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getSupplierId());
        validateSupplierContractExists(updateReqVO.getId());
        supplierContractMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierContractDO.class));
    }

    @Override
    public void deleteSupplierContract(Long id) {
        validateSupplierContractExists(id);
        supplierContractMapper.deleteById(id);
    }

    private void validateSupplierContractExists(Long id) {
        if (supplierContractMapper.selectById(id) == null) {
            throw exception(SUPPLIER_CONTRACT_NOT_EXISTS);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierContractDO getSupplierContract(Long id) {
        return supplierContractMapper.selectById(id);
    }

    @Override
    public List<ErpSupplierContractDO> getSupplierContractList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierContractMapper.selectListBySupplierId(supplierId);
    }

}
