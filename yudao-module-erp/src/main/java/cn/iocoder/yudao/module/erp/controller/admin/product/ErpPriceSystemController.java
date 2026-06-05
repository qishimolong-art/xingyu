package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpPriceSystemDO;
import cn.iocoder.yudao.module.erp.service.product.ErpPriceSystemService;
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

@Tag(name = "管理后台 - ERP 价格体系")
@RestController
@RequestMapping("/erp/price-system")
@Validated
public class ErpPriceSystemController {

    @Resource
    private ErpPriceSystemService priceSystemService;

    @PostMapping("/create")
    @Operation(summary = "创建价格体系")
    @PreAuthorize("@ss.hasPermission('erp:price-system:create')")
    public CommonResult<Long> createPriceSystem(@Valid @RequestBody ErpPriceSystemSaveReqVO createReqVO) {
        return success(priceSystemService.createPriceSystem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新价格体系")
    @PreAuthorize("@ss.hasPermission('erp:price-system:update')")
    public CommonResult<Boolean> updatePriceSystem(@RequestBody ErpPriceSystemSaveReqVO updateReqVO) {
        priceSystemService.updatePriceSystem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除价格体系")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:price-system:delete')")
    public CommonResult<Boolean> deletePriceSystem(@RequestParam("ids") List<Long> ids) {
        priceSystemService.deletePriceSystem(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得价格体系")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:price-system:query')")
    public CommonResult<ErpPriceSystemRespVO> getPriceSystem(@RequestParam("id") Long id) {
        ErpPriceSystemDO priceSystem = priceSystemService.getPriceSystem(id);
        return success(BeanUtils.toBean(priceSystem, ErpPriceSystemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得价格体系分页")
    @PreAuthorize("@ss.hasPermission('erp:price-system:query')")
    public CommonResult<PageResult<ErpPriceSystemRespVO>> getPriceSystemPage(@Valid ErpPriceSystemPageReqVO pageReqVO) {
        PageResult<ErpPriceSystemDO> pageResult = priceSystemService.getPriceSystemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpPriceSystemRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得价格体系精简列表", description = "用于前端的下拉选项")
    @PreAuthorize("@ss.hasPermission('erp:price-system:query')")
    public CommonResult<List<ErpPriceSystemDO>> getPriceSystemSimpleList() {
        List<ErpPriceSystemDO> list = priceSystemService.getPriceSystemListByStatus(null);
        return success(list);
    }

}
