package cn.iocoder.yudao.module.erp.controller.admin.finance.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资金业务列表查询汇总。
 *
 * <p>不同单据只使用其中与自身业务口径对应的字段；金额字段允许为 {@code null}，
 * 用于表达字段权限遮罩，前端不得将其转换为零。</p>
 */
@Data
public class ErpFinanceQuerySummaryRespVO {

    private Long documentCount;
    private Long draftCount;
    private Long processCount;
    private Long approveCount;

    private BigDecimal totalPrice;
    private BigDecimal discountPrice;
    private BigDecimal receiptPrice;
    private BigDecimal paymentPrice;
    private BigDecimal transferPrice;
    private BigDecimal totalAmount;

    private BigDecimal amount;
    private BigDecimal settledAmount;
    private BigDecimal balanceAmount;
    private BigDecimal pendingTransferAmount;
    private BigDecimal transferAvailableAmount;

    private BigDecimal receivableAmount;
    private BigDecimal payableAmount;
    private BigDecimal outstandingAmount;

    private BigDecimal receivableBalance;
    private BigDecimal payableBalance;
    private BigDecimal offsetBalance;

}
