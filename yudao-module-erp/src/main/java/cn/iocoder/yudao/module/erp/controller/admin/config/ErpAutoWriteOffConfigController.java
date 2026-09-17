package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigUpdateReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpAutoWriteOffConfigService;
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

@Tag(name = "管理后台 - ERP 自动核销配置")
@RestController
@RequestMapping("/erp/auto-write-off-config")
@Validated
public class ErpAutoWriteOffConfigController {

    @Resource
    private ErpAutoWriteOffConfigService configService;

    @GetMapping("/get")
    @Operation(summary = "查询自动核销配置")
    @PreAuthorize("@ss.hasAnyPermissions('erp:auto-write-off-config:query', "
            + "'erp:finance-payment:query', 'erp:finance-receipt:query')")
    public CommonResult<ErpAutoWriteOffConfigRespVO> getConfig() {
        return success(configService.getConfig());
    }

    @PutMapping("/update")
    @Operation(summary = "更新自动核销配置")
    @PreAuthorize("@ss.hasPermission('erp:auto-write-off-config:update')")
    public CommonResult<Boolean> updateConfig(
            @Valid @RequestBody ErpAutoWriteOffConfigUpdateReqVO reqVO) {
        configService.updateConfig(reqVO);
        return success(true);
    }

}
