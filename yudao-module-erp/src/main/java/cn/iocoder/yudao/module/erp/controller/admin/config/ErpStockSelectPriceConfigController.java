package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigUpdateReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpStockSelectPriceConfigService;
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
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存选择价格名称配置")
@RestController
@RequestMapping("/erp/stock-select-price-config")
@Validated
public class ErpStockSelectPriceConfigController {

    @Resource
    private ErpStockSelectPriceConfigService configService;

    @GetMapping("/get")
    @Operation(summary = "查询库存选择价格名称配置")
    @PreAuthorize("@ss.hasPermission('erp:stock-select-price-config:query')")
    public CommonResult<ErpStockSelectPriceConfigRespVO> getConfig() {
        return success(configService.getConfig());
    }

    @PutMapping("/update")
    @Operation(summary = "更新库存选择价格名称配置")
    @PreAuthorize("@ss.hasPermission('erp:stock-select-price-config:update')")
    public CommonResult<Boolean> updateConfig(
            @Valid @RequestBody ErpStockSelectPriceConfigUpdateReqVO reqVO) {
        configService.updateConfig(reqVO);
        return success(true);
    }

    @GetMapping("/effective-fields")
    @Operation(summary = "查询当前用户在库存选择场景最终可见的价格字段")
    @Parameter(name = "bizType", description = "sale、purchase 或 stock", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<List<String>> getEffectiveFields(
            @RequestParam("bizType") String bizType,
            @RequestParam(value = "businessDeptId", required = false) Long businessDeptId) {
        return success(configService.getEffectiveVisibleFields(bizType, businessDeptId));
    }

}
