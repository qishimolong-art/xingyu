package cn.iocoder.yudao.module.erp.service.base;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableWriteOffDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableWriteOffDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceTransferMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_DELETE_FAIL_REFERENCED;

/**
 * ERP 基础档案删除前引用校验。
 */
@Service
public class ErpBaseArchiveReferenceService {

    private static final Integer PARTY_TYPE_CUSTOMER = 1;
    private static final Integer PARTY_TYPE_SUPPLIER = 2;
    private static final Integer STOCK_LOCK_STATUS_ACTIVE = 1;

    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpPurchaseInvoiceMapper purchaseInvoiceMapper;
    @Resource
    private ErpPurchaseInvoiceItemMapper purchaseInvoiceItemMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Resource
    private ErpPurchasePriceAdjustItemMapper purchasePriceAdjustItemMapper;
    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Resource
    private ErpSalePriceAdjustItemMapper salePriceAdjustItemMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpStockLockMapper stockLockMapper;
    @Resource
    private ErpStockInMapper stockInMapper;
    @Resource
    private ErpStockInItemMapper stockInItemMapper;
    @Resource
    private ErpStockOutMapper stockOutMapper;
    @Resource
    private ErpStockOutItemMapper stockOutItemMapper;
    @Resource
    private ErpStockMoveMapper stockMoveMapper;
    @Resource
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private ErpStockCheckMapper stockCheckMapper;
    @Resource
    private ErpStockCheckItemMapper stockCheckItemMapper;
    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinanceTransferMapper financeTransferMapper;
    @Resource
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Resource
    private ErpReceivableWriteOffMapper receivableWriteOffMapper;
    @Resource
    private ErpReceivableOtherIncomeMapper receivableOtherIncomeMapper;
    @Resource
    private ErpPayableOtherMapper payableOtherMapper;
    @Resource
    private ErpPayableWriteOffMapper payableWriteOffMapper;
    @Resource
    private ErpPayableExpenseMapper payableExpenseMapper;
    @Resource
    private ErpPreReceiptMapper preReceiptMapper;
    @Resource
    private ErpPreReceivableMapper preReceivableMapper;
    @Resource
    private ErpPrePaymentMapper prePaymentMapper;
    @Resource
    private ErpOtherReceivableMapper otherReceivableMapper;
    @Resource
    private ErpOtherPayableMapper otherPayableMapper;

