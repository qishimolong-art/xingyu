package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 报价订单转手推车及实际出库进度汇总。 */
@Service
public class ErpSaleQuoteProgressService {

    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;

    public Map<Long, Progress> getProgressMap(Collection<Long> quoteIds) {
        if (CollUtil.isEmpty(quoteIds)) {
            return Collections.emptyMap();
        }
        List<Long> ids = quoteIds.stream().filter(java.util.Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ErpSaleQuoteDO> quoteMap = saleQuoteMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(ErpSaleQuoteDO::getId, quote -> quote));
        Map<Long, List<ErpSaleQuoteItemDO>> itemMap = saleQuoteItemMapper.selectListByQuoteIds(ids).stream()
                .collect(Collectors.groupingBy(ErpSaleQuoteItemDO::getQuoteId));
        List<ErpSaleConvertRecordDO> records = saleConvertRecordMapper.selectQuoteToCartListByQuoteIds(ids);
        Set<Long> cartIds = records.stream().map(ErpSaleConvertRecordDO::getTargetId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ErpSaleCartDO> cartMap = CollUtil.isEmpty(cartIds) ? Collections.emptyMap()
                : saleCartMapper.selectByIds(cartIds).stream()
                .collect(Collectors.toMap(ErpSaleCartDO::getId, cart -> cart));
        Map<Long, List<ErpSaleConvertRecordDO>> recordMap = records.stream()
                .filter(record -> cartMap.containsKey(record.getTargetId()))
                .collect(Collectors.groupingBy(ErpSaleConvertRecordDO::getSourceId));

        Map<Long, Progress> result = new LinkedHashMap<>();
        ids.forEach(id -> {
            ErpSaleQuoteDO quote = quoteMap.get(id);
            if (quote != null) {
                result.put(id, buildProgress(quote, itemMap.get(id), recordMap.get(id), cartMap));
            }
        });
        return result;
    }

    public Progress getProgress(Long quoteId) {
        return getProgressMap(Collections.singleton(quoteId)).get(quoteId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Progress refreshQuoteProgress(Long quoteId) {
        ErpSaleQuoteDO quote = saleQuoteMapper.selectByIdForUpdate(quoteId);
        if (quote == null) {
            return null;
        }
        List<ErpSaleQuoteItemDO> items = saleQuoteItemMapper.selectListByQuoteIdForUpdate(quoteId);
        List<ErpSaleConvertRecordDO> records = saleConvertRecordMapper
                .selectQuoteToCartListByQuoteIds(Collections.singleton(quoteId));
        Set<Long> cartIds = records.stream().map(ErpSaleConvertRecordDO::getTargetId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ErpSaleCartDO> cartMap = CollUtil.isEmpty(cartIds) ? Collections.emptyMap()
                : saleCartMapper.selectByIds(cartIds).stream()
                .collect(Collectors.toMap(ErpSaleCartDO::getId, cart -> cart));
        List<ErpSaleConvertRecordDO> activeRecords = records.stream()
                .filter(record -> cartMap.containsKey(record.getTargetId())).collect(Collectors.toList());
        Progress progress = buildProgress(quote, items, activeRecords, cartMap);

        Map<Long, BigDecimal> itemConvertedMap = activeRecords.stream()
                .filter(record -> record.getSourceItemId() != null)
                .collect(Collectors.groupingBy(ErpSaleConvertRecordDO::getSourceItemId,
                        Collectors.reducing(BigDecimal.ZERO,
                                record -> zero(record.getCount()), BigDecimal::add)));
        items.forEach(item -> {
            BigDecimal convertedCount = itemConvertedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (item.getConvertedCount() == null || item.getConvertedCount().compareTo(convertedCount) != 0) {
                saleQuoteItemMapper.updateById(new ErpSaleQuoteItemDO().setId(item.getId())
                        .setConvertedCount(convertedCount));
            }
        });
        if (canRefreshStatus(quote.getStatus()) && !quote.getStatus().equals(progress.getRawStatus())) {
            saleQuoteMapper.updateById(new ErpSaleQuoteDO().setId(quoteId).setStatus(progress.getRawStatus()));
        }
        return progress;
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshByCartIds(Collection<Long> cartIds) {
        if (CollUtil.isEmpty(cartIds)) {
            return;
        }
        Set<Long> quoteIds = saleConvertRecordMapper.selectQuoteToCartListByCartIds(cartIds).stream()
                .map(ErpSaleConvertRecordDO::getSourceId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        quoteIds.forEach(this::refreshQuoteProgress);
    }

    private Progress buildProgress(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> sourceItems,
                                   List<ErpSaleConvertRecordDO> sourceRecords,
                                   Map<Long, ErpSaleCartDO> cartMap) {
        List<ErpSaleQuoteItemDO> items = sourceItems != null ? sourceItems : Collections.emptyList();
        List<ErpSaleConvertRecordDO> records = sourceRecords != null ? sourceRecords : Collections.emptyList();
        BigDecimal totalCount = items.stream().map(ErpSaleQuoteItemDO::getCount)
                .map(ErpSaleQuoteProgressService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal convertedCount = records.stream().map(ErpSaleConvertRecordDO::getCount)
                .map(ErpSaleQuoteProgressService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualOutCount = records.stream()
                .filter(record -> {
                    ErpSaleCartDO cart = cartMap.get(record.getTargetId());
                    return cart != null && ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus());
                })
                .map(ErpSaleConvertRecordDO::getCount).map(ErpSaleQuoteProgressService::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 兼容旧版“报价审核直接生成销售单”的历史 50 状态：禁止重复出库并按已实际出库展示。
        boolean historicalDirectOut = ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())
                && records.isEmpty();
        if (historicalDirectOut) {
            convertedCount = totalCount;
            actualOutCount = totalCount;
        }
        BigDecimal remainingCount = totalCount.subtract(convertedCount).max(BigDecimal.ZERO);
        Integer rawStatus = resolveRawStatus(quote.getStatus(), totalCount, convertedCount, actualOutCount);
        Integer orderStatus = ErpSaleQuoteStatusEnum.CANCEL.getStatus().equals(quote.getStatus())
                ? ErpSaleQuoteStatusEnum.CANCEL.getStatus()
                : (quote.getStatus() != null && quote.getStatus() >= ErpSaleQuoteStatusEnum.APPROVE.getStatus()
                ? ErpSaleQuoteStatusEnum.APPROVE.getStatus() : ErpSaleQuoteStatusEnum.DRAFT.getStatus());
        Integer outStatus = ErpSaleQuoteStatusEnum.CANCEL.getStatus().equals(rawStatus)
                ? ErpSaleQuoteStatusEnum.DRAFT.getStatus()
                : rawStatus >= ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()
                ? ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()
                : rawStatus >= ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus()
                ? ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus()
                : rawStatus >= ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus()
                ? ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus() : ErpSaleQuoteStatusEnum.DRAFT.getStatus();
        return new Progress(quote.getId(), rawStatus, orderStatus, outStatus,
                convertedCount, actualOutCount, remainingCount);
    }

    private Integer resolveRawStatus(Integer currentStatus, BigDecimal totalCount,
                                     BigDecimal convertedCount, BigDecimal actualOutCount) {
        if (!canRefreshStatus(currentStatus)) {
            return currentStatus;
        }
        if (totalCount.compareTo(BigDecimal.ZERO) > 0 && actualOutCount.compareTo(totalCount) >= 0) {
            return ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus();
        }
        if (totalCount.compareTo(BigDecimal.ZERO) > 0 && convertedCount.compareTo(totalCount) >= 0) {
            return ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus();
        }
        if (convertedCount.compareTo(BigDecimal.ZERO) > 0) {
            return ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus();
        }
        return ErpSaleQuoteStatusEnum.APPROVE.getStatus();
    }

    private boolean canRefreshStatus(Integer status) {
        return status != null && status >= ErpSaleQuoteStatusEnum.APPROVE.getStatus()
                && status < ErpSaleQuoteStatusEnum.CANCEL.getStatus();
    }

    private static BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Data
    @AllArgsConstructor
    public static class Progress {
        private Long quoteId;
        private Integer rawStatus;
        private Integer orderStatus;
        private Integer outStatus;
        private BigDecimal convertedCount;
        private BigDecimal actualOutCount;
        private BigDecimal remainingCount;
    }

}
