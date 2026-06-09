package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAccountingSubjectCodeConstants;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAuxiliaryTypeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_DEBIT_CREDIT_NOT_BALANCE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_SUBJECT_CODE_MISSING;

/**
 * ERP 自动凭证组装器
 *
 * 三期 业务单据审核时根据预定模板生成凭证分录。
 * 不查 sumCost、不算合计；调用方需把所需金额准备好传入。
 *
 * @author Claude
 */
@Component
public class ErpAutoVoucherBuilder {

    @Resource
    private ErpAccountingSubjectService subjectService;

    /**
     * 采购入库 分录（S1 修复：借贷必平衡）：
     *  借 1405 库存商品   (totalProductPrice)
     *  借 2221 应交税费   (totalTaxPrice)
     *  贷 2202 应付账款   (totalProductPrice + totalTaxPrice, auxiliary=supplier)
     *  如有折扣：贷 2202 应付账款 -discountPrice（即贷方再扣回） → 借 1002 银行存款 discountPrice
     *  如有其他费用：借 1002 银行存款 otherPrice → 贷 2202 应付账款 otherPrice
     *
     * 实务：贷方应付账款最终为 totalProductPrice + totalTaxPrice - discountPrice + otherPrice
     *      = purchaseIn.totalPrice，但已把折扣 / 其他费用拆出独立分录以保证借贷平衡。
     */
    public List<ErpVoucherItemDO> buildPurchaseInItems(ErpPurchaseInDO purchaseIn, String supplierName) {
        String summary = "采购入库 - " + nullToEmpty(supplierName);
        BigDecimal totalProduct = nullToZero(purchaseIn.getTotalProductPrice());
        BigDecimal totalTax = nullToZero(purchaseIn.getTotalTaxPrice());
        BigDecimal discount = nullToZero(purchaseIn.getDiscountPrice());
        BigDecimal other = resolveFeeAmount(purchaseIn.getFeeAmount(), purchaseIn.getOtherPrice());
        BigDecimal apAmount = totalProduct.add(totalTax).subtract(discount).add(other);
        List<ErpVoucherItemDO> items = new ArrayList<>(5);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.INVENTORY,
                totalProduct, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.TAX_PAYABLE,
                totalTax, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.AP,
                null, apAmount,
                ErpAuxiliaryTypeEnum.SUPPLIER.getType(), purchaseIn.getSupplierId(), supplierName));
        // 折扣：借入金额从供应商免，等同企业现金留存 → 借 1002 银行存款
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（折扣）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    discount, null,
                    null, null, null));
        }
        // 其他费用：企业额外现金支出 → 贷 1002 银行存款
        if (other.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（其他费用）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    null, other,
                    null, null, null));
        }
        List<ErpVoucherItemDO> filtered = filterZeroLines(items);
        validateBalance(filtered, "采购入库自动凭证");
        return filtered;
    }

    /**
     * 采购退货 分录（红字反向，S1 修复：借贷必平衡）：
     *  借 2202 应付账款   (apAmount, auxiliary=supplier)
     *  贷 1405 库存商品   (totalProductPrice)
     *  贷 2221 应交税费   (totalTaxPrice)
     *  如有折扣：贷 1002 银行存款 discountPrice
     *  如有其他费用：借 1002 银行存款 otherPrice
     */
    public List<ErpVoucherItemDO> buildPurchaseReturnItems(ErpPurchaseReturnDO purchaseReturn, String supplierName) {
        String summary = "采购退货 - " + nullToEmpty(supplierName);
        BigDecimal totalProduct = nullToZero(purchaseReturn.getTotalProductPrice());
        BigDecimal totalTax = nullToZero(purchaseReturn.getTotalTaxPrice());
        BigDecimal discount = nullToZero(purchaseReturn.getDiscountPrice());
        BigDecimal other = resolveFeeAmount(purchaseReturn.getFeeAmount(), purchaseReturn.getOtherPrice());
        BigDecimal apAmount = totalProduct.add(totalTax).subtract(discount).add(other);
        List<ErpVoucherItemDO> items = new ArrayList<>(5);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.AP,
                apAmount, null,
                ErpAuxiliaryTypeEnum.SUPPLIER.getType(), purchaseReturn.getSupplierId(), supplierName));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.INVENTORY,
                null, totalProduct,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.TAX_PAYABLE,
                null, totalTax,
                null, null, null));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（折扣）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    null, discount,
                    null, null, null));
        }
        if (other.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（其他费用）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    other, null,
                    null, null, null));
        }
        List<ErpVoucherItemDO> filtered = filterZeroLines(items);
        validateBalance(filtered, "采购退货自动凭证");
        return filtered;
    }

    /**
     * 销售出库 分录（S1 修复：借贷必平衡）：
     *  借 1122 应收账款       (arAmount, auxiliary=customer)
     *  贷 6001 主营业务收入   (totalProductPrice)
     *  贷 2221 应交税费       (totalTaxPrice)
     *  借 6401 主营业务成本   (sumCost)
     *  贷 1405 库存商品       (sumCost)
     *  如有折扣：借 1002 银行存款 discountPrice
     *  如有其他费用：贷 1002 银行存款 otherPrice
     */
    public List<ErpVoucherItemDO> buildSaleOutItems(ErpSaleOutDO saleOut, String customerName, BigDecimal sumCost) {
        String summary = "销售出库 - " + nullToEmpty(customerName);
        BigDecimal totalProduct = nullToZero(saleOut.getTotalProductPrice());
        BigDecimal totalTax = nullToZero(saleOut.getTotalTaxPrice());
        BigDecimal discount = nullToZero(saleOut.getDiscountPrice());
        BigDecimal other = resolveFeeAmount(saleOut.getFeeAmount(), saleOut.getOtherPrice());
        BigDecimal arAmount = totalProduct.add(totalTax).subtract(discount).add(other);
        List<ErpVoucherItemDO> items = new ArrayList<>(7);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.AR,
                arAmount, null,
                ErpAuxiliaryTypeEnum.CUSTOMER.getType(), saleOut.getCustomerId(), customerName));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.REVENUE,
                null, totalProduct,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.TAX_PAYABLE,
                null, totalTax,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.COST,
                sumCost, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.INVENTORY,
                null, sumCost,
                null, null, null));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（折扣）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    discount, null,
                    null, null, null));
        }
        if (other.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（其他费用）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    null, other,
                    null, null, null));
        }
        List<ErpVoucherItemDO> filtered = filterZeroLines(items);
        validateBalance(filtered, "销售出库自动凭证");
        return filtered;
    }

    /**
     * 销售退货 分录（红字反向，S1 修复：借贷必平衡）：
     *  贷 1122 应收账款       (arAmount, auxiliary=customer)
     *  借 6001 主营业务收入   (totalProductPrice)
     *  借 2221 应交税费       (totalTaxPrice)
     *  贷 6401 主营业务成本   (sumCost)
     *  借 1405 库存商品       (sumCost)
     *  如有折扣：贷 1002 银行存款 discountPrice
     *  如有其他费用：借 1002 银行存款 otherPrice
     */
    public List<ErpVoucherItemDO> buildSaleReturnItems(ErpSaleReturnDO saleReturn, String customerName, BigDecimal sumCost) {
        String summary = "销售退货 - " + nullToEmpty(customerName);
        BigDecimal totalProduct = nullToZero(saleReturn.getTotalProductPrice());
        BigDecimal totalTax = nullToZero(saleReturn.getTotalTaxPrice());
        BigDecimal discount = nullToZero(saleReturn.getDiscountPrice());
        BigDecimal other = resolveFeeAmount(saleReturn.getFeeAmount(), saleReturn.getOtherPrice());
        BigDecimal arAmount = totalProduct.add(totalTax).subtract(discount).add(other);
        List<ErpVoucherItemDO> items = new ArrayList<>(7);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.AR,
                null, arAmount,
                ErpAuxiliaryTypeEnum.CUSTOMER.getType(), saleReturn.getCustomerId(), customerName));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.REVENUE,
                totalProduct, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.TAX_PAYABLE,
                totalTax, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.COST,
                null, sumCost,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.INVENTORY,
                sumCost, null,
                null, null, null));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（折扣）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    null, discount,
                    null, null, null));
        }
        if (other.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLine(items.size() + 1, summary + "（其他费用）",
                    ErpAccountingSubjectCodeConstants.BANK,
                    other, null,
                    null, null, null));
        }
        List<ErpVoucherItemDO> filtered = filterZeroLines(items);
        validateBalance(filtered, "销售退货自动凭证");
        return filtered;
    }

    /**
     * 其他入库 2 行分录（盘盈/收料/捐赠等，金额按成本均价）：
     *  借 1405 库存商品           (sumCost)
     *  贷 1901 待处理财产损益     (sumCost)
     */
    public List<ErpVoucherItemDO> buildStockInItems(ErpStockInDO stockIn, BigDecimal sumCost) {
        String summary = "其他入库 - " + nullToEmpty(stockIn.getNo());
        List<ErpVoucherItemDO> items = new ArrayList<>(2);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.INVENTORY,
                sumCost, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.LOSS_AND_GAIN,
                null, sumCost,
                null, null, null));
        return filterZeroLines(items);
    }

    /**
     * 其他出库 2 行分录（盘亏/领用/损耗等，金额按成本均价）：
     *  借 1901 待处理财产损益     (sumCost)
     *  贷 1405 库存商品           (sumCost)
     */
    public List<ErpVoucherItemDO> buildStockOutItems(ErpStockOutDO stockOut, BigDecimal sumCost) {
        String summary = "其他出库 - " + nullToEmpty(stockOut.getNo());
        List<ErpVoucherItemDO> items = new ArrayList<>(2);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.LOSS_AND_GAIN,
                sumCost, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.INVENTORY,
                null, sumCost,
                null, null, null));
        return filterZeroLines(items);
    }

    /**
     * 其他应收单 2 行分录：
     *  借 1221 其他应收款 (actualAmount, auxiliary=partyType→AuxiliaryTypeEnum)
     *  贷 1002 银行存款   (actualAmount)
     */
    public List<ErpVoucherItemDO> buildOtherReceivableItems(ErpOtherReceivableDO receivable) {
        String summary = "其他应收 - " + nullToEmpty(receivable.getPartyName());
        BigDecimal amount = receivable.getActualAmount() == null ? BigDecimal.ZERO : receivable.getActualAmount();
        String auxiliaryType = mapPartyTypeToAuxiliary(receivable.getPartyType());
        List<ErpVoucherItemDO> items = new ArrayList<>(2);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.OTHER_RECEIVABLE,
                amount, null,
                auxiliaryType, receivable.getPartyId(), receivable.getPartyName()));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.BANK,
                null, amount,
                null, null, null));
        return filterZeroLines(items);
    }

    /**
     * 其他应付单 2 行分录：
     *  借 2241 其他应付款 (actualAmount, auxiliary=partyType映射)
     *  贷 1002 银行存款   (actualAmount)
     */
    public List<ErpVoucherItemDO> buildOtherPayableItems(ErpOtherPayableDO payable) {
        String summary = "其他应付 - " + nullToEmpty(payable.getPartyName());
        BigDecimal amount = payable.getActualAmount() == null ? BigDecimal.ZERO : payable.getActualAmount();
        String auxiliaryType = mapPartyTypeToAuxiliary(payable.getPartyType());
        List<ErpVoucherItemDO> items = new ArrayList<>(2);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.OTHER_PAYABLE,
                amount, null,
                auxiliaryType, payable.getPartyId(), payable.getPartyName()));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.BANK,
                null, amount,
                null, null, null));
        return filterZeroLines(items);
    }

    /**
     * 预收款单 2 行分录：
     *  借 1002 银行存款   (actualAmount)
     *  贷 2203 预收账款   (actualAmount, auxiliary=partyType映射+partyId+partyName)
     */
    public List<ErpVoucherItemDO> buildPreReceiptItems(ErpPreReceiptDO preReceipt) {
        String summary = "预收款 - " + nullToEmpty(preReceipt.getPartyName());
        BigDecimal actualAmount = preReceipt.getActualAmount() == null ? BigDecimal.ZERO : preReceipt.getActualAmount();
        String auxiliaryType = mapPartyTypeToAuxiliary(preReceipt.getPartyType());
        List<ErpVoucherItemDO> items = new ArrayList<>(2);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.BANK,
                actualAmount, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.PRE_RECEIPT,
                null, actualAmount,
                auxiliaryType, preReceipt.getPartyId(), preReceipt.getPartyName()));
        return filterZeroLines(items);
    }

    /**
     * 预收账款单 2 行分录：
     *  借 1002 银行存款       (actualAmount)
     *  贷 2204 预收账款(另)   (actualAmount, auxiliary=partyType映射+partyId+partyName)
     */
    public List<ErpVoucherItemDO> buildPreReceivableItems(ErpPreReceivableDO preReceivable) {
        String summary = "预收账款 - " + nullToEmpty(preReceivable.getPartyName());
        BigDecimal actualAmount = preReceivable.getActualAmount() == null ? BigDecimal.ZERO : preReceivable.getActualAmount();
        String auxiliaryType = mapPartyTypeToAuxiliary(preReceivable.getPartyType());
        List<ErpVoucherItemDO> items = new ArrayList<>(2);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.BANK,
                actualAmount, null,
                null, null, null));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.PRE_RECEIVABLE,
                null, actualAmount,
                auxiliaryType, preReceivable.getPartyId(), preReceivable.getPartyName()));
        return filterZeroLines(items);
    }

    /**
     * 预付款单 2 行分录：
     *  借 1123 预付账款   (actualAmount, auxiliary=partyType映射+partyId+partyName)
     *  贷 1002 银行存款   (actualAmount)
     */
    public List<ErpVoucherItemDO> buildPrePaymentItems(ErpPrePaymentDO prePayment) {
        String summary = "预付款 - " + nullToEmpty(prePayment.getPartyName());
        BigDecimal actualAmount = prePayment.getActualAmount() == null ? BigDecimal.ZERO : prePayment.getActualAmount();
        String auxiliaryType = mapPartyTypeToAuxiliary(prePayment.getPartyType());
        List<ErpVoucherItemDO> items = new ArrayList<>(2);
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.PREPAID,
                actualAmount, null,
                auxiliaryType, prePayment.getPartyId(), prePayment.getPartyName()));
        items.add(buildLine(items.size() + 1, summary,
                ErpAccountingSubjectCodeConstants.BANK,
                null, actualAmount,
                null, null, null));
        return filterZeroLines(items);
    }

    /**
     * partyType 映射到辅助核算类型：1=客户 2=供应商 3=员工(个人)
     */
    private String mapPartyTypeToAuxiliary(Integer partyType) {
        if (partyType == null) {
            return null;
        }
        switch (partyType) {
            case 1: return ErpAuxiliaryTypeEnum.CUSTOMER.getType();
            case 2: return ErpAuxiliaryTypeEnum.SUPPLIER.getType();
            case 3: return ErpAuxiliaryTypeEnum.PERSON.getType();
            default: return null;
        }
    }

    // ==================== 私有辅助 ====================

    /**
     * 构造一行分录：subject 信息按 subjectCode 反查；金额标准化为 2 位小数（HALF_UP），null 视作 ZERO。
     */
    private ErpVoucherItemDO buildLine(int lineNo, String summary, String subjectCode,
                                       BigDecimal debit, BigDecimal credit,
                                       String auxiliaryType, Long auxiliaryId, String auxiliaryName) {
        ErpAccountingSubjectDO subject = subjectService.getSubjectByCode(subjectCode);
        if (subject == null) {
            throw exception(VOUCHER_SUBJECT_CODE_MISSING, subjectCode);
        }
        return new ErpVoucherItemDO()
                .setLineNo(lineNo)
                .setSummary(summary)
                .setSubjectId(subject.getId())
                .setSubjectCode(subject.getSubjectCode())
                .setSubjectName(subject.getSubjectName())
                .setAuxiliaryType(auxiliaryType)
                .setAuxiliaryId(auxiliaryId)
                .setAuxiliaryName(auxiliaryName)
                .setDebitAmount(scale2(debit))
                .setCreditAmount(scale2(credit));
    }

    private BigDecimal scale2(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice) {
        return feeAmount != null ? feeAmount : nullToZero(otherPrice);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 借贷平衡校验：sum(debit) == sum(credit)（用 BigDecimal#compareTo），
     * 不平衡时抛 VOUCHER_DEBIT_CREDIT_NOT_BALANCE。S1 修复必备保险。
     */
    private void validateBalance(List<ErpVoucherItemDO> items, String context) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpVoucherItemDO item : items) {
            totalDebit = totalDebit.add(item.getDebitAmount() == null ? BigDecimal.ZERO : item.getDebitAmount());
            totalCredit = totalCredit.add(item.getCreditAmount() == null ? BigDecimal.ZERO : item.getCreditAmount());
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw exception(VOUCHER_DEBIT_CREDIT_NOT_BALANCE,
                    totalDebit.toPlainString(), totalCredit.toPlainString());
        }
    }

    /**
     * 过滤借贷都为 0 的零额行，并按过滤后顺序重写 lineNo。
     * 自动凭证模板按固定行结构生成，但当某项金额为 0（如采购入库无税）时不应保留空行。
     */
    private List<ErpVoucherItemDO> filterZeroLines(List<ErpVoucherItemDO> items) {
        List<ErpVoucherItemDO> result = new ArrayList<>(items.size());
        int lineNo = 1;
        for (ErpVoucherItemDO item : items) {
            BigDecimal debit = item.getDebitAmount() == null ? BigDecimal.ZERO : item.getDebitAmount();
            BigDecimal credit = item.getCreditAmount() == null ? BigDecimal.ZERO : item.getCreditAmount();
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            item.setLineNo(lineNo++);
            result.add(item);
        }
        return result;
    }

}
