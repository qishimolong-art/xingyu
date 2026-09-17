package cn.iocoder.yudao.module.erp.service.finance.accounting;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.*;
import java.math.BigDecimal;
import java.util.List;
/** 已退役的兼容入口，拒绝绕过规则配置、业务确认及预览校验。新生成统一使用 ErpVoucherGenerationService。 */
@Deprecated
public class ErpAutoVoucherBuilder {
    public List<ErpVoucherItemDO> buildPurchaseInItems(ErpPurchaseInDO purchaseIn, String supplierName) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildPurchaseReturnItems(ErpPurchaseReturnDO purchaseReturn, String supplierName) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildSaleOutItems(ErpSaleOutDO saleOut, String customerName, BigDecimal sumCost) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildSaleReturnItems(ErpSaleReturnDO saleReturn, String customerName, BigDecimal sumCost) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildStockInItems(ErpStockInDO stockIn, BigDecimal sumCost) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildStockOutItems(ErpStockOutDO stockOut, BigDecimal sumCost) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildOtherReceivableItems(ErpOtherReceivableDO receivable) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildOtherPayableItems(ErpOtherPayableDO payable) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildPreReceiptItems(ErpPreReceiptDO preReceipt) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildPreReceivableItems(ErpPreReceivableDO preReceivable) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
    public List<ErpVoucherItemDO> buildPrePaymentItems(ErpPrePaymentDO prePayment) {
        throw new IllegalStateException("旧凭证模板已停用，请通过凭证生成页预览生成");
    }
}