    public void validateProductNotReferenced(Long productId) {
        List<String> references = new ArrayList<>();

        ErpPurchaseOrderItemDO purchaseOrderItem = purchaseOrderItemMapper.selectFirstOne(
                ErpPurchaseOrderItemDO::getProductId, productId);
        addReference(references, "采购订单",
                getNoById(purchaseOrderMapper::selectById,
                        ErpPurchaseOrderDO::getNo, getId(purchaseOrderItem, ErpPurchaseOrderItemDO::getOrderId)));
        ErpPurchaseInItemDO purchaseInItem = purchaseInItemMapper.selectFirstOne(ErpPurchaseInItemDO::getProductId, productId);
        addReference(references, "采购入库单",
                getNoById(purchaseInMapper::selectById,
                        ErpPurchaseInDO::getNo, getId(purchaseInItem, ErpPurchaseInItemDO::getInId)));
        ErpPurchaseReturnItemDO purchaseReturnItem = purchaseReturnItemMapper.selectFirstOne(
                ErpPurchaseReturnItemDO::getProductId, productId);
        addReference(references, "采购退货单",
                getNoById(purchaseReturnMapper::selectById,
                        ErpPurchaseReturnDO::getNo, getId(purchaseReturnItem, ErpPurchaseReturnItemDO::getReturnId)));
        ErpPurchaseInvoiceItemDO purchaseInvoiceItem = purchaseInvoiceItemMapper.selectFirstOne(
                ErpPurchaseInvoiceItemDO::getProductId, productId);
        addReference(references, "采购票据",
                getNoById(purchaseInvoiceMapper::selectById,
                        ErpPurchaseInvoiceDO::getNo, getId(purchaseInvoiceItem, ErpPurchaseInvoiceItemDO::getInvoiceId)));
        ErpPurchasePriceAdjustItemDO purchasePriceAdjustItem = purchasePriceAdjustItemMapper.selectFirstOne(
                ErpPurchasePriceAdjustItemDO::getProductId, productId);
        addReference(references, "采购调价单",
                getNoById(purchasePriceAdjustMapper::selectById,
                        ErpPurchasePriceAdjustDO::getNo, getId(purchasePriceAdjustItem, ErpPurchasePriceAdjustItemDO::getAdjustId)));

        ErpSaleQuoteItemDO saleQuoteItem = saleQuoteItemMapper.selectFirstOne(ErpSaleQuoteItemDO::getProductId, productId);
        addReference(references, "销售报价单",
                getNoById(saleQuoteMapper::selectById,
                        ErpSaleQuoteDO::getNo, getId(saleQuoteItem, ErpSaleQuoteItemDO::getQuoteId)));
        ErpSaleCartItemDO saleCartItem = saleCartItemMapper.selectFirstOne(ErpSaleCartItemDO::getProductId, productId);
        addReference(references, "销售手推车",
                getNoById(saleCartMapper::selectById,
                        ErpSaleCartDO::getNo, getId(saleCartItem, ErpSaleCartItemDO::getCartId)));
        ErpSaleOrderItemDO saleOrderItem = saleOrderItemMapper.selectFirstOne(ErpSaleOrderItemDO::getProductId, productId);
        addReference(references, "销售订单",
                getNoById(saleOrderMapper::selectById,
                        ErpSaleOrderDO::getNo, getId(saleOrderItem, ErpSaleOrderItemDO::getOrderId)));
        ErpSaleOutItemDO saleOutItem = saleOutItemMapper.selectFirstOne(ErpSaleOutItemDO::getProductId, productId);
        addReference(references, "销售出库单",
                getNoById(saleOutMapper::selectById,
                        ErpSaleOutDO::getNo, getId(saleOutItem, ErpSaleOutItemDO::getOutId)));
        ErpSaleReturnItemDO saleReturnItem = saleReturnItemMapper.selectFirstOne(ErpSaleReturnItemDO::getProductId, productId);
        addReference(references, "销售退货单",
                getNoById(saleReturnMapper::selectById,
                        ErpSaleReturnDO::getNo, getId(saleReturnItem, ErpSaleReturnItemDO::getReturnId)));
        ErpSalePriceAdjustItemDO salePriceAdjustItem = salePriceAdjustItemMapper.selectFirstOne(
                ErpSalePriceAdjustItemDO::getProductId, productId);
        addReference(references, "销售调价单",
                getNoById(salePriceAdjustMapper::selectById,
                        ErpSalePriceAdjustDO::getNo, getId(salePriceAdjustItem, ErpSalePriceAdjustItemDO::getAdjustId)));

        ErpStockInItemDO stockInItem = stockInItemMapper.selectFirstOne(ErpStockInItemDO::getProductId, productId);
        addReference(references, "其它入库单",
                getNoById(stockInMapper::selectById,
                        ErpStockInDO::getNo, getId(stockInItem, ErpStockInItemDO::getInId)));
        ErpStockOutItemDO stockOutItem = stockOutItemMapper.selectFirstOne(ErpStockOutItemDO::getProductId, productId);
        addReference(references, "其它出库单",
                getNoById(stockOutMapper::selectById,
                        ErpStockOutDO::getNo, getId(stockOutItem, ErpStockOutItemDO::getOutId)));
        ErpStockMoveItemDO stockMoveItem = stockMoveItemMapper.selectFirstOne(ErpStockMoveItemDO::getProductId, productId);
        addReference(references, "库存调拨单",
                getNoById(stockMoveMapper::selectById,
                        ErpStockMoveDO::getNo, getId(stockMoveItem, ErpStockMoveItemDO::getMoveId)));
        ErpStockCheckItemDO stockCheckItem = stockCheckItemMapper.selectFirstOne(ErpStockCheckItemDO::getProductId, productId);
        addReference(references, "库存盘点单",
                getNoById(stockCheckMapper::selectById,
                        ErpStockCheckDO::getNo, getId(stockCheckItem, ErpStockCheckItemDO::getCheckId)));
        ErpStockRecordDO stockRecord = stockRecordMapper.selectFirstOne(ErpStockRecordDO::getProductId, productId);
        addReference(references, "库存流水", stockRecord == null ? null : stockRecord.getBizNo());
        ErpStockLockDO stockLock = stockLockMapper.selectFirstOne(ErpStockLockDO::getProductId, productId,
                ErpStockLockDO::getStatus, STOCK_LOCK_STATUS_ACTIVE);
        addReference(references, "库存锁定", stockLock == null ? null : "ID " + stockLock.getId());

        throwIfReferenced(PRODUCT_DELETE_FAIL_REFERENCED, references);
    }

