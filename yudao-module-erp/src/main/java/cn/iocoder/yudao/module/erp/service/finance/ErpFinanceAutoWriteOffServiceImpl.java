package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpAutoWriteOffConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_FINANCE_PAYMENT_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_FINANCE_RECEIPT_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_UPDATE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.service.common.ErpFinanceAmountUtils.normalize;

/**
 * ERP 财务自动核销 Service 实现类
 */
@Service
@Validated
public class ErpFinanceAutoWriteOffServiceImpl implements ErpFinanceAutoWriteOffService {

    private static final String AUTO_WRITE_OFF_REMARK = "系统自动核销";

    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Resource
    private ErpReceivableMiscMapper receivableMiscMapper;
    @Resource
    private ErpPayableMiscMapper payableMiscMapper;

    @Resource
    private ErpAutoWriteOffConfigService autoWriteOffConfigService;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSaleReturnService saleReturnService;
    @Resource
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpPurchasePriceAdjustService purchasePriceAdjustService;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void autoWriteOffReceipt(Long receiptId, Long userId) {
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectByIdForUpdate(receiptId);
        if (receipt == null) {
            throw exception(FINANCE_RECEIPT_NOT_EXISTS);
        }
        if (!ErpAuditStatus.APPROVE.getStatus().equals(receipt.getStatus())) {
            return;
        }
        if (autoWriteOffConfigService.isAutoWriteOffDisabled(receipt.getDeptId())) {
            return;
        }

        BigDecimal currentAllocatedPrice = normalize(financeReceiptItemMapper.selectEffectivePriceSumMapByReceiptIds(
                Collections.singleton(receipt.getId())).getOrDefault(receipt.getId(), BigDecimal.ZERO));
        BigDecimal receiptTotalPrice = normalize(getZeroIfNull(receipt.getTotalPrice()));
        if (!isValidReceiptAllocatedPrice(receiptTotalPrice, currentAllocatedPrice)) {
            return;
        }

        List<ReceiptWriteOffCandidate> candidates = buildReceiptCandidates(receipt);
        if (CollUtil.isEmpty(candidates)) {
            return;
        }

        List<ErpFinanceReceiptItemDO> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal allocatedPrice = currentAllocatedPrice;
        for (ReceiptWriteOffCandidate candidate : candidates) {
            ReceiptBizSnapshot biz = lockReceiptBiz(candidate.bizType, candidate.bizId, receipt);
            if (biz == null) {
                continue;
            }
            BigDecimal alreadyReceiptedPrice = normalize(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                    candidate.bizId, candidate.bizType));
            BigDecimal writeOffAmount = normalize(biz.totalPrice.subtract(alreadyReceiptedPrice));
            if (writeOffAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal actualWriteOffAmount = writeOffAmount;
            BigDecimal newAllocatedPrice = normalize(allocatedPrice.add(actualWriteOffAmount));
            if (!isValidReceiptAllocatedPrice(receiptTotalPrice, newAllocatedPrice)) {
                if (writeOffAmount.signum() == receiptTotalPrice.signum()) {
                    actualWriteOffAmount = calculateRemainingWriteOffAmount(receiptTotalPrice, allocatedPrice);
                    if (actualWriteOffAmount.compareTo(BigDecimal.ZERO) == 0) {
                        break;
                    }
                    newAllocatedPrice = normalize(allocatedPrice.add(actualWriteOffAmount));
                    if (!isValidReceiptAllocatedPrice(receiptTotalPrice, newAllocatedPrice)) {
                        break;
                    }
                } else {
                    continue;
                }
            }
            items.add(new ErpFinanceReceiptItemDO().setReceiptId(receipt.getId())
                    .setBizType(candidate.bizType).setBizId(candidate.bizId).setBizNo(biz.bizNo)
                    .setTotalPrice(biz.totalPrice).setReceiptedPrice(alreadyReceiptedPrice)
                    .setReceiptPrice(actualWriteOffAmount).setRemark(AUTO_WRITE_OFF_REMARK)
                    .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                    .setWriteOffTime(now).setWriteOffUserId(userId));
            allocatedPrice = newAllocatedPrice;
            if (actualWriteOffAmount.compareTo(writeOffAmount) != 0) {
                break;
            }
        }

        if (CollUtil.isEmpty(items)) {
            return;
        }
        financeReceiptItemMapper.insertBatch(items);
        updateSalePrice(items);
        operateLogService.record(ERP_FINANCE_RECEIPT_TYPE, ERP_UPDATE_SUB_TYPE, receipt.getId(),
                "收款单自动核销，单据编号：" + receipt.getNo() + "，本次核销："
                        + items.stream().map(ErpFinanceReceiptItemDO::getReceiptPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), receipt.getNo());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void autoWriteOffPayment(Long paymentId, Long userId) {
        ErpFinancePaymentDO payment = financePaymentMapper.selectByIdForUpdate(paymentId);
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        if (!ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
            return;
        }
        if (autoWriteOffConfigService.isAutoWriteOffDisabled(payment.getDeptId())) {
            return;
        }

        BigDecimal currentAllocatedPrice = normalize(financePaymentItemMapper.selectEffectivePriceSumMapByPaymentIds(
                Collections.singleton(payment.getId())).getOrDefault(payment.getId(), BigDecimal.ZERO));
        BigDecimal paymentTotalPrice = normalize(getZeroIfNull(payment.getTotalPrice()));
        if (!isValidAllocatedPrice(paymentTotalPrice, currentAllocatedPrice)) {
            return;
        }

        List<PaymentWriteOffCandidate> candidates = buildPaymentCandidates(payment);
        if (CollUtil.isEmpty(candidates)) {
            return;
        }

        List<ErpFinancePaymentItemDO> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal allocatedPrice = currentAllocatedPrice;
        for (PaymentWriteOffCandidate candidate : candidates) {
            PaymentBizSnapshot biz = lockPaymentBiz(candidate.bizType, candidate.bizId, payment);
            if (biz == null) {
                continue;
            }
            BigDecimal alreadyPaidPrice = normalize(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                    candidate.bizId, candidate.bizType));
            BigDecimal writeOffAmount = normalize(biz.totalPrice.subtract(alreadyPaidPrice));
            if (writeOffAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal actualWriteOffAmount = writeOffAmount;
            BigDecimal newAllocatedPrice = normalize(allocatedPrice.add(actualWriteOffAmount));
            if (!isValidAllocatedPrice(paymentTotalPrice, newAllocatedPrice)) {
                if (writeOffAmount.signum() == paymentTotalPrice.signum()) {
                    actualWriteOffAmount = calculateRemainingWriteOffAmount(paymentTotalPrice, allocatedPrice);
                    if (actualWriteOffAmount.compareTo(BigDecimal.ZERO) == 0) {
                        break;
                    }
                    newAllocatedPrice = normalize(allocatedPrice.add(actualWriteOffAmount));
                    if (!isValidAllocatedPrice(paymentTotalPrice, newAllocatedPrice)) {
                        break;
                    }
                } else {
                    continue;
                }
            }
            items.add(new ErpFinancePaymentItemDO().setPaymentId(payment.getId())
                    .setBizType(candidate.bizType).setBizId(candidate.bizId).setBizNo(biz.bizNo)
                    .setTotalPrice(biz.totalPrice).setPaidPrice(alreadyPaidPrice)
                    .setPaymentPrice(actualWriteOffAmount).setRemark(AUTO_WRITE_OFF_REMARK)
                    .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                    .setWriteOffTime(now).setWriteOffUserId(userId));
            allocatedPrice = newAllocatedPrice;
            if (actualWriteOffAmount.compareTo(writeOffAmount) != 0) {
                break;
            }
        }

        if (CollUtil.isEmpty(items)) {
            return;
        }
        financePaymentItemMapper.insertBatch(items);
        updatePurchasePrice(items);
        operateLogService.record(ERP_FINANCE_PAYMENT_TYPE, ERP_UPDATE_SUB_TYPE, payment.getId(),
                "付款单自动核销，单据编号：" + payment.getNo() + "，本次核销："
                        + items.stream().map(ErpFinancePaymentItemDO::getPaymentPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), payment.getNo());
    }

    private List<ReceiptWriteOffCandidate> buildReceiptCandidates(ErpFinanceReceiptDO receipt) {
        List<ReceiptWriteOffCandidate> result = new ArrayList<>();
        addSaleOutCandidates(result, receipt);
        addSaleReturnCandidates(result, receipt);
        addSalePriceAdjustCandidates(result, receipt);
        addReceivableMiscCandidates(result, receipt);
        result.sort(Comparator.comparing(ReceiptWriteOffCandidate::getBizTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ReceiptWriteOffCandidate::getBizNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ReceiptWriteOffCandidate::getBizId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private void addSaleOutCandidates(List<ReceiptWriteOffCandidate> result, ErpFinanceReceiptDO receipt) {
        LambdaQueryWrapperX<ErpSaleOutDO> query = new LambdaQueryWrapperX<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getCustomerId, receipt.getCustomerId())
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpSaleOutDO::getDeptId, receipt.getDeptId());
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectList(query);
        if (CollUtil.isEmpty(saleOuts)) {
            return;
        }
        Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap = convertMultiMap(saleOutService.getSaleOutItemListByOutIds(
                convertSet(saleOuts, ErpSaleOutDO::getId)), ErpSaleOutItemDO::getOutId);
        Map<Long, BigDecimal> allocatedMap = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(saleOuts, ErpSaleOutDO::getId), ErpBizTypeEnum.SALE_OUT.getType());
        saleOuts.forEach(row -> addCandidate(result, ErpBizTypeEnum.SALE_OUT.getType(), row.getId(), row.getNo(),
                row.getOutTime(), ErpOriginalSettlementAmountUtils.calculateSaleOut(row,
                        saleOutItemMap.getOrDefault(row.getId(), Collections.emptyList())),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private void addSaleReturnCandidates(List<ReceiptWriteOffCandidate> result, ErpFinanceReceiptDO receipt) {
        LambdaQueryWrapperX<ErpSaleReturnDO> query = new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getCustomerId, receipt.getCustomerId())
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpSaleReturnDO::getDeptId, receipt.getDeptId());
        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectList(query);
        if (CollUtil.isEmpty(saleReturns)) {
            return;
        }
        Map<Long, BigDecimal> allocatedMap = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(saleReturns, ErpSaleReturnDO::getId), ErpBizTypeEnum.SALE_RETURN.getType());
        saleReturns.forEach(row -> addCandidate(result, ErpBizTypeEnum.SALE_RETURN.getType(), row.getId(), row.getNo(),
                row.getReturnTime(), getZeroIfNull(row.getTotalPrice()).negate(),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private void addSalePriceAdjustCandidates(List<ReceiptWriteOffCandidate> result, ErpFinanceReceiptDO receipt) {
        LambdaQueryWrapperX<ErpSalePriceAdjustDO> query = new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getCustomerId, receipt.getCustomerId())
                .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpSalePriceAdjustDO::getDeptId, receipt.getDeptId());
        List<ErpSalePriceAdjustDO> adjusts = salePriceAdjustMapper.selectList(query);
        if (CollUtil.isEmpty(adjusts)) {
            return;
        }
        Map<Long, BigDecimal> allocatedMap = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(adjusts, ErpSalePriceAdjustDO::getId), ErpBizTypeEnum.SALE_PRICE_ADJUST.getType());
        adjusts.forEach(row -> addCandidate(result, ErpBizTypeEnum.SALE_PRICE_ADJUST.getType(), row.getId(),
                row.getNo(), row.getAdjustDate(), getZeroIfNull(row.getTotalAdjustPrice()),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private void addReceivableMiscCandidates(List<ReceiptWriteOffCandidate> result, ErpFinanceReceiptDO receipt) {
        LambdaQueryWrapperX<ErpReceivableMiscDO> query = new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getCustomerId, receipt.getCustomerId())
                .eq(ErpReceivableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpReceivableMiscDO::getDeptId, receipt.getDeptId());
        List<ErpReceivableMiscDO> rows = receivableMiscMapper.selectList(query);
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, BigDecimal> allocatedMap = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(rows, ErpReceivableMiscDO::getId), ErpBizTypeEnum.RECEIVABLE_MISC.getType());
        rows.forEach(row -> addCandidate(result, ErpBizTypeEnum.RECEIVABLE_MISC.getType(), row.getId(),
                row.getNo(), row.getBizTime(), getZeroIfNull(row.getAmount()),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private <T> void appendDeptCondition(LambdaQueryWrapperX<T> query,
                                         com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column,
                                         Long deptId) {
        if (deptId == null) {
            query.isNull(column);
        } else {
            query.eq(column, deptId);
        }
    }

    private void addCandidate(List<ReceiptWriteOffCandidate> result, Integer bizType, Long bizId, String bizNo,
                              LocalDateTime bizTime, BigDecimal totalPrice, BigDecimal allocatedPrice) {
        BigDecimal unallocatedPrice = normalize(getZeroIfNull(totalPrice).subtract(getZeroIfNull(allocatedPrice)));
        if (unallocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        result.add(new ReceiptWriteOffCandidate(bizType, bizId, bizNo, bizTime));
    }

    private ReceiptBizSnapshot lockReceiptBiz(Integer bizType, Long bizId, ErpFinanceReceiptDO receipt) {
        ReceiptBizSnapshot result;
        if (ObjectUtil.equal(bizType, ErpBizTypeEnum.SALE_OUT.getType())) {
            ErpSaleOutDO row = saleOutMapper.selectOne(new LambdaQueryWrapperX<ErpSaleOutDO>()
                    .eq(ErpSaleOutDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new ReceiptBizSnapshot(row.getCustomerId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), ErpOriginalSettlementAmountUtils.calculateSaleOut(row,
                    saleOutService.getSaleOutItemListByOutIds(Collections.singleton(bizId))));
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.SALE_RETURN.getType())) {
            ErpSaleReturnDO row = saleReturnMapper.selectOne(new LambdaQueryWrapperX<ErpSaleReturnDO>()
                    .eq(ErpSaleReturnDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new ReceiptBizSnapshot(row.getCustomerId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalPrice()).negate());
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.SALE_PRICE_ADJUST.getType())) {
            ErpSalePriceAdjustDO row = salePriceAdjustMapper.selectOne(new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                    .eq(ErpSalePriceAdjustDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new ReceiptBizSnapshot(row.getCustomerId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalAdjustPrice()));
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.RECEIVABLE_MISC.getType())) {
            ErpReceivableMiscDO row = receivableMiscMapper.selectOne(new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                    .eq(ErpReceivableMiscDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new ReceiptBizSnapshot(row.getCustomerId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getAmount()));
        } else {
            return null;
        }
        if (result == null || !ErpAuditStatus.APPROVE.getStatus().equals(result.status)
                || !Objects.equals(result.partyId, receipt.getCustomerId())
                || !Objects.equals(result.deptId, receipt.getDeptId())) {
            return null;
        }
        return result;
    }

    private boolean isValidReceiptAllocatedPrice(BigDecimal receiptTotalPrice, BigDecimal allocatedPrice) {
        return isValidAllocatedPrice(receiptTotalPrice, allocatedPrice);
    }

    private boolean isValidAllocatedPrice(BigDecimal receiptTotalPrice, BigDecimal allocatedPrice) {
        BigDecimal totalPrice = normalize(getZeroIfNull(receiptTotalPrice));
        BigDecimal normalizedAllocatedPrice = normalize(getZeroIfNull(allocatedPrice));
        if (normalizedAllocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        return totalPrice.compareTo(BigDecimal.ZERO) != 0
                && normalizedAllocatedPrice.signum() == totalPrice.signum()
                && normalizedAllocatedPrice.abs().compareTo(totalPrice.abs()) <= 0;
    }

    private BigDecimal calculateRemainingWriteOffAmount(BigDecimal totalPrice, BigDecimal allocatedPrice) {
        BigDecimal normalizedTotalPrice = normalize(getZeroIfNull(totalPrice));
        BigDecimal normalizedAllocatedPrice = normalize(getZeroIfNull(allocatedPrice));
        BigDecimal remainingPrice = normalize(normalizedTotalPrice.subtract(normalizedAllocatedPrice));
        if (remainingPrice.compareTo(BigDecimal.ZERO) == 0
                || normalizedTotalPrice.compareTo(BigDecimal.ZERO) == 0
                || remainingPrice.signum() != normalizedTotalPrice.signum()) {
            return BigDecimal.ZERO;
        }
        return remainingPrice;
    }

    private void updateSalePrice(Collection<ErpFinanceReceiptItemDO> receiptItems) {
        receiptItems.forEach(receiptItem -> {
            BigDecimal totalReceiptPrice = financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                    receiptItem.getBizId(), receiptItem.getBizType());
            if (ErpBizTypeEnum.SALE_OUT.getType().equals(receiptItem.getBizType())) {
                saleOutService.updateSaleInReceiptPrice(receiptItem.getBizId(), totalReceiptPrice);
            } else if (ErpBizTypeEnum.SALE_RETURN.getType().equals(receiptItem.getBizType())) {
                saleReturnService.updateSaleReturnRefundPrice(receiptItem.getBizId(), totalReceiptPrice.negate());
            } else if (ErpBizTypeEnum.SALE_PRICE_ADJUST.getType().equals(receiptItem.getBizType())) {
                salePriceAdjustService.updateSalePriceAdjustReceiptPrice(receiptItem.getBizId(), totalReceiptPrice);
            } else if (ErpBizTypeEnum.RECEIVABLE_MISC.getType().equals(receiptItem.getBizType())) {
                // 其他应收的已收/未收金额通过收款明细动态汇总，不回写主单。
            }
        });
    }

    private List<PaymentWriteOffCandidate> buildPaymentCandidates(ErpFinancePaymentDO payment) {
        List<PaymentWriteOffCandidate> result = new ArrayList<>();
        addPurchaseInCandidates(result, payment);
        addPurchaseReturnCandidates(result, payment);
        addPurchasePriceAdjustCandidates(result, payment);
        addPayableMiscCandidates(result, payment);
        result.sort(Comparator.comparing(PaymentWriteOffCandidate::getBizTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PaymentWriteOffCandidate::getBizNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PaymentWriteOffCandidate::getBizId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private void addPurchaseInCandidates(List<PaymentWriteOffCandidate> result, ErpFinancePaymentDO payment) {
        LambdaQueryWrapperX<ErpPurchaseInDO> query = new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpPurchaseInDO::getDeptId, payment.getDeptId());
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(query);
        if (CollUtil.isEmpty(purchaseIns)) {
            return;
        }
        Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap = convertMultiMap(
                purchaseInService.getPurchaseInItemListByInIds(convertSet(purchaseIns, ErpPurchaseInDO::getId)),
                ErpPurchaseInItemDO::getInId);
        Map<Long, BigDecimal> allocatedMap = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(purchaseIns, ErpPurchaseInDO::getId), ErpBizTypeEnum.PURCHASE_IN.getType());
        purchaseIns.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PURCHASE_IN.getType(), row.getId(),
                row.getNo(), row.getInTime(), ErpOriginalSettlementAmountUtils.calculatePurchaseIn(row,
                        purchaseInItemMap.getOrDefault(row.getId(), Collections.emptyList())),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private void addPurchaseReturnCandidates(List<PaymentWriteOffCandidate> result, ErpFinancePaymentDO payment) {
        LambdaQueryWrapperX<ErpPurchaseReturnDO> query = new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpPurchaseReturnDO::getDeptId, payment.getDeptId());
        List<ErpPurchaseReturnDO> purchaseReturns = purchaseReturnMapper.selectList(query);
        if (CollUtil.isEmpty(purchaseReturns)) {
            return;
        }
        Map<Long, BigDecimal> allocatedMap = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(purchaseReturns, ErpPurchaseReturnDO::getId), ErpBizTypeEnum.PURCHASE_RETURN.getType());
        purchaseReturns.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PURCHASE_RETURN.getType(),
                row.getId(), row.getNo(), row.getReturnTime(), getZeroIfNull(row.getTotalPrice()).negate(),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private void addPurchasePriceAdjustCandidates(List<PaymentWriteOffCandidate> result,
                                                  ErpFinancePaymentDO payment) {
        LambdaQueryWrapperX<ErpPurchasePriceAdjustDO> query = new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpPurchasePriceAdjustDO::getDeptId, payment.getDeptId());
        List<ErpPurchasePriceAdjustDO> adjusts = purchasePriceAdjustMapper.selectList(query);
        if (CollUtil.isEmpty(adjusts)) {
            return;
        }
        Map<Long, BigDecimal> allocatedMap = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(adjusts, ErpPurchasePriceAdjustDO::getId),
                ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
        adjusts.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType(),
                row.getId(), row.getNo(), row.getAdjustTime(), getZeroIfNull(row.getTotalAdjustPrice()),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private void addPayableMiscCandidates(List<PaymentWriteOffCandidate> result, ErpFinancePaymentDO payment) {
        LambdaQueryWrapperX<ErpPayableMiscDO> query = new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPayableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        appendDeptCondition(query, ErpPayableMiscDO::getDeptId, payment.getDeptId());
        List<ErpPayableMiscDO> rows = payableMiscMapper.selectList(query);
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, BigDecimal> allocatedMap = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(rows, ErpPayableMiscDO::getId), ErpBizTypeEnum.PAYABLE_MISC.getType());
        rows.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PAYABLE_MISC.getType(), row.getId(),
                row.getNo(), row.getBizTime(), getZeroIfNull(row.getAmount()),
                allocatedMap.getOrDefault(row.getId(), BigDecimal.ZERO)));
    }

    private void addPaymentCandidate(List<PaymentWriteOffCandidate> result, Integer bizType, Long bizId, String bizNo,
                                     LocalDateTime bizTime, BigDecimal totalPrice, BigDecimal allocatedPrice) {
        BigDecimal unallocatedPrice = normalize(getZeroIfNull(totalPrice).subtract(getZeroIfNull(allocatedPrice)));
        if (unallocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        result.add(new PaymentWriteOffCandidate(bizType, bizId, bizNo, bizTime));
    }

    private PaymentBizSnapshot lockPaymentBiz(Integer bizType, Long bizId, ErpFinancePaymentDO payment) {
        PaymentBizSnapshot result;
        if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PURCHASE_IN.getType())) {
            ErpPurchaseInDO row = purchaseInMapper.selectOne(new LambdaQueryWrapperX<ErpPurchaseInDO>()
                    .eq(ErpPurchaseInDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), ErpOriginalSettlementAmountUtils.calculatePurchaseIn(row,
                    purchaseInService.getPurchaseInItemListByInIds(Collections.singleton(bizId))));
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PURCHASE_RETURN.getType())) {
            ErpPurchaseReturnDO row = purchaseReturnMapper.selectOne(new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                    .eq(ErpPurchaseReturnDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalPrice()).negate());
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType())) {
            ErpPurchasePriceAdjustDO row = purchasePriceAdjustMapper.selectOne(
                    new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                            .eq(ErpPurchasePriceAdjustDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalAdjustPrice()));
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PAYABLE_MISC.getType())) {
            ErpPayableMiscDO row = payableMiscMapper.selectOne(new LambdaQueryWrapperX<ErpPayableMiscDO>()
                    .eq(ErpPayableMiscDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getAmount()));
        } else {
            return null;
        }
        if (result == null || !ErpAuditStatus.APPROVE.getStatus().equals(result.status)
                || !Objects.equals(result.partyId, payment.getSupplierId())
                || !Objects.equals(result.deptId, payment.getDeptId())) {
            return null;
        }
        return result;
    }

    private void updatePurchasePrice(Collection<ErpFinancePaymentItemDO> paymentItems) {
        paymentItems.forEach(paymentItem -> {
            BigDecimal totalPaymentPrice = financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                    paymentItem.getBizId(), paymentItem.getBizType());
            if (ErpBizTypeEnum.PURCHASE_IN.getType().equals(paymentItem.getBizType())) {
                purchaseInService.updatePurchaseInPaymentPrice(paymentItem.getBizId(), totalPaymentPrice);
            } else if (ErpBizTypeEnum.PURCHASE_RETURN.getType().equals(paymentItem.getBizType())) {
                purchaseReturnService.updatePurchaseReturnRefundPrice(paymentItem.getBizId(), totalPaymentPrice.negate());
            } else if (ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType().equals(paymentItem.getBizType())) {
                purchasePriceAdjustService.updatePurchasePriceAdjustPaymentPrice(paymentItem.getBizId(), totalPaymentPrice);
            } else if (ErpBizTypeEnum.PAYABLE_MISC.getType().equals(paymentItem.getBizType())) {
                // 其他应付的已付/未付金额通过付款明细动态汇总，不回写主单。
            }
        });
    }

