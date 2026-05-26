package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontract.ErpSupplierContractRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontract.ErpSupplierContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContractDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierContractService;
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

@Tag(name = "管理后台 - ERP 供应商合同")
@RestController
@RequestMapping("/erp/supplier-contract")
@Validated
public class ErpSupplierContractController {

    @Resource
    private ErpSupplierContractService supplierContractService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商合同")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierContract(@Valid @RequestBody ErpSupplierContractSaveReqVO createReqVO) {
        return success(supplierContractService.createSupplierContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商合同")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierContract(@Valid @RequestBody ErpSupplierContractSaveReqVO updateReqVO) {
        supplierContractService.updateSupplierContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商合同")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierContract(@RequestParam("id") Long id) {
        supplierContractService.deleteSupplierContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商合同")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierContractRespVO> getSupplierContract(@RequestParam("id") Long id) {
        ErpSupplierContractDO contract = supplierContractService.getSupplierContract(id);
        return success(BeanUtils.toBean(contract, ErpSupplierContractRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商合同列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierContractRespVO>> getSupplierContractList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierContractService.getSupplierContractList(supplierId), ErpSupplierContractRespVO.class));
    }

}
