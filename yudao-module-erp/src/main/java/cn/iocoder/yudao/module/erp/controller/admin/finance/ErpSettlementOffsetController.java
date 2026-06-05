package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailExportRespVO;
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
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
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

    @GetMapping("/export-detail")
    @Operation(summary = "导出应收冲应付明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:settlement-offset:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSettlementOffsetDetail(@Valid ErpSettlementOffsetDetailReqVO reqVO,
                                             HttpServletResponse response) throws IOException {
        ErpSettlementOffsetDetailRespVO detail = settlementOffsetService.getSettlementOffsetDetail(reqVO);
        ExcelUtils.write(response, "应收冲应付明细.xls", "明细", ErpSettlementOffsetDetailExportRespVO.class,
                buildSettlementOffsetDetailExportRows(detail));
    }

    private List<ErpSettlementOffsetDetailExportRespVO> buildSettlementOffsetDetailExportRows(
            ErpSettlementOffsetDetailRespVO detail) {
        List<ErpSettlementOffsetDetailExportRespVO> rows = new ArrayList<>();
        if (detail == null) {
            return rows;
        }
        if (detail.getReceivableDetails() != null) {
            for (ErpReceivableDetailRespVO receivable : detail.getReceivableDetails()) {
                ErpSettlementOffsetDetailExportRespVO row = BeanUtils.toBean(receivable,
                        ErpSettlementOffsetDetailExportRespVO.class);
                row.setDirection("应收");
                row.setDecreaseAmount(receivable.getReceiptAmount());
                rows.add(row);
            }
        }
        if (detail.getPayableDetails() != null) {
            for (ErpPayableDetailRespVO payable : detail.getPayableDetails()) {
                ErpSettlementOffsetDetailExportRespVO row = BeanUtils.toBean(payable,
                        ErpSettlementOffsetDetailExportRespVO.class);
                row.setDirection("应付");
                row.setDecreaseAmount(payable.getPaymentAmount());
                rows.add(row);
            }
        }
        return rows;
    }
}