    private BigDecimal getZeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static final class ReceiptWriteOffCandidate {
        private final Integer bizType;
        private final Long bizId;
        private final String bizNo;
        private final LocalDateTime bizTime;

        private ReceiptWriteOffCandidate(Integer bizType, Long bizId, String bizNo, LocalDateTime bizTime) {
            this.bizType = bizType;
            this.bizId = bizId;
            this.bizNo = bizNo;
            this.bizTime = bizTime;
        }

        public Long getBizId() {
            return bizId;
        }

        public String getBizNo() {
            return bizNo;
        }

        public LocalDateTime getBizTime() {
            return bizTime;
        }
    }

    private static final class PaymentWriteOffCandidate {
        private final Integer bizType;
        private final Long bizId;
        private final String bizNo;
        private final LocalDateTime bizTime;

        private PaymentWriteOffCandidate(Integer bizType, Long bizId, String bizNo, LocalDateTime bizTime) {
            this.bizType = bizType;
            this.bizId = bizId;
            this.bizNo = bizNo;
            this.bizTime = bizTime;
        }

        public Long getBizId() {
            return bizId;
        }

        public String getBizNo() {
            return bizNo;
        }

        public LocalDateTime getBizTime() {
            return bizTime;
        }
    }

    private static final class ReceiptBizSnapshot {
        private final Long partyId;
        private final Long deptId;
        private final Integer status;
        private final String bizNo;
        private final BigDecimal totalPrice;

        private ReceiptBizSnapshot(Long partyId, Long deptId, Integer status, String bizNo, BigDecimal totalPrice) {
            this.partyId = partyId;
            this.deptId = deptId;
            this.status = status;
            this.bizNo = bizNo;
            this.totalPrice = totalPrice;
        }
    }

    private static final class PaymentBizSnapshot {
        private final Long partyId;
        private final Long deptId;
        private final Integer status;
        private final String bizNo;
        private final BigDecimal totalPrice;

        private PaymentBizSnapshot(Long partyId, Long deptId, Integer status, String bizNo, BigDecimal totalPrice) {
            this.partyId = partyId;
            this.deptId = deptId;
            this.status = status;
            this.bizNo = bizNo;
            this.totalPrice = totalPrice;
        }
    }

}
