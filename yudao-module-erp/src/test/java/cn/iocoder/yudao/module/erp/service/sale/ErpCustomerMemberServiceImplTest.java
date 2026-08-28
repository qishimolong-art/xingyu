package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMemberMapper;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_USER_BOUND;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_USER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_USER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerMemberServiceImpl} Mockito unit tests.
 */
public class ErpCustomerMemberServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerMemberServiceImpl customerMemberService;

    @Mock
    private ErpCustomerMemberMapper customerMemberMapper;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private MemberUserApi memberUserApi;

    @Test
    void createCustomerMember_success() {
        Long customerId = 10L;
        Long memberUserId = 20L;
        ErpCustomerDO customer = buildCustomer(customerId, "星雨客户");
        MemberUserRespDTO memberUser = buildMemberUser(memberUserId, "13800000000", CommonStatusEnum.ENABLE.getStatus());
        when(customerService.validateCustomer(customerId)).thenReturn(customer);
        when(memberUserApi.getUser(memberUserId)).thenReturn(memberUser);
        when(customerMemberMapper.insert(any(ErpCustomerMemberDO.class))).thenAnswer(invocation -> {
            ErpCustomerMemberDO customerMember = invocation.getArgument(0);
            customerMember.setId(100L);
            return 1;
        });

        Long id = customerMemberService.createCustomerMember(customerId, memberUserId, "首个账号");

        assertEquals(100L, id);
        ArgumentCaptor<ErpCustomerMemberDO> captor = ArgumentCaptor.forClass(ErpCustomerMemberDO.class);
        verify(customerMemberMapper).insert(captor.capture());
        ErpCustomerMemberDO saved = captor.getValue();
        assertEquals(customerId, saved.getCustomerId());
        assertEquals(memberUserId, saved.getMemberUserId());
        assertEquals("13800000000", saved.getMobile());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), saved.getStatus());
        assertEquals("首个账号", saved.getRemark());
    }

    @Test
    void createCustomerMember_customerDisabled() {
        Long customerId = 10L;
        Long memberUserId = 20L;
        when(customerService.validateCustomer(customerId)).thenThrow(exception(CUSTOMER_NOT_ENABLE, "停用客户"));

        assertServiceException(() -> customerMemberService.createCustomerMember(customerId, memberUserId, null),
                CUSTOMER_NOT_ENABLE, "停用客户");
        verify(memberUserApi, never()).getUser(any());
        verify(customerMemberMapper, never()).insert(any(ErpCustomerMemberDO.class));
    }

    @Test
    void createCustomerMember_memberNotExists() {
        Long customerId = 10L;
        Long memberUserId = 20L;
        when(customerService.validateCustomer(customerId)).thenReturn(buildCustomer(customerId, "星雨客户"));
        when(memberUserApi.getUser(memberUserId)).thenReturn(null);

        assertServiceException(() -> customerMemberService.createCustomerMember(customerId, memberUserId, null),
                CUSTOMER_MEMBER_USER_NOT_EXISTS);
        verify(customerMemberMapper, never()).insert(any(ErpCustomerMemberDO.class));
    }

    @Test
    void createCustomerMember_memberDisabled() {
        Long customerId = 10L;
        Long memberUserId = 20L;
        when(customerService.validateCustomer(customerId)).thenReturn(buildCustomer(customerId, "星雨客户"));
        when(memberUserApi.getUser(memberUserId)).thenReturn(
                buildMemberUser(memberUserId, "13800000000", CommonStatusEnum.DISABLE.getStatus()));

        assertServiceException(() -> customerMemberService.createCustomerMember(customerId, memberUserId, null),
                CUSTOMER_MEMBER_USER_NOT_ENABLE);
        verify(customerMemberMapper, never()).insert(any(ErpCustomerMemberDO.class));
    }

    @Test
    void createCustomerMember_duplicateCustomerMember() {
        Long customerId = 10L;
        Long memberUserId = 20L;
        when(customerService.validateCustomer(customerId)).thenReturn(buildCustomer(customerId, "星雨客户"));
        when(memberUserApi.getUser(memberUserId)).thenReturn(
                buildMemberUser(memberUserId, "13800000000", CommonStatusEnum.ENABLE.getStatus()));
        when(customerMemberMapper.selectByCustomerIdAndMemberUserId(customerId, memberUserId))
                .thenReturn(new ErpCustomerMemberDO().setId(99L));

        assertServiceException(() -> customerMemberService.createCustomerMember(customerId, memberUserId, null),
                CUSTOMER_MEMBER_DUPLICATE);
        verify(customerMemberMapper, never()).insert(any(ErpCustomerMemberDO.class));
    }

    @Test
    void createCustomerMember_memberBoundToOtherCustomer() {
        Long customerId = 10L;
        Long memberUserId = 20L;
        when(customerService.validateCustomer(customerId)).thenReturn(buildCustomer(customerId, "星雨客户"));
        when(memberUserApi.getUser(memberUserId)).thenReturn(
                buildMemberUser(memberUserId, "13800000000", CommonStatusEnum.ENABLE.getStatus()));
        when(customerMemberMapper.selectEnabledByMemberUserId(memberUserId)).thenReturn(new ErpCustomerMemberDO()
                .setId(99L).setCustomerId(30L).setMemberUserId(memberUserId).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(customerService.getCustomer(30L)).thenReturn(buildCustomer(30L, "已绑定客户"));

        assertServiceException(() -> customerMemberService.createCustomerMember(customerId, memberUserId, null),
                CUSTOMER_MEMBER_USER_BOUND, "已绑定客户");
        verify(customerMemberMapper, never()).insert(any(ErpCustomerMemberDO.class));
    }

    @Test
    void updateCustomerMemberStatus_disable() {
        Long id = 100L;
        when(customerMemberMapper.selectById(id)).thenReturn(new ErpCustomerMemberDO()
                .setId(id).setCustomerId(10L).setMemberUserId(20L).setStatus(CommonStatusEnum.ENABLE.getStatus()));

        customerMemberService.updateCustomerMemberStatus(id, CommonStatusEnum.DISABLE.getStatus());

        ArgumentCaptor<ErpCustomerMemberDO> captor = ArgumentCaptor.forClass(ErpCustomerMemberDO.class);
        verify(customerMemberMapper).updateById(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        verify(customerService, never()).validateCustomer(any());
        verify(memberUserApi, never()).getUser(any());
    }

    @Test
    void updateCustomerMemberStatus_enable_revalidates() {
        Long id = 100L;
        Long customerId = 10L;
        Long memberUserId = 20L;
        when(customerMemberMapper.selectById(id)).thenReturn(new ErpCustomerMemberDO()
                .setId(id).setCustomerId(customerId).setMemberUserId(memberUserId).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(customerService.validateCustomer(customerId)).thenReturn(buildCustomer(customerId, "星雨客户"));
        when(memberUserApi.getUser(memberUserId)).thenReturn(
                buildMemberUser(memberUserId, "13800000000", CommonStatusEnum.ENABLE.getStatus()));

        customerMemberService.updateCustomerMemberStatus(id, CommonStatusEnum.ENABLE.getStatus());

        verify(customerService).validateCustomer(customerId);
        verify(memberUserApi).getUser(memberUserId);
        verify(customerMemberMapper).selectEnabledByMemberUserId(memberUserId);
        verify(customerMemberMapper).updateById(any(ErpCustomerMemberDO.class));
    }

    @Test
    void updateCustomerMemberStatus_enable_rejectsOtherEnabledCustomer() {
        Long id = 100L;
        Long memberUserId = 20L;
        when(customerMemberMapper.selectById(id)).thenReturn(new ErpCustomerMemberDO()
                .setId(id).setCustomerId(10L).setMemberUserId(memberUserId).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(customerService.validateCustomer(10L)).thenReturn(buildCustomer(10L, "星雨客户"));
        when(memberUserApi.getUser(memberUserId)).thenReturn(
                buildMemberUser(memberUserId, "13800000000", CommonStatusEnum.ENABLE.getStatus()));
        when(customerMemberMapper.selectEnabledByMemberUserId(memberUserId)).thenReturn(new ErpCustomerMemberDO()
                .setId(101L).setCustomerId(30L).setMemberUserId(memberUserId).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(customerService.getCustomer(30L)).thenReturn(buildCustomer(30L, "其他客户"));

        assertServiceException(() -> customerMemberService.updateCustomerMemberStatus(id, CommonStatusEnum.ENABLE.getStatus()),
                CUSTOMER_MEMBER_USER_BOUND, "其他客户");
        verify(customerMemberMapper, never()).updateById(any(ErpCustomerMemberDO.class));
    }

    @Test
    void deleteCustomerMember_success() {
        Long id = 100L;
        when(customerMemberMapper.selectById(id)).thenReturn(new ErpCustomerMemberDO().setId(id));

        customerMemberService.deleteCustomerMember(id);

        verify(customerMemberMapper).deleteLogicById(eq(id));
        verify(memberUserApi, never()).getUser(any());
    }

    @Test
    void deleteCustomerMember_notExists() {
        Long id = 100L;
        when(customerMemberMapper.selectById(id)).thenReturn(null);

        assertServiceException(() -> customerMemberService.deleteCustomerMember(id), CUSTOMER_MEMBER_NOT_EXISTS);
        verify(customerMemberMapper, never()).deleteLogicById(any());
    }

    @Test
    void getEnabledCustomerMemberByMemberUserId_afterDisableReturnsNull() {
        Long memberUserId = 20L;
        when(customerMemberMapper.selectEnabledByMemberUserId(memberUserId)).thenReturn(null);

        ErpCustomerMemberDO result = customerMemberService.getEnabledCustomerMemberByMemberUserId(memberUserId);

        assertNull(result);
        verify(customerMemberMapper).selectEnabledByMemberUserId(memberUserId);
    }

    @Test
    void validateEnabledCustomerMember_success() {
        Long memberUserId = 20L;
        ErpCustomerMemberDO customerMember = new ErpCustomerMemberDO()
                .setId(100L).setMemberUserId(memberUserId).setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(customerMemberMapper.selectEnabledByMemberUserId(memberUserId)).thenReturn(customerMember);

        ErpCustomerMemberDO result = customerMemberService.validateEnabledCustomerMember(memberUserId);

        assertSame(customerMember, result);
    }

    @Test
    void validateEnabledCustomerMember_notExists() {
        Long memberUserId = 20L;
        when(customerMemberMapper.selectEnabledByMemberUserId(memberUserId)).thenReturn(null);

        assertServiceException(() -> customerMemberService.validateEnabledCustomerMember(memberUserId),
                CUSTOMER_MEMBER_NOT_EXISTS);
    }

    @Test
    void getCustomerMemberListByCustomerId_success() {
        Long customerId = 10L;
        List<ErpCustomerMemberDO> list = Collections.singletonList(new ErpCustomerMemberDO().setCustomerId(customerId));
        when(customerMemberMapper.selectListByCustomerId(customerId)).thenReturn(list);

        assertSame(list, customerMemberService.getCustomerMemberListByCustomerId(customerId));
    }

    @Test
    void getCustomerMemberListByMemberUserId_success() {
        Long memberUserId = 20L;
        List<ErpCustomerMemberDO> list = Collections.singletonList(new ErpCustomerMemberDO().setMemberUserId(memberUserId));
        when(customerMemberMapper.selectListByMemberUserId(memberUserId)).thenReturn(list);

        assertSame(list, customerMemberService.getCustomerMemberListByMemberUserId(memberUserId));
    }

    private static ErpCustomerDO buildCustomer(Long id, String name) {
        return new ErpCustomerDO()
                .setId(id)
                .setName(name)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private static MemberUserRespDTO buildMemberUser(Long id, String mobile, Integer status) {
        return new MemberUserRespDTO()
                .setId(id)
                .setMobile(mobile)
                .setStatus(status);
    }

}
