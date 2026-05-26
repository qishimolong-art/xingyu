package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplieraccount.ErpSupplierAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplieraccount.ErpSupplierAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierAccountDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierAccountService;
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

@Tag(name = "管理后台 - ERP 供应商账户")
@RestController
@RequestMapping("/erp/supplier-account")
@Validated
public class ErpSupplierAccountController {

    @Resource
    private ErpSupplierAccountService supplierAccountService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商账户")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierAccount(@Valid @RequestBody ErpSupplierAccountSaveReqVO createReqVO) {
        return success(supplierAccountService.createSupplierAccount(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商账户")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierAccount(@Valid @RequestBody ErpSupplierAccountSaveReqVO updateReqVO) {
        supplierAccountService.updateSupplierAccount(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商账户")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierAccount(@RequestParam("id") Long id) {
        supplierAccountService.deleteSupplierAccount(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商账户")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierAccountRespVO> getSupplierAccount(@RequestParam("id") Long id) {
        ErpSupplierAccountDO account = supplierAccountService.getSupplierAccount(id);
        return success(BeanUtils.toBean(account, ErpSupplierAccountRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商账户列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierAccountRespVO>> getSupplierAccountList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierAccountService.getSupplierAccountList(supplierId), ErpSupplierAccountRespVO.class));
    }

}
