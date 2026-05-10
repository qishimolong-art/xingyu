package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContactDO;

import java.util.List;

public interface ErpCustomerContactService {

    Long createContact(ErpCustomerContactSaveReqVO createReqVO);
    void updateContact(ErpCustomerContactSaveReqVO updateReqVO);
    void deleteContact(Long id);
    ErpCustomerContactDO getContact(Long id);
    PageResult<ErpCustomerContactDO> getContactPage(ErpCustomerContactPageReqVO pageReqVO);
    List<ErpCustomerContactDO> getContactListByCustomerId(Long customerId);

}
