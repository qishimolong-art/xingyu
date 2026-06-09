package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleConfigService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 销售配置")
@RestController
@RequestMapping("/erp/sale-config")
@Validated
public class ErpSaleConfigController {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_config";

    @Resource
    private ErpSaleConfigService saleConfigService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售配置")
    @PreAuthorize("@ss.hasPermission('erp:sale-config:create')")
    public CommonResult<Long> createSaleConfig(@Valid @RequestBody ErpSaleConfigSaveReqVO createReqVO) {
        return success(saleConfigService.createSaleConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售配置")
    @PreAuthorize("@ss.hasPermission('erp:sale-config:update')")
    public CommonResult<Boolean> updateSaleConfig(@Valid @RequestBody ErpSaleConfigSaveReqVO updateReqVO) {
        saleConfigService.updateSaleConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售配置")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-config:delete')")
    public CommonResult<Boolean> deleteSaleConfig(@RequestParam("ids") List<Long> ids) {
        saleConfigService.deleteSaleConfig(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售配置")
    @PreAuthorize("@ss.hasPermission('erp:sale-config:query')")
    public CommonResult<ErpSaleConfigRespVO> getSaleConfig(@RequestParam("id") Long id) {
        ErpSaleConfigRespVO respVO = BeanUtils.toBean(saleConfigService.getSaleConfig(id), ErpSaleConfigRespVO.class);
        fillDeptName(respVO);
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售配置分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-config:query')")
    public CommonResult<PageResult<ErpSaleConfigRespVO>> getSaleConfigPage(@Valid ErpSaleConfigPageReqVO pageReqVO) {
        PageResult<ErpSaleConfigRespVO> respResult = BeanUtils.toBean(saleConfigService.getSaleConfigPage(pageReqVO),
                ErpSaleConfigRespVO.class);
        fillDeptNames(respResult.getList());
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得销售配置精简列表")
    @PreAuthorize("@ss.hasPermission('erp:sale-config:query')")
    public CommonResult<List<ErpSaleConfigDO>> getSaleConfigSimpleList(@RequestParam("configType") String configType,
                                                                        @RequestParam(value = "status", required = false) Integer status) {
        List<ErpSaleConfigDO> list = saleConfigService.getSaleConfigSimpleList(configType, status);
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, list);
        return success(list);
    }

    private void fillDeptName(ErpSaleConfigRespVO respVO) {
        if (respVO == null || respVO.getDeptId() == null) {
            return;
        }
        DeptRespDTO dept = deptApi.getDept(respVO.getDeptId());
        if (dept != null) {
            respVO.setDeptName(dept.getName());
        }
    }

    private void fillDeptNames(List<ErpSaleConfigRespVO> list) {
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(list, ErpSaleConfigRespVO::getDeptId));
        for (ErpSaleConfigRespVO respVO : list) {
            MapUtils.findAndThen(deptMap, respVO.getDeptId(), dept -> respVO.setDeptName(dept.getName()));
        }
    }

}
