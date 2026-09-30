package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only report event; never persisted as another miscellaneous document. */
@Data
public class ErpMiscSettlementLedgerRow {
    private Long id;
    private Long documentId;
    private String no;
    private LocalDateTime bizTime;
    private BigDecimal amount;
    private BigDecimal settledAmount;
    private String remark;
    private String fileUrl;
}
