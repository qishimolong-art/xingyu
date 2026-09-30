package cn.iocoder.yudao.module.erp.controller.admin.finance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 原其他应收/付单的有效结算明细。 */
@Data
public class ErpMiscSettlementRespVO {
    private Long documentId;
    private String no;
    private LocalDateTime bizTime;
    private BigDecimal amount;
    private Long accountId;
    private String accountName;
    private Long deptId;
    private String deptName;
    private Long handlerId;
    private String handlerName;
    private Integer status;
    private String remark;
}
