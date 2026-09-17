package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;

import java.util.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;

/** 科目角色与借贷模板由程序管理，科目编号不参与业务判断。 */
public final class ErpVoucherTemplates {
    public static final List<Integer> TYPES = Arrays.asList(2,3,4,6,8,9,10,12,16,17,18,21,22,23);
    public static final List<Template> ALL = Collections.unmodifiableList(Arrays.asList(
        new Template("PURCHASE", "采购正式入账",8,true,"借：库存、可抵扣进项、采购费用；贷：应付。库存金额须与流水一致。","INVENTORY","AP","INPUT_TAX","SELLING_EXPENSE"),
        new Template("PROVISIONAL", "采购月末暂估",8,true,"月末借：库存；贷：暂估应付。无暂估进项；后续冲回/到票人工处理。","INVENTORY","PROVISIONAL_AP","SELLING_EXPENSE"),
        new Template("PURCHASE_RETURN", "采购退货",9,true,"借：应付；贷：库存、进项及退回采购费用。","INVENTORY","AP","INPUT_TAX","SELLING_EXPENSE"),
        new Template("SALE", "销售确认收入并结转成本",2,true,"借：应收；贷：收入、税额。借：成本；贷：库存。","AR","REVENUE","OUTPUT_TAX","COST","INVENTORY"),
        new Template("SALE_RETURN", "销售退货",3,true,"冲减收入/应收/税额，按退回流水恢复库存及冲减成本。","AR","REVENUE","OUTPUT_TAX","COST","INVENTORY"),
        new Template("RECEIPT", "收款结算",6,false,"借：实际资金、现金折扣费用；贷：应收。退款反向。","AR","FINANCE_EXPENSE"),
        new Template("RECEIPT_ADVANCE", "收款单预收",6,false,"借：实际资金；贷：预收账款。","ADVANCE_RECEIPT"),
        new Template("PAYMENT", "付款结算",12,false,"借：应付；贷：实际资金、享受的现金折扣。退款反向。","AP","FINANCE_EXPENSE"),
        new Template("PAYMENT_ADVANCE", "付款单预付",12,false,"借：预付账款；贷：实际资金。","PREPAID"),
        new Template("PRE_RECEIPT", "预收款实际收款",21,false,"借：实际资金；贷：预收账款。","ADVANCE_RECEIPT"),
        new Template("PRE_RECEIVABLE", "预收账款实际收款",23,false,"借：实际资金；贷：预收账款。","ADVANCE_RECEIPT"),
        new Template("PRE_PAYMENT", "预付款实际付款",22,false,"借：预付账款；贷：实际资金。","PREPAID"),
        new Template("OTHER_AR_CREATE", "借款/押金支付",4,false,"借：其他应收；贷：实际资金。","OTHER_AR"),
        new Template("OTHER_AR_RECOVER", "借款/押金收回",4,false,"借：实际资金；贷：其他应收。","OTHER_AR"),
        new Template("OTHER_AP_CREATE", "借款/押金收到",10,false,"借：实际资金；贷：其他应付。","OTHER_AP"),
        new Template("OTHER_AP_REPAY", "借款/押金偿还",10,false,"借：其他应付；贷：实际资金。","OTHER_AP"),
        new Template("EXPENSE_ACCRUAL", "费用应付确认",10,true,"借：费用、可抵扣进项；贷：其他应付。不代表付款。","EXPENSE","OTHER_AP","INPUT_TAX"),
        new Template("STOCK_GAIN", "盘盈待处理",17,false,"借：库存；贷：待处理财产损溢。最终处理人工办理。","INVENTORY","PENDING"),
        new Template("USAGE_RETURN", "经营领用退回",17,false,"借：库存；贷：原经营费用。","INVENTORY","EXPENSE"),
        new Template("STOCK_LOSS", "盘亏待处理",16,false,"借：待处理财产损溢；贷：库存。涉进项转出需人工处理。","INVENTORY","PENDING"),
        new Template("USAGE", "经营领用",16,false,"借：经营费用；贷：库存。福利、赠送、视同销售人工处理。","INVENTORY","EXPENSE"),
        new Template("TRANSFER", "人民币账户转账",18,false,"借：转入账户、手续费；贷：转出账户（含手续费）。","FINANCE_EXPENSE")
    ));
    public static Template get(String code) {
        return ALL.stream().filter(t -> t.getCode().equals(code)).findFirst().orElse(null);
    }
    private ErpVoucherTemplates() {}
}
