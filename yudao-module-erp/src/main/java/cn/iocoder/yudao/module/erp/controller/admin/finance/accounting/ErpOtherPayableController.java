package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayableRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableItemDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpOtherPayableService;
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

@Tag(name = "管理后台 - ERP 其他应付单")
@RestController
@RequestMapping("/erp/other-payable")
@Validated
public class ErpOtherPayableController {

    @Resource
    private ErpOtherPayableService otherPayableService;

    @PostMapping("/create")
    @Operation(summary = "创建其他应付单")
    @PreAuthorize("@ss.hasPermission('erp:other-payable:create')")
    public CommonResult<Long> createOtherPayable(@Valid @RequestBody ErpOtherPayableSaveReqVO createReqVO) {
        return success(otherPayableService.createOtherPayable(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新其他应付单")
    @PreAuthorize("@ss.hasPermission('erp:other-payable:update')")
    public CommonResult<Boolean> updateOtherPayable(@Valid @RequestBody ErpOtherPayableSaveReqVO updateReqVO) {
        otherPayableService.updateOtherPayable(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新其他应付单状态")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('erp:other-payable:update-status')")
    public CommonResult<Boolean> updateOtherPayableStatus(@RequestParam("id") Long id,
                                                          @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        otherPayableService.updateOtherPayableStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除其他应付单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:other-payable:delete')")
    public CommonResult<Boolean> deleteOtherPayable(@RequestParam("id") Long id) {
        otherPayableService.deleteOtherPayable(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得其他应付单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:other-payable:query')")
    public CommonResult<ErpOtherPayableRespVO> getOtherPayable(@RequestParam("id") Long id) {
        ErpOtherPayableDO payable = otherPayableService.getOtherPayable(id);
        if (payable == null) {
            return success(null);
        }
        List<ErpOtherPayableItemDO> items = otherPayableService.getOtherPayableItemListByPayableId(id);
        return success(BeanUtils.toBean(payable, ErpOtherPayableRespVO.class, vo ->
                vo.setItems(BeanUtils.toBean(items, ErpOtherPayableRespVO.Item.class))));
    }

    @GetMapping("/page")
    @Operation(summary = "获得其他应付单分页")
    @PreAuthorize("@ss.hasPermission('erp:other-payable:query')")
    public CommonResult<PageResult<ErpOtherPayableRespVO>> getOtherPayablePage(@Valid ErpOtherPayablePageReqVO pageReqVO) {
        PageResult<ErpOtherPayableDO> pageResult = otherPayableService.getOtherPayablePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpOtherPayableRespVO.class));
    }

}
