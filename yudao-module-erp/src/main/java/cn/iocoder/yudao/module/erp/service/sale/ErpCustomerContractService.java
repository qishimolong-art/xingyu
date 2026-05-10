package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContractDO;

import java.util.List;

public interface ErpCustomerContractService {

    Long createContract(ErpCustomerContractSaveReqVO createReqVO);
    void updateContract(ErpCustomerContractSaveReqVO updateReqVO);
    void deleteContract(Long id);
    ErpCustomerContractDO getContract(Long id);
    PageResult<ErpCustomerContractDO> getContractPage(ErpCustomerContractPageReqVO pageReqVO);
    List<ErpCustomerContractDO> getContractListByCustomerId(Long customerId);

}
