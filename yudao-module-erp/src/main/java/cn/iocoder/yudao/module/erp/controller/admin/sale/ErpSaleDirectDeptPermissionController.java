package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.deptpermission.ErpSaleDirectForbiddenDeptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.deptpermission.ErpSaleDirectForbiddenDeptSaveReqVO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleDirectDeptPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 销售开单部门权限")
@RestController
@RequestMapping("/erp/sale-dept-permission")
@Validated
public class ErpSaleDirectDeptPermissionController {

    @Resource
    private ErpSaleDirectDeptPermissionService saleDirectDeptPermissionService;

    @GetMapping("/forbidden-depts")
    @Operation(summary = "获得禁止直接做销售单据的部门")
    @PreAuthorize("@ss.hasPermission('erp:sale-dept-permission:query')")
    public CommonResult<List<ErpSaleDirectForbiddenDeptRespVO>> getForbiddenDeptList() {
        return success(saleDirectDeptPermissionService.getForbiddenDeptList());
    }

    @PutMapping("/forbidden-depts")
    @Operation(summary = "保存禁止直接做销售单据的部门")
    @PreAuthorize("@ss.hasPermission('erp:sale-dept-permission:update')")
    public CommonResult<Boolean> updateForbiddenDeptList(
            @Valid @RequestBody ErpSaleDirectForbiddenDeptSaveReqVO updateReqVO) {
        saleDirectDeptPermissionService.updateForbiddenDeptList(updateReqVO.getDeptIds());
        return success(true);
    }

}
