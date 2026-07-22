package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldUpdateReqVO;
import cn.iocoder.yudao.module.system.service.permission.DeptPriceFieldService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 配件价格查看权限")
@RestController
@RequestMapping("/system/dept-price-field")
@Validated
public class DeptPriceFieldController {

    @Resource
    private DeptPriceFieldService deptPriceFieldService;

    @GetMapping("/get-config")
    @Operation(summary = "查询配件价格查看权限配置")
    @PreAuthorize("@ss.hasPermission('erp:product-price-permission:query')")
    public CommonResult<DeptPriceFieldConfigRespVO> getConfig() {
        return success(deptPriceFieldService.getConfig());
    }

    @PutMapping("/update-config")
    @Operation(summary = "更新配件价格查看权限配置")
    @PreAuthorize("@ss.hasPermission('erp:product-price-permission:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody DeptPriceFieldUpdateReqVO reqVO) {
        deptPriceFieldService.updateConfig(reqVO);
        return success(true);
    }

}
