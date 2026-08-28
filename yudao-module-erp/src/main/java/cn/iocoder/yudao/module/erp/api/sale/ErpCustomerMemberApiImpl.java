package cn.iocoder.yudao.module.erp.api.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerMemberService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_AUTH_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_DEPT_REQUIRED;

/**
 * ERP customer mini-app member authorization API implementation.
 */
@Service
@Validated
public class ErpCustomerMemberApiImpl implements ErpCustomerMemberApi {

    @Resource
    private ErpCustomerMemberService customerMemberService;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public ErpCustomerMemberAuthRespDTO getCustomerMemberAuth(Long memberUserId) {
        if (memberUserId == null) {
            return ErpCustomerMemberAuthRespDTO.unauthorized();
        }
        ErpCustomerMemberDO customerMember = customerMemberService.getEnabledCustomerMemberByMemberUserId(memberUserId);
        if (customerMember == null) {
            return ErpCustomerMemberAuthRespDTO.unauthorized().setMemberUserId(memberUserId);
        }
        if (!isCustomerAvailable(customerMember.getCustomerId())) {
            return ErpCustomerMemberAuthRespDTO.unauthorized().setMemberUserId(memberUserId);
        }
        ErpCustomerDO customer = customerService.getCustomer(customerMember.getCustomerId());
        return new ErpCustomerMemberAuthRespDTO()
                .setAuthorized(true)
                .setId(customerMember.getId())
                .setCustomerId(customerMember.getCustomerId())
                .setCustomerName(customer == null ? null : customer.getName())
                .setMemberUserId(customerMember.getMemberUserId())
                .setMobile(customerMember.getMobile())
                .setPriceVisible(true)
                .setOrderEnabled(true);
    }

    @Override
    public ErpCustomerMemberAuthRespDTO validateCustomerMemberAuth(Long memberUserId) {
        ErpCustomerMemberAuthRespDTO auth = getCustomerMemberAuth(memberUserId);
        if (!Boolean.TRUE.equals(auth.getAuthorized())) {
            throw exception(CUSTOMER_MEMBER_AUTH_REQUIRED);
        }
        return auth;
    }

    @Override
    public ErpCustomerMemberAuthRespDTO validateCustomerMemberAuth(Long memberUserId, Long deptId) {
        ErpCustomerMemberAuthRespDTO auth = validateCustomerMemberAuth(memberUserId);
        if (deptId == null) {
            throw exception(CUSTOMER_MEMBER_DEPT_REQUIRED);
        }
        if (!customerService.getCustomerSaleDeptIdsIgnoreDataPermission(auth.getCustomerId()).contains(deptId)) {
            throw exception(CUSTOMER_MEMBER_DEPT_NOT_ALLOWED);
        }
        return auth;
    }

    @Override
    public boolean isCustomerMemberAuthorized(Long memberUserId) {
        return Boolean.TRUE.equals(getCustomerMemberAuth(memberUserId).getAuthorized());
    }

    private boolean isCustomerAvailable(Long customerId) {
        try {
            customerService.getCustomerSaleDeptIdsIgnoreDataPermission(customerId);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }

}
