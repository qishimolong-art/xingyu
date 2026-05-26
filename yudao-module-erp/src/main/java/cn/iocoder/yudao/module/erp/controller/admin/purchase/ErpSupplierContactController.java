package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontact.ErpSupplierContactRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontact.ErpSupplierContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContactDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierContactService;
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

@Tag(name = "管理后台 - ERP 供应商联系人")
@RestController
@RequestMapping("/erp/supplier-contact")
@Validated
public class ErpSupplierContactController {

    @Resource
    private ErpSupplierContactService supplierContactService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商联系人")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierContact(@Valid @RequestBody ErpSupplierContactSaveReqVO createReqVO) {
        return success(supplierContactService.createSupplierContact(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商联系人")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierContact(@Valid @RequestBody ErpSupplierContactSaveReqVO updateReqVO) {
        supplierContactService.updateSupplierContact(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商联系人")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierContact(@RequestParam("id") Long id) {
        supplierContactService.deleteSupplierContact(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商联系人")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierContactRespVO> getSupplierContact(@RequestParam("id") Long id) {
        ErpSupplierContactDO contact = supplierContactService.getSupplierContact(id);
        return success(BeanUtils.toBean(contact, ErpSupplierContactRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商联系人列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierContactRespVO>> getSupplierContactList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierContactService.getSupplierContactList(supplierId), ErpSupplierContactRespVO.class));
    }

}
