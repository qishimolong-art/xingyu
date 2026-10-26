package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

/** 收付款草稿允许先录入表头；默认日期、账户不构成有效草稿内容。 */
final class ErpFinanceSettlementDraftHelper {

    private ErpFinanceSettlementDraftHelper() {
    }

    static boolean hasHeaderContent(Long partyId, BigDecimal totalPrice, BigDecimal discountPrice,
                                    String paymentVoucherNo, String remark) {
        return partyId != null || nonZero(totalPrice) || nonZero(discountPrice)
                || StrUtil.isNotBlank(paymentVoucherNo) || StrUtil.isNotBlank(remark);
    }

    private static boolean nonZero(BigDecimal amount) {
        return amount != null && amount.signum() != 0;
    }
}