    public void validateSupplierNotReferenced(Long supplierId) {
        List<String> references = new ArrayList<>();

        addReference(references, "采购订单", firstNo(purchaseOrderMapper.selectFirstOne(ErpPurchaseOrderDO::getSupplierId, supplierId),
                ErpPurchaseOrderDO::getNo));
        addReference(references, "采购入库单", firstNo(purchaseInMapper.selectFirstOne(ErpPurchaseInDO::getSupplierId, supplierId),
                ErpPurchaseInDO::getNo));
        addReference(references, "采购退货单", firstNo(purchaseReturnMapper.selectFirstOne(ErpPurchaseReturnDO::getSupplierId, supplierId),
                ErpPurchaseReturnDO::getNo));
        addReference(references, "采购票据", firstNo(purchaseInvoiceMapper.selectFirstOne(ErpPurchaseInvoiceDO::getSupplierId, supplierId),
                ErpPurchaseInvoiceDO::getNo));
        addReference(references, "采购调价单", firstNo(purchasePriceAdjustMapper.selectFirstOne(
                ErpPurchasePriceAdjustDO::getSupplierId, supplierId), ErpPurchasePriceAdjustDO::getNo));
        addReference(references, "其它入库单", firstNo(stockInMapper.selectFirstOne(ErpStockInDO::getSupplierId, supplierId),
                ErpStockInDO::getNo));
        addReference(references, "付款单", firstNo(financePaymentMapper.selectFirstOne(ErpFinancePaymentDO::getSupplierId, supplierId),
                ErpFinancePaymentDO::getNo));
        addReference(references, "其它应付单", firstNo(payableOtherMapper.selectFirstOne(ErpPayableOtherDO::getSupplierId, supplierId),
                ErpPayableOtherDO::getNo));
        ErpPayableWriteOffDO writeOff = payableWriteOffMapper.selectFirstOne(ErpPayableWriteOffDO::getSupplierId, supplierId);
        addReference(references, "应付核销", writeOff == null ? null : "ID " + writeOff.getId());
        addReference(references, "预付款单", firstNo(prePaymentMapper.selectFirstOne(ErpPrePaymentDO::getPartyType, PARTY_TYPE_SUPPLIER,
                ErpPrePaymentDO::getPartyId, supplierId), ErpPrePaymentDO::getNo));
        addReference(references, "其它应付账款单", firstNo(otherPayableMapper.selectFirstOne(
                ErpOtherPayableDO::getPartyType, PARTY_TYPE_SUPPLIER, ErpOtherPayableDO::getPartyId, supplierId),
                ErpOtherPayableDO::getNo));

        throwIfReferenced(SUPPLIER_DELETE_FAIL_REFERENCED, references);
    }

