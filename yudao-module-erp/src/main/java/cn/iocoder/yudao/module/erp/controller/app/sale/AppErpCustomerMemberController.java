package cn.iocoder.yudao.module.erp.controller.app.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.controller.app.sale.vo.customermember.AppErpCustomerMemberAuthRespVO;
import cn.iocoder.yudao.module.erp.controller.app.sale.vo.customermember.AppErpCustomerMemberDeptRespVO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - ERP 客户小程序授权")
@RestController
@RequestMapping("/erp/customer-member")
@Validated
public class AppErpCustomerMemberController {

    @Resource
    private ErpCustomerMemberApi customerMemberApi;
    @Resource
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

    @GetMapping("/auth-status")
    @Operation(summary = "获得当前会员 ERP 客户授权状态")
    public CommonResult<AppErpCustomerMemberAuthRespVO> getCustomerMemberAuthStatus() {
        return success(BeanUtils.toBean(customerMemberApi.getCustomerMemberAuth(getLoginUserId()),
                AppErpCustomerMemberAuthRespVO.class));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获得当前会员 ERP 客户可用部门列表")
    public CommonResult<List<AppErpCustomerMemberDeptRespVO>> getCustomerMemberDeptSimpleList() {
        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.getCustomerMemberAuth(getLoginUserId());
        if (!Boolean.TRUE.equals(auth.getAuthorized()) || auth.getCustomerId() == null) {
            return success(Collections.emptyList());
        }
        return success(BeanUtils.toBean(customerDeptPermissionService.getCustomerAppAvailableDeptList(
                auth.getCustomerId()), AppErpCustomerMemberDeptRespVO.class));
    }

}
