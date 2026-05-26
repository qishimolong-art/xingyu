package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextend.ErpSupplierExtendRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextend.ErpSupplierExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierExtendService;
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

@Tag(name = "管理后台 - ERP 供应商动态拓展字段")
@RestController
@RequestMapping("/erp/supplier-extend")
@Validated
public class ErpSupplierExtendController {

    @Resource
    private ErpSupplierExtendService supplierExtendService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商动态拓展字段")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierExtend(@Valid @RequestBody ErpSupplierExtendSaveReqVO createReqVO) {
        return success(supplierExtendService.createSupplierExtend(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商动态拓展字段")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierExtend(@Valid @RequestBody ErpSupplierExtendSaveReqVO updateReqVO) {
        supplierExtendService.updateSupplierExtend(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商动态拓展字段")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierExtend(@RequestParam("id") Long id) {
        supplierExtendService.deleteSupplierExtend(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商动态拓展字段")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierExtendRespVO> getSupplierExtend(@RequestParam("id") Long id) {
        ErpSupplierExtendDO extend = supplierExtendService.getSupplierExtend(id);
        return success(BeanUtils.toBean(extend, ErpSupplierExtendRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商动态拓展字段列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierExtendRespVO>> getSupplierExtendList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierExtendService.getSupplierExtendList(supplierId), ErpSupplierExtendRespVO.class));
    }

}
