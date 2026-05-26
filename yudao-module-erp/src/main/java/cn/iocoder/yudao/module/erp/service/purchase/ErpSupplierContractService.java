package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontract.ErpSupplierContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContractDO;

import java.util.List;

public interface ErpSupplierContractService {

    Long createSupplierContract(ErpSupplierContractSaveReqVO createReqVO);
    void updateSupplierContract(ErpSupplierContractSaveReqVO updateReqVO);
    void deleteSupplierContract(Long id);
    ErpSupplierContractDO getSupplierContract(Long id);
    List<ErpSupplierContractDO> getSupplierContractList(Long supplierId);

}
