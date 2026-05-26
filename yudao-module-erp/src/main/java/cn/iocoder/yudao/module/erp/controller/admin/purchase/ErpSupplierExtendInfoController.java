package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextendinfo.ErpSupplierExtendInfoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextendinfo.ErpSupplierExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendInfoDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierExtendInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 供应商结构化拓展信息")
@RestController
@RequestMapping("/erp/supplier-extend-info")
@Validated
public class ErpSupplierExtendInfoController {

    @Resource
    private ErpSupplierExtendInfoService supplierExtendInfoService;

    @GetMapping("/get-by-supplier-id")
    @Operation(summary = "获得供应商结构化拓展信息")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierExtendInfoRespVO> getSupplierExtendInfo(@RequestParam("supplierId") Long supplierId) {
        ErpSupplierExtendInfoDO info = supplierExtendInfoService.getSupplierExtendInfo(supplierId);
        return success(BeanUtils.toBean(info, ErpSupplierExtendInfoRespVO.class));
    }

    @PostMapping("/save")
    @Operation(summary = "保存供应商结构化拓展信息")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> saveSupplierExtendInfo(@Valid @RequestBody ErpSupplierExtendInfoSaveReqVO saveReqVO) {
        return success(supplierExtendInfoService.saveSupplierExtendInfo(saveReqVO));
    }

}
