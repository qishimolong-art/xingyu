package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentItemDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpPrePaymentService;
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

@Tag(name = "管理后台 - ERP 预付款单")
@RestController
@RequestMapping("/erp/pre-payment")
@Validated
public class ErpPrePaymentController {

    @Resource
    private ErpPrePaymentService prePaymentService;

    @PostMapping("/create")
    @Operation(summary = "创建预付款单")
    @PreAuthorize("@ss.hasPermission('erp:pre-payment:create')")
    public CommonResult<Long> createPrePayment(@Valid @RequestBody ErpPrePaymentSaveReqVO createReqVO) {
        return success(prePaymentService.createPrePayment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新预付款单")
    @PreAuthorize("@ss.hasPermission('erp:pre-payment:update')")
    public CommonResult<Boolean> updatePrePayment(@Valid @RequestBody ErpPrePaymentSaveReqVO updateReqVO) {
        prePaymentService.updatePrePayment(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新预付款单状态")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pre-payment:update')")
    public CommonResult<Boolean> updatePrePaymentStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") Integer status) {
        prePaymentService.updatePrePaymentStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预付款单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pre-payment:delete')")
    public CommonResult<Boolean> deletePrePayment(@RequestParam("id") Long id) {
        prePaymentService.deletePrePayment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得预付款单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pre-payment:query')")
    public CommonResult<ErpPrePaymentRespVO> getPrePayment(@RequestParam("id") Long id) {
        ErpPrePaymentDO prePayment = prePaymentService.getPrePayment(id);
        if (prePayment == null) {
            return success(null);
        }
        ErpPrePaymentRespVO respVO = BeanUtils.toBean(prePayment, ErpPrePaymentRespVO.class);
        List<ErpPrePaymentItemDO> items = prePaymentService.getPrePaymentItemListByPrePaymentId(id);
        respVO.setItems(BeanUtils.toBean(items, ErpPrePaymentRespVO.Item.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得预付款单分页")
    @PreAuthorize("@ss.hasPermission('erp:pre-payment:query')")
    public CommonResult<PageResult<ErpPrePaymentRespVO>> getPrePaymentPage(@Valid ErpPrePaymentPageReqVO pageReqVO) {
        PageResult<ErpPrePaymentDO> pageResult = prePaymentService.getPrePaymentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpPrePaymentRespVO.class));
    }

}
