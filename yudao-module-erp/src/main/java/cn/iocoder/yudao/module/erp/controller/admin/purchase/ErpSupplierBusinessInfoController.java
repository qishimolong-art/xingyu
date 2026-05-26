package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbusinessinfo.ErpSupplierBusinessInfoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbusinessinfo.ErpSupplierBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBusinessInfoDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierBusinessInfoService;
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

@Tag(name = "管理后台 - ERP 供应商工商信息")
@RestController
@RequestMapping("/erp/supplier-business-info")
@Validated
public class ErpSupplierBusinessInfoController {

    @Resource
    private ErpSupplierBusinessInfoService supplierBusinessInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商工商信息")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierBusinessInfo(@Valid @RequestBody ErpSupplierBusinessInfoSaveReqVO createReqVO) {
        return success(supplierBusinessInfoService.createSupplierBusinessInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商工商信息")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierBusinessInfo(@Valid @RequestBody ErpSupplierBusinessInfoSaveReqVO updateReqVO) {
        supplierBusinessInfoService.updateSupplierBusinessInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商工商信息")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierBusinessInfo(@RequestParam("id") Long id) {
        supplierBusinessInfoService.deleteSupplierBusinessInfo(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商工商信息")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierBusinessInfoRespVO> getSupplierBusinessInfo(@RequestParam("id") Long id) {
        ErpSupplierBusinessInfoDO businessInfo = supplierBusinessInfoService.getSupplierBusinessInfo(id);
        return success(BeanUtils.toBean(businessInfo, ErpSupplierBusinessInfoRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商工商信息列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierBusinessInfoRespVO>> getSupplierBusinessInfoList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierBusinessInfoService.getSupplierBusinessInfoList(supplierId), ErpSupplierBusinessInfoRespVO.class));
    }

}
