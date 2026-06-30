package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigCreateCustomReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

@Tag(name = "管理后台 - ERP 字段配置")
@RestController
@RequestMapping("/erp/field-config")
@Validated
public class ErpFieldConfigController {

    @Resource
    private ErpFieldConfigService fieldConfigService;

    @GetMapping("/list")
    @Operation(summary = "获得某模块的字段配置列表")
    @Parameter(name = "moduleKey", description = "模块标识", required = true, example = "purchase_order")
    @PreAuthorize("@ss.hasPermission('erp:field-config:query')")
    public CommonResult<List<ErpFieldConfigRespVO>> getFieldConfigList(@RequestParam("moduleKey") String moduleKey) {
        List<ErpFieldConfigDO> list = fieldConfigService.getFieldConfigListByModule(moduleKey);
        return success(BeanUtils.toBean(list, ErpFieldConfigRespVO.class));
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量保存字段配置", description = "全量覆盖：先删除该模块所有配置，再整批插入")
    @PreAuthorize("@ss.hasPermission('erp:field-config:update')")
    public CommonResult<Boolean> batchUpdateFieldConfig(@Valid @RequestBody ErpFieldConfigBatchUpdateReqVO reqVO) {
        fieldConfigService.batchUpdate(reqVO);
        return success(true);
    }

    @PostMapping("/create-custom-field")
    @Operation(summary = "新增自定义字段")
    @PreAuthorize("@ss.hasPermission('erp:field-config:create-custom-field')")
    public CommonResult<ErpFieldConfigRespVO> createCustomField(
            @Valid @RequestBody ErpFieldConfigCreateCustomReqVO reqVO) {
        ErpFieldConfigDO config = fieldConfigService.createCustomField(reqVO);
        return success(BeanUtils.toBean(config, ErpFieldConfigRespVO.class));
    }

    @PutMapping("/reset")
    @Operation(summary = "清空某模块的自定义配置", description = "清空后前端将按代码默认规则渲染")
    @Parameter(name = "moduleKey", description = "模块标识", required = true, example = "purchase_order")
    @PreAuthorize("@ss.hasPermission('erp:field-config:update')")
    public CommonResult<Boolean> resetFieldConfig(@RequestParam("moduleKey") String moduleKey) {
        fieldConfigService.resetFieldConfig(moduleKey);
        return success(true);
    }

    @GetMapping("/modules")
    @Operation(summary = "获得所有可配置模块")
    @PreAuthorize("@ss.hasPermission('erp:field-config:query')")
    public CommonResult<List<Map<String, String>>> listAllModules() {
        List<Map<String, String>> result = fieldConfigService.listAllModules().stream()
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
