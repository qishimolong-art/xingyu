package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierimage.ErpSupplierImageRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierimage.ErpSupplierImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierImageDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierImageService;
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

@Tag(name = "管理后台 - ERP 供应商图片")
@RestController
@RequestMapping("/erp/supplier-image")
@Validated
public class ErpSupplierImageController {

    @Resource
    private ErpSupplierImageService supplierImageService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商图片")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierImage(@Valid @RequestBody ErpSupplierImageSaveReqVO createReqVO) {
        return success(supplierImageService.createSupplierImage(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商图片")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierImage(@Valid @RequestBody ErpSupplierImageSaveReqVO updateReqVO) {
        supplierImageService.updateSupplierImage(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商图片")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierImage(@RequestParam("id") Long id) {
        supplierImageService.deleteSupplierImage(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商图片")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierImageRespVO> getSupplierImage(@RequestParam("id") Long id) {
        ErpSupplierImageDO image = supplierImageService.getSupplierImage(id);
        return success(BeanUtils.toBean(image, ErpSupplierImageRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商图片列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierImageRespVO>> getSupplierImageList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierImageService.getSupplierImageList(supplierId), ErpSupplierImageRespVO.class));
    }

}
