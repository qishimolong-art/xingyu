package cn.iocoder.yudao.module.erp.api.sale;

import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;

/**
 * ERP customer mini-app member authorization API.
 */
public interface ErpCustomerMemberApi {

    ErpCustomerMemberAuthRespDTO getCustomerMemberAuth(Long memberUserId);

    ErpCustomerMemberAuthRespDTO validateCustomerMemberAuth(Long memberUserId);

    ErpCustomerMemberAuthRespDTO validateCustomerMemberAuth(Long memberUserId, Long deptId);

    boolean isCustomerMemberAuthorized(Long memberUserId);

}
