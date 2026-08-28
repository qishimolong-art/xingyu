package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember.ErpCustomerMemberCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember.ErpCustomerMemberRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember.ErpCustomerMemberUpdateStatusReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerMemberService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 客户小程序授权")
@RestController
@RequestMapping("/erp/customer-member")
@Validated
public class ErpCustomerMemberController {

    @Resource
    private ErpCustomerMemberService customerMemberService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private MemberUserApi memberUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建客户小程序授权")
    @PreAuthorize("@ss.hasPermission('erp:customer-member:create')")
    public CommonResult<Long> createCustomerMember(@Valid @RequestBody ErpCustomerMemberCreateReqVO reqVO) {
        return success(customerMemberService.createCustomerMember(
                reqVO.getCustomerId(), reqVO.getMemberUserId(), reqVO.getRemark()));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新客户小程序授权状态")
    @PreAuthorize("@ss.hasPermission('erp:customer-member:update')")
    public CommonResult<Boolean> updateCustomerMemberStatus(
            @Valid @RequestBody ErpCustomerMemberUpdateStatusReqVO reqVO) {
        customerMemberService.updateCustomerMemberStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户小程序授权")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer-member:delete')")
    public CommonResult<Boolean> deleteCustomerMember(@RequestParam("id") Long id) {
        customerMemberService.deleteCustomerMember(id);
        return success(true);
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户小程序授权列表")
    @Parameter(name = "customerId", description = "客户编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer-member:query')")
    public CommonResult<List<ErpCustomerMemberRespVO>> getCustomerMemberListByCustomer(
            @RequestParam("customerId") Long customerId) {
        List<ErpCustomerMemberDO> list = customerMemberService.getCustomerMemberListByCustomerId(customerId);
        List<ErpCustomerMemberRespVO> respList = BeanUtils.toBean(list, ErpCustomerMemberRespVO.class);
        fillCustomerMemberNames(list, respList);
        return success(respList);
    }

    @GetMapping("/list-by-member")
    @Operation(summary = "获得会员小程序授权列表")
    @Parameter(name = "memberUserId", description = "会员编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer-member:query')")
    public CommonResult<List<ErpCustomerMemberRespVO>> getCustomerMemberListByMember(
            @RequestParam("memberUserId") Long memberUserId) {
        List<ErpCustomerMemberDO> list = customerMemberService.getCustomerMemberListByMemberUserId(memberUserId);
        List<ErpCustomerMemberRespVO> respList = BeanUtils.toBean(list, ErpCustomerMemberRespVO.class);
        fillCustomerMemberNames(list, respList);
        return success(respList);
    }

    private void fillCustomerMemberNames(List<ErpCustomerMemberDO> list, List<ErpCustomerMemberRespVO> respList) {
        if (CollUtil.isEmpty(respList)) {
            return;
        }
        Set<Long> customerIds = convertSet(list, ErpCustomerMemberDO::getCustomerId);
        Map<Long, ErpCustomerDO> customerMap = CollUtil.isEmpty(customerIds)
                ? Collections.emptyMap() : customerService.getCustomerMap(customerIds);
        Set<Long> memberUserIds = convertSet(list, ErpCustomerMemberDO::getMemberUserId);
        Map<Long, MemberUserRespDTO> memberUserMap = CollUtil.isEmpty(memberUserIds)
                ? Collections.emptyMap() : memberUserApi.getUserMap(memberUserIds);
        respList.forEach(item -> {
            ErpCustomerDO customer = customerMap.get(item.getCustomerId());
            if (customer != null) {
                item.setCustomerName(customer.getName());
            }
            MemberUserRespDTO memberUser = memberUserMap.get(item.getMemberUserId());
            if (memberUser != null) {
                item.setMemberNickname(memberUser.getNickname());
            }
        });
    }

}
