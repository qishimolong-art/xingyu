package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 凭证生成-单据来源类型枚举（20 种）
 *
 * 用于「凭证生成」页面的单据类型下拉，与 ErpVoucherTypeEnum（凭证类型）不同：
 * - ErpVoucherTypeEnum: 凭证维度（11 种），如"销售凭证""采购凭证"
 * - ErpVoucherSourceBizTypeEnum: 单据维度（20 种），如"销售出库单""采购入库单"
 *
 * 业务关系：一种凭证类型可对应多种单据来源（如"销售凭证"可来自"销售出库单"或"销售退货单"）。
 *
 * 见 刘/财务问题汇总-ds修订版.md 5.2 节单据类型选项错误。
 */
@RequiredArgsConstructor
@Getter
public enum ErpVoucherSourceBizTypeEnum implements ArrayValuable<Integer> {

    SALE_VOUCHER(1, "销售凭证"),
    SALE_OUT(2, "销售出库单"),
    SALE_RETURN(3, "销售退货单"),
    OTHER_RECEIVABLE(4, "其他应收"),
    PRE_RECEIPT_VOUCHER(5, "预收款凭证"),
    RECEIPT(6, "收款凭证"),
    PURCHASE_VOUCHER(7, "采购凭证"),
    PURCHASE_IN(8, "采购入库单"),
    PURCHASE_RETURN(9, "采购退货单"),
    OTHER_PAYABLE(10, "其他应付"),
    PRE_PAYMENT_VOUCHER(11, "预付款凭证"),
    PAYMENT(12, "付款凭证"),
    OTHER_INCOME_VOUCHER(13, "其他收入凭证"),
    EXPENSE_VOUCHER(14, "费用支出凭证"),
    STOCK_MOVE_OUT(15, "调拨出库单"),
    OTHER_OUT(16, "其他出库单"),
    OTHER_IN(17, "其他入库单"),
    BANK_TRANSFER(18, "银行转账"),
    PURCHASE_PRICE_ADJUST(19, "采购调价"),
    ONLINE_RECEIPT(20, "线上收款"),
    PRE_RECEIPT(21, "pre receipt"),
    PRE_PAYMENT(22, "pre payment"),
    PRE_RECEIVABLE(23, "pre receivable"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpVoucherSourceBizTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