    public void validateCustomerNotReferenced(Long customerId) {
        List<String> references = new ArrayList<>();

        addReference(references, "销售报价单", firstNo(saleQuoteMapper.selectFirstOne(ErpSaleQuoteDO::getCustomerId, customerId),
                ErpSaleQuoteDO::getNo));
        addReference(references, "销售手推车", firstNo(saleCartMapper.selectFirstOne(ErpSaleCartDO::getCustomerId, customerId),
                ErpSaleCartDO::getNo));
        addReference(references, "销售订单", firstNo(saleOrderMapper.selectFirstOne(ErpSaleOrderDO::getCustomerId, customerId),
                ErpSaleOrderDO::getNo));
        addReference(references, "销售出库单", firstNo(saleOutMapper.selectFirstOne(ErpSaleOutDO::getCustomerId, customerId),
                ErpSaleOutDO::getNo));
        addReference(references, "销售退货单", firstNo(saleReturnMapper.selectFirstOne(ErpSaleReturnDO::getCustomerId, customerId),
                ErpSaleReturnDO::getNo));
        addReference(references, "销售调价单", firstNo(salePriceAdjustMapper.selectFirstOne(
                ErpSalePriceAdjustDO::getCustomerId, customerId), ErpSalePriceAdjustDO::getNo));
        addReference(references, "其它出库单", firstNo(stockOutMapper.selectFirstOne(ErpStockOutDO::getCustomerId, customerId),
                ErpStockOutDO::getNo));
        addReference(references, "收款单", firstNo(financeReceiptMapper.selectFirstOne(ErpFinanceReceiptDO::getCustomerId, customerId),
                ErpFinanceReceiptDO::getNo));
        addReference(references, "其它应收单", firstNo(receivableOtherMapper.selectFirstOne(ErpReceivableOtherDO::getCustomerId, customerId),
                ErpReceivableOtherDO::getNo));
        ErpReceivableWriteOffDO writeOff = receivableWriteOffMapper.selectFirstOne(ErpReceivableWriteOffDO::getCustomerId, customerId);
        addReference(references, "应收核销", writeOff == null ? null : "ID " + writeOff.getId());
        addReference(references, "预收款单", firstNo(preReceiptMapper.selectFirstOne(ErpPreReceiptDO::getPartyType, PARTY_TYPE_CUSTOMER,
                ErpPreReceiptDO::getPartyId, customerId), ErpPreReceiptDO::getNo));
        addReference(references, "预收账款单", firstNo(preReceivableMapper.selectFirstOne(
                ErpPreReceivableDO::getPartyType, PARTY_TYPE_CUSTOMER, ErpPreReceivableDO::getPartyId, customerId),
                ErpPreReceivableDO::getNo));
        addReference(references, "其它应收账款单", firstNo(otherReceivableMapper.selectFirstOne(
                ErpOtherReceivableDO::getPartyType, PARTY_TYPE_CUSTOMER, ErpOtherReceivableDO::getPartyId, customerId),
                ErpOtherReceivableDO::getNo));

        throwIfReferenced(CUSTOMER_DELETE_FAIL_REFERENCED, references);
    }

