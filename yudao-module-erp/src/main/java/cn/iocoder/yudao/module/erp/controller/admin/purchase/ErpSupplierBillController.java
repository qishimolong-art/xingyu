package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbill.ErpSupplierBillRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbill.ErpSupplierBillSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBillDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierBillService;
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

@Tag(name = "管理后台 - ERP 供应商票据")
@RestController
@RequestMapping("/erp/supplier-bill")
@Validated
public class ErpSupplierBillController {

    @Resource
    private ErpSupplierBillService supplierBillService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商票据")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierBill(@Valid @RequestBody ErpSupplierBillSaveReqVO createReqVO) {
        return success(supplierBillService.createSupplierBill(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商票据")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierBill(@Valid @RequestBody ErpSupplierBillSaveReqVO updateReqVO) {
        supplierBillService.updateSupplierBill(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商票据")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierBill(@RequestParam("id") Long id) {
        supplierBillService.deleteSupplierBill(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商票据")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierBillRespVO> getSupplierBill(@RequestParam("id") Long id) {
        ErpSupplierBillDO bill = supplierBillService.getSupplierBill(id);
        return success(BeanUtils.toBean(bill, ErpSupplierBillRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商票据列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierBillRespVO>> getSupplierBillList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierBillService.getSupplierBillList(supplierId), ErpSupplierBillRespVO.class));
    }

}
