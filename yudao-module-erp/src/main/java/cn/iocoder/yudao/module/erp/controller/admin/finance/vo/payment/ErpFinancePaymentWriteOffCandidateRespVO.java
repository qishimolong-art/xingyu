package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 付款单可核销业务单据 Response VO")
@Data
public class ErpFinancePaymentWriteOffCandidateRespVO {

    private Integer bizType;
    private String bizTypeName;
    private Long bizId;
    private String bizNo;
    private LocalDateTime bizTime;
    private BigDecimal totalPrice;
    private BigDecimal allocatedPrice;
    private BigDecimal unallocatedPrice;

}
