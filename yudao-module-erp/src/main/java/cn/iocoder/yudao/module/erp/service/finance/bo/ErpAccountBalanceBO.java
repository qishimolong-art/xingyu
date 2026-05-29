package cn.iocoder.yudao.module.erp.service.finance.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 结算账户余额聚合结果
 */
@Data
public class ErpAccountBalanceBO {

    /**
     * 账户编号
     */
    private Long accountId;

    /**
     * 当前余额
     */
    private BigDecimal currentBalance;

}
