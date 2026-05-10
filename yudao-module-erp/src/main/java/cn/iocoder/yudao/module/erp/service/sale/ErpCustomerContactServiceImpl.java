package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContactDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerContactMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CONTACT_NOT_EXISTS;

@Service
@Validated
public class ErpCustomerContactServiceImpl implements ErpCustomerContactService {

    @Resource
    private ErpCustomerContactMapper contactMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public Long createContact(ErpCustomerContactSaveReqVO createReqVO) {
        customerService.validateCustomer(createReqVO.getCustomerId());
        ErpCustomerContactDO contact = BeanUtils.toBean(createReqVO, ErpCustomerContactDO.class);
        contactMapper.insert(contact);
        return contact.getId();
    }

    @Override
    public void updateContact(ErpCustomerContactSaveReqVO updateReqVO) {
        validateContactExists(updateReqVO.getId());
        customerService.validateCustomer(updateReqVO.getCustomerId());
        contactMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerContactDO.class));
    }

    @Override
    public void deleteContact(Long id) {
        validateContactExists(id);
        contactMapper.deleteById(id);
    }

    private void validateContactExists(Long id) {
        if (contactMapper.selectById(id) == null) {
            throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerContactDO getContact(Long id) {
        return contactMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCustomerContactDO> getContactPage(ErpCustomerContactPageReqVO pageReqVO) {
        return contactMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerContactDO> getContactListByCustomerId(Long customerId) {
        customerService.validateCustomer(customerId);
        return contactMapper.selectListByCustomerId(customerId);
    }

}
