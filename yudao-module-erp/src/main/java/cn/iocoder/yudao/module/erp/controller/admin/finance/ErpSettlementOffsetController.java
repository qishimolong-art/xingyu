package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetRespVO;
import cn.iocoder.yudao.module.erp.service.finance.settlement.ErpSettlementOffsetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 应收冲应付")
@RestController
@RequestMapping("/erp/settlement-offset")
@Validated
public class ErpSettlementOffsetController {

    @Resource
    private ErpSettlementOffsetService settlementOffsetService;

    @GetMapping("/page")
    @Operation(summary = "获取应收冲应付分页")
    @PreAuthorize("@ss.hasPermission('erp:settlement-offset:query')")
    public CommonResult<PageResult<ErpSettlementOffsetRespVO>> getSettlementOffsetPage(@Valid ErpSettlementOffsetPageReqVO reqVO) {
        return success(BeanUtils.toBean(settlementOffsetService.getSettlementOffsetPage(reqVO), ErpSettlementOffsetRespVO.class));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取应收冲应付明细")
    @PreAuthorize("@ss.hasPermission('erp:settlement-offset:query')")
    public CommonResult<ErpSettlementOffsetDetailRespVO> getSettlementOffsetDetail(@Valid ErpSettlementOffsetDetailReqVO reqVO) {
        return success(settlementOffsetService.getSettlementOffsetDetail(reqVO));
    }
}
