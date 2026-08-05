package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionCandidateColumnRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionCandidateTableRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionConfigSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionPageReqVO;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionConfigService;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionRebuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 表单数据权限配置")
@RestController
@RequestMapping("/system/form-data-permission")
@Validated
public class FormPermissionConfigController {

    @Resource
    private FormPermissionConfigService formPermissionConfigService;
    @Resource
    private FormPermissionRebuildService formPermissionRebuildService;

    @GetMapping("/page")
    @Operation(summary = "获得表单数据权限配置分页")
    @PreAuthorize("@ss.hasPermission('system:form-data-permission:query')")
    public CommonResult<PageResult<FormPermissionConfigRespVO>> getConfigPage(@Valid FormPermissionPageReqVO pageReqVO) {
        return success(formPermissionConfigService.getConfigPage(pageReqVO));
    }

    @GetMapping("/candidate-tables")
    @Operation(summary = "获得候选业务表")
    @PreAuthorize("@ss.hasPermission('system:form-data-permission:query')")
    public CommonResult<List<FormPermissionCandidateTableRespVO>> getCandidateTables() {
        return success(formPermissionConfigService.getCandidateTables());
    }

    @GetMapping("/candidate-columns")
    @Operation(summary = "获得候选业务表字段")
    @Parameter(name = "tableName", description = "表名", required = true)
    @PreAuthorize("@ss.hasPermission('system:form-data-permission:query')")
    public CommonResult<List<FormPermissionCandidateColumnRespVO>> getCandidateColumns(
            @RequestParam("tableName") String tableName) {
        return success(formPermissionConfigService.getCandidateColumns(tableName));
    }

    @GetMapping("/config/{formType}")
    @Operation(summary = "获得指定表配置")
    @PreAuthorize("@ss.hasPermission('system:form-data-permission:query')")
    public CommonResult<FormPermissionConfigRespVO> getConfig(@PathVariable("formType") String formType) {
        return success(formPermissionConfigService.getConfig(formType));
    }

    @PostMapping("/config")
    @Operation(summary = "保存指定表配置")
    @PreAuthorize("@ss.hasPermission('system:form-data-permission:config')")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody FormPermissionConfigSaveReqVO reqVO) {
        formPermissionConfigService.saveConfig(reqVO);
        return success(true);
    }

    @PostMapping("/rebuild/{formType}")
    @Operation(summary = "重建指定表的存量权限关系")
    @PreAuthorize("@ss.hasPermission('system:form-data-permission:rebuild')")
    public CommonResult<Boolean> rebuild(@PathVariable("formType") String formType) {
        formPermissionRebuildService.rebuild(formType);
        return success(true);
    }

}
