package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ErpStockTransferLedgerTotalRespVO {

    private BigDecimal transferOutCount;
    private BigDecimal transferInCount;
    private BigDecimal differenceCount;
    private Long abnormalGroupCount;

}
