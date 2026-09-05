package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember.ErpCustomerMemberCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember.ErpCustomerMemberRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember.ErpCustomerMemberUpdateStatusReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerMemberService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpCustomerMemberControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerMemberController controller;

    @Mock
    private ErpCustomerMemberService customerMemberService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private MemberUserApi memberUserApi;

    @Test
    public void testCreateCustomerMember_paramPassThrough() {
        ErpCustomerMemberCreateReqVO reqVO = new ErpCustomerMemberCreateReqVO();
        reqVO.setCustomerId(10L);
        reqVO.setMemberUserId(20L);
        reqVO.setRemark("测试授权");
        when(customerMemberService.createCustomerMember(eq(10L), eq(20L), eq("测试授权"))).thenReturn(99L);

        CommonResult<Long> result = controller.createCustomerMember(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(customerMemberService).createCustomerMember(eq(10L), eq(20L), eq("测试授权"));
    }

    @Test
    public void testCreateCustomerMember_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerMemberController.class.getMethod(
                "createCustomerMember", ErpCustomerMemberCreateReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer-member:create"));
    }

    @Test
    public void testUpdateCustomerMemberStatus_paramPassThrough() {
        ErpCustomerMemberUpdateStatusReqVO reqVO = new ErpCustomerMemberUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(0);

        CommonResult<Boolean> result = controller.updateCustomerMemberStatus(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerMemberService).updateCustomerMemberStatus(eq(1L), eq(0));
    }

    @Test
    public void testUpdateCustomerMemberStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerMemberController.class.getMethod(
                "updateCustomerMemberStatus", ErpCustomerMemberUpdateStatusReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer-member:update"));
    }

    @Test
    public void testDeleteCustomerMember_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteCustomerMember(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerMemberService).deleteCustomerMember(eq(1L));
    }

    @Test
    public void testDeleteCustomerMember_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerMemberController.class.getMethod("deleteCustomerMember", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer-member:delete"));
    }

    @Test
    public void testGetCustomerMemberListByCustomer_fillNames() {
        ErpCustomerMemberDO customerMember = new ErpCustomerMemberDO();
        customerMember.setId(1L);
        customerMember.setCustomerId(10L);
        customerMember.setMemberUserId(20L);
        customerMember.setMobile("15601691300");
        customerMember.setStatus(0);
        when(customerMemberService.getCustomerMemberListByCustomerId(eq(10L)))
                .thenReturn(Collections.singletonList(customerMember));
        when(customerService.getCustomerMap(eq(Collections.singleton(10L))))
                .thenReturn(Collections.singletonMap(10L, new ErpCustomerDO().setId(10L).setName("测试客户")));
        MemberUserRespDTO memberUser = new MemberUserRespDTO();
        memberUser.setId(20L);
        memberUser.setNickname("小程序会员");
        when(memberUserApi.getUserMap(eq(Collections.singleton(20L)))).thenReturn(Collections.singletonMap(20L, memberUser));

        CommonResult<List<ErpCustomerMemberRespVO>> result = controller.getCustomerMemberListByCustomer(10L);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("测试客户", result.getData().get(0).getCustomerName());
        assertEquals("小程序会员", result.getData().get(0).getMemberNickname());
        assertEquals("15601691300", result.getData().get(0).getMobile());
    }

    @Test
    public void testGetCustomerMemberListByCustomer_fillMultipleMembers() {
        ErpCustomerMemberDO first = new ErpCustomerMemberDO()
                .setId(1L)
                .setCustomerId(10L)
                .setMemberUserId(20L)
                .setMobile("15601691300")
                .setStatus(0);
        ErpCustomerMemberDO second = new ErpCustomerMemberDO()
                .setId(2L)
                .setCustomerId(10L)
                .setMemberUserId(21L)
                .setMobile("15601691301")
                .setStatus(0);
        when(customerMemberService.getCustomerMemberListByCustomerId(eq(10L)))
                .thenReturn(Arrays.asList(first, second));
        when(customerService.getCustomerMap(eq(Collections.singleton(10L))))
                .thenReturn(Collections.singletonMap(10L, new ErpCustomerDO().setId(10L).setName("测试客户")));
        Map<Long, MemberUserRespDTO> memberUserMap = new HashMap<>();
        memberUserMap.put(20L, new MemberUserRespDTO().setId(20L).setNickname("微信用户A"));
        memberUserMap.put(21L, new MemberUserRespDTO().setId(21L).setNickname("微信用户B"));
        when(memberUserApi.getUserMap(eq(new HashSet<>(Arrays.asList(20L, 21L)))))
                .thenReturn(memberUserMap);

        CommonResult<List<ErpCustomerMemberRespVO>> result = controller.getCustomerMemberListByCustomer(10L);

        assertEquals(0, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals("测试客户", result.getData().get(0).getCustomerName());
        assertEquals("微信用户A", result.getData().get(0).getMemberNickname());
        assertEquals("微信用户B", result.getData().get(1).getMemberNickname());
    }

    @Test
    public void testGetCustomerMemberListByCustomer_emptyListSkipFill() {
        when(customerMemberService.getCustomerMemberListByCustomerId(eq(10L))).thenReturn(Collections.emptyList());

        CommonResult<List<ErpCustomerMemberRespVO>> result = controller.getCustomerMemberListByCustomer(10L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData().isEmpty());
        verify(customerService, never()).getCustomerMap(eq(Collections.emptySet()));
        verify(memberUserApi, never()).getUserMap(eq(Collections.emptySet()));
    }

    @Test
    public void testGetCustomerMemberListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerMemberController.class.getMethod("getCustomerMemberListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer-member:query"));
    }

    @Test
    public void testGetCustomerMemberListByMember_fillNames() {
        ErpCustomerMemberDO customerMember = new ErpCustomerMemberDO();
        customerMember.setId(1L);
        customerMember.setCustomerId(10L);
        customerMember.setMemberUserId(20L);
        customerMember.setMobile("15601691300");
        customerMember.setStatus(0);
        when(customerMemberService.getCustomerMemberListByMemberUserId(eq(20L)))
                .thenReturn(Collections.singletonList(customerMember));
        when(customerService.getCustomerMap(eq(Collections.singleton(10L))))
                .thenReturn(Collections.singletonMap(10L, new ErpCustomerDO().setId(10L).setName("测试客户")));
        MemberUserRespDTO memberUser = new MemberUserRespDTO();
        memberUser.setId(20L);
        memberUser.setNickname("小程序会员");
        when(memberUserApi.getUserMap(eq(Collections.singleton(20L)))).thenReturn(Collections.singletonMap(20L, memberUser));

        CommonResult<List<ErpCustomerMemberRespVO>> result = controller.getCustomerMemberListByMember(20L);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("测试客户", result.getData().get(0).getCustomerName());
        assertEquals("小程序会员", result.getData().get(0).getMemberNickname());
        assertEquals("15601691300", result.getData().get(0).getMobile());
    }

    @Test
    public void testGetCustomerMemberListByMember_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerMemberController.class.getMethod("getCustomerMemberListByMember", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer-member:query"));
    }

}
