package cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "ERP 应收冲应付明细 Response VO")
@Data
public class ErpSettlementOffsetDetailRespVO {

    @Schema(description = "应收明细")
    private List<ErpReceivableDetailRespVO> receivableDetails;

    @Schema(description = "应付明细")
    private List<ErpPayableDetailRespVO> payableDetails;
}
