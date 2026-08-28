package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;

import java.util.List;

/**
 * ERP customer mini-app member authorization Service.
 */
public interface ErpCustomerMemberService {

    Long createCustomerMember(Long customerId, Long memberUserId, String remark);

    void updateCustomerMemberStatus(Long id, Integer status);

    void deleteCustomerMember(Long id);

    ErpCustomerMemberDO getCustomerMember(Long id);

    List<ErpCustomerMemberDO> getCustomerMemberListByCustomerId(Long customerId);

    List<ErpCustomerMemberDO> getCustomerMemberListByMemberUserId(Long memberUserId);

    ErpCustomerMemberDO getEnabledCustomerMemberByMemberUserId(Long memberUserId);

    ErpCustomerMemberDO validateEnabledCustomerMember(Long memberUserId);

}
