package cn.iocoder.yudao.module.erp.enums.finance.accounting;

/**
 * ERP 会计科目编码常量
 *
 * 三期 自动生成凭证使用的固定末级科目编码。
 * 与 sql/mysql/erp_accounting_subject_v26.sql 预置数据一致。
 * 四期 追加 1901 待处理财产损益（其他出/入库），与 erp_accounting_subject_v27.sql 一致。
 * 五期 追加 1221/2203/2204/2241（其他应收/预收/预收另/其他应付），与 erp_finance_voucher_v28.sql 一致。
 */
public interface ErpAccountingSubjectCodeConstants {

    /** 1001 现金 */
    String CASH = "1001";
    /** 1002 银行存款 */
    String BANK = "1002";
    /** 1122 应收账款 */
    String AR = "1122";
    /** 1123 预付账款 */
    String PREPAID = "1123";
    /** 1221 其他应收款（五期 其他应收单） */
    String OTHER_RECEIVABLE = "1221";
    /** 1405 库存商品 */
    String INVENTORY = "1405";
    /** 1901 待处理财产损益（四期 其他出/入库自动凭证使用） */
    String LOSS_AND_GAIN = "1901";
    /** 2202 应付账款 */
    String AP = "2202";
    /** 2203 预收账款（五期 预收款单） */
    String PRE_RECEIPT = "2203";
    /** 2204 预收账款(另)（五期 预收账款单） */
    String PRE_RECEIVABLE = "2204";
    /** 2221 应交税费 */
    String TAX_PAYABLE = "2221";
    /** 2241 其他应付款（五期 其他应付单） */
    String OTHER_PAYABLE = "2241";
    /** 6001 主营业务收入 */
    String REVENUE = "6001";
    /** 6401 主营业务成本 */
    String COST = "6401";

}
