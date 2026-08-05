package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 调拨出入库台账日汇总 Response VO")
@Data
@Accessors(chain = true)
public class ErpStockTransferLedgerSummaryRespVO {

    private LocalDate businessDate;
    private Long transferOutDocumentCount;
    private BigDecimal transferOutCount;
    private Long transferInDocumentCount;
    private BigDecimal transferInCount;
    private BigDecimal differenceCount;
    private Long pendingGroupCount;
    private Long completedGroupCount;
    private Long abnormalGroupCount;
    private String ledgerStatus;

}
