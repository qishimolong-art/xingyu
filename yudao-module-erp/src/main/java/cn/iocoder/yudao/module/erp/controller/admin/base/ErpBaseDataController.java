package cn.iocoder.yudao.module.erp.controller.admin.base;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 基础数据")
@RestController
@RequestMapping("/erp/base-data")
@Validated
public class ErpBaseDataController {

    private static final String FIELD_PERMISSION_MODULE = "erp_base_data";

    @Resource
    private ErpBaseDataService baseDataService;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @PostMapping("/create")
    @Operation(summary = "创建基础数据")
    @PreAuthorize("@ss.hasPermission('erp:base-data:create')")
    public CommonResult<Long> createBaseData(@Valid @RequestBody ErpBaseDataSaveReqVO createReqVO) {
        return success(baseDataService.createBaseData(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新基础数据")
    @PreAuthorize("@ss.hasPermission('erp:base-data:update')")
    public CommonResult<Boolean> updateBaseData(@Valid @RequestBody ErpBaseDataSaveReqVO updateReqVO) {
        baseDataService.updateBaseData(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除基础数据")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:base-data:delete')")
    public CommonResult<Boolean> deleteBaseData(@RequestParam("id") Long id) {
        baseDataService.deleteBaseData(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得基础数据")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:base-data:query')")
    public CommonResult<ErpBaseDataRespVO> getBaseData(@RequestParam("id") Long id) {
        ErpBaseDataDO baseData = baseDataService.getBaseData(id);
        ErpBaseDataRespVO vo = BeanUtils.toBean(baseData, ErpBaseDataRespVO.class);
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, vo);
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获得基础数据分页")
    @PreAuthorize("@ss.hasPermission('erp:base-data:query')")
    public CommonResult<PageResult<ErpBaseDataRespVO>> getBaseDataPage(@Valid ErpBaseDataPageReqVO pageReqVO) {
        PageResult<ErpBaseDataDO> pageResult = baseDataService.getBaseDataPage(pageReqVO);
        PageResult<ErpBaseDataRespVO> result = BeanUtils.toBean(pageResult, ErpBaseDataRespVO.class);
        result.getList().forEach(item -> fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, item));
        return success(result);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得基础数据精简列表", description = "按类型获取启用的基础数据，主要用于前端的下拉选项")
    @Parameter(name = "type", description = "数据类型", required = true, example = "region")
    public CommonResult<List<ErpBaseDataRespVO>> getBaseDataSimpleList(@RequestParam("type") String type) {
        List<ErpBaseDataDO> list = baseDataService.getBaseDataSimpleListByType(type);
        List<ErpBaseDataRespVO> result = convertList(list, data -> new ErpBaseDataRespVO()
                .setId(data.getId()).setName(data.getName()).setCode(data.getCode()));
        for (int i = 0; i < result.size(); i++) {
            fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, result.get(i));
            result.get(i).setCode(list.get(i).getCode());
        }
        return success(result);
    }

}
