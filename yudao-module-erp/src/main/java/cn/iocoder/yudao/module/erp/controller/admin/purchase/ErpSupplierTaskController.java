package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliertask.ErpSupplierTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliertask.ErpSupplierTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierTaskDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierTaskService;
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

@Tag(name = "管理后台 - ERP 供应商任务量")
@RestController
@RequestMapping("/erp/supplier-task")
@Validated
public class ErpSupplierTaskController {

    @Resource
    private ErpSupplierTaskService supplierTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商任务量")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Long> createSupplierTask(@Valid @RequestBody ErpSupplierTaskSaveReqVO createReqVO) {
        return success(supplierTaskService.createSupplierTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商任务量")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> updateSupplierTask(@Valid @RequestBody ErpSupplierTaskSaveReqVO updateReqVO) {
        supplierTaskService.updateSupplierTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商任务量")
    @PreAuthorize("@ss.hasPermission('erp:supplier:update')")
    public CommonResult<Boolean> deleteSupplierTask(@RequestParam("id") Long id) {
        supplierTaskService.deleteSupplierTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商任务量")
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<ErpSupplierTaskRespVO> getSupplierTask(@RequestParam("id") Long id) {
        ErpSupplierTaskDO task = supplierTaskService.getSupplierTask(id);
        return success(BeanUtils.toBean(task, ErpSupplierTaskRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得供应商任务量列表")
    @Parameter(name = "supplierId", description = "供应商编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier:query')")
    public CommonResult<List<ErpSupplierTaskRespVO>> getSupplierTaskList(@RequestParam("supplierId") Long supplierId) {
        return success(BeanUtils.toBean(supplierTaskService.getSupplierTaskList(supplierId), ErpSupplierTaskRespVO.class));
    }

}
