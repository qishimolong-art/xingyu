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
        ErpCustomerContactDO existing = validateContactExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getCustomerId(), updateReqVO.getCustomerId())) {
            throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        }
        customerService.validateCustomer(updateReqVO.getCustomerId());
        contactMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerContactDO.class));
    }

    @Override
    public void deleteContact(Long id) {
        validateContactExists(id);
        contactMapper.deleteById(id);
    }

    private ErpCustomerContactDO validateContactExists(Long id) {
        ErpCustomerContactDO existing = contactMapper.selectById(id);
        if (existing == null) throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    private void validateVisibleCustomer(Long customerId) {
        if (customerId == null || customerService.getCustomer(customerId) == null) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerContactDO getContact(Long id) {
        ErpCustomerContactDO existing = contactMapper.selectById(id);
        if (existing != null) validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    @Override
    public PageResult<ErpCustomerContactDO> getContactPage(ErpCustomerContactPageReqVO pageReqVO) {
        validateVisibleCustomer(pageReqVO.getCustomerId());
        if (pageReqVO.getPageSize() <= 0 || pageReqVO.getPageSize() > 100) pageReqVO.setPageSize(100);
        return contactMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerContactDO> getContactListByCustomerId(Long customerId) {
        validateVisibleCustomer(customerId);
        return contactMapper.selectListByCustomerId(customerId);
    }

}