    public void validateWarehouseNotReferenced(Long warehouseId) {
        List<String> references = new ArrayList<>();

        ErpProductDO product = productMapper.selectFirstOne(ErpProductDO::getDefaultWarehouseId, warehouseId);
        addReference(references, "产品默认仓库", product == null ? null : product.getName());
        ErpStockDO stock = stockMapper.selectFirstOne(ErpStockDO::getWarehouseId, warehouseId);
        addReference(references, "库存余额", stock == null ? null : "ID " + stock.getId());
        ErpStockRecordDO stockRecord = stockRecordMapper.selectFirstOne(ErpStockRecordDO::getWarehouseId, warehouseId);
        addReference(references, "库存流水", stockRecord == null ? null : stockRecord.getBizNo());
        ErpStockLockDO stockLock = stockLockMapper.selectFirstOne(ErpStockLockDO::getWarehouseId, warehouseId,
                ErpStockLockDO::getStatus, STOCK_LOCK_STATUS_ACTIVE);
        addReference(references, "库存锁定", stockLock == null ? null : "ID " + stockLock.getId());

        addWarehouseItemReference(references, "采购订单", purchaseOrderItemMapper.selectFirstOne(
                ErpPurchaseOrderItemDO::getWarehouseId, warehouseId), ErpPurchaseOrderItemDO::getOrderId,
                purchaseOrderMapper::selectById, ErpPurchaseOrderDO::getNo);
        addWarehouseItemReference(references, "采购入库单", purchaseInItemMapper.selectFirstOne(
                ErpPurchaseInItemDO::getWarehouseId, warehouseId), ErpPurchaseInItemDO::getInId,
                purchaseInMapper::selectById, ErpPurchaseInDO::getNo);
        addWarehouseItemReference(references, "采购退货单", purchaseReturnItemMapper.selectFirstOne(
                ErpPurchaseReturnItemDO::getWarehouseId, warehouseId), ErpPurchaseReturnItemDO::getReturnId,
                purchaseReturnMapper::selectById, ErpPurchaseReturnDO::getNo);
        addWarehouseItemReference(references, "采购调价单", purchasePriceAdjustItemMapper.selectFirstOne(
                ErpPurchasePriceAdjustItemDO::getWarehouseId, warehouseId), ErpPurchasePriceAdjustItemDO::getAdjustId,
                purchasePriceAdjustMapper::selectById, ErpPurchasePriceAdjustDO::getNo);
        addWarehouseItemReference(references, "销售报价单", saleQuoteItemMapper.selectFirstOne(
                ErpSaleQuoteItemDO::getWarehouseId, warehouseId), ErpSaleQuoteItemDO::getQuoteId,
                saleQuoteMapper::selectById, ErpSaleQuoteDO::getNo);
        addWarehouseItemReference(references, "销售手推车", saleCartItemMapper.selectFirstOne(
                ErpSaleCartItemDO::getWarehouseId, warehouseId), ErpSaleCartItemDO::getCartId,
                saleCartMapper::selectById, ErpSaleCartDO::getNo);
        addWarehouseItemReference(references, "销售出库单", saleOutItemMapper.selectFirstOne(
                ErpSaleOutItemDO::getWarehouseId, warehouseId), ErpSaleOutItemDO::getOutId,
                saleOutMapper::selectById, ErpSaleOutDO::getNo);
        addWarehouseItemReference(references, "销售退货单", saleReturnItemMapper.selectFirstOne(
                ErpSaleReturnItemDO::getWarehouseId, warehouseId), ErpSaleReturnItemDO::getReturnId,
                saleReturnMapper::selectById, ErpSaleReturnDO::getNo);
        addWarehouseItemReference(references, "其它入库单", stockInItemMapper.selectFirstOne(
                ErpStockInItemDO::getWarehouseId, warehouseId), ErpStockInItemDO::getInId,
                stockInMapper::selectById, ErpStockInDO::getNo);
        addWarehouseItemReference(references, "其它出库单", stockOutItemMapper.selectFirstOne(
                ErpStockOutItemDO::getWarehouseId, warehouseId), ErpStockOutItemDO::getOutId,
                stockOutMapper::selectById, ErpStockOutDO::getNo);
        addWarehouseItemReference(references, "库存调拨单(调出)", stockMoveItemMapper.selectFirstOne(
                ErpStockMoveItemDO::getFromWarehouseId, warehouseId), ErpStockMoveItemDO::getMoveId,
                stockMoveMapper::selectById, ErpStockMoveDO::getNo);
        addWarehouseItemReference(references, "库存调拨单(调入)", stockMoveItemMapper.selectFirstOne(
                ErpStockMoveItemDO::getToWarehouseId, warehouseId), ErpStockMoveItemDO::getMoveId,
                stockMoveMapper::selectById, ErpStockMoveDO::getNo);
        addWarehouseItemReference(references, "库存盘点单", stockCheckItemMapper.selectFirstOne(
                ErpStockCheckItemDO::getWarehouseId, warehouseId), ErpStockCheckItemDO::getCheckId,
                stockCheckMapper::selectById, ErpStockCheckDO::getNo);

        throwIfReferenced(WAREHOUSE_DELETE_FAIL_REFERENCED, references);
    }

