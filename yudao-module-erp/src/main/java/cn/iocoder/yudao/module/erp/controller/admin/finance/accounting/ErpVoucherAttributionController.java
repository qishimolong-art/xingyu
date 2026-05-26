package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionApplyReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateFromBizReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSearchSourceBizReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherAttributionDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherAttributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 凭证归属（跨月调整）")
@RestController
@RequestMapping("/erp/voucher-attribution")
@Validated
public class ErpVoucherAttributionController {

    @Resource
    private ErpVoucherAttributionService voucherAttributionService;

    @PostMapping("/create")
    @Operation(summary = "创建归属记录")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:apply')")
    public CommonResult<Long> createVoucherAttribution(@Valid @RequestBody ErpVoucherAttributionSaveReqVO createReqVO) {
        return success(voucherAttributionService.createVoucherAttribution(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新归属记录")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:apply')")
    public CommonResult<Boolean> updateVoucherAttribution(@Valid @RequestBody ErpVoucherAttributionSaveReqVO updateReqVO) {
        voucherAttributionService.updateVoucherAttribution(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除归属记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:apply')")
    public CommonResult<Boolean> deleteVoucherAttribution(@RequestParam("id") Long id) {
        voucherAttributionService.deleteVoucherAttribution(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得归属记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:query')")
    public CommonResult<ErpVoucherAttributionRespVO> getVoucherAttribution(@RequestParam("id") Long id) {
        ErpVoucherAttributionDO attribution = voucherAttributionService.getVoucherAttribution(id);
        return success(BeanUtils.toBean(attribution, ErpVoucherAttributionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得归属记录分页")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:query')")
    public CommonResult<PageResult<ErpVoucherAttributionRespVO>> getVoucherAttributionPage(
            @Valid ErpVoucherAttributionPageReqVO pageReqVO) {
        PageResult<ErpVoucherAttributionDO> pageResult = voucherAttributionService.getVoucherAttributionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpVoucherAttributionRespVO.class));
    }

    @PutMapping("/apply")
    @Operation(summary = "批量应用归属（设置归属年月）")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:apply')")
    public CommonResult<Boolean> applyAttribution(@Valid @RequestBody ErpVoucherAttributionApplyReqVO reqVO) {
        voucherAttributionService.applyAttribution(reqVO);
        return success(true);
    }

    @PostMapping("/generate-vouchers")
    @Operation(summary = "批量生成凭证")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:generate')")
    public CommonResult<List<Long>> generateVouchers(@Valid @RequestBody ErpVoucherAttributionGenerateReqVO reqVO) {
        return success(voucherAttributionService.generateVouchers(reqVO));
    }

    @PostMapping("/generate-vouchers-from-biz")
    @Operation(summary = "从业务单据快照生成凭证（凭证生成页专用）")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:generate')")
    public CommonResult<List<Long>> generateVouchersFromBiz(
            @Valid @RequestBody ErpVoucherAttributionGenerateFromBizReqVO reqVO) {
        return success(voucherAttributionService.generateVouchersFromBiz(reqVO));
    }

    @GetMapping("/search-source-biz")
    @Operation(summary = "凭证生成-按单据来源类型分页查询业务单据")
    @PreAuthorize("@ss.hasPermission('erp:voucher-attribution:query')")
    public CommonResult<PageResult<ErpVoucherAttributionRespVO>> searchSourceBizPage(
            @Valid ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        return success(voucherAttributionService.searchSourceBizPage(reqVO));
    }

}
