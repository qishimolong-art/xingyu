package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
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
        ErpSupplierContractDO existing = validateSupplierContractExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getSupplierId(), updateReqVO.getSupplierId())) {
            throw exception(SUPPLIER_CONTRACT_NOT_EXISTS);
        }
        supplierContractMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierContractDO.class));
    }

    @Override
    public void deleteSupplierContract(Long id) {
        validateSupplierContractExists(id);
        supplierContractMapper.deleteById(id);
    }

    private ErpSupplierContractDO validateSupplierContractExists(Long id) {
        ErpSupplierContractDO existing = supplierContractMapper.selectById(id);
        if (existing == null) throw exception(SUPPLIER_CONTRACT_NOT_EXISTS);
        validateSupplierExists(existing.getSupplierId());
        return existing;
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierContractDO getSupplierContract(Long id) {
        return validateSupplierContractExists(id);
    }

    @Override
    public List<ErpSupplierContractDO> getSupplierContractList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierContractMapper.selectListBySupplierId(supplierId);
    }


    @Override
    public PageResult<ErpSupplierContractDO> getSupplierContractPage(ErpSupplierItemPageReqVO reqVO) {
        validateSupplierExists(reqVO.getSupplierId());
        reqVO.boundPageSize();
        return supplierContractMapper.selectPage(reqVO);
    }
}
