package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMemberMapper;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_USER_BOUND;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_USER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_USER_NOT_EXISTS;

/**
 * ERP customer mini-app member authorization ServiceImpl.
 */
@Service
@Validated
public class ErpCustomerMemberServiceImpl implements ErpCustomerMemberService {

    @Resource
    private ErpCustomerMemberMapper customerMemberMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private MemberUserApi memberUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomerMember(Long customerId, Long memberUserId, String remark) {
        ErpCustomerDO customer = customerService.validateCustomer(customerId);
        MemberUserRespDTO memberUser = validateMemberUser(memberUserId);
        validateDuplicateCustomerMember(customerId, memberUserId);
        validateMemberUserNotBound(memberUserId, null);

        ErpCustomerMemberDO customerMember = new ErpCustomerMemberDO()
                .setCustomerId(customer.getId())
                .setMemberUserId(memberUser.getId())
                .setMobile(memberUser.getMobile())
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setRemark(remark);
        customerMemberMapper.insert(customerMember);
        return customerMember.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerMemberStatus(Long id, Integer status) {
        ErpCustomerMemberDO customerMember = validateCustomerMemberExists(id);
        if (CommonStatusEnum.isEnable(status)) {
            customerService.validateCustomer(customerMember.getCustomerId());
            validateMemberUser(customerMember.getMemberUserId());
            validateMemberUserNotBound(customerMember.getMemberUserId(), id);
        }
        customerMemberMapper.updateById(new ErpCustomerMemberDO().setId(id).setStatus(status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerMember(Long id) {
        validateCustomerMemberExists(id);
        customerMemberMapper.deleteLogicById(id);
    }

    @Override
    public ErpCustomerMemberDO getCustomerMember(Long id) {
        return customerMemberMapper.selectById(id);
    }

    @Override
    public List<ErpCustomerMemberDO> getCustomerMemberListByCustomerId(Long customerId) {
        return customerMemberMapper.selectListByCustomerId(customerId);
    }

    @Override
    public List<ErpCustomerMemberDO> getCustomerMemberListByMemberUserId(Long memberUserId) {
        return customerMemberMapper.selectListByMemberUserId(memberUserId);
    }

    @Override
    public ErpCustomerMemberDO getEnabledCustomerMemberByMemberUserId(Long memberUserId) {
        return customerMemberMapper.selectEnabledByMemberUserId(memberUserId);
    }

    @Override
    public ErpCustomerMemberDO validateEnabledCustomerMember(Long memberUserId) {
        ErpCustomerMemberDO customerMember = getEnabledCustomerMemberByMemberUserId(memberUserId);
        if (customerMember == null) {
            throw exception(CUSTOMER_MEMBER_NOT_EXISTS);
        }
        return customerMember;
    }

    private ErpCustomerMemberDO validateCustomerMemberExists(Long id) {
        ErpCustomerMemberDO customerMember = customerMemberMapper.selectById(id);
        if (customerMember == null) {
            throw exception(CUSTOMER_MEMBER_NOT_EXISTS);
        }
        return customerMember;
    }

    private MemberUserRespDTO validateMemberUser(Long memberUserId) {
        MemberUserRespDTO memberUser = memberUserApi.getUser(memberUserId);
        if (memberUser == null) {
            throw exception(CUSTOMER_MEMBER_USER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(memberUser.getStatus())) {
            throw exception(CUSTOMER_MEMBER_USER_NOT_ENABLE);
        }
        return memberUser;
    }

    private void validateDuplicateCustomerMember(Long customerId, Long memberUserId) {
        if (customerMemberMapper.selectByCustomerIdAndMemberUserId(customerId, memberUserId) != null) {
            throw exception(CUSTOMER_MEMBER_DUPLICATE);
        }
    }

    private void validateMemberUserNotBound(Long memberUserId, Long selfId) {
        ErpCustomerMemberDO enabledCustomerMember = customerMemberMapper.selectEnabledByMemberUserId(memberUserId);
        if (enabledCustomerMember == null || Objects.equals(enabledCustomerMember.getId(), selfId)) {
            return;
        }
        ErpCustomerDO boundCustomer = customerService.getCustomer(enabledCustomerMember.getCustomerId());
        String boundCustomerName = boundCustomer == null ? String.valueOf(enabledCustomerMember.getCustomerId()) : boundCustomer.getName();
        throw exception(CUSTOMER_MEMBER_USER_BOUND, boundCustomerName);
    }

}
