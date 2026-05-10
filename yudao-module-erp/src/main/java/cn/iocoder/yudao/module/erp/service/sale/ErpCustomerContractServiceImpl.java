package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContractDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerContractMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CONTRACT_NOT_EXISTS;

@Service
@Validated
public class ErpCustomerContractServiceImpl implements ErpCustomerContractService {

    @Resource
    private ErpCustomerContractMapper contractMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public Long createContract(ErpCustomerContractSaveReqVO createReqVO) {
        customerService.validateCustomer(createReqVO.getCustomerId());
        ErpCustomerContractDO contract = BeanUtils.toBean(createReqVO, ErpCustomerContractDO.class);
        contractMapper.insert(contract);
        return contract.getId();
    }

    @Override
    public void updateContract(ErpCustomerContractSaveReqVO updateReqVO) {
        validateContractExists(updateReqVO.getId());
        customerService.validateCustomer(updateReqVO.getCustomerId());
        contractMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerContractDO.class));
    }

    @Override
    public void deleteContract(Long id) {
        validateContractExists(id);
        contractMapper.deleteById(id);
    }

    private void validateContractExists(Long id) {
        if (contractMapper.selectById(id) == null) {
            throw exception(CUSTOMER_CONTRACT_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerContractDO getContract(Long id) {
        return contractMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCustomerContractDO> getContractPage(ErpCustomerContractPageReqVO pageReqVO) {
        return contractMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerContractDO> getContractListByCustomerId(Long customerId) {
        customerService.validateCustomer(customerId);
        return contractMapper.selectListByCustomerId(customerId);
    }

}
