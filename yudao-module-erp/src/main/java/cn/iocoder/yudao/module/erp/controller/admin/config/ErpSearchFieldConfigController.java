package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpSearchFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpSearchFieldConfigRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpSearchFieldConfigDO;
import cn.iocoder.yudao.module.erp.service.config.ErpSearchFieldConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 搜索字段配置")
@RestController
@RequestMapping("/erp/search-field-config")
@Validated
public class ErpSearchFieldConfigController {

    @Resource
    private ErpSearchFieldConfigService searchFieldConfigService;

    @GetMapping("/list")
    @Operation(summary = "获得某模块的搜索字段配置列表")
    @Parameter(name = "moduleKey", description = "模块标识", required = true, example = "purchase_order")
    @PreAuthorize("@ss.hasPermission('erp:search-field-config:query')")
    public CommonResult<List<ErpSearchFieldConfigRespVO>> getSearchFieldConfigList(@RequestParam("moduleKey") String moduleKey) {
        List<ErpSearchFieldConfigDO> list = searchFieldConfigService.getSearchFieldConfigListByModule(moduleKey);
        return success(BeanUtils.toBean(list, ErpSearchFieldConfigRespVO.class));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得某模块已启用的搜索字段配置列表", description = "无配置时返回空列表，前端按默认搜索项降级")
    @Parameter(name = "moduleKey", description = "模块标识", required = true, example = "purchase_order")
    @PreAuthorize("@ss.hasPermission('erp:search-field-config:query')")
    public CommonResult<List<ErpSearchFieldConfigRespVO>> getEnabledSearchFieldConfigList(@RequestParam("moduleKey") String moduleKey) {
        List<ErpSearchFieldConfigDO> list = searchFieldConfigService.getEnabledSearchFieldConfigListByModule(moduleKey);
        return success(BeanUtils.toBean(list, ErpSearchFieldConfigRespVO.class));
    }

    @GetMapping("/runtime-enabled-list")
    @Operation(summary = "Get runtime ERP search field config list")
    @Parameter(name = "moduleKey", description = "Module key", required = true, example = "purchase_order")
    public CommonResult<List<ErpSearchFieldConfigRespVO>> getRuntimeEnabledSearchFieldConfigList(@RequestParam("moduleKey") String moduleKey) {
        List<ErpSearchFieldConfigDO> list = searchFieldConfigService.getEnabledSearchFieldConfigListByModule(moduleKey);
        return success(BeanUtils.toBean(list, ErpSearchFieldConfigRespVO.class));
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量保存搜索字段配置", description = "全量覆盖：先删除该模块所有配置，再整批插入")
    @PreAuthorize("@ss.hasPermission('erp:search-field-config:update')")
    public CommonResult<Boolean> batchUpdateSearchFieldConfig(@Valid @RequestBody ErpSearchFieldConfigBatchUpdateReqVO reqVO) {
        searchFieldConfigService.batchUpdate(reqVO);
        return success(true);
    }

    @PutMapping("/reset")
    @Operation(summary = "清空某模块的搜索字段配置", description = "清空后前端按默认搜索项降级")
    @Parameter(name = "moduleKey", description = "模块标识", required = true, example = "purchase_order")
    @PreAuthorize("@ss.hasPermission('erp:search-field-config:update')")
    public CommonResult<Boolean> resetSearchFieldConfig(@RequestParam("moduleKey") String moduleKey) {
        searchFieldConfigService.resetSearchFieldConfig(moduleKey);
        return success(true);
    }

    @GetMapping("/modules")
    @Operation(summary = "获得所有可配置模块")
    @PreAuthorize("@ss.hasPermission('erp:search-field-config:query')")
    public CommonResult<List<Map<String, String>>> listAllModules() {
        List<Map<String, String>> result = searchFieldConfigService.listAllModules().stream()
                .map(m -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("key", m.getKey());
                    map.put("name", m.getName());
                    return map;
                })
                .collect(Collectors.toList());
        return success(result);
    }

}
