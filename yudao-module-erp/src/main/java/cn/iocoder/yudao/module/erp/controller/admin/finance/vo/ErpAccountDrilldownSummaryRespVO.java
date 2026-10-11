package cn.iocoder.yudao.module.erp.controller.admin.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "ERP 往来账户金额列下钻汇总 Response VO")
@Data
public class ErpAccountDrilldownSummaryRespVO {

    private Long detailCount;
    private BigDecimal openingAmount;
    private BigDecimal increaseAmount;
    private BigDecimal decreaseAmount;
    private BigDecimal targetAmount;
    private BigDecimal endingAmount;
}