    public void validateAccountNotReferenced(Long accountId) {
        List<String> references = new ArrayList<>();

        addReference(references, "采购订单", firstNo(purchaseOrderMapper.selectFirstOne(ErpPurchaseOrderDO::getAccountId, accountId),
                ErpPurchaseOrderDO::getNo));
        addReference(references, "采购入库单", firstNo(purchaseInMapper.selectFirstOne(ErpPurchaseInDO::getAccountId, accountId),
                ErpPurchaseInDO::getNo));
        addReference(references, "采购退货单", firstNo(purchaseReturnMapper.selectFirstOne(ErpPurchaseReturnDO::getAccountId, accountId),
                ErpPurchaseReturnDO::getNo));
        addReference(references, "销售手推车", firstNo(saleCartMapper.selectFirstOne(ErpSaleCartDO::getAccountId, accountId),
                ErpSaleCartDO::getNo));
        addReference(references, "销售订单", firstNo(saleOrderMapper.selectFirstOne(ErpSaleOrderDO::getAccountId, accountId),
                ErpSaleOrderDO::getNo));
        addReference(references, "销售出库单", firstNo(saleOutMapper.selectFirstOne(ErpSaleOutDO::getAccountId, accountId),
                ErpSaleOutDO::getNo));
        addReference(references, "销售退货单", firstNo(saleReturnMapper.selectFirstOne(ErpSaleReturnDO::getAccountId, accountId),
                ErpSaleReturnDO::getNo));
        addReference(references, "收款单", firstNo(financeReceiptMapper.selectFirstOne(ErpFinanceReceiptDO::getAccountId, accountId),
                ErpFinanceReceiptDO::getNo));
        addReference(references, "付款单", firstNo(financePaymentMapper.selectFirstOne(ErpFinancePaymentDO::getAccountId, accountId),
                ErpFinancePaymentDO::getNo));
        addReference(references, "银行转账单(转出)", firstNo(financeTransferMapper.selectFirstOne(
                ErpFinanceTransferDO::getOutAccountId, accountId), ErpFinanceTransferDO::getNo));
        addReference(references, "银行转账单(转入)", firstNo(financeTransferMapper.selectFirstOne(
                ErpFinanceTransferDO::getInAccountId, accountId), ErpFinanceTransferDO::getNo));
        addReference(references, "其它收入单", firstNo(receivableOtherIncomeMapper.selectFirstOne(
                ErpReceivableOtherIncomeDO::getAccountId, accountId), ErpReceivableOtherIncomeDO::getNo));
        addReference(references, "费用支出单", firstNo(payableExpenseMapper.selectFirstOne(
                ErpPayableExpenseDO::getAccountId, accountId), ErpPayableExpenseDO::getNo));
        addReference(references, "预收款单", firstNo(preReceiptMapper.selectFirstOne(ErpPreReceiptDO::getAccountId, accountId),
                ErpPreReceiptDO::getNo));
        addReference(references, "预收账款单", firstNo(preReceivableMapper.selectFirstOne(ErpPreReceivableDO::getAccountId, accountId),
                ErpPreReceivableDO::getNo));
        addReference(references, "预付款单", firstNo(prePaymentMapper.selectFirstOne(ErpPrePaymentDO::getAccountId, accountId),
                ErpPrePaymentDO::getNo));
        addReference(references, "其它应收账款单", firstNo(otherReceivableMapper.selectFirstOne(
                ErpOtherReceivableDO::getAccountId, accountId), ErpOtherReceivableDO::getNo));
        addReference(references, "其它应付账款单", firstNo(otherPayableMapper.selectFirstOne(
                ErpOtherPayableDO::getAccountId, accountId), ErpOtherPayableDO::getNo));

        throwIfReferenced(ACCOUNT_DELETE_FAIL_REFERENCED, references);
    }

    private <T, D> void addWarehouseItemReference(List<String> references, String bizName, T item,
                                                 Function<T, Long> documentIdGetter,
                                                 Function<Long, D> documentGetter,
                                                 Function<D, String> noGetter) {
        Long documentId = getId(item, documentIdGetter);
        addReference(references, bizName, getNoById(documentGetter, noGetter, documentId));
    }

    private <T> Long getId(T item, Function<T, Long> idGetter) {
        return item == null ? null : idGetter.apply(item);
    }

    private <T> String firstNo(T document, Function<T, String> noGetter) {
        return document == null ? null : noGetter.apply(document);
    }

    private <T> String getNoById(Function<Long, T> documentGetter, Function<T, String> noGetter, Long fallbackId) {
        return fallbackId == null ? null : getNo(documentGetter.apply(fallbackId), noGetter, fallbackId);
    }

    private <T> String getNo(T document, Function<T, String> noGetter, Long fallbackId) {
        if (document == null) {
            return fallbackId == null ? null : "ID " + fallbackId;
        }
        String no = noGetter.apply(document);
        return StringUtils.hasText(no) ? no : "ID " + fallbackId;
    }

    private void addReference(List<String> references, String bizName, String no) {
        if (!StringUtils.hasText(no)) {
            return;
        }
        references.add(bizName + " " + no);
    }

    private void throwIfReferenced(ErrorCode errorCode, List<String> references) {
        if (!references.isEmpty()) {
            throw exception(errorCode, "以下业务数据：" + String.join("、", references));
        }
    }

}
